package net.baldy.pixelstats.network;

import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.stats.IVStore;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.PokemonStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class RequestPokemonIvsPacket {

	private final UUID pokemonUuid;

	public RequestPokemonIvsPacket(UUID pokemonUuid) {
		this.pokemonUuid = pokemonUuid;
	}

	public static void encode(RequestPokemonIvsPacket msg, PacketBuffer buf) {
		buf.writeUUID(msg.pokemonUuid);
	}

	public static RequestPokemonIvsPacket decode(PacketBuffer buf) {
		return new RequestPokemonIvsPacket(buf.readUUID());
	}

	public static void handle(RequestPokemonIvsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ServerPlayerEntity player = ctx.getSender();

		ctx.enqueueWork(() -> {
			if (player == null) {
				return;
			}

			Pokemon pokemon = findPokemon(player, msg.pokemonUuid);
			if (pokemon == null || pokemon.getIVs() == null) {
				NetworkHandler.INSTANCE.send(
					PacketDistributor.PLAYER.with(() -> player),
					new SyncPokemonIvsPacket(msg.pokemonUuid, false, 0, false)
				);
				return;
			}

			IVStore ivs = pokemon.getIVs();

			NetworkHandler.INSTANCE.send(
				PacketDistributor.PLAYER.with(() -> player),
				new SyncPokemonIvsPacket(
					msg.pokemonUuid,
					true,
					ivs.getTotal(),
					ivs.isHyperTrained(BattleStatsType.ATTACK) ||
						ivs.isHyperTrained(BattleStatsType.DEFENSE) ||
						ivs.isHyperTrained(BattleStatsType.HP) ||
						ivs.isHyperTrained(BattleStatsType.SPECIAL_ATTACK) ||
						ivs.isHyperTrained(BattleStatsType.SPECIAL_DEFENSE) ||
						ivs.isHyperTrained(BattleStatsType.SPEED)
				)
			);
		});

		ctx.setPacketHandled(true);
	}

	private static Pokemon findPokemon(ServerPlayerEntity player, UUID pokemonUuid) {
		PlayerPartyStorage party = StorageProxy.getParty(player);

		if (party != null) {
			Pokemon fromParty = party.find(pokemonUuid);
			if (fromParty != null) {
				return fromParty;
			}
		}

		PokemonStorage pc = StorageProxy.getPCForPlayer(player);
		if (pc != null) {
			return pc.find(pokemonUuid);
		}

		return null;
	}
}
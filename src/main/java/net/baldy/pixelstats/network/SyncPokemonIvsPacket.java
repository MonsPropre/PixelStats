package net.baldy.pixelstats.network;

import net.baldy.pixelstats.client.ClientIvsCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncPokemonIvsPacket {

	public final UUID pokemonUuid;
	public final boolean valid;
	public final int total;
	public final boolean hyperTrained;

	public SyncPokemonIvsPacket(UUID pokemonUuid, boolean valid, int total, boolean hyperTrained) {
		this.pokemonUuid = pokemonUuid;
		this.valid = valid;
		this.total = total;
		this.hyperTrained = hyperTrained;
	}

	public static void encode(SyncPokemonIvsPacket msg, PacketBuffer buf) {
		buf.writeUUID(msg.pokemonUuid);
		buf.writeBoolean(msg.valid);
		buf.writeInt(msg.total);
		buf.writeBoolean(msg.hyperTrained);
	}

	public static SyncPokemonIvsPacket decode(PacketBuffer buf) {
		return new SyncPokemonIvsPacket(
			buf.readUUID(),
			buf.readBoolean(),
			buf.readInt(),
			buf.readBoolean()
		);
	}

	public static void handle(SyncPokemonIvsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();

		ctx.enqueueWork(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) {
				return;
			}

			ClientIvsCache.put(
				msg.pokemonUuid,
				new ClientIvsCache.IvData(
					msg.valid,
					msg.total,
					msg.hyperTrained,
					System.currentTimeMillis(),
					false
				)
			);
		});

		ctx.setPacketHandled(true);
	}
}
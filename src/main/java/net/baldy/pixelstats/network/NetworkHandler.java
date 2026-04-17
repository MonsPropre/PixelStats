package net.baldy.pixelstats.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

import static net.baldy.pixelstats.PixelStats.MOD_ID;

public final class NetworkHandler {
	private static final String PROTOCOL = "1";
	public static SimpleChannel INSTANCE;

	public static void register() {
		if (INSTANCE != null) {
			return;
		}

		INSTANCE = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(MOD_ID, "main"),
			() -> PROTOCOL,
			PROTOCOL::equals,
			PROTOCOL::equals
		);

		int id = 0;

		INSTANCE.registerMessage(
			id++,
			RequestPokemonIvsPacket.class,
			RequestPokemonIvsPacket::encode,
			RequestPokemonIvsPacket::decode,
			RequestPokemonIvsPacket::handle,
			Optional.of(NetworkDirection.PLAY_TO_SERVER)
		);

		INSTANCE.registerMessage(
			id++,
			SyncPokemonIvsPacket.class,
			SyncPokemonIvsPacket::encode,
			SyncPokemonIvsPacket::decode,
			SyncPokemonIvsPacket::handle,
			Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}

	private NetworkHandler() {
	}
}
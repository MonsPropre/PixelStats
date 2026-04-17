package net.baldy.pixelstats;

import net.baldy.pixelstats.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
D:\Users\elazi\Documents\GitHub\PixelStats
@Mod(PixelStats.MOD_ID)
public class PixelStats {
	public static final String MOD_ID = "pixelstats";
	private static final Logger LOGGER = LogManager.getLogger();

	public PixelStats() {
		MinecraftForge.EVENT_BUS.register(this);

		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(this::commonSetup);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(NetworkHandler::register);
	}

	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		LOGGER.info("PixelStats starting");
	}
}
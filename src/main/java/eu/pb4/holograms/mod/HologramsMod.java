package eu.pb4.holograms.mod;

import eu.pb4.holograms.interfaces.HologramHolder;
import eu.pb4.holograms.mod.hologram.HoloServerWorld;
import eu.pb4.holograms.mod.hologram.StoredHologram;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class HologramsMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("Holograms");
	public static String VERSION = FabricLoader.getInstance().getModContainer("holograms").get().getMetadata().getVersion().getFriendlyString();


	@Override
	public void onInitialize() {
		HologramCommand.register();
		ServerChunkEvents.CHUNK_LOAD.register(((world, chunk) -> {
			List<StoredHologram> holograms = ((HoloServerWorld) world).getHologramManager().hologramsByChunk.get(chunk.getPos());
			if (holograms != null) {
				for (StoredHologram hologram : holograms) {
					hologram.show();
					((HologramHolder) chunk).addHologram(hologram);
				}
			}
		}));
	}
}

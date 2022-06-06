package eu.pb4.holograms.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HologramsMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Holograms");
    public static String VERSION = FabricLoader.getInstance().getModContainer("holograms").get().getMetadata().getVersion().getFriendlyString();


    @Override
    public void onInitialize() {
        HologramCommand.register();
    }
}

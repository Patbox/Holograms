package eu.pb4.holograms.mod.mixin;

import eu.pb4.holograms.mod.hologram.HoloServerWorld;
import eu.pb4.holograms.mod.hologram.HologramManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Spawner;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements HoloServerWorld {
    @Unique private HologramManager hologramManager;

    @Shadow public abstract PersistentStateManager getPersistentStateManager();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initiateHolograms(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> registryKey, DimensionType dimensionType, WorldGenerationProgressListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long l, List<Spawner> list, boolean bl, CallbackInfo ci) {
        this.hologramManager = this.getPersistentStateManager().getOrCreate(() -> new HologramManager((ServerWorld) (Object)this), "holograms");
    }

    @Override
    public HologramManager getHologramManager() {
        return this.hologramManager;
    }
}

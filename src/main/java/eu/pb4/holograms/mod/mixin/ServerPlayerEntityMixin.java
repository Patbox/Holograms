package eu.pb4.holograms.mod.mixin;

import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.interfaces.HologramHolder;
import eu.pb4.holograms.mod.hologram.StoredHologram;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "sendUnloadChunkPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void clearHologramFromChunk(ChunkPos chunkPos, CallbackInfo ci) {
        for (AbstractHologram hologram : new HashSet<>(((HologramHolder) this).getHologramSet())) {
            if (hologram instanceof StoredHologram storedHologram && new ChunkPos(new BlockPos(storedHologram.getPosition())).equals(chunkPos)) {
                hologram.removePlayer((ServerPlayerEntity) (Object) this);
            }
        }
    }
}

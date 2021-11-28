package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.InteractionType;
import eu.pb4.holograms.api.elements.HologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public record ParticleEmitterPlaceholderElement(StoredElement.ParticleEmitter.Value value) implements HologramElement {
    @Override
    public double getHeight() {
        return 0;
    }

    @Override
    public Vec3d getOffset() {
        return Vec3d.ZERO;
    }

    @Override
    public IntList getEntityIds() {
        return IntList.of();
    }

    @Override
    public void createSpawnPackets(ServerPlayerEntity player, AbstractHologram hologram) {
        // No need
    }

    @Override
    public void createRemovePackets(ServerPlayerEntity player, AbstractHologram hologram) {
        // Same as above
    }

    @Override
    public void updatePosition(ServerPlayerEntity player, AbstractHologram hologram) {
        // Also no need
    }

    @Override
    public void onTick(AbstractHologram abstractHologram) {
        var hologram = (StoredHologram) abstractHologram;

        if (this.value.rate() > 0 && hologram.getWorld().getServer().getTicks() % this.value.rate() == 0) {
            var pos = hologram.getElementPosition(this);
            var particlePacket = new ParticleS2CPacket(
                    this.value.parameters(),
                    this.value.force(),
                    this.value.pos().x + pos.x,
                    this.value.pos().y + pos.y,
                    this.value.pos().z + pos.z,
                    this.value.delta().getX(),
                    this.value.delta().getY(),
                    this.value.delta().getZ(),
                    this.value.speed(),
                    this.value.count()
            );

            for (var player : hologram.getPlayerSet()) {
                player.networkHandler.sendPacket(particlePacket);
            }
        }
    }

    @Override
    public void onClick(AbstractHologram hologram, ServerPlayerEntity player, InteractionType type, @Nullable Hand hand, @Nullable Vec3d vec, int entityId) {
        // You can't click particles
    }
}

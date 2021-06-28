package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.InteractionType;
import eu.pb4.holograms.api.elements.clickable.CubeHitboxHologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class CubeExecutorHologramElement extends CubeHitboxHologramElement {
    private final StoredElement.Executor.Value value;

    public CubeExecutorHologramElement(StoredElement.Executor.Value value) {
        super(value.hitbox().size, Vec3d.ZERO);
        this.value = value;
    }

    @Override
    public void onClick(AbstractHologram hologram, ServerPlayerEntity player, InteractionType type, @Nullable Hand hand, @Nullable Vec3d vec, int entityId) {
        player.getServer().getCommandManager().execute(this.value.mode().toSource.apply(player), this.value.command());
    }
}

package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.clickable.EntityHologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MovingEntityHologramElement extends EntityHologramElement {
    public MovingEntityHologramElement(Entity entity) {
        super(entity);
    }

    @Override
    public void createSpawnPackets(ServerPlayerEntity player, AbstractHologram hologram) {
        super.createSpawnPackets(player, hologram);
        Vec3d target = player.getEyePos();
        Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(this.entity);
        double d = target.x - vec3d.x;
        double e = target.y - vec3d.y;
        double f = target.z - vec3d.z;
        double g = Math.sqrt(d * d + f * f);
        float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875D)));
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F);
        player.networkHandler.sendPacket(new EntityS2CPacket.Rotate(this.entity.getId(), (byte) (yaw * 256.0F / 360.0F), (byte) (pitch * 256.0F / 360.0F), true));
        player.networkHandler.sendPacket(new EntitySetHeadYawS2CPacket(this.entity, (byte) (yaw * 255)));
    }

    @Override
    public void onTick(AbstractHologram hologram) {
        for (ServerPlayerEntity player : hologram.getPlayerSet()) {
            Vec3d target = player.getEyePos();
            Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(this.entity);
            double d = target.x - vec3d.x;
            double e = target.y - vec3d.y;
            double f = target.z - vec3d.z;
            double g = Math.sqrt(d * d + f * f);
            float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875D)));
            float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875D) - 90.0F);
            player.networkHandler.sendPacket(new EntityS2CPacket.Rotate(this.entity.getId(), (byte) (yaw * 256.0F / 360.0F), (byte) (pitch * 256.0F / 360.0F), true));
            player.networkHandler.sendPacket(new EntitySetHeadYawS2CPacket(this.entity, (byte) (yaw * 256.0F / 360.0F)));
        }

        super.onTick(hologram);
    }
}

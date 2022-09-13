package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.impl.HologramHelper;
import eu.pb4.holograms.mixin.accessors.AreaEffectCloudEntityAccessor;
import eu.pb4.holograms.mixin.accessors.EntityAccessor;
import eu.pb4.holograms.mixin.accessors.EntityPositionS2CPacketAccessor;
import eu.pb4.holograms.mixin.accessors.EntityTrackerUpdateS2CPacketAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageShortDistanceHologramElement extends ImageAbstractHologramElement {
    public ImageShortDistanceHologramElement(BufferedImage image, int holoHeight, boolean smooth) {
        super(image, holoHeight, smooth);
    }

    @Override
    public void createSpawnPackets(ServerPlayerEntity player, AbstractHologram hologram) {
        Vec3d pos = hologram.getElementPosition(this);

        for (int i = 0; i < this.holoHeight; i++) {
            var entityId = this.entityIds.getInt(i);
            player.networkHandler.sendPacket(new EntitySpawnS2CPacket(entityId, this.uuids.get(i), pos.x, pos.y - 0.9 + this.getHeightDifference(i, hologram), pos.z, 0, 0, EntityType.AREA_EFFECT_CLOUD, 0, Vec3d.ZERO, 0));

            var packet = HologramHelper.createUnsafe(EntityTrackerUpdateS2CPacket.class);
            EntityTrackerUpdateS2CPacketAccessor accessor = (EntityTrackerUpdateS2CPacketAccessor) packet;
            accessor.setId(entityId);

            List<DataTracker.Entry<?>> data = new ArrayList<>();
            data.add(new DataTracker.Entry<>(AreaEffectCloudEntityAccessor.getRadius(), 0f));
            data.add(new DataTracker.Entry<>(EntityAccessor.getCustomName(), Optional.of(this.texts.get(i))));
            data.add(new DataTracker.Entry<>(EntityAccessor.getNameVisible(), true));
            accessor.setTrackedValues(data);

            player.networkHandler.sendPacket(packet);
        }
    }

    @Override
    public void updatePosition(ServerPlayerEntity player, AbstractHologram hologram) {
        for (int i = 0; i < this.holoHeight; i++) {
            var packet = HologramHelper.createUnsafe(EntityPositionS2CPacket.class);
            EntityPositionS2CPacketAccessor accessor = (EntityPositionS2CPacketAccessor) packet;
            accessor.setId(this.entityIds.getInt(i));
            Vec3d pos = hologram.getElementPosition(this).add(0, this.getHeightDifference(i, hologram), 0);
            accessor.setX(pos.x);
            accessor.setY(pos.y - 0.9);
            accessor.setZ(pos.z);
            accessor.setOnGround(false);
            accessor.setPitch((byte) 0);
            accessor.setYaw((byte) 0);

            player.networkHandler.sendPacket(packet);
        }
    }
}

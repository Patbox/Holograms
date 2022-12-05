package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.impl.HologramHelper;
import eu.pb4.holograms.mixin.accessors.*;
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

public class ImageLongDistanceHologramElement extends ImageAbstractHologramElement {
    public ImageLongDistanceHologramElement(BufferedImage image, int holoHeight, boolean smooth) {
        super(image, holoHeight, smooth);
    }

    @Override
    public void createSpawnPackets(ServerPlayerEntity player, AbstractHologram hologram) {
        var pos = hologram.getElementPosition(this);

        for (int i = 0; i < this.holoHeight; i++) {
            var entityId = this.entityIds.getInt(i);

            player.networkHandler.sendPacket(new EntitySpawnS2CPacket(entityId, this.uuids.get(i), pos.x, pos.y + this.getHeightDifference(i, hologram), pos.z, 0, 0, EntityType.ARMOR_STAND, 0, Vec3d.ZERO, 0));

            {
                List<DataTracker.SerializedEntry<?>> data = new ArrayList<>();
                data.add(DataTracker.SerializedEntry.of(EntityAccessor.getNoGravity(), true));
                data.add(DataTracker.SerializedEntry.of(EntityAccessor.getFlags(), (byte) 0x20));
                data.add(DataTracker.SerializedEntry.of(EntityAccessor.getCustomName(), Optional.of(this.texts.get(i))));
                data.add(DataTracker.SerializedEntry.of(EntityAccessor.getNameVisible(), true));
                data.add(DataTracker.SerializedEntry.of(ArmorStandEntityAccessor.getArmorStandFlags(), (byte) 0x19));

                player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(entityId, data));
            }
        }
    }

    @Override
    public void updatePosition(ServerPlayerEntity player, AbstractHologram hologram) {
        for (int i = 0; i < this.holoHeight; i++) {
            var packet = HologramHelper.createUnsafe(EntityPositionS2CPacket.class);
            var accessor = (EntityPositionS2CPacketAccessor) packet;
            accessor.setId(this.entityIds.getInt(i));
            Vec3d pos = hologram.getElementPosition(this).add(0, this.getHeightDifference(i, hologram), 0);
            accessor.setX(pos.x);
            accessor.setY(pos.y - 0.40);
            accessor.setZ(pos.z);
            accessor.setOnGround(false);
            accessor.setPitch((byte) 0);
            accessor.setYaw((byte) 0);

            player.networkHandler.sendPacket(packet);
        }
    }
}

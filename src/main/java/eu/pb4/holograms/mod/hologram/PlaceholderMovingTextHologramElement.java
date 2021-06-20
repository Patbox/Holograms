package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.text.MovingTextHologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.mixin.accessors.EntityAccessor;
import eu.pb4.holograms.mixin.accessors.EntityTrackerUpdateS2CPacketAccessor;
import eu.pb4.holograms.utils.PacketHelpers;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class PlaceholderMovingTextHologramElement extends MovingTextHologramElement {
    private HashMap<UUID, Text> cache = new HashMap<>();
    private Set<UUID> cleanup = new HashSet<>();

    public PlaceholderMovingTextHologramElement(Text text) {
        super(text);
    }

    @Override
    public Text getTextFor(ServerPlayerEntity player) {
        return PlaceholderAPI.parseText(this.getText(), player);
    }

    @Override
    public void onTick(AbstractHologram hologram) {
        super.onTick(hologram);
        StoredHologram holo = (StoredHologram) hologram;
        boolean clean = holo.getWorld().getTime() % 1200 == 0;
        int updateRate = holo.getUpdateRate();

        for(ServerPlayerEntity player : hologram.getPlayerSet()) {
            if (player.age % updateRate == 0) {
                Text text = this.cache.get(player.getUuid());
                Text out = this.getTextFor(player);
                if (!out.equals(text)) {
                    EntityTrackerUpdateS2CPacket packet = PacketHelpers.createEntityTrackerUpdate();
                    EntityTrackerUpdateS2CPacketAccessor accessor = (EntityTrackerUpdateS2CPacketAccessor) packet;
                    accessor.setId(this.entityId);
                    List<DataTracker.Entry<?>> data = new ArrayList<>();
                    data.add(new DataTracker.Entry<>(EntityAccessor.getCustomName(), Optional.of(out)));
                    accessor.setTrackedValues(data);
                    player.networkHandler.sendPacket(packet);
                    this.cache.put(player.getUuid(), out);
                }

                if (clean) {
                    this.cleanup.add(player.getUuid());
                }
            }
        }

        if (clean) {
            this.cache.keySet().removeIf(uuid -> !this.cleanup.contains(uuid));
            this.cleanup.clear();
        }
    }

    @Override
    public void createRemovePackets(ServerPlayerEntity player, AbstractHologram hologram) {
        super.createRemovePackets(player, hologram);
        this.cache.remove(player.getUuid());
    }
}

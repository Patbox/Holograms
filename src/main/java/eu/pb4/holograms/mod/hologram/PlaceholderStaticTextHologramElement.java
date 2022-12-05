package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.text.StaticTextHologramElement;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.impl.HologramHelper;
import eu.pb4.holograms.mixin.accessors.EntityAccessor;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public class PlaceholderStaticTextHologramElement extends StaticTextHologramElement {
    private final TextNode textNode;
    private HashMap<UUID, Text> cache = new HashMap<>();
    private Set<UUID> cleanup = new HashSet<>();

    public PlaceholderStaticTextHologramElement(TextNode text) {

        super(Text.empty());
        this.textNode = text;
    }

    @Override
    public Text getTextFor(ServerPlayerEntity player) {
        return Placeholders.parseText(this.textNode, PlaceholderContext.of(player));
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
                    List<DataTracker.SerializedEntry<?>> data = new ArrayList<>();
                    data.add(DataTracker.SerializedEntry.of(EntityAccessor.getCustomName(), Optional.of(out)));
                    player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(this.entityId, data));
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

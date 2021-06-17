package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.interfaces.HologramHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

import java.util.*;

public class HologramManager extends PersistentState {
    public static final int VERSION = 1;

    public final ServerWorld world;
    public final Map<ChunkPos, ArrayList<StoredHologram>> hologramsByChunk = new HashMap<>();
    public final Map<UUID, StoredHologram> hologramsByUuid = new HashMap<>();
    public final Map<String, StoredHologram> hologramsByName = new HashMap<>();
    public final Set<StoredHologram> holograms = new HashSet<>();

    public HologramManager(ServerWorld world) {
        super("holograms");
        this.world = world;
    }

    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        nbt.putInt("Version", VERSION);
        ListTag list = new ListTag();
        for (StoredHologram hologram : this.holograms) {
            list.add(hologram.toNbt());
        }

        nbt.put("Holograms", list);

        return nbt;
    }

    public boolean addHologram(StoredHologram hologram) {
        if (!this.hologramsByName.containsKey(hologram.name) && this.holograms.add(hologram)) {
            ChunkPos chunkPos = new ChunkPos(new BlockPos(hologram.getPosition()));
            this.hologramsByChunk.computeIfAbsent(chunkPos, (chunkPos1) -> new ArrayList<>()).add(hologram);
            this.hologramsByUuid.put(hologram.uuid, hologram);
            this.hologramsByName.put(hologram.name, hologram);
            hologram.manager = this;

            if (this.world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                ((HologramHolder) this.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z, false)).addHologram(hologram);
                this.world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(chunkPos, false)
                        .forEach(entity -> {
                            System.out.println("Adding player: " + entity.toString());
                            hologram.addPlayer(entity);
                        });
                hologram.show();
            }
            this.setDirty(true);
            return true;
        }
        return false;
    }

    public boolean removeHologram(StoredHologram hologram) {
        if (this.holograms.remove(hologram)) {
            ChunkPos chunkPos = new ChunkPos(new BlockPos(hologram.getPosition()));
            List<StoredHologram> list = this.hologramsByChunk.get(chunkPos);
            list.remove(hologram);
            if (list.size() == 0) {
                this.hologramsByChunk.remove(chunkPos);
            }

            this.hologramsByUuid.remove(hologram.uuid, hologram);
            this.hologramsByName.remove(hologram.name, hologram);

            hologram.manager = null;

            if (this.world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                ((HologramHolder) this.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z, false)).removeHologram(hologram);
                this.world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(chunkPos, false)
                        .forEach( player -> hologram.removePlayer(player));
                hologram.hide();
            }
            this.setDirty(true);
            return true;
        }
        return false;
    }

    public boolean removeHologram(UUID uuid) {
        StoredHologram hologram = this.hologramsByUuid.get(uuid);
        if (hologram != null) {
            return this.removeHologram(hologram);
        }
        return false;
    }

    public boolean removeHologram(String name) {
        StoredHologram hologram = this.hologramsByName.get(name);
        if (hologram != null) {
            return this.removeHologram(hologram);
        }
        return false;
    }

    public void moveHologram(StoredHologram hologram, Vec3d vec3d) {
        hologram.hide();
        for (ServerPlayerEntity player : hologram.getPlayerSet()) {
            hologram.removePlayer(player);
        }
        Vec3d oldPos = hologram.getPosition();
        ChunkPos oldChunkPos = new ChunkPos(new BlockPos(oldPos.x, oldPos.y, oldPos.z));
        ChunkPos chunkPos = new ChunkPos(new BlockPos(vec3d.x, vec3d.y, vec3d.z));
        hologram.setPosition(vec3d);

        if (!oldChunkPos.equals(chunkPos)) {
            List<StoredHologram> list = this.hologramsByChunk.get(oldChunkPos);
            list.remove(hologram);
            if (list.size() == 0) {
                this.hologramsByChunk.remove(oldChunkPos);
            }

            this.hologramsByChunk.computeIfAbsent(chunkPos, (chunkPos1) -> new ArrayList<>()).add(hologram);
        }

        if (this.world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
            ((HologramHolder) this.world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z, false)).addHologram(hologram);
            this.world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(chunkPos, false)
                    .forEach( player -> hologram.addPlayer(player));
            hologram.show();
        }

        this.markDirty();
    }

    @Override
    public void fromTag(CompoundTag nbt) {
        try {
            int version = nbt.getInt("Version");
            if (version == 1) {
                ListTag list = (ListTag) nbt.get("Holograms");
                for (Tag element : list) {
                    try {
                        this.addHologram(StoredHologram.fromNbt((CompoundTag) element, world));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

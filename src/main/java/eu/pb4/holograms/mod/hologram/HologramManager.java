package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.interfaces.HologramHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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
        this.world = world;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Version", VERSION);
        NbtList list = new NbtList();
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
                        .forEach( player -> hologram.addPlayer(player));
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
        for (ServerPlayerEntity player : new HashSet<>(hologram.getPlayerSet())) {
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


    public static HologramManager fromNbt(ServerWorld world, NbtCompound nbt) {
        HologramManager manager = new HologramManager(world);
        try {
            int version = nbt.getInt("Version");
            if (version == 1) {
                NbtList list = nbt.getList("Holograms", NbtElement.COMPOUND_TYPE);
                for (NbtElement element : list) {
                    try {
                        manager.addHologram(StoredHologram.fromNbt((NbtCompound) element, world));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return manager;
    }


}

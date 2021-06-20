package eu.pb4.holograms.mod.hologram;


import eu.pb4.holograms.api.holograms.AbstractHologram;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StoredHologram extends AbstractHologram {
    protected String name;
    protected UUID uuid;
    protected List<StoredElement<?>> storedElements = new ArrayList<>();
    protected int updateRate;
    protected HologramManager manager = null;

    public StoredHologram(ServerWorld world, Vec3d position, VerticalAlign alignment) {
        super(world, position, alignment);
    }

    public int addElement(StoredElement<?> element) {
        if (this.manager != null) {
            this.manager.markDirty();
        }
        this.storedElements.add(element);
        return super.addElement(element.toElement());
    }

    public void insertElement(int pos, StoredElement<?> element) {
        if (this.manager != null) {
            this.manager.markDirty();
        }
        if (this.storedElements.size() <= pos) {
            this.storedElements.add(element);
            super.addElement(element.toElement());
        } else {
            pos = MathHelper.clamp(pos, 0, this.storedElements.size() - 1);
            this.storedElements.add(pos, element);
            super.addElement(pos, element.toElement());
        }
    }

    public int setElement(int pos, StoredElement<?> element) {
        if (this.manager != null) {
            this.manager.markDirty();
        }
        int delta = pos - this.storedElements.size();
        for (int x = 0; x <= delta; x++) {
            this.addElement(new StoredElement.Space(0.0));
        }

        this.storedElements.set(pos, element);
        return super.setElement(pos, element.toElement());
    }

    public StoredElement<?> removeStoredElement(int pos) {
        if (this.manager != null) {
            this.manager.markDirty();
        }
        super.removeElement(pos);
        return this.storedElements.remove(pos);
    }

    public List<StoredElement<?>> getStoredElements() {
        return this.storedElements;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", this.name);
        nbt.putDouble("x", this.position.x);
        nbt.putDouble("y", this.position.y);
        nbt.putDouble("z", this.position.z);
        nbt.putUuid("uuid", this.uuid);
        nbt.putInt("updateRate", Math.max(this.updateRate, 1));

        NbtList list = new NbtList();

        for (StoredElement<?> element : this.storedElements) {
            list.add(element.toNbt());
        }
        nbt.put("elements", list);

        return nbt;
    }

    public Vec3d getPosition() {
        return this.position;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public void setUpdateRate(int value) {
        if (this.manager != null) {
            this.manager.markDirty();
        }
        this.updateRate = Math.max(value, 1);
    }

    public int getUpdateRate() {
        return this.updateRate;
    }

    public void setPosition(Vec3d vec3d) {
        this.position = vec3d;
    }

    public static StoredHologram fromNbt(NbtCompound tag, ServerWorld world) {
        StoredHologram hologram = new StoredHologram(world, new Vec3d(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")), VerticalAlign.TOP);
        hologram.name = tag.getString("name");
        hologram.uuid = tag.getUuid("uuid");
        hologram.updateRate = Math.max(tag.getInt("updateRate"), 1);

        for(NbtElement element : tag.getList("elements", NbtElement.COMPOUND_TYPE)) {
            StoredElement<?> element1 = StoredElement.fromNbt((NbtCompound) element, world);
            if (element1 != null) {
                hologram.addElement(element1);
            }
        }

        return hologram;
    }

    public static StoredHologram create(String name, ServerWorld world, Vec3d pos) {
        StoredHologram hologram = new StoredHologram(world, pos, VerticalAlign.TOP);
        hologram.name = name;
        hologram.uuid = UUID.randomUUID();
        hologram.updateRate = 20;
        hologram.addElement(new StoredElement.Text("<red><lang:'text.holograms.default_text':'" + name + "'></red>", true));
        return hologram;
    }


    // These aren't needed and could only cause issues;

    @Deprecated
    public int addText(Text text) { return 0; }

    @Deprecated
    public int addText(int pos, Text text) { return 0; }

    @Deprecated
    public int setText(int pos, Text text) { return 0; }

    @Deprecated
    public int addText(Text text, boolean isStatic) { return 0; }

    @Deprecated
    public int addText(int pos, Text text, boolean isStatic) { return 0; }

    @Deprecated
    public int setText(int pos, Text text, boolean isStatic) { return 0; }

    @Deprecated
    public int addItemStack(ItemStack stack, boolean isStatic) { return 0; }

    @Deprecated
    public int addItemStack(int pos, ItemStack stack, boolean isStatic) { return 0; }

    @Deprecated
    public int setItemStack(int pos, ItemStack stack, boolean isStatic) { return 0; }

    @Deprecated
    public int addEntity(Entity entity) { return 0; }

    @Deprecated
    public int addEntity(int pos, Entity entity) { return 0; }

    @Deprecated
    public int setEntity(int pos, Entity entity){ return 0; }
}


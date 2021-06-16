package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.HologramElement;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.elements.clickable.EntityHologramElement;
import eu.pb4.holograms.api.elements.item.AbstractItemHologramElement;
import eu.pb4.holograms.api.elements.text.AbstractTextHologramElement;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.TextParser;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public abstract class StoredElement<T> {
    public static final class Text extends StoredElement<String> {
        public Text(String value, boolean isStatic) {
            super(value, isStatic);
        }

        @Override
        public String getType() {
            return "Text";
        }

        @Override
        protected NbtElement valueAsNbt() {
            return NbtString.of(this.value);
        }

        @Override
        public HologramElement toElement() {
            net.minecraft.text.Text text = TextParser.parse(this.value);
            return (PlaceholderAPI.PLACEHOLDER_PATTERN.matcher(this.value).find())
                    ? this.isStatic
                        ? new PlaceholderStaticTextHologramElement(text)
                        : new PlaceholderMovingTextHologramElement(text)
                    : AbstractTextHologramElement.create(TextParser.parse(this.value), this.isStatic);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return ((MutableText) TextParser.parse(this.value)).fillStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(this.value))));
        }
    }

    public static final class Entity extends StoredElement<net.minecraft.entity.Entity> {
        public Entity(net.minecraft.entity.Entity value) {
            super(value, false);
        }

        @Override
        public String getType() {
            return "Entity";
        }

        @Override
        protected NbtElement valueAsNbt() {
            NbtCompound compound = new NbtCompound();
            if (this.value.saveSelfNbt(compound)) {
                return this.value.writeNbt(compound);
            }
            return null;
        }

        @Override
        public HologramElement toElement() {
            return new EntityHologramElement(this.value);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return this.value.getDisplayName();
        }
    }

    public static final class Item extends StoredElement<ItemStack> {
        public Item(ItemStack value, boolean isStatic) {
            super(value, isStatic);
        }

        @Override
        public String getType() {
            return "Item";
        }

        @Override
        protected NbtElement valueAsNbt() {
            return this.value.writeNbt(new NbtCompound());
        }

        @Override
        public HologramElement toElement() {
            return AbstractItemHologramElement.create(this.value, this.isStatic);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return this.value.toHoverableText();
        }
    }

    public static final class Space extends StoredElement<Double> {
        public Space(Double value) {
            super(value, false);
        }

        @Override
        public String getType() {
            return "Space";
        }

        @Override
        protected NbtElement valueAsNbt() {
            return NbtDouble.of(this.value);
        }

        @Override
        public HologramElement toElement() {
            return new SpacingHologramElement(this.value);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return new TranslatableText("text.holograms.space_height", this.value).formatted(Formatting.GRAY, Formatting.ITALIC);
        }
    }

    public StoredElement(T value, boolean isStatic) {
        this.value = value;
        this.isStatic = isStatic;
    }


    protected  T value;
    protected boolean isStatic;

    public abstract String getType();

    public T getValue() {
        return this.value;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("Type", this.getType());
        nbt.putBoolean("isStatic", this.isStatic());
        nbt.put("Value", this.valueAsNbt());

        return nbt;
    }

    protected abstract NbtElement valueAsNbt();

    public abstract HologramElement toElement();

    public abstract net.minecraft.text.Text toText();

    public static StoredElement<?> fromNbt(NbtCompound compound, World world) {
        try {
            boolean isStatic = compound.getBoolean("isStatic");
            NbtElement value = compound.get("Value");

            return switch (compound.getString("Type")) {
                case "Text" -> new Text(value.asString(), isStatic);
                case "Entity" -> new Entity(EntityType.getEntityFromNbt((NbtCompound) value, world).get());
                case "Item" -> new Item(ItemStack.fromNbt((NbtCompound) value), isStatic);
                case "Space" -> new Space(((NbtDouble) value).doubleValue());
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

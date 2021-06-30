package eu.pb4.holograms.mod.hologram;

import eu.pb4.holograms.api.elements.HologramElement;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.elements.clickable.EntityHologramElement;
import eu.pb4.holograms.api.elements.item.AbstractItemHologramElement;
import eu.pb4.holograms.api.elements.text.AbstractTextHologramElement;
import eu.pb4.holograms.mod.util.TextParser;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;

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
        protected Tag valueAsNbt() {
            return StringTag.of(this.value);
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
        public Entity(net.minecraft.entity.Entity value, boolean isStatic) {
            super(value, isStatic);
        }

        @Override
        public String getType() {
            return "Entity";
        }

        @Override
        protected Tag valueAsNbt() {
            CompoundTag compound = new CompoundTag();
            if (this.value.saveSelfToTag(compound)) {
                return this.value.toTag(compound);
            }
            return null;
        }

        @Override
        public HologramElement toElement() {
            return isStatic ? new EntityHologramElement(this.value) : new MovingEntityHologramElement(this.value);
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
        protected Tag valueAsNbt() {
            return this.value.toTag(new CompoundTag());
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
        protected Tag valueAsNbt() {
            return DoubleTag.of(this.value);
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

    public static final class Executor extends StoredElement<Executor.Value> {
        public Executor(Value value) {
            super(value, false);
        }

        @Override
        public String getType() {
            return "Executor";
        }

        @Override
        protected NbtElement valueAsNbt() {
            NbtCompound compound = new NbtCompound();
            compound.putString("Command", this.value.command);
            compound.putString("Hitbox", this.value.hitbox.name());
            compound.putString("Mode", this.value.mode.name());

            return compound;
        }

        @Override
        public HologramElement toElement() {
            return this.value.hitbox.type == EntityType.SLIME ? new CubeExecutorHologramElement(this.value) : new GeneralExecutorHologramElement(this.value);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return new TranslatableText("text.holograms.executor_name")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)
                            .withItalic(true).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText("text.holograms.executor_hover",
                                    new LiteralText(this.value.hitbox.toString().toLowerCase(Locale.ROOT)).formatted(Formatting.RED),
                                    new LiteralText(this.value.mode.toString().toLowerCase(Locale.ROOT)).formatted(Formatting.GOLD),
                                new LiteralText(this.value.command).formatted(Formatting.WHITE)).formatted(Formatting.YELLOW)
                            )));
        }

        public record Value(Hitbox hitbox, Mode mode, String command) {};

        public enum Hitbox {
            SLIME_SMALL(EntityType.SLIME, 0),
            SLIME_NORMAL(EntityType.SLIME, 1),
            SLIME_BIG(EntityType.SLIME, 2),
            CHICKEN(EntityType.CHICKEN, 0),
            ZOMBIE(EntityType.ZOMBIE, 0),
            PIG(EntityType.PIG, 0),
            GIANT(EntityType.CHICKEN, 0);

            public final EntityType<?> type;
            public final int size;

            Hitbox(EntityType<?> type, int size) {
                this.type = type;
                this.size = size;
            }
        }

        public enum Mode {
            PLAYER(p -> p.getCommandSource()),
            PLAYER_SILENT(p -> p.getCommandSource().withSilent()),
            PLAYER_AS_OP(p -> p.getCommandSource().withLevel(4)),
            PLAYER_AS_OP_SILENT(p -> p.getCommandSource().withLevel(4).withSilent()),
            CONSOLE(p -> p.getServer().getCommandSource()),
            CONSOLE_SILENT(p -> p.getServer().getCommandSource().withSilent());


            public final Function<ServerPlayerEntity, ServerCommandSource> toSource;

            Mode(Function<ServerPlayerEntity, ServerCommandSource> fun) {
                this.toSource = fun;
            }
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

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("Type", this.getType());
        nbt.putBoolean("isStatic", this.isStatic());
        nbt.put("Value", this.valueAsNbt());

        return nbt;
    }

    protected abstract Tag valueAsNbt();

    public abstract HologramElement toElement();

    public abstract net.minecraft.text.Text toText();

    public static StoredElement<?> fromNbt(CompoundTag compound, World world) {
        try {
            boolean isStatic = compound.getBoolean("isStatic");
            Tag value = compound.get("Value");

            switch (compound.getString("Type")) {
                case "Text": return new Text(value.asString(), isStatic);
                case "Entity":
                    return new Entity(EntityType.fromTag((CompoundTag) value).get().create(world));
                case "Item": return new Item(ItemStack.fromTag((CompoundTag) value), isStatic);
                case "Space": return new Space(((DoubleTag) value).getDouble());
                default: return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

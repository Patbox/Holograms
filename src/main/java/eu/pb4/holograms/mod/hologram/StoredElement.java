package eu.pb4.holograms.mod.hologram;

import com.mojang.brigadier.StringReader;
import eu.pb4.holograms.api.elements.HologramElement;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.elements.clickable.EntityHologramElement;
import eu.pb4.holograms.api.elements.item.AbstractItemHologramElement;
import eu.pb4.holograms.api.elements.text.AbstractTextHologramElement;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
        protected NbtElement valueAsNbt() {
            return NbtString.of(this.value);
        }

        @Override
        public HologramElement toElement() {
            return (Placeholders.PLACEHOLDER_PATTERN.matcher(this.value).find())
                    ? this.isStatic
                    ? new PlaceholderStaticTextHologramElement(TextParserUtils.formatNodes(this.value))
                    : new PlaceholderMovingTextHologramElement(TextParserUtils.formatNodes(this.value))
                    : AbstractTextHologramElement.create(TextParserUtils.formatText(this.value), this.isStatic);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return ((MutableText) TextParserUtils.formatText(this.value)).fillStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.text.Text.literal(this.value))));
        }

        @Override
        public String toArgs() {
            return (this.isStatic ? "text" : "long-text") + " " + this.value;
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
        protected NbtElement valueAsNbt() {
            NbtCompound compound = new NbtCompound();
            compound.putString("id", EntityType.getId(this.value.getType()).toString());
            return this.value.writeNbt(compound);
        }

        @Override
        public HologramElement toElement() {
            return isStatic ? new EntityHologramElement(this.value) : new MovingEntityHologramElement(this.value);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return this.value.getDisplayName();
        }

        @Override
        public String toArgs() {
            return null;
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

        @Override
        public String toArgs() {
            var nbt = "";

            if (this.value.getNbt() != null) {
                nbt = new StringNbtWriter().apply(this.value.getNbt());
            }
            return "item nbt " + Registry.ITEM.getId(this.value.getItem()) + nbt + " " + this.isStatic;
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
            return net.minecraft.text.Text.translatable("text.holograms.space_height", this.value).formatted(Formatting.GRAY, Formatting.ITALIC);
        }

        @Override
        public String toArgs() {
            return "space " + this.value.toString();
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
            return net.minecraft.text.Text.translatable("text.holograms.executor_name")
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY)
                            .withItalic(true).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, net.minecraft.text.Text.translatable("text.holograms.executor_hover",
                                    net.minecraft.text.Text.literal(this.value.hitbox.toString().toLowerCase(Locale.ROOT)).formatted(Formatting.RED),
                                    net.minecraft.text.Text.literal(this.value.mode.toString().toLowerCase(Locale.ROOT)).formatted(Formatting.GOLD),
                                    net.minecraft.text.Text.literal(this.value.command).formatted(Formatting.WHITE)).formatted(Formatting.YELLOW)
                            )));
        }

        @Override
        public String toArgs() {
            return "execute " + this.value.hitbox.name().toLowerCase(Locale.ROOT) + " " + this.value.mode.name().toLowerCase(Locale.ROOT) + " " + this.value.command;
        }

        public record Value(Hitbox hitbox, Mode mode, String command) {
        }

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
            PLAYER(net.minecraft.entity.Entity::getCommandSource),
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

    public static final class ParticleEmitter extends StoredElement<ParticleEmitter.Value> {
        public ParticleEmitter(Value value) {
            super(value, false);
        }

        @Override
        public String getType() {
            return "ParticleEmitter";
        }

        @Override
        protected NbtElement valueAsNbt() {
            return this.value.toNbt();
        }

        @Override
        public HologramElement toElement() {
            return new ParticleEmitterPlaceholderElement(this.value);
        }

        @Override
        public net.minecraft.text.Text toText() {
            return net.minecraft.text.Text.translatable("text.holograms.particle",
                    this.value.parameters.asString(),
                    this.value.rate,
                    String.format("%.2f %.2f %.2f", this.value.pos.x, this.value.pos.y, this.value.pos.z),
                    String.format("%.2f %.2f %.2f", this.value.delta.getX(), this.value.delta.getY(), this.value.delta.getZ()),
                    String.format("%.2f", this.value.speed),
                    this.value.count,
                    net.minecraft.text.Text.translatable("text.holograms.particle." + (this.value.force ? "force" : "normal"))
            ).formatted(Formatting.GRAY, Formatting.ITALIC);
        }

        @Override
        public @Nullable String toArgs() {
            return "particle " + this.value.parameters.asString() + " " + this.value.rate + " " + this.value.pos.x + " " + this.value.pos.y
                    + " " + this.value.pos.z + " " + this.value.delta.getX() + " " + this.value.delta.getY() + " " + this.value.delta.getZ()
                    + " " + this.value.speed + " " + this.value.count + " " + (this.value.force ? "force" : "normal");
        }

        public record Value(
                ParticleEffect parameters,
                Vec3d pos,
                Vec3f delta,
                float speed,
                int count,
                boolean force,
                int rate
        ) {
            NbtCompound toNbt() {
                var nbt = new NbtCompound();
                nbt.putString("Type", parameters.asString());
                nbt.putDouble("PosX", pos.x);
                nbt.putDouble("PosY", pos.y);
                nbt.putDouble("PosZ", pos.z);
                nbt.putFloat("DeltaX", delta.getX());
                nbt.putFloat("DeltaY", delta.getY());
                nbt.putFloat("DeltaZ", delta.getZ());
                nbt.putFloat("Speed", speed);
                nbt.putInt("Speed", count);
                nbt.putBoolean("Force", force);
                nbt.putInt("Rate", rate);
                return nbt;
            }

            public static Value fromNbt(NbtCompound nbt) {
                try {
                    return new Value(
                            ParticleEffectArgumentType.readParameters(new StringReader(nbt.getString("Type"))),
                            new Vec3d(nbt.getDouble("PosX"), nbt.getDouble("PosY"), nbt.getDouble("PosZ")),
                            new Vec3f(nbt.getFloat("DeltaX"), nbt.getFloat("DeltaY"), nbt.getFloat("DeltaZ")),
                            nbt.getFloat("Speed"),
                            nbt.getInt("Speed"),
                            nbt.getBoolean("Force"),
                            nbt.getInt("Rate")
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Value(ParticleTypes.AMBIENT_ENTITY_EFFECT, Vec3d.ZERO, Vec3f.ZERO, 0f, 0, false, 0);
                }
            }
        }

    }

    public StoredElement(T value, boolean isStatic) {
        this.value = value;
        this.isStatic = isStatic;
    }


    protected T value;
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

    @Nullable
    public abstract String toArgs();

    public StoredElement<?> copy(World world) {
        return StoredElement.fromNbt(this.toNbt(), world);
    }

    public static StoredElement<?> fromNbt(NbtCompound compound, World world) {
        try {
            boolean isStatic = compound.getBoolean("isStatic");
            NbtElement value = compound.get("Value");

            return switch (compound.getString("Type")) {
                case "Text" -> new Text(value.asString(), isStatic);
                case "Entity" -> new Entity(EntityType.getEntityFromNbt((NbtCompound) value, world).get(), isStatic);
                case "Item" -> new Item(ItemStack.fromNbt((NbtCompound) value), isStatic);
                case "Space" -> new Space(((NbtDouble) value).doubleValue());
                case "Executor" -> new Executor(
                        new Executor.Value(
                                Executor.Hitbox.valueOf(((NbtCompound) value).getString("Hitbox")),
                                Executor.Mode.valueOf(((NbtCompound) value).getString("Mode")),
                                ((NbtCompound) value).getString("Command")
                        )
                );
                case "ParticleEmitter" -> new ParticleEmitter(ParticleEmitter.Value.fromNbt((NbtCompound) value));
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

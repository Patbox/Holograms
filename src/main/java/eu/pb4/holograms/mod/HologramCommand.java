package eu.pb4.holograms.mod;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.pb4.holograms.api.holograms.AbstractHologram;
import eu.pb4.holograms.mod.hologram.HoloServerWorld;
import eu.pb4.holograms.mod.hologram.HologramManager;
import eu.pb4.holograms.mod.hologram.StoredElement;
import eu.pb4.holograms.mod.hologram.StoredHologram;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HologramCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                    var command = dispatcher.register(
                            literal("holograms")
                                    .requires(Permissions.require("holograms.main", true))
                                    .executes(HologramCommand::about)
                                    .then(literal("create")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word())
                                                    .executes(HologramCommand::createHologram)
                                                    .then(argument("pos", Vec3ArgumentType.vec3(true))
                                                            .executes(HologramCommand::createHologram)
                                                    )
                                            )
                                    )
                                    .then(literal("remove")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word()).suggests(HologramCommand::hologramsSuggestion)
                                                    .executes(HologramCommand::removeHologram)
                                            )
                                    )
                                    .then(literal("rename")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word()).suggests(HologramCommand::hologramsSuggestion)
                                                    .then(argument("new_name", StringArgumentType.word())
                                                            .executes(HologramCommand::renameHologram)
                                                    )
                                            )
                                    )
                                    .then(literal("teleportTo")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word()).suggests(HologramCommand::hologramsSuggestion)
                                                    .executes(HologramCommand::teleportToHologram)
                                            )
                                    )
                                    .then(literal("modify")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word()).suggests(HologramCommand::hologramsSuggestion)
                                                    .then(literal("update-rate").then(
                                                                    argument("value", IntegerArgumentType.integer(1))
                                                                            .executes(HologramCommand::changeUpdateRate)
                                                            )
                                                    )
                                                    .then(createAlignment(literal("alignment")))
                                                    .then(literal("position")
                                                            .executes(HologramCommand::moveHologram)
                                                            .then(argument("pos", Vec3ArgumentType.vec3(true))
                                                                    .executes(HologramCommand::moveHologram)
                                                            )
                                                    )
                                                    .then(literal("permission")
                                                            .then(literal("remove").executes(HologramCommand::removePermission))
                                                            .then(literal("set")
                                                                    .then(argument("permission", StringArgumentType.word())
                                                                            .executes(HologramCommand::setPermission)
                                                                            .then(argument("op", IntegerArgumentType.integer(1, 4))
                                                                                    .executes(HologramCommand::setPermission)
                                                                            )
                                                                    )
                                                            )
                                                    )
                                                    .then(literal("lines")
                                                            .then(literal("set").then(
                                                                    HologramCommand.modificationArgument(
                                                                            argument("position", IntegerArgumentType.integer(0)),
                                                                            registryAccess,
                                                                            HologramCommand::setElement
                                                                    )))
                                                            .then(HologramCommand.modificationArgument(
                                                                    literal("add"),
                                                                    registryAccess,
                                                                    HologramCommand::addElement
                                                            ))
                                                            .then(literal("insert").then(
                                                                    HologramCommand.modificationArgument(
                                                                            argument("position", IntegerArgumentType.integer(0)),
                                                                            registryAccess,
                                                                            HologramCommand::insertElement
                                                                    )))
                                                            .then(literal("remove").then(
                                                                    argument("position", IntegerArgumentType.integer(0))
                                                                            .executes(HologramCommand::removeElement)
                                                            ))
                                                    )
                                            )
                                    )

                                    .then(literal("info")
                                            .requires(Permissions.require("holograms.admin", 2))
                                            .then(argument("name", StringArgumentType.word()).suggests(HologramCommand::hologramsSuggestion)
                                                    .executes(HologramCommand::infoHologram)
                                            )
                                    )
                    );

                    dispatcher.register(literal("holo").redirect(command));
                    dispatcher.register(literal("hd").redirect(command));
                }
        );
    }

    private static ArgumentBuilder<ServerCommandSource,?> createAlignment(LiteralArgumentBuilder<ServerCommandSource> alignment) {
        for (var val : AbstractHologram.VerticalAlign.values()) {
            alignment = alignment.then(
                    literal(val.name()).executes(ctx -> changeAlignment(ctx, val))
            );
        }

        return alignment;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> modificationArgument(ArgumentBuilder<ServerCommandSource, ?> base, CommandRegistryAccess registryAccess, ModificationCallback callback) {
        return base
                .then(literal("item").then(
                        literal("hand")
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Item(ctx.getSource().getPlayer().getMainHandStack(), false)))
                                .then(argument("static", BoolArgumentType.bool()).executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Item(ctx.getSource().getPlayer().getMainHandStack(), ctx.getArgument("static", Boolean.class))))
                                )
                ).then(
                        literal("nbt").then(
                                argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                        .executes(ctx -> callback.modify(ctx,
                                                new StoredElement.Item(ctx.getArgument("item", ItemStackArgument.class).createStack(1, false), false)))
                                        .then(argument("static", BoolArgumentType.bool()).executes(ctx -> callback.modify(ctx,
                                                new StoredElement.Item(ctx.getArgument("item", ItemStackArgument.class).createStack(1, false), ctx.getArgument("static", Boolean.class))))
                                        )
                        )
                )).then(literal("image").then(
                        argument("image", StringArgumentType.string()).then(
                                argument("height", IntegerArgumentType.integer(1)).then(
                                        argument("smooth", BoolArgumentType.bool()).then(
                                                argument("long_distance", BoolArgumentType.bool())
                                                        .executes(ctx -> callback.modify(ctx,
                                                                new StoredElement.Image(
                                                                        new StoredElement.Image.Value(StringArgumentType.getString(ctx, "image"), IntegerArgumentType.getInteger(ctx, "height"), BoolArgumentType.getBool(ctx, "smooth")),
                                                                        BoolArgumentType.getBool(ctx, "long_distance")))
                                                        )
                                        ).executes(ctx -> callback.modify(ctx,
                                                new StoredElement.Image(
                                                        new StoredElement.Image.Value(StringArgumentType.getString(ctx, "image"), IntegerArgumentType.getInteger(ctx, "height"), BoolArgumentType.getBool(ctx, "smooth")),
                                                        false)))

                                ).executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Image(
                                                new StoredElement.Image.Value(StringArgumentType.getString(ctx, "image"), IntegerArgumentType.getInteger(ctx, "height"), false),
                                                false)))
                        )
                )).then(literal("text").then(
                        argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Text(ctx.getArgument("text", String.class), true)))
                )).then(literal("text-long").then(
                        argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Text(ctx.getArgument("text", String.class), false)))
                )).then(literal("space").then(
                        argument("size", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Space(ctx.getArgument("size", Double.class))))
                )).then(literal("execute")
                        .requires(Permissions.require("holograms.executable", 4))
                        .then(
                                argument("hitbox", StringArgumentType.word())
                                        .suggests(HologramCommand::hitboxSuggestion)
                                        .then(argument("mode", StringArgumentType.word())
                                                .suggests(HologramCommand::modeSuggestion)
                                                .then(argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            String hitboxS = ctx.getArgument("hitbox", String.class);
                                                            try {
                                                                StoredElement.Executor.Hitbox hitbox = StoredElement.Executor.Hitbox.valueOf(hitboxS.toUpperCase(Locale.ROOT));
                                                                String modeS = ctx.getArgument("mode", String.class);
                                                                try {
                                                                    StoredElement.Executor.Mode mode = StoredElement.Executor.Mode.valueOf(modeS.toUpperCase(Locale.ROOT));
                                                                    return callback.modify(ctx,
                                                                            new StoredElement.Executor(new StoredElement.Executor.Value(hitbox, mode, ctx.getArgument("command", String.class))));
                                                                } catch (Exception e) {
                                                                    ctx.getSource().sendFeedback(Text.translatable("text.holograms.invalid_mode", Text.literal(modeS).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
                                                                    return 0;
                                                                }
                                                            } catch (Exception e) {
                                                                ctx.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hitbox", Text.literal(hitboxS).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                        )).then(literal("copy").then(
                        argument("base", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                            ServerWorld world = ctx.getSource().getWorld();
                                            HologramManager manager = ((HoloServerWorld) world).getHologramManager();
                                            String name = ctx.getArgument("name", String.class);

                                            if (!manager.hologramsByName.containsKey(name)) {
                                                ctx.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
                                                return 0;
                                            } else {
                                                List<StoredElement<?>> elements = manager.hologramsByName.get(name).getStoredElements();
                                                int baseLine = ctx.getArgument("base", Integer.class);

                                                if (elements.size() - 1 < baseLine) {
                                                    ctx.getSource().sendFeedback(Text.translatable("text.holograms.non_existing_line", Text.literal(name).formatted(Formatting.GOLD), baseLine).formatted(Formatting.RED), false);
                                                    return 0;
                                                }

                                                return callback.modify(ctx, elements.get(baseLine).copy(ctx.getSource().getWorld()));
                                            }
                                        }
                                )
                )).then(literal("entity").then(
                        argument("entity", EntitySummonArgumentType.entitySummon())
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Entity(
                                                Registry.ENTITY_TYPE.get(ctx.getArgument("entity", Identifier.class))
                                                        .create(ctx.getSource().getWorld()), true))
                                ).then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                        .executes(ctx -> {
                                                    NbtCompound nbtCompound = ctx.getArgument("nbt", NbtCompound.class);
                                                    nbtCompound.putString("id", ctx.getArgument("entity", Identifier.class).toString());
                                                    ServerWorld serverWorld = ctx.getSource().getWorld();
                                                    Entity entity2 = EntityType.loadEntityWithPassengers(nbtCompound, serverWorld, (entity) -> entity);

                                                    return callback.modify(ctx,
                                                            new StoredElement.Entity(entity2, true));
                                                }
                                        ).then(argument("lookAtPlayer", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    NbtCompound nbtCompound = ctx.getArgument("nbt", NbtCompound.class);
                                                    nbtCompound.putString("id", ctx.getArgument("entity", Identifier.class).toString());
                                                    ServerWorld serverWorld = ctx.getSource().getWorld();
                                                    Entity entity2 = EntityType.loadEntityWithPassengers(nbtCompound, serverWorld, (entity) -> entity);

                                                    return callback.modify(ctx,
                                                            new StoredElement.Entity(entity2, !ctx.getArgument("lookAtPlayer", Boolean.class)));
                                                })
                                        )

                                )
                )).then(literal("particle").then(
                        argument("particle", ParticleEffectArgumentType.particleEffect())
                                .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                        20, Vec3d.ZERO, Vec3d.ZERO, 0.0F, 0, false))
                                .then(argument("updateRate", IntegerArgumentType.integer(1))
                                        .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                                IntegerArgumentType.getInteger(ctx, "updateRate"), Vec3ArgumentType.getVec3(ctx, "pos"),
                                                Vec3d.ZERO, 0.0F, 0, false))
                                        .then(argument("pos", Vec3ArgumentType.vec3())
                                                .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                                        IntegerArgumentType.getInteger(ctx, "updateRate"), Vec3ArgumentType.getVec3(ctx, "pos"),
                                                        Vec3d.ZERO, 0.0F, 0, false))

                                                .then(argument("delta", Vec3ArgumentType.vec3(false)).then(argument("speed", FloatArgumentType.floatArg(0.0F))
                                                        .then((argument("count", IntegerArgumentType.integer(0))
                                                                .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                                                        IntegerArgumentType.getInteger(ctx, "updateRate"), Vec3ArgumentType.getVec3(ctx, "pos"),
                                                                        Vec3ArgumentType.getVec3(ctx, "delta"), FloatArgumentType.getFloat(ctx, "speed"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"), false)))

                                                                .then(literal("force")
                                                                        .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                                                                IntegerArgumentType.getInteger(ctx, "updateRate"),
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"), Vec3ArgumentType.getVec3(ctx, "delta"),
                                                                                FloatArgumentType.getFloat(ctx, "speed"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"), true)))
                                                                .then(literal("normal")
                                                                        .executes((ctx) -> createParticle(ctx, callback, ParticleEffectArgumentType.getParticle(ctx, "particle"),
                                                                                IntegerArgumentType.getInteger(ctx, "updateRate"),
                                                                                Vec3ArgumentType.getVec3(ctx, "pos"), Vec3ArgumentType.getVec3(ctx, "delta"),
                                                                                FloatArgumentType.getFloat(ctx, "speed"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"), false)))
                                                        ))
                                                ))
                                )
                ));
    }

    private static int createParticle(CommandContext<ServerCommandSource> context, ModificationCallback callback, ParticleEffect particle, int rate, Vec3d pos, Vec3d delta, float speed, int count, boolean force) {
        return callback.modify(context, new StoredElement.ParticleEmitter(new StoredElement.ParticleEmitter.Value(
                particle,
                pos,
                new Vec3f((float) delta.x, (float) delta.y, (float) delta.z),
                speed,
                count,
                force,
                rate
        )));
    }

    private static int createHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        Vec3d position;
        try {
            position = context.getArgument("pos", PosArgument.class).toAbsolutePos(context.getSource());
        } catch (Exception e) {
            position = context.getSource().getPosition();
        }

        String name = context.getArgument("name", String.class);

        if (manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.already_exist", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = StoredHologram.create(name, world, position);
            manager.addHologram(hologram);
            context.getSource().sendFeedback(Text.translatable("text.holograms.created",
                    Text.literal(name).formatted(Formatting.GOLD),
                    Text.literal(String.format("%.2f %.2f %.2f", position.x, position.y, position.z)).formatted(Formatting.GRAY)
            ), false);
            return 1;
        }
    }

    private static int removeHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            manager.removeHologram(name);
            context.getSource().sendFeedback(Text.translatable("text.holograms.deleted", Text.literal(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int renameHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);
        String new_name = context.getArgument("new_name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            if (manager.hologramsByName.containsKey(new_name)) {
                context.getSource().sendFeedback(Text.translatable("text.holograms.already_exist", Text.literal(new_name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            } else {
                StoredHologram hologram = manager.hologramsByName.remove(name);
                hologram.setName(new_name);
                manager.hologramsByName.put(new_name, hologram);
                context.getSource().sendFeedback(Text.translatable("text.holograms.renamed", Text.literal(name).formatted(Formatting.GOLD), Text.literal(new_name).formatted(Formatting.GOLD)), false);
            }
            return 1;
        }
    }


    private static int moveHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        Vec3d position;
        try {
            position = context.getArgument("pos", PosArgument.class).toAbsolutePos(context.getSource());
        } catch (Exception e) {
            position = context.getSource().getPosition();
        }

        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            manager.moveHologram(hologram, position);
            context.getSource().sendFeedback(Text.translatable("text.holograms.moved",
                    Text.literal(name).formatted(Formatting.GOLD),
                    Text.literal(String.format("%.2f %.2f %.2f", position.x, position.y, position.z)).formatted(Formatting.GRAY)
            ), false);
            return 1;
        }
    }

    private static int teleportToHologram(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            player.teleport(hologram.getPosition().x, hologram.getPosition().y, hologram.getPosition().z);
            return 1;
        }
    }

    private static int changeUpdateRate(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            int updateRate = context.getArgument("value", Integer.class);
            StoredHologram hologram = manager.hologramsByName.get(name);
            hologram.setUpdateRate(updateRate);
            context.getSource().sendFeedback(Text.translatable("text.holograms.changed_update_rate",
                    Text.literal("" + updateRate).formatted(Formatting.YELLOW),
                    Text.literal(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int changeAlignment(CommandContext<ServerCommandSource> context, AbstractHologram.VerticalAlign alignment) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            hologram.setAlignment(alignment);
            context.getSource().sendFeedback(Text.translatable("text.holograms.changed_alignment",
                    Text.literal("" + alignment).formatted(Formatting.YELLOW),
                    Text.literal(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int setPermission(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);

            int operator = 4;
            try {
                operator = context.getArgument("op", Integer.class);
            } catch (Exception e) {
                // Non issue
            }
            String permission = context.getArgument("permission", String.class);

            hologram.setPermissions(permission, operator);

            context.getSource().sendFeedback(Text.translatable("text.holograms.permission_changed",
                    Text.literal(name).formatted(Formatting.GOLD),
                    Text.literal(permission).formatted(Formatting.YELLOW),
                    Text.literal("" + operator).formatted(Formatting.YELLOW)), false);
            return 1;
        }
    }

    private static int removePermission(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);

            hologram.setPermissions("", 0);
            context.getSource().sendFeedback(Text.translatable("text.holograms.permission_removed",
                    Text.literal(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int infoHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            Vec3d pos = hologram.getPosition();
            MutableText text = Text.literal("").append(Text.translatable("text.holograms.info_name",
                            Text.literal(hologram.getName()).formatted(Formatting.BOLD).formatted(Formatting.GOLD),
                            Text.literal(String.format("%.2f %.2f %.2f", pos.x, pos.y, pos.z)).formatted(Formatting.GRAY)))
                    .append("\n")
                    .append(Text.translatable("text.holograms.info_update_rate", Text.literal("" + hologram.getUpdateRate()).formatted(Formatting.YELLOW)))
                    .append("\n")
                    .append(Text.translatable("text.holograms.info_alignment", Text.literal("" + hologram.getAlignment().name()).formatted(Formatting.GOLD)))
                    .append("\n")
                    ;

            context.getSource().sendFeedback(text, false);

            int x = 0;
            for (StoredElement<?> element : hologram.getStoredElements()) {
                var arg = element.toArgs();

                MutableText text1 = Text.literal("")
                        .append(
                                Text.translatable("[%s] ", Text.literal("" + x).formatted(Formatting.WHITE))
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/holograms modify " + hologram.getName() + " lines set " + x + " " + (arg != null ? arg : "")))
                                                .withColor(Formatting.DARK_GRAY)
                                        )
                        )
                        .append(element.toText());

                context.getSource().sendFeedback(text1, false);

                x++;
            }

            return 1;
        }
    }

    private static int removeElement(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            if (hologram.getElements().size() - 1 < pos) {
                context.getSource().sendFeedback(Text.translatable("text.holograms.non_existing_line", Text.literal(name).formatted(Formatting.GOLD), pos).formatted(Formatting.RED), false);
                return 0;
            }

            StoredElement<?> element = hologram.removeStoredElement(pos);
            context.getSource().sendFeedback(Text.translatable("text.holograms.removed_line", Text.literal(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    private static int insertElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            hologram.insertElement(pos, element);
            context.getSource().sendFeedback(Text.translatable("text.holograms.inserted_line", Text.literal(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    /*private static int moveElement(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            int posNew = context.getArgument("new-position", Integer.class);

            if (hologram.getElements().size() - 1 < pos) {
                context.getSource().sendFeedback(Text.translatable("text.holograms.non_existing_line", Text.literal(name).formatted(Formatting.GOLD), pos).formatted(Formatting.RED), false);
                return 0;
            }


            hologram.insertElement(posNew, hologram.removeStoredElement(pos));
            context.getSource().sendFeedback(Text.translatable("text.holograms.moved_line", Text.literal(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }
*/
    private static int addElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            hologram.addElement(element);
            context.getSource().sendFeedback(Text.translatable("text.holograms.added_line", Text.literal(name).formatted(Formatting.GOLD), element.toText()), false);
            return 1;
        }
    }

    private static int setElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(Text.translatable("text.holograms.invalid_hologram", Text.literal(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            hologram.setElement(pos, element);
            context.getSource().sendFeedback(Text.translatable("text.holograms.changed_line", Text.literal(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("Holograms")
                .formatted(Formatting.GREEN)
                .append(Text.literal(" - " + HologramsMod.VERSION)
                        .formatted(Formatting.WHITE)
                ), false);

        return 1;
    }

    private static CompletableFuture<Suggestions> hologramsSuggestion(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        Set<StoredHologram> holograms = ((HoloServerWorld) context.getSource().getWorld()).getHologramManager().holograms;

        for (var hologram : holograms) {
            if (hologram.getName().toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(hologram.getName());
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> hitboxSuggestion(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (var hitbox : StoredElement.Executor.Hitbox.values()) {
            if (hitbox.name().toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(hitbox.name().toLowerCase(Locale.ROOT));
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> modeSuggestion(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (var hitbox : StoredElement.Executor.Mode.values()) {
            if (hitbox.name().toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(hitbox.name().toLowerCase(Locale.ROOT));
            }
        }
        return builder.buildFuture();
    }


    @FunctionalInterface
    private interface ModificationCallback {
        int modify(CommandContext<ServerCommandSource> context, StoredElement<?> element);
    }
}

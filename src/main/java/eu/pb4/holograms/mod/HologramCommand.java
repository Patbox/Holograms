package eu.pb4.holograms.mod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.pb4.holograms.mod.hologram.HoloServerWorld;
import eu.pb4.holograms.mod.hologram.HologramManager;
import eu.pb4.holograms.mod.hologram.StoredHologram;
import eu.pb4.holograms.mod.hologram.StoredElement;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HologramCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(
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
                                            .then(literal("position")
                                                    .executes(HologramCommand::moveHologram)
                                                    .then(argument("pos", Vec3ArgumentType.vec3(true))
                                                            .executes(HologramCommand::moveHologram)
                                                    )
                                            )
                                            .then(literal("lines")
                                                    .then(literal("set").then(
                                                            HologramCommand.modificationArgument(
                                                                    argument("position", IntegerArgumentType.integer(0)),
                                                                    HologramCommand::setElement
                                                            )))
                                                    .then(HologramCommand.modificationArgument(
                                                            literal("add"),
                                                            HologramCommand::addElement
                                                    ))
                                                    .then(literal("insert").then(
                                                            HologramCommand.modificationArgument(
                                                                    argument("position", IntegerArgumentType.integer(0)),
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
        });
    }

    private static ArgumentBuilder<ServerCommandSource, ?> modificationArgument(ArgumentBuilder<ServerCommandSource, ?> base, ModificationCallback callback) {
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
                        argument("item", ItemStackArgumentType.itemStack())
                                .executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Item(ctx.getArgument("item", ItemStackArgument.class).createStack(1, false), false)))
                                .then(argument("static", BoolArgumentType.bool()).executes(ctx -> callback.modify(ctx,
                                        new StoredElement.Item(ctx.getArgument("item", ItemStackArgument.class).createStack(1, false), ctx.getArgument("static", Boolean.class))))
                                )
                )
        )).then(literal("text").then(
                argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> callback.modify(ctx,
                                new StoredElement.Text(ctx.getArgument("text", String.class), true)))
        )).then(literal("space").then(
                argument("size", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> callback.modify(ctx,
                                new StoredElement.Space(ctx.getArgument("size", Double.class))))
        ));
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
            context.getSource().sendFeedback(new TranslatableText("text.holograms.already_exist", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = StoredHologram.create(name, world, position);
            manager.addHologram(hologram);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.created",
                    new LiteralText(name).formatted(Formatting.GOLD),
                    new LiteralText(String.format("%.2f %.2f %.2f", position.x, position.y, position.z)).formatted(Formatting.GRAY)
            ), false);
            return 1;
        }
    }

    private static int removeHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            manager.removeHologram(name);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.deleted", new LiteralText(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int renameHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);
        String new_name = context.getArgument("new_name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            if (manager.hologramsByName.containsKey(new_name)) {
                context.getSource().sendFeedback(new TranslatableText("text.holograms.already_exist", new LiteralText(new_name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            } else {
                StoredHologram hologram = manager.hologramsByName.remove(name);
                hologram.setName(new_name);
                manager.hologramsByName.put(new_name, hologram);
                context.getSource().sendFeedback(new TranslatableText("text.holograms.renamed", new LiteralText(name).formatted(Formatting.GOLD), new LiteralText(new_name).formatted(Formatting.GOLD)), false);
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
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            manager.moveHologram(hologram, position);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.moved",
                    new LiteralText(name).formatted(Formatting.GOLD),
                    new LiteralText(String.format("%.2f %.2f %.2f", position.x, position.y, position.z)).formatted(Formatting.GRAY)
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
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
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
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            int updateRate = context.getArgument("value", Integer.class);
            StoredHologram hologram = manager.hologramsByName.get(name);
            hologram.setUpdateRate(updateRate);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.changed_update_rate",
                    new LiteralText("" + updateRate).formatted(Formatting.YELLOW),
                    new LiteralText(name).formatted(Formatting.GOLD)), false);
            return 1;
        }
    }

    private static int infoHologram(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            Vec3d pos = hologram.getPosition();
            MutableText text = new LiteralText("").append(new TranslatableText("text.holograms.info_name",
                    new LiteralText(hologram.getName()).formatted(Formatting.BOLD).formatted(Formatting.GOLD),
                    new LiteralText(String.format("%.2f %.2f %.2f", pos.x, pos.y, pos.z)).formatted(Formatting.GRAY)))
                    .append("\n")
                    .append(new TranslatableText("text.holograms.info_update_rate", new LiteralText("" + hologram.getUpdateRate()).formatted(Formatting.YELLOW)))
                    .append("\n");

            context.getSource().sendFeedback(text, false);

            int x = 0;
            for (StoredElement<?> element : hologram.getStoredElements()) {
                MutableText text1 = new LiteralText("")
                        .append(
                                new TranslatableText("[%s] ", new LiteralText("" + x).formatted(Formatting.WHITE))
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/holograms modify " + hologram.getName() + " lines set " + x))
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
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            if (hologram.getElements().size() - 1 < pos) {
                context.getSource().sendFeedback(new TranslatableText("text.holograms.non_existing_line", new LiteralText(name).formatted(Formatting.GOLD), pos).formatted(Formatting.RED), false);
                return 0;
            }

            StoredElement<?> element = hologram.removeStoredElement(pos);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.removed_line", new LiteralText(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    private static int insertElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            hologram.insertElement(pos, element);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.inserted_line", new LiteralText(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    private static int addElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            hologram.addElement(element);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.added_line", new LiteralText(name).formatted(Formatting.GOLD), element.toText()), false);
            return 1;
        }
    }

    private static int setElement(CommandContext<ServerCommandSource> context, StoredElement<?> element) {
        ServerWorld world = context.getSource().getWorld();
        HologramManager manager = ((HoloServerWorld) world).getHologramManager();
        String name = context.getArgument("name", String.class);

        if (!manager.hologramsByName.containsKey(name)) {
            context.getSource().sendFeedback(new TranslatableText("text.holograms.invalid_hologram", new LiteralText(name).formatted(Formatting.GOLD)).formatted(Formatting.RED), false);
            return 0;
        } else {
            StoredHologram hologram = manager.hologramsByName.get(name);
            int pos = context.getArgument("position", Integer.class);
            hologram.setElement(pos, element);
            context.getSource().sendFeedback(new TranslatableText("text.holograms.changed_line", new LiteralText(name).formatted(Formatting.GOLD), pos, element.toText()), false);
            return 1;
        }
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(new LiteralText("Holograms")
                .formatted(Formatting.GREEN)
                .append(new LiteralText(" - " + HologramsMod.VERSION)
                        .formatted(Formatting.WHITE)
                ), false);

        return 1;
    }

    private static CompletableFuture<Suggestions> hologramsSuggestion(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        Set<StoredHologram> holograms = ((HoloServerWorld) context.getSource().getWorld()).getHologramManager().holograms;

        for (var hologram : holograms) {
            if (hologram.getName().contains(remaining)) {
                builder.suggest(hologram.getName());
            }
        }
        return builder.buildFuture();
    }

    @FunctionalInterface
    private interface ModificationCallback {
        int modify(CommandContext<ServerCommandSource> context, StoredElement<?> element);
    }
}

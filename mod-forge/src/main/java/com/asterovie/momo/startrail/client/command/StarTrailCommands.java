package com.asterovie.momo.startrail.client.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Registers the StarTrail client commands. Forge executes client commands locally (see
 * {@code ClientCommandHandler}) and forwards anything it cannot parse to the server, so the
 * server-side {@code /startrail ...} plugin commands keep working.
 */
public final class StarTrailCommands {
    /** Same aliases the Fabric build accepted. */
    private static final String[] ALIASES = new String[]{"startrail", "map", "startrail", "minimap"};

    private StarTrailCommands() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(StarTrailCommands::onRegisterClientCommands);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        for (String alias : ALIASES) {
            event.getDispatcher().register(
                    Commands.literal(alias)
                            .then(Commands.literal("crypt")
                                    .executes(ctx -> EncryptCommand.encrypt(ctx.getSource(), false))
                                    .then(Commands.literal("delete")
                                            .executes(ctx -> EncryptCommand.encrypt(ctx.getSource(), true)))
                                    .then(Commands.literal("help")
                                            .executes(ctx -> EncryptCommand.help(ctx.getSource()))))
                            .then(Commands.literal("nav")
                                    .then(Commands.literal("help")
                                            .executes(ctx -> NavCommand.help(ctx.getSource())))
                                    .then(Commands.literal("stop")
                                            .executes(ctx -> NavCommand.stop(ctx.getSource())))
                                    .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                            .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                    .executes(ctx -> NavCommand.nav(
                                                            ctx.getSource(),
                                                            DoubleArgumentType.getDouble(ctx, "x"),
                                                            DoubleArgumentType.getDouble(ctx, "z")))))));
        }
    }
}

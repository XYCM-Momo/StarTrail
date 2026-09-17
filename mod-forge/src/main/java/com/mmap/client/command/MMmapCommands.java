package com.mmap.client.command;

import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Registers the MMmap client commands. Forge executes client commands locally (see
 * {@code ClientCommandHandler}) and forwards anything it cannot parse to the server, so the
 * server-side {@code /mmap ...} plugin commands keep working.
 */
public final class MMmapCommands {
    /** Same aliases the Fabric build accepted. */
    private static final String[] ALIASES = new String[]{"mmap", "map", "mmmap", "minimap"};

    private MMmapCommands() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(MMmapCommands::onRegisterClientCommands);
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
                                            .executes(ctx -> EncryptCommand.help(ctx.getSource())))));
        }
    }
}

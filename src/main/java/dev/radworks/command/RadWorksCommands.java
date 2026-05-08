package dev.radworks.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.radworks.diagnostics.DiagnosticsService;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RadWorksCommands {
    private RadWorksCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("radworks")
                .then(Commands.literal("version")
                        .executes(context -> showVersion(context.getSource())))
                .then(Commands.literal("dump")
                        .executes(context -> writeDump(context.getSource())))
                .then(Commands.literal("validate")
                        .executes(context -> ValidateCommand.run(context.getSource()))));
    }

    private static int showVersion(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(DiagnosticsService.versionText()), false);
        return 1;
    }

    private static int writeDump(CommandSourceStack source) {
        try {
            Path dumpPath = DiagnosticsService.writeDump(source);
            source.sendSuccess(() -> Component.literal("RadWorks dump created: " + dumpPath), false);
            return 1;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("RadWorks dump failed: " + exception.getMessage()));
            return 0;
        }
    }
}

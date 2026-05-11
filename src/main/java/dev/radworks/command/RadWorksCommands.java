package dev.radworks.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.radworks.diagnostics.DiagnosticsService;
import dev.radworks.diagnostics.WarningBuffer;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.commands.arguments.EntityArgument;
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
                        .executes(context -> ValidateCommand.run(context.getSource())))
                .then(Commands.literal("exposure")
                        .executes(context -> ExposureCommand.runSelf(context.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> ExposureCommand.run(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("sources")
                        .executes(context -> SourcesCommand.runSelf(context.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> SourcesCommand.run(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("effect")
                        .then(Commands.literal("apply-self")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> EffectCommand.applySelfManual(context.getSource())))
                        .then(Commands.literal("clear-self")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> EffectCommand.clearSelfManual(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> EffectCommand.statusSelf(context.getSource()))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("status")
                                .executes(context -> DebugCommand.status(context.getSource())))
                        .then(Commands.literal("on")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> DebugCommand.on(context.getSource())))
                        .then(Commands.literal("off")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> DebugCommand.off(context.getSource())))));
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
        } catch (RuntimeException exception) {
            Throwable cause = exception.getCause();
            String message = cause == null ? exception.getMessage() : cause.getMessage();
            WarningBuffer.add("DUMP_FAILED", "dump", message);
            source.sendFailure(Component.literal("RadWorks dump failed: " + message));
            return 0;
        } catch (IOException exception) {
            WarningBuffer.add("DUMP_FAILED", "dump", exception.getMessage());
            source.sendFailure(Component.literal("RadWorks dump failed: " + exception.getMessage()));
            return 0;
        }
    }
}

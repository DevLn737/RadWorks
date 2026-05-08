package dev.radworks.command;

import dev.radworks.diagnostics.DiagnosticsState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class DebugCommand {
    private DebugCommand() {
    }

    public static int on(CommandSourceStack source) {
        DiagnosticsState.setDebugEnabled(true);
        source.sendSuccess(() -> Component.literal("RadWorks debug: enabled"), true);
        return 1;
    }

    public static int off(CommandSourceStack source) {
        DiagnosticsState.setDebugEnabled(false);
        source.sendSuccess(() -> Component.literal("RadWorks debug: disabled"), true);
        return 1;
    }

    public static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("RadWorks debug: "
                + (DiagnosticsState.isDebugEnabled() ? "enabled" : "disabled")), false);
        return 1;
    }
}

package dev.springrad.tui.app;

import java.util.List;

/** Central registry of all slash commands used across TUI screens. */
public final class CommandRegistry {
    private CommandRegistry() {
    }

    public static final List<CommandDoc> COMMAND_CENTER = List.of(
            new CommandDoc("/generate", "Open project wizard"),
            new CommandDoc("/presets", "Open preset manager"),
            new CommandDoc("/quit", "Exit app (double-enter confirms)"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch theme to Ocean"),
            new CommandDoc("/theme graphite", "Switch theme to Graphite"),
            new CommandDoc("/theme neon", "Switch theme to Neon")
    );

    public static final List<CommandDoc> PRESET_MANAGER = List.of(
            new CommandDoc("/list", "List available presets"),
            new CommandDoc("/save", "Save/update preset from form fields"),
            new CommandDoc("/delete", "Delete preset by name"),
            new CommandDoc("/back", "Return to command center"),
            new CommandDoc("/quit", "Exit from preset manager"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch theme to Ocean"),
            new CommandDoc("/theme graphite", "Switch theme to Graphite"),
            new CommandDoc("/theme neon", "Switch theme to Neon")
    );
}

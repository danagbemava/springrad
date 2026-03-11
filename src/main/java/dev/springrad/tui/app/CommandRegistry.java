package dev.springrad.tui.app;

import java.util.List;

/** Central registry of all slash commands used across TUI screens. */
public final class CommandRegistry {
    private CommandRegistry() {
    }

    public static final List<CommandDoc> COMMAND_CENTER = List.of(
            new CommandDoc("/generate", "Open the project wizard to scaffold a new Spring Boot project"),
            new CommandDoc("/presets", "Open the preset manager to list, save, or delete presets"),
            new CommandDoc("/help", "Show available commands and usage tips"),
            new CommandDoc("/quit", "Exit SpringRad"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch to Ocean theme (cool blues)"),
            new CommandDoc("/theme graphite", "Switch to Graphite theme (neutral grays)"),
            new CommandDoc("/theme neon", "Switch to Neon theme (vibrant colors)")
    );

    public static final List<CommandDoc> PRESET_MANAGER = List.of(
            new CommandDoc("/list", "List all available presets with their configuration"),
            new CommandDoc("/save", "Save a new preset from the form fields below"),
            new CommandDoc("/delete", "Delete a preset by name (built-in presets are protected)"),
            new CommandDoc("/help", "Show available commands and usage tips"),
            new CommandDoc("/back", "Return to the command center"),
            new CommandDoc("/quit", "Exit SpringRad"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch to Ocean theme (cool blues)"),
            new CommandDoc("/theme graphite", "Switch to Graphite theme (neutral grays)"),
            new CommandDoc("/theme neon", "Switch to Neon theme (vibrant colors)")
    );
}

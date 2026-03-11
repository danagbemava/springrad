package dev.springrad.tui.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandAutocomplete {
    private CommandAutocomplete() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static List<CommandDoc> filter(String input, List<CommandDoc> commands) {
        String normalized = normalize(input);
        if (!normalized.startsWith("/")) {
            return List.of();
        }
        if ("/".equals(normalized)) {
            return commands;
        }
        List<CommandDoc> matches = new ArrayList<>();
        for (CommandDoc command : commands) {
            if (command.command().startsWith(normalized)) {
                matches.add(command);
            }
        }
        return matches;
    }

    public static String complete(String input, List<CommandDoc> commands) {
        List<CommandDoc> matches = filter(input, commands);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.getFirst().command();
    }
}

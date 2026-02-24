package dev.springrad.tui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

final class ActivityLogStore {
    private final Path logPath;

    ActivityLogStore() {
        this(defaultPath());
    }

    ActivityLogStore(Path logPath) {
        this.logPath = logPath;
    }

    void append(String message) {
        try {
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String line = "[%s] %s%n".formatted(Instant.now(), message);
            Files.writeString(
                    logPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Logging must never break TUI flow.
        }
    }

    List<String> tail(int maxLines) {
        if (maxLines <= 0) {
            return List.of();
        }
        try {
            if (!Files.exists(logPath)) {
                return List.of();
            }
            List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - maxLines);
            return lines.subList(from, lines.size());
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static Path defaultPath() {
        String override = System.getProperty("springrad.activity.log.path");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".springrad", "activity.log");
    }
}

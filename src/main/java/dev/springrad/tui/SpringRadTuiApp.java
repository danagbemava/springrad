package dev.springrad.tui;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class SpringRadTuiApp {
    private final BufferedReader in;
    private final PrintWriter out;

    public SpringRadTuiApp() {
        this(new InputStreamReader(System.in, StandardCharsets.UTF_8), new PrintWriter(System.out, true));
    }

    SpringRadTuiApp(Reader in, PrintWriter out) {
        this.in = new BufferedReader(in);
        this.out = out;
    }

    public InteractiveSelection start(String initialName, List<String> availablePresets) {
        out.println("SpringRad interactive CLI");
        out.println("TamboUI-backed visual screens are next; this is the first guided flow.");
        out.println();

        String presetName = pickPreset(availablePresets);
        String defaultName = defaultIfBlank(initialName, "springrad-app");
        String name = prompt("Project name", defaultName);
        String groupId = prompt("Group ID", "com.example");
        String artifactId = prompt("Artifact ID", slugify(defaultIfBlank(name, defaultName)));
        String javaVersion = prompt("Java version", "21");
        String bootVersion = normalizeBlank(prompt("Spring Boot version (blank = latest)", ""));
        ProjectConfig.Packaging packaging = pickEnum(
                "Packaging",
                List.of(ProjectConfig.Packaging.jar, ProjectConfig.Packaging.war),
                ProjectConfig.Packaging.jar
        );
        ProjectConfig.BuildTool buildTool = pickEnum(
                "Build tool",
                List.of(ProjectConfig.BuildTool.gradle, ProjectConfig.BuildTool.maven),
                ProjectConfig.BuildTool.gradle
        );
        ProjectConfig.AuthStyle authStyle = pickEnum(
                "Auth style",
                List.of(ProjectConfig.AuthStyle.jwt, ProjectConfig.AuthStyle.session, ProjectConfig.AuthStyle.none),
                ProjectConfig.AuthStyle.jwt
        );
        ProjectConfig.Database database = pickEnum(
                "Database",
                List.of(ProjectConfig.Database.postgresql, ProjectConfig.Database.mysql, ProjectConfig.Database.h2),
                ProjectConfig.Database.postgresql
        );
        List<String> extraDependencies = parseCsv(prompt("Extra dependencies (comma-separated)", ""));
        Path output = Path.of(prompt("Output directory", "./" + artifactId));

        out.println();
        out.println("Summary");
        out.println("  preset      : " + presetName);
        out.println("  name        : " + name);
        out.println("  groupId     : " + groupId);
        out.println("  artifactId  : " + artifactId);
        out.println("  javaVersion : " + javaVersion);
        out.println("  bootVersion : " + defaultIfBlank(bootVersion, "latest"));
        out.println("  packaging   : " + packaging);
        out.println("  buildTool   : " + buildTool);
        out.println("  authStyle   : " + authStyle);
        out.println("  database    : " + database);
        out.println("  dependencies: " + (extraDependencies.isEmpty() ? "(none)" : String.join(", ", extraDependencies)));
        out.println("  output      : " + output);
        out.println();

        String confirm = prompt("Generate project now? [y/N]", "N");
        if (!isYes(confirm)) {
            return null;
        }

        CliArgs cliArgs = new CliArgs(
                name,
                groupId,
                artifactId,
                javaVersion,
                bootVersion,
                packaging,
                buildTool,
                authStyle,
                database,
                extraDependencies,
                List.of(),
                output
        );
        return new InteractiveSelection(presetName, cliArgs);
    }

    private String pickPreset(List<String> presets) {
        if (presets == null || presets.isEmpty()) {
            throw new IllegalArgumentException("No presets available for interactive mode");
        }
        out.println("Preset");
        for (int i = 0; i < presets.size(); i++) {
            out.printf("  %d) %s%n", i + 1, presets.get(i));
        }
        while (true) {
            String input = prompt("Select preset", "1");
            Integer index = parseIndex(input, presets.size());
            if (index != null) {
                return presets.get(index);
            }
            out.printf("Invalid selection '%s'. Choose 1-%d.%n", input, presets.size());
        }
    }

    private <E> E pickEnum(String label, List<E> values, E defaultValue) {
        out.println(label);
        for (int i = 0; i < values.size(); i++) {
            out.printf("  %d) %s%n", i + 1, values.get(i));
        }
        int defaultIndex = values.indexOf(defaultValue);
        while (true) {
            String input = prompt("Select " + label.toLowerCase(Locale.ROOT), String.valueOf(defaultIndex + 1));
            Integer index = parseIndex(input, values.size());
            if (index != null) {
                return values.get(index);
            }
            out.printf("Invalid selection '%s'. Choose 1-%d.%n", input, values.size());
        }
    }

    private String prompt(String label, String defaultValue) {
        out.printf("%s [%s]: ", label, defaultValue);
        out.flush();
        String raw = readLine();
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return raw.trim();
    }

    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read interactive input", e);
        }
    }

    private static Integer parseIndex(String input, int size) {
        try {
            int value = Integer.parseInt(input.trim());
            if (value >= 1 && value <= size) {
                return value - 1;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static boolean isYes(String value) {
        return "y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String slugify(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        String trimmed = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return trimmed.isBlank() ? "springrad-app" : trimmed;
    }

    private static List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> items = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String item = token.trim();
            if (!item.isBlank()) {
                items.add(item);
            }
        }
        return new ArrayList<>(items);
    }

    public record InteractiveSelection(String presetName, CliArgs cliArgs) {
    }
}

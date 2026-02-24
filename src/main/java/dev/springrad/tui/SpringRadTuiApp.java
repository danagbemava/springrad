package dev.springrad.tui;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.tamboui.backend.panama.PanamaBackendProvider;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

public class SpringRadTuiApp {
    private final Backend backend;

    public SpringRadTuiApp() {
        this(new TamboUiBackend());
    }

    SpringRadTuiApp(Reader in, PrintWriter out) {
        this(new PromptBackend(in, out));
    }

    private SpringRadTuiApp(Backend backend) {
        this.backend = backend;
    }

    public InteractiveSelection start(String initialName, List<String> availablePresets) {
        return backend.start(initialName, availablePresets);
    }

    private interface Backend {
        InteractiveSelection start(String initialName, List<String> availablePresets);
    }

    private static final class TamboUiBackend implements Backend {
        @Override
        public InteractiveSelection start(String initialName, List<String> availablePresets) {
            if (availablePresets == null || availablePresets.isEmpty()) {
                throw new IllegalArgumentException("No presets available for interactive mode");
            }

            String defaultName = defaultIfBlank(initialName, "springrad-app");
            String defaultArtifactId = slugify(defaultName);
            FormState formState = FormState.builder()
                    .selectField("preset", availablePresets, 0)
                    .textField("name", defaultName)
                    .textField("groupId", "com.example")
                    .textField("artifactId", defaultArtifactId)
                    .textField("javaVersion", "21")
                    .textField("bootVersion", "")
                    .selectField("packaging", List.of("jar", "war"), 0)
                    .selectField("buildTool", List.of("gradle", "maven"), 0)
                    .selectField("authStyle", List.of("jwt", "session", "none"), 0)
                    .selectField("database", List.of("postgresql", "mysql", "h2"), 0)
                    .textField("dependencies", "")
                    .textField("outputDirectory", "./" + defaultArtifactId)
                    .build();

            AtomicReference<InteractiveSelection> selectionRef = new AtomicReference<>();
            ToolkitApp app = new ToolkitApp() {
                @Override
                protected TuiConfig configure() {
                    try {
                        return TuiConfig.builder()
                                .backend(new PanamaBackendProvider().create())
                                .build();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to initialize TamboUI backend", e);
                    }
                }

                @Override
                protected dev.tamboui.toolkit.element.Element render() {
                    FormElement interactiveForm = form(formState)
                            .field("preset", "Preset", FieldType.SELECT)
                            .field("name", "Project name")
                            .field("groupId", "Group ID")
                            .field("artifactId", "Artifact ID")
                            .field("javaVersion", "Java version")
                            .field("bootVersion", "Boot version")
                            .field("packaging", "Packaging", FieldType.SELECT)
                            .field("buildTool", "Build tool", FieldType.SELECT)
                            .field("authStyle", "Auth style", FieldType.SELECT)
                            .field("database", "Database", FieldType.SELECT)
                            .field("dependencies", "Dependencies (CSV)")
                            .field("outputDirectory", "Output directory")
                            .submitOnEnter(true)
                            .arrowNavigation(true)
                            .onSubmit(submitted -> {
                                selectionRef.set(toSelection(submitted, availablePresets));
                                quit();
                            });

                    return column(
                            panel("SpringRad Interactive CLI",
                                    text("Powered by TamboUI toolkit"),
                                    text("Use arrow keys/TAB to move fields and ENTER to generate.")
                            ),
                            spacer(),
                            interactiveForm
                    );
                }
            };

            try {
                app.run();
                return selectionRef.get();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to run TamboUI interactive flow", e);
            }
        }

        private static InteractiveSelection toSelection(FormState formState, List<String> availablePresets) {
            String presetName = formState.selectValue("preset");
            if (!availablePresets.contains(presetName)) {
                presetName = availablePresets.getFirst();
            }

            String name = defaultIfBlank(formState.textValue("name"), "springrad-app");
            String groupId = defaultIfBlank(formState.textValue("groupId"), "com.example");
            String artifactId = defaultIfBlank(formState.textValue("artifactId"), slugify(name));
            String javaVersion = defaultIfBlank(formState.textValue("javaVersion"), "21");
            String bootVersion = normalizeBlank(formState.textValue("bootVersion"));
            String packagingValue = defaultIfBlank(formState.selectValue("packaging"), "jar");
            String buildToolValue = defaultIfBlank(formState.selectValue("buildTool"), "gradle");
            String authStyleValue = defaultIfBlank(formState.selectValue("authStyle"), "jwt");
            String databaseValue = defaultIfBlank(formState.selectValue("database"), "postgresql");
            String output = defaultIfBlank(formState.textValue("outputDirectory"), "./" + artifactId);
            List<String> dependencies = parseCsv(formState.textValue("dependencies"));

            CliArgs cliArgs = new CliArgs(
                    name,
                    groupId,
                    artifactId,
                    javaVersion,
                    bootVersion,
                    parseEnum(ProjectConfig.Packaging.class, packagingValue, ProjectConfig.Packaging.jar),
                    parseEnum(ProjectConfig.BuildTool.class, buildToolValue, ProjectConfig.BuildTool.gradle),
                    parseEnum(ProjectConfig.AuthStyle.class, authStyleValue, ProjectConfig.AuthStyle.jwt),
                    parseEnum(ProjectConfig.Database.class, databaseValue, ProjectConfig.Database.postgresql),
                    dependencies,
                    List.of(),
                    Path.of(output)
            );
            return new InteractiveSelection(presetName, cliArgs);
        }
    }

    private static final class PromptBackend implements Backend {
        private final BufferedReader in;
        private final PrintWriter out;

        private PromptBackend(Reader in, PrintWriter out) {
            this.in = new BufferedReader(in);
            this.out = out;
        }

        @Override
        public InteractiveSelection start(String initialName, List<String> availablePresets) {
            Objects.requireNonNull(availablePresets, "availablePresets");
            if (availablePresets.isEmpty()) {
                throw new IllegalArgumentException("No presets available for interactive mode");
            }

            out.println("SpringRad interactive CLI");
            out.println("TamboUI-backed visual screens are available in normal interactive mode.");
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

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        try {
            return Enum.valueOf(enumType, rawValue);
        } catch (RuntimeException e) {
            return fallback;
        }
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

package dev.springrad.tui;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.GitInitializer;
import dev.springrad.core.InitializrClient;
import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
import dev.springrad.tui.app.AppAction;
import dev.springrad.tui.app.AppRoute;
import dev.springrad.tui.app.AppShell;
import dev.springrad.tui.app.AppState;
import dev.springrad.tui.app.AppStore;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.columns;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.text;

public final class UnifiedSpringRadTuiApp {
    private final PresetRepository presetRepository;
    private final PresetService presetService;
    private final ProjectGenerator projectGenerator;
    private final GitInitializer gitInitializer;
    private final TemplateOverlayEngine templateOverlayEngine;
    private final ActivityLogStore activityLogStore;

    public UnifiedSpringRadTuiApp() {
        this(
                new PresetRepository(),
                new PresetService(),
                new InitializrClient(),
                new GitInitializer(),
                new TemplateOverlayEngine(),
                new ActivityLogStore()
        );
    }

    UnifiedSpringRadTuiApp(
            PresetRepository presetRepository,
            PresetService presetService,
            ProjectGenerator projectGenerator,
            GitInitializer gitInitializer,
            TemplateOverlayEngine templateOverlayEngine,
            ActivityLogStore activityLogStore
    ) {
        this.presetRepository = presetRepository;
        this.presetService = presetService;
        this.projectGenerator = projectGenerator;
        this.gitInitializer = gitInitializer;
        this.templateOverlayEngine = templateOverlayEngine;
        this.activityLogStore = activityLogStore;
    }

    public void start() {
        presetService.seedDefaults();

        List<String> presetNames = presetService.listPresetNames();
        if (presetNames.isEmpty()) {
            throw new IllegalStateException("No presets available");
        }

        AppStore store = new AppStore(AppState.initial(AppRoute.command_center));
        store.dispatch(AppAction.setStatus("Ready"));

        AtomicReference<AppRoute> routeRef = new AtomicReference<>(AppRoute.command_center);
        AtomicBoolean quitArmed = new AtomicBoolean(false);
        AtomicBoolean generationRunning = new AtomicBoolean(false);
        AtomicBoolean awaitingProgressConfirm = new AtomicBoolean(false);
        AtomicReference<String> progressTitle = new AtomicReference<>("Project Generation");
        AtomicReference<String> progressMessage = new AtomicReference<>("Preparing...");
        AtomicInteger progressStep = new AtomicInteger(0);
        AtomicInteger progressTotal = new AtomicInteger(0);

        List<String> activity = Collections.synchronizedList(new ArrayList<>(activityLogStore.tail(60)));
        if (activity.isEmpty()) {
            appendActivity(activity, "SpringRad started.");
        }

        FormState commandFormState = FormState.builder()
                .selectField("action", List.of("generate_project", "manage_presets", "quit"), 0)
                .build();

        String defaultName = "springrad-app";
        String defaultArtifact = slugify(defaultName);
        FormState projectFormState = FormState.builder()
                .selectField("preset", presetNames, 0)
                .textField("name", defaultName)
                .textField("groupId", "com.example")
                .textField("artifactId", defaultArtifact)
                .textField("javaVersion", "21")
                .textField("bootVersion", "")
                .selectField("packaging", List.of("jar", "war"), 0)
                .selectField("buildTool", List.of("gradle", "maven"), 0)
                .selectField("authStyle", List.of("jwt", "session", "none"), 0)
                .selectField("database", List.of("postgresql", "mysql", "h2"), 0)
                .textField("dependencies", "")
                .textField("outputDirectory", "./" + defaultArtifact)
                .build();

        FormState presetFormState = FormState.builder()
                .selectField("action", List.of("list", "save", "delete", "back"), 0)
                .textField("name", "")
                .textField("deps", "")
                .selectField("auth", List.of("jwt", "session", "none"), 0)
                .selectField("database", List.of("postgresql", "mysql", "h2"), 0)
                .textField("groupId", "")
                .textField("javaVersion", "21")
                .textField("bootVersion", "")
                .selectField("buildTool", List.of("gradle", "maven"), 0)
                .selectField("packaging", List.of("jar", "war"), 0)
                .build();

        ToolkitApp app = new ToolkitApp() {
            @Override
            protected TuiConfig configure() {
                return TuiRuntime.createConfig();
            }

            @Override
            protected Element render() {
                AppRoute route = routeRef.get();
                return switch (route) {
                    case command_center -> renderCommandCenter();
                    case project_wizard -> renderProjectWizard();
                    case preset_manager -> renderPresetManager();
                    case progress -> renderProgress();
                };
            }

            private Element renderCommandCenter() {
                final FormElement[] formRef = new FormElement[1];
                FormElement actionForm = form(commandFormState)
                        .field("action", "Action", FieldType.SELECT)
                        .labelWidth(16)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(Color.CYAN)
                        .focusedBorderColor(Color.LIGHT_CYAN)
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(submitted -> {
                            String action = submitted.selectValue("action");
                            switch (action) {
                                case "generate_project" -> {
                                    quitArmed.set(false);
                                    routeRef.set(AppRoute.project_wizard);
                                    store.dispatch(AppAction.navigate(AppRoute.project_wizard));
                                    store.dispatch(AppAction.setStatus("Project wizard"));
                                }
                                case "manage_presets" -> {
                                    quitArmed.set(false);
                                    routeRef.set(AppRoute.preset_manager);
                                    store.dispatch(AppAction.navigate(AppRoute.preset_manager));
                                    store.dispatch(AppAction.setStatus("Preset manager"));
                                }
                                case "quit" -> {
                                    if (!quitArmed.get()) {
                                        quitArmed.set(true);
                                        store.dispatch(AppAction.setStatus("Press ENTER again to exit"));
                                    } else {
                                        quit();
                                    }
                                }
                                default -> store.dispatch(AppAction.setStatus("Unknown action"));
                            }
                        })
                        .onKeyEvent(event -> {
                            if (event.isConfirm()) {
                                formRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        });
                formRef[0] = actionForm;

                Element body = column(
                        text(""),
                        actionForm,
                        text(""),
                        text("   _____            _               _____           _ ").cyan(),
                        text("  / ____|          (_)             |  __ \\         | |").cyan(),
                        text(" | (___  _ __  _ __ _ _ __   __ _  | |__) |__ _  __| |").cyan(),
                        text("  \\___ \\| '_ \\| '__| | '_ \\ / _` | |  _  // _` |/ _` |").cyan(),
                        text("  ____) | |_) | |  | | | | | (_| | | | \\ \\ (_| | (_| |").cyan(),
                        text(" |_____/| .__/|_|  |_|_| |_|\\__, | |_|  \\_\\__,_|\\__,_|").cyan(),
                        text("        | |                  __/ |                     ").cyan(),
                        text("        |_|                 |___/                      ").cyan(),
                        text(""),
                        text("SpringRad generates production-ready Spring Boot projects and keeps").white(),
                        text("project scaffolding and preset management in a single TUI workflow.").white()
                ).spacing(1);

                return AppShell.render(
                        "Command Center",
                        "Unified interactive application",
                        body,
                        store.state().status(),
                        "Keys: TAB/Shift+TAB navigate, arrows change select, ENTER confirm"
                );
            }

            private Element renderProjectWizard() {
                final FormElement[] formRef = new FormElement[1];
                FormElement projectForm = form(projectFormState)
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
                        .labelWidth(22)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(Color.CYAN)
                        .focusedBorderColor(Color.LIGHT_CYAN)
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(this::startGeneration)
                        .onKeyEvent(event -> {
                            if (event.isConfirm()) {
                                formRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            if (event.isCancel()) {
                                routeRef.set(AppRoute.command_center);
                                store.dispatch(AppAction.navigate(AppRoute.command_center));
                                store.dispatch(AppAction.setStatus("Returned to command center"));
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        });
                formRef[0] = projectForm;

                return AppShell.render(
                        "Project Wizard",
                        "Configure and generate a Spring Boot project",
                        columns(
                                projectForm.percent(68),
                                column(
                                        text("Preview").bold().magenta(),
                                        text("Preset:       " + value(projectFormState.selectValue("preset"), "web-api")).yellow(),
                                        text("Name:         " + value(projectFormState.textValue("name"), "springrad-app")).white(),
                                        text("Group:        " + value(projectFormState.textValue("groupId"), "com.example")),
                                        text("Artifact:     " + value(projectFormState.textValue("artifactId"), "springrad-app")).green(),
                                        text("Java:         " + value(projectFormState.textValue("javaVersion"), "21")),
                                        text("Boot:         " + value(projectFormState.textValue("bootVersion"), "latest")),
                                        text("Build:        " + value(projectFormState.selectValue("buildTool"), "gradle")),
                                        text("Auth:         " + value(projectFormState.selectValue("authStyle"), "jwt")),
                                        text("Database:     " + value(projectFormState.selectValue("database"), "postgresql")),
                                        text("Dependencies: " + value(projectFormState.textValue("dependencies"), "(preset defaults)")),
                                        text("Output:       " + value(projectFormState.textValue("outputDirectory"), "./springrad-app")).cyan()
                                ).percent(32)
                        ).spacing(1),
                        store.state().status(),
                        "ENTER generate, ESC back to command center"
                );
            }

            private void startGeneration(FormState submitted) {
                if (generationRunning.get()) {
                    return;
                }
                generationRunning.set(true);
                awaitingProgressConfirm.set(false);
                progressTitle.set("Project Generation");
                progressStep.set(0);
                progressTotal.set(0);
                progressMessage.set("Preparing...");
                routeRef.set(AppRoute.progress);
                store.dispatch(AppAction.navigate(AppRoute.progress));
                store.dispatch(AppAction.setStatus("Starting generation"));

                CliArgs cliArgs = toCliArgs(submitted);
                String preset = submitted.selectValue("preset");

                Thread worker = new Thread(() -> {
                    try {
                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[1/5] Resolving preset and configuration");
                            store.dispatch(AppAction.setStatus("Running step 1 of 5"));
                            progressStep.set(1);
                            progressTotal.set(5);
                            progressMessage.set("Resolving preset and configuration");
                        });

                        ProjectConfig config = presetService.resolve(preset, cliArgs);
                        runner().runOnRenderThread(() -> appendActivity(activity, "  - Output directory: " + config.outputDirectory()));

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[2/5] Generating base Spring project from Initializr");
                            store.dispatch(AppAction.setStatus("Running step 2 of 5"));
                            progressStep.set(2);
                            progressMessage.set("Generating base Spring project from Initializr");
                        });
                        projectGenerator.generate(config);

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[3/5] Applying template overlays and scaffolds");
                            store.dispatch(AppAction.setStatus("Running step 3 of 5"));
                            progressStep.set(3);
                            progressMessage.set("Applying template overlays and scaffolds");
                        });
                        templateOverlayEngine.overlay(config, false, detail -> runner().runOnRenderThread(() -> appendActivity(activity, "  - " + detail)));

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[4/5] Initializing git repository");
                            store.dispatch(AppAction.setStatus("Running step 4 of 5"));
                            progressStep.set(4);
                            progressMessage.set("Initializing git repository");
                        });
                        gitInitializer.initializeRepository(config.outputDirectory(), new PrintWriter(System.err, true));

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[5/5] Finalizing output");
                            appendActivity(activity, "  - Generation complete: " + config.outputDirectory());
                            store.dispatch(AppAction.setStatus("Completed"));
                            progressStep.set(5);
                            progressMessage.set("Completed (press ENTER to return)");
                            awaitingProgressConfirm.set(true);
                        });
                    } catch (Exception e) {
                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "Error: " + e.getMessage());
                            store.dispatch(AppAction.setStatus("Failed"));
                            progressMessage.set("Failed (press ENTER to return)");
                            awaitingProgressConfirm.set(true);
                        });
                    } finally {
                        generationRunning.set(false);
                    }
                }, "springrad-unified-generation");
                worker.setDaemon(true);
                worker.start();
            }

            private Element renderPresetManager() {
                final FormElement[] formRef = new FormElement[1];
                FormElement presetForm = form(presetFormState)
                        .field("action", "Action", FieldType.SELECT)
                        .field("name", "Preset Name")
                        .field("deps", "Dependencies (CSV)")
                        .field("auth", "Auth", FieldType.SELECT)
                        .field("database", "Database", FieldType.SELECT)
                        .field("groupId", "Group ID")
                        .field("javaVersion", "Java Version")
                        .field("bootVersion", "Boot Version")
                        .field("buildTool", "Build Tool", FieldType.SELECT)
                        .field("packaging", "Packaging", FieldType.SELECT)
                        .labelWidth(22)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(Color.CYAN)
                        .focusedBorderColor(Color.LIGHT_CYAN)
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(this::runPresetAction)
                        .onKeyEvent(event -> {
                            if (event.isConfirm()) {
                                formRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            if (event.isCancel()) {
                                routeRef.set(AppRoute.command_center);
                                store.dispatch(AppAction.navigate(AppRoute.command_center));
                                store.dispatch(AppAction.setStatus("Returned to command center"));
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        });
                formRef[0] = presetForm;

                List<Element> logLines = new ArrayList<>();
                synchronized (activity) {
                    int from = Math.max(0, activity.size() - 14);
                    for (int i = from; i < activity.size(); i++) {
                        logLines.add(text(activity.get(i)));
                    }
                }
                if (logLines.isEmpty()) {
                    logLines.add(text("No activity yet.").gray());
                }

                return AppShell.render(
                        "Preset Management",
                        "List, save, and delete presets without leaving the app",
                        columns(
                                presetForm.percent(62),
                                column(
                                        text("Activity").bold().magenta(),
                                        column(logLines.toArray(new Element[0])).spacing(0)
                                ).percent(38)
                        ).spacing(1),
                        store.state().status(),
                        "ENTER execute action, ESC back to command center"
                );
            }

            private void runPresetAction(FormState submitted) {
                String action = value(submitted.selectValue("action"), "list");
                switch (action) {
                    case "list" -> {
                        List<Preset> presets = presetRepository.findAll();
                        if (presets.isEmpty()) {
                            appendActivity(activity, "No presets found.");
                            store.dispatch(AppAction.setStatus("No presets found"));
                        } else {
                            appendActivity(activity, "Presets:");
                            for (Preset preset : presets) {
                                appendActivity(activity, "- %s%s (%s/%s) deps=%s".formatted(
                                        preset.name(),
                                        preset.builtIn() ? " [built-in]" : "",
                                        preset.authStyle(),
                                        preset.database(),
                                        String.join(",", preset.dependencies())
                                ));
                            }
                            store.dispatch(AppAction.setStatus("Listed presets"));
                        }
                    }
                    case "save" -> {
                        String name = submitted.textValue("name");
                        if (name == null || name.isBlank()) {
                            appendActivity(activity, "Cannot save: preset name is required.");
                            store.dispatch(AppAction.setStatus("Save failed: missing preset name"));
                            return;
                        }
                        Preset existing = presetRepository.findByName(name).orElse(null);
                        if (existing != null && existing.builtIn()) {
                            appendActivity(activity, "Cannot overwrite built-in preset: " + name);
                            store.dispatch(AppAction.setStatus("Save failed: built-in preset"));
                            return;
                        }
                        Preset preset = new Preset(
                                name.trim(),
                                false,
                                blankToNull(submitted.textValue("groupId")),
                                value(submitted.textValue("javaVersion"), "21"),
                                blankToNull(submitted.textValue("bootVersion")),
                                parseEnum(ProjectConfig.Packaging.class, submitted.selectValue("packaging"), ProjectConfig.Packaging.jar),
                                parseEnum(ProjectConfig.BuildTool.class, submitted.selectValue("buildTool"), ProjectConfig.BuildTool.gradle),
                                parseEnum(ProjectConfig.AuthStyle.class, submitted.selectValue("auth"), ProjectConfig.AuthStyle.jwt),
                                parseEnum(ProjectConfig.Database.class, submitted.selectValue("database"), ProjectConfig.Database.postgresql),
                                parseCsv(submitted.textValue("deps")),
                                List.of()
                        );
                        presetRepository.save(preset);
                        appendActivity(activity, "Saved preset: " + preset.name());
                        store.dispatch(AppAction.setStatus("Saved preset: " + preset.name()));
                    }
                    case "delete" -> {
                        String name = submitted.textValue("name");
                        if (name == null || name.isBlank()) {
                            appendActivity(activity, "Cannot delete: preset name is required.");
                            store.dispatch(AppAction.setStatus("Delete failed: missing preset name"));
                            return;
                        }
                        if (presetRepository.delete(name.trim())) {
                            appendActivity(activity, "Deleted preset: " + name.trim());
                            store.dispatch(AppAction.setStatus("Deleted preset: " + name.trim()));
                        } else {
                            appendActivity(activity, "Delete failed (not found or built-in): " + name.trim());
                            store.dispatch(AppAction.setStatus("Delete failed: not found or built-in"));
                        }
                    }
                    case "back" -> {
                        routeRef.set(AppRoute.command_center);
                        store.dispatch(AppAction.navigate(AppRoute.command_center));
                        store.dispatch(AppAction.setStatus("Returned to command center"));
                    }
                    default -> store.dispatch(AppAction.setStatus("Unknown preset action"));
                }
            }

            private Element renderProgress() {
                List<Element> logLines = new ArrayList<>();
                synchronized (activity) {
                    int from = Math.max(0, activity.size() - 16);
                    for (int i = from; i < activity.size(); i++) {
                        logLines.add(text(activity.get(i)));
                    }
                }
                if (logLines.isEmpty()) {
                    logLines.add(text("Waiting for execution steps...").gray());
                }

                return AppShell.render(
                                progressTitle.get(),
                                "Live execution updates",
                                column(
                                        text("Step: " + progressStep.get() + "/" + progressTotal.get()).yellow(),
                                        text(progressMessage.get()).white(),
                                        text(awaitingProgressConfirm.get() ? "Press ENTER to return to command center" : "Running...").green(),
                                        text(""),
                                        text("Activity").bold().magenta(),
                                        column(logLines.toArray(new Element[0])).spacing(0)
                                ).spacing(0),
                                store.state().status(),
                                "Each sub-step reports progress including file operations"
                        )
                        .focusable(true)
                        .onKeyEvent(event -> {
                            if (awaitingProgressConfirm.get() && event.isConfirm()) {
                                routeRef.set(AppRoute.command_center);
                                store.dispatch(AppAction.navigate(AppRoute.command_center));
                                store.dispatch(AppAction.setStatus("Ready"));
                                awaitingProgressConfirm.set(false);
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        });
            }
        };

        try {
            app.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run unified TUI", e);
        }
    }

    private void appendActivity(List<String> activity, String message) {
        synchronized (activity) {
            activity.add(message);
            activityLogStore.append(message);
        }
    }

    private static CliArgs toCliArgs(FormState formState) {
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

        return new CliArgs(
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
    }

    private static String value(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }

    private static String normalizeBlank(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String blankToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
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
        return List.copyOf(items);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        try {
            return Enum.valueOf(enumType, value(rawValue, fallback.name()).toLowerCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            try {
                return Enum.valueOf(enumType, value(rawValue, fallback.name()));
            } catch (RuntimeException e) {
                return fallback;
            }
        }
    }

    private static String slugify(String value) {
        String normalized = defaultIfBlank(value, "springrad-app")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "springrad-app" : normalized;
    }
}

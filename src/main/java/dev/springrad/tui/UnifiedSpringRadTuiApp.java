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
import dev.springrad.core.DependencyAliasRegistry;
import dev.springrad.core.DependencyCatalog;
import dev.springrad.core.DependencyEntry;
import dev.springrad.core.GlobalConfig;
import dev.springrad.core.InitializrMetadataClient;
import dev.springrad.tui.app.AppAction;
import dev.springrad.tui.app.AppRoute;
import dev.springrad.tui.app.AppShell;
import dev.springrad.tui.app.AppState;
import dev.springrad.tui.app.AppStore;
import dev.springrad.tui.app.CommandAutocomplete;
import dev.springrad.tui.app.CommandDoc;
import dev.springrad.tui.app.CommandRegistry;
import dev.springrad.tui.app.ThemeText;
import dev.springrad.tui.app.UiTheme;
import dev.springrad.tui.app.UiStyles;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.spinner.SpinnerStyle;

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
import static dev.tamboui.toolkit.Toolkit.dialog;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.lineGauge;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.spinner;
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

        GlobalConfig globalConfig = presetService.globalConfig();
        GlobalConfig.Defaults cfgDefaults = globalConfig.defaults();

        InitializrMetadataClient metadataClient = new InitializrMetadataClient();
        List<String> javaVersions = metadataClient.fetchJavaVersions();
        List<String> bootVersions = metadataClient.fetchBootVersions();
        DependencyCatalog dependencyCatalog = metadataClient.fetchDependencyCatalog();
        DependencyAliasRegistry aliasRegistry = new DependencyAliasRegistry(globalConfig.aliases());

        AppStore store = new AppStore(AppState.initial(AppRoute.command_center));
        store.dispatch(AppAction.setStatus("Ready"));

        AtomicReference<AppRoute> routeRef = new AtomicReference<>(AppRoute.command_center);
        AtomicBoolean quitArmed = new AtomicBoolean(false);
        AtomicBoolean generationRunning = new AtomicBoolean(false);
        AtomicBoolean awaitingProgressConfirm = new AtomicBoolean(false);
        AtomicBoolean generationFailed = new AtomicBoolean(false);
        AtomicReference<String> errorSummary = new AtomicReference<>(null);
        AtomicReference<String> progressTitle = new AtomicReference<>("Project Generation");
        AtomicReference<String> progressMessage = new AtomicReference<>("Preparing...");
        AtomicInteger progressStep = new AtomicInteger(0);
        AtomicInteger progressTotal = new AtomicInteger(0);
        AtomicReference<UiTheme> themeRef = new AtomicReference<>(UiTheme.ocean);
        StyleEngine styleEngine = UiStyles.createEngine();
        AtomicReference<String> lastAppliedPreset = new AtomicReference<>("");

        List<String> activity = Collections.synchronizedList(new ArrayList<>(activityLogStore.tail(60)));
        if (activity.isEmpty()) {
            appendActivity(activity, "SpringRad started.");
        }

        FormState commandFormState = FormState.builder()
                .textField("command", "")
                .build();

        String defaultName = "springrad-app";
        String defaultArtifact = slugify(defaultName);
        AtomicReference<FormState> projectFormRef = new AtomicReference<>(
                buildProjectFormState(presetNames, javaVersions, bootVersions, defaultName, defaultArtifact, cfgDefaults));

        FormState presetFormState = FormState.builder()
                .textField("command", "")
                .textField("name", "")
                .textField("deps", "")
                .selectField("auth", List.of("jwt", "session", "none"), 0)
                .selectField("database", List.of("postgresql", "mysql", "h2"), 0)
                .textField("groupId", "")
                .selectField("javaVersion", javaVersions, 0)
                .selectField("bootVersion", bootVersions, 0)
                .selectField("buildTool", List.of("gradle", "maven"), 0)
                .selectField("packaging", List.of("jar", "war"), 0)
                .build();

        ToolkitApp app = new ToolkitApp() {
            @Override
            protected TuiConfig configure() {
                return TuiRuntime.createConfig();
            }

            @Override
            protected void onStart() {
                runner().styleEngine(styleEngine);
                UiStyles.activate(styleEngine, themeRef.get());
            }

            private int currentTerminalRows() {
                try {
                    return runner().tuiRunner().backend().size().height();
                } catch (Exception ignored) {
                    return 40;
                }
            }

            @Override
            protected Element render() {
                int rows = currentTerminalRows();
                AppRoute route = routeRef.get();
                return switch (route) {
                    case command_center -> renderCommandCenter(rows);
                    case project_wizard -> renderProjectWizard(rows);
                    case preset_manager -> renderPresetManager(rows);
                    case progress -> renderProgress(rows);
                };
            }

            private Element renderCommandCenter(int terminalRows) {
                UiTheme selectedTheme = themeRef.get();

                // Quit confirmation dialog replaces the normal screen
                if (quitArmed.get()) {
                    return AppShell.render(
                            "Command Center",
                            "Generate, scaffold, and configure Spring Boot projects",
                            dialog("Confirm Exit",
                                    text(""),
                                    ThemeText.paint("Are you sure you want to quit SpringRad?", selectedTheme.primaryText()),
                                    text(""),
                                    row(
                                            ThemeText.paint("  ENTER → Quit  ", selectedTheme.statusAccent()),
                                            spacer(4),
                                            ThemeText.paint("  ESC → Cancel  ", selectedTheme.mutedText())
                                    )
                            )
                                    .rounded()
                                    .borderColor(selectedTheme.statusAccent())
                                    .padding(1)
                                    .spacing(1)
                                    .onConfirm(() -> quit())
                                    .onCancel(() -> {
                                        quitArmed.set(false);
                                        store.dispatch(AppAction.setStatus("Ready"));
                                    }),
                            "Press ENTER to quit or ESC to cancel",
                            "/help for commands, ENTER to confirm, ESC to cancel",
                            selectedTheme,
                            0,
                            terminalRows
                    );
                }

                final FormElement[] formRef = new FormElement[1];
                FormElement actionForm = form(commandFormState)
                        .field("command", "Command")
                        .labelWidth(16)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(selectedTheme.panelBorder())
                        .focusedBorderColor(selectedTheme.panelAccentBorder())
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(submitted -> {
                            String action = normalizeSlashCommand(submitted.textValue("command"));
                            if (applyTheme(action, themeRef, styleEngine, store)) {
                                return;
                            }
                            switch (action) {
                                case "/generate", "generate_project" -> {
                                    quitArmed.set(false);
                                    List<String> freshPresets = presetService.listPresetNames();
                                    projectFormRef.set(buildProjectFormState(
                                            freshPresets, javaVersions, bootVersions, defaultName, defaultArtifact, cfgDefaults));
                                    lastAppliedPreset.set("");
                                    routeRef.set(AppRoute.project_wizard);
                                    store.dispatch(AppAction.navigate(AppRoute.project_wizard));
                                    store.dispatch(AppAction.setStatus("Project wizard"));
                                }
                                case "/presets", "manage_presets" -> {
                                    quitArmed.set(false);
                                    routeRef.set(AppRoute.preset_manager);
                                    store.dispatch(AppAction.navigate(AppRoute.preset_manager));
                                    store.dispatch(AppAction.setStatus("Preset manager"));
                                }
                                case "/help" -> {
                                    quitArmed.set(false);
                                    store.dispatch(AppAction.setStatus("Type / to browse commands, TAB to autocomplete"));
                                }
                                case "/quit", "/exit", "quit" -> {
                                    if (!quitArmed.get()) {
                                        quitArmed.set(true);
                                        store.dispatch(AppAction.setStatus("Confirm quit"));
                                    } else {
                                        quit();
                                    }
                                }
                                default -> store.dispatch(AppAction.setStatus("Unknown command — type /help for a list"));
                            }
                        })
                        .onKeyEvent(event -> {
                            if (event.isKey(KeyCode.TAB) && !event.hasShift() && !event.hasCtrl() && !event.hasAlt()) {
                                String completion = CommandAutocomplete.complete(commandFormState.textValue("command"), CommandRegistry.COMMAND_CENTER);
                                if (completion != null) {
                                    commandFormState.setTextValue("command", completion);
                                    store.dispatch(AppAction.setStatus("Autocompleted: " + completion));
                                    return EventResult.HANDLED;
                                }
                            }
                            if (event.isConfirm()) {
                                formRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        })
                        .id("command-form");
                formRef[0] = actionForm;
                String commandInput = value(commandFormState.textValue("command"), "");
                List<CommandDoc> filteredCommands = filterSlashCommands(commandInput, CommandRegistry.COMMAND_CENTER);

                Element body = column(
                        text(""),
                        ThemeText.paint("Generate, scaffold, and configure projects from one place.", selectedTheme.primaryText()),
                        ThemeText.paint("Type / to browse commands. TAB to autocomplete.", selectedTheme.mutedText()),
                        ThemeText.paint("Current theme: " + selectedTheme.label(), selectedTheme.titleAccent()),
                        text(""),
                        actionForm,
                        renderCommandSuggestions(filteredCommands, selectedTheme)
                ).spacing(1);

                return AppShell.render(
                        "Command Center",
                        "Generate, scaffold, and configure Spring Boot projects",
                        body,
                        store.state().status(),
                        "/help for commands, TAB to autocomplete",
                        selectedTheme,
                        0,
                        terminalRows
                );
            }

            private Element renderProjectWizard(int terminalRows) {
                UiTheme theme = themeRef.get();
                boolean compact = terminalRows < 40;
                FormState projectFormState = projectFormRef.get();

                // Auto-fill form fields when preset selection changes
                String currentPreset = projectFormState.selectValue("preset");
                if (currentPreset != null && !currentPreset.equals(lastAppliedPreset.get())) {
                    lastAppliedPreset.set(currentPreset);
                    presetService.findByName(currentPreset).ifPresent(p ->
                            applyPresetToForm(projectFormState, p, javaVersions, bootVersions));
                }

                final FormElement[] formRef = new FormElement[1];
                FormElement projectForm = form(projectFormState)
                        .field("preset", "Preset", FieldType.SELECT)
                        .field("name", "Project name")
                        .field("groupId", "Group ID")
                        .field("artifactId", "Artifact ID")
                        .field("javaVersion", "Java version", FieldType.SELECT)
                        .field("bootVersion", "Boot version", FieldType.SELECT)
                        .field("packaging", "Packaging", FieldType.SELECT)
                        .field("buildTool", "Build tool", FieldType.SELECT)
                        .field("authStyle", "Auth style", FieldType.SELECT)
                        .field("database", "Database", FieldType.SELECT)
                        .field("dependencies", "Dependencies (CSV)")
                        .field("scaffolds", "Scaffolds (CSV)")
                        .field("outputDirectory", "Output directory")
                        .labelWidth(22)
                        .fieldSpacing(compact ? 0 : 1)
                        .rounded()
                        .borderColor(theme.panelBorder())
                        .focusedBorderColor(theme.panelAccentBorder())
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

                String depsInput = value(projectFormState.textValue("dependencies"), "");
                String scaffoldsInput = value(projectFormState.textValue("scaffolds"), "");
                Element depSuggestions = renderDepSuggestions(depsInput, dependencyCatalog, theme);

                Element wizardBody = compact
                        ? projectForm
                        : columns(
                                projectForm.percent(65),
                                column(
                                        panel(" PREVIEW ",
                                                ThemeText.paint(previewLine("Preset", value(projectFormState.selectValue("preset"), "web-api")), theme.statusAccent()),
                                                ThemeText.paint(previewLine("Name", value(projectFormState.textValue("name"), "springrad-app")), theme.primaryText()),
                                                text(previewLine("Group", value(projectFormState.textValue("groupId"), "com.example"))),
                                                ThemeText.paint(previewLine("Artifact", value(projectFormState.textValue("artifactId"), "springrad-app")), theme.successAccent()),
                                                text(previewLine("Java", projectFormState.selectValue("javaVersion"))),
                                                text(previewLine("Boot", projectFormState.selectValue("bootVersion"))),
                                                text(previewLine("Build", value(projectFormState.selectValue("buildTool"), "gradle"))),
                                                text(previewLine("Auth", value(projectFormState.selectValue("authStyle"), "jwt"))),
                                                text(previewLine("Database", value(projectFormState.selectValue("database"), "postgresql"))),
                                                text(previewLine("Deps", depsInput.isEmpty() ? "(preset defaults)" : depsInput)),
                                                text(previewLine("Scaffolds", scaffoldsInput.isEmpty() ? "(preset defaults)" : scaffoldsInput)),
                                                ThemeText.paint(previewLine("Output", value(projectFormState.textValue("outputDirectory"), "./springrad-app")), theme.panelBorder())
                                        )
                                                .rounded()
                                                .borderColor(theme.panelBorder())
                                                .padding(1),
                                        depSuggestions
                                ).spacing(1).percent(35)
                          ).spacing(1);

                return AppShell.render(
                        "Project Wizard",
                        "Configure and generate a Spring Boot project",
                        wizardBody,
                        store.state().status(),
                        "ENTER generate, ESC back to command center",
                        theme,
                        1,
                        terminalRows
                );
            }

            private void startGeneration(FormState submitted) {
                if (generationRunning.get()) {
                    return;
                }

                String validationError = validateProjectForm(submitted, dependencyCatalog, aliasRegistry);
                if (validationError != null) {
                    store.dispatch(AppAction.setStatus(validationError));
                    return;
                }

                generationRunning.set(true);
                awaitingProgressConfirm.set(false);
                generationFailed.set(false);
                errorSummary.set(null);
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

                        Path userTemplateDir = globalConfig.templateDir();
                        java.util.Set<String> userFiles = userTemplateDir != null
                                ? templateOverlayEngine.scanUserFiles(java.util.List.of(userTemplateDir), config)
                                : java.util.Set.of();
                        int totalSteps = userTemplateDir != null ? 6 : 5;
                        int step = 3;

                        int builtinStep = step;
                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[" + builtinStep + "/" + totalSteps + "] Applying built-in template overlays and scaffolds");
                            store.dispatch(AppAction.setStatus("Running step " + builtinStep + " of " + totalSteps));
                            progressStep.set(builtinStep);
                            progressTotal.set(totalSteps);
                            progressMessage.set("Applying built-in template overlays");
                        });
                        templateOverlayEngine.overlay(config, false, userFiles, detail -> runner().runOnRenderThread(() -> appendActivity(activity, "  - " + detail)));
                        step++;

                        if (userTemplateDir != null) {
                            int s = step;
                            runner().runOnRenderThread(() -> {
                                appendActivity(activity, "[" + s + "/" + totalSteps + "] Applying user templates from " + userTemplateDir);
                                store.dispatch(AppAction.setStatus("Running step " + s + " of " + totalSteps));
                                progressStep.set(s);
                                progressTotal.set(totalSteps);
                                progressMessage.set("Applying user templates");
                            });
                            templateOverlayEngine.overlayDirectory(userTemplateDir, config, false,
                                    detail -> runner().runOnRenderThread(() -> appendActivity(activity, "  - " + detail)));
                            step++;
                        }

                        int gitStep = step;
                        int finalStep = step + 1;

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[" + gitStep + "/" + totalSteps + "] Initializing git repository");
                            store.dispatch(AppAction.setStatus("Running step " + gitStep + " of " + totalSteps));
                            progressStep.set(gitStep);
                            progressMessage.set("Initializing git repository");
                        });
                        gitInitializer.initializeRepository(config.outputDirectory(), new PrintWriter(System.err, true));

                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "[" + finalStep + "/" + totalSteps + "] Finalizing output");
                            appendActivity(activity, "  - Generation complete: " + config.outputDirectory());
                            store.dispatch(AppAction.setStatus("Completed"));
                            progressStep.set(finalStep);
                            progressMessage.set("Generation complete!");
                            awaitingProgressConfirm.set(true);
                        });
                    } catch (Exception e) {
                        runner().runOnRenderThread(() -> {
                            appendActivity(activity, "Error: " + e.getMessage());
                            store.dispatch(AppAction.setStatus("Failed"));
                            progressMessage.set("Generation failed");
                            generationFailed.set(true);
                            errorSummary.set(e.getMessage());
                            awaitingProgressConfirm.set(true);
                        });
                    } finally {
                        generationRunning.set(false);
                    }
                }, "springrad-unified-generation");
                worker.setDaemon(true);
                worker.start();
            }

            private Element renderPresetManager(int terminalRows) {
                UiTheme theme = themeRef.get();
                final FormElement[] formRef = new FormElement[1];
                FormElement presetForm = form(presetFormState)
                        .field("command", "Command")
                        .field("name", "Preset Name")
                        .field("deps", "Dependencies (CSV)")
                        .field("auth", "Auth", FieldType.SELECT)
                        .field("database", "Database", FieldType.SELECT)
                        .field("groupId", "Group ID")
                        .field("javaVersion", "Java Version", FieldType.SELECT)
                        .field("bootVersion", "Boot Version", FieldType.SELECT)
                        .field("buildTool", "Build Tool", FieldType.SELECT)
                        .field("packaging", "Packaging", FieldType.SELECT)
                        .labelWidth(22)
                        .fieldSpacing(terminalRows < 40 ? 0 : 1)
                        .rounded()
                        .borderColor(theme.panelBorder())
                        .focusedBorderColor(theme.panelAccentBorder())
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
                String commandInput = value(presetFormState.textValue("command"), "");
                List<CommandDoc> filteredCommands = filterSlashCommands(commandInput, CommandRegistry.PRESET_MANAGER);
                String presetDepsInput = value(presetFormState.textValue("deps"), "");
                Element presetDepSuggestions = renderDepSuggestions(presetDepsInput, dependencyCatalog, theme);

                List<String> activityLines = new ArrayList<>();
                synchronized (activity) {
                    activityLines.addAll(activity);
                }
                if (activityLines.isEmpty()) {
                    activityLines.add("No activity yet.");
                }

                return AppShell.render(
                        "Preset Management",
                        "List, save, and delete presets without leaving the app",
                        columns(
                                presetForm.percent(62),
                                column(
                                        renderCommandSuggestions(filteredCommands, theme),
                                        presetDepSuggestions,
                                        ThemeText.paint("Recent Activity", theme.titleAccent()),
                                        list(activityLines)
                                                .id("activity-list")
                                                .scrollbar()
                                                .scrollToEnd()
                                                .rounded()
                                                .borderColor(theme.panelBorder())
                                                .displayOnly()
                                ).spacing(1).percent(38)
                        ).spacing(1),
                        store.state().status(),
                        "/help for commands, ENTER execute, ESC back",
                        theme,
                        2,
                        terminalRows
                );
            }

            private void runPresetAction(FormState submitted) {
                String action = value(normalizeSlashCommand(submitted.textValue("command")), "/list");
                if (applyTheme(action, themeRef, styleEngine, store)) {
                    return;
                }
                switch (action) {
                    case "/list", "list" -> {
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
                    case "/save", "save" -> {
                        String name = submitted.textValue("name");
                        if (name == null || name.isBlank()) {
                            appendActivity(activity, "Cannot save: preset name is required.");
                            store.dispatch(AppAction.setStatus("Save failed: missing preset name"));
                            return;
                        }
                        if (!dependencyCatalog.isEmpty()) {
                            List<String> deps = parseCsv(submitted.textValue("deps"));
                            List<String> invalid = findUnknownDeps(deps, dependencyCatalog, aliasRegistry);
                            if (!invalid.isEmpty()) {
                                appendActivity(activity, "Unknown dependencies: " + String.join(", ", invalid));
                                store.dispatch(AppAction.setStatus("Save failed: unknown deps — " + String.join(", ", invalid)));
                                return;
                            }
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
                                submitted.selectValue("javaVersion"),
                                "latest".equals(submitted.selectValue("bootVersion"))
                                        ? null : submitted.selectValue("bootVersion"),
                                parseEnum(ProjectConfig.Packaging.class, submitted.selectValue("packaging"), ProjectConfig.Packaging.jar),
                                parseEnum(ProjectConfig.BuildTool.class, submitted.selectValue("buildTool"), ProjectConfig.BuildTool.gradle),
                                parseEnum(ProjectConfig.AuthStyle.class, submitted.selectValue("auth"), ProjectConfig.AuthStyle.jwt),
                                parseEnum(ProjectConfig.Database.class, submitted.selectValue("database"), ProjectConfig.Database.postgresql),
                                parseCsv(submitted.textValue("deps")),
                                List.of(),
                                null
                        );
                        presetRepository.save(preset);
                        appendActivity(activity, "Saved preset: " + preset.name());
                        store.dispatch(AppAction.setStatus("Saved preset: " + preset.name()));
                    }
                    case "/delete", "delete" -> {
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
                    case "/help" -> store.dispatch(AppAction.setStatus("Type / to browse commands, TAB to autocomplete"));
                    case "/back", "back" -> {
                        routeRef.set(AppRoute.command_center);
                        store.dispatch(AppAction.navigate(AppRoute.command_center));
                        store.dispatch(AppAction.setStatus("Returned to command center"));
                    }
                    case "/quit", "/exit", "quit" -> quit();
                    default -> store.dispatch(AppAction.setStatus("Unknown command — type /help for a list"));
                }
            }

            private Element renderProgress(int terminalRows) {
                UiTheme theme = themeRef.get();
                List<String> activityLines = new ArrayList<>();
                synchronized (activity) {
                    activityLines.addAll(activity);
                }
                if (activityLines.isEmpty()) {
                    activityLines.add("Waiting for execution steps...");
                }

                boolean running = generationRunning.get();
                boolean awaiting = awaitingProgressConfirm.get();
                int step = progressStep.get();
                int total = progressTotal.get();
                double ratio = total > 0 ? (double) step / total : 0.0;

                boolean failed = generationFailed.get();
                Element statusIndicator;
                if (running) {
                    statusIndicator = spinner(SpinnerStyle.DOTS, progressMessage.get()).id("gen-spinner");
                } else if (failed) {
                    statusIndicator = ThemeText.paint("✗ " + progressMessage.get(), theme.errorText());
                } else {
                    statusIndicator = ThemeText.paint("✓ " + progressMessage.get(), theme.successAccent());
                }

                List<Element> progressItems = new ArrayList<>();
                progressItems.add(lineGauge(ratio)
                        .filledColor(failed ? theme.errorText() : theme.successAccent())
                        .unfilledColor(theme.mutedText())
                        .label("Step " + step + " / " + total)
                        .thick()
                        .id("progress-gauge"));
                progressItems.add(statusIndicator);

                if (failed && errorSummary.get() != null) {
                    progressItems.add(panel(" ERROR ",
                            ThemeText.paint(errorSummary.get(), theme.errorText())
                    ).rounded().borderColor(theme.errorText()).padding(1));
                }

                if (awaiting) {
                    progressItems.add(ThemeText.paint(
                            "◆ Press ENTER to return to command center",
                            failed ? theme.errorText() : theme.successAccent()));
                }

                progressItems.add(text(""));
                progressItems.add(ThemeText.paint("Execution Log", theme.titleAccent()));
                progressItems.add(list(activityLines)
                        .id("activity-list")
                        .scrollbar()
                        .scrollToEnd()
                        .rounded()
                        .borderColor(theme.panelBorder())
                        .displayOnly());

                return AppShell.render(
                                progressTitle.get(),
                                "Live execution updates",
                                column(progressItems.toArray(new Element[0])).spacing(1),
                                store.state().status(),
                                "Each sub-step reports progress including file operations",
                                theme,
                                -1,
                                terminalRows
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

    private static String validateProjectForm(FormState formState, DependencyCatalog catalog,
                                                  DependencyAliasRegistry aliasRegistry) {
        String name = formState.textValue("name");
        if (name == null || name.isBlank()) {
            return "Validation failed: Project name is required";
        }
        String groupId = formState.textValue("groupId");
        if (groupId != null && !groupId.isBlank() && !groupId.matches("[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*")) {
            return "Validation failed: Group ID must be a valid Java package (e.g. com.example)";
        }
        String artifactId = formState.textValue("artifactId");
        if (artifactId != null && !artifactId.isBlank() && !artifactId.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*")) {
            return "Validation failed: Artifact ID contains invalid characters";
        }
        String output = formState.textValue("outputDirectory");
        if (output != null && !output.isBlank() && output.contains("..")) {
            return "Validation failed: Output directory must not contain '..'";
        }
        if (!catalog.isEmpty()) {
            List<String> deps = parseCsv(formState.textValue("dependencies"));
            List<String> invalid = findUnknownDeps(deps, catalog, aliasRegistry);
            if (!invalid.isEmpty()) {
                return "Unknown dependencies: " + String.join(", ", invalid) + " — type to search the catalog";
            }
        }
        return null;
    }

    /**
     * Returns dependency IDs that are neither valid Initializr IDs nor known aliases.
     */
    private static List<String> findUnknownDeps(List<String> deps, DependencyCatalog catalog,
                                                 DependencyAliasRegistry aliasRegistry) {
        List<String> unknown = new ArrayList<>();
        for (String dep : deps) {
            if (dep.isBlank()) continue;
            if (catalog.isValid(dep)) continue;
            // Check if the alias registry recognizes it (resolve returns the input unchanged for unknown aliases,
            // but known aliases resolve to different IDs)
            List<String> resolved = aliasRegistry.resolve(dep);
            boolean isAlias = !(resolved.size() == 1 && resolved.get(0).equals(dep));
            if (!isAlias) {
                unknown.add(dep);
            }
        }
        return unknown;
    }

    private static FormState buildProjectFormState(List<String> presetNames, List<String> javaVersions,
                                                      List<String> bootVersions, String defaultName,
                                                      String defaultArtifact, GlobalConfig.Defaults defaults) {
        return FormState.builder()
                .selectField("preset", presetNames, 0)
                .textField("name", defaultName)
                .textField("groupId", defaults.groupId() != null ? defaults.groupId() : "com.example")
                .textField("artifactId", defaultArtifact)
                .selectField("javaVersion", javaVersions, indexOfOrDefault(javaVersions, defaults.javaVersion(), 0))
                .selectField("bootVersion", bootVersions, indexOfOrDefault(bootVersions, defaults.bootVersion(), 0))
                .selectField("packaging", List.of("jar", "war"),
                        defaults.packaging() != null ? indexOfOrDefault(List.of("jar", "war"), defaults.packaging().name(), 0) : 0)
                .selectField("buildTool", List.of("gradle", "maven"),
                        defaults.buildTool() != null ? indexOfOrDefault(List.of("gradle", "maven"), defaults.buildTool().name(), 0) : 0)
                .selectField("authStyle", List.of("jwt", "session", "none"), 0)
                .selectField("database", List.of("postgresql", "mysql", "h2"), 0)
                .textField("dependencies", "")
                .textField("scaffolds", "")
                .textField("outputDirectory", "./" + defaultArtifact)
                .build();
    }

    private static void applyPresetToForm(FormState form, Preset preset,
                                            List<String> javaVersions, List<String> bootVersions) {
        if (preset.groupId() != null) {
            form.setTextValue("groupId", preset.groupId());
        }
        if (preset.javaVersion() != null) {
            int idx = indexOfOrDefault(javaVersions, preset.javaVersion(), -1);
            if (idx >= 0) form.selectIndex("javaVersion", idx);
        }
        if (preset.bootVersion() != null) {
            int idx = indexOfOrDefault(bootVersions, preset.bootVersion(), -1);
            if (idx >= 0) form.selectIndex("bootVersion", idx);
        }
        if (preset.packaging() != null) {
            int idx = indexOfOrDefault(List.of("jar", "war"), preset.packaging().name(), -1);
            if (idx >= 0) form.selectIndex("packaging", idx);
        }
        if (preset.buildTool() != null) {
            int idx = indexOfOrDefault(List.of("gradle", "maven"), preset.buildTool().name(), -1);
            if (idx >= 0) form.selectIndex("buildTool", idx);
        }
        if (preset.authStyle() != null) {
            int idx = indexOfOrDefault(List.of("jwt", "session", "none"), preset.authStyle().name(), -1);
            if (idx >= 0) form.selectIndex("authStyle", idx);
        }
        if (preset.database() != null) {
            int idx = indexOfOrDefault(List.of("postgresql", "mysql", "h2"), preset.database().name(), -1);
            if (idx >= 0) form.selectIndex("database", idx);
        }
        if (preset.dependencies() != null && !preset.dependencies().isEmpty()) {
            form.setTextValue("dependencies", String.join(", ", preset.dependencies()));
        }
        if (preset.scaffolds() != null && !preset.scaffolds().isEmpty()) {
            form.setTextValue("scaffolds", String.join(", ", preset.scaffolds()));
        }
    }

    private static int indexOfOrDefault(List<String> options, String value, int fallback) {
        if (value == null) return fallback;
        int idx = options.indexOf(value);
        if (idx >= 0) return idx;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) return i;
        }
        return fallback;
    }

    private static CliArgs toCliArgs(FormState formState) {
        String name = defaultIfBlank(formState.textValue("name"), "springrad-app");
        String groupId = defaultIfBlank(formState.textValue("groupId"), "com.example");
        String artifactId = defaultIfBlank(formState.textValue("artifactId"), slugify(name));
        String javaVersion = formState.selectValue("javaVersion");
        String bootVersion = "latest".equals(formState.selectValue("bootVersion"))
                ? null : formState.selectValue("bootVersion");
        String packagingValue = defaultIfBlank(formState.selectValue("packaging"), "jar");
        String buildToolValue = defaultIfBlank(formState.selectValue("buildTool"), "gradle");
        String authStyleValue = defaultIfBlank(formState.selectValue("authStyle"), "jwt");
        String databaseValue = defaultIfBlank(formState.selectValue("database"), "postgresql");
        String output = defaultIfBlank(formState.textValue("outputDirectory"), "./" + artifactId);
        List<String> dependencies = parseCsv(formState.textValue("dependencies"));
        List<String> scaffolds = parseCsv(formState.textValue("scaffolds"));

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
                scaffolds,
                Path.of(output),
                null,
                false
        );
    }

    private static boolean applyTheme(
            String action,
            AtomicReference<UiTheme> themeRef,
            StyleEngine styleEngine,
            AppStore store) {
        UiTheme theme = parseThemeCommand(action);
        if (theme == null) {
            return false;
        }
        themeRef.set(theme);
        UiStyles.activate(styleEngine, theme);
        store.dispatch(AppAction.setStatus("Theme switched to " + theme.label()));
        return true;
    }

    private static String value(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
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

    private static String previewLine(String label, String value) {
        return String.format("%-12s %s", label + ":", value);
    }

    private static String normalizeSlashCommand(String raw) {
        return CommandAutocomplete.normalize(raw);
    }

    private static UiTheme parseThemeCommand(String normalized) {
        if (normalized == null || !normalized.startsWith("/theme")) {
            return null;
        }
        String[] parts = normalized.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return null;
        }
        String candidate = parts[1].trim();
        if (!UiTheme.valuesList().contains(candidate)) {
            return null;
        }
        return UiTheme.fromValue(candidate);
    }

    private static List<CommandDoc> filterSlashCommands(String input, List<CommandDoc> commands) {
        return CommandAutocomplete.filter(input, commands);
    }

    private static Element renderDepSuggestions(String depsInput, DependencyCatalog catalog, UiTheme theme) {
        if (catalog.isEmpty()) {
            return text("");
        }
        // Extract the last token being typed (after the last comma)
        String query = "";
        if (!depsInput.isBlank()) {
            String[] parts = depsInput.split(",", -1);
            query = parts[parts.length - 1].trim();
        }
        if (query.isEmpty()) {
            return panel(" DEPENDENCIES ",
                    ThemeText.paint(catalog.size() + " dependencies available", theme.mutedText()),
                    ThemeText.paint("Type to search the catalog", theme.mutedText())
            ).rounded().borderColor(theme.panelBorder()).padding(1);
        }
        List<DependencyEntry> matches = catalog.search(query, 8);
        if (matches.isEmpty()) {
            return panel(" DEPENDENCIES ",
                    ThemeText.paint("No matches for: " + query, theme.errorText())
            ).rounded().borderColor(theme.panelBorder()).padding(1);
        }
        List<Element> items = new ArrayList<>();
        for (DependencyEntry entry : matches) {
            boolean valid = entry.id().equalsIgnoreCase(query);
            items.add(ThemeText.paint(
                    (valid ? "✓ " : "  ") + entry.id() + " — " + entry.name(),
                    valid ? theme.successAccent() : theme.primaryText()));
            if (!entry.description().isBlank()) {
                items.add(ThemeText.paint("    " + truncate(entry.description(), 40), theme.mutedText()));
            }
        }
        return panel(" DEPENDENCIES ",
                column(items.toArray(new Element[0])).spacing(0)
        ).rounded().borderColor(theme.panelBorder()).padding(1);
    }

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 1) + "…";
    }

    private static Element renderCommandSuggestions(List<CommandDoc> commands, UiTheme theme) {
        if (commands.isEmpty()) {
            return ThemeText.paint("Type / to show command suggestions.", theme.mutedText()).id("command-suggestions");
        }
        List<Element> items = new ArrayList<>();
        items.add(ThemeText.paint("Commands", theme.titleAccent()));
        for (CommandDoc command : commands) {
            items.add(ThemeText.paint("• " + command.command(), theme.successAccent()));
            items.add(ThemeText.paint("  " + command.description(), theme.mutedText()));
        }
        return column(items.toArray(new Element[0])).id("command-suggestions").spacing(0);
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

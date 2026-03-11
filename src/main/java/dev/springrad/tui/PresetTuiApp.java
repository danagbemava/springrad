package dev.springrad.tui;

import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import dev.springrad.tui.app.AppAction;
import dev.springrad.tui.app.AppRoute;
import dev.springrad.tui.app.AppShell;
import dev.springrad.tui.app.AppState;
import dev.springrad.tui.app.AppStore;
import dev.springrad.tui.app.CommandAutocomplete;
import dev.springrad.tui.app.CommandDoc;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.columns;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.text;

public final class PresetTuiApp {
    private static final List<CommandDoc> COMMANDS = List.of(
            new CommandDoc("/list", "List available presets"),
            new CommandDoc("/save", "Save/update preset from form fields"),
            new CommandDoc("/delete", "Delete preset by name"),
            new CommandDoc("/quit", "Exit preset manager"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch theme to Ocean"),
            new CommandDoc("/theme graphite", "Switch theme to Graphite"),
            new CommandDoc("/theme neon", "Switch theme to Neon")
    );

    private final PresetRepository repository;
    private final PresetService presetService;
    private final ActivityLogStore activityLogStore;

    public PresetTuiApp() {
        this(new PresetRepository());
    }

    PresetTuiApp(PresetRepository repository) {
        this.repository = repository;
        this.presetService = new PresetService(repository);
        this.activityLogStore = new ActivityLogStore();
    }

    public void start() {
        AppStore store = new AppStore(AppState.initial(AppRoute.preset_manager));
        presetService.seedDefaults();
        FormState formState = FormState.builder()
                .textField("command", "")
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

        List<String> activity = new ArrayList<>(activityLogStore.tail(20));
        if (activity.isEmpty()) {
            activity.add("Preset manager started.");
            activityLogStore.append("Preset manager started.");
        }
        AtomicReference<UiTheme> themeRef = new AtomicReference<>(UiTheme.ocean);
        StyleEngine styleEngine = UiStyles.createEngine();
        store.dispatch(AppAction.setStatus("Preset manager ready"));
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

            @Override
            protected Element render() {
                UiTheme theme = themeRef.get();
                final FormElement[] presetFormRef = new FormElement[1];
                FormElement presetForm = form(formState)
                        .field("command", "Command")
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
                        .borderColor(theme.panelBorder())
                        .focusedBorderColor(theme.panelAccentBorder())
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onKeyEvent(event -> {
                            if (event.isKey(KeyCode.TAB) && !event.hasShift() && !event.hasCtrl() && !event.hasAlt()) {
                                String completion = CommandAutocomplete.complete(formState.textValue("command"), COMMANDS);
                                if (completion != null) {
                                    formState.setTextValue("command", completion);
                                    store.dispatch(AppAction.setStatus("Autocompleted: " + completion));
                                    return EventResult.HANDLED;
                                }
                            }
                            if (event.isConfirm()) {
                                presetFormRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        })
                        .onSubmit(submitted -> {
                            String action = value(normalizeSlashCommand(submitted.textValue("command")), "/list");
                            UiTheme requestedTheme = parseThemeCommand(action);
                            if (requestedTheme != null) {
                                themeRef.set(requestedTheme);
                                UiStyles.activate(styleEngine, requestedTheme);
                                store.dispatch(AppAction.setStatus("Theme switched to " + requestedTheme.label()));
                                return;
                            }
                            switch (action) {
                                case "/list", "list" -> {
                                    List<Preset> presets = repository.findAll();
                                    if (presets.isEmpty()) {
                                        activity.add("No presets found.");
                                        activityLogStore.append("No presets found.");
                                        store.dispatch(AppAction.setStatus("No presets found"));
                                    } else {
                                        activity.add("Presets:");
                                        activityLogStore.append("Listing presets.");
                                        store.dispatch(AppAction.setStatus("Listing presets"));
                                        for (Preset preset : presets) {
                                            String row = "- %s%s (%s/%s) deps=%s".formatted(
                                                    preset.name(),
                                                    preset.builtIn() ? " [built-in]" : "",
                                                    preset.authStyle(),
                                                    preset.database(),
                                                    String.join(",", preset.dependencies())
                                            );
                                            activity.add(row);
                                            activityLogStore.append(row);
                                        }
                                    }
                                }
                                case "/save", "save" -> {
                                    String name = submitted.textValue("name");
                                    if (name == null || name.isBlank()) {
                                        activity.add("Cannot save: preset name is required.");
                                        activityLogStore.append("Cannot save preset: missing name.");
                                        store.dispatch(AppAction.setStatus("Save failed: missing preset name"));
                                        return;
                                    }
                                    Preset existing = repository.findByName(name).orElse(null);
                                    if (existing != null && existing.builtIn()) {
                                        activity.add("Cannot overwrite built-in preset: " + name);
                                        activityLogStore.append("Cannot overwrite built-in preset: " + name);
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
                                            List.of(),
                                            null
                                    );
                                    repository.save(preset);
                                    activity.add("Saved preset: " + preset.name());
                                    activityLogStore.append("Saved preset: " + preset.name());
                                    store.dispatch(AppAction.setStatus("Saved preset: " + preset.name()));
                                }
                                case "/delete", "delete" -> {
                                    String name = submitted.textValue("name");
                                    if (name == null || name.isBlank()) {
                                        activity.add("Cannot delete: preset name is required.");
                                        activityLogStore.append("Cannot delete preset: missing name.");
                                        store.dispatch(AppAction.setStatus("Delete failed: missing preset name"));
                                        return;
                                    }
                                    if (repository.delete(name.trim())) {
                                        activity.add("Deleted preset: " + name.trim());
                                        activityLogStore.append("Deleted preset: " + name.trim());
                                        store.dispatch(AppAction.setStatus("Deleted preset: " + name.trim()));
                                    } else {
                                        activity.add("Delete failed (not found or built-in): " + name.trim());
                                        activityLogStore.append("Delete failed (not found or built-in): " + name.trim());
                                        store.dispatch(AppAction.setStatus("Delete failed: not found or built-in"));
                                    }
                                }
                                case "/quit", "/exit", "quit" -> {
                                    activityLogStore.append("Preset manager exited by user.");
                                    store.dispatch(AppAction.setStatus("Exiting preset manager"));
                                    quit();
                                }
                                default -> {
                                    activity.add("Unknown action: " + action);
                                    activityLogStore.append("Unknown preset action: " + action);
                                    store.dispatch(AppAction.setStatus("Unknown action"));
                                }
                            }
                        })
                        .id("command-form");
                presetFormRef[0] = presetForm;
                List<CommandDoc> commandMatches = filterSlashCommands(formState.textValue("command"));

                List<String> activityLines = new ArrayList<>(activity);
                if (activityLines.isEmpty()) {
                    activityLines.add("No activity yet. Choose an action and press ENTER.");
                }

                return AppShell.render(
                        "Preset Management",
                        "Manage presets with one unified flow",
                        columns(
                                presetForm.percent(65),
                                column(
                                        ThemeText.paint("Activity", theme.titleAccent()),
                                        renderCommandSuggestions(commandMatches, theme),
                                        text(""),
                                        list(activityLines)
                                                .id("activity-list")
                                                .scrollbar()
                                                .rounded()
                                                .borderColor(theme.panelBorder())
                                                .displayOnly()
                                ).percent(35)
                        ).spacing(1),
                        store.state().status(),
                        "Commands: /list /save /delete /quit /exit /theme <name>",
                        theme
                );
            }
        };

        try {
            app.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run preset management TUI", e);
        }
    }

    private static String value(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }

    private static String normalizeSlashCommand(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    private static List<CommandDoc> filterSlashCommands(String raw) {
        return CommandAutocomplete.filter(raw, COMMANDS);
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

    private static Element renderCommandSuggestions(List<CommandDoc> commands, UiTheme theme) {
        if (commands.isEmpty()) {
            return ThemeText.paint("Type / to show command suggestions.", theme.mutedText()).id("command-suggestions");
        }
        List<Element> rows = new ArrayList<>();
        rows.add(ThemeText.paint("Commands", theme.titleAccent()));
        for (CommandDoc command : commands) {
            rows.add(ThemeText.paint("• " + command.command(), theme.successAccent()));
            rows.add(ThemeText.paint("  " + command.description(), theme.mutedText()));
        }
        return column(rows.toArray(new Element[0])).id("command-suggestions").spacing(0);
    }

    private static String blankToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String rawValue, E fallback) {
        try {
            return Enum.valueOf(enumType, value(rawValue, fallback.name()));
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
        return List.copyOf(items);
    }
}

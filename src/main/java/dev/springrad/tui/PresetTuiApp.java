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
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.columns;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.text;

public final class PresetTuiApp {
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
                .selectField("action", List.of("list", "save", "delete", "quit"), 0)
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
        store.dispatch(AppAction.setStatus("Preset manager ready"));
        ToolkitApp app = new ToolkitApp() {
            @Override
            protected TuiConfig configure() {
                return TuiRuntime.createConfig();
            }

            @Override
            protected Element render() {
                final FormElement[] presetFormRef = new FormElement[1];
                FormElement presetForm = form(formState)
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
                        .onKeyEvent(event -> {
                            if (event.isConfirm()) {
                                presetFormRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        })
                        .onSubmit(submitted -> {
                            String action = value(submitted.selectValue("action"), "list");
                            switch (action) {
                                case "list" -> {
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
                                case "save" -> {
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
                                            List.of()
                                    );
                                    repository.save(preset);
                                    activity.add("Saved preset: " + preset.name());
                                    activityLogStore.append("Saved preset: " + preset.name());
                                    store.dispatch(AppAction.setStatus("Saved preset: " + preset.name()));
                                }
                                case "delete" -> {
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
                                case "quit" -> {
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
                        });
                presetFormRef[0] = presetForm;

                int from = Math.max(0, activity.size() - 12);
                List<Element> logLines = new ArrayList<>();
                for (int i = from; i < activity.size(); i++) {
                    logLines.add(text(activity.get(i)));
                }
                if (logLines.isEmpty()) {
                    logLines.add(text("No activity yet. Choose an action and press ENTER.").gray());
                }

                return AppShell.render(
                        "Preset Management",
                        "Manage presets with one unified flow",
                        columns(
                                presetForm.percent(65),
                                column(
                                        text("Activity").bold().magenta(),
                                        column(logLines.toArray(new Element[0])).spacing(0)
                                ).percent(35)
                        ).spacing(1),
                        store.state().status(),
                        "Keys: TAB/Shift+TAB to navigate, arrows for selects, ENTER to execute action"
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

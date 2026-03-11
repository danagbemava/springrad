package dev.springrad.tui;

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
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.text;

public final class CommandCenterTuiApp {
    private static final List<CommandDoc> COMMANDS = List.of(
            new CommandDoc("/generate", "Open project wizard"),
            new CommandDoc("/presets", "Open preset manager"),
            new CommandDoc("/quit", "Exit app"),
            new CommandDoc("/exit", "Alias for /quit"),
            new CommandDoc("/theme ocean", "Switch theme to Ocean"),
            new CommandDoc("/theme graphite", "Switch theme to Graphite"),
            new CommandDoc("/theme neon", "Switch theme to Neon")
    );

    public enum Action {
        generate,
        presets,
        quit
    }

    public Action start() {
        AppStore store = new AppStore(AppState.initial(AppRoute.command_center));
        FormState formState = FormState.builder()
                .textField("command", "")
                .build();
        AtomicReference<Action> selection = new AtomicReference<>(Action.quit);
        AtomicReference<UiTheme> themeRef = new AtomicReference<>(UiTheme.ocean);
        StyleEngine styleEngine = UiStyles.createEngine();

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
                final FormElement[] actionFormRef = new FormElement[1];
                FormElement actionForm = form(formState)
                        .field("command", "Command")
                        .labelWidth(16)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(theme.panelBorder())
                        .focusedBorderColor(theme.panelAccentBorder())
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(submitted -> {
                            String raw = normalizeSlashCommand(submitted.textValue("command"));
                            UiTheme requestedTheme = parseThemeCommand(raw);
                            if (requestedTheme != null) {
                                themeRef.set(requestedTheme);
                                UiStyles.activate(styleEngine, requestedTheme);
                                store.dispatch(AppAction.setStatus("Theme switched to " + requestedTheme.label()));
                                return;
                            }
                            selection.set(parseAction(raw));
                            store.dispatch(AppAction.setStatus("Selected action: " + raw));
                            quit();
                        })
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
                                actionFormRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        })
                        .id("command-form");
                actionFormRef[0] = actionForm;
                List<CommandDoc> commandMatches = filterSlashCommands(formState.textValue("command"));

                return AppShell.render(
                        "Command Center",
                        "Everything managed through TUI",
                        column(
                                ThemeText.paint("Choose a slash command and press ENTER", theme.primaryText()),
                                text(""),
                                actionForm,
                                renderCommandSuggestions(commandMatches, theme)
                        ).spacing(1),
                        store.state().status(),
                        "Commands: /generate /presets /quit /exit /theme <name>",
                        theme
                );
            }
        };

        try {
            app.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run command center TUI", e);
        }
        return selection.get();
    }

    private static Action parseAction(String raw) {
        if ("/generate".equals(raw) || "generate_project".equals(raw)) {
            return Action.generate;
        }
        if ("/presets".equals(raw) || "manage_presets".equals(raw)) {
            return Action.presets;
        }
        if ("/exit".equals(raw) || "/quit".equals(raw) || "quit".equals(raw)) {
            return Action.quit;
        }
        return Action.quit;
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
        List<Element> rows = new java.util.ArrayList<>();
        rows.add(ThemeText.paint("Commands", theme.titleAccent()));
        for (CommandDoc command : commands) {
            rows.add(ThemeText.paint("• " + command.command(), theme.successAccent()));
            rows.add(ThemeText.paint("  " + command.description(), theme.mutedText()));
        }
        return column(rows.toArray(new Element[0])).id("command-suggestions").spacing(0);
    }
}

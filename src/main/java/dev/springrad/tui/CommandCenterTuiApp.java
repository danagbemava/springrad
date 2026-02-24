package dev.springrad.tui;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

public final class CommandCenterTuiApp {
    public enum Action {
        generate_project,
        manage_presets,
        quit
    }

    public Action start() {
        FormState formState = FormState.builder()
                .selectField("action", List.of("generate_project", "manage_presets", "quit"), 0)
                .build();
        AtomicReference<Action> selection = new AtomicReference<>(Action.quit);

        ToolkitApp app = new ToolkitApp() {
            @Override
            protected TuiConfig configure() {
                return TuiRuntime.createConfig();
            }

            @Override
            protected Element render() {
                final FormElement[] actionFormRef = new FormElement[1];
                FormElement actionForm = form(formState)
                        .field("action", "Action", FieldType.SELECT)
                        .labelWidth(16)
                        .fieldSpacing(1)
                        .rounded()
                        .borderColor(Color.CYAN)
                        .focusedBorderColor(Color.LIGHT_CYAN)
                        .submitOnEnter(true)
                        .arrowNavigation(true)
                        .onSubmit(submitted -> {
                            String raw = submitted.selectValue("action");
                            selection.set(Action.valueOf(raw));
                            quit();
                        })
                        .onKeyEvent(event -> {
                            if (event.isConfirm()) {
                                actionFormRef[0].submit();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        });
                actionFormRef[0] = actionForm;

                return column(
                        panel(" SPRINGRAD COMMAND CENTER ",
                                text("Everything managed through TUI").bold().white(),
                                text("Choose an action and press ENTER").cyan(),
                                text(""),
                                actionForm,
                                text(""),
                                text("Use TAB/Shift+TAB and arrow keys. ENTER confirms.").gray()
                        ).doubleBorder().borderColor(Color.CYAN).padding(1)
                ).spacing(1);
            }
        };

        try {
            app.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run command center TUI", e);
        }
        return selection.get();
    }
}

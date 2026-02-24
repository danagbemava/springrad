package dev.springrad.tui.app;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Column;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

public final class AppShell {
    private AppShell() {
    }

    public static Column render(String title, String subtitle, Element body, String status, String hint) {
        return column(
                panel(" SPRINGRAD ",
                        text(title).bold().white(),
                        text(subtitle).cyan(),
                        text(""),
                        body,
                        text(""),
                        text(status == null || status.isBlank() ? "Status: ready" : "Status: " + status).yellow(),
                        text(hint).gray()
                ).doubleBorder().borderColor(Color.CYAN).padding(1)
        ).spacing(1);
    }
}

package dev.springrad.tui.app;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.Column;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.columns;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.waveText;

public final class AppShell {
    private AppShell() {
    }

    /**
     * Three-tier logo size derived from terminal height.
     * <ul>
     *   <li>FULL    – ≥ 40 rows: full 6-row ASCII art + wave</li>
     *   <li>COMPACT – 28–39 rows: last 2 ASCII rows + wave; subtitle/version hidden</li>
     *   <li>MINIMAL – &lt; 28 rows: single text line; subtitle/version hidden</li>
     * </ul>
     */
    enum LogoSize {
        FULL, COMPACT, MINIMAL;

        static LogoSize fromRows(int rows) {
            if (rows >= 40) return FULL;
            if (rows >= 28) return COMPACT;
            return MINIMAL;
        }
    }

    // --- public overloads (all delegate to the 8-param canonical form) ---

    public static Column render(String title, String subtitle, Element body, String status, String hint) {
        return render(title, subtitle, body, status, hint, UiTheme.ocean, -1, Integer.MAX_VALUE);
    }

    public static Column render(String title, String subtitle, Element body, String status, String hint, UiTheme theme) {
        return render(title, subtitle, body, status, hint, theme, -1, Integer.MAX_VALUE);
    }

    public static Column render(String title, String subtitle, Element body, String status, String hint, UiTheme theme, int selectedTab) {
        return render(title, subtitle, body, status, hint, theme, selectedTab, Integer.MAX_VALUE);
    }

    public static Column render(String title, String subtitle, Element body, String status, String hint,
                                UiTheme theme, int selectedTab, int terminalRows) {
        LogoSize logoSize = LogoSize.fromRows(terminalRows);
        Element asciiLogo = switch (logoSize) {
            case FULL    -> buildFullLogo(theme);
            case COMPACT -> buildCompactLogo(theme);
            case MINIMAL -> buildMinimalLogo(theme);
        };

        List<Element> content = new ArrayList<>();
        content.add(asciiLogo);
        content.add(text(""));
        if (selectedTab >= 0) {
            content.add(tabs("Command Center", "Project Wizard", "Presets")
                    .selected(selectedTab)
                    .highlightColor(theme.titleAccent())
                    .borderColor(theme.panelBorder())
                    .rounded());
            content.add(text(""));
        }
        content.add(ThemeText.paint(title, theme.primaryText()));
        if (logoSize != LogoSize.MINIMAL) {
            content.add(ThemeText.paint(subtitle, theme.mutedText()));
            content.add(ThemeText.paint("Version: springrad " + VersionInfo.version(), theme.mutedText()));
        }
        content.add(text(""));
        content.add(body);
        content.add(text(""));
        content.add(columns(
                ThemeText.paint(" ◆ " + status, theme.statusAccent()).percent(65),
                ThemeText.paint(hint, theme.mutedText()).percent(35)
        ).spacing(0));

        return column(
                panel(" SPRINGRAD ", column(content.toArray(new Element[0])).spacing(0))
                        .id("root-panel")
                        .addClass("root-panel")
                        .rounded()
                        .borderColor(theme.titleAccent())
                        .padding(1)
        ).addClass("app-shell").spacing(1);
    }

    // --- private logo builders ---

    /** Full 6-row ASCII art + animated wave (≥ 40 rows). */
    private static Element buildFullLogo(UiTheme theme) {
        return column(
                ThemeText.paint(" ███████╗██████╗ ██████╗ ██╗███╗   ██╗ ██████╗ ██████╗  █████╗ ██████╗ ", theme.primaryText()),
                ThemeText.paint(" ██╔════╝██╔══██╗██╔══██╗██║████╗  ██║██╔════╝ ██╔══██╗██╔══██╗██╔══██╗", theme.primaryText()),
                ThemeText.paint(" ███████╗██████╔╝██████╔╝██║██╔██╗ ██║██║  ███╗██████╔╝███████║██║  ██║", theme.primaryText()),
                ThemeText.paint(" ╚════██║██╔═══╝ ██╔══██╗██║██║╚██╗██║██║   ██║██╔══██╗██╔══██║██║  ██║", theme.mutedText()),
                ThemeText.paint(" ███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝██║  ██║██║  ██║██████╔╝", theme.mutedText()),
                ThemeText.paint(" ╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ", theme.panelAccentBorder()),
                waveText("  ░░░░░░ ░░░░░░  ░░░░░░  ░░ ░░░░   ░░ ░░░░░░  ░░░░░░   ░░░░░  ░░░░░░  ", theme.panelAccentBorder())
                        .speed(0.3)
                        .peakCount(2)
                        .id("logo-wave")
        ).spacing(0);
    }

    /** Last 2 ASCII rows + wave (28–39 rows). Saves 4 rows vs FULL. */
    private static Element buildCompactLogo(UiTheme theme) {
        return column(
                ThemeText.paint(" ███████║██║     ██║  ██║██║██║ ╚████║╚██████╔╝██║  ██║██║  ██║██████╔╝", theme.mutedText()),
                ThemeText.paint(" ╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ", theme.panelAccentBorder()),
                waveText("  ░░░░░░ ░░░░░░  ░░░░░░  ░░ ░░░░   ░░ ░░░░░░  ░░░░░░   ░░░░░  ░░░░░░  ", theme.panelAccentBorder())
                        .speed(0.3)
                        .peakCount(2)
                        .id("logo-wave")
        ).spacing(0);
    }

    /** Single text line (< 28 rows). Saves 6 rows vs FULL. */
    private static Element buildMinimalLogo(UiTheme theme) {
        return ThemeText.paint("◆ SPRINGRAD", theme.panelAccentBorder());
    }
}

package dev.springrad.tui.app;

import dev.tamboui.style.Color;

import java.util.List;
import java.util.Locale;

public enum UiTheme {
    ocean("Ocean", Color.CYAN, Color.LIGHT_CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.WHITE, Color.GRAY),
    graphite("Graphite", Color.WHITE, Color.GRAY, Color.CYAN, Color.YELLOW, Color.GREEN, Color.WHITE, Color.GRAY),
    neon("Neon", Color.MAGENTA, Color.CYAN, Color.LIGHT_CYAN, Color.YELLOW, Color.GREEN, Color.WHITE, Color.GRAY);

    private final String label;
    private final Color panelBorder;
    private final Color panelAccentBorder;
    private final Color titleAccent;
    private final Color statusAccent;
    private final Color successAccent;
    private final Color primaryText;
    private final Color mutedText;

    UiTheme(
            String label,
            Color panelBorder,
            Color panelAccentBorder,
            Color titleAccent,
            Color statusAccent,
            Color successAccent,
            Color primaryText,
            Color mutedText
    ) {
        this.label = label;
        this.panelBorder = panelBorder;
        this.panelAccentBorder = panelAccentBorder;
        this.titleAccent = titleAccent;
        this.statusAccent = statusAccent;
        this.successAccent = successAccent;
        this.primaryText = primaryText;
        this.mutedText = mutedText;
    }

    public static UiTheme fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ocean;
        }
        try {
            return UiTheme.valueOf(value.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ocean;
        }
    }

    public static List<String> valuesList() {
        return List.of(ocean.name(), graphite.name(), neon.name());
    }

    public String label() {
        return label;
    }

    public Color panelBorder() {
        return panelBorder;
    }

    public Color panelAccentBorder() {
        return panelAccentBorder;
    }

    public Color titleAccent() {
        return titleAccent;
    }

    public Color statusAccent() {
        return statusAccent;
    }

    public Color successAccent() {
        return successAccent;
    }

    public Color primaryText() {
        return primaryText;
    }

    public Color mutedText() {
        return mutedText;
    }
}

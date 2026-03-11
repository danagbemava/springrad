package dev.springrad.tui.app;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.elements.TextElement;

import static dev.tamboui.toolkit.Toolkit.text;

public final class ThemeText {
    private ThemeText() {
    }

    public static TextElement paint(String content, Color color) {
        if (color == Color.CYAN) {
            return text(content).cyan();
        }
        if (color == Color.LIGHT_CYAN) {
            return text(content).cyan();
        }
        if (color == Color.MAGENTA) {
            return text(content).magenta();
        }
        if (color == Color.YELLOW) {
            return text(content).yellow();
        }
        if (color == Color.GREEN) {
            return text(content).green();
        }
        if (color == Color.GRAY) {
            return text(content).gray();
        }
        return text(content).white();
    }
}

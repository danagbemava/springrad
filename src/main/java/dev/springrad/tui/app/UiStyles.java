package dev.springrad.tui.app;

import dev.tamboui.css.engine.StyleEngine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class UiStyles {
    private UiStyles() {
    }

    public static StyleEngine createEngine() {
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet(UiTheme.ocean.name(), loadResource("/themes/ocean.tcss"));
        engine.addStylesheet(UiTheme.graphite.name(), loadResource("/themes/graphite.tcss"));
        engine.addStylesheet(UiTheme.neon.name(), loadResource("/themes/neon.tcss"));
        engine.setActiveStylesheet(UiTheme.ocean.name());
        return engine;
    }

    public static void activate(StyleEngine engine, UiTheme theme) {
        if (engine == null || theme == null) {
            return;
        }
        engine.setActiveStylesheet(theme.name());
    }

    private static String loadResource(String path) {
        try (InputStream is = UiStyles.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Theme resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load theme resource: " + path, e);
        }
    }
}

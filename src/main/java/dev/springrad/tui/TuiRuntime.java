package dev.springrad.tui;

import dev.tamboui.backend.panama.PanamaBackendProvider;
import dev.tamboui.tui.TuiConfig;

import java.io.IOException;
import java.io.UncheckedIOException;

final class TuiRuntime {
    private TuiRuntime() {
    }

    static TuiConfig createConfig() {
        try {
            return TuiConfig.builder()
                    .backend(new PanamaBackendProvider().create())
                    .build();
        } catch (UnsupportedClassVersionError e) {
            throw new IllegalStateException(
                    "Interactive TUI requires Java 22+ (Panama backend). " +
                            "Run with JAVA_HOME pointing to JDK 22 and " +
                            "JDK_JAVA_OPTIONS=--enable-native-access=ALL-UNNAMED.",
                    e
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize TamboUI backend", e);
        } catch (LinkageError e) {
            throw new IllegalStateException(
                    "Failed to initialize TUI backend. Ensure runtime is Java 22+ for Panama backend.",
                    e
            );
        }
    }
}

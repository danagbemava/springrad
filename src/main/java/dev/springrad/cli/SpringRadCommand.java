package dev.springrad.cli;

import dev.springrad.tui.CommandCenterTuiApp;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Command(
        name = "springrad",
        mixinStandardHelpOptions = true,
        description = "Spring Boot RAD CLI — generate production-ready projects fast.",
        subcommands = {NewCommand.class, PresetCommand.class},
        versionProvider = SpringRadCommand.VersionProvider.class
)
public final class SpringRadCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        if (System.console() == null) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }

        CommandCenterTuiApp.Action action = new CommandCenterTuiApp().start();
        switch (action) {
            case generate_project -> spec.commandLine().execute("new", "springrad-app", "--interactive");
            case manage_presets -> spec.commandLine().execute("preset");
            case quit -> {
            }
        }
    }

    static final class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() throws IOException {
            Properties properties = new Properties();
            try (InputStream in = VersionProvider.class.getClassLoader().getResourceAsStream("version.properties")) {
                if (in == null) {
                    return new String[]{"springrad version: unknown"};
                }
                properties.load(in);
                String version = properties.getProperty("version", "unknown");
                return new String[]{"springrad version: " + version};
            }
        }
    }
}

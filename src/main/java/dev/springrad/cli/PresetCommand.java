package dev.springrad.cli;

import dev.springrad.tui.PresetTuiApp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(
        name = "preset",
        description = "Manage project presets",
        subcommands = {
                PresetListCommand.class,
                PresetSaveCommand.class,
                PresetDeleteCommand.class
        }
)
public final class PresetCommand implements Runnable {
    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        if (System.console() == null) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return;
        }
        try {
            new PresetTuiApp().start();
        } catch (IllegalStateException e) {
            spec.commandLine().getErr().println(e.getMessage());
        }
    }
}

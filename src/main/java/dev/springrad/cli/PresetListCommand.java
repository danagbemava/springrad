package dev.springrad.cli;

import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.List;

@Command(name = "list", description = "List available presets")
public final class PresetListCommand implements Runnable {
    private final PresetRepository repository;

    @Spec
    private CommandSpec spec;

    public PresetListCommand() {
        this(new PresetRepository());
    }

    PresetListCommand(PresetRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run() {
        List<Preset> presets = repository.findAll();
        if (presets.isEmpty()) {
            spec.commandLine().getOut().println("No presets saved. Use `springrad preset save` to create one.");
            return;
        }

        spec.commandLine().getOut().println("Name\tDependencies\tAuth\tDatabase");
        for (Preset preset : presets) {
            String deps = String.join(",", preset.dependencies());
            spec.commandLine().getOut().printf(
                    "%s%s\t%s\t%s\t%s%n",
                    preset.name(),
                    preset.builtIn() ? " (built-in)" : "",
                    deps,
                    preset.authStyle() == null ? "-" : preset.authStyle(),
                    preset.database() == null ? "-" : preset.database()
            );
        }
    }
}

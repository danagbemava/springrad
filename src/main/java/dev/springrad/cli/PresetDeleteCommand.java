package dev.springrad.cli;

import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.Optional;

@Command(name = "delete", description = "Delete a preset")
public final class PresetDeleteCommand implements Runnable {
    private final PresetRepository repository;

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<name>", description = "Preset name")
    String name;

    public PresetDeleteCommand() {
        this(new PresetRepository());
    }

    PresetDeleteCommand(PresetRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run() {
        Optional<Preset> preset = repository.findByName(name);
        if (preset.isEmpty()) {
            spec.commandLine().getErr().printf("Preset '%s' not found.%n", name);
            return;
        }
        if (preset.get().builtIn()) {
            spec.commandLine().getErr().printf("Cannot delete built-in preset '%s'.%n", name);
            return;
        }

        repository.delete(name);
        spec.commandLine().getOut().printf("Deleted preset '%s'.%n", name);
    }
}

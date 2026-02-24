package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.List;

@Command(name = "save", description = "Save a preset")
public final class PresetSaveCommand implements Runnable {
    private final PresetRepository repository;

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<name>", description = "Preset name")
    String name;

    @Option(names = "--deps", split = ",", description = "Comma-separated Initializr dependency IDs")
    List<String> dependencies = List.of();

    @Option(names = "--auth", defaultValue = "jwt")
    ProjectConfig.AuthStyle auth;

    @Option(names = "--database", defaultValue = "postgresql")
    ProjectConfig.Database database;

    @Option(names = "--group")
    String groupId;

    @Option(names = "--java-version", defaultValue = "21")
    String javaVersion;

    @Option(names = "--boot-version")
    String bootVersion;

    @Option(names = "--build", defaultValue = "gradle")
    ProjectConfig.BuildTool buildTool;

    @Option(names = "--packaging", defaultValue = "jar")
    ProjectConfig.Packaging packaging;

    public PresetSaveCommand() {
        this(new PresetRepository());
    }

    PresetSaveCommand(PresetRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run() {
        Preset existing = repository.findByName(name).orElse(null);
        if (existing != null && existing.builtIn()) {
            spec.commandLine().getErr().printf("Cannot overwrite built-in preset '%s'.%n", name);
            return;
        }

        Preset preset = new Preset(
                name,
                false,
                groupId,
                javaVersion,
                bootVersion,
                packaging,
                buildTool,
                auth,
                database,
                dependencies == null ? List.of() : dependencies,
                List.of()
        );
        repository.save(preset);
        spec.commandLine().getOut().printf("Saved preset '%s'.%n", name);
    }
}

package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import dev.springrad.tui.SpringRadTuiApp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.Callable;

@Command(name = "new", description = "Generate a new Spring Boot project")
public final class NewCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<name>", description = "Project name")
    String name;

    @Option(names = {"--interactive", "-i"}, description = "Start interactive TUI flow", defaultValue = "true")
    boolean interactive;

    @Option(names = {"--preset", "-p"}, description = "Named preset to use")
    String presetName;

    @Option(names = {"--group", "-g"}, defaultValue = "com.example", description = "Maven group ID")
    String groupId;

    @Option(names = {"--artifact", "-a"}, description = "Artifact ID (defaults to <name>)")
    String artifactId;

    @Option(names = {"--java-version", "-j"}, defaultValue = "21", description = "Java version")
    String javaVersion;

    @Option(names = "--packaging", defaultValue = "jar", description = "Packaging: ${COMPLETION-CANDIDATES}")
    ProjectConfig.Packaging packaging;

    @Option(names = "--build", defaultValue = "gradle", description = "Build tool: ${COMPLETION-CANDIDATES}")
    ProjectConfig.BuildTool buildTool;

    @Option(names = "--boot-version", description = "Spring Boot version")
    String bootVersion;

    @Option(names = "--auth", defaultValue = "jwt", description = "Auth style: ${COMPLETION-CANDIDATES}")
    ProjectConfig.AuthStyle authStyle;

    @Option(names = {"--database", "-d"}, defaultValue = "postgresql", description = "Database: ${COMPLETION-CANDIDATES}")
    ProjectConfig.Database database;

    @Option(names = {"--output", "-o"}, description = "Output directory")
    Path outputDirectory;

    @Override
    public Integer call() {
        if (interactive) {
            new SpringRadTuiApp().start();
            return 0;
        }

        if (presetName == null) {
            spec.commandLine().getErr().println(
                    "No preset provided. Use --interactive for guided mode or pass --preset <name>."
            );
            return 1;
        }

        CliArgs cliArgs = toCliArgs();
        Preset preset = Preset.named(presetName);
        ProjectConfig config = ProjectConfig.merge(preset, cliArgs);
        spec.commandLine().getOut().printf(
                "Resolved config: name=%s, group=%s, artifact=%s, build=%s, output=%s%n",
                config.name(),
                config.groupId(),
                config.artifactId(),
                config.buildTool().name().toLowerCase(),
                config.outputDirectory()
        );
        return 0;
    }

    CliArgs toCliArgs() {
        return new CliArgs(
                name,
                groupId,
                artifactId,
                javaVersion,
                bootVersion,
                packaging,
                buildTool,
                authStyle,
                database,
                Collections.emptyList(),
                Collections.emptyList(),
                outputDirectory
        );
    }
}

package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.InitializrClient;
import dev.springrad.core.SpringRadException;
import dev.springrad.core.GitInitializer;
import dev.springrad.preset.PresetService;
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
    private final PresetService presetService;
    private final ProjectGenerator projectGenerator;
    private final GitInitializer gitInitializer;

    public NewCommand() {
        this(new PresetService(), new InitializrClient(), new GitInitializer());
    }

    NewCommand(PresetService presetService, ProjectGenerator projectGenerator) {
        this(presetService, projectGenerator, new GitInitializer());
    }

    NewCommand(PresetService presetService, ProjectGenerator projectGenerator, GitInitializer gitInitializer) {
        this.presetService = presetService;
        this.projectGenerator = projectGenerator;
        this.gitInitializer = gitInitializer;
    }

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<name>", description = "Project name")
    String name;

    @Option(names = {"--interactive", "-i"}, description = "Start interactive TUI flow", defaultValue = "false")
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

    @Option(names = {"--verbose", "-v"}, description = "Show full stack traces for errors")
    boolean verbose;

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

        try {
            CliArgs cliArgs = toCliArgs();
            ProjectConfig config = presetService.resolve(presetName, cliArgs);
            projectGenerator.generate(config);
            gitInitializer.initializeRepository(config.outputDirectory(), spec.commandLine().getErr());
            spec.commandLine().getOut().printf("Project generated at %s%n", config.outputDirectory());
            return 0;
        } catch (SpringRadException e) {
            spec.commandLine().getErr().println(e.getUserMessage());
            if (verbose) {
                e.printStackTrace(spec.commandLine().getErr());
            }
            return 1;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return 1;
        }
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

package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.InitializrClient;
import dev.springrad.core.SpringRadException;
import dev.springrad.core.GitInitializer;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
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
    private final SpringRadTuiApp tuiApp;
    private final TemplateOverlayEngine templateOverlayEngine;

    public NewCommand() {
        this(new PresetService(), new InitializrClient(), new GitInitializer(), new SpringRadTuiApp(), new TemplateOverlayEngine());
    }

    NewCommand(PresetService presetService, ProjectGenerator projectGenerator) {
        this(presetService, projectGenerator, new GitInitializer(), new SpringRadTuiApp(), new TemplateOverlayEngine());
    }

    NewCommand(PresetService presetService, ProjectGenerator projectGenerator, GitInitializer gitInitializer) {
        this(presetService, projectGenerator, gitInitializer, new SpringRadTuiApp(), new TemplateOverlayEngine());
    }

    NewCommand(
            PresetService presetService,
            ProjectGenerator projectGenerator,
            GitInitializer gitInitializer,
            SpringRadTuiApp tuiApp,
            TemplateOverlayEngine templateOverlayEngine
    ) {
        this.presetService = presetService;
        this.projectGenerator = projectGenerator;
        this.gitInitializer = gitInitializer;
        this.tuiApp = tuiApp;
        this.templateOverlayEngine = templateOverlayEngine;
    }

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "<name>", description = "Project name")
    String name;

    @Option(
            names = {"--interactive", "-i"},
            description = "Start interactive TUI flow",
            defaultValue = "false",
            arity = "0"
    )
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
        CliArgs cliArgs;
        String resolvedPresetName = presetName;

        if (interactive) {
            SpringRadTuiApp.InteractiveSelection selection = tuiApp.start(name, presetService.listPresetNames());
            if (selection == null) {
                spec.commandLine().getErr().println("Interactive generation cancelled.");
                return 1;
            }
            cliArgs = selection.cliArgs();
            resolvedPresetName = selection.presetName();
        } else {
            if (presetName == null) {
                spec.commandLine().getErr().println(
                        "No preset provided. Use --interactive for guided mode or pass --preset <name>."
                );
                return 1;
            }

            cliArgs = toCliArgs();
        }

        final CliArgs finalCliArgs = cliArgs;
        final String finalPresetName = resolvedPresetName;

        try {
            ProjectConfig config;
            if (interactive) {
                config = tuiApp.runWithProgress("Project Generation", reporter ->
                        executeGenerationFlow(finalPresetName, finalCliArgs, reporter)
                );
            } else {
                config = executeGenerationFlow(
                        finalPresetName,
                        finalCliArgs,
                        (currentStep, totalSteps, message) ->
                                spec.commandLine().getOut().printf("[%d/%d] %s%n", currentStep, totalSteps, message)
                );
            }
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
        } catch (Exception e) {
            spec.commandLine().getErr().println("Generation failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(spec.commandLine().getErr());
            }
            return 1;
        }
    }

    private ProjectConfig executeGenerationFlow(
            String resolvedPresetName,
            CliArgs cliArgs,
            SpringRadTuiApp.ProgressReporter reporter
    ) {
        reporter.step(1, 5, "Resolving preset and configuration");
        ProjectConfig config = presetService.resolve(resolvedPresetName, cliArgs);

        reporter.step(2, 5, "Generating base Spring project from Initializr");
        projectGenerator.generate(config);

        reporter.step(3, 5, "Applying template overlays and scaffolds");
        templateOverlayEngine.overlay(config, false);

        reporter.step(4, 5, "Initializing git repository");
        gitInitializer.initializeRepository(config.outputDirectory(), spec.commandLine().getErr());

        reporter.step(5, 5, "Finalizing output");
        return config;
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

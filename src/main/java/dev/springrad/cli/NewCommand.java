package dev.springrad.cli;

import dev.springrad.core.GitInitializer;
import dev.springrad.core.GlobalConfig;
import dev.springrad.core.InitializrClient;
import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.SpringRadException;
import dev.springrad.core.SpringRadPaths;
import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
import dev.springrad.tui.SpringRadTuiApp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

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

    @Option(names = {"--group", "-g"}, description = "Maven group ID")
    String groupId;

    @Option(names = {"--artifact", "-a"}, description = "Artifact ID (defaults to <name>)")
    String artifactId;

    @Option(names = {"--java-version", "-j"}, description = "Java version")
    String javaVersion;

    @Option(names = "--packaging", description = "Packaging: ${COMPLETION-CANDIDATES}")
    ProjectConfig.Packaging packaging;

    @Option(names = "--build", description = "Build tool: ${COMPLETION-CANDIDATES}")
    ProjectConfig.BuildTool buildTool;

    @Option(names = "--boot-version", description = "Spring Boot version")
    String bootVersion;

    @Option(names = "--auth", description = "Auth style: ${COMPLETION-CANDIDATES}")
    ProjectConfig.AuthStyle authStyle;

    @Option(names = {"--database", "-d"}, description = "Database: ${COMPLETION-CANDIDATES}")
    ProjectConfig.Database database;

    @Option(names = {"--output", "-o"}, description = "Output directory")
    Path outputDirectory;

    @Option(names = "--template-dir", description = "One-off user template directory")
    String templateDir;

    @Option(names = "--no-user-templates", description = "Disable config/preset/CLI user template overlays")
    boolean noUserTemplates;

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
                        new SpringRadTuiApp.ProgressReporter() {
                            @Override
                            public void step(int currentStep, int totalSteps, String message) {
                                spec.commandLine().getOut().printf("[%d/%d] %s%n", currentStep, totalSteps, message);
                            }

                            @Override
                            public void detail(String message) {
                                spec.commandLine().getOut().printf("  - %s%n", message);
                            }
                        }
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
        reporter.detail("Preset selected: " + resolvedPresetName);
        ProjectConfig config = presetService.resolve(resolvedPresetName, cliArgs);
        reporter.detail("Output directory: " + config.outputDirectory());

        reporter.step(2, 5, "Generating base Spring project from Initializr");
        projectGenerator.generate(config);
        reporter.detail("Initializr generation completed");

        reporter.step(3, 5, "Applying template overlays and scaffolds");
        templateOverlayEngine.overlay(config, false, reporter::detail);
        applyUserTemplateLayers(config, resolvedPresetName, cliArgs, reporter);

        reporter.step(4, 5, "Initializing git repository");
        gitInitializer.initializeRepository(config.outputDirectory(), spec.commandLine().getErr());
        reporter.detail("Git initialization complete");

        reporter.step(5, 5, "Finalizing output");
        reporter.detail("Generation flow complete");
        return config;
    }

    private void applyUserTemplateLayers(
            ProjectConfig config,
            String resolvedPresetName,
            CliArgs cliArgs,
            SpringRadTuiApp.ProgressReporter reporter
    ) {
        if (cliArgs.noUserTemplates()) {
            reporter.detail("Skipping user template overlays (--no-user-templates)");
            return;
        }

        GlobalConfig globalConfig = presetService.globalConfig();
        Path configDir = presetService.globalConfigLoader().configDirectory();

        if (globalConfig.templateDir() != null) {
            templateOverlayEngine.overlayDirectory(globalConfig.templateDir(), config, true, reporter::detail);
        }

        Preset preset = presetService.findByName(resolvedPresetName).orElse(null);
        if (preset != null && preset.templateDir() != null && !preset.templateDir().isBlank()) {
            Path presetTemplateDir = SpringRadPaths.resolveUserPath(preset.templateDir(), configDir);
            templateOverlayEngine.overlayDirectory(presetTemplateDir, config, true, reporter::detail);
        }

        if (cliArgs.templateDir() != null && !cliArgs.templateDir().isBlank()) {
            Path oneOffTemplateDir = SpringRadPaths.resolveUserPath(cliArgs.templateDir(), configDir);
            templateOverlayEngine.overlayDirectory(oneOffTemplateDir, config, true, reporter::detail);
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
                outputDirectory,
                templateDir,
                noUserTemplates
        );
    }
}

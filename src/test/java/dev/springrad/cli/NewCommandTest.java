package dev.springrad.cli;

import dev.springrad.core.GenerationResult;
import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.SpringRadException;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
import dev.springrad.tui.SpringRadTuiApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCommandTest {
    @TempDir
    Path tempDir;

    private NewCommand newCommand() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        ProjectGenerator generator = config -> new GenerationResult(config.outputDirectory(), List.of());
        return new NewCommand(new PresetService(repository), generator);
    }

    @Test
    void validFlagsParseAndExecute() {
        Path output = tempDir.resolve("service");
        CommandLine commandLine = new CommandLine(newCommand());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(out, true));
        int exitCode = commandLine.execute(
                "demo-app",
                "--preset", "web-api",
                "--group", "dev.acme",
                "--artifact", "service",
                "--java-version", "21",
                "--packaging", "jar",
                "--build", "gradle",
                "--auth", "jwt",
                "--database", "postgresql",
                "--output", output.toString()
        );

        assertEquals(0, exitCode);
        String stdout = out.toString();
        assertTrue(stdout.contains("[1/5] Resolving preset and configuration"));
        assertTrue(stdout.contains("[5/5] Finalizing output"));
        assertTrue(java.nio.file.Files.exists(output.resolve("src/main/resources/application.yml")));
        assertTrue(java.nio.file.Files.exists(output.resolve("src/main/resources/logback-spring.xml")));
    }

    @Test
    void missingNameReturnsUsageError() {
        CommandLine commandLine = new CommandLine(newCommand());
        int exitCode = commandLine.execute("--preset", "web-api");
        assertEquals(2, exitCode);
    }

    @Test
    void invalidPackagingReturnsUsageError() {
        CommandLine commandLine = new CommandLine(newCommand());
        int exitCode = commandLine.execute("demo-app", "--preset", "web-api", "--packaging", "zip");
        assertEquals(2, exitCode);
    }

    @Test
    void withoutPresetAndNotInteractiveReturnsConfiguredErrorCode() {
        CommandLine commandLine = new CommandLine(newCommand());
        int exitCode = commandLine.execute("demo-app");
        assertEquals(1, exitCode);
    }

    @Test
    void unknownPresetReturnsConfiguredErrorCode() {
        CommandLine commandLine = new CommandLine(newCommand());
        int exitCode = commandLine.execute("demo-app", "--preset", "does-not-exist");
        assertEquals(1, exitCode);
    }

    @Test
    void verbosePrintsStackTraceOnSpringRadException() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        ProjectGenerator generator = config -> {
            throw new SpringRadException("boom", "friendly boom");
        };
        NewCommand command = new NewCommand(new PresetService(repository), generator);
        CommandLine commandLine = new CommandLine(command);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        commandLine.setErr(new java.io.PrintWriter(err, true));

        int exitCode = commandLine.execute("demo-app", "--preset", "web-api", "--verbose");

        String output = err.toString();
        assertEquals(1, exitCode);
        assertTrue(output.contains("friendly boom"));
        assertTrue(output.contains("SpringRadException"));
    }

    @Test
    void interactiveModeUsesSelectionFromTui() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        PresetService presetService = new PresetService(repository);
        AtomicReference<ProjectConfig> captured = new AtomicReference<>();
        ProjectGenerator generator = config -> {
            captured.set(config);
            return new GenerationResult(config.outputDirectory(), List.of());
        };
        SpringRadTuiApp tui = new SpringRadTuiApp() {
            @Override
            public InteractiveSelection start(String initialName, List<String> availablePresets) {
                CliArgs args = new CliArgs(
                        "demo",
                        "dev.acme",
                        "interactive-app",
                        "21",
                        null,
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.session,
                        ProjectConfig.Database.mysql,
                        List.of("web"),
                        List.of(),
                        tempDir.resolve("generated")
                );
                return new InteractiveSelection("web-api", args);
            }

            @Override
            public <T> T runWithProgress(String title, ProgressTask<T> task) throws Exception {
                return task.run((currentStep, totalSteps, message) -> {
                });
            }
        };
        NewCommand command = new NewCommand(
                presetService,
                generator,
                new dev.springrad.core.GitInitializer(),
                tui,
                new TemplateOverlayEngine()
        );
        CommandLine commandLine = new CommandLine(command);

        int exitCode = commandLine.execute("ignored", "--interactive");

        assertEquals(0, exitCode);
        assertEquals("interactive-app", captured.get().artifactId());
        assertEquals(ProjectConfig.AuthStyle.session, captured.get().authStyle());
    }
}

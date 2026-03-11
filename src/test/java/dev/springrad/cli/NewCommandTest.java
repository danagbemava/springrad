package dev.springrad.cli;

import dev.springrad.core.GenerationResult;
import dev.springrad.core.GlobalConfigLoader;
import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.SpringRadException;
import dev.springrad.preset.Preset;
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
        PresetRepository repository = new PresetRepository(tempDir.resolve("config").resolve("presets.json"));
        PresetService presetService = new PresetService(repository, new GlobalConfigLoader(tempDir.resolve("config")));
        ProjectGenerator generator = config -> new GenerationResult(config.outputDirectory(), List.of());
        return new NewCommand(presetService, generator);
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
        PresetRepository repository = new PresetRepository(tempDir.resolve("config").resolve("presets.json"));
        ProjectGenerator generator = config -> {
            throw new SpringRadException("boom", "friendly boom");
        };
        NewCommand command = new NewCommand(new PresetService(repository, new GlobalConfigLoader(tempDir.resolve("config"))), generator);
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
        PresetRepository repository = new PresetRepository(tempDir.resolve("config").resolve("presets.json"));
        PresetService presetService = new PresetService(repository, new GlobalConfigLoader(tempDir.resolve("config")));
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
                        tempDir.resolve("generated"),
                        null,
                        false
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

    @Test
    void oneOffTemplateDirOverridesGlobalAndPresetTemplateDirs() throws Exception {
        Path configDir = tempDir.resolve("config");
        Path globalDir = tempDir.resolve("global-templates");
        Path presetDir = tempDir.resolve("preset-templates");
        Path cliDir = tempDir.resolve("cli-templates");
        Path output = tempDir.resolve("out");

        writeTemplate(globalDir, "global");
        writeTemplate(presetDir, "preset");
        writeTemplate(cliDir, "cli");
        java.nio.file.Files.createDirectories(configDir);
        java.nio.file.Files.writeString(configDir.resolve("config.yml"), "templateDir: " + globalDir.toString() + "\n");

        PresetRepository repository = new PresetRepository(configDir.resolve("presets.json"));
        repository.save(new Preset(
                "custom",
                false,
                "com.example",
                "21",
                null,
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web"),
                List.of(),
                presetDir.toString()
        ));
        PresetService presetService = new PresetService(repository, new GlobalConfigLoader(configDir));

        ProjectGenerator generator = config -> {
            try {
                java.nio.file.Files.createDirectories(config.outputDirectory());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return new GenerationResult(config.outputDirectory(), List.of());
        };
        NewCommand command = new NewCommand(presetService, generator);
        CommandLine commandLine = new CommandLine(command);

        int exitCode = commandLine.execute("demo-app", "--preset", "custom", "--output", output.toString(), "--template-dir", cliDir.toString());

        assertEquals(0, exitCode);
        assertEquals("cli", java.nio.file.Files.readString(output.resolve("src/main/resources/application.yml")).trim());
    }

    @Test
    void noUserTemplatesSkipsAllUserTemplateLayers() throws Exception {
        Path configDir = tempDir.resolve("config");
        Path globalDir = tempDir.resolve("global-templates");
        Path presetDir = tempDir.resolve("preset-templates");
        Path cliDir = tempDir.resolve("cli-templates");
        Path output = tempDir.resolve("out-no-user");

        writeTemplate(globalDir, "global");
        writeTemplate(presetDir, "preset");
        writeTemplate(cliDir, "cli");
        java.nio.file.Files.createDirectories(configDir);
        java.nio.file.Files.writeString(configDir.resolve("config.yml"), "templateDir: " + globalDir.toString() + "\n");

        PresetRepository repository = new PresetRepository(configDir.resolve("presets.json"));
        repository.save(new Preset(
                "custom",
                false,
                "com.example",
                "21",
                null,
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web"),
                List.of(),
                presetDir.toString()
        ));
        PresetService presetService = new PresetService(repository, new GlobalConfigLoader(configDir));
        ProjectGenerator generator = config -> {
            try {
                java.nio.file.Files.createDirectories(config.outputDirectory());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return new GenerationResult(config.outputDirectory(), List.of());
        };

        NewCommand command = new NewCommand(presetService, generator);
        CommandLine commandLine = new CommandLine(command);

        int exitCode = commandLine.execute(
                "demo-app",
                "--preset", "custom",
                "--output", output.toString(),
                "--template-dir", cliDir.toString(),
                "--no-user-templates"
        );

        assertEquals(0, exitCode);
        String content = java.nio.file.Files.readString(output.resolve("src/main/resources/application.yml"));
        assertTrue(!content.trim().equals("global"));
        assertTrue(!content.trim().equals("preset"));
        assertTrue(!content.trim().equals("cli"));
    }

    private void writeTemplate(Path root, String value) throws Exception {
        Path configDir = root.resolve("config");
        java.nio.file.Files.createDirectories(configDir);
        java.nio.file.Files.writeString(configDir.resolve("application.yml"), value + "\n");
    }
}

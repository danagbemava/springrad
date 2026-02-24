package dev.springrad.cli;

import dev.springrad.core.GenerationResult;
import dev.springrad.core.ProjectConfig;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.core.SpringRadException;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

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
        CommandLine commandLine = new CommandLine(newCommand());
        int exitCode = commandLine.execute(
                "demo-app",
                "--preset", "web-api",
                "--group", "dev.acme",
                "--artifact", "service",
                "--java-version", "21",
                "--packaging", "jar",
                "--build", "gradle",
                "--auth", "jwt",
                "--database", "postgresql"
        );

        assertEquals(0, exitCode);
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
}

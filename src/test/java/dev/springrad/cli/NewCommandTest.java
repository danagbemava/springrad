package dev.springrad.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCommandTest {

    @Test
    void validFlagsParseAndExecute() {
        CommandLine commandLine = new CommandLine(new NewCommand());
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
        CommandLine commandLine = new CommandLine(new NewCommand());
        int exitCode = commandLine.execute("--preset", "web-api");
        assertEquals(2, exitCode);
    }

    @Test
    void invalidPackagingReturnsUsageError() {
        CommandLine commandLine = new CommandLine(new NewCommand());
        int exitCode = commandLine.execute("demo-app", "--preset", "web-api", "--packaging", "zip");
        assertEquals(2, exitCode);
    }

    @Test
    void withoutPresetAndNotInteractiveReturnsConfiguredErrorCode() {
        CommandLine commandLine = new CommandLine(new NewCommand());
        int exitCode = commandLine.execute("demo-app");
        assertEquals(1, exitCode);
    }
}

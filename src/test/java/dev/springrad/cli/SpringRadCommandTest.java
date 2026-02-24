package dev.springrad.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringRadCommandTest {

    @Test
    void helpReturnsZero() {
        int exitCode = new CommandLine(new SpringRadCommand()).execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void versionReturnsZeroAndPrintsVersion() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new SpringRadCommand());
        commandLine.setOut(new PrintWriter(out, true));

        int exitCode = commandLine.execute("--version");

        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("springrad version:"));
    }

    @Test
    void noArgsReturnsZero() {
        int exitCode = new CommandLine(new SpringRadCommand()).execute();
        assertEquals(0, exitCode);
    }
}

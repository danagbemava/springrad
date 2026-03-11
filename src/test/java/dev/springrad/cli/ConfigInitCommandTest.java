package dev.springrad.cli;

import dev.springrad.core.GlobalConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigInitCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void createsConfigFileAndPrintsPath() throws Exception {
        GlobalConfigLoader loader = new GlobalConfigLoader(tempDir);
        ConfigInitCommand command = new ConfigInitCommand(loader);
        CommandLine commandLine = new CommandLine(command);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        commandLine.setOut(new PrintWriter(out, true));

        int exitCode = commandLine.execute();

        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertTrue(out.toString().contains("Config file created at"));
    }

    @Test
    void declinesOverwriteWhenUserAnswersNo() throws Exception {
        Path config = tempDir.resolve("config.yml");
        Files.writeString(config, "defaults:\n  groupId: com.keep\n");

        InputStream originalIn = System.in;
        try {
            System.setIn(new ByteArrayInputStream("n\n".getBytes()));
            GlobalConfigLoader loader = new GlobalConfigLoader(tempDir);
            ConfigInitCommand command = new ConfigInitCommand(loader);
            CommandLine commandLine = new CommandLine(command);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            commandLine.setOut(new PrintWriter(out, true));

            int exitCode = commandLine.execute();

            assertEquals(0, exitCode);
            assertTrue(Files.readString(config).contains("com.keep"));
            assertTrue(out.toString().contains("Canceled. Existing config preserved."));
        } finally {
            System.setIn(originalIn);
        }
    }
}

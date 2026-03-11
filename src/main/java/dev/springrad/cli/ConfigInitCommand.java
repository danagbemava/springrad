package dev.springrad.cli;

import dev.springrad.core.GlobalConfigLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "init", description = "Initialize ~/.springrad/config.yml")
public final class ConfigInitCommand implements Runnable {
    private static final String TEMPLATE = """
            # Global defaults applied to all generated projects (lowest priority).
            # CLI flags > preset values > config.yml defaults > built-in defaults.

            defaults:
              # groupId: com.mycompany
              # javaVersion: \"21\"
              # bootVersion: \"3.3.0\"
              # packaging: jar
              # buildTool: gradle
              # author: \"Your Name\"

            # Optional template directory applied to every generation.
            # templateDir: ~/work/springrad-templates

            # Optional custom dependency aliases.
            # aliases:
            #   monitoring: [\"actuator\", \"prometheus\"]
            #   my-stack: [\"web\", \"security\", \"data-jpa\", \"postgresql\", \"flyway\", \"actuator\"]
            """;

    private final GlobalConfigLoader configLoader;

    @Spec
    private CommandSpec spec;

    public ConfigInitCommand() {
        this(new GlobalConfigLoader());
    }

    ConfigInitCommand(GlobalConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Override
    public void run() {
        Path configPath = configLoader.configPath();
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                if (!confirmOverwrite()) {
                    spec.commandLine().getOut().println("Canceled. Existing config preserved.");
                    return;
                }
            }

            Files.writeString(configPath, TEMPLATE, StandardCharsets.UTF_8);
            spec.commandLine().getOut().printf("Config file created at %s%n", configPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize config at " + configPath, e);
        }
    }

    private boolean confirmOverwrite() throws IOException {
        spec.commandLine().getOut().printf("Config file already exists at %s. Overwrite? [y/N]: ", configLoader.configPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String response = reader.readLine();
        if (response == null) {
            return false;
        }
        String normalized = response.trim().toLowerCase();
        return normalized.equals("y") || normalized.equals("yes");
    }
}

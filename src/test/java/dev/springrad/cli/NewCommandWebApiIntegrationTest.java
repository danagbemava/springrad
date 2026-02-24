package dev.springrad.cli;

import dev.springrad.core.GenerationResult;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCommandWebApiIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void webApiPresetGeneratesExpectedScaffoldsAndTemplates() throws Exception {
        Path output = tempDir.resolve("demo-app");
        NewCommand command = commandWithGenerator(config -> {
            try {
                Files.createDirectories(config.outputDirectory());
                Files.createDirectories(config.outputDirectory().resolve("src/main/resources"));
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
            return new GenerationResult(config.outputDirectory(), List.of());
        });

        int exitCode = new CommandLine(command).execute(
                "demo-app",
                "--preset", "web-api",
                "--output", output.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(output.resolve("config/application.yml")));
        assertTrue(Files.exists(output.resolve("config/application-dev.yml")));
        assertTrue(Files.exists(output.resolve("config/application-prod.yml")));
        assertTrue(Files.exists(output.resolve("config/.env.example")));
        assertTrue(Files.exists(output.resolve("src/main/resources/logback-spring.xml")));
        assertTrue(Files.exists(output.resolve("src/main/resources/db/migration/V1__init.sql")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/demo/app/security/SecurityConfigJwt.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/demo/app/auth/AuthController.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/demo/app/api/GlobalExceptionHandler.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/demo/app/ratelimit/RateLimitingFilter.java")));

        // Edge case: web-api preset must not emit kafka-only templates.
        assertFalse(Files.exists(output.resolve("src/main/java/com/example/demo/app/messaging/BaseConsumer.java")));
        assertFalse(Files.exists(output.resolve("src/main/java/com/example/demo/app/messaging/KafkaTopicConfig.java")));
    }

    @Test
    void overlayDoesNotOverwriteExistingLogbackFile() throws Exception {
        Path output = tempDir.resolve("demo-app");
        NewCommand command = commandWithGenerator(config -> {
            try {
                Path resources = config.outputDirectory().resolve("src/main/resources");
                Files.createDirectories(resources);
                Files.writeString(resources.resolve("logback-spring.xml"), "<configuration><root level=\"DEBUG\"/></configuration>");
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
            return new GenerationResult(config.outputDirectory(), List.of());
        });

        int exitCode = new CommandLine(command).execute(
                "demo-app",
                "--preset", "web-api",
                "--output", output.toString()
        );

        assertEquals(0, exitCode);
        assertEquals(
                "<configuration><root level=\"DEBUG\"/></configuration>",
                Files.readString(output.resolve("src/main/resources/logback-spring.xml"))
        );
    }

    private NewCommand commandWithGenerator(ProjectGenerator generator) {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        PresetService presetService = new PresetService(repository);
        return new NewCommand(
                presetService,
                generator,
                new dev.springrad.core.GitInitializer(),
                new dev.springrad.tui.SpringRadTuiApp(),
                new TemplateOverlayEngine()
        );
    }
}

package dev.springrad.cli;

import dev.springrad.core.GenerationResult;
import dev.springrad.core.ProjectGenerator;
import dev.springrad.preset.PresetRepository;
import dev.springrad.preset.PresetService;
import dev.springrad.scaffold.TemplateOverlayEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewCommandEventDrivenIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void eventDrivenPresetGeneratesKafkaScaffoldsAndTemplates() throws Exception {
        Path output = tempDir.resolve("events-app");
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
                "events-app",
                "--preset", "event-driven",
                "--output", output.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(output.resolve("config/application.yml")));
        assertTrue(Files.exists(output.resolve("config/application-dev.yml")));
        assertTrue(Files.exists(output.resolve("config/application-prod.yml")));
        assertTrue(Files.exists(output.resolve("config/.env.example")));
        assertTrue(Files.exists(output.resolve("docker/docker-compose.kafka.yml")));
        assertTrue(Files.exists(output.resolve("src/main/resources/logback-spring.xml")));
        assertTrue(Files.exists(output.resolve("src/main/resources/db/migration/V1__init.sql")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/events/app/messaging/BaseProducer.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/events/app/messaging/BaseConsumer.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/events/app/messaging/KafkaTopicConfig.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/events/app/messaging/DeadLetterQueueConfig.java")));

        // Edge case: event-driven preset does not scaffold auth controller by default.
        assertFalse(Files.exists(output.resolve("src/main/java/com/example/events/app/auth/AuthController.java")));
    }

    @Test
    void overlayDoesNotOverwriteExistingKafkaTopicConfig() throws Exception {
        Path output = tempDir.resolve("events-app");
        NewCommand command = commandWithGenerator(config -> {
            try {
                Path messaging = config.outputDirectory().resolve("src/main/java/com/example/events/app/messaging");
                Files.createDirectories(messaging);
                Files.writeString(messaging.resolve("KafkaTopicConfig.java"), "// keep-existing");
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(e);
            }
            return new GenerationResult(config.outputDirectory(), List.of());
        });

        int exitCode = new CommandLine(command).execute(
                "events-app",
                "--preset", "event-driven",
                "--output", output.toString()
        );

        assertEquals(0, exitCode);
        assertEquals("// keep-existing", Files.readString(output.resolve("src/main/java/com/example/events/app/messaging/KafkaTopicConfig.java")));
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

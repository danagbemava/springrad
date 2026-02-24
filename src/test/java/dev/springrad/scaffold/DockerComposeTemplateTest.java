package dev.springrad.scaffold;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerComposeTemplateTest {
    @TempDir
    Path tempDir;

    @Test
    void webApiSnapshotRendersComposeWithPostgresService() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/docker/docker-compose.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(template, config(Path.of("./tmp"), ProjectConfig.Database.postgresql, List.of("web")));

        String expected = """
                name: demo-app
                                
                services:
                  app:
                    build:
                      context: .
                      dockerfile: Dockerfile
                    container_name: demo-app-app
                    env_file:
                      - .env
                    ports:
                      - "8080:8080"
                    depends_on:
                      db:
                        condition: service_healthy
                                
                db:
                  image: postgres:16-alpine
                  container_name: demo-app-db
                  ports:
                    - "5432:5432"
                  environment:
                    POSTGRES_DB: demo-app_dev
                    POSTGRES_USER: postgres
                    POSTGRES_PASSWORD: postgres
                  healthcheck:
                    test: ["CMD-SHELL", "pg_isready -U postgres"]
                    interval: 10s
                    timeout: 5s
                    retries: 5
                  volumes:
                    - demo-app-db-data:/var/lib/postgresql/data
                                
                volumes:
                  demo-app-db-data:
                                
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void eventDrivenGeneratesKafkaOverrideComposeFile() throws Exception {
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        Path output = tempDir.resolve("generated");
        engine.overlay(config(output, ProjectConfig.Database.postgresql, List.of("web", "kafka")), false);

        Path kafkaFile = output.resolve("docker/docker-compose.kafka.yml");
        assertTrue(Files.exists(kafkaFile));
        String content = Files.readString(kafkaFile);
        assertTrue(content.contains("bitnami/kafka:latest"));
    }

    @Test
    void webApiDoesNotGenerateKafkaOverrideComposeFile() {
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        Path output = tempDir.resolve("generated");
        engine.overlay(config(output, ProjectConfig.Database.postgresql, List.of("web")), false);

        Path kafkaFile = output.resolve("docker/docker-compose.kafka.yml");
        assertFalse(Files.exists(kafkaFile));
    }

    @Test
    void renderedComposeYamlIsParseable() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/docker/docker-compose.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(template, config(Path.of("./tmp"), ProjectConfig.Database.postgresql, List.of("web")));

        assertDoesNotThrow(() -> new Yaml().load(rendered));
    }

    private static ProjectConfig config(Path output, ProjectConfig.Database database, List<String> dependencies) {
        return ProjectConfig.merge(
                Preset.named("test"),
                new CliArgs(
                        "demo",
                        "dev.example",
                        "demo-app",
                        "21",
                        "3.4.1",
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.jwt,
                        database,
                        dependencies,
                        List.of(),
                        output
                )
        );
    }
}

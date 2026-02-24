package dev.springrad.scaffold;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvExampleTemplateTest {

    @Test
    void webApiPresetRendersWithoutKafkaSection() throws Exception {
        String rendered = render(ProjectConfig.AuthStyle.jwt, List.of("web", "security"));
        String expected = """
                # Database
                DB_URL=jdbc:postgresql://localhost:5432/your_db
                DB_USERNAME=your_user
                DB_PASSWORD=your_password
                
                # Security (JWT)
                # Must be at least 256 bits (32 characters)
                JWT_SECRET=change-me-to-a-secure-random-string-at-least-256-bits
                
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void eventDrivenPresetIncludesKafkaSection() throws Exception {
        String rendered = render(ProjectConfig.AuthStyle.jwt, List.of("web", "security", "kafka"));
        String expected = """
                # Database
                DB_URL=jdbc:postgresql://localhost:5432/your_db
                DB_USERNAME=your_user
                DB_PASSWORD=your_password
                
                # Security (JWT)
                # Must be at least 256 bits (32 characters)
                JWT_SECRET=change-me-to-a-secure-random-string-at-least-256-bits
                
                # Kafka (event-driven only)
                KAFKA_BOOTSTRAP_SERVERS=localhost:9092
                
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void noneAuthStyleRendersWithoutJwtSecret() throws Exception {
        String rendered = render(ProjectConfig.AuthStyle.none, List.of("web", "security"));
        String expected = """
                # Database
                DB_URL=jdbc:postgresql://localhost:5432/your_db
                DB_USERNAME=your_user
                DB_PASSWORD=your_password
                
                """;
        assertEquals(expected, rendered);
    }

    private static String render(ProjectConfig.AuthStyle authStyle, List<String> dependencies) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/.env.example"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        ProjectConfig config = ProjectConfig.merge(
                Preset.named("test"),
                new CliArgs(
                        "demo",
                        "dev.example",
                        "demo-app",
                        "21",
                        "3.4.1",
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        authStyle,
                        ProjectConfig.Database.postgresql,
                        dependencies,
                        List.of(),
                        Path.of("./tmp")
                )
        );
        return engine.render(template, config);
    }
}

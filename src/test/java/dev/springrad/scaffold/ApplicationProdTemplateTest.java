package dev.springrad.scaffold;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationProdTemplateTest {

    @Test
    void webApiSnapshotUsesEnvironmentVariables() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-prod.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                config(ProjectConfig.AuthStyle.jwt, List.of("web", "security"))
        );

        String expected = """
                spring:
                  config:
                    activate:
                      on-profile: prod
                  datasource:
                    url: ${DB_URL}
                    username: ${DB_USERNAME}
                    password: ${DB_PASSWORD}
                  jpa:
                    show-sql: false
                                
                                
                server:
                  error:
                    include-stacktrace: never
                    include-message: never
                                
                logging:
                  level:
                    root: WARN
                    dev.example.demo.app: INFO
                                
                app:
                  security:
                    jwt:
                      secret: ${JWT_SECRET}
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void eventDrivenIncludesKafkaEnvironmentVariable() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-prod.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                config(ProjectConfig.AuthStyle.jwt, List.of("web", "kafka", "security"))
        );

        assertTrue(rendered.contains("kafka:"));
        assertTrue(rendered.contains("bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}"));
    }

    @Test
    void containsNoHardcodedCredentialsOrSecrets() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-prod.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                config(ProjectConfig.AuthStyle.jwt, List.of("web", "security"))
        );

        assertFalse(rendered.contains("username: postgres"));
        assertFalse(rendered.contains("username: root"));
        assertFalse(rendered.contains("password: postgres"));
        assertFalse(rendered.contains("password: root"));
        assertTrue(rendered.contains("${DB_PASSWORD}"));
        assertTrue(rendered.contains("${JWT_SECRET}"));
    }

    @Test
    void prodTemplateRendersValidYamlForDatasourceAndJwtConfig() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-prod.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                config(ProjectConfig.AuthStyle.jwt, List.of("web", "security", "kafka"))
        );

        assertDoesNotThrow(() -> new Yaml().load(rendered));
    }

    private static ProjectConfig config(ProjectConfig.AuthStyle authStyle, List<String> dependencies) {
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
                        authStyle,
                        ProjectConfig.Database.postgresql,
                        dependencies,
                        List.of(),
                        Path.of("./tmp")
                )
        );
    }
}

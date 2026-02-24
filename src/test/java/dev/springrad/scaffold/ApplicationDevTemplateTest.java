package dev.springrad.scaffold;

import dev.springrad.core.ProjectConfig;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationDevTemplateTest {

    @Test
    void postgresSnapshotMatchesExpected() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-dev.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.postgresql, List.of("web"))
        );

        String expected = """
                spring:
                  config:
                    activate:
                      on-profile: dev
                  datasource:
                    url: jdbc:postgresql://localhost:5432/demo-app_dev
                    username: postgres
                    password: postgres
                    driver-class-name: org.postgresql.Driver
                  jpa:
                    show-sql: true
                    properties:
                      hibernate:
                        format_sql: true
                                
                                
                logging:
                  level:
                    root: INFO
                    dev.example.demo.app: DEBUG
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void mysqlSnapshotMatchesExpected() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-dev.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.mysql, List.of("web"))
        );

        String expected = """
                spring:
                  config:
                    activate:
                      on-profile: dev
                  datasource:
                    url: jdbc:mysql://localhost:3306/demo-app_dev
                    username: root
                    password: root
                    driver-class-name: com.mysql.cj.jdbc.Driver
                  jpa:
                    show-sql: true
                    properties:
                      hibernate:
                        format_sql: true
                                
                                
                logging:
                  level:
                    root: INFO
                    dev.example.demo.app: DEBUG
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void eventDrivenPresetIncludesKafkaConfig() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-dev.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.postgresql, List.of("web", "kafka"))
        );

        assertTrue(rendered.contains("kafka:"));
        assertTrue(rendered.contains("bootstrap-servers: localhost:9092"));
    }

    @Test
    void devTemplateRendersValidYamlForDatabaseConfig() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/config/application-dev.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.postgresql, List.of("web", "kafka"))
        );

        assertDoesNotThrow(() -> new Yaml().load(rendered));
    }
}

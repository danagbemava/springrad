package dev.springrad.scaffold;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationTemplateTest {

    @Test
    void applicationBaseTemplateSnapshotMatchesExpected() throws Exception {
        String template = Path.of("src/main/resources/templates/config/application.yml").toFile().exists()
                ? java.nio.file.Files.readString(Path.of("src/main/resources/templates/config/application.yml"))
                : "";
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(template, sampleConfig(ProjectConfig.Database.postgresql, List.of("web")));

        String expected = """
                spring:
                  application:
                    name: demo-app
                  profiles:
                    active: dev
                  jpa:
                    hibernate:
                      ddl-auto: validate
                    show-sql: false
                    open-in-view: false
                  jackson:
                    default-property-inclusion: non_null
                    serialization:
                      write-dates-as-timestamps: false
                                
                server:
                  port: 8080
                                
                management:
                  endpoints:
                    web:
                      exposure:
                        include: health,info,metrics,prometheus
                  endpoint:
                    health:
                      show-details: when-authorized
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void applicationBaseTemplateRendersValidYaml() throws Exception {
        String template = java.nio.file.Files.readString(Path.of("src/main/resources/templates/config/application.yml"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(template, sampleConfig(ProjectConfig.Database.postgresql, List.of("web")));

        assertDoesNotThrow(() -> new Yaml().load(rendered));
    }

    static ProjectConfig sampleConfig(ProjectConfig.Database database, List<String> dependencies) {
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
                        Path.of("./tmp")
                )
        );
    }
}

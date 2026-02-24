package dev.springrad.scaffold;

import dev.springrad.core.ProjectConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerTemplateTest {

    @Test
    void dockerfileSnapshotForJava21MatchesExpected() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/docker/Dockerfile"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.postgresql, List.of("web"))
        );

        String expected = """
                FROM eclipse-temurin:21-jdk AS build
                WORKDIR /workspace
                COPY . .
                RUN ./gradlew bootJar --no-daemon
                                
                FROM eclipse-temurin:21-jre-alpine AS runtime
                RUN addgroup -S appgroup && adduser -S appuser -G appgroup
                WORKDIR /app
                ENV SPRING_PROFILES_ACTIVE=prod
                COPY --from=build /workspace/build/libs/*.jar /app/app.jar
                EXPOSE 8080
                USER appuser
                ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
                """;
        assertEquals(expected, rendered);
    }

    @Test
    void dockerfileContainsRequiredStructureAndNoUnresolvedPlaceholders() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/docker/Dockerfile"));
        TemplateOverlayEngine engine = new TemplateOverlayEngine(Path.of("src/main/resources/templates"));
        String rendered = engine.render(
                template,
                ApplicationTemplateTest.sampleConfig(ProjectConfig.Database.postgresql, List.of("web"))
        );

        assertTrue(rendered.contains("FROM eclipse-temurin:21-jdk AS build"));
        assertTrue(rendered.contains("FROM eclipse-temurin:21-jre-alpine AS runtime"));
        assertTrue(rendered.contains("USER appuser"));
        assertTrue(rendered.contains("EXPOSE 8080"));
        assertTrue(rendered.contains("SPRING_PROFILES_ACTIVE=prod"));
        assertTrue(rendered.contains("-XX:+UseContainerSupport"));
        assertTrue(rendered.contains("-XX:MaxRAMPercentage=75"));
        assertFalse(rendered.contains("{{"));
    }
}

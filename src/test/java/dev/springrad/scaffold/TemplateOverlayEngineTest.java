package dev.springrad.scaffold;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateOverlayEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void replacesArtifactPlaceholder() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("config"));
        Files.writeString(templates.resolve("config/app.txt"), "name={{artifactId}}");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output), false);

        assertEquals("name=demo-app", Files.readString(output.resolve("config/app.txt")));
    }

    @Test
    void writesApplicationYamlFilesIntoResourcesDirectory() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("config"));
        Files.writeString(templates.resolve("config/application.yml"), "spring: {}");
        Files.writeString(templates.resolve("config/application-dev.yml"), "spring: {}");
        Files.writeString(templates.resolve("config/application-prod.yml"), "spring: {}");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output), false);

        assertTrue(Files.exists(output.resolve("src/main/resources/application.yml")));
        assertTrue(Files.exists(output.resolve("src/main/resources/application-dev.yml")));
        assertTrue(Files.exists(output.resolve("src/main/resources/application-prod.yml")));
        assertFalse(Files.exists(output.resolve("config/application.yml")));
    }

    @Test
    void doesNotOverwriteExistingFileWithoutForce() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("config"));
        Files.writeString(templates.resolve("config/app.txt"), "name={{artifactId}}");
        Path output = tempDir.resolve("out");
        Files.createDirectories(output.resolve("config"));
        Files.writeString(output.resolve("config/app.txt"), "existing");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        engine.overlay(config(output), false);

        assertEquals("existing", Files.readString(output.resolve("config/app.txt")));
    }

    @Test
    void mirrorsSubdirectoryStructure() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("a/b/c"));
        Files.writeString(templates.resolve("a/b/c/file.txt"), "ok");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output), false);

        assertTrue(Files.exists(output.resolve("a/b/c/file.txt")));
    }

    @Test
    void copiesBinaryFileWithoutModification() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("bin"));
        byte[] binary = new byte[]{0, 1, 2, 3, 4, 5, -1};
        Files.write(templates.resolve("bin/logo.bin"), binary);

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output), false);

        byte[] copied = Files.readAllBytes(output.resolve("bin/logo.bin"));
        assertArrayEquals(binary, copied);
    }

    @Test
    void rendersPackagePathPlaceholderInTargetPath() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/SecurityConfigJwt.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("SecurityConfigJwt")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/SecurityConfigJwt.java")));
    }

    @Test
    void skipsJwtSecurityTemplateWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/SecurityConfigJwt.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/SecurityConfigJwt.java")));
    }

    @Test
    void skipsJwtSecurityTemplateWhenAuthStyleIsNotJwt() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/SecurityConfigJwt.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.session, List.of("SecurityConfigJwt")), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/SecurityConfigJwt.java")));
    }

    @Test
    void rendersSessionSecurityTemplateWhenSelectedAndSessionAuth() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/SecurityConfigSession.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.session, List.of("SecurityConfigSession")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/SecurityConfigSession.java")));
    }

    @Test
    void skipsSessionSecurityTemplateWhenAuthStyleIsJwt() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/SecurityConfigSession.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("SecurityConfigSession")), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/SecurityConfigSession.java")));
    }

    @Test
    void rendersAuthControllerWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/auth"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/auth/AuthController.java"),
                "package {{packageName}}.auth;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("AuthController")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/auth/AuthController.java")));
    }

    @Test
    void skipsAuthControllerWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/auth"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/auth/AuthController.java"),
                "package {{packageName}}.auth;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/auth/AuthController.java")));
    }

    @Test
    void rendersJwtTokenProviderWhenSelectedAndJwtAuth() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/JwtTokenProvider.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("JwtTokenProvider")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/security/JwtTokenProvider.java")));
    }

    @Test
    void skipsJwtTokenProviderWhenSessionAuthOrScaffoldMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/security"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/security/JwtTokenProvider.java"),
                "package {{packageName}}.security;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputSession = tempDir.resolve("out-session");
        engine.overlay(config(outputSession, ProjectConfig.AuthStyle.session, List.of("JwtTokenProvider")), false);
        assertFalse(Files.exists(outputSession.resolve("src/main/java/dev/example/demo/app/security/JwtTokenProvider.java")));

        Path outputNoScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputNoScaffold, ProjectConfig.AuthStyle.jwt, List.of()), false);
        assertFalse(Files.exists(outputNoScaffold.resolve("src/main/java/dev/example/demo/app/security/JwtTokenProvider.java")));
    }

    @Test
    void rendersApiResponseWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/api"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/api/ApiResponse.java"),
                "package {{packageName}}.api;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("ApiResponse")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/api/ApiResponse.java")));
    }

    @Test
    void skipsApiResponseWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/api"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/api/ApiResponse.java"),
                "package {{packageName}}.api;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/api/ApiResponse.java")));
    }

    @Test
    void rendersGlobalExceptionHandlerWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/api"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/api/GlobalExceptionHandler.java"),
                "package {{packageName}}.api;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("GlobalExceptionHandler")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/api/GlobalExceptionHandler.java")));
    }

    @Test
    void skipsGlobalExceptionHandlerWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/api"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/api/GlobalExceptionHandler.java"),
                "package {{packageName}}.api;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/api/GlobalExceptionHandler.java")));
    }

    @Test
    void rendersOpenApiConfigWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/config"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/config/OpenApiConfig.java"),
                "package {{packageName}}.config;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("OpenApiConfig")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/config/OpenApiConfig.java")));
    }

    @Test
    void skipsOpenApiConfigWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/config"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/config/OpenApiConfig.java"),
                "package {{packageName}}.config;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/config/OpenApiConfig.java")));
    }

    @Test
    void rendersAuditableEntityWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/domain"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/domain/AuditableEntity.java"),
                "package {{packageName}}.domain;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("AuditableEntity")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/domain/AuditableEntity.java")));
    }

    @Test
    void skipsAuditableEntityWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/domain"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/domain/AuditableEntity.java"),
                "package {{packageName}}.domain;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of()), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/domain/AuditableEntity.java")));
    }

    @Test
    void rendersFlywayMigrationWhenScaffoldSelectedAndDependencyPresent() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/resources/db/migration"));
        Files.writeString(
                templates.resolve("src/main/resources/db/migration/V1__init.sql"),
                "CREATE TABLE demo (id BIGINT PRIMARY KEY);"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(
                config(output, ProjectConfig.AuthStyle.jwt, List.of("FlywayInitMigration"), List.of("web", "flyway")),
                false
        );

        assertTrue(Files.exists(output.resolve("src/main/resources/db/migration/V1__init.sql")));
    }

    @Test
    void skipsFlywayMigrationWhenScaffoldMissingOrDependencyMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/resources/db/migration"));
        Files.writeString(
                templates.resolve("src/main/resources/db/migration/V1__init.sql"),
                "CREATE TABLE demo (id BIGINT PRIMARY KEY);"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputMissingScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputMissingScaffold, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web", "flyway")), false);
        assertFalse(Files.exists(outputMissingScaffold.resolve("src/main/resources/db/migration/V1__init.sql")));

        Path outputMissingDependency = tempDir.resolve("out-no-flyway");
        engine.overlay(
                config(outputMissingDependency, ProjectConfig.AuthStyle.jwt, List.of("FlywayInitMigration"), List.of("web")),
                false
        );
        assertFalse(Files.exists(outputMissingDependency.resolve("src/main/resources/db/migration/V1__init.sql")));
    }

    @Test
    void rendersKafkaTopicConfigWhenScaffoldSelectedAndKafkaDependencyPresent() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/KafkaTopicConfig.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(
                config(output, ProjectConfig.AuthStyle.jwt, List.of("KafkaTopicConfig"), List.of("web", "kafka")),
                false
        );

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/messaging/KafkaTopicConfig.java")));
    }

    @Test
    void skipsKafkaTopicConfigWhenScaffoldMissingOrKafkaDependencyMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/KafkaTopicConfig.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputMissingScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputMissingScaffold, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web", "kafka")), false);
        assertFalse(Files.exists(outputMissingScaffold.resolve("src/main/java/dev/example/demo/app/messaging/KafkaTopicConfig.java")));

        Path outputMissingDependency = tempDir.resolve("out-no-kafka");
        engine.overlay(
                config(outputMissingDependency, ProjectConfig.AuthStyle.jwt, List.of("KafkaTopicConfig"), List.of("web")),
                false
        );
        assertFalse(Files.exists(outputMissingDependency.resolve("src/main/java/dev/example/demo/app/messaging/KafkaTopicConfig.java")));
    }

    @Test
    void rendersDeadLetterQueueConfigWhenScaffoldSelectedAndKafkaDependencyPresent() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/DeadLetterQueueConfig.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(
                config(output, ProjectConfig.AuthStyle.jwt, List.of("DeadLetterQueueConfig"), List.of("web", "kafka")),
                false
        );

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/messaging/DeadLetterQueueConfig.java")));
    }

    @Test
    void skipsDeadLetterQueueConfigWhenScaffoldMissingOrKafkaDependencyMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/DeadLetterQueueConfig.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputMissingScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputMissingScaffold, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web", "kafka")), false);
        assertFalse(Files.exists(outputMissingScaffold.resolve("src/main/java/dev/example/demo/app/messaging/DeadLetterQueueConfig.java")));

        Path outputMissingDependency = tempDir.resolve("out-no-kafka");
        engine.overlay(
                config(outputMissingDependency, ProjectConfig.AuthStyle.jwt, List.of("DeadLetterQueueConfig"), List.of("web")),
                false
        );
        assertFalse(Files.exists(outputMissingDependency.resolve("src/main/java/dev/example/demo/app/messaging/DeadLetterQueueConfig.java")));
    }

    @Test
    void rendersBaseProducerWhenScaffoldSelectedAndKafkaDependencyPresent() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/BaseProducer.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(
                config(output, ProjectConfig.AuthStyle.jwt, List.of("BaseProducer"), List.of("web", "kafka")),
                false
        );

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/messaging/BaseProducer.java")));
    }

    @Test
    void skipsBaseProducerWhenScaffoldMissingOrKafkaDependencyMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/BaseProducer.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputMissingScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputMissingScaffold, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web", "kafka")), false);
        assertFalse(Files.exists(outputMissingScaffold.resolve("src/main/java/dev/example/demo/app/messaging/BaseProducer.java")));

        Path outputMissingDependency = tempDir.resolve("out-no-kafka");
        engine.overlay(
                config(outputMissingDependency, ProjectConfig.AuthStyle.jwt, List.of("BaseProducer"), List.of("web")),
                false
        );
        assertFalse(Files.exists(outputMissingDependency.resolve("src/main/java/dev/example/demo/app/messaging/BaseProducer.java")));
    }

    @Test
    void rendersBaseConsumerWhenScaffoldSelectedAndKafkaDependencyPresent() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/BaseConsumer.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(
                config(output, ProjectConfig.AuthStyle.jwt, List.of("BaseConsumer"), List.of("web", "kafka")),
                false
        );

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/messaging/BaseConsumer.java")));
    }

    @Test
    void skipsBaseConsumerWhenScaffoldMissingOrKafkaDependencyMissing() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/messaging"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/messaging/BaseConsumer.java"),
                "package {{packageName}}.messaging;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);

        Path outputMissingScaffold = tempDir.resolve("out-no-scaffold");
        engine.overlay(config(outputMissingScaffold, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web", "kafka")), false);
        assertFalse(Files.exists(outputMissingScaffold.resolve("src/main/java/dev/example/demo/app/messaging/BaseConsumer.java")));

        Path outputMissingDependency = tempDir.resolve("out-no-kafka");
        engine.overlay(
                config(outputMissingDependency, ProjectConfig.AuthStyle.jwt, List.of("BaseConsumer"), List.of("web")),
                false
        );
        assertFalse(Files.exists(outputMissingDependency.resolve("src/main/java/dev/example/demo/app/messaging/BaseConsumer.java")));
    }

    @Test
    void rendersRateLimitTemplatesWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/ratelimit"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/ratelimit/RateLimitConfig.java"),
                "package {{packageName}}.ratelimit;"
        );
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/ratelimit/RateLimitingFilter.java"),
                "package {{packageName}}.ratelimit;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("RateLimitingFilter"), List.of("web")), false);

        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/ratelimit/RateLimitConfig.java")));
        assertTrue(Files.exists(output.resolve("src/main/java/dev/example/demo/app/ratelimit/RateLimitingFilter.java")));
    }

    @Test
    void skipsRateLimitTemplatesWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/java/{{packagePath}}/ratelimit"));
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/ratelimit/RateLimitConfig.java"),
                "package {{packageName}}.ratelimit;"
        );
        Files.writeString(
                templates.resolve("src/main/java/{{packagePath}}/ratelimit/RateLimitingFilter.java"),
                "package {{packageName}}.ratelimit;"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web")), false);

        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/ratelimit/RateLimitConfig.java")));
        assertFalse(Files.exists(output.resolve("src/main/java/dev/example/demo/app/ratelimit/RateLimitingFilter.java")));
    }

    @Test
    void rendersLogbackJsonConfigWhenScaffoldSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/resources"));
        Files.writeString(
                templates.resolve("src/main/resources/logback-spring.xml"),
                "<configuration><property name=\"app\" value=\"{{artifactId}}\"/></configuration>"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of("LogbackJsonConfig"), List.of("web")), false);

        Path target = output.resolve("src/main/resources/logback-spring.xml");
        assertTrue(Files.exists(target));
        assertTrue(Files.readString(target).contains("demo-app"));
    }

    @Test
    void skipsLogbackJsonConfigWhenScaffoldNotSelected() throws Exception {
        Path templates = tempDir.resolve("templates");
        Files.createDirectories(templates.resolve("src/main/resources"));
        Files.writeString(
                templates.resolve("src/main/resources/logback-spring.xml"),
                "<configuration/>"
        );

        TemplateOverlayEngine engine = new TemplateOverlayEngine(templates);
        Path output = tempDir.resolve("out");
        engine.overlay(config(output, ProjectConfig.AuthStyle.jwt, List.of(), List.of("web")), false);

        assertFalse(Files.exists(output.resolve("src/main/resources/logback-spring.xml")));
    }

    @Test
    void overlayDirectory_skipsDeniedFiles() throws Exception {
        Path userDir = tempDir.resolve("user-project");
        // allowed — source, config, docker, misc files
        Files.createDirectories(userDir.resolve("src/main/java/com/example"));
        Files.writeString(userDir.resolve("src/main/java/com/example/App.java"), "user code");
        Files.createDirectories(userDir.resolve("src/main/kotlin/com/example"));
        Files.writeString(userDir.resolve("src/main/kotlin/com/example/App.kt"), "user kotlin");
        Files.writeString(userDir.resolve("Dockerfile"), "FROM openjdk:21");
        Files.writeString(userDir.resolve(".env"), "KEY=val");
        Files.writeString(userDir.resolve("docker-compose.yml"), "services:");
        Files.createDirectories(userDir.resolve("config"));
        Files.writeString(userDir.resolve("config/application.yml"), "spring: {}");
        Files.writeString(userDir.resolve("README.md"), "# readme");
        Files.createDirectories(userDir.resolve("data"));
        Files.writeString(userDir.resolve("data/seed.sql"), "INSERT INTO t VALUES(1)");
        // denied — build output, IDE, VCS, build scripts
        Files.writeString(userDir.resolve("build.gradle"), "plugins {}");
        Files.writeString(userDir.resolve("build.gradle.kts"), "plugins {}");
        Files.writeString(userDir.resolve("pom.xml"), "<project/>");
        Files.writeString(userDir.resolve("settings.gradle"), "rootProject.name = 'x'");
        Files.writeString(userDir.resolve("settings.gradle.kts"), "rootProject.name = \"x\"");
        Files.writeString(userDir.resolve("gradlew"), "#!/bin/sh");
        Files.writeString(userDir.resolve("gradlew.bat"), "@echo off");
        Files.writeString(userDir.resolve("mvnw"), "#!/bin/sh");
        Files.writeString(userDir.resolve("mvnw.cmd"), "@echo off");
        Files.writeString(userDir.resolve("HELP.md"), "# help");
        Files.createDirectories(userDir.resolve("build/classes"));
        Files.writeString(userDir.resolve("build/classes/App.class"), "bytecode");
        Files.createDirectories(userDir.resolve("target"));
        Files.writeString(userDir.resolve("target/app.jar"), "jar");
        Files.createDirectories(userDir.resolve(".idea"));
        Files.writeString(userDir.resolve(".idea/workspace.xml"), "<xml/>");
        Files.createDirectories(userDir.resolve(".git"));
        Files.writeString(userDir.resolve(".git/HEAD"), "ref: refs/heads/main");
        Files.createDirectories(userDir.resolve("gradle/wrapper"));
        Files.writeString(userDir.resolve("gradle/wrapper/gradle-wrapper.properties"), "url");
        Files.createDirectories(userDir.resolve(".gradle"));
        Files.writeString(userDir.resolve(".gradle/cache.bin"), "cache");
        Files.createDirectories(userDir.resolve("node_modules/foo"));
        Files.writeString(userDir.resolve("node_modules/foo/index.js"), "module");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(tempDir.resolve("empty-templates"));
        Path output = tempDir.resolve("out");
        Files.createDirectories(output);
        engine.overlayDirectory(userDir, config(output), false, msg -> {});

        // allowed files present
        assertTrue(Files.exists(output.resolve("src/main/java/com/example/App.java")));
        assertTrue(Files.exists(output.resolve("src/main/kotlin/com/example/App.kt")));
        assertTrue(Files.exists(output.resolve("Dockerfile")));
        assertTrue(Files.exists(output.resolve(".env")));
        assertTrue(Files.exists(output.resolve("docker-compose.yml")));
        assertTrue(Files.exists(output.resolve("README.md")));
        assertTrue(Files.exists(output.resolve("data/seed.sql")));
        // config/application.yml gets remapped to src/main/resources/application.yml
        assertTrue(Files.exists(output.resolve("src/main/resources/application.yml")));

        // denied files absent
        assertFalse(Files.exists(output.resolve("build.gradle")));
        assertFalse(Files.exists(output.resolve("build.gradle.kts")));
        assertFalse(Files.exists(output.resolve("pom.xml")));
        assertFalse(Files.exists(output.resolve("settings.gradle")));
        assertFalse(Files.exists(output.resolve("settings.gradle.kts")));
        assertFalse(Files.exists(output.resolve("gradlew")));
        assertFalse(Files.exists(output.resolve("gradlew.bat")));
        assertFalse(Files.exists(output.resolve("mvnw")));
        assertFalse(Files.exists(output.resolve("mvnw.cmd")));
        assertFalse(Files.exists(output.resolve("HELP.md")));
        assertFalse(Files.exists(output.resolve("build/classes/App.class")));
        assertFalse(Files.exists(output.resolve("target/app.jar")));
        assertFalse(Files.exists(output.resolve(".idea/workspace.xml")));
        assertFalse(Files.exists(output.resolve(".git/HEAD")));
        assertFalse(Files.exists(output.resolve("gradle/wrapper/gradle-wrapper.properties")));
        assertFalse(Files.exists(output.resolve(".gradle/cache.bin")));
        assertFalse(Files.exists(output.resolve("node_modules/foo/index.js")));
    }

    @Test
    void overlayDirectory_copiesRandomConfigDirectory() throws Exception {
        Path configDir = tempDir.resolve("my-configs");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("nginx.conf"), "server {}");
        Files.writeString(configDir.resolve("logback.xml"), "<configuration/>");
        Files.createDirectories(configDir.resolve("k8s"));
        Files.writeString(configDir.resolve("k8s/deployment.yml"), "apiVersion: apps/v1");

        TemplateOverlayEngine engine = new TemplateOverlayEngine(tempDir.resolve("empty-templates"));
        Path output = tempDir.resolve("out");
        Files.createDirectories(output);
        engine.overlayDirectory(configDir, config(output), false, msg -> {});

        assertTrue(Files.exists(output.resolve("nginx.conf")));
        assertTrue(Files.exists(output.resolve("logback.xml")));
        assertTrue(Files.exists(output.resolve("k8s/deployment.yml")));
    }

    @Test
    void isAllowedUserFile_denylist() {
        // allowed — source files, config, docker, misc
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("src/main/java/Foo.java"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("src/main/kotlin/Foo.kt"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("src/test/kotlin/FooTest.kt"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("src/test/resources/test.yml"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("config/application-dev.yml"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("Dockerfile"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("docker-compose.yml"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("docker-compose.kafka.yml"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile(".env"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile(".env.example"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("README.md"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("data/seed.sql"));
        assertTrue(TemplateOverlayEngine.isAllowedUserFile("k8s/deployment.yml"));

        // denied — build scripts
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("build.gradle"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("build.gradle.kts"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("pom.xml"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("settings.gradle"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("settings.gradle.kts"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("gradlew"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("gradlew.bat"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("mvnw"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("mvnw.cmd"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("HELP.md"));

        // denied — build output and compiled artifacts
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("build/classes/App.class"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("target/app.jar"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("out/production/App.class"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("libs/dep.jar"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("app.war"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("project.iml"));

        // denied — IDE, VCS, tooling
        assertFalse(TemplateOverlayEngine.isAllowedUserFile(".idea/workspace.xml"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile(".vscode/settings.json"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile(".git/HEAD"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile(".gradle/cache.bin"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("gradle/wrapper/gradle-wrapper.properties"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile(".mvn/wrapper/maven-wrapper.properties"));
        assertFalse(TemplateOverlayEngine.isAllowedUserFile("node_modules/foo/index.js"));
    }

    private static ProjectConfig config(Path output) {
        return config(output, ProjectConfig.AuthStyle.jwt, List.of());
    }

    private static ProjectConfig config(Path output, ProjectConfig.AuthStyle authStyle, List<String> scaffolds) {
        return config(output, authStyle, scaffolds, List.of("web"));
    }

    private static ProjectConfig config(
            Path output,
            ProjectConfig.AuthStyle authStyle,
            List<String> scaffolds,
            List<String> dependencies
    ) {
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
                        scaffolds,
                        output
                )
        );
    }
}

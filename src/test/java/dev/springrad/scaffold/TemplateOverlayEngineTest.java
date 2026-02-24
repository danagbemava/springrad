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

    private static ProjectConfig config(Path output) {
        return config(output, ProjectConfig.AuthStyle.jwt, List.of());
    }

    private static ProjectConfig config(Path output, ProjectConfig.AuthStyle authStyle, List<String> scaffolds) {
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
                        List.of("web"),
                        scaffolds,
                        output
                )
        );
    }
}

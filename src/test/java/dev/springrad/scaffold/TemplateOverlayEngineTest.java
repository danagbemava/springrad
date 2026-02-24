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

    private static ProjectConfig config(Path output) {
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
                        ProjectConfig.Database.postgresql,
                        List.of("web"),
                        List.of(),
                        output
                )
        );
    }
}

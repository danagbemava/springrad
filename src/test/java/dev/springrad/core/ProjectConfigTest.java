package dev.springrad.core;

import dev.springrad.cli.CliArgs;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectConfigTest {

    @Test
    void cliOverridesPreset() {
        Preset preset = new Preset(
                "web-api",
                "com.preset",
                "17",
                "3.3.0",
                ProjectConfig.Packaging.war,
                ProjectConfig.BuildTool.maven,
                ProjectConfig.AuthStyle.session,
                ProjectConfig.Database.mysql,
                List.of("web"),
                List.of("AuthController")
        );

        CliArgs cliArgs = new CliArgs(
                "cli-app",
                "dev.cli",
                "cli-artifact",
                "21",
                "3.4.2",
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web", "security"),
                List.of("GlobalExceptionHandler"),
                Path.of("./out")
        );

        ProjectConfig config = ProjectConfig.merge(preset, cliArgs);

        assertEquals("dev.cli", config.groupId());
        assertEquals("cli-artifact", config.artifactId());
        assertEquals(ProjectConfig.BuildTool.gradle, config.buildTool());
        assertEquals(List.of("web", "security"), config.dependencies());
    }

    @Test
    void presetUsedWhenCliNotProvided() {
        Preset preset = new Preset(
                "web-api",
                "com.preset",
                "21",
                "3.4.0",
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web"),
                List.of("OpenApiConfig")
        );
        CliArgs cliArgs = new CliArgs(
                "preset-app",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null
        );

        ProjectConfig config = ProjectConfig.merge(preset, cliArgs);

        assertEquals("com.preset", config.groupId());
        assertEquals("preset-app", config.artifactId());
        assertEquals("21", config.javaVersion());
        assertEquals(List.of("web"), config.dependencies());
    }

    @Test
    void defaultsAppliedWhenPresetAndCliOmitValues() {
        Preset preset = Preset.named("empty");
        CliArgs cliArgs = new CliArgs(
                "default-app",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null
        );

        ProjectConfig config = ProjectConfig.merge(preset, cliArgs);

        assertEquals("com.example", config.groupId());
        assertEquals("default-app", config.artifactId());
        assertEquals("21", config.javaVersion());
        assertEquals(ProjectConfig.Packaging.jar, config.packaging());
        assertEquals(Path.of("./default-app"), config.outputDirectory());
    }
}

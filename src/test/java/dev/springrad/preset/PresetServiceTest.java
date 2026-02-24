package dev.springrad.preset;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void seedDefaultsAddsBuiltInsOnFirstRun() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        PresetService service = new PresetService(repository);

        service.seedDefaults();

        List<Preset> presets = repository.findAll();
        assertEquals(2, presets.size());
        assertTrue(presets.stream().allMatch(Preset::builtIn));
    }

    @Test
    void resolveUsesCliOverridesOverPresetValues() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        repository.save(new Preset(
                "custom",
                false,
                "com.preset",
                "17",
                "3.3.0",
                ProjectConfig.Packaging.war,
                ProjectConfig.BuildTool.maven,
                ProjectConfig.AuthStyle.session,
                ProjectConfig.Database.mysql,
                List.of("web"),
                List.of("AuthController")
        ));

        PresetService service = new PresetService(repository);
        ProjectConfig config = service.resolve(
                "custom",
                new CliArgs(
                        "demo",
                        "dev.override",
                        "demo-artifact",
                        "21",
                        "3.4.2",
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.jwt,
                        ProjectConfig.Database.postgresql,
                        List.of("web", "security"),
                        List.of("GlobalExceptionHandler"),
                        Path.of("./demo")
                )
        );

        assertEquals("dev.override", config.groupId());
        assertEquals("demo-artifact", config.artifactId());
        assertEquals(ProjectConfig.BuildTool.gradle, config.buildTool());
        assertEquals(List.of("web", "security"), config.dependencies());
    }
}

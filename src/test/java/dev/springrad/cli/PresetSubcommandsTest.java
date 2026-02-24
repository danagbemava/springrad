package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;
import dev.springrad.preset.Preset;
import dev.springrad.preset.PresetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetSubcommandsTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndDeleteCustomPreset() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));

        int saveCode = new CommandLine(new PresetSaveCommand(repository))
                .execute("custom", "--deps", "web,security", "--auth", "jwt", "--database", "postgresql");
        assertTrue(saveCode == 0);
        assertTrue(repository.findByName("custom").isPresent());

        int deleteCode = new CommandLine(new PresetDeleteCommand(repository)).execute("custom");
        assertTrue(deleteCode == 0);
        assertTrue(repository.findByName("custom").isEmpty());
    }

    @Test
    void cannotDeleteBuiltInPreset() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        repository.save(new Preset(
                "web-api",
                true,
                "com.example",
                "21",
                null,
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web"),
                List.of()
        ));

        new CommandLine(new PresetDeleteCommand(repository)).execute("web-api");
        assertTrue(repository.findByName("web-api").isPresent());
    }
}

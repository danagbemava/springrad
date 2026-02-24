package dev.springrad.preset;

import dev.springrad.core.ProjectConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void findAllOnMissingFileReturnsEmptyList() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void saveCreatesFileAndMergesByPresetName() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        repository.save(samplePreset("web-api", false));
        repository.save(samplePreset("event-driven", false));
        repository.save(samplePreset("web-api", false));

        List<Preset> presets = repository.findAll();
        assertEquals(2, presets.size());
        assertTrue(presets.stream().anyMatch(p -> p.name().equals("web-api")));
        assertTrue(presets.stream().anyMatch(p -> p.name().equals("event-driven")));
    }

    @Test
    void deleteRemovesOnlyNamedPresetAndProtectsBuiltIns() {
        PresetRepository repository = new PresetRepository(tempDir.resolve("presets.json"));
        repository.save(samplePreset("custom", false));
        repository.save(samplePreset("web-api", true));

        assertTrue(repository.delete("custom"));
        assertFalse(repository.delete("web-api"));
        assertEquals(1, repository.findAll().size());
        assertEquals("web-api", repository.findAll().getFirst().name());
    }

    private static Preset samplePreset(String name, boolean builtIn) {
        return new Preset(
                name,
                builtIn,
                "com.example",
                "21",
                null,
                ProjectConfig.Packaging.jar,
                ProjectConfig.BuildTool.gradle,
                ProjectConfig.AuthStyle.jwt,
                ProjectConfig.Database.postgresql,
                List.of("web"),
                List.of()
        );
    }
}

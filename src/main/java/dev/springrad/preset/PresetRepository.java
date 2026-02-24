package dev.springrad.preset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PresetRepository {
    private static final TypeReference<List<Preset>> PRESET_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Path storePath;

    public PresetRepository() {
        this(defaultStorePath());
    }

    public PresetRepository(Path storePath) {
        this.objectMapper = new ObjectMapper();
        this.storePath = storePath;
    }

    private static Path defaultStorePath() {
        String override = System.getProperty("springrad.presets.path");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".springrad", "presets.json");
    }

    public List<Preset> findAll() {
        if (!Files.exists(storePath)) {
            return List.of();
        }
        try {
            List<Preset> presets = objectMapper.readValue(storePath.toFile(), PRESET_LIST);
            return presets == null ? List.of() : presets;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read presets from " + storePath, e);
        }
    }

    public Optional<Preset> findByName(String name) {
        return findAll().stream()
                .filter(p -> p.name().equals(name))
                .findFirst();
    }

    public void save(Preset preset) {
        List<Preset> current = new ArrayList<>(findAll());
        current.removeIf(p -> p.name().equals(preset.name()));
        current.add(preset);
        writeAll(current);
    }

    public boolean delete(String name) {
        List<Preset> current = new ArrayList<>(findAll());
        Optional<Preset> target = current.stream().filter(p -> p.name().equals(name)).findFirst();
        if (target.isEmpty() || target.get().builtIn()) {
            return false;
        }
        current.removeIf(p -> p.name().equals(name));
        writeAll(current);
        return true;
    }

    public void saveAll(List<Preset> presets) {
        writeAll(presets);
    }

    private void writeAll(List<Preset> presets) {
        try {
            Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, "presets-", ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), presets);
            Files.move(temp, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist presets to " + storePath, e);
        }
    }
}

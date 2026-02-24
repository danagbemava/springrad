package dev.springrad.preset;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

public final class PresetService {
    private final PresetRepository repository;

    public PresetService() {
        this(new PresetRepository());
    }

    public PresetService(PresetRepository repository) {
        this.repository = repository;
    }

    public void seedDefaults() {
        List<Preset> current = new ArrayList<>(repository.findAll());
        for (Preset builtin : builtInPresets()) {
            boolean exists = current.stream().anyMatch(p -> p.name().equals(builtin.name()));
            if (!exists) {
                current.add(builtin);
            }
        }
        repository.saveAll(current);
    }

    public ProjectConfig resolve(String presetName, CliArgs overrides) {
        seedDefaults();
        Preset preset = presetName == null
                ? Preset.named("defaults")
                : repository.findByName(presetName)
                .orElseThrow(() -> new IllegalArgumentException("Preset not found: " + presetName));
        return ProjectConfig.merge(preset, overrides);
    }

    static List<Preset> builtInPresets() {
        return List.of(
                new Preset(
                        "web-api",
                        true,
                        null,
                        "21",
                        null,
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.jwt,
                        ProjectConfig.Database.postgresql,
                        List.of("web", "security", "data-jpa", "postgresql", "flyway", "actuator"),
                        List.of("SecurityConfigJwt", "AuthController", "GlobalExceptionHandler", "OpenApiConfig")
                ),
                new Preset(
                        "event-driven",
                        true,
                        null,
                        "21",
                        null,
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.jwt,
                        ProjectConfig.Database.postgresql,
                        List.of("web", "kafka", "security", "data-jpa", "postgresql", "flyway", "actuator"),
                        List.of("BaseConsumer", "BaseProducer", "DeadLetterQueueConfig", "KafkaTopicConfig")
                )
        );
    }
}

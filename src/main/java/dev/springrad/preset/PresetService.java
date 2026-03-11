package dev.springrad.preset;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.GlobalConfig;
import dev.springrad.core.GlobalConfigLoader;
import dev.springrad.core.ProjectConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class PresetService {
    private final PresetRepository repository;
    private final GlobalConfigLoader globalConfigLoader;

    public PresetService() {
        this(new PresetRepository(), new GlobalConfigLoader());
    }

    public PresetService(PresetRepository repository) {
        this(repository, new GlobalConfigLoader());
    }

    public PresetService(PresetRepository repository, GlobalConfigLoader globalConfigLoader) {
        this.repository = repository;
        this.globalConfigLoader = globalConfigLoader;
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
        return ProjectConfig.merge(preset, overrides, globalConfig());
    }

    public Optional<Preset> findByName(String presetName) {
        seedDefaults();
        return repository.findByName(presetName);
    }

    public GlobalConfig globalConfig() {
        return globalConfigLoader.load();
    }

    public GlobalConfigLoader globalConfigLoader() {
        return globalConfigLoader;
    }

    public List<String> listPresetNames() {
        seedDefaults();
        return repository.findAll().stream()
                .map(Preset::name)
                .sorted(Comparator.naturalOrder())
                .toList();
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
                        List.of("web", "security", "data-jpa", "postgresql", "flyway", "actuator", "rate-limit"),
                        List.of(
                                "SecurityConfigJwt",
                                "JwtTokenProvider",
                                "AuthController",
                                "ApiResponse",
                                "GlobalExceptionHandler",
                                "OpenApiConfig",
                                "AuditableEntity",
                                "RateLimitingFilter",
                                "LogbackJsonConfig",
                                "FlywayInitMigration"
                        ),
                        null
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
                        List.of(
                                "SecurityConfigJwt",
                                "JwtTokenProvider",
                                "ApiResponse",
                                "GlobalExceptionHandler",
                                "AuditableEntity",
                                "FlywayInitMigration",
                                "LogbackJsonConfig",
                                "BaseConsumer",
                                "BaseProducer",
                                "DeadLetterQueueConfig",
                                "KafkaTopicConfig"
                        ),
                        null
                )
        );
    }
}

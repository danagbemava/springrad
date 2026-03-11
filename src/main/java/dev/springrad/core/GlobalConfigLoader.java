package dev.springrad.core;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GlobalConfigLoader {
    private static final Logger LOGGER = Logger.getLogger(GlobalConfigLoader.class.getName());
    private static final String CONFIG_FILE = "config.yml";

    private final Path configDirectory;

    public GlobalConfigLoader() {
        this(SpringRadPaths.configDirectory());
    }

    public GlobalConfigLoader(Path configDirectory) {
        this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory");
    }

    public Path configPath() {
        return configDirectory.resolve(CONFIG_FILE);
    }

    public Path configDirectory() {
        return configDirectory;
    }

    @SuppressWarnings("unchecked")
    public GlobalConfig load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return GlobalConfig.empty();
        }

        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (InputStream in = Files.newInputStream(path)) {
            Object raw = yaml.load(in);
            if (!(raw instanceof Map<?, ?> root)) {
                return GlobalConfig.empty();
            }

            Map<String, Object> typedRoot = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : root.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    typedRoot.put(key, entry.getValue());
                }
            }

            GlobalConfig.Defaults defaults = parseDefaults((Map<String, Object>) typedRoot.get("defaults"));
            Path templateDir = parseTemplateDir(typedRoot.get("templateDir"));
            Map<String, List<String>> aliases = parseAliases((Map<String, Object>) typedRoot.get("aliases"));

            typedRoot.keySet().stream()
                    .filter(key -> !List.of("defaults", "templateDir", "aliases").contains(key))
                    .forEach(key -> LOGGER.fine("Ignoring unknown global config key: " + key));

            return new GlobalConfig(defaults, templateDir, aliases);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load global config from " + path, e);
        }
    }

    private GlobalConfig.Defaults parseDefaults(Map<String, Object> defaultsMap) {
        if (defaultsMap == null || defaultsMap.isEmpty()) {
            return GlobalConfig.Defaults.empty();
        }

        for (String key : defaultsMap.keySet()) {
            if (!List.of("groupId", "javaVersion", "bootVersion", "packaging", "buildTool", "author").contains(key)) {
                LOGGER.fine("Ignoring unknown global defaults key: " + key);
            }
        }

        return new GlobalConfig.Defaults(
                stringValue(defaultsMap.get("groupId")),
                stringValue(defaultsMap.get("javaVersion")),
                stringValue(defaultsMap.get("bootVersion")),
                enumValue(ProjectConfig.Packaging.class, stringValue(defaultsMap.get("packaging"))),
                enumValue(ProjectConfig.BuildTool.class, stringValue(defaultsMap.get("buildTool"))),
                stringValue(defaultsMap.get("author"))
        );
    }

    private Path parseTemplateDir(Object templateDirValue) {
        String raw = stringValue(templateDirValue);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return SpringRadPaths.resolveUserPath(raw, configDirectory);
    }

    private Map<String, List<String>> parseAliases(Map<String, Object> aliasesMap) {
        if (aliasesMap == null || aliasesMap.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> aliases = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : aliasesMap.entrySet()) {
            if (!(entry.getValue() instanceof List<?> listValue)) {
                LOGGER.log(Level.FINE, "Ignoring alias key {0} because value is not a list", entry.getKey());
                continue;
            }
            List<String> resolved = new ArrayList<>();
            for (Object value : listValue) {
                if (value != null) {
                    String dep = String.valueOf(value).trim();
                    if (!dep.isEmpty()) {
                        resolved.add(dep);
                    }
                }
            }
            aliases.put(entry.getKey(), List.copyOf(resolved));
        }

        return Map.copyOf(aliases);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (T constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid " + type.getSimpleName() + " value in config: " + value);
    }
}

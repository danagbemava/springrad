package dev.springrad.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class DependencyAliasRegistry {
    private static final Logger LOGGER = Logger.getLogger(DependencyAliasRegistry.class.getName());

    private final Map<String, List<String>> aliases;

    public DependencyAliasRegistry() {
        this.aliases = defaultAliases();
    }

    public List<String> resolve(String dependency) {
        if (dependency == null || dependency.isBlank()) {
            return List.of();
        }

        List<String> resolved = aliases.get(dependency);
        if (resolved != null) {
            return resolved;
        }

        LOGGER.warning("Unknown dependency alias: " + dependency + ". Passing through as raw Initializr ID.");
        return List.of(dependency);
    }

    public List<String> resolveAll(List<String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }

        Set<String> flattened = new LinkedHashSet<>();
        for (String dependency : dependencies) {
            flattened.addAll(resolve(dependency));
        }
        return List.copyOf(flattened);
    }

    private static Map<String, List<String>> defaultAliases() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("db", List.of("data-jpa", "postgresql"));
        map.put("db-mysql", List.of("data-jpa", "mysql"));
        map.put("db-h2", List.of("data-jpa", "h2"));
        map.put("security", List.of("security"));
        map.put("obs", List.of("actuator", "prometheus"));
        map.put("migrate", List.of("flyway"));
        map.put("migrate-liquibase", List.of("liquibase"));
        map.put("kafka", List.of("kafka"));
        map.put("rabbit", List.of("amqp"));
        map.put("docs", List.of("springdoc"));
        map.put("web", List.of("web"));
        map.put("rate-limit", List.of("cache"));
        return map;
    }
}

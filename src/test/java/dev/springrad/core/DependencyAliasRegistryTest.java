package dev.springrad.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAliasRegistryTest {

    @Test
    void dbResolvesToJpaAndPostgresql() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        assertEquals(List.of("data-jpa", "postgresql"), registry.resolve("db"));
    }

    @Test
    void obsResolvesToActuatorAndPrometheus() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        assertEquals(List.of("actuator", "prometheus"), registry.resolve("obs"));
    }

    @Test
    void rateLimitResolvesToCache() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        assertEquals(List.of("cache"), registry.resolve("rate-limit"));
    }

    @Test
    void mixedListResolvesFlatWithoutDuplicates() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        List<String> resolved = registry.resolveAll(List.of("db", "web", "security", "data-jpa"));
        assertEquals(List.of("data-jpa", "postgresql", "web", "security"), resolved);
    }

    @Test
    void unknownAliasLogsWarningAndPassesThrough() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        Logger logger = Logger.getLogger(DependencyAliasRegistry.class.getName());
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        try {
            List<String> resolved = registry.resolve("xyz");
            assertEquals(List.of("xyz"), resolved);
            assertTrue(handler.messages.stream().anyMatch(m -> m.contains("Unknown dependency alias: xyz")));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(true);
        }
    }

    @Test
    void knownInitializrIdsDoNotLogWarnings() {
        DependencyAliasRegistry registry = new DependencyAliasRegistry();
        Logger logger = Logger.getLogger(DependencyAliasRegistry.class.getName());
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        try {
            List<String> resolved = registry.resolveAll(List.of(
                    "web", "security", "data-jpa", "postgresql", "flyway", "actuator"
            ));
            assertEquals(List.of("web", "security", "data-jpa", "postgresql", "flyway", "actuator"), resolved);
            assertTrue(handler.messages.isEmpty(), "Expected no warnings for known Initializr IDs");
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(true);
        }
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}

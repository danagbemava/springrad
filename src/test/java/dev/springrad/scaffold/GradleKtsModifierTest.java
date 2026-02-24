package dev.springrad.scaffold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleKtsModifierTest {
    @TempDir
    Path tempDir;

    @Test
    void addsNewImplementationDependency() throws Exception {
        Path buildFile = writeBuild(standardBuild());
        GradleKtsModifier modifier = new GradleKtsModifier(buildFile);

        modifier.addDependency("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0");

        String updated = Files.readString(buildFile);
        assertTrue(updated.contains("implementation(\"org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0\")"));
    }

    @Test
    void duplicateDependencyIsNoOp() throws Exception {
        Path buildFile = writeBuild(standardBuild());
        GradleKtsModifier modifier = new GradleKtsModifier(buildFile);

        modifier.addDependency("implementation", "org.springframework.boot:spring-boot-starter-web");

        String updated = Files.readString(buildFile);
        String needle = "org.springframework.boot:spring-boot-starter-web";
        assertEquals(updated.indexOf(needle), updated.lastIndexOf(needle));
    }

    @Test
    void unrecognizedStructureLogsWarningAndSkips() throws Exception {
        Path buildFile = writeBuild("plugins { java }");
        Logger logger = Logger.getLogger("GradleKtsModifierTestLogger");
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        try {
            GradleKtsModifier modifier = new GradleKtsModifier(
                    buildFile,
                    tempDir.resolve("gradle/libs.versions.toml"),
                    GradleKtsModifier.AtomicWriter.defaultWriter(),
                    logger
            );
            modifier.addDependency("implementation", "a:b:1.0.0");

            assertEquals("plugins { java }", Files.readString(buildFile));
            assertTrue(handler.messages.stream().anyMatch(m -> m.contains("dependencies block not found")));
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(true);
        }
    }

    @Test
    void atomicWriteFailurePreservesExistingFile() throws Exception {
        Path buildFile = writeBuild(standardBuild());
        String original = Files.readString(buildFile);

        GradleKtsModifier modifier = new GradleKtsModifier(
                buildFile,
                tempDir.resolve("gradle/libs.versions.toml"),
                (file, content) -> {
                    throw new IOException("simulated write failure");
                },
                Logger.getLogger("GradleKtsModifierTest")
        );

        assertThrows(
                IllegalStateException.class,
                () -> modifier.addDependency("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
        );
        assertEquals(original, Files.readString(buildFile));
    }

    private Path writeBuild(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle.kts");
        Files.writeString(buildFile, content, StandardCharsets.UTF_8);
        return buildFile;
    }

    private static String standardBuild() {
        return """
                plugins {
                    java
                }
                                
                dependencies {
                    implementation("org.springframework.boot:spring-boot-starter-web")
                    testImplementation("org.junit.jupiter:junit-jupiter")
                }
                """;
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

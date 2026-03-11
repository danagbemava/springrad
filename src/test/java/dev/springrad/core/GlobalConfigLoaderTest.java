package dev.springrad.core;

import dev.springrad.cli.CliArgs;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalConfigLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void missingConfigReturnsEmptyConfig() {
        GlobalConfigLoader loader = new GlobalConfigLoader(tempDir);

        GlobalConfig config = loader.load();

        assertNull(config.templateDir());
        assertNull(config.defaults().groupId());
        assertEquals(0, config.aliases().size());
    }

    @Test
    void resolvesRelativeAndTildeTemplatePathAndAliases() throws Exception {
        Path home = tempDir.resolve("fake-home");
        Files.createDirectories(home);
        Path configDir = home.resolve(".springrad");
        Files.createDirectories(configDir);
        Path relativeTemplateDir = configDir.resolve("custom/templates");
        Files.createDirectories(relativeTemplateDir);

        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        try {
            Files.writeString(configDir.resolve("config.yml"), """
                    defaults:
                      groupId: com.acme
                      javaVersion: "17"
                    templateDir: ./custom/templates
                    aliases:
                      monitoring: ["actuator", "prometheus"]
                    """);

            GlobalConfigLoader loader = new GlobalConfigLoader(configDir);
            GlobalConfig loaded = loader.load();

            assertEquals("com.acme", loaded.defaults().groupId());
            assertEquals("17", loaded.defaults().javaVersion());
            assertEquals(relativeTemplateDir.normalize(), loaded.templateDir());
            assertEquals(List.of("actuator", "prometheus"), loaded.aliases().get("monitoring"));
            assertNotNull(loaded.defaults());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void configDefaultsAndAliasesParticipateInMerge() {
        GlobalConfig globalConfig = new GlobalConfig(
                new GlobalConfig.Defaults("com.global", "17", null, null, null, null),
                null,
                java.util.Map.of("monitoring", List.of("actuator", "prometheus"))
        );

        ProjectConfig merged = ProjectConfig.merge(
                Preset.named("web-api"),
                new CliArgs(
                        "demo-app",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of("monitoring"),
                        List.of(),
                        Path.of("./out")
                ),
                globalConfig
        );

        assertEquals("com.global", merged.groupId());
        assertEquals("17", merged.javaVersion());
        assertEquals(List.of("actuator", "prometheus"), merged.dependencies());
    }
}

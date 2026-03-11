package dev.springrad.core;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record GlobalConfig(
        Defaults defaults,
        Path templateDir,
        Map<String, List<String>> aliases
) {
    public static GlobalConfig empty() {
        return new GlobalConfig(Defaults.empty(), null, Map.of());
    }

    public record Defaults(
            String groupId,
            String javaVersion,
            String bootVersion,
            ProjectConfig.Packaging packaging,
            ProjectConfig.BuildTool buildTool,
            String author
    ) {
        static Defaults empty() {
            return new Defaults(null, null, null, null, null, null);
        }
    }
}

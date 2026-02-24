package dev.springrad.preset;

import dev.springrad.core.ProjectConfig;

import java.util.List;

public record Preset(
        String name,
        boolean builtIn,
        String groupId,
        String javaVersion,
        String bootVersion,
        ProjectConfig.Packaging packaging,
        ProjectConfig.BuildTool buildTool,
        ProjectConfig.AuthStyle authStyle,
        ProjectConfig.Database database,
        List<String> dependencies,
        List<String> scaffolds
) {
    public static Preset named(String name) {
        return new Preset(
                name,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }
}

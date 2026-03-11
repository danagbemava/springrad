package dev.springrad.cli;

import dev.springrad.core.ProjectConfig;

import java.nio.file.Path;
import java.util.List;

public record CliArgs(
        String name,
        String groupId,
        String artifactId,
        String javaVersion,
        String bootVersion,
        ProjectConfig.Packaging packaging,
        ProjectConfig.BuildTool buildTool,
        ProjectConfig.AuthStyle authStyle,
        ProjectConfig.Database database,
        List<String> dependencies,
        List<String> scaffolds,
        Path outputDirectory,
        String templateDir,
        boolean noUserTemplates
) {
    public CliArgs(
            String name,
            String groupId,
            String artifactId,
            String javaVersion,
            String bootVersion,
            ProjectConfig.Packaging packaging,
            ProjectConfig.BuildTool buildTool,
            ProjectConfig.AuthStyle authStyle,
            ProjectConfig.Database database,
            List<String> dependencies,
            List<String> scaffolds,
            Path outputDirectory
    ) {
        this(
                name,
                groupId,
                artifactId,
                javaVersion,
                bootVersion,
                packaging,
                buildTool,
                authStyle,
                database,
                dependencies,
                scaffolds,
                outputDirectory,
                null,
                false
        );
    }
}

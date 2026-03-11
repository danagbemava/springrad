package dev.springrad.core;

import dev.springrad.cli.CliArgs;
import dev.springrad.preset.Preset;

import java.nio.file.Path;
import java.util.List;

public record ProjectConfig(
        String name,
        String groupId,
        String artifactId,
        String javaVersion,
        String bootVersion,
        Packaging packaging,
        BuildTool buildTool,
        AuthStyle authStyle,
        Database database,
        List<String> dependencies,
        List<String> scaffolds,
        Path outputDirectory
) {
    public enum Packaging {jar, war}
    public enum BuildTool {gradle, maven}
    public enum AuthStyle {jwt, session, none}
    public enum Database {postgresql, mysql, h2}

    public static ProjectConfig merge(Preset preset, CliArgs args, GlobalConfig globalConfig) {
        GlobalConfig config = globalConfig == null ? GlobalConfig.empty() : globalConfig;
        GlobalConfig.Defaults defaults = config.defaults();

        String name = firstNonBlank(args.name(), preset.name(), "springrad-app");
        String artifactId = firstNonBlank(args.artifactId(), name);

        Path output = args.outputDirectory();
        if (output == null) {
            output = Path.of("./" + artifactId);
        }

        DependencyAliasRegistry aliasRegistry = new DependencyAliasRegistry(config.aliases());

        return new ProjectConfig(
                name,
                firstNonBlank(args.groupId(), preset.groupId(), defaults.groupId(), "com.example"),
                artifactId,
                firstNonBlank(args.javaVersion(), preset.javaVersion(), defaults.javaVersion(), "21"),
                firstNonBlank(args.bootVersion(), preset.bootVersion(), defaults.bootVersion(), "latest"),
                firstNonNull(args.packaging(), preset.packaging(), defaults.packaging(), Packaging.jar),
                firstNonNull(args.buildTool(), preset.buildTool(), defaults.buildTool(), BuildTool.gradle),
                firstNonNull(args.authStyle(), preset.authStyle(), AuthStyle.jwt),
                firstNonNull(args.database(), preset.database(), Database.postgresql),
                aliasRegistry.resolveAll(nonEmpty(args.dependencies(), preset.dependencies())),
                nonEmpty(args.scaffolds(), preset.scaffolds()),
                output
        );
    }

    public static ProjectConfig merge(Preset preset, CliArgs args) {
        return merge(preset, args, GlobalConfig.empty());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalArgumentException("Expected at least one non-blank value");
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        throw new IllegalArgumentException("Expected at least one non-null value");
    }

    private static List<String> nonEmpty(List<String> primary, List<String> fallback) {
        if (primary != null && !primary.isEmpty()) {
            return List.copyOf(primary);
        }
        if (fallback != null && !fallback.isEmpty()) {
            return List.copyOf(fallback);
        }
        return List.of();
    }
}

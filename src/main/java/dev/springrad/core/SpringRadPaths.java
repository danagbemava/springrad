package dev.springrad.core;

import java.nio.file.Path;

public final class SpringRadPaths {
    private static final String CONFIG_DIR_PROPERTY = "springrad.config.dir";
    private static final String CONFIG_DIR_ENV = "SPRINGRAD_CONFIG_DIR";

    private SpringRadPaths() {
    }

    public static Path configDirectory() {
        String propertyOverride = System.getProperty(CONFIG_DIR_PROPERTY);
        if (propertyOverride != null && !propertyOverride.isBlank()) {
            return Path.of(expandUserHome(propertyOverride)).toAbsolutePath().normalize();
        }

        String envOverride = System.getenv(CONFIG_DIR_ENV);
        if (envOverride != null && !envOverride.isBlank()) {
            return Path.of(expandUserHome(envOverride)).toAbsolutePath().normalize();
        }

        return Path.of(System.getProperty("user.home"), ".springrad").toAbsolutePath().normalize();
    }

    public static Path resolveUserPath(String rawPath, Path relativeBase) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        Path parsed = Path.of(expandUserHome(rawPath));
        if (parsed.isAbsolute()) {
            return parsed.normalize();
        }

        return relativeBase.resolve(parsed).normalize();
    }

    private static String expandUserHome(String path) {
        if (path.equals("~")) {
            return System.getProperty("user.home");
        }
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}

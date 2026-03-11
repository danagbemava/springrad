package dev.springrad.tui.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionInfo {
    private static final String VERSION = loadVersion();

    private VersionInfo() {
    }

    public static String version() {
        return VERSION;
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream in = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in == null) {
                return "unknown";
            }
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}

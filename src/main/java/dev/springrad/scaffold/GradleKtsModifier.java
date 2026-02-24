package dev.springrad.scaffold;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradleKtsModifier {
    private static final Logger LOGGER = Logger.getLogger(GradleKtsModifier.class.getName());

    private final Path buildFile;
    private final Path versionCatalogFile;
    private final AtomicWriter writer;
    private final Logger logger;

    public GradleKtsModifier(Path buildFile) {
        this(buildFile, buildFile.getParent().resolve("gradle").resolve("libs.versions.toml"), AtomicWriter.defaultWriter(), LOGGER);
    }

    GradleKtsModifier(Path buildFile, Path versionCatalogFile, AtomicWriter writer, Logger logger) {
        this.buildFile = buildFile;
        this.versionCatalogFile = versionCatalogFile;
        this.writer = writer;
        this.logger = logger;
    }

    public void addDependency(String configuration, String notation) {
        String content = read(buildFile);
        String ga = groupArtifact(notation);
        if (ga != null && containsDependency(content, ga)) {
            return;
        }

        int start = content.indexOf("dependencies {");
        if (start < 0) {
            logger.warning("Unrecognized build.gradle.kts structure: dependencies block not found. Skipping modification.");
            return;
        }

        int end = findBlockEnd(content, start + "dependencies {".length() - 1);
        if (end < 0) {
            logger.warning("Unrecognized build.gradle.kts structure: dependencies block is malformed. Skipping modification.");
            return;
        }

        String line = "    " + configuration + "(\"" + notation + "\")\n";
        String updated = content.substring(0, end) + line + content.substring(end);
        write(buildFile, updated);
    }

    public void setDependencyVersion(String groupId, String artifactId, String newVersion) {
        String content = read(buildFile);
        String pattern = "(\"" + Pattern.quote(groupId + ":" + artifactId + ":") + ")([^\"\\n]+)(\")";
        Matcher matcher = Pattern.compile(pattern).matcher(content);
        if (!matcher.find()) {
            return;
        }
        String updated = matcher.replaceAll("$1" + Matcher.quoteReplacement(newVersion) + "$3");
        write(buildFile, updated);
    }

    public void addToVersionCatalog(String alias, String version) {
        if (!Files.exists(versionCatalogFile)) {
            logger.warning("Version catalog not found at " + versionCatalogFile + ". Skipping.");
            return;
        }

        String content = read(versionCatalogFile);
        String needle = alias + " = ";
        if (content.contains(needle)) {
            return;
        }

        int versionsStart = content.indexOf("[versions]");
        if (versionsStart < 0) {
            logger.warning("Unrecognized libs.versions.toml structure: [versions] section not found. Skipping.");
            return;
        }
        int nextSection = content.indexOf("\n[", versionsStart + 1);
        int insertAt = nextSection < 0 ? content.length() : nextSection + 1;
        String entry = alias + " = \"" + version + "\"\n";
        String updated = content.substring(0, insertAt) + entry + content.substring(insertAt);
        write(versionCatalogFile, updated);
    }

    private static int findBlockEnd(String content, int openBraceIndex) {
        int level = 0;
        for (int i = openBraceIndex; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                level++;
            } else if (c == '}') {
                level--;
                if (level == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean containsDependency(String content, String groupArtifact) {
        return Pattern.compile("\"" + Pattern.quote(groupArtifact) + "(:[^\"\\n]+)?\"")
                .matcher(content)
                .find();
    }

    private static String groupArtifact(String notation) {
        String[] parts = notation.split(":");
        if (parts.length < 2) {
            return null;
        }
        return parts[0] + ":" + parts[1];
    }

    private String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private void write(Path file, String content) {
        try {
            writer.write(file, content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write " + file, e);
        }
    }

    interface AtomicWriter {
        void write(Path file, String content) throws IOException;

        static AtomicWriter defaultWriter() {
            return (file, content) -> {
                Path dir = file.getParent();
                Path temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            };
        }
    }
}

package dev.springrad.scaffold;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class PomModifier {
    private final Path pomPath;
    private final Model model;
    private boolean dirty;

    public PomModifier(Path pomPath) {
        this.pomPath = pomPath;
        this.model = readModel(pomPath);
        this.dirty = false;
    }

    public void addDependency(String groupId, String artifactId, String version, String scope) {
        List<Dependency> dependencies = model.getDependencies();
        if (dependencies == null) {
            dependencies = new ArrayList<>();
            model.setDependencies(dependencies);
        }

        boolean exists = dependencies.stream()
                .anyMatch(d -> groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()));
        if (exists) {
            return;
        }

        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        dependencies.add(dependency);
        dirty = true;
    }

    public void addProperty(String key, String value) {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }

        String current = properties.getProperty(key);
        if (value.equals(current)) {
            return;
        }
        properties.setProperty(key, value);
        dirty = true;
    }

    public void addPlugin(String groupId, String artifactId, String version) {
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        List<Plugin> plugins = build.getPlugins();
        if (plugins == null) {
            plugins = new ArrayList<>();
            build.setPlugins(plugins);
        }

        boolean exists = plugins.stream()
                .anyMatch(p -> groupId.equals(p.getGroupId()) && artifactId.equals(p.getArtifactId()));
        if (exists) {
            return;
        }

        Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        plugins.add(plugin);
        dirty = true;
    }

    public void setParentVersion(String version) {
        if (model.getParent() == null) {
            return;
        }
        String current = model.getParent().getVersion();
        if (version.equals(current)) {
            return;
        }
        model.getParent().setVersion(version);
        dirty = true;
    }

    public void save() {
        if (!dirty) {
            return;
        }
        try {
            Path temp = Files.createTempFile(pomPath.getParent(), "pom-", ".xml.tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                new MavenXpp3Writer().write(writer, model);
            }
            Files.move(temp, pomPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist pom.xml changes", e);
        }
    }

    private static Model readModel(Path pomPath) {
        try (Reader reader = Files.newBufferedReader(pomPath, StandardCharsets.UTF_8)) {
            return new MavenXpp3Reader().read(reader);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse pom.xml: " + pomPath, e);
        }
    }
}

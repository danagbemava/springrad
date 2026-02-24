package dev.springrad.scaffold;

import dev.springrad.core.ProjectConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class TemplateOverlayEngine {
    private final Path filesystemTemplateRoot;
    private final String classpathRoot;

    public TemplateOverlayEngine() {
        this.filesystemTemplateRoot = null;
        this.classpathRoot = "templates";
    }

    TemplateOverlayEngine(Path filesystemTemplateRoot) {
        this.filesystemTemplateRoot = filesystemTemplateRoot;
        this.classpathRoot = null;
    }

    public void overlay(ProjectConfig config, boolean force) {
        Objects.requireNonNull(config, "config");
        try (ResolvedRoot resolvedRoot = resolveRoot()) {
            applyFromRoot(resolvedRoot.path(), config.outputDirectory(), config, force);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to overlay templates", e);
        }
    }

    private void applyFromRoot(Path root, Path targetRoot, ProjectConfig config, boolean force) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.forEach(path -> {
                try {
                    Path relative = root.relativize(path);
                    if (relative.toString().isEmpty()) {
                        return;
                    }
                    if (shouldSkipTemplate(relative, config)) {
                        return;
                    }
                    String renderedRelative = renderPath(relative, config);
                    Path target = targetRoot.resolve(renderedRelative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(target);
                        return;
                    }
                    if (Files.exists(target) && !force) {
                        return;
                    }

                    Files.createDirectories(target.getParent());
                    byte[] input = Files.readAllBytes(path);
                    if (isBinary(input)) {
                        Files.write(target, input);
                    } else {
                        String rendered = render(new String(input, StandardCharsets.UTF_8), config);
                        Files.writeString(target, rendered, StandardCharsets.UTF_8);
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    String render(String content, ProjectConfig config) {
        Map<String, String> values = placeholders(config);
        String rendered = content;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private Map<String, String> placeholders(ProjectConfig config) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("artifactId", config.artifactId());
        values.put("groupId", config.groupId());
        values.put("packageName", packageName(config.groupId(), config.artifactId()));
        values.put("packagePath", packageName(config.groupId(), config.artifactId()).replace('.', '/'));
        values.put("javaVersion", config.javaVersion());
        values.put("bootVersion", config.bootVersion());
        values.put("authStyle", config.authStyle().name());
        values.put("database", config.database().name());
        values.put("messaging", messaging(config));
        values.put("year", String.valueOf(Year.now().getValue()));
        values.put("datasourceUrl", datasourceUrl(config));
        values.put("datasourceUsername", datasourceUsername(config.database()));
        values.put("datasourcePassword", datasourcePassword(config.database()));
        values.put("datasourceDriverClassName", datasourceDriver(config.database()));
        values.put("messagingDevConfig", messagingDevConfig(config));
        values.put("messagingProdConfig", messagingProdConfig(config));
        values.put("jwtSecretConfig", jwtSecretConfig(config));
        values.put("envJwtSection", envJwtSection(config));
        values.put("envMessagingSection", envMessagingSection(config));
        values.put("composeAppDependsOn", composeAppDependsOn(config));
        values.put("composeDbService", composeDbService(config));
        values.put("composeVolumesSection", composeVolumesSection(config));
        return values;
    }

    private static String packageName(String groupId, String artifactId) {
        return groupId + "." + artifactId.replace('-', '.');
    }

    private static boolean isBinary(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private static String messaging(ProjectConfig config) {
        if (config.dependencies().contains("kafka")) {
            return "kafka";
        }
        if (config.dependencies().contains("amqp")) {
            return "rabbit";
        }
        return "none";
    }

    private static String datasourceUrl(ProjectConfig config) {
        return switch (config.database()) {
            case postgresql -> "jdbc:postgresql://localhost:5432/" + config.artifactId() + "_dev";
            case mysql -> "jdbc:mysql://localhost:3306/" + config.artifactId() + "_dev";
            case h2 -> "jdbc:h2:mem:" + config.artifactId() + "_dev;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        };
    }

    private static String datasourceUsername(ProjectConfig.Database database) {
        return switch (database) {
            case postgresql -> "postgres";
            case mysql -> "root";
            case h2 -> "sa";
        };
    }

    private static String datasourcePassword(ProjectConfig.Database database) {
        return switch (database) {
            case postgresql -> "postgres";
            case mysql -> "root";
            case h2 -> "";
        };
    }

    private static String datasourceDriver(ProjectConfig.Database database) {
        return switch (database) {
            case postgresql -> "org.postgresql.Driver";
            case mysql -> "com.mysql.cj.jdbc.Driver";
            case h2 -> "org.h2.Driver";
        };
    }

    private static String messagingDevConfig(ProjectConfig config) {
        if (config.dependencies().contains("kafka")) {
            return "  kafka:\n    bootstrap-servers: localhost:9092";
        }
        return "";
    }

    private static String messagingProdConfig(ProjectConfig config) {
        if (config.dependencies().contains("kafka")) {
            return "  kafka:\n    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}";
        }
        return "";
    }

    private static String jwtSecretConfig(ProjectConfig config) {
        if (config.authStyle() == ProjectConfig.AuthStyle.jwt) {
            return "app:\n  security:\n    jwt:\n      secret: ${JWT_SECRET}";
        }
        return "";
    }

    private static String envJwtSection(ProjectConfig config) {
        if (config.authStyle() != ProjectConfig.AuthStyle.jwt) {
            return "";
        }
        return """
                
                # Security (JWT)
                # Must be at least 256 bits (32 characters)
                JWT_SECRET=change-me-to-a-secure-random-string-at-least-256-bits
                """;
    }

    private static String envMessagingSection(ProjectConfig config) {
        if (config.dependencies().contains("kafka")) {
            return """
                    
                    # Kafka (event-driven only)
                    KAFKA_BOOTSTRAP_SERVERS=localhost:9092
                    """;
        }
        return "";
    }

    private static String composeAppDependsOn(ProjectConfig config) {
        if (config.database() == ProjectConfig.Database.h2) {
            return "";
        }
        return """
                    depends_on:
                      db:
                        condition: service_healthy
                """;
    }

    private static String composeDbService(ProjectConfig config) {
        return switch (config.database()) {
            case postgresql -> """
                db:
                  image: postgres:16-alpine
                  container_name: %s-db
                  ports:
                    - "5432:5432"
                  environment:
                    POSTGRES_DB: %s_dev
                    POSTGRES_USER: postgres
                    POSTGRES_PASSWORD: postgres
                  healthcheck:
                    test: ["CMD-SHELL", "pg_isready -U postgres"]
                    interval: 10s
                    timeout: 5s
                    retries: 5
                  volumes:
                    - %s-db-data:/var/lib/postgresql/data
                """.formatted(config.artifactId(), config.artifactId(), config.artifactId());
            case mysql -> """
                db:
                  image: mysql:8-alpine
                  container_name: %s-db
                  ports:
                    - "3306:3306"
                  environment:
                    MYSQL_DATABASE: %s_dev
                    MYSQL_USER: root
                    MYSQL_PASSWORD: root
                    MYSQL_ROOT_PASSWORD: root
                  healthcheck:
                    test: ["CMD-SHELL", "mysqladmin ping -h localhost -uroot -proot"]
                    interval: 10s
                    timeout: 5s
                    retries: 5
                  volumes:
                    - %s-db-data:/var/lib/mysql
                """.formatted(config.artifactId(), config.artifactId(), config.artifactId());
            case h2 -> "";
        };
    }

    private static String composeVolumesSection(ProjectConfig config) {
        if (config.database() == ProjectConfig.Database.h2) {
            return "";
        }
        return """
                volumes:
                  %s-db-data:
                """.formatted(config.artifactId());
    }

    private static boolean shouldSkipTemplate(Path relative, ProjectConfig config) {
        String fileName = relative.getFileName().toString();
        if ("docker-compose.kafka.yml".equals(fileName) && !config.dependencies().contains("kafka")) {
            return true;
        }
        if ("SecurityConfigJwt.java".equals(fileName) && config.authStyle() != ProjectConfig.AuthStyle.jwt) {
            return true;
        }
        if ("SecurityConfigJwt.java".equals(fileName) && !config.scaffolds().contains("SecurityConfigJwt")) {
            return true;
        }
        if ("SecurityConfigSession.java".equals(fileName) && config.authStyle() != ProjectConfig.AuthStyle.session) {
            return true;
        }
        if ("SecurityConfigSession.java".equals(fileName) && !config.scaffolds().contains("SecurityConfigSession")) {
            return true;
        }
        if ("AuthController.java".equals(fileName) && !config.scaffolds().contains("AuthController")) {
            return true;
        }
        if ("JwtTokenProvider.java".equals(fileName) && config.authStyle() != ProjectConfig.AuthStyle.jwt) {
            return true;
        }
        if ("JwtTokenProvider.java".equals(fileName) && !config.scaffolds().contains("JwtTokenProvider")) {
            return true;
        }
        if ("ApiResponse.java".equals(fileName) && !config.scaffolds().contains("ApiResponse")) {
            return true;
        }
        if ("GlobalExceptionHandler.java".equals(fileName) && !config.scaffolds().contains("GlobalExceptionHandler")) {
            return true;
        }
        if ("OpenApiConfig.java".equals(fileName) && !config.scaffolds().contains("OpenApiConfig")) {
            return true;
        }
        if ("AuditableEntity.java".equals(fileName) && !config.scaffolds().contains("AuditableEntity")) {
            return true;
        }
        return false;
    }

    private String renderPath(Path relative, ProjectConfig config) {
        String normalized = relative.toString().replace('\\', '/');
        String rendered = render(normalized, config);
        if ("/".equals(java.io.File.separator)) {
            return rendered;
        }
        return rendered.replace("/", java.io.File.separator);
    }

    private ResolvedRoot resolveRoot() throws IOException {
        if (filesystemTemplateRoot != null) {
            return new ResolvedRoot(filesystemTemplateRoot, null);
        }

        try {
            URI uri = Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(classpathRoot),
                    "Template root not found on classpath: " + classpathRoot
            ).toURI();
            if ("jar".equals(uri.getScheme())) {
                String raw = uri.toString();
                String[] parts = raw.split("!");
                URI jarUri = URI.create(parts[0]);
                FileSystem fileSystem;
                try {
                    fileSystem = FileSystems.getFileSystem(jarUri);
                    return new ResolvedRoot(fileSystem.getPath(parts[1]), null);
                } catch (Exception e) {
                    fileSystem = FileSystems.newFileSystem(jarUri, new HashMap<>());
                    return new ResolvedRoot(fileSystem.getPath(parts[1]), fileSystem);
                }
            }
            return new ResolvedRoot(Path.of(uri), null);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid template classpath URI", e);
        }
    }

    private record ResolvedRoot(Path path, FileSystem fsToClose) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (fsToClose != null && fsToClose.isOpen()) {
                fsToClose.close();
            }
        }
    }
}

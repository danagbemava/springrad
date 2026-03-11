package dev.springrad.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Fetches metadata from the Spring Initializr API: Java versions, Boot versions,
 * and the full dependency catalog. Caches the response for the lifetime of this instance
 * to avoid repeated network calls. Falls back to sensible defaults when offline.
 */
public final class InitializrMetadataClient {
    private static final Logger log = Logger.getLogger(InitializrMetadataClient.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    static final List<String> DEFAULT_JAVA_VERSIONS = List.of("21", "17", "23", "24");
    static final List<String> DEFAULT_BOOT_VERSIONS =
            List.of("latest", "4.0.3", "3.5.x", "3.4.x", "3.3.x");

    private final HttpClient httpClient;
    private final URI metadataUri;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile JsonNode cachedMetadata;

    public InitializrMetadataClient() {
        this(URI.create("https://start.spring.io"));
    }

    public InitializrMetadataClient(URI baseUri) {
        this.metadataUri = baseUri;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /** Returns available Java version IDs (e.g. "21", "17"). Falls back to defaults. */
    public List<String> fetchJavaVersions() {
        try {
            JsonNode root = getMetadata();
            List<String> versions = new ArrayList<>();
            for (JsonNode v : root.path("javaVersion").path("values")) {
                String id = v.path("id").asText(null);
                if (id != null && !id.isBlank()) {
                    versions.add(id);
                }
            }
            return versions.isEmpty() ? DEFAULT_JAVA_VERSIONS : versions;
        } catch (Exception e) {
            log.fine("Could not fetch Java versions from Initializr: " + e.getMessage());
            return DEFAULT_JAVA_VERSIONS;
        }
    }

    /**
     * Returns available Spring Boot version IDs prepended with "latest".
     * "latest" maps to omitting the bootVersion param, using the Initializr default.
     * Falls back to defaults.
     */
    public List<String> fetchBootVersions() {
        try {
            JsonNode root = getMetadata();
            List<String> versions = new ArrayList<>();
            versions.add("latest");
            for (JsonNode v : root.path("bootVersion").path("values")) {
                String id = v.path("id").asText(null);
                if (id != null && !id.isBlank()) {
                    versions.add(id);
                }
            }
            return versions.size() == 1 ? DEFAULT_BOOT_VERSIONS : versions;
        } catch (Exception e) {
            log.fine("Could not fetch Boot versions from Initializr: " + e.getMessage());
            return DEFAULT_BOOT_VERSIONS;
        }
    }

    /**
     * Fetches the full dependency catalog grouped by category.
     * Returns an empty list on failure (never null).
     */
    public List<DependencyEntry> fetchDependencies() {
        try {
            JsonNode root = getMetadata();
            List<DependencyEntry> entries = new ArrayList<>();
            for (JsonNode group : root.path("dependencies").path("values")) {
                String groupName = group.path("name").asText("Other");
                for (JsonNode dep : group.path("values")) {
                    String id = dep.path("id").asText(null);
                    String name = dep.path("name").asText(id);
                    String description = dep.path("description").asText("");
                    if (id != null && !id.isBlank()) {
                        entries.add(new DependencyEntry(id, name, description, groupName));
                    }
                }
            }
            return entries;
        } catch (Exception e) {
            log.fine("Could not fetch dependencies from Initializr: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Convenience method that fetches dependencies and wraps them in a {@link DependencyCatalog}.
     */
    public DependencyCatalog fetchDependencyCatalog() {
        return new DependencyCatalog(fetchDependencies());
    }

    private JsonNode getMetadata() throws Exception {
        JsonNode cached = this.cachedMetadata;
        if (cached != null) {
            return cached;
        }
        cached = fetchMetadataFromNetwork();
        this.cachedMetadata = cached;
        return cached;
    }

    private JsonNode fetchMetadataFromNetwork() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(metadataUri)
                .GET()
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Metadata API returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }
}

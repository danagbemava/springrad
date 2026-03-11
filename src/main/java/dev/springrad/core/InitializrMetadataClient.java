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
 * Fetches available Java and Spring Boot versions from the Spring Initializr metadata API.
 * Falls back to a sensible default list when the network is unavailable.
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
            JsonNode root = fetchMetadata();
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
            JsonNode root = fetchMetadata();
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

    private JsonNode fetchMetadata() throws Exception {
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

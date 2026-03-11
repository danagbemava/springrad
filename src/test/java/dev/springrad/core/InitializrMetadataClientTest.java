package dev.springrad.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InitializrMetadataClientTest {

    private HttpServer server;
    private InitializrMetadataClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        client = new InitializrMetadataClient(URI.create("http://localhost:" + port));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchDependencies_parsesGroupedEntries() {
        String json = """
                {
                  "dependencies": {
                    "type": "hierarchical-multi-select",
                    "values": [
                      {
                        "name": "Web",
                        "values": [
                          {"id": "web", "name": "Spring Web", "description": "Build web apps"},
                          {"id": "webflux", "name": "Spring Reactive Web", "description": "Reactive web"}
                        ]
                      },
                      {
                        "name": "SQL",
                        "values": [
                          {"id": "data-jpa", "name": "Spring Data JPA", "description": "JPA support"}
                        ]
                      }
                    ]
                  },
                  "javaVersion": {"values": [{"id": "21"}]},
                  "bootVersion": {"values": [{"id": "3.4.1"}]}
                }
                """;
        serveJson(json);
        server.start();

        List<DependencyEntry> deps = client.fetchDependencies();
        assertEquals(3, deps.size());
        assertEquals("web", deps.get(0).id());
        assertEquals("Spring Web", deps.get(0).name());
        assertEquals("Web", deps.get(0).group());
        assertEquals("data-jpa", deps.get(2).id());
        assertEquals("SQL", deps.get(2).group());
    }

    @Test
    void fetchDependencyCatalog_returnsWorkingCatalog() {
        String json = """
                {
                  "dependencies": {
                    "values": [
                      {
                        "name": "Web",
                        "values": [
                          {"id": "web", "name": "Spring Web", "description": "Build web apps"}
                        ]
                      }
                    ]
                  },
                  "javaVersion": {"values": []},
                  "bootVersion": {"values": []}
                }
                """;
        serveJson(json);
        server.start();

        DependencyCatalog catalog = client.fetchDependencyCatalog();
        assertFalse(catalog.isEmpty());
        assertTrue(catalog.isValid("web"));
        assertFalse(catalog.isValid("unknown"));
    }

    @Test
    void fetchDependencies_fallsBackOnError() {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });
        server.start();

        List<DependencyEntry> deps = client.fetchDependencies();
        assertTrue(deps.isEmpty());
    }

    @Test
    void fetchDependencies_skipsEntriesWithNullId() {
        String json = """
                {
                  "dependencies": {
                    "values": [
                      {
                        "name": "Web",
                        "values": [
                          {"id": "web", "name": "Spring Web", "description": "ok"},
                          {"name": "No ID", "description": "should be skipped"}
                        ]
                      }
                    ]
                  },
                  "javaVersion": {"values": []},
                  "bootVersion": {"values": []}
                }
                """;
        serveJson(json);
        server.start();

        List<DependencyEntry> deps = client.fetchDependencies();
        assertEquals(1, deps.size());
        assertEquals("web", deps.get(0).id());
    }

    @Test
    void metadataIsCached_singleNetworkCall() {
        int[] callCount = {0};
        String json = """
                {
                  "dependencies": {"values": [{"name": "Web", "values": [{"id": "web", "name": "Spring Web", "description": ""}]}]},
                  "javaVersion": {"values": [{"id": "21"}]},
                  "bootVersion": {"values": [{"id": "3.4.1"}]}
                }
                """;
        server.createContext("/", exchange -> {
            callCount[0]++;
            byte[] body = json.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        client.fetchJavaVersions();
        client.fetchBootVersions();
        client.fetchDependencies();

        assertEquals(1, callCount[0], "Metadata should be fetched only once");
    }

    private void serveJson(String json) {
        server.createContext("/", exchange -> {
            byte[] body = json.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }
}

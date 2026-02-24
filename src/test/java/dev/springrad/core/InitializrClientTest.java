package dev.springrad.core;

import com.sun.net.httpserver.HttpServer;
import dev.springrad.cli.CliArgs;
import dev.springrad.preset.Preset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitializrClientTest {
    @TempDir
    Path tempDir;

    @Test
    void queryParametersAreAssembledCorrectly() throws Exception {
        AtomicReference<String> queryRef = new AtomicReference<>();
        byte[] zip = zipBytes(new Entry("README.md", "# demo"));
        HttpServer server = startServer(exchange -> {
            queryRef.set(exchange.getRequestURI().getQuery());
            writeResponse(exchange, 200, zip);
        });

        try {
            InitializrClient client = new InitializrClient(serverUri(server));
            client.generate(sampleConfig(tempDir.resolve("generated")));
            String query = queryRef.get();
            assertTrue(query.contains("groupId=dev.example"));
            assertTrue(query.contains("artifactId=demo-app"));
            assertTrue(query.contains("type=gradle-project"));
            assertTrue(
                    query.contains("dependencies=web%2Csecurity") || query.contains("dependencies=web,security"),
                    "Unexpected query: " + query
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void okResponseExtractsZipToOutputDirectory() throws Exception {
        Path output = tempDir.resolve("generated");
        byte[] zip = zipBytes(
                new Entry("README.md", "# demo"),
                new Entry("src/main/resources/application.yml", "spring:\n  application:\n    name: demo")
        );
        HttpServer server = startServer(exchange -> writeResponse(exchange, 200, zip));
        try {
            InitializrClient client = new InitializrClient(serverUri(server));
            GenerationResult result = client.generate(sampleConfig(output));
            assertEquals(output, result.outputPath());
            assertTrue(Files.exists(output.resolve("README.md")));
            assertTrue(result.topLevelEntries().contains("README.md"));
            assertTrue(result.topLevelEntries().contains("src"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void non200ThrowsInitializrApiException() throws Exception {
        HttpServer server = startServer(exchange ->
                writeResponse(exchange, 503, "service unavailable".getBytes(StandardCharsets.UTF_8))
        );
        try {
            InitializrClient client = new InitializrClient(serverUri(server));
            InitializrApiException exception = assertThrows(
                    InitializrApiException.class,
                    () -> client.generate(sampleConfig(tempDir.resolve("generated")))
            );
            assertEquals(503, exception.getStatusCode());
            assertTrue(exception.getResponseBody().contains("service unavailable"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void gradlewIsExecutableAfterExtraction() throws Exception {
        Path output = tempDir.resolve("generated");
        byte[] zip = zipBytes(new Entry("gradlew", "#!/bin/sh\necho ok"));
        HttpServer server = startServer(exchange -> writeResponse(exchange, 200, zip));
        try {
            InitializrClient client = new InitializrClient(serverUri(server));
            client.generate(sampleConfig(output));
            assertTrue(Files.isExecutable(output.resolve("gradlew")));
        } finally {
            server.stop(0);
        }
    }

    private static URI serverUri(HttpServer server) {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }

    private static HttpServer startServer(Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/starter.zip", exchange -> {
            try (exchange) {
                handler.handle(exchange);
            }
        });
        server.start();
        return server;
    }

    private static void writeResponse(com.sun.net.httpserver.HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static byte[] zipBytes(Entry... entries) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(byteStream)) {
            for (Entry entry : entries) {
                ZipEntry zipEntry = new ZipEntry(entry.path());
                zos.putNextEntry(zipEntry);
                zos.write(entry.content().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return byteStream.toByteArray();
    }

    private static ProjectConfig sampleConfig(Path output) {
        return ProjectConfig.merge(
                Preset.named("test"),
                new CliArgs(
                        "demo",
                        "dev.example",
                        "demo-app",
                        "21",
                        "3.4.1",
                        ProjectConfig.Packaging.jar,
                        ProjectConfig.BuildTool.gradle,
                        ProjectConfig.AuthStyle.jwt,
                        ProjectConfig.Database.postgresql,
                        List.of("web", "security"),
                        List.of(),
                        output
                )
        );
    }

    private record Entry(String path, String content) {
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}

package org.modsync.server;

import org.modsync.Manifest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManifestEndpointTest {

    @TempDir
    Path jarDir;

    private HttpServerRunner runner;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws IOException {
        runner = new HttpServerRunner("127.0.0.1", 0, jarDir, testManifest());
        runner.start();
    }

    @AfterEach
    void stopServer() {
        runner.stop();
    }

    @Test
    void getReturns200WithJsonContentType() throws Exception {
        var response = get("/manifest.json");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"),
                "content-type should be application/json");
    }

    @Test
    void responseBodyContainsManifestFields() throws Exception {
        var response = get("/manifest.json");
        String body = response.body();

        assertTrue(body.contains("\"pack_name\""), "body should contain pack_name");
        assertTrue(body.contains("\"TestPack\""), "body should contain pack name value");
        assertTrue(body.contains("\"pack_version\""), "body should contain pack_version");
        assertTrue(body.contains("\"minecraft_version\""), "body should contain minecraft_version");
        assertTrue(body.contains("\"fabric\""), "body should contain loader type");
    }

    @Test
    void postReturns405() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(manifestUri())
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    @Test
    void putReturns405() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(manifestUri())
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + runner.port() + path))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI manifestUri() {
        return URI.create("http://127.0.0.1:" + runner.port() + "/manifest.json");
    }

    private static Manifest testManifest() {
        return new Manifest(
                "TestPack", "deadbeef", "1.21.1",
                new Manifest.Loader("fabric", "0.16.0"),
                List.of(new Manifest.ModEntry("mod.jar", "abc123", "http://example.com/jars/mod.jar")));
    }
}

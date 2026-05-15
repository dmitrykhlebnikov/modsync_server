package org.modsync.server;

import org.modsync.Config;
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

class LandingPageTest {

    @TempDir Path jarDir;
    @TempDir Path staticDir;

    private HttpServerRunner runner;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws IOException {
        Config config = new Config("127.0.0.1", 0, "http://127.0.0.1:9999",
                jarDir.toString(), staticDir.toString(), "CoolPack", "1.21.1", "fabric", "0.16.0");
        Manifest manifest = new Manifest("CoolPack", "abc", "1.21.1",
                new Manifest.Loader("fabric", "0.16.0"), List.of());
        runner = new HttpServerRunner(config, manifest);
        runner.start();
    }

    @AfterEach
    void stopServer() {
        runner.stop();
    }

    @Test
    void rootReturns200WithHtmlContentType() throws Exception {
        var response = get("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("text/html"));
    }

    @Test
    void bodyContainsPackName() throws Exception {
        assertTrue(get("/").body().contains("CoolPack"));
    }

    @Test
    void bodyContainsManifestUrl() throws Exception {
        assertTrue(get("/").body().contains("/manifest.json"));
    }

    @Test
    void bodyContainsExeDownloadLink() throws Exception {
        assertTrue(get("/").body().contains("modsync.exe"));
    }

    @Test
    void noUnsubstitutedPlaceholders() throws Exception {
        String body = get("/").body();
        assertFalse(body.contains("{{"), "body should have no unreplaced {{}} placeholders");
    }

    @Test
    void postReturns405() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(uri("/"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder().uri(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + runner.port() + path);
    }
}

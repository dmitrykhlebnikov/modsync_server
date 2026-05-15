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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticEndpointTest {

    @TempDir Path staticDir;
    @TempDir Path jarDir;

    private HttpServerRunner runner;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws IOException {
        Files.write(staticDir.resolve("modsync.exe"), "fake exe content".getBytes());
        Manifest manifest = new Manifest("TestPack", "abc", "1.21.1",
                new Manifest.Loader("fabric", "0.16.0"), List.of());
        runner = new HttpServerRunner("127.0.0.1", 0, jarDir, staticDir, manifest);
        runner.start();
    }

    @AfterEach
    void stopServer() {
        runner.stop();
    }

    @Test
    void exeFileReturns200WithOctetStreamContentType() throws Exception {
        var response = get("/modsync.exe");

        assertEquals(200, response.statusCode());
        assertEquals("application/octet-stream",
                response.headers().firstValue("content-type").orElse(""));
    }

    @Test
    void exeFileHasAttachmentContentDisposition() throws Exception {
        var response = get("/modsync.exe");

        String disposition = response.headers().firstValue("content-disposition").orElse("");
        assertTrue(disposition.contains("attachment"), "should be attachment");
        assertTrue(disposition.contains("modsync.exe"), "should include filename");
    }

    @Test
    void exeBodyMatchesDiskFile() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder().uri(uri("/modsync.exe")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        byte[] expected = Files.readAllBytes(staticDir.resolve("modsync.exe"));
        assertArrayEquals(expected, response.body());
    }

    @Test
    void contentLengthMatchesFileSize() throws Exception {
        var response = get("/modsync.exe");
        long expectedSize = Files.size(staticDir.resolve("modsync.exe"));

        assertEquals(expectedSize,
                response.headers().firstValueAsLong("content-length").orElse(-1));
    }

    @Test
    void unknownFileReturns404() throws Exception {
        var response = get("/notfound.exe");

        assertEquals(404, response.statusCode());
    }

    @Test
    void postReturns405() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(uri("/modsync.exe"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    private HttpResponse<Void> get(String path) throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder().uri(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + runner.port() + path);
    }
}

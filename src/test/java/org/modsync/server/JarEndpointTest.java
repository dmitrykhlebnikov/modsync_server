package org.modsync.server;

import org.modsync.Config;
import org.modsync.Manifest;
import org.modsync.ManifestBuilder;
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

import static org.junit.jupiter.api.Assertions.*;

class JarEndpointTest {

    @TempDir Path jarDir;
    @TempDir Path staticDir;

    private HttpServerRunner runner;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @BeforeEach
    void startServer() throws IOException {
        Files.write(jarDir.resolve("sodium.jar"), "fake sodium content".getBytes());
        Config config = new Config("127.0.0.1", 0, "http://127.0.0.1:0",
                jarDir.toString(), staticDir.toString(), "TestPack", "1.21.1", "fabric", "0.16.0");
        Manifest manifest = ManifestBuilder.scan(jarDir, config);
        runner = new HttpServerRunner(config, manifest);
        runner.start();
    }

    @AfterEach
    void stopServer() {
        runner.stop();
    }

    @Test
    void knownJarReturns200WithJavaArchiveContentType() throws Exception {
        var response = getJar("sodium.jar");

        assertEquals(200, response.statusCode());
        assertEquals("application/java-archive",
                response.headers().firstValue("content-type").orElse(""));
    }

    @Test
    void knownJarBodyMatchesDiskFile() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder().uri(jarUri("sodium.jar")).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        byte[] expected = Files.readAllBytes(jarDir.resolve("sodium.jar"));
        assertArrayEquals(expected, response.body());
    }

    @Test
    void contentLengthMatchesFileSize() throws Exception {
        var response = getJar("sodium.jar");
        long expectedSize = Files.size(jarDir.resolve("sodium.jar"));

        assertEquals(expectedSize,
                response.headers().firstValueAsLong("content-length").orElse(-1));
    }

    @Test
    void unknownJarReturns404() throws Exception {
        var response = getJar("nonexistent.jar");

        assertEquals(404, response.statusCode());
    }

    @Test
    void filenameNotInManifestReturns404() throws Exception {
        // File exists on disk but is not in the manifest (added after scan)
        Files.write(jarDir.resolve("unlisted.jar"), new byte[]{1});

        var response = getJar("unlisted.jar");

        assertEquals(404, response.statusCode());
    }

    @Test
    void postReturns405() throws Exception {
        var response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(jarUri("sodium.jar"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals(405, response.statusCode());
    }

    private HttpResponse<Void> getJar(String filename) throws Exception {
        return CLIENT.send(
                HttpRequest.newBuilder().uri(jarUri(filename)).GET().build(),
                HttpResponse.BodyHandlers.discarding());
    }

    private URI jarUri(String filename) {
        return URI.create("http://127.0.0.1:" + runner.port() + "/jars/" + filename);
    }
}

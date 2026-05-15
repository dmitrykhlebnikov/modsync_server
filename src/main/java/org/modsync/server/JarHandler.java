package org.modsync.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.modsync.Manifest;
import org.modsync.util.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

class JarHandler implements HttpHandler {

    private static final String PREFIX = "/jars/";

    private final Path jarDir;
    private final Set<String> allowedFilenames;

    JarHandler(Path jarDir, Manifest manifest) {
        this.jarDir = jarDir;
        this.allowedFilenames = manifest.mods().stream()
                .map(Manifest.ModEntry::filename)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, 0);
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.length() > PREFIX.length()
                ? requestPath.substring(PREFIX.length())
                : "";

        if (!allowedFilenames.contains(filename)) {
            send(exchange, 404, 0);
            return;
        }

        Path jarPath = PathSafety.resolve(jarDir, filename);
        if (jarPath == null || !Files.exists(jarPath)) {
            send(exchange, 404, 0);
            return;
        }

        long size = Files.size(jarPath);
        exchange.getResponseHeaders().set("Content-Type", "application/java-archive");
        exchange.sendResponseHeaders(200, size);
        try (var out = exchange.getResponseBody()) {
            Files.copy(jarPath, out);
        }
        Logging.logRequest(exchange, 200, size);
    }

    private static void send(HttpExchange exchange, int status, long bytes) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
        Logging.logRequest(exchange, status, bytes);
    }
}

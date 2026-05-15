package org.modsync.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.modsync.util.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class StaticHandler implements HttpHandler {

    private final Path staticDir;

    StaticHandler(Path staticDir) {
        this.staticDir = staticDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405);
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        String filename = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;

        Path filePath = PathSafety.resolve(staticDir, filename);
        if (filePath == null || !Files.exists(filePath)) {
            send(exchange, 404);
            return;
        }

        long size = Files.size(filePath);
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        if (filename.endsWith(".exe")) {
            exchange.getResponseHeaders().set("Content-Disposition",
                    "attachment; filename=\"" + filename + "\"");
        }
        exchange.sendResponseHeaders(200, size);
        try (var out = exchange.getResponseBody()) {
            Files.copy(filePath, out);
        }
        Logging.logRequest(exchange, 200, size);
    }

    private static void send(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
        Logging.logRequest(exchange, status, 0);
    }
}

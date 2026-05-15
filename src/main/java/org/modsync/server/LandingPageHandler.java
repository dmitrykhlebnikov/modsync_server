package org.modsync.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.modsync.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class LandingPageHandler implements HttpHandler {

    private final byte[] body;

    LandingPageHandler(String baseUrl, String packName) {
        String manifestUrl = baseUrl + "/manifest.json";
        String html = loadTemplate()
                .replace("{{base_url}}", baseUrl)
                .replace("{{manifest_url}}", manifestUrl)
                .replace("{{pack_name}}", packName);
        this.body = html.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            Logging.logRequest(exchange, 405, 0);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
        Logging.logRequest(exchange, 200, body.length);
    }

    private static String loadTemplate() {
        try (InputStream in = LandingPageHandler.class.getResourceAsStream("/landing.html.template")) {
            if (in == null) throw new RuntimeException("landing.html.template not found in classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load landing.html.template", e);
        }
    }
}

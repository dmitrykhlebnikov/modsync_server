package org.modsync.server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.modsync.Manifest;
import org.modsync.util.Logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class ManifestHandler implements HttpHandler {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final byte[] body;

    ManifestHandler(Manifest manifest) {
        this.body = GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            Logging.logRequest(exchange, 405, 0);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
        Logging.logRequest(exchange, 200, body.length);
    }
}

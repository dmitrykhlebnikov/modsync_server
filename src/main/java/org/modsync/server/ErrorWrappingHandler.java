package org.modsync.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

class ErrorWrappingHandler implements HttpHandler {

    private static final int MAX_REQUEST_BODY_DRAIN = 4096;

    private final HttpHandler delegate;

    ErrorWrappingHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        drainRequestBody(exchange);
        try {
            delegate.handle(exchange);
        } catch (Exception e) {
            System.err.printf("[ERROR] %s %s — %s: %s%n",
                    exchange.getRequestMethod(), exchange.getRequestURI(),
                    e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace(System.err);
            try {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            } catch (Exception ignored) {
                // headers already sent; nothing we can do
            }
        }
    }

    private static void drainRequestBody(HttpExchange exchange) {
        try (var body = exchange.getRequestBody()) {
            //noinspection ResultOfMethodCallIgnored
            body.readNBytes(MAX_REQUEST_BODY_DRAIN);
        } catch (IOException ignored) {
            // best-effort
        }
    }
}

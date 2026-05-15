package org.modsync.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ErrorWrappingHandlerTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @Test
    void exceptionInHandlerReturns500() throws Exception {
        HttpServer server = startServer(exchange -> {
            throw new RuntimeException("intentional failure");
        });
        try {
            var response = CLIENT.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.discarding());

            assertEquals(500, response.statusCode());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ioExceptionInHandlerReturns500() throws Exception {
        HttpServer server = startServer(exchange -> {
            throw new java.io.IOException("simulated I/O failure");
        });
        try {
            var response = CLIENT.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.discarding());

            assertEquals(500, response.statusCode());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void successfulHandlerPassesThroughUnaffected() throws Exception {
        HttpServer server = startServer(exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        try {
            var response = CLIENT.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"))
                            .GET().build(),
                    HttpResponse.BodyHandlers.discarding());

            assertEquals(200, response.statusCode());
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(com.sun.net.httpserver.HttpHandler delegate) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(null);
        server.createContext("/", new ErrorWrappingHandler(delegate));
        server.start();
        return server;
    }
}

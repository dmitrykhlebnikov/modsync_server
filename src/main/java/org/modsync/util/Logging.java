package org.modsync.util;

import com.sun.net.httpserver.HttpExchange;

import java.time.Instant;

public class Logging {

    public static void logRequest(HttpExchange exchange, int status, long bytes) {
        System.out.printf("[%s] %s %s %s %d %d%n",
                Instant.now(),
                exchange.getRemoteAddress(),
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                status,
                bytes);
    }
}

package org.modsync.server;

import com.sun.net.httpserver.HttpServer;
import org.modsync.Config;
import org.modsync.Manifest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class HttpServerRunner {

    private final HttpServer server;

    public HttpServerRunner(Config config, Manifest manifest) throws IOException {
        Path jarDir = Path.of(config.jarDirectory());
        Path staticDir = Path.of(config.staticDirectory());

        server = HttpServer.create(new InetSocketAddress(config.bindAddress(), config.port()), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/manifest.json", wrap(new ManifestHandler(manifest)));
        server.createContext("/jars/", wrap(new JarHandler(jarDir, manifest)));

        LandingPageHandler landing = new LandingPageHandler(config.baseUrl(), manifest.packName());
        StaticHandler staticFiles = new StaticHandler(staticDir);
        server.createContext("/", wrap(exchange -> {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                landing.handle(exchange);
            } else {
                staticFiles.handle(exchange);
            }
        }));
    }

    public void start() {
        server.start();
    }

    private static ErrorWrappingHandler wrap(com.sun.net.httpserver.HttpHandler h) {
        return new ErrorWrappingHandler(h);
    }

    public void stop() {
        server.stop(2);
    }

    public int port() {
        return server.getAddress().getPort();
    }
}

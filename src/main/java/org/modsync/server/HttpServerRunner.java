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
        server.createContext("/manifest.json", new ManifestHandler(manifest));
        server.createContext("/jars/", new JarHandler(jarDir, manifest));

        LandingPageHandler landing = new LandingPageHandler(config.baseUrl(), manifest.packName());
        StaticHandler staticFiles = new StaticHandler(staticDir);
        server.createContext("/", exchange -> {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                landing.handle(exchange);
            } else {
                staticFiles.handle(exchange);
            }
        });
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(2);
    }

    public int port() {
        return server.getAddress().getPort();
    }
}

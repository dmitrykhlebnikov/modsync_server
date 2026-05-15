package org.modsync.server;

import com.sun.net.httpserver.HttpServer;
import org.modsync.Manifest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class HttpServerRunner {

    private final HttpServer server;

    public HttpServerRunner(String bindAddress, int port, Path jarDir, Manifest manifest) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/manifest.json", new ManifestHandler(manifest));
        server.createContext("/jars/", new JarHandler(jarDir, manifest));
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

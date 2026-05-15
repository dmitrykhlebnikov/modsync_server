package org.modsync;

import org.modsync.server.HttpServerRunner;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path configPath = parseConfigArg(args);
        Config config = Config.load(configPath);

        Manifest manifest = ManifestBuilder.scan(Path.of(config.jarDirectory()), config);
        System.out.println("Loaded " + manifest.mods().size() + " mod(s), pack_version=" + manifest.packVersion());

        HttpServerRunner runner = new HttpServerRunner(config, manifest);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> runner.stop()));
        runner.start();

        System.out.println("Listening on " + config.bindAddress() + ":" + config.port());
        System.out.println("Manifest: " + config.baseUrl() + "/manifest.json");
    }

    static Path parseConfigArg(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) {
                return Path.of(args[i + 1]);
            }
        }
        return Path.of("config.json");
    }
}
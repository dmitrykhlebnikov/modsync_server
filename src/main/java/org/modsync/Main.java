package org.modsync;

import org.modsync.server.HttpServerRunner;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path configPath = parseConfigArg(args);
        Config config = Config.load(configPath);

        Manifest manifest = ManifestBuilder.scan(Path.of(config.jarDirectory()), config);

        HttpServerRunner runner = new HttpServerRunner(config, manifest);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> runner.stop()));
        runner.start();

        String base = config.baseUrl();
        System.out.println("pack:      " + manifest.packName() + "  (" + manifest.mods().size() + " mods)");
        System.out.println("version:   " + manifest.packVersion());
        System.out.println("listening: " + config.bindAddress() + ":" + config.port());
        System.out.println("landing:   " + base + "/");
        System.out.println("manifest:  " + base + "/manifest.json");
        System.out.println("exe:       " + base + "/modsync.exe");
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
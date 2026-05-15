package org.example;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path configPath = parseConfigArg(args);
        Config config = Config.load(configPath);
        System.out.println("bind_address:      " + config.bindAddress());
        System.out.println("port:              " + config.port());
        System.out.println("base_url:          " + config.baseUrl());
        System.out.println("jar_directory:     " + config.jarDirectory());
        System.out.println("static_directory:  " + config.staticDirectory());
        System.out.println("pack_name:         " + config.packName());
        System.out.println("minecraft_version: " + config.minecraftVersion());
        System.out.println("loader_type:       " + config.loaderType());
        System.out.println("loader_version:    " + config.loaderVersion());
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
package org.modsync;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path configPath = parseConfigArg(args);
        Config config = Config.load(configPath);

        Manifest manifest = ManifestBuilder.scan(Path.of(config.jarDirectory()), config);
        System.out.println("Loaded " + manifest.mods().size() + " mod(s), pack_version=" + manifest.packVersion());

        String json = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting()
                .create()
                .toJson(manifest);
        System.out.println(json);
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
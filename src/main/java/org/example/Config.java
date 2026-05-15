package org.example;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record Config(
        String bindAddress,
        int port,
        String baseUrl,
        String jarDirectory,
        String staticDirectory,
        String packName,
        String minecraftVersion,
        String loaderType,
        String loaderVersion
) {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static Config load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IllegalArgumentException("Config file not found: " + configPath);
        }
        Raw raw = GSON.fromJson(Files.readString(configPath), Raw.class);
        validate(raw);
        return new Config(
                raw.bindAddress != null ? raw.bindAddress : "0.0.0.0",
                raw.port > 0 ? raw.port : 8080,
                raw.baseUrl,
                raw.jarDirectory,
                raw.staticDirectory,
                raw.packName,
                raw.minecraftVersion,
                raw.loaderType,
                raw.loaderVersion
        );
    }

    private static void validate(Raw raw) {
        requireField("base_url", raw.baseUrl);
        requireField("jar_directory", raw.jarDirectory);
        requireField("static_directory", raw.staticDirectory);
        requireField("pack_name", raw.packName);
        requireField("minecraft_version", raw.minecraftVersion);
        requireField("loader_type", raw.loaderType);
        requireField("loader_version", raw.loaderVersion);
    }

    private static void requireField(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required config field missing: " + name);
        }
    }

    private static class Raw {
        String bindAddress;
        int port;
        String baseUrl;
        String jarDirectory;
        String staticDirectory;
        String packName;
        String minecraftVersion;
        String loaderType;
        String loaderVersion;
    }
}

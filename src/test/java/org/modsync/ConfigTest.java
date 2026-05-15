package org.modsync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsAllFields() throws IOException {
        Path f = writeConfig(tempDir, """
                {
                  "bind_address": "127.0.0.1",
                  "port": 9090,
                  "base_url": "http://example.com:9090",
                  "jar_directory": "/mods",
                  "static_directory": "/static",
                  "pack_name": "TestPack",
                  "minecraft_version": "1.21.1",
                  "loader_type": "fabric",
                  "loader_version": "0.16.0"
                }
                """);

        Config config = Config.load(f);

        assertEquals("127.0.0.1", config.bindAddress());
        assertEquals(9090, config.port());
        assertEquals("http://example.com:9090", config.baseUrl());
        assertEquals("/mods", config.jarDirectory());
        assertEquals("/static", config.staticDirectory());
        assertEquals("TestPack", config.packName());
        assertEquals("1.21.1", config.minecraftVersion());
        assertEquals("fabric", config.loaderType());
        assertEquals("0.16.0", config.loaderVersion());
    }

    @Test
    void defaultsForOptionalFields() throws IOException {
        Path f = writeConfig(tempDir, """
                {
                  "base_url": "http://example.com:8080",
                  "jar_directory": "/mods",
                  "static_directory": "/static",
                  "pack_name": "TestPack",
                  "minecraft_version": "1.21.1",
                  "loader_type": "neoforge",
                  "loader_version": "21.1.0"
                }
                """);

        Config config = Config.load(f);

        assertEquals("0.0.0.0", config.bindAddress());
        assertEquals(8080, config.port());
    }

    @Test
    void throwsWithFileNameWhenFileMissing() {
        Path missing = tempDir.resolve("missing.json");

        var ex = assertThrows(IllegalArgumentException.class, () -> Config.load(missing));
        assertTrue(ex.getMessage().contains("missing.json"), ex.getMessage());
    }

    @Test
    void throwsWithFieldNameWhenRequiredFieldAbsent() throws IOException {
        Path f = writeConfig(tempDir, """
                {
                  "base_url": "http://example.com:8080",
                  "jar_directory": "/mods",
                  "static_directory": "/static",
                  "pack_name": "TestPack",
                  "minecraft_version": "1.21.1",
                  "loader_type": "fabric"
                }
                """);

        var ex = assertThrows(IllegalArgumentException.class, () -> Config.load(f));
        assertTrue(ex.getMessage().contains("loader_version"), ex.getMessage());
    }

    private static Path writeConfig(Path dir, String json) throws IOException {
        Path f = dir.resolve("config.json");
        Files.writeString(f, json);
        return f;
    }
}

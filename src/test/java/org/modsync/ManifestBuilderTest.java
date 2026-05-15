package org.modsync;

import org.modsync.util.Hashing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ManifestBuilderTest {

    @TempDir
    Path tempDir;

    private Config config() {
        return new Config("0.0.0.0", 8080, "http://example.com:8080",
                tempDir.toString(), "/static", "TestPack",
                "1.21.1", "fabric", "0.16.0");
    }

    @Test
    void emptyDirectoryProducesNoMods() throws IOException {
        Manifest manifest = ManifestBuilder.scan(tempDir, config());

        assertTrue(manifest.mods().isEmpty());
    }

    @Test
    void scansAllJarFiles() throws IOException {
        Files.write(tempDir.resolve("alpha.jar"), new byte[]{1});
        Files.write(tempDir.resolve("beta.jar"), new byte[]{2});

        Manifest manifest = ManifestBuilder.scan(tempDir, config());

        assertEquals(2, manifest.mods().size());
    }

    @Test
    void ignoresNonJarFiles() throws IOException {
        Files.writeString(tempDir.resolve("README.txt"), "nope");
        Files.write(tempDir.resolve("mod.jar"), new byte[]{1});

        Manifest manifest = ManifestBuilder.scan(tempDir, config());

        assertEquals(1, manifest.mods().size());
        assertEquals("mod.jar", manifest.mods().get(0).filename());
    }

    @Test
    void modEntryHasCorrectHashAndUrl() throws IOException {
        Files.write(tempDir.resolve("mod.jar"), "fake jar content".getBytes());
        String expectedHash = Hashing.sha256(tempDir.resolve("mod.jar"));

        Manifest manifest = ManifestBuilder.scan(tempDir, config());
        Manifest.ModEntry entry = manifest.mods().get(0);

        assertEquals("mod.jar", entry.filename());
        assertEquals(expectedHash, entry.sha256());
        assertEquals("http://example.com:8080/jars/mod.jar", entry.url());
    }

    @Test
    void modsAreSortedByFilename() throws IOException {
        Files.write(tempDir.resolve("zzz.jar"), new byte[]{1});
        Files.write(tempDir.resolve("aaa.jar"), new byte[]{2});

        Manifest manifest = ManifestBuilder.scan(tempDir, config());

        assertEquals("aaa.jar", manifest.mods().get(0).filename());
        assertEquals("zzz.jar", manifest.mods().get(1).filename());
    }

    @Test
    void packVersionChangesWhenModContentChanges() throws IOException {
        Files.write(tempDir.resolve("mod.jar"), "v1".getBytes());
        Manifest m1 = ManifestBuilder.scan(tempDir, config());

        Files.write(tempDir.resolve("mod.jar"), "v2".getBytes());
        Manifest m2 = ManifestBuilder.scan(tempDir, config());

        assertNotEquals(m1.packVersion(), m2.packVersion());
    }

    @Test
    void packVersionIsDeterministic() throws IOException {
        Files.write(tempDir.resolve("mod.jar"), "content".getBytes());

        assertEquals(
                ManifestBuilder.scan(tempDir, config()).packVersion(),
                ManifestBuilder.scan(tempDir, config()).packVersion());
    }

    @Test
    void manifestMetadataComesFromConfig() throws IOException {
        Manifest manifest = ManifestBuilder.scan(tempDir, config());

        assertEquals("TestPack", manifest.packName());
        assertEquals("1.21.1", manifest.minecraftVersion());
        assertEquals("fabric", manifest.loader().type());
        assertEquals("0.16.0", manifest.loader().version());
    }
}

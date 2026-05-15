package org.modsync.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashingTest {

    @TempDir
    Path tempDir;

    @Test
    void sha256OfKnownContent() throws IOException {
        Path file = tempDir.resolve("test.bin");
        Files.writeString(file, "hello");

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                Hashing.sha256(file));
    }

    @Test
    void sha256OfEmptyFile() throws IOException {
        Path file = tempDir.resolve("empty.bin");
        Files.write(file, new byte[0]);

        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Hashing.sha256(file));
    }
}

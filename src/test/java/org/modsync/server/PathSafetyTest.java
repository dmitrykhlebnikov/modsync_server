package org.modsync.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathSafetyTest {

    @TempDir
    Path baseDir;

    @Test
    void normalFilenameResolvesInsideBase() {
        Path result = PathSafety.resolve(baseDir, "mod.jar");
        assertNotNull(result);
        assertEquals(baseDir.resolve("mod.jar").normalize(), result);
    }

    @Test
    void dotDotSequenceIsRejected() {
        assertNull(PathSafety.resolve(baseDir, "../etc/passwd"));
    }

    @Test
    void embeddedDotDotIsRejected() {
        assertNull(PathSafety.resolve(baseDir, "mods/../secret"));
    }

    @Test
    void forwardSlashIsRejected() {
        assertNull(PathSafety.resolve(baseDir, "sub/mod.jar"));
    }

    @Test
    void backslashIsRejected() {
        assertNull(PathSafety.resolve(baseDir, "sub\\mod.jar"));
    }

    @Test
    void nullByteIsRejected() {
        assertNull(PathSafety.resolve(baseDir, "mod\0.jar"));
    }

    @Test
    void emptyNameIsRejected() {
        assertNull(PathSafety.resolve(baseDir, ""));
    }
}

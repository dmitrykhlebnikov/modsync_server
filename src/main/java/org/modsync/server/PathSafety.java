package org.modsync.server;

import java.nio.file.Path;

public class PathSafety {

    /**
     * Resolves requestedName against baseDir only if the name is safe.
     * Returns null if the name is blank, contains traversal sequences (..),
     * path separators (/ or \), or null bytes, or if the resolved path
     * escapes the base directory.
     */
    public static Path resolve(Path baseDir, String requestedName) {
        if (requestedName == null || requestedName.isBlank()) return null;
        if (requestedName.contains("..")) return null;
        if (requestedName.contains("/")) return null;
        if (requestedName.contains("\\")) return null;
        if (requestedName.contains("\0")) return null;

        Path resolved = baseDir.resolve(requestedName).normalize();
        if (!resolved.startsWith(baseDir.normalize())) return null;

        return resolved;
    }
}

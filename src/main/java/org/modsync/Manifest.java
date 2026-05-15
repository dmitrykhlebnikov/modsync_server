package org.modsync;

import java.util.List;

public record Manifest(
        String packName,
        String packVersion,
        String minecraftVersion,
        Loader loader,
        List<ModEntry> mods
) {
    public record Loader(String type, String version) {}

    public record ModEntry(String filename, String sha256, String url) {}
}

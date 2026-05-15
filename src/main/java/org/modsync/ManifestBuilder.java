package org.modsync;

import org.modsync.util.Hashing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ManifestBuilder {

    public static Manifest scan(Path jarDir, Config config) throws IOException {
        List<Manifest.ModEntry> mods;
        try (var stream = Files.list(jarDir)) {
            mods = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> toEntry(p, config.baseUrl()))
                    .collect(Collectors.toList());
        }

        String packVersion = computePackVersion(mods);

        return new Manifest(
                config.packName(),
                packVersion,
                config.minecraftVersion(),
                new Manifest.Loader(config.loaderType(), config.loaderVersion()),
                mods
        );
    }

    private static Manifest.ModEntry toEntry(Path jarPath, String baseUrl) {
        try {
            String filename = jarPath.getFileName().toString();
            String hash = Hashing.sha256(jarPath);
            String url = baseUrl + "/jars/" + filename;
            return new Manifest.ModEntry(filename, hash, url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to hash " + jarPath, e);
        }
    }

    private static String computePackVersion(List<Manifest.ModEntry> mods) {
        String combined = mods.stream()
                .map(Manifest.ModEntry::sha256)
                .collect(Collectors.joining());
        return Hashing.sha256(combined.getBytes(StandardCharsets.UTF_8));
    }
}

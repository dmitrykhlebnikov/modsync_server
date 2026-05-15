# modsync-server

A lightweight HTTP server that runs alongside a Minecraft server and distributes a mod pack to players. It scans a folder of mod jars at startup, builds a content-addressed manifest, and serves the manifest, jars, and a client executable over plain HTTP. No Minecraft integration required — it is a standalone Java program.

The companion client (`modsync.exe`) reads the manifest and syncs the player's local mods folder to match.

## How it works

1. On startup the server hashes every `.jar` in `jar_directory` and builds a manifest JSON containing each mod's filename, SHA-256, and download URL, plus a `pack_version` fingerprint of the whole set.
2. It serves four things over HTTP:
   - `GET /manifest.json` — the manifest
   - `GET /jars/<filename>` — individual mod jars
   - `GET /modsync.exe` — the client executable (from `static_directory`)
   - `GET /` — a landing page with copy-paste instructions for new players
3. The client fetches the manifest, compares `pack_version` to what it last synced, downloads only the jars that differ, and deletes jars that are no longer in the pack.

The server never writes to disk after startup and requires a restart to pick up jar changes.

## Requirements

- Java 21

## Building

```bash
./gradlew build
```

Produces a runnable distribution:

```bash
./gradlew installDist
# executable at build/install/modpacksend/bin/modpacksend (or .bat on Windows)
```

## Configuration

Create a `config.json` next to the executable:

```json
{
  "base_url":          "http://your-dyndns.example.com:8080",
  "jar_directory":     "C:/minecraft-server/mods",
  "static_directory":  "C:/minecraft-server/modsync",
  "pack_name":         "My Modpack",
  "minecraft_version": "1.21.1",
  "loader_type":       "fabric",
  "loader_version":    "0.16.0"
}
```

| Field | Required | Default | Description |
|---|---|---|---|
| `base_url` | yes | — | Public URL clients will use to reach this server |
| `jar_directory` | yes | — | Folder containing the mod `.jar` files to serve |
| `static_directory` | yes | — | Folder containing `modsync.exe` and other static assets |
| `pack_name` | yes | — | Display name shown on the landing page |
| `minecraft_version` | yes | — | e.g. `"1.21.1"` |
| `loader_type` | yes | — | `"fabric"` or `"neoforge"` |
| `loader_version` | yes | — | e.g. `"0.16.0"` |
| `bind_address` | no | `0.0.0.0` | Interface to bind to |
| `port` | no | `8080` | Port to listen on |

## Running

```bash
# default config.json in the working directory
./gradlew run

# explicit config path
./gradlew run --args="--config /path/to/config.json"

# from the installed distribution
build/install/modpacksend/bin/modpacksend --config config.json
```

Startup output:

```
pack:      My Modpack  (12 mods)
version:   3a7f2c...
listening: 0.0.0.0:8080
landing:   http://your-server:8080/
manifest:  http://your-server:8080/manifest.json
exe:       http://your-server:8080/modsync.exe
```

Stop with Ctrl-C — the server shuts down gracefully.

## Giving the URL to players

Point players to the landing page (`base_url + /`). It shows the manifest URL in a copy-paste block and a download link for `modsync.exe`.

Players run:

```
modsync.exe http://your-server:8080/manifest.json
```

## Updating the pack

Stop the server, add or remove jars from `jar_directory`, then restart. The manifest and `pack_version` are recomputed from scratch on every startup.

## Running tests

```bash
./gradlew test
```

## Client protocol

See [PROTOCOL.md](PROTOCOL.md) for the full HTTP API reference, the `pack_version` algorithm, and the recommended client sync loop.

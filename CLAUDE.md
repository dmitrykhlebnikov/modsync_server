# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`modpacksend` (also called `modsync-server`) is a standalone Java HTTP server that runs alongside a Minecraft server on Windows. It scans a directory of mod jars, generates a content-addressed manifest, and serves the manifest + jars + `modsync.exe` + a landing page over HTTP. No Minecraft integration — pure HTTP file server.

## Commands

```bash
# Build
./gradlew build

# Run (requires a config.json)
./gradlew run --args="--config config.json"

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.modsync.PathSafetyTest"

# Produce a runnable distribution
./gradlew installDist
# Then run: build/install/modpacksend/bin/modpacksend --config config.json
```

## Tech Choices

- **Java 21** — virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`)
- **`com.sun.net.httpserver.HttpServer`** — stdlib HTTP, no framework
- **Gson** — JSON for config and manifest serialization
- **JUnit 5 (Jupiter)** — testing
- **Gradle** with `application` plugin

## Planned Architecture

The implementation plan lives in `modsync-server-plan.md`. Target package structure under `src/main/java/<pkg>/`:

```
Main.java                  # entry point: parse args, load config, wire up
Config.java                # config record + loader (from config.json)
ManifestBuilder.java       # scan jar dir, compute SHA-256, build Manifest
Manifest.java              # data classes: Manifest, ModEntry
server/
  HttpServerRunner.java    # creates HttpServer, registers handlers, starts
  ManifestHandler.java     # GET /manifest.json
  JarHandler.java          # GET /jars/<filename>
  StaticHandler.java       # GET /modsync.exe and other static assets
  LandingPageHandler.java  # GET /
  PathSafety.java          # path-traversal guard used by Jar+Static handlers
util/
  Hashing.java             # streaming SHA-256 (don't load jars into memory)
  Logging.java             # request logger: timestamp, remote addr, method, path, status, bytes
src/main/resources/
  landing.html.template    # uses {{base_url}}, {{manifest_url}}, {{pack_name}} placeholders
```

## Config (`config.json`)

Required fields: `bind_address`, `port`, `base_url`, `jar_directory`, `static_directory`, `pack_name`, `minecraft_version`, `loader_type` (`fabric`|`neoforge`), `loader_version`. Fail loudly if any required field is missing.

## Key Invariants

- **`PathSafety`**: reject `..`, `/`, `\`, null bytes; resolve + normalize; verify result is still inside the base directory. Used by both `JarHandler` and `StaticHandler`.
- **`JarHandler` allowlist**: only serve filenames present in the current manifest — prevents serving arbitrary files even if path safety is bypassed.
- **Manifest `pack_version`**: a hex hash-of-hashes over all mod entries — clients skip re-download if it matches.
- **Manifest is built once at startup** and held in memory. Restart the server to pick up jar changes.

## Testing Approach

- Unit tests for `Hashing`, `PathSafety`, `ManifestBuilder` (temp directory with fake jars).
- Integration tests: start server on an ephemeral port, exercise each endpoint with `java.net.http.HttpClient`, verify status codes, content types, hashes.

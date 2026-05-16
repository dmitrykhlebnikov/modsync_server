# Interactive Shell Design

**Date:** 2026-05-16
**Status:** Approved

## Overview

When the server launches it opens an interactive shell on stdin. The shell runs on the main thread, blocking it (which keeps the JVM alive). The existing Ctrl-C shutdown hook is retained. Three commands are supported: `/stop`, `/status`, `/reload`.

## Architecture

```
Main.java
  └─ starts HttpServerRunner
  └─ creates ConsoleShell(runner, config, manifestRef)
  └─ calls shell.run()   ← blocks main thread

ConsoleShell (org.modsync.shell.ConsoleShell)
  └─ BufferedReader loop on System.in
  └─ Map<String, Runnable> commands
  └─ /stop   → runner.stop(), return
  └─ /status → print server info from runner + manifest
  └─ /reload → ManifestBuilder.scan(), runner.updateManifest(m), print result

HttpServerRunner
  └─ AtomicReference<Manifest> manifestRef  (was: final Manifest)
  └─ updateManifest(Manifest m) swaps the ref
  └─ handlers read manifestRef.get() per request

Handlers updated to use AtomicReference<Manifest>:
  ManifestHandler, JarHandler
  (LandingPageHandler is unaffected — it takes packName from config, not manifest)
```

## Components

### ConsoleShell

- Constructor: `ConsoleShell(HttpServerRunner runner, Config config, AtomicReference<Manifest> manifestRef)`
- `run()`: prints `> `, reads a line, dispatches to command map, loops
- EOF on stdin: calls `runner.stop()` and returns (same effect as `/stop`)
- Unknown input: prints `Unknown command. Commands: /stop /status /reload`

### Commands

| Command   | Behaviour |
|-----------|-----------|
| `/stop`   | Calls `runner.stop()`, exits the loop |
| `/status` | Prints bind address, port, pack name, mod count, manifest version |
| `/reload` | Calls `ManifestBuilder.scan(jarDir, config)`, swaps manifest via `runner.updateManifest()`, prints new mod count and version |

### AtomicReference<Manifest> swap (for /reload)

`HttpServerRunner` replaces its `final Manifest manifest` field with `AtomicReference<Manifest> manifestRef`. A new method `updateManifest(Manifest m)` calls `manifestRef.set(m)`. All three affected handlers receive the `AtomicReference` at construction and call `.get()` on each request — lock-free and safe under concurrent HTTP traffic.

## Error Handling

- `/reload` IOException: print error, continue loop, keep serving old manifest
- stdin EOF: treat as `/stop` — stop server and exit cleanly
- `> ` prompt: flushed explicitly with `System.out.flush()` before each read

## Files Changed

| File | Change |
|------|--------|
| `Main.java` | Create `ConsoleShell`, call `shell.run()` after `runner.start()` |
| `server/HttpServerRunner.java` | Replace `Manifest` field with `AtomicReference<Manifest>`, add `updateManifest()` |
| `server/ManifestHandler.java` | Hold `AtomicReference<Manifest>`, call `.get()` in handler |
| `server/JarHandler.java` | Hold `AtomicReference<Manifest>`, call `.get()` in handler |
| `shell/ConsoleShell.java` | New class |

## Testing

- No new unit tests required (ManifestBuilder and AtomicReference are already covered).
- Existing integration tests unaffected (they don't interact with stdin).
- Manual smoke test: launch server, type `/status`, `/reload`, `/stop`; verify each produces correct output and behaviour.

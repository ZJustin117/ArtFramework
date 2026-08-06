# Android Arthas diagnostics

Optional maintainer tooling for diagnosing the SlayTheAmethyst Android JVM while developing ArtFramework. Arthas, connector forwarding, and device serials are development infrastructure; they are not ArtFramework runtime configuration or release dependencies.

## Scope

- Arthas diagnoses JVM threads, class loading, decompilation, method arguments/results, and call timing.
- ArtFramework `art probe`, `art ui`, and `art op` diagnose UI state and UI operations.
- JUnit remains the semantic/API gate. `tools/art-verify` remains the UI fixture gate.
- Multiplayer protocol, party/combat authority, and dual-device life scenarios are out of scope.

The `@android-arthas` subagent is read-only. It accepts one bounded diagnosis, uses `start -> query -> stop`, and attempts bridge cleanup after a failed query. It does not start or stop the connector daemon, enter an interactive shell, edit source, push jars, or run out-of-repo scenarios.

## Environment

Values belong in gitignored `.env.local`; shared documents contain names only.

| Key | Role | Default |
|-----|------|---------|
| `SLAY_THE_AMETHYST_ROOT` | Amethyst checkout and Python import root | required |
| `ART_AMETHYST_TOOLS_DIR` | Amethyst tools directory | `$SLAY_THE_AMETHYST_ROOT/scripts/tools` |
| `STS_CONNECTOR_PORT` | Connector daemon port | required |
| `ART_D1_SERIAL` | Default target device | required for default D1 diagnosis |
| `ART_D2_SERIAL` | Optional second target | only when explicitly requested |
| `ART_GAME_PROBE_PORT` | Game-probe agent port | `9099` |
| `ART_ARTHAS_PORT` | Arthas bridge port | `8099` |

ArtFramework commands use only the `ART_*` names. Do not put real paths, serials, or ports in production code or committed documentation.

## Topology

```text
Developer machine
├── ADB serial $ART_D1_SERIAL -> D1
└── connector daemon :$STS_CONNECTOR_PORT
    ├── game-probe :$ART_GAME_PROBE_PORT -> LOAD_AGENT
    └── arthas bridge :$ART_ARTHAS_PORT -> query stream
```

The connector must already be running. Its lifecycle is outside this subagent and may be shared with the ArtFramework single-device lab. The Arthas port is device-side forwarding and is unrelated to any game network port.

## Preconditions

1. The game is running in a debug-compatible mode and game-probe can accept `LOAD_AGENT`.
2. The connector daemon is already online on `$STS_CONNECTOR_PORT`.
3. A target device is explicit, preferably `--device "$ART_D1_SERIAL"`.
4. The Arthas tooling is available below `$ART_AMETHYST_TOOLS_DIR`.

Check connector status from the Amethyst root:

```bash
PYTHONPATH="$SLAY_THE_AMETHYST_ROOT${PYTHONPATH:+:$PYTHONPATH}" \
  python3 -m scripts.tools.connector status --port "$STS_CONNECTOR_PORT"
```

Do not start or stop the connector from the diagnostic subagent. If status fails, report the connector blocker and stop.

## Quick start

Run from the ArtFramework repository root, with the Amethyst root on `PYTHONPATH`:

```bash
export DEVICE="$ART_D1_SERIAL"
export PYTHONPATH="$SLAY_THE_AMETHYST_ROOT${PYTHONPATH:+:$PYTHONPATH}"

python3 -m scripts.tools.arthas --device "$DEVICE" start
python3 -m scripts.tools.arthas --device "$DEVICE" query "version"
python3 -m scripts.tools.arthas --device "$DEVICE" stop
```

The CLI may also accept explicit forwarding ports:

```bash
python3 -m scripts.tools.arthas \
  --device "$DEVICE" \
  --agent-port "$ART_GAME_PROBE_PORT" \
  --arthas-port "$ART_ARTHAS_PORT" \
  start
```

Use `@android-arthas` for bounded non-interactive diagnosis. The subagent should always attempt `stop` after `start`, including when `query` fails. Use the manual `shell` command only for a separately approved maintainer session.

## Useful queries

Thread and runtime overview:

```bash
python3 -m scripts.tools.arthas --device "$DEVICE" query "thread -n 5"
python3 -m scripts.tools.arthas --device "$DEVICE" query "dashboard"
```

For ModTheSpire-loaded classes, inspect the classloader first:

```bash
python3 -m scripts.tools.arthas --device "$DEVICE" query \
  "sc -d artframework.ArtFrameworkMod"
# Replace <hash> with sc output's classLoaderHash.
python3 -m scripts.tools.arthas --device "$DEVICE" query \
  "jad -c <hash> artframework.ArtFrameworkMod"
```

Method observation and timing are appropriate for a narrow investigation:

```bash
python3 -m scripts.tools.arthas --device "$DEVICE" query \
  "watch artframework.api.ArtFramework open '{params,returnObj}' -x 2"
python3 -m scripts.tools.arthas --device "$DEVICE" query \
  "trace artframework.api.ArtFramework open"
```

Do not use `retransform`, `redefine`, `heapdump`, `jfr`, `profiler`, or arbitrary side-effecting `ognl` through the subagent. Those require a separate manual diagnostic session and explicit review.

## Troubleshooting

| Symptom | Check |
|---------|-------|
| Multiple devices online | Pass the intended `--device` explicitly; do not rely on auto-selection. |
| Connector refused / status unavailable | Confirm the daemon and `$STS_CONNECTOR_PORT`; the subagent does not manage its lifecycle. |
| `LOAD_AGENT` unavailable | Confirm debug mode, game-probe readiness, and the target serial. |
| `already bind` | A bridge may already be loaded; cold-start the game, then run `start` again. |
| `connect_stream` broken pipe | The bridge stopped; cold-start the game and restart the lifecycle. |
| `Type xxx not present` | The first class retransformation may not be ready; retry on the same serial after a clean start. |
| `ognl` returns `null` | A void method may have been queried; use a method with a return value. |

## OpenCode subagent

Invoke `@android-arthas` only for a specific JVM diagnosis. The agent definition is `.opencode/agent/android-arthas.md`. It requires the ArtFramework environment names, uses explicit D1/D2 serials, and returns findings without editing files.

After changing the agent or `.opencode/plugins/local-env.ts`, restart OpenCode so the definition and environment whitelist reload.

## Boundary

Arthas is not shipped inside `ArtFramework.jar`, and this repository does not own the bridge runtime. The bridge and its supporting files remain in `$ART_AMETHYST_TOOLS_DIR/arthas` provided by the SlayTheAmethyst tooling repository.

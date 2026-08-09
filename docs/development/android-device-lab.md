# ArtFramework Android device lab (SlayTheAmethyst)

Amethyst test-bed for **single-device UI** checks.
**Not** dual-device life / co-op.

Shared docs use env **names** only. Values live in gitignored `.env.local`.

## Topology (D1 only by default)

```text
开发机
├── ADB serial $ART_D1_SERIAL → D1
├── connector daemon 127.0.0.1:$STS_CONNECTOR_PORT
└── sts-harness → game-probe :$ART_GAME_PROBE_PORT → BaseMod DevConsole
         → art probe | art op … | art ui …

设备
└── mods_library/ArtFramework.jar
```

| Key | Role |
|-----|------|
| `SLAY_THE_AMETHYST_ROOT` | Amethyst checkout (import root for `scripts.tools`) |
| `ART_AMETHYST_TOOLS_DIR` | Optional; default `$SLAY_THE_AMETHYST_ROOT/scripts/tools` |
| `STS_CONNECTOR_PORT` | Connector daemon |
| `ART_D1_SERIAL` | ADB serial |
| `ART_GAME_PROBE_PORT` | Device game-probe (default `9099`) |
| `ART_ARTHAS_PORT` | Optional device-side Arthas bridge (default `8099`) |
| `ART_HARNESS_OUT_DIR` | Absolute dir for harness `result.json` (gitignored) |
| `ART_UI_VERIFY_OUT_DIR` | Optional art-verify JSON out |

Set these names directly in ArtFramework's gitignored `.env.local`. Keep shared machine-specific values out of committed docs.

## Load env

```bash
set -a && source .env.local && set +a
# or: OpenCode local-env plugin (restart opencode after plugin changes)
```

## Entry points (workdir rules)

| Action | Workdir | Command |
|--------|---------|---------|
| connector start/status | **`$SLAY_THE_AMETHYST_ROOT`** | `python3 -m scripts.tools.connector start --port "$STS_CONNECTOR_PORT"` |
| sts-harness | **ArtFramework repo root** | `python3 "$ART_AMETHYST_TOOLS_DIR/main.py" sts-harness ...` |
| jar build/push | ArtFramework root | `./scripts/with-art-env.sh jar` then `@android-deploy-jar` or manual push |
| UI YAML | ArtFramework root | `python3 tools/art-verify/run.py tests/ui-scenarios/... --device` |
| Arthas query | ArtFramework root (`PYTHONPATH` = Amethyst) | `python3 -m scripts.tools.arthas --device "$ART_D1_SERIAL" …` (see [`android-arthas.md`](./android-arthas.md)) |

Do **not** set harness tool workdir to Amethyst root for `main.py` invocations from ArtFramework (Amethyst resolves its own repo_root from the tools path). Connector **must** run with `PYTHONPATH`/`cwd` = Amethyst root so `scripts.tools` imports.

## Common D1 commands

Use `scripts/art-lab` from the ArtFramework root for the common bounded D1
flow. It validates the device and connector status, invokes Harness with its
required parameters, and reports the newest Harness `result.json` rather than
relying only on the process exit status.

```bash
scripts/art-lab ready
scripts/art-lab status
scripts/art-lab console "art probe"
scripts/art-lab combat verify-full
scripts/art-lab stop
```

`ready` starts `mts_basemod` with game-probe enabled and polls until Harness
observes `READY`. `combat verify-full` reuses
`d1_full_present_combat_ready.yaml`: ART lab navigation, BaseMod `fight
Cultist`, and `FULL_READY` probe assertions remain in the evidence-producing
scenario. The wrapper never starts, stops, or restarts the shared connector.
Arthas remains on its native CLI because its start/query/cleanup choice is a
diagnostic decision, not a device-lab default.

## Concurrent D1 operations

D1 commands are not serialized by ArtFramework. Do not run overlapping
deployment, Harness, `art-verify`, or Arthas operations against the same
device unless their interaction is intentional.

## Bring-up checklist (UI smoke)

1. ADB: `adb connect` / `adb -s "$ART_D1_SERIAL" get-state` → `device`
2. `.env.local` loaded (keys above)
3. Connector (once per session):

```bash
cd "$SLAY_THE_AMETHYST_ROOT"
python3 -m scripts.tools.connector start --port "$STS_CONNECTOR_PORT"
python3 -m scripts.tools.connector status --port "$STS_CONNECTOR_PORT"
```

4. Build + push `ArtFramework.jar` → `mods_library/ArtFramework.jar`  
   (`@android-deploy-jar` or [`android-deploy.md`](./android-deploy.md))
5. **Enable the mod** on Amethyst (jar in `mods_library` alone is not enough):

```bash
./scripts/ensure-art-enabled-mods.sh
```

   Writes `sts/enabled_mods.txt` with **only** ArtFramework enabled.
   Then `am force-stop io.stamethyst`. Confirm next cold start log contains `artframework (0.2.0)` and `ArtFramework: demo + native templates`.
6. Cold start with game-probe:

```bash
test -n "$ART_HARNESS_OUT_DIR"
python3 "${ART_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command start -LaunchMode mts_basemod -DebugMode \
  -DeviceSerial "$ART_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$ART_HARNESS_OUT_DIR"
python3 "${ART_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command status -DeviceSerial "$ART_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$ART_HARNESS_OUT_DIR"
```

Wait until status is **READY** (not merely “start accepted”).

7. Console UI check:

```bash
python3 "${ART_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command console \
  -DeviceSerial "$ART_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$ART_HARNESS_OUT_DIR" \
  -ConsoleCommand "art probe"
# also: art ui list | art ui tree demo | art open demo …
# lab run nav (fresh menu / embark): art lab dump | ensure-fresh-menu | start-run IRONCLAD
```

game-probe often returns only `ok`; scrape device log for `ART_PROBE` / `ART_UI` / `ART_LAB`  
(`/sdcard/Android/data/io.stamethyst/files/sts/latest.log`) — `tools/art-verify` does this automatically.  
Command reference: [`console-commands.md`](./console-commands.md).  
Lab nav design: [`lab-run-nav.md`](../design/lab-run-nav.md).

8. YAML:

```bash
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_probe_smoke.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_present_profiles_catalog.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_present_packs.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lightwave_demo.yaml --device
# lab navigation:
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_dump.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_ensure_fresh_menu.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_start_run.yaml --device
# full-present policy (after jar deploy + cold start READY):
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_observe.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_combat_on.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_combat_ready.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_map.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_map_ready.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_lifecycle.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_panic.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_event.yaml --device
```

## Mod isolation

- Art lab `enabled_mods.txt` enables **ArtFramework only**.
- Do **not** use ArtFramework lab for dual host/join life.
- The connector daemon is external tooling; this repo does not own its lifecycle.

## OpenCode order

`junit-test` → `android-deploy-jar` → connector up → `@android-harness` start / console / stop → `@art-verify` device. Use `@android-arthas` separately for a bounded JVM diagnosis; it does not manage connector lifecycle or replace UI verification.

## Related

- [`ui-layer-verification.md`](./ui-layer-verification.md)
- [`android-arthas.md`](./android-arthas.md)
- [`android-deploy.md`](./android-deploy.md)

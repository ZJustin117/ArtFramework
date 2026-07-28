# ArtFramework Android device lab (SlayTheAmethyst)

Mirror of CrossSpire’s Amethyst test-bed for **single-device UI** checks.  
**Not** dual-device life / co-op (that stays in CrossSpire).

Shared docs use env **names** only. Values live in gitignored `.env.local`.

## Topology (same as CrossSpire, D1 only by default)

```text
开发机
├── ADB serial $ART_D1_SERIAL → D1
├── connector daemon 127.0.0.1:$STS_CONNECTOR_PORT
└── sts-harness → game-probe :$ART_GAME_PROBE_PORT → BaseMod DevConsole
         → art probe | art op … | art ui …

设备
└── mods_library/ArtFramework.jar  (+ optional CrossSpire.jar for co-load)
```

| Key | Role |
|-----|------|
| `SLAY_THE_AMETHYST_ROOT` | Amethyst checkout (import root for `scripts.tools`) |
| `ART_AMETHYST_TOOLS_DIR` | Optional; default `$SLAY_THE_AMETHYST_ROOT/scripts/tools` |
| `STS_CONNECTOR_PORT` | Connector daemon (same name as CrossSpire / Amethyst) |
| `ART_D1_SERIAL` | ADB serial (same physical device as CrossSpire D1 when mirrored) |
| `ART_GAME_PROBE_PORT` | Device game-probe (default `9099`) |
| `ART_HARNESS_OUT_DIR` | Absolute dir for harness `result.json` (gitignored) |
| `ART_UI_VERIFY_OUT_DIR` | Optional art-verify JSON out |

Same machine as CrossSpire: copy serial / connector / tools paths from CrossSpire `.env.local`, but **rename** `CROSSSPIRE_D1_SERIAL` → `ART_D1_SERIAL`, etc. Keep `STS_CONNECTOR_PORT` and `SLAY_THE_AMETHYST_ROOT` shared.

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

Do **not** set harness tool workdir to Amethyst root for `main.py` invocations from ArtFramework (Amethyst resolves its own repo_root from the tools path). Connector **must** run with `PYTHONPATH`/`cwd` = Amethyst root so `scripts.tools` imports.

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

   Writes `sts/enabled_mods.txt` with ArtFramework before CrossSpire when both jars exist.  
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
# lab navigation:
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_dump.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_ensure_fresh_menu.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_lab_start_run.yaml --device
# full-present policy (after jar deploy + cold start READY):
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_observe.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_combat_on.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_map.yaml --device
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_full_present_panic.yaml --device
```

## Coexistence with CrossSpire

- Both jars in `mods_library`; MTS loads `basemod` → `artframework` → `crossspire`.
- Do **not** use ArtFramework lab for dual host/join life; use CrossSpire `@device-scenario life`.
- Same connector port/daemon can serve both repos if serials match.

## OpenCode order

`junit-test` → `android-deploy-jar` → connector up → harness start → `@art-verify` device.

## Related

- CrossSpire: `docs/development/android-harness.md` (full dual-device)
- [`ui-layer-verification.md`](./ui-layer-verification.md)
- [`android-deploy.md`](./android-deploy.md)

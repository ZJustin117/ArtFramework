# SpireUI Android device lab (SlayTheAmethyst)

Mirror of CrossSpire’s Amethyst test-bed for **single-device UI** checks.  
**Not** dual-device life / co-op (that stays in CrossSpire).

Shared docs use env **names** only. Values live in gitignored `.env.local`.

## Topology (same as CrossSpire, D1 only by default)

```text
开发机
├── ADB serial $SPIREUI_D1_SERIAL → D1
├── connector daemon 127.0.0.1:$STS_CONNECTOR_PORT
└── sts-harness → game-probe :$SPIREUI_GAME_PROBE_PORT → BaseMod DevConsole
         → spireui probe | spireui op …

设备
└── mods_library/SpireUI.jar  (+ optional CrossSpire.jar for co-load)
```

| Key | Role |
|-----|------|
| `SLAY_THE_AMETHYST_ROOT` | Amethyst checkout (import root for `scripts.tools`) |
| `SPIREUI_AMETHYST_TOOLS_DIR` | Optional; default `$SLAY_THE_AMETHYST_ROOT/scripts/tools` |
| `STS_CONNECTOR_PORT` | Connector daemon (same name as CrossSpire / Amethyst) |
| `SPIREUI_D1_SERIAL` | ADB serial (same physical device as CrossSpire D1 when mirrored) |
| `SPIREUI_GAME_PROBE_PORT` | Device game-probe (default `9099`) |
| `SPIREUI_HARNESS_OUT_DIR` | Absolute dir for harness `result.json` (gitignored) |
| `SPIREUI_UI_VERIFY_OUT_DIR` | Optional ui-verify JSON out |

Same machine as CrossSpire: copy serial / connector / tools paths from CrossSpire `.env.local`, but **rename** `CROSSSPIRE_D1_SERIAL` → `SPIREUI_D1_SERIAL`, etc. Keep `STS_CONNECTOR_PORT` and `SLAY_THE_AMETHYST_ROOT` shared.

## Load env

```bash
set -a && source .env.local && set +a
# or: OpenCode local-env plugin (restart opencode after plugin changes)
```

## Entry points (workdir rules)

| Action | Workdir | Command |
|--------|---------|---------|
| connector start/status | **`$SLAY_THE_AMETHYST_ROOT`** | `python3 -m scripts.tools.connector start --port "$STS_CONNECTOR_PORT"` |
| sts-harness | **SpireUI repo root** | `python3 "$SPIREUI_AMETHYST_TOOLS_DIR/main.py" sts-harness ...` |
| jar build/push | SpireUI root | `./scripts/with-env.sh jar` then `@android-deploy-jar` or manual push |
| UI YAML | SpireUI root | `python3 tools/ui-verify/run.py tests/ui-scenarios/... --device` |

Do **not** set harness tool workdir to Amethyst root for `main.py` invocations from SpireUI (Amethyst resolves its own repo_root from the tools path). Connector **must** run with `PYTHONPATH`/`cwd` = Amethyst root so `scripts.tools` imports.

## Bring-up checklist (UI smoke)

1. ADB: `adb connect` / `adb -s "$SPIREUI_D1_SERIAL" get-state` → `device`
2. `.env.local` loaded (keys above)
3. Connector (once per session):

```bash
cd "$SLAY_THE_AMETHYST_ROOT"
python3 -m scripts.tools.connector start --port "$STS_CONNECTOR_PORT"
python3 -m scripts.tools.connector status --port "$STS_CONNECTOR_PORT"
```

4. Build + push `SpireUI.jar` → `mods_library/SpireUI.jar`  
   (`@android-deploy-jar` or [`android-deploy.md`](./android-deploy.md))
5. **Enable the mod** on Amethyst (jar in `mods_library` alone is not enough):

```bash
./scripts/ensure-enabled-mods.sh
```

   Writes `sts/enabled_mods.txt` with SpireUI before CrossSpire when both jars exist.  
   Then `am force-stop io.stamethyst`. Confirm next cold start log contains `spireui (0.2.0)` and `SpireUI: demo + native templates`.
6. Cold start with game-probe:

```bash
test -n "$SPIREUI_HARNESS_OUT_DIR"
python3 "${SPIREUI_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command start -LaunchMode mts_basemod -DebugMode \
  -DeviceSerial "$SPIREUI_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$SPIREUI_HARNESS_OUT_DIR"
python3 "${SPIREUI_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command status -DeviceSerial "$SPIREUI_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$SPIREUI_HARNESS_OUT_DIR"
```

Wait until status is **READY** (not merely “start accepted”).

7. Console UI check:

```bash
python3 "${SPIREUI_AMETHYST_TOOLS_DIR:-$SLAY_THE_AMETHYST_ROOT/scripts/tools}/main.py" sts-harness \
  -Command console \
  -DeviceSerial "$SPIREUI_D1_SERIAL" \
  -ConnectorPort "$STS_CONNECTOR_PORT" \
  -OutDir "$SPIREUI_HARNESS_OUT_DIR" \
  -ConsoleCommand "spireui probe"
```

game-probe often returns only `ok`; scrape device log for `SPIREUI_PROBE`  
(`/sdcard/Android/data/io.stamethyst/files/sts/latest.log`) — `tools/ui-verify` does this automatically.

8. YAML:

```bash
python3 tools/ui-verify/run.py tests/ui-scenarios/device/d1_probe_smoke.yaml --device
```

## Coexistence with CrossSpire

- Both jars in `mods_library`; MTS loads `basemod` → `spireui` → `crossspire`.
- Do **not** use SpireUI lab for dual host/join life; use CrossSpire `@device-scenario life`.
- Same connector port/daemon can serve both repos if serials match.

## OpenCode order

`junit-test` → `android-deploy-jar` → connector up → harness start → `@ui-verify` device.

## Related

- CrossSpire: `docs/development/android-harness.md` (full dual-device)
- [`ui-layer-verification.md`](./ui-layer-verification.md)
- [`android-deploy.md`](./android-deploy.md)

# SpireUI Android jar deploy

Optional path for on-device UI checks. **Not** the semantic default gate (use JUnit / `@junit-test` first).

## Env

| Key | Required | Role |
|-----|----------|------|
| `SPIREUI_STS_JAR` / `SPIREUI_BASEMOD_JAR` / `SPIREUI_MODTHESPIRE_JAR` | build | Same as unit test |
| `SPIREUI_D1_SERIAL` | push | Default single device |
| `SPIREUI_D2_SERIAL` | optional | Only when dual deploy explicitly requested |

Serials may match CrossSpire device serials; use **`SPIREUI_*` key names** in this repo’s `.env.local`.

## Paths

| | |
|--|--|
| Local | `build/libs/SpireUI.jar` |
| Remote | `/sdcard/Android/data/io.stamethyst/files/sts/mods_library/SpireUI.jar` |
| App id | `io.stamethyst` |

Build:

```bash
./scripts/with-env.sh jar
```

Push pattern (D1):

```bash
adb -s "$SPIREUI_D1_SERIAL" shell mkdir -p /sdcard/Android/data/io.stamethyst/files/sts/mods_library
adb -s "$SPIREUI_D1_SERIAL" push build/libs/SpireUI.jar \
  /sdcard/Android/data/io.stamethyst/files/sts/mods_library/SpireUI.jar
adb -s "$SPIREUI_D1_SERIAL" shell am force-stop io.stamethyst
```

Prefer OpenCode `@android-deploy-jar` so size verification and failure modes stay consistent.

## Coexistence with CrossSpire

- Remote filename is **`SpireUI.jar`**, not `CrossSpire.jar` — both can sit in `mods_library`.
- ModTheSpire load order / consumer `dependencies` on `spireui` is a product integration step (roadmap item 5), not part of this deploy script.

## After deploy

1. Ensure `sts/enabled_mods.txt` includes `…/mods_library/SpireUI.jar` (Amethyst will not load optional jars otherwise). Prefer SpireUI **before** CrossSpire when both present.
2. Cold start via harness (`-DebugMode`), not only SkipInstall — see [`android-device-lab.md`](./android-device-lab.md).
3. UI-layer checks: [`ui-layer-verification.md`](./ui-layer-verification.md) / `@ui-verify` device YAML.

## Out of scope

- Dual host/join, CrossSpire life YAML, connector daemon, Arthas
- Using deploy as a substitute for failing pure API tests

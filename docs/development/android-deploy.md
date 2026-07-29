# ArtFramework Android jar deploy

Optional path for on-device UI checks. **Not** the semantic default gate (use JUnit / `@junit-test` first).

## Env

| Key | Required | Role |
|-----|----------|------|
| `ART_STS_JAR` / `ART_BASEMOD_JAR` / `ART_MODTHESPIRE_JAR` | build | Same as unit test |
| `ART_D1_SERIAL` | push | Default single device |
| `ART_D2_SERIAL` | optional | Only when dual deploy explicitly requested |

Serials may match CrossSpire device serials; use **`ART_*` key names** in this repo’s `.env.local`.

## Paths

| | |
|--|--|
| Local | `build/libs/ArtFramework.jar` |
| Remote | `/sdcard/Android/data/io.stamethyst/files/sts/mods_library/ArtFramework.jar` |
| App id | `io.stamethyst` |

Build:

```bash
./scripts/with-art-env.sh jar
```

Push pattern (D1):

```bash
adb -s "$ART_D1_SERIAL" shell mkdir -p /sdcard/Android/data/io.stamethyst/files/sts/mods_library
adb -s "$ART_D1_SERIAL" push build/libs/ArtFramework.jar \
  /sdcard/Android/data/io.stamethyst/files/sts/mods_library/ArtFramework.jar
adb -s "$ART_D1_SERIAL" shell am force-stop io.stamethyst
```

Prefer OpenCode `@android-deploy-jar` so size verification and failure modes stay consistent.

## Coexistence with CrossSpire

- Remote filename is **`ArtFramework.jar`**, not `CrossSpire.jar` — both can sit in `mods_library`.
- ModTheSpire load order / consumer `dependencies` on `artframework` is a product integration step (roadmap item 5), not part of this deploy script.

## After deploy

1. Ensure `sts/enabled_mods.txt` includes `…/mods_library/ArtFramework.jar` only (Amethyst will not load optional jars otherwise). Art lab does **not** enable CrossSpire.
2. Cold start via harness (`-DebugMode`), not only SkipInstall — see [`android-device-lab.md`](./android-device-lab.md).
3. UI-layer checks: [`ui-layer-verification.md`](./ui-layer-verification.md) / `@art-verify` device YAML.

## Out of scope

- Dual host/join, CrossSpire life YAML, connector daemon, Arthas
- Using deploy as a substitute for failing pure API tests

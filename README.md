# SpireUI

BaseMod-oriented **UI toolkit** for Slay the Spire 1 (Desktop ModTheSpire + Amethyst-compatible jars).

- **Separate repository** from multiplayer logic (e.g. CrossSpire) — no protocol/party types here.
- **Dual track** (see [`docs/design/dual-track.md`](docs/design/dual-track.md)):
  1. **C1 Synthetic** — scene2d.ui windows (layout DSL + StageHost + StsSkin)
  2. **C2 Native templates** — map/event/select/end-turn hooks + `EntityPresent`

Open work: [`docs/task.md`](docs/task.md). Consumer integration: [`docs/development/consumer.md`](docs/development/consumer.md).  
UiOps/probe design: [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md). UI verify: [`docs/development/ui-layer-verification.md`](docs/development/ui-layer-verification.md).

## Requirements

- JDK 8+ (bytecode target 1.8)
- Absolute paths to:

  | Env (SpireUI) | Typical source |
  |---------------|----------------|
  | `SPIREUI_STS_JAR` | STS `desktop-1.0.jar` |
  | `SPIREUI_BASEMOD_JAR` | BaseMod.jar |
  | `SPIREUI_MODTHESPIRE_JAR` | ModTheSpire.jar |

Copy [`.env.example`](.env.example) → `.env.local`. **Reuse the same file paths as CrossSpire**, but use the **`SPIREUI_*` key names** (not `CROSSSPIRE_*`).

Optional: `SPIREUI_D1_SERIAL` / `SPIREUI_D2_SERIAL` (device jar push); `SPIREUI_INSTALL_DIR` / `SPIREUI_CONSUMER_JAR` (publish-local).

## Build & test

```bash
./scripts/with-env.sh test
./scripts/with-env.sh jar
./scripts/publish-local.sh          # jar + optional install via env
```

Artifact: `build/libs/SpireUI.jar` (version from `gradle.properties` → `spireui.version`).

**Default gate is JUnit.** OpenCode: `@junit-test`. Optional on-device: `@android-deploy-jar`.  
UI tooling (fixture): `cd tools/ui-verify && python3 -m unittest discover -s tests -v` or `@ui-verify`.

## ModTheSpire

- `modid`: `spireui`
- Depends on: `basemod`
- Consumers: `"dependencies": ["basemod", "spireui"]` and ship **both** jars (do not fat-jar SpireUI into the consumer).

## Package layout

| Package | Role |
|---------|------|
| `spireui.api` | Facade, window defs/handles, `entities()` |
| `spireui.c1` | Synthetic runtime, StageHost, layout DSL, StsSkin |
| `spireui.c2` | Native templates + EntityPresent |
| `spireui.SpireUiMod` | MTS entry |

## License

[MIT](LICENSE) — Copyright (c) 2026 ZJustin117.

This project does **not** grant rights to Slay the Spire, MegaCrit assets, or third-party mods (e.g. BaseMod, ModTheSpire). Runtime use requires a legitimate game install and those dependencies under their own terms.

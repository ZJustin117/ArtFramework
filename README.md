# ArtFramework

**ART Framework** — presentation layer for Slay the Spire 1 (Desktop ModTheSpire + Amethyst-compatible jars). Synthetic scene2d UI, native presenters, effects/shaders, host SPI.

- **Separate repository** from multiplayer logic (e.g. CrossSpire) — no protocol/party types here.
- **Dual track** (see [`docs/design/dual-track.md`](docs/design/dual-track.md)):
  1. **C1 Synthetic** — scene2d.ui windows (layout DSL + StageHost + StsSkin)
  2. **C2 Native templates** — map/event/select/end-turn hooks + `EntityPresent`

Open work: [`docs/task.md`](docs/task.md). Framework design: [`docs/design/art-framework.md`](docs/design/art-framework.md).  
Consumer: [`docs/development/consumer.md`](docs/development/consumer.md). UiOps/probe: [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md).  
Verify: [`docs/development/ui-layer-verification.md`](docs/development/ui-layer-verification.md).
API stability: [`docs/development/api-stability.md`](docs/development/api-stability.md).

## Requirements

- JDK 8+ (bytecode target 1.8)
- Absolute paths to:

  | Env (ArtFramework) | Typical source |
  |---------------|----------------|
  | `ART_STS_JAR` | STS `desktop-1.0.jar` |
  | `ART_BASEMOD_JAR` | BaseMod.jar |
  | `ART_MODTHESPIRE_JAR` | ModTheSpire.jar |

Copy [`.env.example`](.env.example) → `.env.local`. **Reuse the same file paths as CrossSpire**, but use the **`ART_*` key names** (not `CROSSSPIRE_*`).

Optional: `ART_D1_SERIAL` / `ART_D2_SERIAL` (device jar push); `ART_INSTALL_DIR` / `ART_CONSUMER_JAR` (publish-art-local).

## Build & test

```bash
./scripts/with-art-env.sh test
./scripts/with-art-env.sh jar
./scripts/publish-art-local.sh          # jar + optional install via env
```

Artifact: `build/libs/ArtFramework.jar` (version from `gradle.properties` → `artframework.version`).

**Default gate is JUnit.** OpenCode: `@junit-test`. Optional on-device: `@android-deploy-jar`.  
UI tooling (fixture): `cd tools/art-verify && python3 -m unittest discover -s tests -v` or `@art-verify`.

## ModTheSpire

- `modid`: `artframework`
- Depends on: `basemod`
- Consumers: `"dependencies": ["basemod", "artframework"]` and ship **both** jars (do not fat-jar ArtFramework into the consumer).

## Package layout

| Package | Role |
|---------|------|
| `artframework.api` | Facade, window defs/handles, `entities()` |
| `artframework.c1` | Synthetic runtime, StageHost, layout DSL, StsSkin |
| `artframework.c2` | Native templates + EntityPresent |
| `artframework.ArtFrameworkMod` | MTS entry |

## License

[MIT](LICENSE) — Copyright (c) 2026 ZJustin117.

This project does **not** grant rights to Slay the Spire, MegaCrit assets, or third-party mods (e.g. BaseMod, ModTheSpire). Runtime use requires a legitimate game install and those dependencies under their own terms.

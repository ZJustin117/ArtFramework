# ArtFramework

**ART Framework** — presentation layer for Slay the Spire 1 (Desktop ModTheSpire + Amethyst-compatible jars).

Version: see `gradle.properties` → `artframework.version` (currently **1.0.0-alpha.4**).

- **Presentation-only** — no multiplayer protocol, party, or combat-authority types here.
- **Dual track** ([`docs/design/dual-track.md`](docs/design/dual-track.md)):
  1. **C1 Synthetic** — scene2d.ui windows (layout DSL / LML + StageHost + StsSkin)
  2. **C2 Native** — thin templates + **full-present** surfaces (hand/map/event/select/reward/…)
- **SignalBus** + **HostAssets** + **EntityPresent** presentation slots
- Implemented milestones **0–46**. The current `1.0.0-alpha.4` release baseline is documented
  below in `CHANGELOG.md`; later completed work remains under `Unreleased` until the next version.
  This includes room/full-present production, PresentPack, node-signal runtime, C2 Lightwave
  chrome/FX, Spine 4.2 data-path support, and the unified presentation entity runtime.

Open work: [`docs/task.md`](docs/task.md). Design hub: [`docs/design/art-framework.md`](docs/design/art-framework.md).  
Consumer: [`docs/development/consumer.md`](docs/development/consumer.md).  
API stability: [`docs/development/api-stability.md`](docs/development/api-stability.md).  
Changelog: [`CHANGELOG.md`](CHANGELOG.md).  
Release gate: `./scripts/release-gate.sh`.

## Requirements

- JDK 8+ (bytecode target 1.8)
- Absolute paths to:

  | Env (ArtFramework) | Typical source |
  |---------------|----------------|
  | `ART_STS_JAR` | STS `desktop-1.0.jar` |
  | `ART_BASEMOD_JAR` | BaseMod.jar |
  | `ART_MODTHESPIRE_JAR` | ModTheSpire.jar |

Copy [`.env.example`](.env.example) → `.env.local` and fill the **`ART_*` key names** with local STS / BaseMod / ModTheSpire paths.

Optional: `ART_D1_SERIAL` / `ART_D2_SERIAL` (device jar push); `ART_INSTALL_DIR` / `ART_CONSUMER_JAR` (publish-art-local).

Optional developer-only STS2 assets use `ART_STS2_ROOT` and can be bundled with
`./scripts/package-sts2-assets.sh`. The generated `Sts2Assets.jar` is gitignored and is
never included in either release artifact.

The optional Spine 4.2 runtime artifact is built separately with
`./scripts/build-spine42-runtime.sh`; its build contains no tests or STS2 assets.

## Build & test

```bash
./scripts/with-art-env.sh test
./scripts/with-art-env.sh jar
./scripts/release-gate.sh           # JUnit + art-verify + release docs + version + consumer fixture
./scripts/publish-art-local.sh      # jar + optional install via env
```

Artifact: `build/libs/ArtFramework.jar`.

**Default gate is JUnit.** OpenCode: `@junit-test`. Optional on-device: `@android-deploy-jar`.  
UI tooling: `cd tools/art-verify && python3 -m unittest discover -s tests -v` or `@art-verify`.

Common D1 lab operations:

```bash
scripts/art-lab ready
scripts/art-lab combat verify-full
scripts/art-lab console "art probe"
```

## ModTheSpire

- `modid`: `artframework`
- Depends on: `basemod`
- Consumers: `"dependencies": ["basemod", "artframework"]` and ship **both** jars (do not fat-jar ArtFramework into the consumer).

## Package layout

| Package | Role |
|---------|------|
| `artframework.api` | Facade, UiOps/UiProbe, window defs |
| `artframework.core` | UiTree, signals, Theme, HostBackend |
| `artframework.context` | ContextFrame, present surfaces, intents |
| `artframework.ecs` | EntityId + PresentationWorld for ART-owned presentation state |
| `artframework.assets` | HostAssets packs / ResourceId |
| `artframework.c1` | Synthetic runtime, StageHost, layout/LML |
| `artframework.c2` | Native templates + EntityPresent |
| `artframework.sts1` | STS1 host backend, draw paths, patches, lab |
| `artframework.component` / `render` | Composition AST + effects |
| `artframework.ArtFrameworkMod` | MTS entry |

## License

[MIT](LICENSE) — Copyright (c) 2026 ZJustin117.

This project does **not** grant rights to Slay the Spire, MegaCrit assets, or third-party mods (e.g. BaseMod, ModTheSpire). Runtime use requires a legitimate game install and those dependencies under their own terms.

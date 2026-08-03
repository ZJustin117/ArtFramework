# ArtFramework — agent / contributor rules

## Boundary

- **This repo is a presentation framework (ART).** Do not depend on CrossSpire packages, protocol schema, or party/combat authority.
- Dual track: **C1** = scene2d synthetic windows; **C2** = native STS templates + full-present surfaces + EntityPresent. See [`docs/design/dual-track.md`](docs/design/dual-track.md).
- Presentation graph + Host SPI: [`docs/design/art-framework.md`](docs/design/art-framework.md).
- Godot-aligned API (UiTree / signals / C2 as components / host SPI): [`docs/design/godot-aligned-ui.md`](docs/design/godot-aligned-ui.md).
- Unified UI surface: **UiOps** / **UiProbe** — [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md).
- Backend / full present / HostAssets / EntityPresent: [`docs/design/backend-context.md`](docs/design/backend-context.md), [`docs/design/c2-full-present.md`](docs/design/c2-full-present.md), [`docs/design/host-assets.md`](docs/design/host-assets.md), [`docs/design/entity-present.md`](docs/design/entity-present.md).
- Temporary files → `agent-tmp/` (gitignored). Debug dumps → `debug-artifacts/` (gitignored).

## Document map

| File | Role |
|------|------|
| [`docs/design/art-framework.md`](docs/design/art-framework.md) | ART presentation graph, LML, Host SPI, milestone 12 |
| [`docs/design/dual-track.md`](docs/design/dual-track.md) | C1/C2 design + roadmap |
| [`docs/design/godot-aligned-ui.md`](docs/design/godot-aligned-ui.md) | Godot-aligned core API + C2 components + host SPI |
| [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md) | UiOps / UiProbe contract |
| [`docs/design/dev-ui-console.md`](docs/design/dev-ui-console.md) | `art ui` inspect / emit / invoke (lab) |
| [`docs/design/lab-run-nav.md`](docs/design/lab-run-nav.md) | `art lab` menu / fresh run / embark (D1) |
| [`docs/design/backend-context.md`](docs/design/backend-context.md) | Pluggable Backend, three faces, context frames, intents (15) |
| [`docs/design/c2-full-present.md`](docs/design/c2-full-present.md) | C2 full present / hard-sync / surfaces (15) |
| [`docs/design/host-assets.md`](docs/design/host-assets.md) | HostAssets ResourceId / packs / resolve (15) |
| [`docs/design/entity-present.md`](docs/design/entity-present.md) | EntityPresent co-op chrome draw (24) |
| [`docs/design/present-profile.md`](docs/design/present-profile.md) | PresentProfile / Lightwave (Theme + chrome + FX) |
| [`docs/design/node-signal-runtime.md`](docs/design/node-signal-runtime.md) | connections / UiActions / NodeStateMachine (39–42) |
| [`docs/task.md`](docs/task.md) | Open implementation tasks |
| [`docs/development/logic-layer-testing.md`](docs/development/logic-layer-testing.md) | Test pyramid + pure API rules |
| [`docs/development/ui-layer-verification.md`](docs/development/ui-layer-verification.md) | UI intercept/trigger/C1 verify + tools/art-verify |
| [`docs/development/console-commands.md`](docs/development/console-commands.md) | BaseMod `art` console reference |
| [`docs/development/android-deploy.md`](docs/development/android-deploy.md) | Optional device jar push |
| [`docs/development/android-device-lab.md`](docs/development/android-device-lab.md) | Amethyst D1 lab / connector / harness |
| [`docs/development/android-arthas.md`](docs/development/android-arthas.md) | Optional Arthas JVM diagnostics (not a default gate) |
| [`docs/development/README.md`](docs/development/README.md) | Infra index |
| `AGENTS.md` | This file — agent / contributor rules |

## Build / test

- Env keys: `ART_STS_JAR`, `ART_BASEMOD_JAR`, `ART_MODTHESPIRE_JAR` (paths may match CrossSpire machine setup; **key names** stay `ART_*`).
- Optional deploy / UI device: `ART_D1_SERIAL` (mirror CrossSpire D1), `ART_D2_SERIAL` (dual only if requested).
- Device lab (Amethyst, same pattern as CrossSpire): `STS_CONNECTOR_PORT`, `SLAY_THE_AMETHYST_ROOT`, `ART_AMETHYST_TOOLS_DIR`, `ART_HARNESS_OUT_DIR`, `ART_GAME_PROBE_PORT`, `ART_ARTHAS_PORT` — see [`docs/development/android-device-lab.md`](docs/development/android-device-lab.md) and [`docs/development/android-arthas.md`](docs/development/android-arthas.md).
- Arthas startup additionally requires the game to run with `launchMode=mts` and a debug-compatible game-probe (`debugMode`, `autoplay`, `forceJvmCrash`, `forceRuntimeCrash`, or `performanceDeepDiagnostics`), so game-probe can accept `LOAD_AGENT`.
- The connector daemon must already be online on `STS_CONNECTOR_PORT`; Arthas diagnostics do not manage its lifecycle. Run the CLI with `PYTHONPATH` including `SLAY_THE_AMETHYST_ROOT`, and select the target explicitly with `--device "$ART_D1_SERIAL"` when more than one device is online. The CLI defaults to `9099` for game-probe and `8099` for the Arthas bridge; use `ART_GAME_PROBE_PORT` and `ART_ARTHAS_PORT` when overriding them.
- Optional: `ART_UI_VERIFY_OUT_DIR` for `tools/art-verify` JSON output.
- Default gate: `./scripts/with-art-env.sh test` (or `./gradlew test` with `-PstsJar` / `-PbaseModJar` / `-PmodTheSpireJar`).
- UI tooling offline: `cd tools/art-verify && python3 -m unittest discover -s tests -v`.
- Java 8 bytecode; JUnit 4. **No device harness required** for ArtFramework unit work.
- OpenCode plugin [`.opencode/plugins/local-env.ts`](.opencode/plugins/local-env.ts) loads allowlisted `.env.local` keys into shell env and test-agent context. Restart opencode after changing agents/plugins.

## OpenCode subagents

Read-only verification agents live in `.opencode/agent/*.md`. The **main agent writes code**; delegate test/deploy via Task / `@` so the parent session is not flooded with gradle/adb logs.

| Agent | When to use | When not to |
|-------|-------------|-------------|
| `junit-test` | **Default semantic gate** after API/registry/runtime pure-logic changes; user asks for JUnit | Docs-only; code will not compile; device-only ops |
| `android-deploy-jar` | Need fresh `ArtFramework.jar` on device after UI source changes; before manual/on-device UI checks | Semantic regression (use junit); no device / unset serial; jar unchanged |
| `art-verify` | Fixture YAML / offline runner; optional D1 UI smoke after deploy when probe/ops exist | Pure API rules (junit); CrossSpire life/co-op |
| `android-arthas` | Explicit bounded Android JVM diagnosis: threads, classloading, methods, traces, or bridge failures | Default gate; UI semantics; jar deploy; connector lifecycle; CrossSpire life/co-op |
| `android-harness` | D1 harness lifecycle, game status, logs/screenshots, and bounded BaseMod console commands | Source edits; jar deploy; connector lifecycle; Arthas; CrossSpire life/co-op |

**Do not add** CrossSpire-style dual-device **life** suites or protocol assertions here. Arthas is optional read-only JVM diagnostics, not a default ArtFramework gate; connector lifecycle and dual-device life stay outside this repository's default workflow. ArtFramework may run **single-device UI** smoke via `@art-verify`.

### Delegation rules

1. One Task = one narrow goal (full `./scripts/with-art-env.sh test`, deploy jar, or art-verify). Do not bundle refactor + test + fix in one subagent.
2. Order: code change → **`@junit-test`** → offline **`@art-verify`** if runner/YAML touched → **`@android-deploy-jar`** if jar needed → **`@android-harness`** for connector-ready game lifecycle and console operations → device **`@art-verify`**. Use **`@android-arthas`** only when a separate JVM diagnosis is requested.
3. Subagents **report summaries only** (`edit: deny`). Parent fixes source, then re-delegates.
4. Prefer not running full suites in the parent session when subagents are available.
5. Task resume: `task_id` only from a real `ses…` id; **omit `task_id` on new tasks** (do not invent UUIDs). Plugin strips non-`ses` ids.
6. Missing env: subagent stops and lists **key names**; parent must not invent absolute paths.

## Code

- Package root: `artframework`.
- Prefer pure API tests for registry/gates; native patches need **explicit design** before adding `@SpirePatch`.
- Do not commit secrets or `.env.local`.

## TDD workflow

1. Register or refine open work in [`docs/task.md`](docs/task.md); design in `docs/design/dual-track.md` / `ui-ops-probe.md` when tracks change.
2. Start behavior changes with a focused failing JUnit (pure registry/API preferred).
3. Smallest implementation to green. Keep CrossSpire protocol/party types out of this repo.
4. Delegate **`@junit-test`** after each coherent slice. Deploy jar only when on-device UI verification is needed.

## Git

- Prefer prefixes: `feat:`, `fix:`, `perf:`, `chores:`.
- Do not commit secrets, `build/`, `node_modules/`, or `.env.local`.

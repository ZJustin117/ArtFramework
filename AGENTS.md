# ArtFramework — agent / contributor rules

## Boundary

- **This repo is a presentation framework (ART).** Do not depend on downstream project packages, protocol schema, or party/combat authority.
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
| [`docs/design/entity-present.md`](docs/design/entity-present.md) | EntityPresent chrome draw (24) |
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

- Env keys: `ART_STS_JAR`, `ART_BASEMOD_JAR`, `ART_MODTHESPIRE_JAR` (**key names** stay `ART_*`).
- Optional deploy / UI device: `ART_D1_SERIAL`, `ART_D2_SERIAL` (dual only if requested).
- Device lab (Amethyst): `STS_CONNECTOR_PORT`, `SLAY_THE_AMETHYST_ROOT`, `ART_AMETHYST_TOOLS_DIR`, `ART_HARNESS_OUT_DIR`, `ART_GAME_PROBE_PORT`, `ART_ARTHAS_PORT` — see [`docs/development/android-device-lab.md`](docs/development/android-device-lab.md) and [`docs/development/android-arthas.md`](docs/development/android-arthas.md).
- Arthas startup additionally requires the game to run with `launchMode=mts` and a debug-compatible game-probe (`debugMode`, `autoplay`, `forceJvmCrash`, `forceRuntimeCrash`, or `performanceDeepDiagnostics`), so game-probe can accept `LOAD_AGENT`.
- The connector daemon must already be online on `STS_CONNECTOR_PORT`; neither Arthas nor Harness subagents manage its lifecycle. Check connector status first and report its unavailability rather than starting or restarting it.
- Run Arthas from the ArtFramework root with `PYTHONPATH` including `SLAY_THE_AMETHYST_ROOT`, explicit `--device "$ART_D1_SERIAL"`, `--agent-port "$ART_GAME_PROBE_PORT"`, and `--arthas-port "$ART_ARTHAS_PORT"`. Default diagnostic cleanup is `stop` (`reset`; bridge remains listening); use `shutdown` (releases the bridge port) only when a parent explicitly requests full teardown. Bound `monitor`, `watch`, and `trace` queries with `--duration`.
- For the standard bounded D1 flow, use `scripts/art-lab` from the ArtFramework root: `ready`, `status`, `stop`, `console "<BaseMod command>"`, or `combat verify-full`. It checks device/connector availability, passes Harness connection arguments, reports `result.json`, and `combat verify-full` runs the evidence-producing FULL combat YAML. Do not reproduce its Harness lifecycle or BaseMod combat recipe inline.
- Run raw Harness only when `scripts/art-lab` does not support the requested operation. Keep the ArtFramework root workdir and pass `-DeviceSerial`, `-ConnectorPort`, `-AgentPort`, and `-OutDir`. A successful `start` must be followed by status polling until `READY`; console work must use one-shot `-ConsoleCommand`, and UI claims need result artifacts or relevant `ART_PROBE` / `ART_UI` / `ART_LAB` log evidence rather than exit status alone.
- Optional: `ART_UI_VERIFY_OUT_DIR` for `tools/art-verify` JSON output.
- Default gate: `./scripts/with-art-env.sh test` (or `./gradlew test` with `-PstsJar` / `-PbaseModJar` / `-PmodTheSpireJar`).
- UI tooling offline: `cd tools/art-verify && python3 -m unittest discover -s tests -v`.
- Java 8 bytecode; JUnit 4. **No device harness required** for ArtFramework unit work.
- OpenCode plugin [`.opencode/plugins/local-env.ts`](.opencode/plugins/local-env.ts) loads allowlisted `.env.local` keys into shell env and developer/test-agent context. Restart opencode after changing agents/plugins.

## OpenCode subagents

Project subagents live in `.opencode/agent/*.md`. The **main agent owns task framing, review, and integration**; delegate bounded implementation to `@developer` and verification/deploy work to the specialized agents so the parent session is not flooded with code-search, diff, gradle, adb, or harness logs.

| Agent | When to use | When not to |
|-------|-------------|-------------|
| `developer` | Bounded ArtFramework source/test/doc implementation scoped by the parent; isolate code-search and edit context | Vague tasks without scope; commits/merges/pushes; deploy/device/harness/Arthas work; recursive delegation |
| `junit-test` | **Default semantic gate** after API/registry/runtime pure-logic changes; user asks for JUnit | Docs-only; code will not compile; device-only ops |
| `android-deploy-jar` | Need fresh `ArtFramework.jar` on device after UI source changes; before manual/on-device UI checks | Semantic regression (use junit); no device / unset serial; jar unchanged |
| `art-verify` | Fixture YAML / offline runner; optional D1 UI smoke after deploy when probe/ops exist; scenarios other than `scripts/art-lab combat verify-full` | Pure API rules (junit); standard `ready` / `status` / `console` / `combat verify-full` wrapper operations; out-of-repo life/co-op |
| `android-arthas` | Explicit bounded Android JVM diagnosis: threads, classloading, methods, traces, or bridge failures; default `start -> query -> stop` cleanup | Default gate; UI semantics; jar deploy; connector lifecycle; out-of-repo life/co-op |
| `android-harness` | D1 logs/screenshots, `doctor` / `mods` / `set-mods`, or a bounded Harness command not exposed by `scripts/art-lab` | Standard `scripts/art-lab ready/status/stop/console/combat verify-full`; source edits; jar deploy; connector lifecycle; Arthas; out-of-repo life/co-op |
| `art-reviewer` | Read-only, explicitly scoped refactor/diff review through the `refacter` skill; reports evidence-based findings | Source edits; task/ledger ownership; test/deploy gates; recursive delegation |

**Do not add** dual-device **life** suites or protocol assertions here. Arthas is optional read-only JVM diagnostics, not a default ArtFramework gate; connector lifecycle and dual-device life stay outside this repository's default workflow. ArtFramework may run **single-device UI** smoke via `@art-verify`.

### Delegation rules

1. Prefer `@developer` for bounded implementation when context isolation matters. The parent must provide the goal, allowed scope, key constraints, and expected verification; `@developer` edits only that scope and never commits, merges, pushes, deploys, or delegates.
2. Prefer a script for a deterministic, parameterized operation it already owns. In particular, call `scripts/art-lab ready`, `status`, `stop`, `console`, or `combat verify-full` directly instead of creating a subagent Task. A script result is sufficient only for the evidence it explicitly reports; do not infer unrelated UI behavior.
3. Create one narrow verification Task when the work needs a specialist: full JUnit, jar deploy, a nonstandard UI YAML suite, Harness logs/screenshots or unsupported operations, or a bounded Arthas diagnosis. Do not bundle implementation + verification + deploy in one subagent.
4. Order: scoped implementation (parent or **`@developer`**) → **`@junit-test`** → offline **`@art-verify`** if runner/YAML touched → **`@android-deploy-jar`** if jar needed → confirm connector is already online → `scripts/art-lab ready` / `combat verify-full` for the standard D1 smoke. Use **`@android-harness`** only for work outside that wrapper; use device **`@art-verify`** for scenarios the wrapper does not own. Use **`@android-arthas`** only when a separate bounded JVM diagnosis is requested.
5. Verification subagents **report summaries only** (`edit: deny`). `@developer` may edit scoped source/test/docs but still reports summaries only and returns unresolved verification failures to the parent.
6. Prefer not running full suites in the parent session when subagents are available.
7. Task resume: `task_id` only from a real `ses…` id; **omit `task_id` on new tasks** (do not invent UUIDs). Plugin strips non-`ses` ids.
8. Missing env: scripts and subagents stop and list **key names**; parent must not invent absolute paths.

### Refacter supervision

- Long-running refactor state lives under `docs/refacter/<project-name>/`. Supplying that directory
  with `@` resumes the project; read its project Markdown files before planning or editing.
- Load the `refacter` skill for incomplete refactors, architecture migrations, ownership cleanup,
  or explicit refactor review. The primary agent owns the depth-first review queue, edits, ledger,
  and completion decisions.
- `art-reviewer` is advisory and read-only. It reviews one frozen scope at a time and may not
  delegate. A reviewer `PASS` never replaces JUnit, art-verify, deploy, or device evidence.
- Persist before/after evidence, review session IDs, finding disposition, exact tests, blockers, and
  the next action in the project directory before ending a refactor session.

## Code

- Package root: `artframework`.
- Prefer pure API tests for registry/gates; native patches need **explicit design** before adding `@SpirePatch`.
- Do not commit secrets or `.env.local`.

## TDD workflow

1. Register or refine open work in [`docs/task.md`](docs/task.md); design in `docs/design/dual-track.md` / `ui-ops-probe.md` when tracks change.
2. Start behavior changes with a focused failing JUnit (pure registry/API preferred).
3. Smallest implementation to green. Keep downstream protocol/party types out of this repo.
4. Delegate **`@junit-test`** after each coherent slice. Deploy jar only when on-device UI verification is needed.

## Git

- Prefer prefixes: `feat:`, `fix:`, `perf:`, `chores:`.
- Do not commit secrets, `build/`, `node_modules/`, or `.env.local`.
- When a task changes tracked files, complete the applicable verification before delivery. ART pure API, registry, or runtime changes require the default `@junit-test` gate. Do not commit, push, or merge after a failed test, compile failure, unresolved conflict, or incomplete required verification.
- Before committing, inspect `git status`, `git diff --check`, and the staged diff. Stage only task-owned changes; if pre-existing changes cannot be distinguished from task changes, stop and report rather than committing them. Never bypass hooks.
- After verification, create one focused, non-empty commit using the repository prefix convention, then `git fetch origin` and merge the current `origin/main` into the task branch. If the merge conflicts, stop and report; do not resolve conflicts automatically.
- Run the applicable verification again after merging `origin/main`. Only after it passes, integrate directly by fast-forward push: `git push origin HEAD:refs/heads/main`.
- Never use `--force`, `--force-with-lease`, history rewrites, or protected-branch bypasses. If the direct push is rejected for non-fast-forward updates, missing permission, branch protection, hooks, or CI policy, stop and report the result. Do not create a PR or retry against a changed remote without an explicit user request.
- After a successful push, report the resulting commit.

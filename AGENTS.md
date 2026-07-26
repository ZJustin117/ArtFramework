# SpireUI — agent / contributor rules

## Boundary

- **This repo is UI toolkit only.** Do not depend on CrossSpire packages, protocol schema, or party/combat authority.
- Dual track: **C1** = scene2d synthetic windows; **C2** = native STS templates + entity presenters. See [`docs/design/dual-track.md`](docs/design/dual-track.md).
- Unified UI surface (roadmap): **UiOps** / **UiProbe** — [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md).
- Temporary files → `agent-tmp/` (gitignored). Debug dumps → `debug-artifacts/` (gitignored).

## Document map

| File | Role |
|------|------|
| [`docs/design/dual-track.md`](docs/design/dual-track.md) | C1/C2 design + roadmap |
| [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md) | UiOps / UiProbe contract |
| [`docs/task.md`](docs/task.md) | Open implementation tasks |
| [`docs/development/logic-layer-testing.md`](docs/development/logic-layer-testing.md) | Test pyramid + pure API rules |
| [`docs/development/ui-layer-verification.md`](docs/development/ui-layer-verification.md) | UI intercept/trigger/C1 verify + tools/ui-verify |
| [`docs/development/android-deploy.md`](docs/development/android-deploy.md) | Optional device jar push |
| [`docs/development/README.md`](docs/development/README.md) | Infra index |
| `AGENTS.md` | This file — agent / contributor rules |

## Build / test

- Env keys: `SPIREUI_STS_JAR`, `SPIREUI_BASEMOD_JAR`, `SPIREUI_MODTHESPIRE_JAR` (paths may match CrossSpire machine setup; **key names** stay `SPIREUI_*`).
- Optional deploy / UI device: `SPIREUI_D1_SERIAL` (default single device; mirror CrossSpire D1 value), `SPIREUI_D2_SERIAL` (only if dual deploy requested).
- Optional: `SPIREUI_UI_VERIFY_OUT_DIR` for `tools/ui-verify` JSON output.
- Default gate: `./scripts/with-env.sh test` (or `./gradlew test` with `-PstsJar` / `-PbaseModJar` / `-PmodTheSpireJar`).
- UI tooling offline: `cd tools/ui-verify && python3 -m unittest discover -s tests -v`.
- Java 8 bytecode; JUnit 4. **No device harness required** for SpireUI unit work.
- OpenCode plugin [`.opencode/plugins/local-env.ts`](.opencode/plugins/local-env.ts) loads allowlisted `.env.local` keys into shell env and test-agent context. Restart opencode after changing agents/plugins.

## OpenCode subagents

Read-only verification agents live in `.opencode/agent/*.md`. The **main agent writes code**; delegate test/deploy via Task / `@` so the parent session is not flooded with gradle/adb logs.

| Agent | When to use | When not to |
|-------|-------------|-------------|
| `junit-test` | **Default semantic gate** after API/registry/runtime pure-logic changes; user asks for JUnit | Docs-only; code will not compile; device-only ops |
| `android-deploy-jar` | Need fresh `SpireUI.jar` on device after UI source changes; before manual/on-device UI checks | Semantic regression (use junit); no device / unset serial; jar unchanged |
| `ui-verify` | Fixture YAML / offline runner; optional D1 UI smoke after deploy when probe/ops exist | Pure API rules (junit); CrossSpire life/co-op |

**Do not add** CrossSpire-style dual-device **life** suites, connector, or Arthas as default SpireUI gates. Protocol and dual-device life stay in CrossSpire. SpireUI may run **single-device UI** smoke via `@ui-verify` only.

### Delegation rules

1. One Task = one narrow goal (full `./scripts/with-env.sh test`, deploy jar, or ui-verify). Do not bundle refactor + test + fix in one subagent.
2. Order: code change → **`@junit-test`** → offline **`@ui-verify`** if runner/YAML touched → **`@android-deploy-jar`** only if devices need a new jar → optional device **`@ui-verify`**.
3. Subagents **report summaries only** (`edit: deny`). Parent fixes source, then re-delegates.
4. Prefer not running full suites in the parent session when subagents are available.
5. Task resume: `task_id` only from a real `ses…` id; **omit `task_id` on new tasks** (do not invent UUIDs). Plugin strips non-`ses` ids.
6. Missing env: subagent stops and lists **key names**; parent must not invent absolute paths.

## Code

- Package root: `spireui`.
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

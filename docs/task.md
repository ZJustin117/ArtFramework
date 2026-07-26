# SpireUI open tasks

Checkbox list for open work. Tick when done; milestone notes stay short.

## Infrastructure (P0–P2)

- [x] OpenCode `junit-test` + `local-env` + `opencode.json`
- [x] AGENTS subagent table + delegation order
- [x] `docs/development` testing + deploy notes
- [x] Expand pure API JUnit beyond smoke
- [x] Optional `@android-deploy-jar` (default D1)
- [x] D1/D2 serial keys documented; local D1 mirrors CrossSpire device
- [x] `tools/ui-verify` scaffold + offline unittest + `@ui-verify` agent
- [x] Design: `ui-ops-probe.md` + `ui-layer-verification.md` + dual-track roadmap 6–8

## Product roadmap (from dual-track)

- [x] 0. Scaffold — registry API + tests
- [x] 1a. C1 logic runtime + layout DSL + demo resource + open dispatch
- [x] 1b. Stage host + StsSkin + StageBackend (optional on-device when D1 set)

- [x] 2. C2 map template intercept + pin decorator hooks (logic; patches later)
- [x] 3. C2 event/select/end-turn templates (logic; patches later)
- [x] 4. EntityPresent lifecycle API (logic; STS draw later)
- [x] 5. Consumer contract: versioned jar + CrossSpire `compileOnly` + MTS dep

### 6. UiOps / UiProbe (unified UI commands + snapshot)

- [x] 6.1 Pure `UiOps` / `UiProbe` / `UiOpResult` + `FakeNativeOps` + JUnit
- [x] 6.2 Select grid/hand + confirm ops
- [x] 6.3 Map node click + pin query on probe
- [x] 6.4 Event option + end-turn press ops
- [x] 6.5 Hand play **gesture** + C1 `clickButton`
- [x] 6.6 Console `spireui probe` / `spireui op` + `StsNativeOps` install

### 7. C2 SpirePatch thin hooks

- [x] 7.1 Map transition → `NativeUiHooks.onMapNodeClick` (BLOCK clears nextRoom)
- [x] 7.2 Grid confirm button → `onSelectConfirm(GRID)`; grid/hand update observe stubs
- [x] 7.3 Event `buttonEffect` prefix → `onEventOption`
- [x] 7.4 EndTurn enable/disable(true) UI gates (no protocol broadcast)

### 8. UI-layer verification

- [x] 8.0 Fixture YAML + assert runner offline
- [ ] 8.1 Device mode wired to real log scrape of `SPIREUI_PROBE` (optional; console prints line now)
- [x] 8.2 Fixture smoke: probe shape + C1 window + intercept-related template flags (JUnit owns BLOCK/ALLOW)
# ArtFramework open tasks

Checkbox list for open work. Tick when done; milestone notes stay short.

## Infrastructure (P0–P2)

- [x] OpenCode `junit-test` + `local-env` + `opencode.json`
- [x] AGENTS subagent table + delegation order
- [x] `docs/development` testing + deploy notes
- [x] Expand pure API JUnit beyond smoke
- [x] Optional `@android-deploy-jar` (default D1)
- [x] D1/D2 serial keys documented; local D1 mirrors CrossSpire device
- [x] `tools/art-verify` scaffold + offline unittest + `@art-verify` agent
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
- [x] 6.6 Console `art probe` / `art op` + `StsNativeOps` install

### 7. C2 SpirePatch thin hooks

- [x] 7.1 Map transition → `NativeUiHooks.onMapNodeClick` (BLOCK clears nextRoom)
- [x] 7.2 Grid confirm button → `onSelectConfirm(GRID)`; grid/hand update observe stubs
- [x] 7.3 Event `buttonEffect` prefix → `onEventOption`
- [x] 7.4 EndTurn enable/disable(true) UI gates (no protocol broadcast)

### 8. UI-layer verification

- [x] 8.0 Fixture YAML + assert runner offline
- [x] 8.1 Device mode: Amethyst connector console + `ART_PROBE` log scrape (`device_console.py`)
- [x] 8.2 Fixture smoke: probe shape + C1 window + intercept-related template flags (JUnit owns BLOCK/ALLOW)
- [x] 8.3 Lab doc: `android-device-lab.md` (CrossSpire/Amethyst parity, D1 UI only)
- [x] 8.4 On-device pass of `tests/ui-scenarios/device/*` (D1 READY; enabled_mods + cold start)

### 9. Lab intercept + deploy hardening

- [x] 9.1 `GateLab` + console `art gate … block|clear` + JUnit
- [x] 9.2 Device scenario `d1_gate_block_ops` (ops under lab gate)
- [x] 9.3 `scripts/ensure-enabled-mods.sh` + deploy agent note
- [x] 9.4 Remove empty select update stub patches

### 10. Component composition framework

- [x] 10.1 Design: `component-composition.md` + `component-layout-fx.md`
- [x] 10.2 Pure `UiNode` AST + Nest containers (`row`/`col`/`stack`/`panel`/`fragment`)
- [x] 10.3 `LayoutEngine` bounds + id index (JUnit)
- [x] 10.4 `ComponentRegistry` + `ref` / `slot` expand (JUnit)
- [x] 10.5 `UiNodeLoader` JSON + shorthand props; legacy `LayoutLoader` still works
- [x] 10.6 Scenario fixtures / offline art-verify coverage for composition samples
- [x] 10.7 Leaf runtime (`WidgetSession`) + UiOps slider/hitarea/click + probe controls
- [x] 10.8 RenderHost + Effect/Shader registry + glow GLSL assets + entity Attach + probe `render`
- [x] 10.9 Composition Stage inflate (`ComponentActors` + `attachComposition`) + effect SpriteBatch draw
- [x] 10.10 ShaderRuntime compile path + glow ShaderProgram draw fallback + probe shader status
- [x] 10.11 FULL_FRAME enable/sync/bind + console `artframework fx` + StageHost screen bounds
- [x] 10.12 FrameCapture + blur/glass post-process + `glass` component + glass_demo layout

### 11. Godot-aligned UI core

Design: [`docs/design/godot-aligned-ui.md`](design/godot-aligned-ui.md).

- [x] 11.0 Design: `godot-aligned-ui.md` + dual-track / composition / layout-fx / ui-ops-probe / AGENTS links
- [x] 11.1 `UiInstance` + `UiTree` + `SignalHub` (C1) + JUnit
- [x] 11.2 `LayoutSpec` size flags + minSize + `LayoutEngine`
- [x] 11.3 Theme MVP + `StsTheme`
- [x] 11.4 C1 widgets: textfield / checkbox / progress / scroll (priority order)
- [x] 11.5 C2 NativeControl (`sts.*`) + UiOps invoke + probe `components`
- [x] 11.6 API mount/unmount + `HostBackend` SPI + consumer.md
- [x] 11.7 Optional: C1 end-turn chrome pilot (`endturn_chrome_pilot` layout + JUnit)

### 12. ART Framework (presentation graph)

Design: [`docs/design/art-framework.md`](design/art-framework.md). Breaking identity rename done in phase 12.0 prep.

- [x] 12.0 Identity rename (`artframework` package/mod/jar/console/probe/env) + `art-framework.md` + task links
- [x] 12.1 Declared signals on AST + connect/emit validation + JUnit
- [x] 12.2 `NodeRegistry` / type SPI; loaders resolve registered types; namespaced third-party types
- [x] 12.3 LML → AST loader + resource dispatch (`.json` / `.lml`) + art-verify sample
- [x] 12.4 C1 node factory SPI; migrate `ComponentActors` built-ins
- [x] 12.5 RenderGraph / host render backend boundary (behavior-preserving)
- [x] 12.6 Built-in `animation_player` + `shader_effect` nodes
- [x] 12.7 Skeleton provider SPI + fake provider JUnit
- [x] 12.8 Native id namespace `sts1.*` + presenter bridge naming (consumer-visible)
- [x] 12.9 Signal completion: C2 native emit/validation, C1 interceptors, SignalHub sugar,
  registry defaults, stable anonymous keys, and legacy actor routing

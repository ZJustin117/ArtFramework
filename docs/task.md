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

### 13. Runtime hardening

- [x] 13.0 Freeze public IDs/API; canonical native aliases, handle lifecycle, and probe contract
- [x] 13.1 Unify UiTree / WidgetSession / Host lifecycle and cleanup guarantees
- [x] 13.2 Complete Host SPI capabilities, input, and unified tick
- [x] 13.3 Converge UiOps delegation and versioned UiProbe results
- [x] 13.4 Verify the C1 real input and signal loop on Stage/D1
- [x] 13.5 Harden C2 NativeControl lifecycle, payloads, and screen recreation
- [x] 13.6 Capability-aware render fallback and D1 effects verification
- [x] 13.7 Consumer fixture, API stability checklist, and release versioning

### 14. Component action convergence

- [x] 14.1 Route legacy C2 UiOps sugar through `UiComponent.action`; keep gate/gesture dispatch internal
- [x] 14.2 Expose mounted C1 windows as `UiComponent` actions and probe slices

### 15. Backend / full C2 present / HostAssets

Design: [`docs/design/backend-context.md`](design/backend-context.md),
[`docs/design/c2-full-present.md`](design/c2-full-present.md),
[`docs/design/host-assets.md`](design/host-assets.md).

Target (not yet code): pluggable Primary Backend + context frames; C2 **full present**
(hard-sync draw, signals/intents replace native UI callbacks); HostAssets unified pack
library for ART resolve-and-render. Thin C2 intercept remains migration bridge.

- [x] 15.0 Design docs + task / AGENTS / design cross-links
- [x] 15.1 Context / Backend pure interfaces + `FakeBackend` frame/intent JUnit
- [x] 15.2 HostAssets pure merge/config/`FakeHostAssets` + JUnit (no GL)
- [x] 15.3 ResourceId conventions + minimal vanilla catalog (card / map / UI)
- [x] 15.4 ART draw paths through `resolve` (Theme icon/style + present hand art probe)
- [x] 15.5 Combat hand / card_slots snapshot hard-sync present (projection + probe)
- [x] 15.6 Drag / play intent + signal intercept chain (`CardRef` multi-instance)
- [x] 15.7 Map / controls / skeleton surfaces (full-present slices)
- [x] 15.8 Pack register API for beautify mods + `probeAssets` / console `art assets`
- [x] 15.9 Consumer notes: intents replace native UI callbacks; `sts.*` aliases

Default gate remains `./scripts/with-art-env.sh test`. Device: optional D1 only.

### 16. STS1 full-present host implementation

Design: [`docs/design/backend-context.md`](design/backend-context.md),
[`docs/design/c2-full-present.md`](design/c2-full-present.md),
[`docs/design/host-assets.md`](design/host-assets.md).

15.x completed the host-agnostic contract and pure-logic surfaces. 16.x is the separate
STS1 host/render/input implementation required before a surface may claim full-present D1
coverage.

- [x] 16.0 Strong typed frame views (`ControlsView` / `MapView`), scene epoch policy,
  `PresentLevel` OFF|OBSERVE|FULL + `FullPresentMode` probe; JUnit + f5 fixture
- [x] 16.1 `Sts1PresentationBackend` strong-typed snapshot + intent gate by present level
  (D1 probe optional; still READ_ONLY until 16.5 executor)
- [x] 16.1b D1 probe smoke: `d1_full_present_observe` / `d1_full_present_combat_on` (D1 PASS)
- [x] 16.2 STS1 HostAssets vanilla catalog (`Sts1VanillaCatalog` / `Sts1HostAssets`) + JUnit
  (texture handle materialization deferred to 16.3/16.4 draw path)
- [x] 16.3 C2 render plan: layers, BatchStateGuard, clip, overlay observe + JUnit
  (host SpriteBatch still delegates card.render; true atlas path in 16.4)
- [x] 16.4 Hand draw path + geometry compare (pure JUnit); host applies projection pose
  before card.render; D1 geometry fixture optional later
- [x] 16.4b Hand geometry compare remains pure JUnit (`GeometryCompare`); live D1 pose
  fixture optional when combat room scripted
- [x] 16.5 CombatInputRouter + IntentExecutor SPI + RecordingIntentExecutor JUnit;
  Sts1PresentationBackend delegates submitIntent; native input suppress flag
  (live STS play/drag executor host body still thin — uses Fake/Recording in tests)
- [x] 16.5b `Sts1IntentExecutor` (drag/play/end-turn/map ack) + hand input suppress patch;
  installed at PostInitialize; JUnit soft-reject without dungeon
- [x] 16.6 ControlsDrawPath + end-turn suppress patch + ART label draw when FULL;
  `art present combat` sets controls level; probe `controlsDraw`
- [x] 16.6b Covered by D1 `d1_full_present_combat_on` controlsDraw/presentLevel FULL
- [x] 16.7 MapDrawPath + MapPanZoom + hitTest + map suppress patch + `art present map`;
  JUnit + f10; D1 `d1_full_present_map`
- [x] 16.8 ArtAudioBridge + Sts1SkeletonBridge + host recreate cleanup via PresentSafety
- [x] 16.9 PresentSafety panic/clear-panic, probe matrix f11, consumer freeze notes,
  D1 `d1_full_present_panic`

### 17. Dev UI console (`art ui`)

Design: [`docs/design/dev-ui-console.md`](design/dev-ui-console.md).

Inspect ART trees, emit signals, invoke actions, limited STS native dump/click for lab/D1.
Does not block 16.x full-present host work.

- [x] 17.0 Design: `dev-ui-console.md` + task / ui-ops-probe / ui-layer-verification links
- [x] 17.1 Pure `UiInspect` + `UiLabListeners` + JUnit
- [x] 17.2 Console `art ui list|tree|node|emit|invoke|listen`
- [x] 17.3 `StsUiReflect` native dump/click whitelist
- [x] 17.4 Device scenario + D1 smoke (optional after deploy)

### 18. Lab run navigation (`art lab`)

Design: [`docs/design/lab-run-nav.md`](design/lab-run-nav.md).

Atomic + composite console commands to reach main menu / fresh run / embark for D1
full-present scenarios. Lab-only (not consumer API).

- [x] 18.0 Design: `lab-run-nav.md` + task / console / device-lab links
- [x] 18.1 `LabStateSnapshot` / `LabHost` / `FakeLabHost` + `art lab dump` + JUnit
- [x] 18.2 L1: clear-saves / strip-resume / open-char-select / char / embark / seed
- [x] 18.3 L1: menu-click / abandon / abandon-confirm / return-menu / proceed
- [x] 18.4 L2: ensure-menu / ensure-fresh-menu / start-run + D1 YAML (`d1_lab_*`)
- [x] 18.5 Console `art lab …` + docs (`console-commands`, device-lab, ui-layer-verify)

### 19. STS1 full-present production readiness

- [x] 19.1 Effective capability state: FULL requires mounted scene + ready executor before ART
  suppresses native UI or owns input; probe state/reason + JUnit
- [x] 19.2 HostAssets-native hand/card draw path (no `AbstractCard.render` delegation); D1
  `fight Cultist` combat-ready scenario confirms FULL_READY, native suppression, and ART hand draw
- [x] 19.3 HostAssets controls/map draw path + real STS1 map intent execution;
  D1 `d1_full_present_map_ready` (FULL_READY, texture nodes, `art op map first` leaves map)
- [x] 19.4 Scene lifecycle/recovery: PresentSafety matrix JUnit + D1
  `d1_full_present_lifecycle` (combat FULL → map fallback → re-arm → panic/clear)
- [x] 19.5 Event/select full-present surfaces (`sts1.event`, `sts1.select.grid|hand`) + JUnit
- [x] 19.6 HostAssets consumer pack/release validation (`ConsumerFixture` + verify script)

### 20. Historical FrameSignal authority boundary

Design: [`backend-context.md`](design/backend-context.md).

- [x] Superseded by milestone 21; endpoint categories and transaction types were removed.

### 21. Unified SignalBus

- [x] 21.1 One ordered bus for C1, C2, frame, backend, and host signals; exact and regex subscriptions
- [x] 21.2 Generic `continue` / `replace` / `stopHandled` / `stopRejected` delivery decisions
- [x] 21.3 C2 imperative actions, native hooks, GateLab, full-present actions, and frame publish use bus delivery
- [x] 21.4 Remove FrameEndpoint transaction API and migrate fixtures to `FakeSignalBackend`
- [x] 21.5 `HostPatchResults` + hooks return `SignalDispatchResult`; patches use single adapter

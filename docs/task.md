# ArtFramework open tasks

Checkbox list for open work. Tick when done; milestone notes stay short.

## Infrastructure (P0–P2)

- [x] OpenCode `junit-test` + `local-env` + `opencode.json`
- [x] AGENTS subagent table + delegation order
- [x] `docs/development` testing + deploy notes
- [x] Expand pure API JUnit beyond smoke
- [x] Optional `@android-deploy-jar` (default D1)
- [x] D1/D2 serial keys documented for optional device UI smoke
- [x] `tools/art-verify` scaffold + offline unittest + `@art-verify` agent
- [x] Design: `ui-ops-probe.md` + `ui-layer-verification.md` + dual-track roadmap 6–8
- [x] Optional `@android-arthas` + `android-arthas.md` + `ART_ARTHAS_PORT` (not a default gate)

## Product roadmap (from dual-track)

### 46. Traditional ECS convergence

Design: [`docs/design/traditional-ecs.md`](design/traditional-ecs.md). Entity IDs only;
components are data only; systems are stateless and own all ART interaction handling.

**Current checkpoint (2026-08-14):** ECS data authority migration and the six planned convergence
slices are complete. C1/C2 state, native input records, intent lifecycle records, authority-frame
projection, render-plan inputs, Skeleton/EntityPresent state, and diagnostic projections are ECS
authoritative. The production schedule now owns authority projection, confirmation, animation,
effects, host presentation, render projection, render clock, and backend advancement. Window
handles, SignalBus subscriptions, provider handles, scene2d actors, and RenderHost targets remain
explicit disposable compatibility or host caches. Remaining work is limited to future native
adapter simplification and device verification for newly changed host paths.

- [x] 46.0 Define strict ECS contract; add data-only entity identity, component query, and
  ordered stateless system pipeline with JUnit
- [x] 46.1 Registered presentation scopes share one ART world; scope close/reset now destroys only
  owned entities while preserving the registered context identity
- [x] 46.2 Materialize C1 declarations through ECS systems; control current values now live on
  ECS entities, control normalization runs during runtime ticks, WidgetSession is constructed as an immutable
  declaration/index compatibility data, C1 visual entities include ECS host-binding keys, and
  UiOps/probe/scene2d widgets/UiActions/FX read ECS values; WindowManager no longer owns a layout
  root map. C1 target/effect projection now consumes ECS frames only and StageHost reconciles its
  actor cache from ECS host bindings; context/entity lifecycle queries resolve registered ECS scope
  data, while signal callback objects remain a disposable host cache
- [x] 46.3 Materialize STS observations and C2 surfaces through ECS systems; card projections now
  create/update/destroy shared-world entities and data-only card components, with frame lifecycle
  metadata, drag interaction metadata, and immutable frame snapshots on a projection root entity;
  card lookup, listing, counts, and cleanup now resolve through ECS card components, while the
  CardEntity compatibility view is derived on demand; C2 surface mount state and Entity lookup are
  now ECS-derived; native template facades no longer retain local active flags and template probe
  reads use ECS bind/pin/end-turn data
- [x] 46.4 Route every native input/intercept through ECS input, action, intent, and result data;
  C2 surface submissions now record data-only action/intent/result components before and after the
  existing SignalBus compatibility executor, and the STS1 combat router records native input plus
  intercept decisions per surface ECS entity for hand, controls, and executor paths; native map,
  event, select, and end-turn hooks now record their unbound, emitted, disabled, and rejected paths
  through the same model. Intent lifecycle transitions are centralized in
  `NativeIntentLifecycleSystem`; `PresentSurfaces.submit()` now writes a one-shot ECS execution
  request consumed by `SurfaceIntentExecutionSystem`, with SignalBus retained as the host executor
  boundary.
- [x] 46.5 Derive all rendering/effects/skeleton host caches from ECS data; animation playback,
  pulse envelopes, and skeleton identity/snapshot state are ECS authoritative, while RenderHost
  C1/C2 item target caches are ECS-frame-derived; surface/full-frame and host resource paths now
  consume ECS render-state components; D1 `d1_full_present_combat_ready`, EntityPresent draw/
  recreation/cleanup, and Spine34 native takeover/recreation/cleanup pass. Remaining strict work is
  `RenderHost.recreateFromEcs()` is the explicit host recreation boundary. Animation playback, effect pulses, coalesced render projection,
  the render clock, and the host backend tick now advance through stateless production ECS systems; `RenderHost.tick` and
  immutable-plan rebuilds are render-package-only, while lifecycle and STS filtered projections use
  explicit `RenderProjectionQueue` APIs.
- [x] 46.6 Derive Probe/API compatibility views from ECS only and remove legacy stores; C1 probe,
  lifecycle queries, render-state projection, and business confirmation are ECS-derived, while
  callback/resource caches remain explicit host boundaries. UiProbe, UiInspect, console/probe
  reads, and PresentSurfaces registry recreation have been audited; remaining cleanup is limited
  to concrete duplicate stores discovered by subsequent ownership audits. `ArtFramework.OPEN` is
  now handle/alias cache only; open ids and handle status query ECS lifecycle/template state.

#### 46.x completion checklist

- [x] 46.1.1 One ART World; context-owned entity index; scope close/reset isolation tests
- [x] 46.2.1 C1 control values and normalization are ECS authoritative
- [x] 46.2.2 C1 window/entity lifecycle, visibility, hierarchy, and host actor bindings are ECS data;
  StageHost reconciles actor objects through `HostBindingComponent`; scoped signals and callbacks
  are disposable host caches
- [x] 46.3.1 C2 card projection entities and card data components are ECS authoritative
- [x] 46.3.2 Projection root stores frame lifecycle, interaction, and immutable snapshot data
- [x] 46.3.3 C2 surface mount state and surface Entity identity are ECS authoritative
- [x] 46.3.4 Native template state is observed into ECS; bind, native component mounted state,
  event ID, end-turn enabled state, and map pins are ECS data while adapter callbacks remain host cache
- [x] 46.3.4a Native template bind/unbind state is stored in `NativeTemplateStateComponent` and
  runtime bound queries read the shared ECS world
- [x] 46.4.1 Surface action, intent identity, and immediate result are ECS recorded
- [x] 46.4.2 Combat router input/intercept records are ECS data per surface
- [x] 46.4.3 Map, event, select, and end-turn native hook records are ECS data per surface
- [x] 46.4.4 Intent execution lifecycle records requested/sent/executed/queued/rejected in ECS;
  `NativeIntentLifecycleSystem` consumes one-shot execution events and the next available/unavailable
  authority-frame observation into confirmed/failed state, while business confirmation separately
  records pending/confirmed/failed ECS data from card, map, event, reward, select, and room evidence
- [x] 46.4.5 Declarative signal connections/state-machine transitions are ECS data; subscriptions are cache-only,
  node state is stored in `NodeStateComponent`, and connection/legacy-trigger declarations are immutable
  `ConnectionDeclarationsComponent` data
- [x] 46.4.6 Node properties and effect attachments are immutable ECS values; writes replace the
  corresponding Component through `PresentationWorld`
- [x] 46.4.7 One production `PresentationSchedule` orders authority projection/confirmation,
  shared-world normalization, C1 animation, effect envelopes, host presentation, coalesced render
  projection, render clock, and host backend tick; `tick` delegates to the same schedule
- [x] 46.5.0 `RenderPlan.fromEcs()` is the sole target/effect cache input; C1/C2/surface/full-frame/
  EntityPresent targets rebuild after host cache clearing, direct RenderHost mutation APIs and
  RenderTarget setters are internal, and StageHost writes actor geometry back to ECS bounds
- [x] 46.5.1 C1/C2 render plans, effects, and profiles derive from ECS data only; animation
  playback and pulse state are data-only components, C1 declarations now materialize draw/bounds/
  visibility/effects into ECS frames, C1 and active C2 item targets rebuild in shared projections,
  while scheduled EffectPulse, animation property effects, and Lightwave writes are coalesced into
  one C1 host-cache projection per dirty window; surface/full-frame host APIs now consume
  `RenderSurfaceComponent` and `FullFrameRenderComponent`; D1 `d1_full_present_combat_ready` passes
- [x] 46.5.2 Skeleton animation/pose/binding lifecycle derives from ECS data only; identity,
  frame, asset, pose, animation, and visual state are ECS components, while native binding lookup
  remains a provider cache rebuilt from retained ECS state; D1 native takeover and cleanup are
  covered, including repeatable device-side recreation capture
- [x] 46.5.3 EntityPresent slot identity, snapshot, and transform state are ECS components;
  `EntitySlot` is an on-demand immutable compatibility view and listeners are host callbacks;
  the RenderHost entity target cache rebuilds from ECS slot queries; D1 attach/draw/detach and
  cache cleanup are covered, and pure tests cover host recreation rebuilding retained slot state
- [x] 46.6.1 UiProbe, UiOps, console, and inspect query ECS only; C1 UiOps control lookup and C1
  window/component probe snapshots and native template pin/end-turn snapshots read ECS data, while
  C2 EntityPresent snapshots also read ECS data; C1 window control/title/profile probe fields now
  query registered context components directly, while console/render legacy paths remain; UiOps no
  longer retains a second last-result map. UiProbe window ids now come from ECS lifecycle state and
  mounted native template state, so diagnostic output does not depend on the OPEN handle map.
- [x] 46.6.2 Remove or demote remaining legacy stores after each matching checklist row passes;
  WindowManager, SyntheticComponents, and EntitySlot remain derived compatibility views, while
  fixed component catalogs, signal subscription hubs, asset/profile catalogs, and host/provider
  caches are explicitly retained as non-authoritative integration state. PresentSurfaces now
  resolves its context/world on demand, allowing ECS registry recreation without stale writes.

- [x] 0. Scaffold — registry API + tests
- [x] 1a. C1 logic runtime + layout DSL + demo resource + open dispatch
- [x] 1b. Stage host + StsSkin + StageBackend (optional on-device when D1 set)

- [x] 2. C2 map template intercept + pin decorator hooks (logic; patches later)
- [x] 3. C2 event/select/end-turn templates (logic; patches later)
- [x] 4. EntityPresent lifecycle API (logic; STS draw later)
- [x] 5. Consumer contract: versioned jar + `compileOnly` + MTS dep

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
- [x] 8.3 Lab doc: `android-device-lab.md` (Amethyst D1 UI only)
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

**Done (contract):** pluggable Primary Backend + context frames; C2 full-present surfaces;
HostAssets packs. Thin C2 intercept remains migration bridge. STS1 host draw for combat/map
is milestone 16/19; event/select host draw is milestone **22**.

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

### 22. STS1 event / select full-present host

19.5 delivered pure surfaces + FakeBackend JUnit (draw deferred). 22.x is the STS1 host
draw / executor / suppress / D1 path for `sts1.event` and `sts1.select.grid|hand`.

- [x] 22.0 Design/task links + c2-full-present status note
- [x] 22.1 `EventView` / `SelectView` + `ContextFrame` + projection + JUnit
- [x] 22.2 `SurfaceDrawPlan` event/select + PresentSafety unmount + pipeline
- [x] 22.3 `EventDrawPath` / `SelectDrawPath` + renderer + backend snapshot + suppress patches
- [x] 22.4 `Sts1IntentExecutor` SELECT_* + `StsNativeOps` hand select
- [x] 22.5 Console `art present event|select` + probe `eventDraw` / `selectDraw`
- [x] 22.6 D1 YAML + consumer freeze notes

### 23. Release hardening

Post-22 product freeze for consumers. No new full-present surfaces in this milestone.

- [x] 23.0 Design/task status pass (c2-full-present / dual-track / AGENTS / README)
- [x] 23.1 API stability ↔ consumer freeze alignment
- [x] 23.2 Probe schema contract (v1 field groups)
- [x] 23.3 Consumer fixture expansion (frames / present / CardRef)
- [x] 23.4 Release gate script (`scripts/release-gate.sh`)
- [x] 23.5 CHANGELOG + versioning notes
- [x] 23.6 Version bump `1.0.0-alpha.3`

### 24. EntityPresent host draw

Co-op chrome slots: typed snapshot → EntityDrawPath → probe → optional host paint.
Combat HAND full-present remains authoritative for in-combat cards.

- [x] 24.0 Design note + task links (`entity-present.md`)
- [x] 24.1 Typed `EntitySnapshot` + parse helpers (JUnit)
- [x] 24.2 `EntityDrawPath` pure projection (JUnit)
- [x] 24.3 Probe `entities.slots` + RenderHost bounds by kind
- [x] 24.4 Policy: combat HAND FULL wins over EntityPresent CARD
- [x] 24.5 CARD host paint path (reuse hand renderer pattern; flag)
- [x] 24.6 RELIC icon path
- [x] 24.7 PLAYER / MONSTER minimal chrome path
- [x] 24.8 Skeleton PostRender draw when `shouldDraw`
- [x] 24.9 Offline fixture + optional D1 entity smoke

### 25. More STS1 full-present surfaces

Extend Backend `scene()` + View → DrawPath → suppress → executor → D1.
Reuse 16/22 pipeline. Meta menus stay C1 + `art lab`.

#### Wave A — combat chrome

- [x] 25.1 `sts1.combat.proceed` (+ cancel) controls extension
- [x] 25.2 `sts1.combat.energy` orb present from ControlsView.energy

#### Wave B — run decisions

- [x] 25.3 `sts1.reward.combat` View / DrawPath / host / D1
- [x] 25.4 `sts1.reward.card` View / DrawPath / host / JUnit
- [x] 25.5 `sts1.rest` View / DrawPath / host / JUnit
- [x] 25.6 `sts1.treasure` View / DrawPath / host / JUnit
- [x] 25.7 `sts1.shop` View / DrawPath / host / JUnit
- [x] 25.8 `sts1.reward.boss_relic` View / path / JUnit

#### Wave C — HUD + combat clarity

- [x] 25.9 `sts1.top_panel` View / DrawPath / probe
- [x] 25.10 `sts1.combat.intents` observe-first path
- [x] 25.11 Console `art present …` + probe fields for new surfaces
- [x] 25.12 D1 YAML smoke for reward/rest (lab-reachable) + offline fixtures

### 26. Room FULL production readiness

Raise 25 room/chrome surfaces to combat/map-class FULL_READY (scene match + suppress +
executor + host paint).

- [x] 26.0 Design/task matrix + c2-full-present / CHANGELOG status
- [x] 26.1 Capability / sceneReady for room surfaces (not mount-only) + JUnit
- [x] 26.2 Real room IntentExecutor gestures + soft-reject without dungeon + JUnit
- [x] 26.3 Suppress patches (reward/rest/shop/treasure) + shop/treasure/proceed render
- [x] 26.4 Probe capability fields + offline fixture `f13_room_full_ready`
- [x] 26.5 D1: reward capability fields + `d1_full_present_combat_chrome`

### 27. C1 / beautify components

- [x] 27.1 `grid` / `tabs` containers (LayoutEngine + Stage + sample layout)
- [x] 27.2 HostAssets beautify pack sample JUnit
- [x] 27.3 Register `grid_tabs_demo` window

### 28. Release hardening (alpha.4)

- [x] 28.1 Known-limits / consumer freeze notes refresh
- [x] 28.2 Version bump `1.0.0-alpha.4` + CHANGELOG

### 29. PresentProfile + Lightwave (theme / cascade / declarative)

Design: PresentProfile aggregates Theme + PresentChromeStyle + optional pack; not a mid-draw
RenderInterceptor. Suppress still owned by FullPresentMode.

- [x] 29.1 `PresentProfile` / `PresentProfiles` + `sts` / `lightwave` builtins + probe
- [x] 29.2 `LightwaveTheme` (semi-transparent panel, white border, cool accent, Card alpha)
- [x] 29.3 Theme cascade: font/icon/style + `themeType` variation
- [x] 29.4 Declarative `present_profile` / `theme` on window root + JUnit

### 30. Lightwave FX + animation triggers + demo

- [x] 30.1 `LightwaveEffect` + GLSL + fallback strips + `art fx lightwave`
- [x] 30.2 White border via effect fallback / chrome tokens
- [x] 30.3 `animation_player` `auto_play` + `triggers` (no new LML component type)
- [x] 30.4 C1 `opacity` prop applied on actors
- [x] 30.5 `layouts/lightwave_demo.json` + window register

### 31. C1 StsSkin from Theme

- [x] 31.1 `StsSkin.create(Theme)` + pure mapping helpers JUnit
- [x] 31.2 StageHost uses `Themes.getDefault()` at init

### 32. C2 PresentChromeStyle consumption

- [x] 32.1 `PresentChromeStyle` + fromTheme / probe
- [x] 32.2 Hand card alpha + white border; controls label colors from chrome
- [x] 32.3 HandDrawPath probe includes chrome + presentProfile

### 33. Console / docs / scenarios

- [x] 33.1 `art profile|theme list|get|set`
- [x] 33.2 Offline fixture `f14_present_profile_lightwave` + D1 `d1_lightwave_demo`
- [x] 33.3 task / design / CHANGELOG notes

### 34. Node-scoped PresentProfile (no process active)

Design: Present resources + ProjectPresent fallback + `art.present_profile` attach/override;
effects stay explicit nodes. See [`present-profile.md`](design/present-profile.md).

- [x] 34.1 `ProjectPresent` + `PresentResolve` + `PresentBinding` / `PresentMode`
- [x] 34.2 Remove process active; registry is resources only
- [x] 34.3 UiTree/UiInstance resolve; root sugar + `art.present_profile`
- [x] 34.4 C1 StageHost + C2 chrome via resolve / project
- [x] 34.5 Probe `projectPresent` + `windows.*.present`; console project/resolve
- [x] 34.6 JUnit + f14 + d1_lightwave_demo (node assert, no global set required)

### 35. Present production (hot restyle / C2 surface / pack)

Design: close Known limits from PresentProfile/Lightwave showcase — restyle open C1,
surface-scoped chrome, packId → HostAssets. See [`present-profile.md`](design/present-profile.md).

- [x] 35.1 Hot restyle: project/present change refreshes trees + re-attaches Stage (project-fallback windows)
- [x] 35.2 `SurfacePresent` bind + `PresentResolve.chromeForSurface` for C2 draw paths
- [x] 35.3 C2 chrome expand: event/select/room/proceed/energy/intents/top labels via chrome
- [x] 35.4 `packId` → HostAssets prefer/enable + JUnit + probe
- [x] 35.5 API stability + CHANGELOG notes (setProjectPresent restyles; SurfacePresent)

### 36. Lightwave visual deepen (optional polish)

- [x] 36.1 Lightwave border/band tokens from effect params (theme-aligned defaults)
- [x] 36.2 `glass_lightwave_demo` layout (glass + lightwave co-window)
- [x] 36.3 Third-party PresentProfile register sample JUnit (namespaced theme + chrome)

### 37. Global PresentProfile catalog (skin register facade)

Design: PresentProfiles is the sole skin resource catalog; facade register ≠ apply.
See [`present-profile.md`](design/present-profile.md).

- [x] 37.1 `PresentProfiles.register` syncs `Themes` (name / profile id)
- [x] 37.2 `ArtFramework.registerPresentProfile` / `get` / `ids` / `presentProfiles()`
- [x] 37.3 JUnit `PresentProfileCatalogTest` (register no apply; set applies; reset)
- [x] 37.4 Probe `presentProfiles` + f16 fixture; design / api-stability / consumer

### 38. PresentPack + enabled profiles (regex select/modify)

Design: Profile = skin; Pack = LML/JSON templates + windows. Select profile → activate pack by
packId / profileId link. No profile-id special cases in core. Regex enable/select/modify.

- [x] 38.1 `PresentPack` / `PresentPacks` / manifest loader
- [x] 38.2 activate/deactivate → ComponentRegistry + WindowDef
- [x] 38.3 `ProjectPresent.set` → `activateForProfile`
- [x] 38.4 `EnabledPresents` + regex select/modify + packId patch
- [x] 38.5 Builtin lightwave pack + facade / probe / console
- [x] 38.6 JUnit `PresentPackTest` + f18 + D1 `d1_present_packs`

### 39. Signal wiring aligned with bus (exact + regex)

Design: [`node-signal-runtime.md`](design/node-signal-runtime.md). Backend ↔ Signal ↔ Node.

- [x] 39.1 `SignalHub` / `UiTree.connectBus` exact + `Pattern`
- [x] 39.2 `NodeConnections` + `connections` decl; legacy `triggers` normalize
- [x] 39.3 JUnit exact / regex / unmount clear

### 40. UiActions (full builtin set)

- [x] 40.1 `UiActions` registry + `ArtFramework.registerUiAction`
- [x] 40.2 Builtins: play/pause/stop/resume/set_prop/pulse_effect/emit/close_window
- [x] 40.3 `PropEffectBridge` + `EffectPulse` (signal path for lightwave pulse)
- [x] 40.4 JUnit actions + third-party register + pulse binding

### 41. NodeStateMachine + AnimationPlayer once/loop/pause

- [x] 41.1 `NodeStateMachine` / `NodeStateMachines` from `states` decl
- [x] 41.2 AnimationPlayer pause/resume/loop + state idle/playing/paused
- [x] 41.3 Signals paused/resumed/looped; JUnit

### 42. Integration / demo / docs / D1

- [x] 42.1 `lightwave_demo` connections (pulse_effect + set_prop); no imperative ok pulse
- [x] 42.2 Design `node-signal-runtime.md` + task / consumer notes
- [x] 42.3 D1 `d1_node_connections` + existing `d1_lightwave_demo`

### 43. C2 Lightwave chrome + surface FX

- [x] 43.1 Baseline coverage and C2 surface target lifecycle
- [x] 43.2 Surface chrome panels/borders for ART-owned C2 draw regions
- [x] 43.3 Pack-driven per-surface LightwaveEffect bindings with inactive-surface cleanup
- [x] 43.4 Probe + offline fixture + `C2LightwaveSurfaceTest`
- [x] 43.5 D1 `d1_lightwave_c2_full` scenario for combat C2 visual verification

### 44. Spine 4.2 present architecture

Design: [`docs/design/spine42-present.md`](design/spine42-present.md).

Boundary: STS2 assets are never committed or packaged. Spine runtime users must satisfy
the Spine Runtime license; 4.2 runtime integration is provider-side and kept separate from
core presentation contracts.

- [x] 44.1 Design: Spine 4.2 present architecture, license/resource boundary, dual provider plan
- [x] 44.2 Pure atlas 4.x parser for compact `bounds` / `offsets` / `rotate:90` regions
- [x] 44.3 `AnimState`, `AnimGraph`, `SkeletonMixTable`, and STS2-style `SkeletonAnimator`
- [x] 44.4 Extended `SkeletonProvider` commands with fake-provider JUnit coverage
- [x] 44.5 STS1 Spine 3.4 provider baseline using host-bundled runtime
- [x] 44.6 Spine 4.2 provider probe shell for shaded runtime detection and safe degradation
- [x] 44.7 JUnit coverage for parser, graph, animator, provider commands, and provider probes
- [x] 44.8 Optional `spine42-runtime` sub-build: relocated runtime jar, Java 8 output, license,
  and artifact allowlist verification; separate from the main jar and test lifecycle
- [x] 44.9 `ART_STS2_ROOT` local asset bundle script and gitignored `Sts2Assets.jar`
- [x] 44.10 Independent `tests/spine42-assets` bundle checks; standard JUnit has no asset dependency
- [x] 44.11 Developer runtime loader, device asset deployment, and `spine42` dev console controls
- [x] 44.12 D1 resource status/load/animation/lifecycle scenario skeletons

### 45. Unified Presentation Entity Runtime

Design: [`docs/design/presentation-entity-runtime.md`](design/presentation-entity-runtime.md).

- [x] 45.1 Add dependency-neutral `presentation` runtime: context, stable keys, common entity
      components, immutable frame snapshots, and pure ECS tests.
- [x] 45.2 Replace `UiInstance` and the temporary object-tree facade with
      `PresentationContext` + `EntityId`; migrate lifecycle, path lookup, props, theme resolution,
      and public API.
- [x] 45.3 Migrate signals, declarative connections, state machines, and animation to node/entity
      identity while retaining declared-port and first-stop semantics.
- [x] 45.4 Migrate C1 Stage materialization and render synchronization to presentation frames.
- [x] 45.5 Migrate C2 surfaces, projection items, entity present, and skeleton lifecycle into the
      unified context; retain STS policy and intent authority in the host adapter.
- [x] 45.6 Derive render attachments from entities; migrate Lightwave chrome/effects to exact
      visual-item attachments and remove target-map authority.
- [x] 45.7 Replace legacy runtime/docs/fixtures and complete JUnit, offline verifier, and D1
      C1/C2 visual verification.
- [x] 44.13 D1 data-path evidence: source-patched runtime loads a real 4.2 `.skel`, animates,
  reports a bone transform, and cleans lifecycle state; pixel renderer intentionally deferred

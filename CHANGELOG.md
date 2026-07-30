# Changelog

## Unreleased

### Changed

- Milestone **34** node-scoped PresentProfile (no process active):
  - `ProjectPresent` fallback + `PresentResolve` cascade (`override` / `attach`)
  - `art.present_profile` node; root `present_profile` sugar
  - C1/C2 consume resolve or project chrome — not `PresentProfiles.active`
  - Probe `projectPresent` + `windows.*.present`; console `project` / `resolve`
  - Design: [`docs/design/present-profile.md`](docs/design/present-profile.md)

### Added

- Milestone **29–33** PresentProfile + Lightwave showcase:
  - `PresentProfile` / `PresentProfiles` (`sts`, `lightwave`) + `PresentChromeStyle`
  - `LightwaveTheme`; Theme cascade font/icon/style + `themeType` variation
  - Declarative window `present_profile` / `theme`; `LightwaveEffect` + GLSL + fallback
  - `animation_player` `auto_play` / `triggers`; C1 `opacity` on actors
  - `StsSkin.create(Theme)`; hand/controls chrome from profile resource
  - Console `art profile|theme list|get|set`; `art fx lightwave`
  - Demo `lightwave_demo`; fixture `f14_present_profile_lightwave`; D1 `d1_lightwave_demo`
  - Design: [`docs/design/present-profile.md`](docs/design/present-profile.md)

### Fixed

- Lightwave / glass FX misaligned with C1 windows: StageHost syncs RenderHost target bounds
  from named scene2d actors after `stage.act` (LayoutEngine was origin-local only)
- Demo windows stayed STS cream skin: attach now uses `StsSkin.create(tree.theme())`;
  panels get Theme `panel-bg` drawable
- Lightwave demo: `ArtFramework.tick` from StageHost; panel declared size; window pack;
  lightwave border on shader path; animation_player not layout-consuming; clickButton always OK after emit
- Probe `render.demoEffects.lightwave_demo` + target w/h/effectIds for automated effect contracts
  (JUnit + f15 + D1 `d1_lightwave_demo`)
- C1 FX draw **under** scene2d (lightwave no longer paints over labels); opaque font colors;
  Pulse/Close drive intensity flash via `LightwaveControls` (Pulse returns to slider baseline)
- C1 text via `StsTextActor` + `FontHelper.render*` (scene2d Label/TextButton glyphs unusable with
  shared STS fonts on device); buttons are Table chrome + FontHelper caption

### Known limits

- Changing project present does not rebuild already-open C1 windows (close/reopen)
- C2 without surface present node uses project chrome only
- C2 chrome beyond hand/controls still mostly hardcoded label colors
- Lightwave does not auto-enable FULL present

## 1.0.0-alpha.4

### Added

- Milestone **26** room FULL production: scene-matched `FULL_READY` for reward/rest/shop/treasure
  and combat energy/intents/proceed; real room IntentExecutor gestures; suppress patches for
  CombatRewardScreen / CampfireUI / ShopScreen / TreasureRoom; shop/treasure/proceed host paint
- Probe: `capability` / `capabilityReason` on room draw slices; `backend.input.rewardState` etc.
- Offline fixture `f13_room_full_ready`; D1 `d1_full_present_combat_chrome`
- Milestone **27**: C1 `grid` / `tabs` containers + `layouts/grid_tabs_sample.json`; beautify pack
  sample JUnit

### Changed

- Room surfaces no longer treat mount-alone as scene-ready (aligned with map/combat policy)
- Version **1.0.0-alpha.4**

### Known limits

- Shop/rest/treasure host paint remains label/chrome level (not full STS atlas fidelity)
- Boss relic / shop D1 still lab-hard; JUnit + offline fixtures are primary
- Hand select / dual-mod CrossSpire patch coexistence: see `ui-ops-probe.md` Known limits
- EntityPresent CARD never replaces combat hand FULL hard-sync

## 1.0.0-alpha.3

### Added

- Milestone **23** release hardening: API stability expansion, probe field contract,
  consumer fixture links for frames/present/CardRef, `scripts/release-gate.sh`
- Milestone **24** EntityPresent draw path: `EntitySnapshot`, `EntityDrawPath`, richer
  `entities` probe, kind-aware RenderHost bounds, skeleton PostRender when DRAW
- Milestone **25** full-present surfaces: combat proceed/energy, reward (combat/card/boss
  relic), rest, treasure, shop, top panel, combat intents — View/DrawPath/probe/JUnit;
  D1 scenarios where lab-reachable

### Changed

- Design docs status: shipped through 22; 23–25 open work closed in this release
- Consumer freeze notes updated for new surface ids and intent path (SignalBus / surface
  `action`, not `UiOps.submitIntent`)

### Known limits

- Shop/rest/treasure host paint is best-effort chrome; native STS screens remain until
  FULL_READY + suppress patches fire on device
- Boss relic / shop D1 paths are harder to script; offline fixtures + JUnit are primary
- EntityPresent CARD never replaces combat hand FULL hard-sync
- See [`docs/design/ui-ops-probe.md`](docs/design/ui-ops-probe.md) Known limits

## 1.0.0-alpha.2

- Milestones 0–22: dual-track C1/C2, UiOps/UiProbe, composition, Godot-aligned core,
  Backend/SignalBus, HostAssets, STS1 full-present combat/map/event/select, lab nav,
  dev UI console

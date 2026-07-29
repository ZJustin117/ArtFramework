# Changelog

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

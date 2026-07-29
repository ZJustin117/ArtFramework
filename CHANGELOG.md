# Changelog

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

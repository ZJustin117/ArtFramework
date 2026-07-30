# ArtFramework UI-layer verification

Prove **intercept**, **trigger**, and **C1 panel** behavior without owning CrossSpire multiplayer life suites.

## Pyramid

```
rare:     dual-device life / co-op (CrossSpire @device-scenario)
optional: single-device UI smoke (D1) after @android-deploy-jar
default:  pure JUnit (@junit-test) + offline/art-verify offline unittest
```

| Layer | Tool | When |
|-------|------|------|
| Pure API | `@junit-test` | After every ops/template/API change |
| Tooling offline | `tools/art-verify` unittest | After runner/assert changes |
| On-device UI | `@art-verify` + `ART_D1_SERIAL` | After patches / real gestures land |
| Co-op life | CrossSpire only | Never default gate here |

## What ArtFramework verifies

1. **Intercept** — bind template + interceptor BLOCK → ops/dispatch returns blocked; no fake “engine” side effect in pure tests.
2. **Trigger** — ALLOW + `UiOps` / dispatch → observable probe or Fake backend state changes.
3. **C1** — open layout → probe lists window → `clickButton` fires registered handler (FakeStage / pure).
4. **Composition** — pure `UiNode` nest/slot/ref + layout bounds (JUnit); fixture `f4_composition_tree` for sample tree shape offline.
5. **Lightwave demo effects** — `LightwaveDemoContractTest` + `f15_lightwave_demo_effects` + D1 `d1_lightwave_demo`:
   `render.demoEffects.lightwave_demo` bound/hasLightwave/borderDrawn/w/h; controls + ops; close clears bind.
   Demo-only (not native UI). Pixel look is manual; probe is the automated gate.

## What stays CrossSpire

- `crossspire probe` facets: connected / peers / party / combat powers / nav unlock
- YAML life l1–l5, dual host/join, tunnel, connector
- Protocol messages, queue submit authority

## tools/art-verify

Minimal YAML runner for **UI** scenarios (fixture or future device console).

```bash
# Offline (no device) — always safe
cd tools/art-verify && python3 -m unittest discover -s tests -v

# Fixture scenarios (no adb)
python3 tools/art-verify/run.py tests/ui-scenarios/fixtures/

# Device (lab: android-device-lab.md — connector + harness cold start + ArtFramework.jar)
set -a && source .env.local && set +a
python3 tools/art-verify/run.py tests/ui-scenarios/device/d1_probe_smoke.yaml --device
```

Env (device):

| Key | Role |
|-----|------|
| `ART_D1_SERIAL` | ADB serial |
| `STS_CONNECTOR_PORT` | Amethyst connector (shared with CrossSpire) |
| `SLAY_THE_AMETHYST_ROOT` | Import root for `scripts.tools` |
| `ART_AMETHYST_TOOLS_DIR` | Optional tools path |
| `ART_GAME_PROBE_PORT` | Default 9099 |
| `ART_UI_VERIFY_OUT_DIR` | Optional result JSON dir |

Device probe: `art probe` console → scrape `ART_PROBE` from device `sts/latest.log` if console body is only `ok`.

Lab intercept (device): `art gate map block` then `art op map …` → ops return BLOCKED; clear with `art gate all clear`. See `d1_gate_block_ops.yaml`.

Dev UI inspect (device): `art open demo` then `art ui list` / `art ui tree demo` / `art ui emit demo/… pressed`.
Log prefix `ART_UI` (and `ART_UI_SIGNAL` for `art ui listen`).  
Commands: [`console-commands.md`](./console-commands.md). Design: [`dev-ui-console.md`](../design/dev-ui-console.md).

Lab run nav (device): `art lab dump` / `art lab ensure-fresh-menu` / `art lab start-run IRONCLAD`.  
Log prefix `ART_LAB`. Design: [`lab-run-nav.md`](../design/lab-run-nav.md). YAML: `d1_lab_*.yaml`.

OpenCode: `@art-verify` after deploy when validating on-device UI; **not** a substitute for `@junit-test`.

## YAML v1 (UI)

```yaml
name: f1_probe_shape
schemaVersion: 1
mode: fixture          # fixture | device
fixture: fixtures/sample_probe.json
steps:
  - assert:
      path: schemaVersion
      eq: 1
  - assert:
      path: templates.mapBound
      eq: true
```

Device steps (when console exists):

| step | Role |
|------|------|
| `probe` | Run `art probe` (or read last `ART_PROBE` line from log) |
| `op` | `art op …` (future) |
| `console` | Raw BaseMod console string |
| `wait_ms` | Sleep |
| `wait_probe` | Poll `art probe` until its nested `assert` succeeds; supports `timeout_ms` / `interval_ms` |
| `assert` | Same path operators as fixture mode |

Combat-ready device validation may use BaseMod's `fight Cultist` after `art lab start-run` has
initialized a run. It requires a current map node, so it cannot run from the title menu. See
`d1_full_present_combat_ready.yaml`; it arms ART FULL before the room transition and waits for the
actual `FULL_READY` probe state rather than relying on a fixed combat delay.

## Delegation order

1. Code change  
2. `@junit-test`  
3. `tools/art-verify` offline unittest (if runner touched)  
4. `@android-deploy-jar` (D1) if jar needed  
5. `@art-verify` device smoke (optional)

## Related

- [`console-commands.md`](./console-commands.md)
- [`ui-ops-probe.md`](../design/ui-ops-probe.md)
- [`logic-layer-testing.md`](./logic-layer-testing.md)
- [`android-deploy.md`](./android-deploy.md)

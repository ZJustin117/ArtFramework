# SpireUI UI-layer verification

Prove **intercept**, **trigger**, and **C1 panel** behavior without owning CrossSpire multiplayer life suites.

## Pyramid

```
rare:     dual-device life / co-op (CrossSpire @device-scenario)
optional: single-device UI smoke (D1) after @android-deploy-jar
default:  pure JUnit (@junit-test) + offline/ui-verify offline unittest
```

| Layer | Tool | When |
|-------|------|------|
| Pure API | `@junit-test` | After every ops/template/API change |
| Tooling offline | `tools/ui-verify` unittest | After runner/assert changes |
| On-device UI | `@ui-verify` + `SPIREUI_D1_SERIAL` | After patches / real gestures land |
| Co-op life | CrossSpire only | Never default gate here |

## What SpireUI verifies

1. **Intercept** — bind template + interceptor BLOCK → ops/dispatch returns blocked; no fake “engine” side effect in pure tests.
2. **Trigger** — ALLOW + `UiOps` / dispatch → observable probe or Fake backend state changes.
3. **C1** — open layout → probe lists window → `clickButton` fires registered handler (FakeStage / pure).
4. **Composition** — pure `UiNode` nest/slot/ref + layout bounds (JUnit); fixture `f4_composition_tree` for sample tree shape offline.

## What stays CrossSpire

- `crossspire probe` facets: connected / peers / party / combat powers / nav unlock
- YAML life l1–l5, dual host/join, tunnel, connector
- Protocol messages, queue submit authority

## tools/ui-verify

Minimal YAML runner for **UI** scenarios (fixture or future device console).

```bash
# Offline (no device) — always safe
cd tools/ui-verify && python3 -m unittest discover -s tests -v

# Fixture scenarios (no adb)
python3 tools/ui-verify/run.py tests/ui-scenarios/fixtures/

# Device (lab: android-device-lab.md — connector + harness cold start + SpireUI.jar)
set -a && source .env.local && set +a
python3 tools/ui-verify/run.py tests/ui-scenarios/device/d1_probe_smoke.yaml --device
```

Env (device):

| Key | Role |
|-----|------|
| `SPIREUI_D1_SERIAL` | ADB serial |
| `STS_CONNECTOR_PORT` | Amethyst connector (shared with CrossSpire) |
| `SLAY_THE_AMETHYST_ROOT` | Import root for `scripts.tools` |
| `SPIREUI_AMETHYST_TOOLS_DIR` | Optional tools path |
| `SPIREUI_GAME_PROBE_PORT` | Default 9099 |
| `SPIREUI_UI_VERIFY_OUT_DIR` | Optional result JSON dir |

Device probe: `spireui probe` console → scrape `SPIREUI_PROBE` from device `sts/latest.log` if console body is only `ok`.

Lab intercept (device): `spireui gate map block` then `spireui op map …` → ops return BLOCKED; clear with `spireui gate all clear`. See `d1_gate_block_ops.yaml`.

OpenCode: `@ui-verify` after deploy when validating on-device UI; **not** a substitute for `@junit-test`.

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
| `probe` | Run `spireui probe` (or read last `SPIREUI_PROBE` line from log) |
| `op` | `spireui op …` (future) |
| `console` | Raw BaseMod console string |
| `wait_ms` | Sleep |
| `assert` | Same path operators as fixture mode |

## Delegation order

1. Code change  
2. `@junit-test`  
3. `tools/ui-verify` offline unittest (if runner touched)  
4. `@android-deploy-jar` (D1) if jar needed  
5. `@ui-verify` device smoke (optional)

## Related

- [`ui-ops-probe.md`](../design/ui-ops-probe.md)
- [`logic-layer-testing.md`](./logic-layer-testing.md)
- [`android-deploy.md`](./android-deploy.md)

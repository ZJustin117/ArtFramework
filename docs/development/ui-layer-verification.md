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

## What stays CrossSpire

- `crossspire probe` facets: connected / peers / party / combat powers / nav unlock
- YAML life l1–l5, dual host/join, tunnel, connector
- Protocol messages, queue submit authority

## tools/ui-verify

Minimal YAML runner for **UI** scenarios (fixture or future device console).

```bash
# Offline (no device) — always safe
cd tools/ui-verify && python3 -m unittest discover -s tests -v

# Run fixture scenarios (no adb)
python3 tools/ui-verify/run.py tests/ui-scenarios/fixtures/f1_probe_shape.yaml

# Device mode (requires SPIREUI_D1_SERIAL + game + future spireui probe)
python3 tools/ui-verify/run.py tests/ui-scenarios/smoke/s1_mod_loaded.yaml --device
```

Env:

| Key | Role |
|-----|------|
| `SPIREUI_D1_SERIAL` | Default device (same value as CrossSpire `CROSSSPIRE_D1_SERIAL`, **SPIREUI_*** name) |
| `SPIREUI_D2_SERIAL` | Optional; not required for UI smoke |
| `SPIREUI_UI_VERIFY_OUT_DIR` | Optional result JSON dir (else `debug-artifacts/ui-verify`) |

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

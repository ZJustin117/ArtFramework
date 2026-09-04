# ArtFramework UI-layer verification

Prove **intercept**, **trigger**, and **C1 panel** behavior without owning multiplayer life suites.

## Pyramid

```
rare:     dual-device life / co-op (out of repo)
optional: single-device UI smoke (D1) after @android-deploy-jar
default:  pure JUnit (@junit-test) + offline/art-verify offline unittest
```

| Layer | Tool | When |
|-------|------|------|
| Pure API | `@junit-test` | After every ops/template/API change |
| Tooling offline | `tools/art-verify` unittest | After runner/assert changes |
| On-device UI | `@art-verify` + `ART_D1_SERIAL` | After patches / real gestures land |
| JVM diagnose | `@android-arthas` | Explicit thread/classloader/method investigations only |
| Co-op life | Out of repo | Never default gate here |

## What ArtFramework verifies

1. **Intercept** — bind template + interceptor BLOCK → ops/dispatch returns blocked; no fake “engine” side effect in pure tests.
2. **Trigger** — ALLOW + `UiOps` / dispatch → observable probe or Fake backend state changes.
3. **C1** — open layout → probe lists window → `clickButton` fires registered handler (FakeStage / pure).
4. **Composition** — pure `UiNode` nest/slot/ref + layout bounds (JUnit); fixture `f4_composition_tree` for sample tree shape offline.
5. **Lightwave demo effects** — `LightwaveDemoContractTest` + `f15_lightwave_demo_effects` + D1 `d1_lightwave_demo`:
   `render.demoEffects.lightwave_demo` bound/hasLightwave/borderDrawn/w/h; controls + ops; close clears bind.
6. **Present production (35)** — `PresentProductionTest`: project hot restyle, `SurfacePresent`, packId → HostAssets,
   third-party profile sample; optional open `glass_lightwave_demo`.
7. **PresentProfile catalog (37)** — `PresentProfileCatalogTest` + fixture `f16_present_profiles_catalog` +
   D1 `d1_present_profiles_catalog`: register ≠ apply; probe `presentProfiles.ids` / `byId`; project set + demo fromProject.
8. **PresentPack (38)** — `PresentPackTest` + f18 + D1 `d1_present_packs`: pack activate by packId, regex select.
    Demo-only (not native UI). Pixel look is manual; probe is the automated gate.
9. **C2 Lightwave chrome/FX (43)** — `C2LightwaveSurfaceTest` + f18 surface effect contract + D1
   `d1_lightwave_c2_full`: profile activation, C2 effect targets, combat FULL readiness, and cleanup
   after returning to `sts`. Visual correctness remains manual on D1.
10. **Skeleton + EntityPresent lifecycle (46.5)** — D1 `d1_spine34_native_takeover` proves a real
    creature skeleton is claimed and rendered without provider errors, then panic cleanup releases
    runtime bindings/claims. `d1_entity_present_smoke` proves an ECS slot reaches the draw path and
    RenderPlan target, then detach removes all three views.

## Out of scope

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
| `STS_CONNECTOR_PORT` | Amethyst connector |
| `SLAY_THE_AMETHYST_ROOT` | Import root for `scripts.tools` |
| `ART_AMETHYST_TOOLS_DIR` | Optional tools path |
| `ART_GAME_PROBE_PORT` | Default 9099 |
| `ART_UI_VERIFY_OUT_DIR` | Optional result JSON dir |
| `ART_SPINE42_REFERENCE_PNG` | Required local reference PNG for `d1_spine42_screenshot` |
| `ART_SPINE42_CROP` | Required `X,Y,W,H` crop for `d1_spine42_screenshot`; set for the device orientation/capture geometry |
| `ART_SPINE42_DIFF_PNG` | Optional local diff PNG path when used by a screenshot scenario |

Device probe: `art probe` console → scrape `ART_PROBE` from device `sts/latest.log` if console body is only `ok`.

### Real screenshot pixel parity (D1 only)

After a D1 Harness run has produced a real GL `screencap` PNG, compare it with the reference image using the dependency-free tool:

```bash
python3 tools/art-verify/compare_png.py \
  tests/ui-scenarios/references/example.png \
  debug-artifacts/harness/screencap.png \
  --threshold 2 --max-diff-pixels 20 --max-diff-ratio 0.001 \
  --diff debug-artifacts/harness/screencap.diff.png
```

The tool checks dimensions, reports the maximum channel error and the count/ratio of pixels over the per-pixel threshold, and optionally writes a diff PNG. It supports non-interlaced 8-bit RGB/RGBA PNGs without extra dependencies. This is only real GL evidence when the actual image came from D1 Harness `screencap`; offline YAML fixtures and offline tool tests cannot establish or claim pixel parity.

Device `screenshot` steps use a bounded 30-second Harness subprocess timeout by default. Override it with the positive `ART_SCREENSHOT_TIMEOUT_SECONDS` environment key when necessary; a timeout fails the scenario instead of leaving it running indefinitely.

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
| `screenshot` | Device-only: invokes the existing Amethyst Harness `screenshot` command and records its result JSON and PNG |
| `compare_screenshot` | Device-only: compares the most recent screenshot with a local reference; supports optional `reference_kind`, `crop`, `threshold`, `max_diff_pixels`, `max_diff_ratio`, and `diff` |

`screenshot` is skipped in fixture mode because fixtures cannot produce a real GL image. In device mode it
requires `ART_D1_SERIAL`, `STS_CONNECTOR_PORT`, `ART_GAME_PROBE_PORT`, `ART_HARNESS_OUT_DIR`, and either
`ART_AMETHYST_TOOLS_DIR` or `SLAY_THE_AMETHYST_ROOT`; the runner does not manage Harness or connector lifecycle.
Use `tests/ui-scenarios/device/d1_spine42_screenshot.yaml` for a Spine42 `idle_loop` capture. The resulting PNG
is real D1 GL evidence only. Its developer-local reference must still be captured independently through the
native capture workflow; the scenario does not generate native pixels.
`compare_screenshot` uses the same comparator inline. It requires a prior screenshot, resolves relative
reference and diff paths from the scenario file, and also accepts a whole-path `${ENV_KEY}` reference.
`crop` accepts either a four-integer list or a whole-value `${ENV_KEY}` with an `X,Y,W,H` value; an unset or
invalid crop key reports that key and its configuration error. The Spine42 scenario requires
`ART_SPINE42_REFERENCE_PNG` and orientation-specific `ART_SPINE42_CROP`; `ART_SPINE42_DIFF_PNG` is optional
for scenarios that want to configure diff output. Its `reference_kind: native_capture` is recorded in the
comparison result: the developer-local reference must be a paired native capture from the same fixed frozen
state, while the scenario itself captures only ART output and does not generate native pixels. The step records
metrics and artifacts in the result JSON and fails on missing/invalid inputs, size mismatch, or configured
limits. Do not commit reference images.

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

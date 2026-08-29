# Native Render Ownership Reviews

Status: historical and frozen. Findings and PASS results below are preserved as review facts
for their original revisions. Current policy supersedes the R2/R3/R5 blanket
native-continuation conclusions: manifest `ART_DELEGATED` surfaces may now suppress per
invocation only when FULL_READY, with fail-open recovery and strict draw-evidence closure.
Transient effects remain `CAPTURE_AND_PASS`; this superseding note does not rewrite the
historical findings.

## Baseline - Explore Audit

- Session: `ses_fc86b09b2ffeUkSx5KgMQvOd0X`
- Scope: frozen baseline audit of STS1 render patches, `Sts1SurfaceRenderer`, native bridge, docs, and tests.
- Result: FINDINGS
- Findings:
  - `BASE-01` high, accepted, pending: combat hand suppresses `AbstractPlayer.renderHand` and hand-builds `AbstractCard` pixels.
  - `BASE-02` high, accepted, pending: controls, energy, intents, map, event, select, reward, rest, shop, and treasure are full native pixel rewrites.
  - `BASE-03` medium, accepted, pending: NRCC docs prohibit rewriting STS render paths but still allow broad `DELEGATE_TO_ART` without ownership manifest enforcement.
  - `BASE-04` medium, accepted, pending: tests cover bridge disposition mechanics but not every patch adapter or native continuation invariant.
- Residual risk: skeleton/native provider overlap and transient effect cleanup need a later focused review.

## Round R1 - Hand/Card Native Render Restoration

- Session: `ses_fc7ef8accffe6vEuYyRxCpnyT3` (implementation attempts), parent session completion
- Scope: `Sts1HandCardRenderer`, `Sts1SurfaceRenderer`, `Sts1VanillaDraw`, `Sts1HandCardRendererTest`, `build.gradle.kts`
- Result: PASS (no actionable finding within slice scope)
- Findings:
  - `R1-01` high, accepted, fixed, verified: `Sts1HandCardRenderer` no longer hand-builds card pixels; it syncs pose onto the live card and calls `AbstractCard.render(sb)`.
  - `R1-02` high, accepted, fixed, verified: hand-built card pixel helpers (`drawCardBackground`, `drawCardPortrait`, `drawCardFrame`, `drawRotated`, etc.) were removed from `Sts1VanillaDraw` because they were only used by the old hand renderer.
  - `R1-03` medium, accepted, fixed, verified: `build.gradle.kts` now includes STS/BaseMod/MTS jars on `testImplementation` so tests can reference `SpriteBatch` and other host types.
  - `R1-04` low, accepted, fixed, verified: `Sts1HandCardRendererTest` asserts native-card authority and returns 0 when no live card is supplied.
- Evidence:
  - JUnit gate: 766 tests passed, `BUILD SUCCESSFUL`.
  - D1 combat verify: `pass d1_full_present_combat_ready`.
  - D1 screenshot: `/home/justinz/SpireUI/debug-artifacts/harness/20260825-095210-996221/sts-screen-20260825-095211-006989.png`
- Residual risk: duplicate-card identity still maps by `cardId` fallback in `Sts1VanillaDraw` if any helpers remain; this path is now unused for hand cards but other surfaces may still use similar lookup and should be reviewed in later slices.

## Round R3 - Map/Event/Select/Room Native Render Restoration

- Session: (current)
- Scope: `MapRenderPatches`, `EventRenderPatches`, `SelectRenderPatches`, `RoomRenderPatches`, `SurfaceDrawPlan`, `Sts1SurfaceRenderer`, `MapDrawPathTest`, `EventDrawPathTest`, `SelectDrawPathTest`, `NativeRenderBridgeTest`, `Sts1RenderPipelineTest`
- Result: PASS (no actionable finding within slice scope)
- Findings:
  - `R3-01` high, accepted, fixed, verified: `MapRenderPatches`, `EventRenderPatches`, `SelectRenderPatches`, and `RoomRenderPatches` no longer return `SpireReturn.Return(null)`; they observe via `NativeRenderBridge.beginSurface` and let the native renderers continue.
  - `R3-02` high, accepted, fixed, verified: `SurfaceDrawPlan` treats `MAP`, `EVENT`, `SELECT_GRID`, `SELECT_HAND`, `REWARD_COMBAT`, `REWARD_CARD`, `REWARD_BOSS_RELIC`, `REST`, `SHOP`, and `TREASURE` as native-pixel-authoritative surfaces, so `entry.suppressNative` stays false and `NativeRenderBridge.beginSurface` produces `PASS_THROUGH` rather than `DELEGATE_TO_ART`.
  - `R3-03` high, accepted, fixed, verified: `Sts1SurfaceRenderer.renderMap`, `renderEvent`, `renderSelect`, `renderReward`, `renderRest`, `renderShop`, and `renderTreasure` no longer draw replacement pixels; C2 items are still synced in `prepareMapVisuals`/`prepareEventVisuals`/`prepareSelectVisuals`/`prepareRewardVisuals` for input/hit geometry.
  - `R3-04` medium, accepted, fixed, verified: `MapDrawPathTest`, `EventDrawPathTest`, `SelectDrawPathTest`, `NativeRenderBridgeTest`, and `Sts1RenderPipelineTest` expectations were updated to assert that these surfaces never suppress native rendering in FULL mode.
- Evidence:
  - JUnit gate (full): 769 tests passed, `BUILD SUCCESSFUL`.
  - D1 combat verify: `pass d1_full_present_combat_ready`.
  - D1 screenshot: `/home/justinz/SpireUI/debug-artifacts/harness/20260825-111610-606246/sts-screen-20260825-111610-614752.png`
- Residual risk: later slice NRO-04 must apply the same native-authority policy to skeleton/effect surfaces; the `SurfaceDrawPlan.keepsNativePixelAuthority` helper is scoped to all non-hand/card surfaces except skeleton/effect.

## Round R2 - Combat Controls/Energy/Intents Native Render Restoration

- Session: parent session completion
- Scope: `CombatControlsRenderPatches`, `CombatEnergyRenderPatches`, `CombatIntentRenderPatches`, `Sts1SurfaceRenderer`, `SurfaceDrawPlan`, `ControlsDrawPathTest`, `Sts1RenderPipelineTest`, `NativeRenderBridgeTest`
- Result: PASS (no actionable finding within slice scope)
- Findings:
  - `R2-01` high, accepted, fixed, verified: `CombatControlsRenderPatches`, `CombatEnergyRenderPatches`, and `CombatIntentRenderPatches` no longer return `SpireReturn.Return(null)`; they observe via `NativeRenderBridge.beginSurface` and let the native renderers continue.
  - `R2-02` high, accepted, fixed, verified: `SurfaceDrawPlan` treats `COMBAT_CONTROLS`, `COMBAT_ENERGY`, and `COMBAT_INTENTS` as native-pixel-authoritative surfaces, so `entry.suppressNative` stays false and `NativeRenderBridge.beginSurface` produces `PASS_THROUGH` (nativeContinuation=true) rather than `DELEGATE_TO_ART`.
  - `R2-03` high, accepted, fixed, verified: `Sts1SurfaceRenderer.renderControls`, `renderEnergy`, and `renderIntents` no longer draw replacement pixels; C2 items are still synced in `prepareControlsVisuals`/`prepareIntentVisuals` for input/hit geometry.
  - `R2-04` medium, accepted, fixed, verified: `ControlsDrawPathTest` and `Sts1RenderPipelineTest` expectations were updated to assert that controls/energy/intents never suppress native rendering, and `NativeRenderBridgeTest` asserts `PASS_THROUGH` dispositions for these surfaces in FULL mode.
- Evidence:
  - JUnit gate (full): 769 tests passed, `BUILD SUCCESSFUL`.
  - D1 combat verify: `pass d1_full_present_combat_ready` after updating `tests/ui-scenarios/device/d1_full_present_combat_ready.yaml` to expect `backend.controlsDraw.suppressNativeEndTurn == false`.
  - D1 screenshot: `/home/justinz/SpireUI/debug-artifacts/harness/20260825-111610-606246/sts-screen-20260825-111610-614752.png`
- Residual risk: later slices (NRO-03/NRO-04) must apply the same native-authority policy to map/event/select/room/skeleton/effect surfaces; the `SurfaceDrawPlan.keepsNativePixelAuthority` helper is intentionally scoped to controls/energy/intents only.

## Round R5 - Static Manifest and Native Pixel Authority Gate

- Session: (current)
- Scope: `docs/design/native-render-coverage-sdd.md`, `docs/design/c2-full-present.md`, `tools/nrcc/coverage_manifest.py`, `tools/nrcc/manifests/sts1-native-coverage.yaml`, `RenderPatchOwnershipTest`, `SurfaceDrawPlan.keepsNativePixelAuthority`
- Result: PASS (no actionable finding within slice scope)
- Findings:
  - `R5-01` high, accepted, fixed, verified: NRCC docs now define the native-pixel-authority rule: `DELEGATE_TO_ART` is only allowed for ART-owned surfaces/effects with no STS native renderer; a patch returning `SpireReturn.Return(null)` requires a documented ownership justification and a focused test.
  - `R5-02` high, accepted, fixed, verified: `NATIVE_WITH_ART_OVERLAY` policy added to the manifest; controls, energy, intents, map, event, select, reward, rest, shop, and treasure surfaces are classified as `NATIVE_WITH_ART_OVERLAY` (native pixels continue, ART may add non-authoritative overlays).
  - `R5-03` high, accepted, fixed, verified: `tools/nrcc/coverage_manifest.py` now enforces that any render/draw patch with `SpireReturn.Return(null)` has an `ART_DELEGATED` manifest entry with non-empty `justification` and `test` fields.
  - `R5-04` high, accepted, fixed, verified: `RenderPatchOwnershipTest` added to scan patch source files and assert that only `CombatHandRenderPatches`, `SkeletonRenderPatches`, and `TransientEffectRenderPatches` suppress native rendering; controls/energy/intents and map/event/select/room patches are asserted to not contain `SpireReturn.Return(null)`.
  - `R5-05` medium, accepted, fixed, verified: `SurfaceDrawPlan.keepsNativePixelAuthority` is package-private and tested to contain the expected surface ids; `sts1.combat.hand` remains the only surface allowed to suppress native rendering.
  - `R5-06` medium, accepted, fixed, verified: `docs/design/c2-full-present.md` updated to clarify that C2 is an invocation-boundary/overlay/input layer, not a replacement renderer, except for explicitly ART-owned surfaces.
- Evidence:
  - JUnit gate (full): all tests passed, `BUILD SUCCESSFUL`.
  - Python gate: `tools/nrcc/tests` passed (16 tests).
  - NRCC manifest check: `ok: true`, `ownershipErrors: []` after regenerating `tools/nrcc/manifests/sts1-native-coverage.yaml`.
  - D1 combat verify: `pass d1_full_present_combat_ready`.
  - D1 screenshot: `/home/justinz/SpireUI/debug-artifacts/harness/20260825-141431-023778/sts-screen-20260825-141431-050497.png`
- Residual risk: the static manifest still contains many `UNKNOWN` entries for unpatched native methods; these are acceptable as long as no render patch suppresses them without an `ART_DELEGATED` entry. Future work can classify additional surfaces as needed.

## Round R4 - Skeleton/Effect Native Render Ownership

- Session: (current)
- Scope: `SkeletonRenderPatches`, `TransientEffectRenderPatches`, `Sts1SkeletonBridge`, `SurfaceDrawPlan`, `Sts1SurfaceRenderer`, `Sts1SkeletonBridgeTest`, `NativeRenderBridgeTest`, `Sts1RenderPipelineTest`
- Result: PASS (no actionable finding within slice scope)
- Findings:
  - `R4-01` high, accepted, fixed, verified: `SkeletonRenderPatches` only suppresses `SkeletonMeshRenderer.draw` for per-instance ART claims; unclaimed skeletons always `Continue()`. A delegated claim always returns `SpireReturn.Return(null)` so the native draw is never also executed.
  - `R4-02` high, accepted, fixed, verified: `Sts1SkeletonBridge` introduced provider-agnostic `SkeletonNativeSlotRenderer` interface; `canRenderClaimedNative` and `renderClaimedNative` accept any provider implementing it, so non-native backends can claim skeleton instances without double draw.
  - `R4-03` high, accepted, fixed, verified: `SurfaceDrawPlan` treats `SKELETON` as native-pixel-authoritative, so the surface-level plan never requests wholesale native suppression.
  - `R4-04` high, accepted, fixed, verified: `Sts1SurfaceRenderer.renderSkeleton` does not draw replacement or fallback pixels; it only asks the active provider to render.
  - `R4-05` high, accepted, fixed, verified: `TransientEffectRenderPatches` remains observe-only (`CAPTURE_AND_PASS`) and the native effect queue continues.
  - `R4-06` medium, accepted, fixed, verified: focused JUnit tests assert unclaimed skeleton passes through, claimed skeleton delegates with no native continuation, host recreation restores the claim, transient effects always capture and pass, and the skeleton surface never suppresses native wholesale.
- Evidence:
  - JUnit gate (full): all tests passed, `BUILD SUCCESSFUL`.
  - D1 combat verify: `pass d1_full_present_combat_ready`.
  - D1 screenshot: `/home/justinz/SpireUI/debug-artifacts/harness/20260825-130143-653636/sts-screen-20260825-130143-662489.png`
- Residual risk: the new `SkeletonNativeSlotRenderer` interface is a runtime capability check; future 3rd-party providers must implement it to claim a native slot safely.

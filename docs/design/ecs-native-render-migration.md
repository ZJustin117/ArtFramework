# ECS Native Render Migration

Status: active migration; Background remains gated on full in-run render ownership.

## Target

STS remains authoritative for game rules, state mutation, and lifecycle. Its adapter samples
display-relevant values into immutable host-neutral ECS components. Stateless ECS systems derive
an immutable ordered frame. One ART backend consumes that frame and owns pixels for migrated
content. Native renderers are migration references only; they must not remain pixel authorities
for the same content after a family is accepted.

ECS completion does not mean that an ECS component exists for every native method. A family is
migrated only when its sampled input, deterministic ECS projection, ART backend consumer, native
pixel suppression, recovery behavior, and draw evidence have all been verified together.

## Implemented foundation

- `NativeRenderInputComponent` is immutable, host-neutral data: family, owner, scene/frame,
  phase/z/stable key, bounds, visibility, and ownership disposition.
- `NativeRenderFrameExtractor` creates an immutable snapshot sorted by `(phase.rank, z,
  stableKey)` and rejects duplicate stable keys.
- `NativeRenderInputSystem` is a stateless system executed by the sole `PresentationSchedule`
  during `RENDER_PROJECTION`.
- Transient effect observers project their NRCC family (via the controlled
  `NativeRenderFamilyClassifier` subset of `families.py`) as `OBSERVED`. This records data only;
  native VFX drawing continues and no ART pixel evidence is manufactured.
- Delegating surface projections publish ownership derived from the bridge disposition:
  `DELEGATE_TO_ART` → `DELEGATED_TO_ART`, `CAPTURE_AND_PASS`/`PASS_THROUGH` →
  `NATIVE_WITH_ART_OVERLAY`, `FAIL_OPEN`/`BLOCKED` → `OBSERVED`. Non-projecting branches
  (observe-only and native-retained pass) never fabricate an ECS input.
- `RenderPlan.appendNativeRetainedEntries` consumes the ECS input snapshot for detached
  NATIVE_RETAINED entries and skips `DELEGATED_TO_ART`; entities without an input still fall back
  to the presentation-frame entry, so surface owners are not dropped.
- The ART-authored VFX bundle runtime (not a native family) publishes each root's payload entries
  into the shared `ArtRenderFrame` from the ECS projection system, keyed by a per-root
  `producerId`. Each draw maps to a host-neutral `RenderPixelPayload` (`resourceId`/label, geometry,
  UV source rect, flip, rotation, scale, rgba, blend, flipbook). The STS1 backend consumes the
  shared frame segmented by producer and submits through `Sts1VfxOverlayRenderer`; it no longer
  reads ECS draw state on the pixel path. The payload→draw-parameter mapping is a pure function
  shared with the legacy draw path, and a field-for-field payload parity test still proves equality.

`RenderPlan` is the single frame authority. `RenderPlan.unifiedFrame(identityEntries,
payloadEntries)` merges payload-less identity/geometry entries (for example `NATIVE_RETAINED`
entries that keep native pixels) with payload-bearing ART entries, sorted by `(phase, z,
stableKey)`. Payload never participates in ordering, de-duplication, or ownership.

`RenderPlan` remains the ordered render-target plan for host targets. In parallel, the shared
`ArtRenderFrame` is the multi-producer payload aggregation point: producers publish
`RenderPlan.Entry` payload lists via `ArtRenderContributionComponent` (keyed by producer id), a
stateless `ArtRenderFrameAggregationSystem` in `RENDER_PROJECTION` merges them into one immutable
`ArtRenderFrame` sorted by `(phase.rank, z, stableKey)`, and consumers read it via
`ArtRenderFrameComponent.read` plus owner-based `entriesFor(producerId)`. Each entry keeps a
structured producer owner, so consumers filter by identity rather than a `stableKey` prefix.
Duplicate stable keys are rejected; the rejection is contained at the aggregation boundary (the
safe empty frame is published, later schedule phases still run) and the cumulative count/reason are
recorded on the frame-independent `ArtRenderFrameDiagnosticsComponent`, so they survive empty ticks.
`ArtRenderFrame` holds no host object; it is not a second mutable draw state.

The native input snapshot is still not a full backend frame for native families: `NATIVE_RETAINED`
entries carry no payload, so observed native families remain native-drawn. ART-authored VFX has an
end-to-end ECS→frame→payload→pixels path, but that is not a migration of observed native VFX.

## Family migration ledger

The family ids below are governed by `tools/nrcc/families.py`; static path classification is
inventory, not pixel coverage. Update this table only with implementation and verification
evidence for the corresponding family.

| Family | Current status | Migration requirement |
|---|---|---|
| `vfx-misc-root` | ECS observation input for transient effect instances; native draw continues | Capture sufficient immutable visual parameters, add ART frame/backend consumer, then suppress only claimed instances with correlated evidence and fail-open recovery |
| `vfx-combat` | ECS observation input via effect classifier; native draw continues | Same per-instance path as `vfx-misc-root`; queue itself remains STS lifecycle authority |
| `vfx-scene-world` | ECS observation input via effect classifier; native draw continues | Same per-instance path; preserve scene ordering and lifecycle |
| `vfx-campfire-rest` | ECS observation input via effect classifier; native draw continues | Same per-instance path; preserve room-specific timing |
| `vfx-card-manipulation` | ECS observation input via effect classifier; native draw continues | Same per-instance path; card state remains STS authority |
| `vfx-stance-aura` | ECS observation input via effect classifier; native draw continues | Same per-instance path; preserve creature attachment and cleanup |
| `skeleton-runtime` | Per-instance claimed skeleton adapter | Move pose/asset draw input behind the common frame/backend contract; retain per-instance claim and recovery gate |
| `inrun-fullscreens` | Partial surface delegation with documented pixel gaps | Replace each surface's bespoke draw branch with common ECS frame input and backend item; close parity and evidence per screen |
| `buttons-controls` | Partial surface delegation with documented pixel gaps | Sample control state/geometry into ECS; common ART backend owns each delegated control's pixels |
| `hud-top-panel` | Partial surface delegation with documented pixel gaps | Project all HUD content and animation state; common ART backend owns delegated HUD pixels |
| `room-shells` | Overlay chrome migrated to the frame path: stateless `RoomShellRenderProjectionSystem` publishes an immutable `RoomShellRenderFrame` (payload entry for the shell image + host-side title); `Sts1RoomShellDrawPath` consumes the published frame only and never re-reads the observation at draw time. Ownership stays `NATIVE_WITH_ART_OVERLAY`; native room pixels continue | Still needs room-specific native content accounting plus parity/evidence for the native room body before any suppression; the title remains a host-side text draw (payload cannot express `FontHelper` without a pixel change) |
| `event-dialogs` | Partial surface delegation with documented pixel gaps | Project full dialog/options state and draw through common backend |
| `shop-rewards-chests` | Partial surface delegation with documented pixel gaps | Project every visible entry and interaction state; common backend owns pixels |
| `overlay-targeting` | Observe-first; native targeting pixels authoritative | Project targeting geometry/state into ECS and draw through common backend before enabling suppression |
| `monsters-bosses` | Native body pixels; observed anchors and optional claimed skeleton | Use ECS entity/pose inputs and common backend for each explicitly claimed creature; no unclaimed native suppression |
| `player-character` | Native player pixels; partial hand layout delegation | Project body/orbs/blights/stat/hover visuals; make card rendering a separately specified family boundary |
| `cards-piles-soul` | Native card pixels; pile/soul chrome overlay | Define full immutable card visual snapshot and backend path before changing the explicit `AbstractCard.render` boundary |
| `orbs` | Native pixels plus optional ART overlay | Project orb state and animation; transfer pixel ownership only after backend parity |
| `relics-blights-potions` | Native pixels plus optional ART overlay | Project item identity, visual state, cooldown/animation, and draw ordering into ECS |
| `stances-state` | Draw-input observation and shared-frame payload shipped: `OrbStanceView.Entry` collects angle/rgba/center/size/scale/hasImage and `StanceRenderProjectionSystem` publishes it to the shared `ArtRenderFrame` (producerId `stance:<id>`); the ART-side real draw (`StanceArtRenderer`, additive blend, rotation `-angle`) and production consumption of the DELEGATE token (`recordStanceDraw\|Failure`) are shipped; the default-off `beginStanceRender` + `StanceRenderPatches` seam is shipped. **But vanilla `AbstractStance.render` is a no-op** (vanilla never sets `img` and ships no stance textures), so that seam suppresses no pixels on vanilla — vanilla stance pixels actually come from `vfx-stance-aura` (`StanceAuraEffect` / `CalmParticleEffect` / `WrathParticleEffect` / `DivinityParticleEffect`, queued into `effectsQueue` by `updateAnimation`). | The family that actually needs takeover is `vfx-stance-aura` (per-instance effect takeover through the shared VFX request/frame path); the `AbstractStance.render` seam only has pixel meaning when a mod supplies an `img` texture and should stay optional/default-off. Keep the per-stance D1 pixel/order evidence requirement |
| `map-graph` | Native map graph pixels; partial screen-level delegation | Project nodes, edges, pan/zoom, legend, selection and pins into ECS/backend |
| `draw-primitives-tips` | Native shared primitives, deliberately unhooked | Replace or explicitly retain as a host primitive service; it cannot silently draw migrated ART content |
| `word-tip-ui` | Native shared text/layout helpers | Project text/layout into ART backend or explicitly classify as a non-content host service |
| `core-game-root` | Structural native orchestration/passthrough | Keep frame scheduling/state authority in host adapter; ensure it does not directly draw content claimed by ART |
| `meta-outofrun-screens` | Explicitly out of current in-run scope | Remains outside this migration unless scope is expanded |

## Required pipeline

```text
STS state and lifecycle
  -> host adapter snapshots
  -> host-neutral ECS components
  -> stateless projection systems
  -> immutable ordered frame
  -> ART backend resource resolution and pixel submission
```

The backend may retain disposable GPU/resource caches, but no game-state authority. Cache
recreation must be derivable from ECS/frame input. Render systems cannot call drawing APIs, and
components cannot contain host objects. Native continuations are permitted only for families that
are not yet migrated; an invocation must never produce both native and ART pixels for the same
content.

## Background gate

Background implementation resumes only after:

1. The common immutable frame has a real backend consumer; it is not merely validated and dropped.
   (ART-authored VFX meets this; observed native families do not yet.)
2. Every in-run family is either migrated to ART pixel ownership or has an explicit, reviewed
   exclusion that cannot paint over/under the background unexpectedly.
3. The native invocation inventory and runtime ledger close with no unknown/uncovered draw paths
   in the selected background scenario.
4. Each migrated family has tests for snapshot determinism, native suppression ownership, draw
   evidence, recovery/fail-open, and D1 pixel/order evidence.
5. Background is inserted as the lowest item in that same frame/backend path, not as a parallel
   rendering loop or a post-render overlay.

## Current position

Verified on the pure-semantic gate (`./scripts/with-art-env.sh test`, 1447 tests, 0 failures).

Done: host-neutral input contract and immutable snapshot; effect-family classification; single
schedule integration; disposition-derived ownership with reconcile; `RenderPlan` consumption of the
ECS input with de-duplication and `DELEGATED_TO_ART` exclusion; recovery cleanup of surface inputs;
`RenderPixelPayload` contract and `RenderPlan.unifiedFrame`; ART-authored VFX end-to-end
ECS→frame→payload→pixels with legacy parity; shared multi-producer `ArtRenderFrame` aggregation
with owner-based consumer filtering, contained duplicate rejection, and durable rejection
diagnostics; `room-shells` overlay chrome migrated to the same ECS→frame→payload→backend path
(overlay-only, non-delegated).

Remaining before Background: the migrated families are overlay-only and suppress no native
pixels; no native family has a full pixel payload and backend consumer yet; surface/HUD/controls/
skeleton/entity families are still native- or partially-delegated; `NATIVE_RETAINED` entries still
carry no payload.
Continue per-family migration down the ledger above, then implement Background as the lowest item
of the same frame/backend path.

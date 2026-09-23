# ART Render Z-Order Design and Implementation Plan

Status: planned. This document defines the ART-owned render ordering model for the visual
verification background and later cross-component presentation work. It does not authorize
reordering of STS authority or native render calls.

Related: [`art-framework.md`](./art-framework.md), [`traditional-ecs.md`](./traditional-ecs.md),
[`native-render-family-systems.md`](./native-render-family-systems.md),
[`native-render-coverage-sdd.md`](./native-render-coverage-sdd.md).

## 1. Goal

ART needs deterministic control over the order in which its presentation components become
pixels. The first consumer is a visual-verification background that must remain below retained
native pixels while making unrelated native scene content suppressible. The same mechanism must
also support C1, C2, entity, effect, skeleton, and optional guide content without relying on the
current fixed sequence in `StageHost.receivePostRender()`.

The design must preserve these boundaries:

- ECS system execution order remains unchanged unless a separate system-scheduling change is
  explicitly designed.
- Components remain data-only; they do not draw during ECS iteration.
- Native STS rendering remains a host boundary. ART z-order cannot silently insert pixels inside
  an unfiltered native call.
- A render order is deterministic across map/hash iteration and independent of component creation
  timing.
- Existing C1/C2 visual ownership and native continuation policies remain unchanged during the
  migration.

## 2. Render model

The runtime is split into three distinct steps:

```text
ECS systems update authoritative presentation components
        |
        v
Render extraction creates immutable RenderItems for this frame
        |
        v
Render submission sorts RenderItems and draws them in order
```

ECS systems must not call `SpriteBatch`, mutate render targets while drawing, or depend on the
iteration order of another render component. A system may update a component or enqueue a
coalesced render projection; extraction observes the resulting state after the normal ECS update
phases.

`RenderItem` is a per-frame, host-neutral draw description. Its minimum data is:

| Field | Meaning |
|---|---|
| `entityId` | Owning ART presentation entity; used for diagnostics and cleanup correlation |
| `targetId` | Render target or surface identity |
| `phase` | Coarse ordering boundary; prevents accidental cross-family interleaving |
| `z` | Fine ordering value within a phase |
| `stableKey` | Deterministic final tie-breaker |
| `bounds` | Immutable frame-space geometry |
| `visibility` | Whether the item is eligible for submission |
| `payload` | Host-neutral resource/effect/draw data; no mutable host object ownership |

The first implementation may adapt existing `RenderTarget` snapshots instead of introducing a
public `RenderItem` API immediately. The extraction boundary is still required conceptually and
must be testable independently of GL.

## 3. Ordering key

Every submitted item is sorted lexicographically by:

```text
(phase.rank, z, stableKey)
```

The comparator must be ascending: lower values draw first and higher values paint over them.

### 3.1 Phase

`phase` is not a user-facing arbitrary z value. It is a safety boundary for ownership and native
integration. The initial phase table is:

| Phase | Rank | Purpose | Native pixels |
|---|---:|---|---|
| `ART_BACKGROUND` | 100 | Solid/checker background and low-noise verification grid | Drawn where native filtering leaves a gap |
| `NATIVE_RETAINED` | 200 | Explicitly retained native continuation boundary | Host-owned, not an ART item |
| `C1_CONTENT` | 300 | Synthetic windows and widgets | ART-owned |
| `C2_CONTENT` | 400 | Full-present surfaces and surface items | ART-owned |
| `ENTITY_CONTENT` | 500 | Entity chrome, claimed skeleton content, entity overlays | ART-owned or claimed host adapter |
| `ART_EFFECTS` | 600 | Effects that intentionally sit above content | ART-owned |
| `VERIFY_GUIDES` | 1000 | Optional center lines, bounds, labels, and diagnostics | ART-owned |

`NATIVE_RETAINED` is a boundary, not a normal target that ART can move above or below. In the
STS1 host, native drawing happens in a host-controlled interval. ART may draw the background before
that interval only when the host has installed a safe pre-native hook, or may use a filtered native
path that leaves the background visible. ART must not claim that a post-render draw is below native
pixels.

Phase constants belong to the render package and are not encoded as magic integers in layout JSON.
Consumers may select a `z` value within an allowed phase range, but cannot select another phase
unless the phase is explicitly exposed by the component contract.

### 3.2 Z value

`z` is a signed, finite integer or fixed-point value. The initial implementation should use an
integer to make probe output and tests unambiguous. Values are local to a phase; equal z values are
valid and resolved by `stableKey`.

Suggested reserved ranges:

```text
0..999       ordinary component content
1000..1999   component-local decorations
2000..2999   explicit top content within the same phase
```

The ranges are conventions, not a second ordering mechanism. A component must not use an extreme
z value to cross an ownership boundary. Validation should reject non-finite values, unknown phase
ids, and values outside a phase's declared range when a range is enforced.

### 3.3 Stable key

`stableKey` is generated from stable data, never from an object identity or hash-map iteration.
The recommended form is:

```text
<entity scope>/<entity value>/<target id>/<item id>
```

If two items still collide, the renderer must reject the frame in pure planning tests or apply an
explicit documented secondary ordinal assigned during deterministic extraction. It must not use
the insertion order of a concurrent map as an implicit tie-breaker.

## 4. Component contract

Render-capable components gain a data-only ordering value. The exact component name should follow
the existing render-state vocabulary, but the contract is:

```text
RenderOrderComponent {
    RenderPhase phase;
    int z;
    String stableKey;
}
```

Defaults are phase-specific and preserve current behavior during migration:

- C1 synthetic windows/widgets: `C1_CONTENT`, `z = 0`.
- C2 surfaces/items: `C2_CONTENT`, `z = 0`, with existing item ordering represented in
  `stableKey` or an explicit local z.
- Entity content: `ENTITY_CONTENT`, `z = 0`.
- Verification background: `ART_BACKGROUND`, `z = 0`.
- Verification guides: `VERIFY_GUIDES`, `z = 0`.

The component is written by projection/materialization systems and read by render extraction. Host
objects such as `Actor`, `Texture`, `Skeleton`, and `SpriteBatch` do not belong in the component.

For backwards compatibility during the migration, missing ordering data is normalized to these
defaults. This compatibility path is for existing persisted/constructed ECS state only; new
components should always receive explicit normalized ordering data.

## 5. ECS interaction

Adding z-order must not reorder ECS systems. The intended data flow is:

```text
PresentationSchedule phases
  -> projection/materialization systems write render components
  -> RenderProjectionQueue coalesces updates
  -> RenderPlan extraction reads one immutable ECS snapshot
  -> RenderPlan.sort(phase, z, stableKey)
  -> host renderer submits pixels
```

Rules:

1. Systems may determine visibility, bounds, phase, z, and payload, but never draw.
2. Extraction occurs after the systems that produce the frame's render state.
3. Render submission is read-only with respect to ECS.
4. A draw failure is recorded in host/evidence state and must not mutate ordering state.
5. Cleanup removes the entity/component state; the next extraction naturally removes its item.
6. Host cache recreation rebuilds targets from the same ECS render components and must preserve
   the ordering key.

This keeps the ECS execution graph stable while making the pixel graph explicit and deterministic.

## 6. Native boundary and visual-verification background

The complete frame is not one sortable ART list. The STS1 host has an external native interval:

```text
ART background / retained native preparation
    -> native STS draw interval (filtered or continued by host policy)
    -> ART C1/C2/entity/effect items ordered within their phases
    -> optional verification guides
```

The visual-verification background therefore requires two separate capabilities:

- `BackgroundRenderer`: produces the ART background item and optional guides.
- `NativeRenderFilter`: explicitly marks unrelated native families as skipped while allowing the
  target family to continue.

The background is not a `FULL_FRAME` overlay and must not be rendered through
`RenderHost.kindsOverUi()`. A post-render background would cover retained native pixels and is
invalid for this use case.

The host integration must expose a verified pre-native draw point, or draw the background after a
native family has been skipped. If neither is available, the mode must report `unsupported` rather
than pretending that the background is underneath native content.

Native filtering remains family- and invocation-specific. It must reuse the existing
`NativeRenderBridge` disposition/ledger rules and fail open for unknown, unclaimed, panic, or
host-failure cases. Z-order does not grant permission to suppress native pixels.

## 7. Migration from current fixed passes

Current fixed calls in `StageHost.receivePostRender()` should be migrated in stages:

1. Extract current C1, C2, entity, and effect calls into named render item groups without changing
   their output order.
2. Add `RenderPhase` and normalized z defaults to the existing ECS render state.
3. Build one deterministic `RenderPlan` containing all ART-owned groups.
4. Sort the plan by `(phase, z, stableKey)` and prove that default output matches the existing
   fixed pass order.
5. Move `RenderHost` target submission to consume the sorted plan; retain target-kind filters as
   phase safety checks, not as the primary ordering mechanism.
6. Add the background renderer at `ART_BACKGROUND` and a host pre-native integration point.
7. Add optional guides at `VERIFY_GUIDES`; keep them disabled by default.
8. Migrate native filter scopes one family at a time, with ledger evidence for each suppressed
   invocation.
9. Remove fixed ordering assumptions only after all existing D1 scenarios and pure render-plan
   tests pass.

During migration, a legacy fixed-pass adapter may create phase-grouped items. It must be temporary
and must not become a second render authority.

## 8. Probe and console contract

Probe should expose the resolved plan, not only individual targets:

```json
{
  "renderOrder": {
    "status": "ready",
    "items": [
      {"id": "verify/background", "phase": "ART_BACKGROUND", "z": 0, "stableKey": "..."}
    ],
    "duplicateStableKeys": [],
    "nativeBoundary": "filtered_pre_native"
  }
}
```

The first console surface should be diagnostic and explicit:

```text
art verify mode off|background|guides|bounds
art verify native <family> on|off
art verify native clear
art verify status
```

`mode` and `native` are implemented. `native <family> on` only narrows an existing delegation
decision (never upgrade fail-open/panic), and `status` reports configured ordering separately from
actually submitted ordering. The `sts1.room.background` pre-native renderer/filter boundary and its
D1 variants are shipped and verified.

Native isolate is a separate, explicit verification policy: a live mounted surface already routed
through `NativeRenderBridge.beginSurface` is denied by default while isolate is active; a matching
`family:`, `surface:`, `class:`, or `method:` target restores native continuation. Unknown owners,
unmounted surfaces, panic, bridge errors, claimed-skeleton failures, and unpatched owners remain
fail-open. Current D1 combat evidence covers hand, controls, energy, and top panel. Targeting is
observe-only/native-authoritative; skeleton suppression is per claimed instance; transient effects
are observed through the typed effect bridge and are not a claim over arbitrary native VFX owners.
Per-family pixel replacement beyond the background family, and complete UI-family coverage, remain
unimplemented.

## 9. Test and acceptance plan

### Pure tests

- Equal z values are ordered by stable key, never insertion order.
- Different phases always retain phase ordering regardless of z values.
- Invalid phase, z, and duplicate-key behavior is deterministic.
- Extraction does not mutate ECS state.
- Host recreation preserves ordering data.
- Default normalized values reproduce the current C1/C2/entity ordering.
- A render failure does not reorder or mutate the next item.

### Native-boundary tests

- Unknown native invocation fails open.
- A selected native family can continue while unrelated configured families are skipped.
- Panic/recovery/host recreation clears filters and restores native continuation.
- A background mode reports ready through the verified pre-native hook.

### D1 visual scenarios

- Retained native target remains visible over the background.
- Unrelated native room/background content is absent only when explicitly filtered.
- C1, C2, entity, and guide items appear in configured z order.
- Equal-z output is stable across repeated captures.
- Turning guides off produces the same content pixels apart from the guide layer.

Pixel references must remain developer-local and paired with the exact frozen native state. Probe
and ledger assertions are the primary automated evidence for ordering and native suppression;
screenshot comparison validates the final composition only after those contracts pass.

## 10. Implementation checklist

Shipped slices (commits `b4b59ac`..`1a1f724`):

- [x] Add `RenderPhase` and the data-only ordering component/value object.
- [x] Extend render extraction and `RenderPlan` with immutable ordered items.
- [x] Add deterministic comparator and duplicate/tie diagnostics.
- [x] Preserve current fixed-pass output through the legacy adapter.
- [x] Order VFX particle projections by the shared render key.
- [x] Submit VFX in `ART_EFFECTS` after C2/entity content, without crossing `stage.draw`.
- [x] Report the STS1 native/ART boundary capability through probe (background now `pre_native`).
- [x] Add and validate resolved render-order probe diagnostics (`renderOrder`, `duplicateStableKeys`).
- [x] Add `art verify` console diagnostics (`art verify status|mode off|background|guides|bounds`).
- [x] Add pure render-plan and host-order tests.
- [x] Implement `art verify native <family> on|off|clear` on top of the narrowing filter scope
      (`Sts1VerifyDiagnostics.enableNativeFilter` / `disableNativeFilter` / `clearNativeFilters`,
      probe `backend.verify.nativeFilters`). The pre-native background is implemented in
      section 12; per-family native pixel replacement beyond `sts1.room.background` remains
      unimplemented.
- [x] Report `ready` for a visual mode on the verified pre-native hook (`background`); other
      unverified modes still report `unsupported` rather than claiming pixels.
- [x] Add the guides/bounds overlay at `VERIFY_GUIDES` (disabled by default); `guides`/`bounds`
      report `ready` without a pre-native hook, and `background` now reports `ready` through the
      verified pre-native scene hook.

Remaining:

- [x] Add the background renderer without using `FULL_FRAME`. The renderer draws in the pre-native
      scene Prefix (section 12); a non-filtered native family still paints over it, and filtering the
      `sts1.room.background` family lets the ART fill reach the frame. Implemented and D1-verified.
- [x] Define and verify a real STS1 pre-native/filtered boundary. Design recorded in section 12;
      implemented with device evidence in `tests/ui-scenarios/device/d1_verify_background.yaml`.
- [x] Add native filter scopes with fail-open cleanup. `NativeFilterScope` is consulted by
      `NativeRenderBridge.beginSurface` only after the panic/unknown-owner fail-open checks and only
      inside the delegation branch, so it can only downgrade a would-be delegation to
      pass-through/capture (reason `filter_scope:<family>`); panic and recovery clear it. It does
      not implement the pre-native background or per-family native pixel replacement, and it grants
      no new permission to suppress native pixels.
- [x] Add native-boundary tests that suppress a selected family while failing open elsewhere.
      Offline coverage lives in `NativeFilterScopeTest` / `NativeRenderBridgeTest` (unknown-invocation
      fail-open, panic continuation, host-recreation recovery, and a filtered non-selected family
      being narrowed). Remaining: per-family native pixel replacement and D1 pixel-parity evidence.
- [x] Add D1 visual verification scenarios and local screenshot workflow.
      `tests/ui-scenarios/device/d1_render_zorder_contract.yaml` now covers the D1 probe contract
      (`renderOrder` monotonic phase order, empty `duplicateStableKeys`, `renderBoundary`, and
      `art verify mode`), and passes on D1 (1/1). Local screenshot capture
      (`adb exec-out screencap`) plus independent visual review found no visual defect in the
      full-present combat frame. Per-family native suppression and pixel-parity scenarios remain
      open.
- [x] Remove the legacy adapter after migration evidence is complete. The "legacy adapter" was never
      a distinct class; it was the decision to reuse `RenderTarget` snapshots and `RenderPlan.Entry`
      as the ordered item model. No removable adapter code remains, so this item is resolved as
      doc-only. Fixed-pass submission still exists by design (`StageHost.receivePostRender` pass
      order) and is not the same thing as an adapter.

The first implementation adapts existing `RenderTarget` snapshots and `RenderPlan.Entry` as the
ordered item model instead of introducing a parallel public `RenderItem` type, as permitted by
section 2; `RenderOrder` is the shared ordering key.

## 11. Delivered behavior (evidence)

- `RenderPhase` ranks: `ART_BACKGROUND` 100, `NATIVE_RETAINED` 200, `C1_CONTENT` 300,
  `C2_CONTENT` 400, `ENTITY_CONTENT` 500, `ART_EFFECTS` 600, `VERIFY_GUIDES` 1000.
- `RenderOrder` rejects non-finite `z` and empty `stableKey`; `RenderPlan` sorts by
  `(phase.rank, z, stableKey)` and rejects two different target ids sharing one stable key.
- `RenderTarget` carries `phase`/`stableKey`; `RenderHost.drawFrame` sorts each existing pass with
  the same comparator, so output no longer depends on target insertion order.
- `RenderHost.probeMap()` exposes `renderOrder` (`status`, `count`, `monotonic`, `items`,
  `duplicateStableKeys`)
  and per-target `phase`/`z`/`stableKey`.
- `Sts1RenderBoundary.probeSlice()` reports `nativeInterval=stage.draw`,
  `artInterval=post_native_overlay`, `backgroundCapability=pre_native`,
  `supportsPreNativeBackground=true`; `UiProbe.backendMap()` exposes it as `renderBoundary`.
- `Sts1VerifyDiagnostics` backs `art verify status|mode ...`: `guides`/`bounds` are ART-owned
  `VERIFY_GUIDES` overlays and report `submissionStatus=ready` (drawn by
  `VerifyGuideDrawPath`, off by default), `background` reports `ready` through the verified
  pre-native scene hook, `off` reports `disabled`, and `UiProbe.backendMap()`
  exposes the same slice as `verify`. `Sts1VerifyDiagnostics.enableNativeFilter(family)` /
  `disableNativeFilter(family)` / `clearNativeFilters()` implement `art verify native`; they
  delegate to the narrowing `NativeFilterScope` and surface as `verify.nativeFilters`
  (`active`, `selectedFamilies`, `filteredFamilies`).
- Native `stage.draw()` order is unchanged; native pixel suppression is enabled only for the
  `sts1.room.background` family via the pre-native gate (section 12).
- `NativeRenderBridge.probeSlice()` exposes `filterScopes` (`active`, `selectedFamilies`,
  `filteredFamilies`). `NativeFilterScope` can only narrow an existing delegation decision: a
  filtered, non-selected family that would delegate becomes `PASS_THROUGH` (`filter_scope:<family>`),
  while panic/unknown-owner fail-open and OBSERVE behavior are untouched. `clear()`, recovery
  (`clearTransientEffectsForRecovery`), and `resetForTests` clear the scope. The pre-native
  background is implemented via the scene Prefixes; remaining: per-family native pixel replacement
  beyond `sts1.room.background`.

## 12. Pre-native background and native suppression (implemented)

This section records the explicit `@SpirePatch` design required by `AGENTS.md` before any native
render patch is added for the visual-verification background. It is now implemented:
`Sts1RenderBoundary.backgroundCapability()` is `PRE_NATIVE`, verified by
`tests/ui-scenarios/device/d1_verify_background.yaml` (all three variants) on D1.

### 12.1 Why a background alone is not enough

Decompiled `com.megacrit.cardcrawl.dungeons.AbstractDungeon.render(SpriteBatch)` shows the frame
order inside one method:

```text
AbstractScene.renderCombatRoomBg(sb)   // native room background, opaque
renderLetterboxGradient(sb)
AbstractScene.renderEventRoom(sb)
effects loop (AbstractGameEffect.render)
AbstractRoom.render(sb)                // native room contents
AbstractScene.renderCombatRoomFg(sb)
AbstractRoom.renderEventTexts(sb)
effects loop
OverlayMenu.render / renderBlackScreen
screen-specific render (map, select, rewards, ...)
```

A Prefix on `AbstractDungeon.render` runs before `renderCombatRoomBg`, but that call paints opaque
native pixels over anything ART drew. Drawing a background first is therefore invisible, not
"underneath native pixels". This is the same reason a post-render `FULL_FRAME` overlay is invalid:
it would cover retained native pixels.

### 12.2 Required capability pair

Two capabilities must land together; neither is useful alone:

1. `BackgroundRenderer` — draws the selected variant directly in the pre-native scene Prefix (no
   `RenderHost` item and no post-native path; the Prefix runs inside `AbstractDungeon.render` before
   the native scene paints, which is the only interval that can sit under retained pixels).
2. `NativeRenderFilter` (family-level) — the `NativeFilterScope` shipped in this repo plus a
   family->native-invocation map, wired to `SpireReturn.Return(null)` on the specific native
   background family only.

The filter must reuse `NativeRenderBridge` disposition/ledger rules and fail open for unknown,
unclaimed, panic, or host-failure cases. Z-order grants no permission to suppress native pixels.

### 12.3 Patch mechanism (final)

`AbstractScene.renderCombatRoomBg` is **abstract**, and ModTheSpire cannot attach a Prefix to a
bodyless method (the same constraint documented in `TransientEffectContainerPatches`). The concrete
overrides are the real pixel owners:

| Native path | Kind | Patchable |
|---|---|---|
| `AbstractScene.renderCombatRoomBg` | abstract | no |
| `TheBottomScene` / `TheCityScene` / `TheBeyondScene` / `TheEndingScene` `renderCombatRoomBg` | concrete override | yes, `@SpirePatch` Prefix on each class |
| `AbstractDungeon.render` `renderCombatRoomBg` call site | concrete | instrumentable, but rejected (below) |

`AbstractDungeon.render(SpriteBatch)` dispatches `AbstractScene.renderCombatRoomBg` with
`invokevirtual`, so a Prefix on each concrete override intercepts every in-run scene background. An
`@SpireInstrumentPatch` on the `AbstractDungeon.render` call site is rejected on purpose:

- `ExprEditor.call.replace(...)` contains no `SpireReturn.Return` literal, so it would silently
  bypass both the JUnit `RenderPatchOwnershipTest` allowlist (`onlyApprovedPatchesSuppressNativeDraw`)
  and the NRCC `check_patch_ownership` gate. A Prefix returning `SpireReturn.Return(null)` is caught
  by both, so the suppression cannot land unregistered.
- `AbstractDungeon#render` is already a shared `OBSERVED` observation entry (the effect-queue hook in
  `TransientEffectContainerPatches`). Relabeling it as a pixel-suppression owner would make that
  observation-only justification false.

The four Prefixes are near-duplicates by design: one honest `ART_DELEGATED` manifest owner per
concrete scene class, each provable by the same static gate.

```java
@SpirePatch(clz = TheBottomScene.class, method = "renderCombatRoomBg",
        paramtypez = {SpriteBatch.class})
public static class ObserveNativeBottomBackground {
    public static SpireReturn<Void> Prefix(TheBottomScene __instance, SpriteBatch sb) {
        return BackgroundRenderGate.artOwnsBackground()
                && BackgroundRenderGate.renderIfOwned(sb)
                ? SpireReturn.Return(null)   // ART background painted; native pixels skipped
                : SpireReturn.Continue();    // native continuation, unchanged
    }
}
```

Rules the implementation must satisfy:

- Observe-first: default native continuation. Suppression happens only for the exact
  family/invocation, never queue-wide.
- Suppression requires the developer opt-in pair: a non-`off` background variant AND the
  background family explicitly filtered via `art verify native sts1.room.background on`, with no
  panic. `art verify` is a developer diagnostic surface, not a player-facing present surface, so
  the opt-in pair replaces the `FULL` + mounted surface gate and is strictly narrower than
  `FULL`; no `FullPresentMode` surface is registered for the background.
- Unknown family, unknown owner, panic, unmounted, recovery, or renderer failure => continue native
  (in particular, if the ART background draw throws or paints nothing, the native background must
  still run; only `painted && filtered` suppresses).
- The `render-owner` manifest entry must be `ART_DELEGATED` with a non-empty `justification` and a
  focused suppression-gate test (`tools/nrcc/coverage_manifest.py --check-manifest`).
- Panic/recovery must clear the filter scope and background variant and restore native continuation.

Concrete suppression targets (one native owner each, because the `AbstractScene` methods are
abstract):

```text
com.megacrit.cardcrawl.scenes.TheBottomScene#renderCombatRoomBg(SpriteBatch)
com.megacrit.cardcrawl.scenes.TheCityScene#renderCombatRoomBg(SpriteBatch)
com.megacrit.cardcrawl.scenes.TheBeyondScene#renderCombatRoomBg(SpriteBatch)
com.megacrit.cardcrawl.scenes.TheEndingScene#renderCombatRoomBg(SpriteBatch)
```

Each Prefix draws the ART background at `ART_BACKGROUND` and returns `SpireReturn.Return(null)` only
when the gate owns the background; otherwise it returns `SpireReturn.Continue()` unchanged.

NRCC registration (required so the suppression is gate-checked, not silently allowed): the scanner
only emits a path for methods in `scan_sts_render.RENDER_METHOD_NAMES`, and
`check_patch_ownership` only inspects methods in its render/draw set. Without both additions a
Prefix on a scene override would suppress native pixels entirely outside the static gate. The
slice therefore also:

- adds `renderCombatRoomBg` to `RENDER_METHOD_NAMES` and to `check_patch_ownership`'s method set;
- adds a `room-backgrounds` family (method rule `^renderCombatRoomBg$`, evaluated before the
  `scenes.` -> `meta-outofrun-screens` prefix rule) whose default keeps native authority, with
  explicit `ART_DELEGATED` + `justification` + `test` for the four concrete overrides.

This is the point earlier sessions hit: extending the vocabulary is what makes the gate *stronger*
(it now constrains this suppression), not a way to weaken it.

### 12.4 Background variants

`ART_BACKGROUND` is one render phase with multiple selectable variants; the variant is chosen by
`art verify mode background [variant]` and reported in the probe. Each variant must be independently
implementable and visually verifiable:

| Variant | Pixels | Purpose |
|---|---|---|
| `off` | none | default; native background fully retained |
| `solid` | one opaque `Settings.WIDTH x Settings.HEIGHT` quad | simplest proof that a pre-native fill reaches the frame |
| `checker` | procedural checkerboard, deterministic cell size | proves the background is drawn below retained native pixels, not over them |
| `grid` | low-contrast grid lines over a solid base | verification alignment/scale reference |

Rules:

- Variants are pure `SpriteBatch` draws using `ImageMaster.WHITE_SQUARE_IMG`, with the same
  fail-open/panic guards and color restoration as `VerifyGuideDrawPath`.
- Draw and suppression are **decoupled but ordered**: the ART background is drawn whenever a
  non-`off` variant is selected and no panic is active (draw is the variant's job); native
  suppression additionally requires the `sts1.room.background` family to be explicitly filtered
  **and the draw to have succeeded this frame**. The draw entry reports whether it painted; the
  Prefix suppresses only on `painted && filtered`. This is what makes the z-order claim testable:
  with the variant selected but the family retained, the ART background is drawn and then painted
  over by the native scene background (invisible), whereas with the family filtered the native
  pixels are skipped and the ART background is what reaches the frame.
- Renderer failure still continues native: if `renderSelectedVariant` throws or the batch/texture is
  unavailable, it reports "not painted", so the Prefix continues even when the family is filtered.
  The retained-and-painted case is the only case that draws without suppressing.
- Consequence for evidence: `off` => `artDrawCount` stays 0 and pixels are native; selected +
  retained => `artDrawCount` grows while pixels stay close to `off` (hidden underneath); selected +
  filtered => pixels change to the ART fill and native is skipped.
- The draw path must never cross `stage.draw()` and must not use `kindsOverUi()` / `FULL_FRAME`.
  There is no post-native `BackgroundRenderGate.render()` path; the only draw point is the
  pre-native scene Prefix.

### 12.5 Order of work

1. Extend the NRCC vocabulary: `renderCombatRoomBg` in `RENDER_METHOD_NAMES` +
   `check_patch_ownership`, a `room-backgrounds` family, and `ART_DELEGATED` manifest rows for the
   four concrete overrides (`justification` + `test`). Fix the `test_families` fixture/count so the
   new family is covered.
2. Implemented the `BackgroundRenderer` variants as direct pre-native `SpriteBatch` draws inside
   the scene Prefix (no `RenderHost` item and no post-native path); each variant draws with the
   native family still continuing (invisible, so also verify with guides on) and `off` draws
   nothing. **Implemented**: the suppression decision (`suppressNativeBackground`), the
   `off|solid|checker|grid` draws, the probe slice, and the `art verify mode background [variant]`
   parsing all exist, wired to the scene Prefix hook.
3. Added the four suppression Prefixes behind the gate; the focused test proves native
   continuation becomes false only in the variant+filtered+no-panic state, and the patch files are
   registered in `RenderPatchOwnershipTest.ALLOWED_SUPPRESS_PATCHES` +
   `EXPECTED_DELEGATED_SURFACES_BY_PATCH`.
4. D1 evidence: paired captures per variant (`solid`, `checker`, `grid`) with the native family
   retained and suppressed, plus the ledger counters, completed; `backgroundCapability()` was
   flipped to `PRE_NATIVE` on that paired-capture evidence.

`art verify mode background` now reports `ready` (background capability is `PRE_NATIVE`): the
variant is settable, `modeSupported()` is true, and `submissionStatus()` is `ready`.

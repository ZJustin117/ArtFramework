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
art verify status
```

`status` must distinguish configured ordering from actually submitted ordering, and must report
when the host cannot provide the required pre-native boundary.

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
- A background mode without a verified pre-native hook reports unsupported.

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

- [ ] Add `RenderPhase` and the data-only ordering component/value object.
- [ ] Extend render extraction and `RenderPlan` with immutable ordered items.
- [ ] Add deterministic comparator and duplicate/tie diagnostics.
- [ ] Preserve current fixed-pass output through the legacy adapter.
- [ ] Add the background renderer without using `FULL_FRAME`.
- [ ] Define and verify the STS1 pre-native/filtered boundary.
- [ ] Add native filter scopes with fail-open cleanup.
- [ ] Add probe and `art verify` diagnostics.
- [ ] Add pure render-plan and native-boundary tests.
- [ ] Add D1 visual verification scenarios and local screenshot workflow.
- [ ] Remove the legacy adapter after migration evidence is complete.

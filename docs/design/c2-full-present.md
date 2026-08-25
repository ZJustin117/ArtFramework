# C2 full present — native invocation delegation

Target design for **C2 as an invocation-boundary/overlay/input layer**: ART intercepts the
relevant native render invocations, records display input from hard-synced Backend frames, and
routes native UI callbacks through **signals + intents**. ART does not rewrite STS render
implementations or game authority. Original STS renderers remain the visual authority unless the
surface is explicitly ART-owned and has no corresponding native renderer.
Complements [`backend-context.md`](./backend-context.md), [`host-assets.md`](./host-assets.md),
[`dual-track.md`](./dual-track.md), [`godot-aligned-ui.md`](./godot-aligned-ui.md),
[`native-render-coverage-sdd.md`](./native-render-coverage-sdd.md).
Roadmap: [`docs/task.md`](../task.md) milestones **15–22** (shipped), **24–26** (entity + room
FULL production).

## Native render boundary

The STS1 adapter is an invocation boundary, not a second STS renderer implementation:

```text
STS native render invocation
  -> NativeRenderBridge
       -> capture owner / source identity
       -> project display input
       -> PASS_THROUGH | CAPTURE_AND_PASS | DELEGATE_TO_ART | FAIL_OPEN
```

`DELEGATE_TO_ART` is scoped to the intercepted invocation and is only allowed when the native
surface/effect has no STS renderer or the native renderer is explicitly replaced by a
documented, tested ART path (see [`native-render-coverage-sdd.md`](./native-render-coverage-sdd.md)).
For a complete native surface, the native owner may be delegated as a whole only when ART is the
sole pixel owner. For transient effects, delegation is per effect instance. Uncovered native calls
continue through the original implementation.

The bridge never executes relic, Power, card, combat, or room rules. It only controls whether the
native display invocation continues after its display input has been captured.

## Status vs shipped code

| | Shipped (0–26) | Notes |
|--|----------------|-------|
| Draw | Combat/map/event/select FULL; room/chrome FULL_READY when scene matches | Invocation hooks and suppression patches for reward/rest/shop/treasure |
| Input | SignalBus + intents including room CLAIM/REST/SHOP/CHEST | Soft-reject without dungeon |
| Data | Context frames + strong views including event/select/reward/rest/shop | |
| Objects | Registerable full-present surfaces + EntityPresent draw path | Thin intercept remains migration bridge |

Thin intercept ([`dual-track.md`](./dual-track.md) roadmap 7, patches) remains a **migration
bridge**. The fixed target is a typed `NativeRenderBridge` and delegation ledger; the current
patch classes are host-boundary adapters toward that target.

## STS1 implementation status

The host-agnostic Backend/Projection/Surface contract is implemented in milestone 15. It is
not evidence that STS1 native render invocations have been fully delegated. STS1 full-present integration is tracked
separately in milestone **16**:

| Capability | 15.x | 16.x completion criterion |
|------------|------|---------------------------|
| Context frames / intents | pure API + fake | real STS1 backend and scene lifecycle |
| Hand / slots | projection + probe | SpriteBatch draw, input, invocation delegation, D1 validation |
| Controls / map | surface actions | draw, input, executor, invocation delegation, D1 validation |
| Assets | key/pack resolver | STS1 texture/font/audio handle resolver |
| Skeleton | provider/surface API | STS1 draw/event/lifecycle bridge |

No C2 surface is called full-present on device until its complete D1 acceptance row is green.

### PresentLevel policy (16.0)

| Level | Snapshot / probe | Native continuation | ART owns input |
|-------|------------------|--------------------|-----------------|
| `OFF` | optional observe via backend bind | native continues | no |
| `OBSERVE` | yes | native continues | no |
| `FULL` | yes | delegated invocation stops when surface is ready | yes when surface mounted + executor ready |

Mount alone never hides native UI. `FullPresentMode` defaults all surfaces to `OFF`.
Console: `art present combat on|off|observe|status` (on ≡ FULL).

### Strong frame views (16.0)

`ContextFrame` carries `ControlsView` + `MapView` (+ `ViewportView`). Legacy
`Map<String,Object> controls/map` fields remain probe bridges derived from the strong views.

### Render plan (16.3)

`SurfaceDrawPlan` / `Sts1RenderPipeline` decide per surface `SKIP|OBSERVE|DRAW` from
`PresentLevel` + mount + scene. Overlay-observe mode downgrades FULL→OBSERVE (native stays
visible). `BatchStateGuard` + `ClipRect` bookkeep host SpriteBatch restore without leaking GL
into pure tests. Probe: `backend.renderPlan`.

### Input router (16.5)

`CombatInputRouter` gates drag/play/end-turn when `PresentLevel.FULL` + mounted; submits
`UiIntent` through the unified signal bus. `IntentExecutor` SPI + `RecordingIntentExecutor`
for tests; STS1 backend `submitIntent` → `executeIfOwned`. Live useCard executor is 16.5b.
Probe: `backend.input`.

## Purpose

1. Treat game-coupled frontends (hand, drag/play, slots, buttons, skeleton, map) as
   **injectable surface objects** with enough API to observe, gate, present, and request.
2. Drive display from Backend frames: **hard sync**, instance-stable cards, asset keys via
   HostAssets.
3. Keep combat/protocol authority **out** of ART: only intents return to the engine.

## Non-goals

- Reimplementing STS combat resolution, `useCard` pipeline ownership, or room enter authority.
- Soft-diverging authoritative card geometry from the Primary Backend frame.
- Free-form signal handlers that mutate game state.
- Dual-device life / connector as default gates.

## Architecture

```text
Primary Backend frame (cards, slots, controls, map, resourceIds)
        → ART applyFrame (normalize, align, diff)
        → Surface projection (UiComponent + layout + drag session)
        → HostAssets.resolve(keys)
  → ART draw / overlay (only for ART-owned or overlay surfaces)
        → native renderers remain the visual authority otherwise
        → input → signal → intercept → intent → Backend
        → next frame converges pixels
```

C1 remains synthetic windows and overlays on the **same** context and asset pipeline.
C2 is the host-coupled full-present path, not a second business model.

## Native Surface model

Each large native region is one **surface** (not one patch per button):

```text
Native Surface
  id (sts1.*)
  lifecycle mount / unmount / scene recreate
  state API     ← frame projection + probeSlice
  event API     ← signals
  intercept API ← first BLOCK wins
  action API    ← intents / UiComponent.action
  present API   ← anchors, bounds, z for overlays/FX/skeleton
```

Implements existing `UiComponent` contract: `id`, `kind`, `mount`/`unmount`, `connect`,
`action`, `probeSlice`. Registry evolves from static five entries to **registerable surfaces**.

### Canonical surfaces (initial)

| Surface id | Covers | Intents (min) | Signals (min) |
|------------|--------|---------------|---------------|
| `sts1.combat.hand` | Hand cards, drag, play request | `begin_drag`, `move_drag`, `drop_card`, `cancel_drag`, `play_card` | `card_pressed`, `drag_started`, `drag_moved`, `drag_ended`, `play_requested` |
| `sts1.combat.card_slots` | Zones: hand/draw/discard/exhaust/select | `inspect_slot`, `select_card` | `slot_changed`, `card_selected` |
| `sts1.combat.controls` | End turn and combat chrome controls | `press`, `set_enabled_hint` | `pressed`, `enabled_changed` |
| `sts1.map` | Map nodes, pins, present anchors | `click_node`, `set_pins` | `node_hovered`, `node_clicked` |
| `sts1.skeleton` | Skeleton instances / anchors | `play`, `stop`, `set_transform` | `finished` |
| `sts1.combat.surface` | Combat UI aggregate root | `attach_overlay`, `detach_overlay` | `surface_opened`, `surface_closed` |

### Compatibility aliases

Consumer-visible legacy ids remain aliases where documented:

| Legacy | Canonical |
|--------|-----------|
| `sts.map` | `sts1.map` (or map subset of full present) |
| `sts.event` | keep until event surface full-present slice |
| `sts.select.grid` / `sts.select.hand` | select zone under card_slots / dedicated select surface |
| `sts.endturn` | `sts1.combat.controls` action sugar |

[`NativeTemplateIds`](../../src/main/java/artframework/c2/NativeTemplateIds.java) canonicalize
toward `sts1.*` (milestone 12.8 direction).

## Card information drive (hard sync)

### Frame contents (conceptual)

Per card entry at minimum:

- **instance id** (stable across frames while the engine instance lives)
- type / catalog id
- zone + slot index
- layout: x, y, rotation, scale, z, visible
- interact: hover, selected, dragging, playable hint, disabled
- present: highlight, preview, cost display fields, FX/skeleton anchor ids
- **resource ids** for art/frame (HostAssets keys) — not raw GL objects on the public boundary

### applyFrame pipeline

```text
1. Validate frameId / scene; drop stale
2. Normalize coordinates into ART present space
3. Identity align: create / update / migrate zone / retire entities
4. Diff vs previous projection
5. Update ART card entities + slots
6. Resolve assets; mark missing
7. Emit structural signals only when policy requires (prefer probe for bulk)
8. Draw from projection
```

| Diff | Present effect |
|------|----------------|
| Add | Enter entity + slot bind |
| Remove | Exit / release anchors; cancel drag if that instance |
| Reorder / slot | Relayout |
| Pose change | Hard-sync geometry (hit targets match frame) |
| Flags / display fields | Update chrome without full rebuild |
| None | Skip work |

### Hard sync rules

- **Hit testing and intercept geometry** follow the frame exactly.
- Optional **decorative** C1 overlays may soft-follow only if marked non-authoritative; they
  re-snap on authority frames.
- Entry/exit **presentation animations** may run briefly, then snap to latest frame — no
  long divergence from authority.

### Multi-instance cards

Never key sole identity by type `cardId`. Tests must cover two identical strikes in hand.

## Drag and play lifecycle

```text
press → begin_drag → move_drag → target hover → drop | cancel
```

Each step:

```text
input → surface signal → interceptors → intent (if ALLOW) → Backend
      → next frames update pose / zone membership → UI hard-sync
```

- Frames keep refreshing drag pose while active.
- Allow/deny start or drop is **input policy**, not frame logic.
- Play success/failure is reflected by **later snapshots**, not by painting inside the intent
  return.
- `UiOps.playHandCard` becomes sugar over `play_card` with `CardRef` (+ optional `TargetRef`);
  type-id-only targeting is legacy and insufficient for full present.

## Map, controls, skeleton

| Area | Full present expectation |
|------|--------------------------|
| **Map** | Native `DungeonMapScreen.render` continues to draw the map; ART projects nodes/paths for input/overlays; enter-room authority stays Backend/consumer policy via intent |
| **Controls** | Native `EndTurnButton.render` and `EnergyPanel.render` continue; ART observes and routes press intents; generic control surface (not end-turn-only special case) |
| **Skeleton** | Per-instance `SkeletonMeshRenderer.draw` suppression only when ART claims that instance; unclaimed skeletons continue natively; Backend/anchor keys + [`SkeletonProvider`](./art-framework.md) SPI; no combat rules in anim |

## Four intercept / observe kinds

| Kind | Role |
|------|------|
| Observe | Sync projection from frames / safe post-hooks during migration |
| Input preprocess | Gate before engine side effects |
| Present | Expose slots, bounds, enabled, map layout for draw and overlays |
| Overlay draw | C1 marks, FX, co-op chrome on anchors |

Migration may still use thin `@SpirePatch` → pure hooks to **feed** the backend adapter;
patches must not remain the long-term display owner.

## EntityPresent

Slot lifecycle for entity chrome stays display-layer: attach/sync/layout/detach and render
anchors. Not free-form window children; not combat authority.
See dual-track EntityPresent; full present binds anchors to surfaces and HostAssets.

## Probe / ops

- `probeSlice` per surface: mounted, bound/available, actions, signals, frame/version,
  constrained summary (counts, drag active, missing assets).
- `UiOps.invoke(surfaceId, action, …)` remains the unified command entry.
- Schema bumps documented when probe shape changes ([`ui-ops-probe.md`](./ui-ops-probe.md)).

## Iron rules

1. **Authority read-only** in ART and consumers.
2. **Display hard-sync** to Primary Backend frames.
3. **Native STS renderers stay the visual authority** unless the surface is explicitly ART-owned and has no corresponding native renderer.
4. **Signals do not execute rules**; intents do (Backend).
5. **Assets by ResourceId** via HostAssets; ART only resolves and draws.

## Testing

| Layer | Focus |
|-------|--------|
| JUnit | applyFrame align/diff, multi-instance cards, drag session + BLOCK, intent log |
| art-verify | Offline fixtures for hand frame, drag block, control press, map click shape |
| D1 optional | Real STS1 backend full-present smoke after jar deploy — single device only |

## Phasing

| Slice | Deliverable |
|-------|-------------|
| 15.0 | This design + links |
| 15.5 | Hand / slots snapshot hard-sync present (draw path) |
| 15.6 | Drag / play intent + signal intercept chain |
| 15.7 | Map / controls / skeleton surfaces |
| 15.9 | Consumer notes: intents replace native UI callbacks; `sts.*` aliases |

## Related

- [`backend-context.md`](./backend-context.md) — Backend / three faces / intents
- [`host-assets.md`](./host-assets.md) — skins and resource resolve
- [`dual-track.md`](./dual-track.md) — historical C1/C2 and thin intercept
- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — UiComponent / signals term map
- [`ui-ops-probe.md`](./ui-ops-probe.md) — ops and probe contract

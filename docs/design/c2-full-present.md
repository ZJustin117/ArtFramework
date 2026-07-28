# C2 full present — replace native display

Target design for **C2 as full display takeover**: ART draws the surface, hard-syncs from
Backend frames, and replaces native UI callbacks with **signals + intents**.
Complements [`backend-context.md`](./backend-context.md), [`host-assets.md`](./host-assets.md),
[`dual-track.md`](./dual-track.md), [`godot-aligned-ui.md`](./godot-aligned-ui.md).
Roadmap: [`docs/task.md`](../task.md) milestone **15**.

## Status vs shipped code

| | Today (milestones 0–14) | This doc (target) |
|--|-------------------------|-------------------|
| Draw | Native STS draws; ART observes / decorates / thin gates | **ART draws** the surface |
| Input | Prefix/hooks on native hitboxes | **ART captures** input; native callbacks leave the business path |
| Data | Template bind + partial `StsNativeOps` gestures | Per-frame **Context** hard-sync |
| Objects | Fixed `sts.*` five-pack | **Native Surface** objects for whole UI regions |

Thin intercept ([`dual-track.md`](./dual-track.md) roadmap 7, patches) remains a **migration
bridge**, not the end state.

## STS1 implementation status

The host-agnostic Backend/Projection/Surface contract is implemented in milestone 15. It is
not evidence that STS1 has replaced a native display. STS1 full-present integration is tracked
separately in milestone **16**:

| Capability | 15.x | 16.x completion criterion |
|------------|------|---------------------------|
| Context frames / intents | pure API + fake | real STS1 backend and scene lifecycle |
| Hand / slots | projection + probe | SpriteBatch draw, input, suppression, D1 validation |
| Controls / map | surface actions | draw, input, executor, suppression, D1 validation |
| Assets | key/pack resolver | STS1 texture/font/audio handle resolver |
| Skeleton | provider/surface API | STS1 draw/event/lifecycle bridge |

No C2 surface is called full-present on device until its complete D1 acceptance row is green.

### PresentLevel policy (16.0)

| Level | Snapshot / probe | Suppress native | ART owns input |
|-------|------------------|-----------------|----------------|
| `OFF` | optional observe via backend bind | no | no |
| `OBSERVE` | yes | no | no |
| `FULL` | yes | yes when surface mounted | yes when surface mounted + executor ready |

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
        → ART draw (full present)
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
| `sts1.skeleton` | Skeleton instances / anchors | `play`, `stop`, `set_transform` | `animation_finished` |
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
| **Map** | ART draws nodes/paths from map context; pins decorative; enter-room authority stays Backend/consumer policy via intent |
| **Controls** | Generic control surface (not end-turn-only special case): visible, enabled hint, press intent |
| **Skeleton** | Backend/anchor keys + [`SkeletonProvider`](./art-framework.md) SPI; ART plays; no combat rules in anim |

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
3. **Signals do not execute rules**; intents do (Backend).
4. **Assets by ResourceId** via HostAssets; ART only resolves and draws.

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

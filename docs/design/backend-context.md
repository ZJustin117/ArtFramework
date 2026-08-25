# Backend / Context — engine · UI · return

Contract for the unified **SignalBus**. Backend, UI, projection, host patches, and tests are
ordinary signal emitters/listeners; no backend-specific signal transport exists. Complements
[`c2-full-present.md`](./c2-full-present.md), [`host-assets.md`](./host-assets.md),
[`art-framework.md`](./art-framework.md), [`ui-ops-probe.md`](./ui-ops-probe.md).
Roadmap: [`docs/task.md`](../task.md) milestone **15**.

## Purpose

1. Split **game authority** from **display orchestration**: Endpoint owns committed world
   state; ART owns projection, signal dispatch, intercept, and render.
2. Make the hard-sync source a **Primary Backend**, not “must be the live STS process”.
3. Define a stable **down / up** contract so tests, replay, spectate, and future hosts share
   one ART surface.

## Non-goals

- Multiplayer protocol, party election, combat phase authority.
- Letting signals execute game rules or mutate authority.
- Multiple Primary backends writing the same display domain in one frame.
- Leaking STS concrete types (`AbstractCard`, `Hitbox`, …) into core or consumer public API.

## Signal bus

```text
Backend / UI / host                 SignalBus                         listeners
─────────────────                   ─────────                         ─────────
emit named signal              →     ordered broadcast          →    observe/replace/stop
```

| Face | Who implements | Who calls | Role |
|------|----------------|-----------|------|
| **SignalBus** | ART provides | All participants | One ordered exact/regex broadcast bus |
| **Backend** | consumer-defined | Listener/emitter | Listens only to signals it handles; emits state updates |
| **Projection** | ART internal | Listener | Applies `context/frame/updated` payloads |

### Broadcast semantics

Backend-facing native hooks route display invocations through ART calls without rewriting native
render implementations or moving authority into ART:

- Every event is an immutable `UiSignal(name, source, payload, metadata)`.
- Exact-name and regex subscriptions share one global registration sequence.
- Matching listeners are selected at emit start and run in registration order.
- A listener returns `continue`, `replace`, `stopHandled`, or `stopRejected`; later listeners
  receive replacements, while either stop ends propagation.
- Signal replacement preserves name and ID, so the listener set remains deterministic.
- `context/frame/updated` carries an immutable `ContextFrame`; projection is only another
  listener, with no privileged backend protocol.

### UI runtime

- Consume down-calls → normalize → identity-align → diff → update **ART projection**.
- Hard-sync draw from projection + [`host-assets.md`](./host-assets.md) resolve.
- Capture input → `ACTION` signal → interceptors → endpoint receipt.
- Owns derived state only: component tree, drag session, effect binds, probe — **not** game authority.

### Backend participation

Backend is not a special endpoint. It subscribes to ordinary named signals and emits ordinary
signals after it changes its own state. If it has no listener for a signal, that signal is simply
unhandled rather than automatically rejected.

| Layer | Audience | Role |
|-------|----------|------|
| `ui/<surface>/<event>` | UI, C2 hooks, full-present | Button, drag, selection, control activity |
| `context/frame/updated` | Backend | `ContextFrame` state update |
| `ui/<component>/<event>` | Native host patch | C2-native callback activity |

Authority remains a backend/domain concern, not a transport category: a backend's subsequent
frame signal is its declaration of current state.

## Frame path vs input path

```text
Frame path (layout-complete / pre-draw):
  Backend.snapshot(frameId) → ART.applyFrame → diff → hard-sync projection → draw

Input path (event):
  input → signal → intercept → intent → Backend.execute
        → authority changes → next snapshot reflects result → UI converges
```

| Path | Cadence | Answers |
|------|---------|---------|
| Frame | Per frame or post-layout | “What is true now?” |
| Input | On interaction | “May this UI act, and what intent is submitted?” |

## Primary Backend

Hard-sync **authority** for a display domain comes from exactly one **Primary Backend**.

| Backend kind | Snapshot | Intent | Notes |
|--------------|----------|--------|-------|
| Local game (default) | yes | yes | STS1 full host |
| Test fake | scripted | recorded | Default JUnit |
| Replay | stream | none / reject | Read-only |
| Spectate / mirror | remote authority | policy-defined | No dual primary |
| Future host (e.g. STS2) | host-defined | host-defined | Same ART core |

### Modes

| Mode | Snapshot | Intent |
|------|----------|--------|
| Full control | yes | execute |
| Read-only | yes | reject or absent |
| Local predict (optional) | authority + marked predict layer | UI may move predict; **authority frame forces converge** |
| Proxy control | yes | forward to remote authority |

### Overlay provider (optional)

A non-primary **overlay** may supply decorative annotations (co-op marks, vote chrome).
It **must not** change card membership, rules fields, or authoritative layout identity.

### Bind / switch

```text
bindBackend(primary)
  → reset projection, drag sessions, surface mounts
  → re-scope HostAssets view for that backend
```

Silent multi-primary is forbidden.

## Context views (read-only)

Conceptual packages (names illustrative): game, combat, character, map, controls, presentation.

| Context | Examples |
|---------|----------|
| **Game** | screen, flow phase, paused, global UI flags |
| **Combat** | turn/energy hints for UI, hand/draw/discard/exhaust zones, targets, combat chrome |
| **Character** | present slots, hp/block display fields, anim anchors |
| **Card** | **instance id**, card type id, zone, slot index, layout pose, interact flags, resource keys |
| **Map** | nodes, edges, reachability, positions, current node |
| **Controls** | end-turn / confirm / cancel visibility and enabled hints |
| **Presentation** | viewport, coordinate space, host capabilities, theme profile |

Rules:

- Views are **immutable snapshots** or pure value objects for the frame.
- Identity for cards is **instance id**, not type `cardId` alone (duplicates in hand).
- No STS engine types on the public core/consumer boundary.
- Unavailable / wrong screen → empty or `unavailable` views, not stale cross-scene bleed.

## Intent catalog (minimum, versioned)

Intents are named, argument-typed, and rejectable. Sugar on `UiOps` may wrap them.

| Domain | Intent examples |
|--------|-----------------|
| Hand / card | `begin_drag`, `move_drag`, `drop_card`, `cancel_drag`, `play_card`, `select_card` |
| Select screens | `confirm_select` |
| Map | `click_map_node` |
| Event | `choose_event_option` |
| Combat controls | `press_end_turn` |
| Assets (optional) | `select_skin_profile`, `enable_pack` — config only; does not change combat rules |

Payloads use ART refs (`CardRef`, `MapNodeRef`, `TargetRef`), not engine objects.

## Intercept policy

1. Unbound / inactive surface → `notBound` or documented passthrough (match [`ui-ops-probe.md`](./ui-ops-probe.md)).
2. First interceptor `BLOCK` → no intent submit, no engine side effect.
3. `ALLOW` → emit signal (as documented per surface) → submit intent → backend result.
4. Never import multiplayer protocol / party / combat-phase types into this repo.

## Timing

Preferred injection point for frames:

```text
native or backend update / layout complete
  → Context.snapshot(frameId)
  → ART.applyFrame
  → render (ART C1/C2 and/or host)
```

- Main thread only for snapshot production unless a backend documents otherwise.
- Drop non-monotonic `frameId`.
- Same-frame duplicate apply: last wins or reject — pick one and test it.
- Do not resolve authority mid-draw from live engine mutation.

## Relationship to existing APIs

| Existing | Evolution |
|----------|-----------|
| `ArtFramework.mount` / `ops` / `probe` | Keep; down/up route through backend when bound |
| `NativeOpsBackend` / `StsNativeOps` | Become **intent execution** side of STS1 backend (not consumer-facing authority) |
| `UiOps` named C2 methods | Sugar → surface `action` / intent |
| `FakeNativeOps` | Extend toward `FakeBackend` (snapshot + intent log) |
| Host SPI in [`art-framework.md`](./art-framework.md) | `PresentationHost` gains explicit backend + assets binding |

## Testing

- Pure JUnit: `FakeBackend` scripted frames, intent log, intercept order, stale/unavailable.
- No device required for contract tests.
- Optional D1 only after STS1 backend draws real surfaces ([`c2-full-present.md`](./c2-full-present.md)).

## Phasing

See [`docs/task.md`](../task.md) **15.x**. Design-first slices:

| Slice | Deliverable |
|-------|-------------|
| 15.0 | This doc + c2-full-present + host-assets + task links |
| 15.1 | Context/Backend pure interfaces + FakeBackend JUnit |
| 15.5+ | Combat/map surfaces consume frames (implementation in c2 doc) |

## Related

- [`c2-full-present.md`](./c2-full-present.md) — full C2 present + card hard-sync
- [`host-assets.md`](./host-assets.md) — ResourceId / packs / resolve
- [`ui-ops-probe.md`](./ui-ops-probe.md) — commands + snapshot
- [`dual-track.md`](./dual-track.md) — C1/C2 host tracks (thin intercept = transitional)
- [`docs/development/consumer.md`](../development/consumer.md) — consumer jar surface

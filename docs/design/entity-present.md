# EntityPresent — co-op chrome slots

Display-layer slot lifecycle for player / card / relic / monster chrome.
Complements [`c2-full-present.md`](./c2-full-present.md), [`dual-track.md`](./dual-track.md).
Roadmap: [`docs/task.md`](../task.md) milestone **24**.

## Role

| Concern | Owner |
|---------|--------|
| Combat hand cards | Full-present `sts1.combat.hand` + projection `CardEntity` |
| Co-op overlays / lobby chrome | **EntityPresent** slots + `EntityDrawPath` |
| Skeleton animation | `sts1.skeleton` surface + host bridge |
| Combat authority | **Not** ART |

## API

```text
attach(slotId, kind, refId) → sync(snapshot) → layout(x,y,scale) → detach
```

- `kind`: `PLAYER` | `CARD` | `RELIC` | `MONSTER`
- Snapshot: framework-owned immutable `EntitySnapshot` (pose, resourceIds, chrome flags).
  `sync(Object)` remains a compatibility ingress for `null`, `EntitySnapshot`, and recursively
  immutable string-key Maps; it rejects unsupported values and never retains the caller object.
- Presentation entity `c2:entity:{slotId}` is the draw/chrome anchor; RenderHost targets are derived
  caches. EntityPresent slots are intentionally effect-free in the current API. They own identity,
  snapshot, transform, and host draw state only. FX or co-op marks must use a separate ECS-owned
  visual/overlay entity rather than a host-only `EffectBinding` on the slot target.

## Policy (24.4)

When combat hand is `PresentLevel.FULL` and hand surface is mounted, **EntityPresent CARD**
slots are **overlay-only** for the slot draw (for example, co-op chrome). They do **not** replace
hand hard-sync draw. Any FX/co-op mark is a separate visual item, not an effect attachment on the
EntityPresent slot itself.
Non-CARD kinds are independent of hand FULL.

## Draw path

```text
EntityPresent slots → EntityDrawPath items → HostAssets.resolve → optional host paint
```

Probe: `entities.slotCount`, `entities.slots[]` (id, kind, refId, laidOut, resources).

## Non-goals

- Replacing `AbstractPlayer` / monster combat resolution
- Dual card identity models for in-combat hand (use `CardRef` / projection)

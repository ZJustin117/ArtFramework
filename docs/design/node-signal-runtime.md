# Node ↔ Signal runtime (connections, actions, FSM)

Architecture: **external operation ↔ SignalGroup ↔ Node/backend consumer**. Complements
[`backend-context.md`](./backend-context.md), [`art-framework.md`](./art-framework.md),
[`component-composition.md`](./component-composition.md).

## Principles

1. LML/JSON declares structure, signal **capability**, and **data-only** wiring — never scripts.
2. Every signal belongs to a `SignalGroup`; the default `native` group routes external operations.
   Exact names and **regex** never cross a group boundary.
3. Backend listeners consume operations from `native`; they do not emit operations. Internal component
   writes use ECS directly and never publish a signal.
4. Host `tick(dt)` advances AnimationPlayer / EffectPulse — **not** `context/frame/updated`
   (authority snapshot only).
5. C1 control values stay in thin **WidgetSession**; domain authority stays Backend.

## Layers

```text
External operation / native hook
        ↕  SignalGroup `native` (exact + Pattern, interceptable)
Node
  connections[]  →  UiActions
  states{}       →  NodeStateMachine
  art.animation_player  →  once|loop + pause (FSM idle/playing/paused)
        ↕
WidgetSession · EffectBinding · RenderHost
```

## Declarative `connections`

On any node props (typically window or behavior node):

```json
"connections": [
  { "match": "ui/ok/pressed", "action": "pulse_effect",
    "args": { "target": "panel", "effect": "lightwave", "duration": 0.45 } },
  { "match_pattern": "ui/wave/value_changed", "action": "set_prop",
    "args": { "target": "panel", "prop": "fx_intensity", "from_payload": 0 } }
]
```

| Field | Role |
|-------|------|
| `match` | Exact group-local signal name |
| `match_pattern` | Java regex on full bus name |
| `source` + `signal` | Shorthand → `ui/<source>/<signal>` |
| `action` | Registered id |
| `args` | Pure data; `from_payload` indexes emit payload; `if_payload` gates on first payload (e.g. anim name); `from_slider` reads WidgetSession |

Legacy `triggers: [{ source, signal, play }]` normalizes to `action=play`.

Unknown `action` → mount fails.

## Built-in actions

| id | Role |
|----|------|
| `play` / `pause` / `stop` / `resume` | AnimationPlayer |
| `set_prop` | Target prop + `PropEffectBridge` (`fx_*` → effect params) |
| `pulse_effect` | `EffectPulse` envelope on binding |
| `emit` | Re-emit another declared signal |
| `close_window` | Close (optional FX pulse) |

Third-party: `ArtFramework.registerUiAction("mod.foo", action)`.

## NodeStateMachine

Optional `states` prop:

```json
"states": {
  "initial": "closed",
  "transitions": [
    { "from": "closed", "to": "open", "match": "ui/open_btn/pressed" },
    { "from": "open", "to": "closed", "match_pattern": "ui/close_.*/pressed",
      "on_enter": [{ "action": "mod.enter_hit" }] }
  ]
}
```

- Finite string states; no expression guards / hierarchical regions (v1).
- AnimationPlayer uses ECS-backed idle/playing/paused state. Completion chaining is declaration data
  (`next`, optional `next_mode`), not an internal lifecycle signal.

## Widget data

| Kind | Store |
|------|--------|
| Slider / text / checkbox | `WidgetSession` (flat) |
| FX / anim props | `NodePropertiesComponent` + `EffectsComponent`, rendered from `PresentationFrame` |
| Game / settings authority | Backend + frame / intent |

## Related APIs

- `SignalGroups.nativeGroup().connect(name|Pattern, listener)`
- `ArtFramework.registerUiAction` / `uiActionIds` / `nodeState`
- `EffectPulse.tick` via `ArtFramework.tick`

## Non-goals

- Executable LML, per-frame `_update` bus spam, frame-signal as GL clock, frontend domain store,
  component-change lifecycle signals.

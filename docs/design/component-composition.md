# Component composition (ArtFramework)

Composable, extensible UI declaration for C1 (and render attach for C2). Complements [`dual-track.md`](./dual-track.md). Implementation is phased; this doc is the contract.

## Goals

1. **Composable** — nest containers, named slots, registered templates (`ref`).
2. **Extensible** — register new template ids and (later) leaf types without rewriting the host.
3. **Testable** — pure Java expand + layout bounds; default JUnit gate; optional art-verify fixtures.
4. **Compatible** — existing `window` / `label` / `button` JSON still loads.

## Four composition primitives

| Primitive | Mechanism | Role |
|-----------|-----------|------|
| **Nest** | `children[]` on containers | Structure / flex-like layout |
| **Slot** | `slots.{name}` + template `{ "type":"slot","name":"…" }` | Named insertion for composites |
| **Attach** | `effects[]` → future `RenderHost` | Visual stack; does not affect layout math |
| **Bind** | Signals + `UiOps` + `connections` → `UiActions` + runtime props | Behavior; data-only wiring / registered actions — no scripts. [`node-signal-runtime.md`](./node-signal-runtime.md), [`godot-aligned-ui.md`](./godot-aligned-ui.md) |

## Node shape

```json
{
  "type": "col",
  "id": "root_col",
  "props": { "text": "optional leaf text" },
  "layout": { "width": 400, "height": 240, "pad": 8, "gap": 6, "grow": true },
  "effects": [{ "id": "glow", "params": { "intensity": 0.5 } }],
  "children": [],
  "slots": {},
  "ref": "dialog"
}
```

Shorthand (normalized by loader): top-level `text` / `title` / `min` / `max` / `value` → `props`.

## Built-in `type` values (framework)

| type | Kind | children? |
|------|------|-----------|
| `window` | container | yes |
| `row` / `col` / `stack` / `panel` | container | yes |
| `fragment` | pass-through | yes |
| `label` / `button` / `slider` / `hitarea` | leaf | no |
| `ref` | expand via `ComponentRegistry` | slots only |
| `slot` | template placeholder | no |

Unknown types → load error (or future registry of leaf factories).

## Template expand (`ref`)

1. `ComponentRegistry.register(name, templateRoot)`.
2. Instance: `{ "type":"ref", "ref":"name", "props":{…}, "slots":{ "body":[…] } }`.
3. Expander substitutes `props` into template string props (`${key}` or exact prop overlay) and replaces `slot` nodes.
4. Result is a normal tree (no remaining `ref`/`slot`) for layout.

Rules: pure, no GL; window-local `id` uniqueness recommended for interactive leaves.

## Layout engine (pure)

- Containers assign child bounds from `layout.width/height/pad/gap` and child preferred sizes.
- `effects` ignored for geometry.
- Output: `LayoutResult` with optional id → `Rect` map + full tree for tests.
- **Forward:** `grow` expands toward Godot-like size flags + min size + stretch ratio ([`godot-aligned-ui.md`](./godot-aligned-ui.md)); container-first, anchors optional at root only.

## Runtime

- **Today:** expand → `WidgetSession` + Stage inflate (`ComponentActors`) + `RenderHost` from `effects`.
- **Target:** `Node` / `NodeTree` per scope; signals for leaf interaction; layout truth in `BoundsComponent` ([`godot-aligned-ui.md`](./godot-aligned-ui.md)).
- C2 entity targets via Attach only (not window children); C2 screens as NativeControl components (same doc).

## Testing

| Layer | What |
|-------|------|
| JUnit | parse, expand, layout bounds, registry, id index |
| art-verify fixture | offline assert on composition scenario JSON / probe fields when wired |
| Device | optional after Stage inflate |

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — unified API, UiTree/signals, C2 components
- [`component-layout-fx.md`](./component-layout-fx.md) — controls + GLSL shell roadmap
- [`docs/development/logic-layer-testing.md`](../development/logic-layer-testing.md)

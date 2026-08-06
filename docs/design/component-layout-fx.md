# Component layout + effects roadmap

Builds on [`component-composition.md`](./component-composition.md).

## Phase status

| Phase | Deliverable | Status |
|-------|-------------|--------|
| A | UiNode AST, Nest containers, LayoutEngine, ComponentRegistry, ref/slot expand, JUnit | done |
| B | Leaf widgets state + UiOps slider/hitarea; Stage inflate | done |
| C | RenderHost + EffectRegistry + glow/tint draw; entity Attach | done |
| D | ShaderProgram compile path (`ShaderRuntime`) | done |
| E | FULL_FRAME overlay (enable + tint/glow bind) | done |
| F | Screen capture (`FrameCapture`) + blur/glass shaders + `glass` component | done |
| G | Multi-pass Gaussian FBO chain; richer widgets | pending |
| H | Godot-aligned core: size flags / Theme / C1 widgets / C2 NativeControl | see [`godot-aligned-ui.md`](./godot-aligned-ui.md) + task 11.x |

## Public surface

- `artframework.component.*` — composition AST + WidgetSession
- `artframework.render.*` — `RenderHost`, targets, `tint` / `glow`, `ShaderRegistry`
- `ArtFramework.render()` — global host; `UiProbe` field `render`

## Not in ArtFramework

- Multiplayer protocol / combat authority
- Replacing STS hand-play pipeline with scene2d

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — API/runtime roadmap beyond FX
- [`component-composition.md`](./component-composition.md)

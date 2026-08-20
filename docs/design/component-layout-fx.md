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
| G | Multi-pass Gaussian FBO chain; richer widgets | Gaussian FBO chain done; richer widgets remain future work |
| H | Godot-aligned core: size flags / Theme / C1 widgets / C2 NativeControl | see [`godot-aligned-ui.md`](./godot-aligned-ui.md) + task 11.x |

## Public surface

- `artframework.component.*` — composition AST + WidgetSession
- `artframework.render.*` — `RenderHost` frame cache, `tint` / `glow`, `ShaderRegistry`
- `ArtFramework.render()` — global host; `UiProbe` field `render`

## Phase G Boundary

`FrameCapture` now owns a bounded 1–4 pair separable Gaussian chain using horizontal and vertical
ping-pong frame buffers. `BlurEffect` and `GlassEffect` sample the chain output when a compiled
shader and GL context are available; capture, effect, and headless fallback behavior remains
unchanged when they are not. The remaining Phase G work is richer widget coverage, not another
capture authority or a second render pipeline.

## Not in ArtFramework

- Multiplayer protocol / combat authority
- Replacing STS hand-play pipeline with scene2d

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — API/runtime roadmap beyond FX
- [`component-composition.md`](./component-composition.md)

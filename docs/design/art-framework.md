# ART Framework — presentation graph

ART (presentation framework) is the **display-layer base** for STS1 mods and a structural target for future STS2 hosts. It is not multiplayer/protocol authority.

Identity (breaking, post-rename):

| Surface | Value |
|---------|--------|
| Product | ART Framework |
| Package root | `artframework` |
| MTS modid | `artframework` |
| Artifact | `ArtFramework.jar` |
| Facade | `artframework.api.ArtFramework` |
| Console | `art` |
| Probe prefix | `ART_PROBE` |
| Env prefix | `ART_*` |
| Verify tooling | `tools/art-verify` |

Complements: [`godot-aligned-ui.md`](./godot-aligned-ui.md), [`dual-track.md`](./dual-track.md), [`component-composition.md`](./component-composition.md), [`ui-ops-probe.md`](./ui-ops-probe.md), [`backend-context.md`](./backend-context.md), [`c2-full-present.md`](./c2-full-present.md), [`host-assets.md`](./host-assets.md). Roadmap: [`docs/task.md`](../task.md) milestones **12** (done), **15** (Backend / full C2 / HostAssets).

## Purpose

1. One **Presentation Graph** for UI, native screen presenters, overlays, effects, animation, and (later) skeleton/particle nodes.
2. **Host SPI**: core types stay free of STS / BaseMod / libGDX concrete APIs; STS1 is the first full host.
3. **Declaration vs behavior**: JSON/LML declare structure, props, layout, effects, and **named signals**; Java owns handlers and game rules.
4. Attract display-layer mods as an MTS **dependency** (`basemod` + `artframework`) without owning combat/party protocol.

## Non-goals

- CrossSpire protocol, party election, combat phase authority.
- Executable logic / Java class names / scripts / raw GLSL inside LML or layout JSON.
- Cloning full Godot / Spine editor surfaces.
- Dual-device life / connector as default ArtFramework gates.

## Architecture

```text
Caller
  → artframework.api.ArtFramework
       register | mount | unmount | tree | ops | probe | render | nodes (planned)
  → Core (host-agnostic)
       PresentationNode (decl AST; today UiNode)
       PresentationTree / PresentationInstance (today UiTree / UiInstance)
       SignalHub · LayoutEngine · Theme · ComponentRegistry · NodeRegistry (planned)
       RenderGraph bookkeeping (today RenderHost pure side)
  → Host SPI
       PresentationHost · HostRenderBackend · HostInput · HostAssets · NativePresentationBridge
  → host.sts1 (today c1 + c2 patches + StsNativeOps)
       StageHost · ComponentActors · StsSkin · @SpirePatch hooks · StsNativeOps
  → host.sts2 (future; design only until real API exists)
```

| Layer | Role |
|-------|------|
| Declaration | Immutable tree: type, id, props, layout, effects, children, slots, **signals** |
| Runtime tree | Per-mount index, lifecycle, prop overlay, rect, theme cascade, signal hub |
| Render | Targets, effect bindings, shader registry; GL only on host backend |
| Native | Observe/present STS1 screens via thin patches → pure hooks → `UiComponent` |
| Consumer | Mount/bind, connect signals, UiOps, register templates/effects/node types |

## Presentation node kinds (target)

| Kind | Layout? | Draw? | Examples |
|------|---------|-------|----------|
| Control | yes | yes | window, row/col, label, button, … |
| Behavior | no | no | animation_player (planned) |
| Visual / effect | wraps or attaches | yes | shader_effect, glass, UiNode.effects |
| Composition | expand-time | — | ref, slot |
| Native presenter | host-owned | host | sts1.map (migrate from `sts.*`) |

Third-party types: **namespaced** (`my_mod.ripple_effect`). Registration via explicit SPI — no LML reflection of Java classes.

## Signals

- Godot-shaped: `connect` / `disconnect` / `emit` on tree/instance (`SignalHub`).
- Declaration lists allowed signal names on the node (LML requires explicit `signals="…"`; JSON may auto-fill built-in control defaults during migration).
- Payload contracts documented per type (e.g. `pressed`, `value_changed(float)`).
- No business methods in declaration; handlers stay in Java.
- C1 signal interceptors run before state mutation and emission; C2 native signals emit after
  the native gate allows the action and before the backend gesture.
- **Declarative wiring (39–42):** `connections` / legacy `triggers` → registered `UiActions`;
  exact or regex on full bus names; optional `states` FSM. See
  [`node-signal-runtime.md`](./node-signal-runtime.md). Not a substitute for Backend
  `context/frame/updated`.

## LML (planned)

- Pure Java XML → same AST as JSON (`UiNode` / future `PresentationNode`).
- Safe parser (no external entities/DTD).
- Maps tags/attrs → type, id, props, layout, effects, slots, signals.
- Forbidden: controller methods, class names, scripts, embedded GLSL.

## Effects & shaders

- Already: `EffectRegistry`, `ShaderRegistry`, `ShaderRuntime`, `RenderHost`, `EffectDecl` on nodes.
- Third parties register effects/shaders by id; declaration only references registered ids.
- Capability-aware hosts: unsupported capture/shader → validated failure / fallback, not silent crash.

## Host SPI (phased)

Today: `HostBackend` attach/detach/applyLayout + C1 `StageBackend` + C2 patches.

Target `PresentationHost`:

```text
capabilities()
renderer() · input() · assets() · nativeBridge()
attach(tree) · detach(tree) · tick(delta)
```

`HostCapabilities` (examples): scene_graph, offscreen_target, frame_capture, shader_program, skeleton_renderer, native_intercept, entity_anchor.

STS1 implements full set used by current C1/C2. STS2 implements what the real host exposes; core never assumes STS1-only globals.

## Native presentation

- C2 templates remain until migrated to `NativePresentationBridge` + `sts1.*` ids.
- Patches stay thin; pure logic in hooks/templates/components.
- Entity slots are anchors for overlays/effects — not free-form window children.
- Display-layer only: intercept/observe/present; do not smuggle combat authority into ART.

## Compatibility

| Surface | Policy after rename |
|---------|---------------------|
| `spireui` package / modid / jar / console | **Removed** (breaking) |
| `ArtFramework.open` / `bind` / `close` | Keep; aliases of mount/unmount where applicable |
| `UiOps` / `UiProbe` names | Keep until Presentation Graph rename slice |
| `UiNode` / `UiTree` | Keep as implementation; public rename is a later milestone |
| Probe `modId` | `artframework` |
| Console root | `art` |

## Implementation phases (summary)

| Phase | Deliverable |
|-------|-------------|
| **12.0** | This doc + task list + AGENTS/README links (identity already renamed) |
| **12.1** | Declared signals on AST + connect/emit validation + JUnit |
| **12.2** | `NodeRegistry` / type SPI; loader uses registry; namespaced third-party types |
| **12.3** | LML → AST loader + SyntheticRuntime resource dispatch + art-verify sample |
| **12.4** | C1 node factory SPI; migrate `ComponentActors` branches |
| **12.5** | RenderGraph split / host render backend boundary (no behavior loss) |
| **12.6** | `animation_player` + `shader_effect` nodes (data-only tracks; registered effects) |
| **12.7** | Skeleton provider SPI + fake provider tests |
| **12.8** | Native id namespace `sts1.*` + bridge naming (consumer-visible break) |

Default gate: `./scripts/with-art-env.sh test`. Offline: `tools/art-verify`. Device: optional D1 only.

## Related

- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — Godot term map and milestone 11 (done)
- [`dual-track.md`](./dual-track.md) — C1/C2 host tracks (thin intercept transitional)
- [`backend-context.md`](./backend-context.md) — pluggable Backend, three faces, intents (15)
- [`c2-full-present.md`](./c2-full-present.md) — C2 full present / hard-sync (15)
- [`host-assets.md`](./host-assets.md) — HostAssets packs / ResourceId (15)
- [`component-composition.md`](./component-composition.md) — Nest / Slot / Bind
- [`component-layout-fx.md`](./component-layout-fx.md) — layout + FX
- [`ui-ops-probe.md`](./ui-ops-probe.md) — commands + snapshot
- [`docs/development/consumer.md`](../development/consumer.md) — consumer jar / MTS deps

# Presentation Entity Runtime

## Status

This document is the implementation contract for replacing the parallel C1 tree, C2 surface,
projection, entity-present, and render-target stores with one runtime presentation model.

## Goals

1. `PresentationWorld` is the sole owner of mutable ART presentation state.
2. `NodeTree` and `Node` provide the Godot-shaped public scene-tree API over ECS entities.
3. C1 declarations and C2 host projections materialize the same entity/component vocabulary.
4. Effects, including Lightwave, attach to visual entities rather than string render targets.
5. Host implementations consume immutable frame snapshots and never become presentation-state
   authorities.

## Non-goals

- Store STS game authority, Java listeners, libGDX objects, textures, shader programs, or
  BaseMod objects in ECS components.
- Force C2 host projections to be JSON `UiNode` declarations.
- Make a decorative border, glyph, shader strip, or atlas layer a separate entity when it has no
  independent identity or lifecycle.

## Layers

```text
UiNode / NodeDef              C2 projection / native source
immutable declaration         host-derived source
             |                         |
             +------ materialize ------+
                                       v
                         PresentationContext / PresentationWorld
                                       |
                                  NodeTree / Node
                                       |
                             immutable PresentationFrame
                                       |
                    C1 Stage adapter        C2 native/full-present adapter
```

`UiNode` remains the immutable declaration AST. It supplies initial type, props, children,
declared signals, and default effects; it is never a runtime entity. A C2 source directly
materializes runtime nodes from backend/native projection data.

## Entity Ownership

Every runtime node has exactly one `EntityId`. The entity is authoritative for mutable state.

| Concern | Component / service | Notes |
|---|---|---|
| Stable identity | `NodeIdentityComponent` | Key, name, kind, source track |
| Tree structure | `NodeHierarchyComponent` | One parent, ordered child ids |
| Lifecycle | `NodeLifecycleComponent` | declared, mounted, ready, detached |
| Runtime props | `NodePropertiesComponent` | declaration defaults copied at materialization |
| Geometry | `BoundsComponent` / `TransformComponent` | layout and draw positions |
| Visibility/input | `VisibilityComponent` / `InteractionComponent` | enabled, visible, opacity, focus/hover |
| Visual content | `DrawComponent` | host-neutral resource/text payload |
| Style | `ChromeComponent` | resolved panel, border, label/accent, alpha |
| Effects | `EffectsComponent` | ambient and pulse attachment state |
| Host realization | `HostBindingComponent` | host kind plus local key/opaque handle |
| C2 ownership | `PresentPolicyComponent` | FULL/OBSERVE, native suppress, input owner |
| Signals | `SignalPortsComponent` | declared ports only |

Domain-specific components such as card placement, surface policy details, skeleton bindings, and
entity snapshots remain owned by their packages. They may reference common entity ids but do not
replace the common components above.

## Parent vs Child

A child is an entity when it has independent identity, lifecycle, geometry, interaction, visual
state, effect state, z-order, or probe/action addressability. A parent component owns shared
policy, layout, clipping, inherited defaults, and group lifecycle.

```text
combat.hand                      parent entity
  PresentPolicy + fan layout + Lightwave defaults
  |
  +-- card:<instance-id>          child visual entity
  |     bounds + draw + chrome + effects + interaction
  |
  +-- card:<instance-id>
```

Transparency and borders are `ChromeComponent` fields on the owning visual entity. A normal
Lightwave band is an effect attachment on the same entity. Shader strips, border edges, glyphs,
and atlas layers are renderer details, not entities. A cross-item sweep becomes a distinct entity
only when it has independent transform, lifetime, or target scope.

## Node API

`Node` is a lightweight facade over `PresentationContext` and `EntityId`; it has no mutable
hierarchy, props, bounds, or lifecycle copy. `NodeTree` is the Godot-shaped API over the ECS
hierarchy and owns signal listener cleanup and lifecycle ordering.

```text
Node.parent()        -> NodeHierarchyComponent.parent
Node.children()      -> NodeHierarchyComponent.children
Node.get/set(prop)   -> NodePropertiesComponent
Node.rect()          -> BoundsComponent
Node.connect/emit()  -> NodeTree SignalHub
```

Godot alignment:

| Godot | ART |
|---|---|
| `PackedScene` | `UiNode` / `NodeDef` |
| `SceneTree` | `NodeTree` |
| `Node` | ECS-backed `Node` facade |
| `NodePath` | hierarchy lookup in `NodeTree` |
| `Control` | entity with bounds, draw, and interaction components |
| `Theme` / `StyleBox` | immutable resource resolved into chrome component data |
| signal | NodeTree API plus `SignalHub` service |

## Signals

Signals are node API, not listener-bearing ECS data.

```text
host input / state transition
  -> NodeTree.emit(entity, signal, payload)
  -> SignalHub dispatch
  -> declarative connection or UiAction
  -> ECS component mutation
  -> next PresentationFrame
```

`SignalPortsComponent` declares permitted ports. Serializable declarative connections may be ECS
data. Java listeners, regex subscriptions, ordering, first-stop dispatch, and unsubscribe handles
remain in `SignalHub`. Entity destruction disconnects incoming and outgoing subscriptions before
components are removed.

## C1 and C2 Materialization

C1 materializes `UiNode` declarations into entities. The Stage host is an adapter/cache that maps
entities to scene2d actors.

C2 materializes surface parents and exact visual children from projection/native state. A surface
entity owns C2 policy and layout scope. Cards, controls, energy, intents, map nodes, rewards,
options, and shop items are visual children. The STS1 adapter remains responsible for native
interception and intent execution; the runtime owns only presentation state.

## Lightwave

Profile resolution provides inherited chrome/effect templates. During visual entity materialization
the resolver writes resolved `ChromeComponent` and `EffectsComponent` data. Lightwave has no C1 or
C2-specific core identity:

```text
visual entity
  ChromeComponent(panel alpha, border)
  EffectsComponent(lightwave ambient[, lightwave pulse])
```

The render pass clips effects to exact visual item bounds. Layout/policy surface entities do not
draw broad fallback bands.

## Rendering

`PresentationFrame` is an immutable per-frame snapshot ordered by layer/z. It is the sole input to
host render adapters. `RenderHost`, `RenderTarget`, and `EffectBinding` are compatibility/render
execution types only; they must be derived from the frame and may not hold independent authority.

## Migration Rules

1. Remove `UiInstance`; do not retain a second mutable node runtime.
2. Replace all independently owned presentation worlds with contexts registered in one runtime
   registry and stable `PresentationKey` namespaces.
3. Remove string render-target ids from public/runtime identity. They may remain adapter-local.
4. Preserve ART display-layer boundaries: host bindings are keys/opaque handles, never game
   authority.
5. Every migration slice adds focused pure JUnit before adapter/device verification.

## Acceptance Criteria

- Every runtime node resolves to one `EntityId` and all node mutation is ECS mutation.
- `NodeTree` reads hierarchy solely from ECS components.
- C1 and C2 visual items use the same bounds/chrome/effect components.
- C2 surface parents do not paint coarse Lightwave rectangles; exact visual children do.
- C1/C2 signals share declared-port validation, dispatch, cleanup, and action semantics.
- Probe exposes stable presentation keys, hierarchy, resolved chrome, effects, host binding, and
  policy without exposing host-native authority.

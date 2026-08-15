# Traditional ECS Runtime

## Status

This is the target implementation model for the ART runtime refactor. It supersedes the
multi-runtime interpretation of C1, C2, projection, render-target, and host state.

## Current Checkpoint

As of 2026-08-15, the refactor is approximately 80% complete against the full traditional-ECS
target. ECS is authoritative for persistent presentation data: C1/C2 entities, projection
frames, surface/template observations, native input and intent records, map gesture state,
profile selections, render-plan inputs, capture policy, Skeleton state, EntityPresent slots, and
diagnostic snapshots. A single production `PresentationSchedule` defines the frame order, and D1
verifies EntityPresent draw/recreation/cleanup plus Spine34 native takeover/recreation/cleanup.

The production schedule owns fixed instances for surface intent execution, authority projection,
business confirmation, native intent lifecycle, normalization, animation, effects, render
projection, render clock, and backend advancement. Synchronous compatibility callers use those
schedule-owned systems rather than constructing systems or maintaining a second result store.
`RenderHost` plan rebuilds and its render clock are render-package internals; explicit lifecycle
and active-surface projection requests go through `RenderProjectionQueue`. `OPEN` is now an alias
and legacy layout-root cache only, while `FullFrameRenderComponent` is the sole full-frame enabled
state. Fixed catalogs, callback hubs, asset catalogs, host resources, and provider caches remain
intentionally outside ECS authority.

## Rules

1. An ART entity has only an ID. It exists only when ART gives it an independent lifecycle.
2. Components contain data only. Data source does not affect component semantics.
3. Systems are stateless. They query and update component data during an ordered tick.
4. All persistent ART presentation facts live in components. Static registries and host caches
   are transitional only and may not become a second authority.
5. Game objects, scene2d objects, GL objects, listeners, reflection handles, and callbacks are
   external implementation details, never component values.
6. Every native game interaction enters ART first. The ECS data flow decides whether ART handles
   it or explicitly allows the game to continue.

## Data Flow

```text
game observation / declaration / raw input
                 |
                 v
          ART component data
                 |
                 v
  ordered stateless ECS systems
       |                    |
       v                    v
component updates     native intent execution
       |
       v
rendered ART presentation
```

Production advances that flow through one `PresentationSchedule` in this fixed order:

1. surface intent execution via `SurfaceIntentExecutionSystem`
2. authority projection, business confirmation, and native intent lifecycle
3. shared-world normalization
4. C1 animation playback via `AnimationPlaybackSystem`
5. effect envelopes via `EffectPulseSystem`
6. host-specific presentation advancement, including skeleton provider caches
7. coalesced render projection via `RenderProjectionSystem`
8. render clock via `RenderClockSystem`
9. host backend tick via `HostBackendTickSystem`

`ArtFramework.advanceFrame(delta, authorityFrame)` is the production entry. Compatibility
`tick(delta)` delegates to the same schedule without an authority frame; `publishFrame(frame)`
remains an immediate signal-compatible ingestion API. Property/effect writes made during a
scheduled update mark C1 windows dirty and rebuild each host render cache once at the render
projection phase.

## Migration

The refactor is incremental. A legacy store may remain only until its corresponding entities and
components are produced and consumed exclusively by ECS systems. New behavior must not add to a
legacy authority. Each slice requires focused JUnit before host/device verification.

## Migration Ledger

The following order is mandatory. A row is complete only when its legacy authority is removed or
reduced to a read-only host cache, its compatibility API reads ECS data, and its focused JUnit plus
the full suite pass. Native/device verification is required only after source that reaches an STS
host hook or drawing path changes.

| Slice | Legacy authority | ECS destination | Completion evidence |
|---|---|---|---|
| 1 | `PresentationRegistry` scope worlds | One `ArtEcs` world, context-owned entity index | Scope close/reset destroys only owned entities; shared-world JUnit |
| 2 | `WidgetSession` control values | `ControlValueComponent` | UiOps, probe, actor materialization, actions, and FX read ECS values |
| 3 | `PresentProjection` card maps and frame fields | Card components plus projection-root frame, interaction, and snapshot components | Add/update/remove/epoch/reset and compatibility lookup JUnit |
| 4 | `PresentSurfaces` mounted flag and entity-ID map | `SurfaceLifecycleComponent` and stable surface key | Mount/unmount/reset/probe JUnit; lifecycle facade submits a one-shot ECS request |
| 5 | C2 surface action return path | Surface action, intent identity, and result components | Accepted, queued, rejected, and blocked SignalBus JUnit; synchronous compatibility calls use the schedule-owned execution system |
| 6 | Native patch/router input decisions | Per-surface native input and intercept components | Hand, controls, map, event, select, end-turn, and legacy UiOps allow/block records |
| 7 | C1 declaration/session lifecycle | Hierarchy, lifecycle, properties, visibility, and host-binding components | Complete: C1 materializes directly into registered contexts; object-tree and session registries are deleted; `WidgetSession` is an on-demand immutable declaration view; StageHost actors are binding-derived caches |
| 8 | C2 template active state and native-template mappings | Surface/template observation components | Native template facades no longer retain active flags; bind, map pin, and end-turn probe data query ECS. Scene/rebuild adapter cleanup remains |
| 9 | Native intent execution and pending gestures | Intent lifecycle/result components; executor as host boundary only | Complete: router/executor outcomes are one-shot ECS events consumed by `NativeIntentLifecycleSystem`; the next available authority frame transitions executed intent to `CONFIRMED`, and an unavailable frame transitions it to `FAILED`. Domain-specific business confirmation remains separate |
| 10 | Signal connection and state-machine registries | Declarative connection/state components; SignalBus subscriptions as host cache | Complete: `ConnectionDeclarationsComponent` and `NodeStateComponent` are ECS data; subscriptions are disposed/rebuilt cache with focused JUnit |
| 11 | Render target/effect/animation authority | Draw, effect, animation, profile, and resource-description components | `PresentationSchedule` orders all production systems; C1/C2/surface/full-frame/entity targets rebuild from ECS. `FullFrameRenderComponent` is the sole enabled-state authority. D1 `d1_full_present_combat_ready` passes |
| 12 | Skeleton and EntityPresent lifecycle/native bindings | Skeleton descriptor, pose, animation, effect, and host-binding components | Skeleton identity/snapshot frame/asset/pose/animation/visual state and EntityPresent slot identity/snapshot/transform state are ECS data; native handles/listeners are host caches. `SkeletonPresentationFrames.publish()` notifies the bridge to build ECS-backed provider bindings and native claims. Pure tests prove host cache recreation from retained ECS state; D1 EntityPresent draw/cleanup, Spine34 lifecycle, native takeover, recreation, and cleanup pass |
| 13 | Probe/API/console compatibility stores | ECS-only query adapters | C1 controls, native template state, EntityPresent, window-open, profile selection, and render capture queries read ECS; fixed catalogs, signal hubs, and host/provider caches remain explicitly non-authoritative |

### Per-Slice Procedure

1. Add a focused failing pure JUnit that expresses the desired ECS authority and cleanup boundary.
2. Add or refine data-only components. Do not store game objects, scene2d actors, GL resources,
   callbacks, listeners, reflection handles, or executor objects in a component.
3. Implement the smallest stateless writer/query path. A host interaction may remain a local call
   boundary, but it must receive its request from ECS data and write its outcome back to ECS.
4. Convert compatibility methods to query ECS. During transition they may construct immutable
   compatibility views on demand, but must not retain a second mutable store.
5. Destroy entities through `PresentationContext` so both the world and the context ownership index
   are cleaned together. Verify scene epoch, reset, and parent/surface close where applicable.
6. Run the focused tests, then `./scripts/with-art-env.sh test`. For hook/render source changes,
   deploy and run the applicable D1 verification only after JUnit passes.
7. Mark the ledger row complete only after its old authority is deleted or explicitly documented as
   a read-only host cache with no presentation authority.

### State Classification

| Current store | Classification during migration | Final treatment |
|---|---|---|
| Removed C1 object/session registries; `PresentProjection`, `PresentSurfaces` remain | Transitional presentation authority | C1 declaration, hierarchy, lifecycle, controls, signals, animations, and state machines use context/entity APIs; disposable subscriptions and actor objects remain host caches |
| `NativeTemplateRuntime`, template classes, native patch hooks | External-game observation/input adapter | Snapshot into components; do not store native objects |
| `RenderHost`, `EffectTargetActors`, STS textures/shaders | Host render implementation/cache | `RenderPlan.fromEcs()` is the sole target/effect cache input; RenderTarget, EffectBinding, Actor, texture, shader, and capture objects remain non-authoritative host resources |
| `Sts1IntentExecutor`, map gesture bridge, native skeleton bridge | Host action implementation | Read ECS request data, execute locally, write structured outcome components |
| `SignalBus`, scene2d listeners, subscriptions | External callback mechanism | Store declarative ports/connections in ECS; rebuild subscriptions as disposable cache |
| Pack/profile/theme catalogs | Immutable configuration source | Resolve to runtime style/resource-description components |

### Audited Host Boundaries

The following containers are compatibility or host boundaries. They must not retain mutable
presentation facts or be reclassified as ECS state merely by wrapping them.

| Container | Remaining responsibility | Required destination |
|---|---|---|
| scene2d actor bindings | C1 host realization cache | ECS hierarchy/lifecycle/host-binding data with disposable actor cache; lookup is context/entity based |
| `NativeTemplateRuntime`, `SyntheticComponents`, `PresentSurfaces`, `ArtFramework.OPEN` facades | Compatibility component lookup, native adapter dispatch, handle/layout-root aliases | ECS-derived compatibility views and action/intent systems; `PresentSurfaces` resolves its context/world on demand; `OPEN` is not lifecycle authority |
| `UiOps` handler map, `UiInspect` registry reads | Imperative callback sugar and inspect routing | UiOps result history map removed; handler callbacks remain host cache; UiProbe C1 window controls/title/profile snapshot now reads registered ECS context. ECS business confirmation records card/map/event/reward/select evidence |
| `RenderHost` targets/bindings, `EffectTargetActors` | Surface/full-frame target mutation and host actor lookup | `RenderSurfaceComponent`, `FullFrameRenderComponent`, and immutable per-frame ECS plans consumed by host cache; C1 and C2 item targets rebuild from ECS bindings/visuals; no RenderHost full-frame enabled mirror |
| `NodeConnections` declaration maps | Parsed connection declaration retention | Dedicated immutable connection component plus rebuilt disposable subscriptions |
| `NodePropertiesComponent`, `EffectsComponent` | Mutable in-component overlays/attachments | Immutable value components replaced through the world on writes |

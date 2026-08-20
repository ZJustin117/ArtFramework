# API Overview

ArtFramework exposes a small consumer facade and a set of typed domain and extension APIs.
The facade is the default entry point for mods. The lower-level APIs are for consumers that need
typed frame access, custom providers, or host integration.

## API Layers

### 1. Consumer Facade

Stable, default entry points:

- `artframework.api.ArtFramework`
- `artframework.api.UiOps`
- `artframework.api.UiProbe`
- `artframework.api.WindowDef`
- `artframework.api.WindowClass`
- `artframework.api.WindowHandle`

Use this layer to register and mount UI, invoke actions, inspect state, connect signals, and select
presentation profiles or packs. It hides whether a component is implemented by C1 synthetic UI, C2
native templates, or a full-present surface.

```java
ArtFramework.register(windowDef);
WindowHandle handle = ArtFramework.mount("mod.window");
UiOpResult result = ArtFramework.ops().invoke("mod.window", "confirm");
Map<String, Object> snapshot = ArtFramework.probe().asMap();
ArtFramework.unmount("mod.window");
```

`mount` / `unmount` are the preferred lifecycle names. `open` / `bind` / `close` remain compatibility
aliases and should not be used in new code unless a consumer must support the historical API.

### 2. Stable Typed Domain API

These APIs expose typed presentation contracts without exposing host implementation details:

- `artframework.context`: `ContextFrame`, surface views, `CardRef`, `UiIntent`, and projections
- `artframework.core`: `SignalBus`, `SignalGroup`, `Theme`, `PresentProfile`, `UiComponent`
- `artframework.presentation`: `PresentationContext`, `PresentationRuntime`, and frames
- `artframework.ecs`: `EntityId` and `PresentationWorld`
- `artframework.assets`: `HostAssets`, `AssetPack`, resource ids, and resolve results
- `artframework.skeleton`: provider contracts and skeleton sources

Use typed domain APIs when a consumer needs frame semantics, card identity, signal results, resource
resolution, or presentation-runtime integration. Prefer these types over parsing `UiProbe` maps for
runtime behavior. Probe output is diagnostic and follows its own schema contract.

```java
ContextFrame frame = ArtFramework.projection().lastFrame();
ControlsView controls = frame.controlsView;
CardRef card = frame.cards.get(0).ref;

SignalDispatchResult result =
        SignalGroups.nativeGroup().dispatch(new UiSignal("combat/end_turn"));
```

`PresentationWorld` is the authority for mutable ART presentation state. `EntityId` identifies
runtime presentation entities; it is not a game-authority or multiplayer entity id.

### 3. Extension SPI

Extension APIs are stable contracts with stricter ownership rules:

- `HostBackend` for host frame projection and presentation integration
- `HostAssets` / `AssetPack` for resource catalogs and resolution
- `SkeletonProvider` for provider-specific skeleton runtimes
- `C1NodeFactory`, `UiNodeRegistry`, and `ComponentRegistry` for custom UI nodes
- `UiAction` and `registerUiAction` for declarative connections
- `artframework.api.HostPresentationSystem` for the host presentation phase

Extensions may provide host objects and caches, but persistent presentation facts must be written to
the ART ECS model. A host object, callback cache, texture handle, scene2d actor, or provider cache is
not a second presentation authority.

```java
ArtFramework.setHostBackend(customBackend);
ArtFramework.nodes().register(customNodeType);
ArtFramework.skeletons().register(customProvider);
ArtFramework.registerUiAction("mod.confirm", action);
```

### 4. Host, Lab, and Compatibility API

These APIs are implementation-facing and may evolve without the same stability promise as the
facade and typed domain contracts:

- `artframework.sts1.render.*`
- `artframework.sts1.patch.*`
- `Sts1IntentExecutor` implementation details
- `StageHost`, `SyntheticRuntime`, and `NativeTemplateRuntime` internals
- `artframework.inspect.*` and `artframework.sts1.lab.*`
- `art ui` / `art lab` console internals
- test-only reset helpers

Use these APIs only for STS1 host integration, device lab tooling, framework tests, or compatibility
adapters. Do not make ordinary consumer code depend on draw-path classes or SpirePatch details.

## Facade to Domain Mapping

| Facade API | Underlying contract | Authority | Recommended use |
|---|---|---|---|
| `ArtFramework.mount` / `unmount` | C1 `SyntheticRuntime` or C2 `NativeTemplateRuntime` | Presentation lifecycle | Consumer lifecycle |
| `ArtFramework.component` | C1/C2/full-present `UiComponent` views | ECS-derived component state | Component actions |
| `ArtFramework.ops` | `UiOps` and native operation backend | Action / signal pipeline | Commands and gestures |
| `ArtFramework.probe` | `UiProbe` and inspect projections | ECS-derived diagnostic view | Diagnostics and tests |
| `ArtFramework.connect` / `dispatch` | `SignalGroup` -> `SignalBus` | Unified signal bus | Signals and interception |
| `ArtFramework.assets` | `HostAssetsHolder` -> `HostAssets` | Asset catalog and resolve | Resource registration |
| `ArtFramework.host` | `HostBackends` -> `HostBackend` | Host adapter | Custom host integration |
| `ArtFramework.tick` / `advanceFrame` | `PresentationSchedule` | Ordered production systems | Frame advancement |
| `ArtFramework.render` | `RenderHosts` -> `RenderHost` | Derived render adapter cache | Rendering extensions |
| `ArtFramework.entities` | `NativeTemplateRuntime` -> `EntityPresent` | EntityPresent ECS state | Entity chrome slots |
| `ArtFramework.skeletons` | `SkeletonProviders` | Provider registry | Skeleton extensions |

The facade is a convenience surface, not a second implementation. It must not retain an independent
mutable hierarchy, signal dispatcher, component registry, render target map, or entity identity.

## Common Workflows

### Register and mount a synthetic window

```java
ArtFramework.register(new WindowDef("mod.window", WindowClass.SYNTHETIC, layout));
ArtFramework.mount("mod.window");
ArtFramework.ops().clickButton("mod.window", "ok");
ArtFramework.unmount("mod.window");
```

### Use a native or full-present component

```java
UiComponent component = ArtFramework.component("sts1.map");
UiOpResult result = component.action("select_node", nodeId);
```

The accepted action is not the final authoritative frame. The next backend `ContextFrame` is the
source of truth for the resulting presentation state.

### Register and apply presentation resources

```java
ArtFramework.registerPresentProfile("mod.coolwave", theme, "mod.cool_pack");
ArtFramework.registerPresentPack(pack);
ArtFramework.setProjectPresent("mod.coolwave");
ArtFramework.bindSurfacePresent("sts1.combat.hand", "mod.coolwave");
```

Registration and application are separate operations:

- registering a profile does not apply it;
- registering a pack does not activate it;
- project presentation is a fallback;
- a node or surface-specific binding can override that fallback.

### Observe and dispatch signals

```java
SignalSubscription subscription = ArtFramework.connect(
        "context/frame/updated",
        signal -> observe(signal));

SignalDispatchResult result = ArtFramework.dispatch(
        new UiSignal("combat/end_turn"));

subscription.disconnect();
```

Use `dispatch` when the decision or replacement result matters. `emit` remains a compatibility
result-bearing alias. Accepted input does not replace backend authority; the next frame confirms the
visible result.

## Ownership Rules

All API layers follow these rules:

1. `PresentationWorld` is the sole owner of mutable ART presentation state.
2. Components contain data only; production systems are stateless and run through the fixed schedule.
3. C1, C2, EntityPresent, skeleton, and render adapters derive state from ECS frames.
4. Scene2d actors, native objects, GL resources, listeners, and provider caches remain host-side.
5. `UiProbe` and compatibility views are derived reads, not new writers.
6. Signals use the unified `SignalBus`; consumers must not create a parallel dispatch path.
7. ArtFramework does not own game authority, multiplayer protocol, party state, or combat rules.

## Stability and Compatibility

The stable facade and typed domain surface are documented in
[`api-stability.md`](api-stability.md). Consumer setup, Gradle wiring, and release validation are in
[`consumer.md`](consumer.md).

Compatibility rules currently include:

- `open`, `bind`, and `close` remain lifecycle aliases for at least one minor release;
- legacy `sts.*` ids are accepted as input and canonicalized to `sts1.*` where applicable;
- Probe schema v1 is additive until a documented migration changes it;
- host, lab, patch, and draw-path implementation classes may evolve independently.

ArtFramework remains a separate ModTheSpire dependency. Consumers compile against the jar with
`compileOnly` and ship the ArtFramework jar beside the consumer jar.

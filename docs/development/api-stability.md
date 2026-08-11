# API Stability Checklist

The public consumer surface is the `artframework.api` facade and the explicitly documented
contracts below. Consumers should compile against `ArtFramework.jar` with `compileOnly` and load
the jar as a separate ModTheSpire dependency.

## Stable Surface

### Facade (`artframework.api.ArtFramework`)

| Method group | Role |
|--------------|------|
| `register` / `isRegistered` / `find` / `listOpenIds` | Window registry |
| `mount` / `unmount` / `open` / `bind` / `close` | Lifecycle (`open`/`bind`/`close` aliases) |
| `ops` / `probe` | Unified commands + snapshot |
| `signals` / `connect` / `emit` | SignalBus |
| `host` / `setHostBackend` | Host SPI |
| `theme` / `setTheme` | Default theme (project present when name is a profile id) |
| `registerUiAction` / `getUiAction` / `uiActionIds` / `nodeState` | Declarative connections actions + node FSM (39–42) |
| `projectPresent` / `setProjectPresent` / `presentProfile` / `setPresentProfile` | Project fallback present; set hot-restyles project-fallback C1 |
| `registerPresentProfile` / `getPresentProfile` / `presentProfileIds` / `presentProfiles()` | Global skin catalog (register ≠ apply) |
| `registerPresentPack` / `activatePresentPack` / `presentPackIds` / `activePresentPack` | UI pack catalog (LML/JSON modules) |
| `enabledPresentIds` / `setEnabledPresentProfiles` / `selectPresentMatching` / `modifyPresentsMatching` | Enable set + regex select/modify |
| `presentChrome` / `resolvePresent` | Project chrome / window resolve |
| `bindSurfacePresent` / `unbindSurfacePresent` / `resolveSurfacePresent` / `surfaceChrome` | C2 surface → PresentProfile |
| `restyleOpenPresent` | Force present refresh + Stage reattach for project-fallback windows |
| `component` | C1 / native / full-present `UiComponent` |
| `projection` / `publishFrame` | Present projection + frame publish |
| `assets` | HostAssets |
| `render` | RenderHost |
| `nodes` / `c1Nodes` / `animation` / `skeletons` | Presentation graph SPIs |
| `entities` / `entityPresent` | EntityPresent slots |
| `uiRoot` / `widgets` / `tree` / `tick` / `layoutRoot` | C1 runtime |

Also stable: `WindowDef`, `WindowClass`, `WindowHandle`, `UiOps`, `UiOpResult`, `UiProbe`.

### Core / context / assets

- `artframework.presentation`: `NodeTree`, `Node`, `PresentationFrame`, `PresentationWorld`
- `artframework.core`: `SignalHub`, `SignalBus`, `Theme`, `HostBackend`,
  `HostCapabilities`, `UiComponent`
- `artframework.ecs`: `EntityId`, `PresentationWorld` for deterministic ART-owned presentation
  state. Scope/id semantics are stable; internal component classes remain package/domain owned.
- `artframework.context`: `ContextFrame`, `CardRef`, `CardView`, strong views (`ControlsView`,
  `MapView`, `EventView`, `SelectView`, reward/rest/shop/treasure/top-panel/intent views),
  `PresentSurfaces`, `PresentProjection`, `SurfaceIds`, `IntentNames`, `UiIntent`,
  `FakeSignalBackend`, `EntitySnapshot` (via c2)
- `artframework.assets`: `HostAssets`, `AssetPack`, `ResourceIds`, resolve results
- `artframework.skeleton`: `SkeletonProvider`, `SkeletonProviders`, `SkeletonSource`, and
  `SkeletonHandle` SPI; provider implementations are host-specific.
- `artframework.c2.NativeTemplateIds`: canonical `sts1.*` (+ legacy `sts.*` input)
- `artframework.sts1.PresentLevel`, `FullPresentMode` (policy); **not** patch/draw path classes

### Host-only / lab-only (may evolve without major version)

- `artframework.sts1.render.*`, `sts1.patch.*`, `sts1.input.Sts1IntentExecutor` bodies
- `artframework.inspect.*`, `artframework.sts1.inspect.*`, `artframework.sts1.lab.*`, console
  `art lab` / `art ui` native reflect
- SpirePatch suppress points, pan/zoom defaults

## Intents (not `UiOps.submitIntent`)

There is **no** `UiOps.submitIntent`. Consumers:

1. `ArtFramework.emit(UiSignal)` / surface `action(...)` → SignalBus
2. Backend / `IntentExecutor` handles accepted intents
3. Next `ContextFrame` hard-syncs pixels

## Compatibility Rules

- `open`, `bind`, and `close` remain aliases for the mount lifecycle for at least one minor release.
- Legacy `sts.*` IDs are accepted as input and canonicalized to `sts1.*`; probe output uses canonical IDs.
- `UiProbe.SCHEMA_VERSION` stays **1** for additive field groups; bump only with a documented migration.
- `UiOpResult.Status` is additive only within a minor release.
- Extension packages (`component`, `c1.layout`, `render` internals) may evolve with release notes.

## Probe schema v1 (field groups)

| Root key | Content |
|----------|---------|
| `schemaVersion` | Always `1` |
| `modId` | `artframework` |
| `windows` / `templates` / `map` / `endTurn` | C1/C2 thin templates |
| `entities` | `slotCount` + `slots[]` |
| `components` / `present` | UiComponent slices |
| `projection` | frame/scene/card counts + event/select/reward fields |
| `backend` | `fullPresent`, `renderPlan`, `*Draw` (+ `capability`), `input` (+ room states), `safety`, … |
| `assets` / `host` / `render` / `theme` | HostAssets + host SPI |

YAML art-verify `schemaVersion` is a **scenario** contract (also currently `1`), separate from
`ART_PROBE` schema.

## Release Gate

```bash
./scripts/release-gate.sh
```

Equivalent steps:

1. `./scripts/with-art-env.sh test`
2. `cd tools/art-verify && python3 -m unittest discover -s tests -v`
3. Build jar; assert `gradle.properties` == jar manifest == `ModTheSpire.json` version
4. `./scripts/verify-consumer-fixture.sh`
5. (Optional) Deploy ArtFramework before D1 UI verification

Version policy: [`versioning.md`](./versioning.md). Consumer guide: [`consumer.md`](./consumer.md).

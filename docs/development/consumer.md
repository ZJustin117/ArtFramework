# Consuming ArtFramework.jar

ArtFramework is a **separate ModTheSpire mod** (`modid`: `artframework`). Consumers compile against the jar and load it **beside** their own jar — do **not** shade ArtFramework classes into the consumer fat jar (would duplicate `ArtFrameworkMod`).

Start with [`api-overview.md`](api-overview.md) for the distinction between the Consumer Facade,
Stable Typed Domain API, Extension SPI, and Host/Lab API. This document focuses on consumer setup
and concrete integration examples.

## Build artifact

```bash
# from ArtFramework repo
./scripts/with-art-env.sh jar
# → build/libs/ArtFramework.jar

# optional install
ART_INSTALL_DIR=/path/to/mods_library ./scripts/publish-art-local.sh
# or exact path:
ART_CONSUMER_JAR=/path/to/cache/ArtFramework.jar ./scripts/publish-art-local.sh
```

Version: `gradle.properties` → `artframework.version` (also written into `ModTheSpire.json` + jar manifest).

## Gradle (consumer)

```kotlin
// -PartFrameworkJar=/absolute/path/to/ArtFramework.jar
val artFrameworkJar = requiredJar("artFrameworkJar")

dependencies {
    compileOnly(files(stsJar, baseModJar, modTheSpireJar, artFrameworkJar))
    // NOT implementation(files(artFrameworkJar)) — keep ArtFramework as its own MTS mod
}
```

Use a consumer-local Gradle property or environment variable for the built `ArtFramework.jar` path; this repository does not prescribe downstream names.

## ModTheSpire.json (consumer)

```json
"dependencies": ["basemod", "artframework"]
```

Deploy **both** `ArtFramework.jar` and `Consumer.jar` into the mods library. Load order: BaseMod → ArtFramework → consumer.

## Public API surface (stable for consumers)

The default entry point is the facade:

```java
ArtFramework.mount("mod.window");
ArtFramework.ops().invoke("mod.window", "confirm");
ArtFramework.unmount("mod.window");
```

Use the typed APIs below when a consumer needs frame-level or extension-level control. Do not treat
host draw paths, patch classes, scene2d actors, or native objects as stable consumer APIs.

| Entry | Role |
|-------|------|
| `artframework.api.ArtFramework` | `register` / **`mount` / `unmount`** / `open` / `bind` / `close` / `component` / `theme` / `host` / `entities()` / `ops()` / `probe()` / **`signals()` / `connect()` / `dispatch()` / `emit()`** / **`assets()`** / **`projection()`** / **`registerUiAction` / `nodeState`** / **`registerPresentProfile` / `presentProfiles()` / `setProjectPresent` / `bindSurfacePresent`** |
| `artframework.api.UiOps` / `UiOpResult` / `UiProbe` | Unified commands + snapshot; **`invoke(componentId, action, …)`** for NativeControl / full-present surfaces; **`playHandCardRef`** |
| `artframework.api.WindowDef` / `WindowClass` / `WindowHandle` | Registration + handles |
| `artframework.presentation.*` | `PresentationContext` / `PresentationRuntime` / `PresentationFrame` |
| `artframework.core.*` | `SignalHub` / `SignalBus` / `SignalGroup` / `SignalGroups` / `UiSignal` / `SignalDispatchResult` / `Theme` / `PresentProfile` / `HostBackend` / `UiComponent` |
| `artframework.ecs.*` | `EntityId` / `PresentationWorld` for ART-owned presentation state |
| `artframework.context.*` | **Signal and frame model:** `ContextSignals` / `ContextFrame` / `PresentProjections` / `PresentProjection` / `FakeSignalBackend` / `SignalBackend` / `CardRef` / `PresentSurfaces` |
| `artframework.assets.*` | **HostAssets** ResourceId / packs / resolve / `FakeHostAssets` |
| `artframework.c1.SyntheticRuntime` | C1 layout open; Stage via `StageHost` |
| `artframework.c2.NativeTemplateRuntime` | `map` / `event` / `selectGrid` / `selectHand` / `endTurn` / `entities` |
| `artframework.c2.NativeComponents` / `NativeTemplateIds` | C2 as `UiComponent`; `sts.map`, `sts.event`, `sts.select.*`, `sts.endturn` |
| `artframework.c2.EntityPresent` | attach / sync / layout / detach |
| `artframework.c2.hooks.NativeUiHooks` | Pure patch entry (dispatch gates) |
| `artframework.ops.NativeOpsBackend` / `StsNativeOps` | Engine gestures after ALLOW |
| `artframework.component.*` | Composition AST + `WidgetSession` / layout size flags |
| `ArtFramework.uiRoot` / `widgets` | Expanded declaration + ECS-derived control view |
| `UiOps` C1 | `clickButton` / `setSlider` / `clickHitArea` / `setText` / `setChecked` / `setProgress` / … |
| `ArtFramework.render()` / `artframework.render.*` | Effects + full-frame + blur/glass |
| Layout types | `window`/`row`/`col`/… + `textfield`/`checkbox`/`progress`/`scroll`/`center`/`margin`/`glass` |

Design: [`docs/design/godot-aligned-ui.md`](../design/godot-aligned-ui.md),
[`docs/design/backend-context.md`](../design/backend-context.md),
[`docs/design/c2-full-present.md`](../design/c2-full-present.md),
[`docs/design/host-assets.md`](../design/host-assets.md).  
`open`/`bind` remain aliases of `mount` paths (compatibility).

### Node connections / UiActions / FSM (39–42)

Declarative wiring (no scripts in LML). Full bus names + regex; actions are registered ids.

```java
ArtFramework.registerUiAction("mod.hit", ctx -> { /* ... */ return true; });
// Layout connections: { "match_pattern": "ui/ok/.*", "action": "mod.hit" }
// Or builtins: play, pause, stop, resume, set_prop, pulse_effect, emit, close_window
ArtFramework.nodeState(windowId, nodeId); // optional states{} FSM
```

See [`docs/design/node-signal-runtime.md`](../design/node-signal-runtime.md).

### PresentProfile / PresentPack (37–38)

```java
// Skin
ArtFramework.registerPresentProfile("mod.coolwave", theme, "mod.cool_pack");
// UI module (LML/JSON) — effects live in the layout files
ArtFramework.registerPresentPackClasspath("present-packs/mod_cool/pack.json");
// or PresentPack.builder("mod.cool_pack").template(...).window(...).build()

ArtFramework.setProjectPresent("mod.coolwave"); // restyle + activate pack
ArtFramework.selectPresentMatching("^mod\\.");  // first enabled match
ArtFramework.modifyPresentsMatching("^mod\\.", true); // regex enable
```

Probe: `presentProfiles`, `presentPacks`, `enabledPresents`.  
Design: [`docs/design/present-profile.md`](../design/present-profile.md).

### Milestone 15–20 consumer notes (signals + full present)

- Backend installs ordinary signal listeners; publish authority frames as `context/frame/updated` and use `ArtFramework.dispatch(...)` when the result must be observed. `emit(...)` remains the compatibility result-bearing alias.
- Prefer `ACTION` signals or surface `action` over native hitbox callbacks. An interceptor may transform or reject a signal, but only an endpoint committed frame is authoritative.
- Full-present surfaces: `sts1.combat.hand`, `sts1.combat.card_slots`, `sts1.combat.controls`,
  `sts1.combat.proceed`, `sts1.combat.energy`, `sts1.combat.intents`, `sts1.map`, `sts1.skeleton`,
  `sts1.combat.surface` (`mount_combat`), `sts1.event`, `sts1.select.grid`, `sts1.select.hand`,
  `sts1.reward.combat|card|boss_relic`, `sts1.rest`, `sts1.treasure`, `sts1.shop`, `sts1.top_panel`.
- Legacy `sts.*` / `sts1.endturn` **NativeComponents** remain; end-turn full-present is `sts1.combat.controls` (not an alias that steals `sts1.endturn`).
- Card identity: use **`CardRef.instanceId`** (multi-instance safe); `playHandCard(cardId)` still resolves first hand match when hand surface is mounted.
- EntityPresent chrome: `ArtFramework.entities()` + `EntitySnapshot` / `EntityDrawPath`; combat HAND FULL owns in-combat cards.
- Assets: register packs on `ArtFramework.assets()`; Theme icons/style resolve via HostAssets.
- Pack release gate: `./scripts/verify-consumer-fixture.sh` / `./scripts/release-gate.sh`.

### Milestone 16–28 freeze (STS1 host full-present)

**Stable for consumers (do not break without major version):**

| Surface | API |
|---------|-----|
| Policy | `PresentLevel` OFF\|OBSERVE\|FULL; mount alone never suppresses native |
| Frames | `ContextFrame` + controls/map/event/select/reward/rest/treasure/shop/top/intents views + `CardRef` / `sceneEpoch` |
| Ops | `UiOps.invoke` / surface `action` / `playHandCardRef` / `ArtFramework.emit` (no `UiOps.submitIntent`) |
| Probe | `backend.fullPresent`, `renderPlan`, `*Draw`, `entityDraw`, `input`, `safety` |
| Console lab | `art present combat\|map\|skeleton\|event\|select\|reward\|rest\|treasure\|shop\|top\|intents\|proceed\|energy …`, panic/clear-panic |

**Host-only (may evolve):** `Sts1IntentExecutor` gesture bodies, SpriteBatch label fallbacks, SpirePatch suppress points, pan/zoom defaults.

**Panic:** `PresentSafety.panic` forces all levels OFF and unmounts present surfaces; native UI is restored.

Console (in-game): full reference [`console-commands.md`](./console-commands.md) — `art probe` | `art ui …` | `art frame` | `art present …` | `art assets …` | `art op …` | `art gate …` | `art lab …`

**Out of ArtFramework:** multiplayer protocol, party election, and combat authority.

## Smoke

After wiring, consumer JUnit should still pass with `compileOnly` ArtFramework on the test classpath (same `-PartFrameworkJar`). Device deploy must push **both** jars.

This repository also ships a minimal compile-only consumer gate:

```bash
./scripts/verify-consumer-fixture.sh
```

It compiles `tools/consumer-fixture/src/ConsumerFixture.java` against the freshly built
`ArtFramework.jar`, ensuring the documented stable API remains linkable by an external consumer.

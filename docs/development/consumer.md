# Consuming ArtFramework.jar

ArtFramework is a **separate ModTheSpire mod** (`modid`: `artframework`). Consumers (e.g. CrossSpire) compile against the jar and load it **beside** their own jar — do **not** shade ArtFramework classes into the consumer fat jar (would duplicate `ArtFrameworkMod`).

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

Env key name used by CrossSpire: **`CROSSSPIRE_ART_JAR`** (path to built `ArtFramework.jar`).

## ModTheSpire.json (consumer)

```json
"dependencies": ["basemod", "artframework"]
```

Deploy **both** `ArtFramework.jar` and `Consumer.jar` into the mods library. Load order: BaseMod → ArtFramework → consumer.

## Public API surface (stable for consumers)

| Entry | Role |
|-------|------|
| `artframework.api.ArtFramework` | `register` / **`mount` / `unmount`** / `open` / `bind` / `close` / `tree` / `component` / `theme` / `host` / `entities()` / `ops()` / `probe()` / **`signals()` / `connect()` / `emit()`** / **`assets()`** / **`projection()`** |
| `artframework.api.UiOps` / `UiOpResult` / `UiProbe` | Unified commands + snapshot; **`invoke(componentId, action, …)`** for NativeControl / full-present surfaces; **`playHandCardRef`** |
| `artframework.api.WindowDef` / `WindowClass` / `WindowHandle` | Registration + handles |
| `artframework.core.*` | `UiTree` / `UiInstance` / `SignalHub` / `Theme` / `HostBackend` / `UiComponent` |
| `artframework.context.*` | **Signal model:** `ContextSignals` / `ContextFrame` / `PresentProjections` / `FakeSignalBackend` / `SignalBackend` / `CardRef` / `PresentSurfaces` |
| `artframework.assets.*` | **HostAssets** ResourceId / packs / resolve / `FakeHostAssets` |
| `artframework.c1.SyntheticRuntime` | C1 layout open; Stage via `StageHost` |
| `artframework.c2.NativeTemplateRuntime` | `map` / `event` / `selectGrid` / `selectHand` / `endTurn` / `entities` |
| `artframework.c2.NativeComponents` / `NativeTemplateIds` | C2 as `UiComponent`; `sts.map`, `sts.event`, `sts.select.*`, `sts.endturn` |
| `artframework.c2.EntityPresent` | attach / sync / layout / detach |
| `artframework.c2.hooks.NativeUiHooks` | Pure patch entry (dispatch gates) |
| `artframework.ops.NativeOpsBackend` / `StsNativeOps` | Engine gestures after ALLOW |
| `artframework.component.*` | Composition AST + `WidgetSession` / layout size flags |
| `ArtFramework.uiRoot` / `widgets` / `tree` | Expanded tree + control state + instance tree |
| `UiOps` C1 | `clickButton` / `setSlider` / `clickHitArea` / `setText` / `setChecked` / `setProgress` / … |
| `ArtFramework.render()` / `artframework.render.*` | Effects + full-frame + blur/glass |
| Layout types | `window`/`row`/`col`/… + `textfield`/`checkbox`/`progress`/`scroll`/`center`/`margin`/`glass` |

Design: [`docs/design/godot-aligned-ui.md`](../design/godot-aligned-ui.md),
[`docs/design/backend-context.md`](../design/backend-context.md),
[`docs/design/c2-full-present.md`](../design/c2-full-present.md),
[`docs/design/host-assets.md`](../design/host-assets.md).  
`open`/`bind` remain aliases of `mount` paths (compatibility).

### Milestone 15–20 consumer notes (signals + full present)

- Backend installs ordinary signal listeners; publish authority frames as `context/frame/updated` and use `ArtFramework.emit(...)` for all interactions.
- Prefer `ACTION` signals or surface `action` over native hitbox callbacks. An interceptor may transform or reject a signal, but only an endpoint committed frame is authoritative.
- Full-present surfaces: `sts1.combat.hand`, `sts1.combat.card_slots`, `sts1.combat.controls`, `sts1.map`, `sts1.skeleton`, `sts1.combat.surface` (`mount_combat`).
- Legacy `sts.*` / `sts1.endturn` **NativeComponents** remain; end-turn full-present is `sts1.combat.controls` (not an alias that steals `sts1.endturn`).
- Card identity: use **`CardRef.instanceId`** (multi-instance safe); `playHandCard(cardId)` still resolves first hand match when hand surface is mounted.
- Assets: register packs on `ArtFramework.assets()`; Theme icons/style resolve via HostAssets.
- Pack release gate: `./scripts/verify-consumer-fixture.sh` compiles `tools/consumer-fixture`
  which registers a CARD pack and asserts resolve winner `packId` (milestone 19.6).

### Milestone 16 freeze (STS1 host full-present)

**Stable for consumers (do not break without major version):**

| Surface | API |
|---------|-----|
| Policy | `PresentLevel` OFF\|OBSERVE\|FULL; mount alone never suppresses native |
| Frames | `ContextFrame` + `ControlsView` / `MapView` / `CardRef` / `sceneEpoch` |
| Ops | `UiOps.invoke` / `submitIntent` / `playHandCardRef` |
| Probe | `backend.fullPresent`, `renderPlan`, `handDraw`, `controlsDraw`, `mapDraw`, `input`, `safety` |
| Console lab | `art present combat\|map\|skeleton on\|off\|observe`, `art present panic\|clear-panic` |

**Host-only (may evolve):** `Sts1IntentExecutor` gesture bodies, SpriteBatch label fallbacks, SpirePatch suppress points, pan/zoom defaults.

**Panic:** `PresentSafety.panic` forces all levels OFF and unmounts present surfaces — consumers should treat as “native UI restored”.

Console (in-game): full reference [`console-commands.md`](./console-commands.md) — `art probe` | `art ui …` | `art frame` | `art present …` | `art assets …` | `art op …` | `art gate …` | `art lab …`

**Out of ArtFramework:** multiplayer protocol, party election, combat authority (consumer owns these).

## Smoke

After wiring, consumer JUnit should still pass with `compileOnly` ArtFramework on the test classpath (same `-PartFrameworkJar`). Device deploy must push **both** jars.

This repository also ships a minimal compile-only consumer gate:

```bash
./scripts/verify-consumer-fixture.sh
```

It compiles `tools/consumer-fixture/src/ConsumerFixture.java` against the freshly built
`ArtFramework.jar`, ensuring the documented stable API remains linkable by an external consumer.

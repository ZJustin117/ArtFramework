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
| `artframework.api.ArtFramework` | `register` / **`mount` / `unmount`** / `open` / `bind` / `close` / `tree` / `component` / `theme` / `host` / `entities()` / `ops()` / `probe()` |
| `artframework.api.UiOps` / `UiOpResult` / `UiProbe` | Unified commands + snapshot; **`invoke(componentId, action, …)`** for NativeControl |
| `artframework.api.WindowDef` / `WindowClass` / `WindowHandle` | Registration + handles |
| `artframework.core.*` | `UiTree` / `UiInstance` / `SignalHub` / `Theme` / `HostBackend` / `UiComponent` |
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

Design: [`docs/design/godot-aligned-ui.md`](../design/godot-aligned-ui.md).  
`open`/`bind` remain aliases of `mount` paths (compatibility).

Console (in-game): `art probe` | `art op select|confirm|map|event|endturn|play|button …`

**Out of ArtFramework:** multiplayer protocol, party election, combat authority (consumer owns these).

## Smoke

After wiring, consumer JUnit should still pass with `compileOnly` ArtFramework on the test classpath (same `-PartFrameworkJar`). Device deploy must push **both** jars.

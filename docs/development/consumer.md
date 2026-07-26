# Consuming SpireUI.jar

SpireUI is a **separate ModTheSpire mod** (`modid`: `spireui`). Consumers (e.g. CrossSpire) compile against the jar and load it **beside** their own jar — do **not** shade SpireUI classes into the consumer fat jar (would duplicate `SpireUiMod`).

## Build artifact

```bash
# from SpireUI repo
./scripts/with-env.sh jar
# → build/libs/SpireUI.jar

# optional install
SPIREUI_INSTALL_DIR=/path/to/mods_library ./scripts/publish-local.sh
# or exact path:
SPIREUI_CONSUMER_JAR=/path/to/cache/SpireUI.jar ./scripts/publish-local.sh
```

Version: `gradle.properties` → `spireui.version` (also written into `ModTheSpire.json` + jar manifest).

## Gradle (consumer)

```kotlin
// -PspireUiJar=/absolute/path/to/SpireUI.jar
val spireUiJar = requiredJar("spireUiJar")

dependencies {
    compileOnly(files(stsJar, baseModJar, modTheSpireJar, spireUiJar))
    // NOT implementation(files(spireUiJar)) — keep SpireUI as its own MTS mod
}
```

Env key name used by CrossSpire: **`CROSSSPIRE_SPIREUI_JAR`** (path to built `SpireUI.jar`).

## ModTheSpire.json (consumer)

```json
"dependencies": ["basemod", "spireui"]
```

Deploy **both** `SpireUI.jar` and `Consumer.jar` into the mods library. Load order: BaseMod → SpireUI → consumer.

## Public API surface (stable for consumers)

| Entry | Role |
|-------|------|
| `spireui.api.SpireUI` | `register` / **`mount` / `unmount`** / `open` / `bind` / `close` / `tree` / `component` / `theme` / `host` / `entities()` / `ops()` / `probe()` |
| `spireui.api.UiOps` / `UiOpResult` / `UiProbe` | Unified commands + snapshot; **`invoke(componentId, action, …)`** for NativeControl |
| `spireui.api.WindowDef` / `WindowClass` / `WindowHandle` | Registration + handles |
| `spireui.core.*` | `UiTree` / `UiInstance` / `SignalHub` / `Theme` / `HostBackend` / `UiComponent` |
| `spireui.c1.SyntheticRuntime` | C1 layout open; Stage via `StageHost` |
| `spireui.c2.NativeTemplateRuntime` | `map` / `event` / `selectGrid` / `selectHand` / `endTurn` / `entities` |
| `spireui.c2.NativeComponents` / `NativeTemplateIds` | C2 as `UiComponent`; `sts.map`, `sts.event`, `sts.select.*`, `sts.endturn` |
| `spireui.c2.EntityPresent` | attach / sync / layout / detach |
| `spireui.c2.hooks.NativeUiHooks` | Pure patch entry (dispatch gates) |
| `spireui.ops.NativeOpsBackend` / `StsNativeOps` | Engine gestures after ALLOW |
| `spireui.component.*` | Composition AST + `WidgetSession` / layout size flags |
| `SpireUI.uiRoot` / `widgets` / `tree` | Expanded tree + control state + instance tree |
| `UiOps` C1 | `clickButton` / `setSlider` / `clickHitArea` / `setText` / `setChecked` / `setProgress` / … |
| `SpireUI.render()` / `spireui.render.*` | Effects + full-frame + blur/glass |
| Layout types | `window`/`row`/`col`/… + `textfield`/`checkbox`/`progress`/`scroll`/`center`/`margin`/`glass` |

Design: [`docs/design/godot-aligned-ui.md`](../design/godot-aligned-ui.md).  
`open`/`bind` remain aliases of `mount` paths (compatibility).

Console (in-game): `spireui probe` | `spireui op select|confirm|map|event|endturn|play|button …`

**Out of SpireUI:** multiplayer protocol, party election, combat authority (consumer owns these).

## Smoke

After wiring, consumer JUnit should still pass with `compileOnly` SpireUI on the test classpath (same `-PspireUiJar`). Device deploy must push **both** jars.

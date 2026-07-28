# ArtFramework dual-track design

## Two jobs

1. **C1 Synthetic windows** — build UI with **scene2d.ui** (window manager, StsSkin, widgets).
2. **C2 Native template windows** — wrap STS native UI as **callable templates** (intercept/decorate), plus **entity presenters** (player/card/relic/monster draw helpers for co-op consumers).

Both share one facade (`artframework.api.ArtFramework`) and registration format; runtimes differ.

**API target:** one consumer surface (mount / tree / signals / ops / probe); C2 as encapsulated **NativeControl** / **Native Surface** components; host remains dual. See [`godot-aligned-ui.md`](./godot-aligned-ui.md).

**Next evolution (milestone 15):** pluggable Backend + context frames; C2 **full present** (ART draws, hard-sync, intents replace native UI callbacks); HostAssets unified packs. See [`backend-context.md`](./backend-context.md), [`c2-full-present.md`](./c2-full-present.md), [`host-assets.md`](./host-assets.md). Thin SpirePatch intercept remains a **migration bridge**, not the end state.

```
Caller
  → ArtFramework.register / open / bind / close
       ├─ C1 SyntheticRuntime   (scene2d)
       └─ C2 NativeTemplateRuntime + EntityPresent
```

## WindowClass

| Class | Runtime | Examples |
|-------|---------|----------|
| `SYNTHETIC` | scene2d Stage / Window | Lobby, vote, queue, settings, fallback event modal |
| `NATIVE_TEMPLATE` | Patched STS screens + decorators | `sts.map`, `sts.event`, grid/hand select, end turn; entity present slots |

- **C1 `open`**: create synthetic window (layout + optional Stage).
- **C2 `bind`**: activate native template session (map/event/…). `open` on `NATIVE_TEMPLATE` delegates to `bind`.

## C2 map template (`sts.map`)

Logic API (no SpirePatch in v1 unit gate):

| Type | Role |
|------|------|
| `MapTemplate` | Active session: interceptors + pins |
| `MapNodeInterceptor` | `ALLOW` / `BLOCK` on node click (chain, first BLOCK wins) |
| `MapPin` / `MapPinDecorator` | Decorative markers; decorators notified on change |
| `NativeTemplateRuntime.bind` | Activates map when `WindowDef.resource` is `sts.map` |

Engine integration (future): patch `DungeonMapScreen` / `MapRoomNode` to call `MapTemplate.dispatchNodeClick` and draw pins — design before adding `@SpirePatch`.

## C2 event / select / end-turn

| Resource | Session | Hooks |
|----------|---------|-------|
| `sts.event` | `EventTemplate` | `EventOptionInterceptor` on option index |
| `sts.select.grid` | `SelectTemplate` (GRID) | card pick + confirm intercept |
| `sts.select.hand` | `SelectTemplate` (HAND) | card pick + confirm intercept |
| `sts.endturn` | `EndTurnTemplate` | press intercept + `setButtonEnabled` hint |

Shared: `GateResult.ALLOW|BLOCK`. Bind via `ArtFramework.bind(id)` / `NativeTemplateRuntime`. STS patches later (`AbstractEvent`, `GridCardSelectScreen`, `HandCardSelectScreen`, `EndTurnButton`).

## C2 EntityPresent

Slot lifecycle for co-op entity chrome (no combat authority):

| API | Role |
|-----|------|
| `EntityKind` | `PLAYER` / `CARD` / `RELIC` / `MONSTER` |
| `EntitySlot` | id, kind, refId, opaque snapshot, layout x/y/scale |
| `EntityPresent` | `attach` / `sync` / `layout` / `detach` / query |
| `DefaultEntityPresent` | In-memory impl + `EntityPresentListener` |
| `ArtFramework.entities()` | Facade → `NativeTemplateRuntime.entities()` |

Render adapters (`AbstractCard.render*`, player/monster draw) are **out of this slice**.

## Not ArtFramework

- Multiplayer protocol, party election, combat phase authority
- Serializing entire STS screens into scene2d trees as a production path
- Replacing `AbstractPlayer` / full hand-play pipeline with scene2d

## STS assets

Prefer `FontHelper`, `ImageMaster`, `ScrollBar`, `AbstractCard.render*` via adapters — not a browser/CSS stack.

## C1 layout DSL

Synthetic windows use a **minimal JSON layout** (not gdx-lml / VisUI). `WindowDef.resource` is a classpath path (e.g. `layouts/demo.json`).

Supported node `type` values (v1 production open path):

| type | Fields |
|------|--------|
| `window` | `id?`, `title` (required), `width?`, `height?`, `children?` |
| `label` | `text?` |
| `button` | `id?`, `text?` |

- Parser: `artframework.c1.layout.LayoutLoader` (pure Java JSON subset → immutable `LayoutNode`).
- Open path: `ArtFramework.open` → `SyntheticRuntime.open` → `WindowManager` + optional `StageBackend.attach`.
- Stage: `StageHost` (BaseMod PostInitialize / PostUpdate / PostRender; modal input like ModPanel). No SpirePatch.
- Skin: `StsSkin` from `FontHelper` / `ImageMaster` / `Settings`; actors via `LayoutActors`.
- Unit tests: parse + open/close + `FakeStageBackend`; no GL for default gate.

### Composition framework (extensible tree)

Composable Nest / Slot / Attach / Bind model lives in `artframework.component` (`UiNode`, `LayoutEngine`, `ComponentRegistry`). See [`component-composition.md`](./component-composition.md). Production Stage inflates composition via `ComponentActors` / `attachComposition`; legacy v1 `LayoutNode` bridge remains for simple window/label/button trees. Runtime instance tree + signals: [`godot-aligned-ui.md`](./godot-aligned-ui.md).

## Roadmap (high level)

0. Scaffold (this repo) — registry + tests  
1a. C1 logic runtime + layout DSL + demo + open dispatch  
1b. C1 Stage host + StsSkin + optional on-device  
2. C2 map template intercept + pin decorator hooks (logic done; SpirePatch later)  

3. C2 event/select/end-turn templates (logic done; SpirePatch later)  

4. EntityPresent lifecycle API (logic done; STS draw later)  

5. Consumer (CrossSpire) depends on `ArtFramework.jar` (compileOnly + MTS `artframework`; see consumer.md)  

6. **UiOps / UiProbe** — unified C1+C2 command + query surface (see [`ui-ops-probe.md`](./ui-ops-probe.md))  

7. C2 SpirePatch thin hooks call `dispatch*` only; consumers attach interceptors  

8. UI-layer verification — pure JUnit + `tools/art-verify` (fixture offline; optional D1 device)

9. Lab intercept + GateLab + deploy hardening — done (see task.md)

10. Component composition + RenderHost / glass FX — done (see task.md)

11. **Godot-aligned UI core** — UiTree/signals, Theme, C1 widgets, C2 as NativeControl, host SPI ([`godot-aligned-ui.md`](./godot-aligned-ui.md), task 11.x) — done  

12–14. Presentation graph, runtime hardening, action convergence — done (see task.md)  

15. **Backend / full C2 present / HostAssets** — context frames, full present surfaces, pack library ([`backend-context.md`](./backend-context.md), [`c2-full-present.md`](./c2-full-present.md), [`host-assets.md`](./host-assets.md), task 15.x)

## Unified ops / probe (track-agnostic)

Above C1/C2 runtimes, consumers and tests should prefer:

```
ArtFramework.ops()    → imperative UI (select, confirm, map click, event option, end turn, C1 button, hand play gesture)
ArtFramework.probe()  → UI snapshot (open windows, bound templates, pins, select summary)
```

- **Gesture only** for combat hand play — no queue / protocol / party authority in this repo.
- Map **decorative** pins stay in `MapTemplate`; co-op pin consensus stays in the consumer.
- Device serials for optional UI smoke: `ART_D1_SERIAL` (same physical device as CrossSpire D1 when set).

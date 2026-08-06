# Godot-aligned UI core (ArtFramework)

Design contract for a **Godot-shaped** public API: one consumer surface, full C1 synthetic UI, C2 native STS screens as **encapsulated components**, shared structure for STS1 / future STS2 hosts.

Complements [`art-framework.md`](./art-framework.md) (presentation graph / Host SPI / LML), [`dual-track.md`](./dual-track.md), [`component-composition.md`](./component-composition.md), [`ui-ops-probe.md`](./ui-ops-probe.md), [`backend-context.md`](./backend-context.md), [`c2-full-present.md`](./c2-full-present.md), [`host-assets.md`](./host-assets.md). Milestones **11–43** are shipped in [`docs/task.md`](../task.md).

## Purpose

1. Align **mental model and API shape** with Godot Control / Node / Container / Theme / signals — not a port of the engine, GDScript, or editor.
2. Make **C1** the primary expressive surface for new mod UI (lobby, settings, modals, future self-drawn chrome).
3. Encapsulate **C2** as replaceable **NativeControl** components (same mount / signal / ops / probe paths as C1).
4. Keep **declaration + core logic** host-agnostic so tower-1 (STS1) and tower-2 (future host) share structure; only `HostBackend` differs.

## Non-goals

- Multiplayer protocol, party election, combat phase authority.
- Cloning full Godot Control set, accessibility stack, SubViewport as a general feature, or anchor editor presets.
- Replacing the full STS hand-play / `AbstractPlayer` pipeline with scene2d as the default production path.
- Executable logic inside layout JSON (behavior stays Java Bind / signals).

## Official Godot sources (stable)

| Topic | URL |
|-------|-----|
| UI index | https://docs.godotengine.org/en/stable/tutorials/ui/index.html |
| Control | https://docs.godotengine.org/en/stable/classes/class_control.html |
| Node | https://docs.godotengine.org/en/stable/classes/class_node.html |
| Containers | https://docs.godotengine.org/en/stable/tutorials/ui/gui_containers.html |
| Size and anchors | https://docs.godotengine.org/en/stable/tutorials/ui/size_and_anchors.html |
| GUI skinning | https://docs.godotengine.org/en/stable/tutorials/ui/gui_skinning.html |
| Custom GUI controls | https://docs.godotengine.org/en/stable/tutorials/ui/custom_gui_controls.html |

## Term map (Godot → ArtFramework)

| Godot | ArtFramework target | Notes |
|-------|----------------|-------|
| Node / SceneTree | `UiInstance` + `UiTree` (per mount) | No global engine tree; one tree per open/mounted root |
| Control | Control contract on `UiInstance` (bounds, minSize, focus, signals) | |
| Container | Nest containers (`row`/`col`/…); container-first layout | Children do not self-position under containers |
| Size flags / stretch ratio | Extended `LayoutSpec` | Today: `grow`; target: flags + ratio |
| Anchors / offsets | Optional root / non-container presets | Complex UI prefers containers (Godot guidance) |
| Theme | `Theme` + cascade; default `StsTheme` | `StsSkin` becomes scene2d realization of default theme |
| signal / connect / emit | `SignalHub` on tree/instance | `UiOps.onButton` etc. become sugar |
| PackedScene instance | `ref` + `ComponentRegistry` + slots | Already present; elevate as ComponentDef |
| `_get_minimum_size` | leaf/container preferred min size | Feeds pure `LayoutEngine` |
| `_gui_input` | host input → signals / native actions | Pure tests own state machines without GL |
| Custom Control | `LeafFactory` / registered leaf types | |
| *(ArtFramework-only)* | **NativeControl** | C2 STS screens as `UiComponent` |
| *(ArtFramework-only)* | **HostBackend** | STS1 Stage/patches; future STS2 |
| *(ArtFramework-only)* | **UiOps / UiProbe** | Imperative commands + JSON snapshot |

## Architecture target

```
Caller
  → artframework.api.ArtFramework
       register | mount | unmount | tree | ops | probe | theme | render
  → artframework.core (suggested package)
       UiNode (decl) → UiInstance (live)
       LayoutEngine · SignalHub · Theme · ComponentRegistry
  → host (implementation)
       host.sts1.c1     StageHost + ComponentActors + StsSkin
       host.sts1.native sts.* NativeControl + patches + StsNativeOps
       host.sts2.*      (future SPI only)
```

| Layer | Role |
|-------|------|
| **API single track** | Consumers use one mount/tree/signal/ops/probe surface |
| **Host dual track** | C1 synthetic draw vs C2 native intercept remain implementations |
| **Dual-track today** | `WindowClass.SYNTHETIC` / `NATIVE_TEMPLATE` — keep as kind; evolve toward `ComponentKind` |

Suggested package split is implemented as the current public/API package map; avoid churn unless a new milestone explicitly changes the stability tier.

## Contracts (design level)

### Declaration (`UiNode`)

Existing immutable AST stays the declaration source (JSON layout). Evolve:

| Area | Current | Target |
|------|---------|--------|
| Nest / Slot / ref / effects | done | keep |
| `LayoutSpec` | width, height, pad, gap, grow | + minW/H, sizeFlagsH/V, stretchRatio, align |
| Theme type | — | optional `themeType` / variation |
| Behavior in JSON | none (correct) | still none; signals named only if ever declared as metadata |

### Runtime (`UiInstance` / `UiTree`)

| Concept | Role |
|---------|------|
| `UiInstance` | Live node: id, type, props overlay, parent/children, rect, minSize, focus/mouse flags |
| `UiTree` | One mount root + id index + theme + host |
| `find(path)` | Simplified NodePath (`"panel/ok"`) |
| Lifecycle | `onMount` → `onReady` (children first) → `onUnmount` |

| Godot | ArtFramework |
|-------|---------|
| enter_tree | onMount |
| ready | onReady |
| exit_tree | onUnmount |

`WidgetSession` merges into or becomes a view over instance state (avoid dual sources of truth long-term).

### Signals

| Source | Signal | Payload (conceptual) |
|--------|--------|----------------------|
| button / hitarea | `pressed` | — |
| slider | `value_changed` | float |
| textfield (new) | `text_changed` / `text_submitted` | string |
| checkbox (new) | `toggled` | bool |
| native `sts.map` | `node_clicked` | MapNodeRef |
| native `sts.event` | `option_chosen` | index, label |
| native select | `card_selected` / `confirmed` | refs |
| native endturn | `pressed` | — |

Connect/disconnect/emit are pure and testable. Existing `UiOps.onButton` / `onSlider` / `onHitArea` remain compatibility sugar.

C1 `UiTree` also exposes signal interceptors. They run in registration order before the
state change and emission; the first `BLOCK` prevents both. C2 native signals are emitted
after native interceptors allow the action and before the native backend gesture.

### Theme

Item kinds (Godot-aligned): **COLOR**, **CONSTANT**, **FONT**, **FONT_SIZE**, **ICON**, **STYLEBOX**.

Lookup order:

1. Instance local override  
2. Walk parent chain themes (type variation → type → parent types)  
  3. Project fallback theme from **ProjectPresent** / nearest present-profile node (`sts` / `lightwave`; see [`present-profile.md`](./present-profile.md))

Controls **consume** theme; theme does not draw by itself. `StsSkin.create(Theme)` realizes C1 scene2d styles from the active theme.

### UiComponent (C1 leaf + C2 native)

```
UiComponent
  id, ComponentKind (SYNTHETIC | NATIVE_HOST)
  mount / unmount
  connect(signal, handler)
  action(name, args…) → UiOpResult
  probeSlice() → map for UiProbe
```

Built-in native ids (canonical): `sts.map`, `sts.event`, `sts.select.grid`, `sts.select.hand`, `sts.endturn`.  
Interceptors / gates stay **inside** the component; patches call thin hooks only.

`EntityPresent` remains slot lifecycle for entity chrome; targets attach via RenderHost (Attach), not as free-form window children.

### HostBackend

```
attach(tree) / detach(tree) / applyLayout(tree)
```

- **STS1:** Stage inflate + input capture; native bridge for gestures/patches.  
- **STS2:** interface + design only until a real host exists.  
- Default JUnit: fake host / no GL.

### UiOps / UiProbe evolution

| Today | Target |
|-------|--------|
| Hard-coded `clickMapNode`, `chooseEventOption`, … | Delegate to `component(id).action(...)`; keep methods as sugar |
| Probe templates / windows fields | Add `components[]` slices; bump `schemaVersion` when shape changes |
| `open` / `bind` | Aliases of `mount` / activate native; compatibility ≥1 version |

Rules from [`ui-ops-probe.md`](./ui-ops-probe.md) still hold: first BLOCK wins; unbound → notBound; no protocol types.

## Layout policy (container-first)

Godot: complex UIs use **Containers**; anchors alone get hard for game tools UIs.

ArtFramework:

1. Default path: pure `LayoutEngine` on nested containers (JUnit).  
2. Root `window`: optional center / edge presets (not full four-float anchors).  
3. Convergence: **`UiInstance.rect` is layout truth**; scene2d Table must follow (document dual path only during transition).  
4. Effects never affect geometry (existing rule).

## Control and container roadmap

### Containers

| Godot | ArtFramework type | Priority |
|-------|--------------|----------|
| HBox / VBox | `row` / `col` (+ size flags) | enhance existing |
| PanelContainer | `panel` | existing |
| Z-stack | `stack` | existing |
| glass plate | `glass` | existing |
| Margin / Center | `margin` / `center` or pad helpers | P1 |
| ScrollContainer | `scroll` | P1 |
| Grid / Tabs | `grid` / `tabs` or ref templates | P2 |
| Split / Flow / AspectRatio | later | P3+ |

### Content

| Godot | ArtFramework | Priority |
|-------|---------|----------|
| Label / Button / Slider | existing | — |
| custom hit | `hitarea` | — |
| LineEdit | `textfield` | P1 |
| CheckBox | `checkbox` | P1 |
| ProgressBar | `progress` | P1 |
| RichText / Tree / ItemList | later or ref composites | P3 |

Extension: register **LeafFactory** (min size + inflate + optional pure input state) without rewriting Stage host.

## C2 encapsulation and rewrite path

```
sts.map  → NativeControl("sts.map")  → Sts1MapHost (patch + gesture)
                                      → future SyntheticMapHost (optional)
```

- Consumer always uses component id + signals/actions.  
- Swapping backend must preserve the generic component id + signal/action contract.
- Full map/event/hand redraw in C1 is **not** the default next step; optional pilot (task 11.7) only for low-risk chrome.

## Compatibility

| Surface | Policy |
|---------|--------|
| `ArtFramework.open` / `bind` / `close` | Keep; document as mount aliases |
| `UiOps` named C2 methods | Keep ≥1 version; implement via component.action |
| Probe fields | Additive + schemaVersion; do not break fixture keys without migration note |
| `WindowClass` | Keep; may mirror `ComponentKind` |
| consumer.md | Update when 11.5–11.6 land |

## Phased implementation

| Task | Deliverable | Depends |
|------|-------------|---------|
| **11.0** | This doc + cross-links + task list | — |
| **11.1** | `UiInstance` / `UiTree` / `SignalHub` (C1) + JUnit | 11.0 |
| **11.2** | `LayoutSpec` size flags + minSize + LayoutEngine | 11.1 |
| **11.3** | Theme MVP + StsTheme | 11.1 |
| **11.4** | C1 widgets: textfield, checkbox, progress, scroll (priority order) | 11.2–11.3 parallel OK |
| **11.5** | C2 NativeControl for `sts.*` + UiOps delegate + probe `components` | 11.1 |
| **11.6** | API mount/unmount, host.sts1 boundary, consumer.md | 11.5 |
| **11.7** | Optional one-surface C1 rewrite pilot | 11.6 |

Default gate remains `./scripts/with-art-env.sh test` (pure logic; no device required).

## Related

- [`dual-track.md`](./dual-track.md) — C1/C2 host tracks  
- [`backend-context.md`](./backend-context.md) — Backend / context / intents (15)  
- [`c2-full-present.md`](./c2-full-present.md) — C2 full present (15)  
- [`host-assets.md`](./host-assets.md) — HostAssets packs (15)  
- [`component-composition.md`](./component-composition.md) — Nest / Slot / Attach / Bind  
- [`component-layout-fx.md`](./component-layout-fx.md) — layout + FX phases  
- [`ui-ops-probe.md`](./ui-ops-probe.md) — commands + snapshot  
- [`docs/task.md`](../task.md) — checkbox milestones 11 / 15  
- [`docs/development/consumer.md`](../development/consumer.md) — public jar surface  

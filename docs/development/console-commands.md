# ArtFramework console commands

BaseMod DevConsole namespace **`art`**, implemented by `artframework.console.ArtCommand`.

Use in-game (BaseMod console) or via Amethyst harness / `tools/art-verify` device steps.
Harness often returns only `ok`; scrape device log for machine lines (`ART_PROBE`, `ART_UI`, …).

Design detail for inspect/emit: [`docs/design/dev-ui-console.md`](../design/dev-ui-console.md).  
UiOps contract: [`docs/design/ui-ops-probe.md`](../design/ui-ops-probe.md).  
Lab bring-up: [`android-device-lab.md`](./android-device-lab.md).

---

## Quick index

| Command | Role |
|---------|------|
| `art probe` | UI snapshot JSON (`ART_PROBE …`) |
| `art open` / `bind` / `close` | Mount synthetic / bind native / unmount |
| `art gate …` | Lab intercept BLOCK/CLEAR |
| `art op …` | UiOps gestures (C1 + C2 sugar) |
| `art ui …` | Inspect tree, emit signals, invoke, native dump/click |
| `art lab …` | Lab run nav: menu / fresh / start-run (D1) |
| `art fx …` | Full-frame effects (incl. `lightwave`) |
| `art profile` / `art theme …` | PresentProfile list/get/set (`sts`, `lightwave`) |
| `art assets …` | HostAssets packs / resolve |
| `art frame` | Sync context frame from backend |
| `art present combat …` | Combat full-present toggle |

```
art: probe | open|bind|close <id> | gate … | ui … | lab … | fx … | profile|theme … | assets … | frame | present … | op …
```

---

## Windows / templates

| Command | Description |
|---------|-------------|
| `art open <id>` | Open registered synthetic window (e.g. `demo`, `glass_demo`, `lightwave_demo`) |
| `art bind <id>` | Bind native template (`sts1.map`, `sts1.event`, `sts1.select.grid`, `sts1.select.hand`, `sts1.endturn`; legacy `sts.*` accepted) |
| `art close <id>` | Close / unbind |

Built-in demo ids are registered at PostInitialize (`ArtFrameworkMod`).

---

## Probe

| Command | Description |
|---------|-------------|
| `art probe` | One line: `ART_PROBE` + compact JSON (`schemaVersion`, windows, templates, components, present, assets, …) |

Device: if console body is only `ok`, scrape:

`/sdcard/Android/data/io.stamethyst/files/sts/latest.log` for `ART_PROBE`.

---

## Lab gate

| Command | Description |
|---------|-------------|
| `art gate` / `art gate status` | Print gate status |
| `art gate <target> block` | Install BLOCK interceptor on target |
| `art gate <target> clear` | Clear lab gate on target |

**Targets:** `map` · `event` · `endturn` · `select` · `select-grid` · `select-hand` · `all`

Binds templates as needed so interceptors attach to live sessions. Example:

```
art gate map block
art op map 1 0 monster    → BLOCKED
art gate all clear
```

---

## Ops (`art op`)

Imperative UiOps (interceptor first, then native/C1 gesture). Result: `op <kind> → OK|BLOCKED|…`.

| Command | Description |
|---------|-------------|
| `art op select grid\|hand <cardId> [index]` | Select card in grid/hand select |
| `art op confirm [grid\|hand]` | Confirm select (default grid) |
| `art op map <row> <col> [roomType]` | Map node click / gate |
| `art op event <index> [label…]` | Event option |
| `art op endturn` | End turn press |
| `art op play <cardId> [target]` | Hand play gesture |
| `art op button <windowId> <buttonId>` | C1 button |
| `art op slider <windowId> <sliderId> <value>` | C1 slider |
| `art op hitarea <windowId> <hitAreaId>` | C1 hitarea |
| `art op click <windowId> <controlId>` | Generic C1 click |

`emit` (under `art ui`) only fires SignalHub handlers; gate + engine side effects still use `art op` or `art ui invoke`.

---

## Dev UI (`art ui` / `art inspect`)

Log prefix **`ART_UI`**. Listen events: **`ART_UI_SIGNAL`**.

| Command | Description |
|---------|-------------|
| `art ui list` | Open windows + component ids (JSON after prefix) |
| `art ui tree <windowId> [depth]` | Indented tree (`id type [signals]`); default depth 6 |
| `art ui node <windowId> <id\|path>` | Node detail JSON (type, props, signals, handlerCounts, rect) |
| `art ui emit <target> <signal> [args…]` | Emit on C1 or component |
| `art ui emit <windowId> <controlId> <signal> [args…]` | C1 form without slash |
| `art ui invoke <componentId> <action> [args…]` | `UiOps.invoke` / `UiComponent.action` |
| `art ui listen <target> <signal>` | Lab handler → log each emit |
| `art ui listen <target> <signal> off` | Remove lab listener |
| `art ui native dump` | STS screen/room/endturn/select summary (soft-fail if not in run) |
| `art ui native click endturn` | Whitelist: schedule end-turn |
| `art ui native click grid.confirm` | Whitelist: grid confirm hitbox |
| `art ui native click event [index]` | Whitelist: event `buttonEffect` |

### Emit targets

| Form | Resolves to |
|------|-------------|
| `windowId/controlId` | C1 `UiTree.emit` |
| `windowId controlId` + signal | Same (three-token form) |
| Bare id (`sts1.map`, open window root, present surface) | `ArtFramework.component(…).emit` |

Undeclared signal → `UNAVAILABLE` message; console does not crash.

### Arg tokens (emit / invoke)

| Token | Parsed as |
|-------|-----------|
| (none) | empty payload |
| `true` / `false` | Boolean |
| integer / float | Number |
| `row,col` or `row,col,room` | `MapNodeRef` |
| other | String |

### Lab examples

```
art open demo
art ui list
art ui tree demo
art ui node demo close
art ui listen demo/close pressed
art ui emit demo/close pressed
# log: ART_UI_SIGNAL demo/close pressed
art ui listen demo/close pressed off

art open comp_sample
art ui tree comp_sample
art ui invoke comp_sample click_button ok

art bind sts1.endturn
art ui emit sts1.endturn pressed
art ui native dump
```

Path form also works: `art ui emit demo/close pressed`.

---

## Full-frame FX (`art fx` / `art fullframe`)

| Command | Description |
|---------|-------------|
| `art fx enable [w] [h]` | Enable full-frame (default 1920×1080) |
| `art fx disable` | Disable full-frame |
| `art fx tint [alpha]` | Bind tint (default 0.12) |
| `art fx glow [intensity]` | Bind glow (default 0.4) |
| `art fx blur [radius]` | Blur + screen capture (default radius 2.5) |
| `art fx glass [radius] [tint]` | Glass post (defaults 2.5 / 0.45) |
| `art fx capture [on\|off]` | Toggle screen capture |
| `art fx clear` | Clear full-frame effects |

---

## Assets (`art assets` / `art pack`)

| Command | Description |
|---------|-------------|
| `art assets probe` | Pack ids + full `ART_PROBE` line |
| `art assets enable <packId>` | Enable pack |
| `art assets disable <packId>` | Disable pack |
| `art assets order <ids…>` | Set pack order |
| `art assets resolve <resourceId>` | Resolve one ResourceId |

---

## Frame / present

| Command | Description |
|---------|-------------|
| `art frame` / `art sync` | Reports that frames are published by the authority endpoint; no pull operation exists |
| `art present status` | `ART_PRESENT` policy + scene/epoch/hand/map/endTurn |
| `art present combat on\|off\|observe` | Hand+controls level + mount/unmount |
| `art present map on\|off\|observe` | Map surface level + mount |
| `art present skeleton on\|off\|observe` | Skeleton surface level + mount |
| `art present event on\|off\|observe` | Event surface level + `mount_event` |
| `art present select on\|off\|observe` | Select grid+hand level + `mount_select` |
| `art present panic [reason]` | Force all OFF + unmount (native safe) |
| `art present clear-panic` | Clear panic flag (re-enable present) |

Probe extras (via `art probe`): `backend.fullPresent`, `renderPlan`, `handDraw`, `controlsDraw`,
`mapDraw`, `eventDraw`, `selectDraw`, `input`, `audio`, `skeleton`, `safety`, `controls`, `mapView`.

---

## Lab run navigation (`art lab`)

Design: [`docs/design/lab-run-nav.md`](../design/lab-run-nav.md). Lab-only; not consumer API.

Log prefix **`ART_LAB`**.

### L1 atomic

| Command | Description |
|---------|-------------|
| `art lab dump` | Mode / menuScreen / buttons / inGame / inCombat JSON |
| `art lab clear-saves` | Delete per-class SaveAndContinue + `.backUp` |
| `art lab strip-resume` | Remove RESUME/ABANDON menu buttons; ensure PLAY |
| `art lab open-char-select` | `charSelectScreen.open(false)` (skips Play panel) |
| `art lab char <id>` | Select character (class name / label; paging) |
| `art lab embark` | Character select confirm |
| `art lab seed [text]` | Optional seed; bare `seed` skips |
| `art lab menu-click <RESULT>` | MenuButton by ClickResult (`PLAY`, `ABANDON_RUN`, …) |
| `art lab abandon` | Best-effort leave run / abandon |
| `art lab abandon-confirm` | Confirm abandon popup |
| `art lab return-menu` | Death/victory return-to-menu |
| `art lab proceed` | Overlay proceed hitbox |
| `art lab tick` | One debug step toward run |

### L2 recipes

| Command | Description |
|---------|-------------|
| `art lab ensure-menu` | Arm async → main menu (postUpdate ticks) |
| `art lab ensure-fresh-menu` / `reset` | Arm async fresh menu |
| `art lab start-run [char] [seed=…]` | Arm async embark path |
| `art lab status` | Recipe runner busy/status/message JSON |

L2 **arms** a job and returns `OK … armed`. Wait wall-clock (YAML `wait_ms`) then
`art lab status` / `dump` until `recipe.busy=false` and `recipe.status=ok`.

Examples:

```
art lab dump
art lab ensure-fresh-menu
# wait ~ few seconds
art lab status
art lab start-run IRONCLAD
# wait ~ 15–20s for char select + embark
art lab status
art lab start-run char=THE_SILENT seed=ABC12
```

Device YAML: `tests/ui-scenarios/device/d1_lab_*.yaml`.

---

## Log prefixes (machine scrape)

| Prefix | Source |
|--------|--------|
| `ART_PROBE ` | `art probe` / assets probe JSON |
| `ART_UI ` | `art ui` list/tree/node/emit/invoke/listen/native |
| `ART_UI_SIGNAL ` | Lab `art ui listen` firings |
| `ART_LAB ` | `art lab` dump / step results |
| `ART_PRESENT ` | `art present combat …` |

---

## Implementation

| Piece | Location |
|-------|----------|
| Console entry | `artframework.console.ArtCommand` |
| Inspect / emit | `artframework.inspect.UiInspect` |
| Lab listeners | `artframework.inspect.UiLabListeners` |
| STS whitelist | `artframework.sts1.inspect.StsUiReflect` |
| Ops / probe API | `ArtFramework.ops()` / `ArtFramework.probe()` |

Registration: `ConsoleCommand.addCommand("art", ArtCommand.class)` in `ArtFrameworkMod`.

---

## Not in this namespace

- CrossSpire co-op / party / life (`crossspire …`) — other repo
- Arbitrary Java reflection REPL
- Dual-device connector protocol

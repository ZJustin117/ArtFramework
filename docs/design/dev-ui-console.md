# Dev UI console (`art ui`)

BaseMod console surface for **inspecting** ART UI graphs, **emitting** signals, **invoking**
component actions, and limited **STS native** dump/click. For lab / harness / manual debug —
not co-op protocol.

Complements [`ui-ops-probe.md`](./ui-ops-probe.md) (`art probe` / `art op`).

## Why

| Need | Gap before this slice |
|------|------------------------|
| See mounted tree shape | `art probe` is coarse (button id lists, component slices) |
| Fire declared signals | No console path to `UiTree.emit` / `UiComponent.emit` |
| Generic component action | `art op` is sugar-only for known gestures |
| STS screen peek | Only ad-hoc reflection inside `StsNativeOps` |

## Architecture

```
art ui …
  ├─ list | tree | node     → pure UiInspect (UiTrees + UiComponent)
  ├─ emit | invoke | listen → SignalHub / UiOps.invoke
  └─ native dump | click    → StsUiReflect (whitelist only)
```

- **Primary path** = ART graph walk (no arbitrary Java reflection).
- **STS path** = fixed whitelist; failures → `unavailable`, never crash console.
- Log prefix: **`ART_UI`** (multi-line tree ok; node/emit/invoke prefer one line + logger).

## Commands

| Command | Role |
|---------|------|
| `art ui list` | Open windows + mounted/present/native component ids |
| `art ui tree <windowId> [depth]` | Indented tree (`id type [signals]`) |
| `art ui node <windowId> <id\|path>` | Node map: type, props, signals, handlerCounts, rect |
| `art ui emit <target> <signal> [args…]` | Emit on C1 instance or C2/present component |
| `art ui invoke <componentId> <action> [args…]` | `UiOps.invoke` / `UiComponent.action` |
| `art ui listen <target> <signal> [off]` | Lab handler → log `ART_UI_SIGNAL` |
| `art ui native dump` | Screen/room/event/endturn/select summary |
| `art ui native click <path>` | Whitelist: `endturn`, `grid.confirm`, `event <i>` |

### Emit target

- `windowId/controlId` or `windowId controlId` → C1 `tree.emit`
- Bare component id (`sts1.map`, present surface, open synthetic root) → `component.emit`
- Undeclared signal → console error, no throw to DevConsole

### Arg tokens

| Token | Parsed as |
|-------|-----------|
| (none) | empty payload |
| `true` / `false` | Boolean |
| integer / float | Number |
| `row,col` or `row,col,room` | `MapNodeRef` |
| other | String |

`emit` only notifies handlers. Gate + engine gesture still use `art op` or `ui invoke`.

## Packages

| Type | Package |
|------|---------|
| Pure inspect / emit / invoke | `artframework.inspect.UiInspect` |
| Lab listeners | `artframework.inspect.UiLabListeners` |
| Console | `artframework.console.ArtCommand` (`ui` subcommand) |
| STS whitelist | `artframework.sts1.inspect.StsUiReflect` |

## Non-goals (v1)

- Arbitrary `Class.forName` / field REPL
- Layout hot-reload
- Replacing `ART_PROBE` schema (optional future merge of tree summary)
- Dual-device life / multiplayer protocol

## Related

- Full command list (all `art …`): [`docs/development/console-commands.md`](../development/console-commands.md)
- [`ui-ops-probe.md`](./ui-ops-probe.md)
- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — signals / UiComponent
- [`docs/development/ui-layer-verification.md`](../development/ui-layer-verification.md)
- Task §17 in [`docs/task.md`](../task.md)

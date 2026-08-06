# UiOps / UiProbe design

Unified **command** and **query** surface over C1 synthetic windows and C2 native STS templates.  
Complements [`dual-track.md`](./dual-track.md). Implementation is phased; this doc is the contract.

## Why

ArtFramework owns **UI-layer** instructions and snapshots so:

- interceptors (BLOCK/ALLOW) and imperative ops share one path;
- consumers and tests do not re-implement grid/map/event gestures;
- ArtFramework can verify **intercept / trigger / C1 panel** without game authority.

## Facades (target API)

| Entry | Role |
|-------|------|
| `ArtFramework.ops()` → `UiOps` | Imperative UI commands |
| `ArtFramework.probe()` → `UiProbe` | Read-only UI snapshot (JSON-friendly) |
| Existing | `register` / `open` / `bind` / `close` / `entities()` |

Package sketch (Java 8):

- `artframework.api.UiOps`
- `artframework.api.UiProbe`
- `artframework.ops.UiOpResult` (`ok` \| `blocked` \| `unavailable` \| `notBound` + message)
- `artframework.ops.NativeOpsBackend` (STS impl + `FakeNativeOps` for JUnit)

## UiOps (first wave)

| Method (conceptual) | Track | Notes |
|---------------------|-------|--------|
| `selectCard(kind, cardId)` | C2 grid/hand | Dispatch card interceptors first |
| `confirmSelect(kind)` | C2 | Confirm interceptors |
| `clickMapNode(nodeRef)` | C2 map | `dispatchNodeClick`; pin **protocol** stays consumer |
| `chooseEventOption(index)` | C2 event | `dispatchOption` then option gesture |
| `pressEndTurn()` | C2 endturn | `dispatchPress`; no combat authority |
| `playHandCard(cardId, target?)` | C2 combat UI | **Gesture only** — no queue/protocol |
| `clickButton(windowId, buttonId)` | C1 | Layout button nodes |

Rules:

1. Inactive / unbound template → `notBound` (or documented passthrough).
2. First interceptor `BLOCK` → return `blocked`; no engine side effect.
3. Never import multiplayer protocol / party / combat phase types.

C1 signal-producing operations apply the same first-`BLOCK` rule through
`UiTree.addSignalInterceptor`. `UiOps.onButton`, `onSlider`, and `onHitArea` register
handlers on the tree `SignalHub`; they do not form a second dispatch path.

## UiProbe (first wave)

Machine-oriented snapshot (future console: `art probe` → one line `ART_PROBE` + JSON):

| Field group | Examples |
|-------------|----------|
| schema | `schemaVersion`, `modId` |
| windows | open ids, windowClass, synthetic layout summary |
| templates | bound map/event/select/endturn flags |
| map | pin list (decorative), last node ref if any |
| select | kind, pool card ids (when screen up — engine later) |
| endTurn | `buttonEnabled` hint |
| entities | slot count / kinds (no combat authority) |

**Out of probe:** `connected`, `peers`, party election, combat phase tables, and other domain-authority state.

## SpirePatch boundary

Patches live in ArtFramework and only call `NativeTemplateRuntime` / active templates.  
Consumers register interceptors for policy; ArtFramework patches remain thin UI-layer adapters.

## Phasing

| Phase | Deliverable | Status |
|-------|-------------|--------|
| P0 | This doc + dual-track + ui-layer-verification | done |
| P1 | Pure `UiOps`/`UiProbe` + Fake + JUnit | done |
| P2 | STS `NativeOpsBackend` (`StsNativeOps`) | done (best-effort gestures) |
| P3 | `@SpirePatch` thin hooks + `NativeUiHooks` | done |
| P4 | Console `art probe` / `art op` + fixture art-verify | done; device log scrape optional |
| P5 | Public consumer contract and fixture verification | done |

## Forward compatibility (Godot-aligned + milestone 15)

See [`godot-aligned-ui.md`](./godot-aligned-ui.md), [`backend-context.md`](./backend-context.md),
[`c2-full-present.md`](./c2-full-present.md). Existing methods stay stable; evolution is additive:

| Direction | Notes |
|-----------|--------|
| `component(id).action(name, …)` / `ops().invoke` | C2 (and later C1) route through `UiComponent`; named methods remain sugar |
| Probe `components[]` | Self-describing slices for synthetic trees + native hosts; bump `schemaVersion` when required |
| Signals | Leaf/native events via `connect`; `onButton` / interceptors unchanged in role |
| `open` / `bind` | Compatibility aliases for mount / activate native |
| Intent vs signal (15) | Signals observe/gate; **intents** hit Primary Backend; final pixels from next frame |
| Full C2 present (15) | Surfaces hard-sync from context; `playHandCard` → `play_card` + `CardRef` |
| `probeAssets` (15) | HostAssets pack/conflict/missing; not protocol state |

P0–P5 status above is **done** and not reopened by this section.

### Known limits (honest)

- Combat hand FULL uses ART draw + live executor; not a full reimplementation of every STS card chrome edge case.
- Map programmatic travel: ART map intent when FULL; domain code still owns enter-room policy.
- Event option invoke uses reflection (`buttonEffect` protected); FULL event suppresses `GenericEventDialog.render`.
- Event **gate patch**: instrument `AbstractEvent.update` call sites — Prefix on protected abstract `buttonEffect` NPEs MTS ParamInfo.
- Select FULL: grid select is primary; hand select is best-effort via `HandCardSelectScreen` fields.
- Amethyst: jar in `mods_library` requires `enabled_mods.txt` entry.
- Room FULL_READY requires matching `projection.scene` (`reward`/`rest`/`shop`/`treasure`); mount alone falls back to native.
- Shop/rest/treasure ART paint is chrome/labels; native atlas fidelity is not a 26 goal.

## Related

- [`dev-ui-console.md`](./dev-ui-console.md) — `art ui` inspect / emit / invoke / native dump (lab)
- [`docs/development/console-commands.md`](../development/console-commands.md) — full BaseMod `art` command reference
- [`backend-context.md`](./backend-context.md) — Backend / return channel / intents
- [`c2-full-present.md`](./c2-full-present.md) — full present surfaces
- [`host-assets.md`](./host-assets.md) — asset resolve / packs
- [`docs/development/ui-layer-verification.md`](../development/ui-layer-verification.md)
- [`docs/development/consumer.md`](../development/consumer.md)

# UiOps / UiProbe design

Unified **command** and **query** surface over C1 synthetic windows and C2 native STS templates.  
Complements [`dual-track.md`](./dual-track.md). Implementation is phased; this doc is the contract.

## Why

CrossSpire today drives native UI via console (`select` / `confirm` / map patches) and probes co-op state via `crossspire probe`.  
SpireUI should own **UI-layer** instructions and snapshots so:

- interceptors (BLOCK/ALLOW) and imperative ops share one path;
- consumers (CrossSpire, tests) do not re-implement grid/map/event gestures;
- SpireUI can verify **intercept / trigger / C1 panel** without multiplayer protocol.

## Facades (target API)

| Entry | Role |
|-------|------|
| `SpireUI.ops()` → `UiOps` | Imperative UI commands |
| `SpireUI.probe()` → `UiProbe` | Read-only UI snapshot (JSON-friendly) |
| Existing | `register` / `open` / `bind` / `close` / `entities()` |

Package sketch (Java 8):

- `spireui.api.UiOps`
- `spireui.api.UiProbe`
- `spireui.ops.UiOpResult` (`ok` \| `blocked` \| `unavailable` \| `notBound` + message)
- `spireui.ops.NativeOpsBackend` (STS impl + `FakeNativeOps` for JUnit)

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
3. Never import CrossSpire protocol / party / combat phase types.

## UiProbe (first wave)

Machine-oriented snapshot (future console: `spireui probe` → one line `SPIREUI_PROBE` + JSON):

| Field group | Examples |
|-------------|----------|
| schema | `schemaVersion`, `modId` |
| windows | open ids, windowClass, synthetic layout summary |
| templates | bound map/event/select/endturn flags |
| map | pin list (decorative), last node ref if any |
| select | kind, pool card ids (when screen up — engine later) |
| endTurn | `buttonEnabled` hint |
| entities | slot count / kinds (no combat authority) |

**Out of probe:** `connected`, `peers`, party election, combat phase tables — CrossSpire `GameStateProbe`.

## SpirePatch boundary

Patches live in SpireUI and only call `NativeTemplateRuntime` / active templates.  
Consumers register interceptors for policy. Dual-call migration then delete consumer duplicates.

## Phasing

| Phase | Deliverable | Status |
|-------|-------------|--------|
| P0 | This doc + dual-track + ui-layer-verification | done |
| P1 | Pure `UiOps`/`UiProbe` + Fake + JUnit | done |
| P2 | STS `NativeOpsBackend` (`StsNativeOps`) | done (best-effort gestures) |
| P3 | `@SpirePatch` thin hooks + `NativeUiHooks` | done |
| P4 | Console `spireui probe` / `spireui op` + fixture ui-verify | done; device log scrape optional |
| P5 | CrossSpire consumer migration (other repo) | open |

### Known limits (honest)

- Hand select card pick / full hand-play pipeline: gate + partial UI only; not full STS useCard.
- Map programmatic travel: gate + ok message; consumer still owns pin protocol / enter room.
- Event option invoke uses reflection (`buttonEffect` protected).
- Dual-mod with CrossSpire map/end-turn patches: both may run — consumer should migrate interceptors to SpireUI and slim own patches.

## Related

- [`docs/development/ui-layer-verification.md`](../development/ui-layer-verification.md)
- [`docs/development/consumer.md`](../development/consumer.md)
- CrossSpire `docs/console-commands.md` (source of gesture patterns; not protocol migration)

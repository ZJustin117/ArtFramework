# Signal API before/after ledger

## Baseline Inventory

Captured 2026-08-18 by read-only repository inspection. These are navigation evidence, not
completion criteria; recapture the relevant negative inventory before closure.

| Measure | Before | After | Interpretation |
|---|---:|---:|---|
| Core signal types (`SignalBus`, `SignalHub`, `SignalGroup`, `UiSignal`, decisions, subscriptions) | 9 | pending | Ownership map, not a deletion target by itself |
| Production `SignalHandler` references | 35 matches | pending | Compatibility surface and migration inventory |
| Production `SignalListener` references | 32 matches | pending | Target core listener surface |
| Public `emit` / `dispatch` pairs | 3 layers | pending | Return-value and naming consistency |
| Signal declaration paths | Node ports + entity/component declarations | pending | Node/entity validation parity |
| Signal name constants | 31 constants in one class | pending | Semantic grouping without behavior drift |

## Project Table

| ID | Scope | Before | Target after | Evidence before | Evidence after | Tests | Review | Status |
|---|---|---|---|---|---|---|---|---|
| SA-01 | Baseline and public-boundary decision | `SignalBus` is the runtime authority, but public APIs expose both `SignalListener` and `SignalHandler`, and `SignalHub` mixes scoped/raw responsibilities | Frozen target boundary identifies one core callback model, compatibility adapters, and explicit out-of-scope APIs before source edits | `SignalBus.java`, `SignalHub.java`, `SignalHandler.java`, `SignalListener.java`, `UiComponent.java`, `PresentationRuntime.java`, design docs | Core callback and send boundary frozen: `SignalListener`/`UiSignal`/`SignalDecision` are authoritative; `SignalHandler` and void `emit` are compatibility adapters; C1/C2 are removed from project scope | `SignalApiContractTest` 9/9; full JUnit 679/679 | R1-R5 findings recorded; final closure review pending | complete |
| SA-02 | Callback model | `SignalHandler.handle(Object...)` is used by node/entity adapters and UiOps, wrapped into `SignalListener`; it cannot replace or reject | New core paths use `SignalListener`; any retained handler API is a thin compatibility adapter with identical routing and cleanup semantics | `SignalHub.wrapHandler`; `UiComponent.connect`; 35 production references | Scoped listener operations expose full `UiSignal` decisions; legacy handler remains payload-only; Java 8 defaults preserve old implementations | `SignalApiContractTest` listener isolation, replacement, stop, payload, disconnect; 9/9; full JUnit 679/679 | R2/R4 closure evidence; final closure review pending | complete |
| SA-03 | Send result API | `emit()` and `dispatch()` had inconsistent result conventions across raw/scoped/component layers | `dispatch` is the canonical result-aware operation; historical public result-bearing `emit` descriptors remain binary-compatible and delegate to the same dispatch authority; void Hub/runtime emit remains unchanged | `SignalHub.emit/dispatch`; `SignalGroup.emit/dispatch`; `ArtFramework.emit/dispatch`; `UiComponent.emit/dispatch` | Public result-bearing emit descriptors restored where previously published; internal result callers use `dispatch`; no duplicate routing path | `SignalApiContractTest.publicGroupEmitRemainsResultBearingAlias`, result matrix; full JUnit passed | SA-08-01 fixed and verified; final review scope reconciled | complete |
| SA-04 | Scoped Hub vs raw Bus boundary | `SignalHub` exposes node-local connect plus full-name and regex Bus connect, all against the global native group | Scoped node API and raw group API have explicit ownership; no duplicate subscription registry or ambiguous path API remains | `SignalHub.connect`, `connectBus`, `dispatch`, registration cache | Empty instance IDs no longer clear/report raw registrations; context signal clear clears declarative connection/FSM registries; mismatched FSM contexts reject; runtime reset clears all scoped hubs | `SignalApiContractTest` cleanup/reset; 9/9; full JUnit 679/679 | SA-04 findings fixed; final closure review pending | complete |
| SA-05 | Declaration and path contract | Declarations use local names while routing uses string-built node/entity paths; validation is implicit in context/component adapters | Local signal capability and canonical routed identity are explicit at one boundary, with node/entity parity and unchanged path compatibility | `SignalPortsComponent`; `SignalPaths`; `PresentationRuntime.requirePort`; entity adapters | Signal port declarations reject null/blank values, trim names, and deduplicate after normalization; context cleanup shares declarative lifecycle owner; path canonicalization and identity lifecycle remain explicit follow-ups | `SignalDeclarationTest`, `SignalApiContractTest`; focused 9/9; full JUnit 679/679 | SA-05 partial findings accepted as future bounded work; evidence reconciled | complete |
| SA-06 | Signal name taxonomy | `SignalNames` is one flat class spanning control, native surface, animation, FX, drag, intent, and lifecycle events | Naming taxonomy is documented or minimally split without changing wire strings or introducing domain authority into core | `SignalNames.java`; design signal tables; call-site inventory | Canonical skeleton docs use `finished`; consumer docs distinguish void `emit` from result-bearing `dispatch`; no wire rename introduced | Full JUnit 679/679; exact wire docs updated | SA-06 findings dispositioned; final closure review pending | complete |
| SA-07 | Consumer and compatibility migration | `UiOps`, inspect, entity/host adapters and STS listeners consume mixed APIs | All production callers follow the target surface; compatibility readers delegate without second mutable signal truth | `UiOps.java`, `UiLabListeners.java`, entity/host adapters, STS listeners | Inspect maps dispatch outcomes; synthetic unmount has non-recursive cleanup; registry dispatch paths expose results | `SignalApiContractTest` host/reset; 9/9; full JUnit 679/679 | SA-07-01 fixed; lab listener and UiOps cache risks explicitly retained as follow-ups | complete |
| SA-08 | Final negative inventory and closure | No persisted inventory currently proves absence of stale callback, duplicate dispatch, or bypass paths | Negative searches, docs, tests, review closure, and final semantic gate prove the target boundary | Cross-repo grep inventory from SA-01 through SA-07 | Expanded scope includes TE-20 pack/render paths; public `SignalGroup.emit` descriptor restored; mixed scoped/raw multi-hub cleanup test passes; compatibility cleanup indexes are documented non-authoritative; SA-05/07 follow-ups remain explicitly outside scope | `SignalApiContractTest` 11/11; `./scripts/with-art-env.sh test` passed; negative grep; consumer compatibility review | R6 blocker resolved by explicit user scope; final review residuals SA-08-03/04 bounded and verified | complete |

## Status Rules

- `pending`: not investigated
- `reviewing`: baseline or findings in progress
- `implementing`: accepted findings being fixed
- `verifying`: implementation exists and gates are running
- `blocked`: named prerequisite or decision prevents progress
- `complete`: after evidence, tests, gates, and review closure are recorded

Never overwrite the before state. Append concise evidence to `Evidence after` and record exact test
classes or scenarios instead of writing only `tests pass`.

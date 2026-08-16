# Traditional ECS before/after ledger

## Baseline Inventory

Captured 2026-08-16 by read-only repository inspection. Counts are navigation and trend evidence;
they are not completion criteria and must be recaptured before final project closure.

| Measure | Before | After | Interpretation |
|---|---:|---:|---|
| Main Java files | 395 | pending | Scope size only |
| Main Java lines | 40,247 | pending | Prefer reduced duplicate responsibility, not arbitrary line reduction |
| Test Java files | 112 | pending | Behavioral mapping matters more than count |
| Test methods | 577 | pending | Exact focused tests are recorded per row |
| Component classes | 67 | pending | Audit data-only fields and duplicate concepts |
| `EcsSystem` implementations | 12 | pending | Every production system needs explicit ownership and order |
| Production `EcsPipeline.run` call sites | 3 | pending | Classify schedule phases and synchronous command boundaries |
| Production immediate `projectNow()` calls | 17 | pending | Each bypass requires a timing/lifecycle justification or migration |
| Coverage instrumentation | none | pending | Use behavior matrix unless instrumentation is separately approved |

## Project Table

| ID | Scope | Before | Target after | Evidence before | Evidence after | Tests | Review | Status |
|---|---|---|---|---|---|---|---|---|
| TE-01 | Schedule and execution ownership | Production uses the central schedule plus direct pipeline execution whose ownership classification is not yet persisted | Every production phase is schedule-owned, or is a documented one-shot command boundary using schedule-owned stateless system instances | `PresentationSchedule`; production `EcsPipeline.run` inventory; direct surface lifecycle paths | pending | Existing schedule and pipeline tests to inventory; add focused ordering/ownership tests for accepted findings | R1 pending | reviewing |
| TE-02 | Render projection mutation boundary | Scheduled dirty projection coexists with immediate projection from lifecycle and render paths | Render writes and projection timing have one explicit contract; immediate paths are removed or justified with lifecycle tests | `RenderProjectionQueue`; `RenderHost`; production `projectNow()` and `projectActiveSurfaces()` inventory | pending | Render plan recreation, dirty coalescing, active-surface, and no-frame-lag tests | pending | pending |
| TE-03 | Compatibility stores and read purity | Compatibility containers include mutable state candidates; some nominal reads may synchronize ECS state | Compatibility views query ECS without retaining truth; commands and reads are visibly separated | Audit `PresentProjections`, `PresentSurfaces`, `PresentPackApply`, `ArtFramework.OPEN`, probe, inspect, and console paths | pending | Query purity, reset/recreation, and compatibility behavior tests | pending | pending |
| TE-04 | Component data-only and duplicate concepts | Component inventory has not been reviewed recursively against prohibited host values and duplicate identity/hierarchy/lifecycle concepts | Every persistent fact has one component owner; components contain immutable/data-only values; duplicate concepts are removed or justified | Field/type inventory across `artframework.ecs`, `core`, and `presentation` components | pending | Component replacement, lifecycle, and serialization/probe tests per accepted finding | pending | pending |
| TE-05 | Host cache recreation | Existing C1/C2/STS1 host and provider caches are intended to be disposable, but proof is distributed | Clearing each cache preserves ECS authority and recreation restores behavior from retained ECS state | Audit StageHost, render targets, native templates, skeleton bindings, and EntityPresent host paths | pending | Pure recreation tests first; applicable D1 lifecycle scenarios only for changed host paths | pending | pending |
| TE-06 | Dependency direction | Single-module compilation permits cross-package imports without build enforcement | Core ECS and presentation ownership stay independent of STS/native adapters; allowed inversions are explicit host boundaries | Import-edge inventory, especially inward `sts1`, `c1`, and `c2` dependencies | pending | Compile/full JUnit plus focused API tests for moved boundaries | pending | pending |
| TE-07 | Final negative inventory and closure | Legacy authority removal claims are distributed across design/task notes | No unexplained old writer, duplicate mutable store, bypass API, or stale compatibility path remains | Repeat searches and compare against baseline inventory | pending | Full `@junit-test`; applicable offline/device gates from changed rows | independent final review pending | pending |

## Status Rules

- `pending`: not investigated
- `reviewing`: baseline or findings in progress
- `implementing`: accepted findings being fixed
- `verifying`: implementation exists and gates are running
- `blocked`: named prerequisite or decision prevents progress
- `complete`: after evidence, tests, gates, and review closure are recorded

Do not overwrite the before state after implementation. Add concise evidence to `Evidence after` and
record exact test classes or scenarios rather than writing only `tests pass`.

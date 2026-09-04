# Transient signal runtime before/after ledger

## Project table

| ID | Scope | Before | Target after | Evidence after | Tests | Review | Status |
|---|---|---|---|---|---|---|---|
| TSR-01 | Signal protocol | Transient ECS wrappers may carry runtime work outside the unified signal path | One ordered, synchronous signal protocol is the transient runtime authority | Isolated `transient-runtime` group, stable routes, immutable typed payloads, dispatch admission and reset retirement; no ECS wrapper migration | Extended focused core/context gate: 123 actual tests passed; full gate passed | R1-01 through R1-05 and R2-01/R2-02 fixed and verified by focused tests; `TSR-FINAL-01-01` fixed/verified by successful full gate in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-02 | Surface lifecycle command | Surface lifecycle wrappers may be represented as transient ECS data | Lifecycle commands dispatch synchronously; durable surface state remains ECS-owned | Canonical lifecycle payloads dispatch from `c2-surfaces`; keyed identity is validated; unmount dispatch precedes destruction; request component removed | Three-class schedule/authority/surface gate passed; extended focused gate passed; full gate passed | Implementation and TSR-01-04 fixed and verified by focused tests; stale blocker fixed/verified by `TSR-FINAL-01-01` in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-03 | Surface intent command/result | Surface intent wrappers may duplicate signal dispatch or persistence | Intent command/result uses the synchronous signal path and preserves receipt/result behavior | Typed payloads dispatch synchronously; the consumer validates identity, invokes the native dispatcher once, and records durable results; stopped intents fail confirmation and handled queued intents record results | Three-class gate passed; extended focused gate passed; full gate passed | TSR-FINAL-05 and queued-result fix verified by focused tests; stale blocker fixed/verified by `TSR-FINAL-01-01` in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-04 | Native intent lifecycle event | Native callback lifecycle wrappers may cross ECS as transient state | Native lifecycle activity is emitted as a synchronous runtime signal | Native input emits canonical lifecycle payloads; the consumer validates source and keyed identity, writes durable lifecycle state, and preserves observation transitions | Three-class gate passed; extended focused gate passed; full gate passed | TSR-01-04 fixed and verified by focused tests; stale blocker fixed/verified by `TSR-FINAL-01-01` in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-05 | Authority frame ingestion | Authority frames may be wrapped as transient command/event transport | Immutable authority frames enter through the established frame ingestion path | Typed `ContextFrame` dispatch projects through existing authority with no temporary wrapper; unavailable frames use monotonic epoch admission | Three-class gate passed; extended focused gate passed; full gate passed | TSR-FINAL-10 fixed and verified by focused tests; stale blocker fixed/verified by `TSR-FINAL-01-01` in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-06 | Business confirmation frame wrapper | Business confirmation may use a frame-shaped transient wrapper | Confirmation preserves frame/authority semantics without a second transient ECS transport | Immutable before/after frames dispatch through the runtime; durable confirmation rows remain authoritative; stop, replacement, and isolated disposal behavior are covered | Extended focused gate passed; full gate passed | TSR-FINAL-08-01 and TSR-FINAL-09-01 fixed and verified by focused tests; stale blocker fixed/verified by `TSR-FINAL-01-01` in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-07 | Cleanup and negative inventory | Stale wrappers, writers, readers, or cleanup may remain | Removed wrappers have no independent state authority | No references to the five deleted wrappers remain in `src/main/java` or `src/test/java`; historical documentation/debug strings are non-runtime | Focused gate passed; full gate passed | Negative inventory recorded; `TSR-FINAL-01-01` fixed/verified by successful full gate in `ses_f9408a74dffeL9vk22Ck4WQe0Z` | complete |
| TSR-08 | Final verification | Row-level evidence may not prove the integrated boundary | Focused tests, required gate, review, and negative inventory prove closure | `PresentProjectionTest` + `StrongFrameViewsTest` passed; extended focused core/context gate had 123 actual passing tests; STS/map/render classes passed 29/29 | `./scripts/with-art-env.sh clean test --no-daemon --no-parallel`: passed, `BUILD SUCCESSFUL`, 6 actionable tasks executed; Gradle JUnit XML stdout/stderr and HTML report generation were disabled in `build.gradle.kts` | `ses_f9408a74dffeL9vk22Ck4WQe0Z` reported the sole finding `TSR-FINAL-01-01` against the stale ledger blocker; fixed/verified by the successful full gate; no reviewer PASS claimed | complete |

## Final reconciliation

- TSR-01 through TSR-08 are complete.
- Focused tests passed, including the high-epoch unavailable cleanup in
  `PresentProjectionTest` + `StrongFrameViewsTest`.
- Full gate passed with the exact command recorded in TSR-08.
- Negative inventory found no references to the five deleted wrappers in `src/main/java` or
  `src/test/java`.
- `ses_f9408a74dffeL9vk22Ck4WQe0Z` returned one finding, `TSR-FINAL-01-01`, against the stale
  ledger blocker. The disposition is fixed/verified by the successful full gate, not reviewer PASS.
- `ses_f941ec280ffeSZrkbeqse3VSh7` returned no usable final report; it is recorded as no response.

## Status rules

- `pending`: not investigated.
- `reviewing`: baseline or findings are being assessed.
- `implementing`: accepted findings are being changed.
- `verifying`: implementation exists and required checks are running.
- `blocked`: a named decision or prerequisite prevents progress.
- `complete`: evidence, verification, and review closure are recorded.

Never overwrite a row's before state. Append concise after evidence and exact test classes or
commands when a row advances.

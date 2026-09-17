# Native Render Memory Lifecycle Reviews

Historical review entries retain the status known at the time of each round. The later
closure entries and the ledger table are authoritative for final status; references to
NRM-08/09/10/11 as blocked or verifying are superseded historical text where later closure
evidence is recorded.

## Round R1 - Ledger query and recovery contract

- The original `R1-01` heading text was truncated during an R13 prepend and is not reconstructed.
- Remaining recorded findings:
  - `R1-02` high, accepted, pending: generic evidence accepts already completed fallback,
    transition, close, or recovery records.
  - `R1-03` high, accepted, pending: disposition commit and Bridge token publication can cross
    recovery cleanup, leaving a stale pending token.
  - `R1-04` medium, accepted, contract resolved: IDs strictly increase per reset; evicted terminal
    IDs cannot be reused; callbacks outside recent/tombstone are stale/orphan and cannot recreate
    state.
  - `R1-05` medium, accepted, contract resolved: `delegatedWithoutEvidence` combines current open
    delegated gaps and cumulative terminal missing-evidence errors; diagnostic eviction is inert.
  - `R1-06` medium, accepted, contract resolved: collection queries are bounded open+recent
    snapshots and need not equal cumulative count APIs.
- Decisions:
  - Recovery fail-open is provisional only until exactly one late disposition settles it.
  - Recovery tombstones never admit late draw evidence.
  - Existing `int` API/probe count types remain compatible; width changes are out of scope.
- Residual risk: terminal accounting, bounded retention, and the recovery/token publication race
  still require implementation, focused tests, and re-review.

## Review Policy

## Round R15 - NRM-09 heartbeat/full-probe split

- Scope: `ProbePublisher`, `ProbeSidecar`, `StageHost`, `ArtCommand`, `UiProbe`, focused probe
  tests, and NRM-09 records. Existing worktree edits outside this scope were preserved; no
  deploy, device, downstream, or art-verify changes were made.
- Contract: one `postUpdate` monotonic sample drives heartbeat at 500ms and full snapshots at
  5000ms. `postRender` does not tick. `ART_PROBE ` and `art_probe_latest.log` retain v1 shape/prefix.
  Heartbeat schema v1 is separate; `sequence` is heartbeat-local, `probeSequence` identifies
  full publications, and `lastFullProbeFrame/Nanos` are freshness markers. Consumers use heartbeat
  for liveness and the latest full file for payload.
- `R15-05` high, accepted and fixed: full `probeSequence` and freshness commit only after snapshot
  construction and sink success. The sink reports whether at least one mount replaced its file;
  total failure keeps metadata unchanged and schedules a bounded 500ms retry.
- `R15-06` high, accepted and fixed: explicit full construction and persistence now occur inside
  the same synchronized publisher transaction. The publisher returns the constructed line for
  `ArtCommand` to log, and the preconstructed-line overload is removed. A latch-based regression
  proves explicit and automatic publication order cannot interleave.
- `R15-07` high, accepted and fixed: each sidecar mount writes a same-directory temporary and uses
  `java.io.File.renameTo` for replacement. Rename failure fails that mount, never direct-truncates
  the destination, and cleans the temporary best-effort; the other mount can still make the
  publication successful.
- Verification: focused publisher/sidecar/StageHost/legacy probe tests passed 40/40; default JUnit
  passed 1239/1239. No art-verify run was needed because tooling/YAML did not change.
- Disposition: findings fixed and semantically verified. NRM-09 remains `verifying` pending device;
  no deploy, device, or live allocation claim is made.

### R15-07 D1 regression correction

- Jar `df586...` reached READY on D1, then `scripts/art-lab combat verify-full` failed with
  `no fresh ART_COMMAND result for lab command`. The `File.renameTo` implementation did not
  overwrite an existing destination on Android; all latest-file sidecars could go stale after
  their first write. The earlier R15-07 source-level disposition did not establish device success.
- Corrected `ProbeSidecar.writeAtomically` to use Java 8 `Files.move` with `ATOMIC_MOVE` and
  `REPLACE_EXISTING` for same-directory replacement. An unsupported/failed move leaves the old
  destination intact, returns false for that mount, and does not silently fall back to truncation.
- Focused JUnit passed 40/40, full JUnit passed 1239/1239, and `git diff --check` passed.
  Corrected-jar D1 evidence is recorded below; NRM-09 remains `verifying` until the closure addendum
  is applied.

### R15 D1 closure

- Corrected jar `e1fcff48bde3c6d6bde730e7e6f7208aa4e7de6a44e32e462246f9bb17f5faf1` passed deployment
  verification and standard D1 FULL combat 1/1. Sidecar sampling confirmed separate heartbeat and
  full-probe files, valid one-line JSON, and the approved 500ms/5000ms behavior.
- Heartbeat/full payload sizes were 302 bytes and 184058-185222 bytes. The full file stayed unchanged
  at 1.2 seconds while heartbeat sequence advanced, then the full file advanced within approximately
  six seconds. Explicit `art probe` executed successfully.
- NRM-09 is complete. This is cadence/payload evidence only and does not claim paired allocation or
  GC improvement.

## Round R1 - NRM-05 Callback Admission Findings

- `R1-09` high, accepted and fixed: object skeleton failure callbacks performed owner lookup and
  FIFO token consumption outside `BRIDGE_LOCK`, so recovery could consume a token without closing
  its invocation. Failure admission now happens before token retirement, atomically under the bridge
  lock; rejected callbacks are bounded orphan diagnostics.
- `R1-10` high, accepted and fixed: ID skeleton failure callbacks queried invocation/disposition
  separately and then called a throwing open-only fallback path. The bridge now uses non-throwing
  atomic admission for OPEN delegated records; stale, recent, terminal, and non-delegated callbacks
  produce orphan diagnostics and never evidence. The legacy ledger method remains strict for direct
  callers and is covered by existing duplicate tests.

- Use finding IDs `R1-01`, `R1-02`, and so on for the NRM-01 baseline.
- If implementation changes the frozen scope, mark R1 `changed` and resume the same reviewer only
  for a focused re-review of accepted fixes.
- NRM-04 requires an independent sibling review because recovery ordering is a high-risk lifecycle
  boundary.
- A reviewer PASS never replaces focused JUnit, the default semantic gate, D1 verification, or
  diagnostic memory evidence.

## Round R2 - NRM-06 Transient-effect retention

- Session: primary bounded implementation session; no delegated reviewer was requested.
- Scope: `TransientEffectLedger.java`, `TransientEffectLifecycleAdapter.java`,
  `TransientEffectLedgerTest.java`, focused adapter/Bridge tests, and the NRM-06 project records.
- Result: focused verification complete; no reviewer PASS claimed.
- Findings and decisions:
  - Active `records` now contains only unfinished effects. Terminal records are retained only in a
    fixed-capacity recent window, with default capacity 256 and a constructor for small pure tests.
  - `total` counts terminal records once and `evicted` counts recent-window eviction; neither is
    altered by cleanup or recovery. `unknownLifecycle`, `leaked`, and `failOpen` remain independent.
  - Recent records preserve the existing duplicate termination behavior, including `complete` then
    `dispose`; callbacks after eviction are unknown and cannot recreate lifecycle state.
  - `reset` clears active, recent, cumulative, and diagnostic counters. No global timed clear exists.
- Verification: `./scripts/with-art-env.sh test --tests 'artframework.sts1.render.TransientEffect*'
  --tests 'artframework.sts1.patch.TransientEffectContainerPatchesTest' --tests
  'artframework.sts1.render.NativeRenderBridgeTest' --tests
  'artframework.sts1.render.NativeRenderLifecycleCleanupMatrixTest'` passed; 43 tests passed.
- Residual risk: this bounded task did not run deployment, D1, or a separate reviewer, and NRM-07
  allocation work remains pending.

## Round R3 - NRM-06 R2-01 late-render admission fix

- Scope: transient ledger render admission, lifecycle adapter render path, required NativeRenderBridge
  effect tests, and NRM-06 records. NativeRenderLedger, Bridge token state, render plan, probe
  cadence, asset materializer, and skeleton code remain outside scope.
- Result: fixed and focused-verified; no reviewer PASS claimed.
- Finding `R2-01`: recent terminal records could be mutated by late render, while the adapter's
  unconditional `create` could recreate active state after recent eviction or recovery cleanup.
- Fix: `admitRender` accepts only an identity matching an active record or an entirely unknown
  identity; recent terminal, evicted, and recovery-cleared identities are rejected and counted as
  bounded unknown lifecycle diagnostics. A fixed-capacity stale identity marker prevents implicit
  recreation without an unbounded tombstone. Explicit `create` remains the new-lifecycle admission
  and permits a genuinely new same-ID identity, including after recovery cleanup.
- Semantics: late render does not change state, `total`, or terminal diagnostics; the existing
  `complete -> dispose` transition remains supported, while repeated completion/dispose remains
  rejected without second accounting; update/dispose callbacks do not implicitly create records;
  Bridge `beginEffectRender` still returns `CAPTURE_AND_PASS`, preserving native continuation.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.render.TransientEffectLedgerTest' --tests
  'artframework.sts1.render.TransientEffectLifecycleAdapterTest' --tests
  'artframework.sts1.patch.TransientEffectContainerPatchesTest' --tests
  'artframework.sts1.render.NativeRenderBridgeTest' --tests
  'artframework.sts1.render.NativeRenderLifecycleCleanupMatrixTest'` passed, 57 tests.
- Residual risk: no deployment/D1 or independent reviewer was requested; NRM-07 allocation work
  remains pending.

## Round R4 - NRM-07 through NRM-10 bounded implementation

- Scope: `Sts1RenderPipeline`, `SurfaceDrawPlan`, `FullPresentMode`, `CombatInputRouter`,
  `Sts1AssetMaterializer`, and focused tests/docs. Existing NRM-01 through NRM-06 work was preserved.
- Result: NRM-07 implemented at the derived-plan boundary; NRM-08 and NRM-09 were then deferred for missing safe
  differential-update and heartbeat/full-probe compatibility contracts; NRM-10 partially implemented.
- NRM-07: cache identity includes projection frame, policy revision, executor revision/readiness,
  scene, mount flags, overlay, and panic state. `SurfaceDrawPlan` precomputes immutable draw order.
- NRM-10: production cache clear disposes cached libGDX textures; `BoundedTextureCache` provides a
  pure AutoCloseable lifecycle seam and exactly-once clear/dispose behavior.
- Verification: `./scripts/with-art-env.sh test --tests 'artframework.sts1.render.Sts1RenderPipelineTest'
  --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest'` passed (35 tests); the default
  `./scripts/with-art-env.sh test` gate also passed.
- Follow-up asset finding fixed in the primary implementation scope: missing-key storage is now
  fixed-capacity and resource liveness checks use object identity rather than `equals`; focused asset
  tests pass (8 tests), and the default gate passes.
- No D1, deployment, or Arthas evidence was produced. NRM-07/NRM-10 remained verification-stage rows;
  the NRM-11 pending status in this early round is superseded by later closure records.

## Round R5 - Transient-effect registry negative inventory

- Session: primary audit; the two requested `@developer` tasks failed before startup with an upstream
  task-service error and produced no edits or verification claims.
- Scope: `TransientEffectRegistry.java`, `TransientEffectProjectionSystem.java`,
  `TransientEffectLifecycleAdapter.java`, `NativeRenderBridge.java`, and
  `TransientEffectProjectionSystemTest.java`.
- Result: bounded queue confirmed; follow-up risk recorded.
- Findings:
  - The pending projection queue is keyed by instance ID, coalesces repeated callbacks, and evicts
    at `DEFAULT_PENDING_CAPACITY` 256. Existing `pendingProjectionStateIsCoalescedAndBounded` covers
    coalescing and capacity.
  - `clear()` can enqueue removal events for more active entities than the pending capacity, so a
    `cleanupAll()` caller could leave some removals unrepresented in the deferred queue. Recovery
    currently also calls `Sts1NativePresentationAdapter.clear()` directly, so this is not evidence of
    a current D1 leak; it remains an untested lifecycle edge requiring an explicit clear-all contract
    before implementation.
- Verification: `./scripts/with-art-env.sh test` passed with `BUILD SUCCESSFUL` after the audit.
  `TransientEffectProjectionSystemTest`, `TransientEffectLifecycleAdapterTest`,
  `TransientEffectLedgerTest`, and `NativeRenderBridgeTest` also passed 58/58.
- Residual risk: no independent source review PASS; no paired memory/allocation measurement; no live
  host texture recreation evidence.

## Round R6 - Clear-all ownership and query purity

- Session: `ses_f8352c7f7ffeFQbdPSXx13HGyL`
- Scope: frozen; NRM-01 through NRM-06 lifecycle sources and focused tests, including the registry
  clear-all implementation.
- Result: FINDINGS, then PASS after focused fixes.
- Findings:
  - `R6-01` high, accepted, fixed, verified: production recovery used broad shared-context clear,
    which could remove unrelated native surface entities. `NativeRenderBridge.clearTransientEffectsForRecovery()`
    now calls targeted `clearTransientEffects()`. The bridge regression covers more than the pending
    capacity of transient entities alongside an unrelated native entity.
  - `R6-02` medium, accepted, fixed, verified: `TransientEffectLedger.records()` exposed mutable
    lifecycle records. The ledger now stores private mutable records and returns immutable snapshots;
    the regression verifies snapshot isolation and list immutability.
- Focused verification: `NativeRenderBridgeTest`, `TransientEffectLedgerTest`,
  `Sts1NativePresentationAdapterTest`, and `TransientEffectLifecycleAdapterTest` passed; the
  subsequent combined lifecycle run passed 60/60.
- Re-review: same session returned `PASS`; both findings were resolved with no open questions.
- Residual risk: allocation/GC comparison, live host texture recreation, and final NRM-11 memory
  closure remain outside this lifecycle review.

## Round R7 - NRM-07/NRM-10 verification continuation

- Scope: existing `Sts1RenderPipeline`/`SurfaceDrawPlan` cache, `Sts1AssetMaterializer` bounded
  cache seam, focused tests, and this project's records. NRM-08 and NRM-09 remain outside scope and
  blocked by their ledgered differential-update and heartbeat/full-probe compatibility contracts.
- Result: verification evidence added; no production source change was justified.
- NRM-07: `agent-tmp/native-render-memory-lifecycle/run-benchmark.sh 2000000 100000` measured,
  with allocation tracking enabled, 2,224.00008 bytes/call for the current plan cache versus
  3,872.00008 bytes/call for the HEAD baseline; current plan identity reused and baseline did not.
  This confirms an isolated derived-plan hit improvement, not live callback/draw/probe or D1 GC
  behavior.
- NRM-10: focused pure tests passed, including clear/recreate, missing-key capacity, replacement
  and eviction disposal, shared-value identity, and disposed-cache behavior. The host-recreation
  command and production cache-clear seam were inspected but not run on a live host; no GL-context
  claim is made.
- Verification: focused command output reached `BUILD SUCCESSFUL` with 35 tests (pipeline plus
  asset materializer). The client timed out waiting for the finished Gradle message, so the default
  gate is still required before this round can be closed.
- Residual risk at the time: no independent allocation review, paired D1 before/after memory evidence,
  live host recreation evidence, or NRM-11 closure. NRM-10/11 status is superseded by later closure
  records; NRM-07 remained `verifying`.

## Round R7 - NRM-07/NRM-10 verification slice

- Scope: persisted NRM-07 frame-plan benchmark artifacts, `Sts1RenderPipeline`/`SurfaceDrawPlan`
  allocation seam, `Sts1AssetMaterializer` bounded cache seam, host-recreation path, and focused
  tests. NRM-08/09 were outside that round's scope under their then-unresolved ledger contracts.
- Result: verification evidence added; no production source change was justified. The paired
  synthetic benchmark completed at 100,000 iterations/10,000 warmup iterations with allocation
  tracking enabled: current 2224.0016 bytes/call versus HEAD baseline 3872.05216 bytes/call, with
  current plan identity reuse true and baseline false. A 1,000,000-iteration attempt was not used
  because its baseline benchmark class was truncated by the artifact rebuild.
- Verification: focused render/asset/recreation command passed 39 tests. The default gate ran all
  tests successfully but failed during Gradle result finalization because
  `build/test-results/test/binary/output.bin.idx` was missing; no semantic test failure was
  observed in the captured output.
- NRM-10 live GL/host recreation was not run or claimed. The pure cache and existing
  `PresentSafety.onHostRecreated()` path remain covered without fabricating a GL context.
- Residual risk at the time: NRM-07 had only synthetic source-level allocation evidence and no
  independent allocation review or paired D1 allocation/GC run; NRM-10/NRM-11 status here is
  superseded by later closure records. NRM-08 and NRM-09 were then deferred pending their
  contracts.

## Round R8 - NRM-07 accepted findings R8-01 and R8-04

- Scope: `Sts1RenderPipeline.java`, `SurfaceDrawPlan.java`, `Sts1RenderPipelineTest.java`, and
  project records only. NRM-08/09 and asset/host lifecycle work remain outside scope.
- Result: fixed and focused-verified; no completion status is claimed for NRM-07.
- `R8-01`: accepted and fixed. Executor readiness and panic are captured once by the pipeline and
  the same snapshot is used for cache-key inputs and `SurfaceDrawPlan.buildFromSnapshot`, avoiding
  a second readiness read during construction while preserving invalidation and output semantics.
- `R8-04`: accepted and fixed. Focused tests explicitly assert that a new frame ID creates a new
  derived plan identity, while unchanged listed presentation inputs determine its exact draw output;
  frame data is not treated as draw-plan authority.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.render.Sts1RenderPipelineTest'` passed, 30 tests. Full default gate remains
  to be run for this slice.

## Round R8 - NRM-07 accepted independent findings

- Scope: `Sts1RenderPipeline.java`, `SurfaceDrawPlan.java`, `Sts1RenderPipelineTest.java`, and
  these project records only. NRM-08/NRM-09, asset materializer/host lifecycle, and downstream
  projects remain outside scope.
- Findings:
  - `R8-01` accepted and fixed: readiness is sampled once by `Sts1RenderPipeline.plan()` and the
    resulting snapshot is supplied to `SurfaceDrawPlan` construction, so the cache key and derived
    plan cannot observe different readiness values.
  - `R8-04` accepted and fixed in tests: frame ID is asserted to invalidate derived plan identity,
    while the exact plan content is asserted from the current scene input (`wrong_scene` for the
    combat hand after switching to map). Frame data is not treated as presentation authority.
- Verification: focused `Sts1RenderPipelineTest` and the default JUnit gate are required for this
  slice; results will be appended after execution.

## Round R8 - bounded slice verification

- Scope: the accepted R8-01/R8-04 implementation and tests in `Sts1RenderPipeline.java`,
  `SurfaceDrawPlan.java`, and `Sts1RenderPipelineTest.java`. Unrelated worktree changes and
  NRM-08/09/10 remain outside scope.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.render.Sts1RenderPipelineTest'` passed (28 tests); the full
  `./scripts/with-art-env.sh test` gate passed with `BUILD SUCCESSFUL`.
- Additional checks: `git diff --check` passed; scoped diff inspection confirmed one readiness
  snapshot is used for cache-key construction and `buildFromSnapshot`, and the frame-identity
  tests assert derived-plan invalidation without treating frame payload as draw-plan authority.
- Disposition: R8-01 and R8-04 are ready for independent re-review. NRM-07 remains in verification
  status; this does not claim live-host, D1, or paired allocation/GC evidence.

## Round R8 - bounded-slice repair verification

- Scope: the accepted R8-01/R8-04 implementation and tests in `Sts1RenderPipeline.java`,
  `SurfaceDrawPlan.java`, and `Sts1RenderPipelineTest.java`; NRM-08/09 and NRM-10 remain outside
  scope.
- Repair: removed the duplicate/inconsistent readiness helpers left by the prior attempt, retaining
  one Java 8-compatible readiness surface table, snapshot type, and capture path. `plan()` now uses
  that single snapshot for both cache-key flags and `buildFromSnapshot()` construction.
- Verification: focused `./scripts/with-art-env.sh test --tests
  'artframework.sts1.render.Sts1RenderPipelineTest'` passed (30 tests); full
  `./scripts/with-art-env.sh test` passed (`BUILD SUCCESSFUL`). `git diff --check` passed.
- Disposition: R8-01 and R8-04 are ready for independent re-review. NRM-07 remains verification-stage;
   this does not claim live/D1 allocation evidence or completion of NRM-11.

## Round R9 - NRM-10 host-recreation lifecycle seam

- Scope: existing `Sts1AssetMaterializer` bounded cache and host-recreation entry point,
  `PresentSafety.onHostRecreated()`, `PresentSafetyTest`, and the existing `RenderHost`/ECS
  recreation contract. NRM-08/09 were outside that round's scope under their then-unresolved contracts.
- Result: focused pure/integration coverage added; no unrelated production lifecycle rewrite was
  justified. The current production seam remains `Sts1AssetMaterializer.onHostRecreated()`, which
  clears disposable materialized textures and missing-path markers; `PresentSafety` invokes it
  after host cache recreation and before the final projection pass. Authoritative ECS state is not
  cleared and the final projection rebuilds the disposable `RenderHost` target from that state.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.sts1.PresentSafetyTest' --tests
  'artframework.render.RenderPlanRebuildTest'` passed, 26 tests. The new regression asserts ECS
  retention and target reprojection through the production `PresentSafety.onHostRecreated()` path.
  Live GL/host execution was not performed by this agent and is not claimed.
- Disposition: NRM-10 remains `verifying`; independent re-review is needed for this bounded slice.
  Completion still requires direct live host/GL resource recreation evidence.

## Round R9 - NRM-10 host-recreation lifecycle seam

- Scope: `Sts1AssetMaterializer.java`, `PresentSafety.java`, `RenderPlanRebuildTest.java`,
  `Sts1AssetMaterializerTest.java`, and this project record. NRM-08/09 remain outside scope and
  blocked by their ledgered contracts.
- Result: bounded implementation and pure/integration verification; no completion status claimed.
- Finding `R9-01`: the production recreation path cleared the materializer through the generic
  `clearCache()` method, so the required disposable-host lifecycle boundary was implicit even
  though `PresentSafety.onHostRecreated()` already ordered it after host disposal and before
  projection rebuild. Accepted and fixed by adding the explicit `onHostRecreated()` materializer
  seam; it delegates to the existing identity-safe, reusable cache clear and does not alter the
  render authority or GL ordering.
- Tests now assert that ECS surface/full-frame state and effect declarations survive
  `RenderHost.recreateHostCache()`, disposable targets are cleared, and both targets/effects are
  rebuilt from ECS state. The cache lifecycle test asserts disposal, missing-marker reset, and
  post-clear rematerialization using a pure disposer.
- Live GL/D1 host recreation was not run and is not claimed. The production texture disposal path
  still requires execution on the host's valid GL lifecycle; this slice supplies no device or
  context evidence.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest'` passed (20 tests).
  `./scripts/with-art-env.sh test` passed with `BUILD SUCCESSFUL`.
- Disposition: `R9-01` fixed and test-verified. Historical NRM-10/NRM-11 status from this round is
  superseded by later closure records. Independent re-review was still needed for the new production
  seam at this point.

## Round R9 - accepted lifecycle hardening findings

- Scope: `Sts1AssetMaterializer.BoundedTextureCache`, `PresentSafety.onHostRecreated()`,
  `Sts1AssetMaterializerTest`, `PresentSafetyTest`, `RenderPlanRebuildTest`, and this record only.
- `R9-01`: accepted and fixed. Cache cleanup now detaches value and missing-marker state before
  invoking disposers, attempts each distinct resident identity despite failures, propagates the
  first failure with later failures suppressed, and leaves `dispose()` terminal via `finally`.
  Reinserted same-object behavior remains intentionally supported after `clear()` (clear ends the
  cache residency; the caller may materialize the same logical resource again).
- `R9-02`: accepted and fixed. PresentSafety isolates RenderHost cleanup, materializer cleanup,
  and final projection as separate fail-open steps, preserving retained ECS authority and ensuring
  a later projection attempt is not skipped by an earlier host failure. A package-scoped test runner
  injects the deterministic host-step failure without changing production APIs. Existing C1
  recreation already reports its own failure status and remains unchanged.
- `R9-03`: accepted and fixed with bounded pure/integration coverage. Tests exercise repeated
  production PresentSafety recreation, ECS retention, disposable target clearing/reprojection,
  cache reset, and rematerialization without constructing GL textures. No live GL/D1 evidence is
  claimed.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.PresentSafetyTest'` passed (27 tests).
  `./scripts/with-art-env.sh test` passed with `BUILD SUCCESSFUL`; `git diff --check` passed.
- Disposition: `R9-01`, `R9-02`, and `R9-03` fixed and test-verified. Historical NRM-10/NRM-11
  `verifying` status is superseded by later closure records; independent re-review was still
  required at this point.

## Round R9 - lifecycle hardening re-verification

- Scope: the existing R9-01/R9-02/R9-03 implementation and tests only; NRM-08/09 and unrelated
  worktree changes remain outside scope.
- Additional hardening: `BoundedTextureCache` cleanup now uses a detached resident snapshot without
  unchecked generic-array conversion. Tests cover identity de-duplication, continued attempts after
  disposer failures, suppressed later failures, detached state, reusable `clear()`, and terminal
  `dispose()` behavior.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.PresentSafetyTest'` passed (28 tests).
  `./scripts/with-art-env.sh test` passed with `BUILD SUCCESSFUL`; `git diff --check` passed.
- Disposition: R9-01, R9-02, and R9-03 remain fixed and test-verified. Historical NRM-10/NRM-11
  `verifying` status is superseded by later closure records; live GL/D1 resource evidence and
  independent re-review were still required at this point.

## Round R9 - final preamble isolation

- Scope: the existing R9 lifecycle implementation and tests only; NRM-08/09 and unrelated worktree
  changes remain outside scope.
- Change: the pre-materializer presentation-state recreation block is now one failure-isolated step,
  followed in all cases by independent RenderHost cleanup, materializer cleanup, and final projection
  attempts. This preserves successful ordering while ensuring a state-reset exception cannot bypass
  disposable asset cleanup or ECS-backed reprojection.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest'` passed (25 tests). `./scripts/with-art-env.sh test
  --tests 'artframework.sts1.PresentSafetyTest'` passed (8 tests). `./scripts/with-art-env.sh test`
  passed with `BUILD SUCCESSFUL`; `git diff --check` passed.
- Disposition: R9-01, R9-02, and R9-03 remain fixed and test-verified. No live GL/D1 evidence is
  claimed in this historical round; NRM-10/NRM-11 status is superseded by later closure records.

## Round R10 - persisted-state reconciliation and ordered contract assessment

- Scope: current R9 lifecycle source/tests, NRM-07 cache source/tests, existing architecture/API
  contracts relevant to NRM-08/09, and project records. No device, live host, or allocation tooling
  was run.
- R9 reconciliation: current source still implements all accepted R9-01/R9-02/R9-03 hardening:
  identity-safe detached cache cleanup with failure aggregation, terminal disposal, explicit
  materializer recreation, independent fail-open host/materializer/projection steps, retained ECS
  authority, and final reprojection. No concrete remaining source/test finding was found. Final
  combined focused verification passed 35/35. No independent re-review result is present, so R9 review
  closure remains pending; no live GL/D1 evidence is claimed.
- NRM-07 closure inspection: current source uses one packed primitive readiness/panic sample for
  cache identity and construction, reuses the immutable plan/draw-order for identical inputs, and
  invalidates on frame/policy/executor/scene/mount/overlay/safety changes. No concrete remaining
  source/test finding was found. Focused verification passed 29/29. NRM-07 still needs independent
  allocation review and live paired allocation/GC evidence.
- NRM-08 assessment: one narrow component-writer slice is present and verified. The existing
  `c2-full-present.md` rule `Diff: None -> Skip work` and existing incremental component tests permit
  `RenderStateEcs` to retain its current component when all represented surface geometry/enabled,
  surface-effect, or full-frame values are equal; changed values
  still replace the component, so ECS and render authority do not move. `RenderPlanRebuildTest`
  freezes both equal no-op and changed replacement behavior (13/13 focused). The broader row remains
  blocked: `backend-context.md` states `diff` only as an architectural stage, while
  `traditional-ecs.md` freezes immutable-plan projection, coalescing, and stale-output correctness
  but does not define whether unchanged `RenderTarget`/`EffectBinding` identity must survive, how
  active-surface stale removal composes with retained non-C2 targets, or which lifecycle event
  invalidates differential host state. No host-target differential behavior was invented.
- NRM-09 assessment: remains blocked. `UiProbe.SCHEMA_VERSION == 1`, `asMap()`, `toJsonLine()`, and
  `UiOpsProbeTest.probeShape` freeze one complete JSON-friendly probe and `ART_PROBE` prefix.
  `StageHost` currently invokes that full serializer after 30 combined post-update/post-render
  callback opportunities, but this is implementation-local and has no documented frame/time
  cadence. There is no owning writer clock, heartbeat payload/schema version, full-snapshot cadence,
  freshness marker, consumer fallback/merge rule, or schema-version compatibility rule. No split was invented.
- Focused verification: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest' --tests 'artframework.sts1.PresentSafetyTest'` passed
  35/35; `./scripts/with-art-env.sh test --tests
  'artframework.sts1.render.Sts1RenderPipelineTest'` passed 29/29; `./scripts/with-art-env.sh test
  --tests 'artframework.render.RenderPlanRebuildTest'` passed 13/13. The full
  `./scripts/with-art-env.sh test` gate passed 1189/1189.
- Disposition: R9 findings and NRM-07 source are test-verified but remain review/evidence-open.
  NRM-08/09 were then deferred on the exact decisions above. NRM-10/11 remain `verifying`; no live
  allocation, memory, GL, D1, or independent-review evidence is claimed.

### R10 final verification

- Final focused R9/NRM-08 command passed 35/35:
  `./scripts/with-art-env.sh test --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest'
  --tests 'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.PresentSafetyTest'`.
- Final `./scripts/with-art-env.sh test` passed 1189/1189 with `BUILD SUCCESSFUL`.
- The first green NRM-08 run executed 13/13 passing tests but Gradle failed result finalization
  because `build/test-results/test/binary/output.bin.idx` disappeared; the immediate focused rerun
  passed normally. This was not a semantic test failure.

## Round R10 - accepted NRM-08 ECS-writer findings

- Scope: `RenderStateEcs.java`, `RenderPlanRebuildTest.java`, and this project's records only. No
  `RenderHost`, `RenderTarget`, `EffectBinding`, probe, device, or allocation-measurement behavior is
  included.
- `R10-01` accepted, fixed, and verified: `captureEnabled` now retains the current immutable
  `RenderCaptureComponent` for a value-equal write. A changed enabled value replaces the component.
  Capture lifecycle and ECS authority are unchanged; no host mirror was added.
- `R10-02` accepted, fixed, and verified: `updateC1Bounds` now retains the current
  `BoundsComponent` when all four coordinates/dimensions are equal. Changed geometry replaces the
  component while retaining its existing z. Host binding lookup remains the boundary and no
  scene2d object reference enters ECS.
- `R10-03` accepted, fixed, and verified: focused JUnit now covers equal-instance retention and
  changed-value replacement across surface coordinates, dimensions, and enabled state; ordered
  surface-effect id, layer, parameters, and enabled state; full-frame dimensions, enabled state,
  and effects; capture enabled state; and C1 coordinate/dimension changes with retained z. It does
  not assert host target or binding identity.
- Verification: `./scripts/with-art-env.sh test --tests
  'artframework.render.RenderPlanRebuildTest'` passed 13/13; `./scripts/with-art-env.sh test` passed
  1189/1189 with `BUILD SUCCESSFUL`; `git diff --check` passed.
- Disposition: R10-01, R10-02, and R10-03 are fixed, test-verified, and ready for independent
  re-review. NRM-08 was then unresolved beyond this sub-boundary on host-plan equality,
  target/binding identity, stale-target composition, and lifecycle invalidation contracts. NRM-09
  remains blocked. No device, live allocation, memory, or GC evidence is claimed.

### R10 accepted-findings final rerun

- Focused `./scripts/with-art-env.sh test --tests 'artframework.render.RenderPlanRebuildTest'`
  executed 13/13 passing tests and printed `BUILD SUCCESSFUL`; the client reported a socket timeout
  only after receiving that finished Gradle output.
- Full `./scripts/with-art-env.sh test` passed 1189/1189 with `BUILD SUCCESSFUL`.
- `git diff --check` passed. Scoped diff inspection found no `RenderHost`, `RenderTarget`, or
  `EffectBinding` identity behavior and no edits outside the allowed source/test/project-record
  scope for this continuation.

### R10 independent closure reconciliation

- Result: PASS for each of R10-01, R10-02, and R10-03, as supplied by the parent together with
  independent JUnit verification. Reviewer session IDs and separate review transcripts were not
  available in this workspace record, so none are invented here.
- Boundary disposition: the value-equal `RenderStateEcs` writer slice is a completed sub-boundary.
  Its PASS covers surface geometry/enabled, ordered effects, full-frame, capture, and C1-bounds
  component retention/replacement only. It does not cover `RenderHost`, `RenderTarget`, or
  `EffectBinding` identity, stale-target composition, lifecycle invalidation, or allocation results.
- Historical row disposition: NRM-08 was unresolved, rather than complete, because those broader host-target
  differential semantics and measured evidence are still absent.

## Round R11 - ordered remaining-row contract and source assessment

- Scope: NRM-09 `UiProbe`, `ProbeSidecar`, `StageHost`, `UiOpsProbeTest`, probe/API design docs;
  then the current NRM-07 plan cache, NRM-10 materializer/recreation lifecycle, NRM-11 retained-store
  inventory, focused tests, and persisted records. No downstream project, device, live GL, Arthas,
  allocation tool, or GC measurement was used.
- NRM-09: no implementation is safe. `UiProbe.SCHEMA_VERSION == 1`, `asMap()`, `toJsonLine()`, the
  `ART_PROBE ` prefix, and `UiOpsProbeTest.probeShape` define a complete snapshot. The compatibility
  docs permit additive schema-v1 groups but do not define a second heartbeat payload. `StageHost`
  increments one counter from both post-update and post-render and serializes at 30 callback
  opportunities; `ProbeSidecar` only overwrites local and external latest-snapshot files. Missing
  decisions are the owning writer clock, heartbeat schema/version and fields, full-snapshot cadence,
  freshness marker, consumer fallback/merge behavior, and schema migration. No behavior or test was
  invented.
- NRM-07: reinspection found no additional bounded source/test correction beyond the existing
  primitive readiness/panic sample, exact cache invalidators, immutable plan reuse, and precomputed
  draw order. Existing tests cover same-input reuse and frame/policy/readiness/mount/overlay/panic
  invalidation. Completion still needs independent review and comparable live allocation/GC evidence;
  the persisted synthetic benchmark remains source-level evidence only.
- NRM-10: reinspection found no additional safe pure correction. `BoundedTextureCache` bounds values
  and missing markers, performs identity-safe state-first cleanup with failure aggregation, keeps
  disposal terminal, and the recreation path failure-isolates cleanup and reprojection while retaining
  ECS authority. Existing tests exercise those semantics without GL. Independent R9 review and live
  valid-GL host recreation evidence remain required.
- NRM-11: existing bounded ledger, bridge, effect ledger/registry, targeted clear-all, immutable query,
  plan-cache, and materializer tests then provided no substitute for paired D1 memory/allocation and
  final independent review. That blocker is superseded by the later user-scoped closure standard; no
  paired improvement claim is made.
- Result: records-only reconciliation. No safe source/test change was justified under the frozen
  contracts and evidence restrictions.
- Verification for this records-only continuation: source/JUnit was not rerun because no source or
  test changed; `git diff --check` passed on 2026-09-09. The prior R10 evidence remains the exact
  focused 13/13 `RenderPlanRebuildTest` run and 1189/1189 default gate recorded above.
- Concrete next action: freeze NRM-09's writer clock, heartbeat prefix/schema/fields, full-snapshot
  cadence, freshness semantics, consumer merge/fallback behavior, and migration rule before any
  implementation. Independently close R8/R9 review and collect the paired D1 memory/allocation,
  long-run GC, and live valid-GL recreation evidence required by NRM-07/NRM-10/NRM-11.

## Round R12 - D1 host-recreation thread defect

- Evidence inspected: `debug-artifacts/art-verify/d1_entity_present_smoke.json` is a 16/16 pass.
  Before recreation it asserted one EntityPresent slot, one entity draw item, and enabled target
  `c2_entity_art-lab-entity`; after recreation it again asserted draw count 1 and the enabled target;
  final detach asserted cleanup. `d1_entity_present_smoke.latest.log` records StageHost ready at
  startup, then repeated `Thread[agent-session]: No context is current` messages immediately before
  `ART_LAB host-recreate OK`. The following probe reports
  `backend.safety.c1HostRecreation = failed: StageHost` while ECS/draw/target rebuilding succeeded.
- Root cause: `ArtCommand` invokes `PresentSafety.onHostRecreated()` directly on the connector
  `agent-session` thread and then unconditionally returns `UiOpResult.ok`. PresentSafety synchronously
  entered StageHost; `StageHost.recreateHost()` caught the off-context Stage disposal/creation failure
  and returned false, which PresentSafety truthfully converted to the probe failure. The already
  failure-isolated later RenderHost, materializer, and projection steps still ran, explaining why the
  scenario's C2 assertions passed despite the C1 failure.
- Fix: PresentSafety marks the request scheduled and dispatches the entire ordered recreation body to
  `Gdx.app.postRunnable`; pure tests fall back synchronously when no application exists. The existing
  per-step fail-open ordering remains `presentationState`, `c1Host`, `renderHost`, `materializer`,
  `projection`, so a C1 or earlier failure cannot skip materializer cleanup or ECS reprojection.
  Dispatcher failure remains truthfully reported rather than producing a false successful probe;
  StageHost now preserves a specific terminal recreation phase/type/message instead of collapsing
  every false result to `failed: StageHost`. The command reports that recreation was requested rather
  than claiming synchronous completion.
- Regression: `PresentSafetyTest.hostRecreateDispatchesLifecycleWorkBeforeReportingC1Result` proves
  no lifecycle step runs before dispatched work, status is `scheduled` while queued, and all later
  steps execute in order. Focused materializer/rebuild/PresentSafety verification passed 36/36;
  full `./scripts/with-art-env.sh test` passed 1190/1190; `git diff --check` passed.
- Limitations: this is not post-fix device evidence. The passing D1 artifact has no `ART_UI` evidence
  or pixel assertion and did not materialize/track a real cached Texture, so it proves neither visual
  output nor actual Texture disposal. Historical NRM-10/NRM-11 status is superseded by
  later closure records; NRM-08 and NRM-09 were then deferred on their recorded contracts.
- Next action: deploy outside this session and rerun the same D1 scenario, asserting terminal C1
  recreation success in addition to ECS/draw/target rebuild and cleanup. Obtain separate valid-GL
  evidence with a real materialized Texture before claiming disposal, then close independent review
  and paired memory/allocation evidence.

## Round R12 - accepted admission and command-path hardening

- Scope: `PresentSafety.java`, `ArtCommand.java`, `PresentSafetyTest.java`, and this project record;
  NRM-08/09 were then deferred and NRM-10/11 remain verification-stage.
- `R12-01` accepted and fixed: host recreation exposes bounded admission outcomes for scheduled,
  synchronous, and dispatcher-failure paths. Dispatcher rejection/throw records a truthful probe
  failure and returns before any lifecycle step. Scheduled admission remains non-blocking and reports
  only `requested`; it does not claim terminal recreation success.
- `R12-02` safe unit/command portion accepted and fixed: `art lab host-recreate` maps dispatcher
  failure to an error result while mapping both accepted modes to requested semantics. The ordered,
  failure-isolated lifecycle remains presentationState, c1Host, renderHost, materializer, projection.
  No request coalescing or production GL success was invented.
- Verification on 2026-09-09: `./scripts/with-art-env.sh test --tests
  'artframework.sts1.PresentSafetyTest' --tests 'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest'` passed 40/40; full
  `./scripts/with-art-env.sh test` passed 1194/1194 with `BUILD SUCCESSFUL`; `git diff --check`
  passed and the allowed-path diff was inspected. D1/deployment/live GL evidence was not run.
  NRM-10/11 remain `verifying`; NRM-08/09 were outside that round's scope. R12-01 and the safe unit/command-path
  portion of R12-02 are ready for independent re-review.

## Round R12 - bounded dispatch-admission hardening

- Scope: `PresentSafety.java`, `ArtCommand.java`, `PresentSafetyTest.java`, and these project records.
  No command test seam exists in the current test tree, so no unrelated console test file was added.
- `R12-01` accepted and fixed: host-recreation dispatch now returns a bounded admission result,
  distinguishing `SCHEDULED`, `SYNCHRONOUS`, and `DISPATCHER_FAILURE`. Rejection or an exception
  leaves the probe at an explicit dispatcher failure and runs no lifecycle step.
- `R12-02` safe unit/command-path portion accepted and fixed: `art lab host-recreate` maps dispatcher
  failure to an error result, while both asynchronous scheduling and synchronous fallback report only
  `host cache recreation requested`; the command never waits for deferred completion or claims its
  terminal result. Terminal probe state is still written by the deferred lifecycle itself.
- Ordered execution remains failure-isolated and unchanged: `presentationState`, `c1Host`,
  `renderHost`, `materializer`, `projection`; later steps still run after an earlier step failure.
- Verification: `./scripts/with-art-env.sh clean test --tests 'artframework.sts1.PresentSafetyTest'
  --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.render.NativeRenderLifecycleCleanupMatrixTest'` passed 43/43 (13 PresentSafety,
  14 materializer, 13 render-plan, and 3 lifecycle-matrix tests). A direct command-result seam is
  available through `ArtCommand.hostRecreateResult()` and
  its dispatcher-failure path is covered by `PresentSafetyTest`; no full console harness was needed.
  Full gate passed 1194/1194 and `git diff --check` passed.
- Disposition: `R12-01` and the safe unit/command-path portion of `R12-02` are fixed and ready for
  independent re-review after the remaining gates. NRM-10/11 remain `verifying`; NRM-08/09 remain
  blocked. No request coalescing, production GL success, device, or live-host evidence is claimed.

### R12 post-fix D1 evidence

- Authoritative artifacts: `debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.json`
  and `debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.latest.log`.
- Result: `d1_entity_present_smoke` passed 1/1 scenario and 16/16 steps. After `art lab
  host-recreate`, the probe reports `backend.safety.c1HostRecreation=rebuilt`,
  `entities.slotCount=1`, `backend.entityDraw.count=1`, and target
  `c2_entity_art-lab-entity` enabled. Detach cleanup reports draw count 0, slot count 0, and the
  target absent.
- Evidence boundary: the artifacts contain no Texture-disposal/rematerialization proof and no
  pixel/`ART_UI` proof. No independent R12 review result or reviewer session ID is present in this
  project record, so no R12 PASS is claimed. The then-current NRM-10/11 `verifying` status in this
  R12 section is superseded by later NRM-10 completion and user-scoped NRM-11 closure records.
  NRM-08/09 were then deferred on their recorded contracts.

## Round R12 - accepted compatibility and command-matrix findings

- Scope: `PresentSafety.java`, `ArtCommand.java`, `PresentSafetyTest.java`, and this project record
  only. NRM-08/09 were outside that round's scope; NRM-10/11 remain verifying. No downstream callers, YAML, device,
  deployment, Arthas, Texture/pixel, or paired-memory behavior was changed or claimed.
- `R12-03` accepted and fixed: `PresentSafety.onHostRecreated()` is again a public static `void`
  compatibility wrapper with JVM descriptor `()V`; it calls `requestHostRecreation()` and does not
  wait for deferred application-thread work. `ArtCommand.hostRecreateResult()` uses the separately
  named admission-returning method, preserving immediate dispatcher rejection/throw as command
  `ERROR` while accepted scheduled and synchronous requests remain `OK ... requested`.
- `R12-04` accepted and fixed: focused command coverage now exercises scheduled admission with work
  deferred, synchronous fallback with ordered execution, explicit rejection, dispatcher throw, no
  lifecycle execution on both rejection paths, and truthful probe failure. Existing ordered,
  failure-isolated lifecycle tests remain in the matrix; no request coalescing was introduced.
- Verification on 2026-09-09:
  `./scripts/with-art-env.sh test --tests 'artframework.sts1.PresentSafetyTest' --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest' --tests
  'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.sts1.render.NativeRenderLifecycleCleanupMatrixTest'` passed 47/47
  (17 PresentSafety, 14 materializer, 13 render-plan, 3 lifecycle-matrix tests). The full
  `./scripts/with-art-env.sh test` gate passed with `BUILD SUCCESSFUL`; `git diff --check` passed.
  `javap -s -classpath build/classes/java/main artframework.sts1.PresentSafety` confirmed both
  `public static void onHostRecreated(); descriptor: ()V` and the new
  `public static ... requestHostRecreation();` descriptor. The focused test also asserts the void
  return type and static compatibility surface.
- Disposition: `R12-03` and `R12-04` are fixed, focused/default-JUnit verified, and ready for
  independent re-review. NRM-10/11 remain `verifying`; live Texture/pixel/paired-memory and final
  integrated review limitations are intentionally retained. NRM-08/09 were then deferred on their
  recorded contracts.

## Round R12 - accepted findings R12-03 and R12-04

- Scope: `PresentSafety.java`, `ArtCommand.java`, `PresentSafetyTest.java`, and this project record.
  NRM-08/09 were outside that round's scope; NRM-10/11 remain `verifying`.
- `R12-03` accepted and fixed: the public static `PresentSafety.onHostRecreated()` method is again
  `void` with JVM descriptor `( )V` (rendered by `javap` as `onHostRecreated:()V`). It delegates to
  the separately named `requestHostRecreation()` admission API and does not wait for deferred work.
  `ArtCommand.hostRecreateResult()` uses the admission API, preserving immediate dispatcher
  rejection as a command error.
- `R12-04` accepted and fixed: focused tests cover scheduled admission with deferred lifecycle work,
  synchronous fallback, explicit rejection, and dispatcher throw. Accepted requests return `OK`
  with `host cache recreation requested`; rejection/throw returns `ERROR`, executes no lifecycle
  step, and leaves a truthful dispatcher failure in the probe. Ordered failure-isolated lifecycle
  behavior remains covered, with no request coalescing added.
- Verification: `./scripts/with-art-env.sh test --tests 'artframework.sts1.PresentSafetyTest'
  --tests 'artframework.sts1.render.NativeRenderLifecycleCleanupMatrixTest' --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest'` passed 34/34 (17 PresentSafety, 14
  materializer, 3 lifecycle tests). `./scripts/with-art-env.sh test` passed 1198/1198 with
  `BUILD SUCCESSFUL`; `javap -classpath build/classes/java/main -s artframework.sts1.PresentSafety`
  confirmed `onHostRecreated` descriptor `()V` and `requestHostRecreation` returning
  `HostRecreationAdmission`; `git diff --check` passed. An initial non-clean focused invocation and
  a clean invocation overlapped existing worktree/build state and failed at unrelated test
  compilation; the clean successful rerun above is the authoritative result.
- No live GL, D1, Texture, pixel, `ART_UI`, paired-memory, or Arthas evidence is claimed. The accepted
  findings are ready for independent re-review. NRM-10/11 remain `verifying`; NRM-08/09 remain
  blocked, and the live Texture/pixel/paired-memory limitations are retained.

## Round R13 - materializer failure isolation coverage

- Scope: `PresentSafety.java`, `PresentSafetyTest.java`, and this project record. NRM-08/09 remain
  blocked; NRM-10/11 remain `verifying`. R12-03/R12-04 stay independently reviewed PASS and are not
  reopened. No YAML, ArtCommand, D1, Texture/pixel, or paired-memory claim is made.
- Result: focused coverage added. Production already continued projection after a materializer
  cleanup throw; errors were previously swallowed with no materializer-specific probe text.
- Change: `PresentSafetyTest.hostRecreateContinuesProjectionAfterMaterializerFailure` injects a
  `HostRecreationStepRunner` that throws on `materializer` and still records later steps. It asserts
  ordered execution through `projection`, retained ECS authority, a projectable host target, and
  `materializerRecreation=failed: materializer`. A dedicated probe field
  records that isolated step without overwriting C1 status or claiming rebuilt/success from later
  projection.
- Compatibility: `onHostRecreated()` remains public static void (`()V`) and still returns without
  waiting for deferred application-thread work. Existing scheduled-admission and command-path tests
  remain in the suite.
- Verification: focused `./scripts/with-art-env.sh test --tests 'artframework.sts1.PresentSafetyTest'`
  passed 18/18. Full JUnit, deploy, D1, and art-verify were not run.
- Disposition: no reviewer session ID or PASS is claimed. Next action remains independent review /
  real Texture evidence; this slice does not close NRM-10.

## Round R13 - independent review

- Session: `ses_f753816c9ffe7O6YOWLHCHEvbA`
- Scope: frozen `PresentSafety.java`, `PresentSafetyTest.java`, and this project record.
- Result: FINDINGS
- Findings:
  - `R13-01` medium, accepted, pending: materializer fail-open catch writes only
    `failed: materializer` and drops exception type/message.
  - `R13-02` medium, accepted, pending: scheduled and dispatcher-reject/throw tests assert only
    `c1HostRecreation`, not lockstep `materializerRecreation`.
- Residual risk: NRM-10 remains `verifying`; no Texture/pixel/paired-memory claim. Original R1-01
  heading text was truncated earlier and is not reconstructed.

## Round R13 - accepted findings R13-01 and R13-02

- Session: `ses_f753816c9ffe7O6YOWLHCHEvbA` Result: FINDINGS. No new PASS or session ID is claimed.
- Scope: `PresentSafety.java`, `PresentSafetyTest.java`, and this project record. NRM-08/09 remain
  blocked; NRM-10/11 remain `verifying`.
- `R13-01` accepted and fixed: `runHostRecreationStep()` keeps fail-open continuation and the
  dedicated `materializerRecreation` field, and now writes `failed: ` plus the exception simple
  name. Projection does not rewrite the field.
- `R13-02` accepted and fixed: scheduled-admission and dispatcher-reject/throw tests assert
  lockstep `c1HostRecreation` and `materializerRecreation` (`scheduled`,
  `failed: dispatcher rejected`, `failed: dispatcher IllegalStateException`) as distinct keys.
  `hostRecreateContinuesProjectionAfterMaterializerFailure` throws
  `DistinctMaterializerRecreationException` and asserts
  `materializerRecreation=failed: DistinctMaterializerRecreationException`, C1 not rebuilt, and
  projection still runs.
- Compatibility: `onHostRecreated()` remains public static void (`()V`) and non-blocking.
- Verification: focused `./scripts/with-art-env.sh test --tests
  'artframework.sts1.PresentSafetyTest'` passed 18/18. Full JUnit, deploy, D1, and
  art-verify were not run.

## Round R13 - focused fix re-review

- Session: `ses_f753816c9ffe7O6YOWLHCHEvbA`
- Scope: frozen
- Result: PASS
- Findings: none
- Residual risk: NRM-10 remains `verifying`. Exception messages are still omitted (simple name
  only). `presentationState` / `renderHost` / `projection` failures still have no dedicated probe
  field. Real Texture disposal/rematerialization evidence remains outside this slice. This PASS
  does not complete NRM-10.

- Post-fix default `./scripts/with-art-env.sh test` passed 1199/1199; `PresentSafetyTest` 18/18.

## Round R14 - additive materializer probe counters

- Scope: `Sts1AssetMaterializer.java`, `Sts1AssetMaterializerTest.java`, `UiProbe.java`
  nesting only, and this project record. PresentSafety, YAML, NRM-08/09, and R13-01/R13-02
  were not reopened. NRM-10/11 remain `verifying`; NRM-08/09 were outside that round's scope.
- Result: focused implementation of additive diagnostic counters. No reviewer PASS or
  session ID is claimed. No live Texture/GL/D1 evidence is claimed.
- Change: `BoundedTextureCache` records resident, missing, cumulative put/load, cumulative
  dispose/clear-attempt, and optional hit counts. `Sts1AssetMaterializer.probeSlice()`
  exposes them. `UiProbe` nests the slice at `backend.materializer` without overwriting
  `backend.safety.materializerRecreation`. Probe reads do not dispose. Cumulative put and
  dispose counts survive clear; resident and missing drop to 0 after a successful clear.
  Identity-safe clear still attempts each distinct identity once, including disposer
  failure, and records those attempts. `resetForTests` / `setCacheForTests` isolate
  counters so tests do not leak.
- Verification: focused
  `./scripts/with-art-env.sh test --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest'
  --tests 'artframework.sts1.PresentSafetyTest'` passed 38/38 (20 materializer, 18
  PresentSafety). Full JUnit, deploy, D1, and art-verify were not run.
- Disposition: this slice supplies the probe keys a later D1 host-recreate can assert.
  It is not itself live Texture evidence. Next parent action: `@junit-test`, then
  independent review; extend D1 only after probe review.

## Round R14 - independent review

- Session: `ses_f7493de3effeEJyJafJ0sSEaUY`
- Scope: frozen
- Result: FINDINGS
- Findings:
  - `R14-01` medium, accepted, fixed: `hitCount` is published but focused tests never assert
    a hit increment, a miss/probe no-op, or survival across clear.
- Residual risk: NRM-10 remains `verifying`. Probe/read purity and
  `backend.materializer` nesting look intact. This is not live Texture evidence.

## Round R14 - accepted finding R14-01

- Session: `ses_f7493de3effeEJyJafJ0sSEaUY` Result: FINDINGS. No new PASS or session ID is
  claimed. R13 PASS `ses_f753816c9ffe7O6YOWLHCHEvbA` stays closed.
- Scope: `Sts1AssetMaterializerTest.java` and this project record. Production hit/miss
  semantics were already correct (`BoundedTextureCache.get()` increments only on non-null
  values; `probeSlice()` is read-only; `clear()` leaves `hitCount` in place). TextureCache
  is unchanged. `disposeCount` remains attempted identities, not GL success. NRM-08/09
  remain `blocked`; NRM-10/11 remain `verifying`.
- `R14-01` accepted and fixed: focused tests now assert a non-null `get()` increments
  `hitCount`, a miss does not, `probeSlice()` does not, and `hitCount` survives successful
  `clear()` while `residentCount`/`missingCount` drop to 0.
- Verification: focused `./scripts/with-art-env.sh test --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest'` passed 21/21. Full JUnit, deploy,
  D1, and art-verify were not run. This is not live Texture/GL evidence.

## Round R14 - focused fix re-review

- Session: `ses_f7493de3effeEJyJafJ0sSEaUY`
- Scope: frozen
- Result: PASS
- Findings: none
- Residual risk: no test suite was run by the reviewer. No Texture, live GL, deployment, YAML,
  or D1 evidence is established by this PASS. NRM-10/11 remain `verifying`; NRM-08/09 remain
  `blocked`.

## Round R14 - D1 materializer verification slice

- Scope: `tools/art-verify/assert_ops.py`, `tools/art-verify/runner.py`, focused offline tests,
  verification documentation, and `tests/ui-scenarios/device/d1_full_present_combat_ready.yaml`.
- Result: verification evidence collected; independent review result not recorded because the
  review task returned no contract result. NRM-10/11 remain `verifying`.
- Tool contract: typed `capture` stores a resolved probe value and `gt_var` performs strict
  numeric comparison. Offline `python3 -m unittest discover -s tests -v` passed 62/62.
- Device fixture: the FULL combat energy path materializes the file-backed vanilla image
  `images/ui/topPanel/red/layer1.png`. The expanded scenario has 26 steps and uses relative
  counter captures, avoiding assumptions about process-global starting values.
- Device result: `scripts/art-lab combat verify-full` passed `1/1`; artifact
  `debug-artifacts/art-verify/d1_full_present_combat_ready.json` recorded pre-recreation
  `putCount=2`, `disposeCount=0`, `hitCount=135`, `residentCount=2`; after host recreation and
  rematerialization it recorded `putCount=3`, `disposeCount=2`, `hitCount=149`,
  `residentCount=1`; later cache reuse reached `hitCount=189`.
- Interpretation: the device evidence proves cache disposal attempts, rematerialization, and
  later cache hits for the active combat energy resource. `disposeCount` is not proof that the
  GL driver completed deletion, and this slice has no pixel/ART_UI assertion.
- Residual risk: independent review closure for this changed tooling/fixture slice and paired
  D1 memory/allocation/GC evidence remain open; NRM-10/11 are not complete.

## Round R14 - energy attribution write-path hardening

- Scope: frozen `Sts1AssetMaterializer.java` and `Sts1AssetMaterializerTest.java`.
- Finding `R14-D1-01-02`: accepted and fixed. `recordSuccessfulEnergyResolution` is private and
  its only production call follows `resolveTexture(result)` with a non-null texture. The guarded
  path validates energy prefix, exact requested/result identity, found status, and file-backed
  source before touching the cache. Tests seed attribution only through this path and cover null
  materialization, shared-path non-energy input, mismatched IDs, unresolved/non-file-backed input,
  and recorder visibility.
- Focused verification: clean forced `./scripts/with-art-env.sh clean test --rerun-tasks --tests
  'artframework.sts1.assets.Sts1AssetMaterializerTest'` passed **27/27**. A concurrent non-clean
  attempt failed during `compileTestJava` because build class files were missing; it had no
  semantic test failure and is superseded by the clean run.
- Independent re-review: `ses_f6bb24444ffe5zc5DasuD5yNcx`, Result `PASS`, no findings.
- Default semantic gate: `./scripts/with-art-env.sh test` passed **1212/1212**, `BUILD SUCCESSFUL`.
- Residual risk at the time: no new deployment or D1 run was performed for this fix. Historical
  NRM-10/NRM-11 status is superseded by later D1 and user-scoped closure records.

## Round R14 - lab recipe lifecycle hardening

- Scope: frozen `LabRecipeRunner.java` and `StsLabRecipesTest.java`.
- Accepted findings `R14-LAB-01` through `R14-LAB-05` were implemented: a ready dungeon room is
  not abandoned for stale character-select fields; pre-room embark is enabled and one-shot;
  seed application is checked on every path; and focused tests cover stale overlays, duplicate
  embark prevention, seed failure, no-abandon, and single seed application.
- Verification: clean focused `./scripts/with-art-env.sh clean test --rerun-tasks --tests
  'artframework.sts1.lab.StsLabRecipesTest'` passed **26/26**. Default
  `./scripts/with-art-env.sh test` passed **1214/1214**, `BUILD SUCCESSFUL`.
- Review session `ses_f5fe3e73fffea3UgflBdjMcvpa` reported three additional findings. `R14-LAB-06`
  was rejected for contradicting the frozen `LabStateSnapshot.isRunReady()` contract
  (`inGame && non-empty roomPhase`); `R14-LAB-07` was rejected as a broader L1 operation-result
  policy outside this D1 stuck-run scope; and `R14-LAB-08` was rejected because seed failure is
  intentionally terminal and the focused test proves repeated ticks do not retry it. The actual
  D1 snapshot combination and accepted one-shot/checked-seed behavior remain covered.
- D1: latest jar build/push/remote-size verification, ArtFramework enablement, and force-stop all
  passed. Fresh harness stop/start reached READY. `scripts/art-lab combat verify-full` passed
  **1/1**; artifact `debug-artifacts/art-verify/d1_full_present_combat_ready.json` shows the
  recipe completed and energy attribution is true with `putCount=2`, `hitCount=93`, and
  `residentCount=2` in the combat snapshot. No pixel/GL-driver deletion claim is made.
- Residual risk at the time: connector restart/ADB had previously been unstable; this fresh run
  recovered it. Historical NRM-10/11 status here is superseded by later review and closure records.

## R14-D1 Closure

- `R14-D1-01` is closed by adding final strict assertions to the FULL fixture and regenerating the
  artifact. The final four records are `pass` with `accepted=true`, `openInvocation=0`,
  `delegatedWithoutEvidence=0`, and `orphanArtOutput=0`.
- Offline verifier passed 62/62. Latest `scripts/art-lab combat verify-full` passed 1/1 with 30/30
  steps. Fresh independent review `ses_f5f32b64fffe5MN9tYzOgcF5Xn` returned PASS with no findings.
- NRM-10 is complete for the bounded materializer/cache lifecycle contract. The D1 counters prove
  disposal attempts, rematerialization, and cache reuse; they do not claim GL-driver deletion or
  pixel parity. The then-current NRM-11 paired-evidence blocker is superseded by the later
  user-scoped closure standard.

## NRM-11 Post-fix Arthas Sample

- A bounded post-fix D1 Arthas sample was collected in
  `debug-artifacts/nrm-11-arthas-20260915T1335Z/` after the fresh FULL-combat run passed. It records
  a 512 MiB heap, heap samples ranging 164-426 MiB, Old Gen approximately 141-159 MiB, Young GC
  count 34->41 over the dashboard window, and final heap/Old Gen of 437/159 MiB.
- Review `ses_f5ab4c287ffeaBhmPrALoE6vv1` found the sample internally useful but not comparable to
  the older isolated diagnostic: there is no matching pre-fix command set, start state, duration,
  workload window, build identity, allocation metric, or post-GC baseline.
- Superseded disposition: this review predates the user-scoped closure standard and remains only as
  evidence that no paired improvement claim may be made.

## NRM-11 Pre-fix Sampling Attempt

- A valid pre-fix jar was built from `203e525295b98ea4bf6d8d062e3f378cd3b14c23`; SHA-256
  `251cbc05b6e129a9d3843dbcacbbf7eeef9f7eea7c0d960859aa07882392aac8`, versus post-fix
  `ebbb4b810fa064ecf252b80040e9dc908122742f1e699c601c15bb4e79accef3`.
- The valid pre-fix D1 run reached READY, but the current FULL fixture failed before the matched
  workload because the old jar lacks the fresh `ART_COMMAND` result contract. The fallback Arthas
  sample in `debug-artifacts/nrm-11-arthas-20260915T-pref-valid/` is therefore a valid old-jar JVM
  observation, not a matched fixture pass.
- The earlier `debug-artifacts/nrm-11-arthas-20260915T-pref/` sample is explicitly invalid because
  the jar SHA matched the post-fix jar.
- Independent review `ses_f5a2f5429ffe1FX2GG4ZuEucqG` returned findings: no before/after allocation,
  GC cadence, or retained-memory conclusion is defensible from the available samples. That
  paired-evidence blocker is superseded by the user-scoped closure standard below; D1 was restored
  to the post-fix jar and the standard FULL fixture passed again.

## NRM-11 User-Scoped Closure Standard

- User decision: equivalent pre-fix stress evidence is unavailable; close NRM-11 only on preserving
  normal original-game operation under current ART changes.
- Closure is therefore scoped to current-build semantics, D1 FULL strict acceptance, materializer
  lifecycle completion, bounded post-fix Arthas observations, and negative-inventory review. No
  before/after allocation or GC improvement is claimed.

## NRM-11 Final Integrated Review

- Review `ses_f5a07cb1affeOWBkm00OV3dHmG` found the supplied current-build evidence consistent and
  identified only stale historical `verifying` language. That language is now marked superseded.
- Final re-check in `ses_f5a07cb1affeOWBkm00OV3dHmG` returned PASS with no findings.
- NRM-11 is complete under the user-scoped standard. Residual non-claims remain explicit: no paired
  allocation-rate improvement, no GL-driver deletion proof, no pixel parity proof, and no long-run
  heap proof beyond the bounded post-fix Arthas window.

## Round R15 - NRM-08 unified RenderPlan reconciliation

- Contract frozen: C1/C2 are component and render-system distinctions, not branches in the host
  reconcile kernel. ECS/presentation state remains authoritative; each system supplies an immutable
  desired `RenderPlan` plus its managed existing target ids, and host targets/bindings remain cache.
- Implemented target id+kind retention with mutable field synchronization, fail-fast kind mismatch,
  stale plan-owned removal, manual-overlay preservation, and destructive explicit recreation.
- Implemented ordered binding identity retention by target/index/effect/layer with complete
  parameter/enabled synchronization and per-target list replacement on identity/order mismatch.
  The initial active-surface compatibility implementation delegated to the full-plan path; the D1
  correction below supersedes that scope choice without changing the generic identity algorithm.
- Focused verification passed 34/34:
  `./scripts/with-art-env.sh test --tests 'artframework.render.RenderPlanRebuildTest' --tests
  'artframework.render.RenderHostTest'`.
- Disposition: implementation is `verifying` pending independent review. No paired allocation
  improvement is claimed and NRM-08 is not complete.

### R15 findings disposition

- `R15-01` high, accepted and fixed: complete plan structure and all known-effect candidate bindings
  are staged and validated before stale removal, target field updates, binding synchronization, or
  ownership changes. Invalid retained and new attachment rollback tests preserve identities/values.
- `R15-02` medium, accepted and fixed: every duplicate target id, including same-kind duplicates,
  is rejected deterministically before mutation. The focused test constructs the private immutable
  plan reflectively and invokes the private reconciliation boundary; no production test API was added.
- `R15-03` medium, accepted and fixed: unknown planned effects are explicitly skipped before ordered
  identity comparison. Valid bindings retain order/identity, and registration plus rebuild may
  realize the formerly unknown effect. Other validation failures remain atomic.
- `R15-04` medium, accepted and fixed: `EffectBinding` publishes immutable parameter snapshots via a
  volatile reference, uses volatile enabled state, and copy-and-swap writes. A bounded concurrent
  reconcile/read test observes complete paired marker values without exceptions.
- Verification: focused `RenderPlanRebuildTest` + `RenderHostTest` passed 38/38. NRM-08 remains
  `verifying` pending independent re-review; no paired allocation improvement is claimed.

### R15 D1 active-surface regression and correction

- Deployed jar SHA `fbf4d6fd...`; `scripts/art-lab combat verify-full` failed only
  `backend.handRender.ok`. Hand projection/render counts were 5/5 with `sampleFrames=120`, but
  `p95Nanos=18,682,000` exceeded `16,666,667`.
- Root cause: the per-STS-render active-surface adapter called `RenderPlan.fromEcs(activeSurfaceIds)`,
  repeatedly materializing and reconciling unrelated native/entity/C1/full-frame entries.
- Corrected contract: unified means one validated/staged track-agnostic reconcile algorithm, not one
  full plan per system call. Full projection manages the complete plan-owned set. Active-surface
  projection builds `RenderPlan.fromActiveC2Surfaces(...)` and retains its own managed-id set;
  unrelated full-frame/entity/C1/manual targets are not scanned, staged, or updated.
- Focused tests prove active identity retention, stale active removal, unrelated target preservation,
  and the unchanged generic rollback/duplicate/unknown/concurrency semantics. D1 rerun and independent
  re-review remain pending; NRM-08 stays `verifying`.

### NRM-08-01 ownership correction

- `NRM-08-01` accepted and fixed: reconciliation registers entries only in the caller-provided
  managed ownership set. Stale removal drops that owner first and removes the target only when no
  other ownership set retains the identity; no C1/C2 branch is used.
- Focused coverage adds full-plus-active overlapping ownership: active stale removal preserves the
  target while full projection still owns it, and full stale removal subsequently removes it.
- NRM-08 remains `verifying`; D1 rerun and independent re-review are still pending.

### NRM-08-02 / NRM-08-03 synchronization correction

- `NRM-08-02` accepted and fixed: full and active RenderPlan snapshots are now constructed while
  holding the same `RenderHost` monitor used by reconciliation, preventing an older snapshot from
  applying after a newer one.
- `NRM-08-03` accepted and fixed: destructive cache clear/recreation operations and `clearTargets`
  use that same reentrant monitor boundary. Existing ownership overlap, rollback, duplicate,
  unknown-effect, concurrency, and identity semantics remain covered by the focused suite.
- NRM-08 remains `verifying`; no generation scheme was added because monitor serialization is the
  established lifecycle contract. D1 rerun and independent review remain pending.

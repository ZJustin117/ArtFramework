# Native Render Memory Lifecycle Refacter

## Authoritative Status

NRM-01 through NRM-06, NRM-08, NRM-09, NRM-10, and NRM-11 are complete under the
recorded contracts. NRM-07 remains `verifying`: the derived-plan cache is implemented
and tested, but no paired allocation improvement claim is made. Earlier checkpoint and
review paragraphs that describe NRM-08/09/10/11 as blocked or verifying are historical
records and are superseded by the closure sections and the ledger table.

## Goal

Bound native-render evidence memory independently of process uptime, then reduce allocation
pressure in FULL presentation without weakening invocation correlation, strict NRCC acceptance,
native fail-open behavior, or visible output.

The work has two ordered tracks:

1. Stop retained-heap growth by separating open correlation state, cumulative counters, bounded
   recent diagnostics, and recovery-race tombstones.
2. After retained state is proven bounded, reduce repeated per-frame materialization and probe
   serialization using measured allocation evidence.

## Non-goals

- Do not change STS combat, room, input, effect-queue, or native renderer authority.
- Do not change which surfaces may return `DELEGATE_TO_ART` or suppress native rendering.
- Do not weaken exact invocation/entity/frame evidence correlation or strict error reporting.
- Do not use periodic global clears, time-only cleanup, or unbounded compatibility archives.
- Do not combine ledger lifecycle, frame-plan caching, probe throttling, transient effects, and
  texture disposal in one implementation slice.
- Do not introduce downstream protocol, party, or combat types.

## Architecture Authority

- [`native-render-coverage-sdd.md`](../../design/native-render-coverage-sdd.md) defines native
  invocation, disposition, draw-evidence, fail-open, and strict acceptance semantics.
- [`c2-full-present.md`](../../design/c2-full-present.md) defines FULL presentation ownership and
  host lifecycle.
- `NativeRenderLedger` is the sole writer and source of truth for open invocation correlation,
  terminal classification, cumulative native-render counters, and strict error counters.
- `NativeRenderBridge` may retain only pending callback tokens grouped by owner; it must not become
  a second evidence history or terminal-state authority.
- `TransientEffectLedger` remains a separate lifecycle authority and receives its own later slice.
- Frame-plan and probe caches are derived, bounded caches. They may be recreated from current
  presentation state and must never become presentation authority.

## Target Lifecycle

```text
record invocation
-> record exactly one disposition
-> native continuation closes immediately
   OR delegated output waits for exact entity/frame evidence
   OR fallback/transition/recovery explicitly closes
-> classify strict outcome and increment cumulative counters once
-> move only bounded diagnostic data to recent history/tombstone storage
-> remove heavyweight correlation state
```

The runtime state is split by purpose:

- `open`: full invocation/disposition data required for current correlation.
- `totals`: O(1) cumulative counts used by probes and acceptance reports.
- `recent`: fixed-capacity completed diagnostic records; eviction cannot change totals or strict
  acceptance.
- `recovery tombstones`: minimal, bounded IDs/outcomes required to settle one late disposition or
  callback after recovery.

`invocationCount`, `dispositionCount`, `evidenceCount`, and disposition-mode counts continue to
mean cumulative counts since reset. `openInvocation` and new retained/recent fields report current
storage. ID-based detail lookup is guaranteed for open records and records still present in the
documented recent window, not for the full process history. This compatibility contract must be
frozen by NRM-01 tests and review before production storage changes.

The frozen negative and recovery semantics are:

- Invocation IDs are strictly increasing and non-reusable within one reset epoch. The ledger keeps
  only a high-water mark, so an evicted terminal ID cannot be registered again.
- `invocations()` and `dispositions()` return bounded queryable snapshots (open plus recent) and
  their sizes are not cumulative counts.
- A normal terminal outcome is immutable. Duplicate fallback/close/evidence and evidence arriving
  after fallback, cancellation, or recovery are rejected without a second terminal accounting.
- A recovery fail-open inserted before the original disposition commit is provisional. A bounded
  tombstone may settle exactly one late disposition and remove that provisional strict gap; it does
  not accept late draw evidence.
- Outside the recent/tombstone window, a callback for an old ID is stale/orphan input and cannot
  recreate correlation state or change successful evidence totals.
- `delegatedWithoutEvidence` is the sum of currently open delegated gaps and cumulative terminal
  missing-evidence errors. Successful evidence and transition/recovery cancellation do not add a
  terminal gap; delegated fallback and non-cancellation recovery do. Diagnostic eviction never
  changes this value.
- Existing `int` count APIs and probe value types remain compatible in this refactor. Counter-width
  expansion is out of scope and must be a separate API change.

## Ordered Review Tree

1. Public/package-visible ledger query and probe compatibility contract.
2. Terminal classification and one-time cumulative accounting.
3. Exact evidence correlation and bounded recent diagnostic retention.
4. Recovery, transition cancellation, fallback, duplicate, orphan, and late-callback races.
5. Bridge pending-token retirement and negative inventory for duplicate histories.
6. Transient-effect terminal record reclamation.
7. Measured frame-plan and draw-path allocation ownership.
8. Probe heartbeat/full-snapshot separation and serialization frequency.
9. Texture/materializer disposal lifecycle as an independent resource slice.
10. Integrated semantic, D1, and memory evidence closure.

Each row is reviewed independently. A child review that discovers a separate ownership concern
adds a ledger row rather than expanding the active slice.

## Verification Policy

- Start NRM-01 through NRM-05 with focused JUnit 4 tests in `NativeRenderLedgerTest`,
  `NativeRenderBridgeTest`, `NativeRenderDelegatedEvidenceCorrelationParameterizedTest`, and
  `NativeRenderLifecycleCleanupMatrixTest` as applicable.
- Add a high-volume pure-Java test that closes substantially more records than the configured
  recent/tombstone capacities and proves retained counts remain bounded while cumulative counts and
  strict results remain correct.
- Delegate the default semantic gate after each coherent runtime slice:
  `./scripts/with-art-env.sh test`.
- Run offline `art-verify` only if verifier code or YAML is changed.
- For ledger/bridge changes reaching STS render hooks, build and deploy after JUnit, then run
  `scripts/art-lab combat verify-full` on D1.
- Use Arthas only for diagnostic evidence. For NRM-11, the user explicitly accepted that an
  equivalent pre-fix stress workload cannot be provided; closure is based on current ART-change
  operational health for original gameplay, not a before/after performance-improvement proof.
- Allocation optimizations remain best-effort diagnostic observations unless a comparable workload
  exists. Do not claim measured allocation-rate improvement without paired evidence.

## Completion Definition

The refactor is complete only when:

- every ledger row is `complete` with direct after evidence, focused tests, applicable full gates,
  and review closure;
- cumulative invocation/disposition/evidence counts can grow while retained ledger and tombstone
  storage stay within documented fixed bounds;
- open delegated invocations remain strict gaps until exact evidence or an explicit cleanup outcome;
- duplicate, orphan, mismatch, transition, fallback, and recovery-race behavior remains covered;
- D1 FULL combat keeps strict acceptance and no pending callback queue grows without bound;
- bounded post-fix D1 Arthas observation shows original gameplay remains operational under ART
  changes, with no Old GC and no obvious runaway heap/Old Gen failure during the sampled window;
- allocation-track measurements are recorded as diagnostic only unless a matching baseline exists;
- final negative inventory finds no second unbounded native-render/effect history in scope.

## Checkpoint

- State: NRM-01 through NRM-06 are complete: focused/default JUnit, D1 FULL combat, and R6 review
  closure cover the bounded lifecycle ownership changes. NRM-07 and the pure/testable portion of
  NRM-10 are implemented and default-JUnit verified. Current-source reconciliation found no open
  R9-01/R9-02/R9-03 source or test finding, but no independent R9 re-review result is recorded.
  NRM-08 has a completed narrow value-equal ECS-writer no-op sub-boundary covering surface, effects,
  full-frame, capture, and C1 bounds. Accepted findings R10-01/R10-02/R10-03 are fixed,
  JUnit-verified, and independently reviewed PASS. The broader NRM-08 RenderHost contract is now
  frozen and implemented as one generic differential RenderPlan reconcile kernel over each calling
  system's local desired plan and managed target ids. R15 findings are
  addressed with whole-plan validation staging, deterministic duplicate-id rejection, explicit
  unknown-effect skipping, and concurrent-safe immutable binding snapshots. Focused verification
  passes. Deployed jar `fbf4d6fd...` exposed a D1 active-surface regression: FULL combat failed only
  `backend.handRender.ok` with 5/5 projected/rendered cards and 120 samples because p95 was
  18,682,000 ns over the 16,666,667 ns limit. The active adapter no longer builds a full ECS plan;
  The corrected jar passed focused tests 39/39, default JUnit 1227/1227, final D1 FULL combat 1/1,
  and independent review `ses_f51004cfbffexhGwjsMJJIHQ4p` with no findings. NRM-08 is `complete`;
  no paired allocation improvement is claimed. NRM-09 remains contract-blocked.
- Active row: NRM-11 integrated closure now follows the user-scoped operational-health standard:
   preserve original gameplay performance under current ART changes rather than proving a paired
   pre/post performance improvement. NRM-07's current cache-hit readiness sample is a
  packed primitive, so it does not allocate a snapshot object; this remains source-level evidence,
  not a live-host allocation claim. Reinspection of `UiProbe`, `ProbeSidecar`, `StageHost`, their
  existing tests, and the probe/design compatibility docs did not resolve the then-open NRM-08/09
  host-target/probe decisions. The later NRM-08 slice freezes and implements its host-target
  contract; NRM-09 remains unresolved. Historical text here found no additional safe source/test change for
   NRM-07/10/11 without then-outstanding independent/live/paired evidence; NRM-10 and NRM-11 are
   superseded by later closure records. The authoritative
   post-fix D1 EntityPresent result is recorded below. The earlier run exposed an actionable
   command-thread defect: libGDX reported no current context on `agent-session`, `StageHost` returned
   false, and the probe truthfully retained `failed: StageHost` while the command itself reported OK.
   `PresentSafety` now dispatches the ordered recreation lifecycle to the libGDX application thread
   before touching StageHost, RenderHost, materializer, or projection state; StageHost also retains
   a specific terminal phase/type/message instead of collapsing every failure to a boolean.
- NRM-06 changed only the transient-effect ledger/adapter admission path, its focused tests, and
  project records. Pre-existing worktree edits outside this task scope remain untouched.
- Prior review session `ses_f891fcb98ffe3XjMwTe4c5AFip` was blocked because this project,
  ledger row, frozen scope, and authority boundary did not yet exist. It supplied no source finding.

## Post-fix D1 evidence

- Authoritative `d1_entity_present_smoke` rerun: 1/1 scenario and 16/16 steps passed.
- After `art lab host-recreate`, the probe reported `backend.safety.c1HostRecreation=rebuilt`,
  `entities.slotCount=1`, `backend.entityDraw.count=1`, and target
  `c2_entity_art-lab-entity enabled=true`.
- Cleanup left draw count 0, slot count 0, and target `c2_entity_art-lab-entity` absent.
- JSON: `/home/justinz/ArtFramework/debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.json`.
- Log: `/home/justinz/ArtFramework/debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.latest.log`.
- This supplies no Texture-disposal/rematerialization evidence and no pixel or `ART_UI` proof.
- NRM-10 remains `verifying` pending real Texture lifecycle evidence or independent review. No real
  R12 review result is recorded, so no R12 PASS or session ID is claimed.
- Superseded NRM-11 status: this checkpoint predates the user-scoped closure standard. Final NRM-11
  disposition is now based on current ART-change operational health, not paired improvement proof.
- NRM-08's generic target/binding identity, caller-managed stale removal, manual-overlay preservation,
  active-system local plan, and destructive-recreation contracts are implemented. Focused tests pass;
  NRM-08-01 also preserves a target when another system ownership set still retains its identity.
  NRM-08-02/03 serialize plan snapshot construction, reconciliation, and destructive cache
  recreation on one RenderHost monitor. Focused/default JUnit, final D1 FULL combat, and independent
  review all pass. No paired allocation improvement is claimed.
- NRM-09 remains blocked pending an owning writer clock, heartbeat schema/version and fields,
  full-snapshot cadence, freshness marker, consumer fallback/merge behavior, and schema migration.

## NRM-09 Verification

NRM-09 is implemented under the user-approved contract: `StageHost` owns a synchronized,
injectable-clock publisher with 500ms heartbeat and 5000ms full cadence from `postUpdate` only;
explicit `art probe` publishes immediately. `ART_PROBE ` v1 remains unchanged, while
`ART_HEARTBEAT ` schema v1 is written to `art_heartbeat_latest.log`; consumers use heartbeat for
liveness and the separate latest full file for payload. R15-05/06/07 are fixed: full metadata commits
only after snapshot plus persistence success, explicit construction is serialized inside the
 publisher, and sidecar replacement uses Java 8 `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`.
 Focused JUnit passed 40/40 and the default JUnit gate passed 1239/1239. Corrected jar
 `e1fcff48bde3c6d6bde730e7e6f7208aa4e7de6a44e32e462246f9bb17f5faf1` was deployed to D1 and
 standard FULL combat passed 1/1. D1 sidecar sampling confirmed complete one-line schema-v1 JSON:
 heartbeat 302 bytes with sequence 18987 and probeSequence 1926; full probe 185222 bytes with
 representative frame 286686. NRM-09 is `complete`; no paired allocation or GC improvement claim
 is made.

Historical D1 follow-up: jar `df586...` reached READY, but standard combat verification failed with
`no fresh ART_COMMAND result for lab command`. Android `File.renameTo` did not replace an existing
sidecar destination, leaving the command/probe latest files stale after the first write. The
same-directory commit path now uses Java 8 `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` and fails
closed on unsupported atomic replacement. Focused/default JUnit pass; no corrected-jar D1 result
has been recorded. The corrected jar resolved this issue; the historical failure remains recorded
for traceability.

## Next Action

R14 adds additive materializer diagnostic counters under `backend.materializer`
(`residentCount`, `missingCount`, `putCount`, `disposeCount`, `hitCount`). They are
read-only cache diagnostics, not GL/Texture success. NRM-10 remains `verifying`.
R13 remains independently reviewed PASS `ses_f753816c9ffe7O6YOWLHCHEvbA` and is not
reopened. Next: parent can deploy and extend D1 assertions against those probe keys
after independent review of this probe slice; do not mark NRM-10 complete without
real Texture disposal/rematerialization evidence.

R14-01 is accepted and test-fixed in `Sts1AssetMaterializerTest`: non-null `get()`
increments `hitCount`, misses and `probeSlice()` do not, and `hitCount` survives
successful clear while resident/missing drop to 0. Production hit/miss semantics
were already correct. Focused fix re-review `ses_f7493de3effeEJyJafJ0sSEaUY`
returned PASS with no findings. The reviewer did not run tests, and this remains no live
Texture/GL/D1 evidence. Then-current next step was to run the default JUnit gate and verify bounded
D1 assertions for disposal/rematerialization; that NRM-10/11 status is superseded by the later
user-scoped closure records. NRM-08 is verifying; NRM-09 remains blocked.

The R14 D1 slice is now executed. `capture`/`gt_var` support passed the offline art-verify
suite 62/62, and the expanded 26-step `d1_full_present_combat_ready` passed 1/1 through
`scripts/art-lab combat verify-full`. The artifact records pre-recreation materializer counters
`put=2`, `dispose=0`, `hit=135`, `resident=2`, followed by post-recreation/rematerialization
`put=3`, `dispose=2`, `hit=149`, `resident=1`, and later `hit=189`. This is direct D1 cache
disposal-attempt/rematerialization/reuse evidence for the combat energy image; it does not prove
GL-driver deletion or pixel correctness. Then-current next step was independent review closure and
paired D1 memory/allocation/GC evidence; that NRM-10/11 status is superseded by later closure
records. NRM-08 is verifying; NRM-09 remains blocked.

R13 materializer fail-open coverage is independently reviewed PASS
`ses_f753816c9ffe7O6YOWLHCHEvbA`. Next: obtain real Texture disposal/rematerialization evidence
for NRM-10; do not mark NRM-10 complete without it. Also obtain independent re-review closure
for the frozen R9 lifecycle slice and NRM-07 cache slice; do not mark either complete without
the applicable live evidence. Preserve the independently reviewed PASS
R10-01 through R10-03 ECS-writer sub-boundary as complete without conflating it with the broader
NRM-08 row. The later NRM-08 slice freezes and implements target-identity preservation,
stale plan-owned removal, effect-binding reuse, and destructive lifecycle invalidation semantics.
NRM-09 is blocked because `UiProbe` freezes one schema-v1 full snapshot while `StageHost` merely has
an implementation-local 30-callback sidecar interval shared by post-update and post-render;
`ProbeSidecar` only overwrites two latest-snapshot files. No heartbeat schema, full-snapshot cadence,
freshness marker, owning writer clock, consumer fallback/merge rule, or schema-version migration rule
exists. NRM-10 bounds value and missing-key storage, uses identity-safe exactly-once
disposal, and adds a pure cache seam; post-fix live host recreation is evidenced above. Historical
NRM-11 text in this checkpoint required paired D1 memory/allocation and long-run allocation evidence;
that requirement is superseded by the user-scoped operational-health standard below. The registry
pending queue is bounded and coalesced; its clear-all marker now removes only transient `effect:`
entities even beyond queue capacity, and ledger query snapshots are immutable. NRM-08 is verifying;
NRM-09 remains blocked on its probe contract.

R12 admission hardening now makes dispatch acceptance explicit: scheduled and synchronous fallback
are accepted, while immediate dispatcher rejection/throw is returned to the command as an error and
recorded in the probe without running lifecycle steps. Accepted command results say only `requested`;
terminal rebuilt/failure remains probe-owned. This historical note predates later NRM-10 completion
and user-scoped NRM-11 closure. The later NRM-08 contract supersedes its earlier blocker; NRM-09 is unchanged.

R12's safe dispatch-admission and command-path slice is now implemented: host recreation distinguishes
scheduled, synchronous fallback, and dispatcher failure; the lab command reports requested semantics
without waiting, and rejects are errors. The authoritative post-fix D1 rerun is recorded at
`debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.json` with companion log
`debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.latest.log`: 1/1 scenario
and 16/16 steps passed, including rebuilt C1 status, one slot/draw/enabled target after recreation,
and zero draw/slot/target after cleanup. This still provides no Texture-disposal or pixel/ART_UI
proof; the then-current NRM-10/11 verifying status is superseded by later closure records, while
NRM-08 is verifying under its frozen contract; NRM-09 remains blocked on its exact contract.

R13 adds focused coverage that a materializer cleanup failure still executes the final ECS
projection. Fail-open ordering remains presentationState, c1Host, renderHost, materializer,
projection. The public `onHostRecreated()V` wrapper still does not wait. A dedicated
`materializerRecreation` probe field records a truthful materializer failure as
`failed: ` plus the exception simple name, instead of letting a later projection imply
rebuilt/success. Accepted R13-01/R13-02 keep that field lockstep with `c1HostRecreation` for
  scheduled and dispatcher-reject/throw admission. Focused fix re-review
  `ses_f753816c9ffe7O6YOWLHCHEvbA` Result: PASS. Post-fix default JUnit passed 1199/1199.
  Historical NRM-10/11 verifying status from this R13 checkpoint is superseded by later closure
  records; NRM-08 is verifying and NRM-09 stays blocked.

R14 adds additive `backend.materializer` counters (`residentCount`, `missingCount`,
`putCount`, `disposeCount`, `hitCount`) without overwriting
`backend.safety.materializerRecreation`. Probe reads do not dispose. This historical note is
superseded by later D1 materializer evidence and user-scoped NRM-11 closure; NRM-08 is verifying and NRM-09 stays blocked.

R14 adds additive `backend.materializer` counters (`residentCount`, `missingCount`,
`putCount`, `disposeCount`, `hitCount`) without overwriting
`backend.safety.materializerRecreation`. Probe reads do not dispose. This duplicate historical note
is superseded by later D1 materializer evidence and user-scoped NRM-11 closure; NRM-08 is verifying
and NRM-09 stays blocked.

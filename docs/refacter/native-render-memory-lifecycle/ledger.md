# Native Render Memory Lifecycle Before/After Ledger

The table status is authoritative. Older addenda below preserve historical states and
must be read as superseded when they conflict with a later closure entry.

| ID | Scope | Before | Target after | Evidence before | Evidence after | Tests | Review | Status |
|---|---|---|---|---|---|---|---|---|
| NRM-01 | Ledger compatibility and authority contract | `NativeRenderLedger` uses the same unbounded maps both as active correlation state and complete process history; count and ID-query methods expose that storage directly | Counts/modes are cumulative `int` values since reset; retained/open/recent are separate; collections and ID lookup expose only open+recent; IDs strictly increase per reset; terminal state is immutable; recovery tombstones settle one late disposition but never late evidence; `delegatedWithoutEvidence` is open gaps plus cumulative terminal missing-evidence errors | Source and prior R1 findings established the unbounded-history and undefined-query baseline | Contract frozen in README and implemented in `NativeRenderLedger`; focused ledger/bridge/correlation tests pass, including reset, high-water, bounded query, exact correlation, and strict-gap semantics | `NativeRenderLedgerTest`, `NativeRenderBridgeTest`, `NativeRenderDelegatedEvidenceCorrelationParameterizedTest`, `NativeRenderLifecycleCleanupMatrixTest`; default `./scripts/with-art-env.sh test` and D1 FULL combat pass | R1 fixes verified; R6 final review PASS | complete |
| NRM-02 | Terminal accounting and active-state reclamation | PASS/CAPTURE/FAIL_OPEN invocations become `COMPLETE` but remain in invocation/disposition/state maps forever | Every terminal path increments cumulative totals exactly once and removes heavyweight active correlation data without changing strict results | Arthas samples observed invocation/disposition maps growing about 570 entries/second during combat; source terminal writes only change `State` | Terminal accounting is O(1) and active correlation is reclaimed; bounded-history and one-time accounting tests pass; default gate and D1 FULL combat pass | `NativeRenderLedgerTest` high-volume/duplicate/reset cases plus bridge and lifecycle tests; default `./scripts/with-art-env.sh test` passed | R6 final review PASS | complete |
| NRM-03 | Delegated evidence and recent diagnostics | Delegated invocation, disposition, and evidence remain forever after exact evidence closes them | Exact ID/entity/frame validation occurs before closure; completed detail enters a fixed-capacity recent window; eviction changes neither totals nor acceptance | `recordEvidence` previously retained all completed records indefinitely | Exact correlation and bounded recent diagnostics implemented; eviction leaves cumulative counts and strict report stable; focused/default gates and D1 FULL combat pass | `NativeRenderLedgerTest`, parameterized correlation tests, bridge tests; default `./scripts/with-art-env.sh test` passed | R6 final review PASS | complete |
| NRM-04 | Recovery/fallback/transition race lifecycle | Recovery relies on completed entries plus unbounded `recoveryDispositions` and `transitionCancelled` sets to recognize late outcomes | Recovery/fallback/transition closes once; minimal tombstones are capacity/frame bounded; one allowed late recovery disposition settles correctly; later duplicates/orphans remain detectable by documented semantics | Prior source retained complete records and marker IDs | Recovery tombstones, fallback, cancellation, duplicate, orphan, and late-disposition behavior implemented and covered; default gate and D1 FULL combat pass | `NativeRenderLedgerTest`, `NativeRenderBridgeTest`, lifecycle cleanup matrix; default `./scripts/with-art-env.sh test` passed | R6 final review PASS | complete |
| NRM-05 | Bridge callback token ownership | Surface/skeleton queues and ledger both reference invocation IDs; cleanup ordering can leave stale callbacks | Bridge queues retain only currently pending delegated tokens and retire exact IDs on evidence, fallback, transition, and recovery; no duplicate history exists outside ledger | `SURFACE_INVOCATIONS` and `SKELETON_INVOCATIONS`; bridge evidence methods query ledger detail before queue removal | Bridge-wide publication/recovery lock prevents terminal invocation tokens from being published after recovery; atomic fallback admission accepts only OPEN delegated records; stale/recent/terminal/non-delegated callbacks are orphan diagnostics with no evidence | `NativeRenderBridgeTest`, `NativeRenderLedgerTest`, `NativeRenderDelegatedEvidenceCorrelationParameterizedTest`, `NativeRenderLifecycleCleanupMatrixTest`, and `Sts1SkeletonBridgeTest`; default gate and D1 FULL combat pass | R1-09/R1-10 fixed; R6 final review PASS | complete |
| NRM-06 | Transient-effect ledger lifecycle | Completed/disposed effect records stay in `TransientEffectLedger.records` until broad recovery/reset | Active records remain authoritative; terminal records contribute cumulative diagnostics and are reclaimed into a fixed recent window; probe separates active/recent/total/evicted; late render cannot mutate terminal state or recreate active state | `complete`/`dispose` only set state; `records` is cleared only by recovery/reset; current D1 sample was stable at 106 and is not the primary leak; R2-01 found render admission could still mutate recent records or implicitly recreate after cleanup | Active `records` retains only open effects; terminal records move to the configurable recent window (default 256); render admission is explicit and rejects recent/evicted/recovery-cleared identities; bounded stale identity markers cover cleanup/eviction; clear-all is a one-bit bounded marker that removes only transient entities; query records are immutable snapshots | `TransientEffectLedgerTest`, `TransientEffectLifecycleAdapterTest`, `TransientEffectContainerPatchesTest`, `NativeRenderBridgeTest`, and `NativeRenderLifecycleCleanupMatrixTest`; latest combined lifecycle run 60/60; default gate and D1 FULL combat pass | R2-01 and R6-01/R6-02 fixed; R6 final review PASS | complete |
| NRM-07 | Frame render-plan and draw-snapshot allocation | Native callback, prepare, draw, count, and probe paths may repeatedly materialize plans/lists in one frame | `Sts1RenderPipeline` reuses one derived immutable `SurfaceDrawPlan` and precomputed `drawOrder` only when frame, policy, executor/readiness, scene, mount, overlay, and panic inputs match; reset invalidates it | Prior Arthas dashboard showed Eden growing 200-300 MiB within seconds and Young GC roughly every 3.9 seconds; no allocation baseline was available | The hit path compares primitive frame/revision/flag fields and scene directly against one retained key; it creates no per-hit key, readiness snapshot object, or varargs array. R8-01 uses one packed primitive readiness/panic sample for both key and construction; R8-04 asserts frame identity invalidation while current listed presentation inputs drive exact output. Prior synthetic allocation evidence remains unchanged and is not D1 evidence. | `./scripts/with-art-env.sh test --tests 'artframework.sts1.render.Sts1RenderPipelineTest'` passed 29/29 on 2026-09-09; `./scripts/with-art-env.sh test` passed 1189/1189 | No independent allocation review; R8 findings disposition recorded; synthetic benchmark does not establish live/D1 allocation or GC behavior | verifying |
| NRM-08 | Projection/ECS/render-target differential updates | Unchanged native values could rebuild complete target and binding collections despite unchanged desired identity; the initial active-surface compatibility change also forced a full ECS plan through every STS render pass | ECS/presentation state remains authority. One generic track-agnostic reconcile kernel accepts each system's immutable desired `RenderPlan` and caller-managed ownership set. Target identity is id+kind; binding identity is target id+ordered index+effect id+layer. Equal identities retain cache objects; duplicates/kind mismatch fail before mutation; stale removal drops only the caller's owner and removes the target only when no other owner remains; manual overlays and other systems' targets survive; explicit recreation is destructive. Snapshot construction, reconciliation, and destructive cache operations share one RenderHost monitor | Deployed jar `fbf4d6fd...` failed only `backend.handRender.ok`: hand projected/rendered 5/5 with 120 samples, but p95 18,682,000 ns exceeded 16,666,667 ns. The active adapter called `RenderPlan.fromEcs(activeSurfaceIds)`, materializing unrelated native/entity/C1/full-frame entries per STS render pass | Unified reconciliation retains staged validation, unknown-effect compatibility, and immutable concurrent-safe binding snapshots. Full projection manages the complete plan-owned set; active projection uses `fromActiveC2Surfaces` with its own retained managed-id set, so unrelated targets are not scanned, staged, or updated. NRM-08-01 overlap preservation and NRM-08-02/03 monitor serialization plus rollback/duplicate/unknown/concurrency and active identity/stale/unrelated tests pass. D1 rerun is pending; no paired allocation improvement is claimed. | `./scripts/with-art-env.sh test --tests 'artframework.render.RenderPlanRebuildTest' --tests 'artframework.render.RenderHostTest'` pending rerun | R15-01 through R15-04 and NRM-08-01/02/03 fixed; D1 regression correction awaits rerun and independent re-review | verifying |
| NRM-09 | Probe cadence and payload allocation | Automatic probe periodically built a complete Map/List/JSON tree from a shared 30-callback post-update/post-render counter | `StageHost` owns one post-update monotonic publisher: 500ms lightweight heartbeat, 5000ms compatible full snapshot, 2000ms stale marker, explicit full publication remains immediate, and old ART_PROBE v1 remains compatible | The old path called full `ArtFramework.probe().toJsonLine()` every 30 callbacks and overwrote one latest file; no heartbeat, freshness, or separate payload contract existed | `ART_HEARTBEAT` schema v1 and `art_heartbeat_latest.log` are separate from `ART_PROBE`/`art_probe_latest.log`; atomic same-directory replacement, success-aware freshness, bounded retry, and serialized explicit publication are implemented. D1 sampling confirmed complete one-line JSON, heartbeat 302 bytes and sequence advancement over 1.2/6 seconds, full probe 184058 bytes stable at 1.2 seconds then advancing within 6 seconds; explicit `art probe` succeeded | Focused JUnit 40/40; default `./scripts/with-art-env.sh test` 1239/1239; offline art-verify 62/62; standard D1 FULL combat 1/1 | R15-05/06/07 fixed; final R15 review PASS; corrected-jar D1 sampling complete | complete |
| NRM-10 | Asset materializer resource lifecycle | Texture and missing-path caches had no disposal on clear | `Sts1AssetMaterializer.clearCache()` disposes each cached Texture once; pure `BoundedTextureCache` seam covers hit/miss-compatible storage, clear/recreate, replacement/eviction, exactly-once dispose, and fallback/missing markers | Source risk identified in `Sts1AssetMaterializer`; not established as the Java-heap leak in the combat sample | Production clear disposes cached textures; pure cache bounds both value and missing-key storage, detaches state before cleanup, attempts each distinct identity despite disposer failure, and leaves `dispose()` terminal. `PresentSafety.onHostRecreated()` failure-isolates presentation-state reset, C1/RenderHost cleanup, materializer cleanup, and final ECS projection, dispatches that ordered lifecycle to the libGDX application thread, and exposes dispatch admission/rejection. Authoritative post-fix D1 `debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.json` and `.latest.log` report 1/1 scenario and 16/16 steps passed; the probe reports `backend.safety.c1HostRecreation=rebuilt`, then one entity slot, one entity draw, and enabled target `c2_entity_art-lab-entity`, followed by cleanup with draw count 0, slot count 0, and target absent. No Texture-disposal or pixel/ART_UI proof exists. R14 adds additive diagnostic counters under `backend.materializer` (`residentCount`, `missingCount`, `putCount`, `disposeCount`, `hitCount`); these are cache diagnostics, not GL success, and do not overwrite `backend.safety.materializerRecreation`. | 2026-09-09 focused command passed 43/43; full `./scripts/with-art-env.sh test` passed 1194/1194; post-fix D1 EntityPresent scenario passed 1/1 and 16/16. R13-01/R13-02 record materializer fail-open as `failed: ` plus exception simple name and lockstep scheduled/dispatcher probe fields; focused PresentSafetyTest passed 18/18. R14 focused materializer/PresentSafety verification is recorded in reviews.md. | Dispatch admission and command error mapping are JUnit-covered; R13 materializer-failure continuation is JUnit-covered; R14 probe counters are JUnit-covered; independent re-review and real Texture disposal/rematerialization evidence remain required | verifying |
| NRM-11 | Integrated verification and closure | Existing semantic tests and one 180-second diagnostic establish the failure mechanism but not the repaired long-run behavior; an equivalent pre-fix stress workload is unavailable under the current fixture contract | Under the user-scoped standard, current ART changes preserve original-game operation: full JUnit, applicable D1 FULL combat, bounded post-fix Arthas observation, NRM-10 resource lifecycle closure, negative inventory, and final review prove operational health without claiming paired performance improvement | Heap limit 512 MiB; historical samples were not comparable enough for a strict before/after allocation claim; valid pre-fix jar sampling was attempted but current fixture failed early on the older command-result contract | Default JUnit passed 1214/1214; offline art-verify passed 62/62; latest D1 `d1_full_present_combat_ready` passed 1/1 with 30/30 steps, final strict acceptance, zero open/delegated/orphan gaps, and materializer disposal-attempt/rematerialization/cache-hit evidence. Bounded post-fix Arthas sample `debug-artifacts/nrm-11-arthas-20260915T1335Z/` observed heap 164-426 MiB / 512 MiB, Old Gen about 141-159 MiB, Young GC 34->41, and Old GC 0. Negative inventory covers bounded ledger/bridge/effect/registry stores and targeted clear-all; later NRM-08 work is tracked independently as verifying and NRM-09 remains contract-blocked | `./scripts/with-art-env.sh test` 1214/1214; `tools/art-verify` offline 62/62; `scripts/art-lab combat verify-full` 1/1 after restoring post-fix jar; bounded Arthas dashboard/jvm/memory sample | R6 final review PASS; R14-D1 review PASS `ses_f5f32b64fffe5MN9tYzOgcF5Xn`; user-scoped final integrated review `ses_f5a07cb1affeOWBkm00OV3dHmG` PASS after stale-language cleanup | complete |

## Status Rules

## NRM-09 Verification Addendum

The historical blocked row above is superseded by the approved NRM-09 contract and implementation.
`StageHost` now owns one synchronized, injectable-clock publisher: heartbeat every 500ms and full
snapshot every 5000ms from `postUpdate` only; `postRender` does not tick. `ART_PROBE ` v1 and
`art_probe_latest.log` are unchanged. `ART_HEARTBEAT ` schema v1 is written separately to
`art_heartbeat_latest.log`; heartbeat is liveness metadata and consumers read the latest full file
for payload. R15-05/06/07 are fixed: metadata commits only after full construction and at least one
mount succeeds, failed full publication retries after 500ms, explicit construction/persistence is
one synchronized transaction, and same-directory `File.renameTo` replaces without direct-truncate
fallback. Focused publisher/sidecar/StageHost/legacy-probe tests passed 40/40; the default JUnit gate
passed 1239/1239. Status: `verifying` pending device evidence; no deploy/device evidence is claimed.

### NRM-09 D1 regression and correction

- Live jar `df586...` reached READY; `scripts/art-lab combat verify-full` then failed with
  `no fresh ART_COMMAND result for lab command`. Android `File.renameTo` did not overwrite the
  existing sidecar destination, leaving `ART_COMMAND`/probe latest files stale after first write.
- The sidecar commit now uses same-directory Java 8 `Files.move` with `ATOMIC_MOVE` and
  `REPLACE_EXISTING`. Unsupported/failed atomic moves return false, preserve the old destination,
  and clean the temporary best-effort; no direct-truncate or non-atomic fallback is used.
- Focused publisher/sidecar/StageHost/legacy-probe tests pass 40/40, including repeated overwrite;
  the default JUnit gate passes 1239/1239. Status remains `verifying` pending corrected-jar D1.

- `pending`: not yet investigated.
- `reviewing`: baseline or findings are being established.
- `implementing`: accepted findings are being fixed.
- `verifying`: implementation exists and required checks are running.
- `blocked`: a named decision or prerequisite prevents progress.
- `complete`: authority, after evidence, tests, applicable gates, and review closure are recorded.

Do not overwrite before evidence. Append exact test commands, result counts, D1 artifacts, Arthas
artifacts, and finding dispositions as each row advances.

## Post-fix D1 evidence

- NRM-10/NRM-11: authoritative `d1_entity_present_smoke` rerun passed 1/1 scenario and 16/16
  steps. After `art lab host-recreate`, `backend.safety.c1HostRecreation=rebuilt`,
  `entities.slotCount=1`, `backend.entityDraw.count=1`, and target
  `c2_entity_art-lab-entity enabled=true`. Cleanup left draw count 0, slot count 0, and the target
  absent.
- JSON: `/home/justinz/ArtFramework/debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.json`.
  Log: `/home/justinz/ArtFramework/debug-artifacts/art-verify/nrm10-post-fix-rerun/d1_entity_present_smoke.latest.log`.
- No Texture-disposal/rematerialization evidence and no pixel or `ART_UI` proof exists. NRM-10
  remains `verifying` pending real Texture lifecycle evidence or independent review; no real R12
  review result is recorded.
- Superseded NRM-11 status: this section predates the user-scoped closure standard and no longer
  controls final disposition.
- NRM-08 unified differential reconciliation is implemented and `verifying`; no paired allocation
  improvement is claimed.
- NRM-09 remains `blocked` pending the owning writer clock, heartbeat schema/version and fields,
  full-snapshot cadence, freshness marker, consumer fallback/merge rule, and schema migration rule.

## R12 disposition

- `R12-01`: fixed in `PresentSafety`; admission distinguishes scheduled, synchronous, and dispatcher
  failure. Rejection/throw performs no lifecycle step and is probe-visible. Accepted asynchronous
  dispatch returns without waiting or claiming terminal success.
- `R12-02`: safe unit/command-path portion fixed in `ArtCommand`; `art lab host-recreate` returns
  `ERROR` for dispatcher admission failure and `OK ... requested` for either accepted mode. Ordered
  failure isolation remains covered by `PresentSafetyTest`.
- 2026-09-09 focused PresentSafety/rebuild/materializer tests passed 40/40; full default JUnit passed
  1194/1194; `git diff --check` passed. No D1/live GL/Texture evidence is claimed. NRM-10/11 remain
  `verifying`; NRM-08 is `verifying` and NRM-09 remains `blocked`.

## R13 disposition

- `R13-01` accepted and fixed: materializer fail-open records `failed: ` plus the exception
  simple name on the dedicated `materializerRecreation` field. Later projection does not rewrite
  it or invent rebuilt/GL success.
- `R13-02` accepted and fixed: scheduled and dispatcher-reject/throw tests assert
  `materializerRecreation` in lockstep with `c1HostRecreation` (`scheduled`,
  `failed: dispatcher rejected`, `failed: dispatcher IllegalStateException`) while keeping the
  two probe keys distinct.
- Public `onHostRecreated()V` remains a non-blocking compatibility wrapper. No request coalescing
  and no GL/Texture success were invented.
- Focused `./scripts/with-art-env.sh test --tests 'artframework.sts1.PresentSafetyTest'` passed 18/18.
- NRM-10/11 remain `verifying`; NRM-08 is `verifying` and NRM-09 remains `blocked`. This slice does not close NRM-10.

- Focused fix re-review `ses_f753816c9ffe7O6YOWLHCHEvbA` Result: PASS. Findings none.
- Post-fix default `./scripts/with-art-env.sh test` passed 1199/1199 (`PresentSafetyTest` 18/18).
  This does not complete NRM-10.

## R14 disposition

- Additive materializer counters are nested at `backend.materializer` and remain distinct
  from `backend.safety.materializerRecreation`.
- Counters: `residentCount`, `missingCount`, `putCount`, `disposeCount`, `hitCount`.
  Resident/missing drop to 0 after a successful clear; put/dispose totals survive clear.
  Dispose counts distinct identities passed to the disposer during clear/evict/replace,
  including attempted identities when the disposer throws.
- Probe is read-only and does not dispose. This slice is not live Texture/GL/D1 evidence.
- NRM-10/11 remain `verifying`; NRM-08 is `verifying` and NRM-09 remains `blocked`. R13-01/R13-02 stay PASS
  `ses_f753816c9ffe7O6YOWLHCHEvbA` and are not reopened.

- Focused R14 verification:
  `./scripts/with-art-env.sh test --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest'
  --tests 'artframework.sts1.PresentSafetyTest'` passed 38/38 (20 materializer, 18
  PresentSafety). Full JUnit, deploy, and D1 were not run.

- `R14-01` accepted and fixed: focused tests now assert `hitCount` increments only on
  non-null `get()`, misses and `probeSlice()` do not increment it, and `hitCount`
  survives successful `clear()` while resident/missing drop to 0. Production cache
  semantics were unchanged. Focused
  `./scripts/with-art-env.sh test --tests 'artframework.sts1.assets.Sts1AssetMaterializerTest'`
  passed 21/21. Focused fix re-review `ses_f7493de3effeEJyJafJ0sSEaUY` returned PASS with no
  findings. The reviewer did not run tests and supplied no Texture/live GL/D1 evidence. NRM-10/11
  remain `verifying`; NRM-08 is `verifying` and NRM-09 remains `blocked`.

- R14 D1 verification slice added typed `capture` and strict numeric `gt_var` support to
  `tools/art-verify`, with offline verification passing 62/62. The FULL combat fixture now
  exercises the file-backed combat-energy image through `Sts1AssetMaterializer`, captures
  process-local counter baselines, and asserts disposal-attempt, rematerialization, and later
  cache-hit deltas. The expanded fixture has 26 steps; `scripts/art-lab combat verify-full`
  passed 1/1. Artifact `debug-artifacts/art-verify/d1_full_present_combat_ready.json` recorded
  pre-recreation `put=2`, `dispose=0`, `hit=135`, `resident=2`; post-recreation/rematerialization
  `put=3`, `dispose=2`, `hit=149`, `resident=1`; later hit reuse reached `hit=189`.
  This is real D1 cache/materializer evidence, not proof of GL-driver deletion or pixel output.
  Independent review of this changed tooling/fixture slice and paired D1 memory/allocation/GC
  evidence remain open; NRM-10/11 stay `verifying`.

## R14-D1-01-02 Disposition

- The energy attribution writer is private and reachable only after successful non-null
  materialization through `resolveEnergyTexture`, with exact logical ID and file-backed checks.
- Clean focused materializer verification passed 27/27; the default semantic gate passed 1212/1212.
- Independent focused re-review `ses_f6bb24444ffe5zc5DasuD5yNcx` returned PASS with no findings.
- No new deployment or D1 run followed this hardening. NRM-10/11 remain `verifying`.

## R14-LAB Disposition

- `LabRecipeRunner` now treats the existing `inGame && non-empty roomPhase` contract as terminal,
  never abandons or embarks after readiness, gates pre-room embark to enabled state exactly once,
  and checks seed operation results. `StsLabRecipesTest` covers stale overlay completion,
  duplicate embark prevention, and terminal seed failure.
- Clean focused lab verification passed 26/26; default JUnit passed 1214/1214.
- Latest deployment pushed and enabled the jar on D1, followed by fresh READY startup. Standard
  `scripts/art-lab combat verify-full` passed 1/1. Artifact
  `debug-artifacts/art-verify/d1_full_present_combat_ready.json` records successful recipe-driven
  combat setup and energy attribution (`putCount=2`, `hitCount=93`, `residentCount=2`).
- The three additional review concerns were dispositioned as outside the frozen contract/scope;
  historical NRM-10/11 status here is superseded by later review and closure records.

## R14-D1 Closure

- `R14-D1-01` is closed. The regenerated 30-step FULL fixture now asserts final strict acceptance,
  zero open delegated invocations, zero delegated-without-evidence gaps, and zero orphan output;
  all four final records are `pass` in `debug-artifacts/art-verify/d1_full_present_combat_ready.json`.
- Independent fresh review `ses_f5f32b64fffe5MN9tYzOgcF5Xn` returned PASS with no findings.
- NRM-10 is complete for the bounded materializer/cache lifecycle contract: focused/default JUnit,
  offline verifier, D1 disposal-attempt/rematerialization/cache-hit evidence, and independent review
  are complete. This does not claim successful GL-driver deletion or pixel parity.
- Superseded NRM-11 status: paired D1 memory/allocation/GC measurements are no longer required by
  the user-scoped closure standard below.

## NRM-11 Post-fix Arthas Sample

- Bounded post-fix D1 sample was collected after a fresh `mts` debug-compatible start and a passing
  `scripts/art-lab combat verify-full` run. Arthas used `dashboard -n 30 -i 1000` for approximately
  35 seconds, then `jvm`, `memory`, and reset cleanup.
- Artifact directory: `debug-artifacts/nrm-11-arthas-20260915T1335Z/`. The dashboard observed heap
  164-426 MiB / 512 MiB, Old Gen approximately 141-159 MiB, and Young GC count 34->41 with no Old GC;
  final memory reported heap 437 MiB and Old Gen 159 MiB. The supported CLI exposed no direct
  allocation-rate metric.
- Independent review `ses_f5ab4c287ffeaBhmPrALoE6vv1` disposition: useful post-fix observation,
  not a paired comparison. It does not establish improved allocation/GC cadence, stable post-GC
  Old Gen baseline, or bounded retained-store behavior.
- Superseded disposition: under the later user-scoped closure standard, this post-fix sample is
  operational-health evidence rather than paired improvement evidence.

## NRM-11 Final Integrated Review

- Final integrated review `ses_f5a07cb1affeOWBkm00OV3dHmG` initially found only stale historical
  `verifying` language after the user-scoped standard changed. Those earlier blocker statements are
  now marked superseded; the NRM-11 table row and final closure section are authoritative.
- Final re-check in the same review session returned PASS with no findings.
- NRM-11 is complete under the user-scoped standard: original-game operation remains normal under
  current ART changes, latest D1 FULL strict fixture passes, bounded post-fix Arthas shows no Old GC
  or obvious runaway during the sampled window, and no paired allocation improvement is claimed.

## NRM-11 Pre-fix Sampling Attempt

- A true pre-fix jar was built from `203e525295b98ea4bf6d8d062e3f378cd3b14c23` in a detached
  `/tmp/artframework-pref` worktree. The valid pre-fix jar SHA-256 is
  `251cbc05b6e129a9d3843dbcacbbf7eeef9f7eea7c0d960859aa07882392aac8`, distinct from the saved
  post-fix jar SHA-256 `ebbb4b810fa064ecf252b80040e9dc908122742f1e699c601c15bb4e79accef3`.
- D1 was temporarily switched to the valid pre-fix jar, started fresh, and reached READY, but the
  current standard FULL fixture failed early with `no fresh ART_COMMAND result for lab command`.
  The old jar is therefore not compatible with the current fixture command-result contract.
- A bounded pre-fix Arthas fallback sample was still collected under
  `debug-artifacts/nrm-11-arthas-20260915T-pref-valid/`: dashboard uptime 648-677 seconds, heap
  186-349 MiB during the dashboard window, Old Gen 168 MiB, Young GC unchanged at 35, and Old GC 0.
  Workload is explicitly not a same FULL fixture pass.
- The invalid earlier pre-fix attempt under `debug-artifacts/nrm-11-arthas-20260915T-pref/` used a
  jar with the same SHA as the post-fix jar and is not evidence.
- Independent review `ses_f5a2f5429ffe1FX2GG4ZuEucqG` concluded that no paired memory/allocation/GC
  claim is defensible: workload, uptime, GC phase, and allocation metrics do not match. D1 was
  restored to the post-fix jar and the standard FULL fixture passed again.

## NRM-11 User-Scoped Closure Standard

- User decision: an equivalent pre-fix stress workload cannot be provided. NRM-11 no longer requires
  a same-procedure pre/post performance-improvement proof; closure is limited to confirming that
  original gameplay remains operationally healthy under the current ART changes.
- Accepted closure evidence: latest default JUnit passed 1214/1214; offline art-verify passed 62/62;
  repeated D1 FULL combat passed 1/1 with final strict acceptance and zero open/delegated/orphan
  gaps; NRM-10 materializer lifecycle is complete; bounded post-fix Arthas observation shows no Old
  GC, bounded Old Gen around 141-159 MiB during the sampled window, and normal Young-GC sawtooth
  behavior while the original game remains playable.
- Explicit non-claims: no paired allocation-rate improvement, no GL-driver deletion proof, and no
  pixel-parity proof. The invalid and non-comparable pre-fix attempts remain documented as rejected
  evidence.

## NRM-09 Closure

- The approved split is implemented: one `postUpdate` monotonic publisher emits a lightweight
  `ART_HEARTBEAT` every 500ms and a compatible `ART_PROBE` full snapshot every 5000ms. Explicit
  `art probe` remains immediate; `postRender` does not advance either cadence.
- Focused JUnit passed 40/40; default `./scripts/with-art-env.sh test` passed 1239/1239; offline
  art-verify passed 62/62; final review found no issues after R15-05/06/07 corrections.
- Corrected jar `e1fcff48bde3c6d6bde730e7e6f7208aa4e7de6a44e32e462246f9bb17f5faf1` was deployed to D1.
  Standard `scripts/art-lab combat verify-full` passed 1/1 after transient startup/probe retries.
- D1 sidecar sampling confirmed complete newline-terminated JSON. Heartbeat was 302 bytes and
  advanced from sequence 620 to 632 over approximately six seconds; the full probe was 184058
  bytes, remained unchanged at 1.2 seconds, and advanced by approximately six seconds. A later
  sample measured heartbeat 302 bytes at sequence 18987/probeSequence 1926 and full probe 185222
  bytes at representative frame 286686. Explicit `art probe` executed successfully.
- The previous implementation generated full probes from a shared 30-callback post-update/
  post-render counter. The new implementation separates liveness from payload and avoids full
  snapshot construction on heartbeat ticks. This demonstrates cadence and payload separation, not
  a paired allocation-rate or GC improvement claim.
- NRM-09 is complete under the frozen contract.

## NRM-08 Closure

- The initial D1 regression was corrected by keeping the active-surface desired plan system-local;
  the generic RenderHost reconcile kernel remains track-agnostic.
- Focused RenderPlan/RenderHost tests passed 39/39. The default `./scripts/with-art-env.sh test`
  gate passed 1227/1227.
- Final jar `47ac04adafba602f9f5fc1d492f99783dcf22d527b41b5dd6f354122ec8cfad3` was deployed to D1.
  The final `scripts/art-lab combat verify-full` run passed 1/1 after one transient probe reconnect
  failure; the successful artifact is `debug-artifacts/art-verify/d1_full_present_combat_ready.json`.
- Final independent review `ses_f51004cfbffexhGwjsMJJIHQ4p` returned PASS with no findings.
- NRM-08 is complete under the frozen contract. No paired allocation improvement is claimed.

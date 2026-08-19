# Traditional ECS review log

Reviewer sessions are advisory evidence. JUnit, tooling, and device gates remain separate.

## Round R1 - Schedule and execution ownership

- Ledger row: `TE-01`
- Session: `ses_ff70a74c5ffeqvqniqsqugWgtd`
- Scope: changed only for accepted fixes: `PresentationSchedule`, `ArtFramework`, `PresentSurfaces`,
  and their focused tests
- Result: PASS after focused fix review
- Intended scope:
  - `src/main/java/artframework/api/PresentationSchedule.java`
  - all production `EcsPipeline.run` call sites
  - schedule-owned system construction and synchronous compatibility callers
- Governing claim: production phases use one fixed order and stateless systems; synchronous APIs
  must not create systems or retain a second result authority
- Findings:
  - `R1-01` high, accepted, fixed, verified: authority input commands are destroyed in a
    `finally` block after projection attempt; malformed-frame tests prove no retained/retried
    authority entity for `publishFrame` or `advanceFrame`.
  - `R1-02` medium, accepted, fixed, verified: `SurfaceLifecycleSystem` is a fixed
    `PresentationSchedule` instance; `PresentSurfaces` writes the request then delegates through
    `ArtFramework`, with no direct production `EcsPipeline.run` call.
- Verification: `./scripts/with-art-env.sh test` passed on 2026-08-16 (`BUILD SUCCESSFUL`, full
  suite); negative inventory finds one production `EcsPipeline.run` and one
  `new SurfaceLifecycleSystem`, both in `PresentationSchedule`.
- Residual risk: confirmation/native lifecycle suppression after failed authority projection is
  established by control flow but has no direct observable assertion. Render projection timing is
  reserved for `TE-02`.

## Finding Disposition

Use stable IDs and preserve rejected findings with evidence:

| ID | Severity | Disposition | Fix evidence | Verification |
|---|---|---|---|---|
| R1-01 | high | accepted, fixed, verified | `PresentationSchedule.runAuthorityProjection` destroys the queued entity in `finally` | Focused malformed-frame JUnit plus full `./scripts/with-art-env.sh test` |
| R1-02 | medium | accepted, fixed, verified | `SurfaceLifecycleSystem` ownership moved to `PresentationSchedule`; facade delegates through `ArtFramework` | Focused facade lifecycle JUnit, negative inventory, and full `./scripts/with-art-env.sh test` |

## Round R2 - Render projection mutation boundary

- Ledger row: `TE-02`
- Session: `ses_ff65df0fdffeDRrWqfH2KrQnWO`
- Scope: changed for accepted fixes in `RenderPlan`, `PresentPackApply`, `Sts1SurfaceRenderer`,
  `RenderPlanRebuildTest`, and `HostRenderBackendTest`
- Result: PASS after focused fix review and D1 render verification
- Intended scope:
  - `src/main/java/artframework/render/RenderProjectionQueue.java`
  - `src/main/java/artframework/render/RenderProjectionSystem.java`
  - `src/main/java/artframework/render/RenderHost.java`
  - all production `projectNow()` and `projectActiveSurfaces()` callers
- Governing claim: ECS render-plan inputs are authoritative; host targets are disposable caches;
  scheduled mutation coalesces to one projection, and immediate host/lifecycle boundaries must be
  explicit and tested.
- Findings:
  - `R2-01`, accepted, fixed: active C2 effects now reach the host effect draw band.
  - `R2-02`, accepted, fixed: active projection filters both C2 parent and item targets.
  - `R2-03`, accepted as independent design work: moved to `TE-08` because current
    `EntityPresent` methods are separate synchronous public operations with no atomic transaction.
  - `R2-04`, accepted, fixed: pack state mutates completely before one final projection.
  - `R2-05` through `R2-10`, accepted, fixed: the C2 effect band precedes content; every active
    select/reward owner is materialized; inactive, removed, and native-fallback visuals are cleaned.
- Verification: final `./scripts/with-art-env.sh test` passed 581/581. Review returned PASS.
  `@android-deploy-jar` subsequently verified D1 `ArtFramework.jar` at 936,484 bytes and enabled;
  `scripts/art-lab combat verify-full` passed `d1_full_present_combat_ready`.
- Residual risk: focused tests do not directly assert production draw ordering, stale-item removal,
  multi-variant select/reward preparation, or projection-count coalescing. The first D1 smoke
  attempt failed before scenario setup on an empty console response; status was READY and the
  immediate retry passed.

## Round R2 Finding Disposition

| ID | Severity | Disposition | Evidence |
|---|---|---|---|
| R2-01 | high | accepted, fixed, review PASS | C2 effect band calls `drawFrame(...kindsC2UnderPresent())` |
| R2-02 | medium | accepted, fixed, verified | Active parent/item filtering plus focused JUnit |
| R2-03 | medium | moved to `TE-08` | Requires an explicit EntityPresent transaction/API decision |
| R2-04 | medium | accepted, fixed, review PASS | One final pack projection after all ECS writes |
| R2-05–R2-10 | high/medium | accepted, fixed, review PASS | Preprojection ordering and active/current/native-fallback cleanup |

## Round R3 - Compatibility stores and read purity

- Ledger row: `TE-03`
- Session: `ses_ff4ee27d7ffeXw6wpySr4y4TKK`
- Scope: changed for accepted fixes in `PresentProjection`, `PresentSurfaces`, and focused tests
- Result: PASS after focused fix review
- Intended scope:
  - `PresentProjections`, `PresentProjection`, and projection compatibility readers
  - `PresentSurfaces`, `UiProbe`, `ArtFramework.OPEN`, inspect, and console read paths
  - read-side context/entity creation, component writes, synchronization, and mutable mirrors
- Governing claim: queries and diagnostics do not create/synchronize production state; compatibility
  facades derive views from ECS and do not retain a second mutable presentation authority.
- Findings:
  - `R3-01` high, accepted, fixed, verified: `PresentSurfaces.probeAll()` created/synchronized every
    surface through `probeSlice`; reads now avoid `syncComponents` and use existing contexts only.
  - `R3-02` high, accepted, fixed, verified: `PresentProjection` construction and reads lazily created
    metadata/components; reads now return immutable defaults while authority/reset paths create state.
  - `R3-03` medium, accepted, fixed, verified: `UiInspect.listSurfaces()` created `c2-surfaces` through
    `isMounted`; surface reads now use `PresentationRegistry.existingContext`.
- Verification: initial `@junit-test` was blocked by concurrent writes to Gradle output directories;
  after contention cleared, full JUnit passed 584/584 including all three focused zero-state tests.
- Review closure: resumed session `ses_ff4ee27d7ffeXw6wpySr4y4TKK` returned PASS with no findings.
- Residual risk: immutable catalogs, callback hubs, asset resources, and host caches are not in scope
  unless a read path converts one into presentation authority.

## Round R4 - Component data-only and duplicate concepts

- Ledger row: `TE-04`
- Session: `ses_ff4beaf3cffe2bm4iCG0yUhcJq`
- Scope: changed for accepted R4-01/R4-07 fixes; schema findings split into bounded ledger rows
- Result: PASS after focused fix review
- Intended scope:
  - all production `*Component` classes and their field value types
  - duplicate node/tree identity, hierarchy, and lifecycle concepts in `core` and `presentation`
  - opaque `Object`, nested collection, declaration, snapshot, and binding values retained in ECS
- Governing claim: components contain immutable host-agnostic data only, and each persistent
  identity/hierarchy/lifecycle/presentation fact has one component owner.
- Findings:
  - `R4-01` medium, accepted, fixed, verified: deleted unused duplicate `core` node identity,
    hierarchy, lifecycle, and tree identity components; `presentation` components remain authority.
  - `R4-02` and `R4-08` high/medium, accepted and moved to `TE-09`: recursively immutable C1
    declaration/property schema requires a separate API decision.
  - `R4-03` high, accepted and moved to `TE-10`: EntityPresent snapshot schema.
  - `R4-04` high, accepted and moved to `TE-11`: supported control value schema.
  - `R4-05` and `R4-06` high, accepted and moved to `TE-12`: intent argument and connection
    declaration schemas.
  - `R4-07` medium, accepted, fixed, verified: `SurfacePolicyComponent` now stores a host-neutral
    level string; STS1 `PresentLevel` conversion stays at the facade writer boundary.
- Verification: full `./scripts/with-art-env.sh test` passed 584/584; negative searches found no
  deleted component references and no `artframework.sts1` imports in context components.
- Review closure: resumed session `ses_ff4beaf3cffe2bm4iCG0yUhcJq` returned PASS.
- Residual risk: one-shot command DTOs may retain generic values only when the API validates they
  are host-agnostic immutable data; these contracts are explicitly tracked in TE-09 through TE-12.
  External binary consumers of the deleted, unused public `core` classes were not evidenced in-repo.

## Round R5 - Host cache recreation

- Ledger row: `TE-05`
- Session: `ses_ff2b6b62bffeXNqo2qbgOas5Yb`
- Scope: changed for accepted cache release/recreate fixes in `RenderHost`, `PresentSafety`, C1
  Stage/actor cache classes, and `RenderPlanRebuildTest`
- Result: PASS after final closure review
- Intended scope:
  - `RenderHost`/`RenderHosts`, `PresentSafety`, `Sts1AssetMaterializer`
  - `StageHost`, `EffectTargetActors`, `SyntheticRuntime`
  - `NativeTemplateRuntime`, EntityPresent, and `Sts1SkeletonBridge`
- Governing claim: host actors, textures, targets, native handles, provider bindings, subscriptions,
  and other host caches are disposable; clearing them retains ECS facts and recreation rebuilds
  equivalent behavior from retained state.
- Findings:
  - `R5-01` high, accepted, fixed, verified: `PresentSafety` invalidates render host resources and
    STS1 texture cache before ECS projection rebuild; Stage recreation returns failure rather than
    a false rebuilt status when old-cache disposal, construction, declaration lookup, or attach fails.
  - `R5-02` medium, accepted, fixed, verified: `StageHost.recreateHost()` rebuilds disposable C1
    actors from retained open ECS declarations and fails closed rather than retaining partial state.
  - `R5-03` medium, accepted, fixed, verified: Stage recreation clears global
    `EffectTargetActors` before rebuilding current bindings.
  - `R5-04` low, accepted, fixed by invalidation: host recreation clears STS1 texture references;
    ownership of `ImageMaster` textures remains external, so they are not disposed here.
- Verification: `SkinOwnershipTest` covers detach, retired defaults, unattached conversion failure,
  reset clear, and shared identity cleanup. `RenderPlanRebuildTest` covers render cache removal and
  rebuild from retained ECS. Full `./scripts/with-art-env.sh test` passed 589/589. Final D1 deployment
  verified `ArtFramework.jar` at 940,143 bytes with SHA-256
  `de37ff59d9afee3d24d3bca988e3bdb68a0a9125663966645fd30ddf7811893d`; cold start reached READY;
  `art lab host-recreate` executed; `scripts/art-lab combat verify-full` passed
  `d1_full_present_combat_ready`.
- Review closure: final focused session `ses_ff1a4da5effeP6z6Wc4PLx16G3` returned PASS after the
  shared-Skin identity-deduplication correction.
- Residual risk: exceptional concrete Stage/Skin failure injection remains source-inspection evidence;
  production C1 host behavior is covered by the successful D1 recreation and combat smoke.

## Round R6 - Dependency direction: MapNodeRef boundary

- Ledger row: `TE-06`
- Session: `ses_ff1a4da5effeP6z6Wc4PLx16G3`
- Scope: changed for the first dependency slice: `MapNodeRef` moved from `artframework.c2` to
  host-neutral `artframework.component`, with production and test imports updated
- Result: PASS
- Governing claim: pure ECS data values must not force `context` or other lower-level packages to
  depend on C2/native adapter packages; host adapters may consume the neutral value at their boundary.
- Evidence: initial import inventory found `context/MapGestureComponent -> c2/MapNodeRef`; negative
  search after migration finds no `artframework.c2.MapNodeRef` references. Full
  `./scripts/with-art-env.sh test` passed after the migration.
- Review closure: MapNodeRef is immutable host-neutral data; no stale duplicate or import remains.
- Remaining scope: classify `context/SurfaceIds -> c2/NativeTemplateIds` and
  `core -> c1/SyntheticRuntime` separately; do not fold them into this review.
- Residual risk: package relocation changes the public type name for external consumers; no in-repo
  compatibility alias was added because the refactor target is a host-neutral data boundary.

## Round R7 - Dependency direction: neutral IDs and C1 orchestration

- Ledger row: `TE-06`
- Session: `ses_ff0d7741bffeLipokRU6HuTD15`, resumed as `ses_ff0d41390ffegG3iR0XcOMUlzK`
- Scope: changed for `NativeTemplateIds` relocation to `artframework.component`, core pack loader
  decoupling, `PresentRestyleHost` adapter, C1 registration, and the consumer fixture import
- Result: PASS after R7-01 fix
- Governing claim: context/core packages do not depend inward on C1/C2/STS1; neutral identifiers are
  owned below adapters; C1 host behavior is requested through an explicit optional adapter.
- Findings:
  - `R7-01` high, accepted, fixed: `tools/consumer-fixture/src/ConsumerFixture.java` retained
    `artframework.c2.NativeTemplateIds`; it now imports `artframework.component.NativeTemplateIds`.
- Verification: no `artframework.c2.NativeTemplateIds` references remain; `scripts/verify-consumer-fixture.sh`
  compiled the fixture against `build/libs/ArtFramework.jar`; full JUnit before fixture correction passed
  590/590; final full JUnit after fixture correction also passed 590/590.
- Review closure: final focused session `ses_ff0d41390ffegG3iR0XcOMUlzK` returned PASS with no
  additional findings.
- Residual risk: `PresentRestyleHost` adapter install/delegation/reset has no focused test; pack-loader
  parity with `SyntheticRuntime.loadLayoutResource` is source-inspected rather than directly tested.

## Round R8 - Final negative inventory and closure

- Ledger row: `TE-07`
- Session: `ses_ff0c2a3aaffeuMBANViRjKcrNh`
- Scope: frozen full production source inventory plus `tools/consumer-fixture`
- Result: PASS
- Governing claim: no unexplained old writer, duplicate mutable presentation authority, direct
  pipeline bypass, stale deleted-type reference, inward core/context host import, or stale public
  package path remains.
- Evidence: one production `EcsPipeline.run` remains in `PresentationSchedule`; deleted core
  identity/hierarchy/lifecycle/tree types have no references; no `context/core -> c1/c2/sts1` imports
  remain; direct projection calls are classified lifecycle/host/console boundaries; consumer fixture
  compiles against current public packages.
- Verification: full `./scripts/with-art-env.sh test` passed 590/590; `scripts/verify-consumer-fixture.sh`
  passed; review found no actionable findings.
- Residual risk: `PresentPacks`/`PresentPackApply` compatibility caches and
  `PresentProjection`/`PresentProjections` last-result cache remain source-inspected as
  non-authoritative boundaries without dedicated runtime authority assertions.

## Round R9 - EntityPresent atomic update contract

- Ledger row: `TE-08`
- Session: `ses_ff0914405ffexJ8G3ZlnYKEKCh`
- Scope: changed `EntityPresent` atomic API, render projection queue batching/reset, console
  entity attach caller, and focused EntityPresent/queue tests
- Result: PASS after R9-01 through R9-03 test refinements
- Governing claim: independent `attach`/`sync`/`layout`/`detach` retain synchronous compatibility
  visibility; `present` is an explicit atomic construction sequence with exactly one final host
  projection, including under an outer batch.
- Findings:
  - `R9-01` medium, accepted, fixed: creation and replacement `present` now assert lifecycle order
    and callback slot state.
  - `R9-02` medium, accepted, fixed: projection-count evidence proves one standalone and one
    outer-batch-final rebuild for `present`.
  - `R9-03` low, accepted, fixed: reset during nested projection work discards stale work and a
    later standalone projection remains immediate.
- Verification: `EntityPresentTest` and `RenderProjectionQueueTest` focused cases; full
  `./scripts/with-art-env.sh test` passed 596/596.
- Residual risk: listener exception rollback is intentionally undefined. `present` flushes its owned
  batch in `finally`, but mutations committed before a listener exception may remain in ECS.

## Round R10 - C1 declaration and property value schema

- Ledger row: `TE-09`
- Session: `ses_ff05c2e77ffevblrABVBp8BWPM`
- Scope: `ImmutableUiValue`, UiNode/effect/property declaration owners, loader/template ingress, and
  focused schema tests
- Result: PASS after R10-01 through R10-04 fixes and test refinements
- Governing claim: persistent C1 declaration/property data uses only null, scalar, recursively
  immutable string-key-map/list values; host objects and cycles cannot enter ECS declarations.
- Findings:
  - `R10-01` high, accepted, fixed: `EffectDecl.params` now uses the same schema as UiNode props.
  - `R10-02` medium, accepted, fixed: identity-tracked recursion rejects cyclic maps/lists and
    cleans tracking in `finally`.
  - `R10-03` medium, accepted, fixed: focused tests cover builder/property overlay and shared
    acyclic nested-value isolation.
  - `R10-04` low, accepted, fixed: template nested call properties are copied, immutable, and
    reject host objects at final UiNode construction.
- Verification: `ImmutableUiValueTest`, `TemplateExpanderTest.nestedCallPropertiesAreCopiedAndValidated`;
  full `./scripts/with-art-env.sh test` passed 608/608.
- Residual risk: TE-10 through TE-12 retain their separately scoped generic snapshot, control,
  intent, and connection value schemas.

## Round R11 - EntityPresent snapshot value schema

- Ledger row: `TE-10`
- Sessions: `ses_ff037bbc6ffe0ujzzV6bBTrBz6` (baseline findings); closure
  `ses_ff0237f7effeWyq4sRZCeThc21`
- Scope: `EntityPresent` compatibility ingress, `EntitySnapshot`, ECS snapshot component, derived
  slot views, draw/probe readers, design note, and focused EntityPresent/draw tests
- Result: PASS after R11-01 through R11-06 fixes
- Governing claim: ECS components retain only framework-owned immutable host-neutral
  `EntitySnapshot` data. Compatibility `Object` ingress normalizes immediately and never retains a
  caller-owned object; views/readers expose only the typed immutable snapshot.
- Findings:
  - `R11-01` critical, accepted, fixed, verified: `DefaultEntityPresent.sync` wrote the
    caller `Object` straight into `EntitySlotSnapshotComponent`; it now normalizes before write and
    the component is typed.
  - `R11-02` high, accepted, fixed, verified: `EntitySnapshot.extras` shallow-copied
    arbitrary nested values; constructor and map parser now use `ImmutableUiValue`.
  - `R11-03` high, accepted, fixed, verified: typed `EntitySnapshot` inputs bypassed
    normalization; they now copy through the immutable constructor before storage.
  - `R11-04` medium, accepted, fixed, verified: `EntitySlot.snapshot()` exposed raw
    `Object`; it now returns only typed `EntitySnapshot` and draw/probe readers consume it directly.
  - `R11-05` medium, accepted, fixed, verified: Map parsing retained arbitrary nested
    caller values; it now validates/deep-copies string-key maps before field parsing and extras
    retention.
  - `R11-06` low, accepted, fixed, verified: identity tests documented opaque object
    retention; they now assert typed value semantics, mutation isolation, and rejected ingress.
- Verification: first full JUnit result failed 4 legacy string-snapshot test inputs, all in
  `EntityPresentTest`; tests were corrected to use the typed schema. Final
  `./scripts/with-art-env.sh test` passed 612/612 with 0 failures and 0 ignored. No device gate
  applies because this is pure ECS/API data behavior.
- Residual risk: external consumers compiled against `EntitySlot.snapshot(): Object` require source
  migration to typed `EntitySnapshot`; the public `sync/present(Object)` compatibility ingress is
  retained for supported map and typed inputs.

## Round R12 - Control value schema

- Ledger row: `TE-11`
- Session: `ses_ff020d900ffeXFqowajkap8TiE`
- Scope: `ControlValueComponent`, C1 materialization, WidgetSession, UiOps, ControlValueSystem,
  direct ECS readers/writers, and focused control tests
- Result: FINDINGS accepted; implementation and final review in progress
- Governing claim: persistent control state retains only finite `Float`, `String`, or `Boolean`
  values used by slider/progress, textfield, and checkbox controls; mutable, host, null, and
  non-finite values cannot enter the ECS component.
- Findings:
  - `R12-01` high, accepted, fixed pending verification: `ControlValueComponent` stored arbitrary
    `Object` values and direct writers bypassed validation; the constructor now enforces the closed
    scalar schema.
  - `R12-02` medium, accepted, fixed pending verification: scheduled normalization accepted host
    objects through `String.valueOf` and arbitrary numeric values; finite fallback handling now
    prevents invalid values from becoming persistent control state.
  - `R12-03` medium, accepted, fixed pending verification: `UiOps` allowed NaN/Infinity through
    range clamping, producing invalid probe JSON; public slider/progress setters now reject them and
    bound parsing uses finite fallbacks.
- Verification: baseline review found no focused invalid-value tests. Added scalar/rejection tests
  in `PresentationRuntimeTest` and finite input/bounds tests in `UiOpsProbeTest`; final JUnit is
  running. No device gate applies because this is pure C1 ECS value behavior.
- Residual risk: numeric-string declaration compatibility remains intentionally supported at the
  declaration/materialization boundary; runtime component values are closed typed scalars.

### R12 Closure

- Additional finding `R12-04` medium, accepted, fixed, verified: `WidgetSession` and
  `ComponentActors` reinterpreted declaration `min/max` values instead of consuming the
  materialized range. Added `ControlBoundsComponent`; mounted UiOps, scheduled normalization,
  WidgetSession, and scene2d actor paths now consume that ECS component. Unmounted compatibility
  fallback is fixed `0..1`.
- Verification: `PresentationRuntimeTest.controlBoundsRemainMaterializedAfterPropertyChanges` and
  `WidgetSessionTest.setSliderUsesMaterializedBoundsAfterPropertyChange`; full
  `./scripts/with-art-env.sh test` passed 621/621 with 0 failures and 0 ignored.
- Closure review: `ses_fefcff102ffehaseJ1cP2LpRAO` returned PASS with no findings.
- Final residual risk: no device gate applies; unmounted ComponentActors fallback remains a
  disposable compatibility path and does not become ECS authority.

## Round R13 - Intent and connection declaration schemas

- Ledger row: `TE-12`
- Sessions: baseline `ses_fefcda225ffevBiMQla6KlPHCq`; closure findings
  `ses_fef9f4cb4ffe2MbPQ1qEW0usTX`
- Scope: one-shot surface intent args, persistent connection declarations, listener action args,
  dispatch cleanup, focused tests, and explicit compatibility value boundary
- Result: PASS after accepted findings were fixed
- Governing claim: retained one-shot and persistent declaration values use the recursive immutable
  schema. Only `CardRef` and `MapNodeRef` are explicit intent wire values; C1 connection
  declarations remain general immutable data and do not broaden to intent objects.
- Findings:
  - `R13-01` high, accepted, fixed, verified: shallow intent args now recursively
    validate/copy; `CardRef` and `MapNodeRef` encode at every nested depth and restore immediately
    before native dispatch.
  - `R13-02` high, accepted, fixed, verified: connection declarations and listener
    action args recursively copy through `ImmutableUiValue` instead of retaining nested aliases.
  - `R13-03` high, accepted, fixed, verified: one-shot execution removes its request in
    `finally` when dispatch throws.
  - `R13-04` medium, accepted, fixed, verified: `__art_type` is an internal reserved
    intent wire key; generic input maps using it reject at ingress rather than being silently
    retyped during dispatch.
  - `R13-05` medium, accepted, fixed, verified: focused tests now execute nested
    reference restoration through the dispatch system and verify action listener args remain
    recursively immutable after source mutation.
  - `R13-06` medium, split to `TE-13`: `NodeStateMachine` transition/enter-action maps retain
    nested values shallowly; it is an independent state-machine declaration boundary.
- Verification: `./scripts/with-art-env.sh test` passed 629/629 with 0 failures, 0 errors, and 0
  ignored. The final run includes reserved-key, nested-reference dispatch, listener-boundary, and
  the missing `AtomicReference` test import correction. No device gate applies to this pure ECS/API
  schema slice.
- Closure review: focused read-only review session `ses_fed776d3fffen6UHqPbYmd3Efl` returned PASS
  with no findings. The reviewed diff remained within the TE-12 scope.
- Residual risk: `NodeStateMachine` action declaration schema remains independently tracked in
  TE-13; device, deployment, serialization, and host-runtime behavior are outside this slice.

## Round R14 - NodeStateMachine action declaration schema

- Ledger row: `TE-13`
- Sessions: baseline closure `ses_fed706ae2ffepArOrs8h97Xutk`; fix review resumed with the same
  session
- Scope: `NodeStateMachine.Transition`, `setEnterActions`, action declaration ingress, and focused
  `NodeStateMachineTest` coverage
- Result: PASS after R14-01 was fixed and re-reviewed
- Governing claim: transition and state-enter action declarations use the recursive immutable
  host-agnostic schema already established for C1 connections; state-machine runtime retains no
  nested caller-owned mutable values.
- Findings:
  - `R14-01` medium, accepted, fixed, verified: `Transition.onEnter` and `enterByState` shallow
    copied action lists and retained nested caller values. The implementation now normalizes each
    non-null action map through `ImmutableUiValue`; direct transition and state-enter tests cover
    mutation isolation, immutable nested args, and host-value rejection.
- Verification: `./scripts/with-art-env.sh test` passed 633/633 with 0 failures, 0 errors, and 0
  ignored. Focused closure review resumed as `ses_fed706ae2ffepArOrs8h97Xutk` returned PASS with
  no findings. No device gate applies because this is pure ECS/API declaration behavior.
- Residual risk: the state-machine declaration source itself is already delivered through C1
  immutable property values; TE-13 only closes the additional runtime declaration retention
  boundary. Broader lifecycle, recreation, serialization, host-adapter, and device behavior remain
  outside this focused scope.

## Round R15 - PresentPack operation runtime

- Ledger row: `TE-14`
- Sessions: baseline/fix closure `ses_fec6dfe63ffeOjw8psACMBg7WM`; JUnit gates
  `ses_fec6dfe8bffe8hT94heG91gMie`, `ses_fec67c140ffeGbAqqwGpl7CDAc`, and
  `ses_fec6134e9ffeMMw1SjQmr1atbp`
- Scope: `PackWorld` operation transaction foundation; fixed-phase `PackSystems`; reversible
  ECS component, HostAssets, template/window operation adapters; `PresentPacks` runtime bridge;
  compatibility constructor; focused pack/runtime tests
- Result: PASS after TE-14-01 through TE-14-04 fixes and closure review
- Governing claim: a pack enables through one reversible, dependency-checked operation boundary.
  Components remain ECS data, systems run only through fixed schedule phases, and HostAssets plus
  registration domains retain their own authority while participating in the same transaction.
- Findings:
  - `TE-14-01` high, accepted, fixed, verified: failed activation formerly left templates/windows
    installed. Legacy load now snapshots and restores replacement registrations, including an
    existing host or pack registration, on failure.
  - `TE-14-02` high, accepted, fixed, verified: rollback now attempts every undo in reverse order,
    preserves the first cleanup failure with later failures suppressed, and retains the operation
    record until disable succeeds.
  - `TE-14-03` medium, accepted, fixed, verified: duplicate phase/system IDs reject regardless of
    Java instance identity, preserving the original enabled system.
  - `TE-14-04` medium, accepted, fixed, verified: the former public `PresentPack` constructor is
    retained and delegates with an empty immutable operation list.
- Verification: focused `PackWorldTest` passed seven cases; focused `PresentPackTest` passed,
  including failed replacement restoration and compatibility construction. Final
  `./scripts/with-art-env.sh test` passed 643/643 with 0 failures, errors, or ignored tests.
- Closure review: resumed session `ses_fec6dfe63ffeOjw8psACMBg7WM` returned PASS.
- Residual risk: templates, windows, and ambient legacy fields are snapshot-reversible but still
  execute through `PresentPacks` / `PresentPackApply`, not direct `PackWorld` operation entries.
  Their incremental migration is independently tracked as TE-15; no device gate applies to this
  pure runtime/API foundation.

## Round R16 - Lightwave C1 default-effect declaration migration

- Ledger row: `TE-15`
- Session: initial review and pending closure `ses_fec440784ffeq6ueWYJWoYzX6Y`
- Scope: `PackEffectDefaultsComponent`, `PackEffectDefaults`, normalized PresentPack operation
  creation, C1 materialization, title render planning, and focused tests
- Result: PASS after TE-15-01 through TE-15-04 fixes
- Governing claim: enabled pack C1 defaults are immutable ECS contributions, not an independent
  `PresentPackApply` authority; every construction path has one matching reversible contribution.
- Findings:
  - `TE-15-01` high, accepted, fixed, verified: the legacy public `PresentPack` constructor did not
    compile non-empty defaults into an operation. Both constructors now normalize defaults into one
    matching contribution; lifecycle coverage is in `PackEffectDefaultsTest`.
  - `TE-15-02` high, accepted, fixed, verified: explicit default operations could duplicate the
    builder-generated contribution or claim another owner. The dedicated operation is normalized and
    duplicate/foreign-owner declarations are rejected.
  - `TE-15-03` high, accepted, fixed, verified: generic Component CRUD could bypass the dedicated
    contribution boundary. `PackOperations.createComponent/updateComponent` now reject the reserved
    `PackEffectDefaultsComponent` type; focused tests cover both writers.
  - `TE-15-04` medium, accepted, fixed, verified: an empty ECS contribution previously could still
    trigger the legacy facade fallback. `PackEffectDefaults.hasContribution` now separates
    contribution presence from matching values; C1 materialization and RenderPlan only use legacy
    fallback when no migrated contribution exists. Consumer-level tests cover both paths.
- Verification: focused tests passed; final `./scripts/with-art-env.sh test` passed 648/648 with
  0 failures, errors, or ignored tests. Focused consumer tests and `git diff --check` passed.
- Closure review: independent focused session `ses_fec046d66ffeJyPY9JKXGRlqYL` returned PASS.
- Residual risk: broader pack declarations (`surfaceEffects`, `fullFrameEffects`, `bindSurfaces`,
  templates, and windows) still use legacy field-specific activation paths and are tracked for TE-16.
  No device gate applies to this pure ECS/API slice.

## Round R17 - C2 surface-effect declaration migration

- Ledger row: `TE-16`
- Session: initial/fix review `ses_febe8845cffeRibFYWggCODumL`
- Scope: `PackSurfaceEffectsComponent`, `PackSurfaceEffects`, normalized `PresentPack.surfaceEffects`
  operation creation, `PresentationVisuals`, `PresentPackApply`, and focused C2 surface tests
- Result: PASS after TE-16-01 and stale-effect lifecycle fixes
- Governing claim: enabled C2 surface effects are immutable ECS contributions created and removed by
  PackWorld, and legacy `pack.surfaceEffects` is only a fallback when no migrated contribution exists.
- Findings:
  - `TE-16-01` high, accepted, fixed, verified: ECS-first projection iterated legacy
    `pack.surfaceEffects.keySet()`, so explicit migrated operations with empty legacy fields would
    not project to `RenderStateEcs`. `PackSurfaceEffects` now exposes ECS-contributed surface IDs and
    `PresentPackApply` iterates those IDs.
- Additional closure finding, medium, accepted, fixed, verified: existing C2 item entities could retain
  stale prior pack ambient effects across contribution switches. `PresentationVisuals` now tracks and
  removes prior pack surface-effect IDs before applying the current contribution.
- Verification: focused `PackSurfaceEffectsTest`, `C2LightwaveSurfaceTest`, and
  `PresentationVisualsTest` passed after the fixes. Final `./scripts/with-art-env.sh test` passed
  653/653 with 0 failures, errors, or ignored tests. `git diff --check` passed.
- Closure review: independent focused session `ses_febccdc99ffezoyu6y3HBfq1Uv` returned PASS.
- Residual risk: broader pack declarations (`fullFrameEffects`, `bindSurfaces`, templates, and windows)
  still use field-specific activation paths and are tracked for TE-17. No device gate applies to this
  pure ECS/API slice.

## Round R18 - Full-frame effect declaration migration

- Ledger row: `TE-17`
- Sessions: initial review `ses_feb7459e1ffeD5uau4E9RQa3cI`; closure `ses_feb688398ffeUrHe3cHiRN6j4f`
- Scope: `PackFullFrameEffectsComponent`, `PackFullFrameEffects`, normalized full-frame operation,
  `PresentPackApply.applyFullFrame`, and focused full-frame tests
- Result: PASS after TE-17-01/02 fixes
- Governing claim: enabled pack full-frame effects are immutable ECS contributions and the host-facing
  `FullFrameRenderComponent` is projected from current ECS contribution data.
- Findings:
  - `TE-17-01` high, accepted, fixed, verified: deactivation now clears active identity and the
    host-facing full-frame projection in `finally`, while failed undo state remains recoverable and
    reset can discard irrecoverable test residue.
  - `TE-17-02` medium, accepted, fixed, verified: full-frame resolution and projection filter by
    active pack owner and no longer aggregate unrelated enabled pack contributions.
- Verification: focused `PackFullFrameEffectsTest`, `PresentPackFullFrameLifecycleTest`,
  `PresentPackTest`, and `FullFrameTest` passed; final `./scripts/with-art-env.sh test` passed
  656/656 with 0 failures, errors, or ignored tests.
- Closure review: independent focused session `ses_feb688398ffeUrHe3cHiRN6j4f` returned PASS.
- Residual risk: `bindSurfaces`, templates, and windows remain outside this slice and are tracked as
  TE-18 and TE-19.

## Round R19 - Bind-surface declaration migration

- Ledger row: `TE-18`
- Session: pending focused review
- Scope: `PackSurfaceBindingsComponent`, `PackSurfaceBindings`, normalized bind-surface operation,
  `PresentPackApply.applySurfaceBinds`, and focused binding/C2 tests
- Result: PASS
- Governing claim: pack surface profile bindings are immutable owner-scoped ECS contributions;
  `SurfacePresent` is the host-facing ECS projection and legacy field fallback is transitional only.
- Findings:
  - `R19-01` high, accepted, fixed, verified: empty owner contributions now count as present and
    suppress legacy fallback independently of resolved binding values.
  - `R19-02` medium, accepted, fixed, verified: `PresentPack.profileId` is trimmed at construction,
    keeping migrated and fallback projection paths consistent.
  - `R19-03` medium, accepted, fixed, verified: cleanup now snapshots and restores pre-existing
    `SurfacePresent` bindings instead of blindly unbinding them.
- Verification: focused `PackSurfaceBindingsTest`, `PresentPackTest`, `C2LightwaveSurfaceTest`,
  and `LightwaveCoverageTest` passed; `PackSurfaceBindingsTest` now covers both reserved generic
  create and update paths; final `./scripts/with-art-env.sh test` passed 665/665 with
  0 failures, errors, or ignored tests.
- Closure review: `ses_feaf2772fffefqWiEkMCH89Rt4` returned PASS with no findings.
- Residual risk: templates/windows were migrated separately under TE-19.

## Round R20 - Template/window registration migration

- Ledger row: `TE-19`
- Session: pending focused review
- Scope: resource-backed template/window registration, unified PackWorld operation log, and
  normal deactivation versus failed-enable restoration policies
- Result: PASS
- Findings:
  - `TE-19-01` high, fixed: activation failures after operation enable now use `PackWorld.abort`,
    forcing template/window restoration even when normal deactivation flags retain registrations.
  - `TE-19-02` medium, fixed: abort removes the enabled record in `finally` and appends rollback
    failures as suppressed exceptions to the original activation failure.
- Verification: `PresentPackTest.declarationsBecomeOperationsAndRespectDeactivateRegistrationPolicies`
  passed; final `./scripts/with-art-env.sh test` passed 665/665 with 0 failures, errors, or ignored.
- Closure review: independent session `ses_feb0dfa7cffe9xW4r6c5u1WOtY` returned PASS with no
  findings.

## Round R21 - Pack projection ownership and ECS cleanup boundary

- Ledger row: `TE-20`
- Session: `ses_fead5f2adffe2amOrVmnQElV5L`
- Scope: frozen baseline review of `PresentPackApply` and direct pack/surface/render lifecycle
  callers; no source changes were made during review
- Result: FINDINGS
- Governing claim: PackWorld operation ownership and ECS-backed presentation projection must not
  be split across an untracked static cleanup authority; pack cleanup must preserve unrelated ECS
  state and isolate the active pack owner.
- Findings:
  - `TE-20-01` high, accepted, pending fix: static `PresentPackApply` cleanup fields track writes
    to ECS-backed `SurfacePresent` and `RenderStateEcs` state, creating a second mutable ownership
    ledger. Add lifecycle tests for pre-existing full-frame and surface state preservation.
  - `TE-20-02` high, accepted, pending fix: C2 surface-effect projection checks all contributions
    globally instead of the active pack owner and cleanup removes the whole surface state. Add
    multi-pack and pre-existing surface-state tests.
  - `TE-20-03` high, accepted, pending fix: failed activation aborts PackWorld operations but does
    not force projection cleanup after a partial `syncFromActivePack`.
  - `TE-20-04` medium, accepted, pending fix: stale previous-binding snapshots can overwrite an
    external binding update, and cleanup failures are silently forgotten.
- Open questions:
  - Whether multiple packs may remain enabled while only one is active must be made explicit in the
    projection contract.
  - `RenderStateEcs` full-frame and surface state have other production writers; pack cleanup cannot
    assume exclusive ownership of the complete ECS record.
- Verification after fixes: isolated focused runs passed `PackSurfaceEffectsTest`,
  `C2LightwaveSurfaceTest`, `PackFullFrameEffectsTest`, and `PackSurfaceBindingsTest`. The
  combined forced rerun hit Gradle test-output state tracking (`output.bin.idx` missing), not a
  test assertion. The default full gate is currently blocked before tests by unrelated Signal API
  compile errors in untracked `docs/refacter/signal-api/` worktree changes.
- Residual risk: failed activation after partial projection still needs an explicit failure-injection
  test and cleanup contract; listener/resource caches remain outside this slice.

## Round R22 - TE-20 cleanup retry closure

- Ledger row: `TE-20`
- Session: `ses_fead5f2adffe2amOrVmnQElV5L` resumed
- Scope: changed TE-20 cleanup retry behavior in `PresentPackApply` plus focused pack/render tests
- Result: PASS
- Findings:
  - `TE-20-08` medium, accepted, fixed, verified: cleanup now retains failed binding records,
    propagates cleanup failures, and retries them on the next sync; successful records are removed
    only after restoration completes.
- Verification: `PackSurfaceEffectsTest`, `PackSurfaceBindingsTest`, `PackFullFrameEffectsTest`,
  `PresentPackTest`, and full `./scripts/with-art-env.sh test` passed.
- Residual risk: host application exceptions remain intentionally tolerated at individual pack write
  boundaries; no device gate applies to this pure pack/render state slice.

## Round R23 - Published completion checkpoint baseline review

- Ledger row: `TE-21`
- Session: `ses_fe799cb20ffedoOD3W0FQPlL1E`
- Scope: `docs/design/traditional-ecs.md`, task 46 in `docs/task.md`, and the
  `docs/refacter/traditional-ecs/` control files
- Result: PASS after focused fix review
- Governing claim: published project status must agree with the completed TE-01 through TE-20
  ledger and must not present an obsolete incomplete migration as current work.
- Findings:
  - `R23-01` medium, accepted, fixed, verified: the design checkpoint and task 46 still
    described the 2026-08-15 80% baseline and left render rows `46.5`/`46.5.1` incomplete despite
    closed TE-01 through TE-20 evidence. The current checkpoint now declares completion, while the
    baseline remains explicitly historical in the refacter README. The review also found the README
    still described the already-closed TE-21 as active; it now declares all TE-01 through TE-21 rows
    complete. Focused re-review in the same session returned PASS.
- Verification: document consistency search found no remaining active traditional-ECS ledger row;
  `git diff --check` passed. Focused re-review returned PASS. No JUnit, tooling, deployment, or
  device gate applies because this slice changes no production source or behavioral contract.
- Residual risk: future source changes that introduce a duplicate authority must open a new ledger
  row before this completion checkpoint is revised.

## Round R24 - Closure re-audit: residual duplicate authority and dead code

- Ledger rows: `TE-22`, `TE-23`, `TE-24`, `TE-25`
- Session: none recorded. Three `art-reviewer` / `explore` delegations for this round returned empty
  result payloads, so no reviewer session id may be cited. All findings below were established by
  primary-session source inspection and are recorded as self-review, not as a reviewer PASS.
- Scope: changed for TE-22 through TE-25; frozen scope was the 15-file diff plus the new
  `PackLegacyDeclarationMigrationTest`
- Result: FINDINGS (all accepted, fixed, and verified in-session)
- Governing claim: after the TE-21 completion checkpoint, no persistent presentation fact may retain
  a second mutable authority, and no unreachable legacy authority path may remain undocumented.
- Findings:
  - `R24-01` high, accepted, fixed, verified: `PresentationContext.java:18-20` exposed a public
    constructor allocating `new PresentationWorld(scope)` with `ownsWorld=true`, permitting an
    unregistered second mutable presentation world contrary to rule 4. The constructor, the
    `ownsWorld` field, and the `if (ownsWorld) world.close()` branch were removed so
    `PresentationRegistry` is the sole factory over `ArtEcs.world()`.
  - `R24-02` medium, accepted, fixed, verified: every `pack.<legacyField>` fallback in
    `PresentPackApply`, `C1Materializer`, `RenderPlan`, and `PresentationVisuals` was unreachable,
    because `PresentPack.normalizeOperations` always emits an operation for a non-empty legacy field
    and `PresentPacks.activate` always enables operations before projecting. The branches and
    `PresentPackApply.effectDefaultsForType` were deleted after the unreachability proof test passed
    against the pre-deletion code.
  - `R24-03` medium, accepted, fixed, verified: `PresentPackApply.probeSummary` reported
    `effectDefaultTypes`/`fullFrameEffectCount` from raw pack fields, presenting a legacy declaration
    as the pack presentation authority. Both now derive from the owner-scoped ECS contribution via
    the new `PackEffectDefaults.nodeTypes` and `PackFullFrameEffects.effects`.
  - `R24-04` high, accepted, fixed, verified: self-review of the TE-23 diff found the refactor had
    introduced a per-surface `catch (RuntimeException ignored)` into `applyC2SurfaceEffects`, whose
    previously reachable ECS branch had no catch. That would have silently swallowed genuine
    render-write failures and could leave a `PREVIOUS_C2_SURFACES` entry without a matching
    `APPLIED_C2_SURFACES` record. The catch was removed and the intent documented in place.
  - `R24-05` medium, accepted, verified as behavior-preserving: removing the legacy
    `PresentProfiles.contains` pre-check did not weaken validation, because `SurfacePresent.bind`
    rejects an unknown profile at `SurfacePresent.java:31-33` and `applySurfaceBinds` already
    tolerated that rejection per surface. Locked in by
    `PackLegacyDeclarationMigrationTest.unregisteredProfileBindsNothingAndRecordsNoCleanup`.
  - `R24-06` low, accepted, fixed, verified: `skeleton/SkeletonSignals.java` had no reference
    anywhere in the repository including its own string literals; `PresentProfiles` retained five
    zero-caller `@Deprecated` delegations; `LightwaveControls.flushPulses`/`flushPendingCloses` were
    zero-caller empty-bodied no-ops. All were deleted.
  - `R24-07` low, accepted, fixed, verified: `docs/development/api-stability.md:51` documented
    `artframework.c2.NativeTemplateIds` although TE-06 moved the type to `artframework.component`,
    and the baseline inventory `After` column was never measured. Both were corrected.
  - `R24-08` low, accepted, rejected as a violation with evidence: `PackEffectDefaultsComponent`
    returns `byNodeType.keySet()`. This is safe because the backing map is
    `Collections.unmodifiableMap`, whose `keySet` rejects mutation (verified directly), and
    `PackEffectDefaults.nodeTypes` additionally wraps its result in `unmodifiableSet`.
- Verification: `./scripts/with-art-env.sh clean test --rerun-tasks` passed 694/694 with 0 failures,
  0 errors, 0 skipped; `PackLegacyDeclarationMigrationTest` 8/8; the 13 pack/render/C1 classes named
  in the TE-23 row all passed; `scripts/verify-consumer-fixture.sh` passed; a `javac -Xlint:all`
  warning-set diff between `HEAD` and the worktree was identical, proving no dangling reference from
  the deletions.
- Residual risk: no independent reviewer session corroborates this round, so it rests on
  primary-session inspection plus the test and tooling gates. `PresentPack.effectDefaultsFor` is
  retained as an immutable declaration view for `LightwaveCoverageTest`; it is not read by any
  production path. `applySurfaceBinds` still tolerates a per-surface bind rejection by design. No
  device gate applies because no STS hook, draw path, or host lifecycle source changed.

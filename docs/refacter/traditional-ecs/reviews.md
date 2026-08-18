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

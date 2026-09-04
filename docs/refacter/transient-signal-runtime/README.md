# Transient signal runtime refacter

## Goal

Migrate transient ECS command and event wrappers to synchronous runtime signals. Preserve
persistent ECS presentation state and the existing synchronous public API semantics.

Transient inputs and lifecycle notifications must dispatch through the runtime signal protocol;
they must not become durable ECS state. Persistent projection, component, surface, and frame state
remain ECS-owned and are updated only through their established authority paths.

## Non-goals

- Do not move game, endpoint, or backend authority into ART signals.
- Do not replace `ContextFrame`, projection, or persistent presentation components with events.
- Do not change signal ordering, replacement, stop, or synchronous return semantics.
- Do not add downstream protocol types, game rules, native renderer rewrites, or device work.
- Do not perform unrelated ECS naming, storage, or API cleanup.

## Authority model

```text
native/UI operation -> synchronous signal -> interceptor/consumer -> intent/result
backend authority -> immutable ContextFrame -> ECS projection/persistent presentation state
```

The Primary Backend remains the sole authority for a display domain. Signals request, observe, or
route transient work; they do not commit domain truth. A later authority frame is the convergence
source. Runtime signal dispatch remains ordered, interceptable, and synchronous, including its
existing result/replace/stop behavior.

## Source design docs

- [`backend-context.md`](../../design/backend-context.md): authority, frames, intents, SignalBus.
- [`node-signal-runtime.md`](../../design/node-signal-runtime.md): SignalGroup and runtime rules.
- [`c2-full-present.md`](../../design/c2-full-present.md): native invocation and intent boundary.
- [`art-framework.md`](../../design/art-framework.md): presentation graph and host SPI.
- [`presentation-entity-runtime.md`](../../design/presentation-entity-runtime.md): ECS ownership and lifecycle.

## Ordered review tree

Review one frozen ledger row at a time, in order:

1. Signal protocol and synchronous compatibility contract.
2. Surface lifecycle command migration.
3. Surface intent command and result migration.
4. Native intent lifecycle event migration.
5. Authority frame ingestion boundary.
6. Business confirmation frame wrapper boundary.
7. Cleanup and negative inventory.
8. Final verification and closure.

New ownership questions require a new ledger row; do not silently expand a frozen row.

## Verification policy

- Start each behavior change with focused JUnit 4 coverage for synchronous dispatch and lifecycle.
- Run the applicable focused test after each completed row, then the default JUnit gate when feasible.
- Preserve existing public synchronous API behavior with compatibility tests before removing a wrapper.
- Use negative searches before closure to prove transient wrappers no longer persist as ECS state and
  persistent state has not been routed through transient signals.
- Review evidence never replaces test evidence. Native/device validation is only required when a
  changed host boundary requires it and is separately scoped.

## Completion definition

The project is complete only when every ledger row is `complete`, each migrated transient wrapper
has one synchronous signal authority, persistent ECS state remains on its frame/projection path,
focused compatibility and lifecycle tests pass, required gates and reviews are recorded, and final
negative inventory finds no stale command/event wrapper or authority bypass in scope.

## Checkpoint

- State: project complete. TSR-01 through TSR-08 are complete based on focused tests, the
  successful full gate, review evidence, and negative inventory.
- Active row: none.
- Next action: none. The full gate passed after Gradle JUnit XML stdout/stderr and HTML report
  generation were disabled in `build.gradle.kts`.

## Final review fixes

- The `ArtFramework` singleton remains the sole schedule whose consumers attach to the process-wide
  `transient-runtime` group. Public ad-hoc `PresentationSchedule` instances receive isolated runtime
  groups and expose `close()`/`resetForTests()` for explicit listener disposal. Their dispatches are
  therefore independent rather than duplicated on the process-global group.
- Typed runtime dispatch has two ordered stages: ordinary runtime listeners are interceptors and
  execute with existing `SignalBus` behavior; committing schedule consumers execute only after that
  stage continues, using its effective replacement signal. A stop therefore has no consumer side
  effect. Raw `UiSignal` dispatch and raw native `SignalBus` ordering remain unchanged.
- Authority confirmation is dispatched only after an authority signal reaches its consumer. Its
  `after` frame is the consumer's effective replacement frame, so rejected authority signals never
  confirm durable requests against an unprojected frame.
- Unavailable authority frames are admitted monotonically by epoch; shared schedules serialize
  dispatch and projection, and queued surface intents record their handled result rather than
  losing it. The traditional-ecs design wording now describes the transient-signal migration
  boundary, and unavailable-epoch fixtures use the corrected epoch setup.

## Final verification checkpoint

- Focused schedule/authority/surface gate: passed.
- Extended focused core/context gate: 123 actual passing tests.
- Separately rerun STS/map/render classes: 29/29 passed.
- `PresentProjectionTest` + `StrongFrameViewsTest` focused gate: passed; high-epoch unavailable
  cleanup is covered.
- Full gate: `./scripts/with-art-env.sh clean test --no-daemon --no-parallel` passed with
  `BUILD SUCCESSFUL` and 6 actionable tasks executed, after disabling Gradle JUnit XML
  stdout/stderr and HTML report generation in `build.gradle.kts`.
- Final review session: `ses_f9408a74dffeL9vk22Ck4WQe0Z` reported the sole finding
  `TSR-FINAL-01-01`, which fixed the stale ledger blocker and was verified by the successful full
  gate. This records the finding disposition, not a reviewer `PASS`.
- Earlier session `ses_f941ec280ffeSZrkbeqse3VSh7` returned no usable final report and is recorded
  as no response.
- Negative inventory: no references to the five deleted wrappers remain in `src/main/java` or
  `src/test/java`.

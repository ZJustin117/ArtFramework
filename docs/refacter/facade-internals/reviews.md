# Facade Internals Reviews

## Round R1 - Initial facade internal extraction

- Session: `ses_fdf3fc675ffe3uGxcxWVx6eRTz`
- Scope: changed after FI2-01 fix; final re-review pending
- Result: FINDINGS
- Findings:
  - `FI2-01`, medium, accepted and fixed: `ArtFramework.resetForTests()` duplicated the signal
    reset calls now owned by `FrameworkTestReset`. Focused and full JUnit passed after removing the
    duplicate facade calls.
  - `FI2-02`, medium, partially addressed: added facade `publishFrame` bridge coverage in
    `PresentationScheduleTest`; existing `MountHostTest`, `SyntheticRuntimeTest`, and full JUnit
    cover native alias, recreation, retirement, and host attach/detach behavior. The bridge remains
    package-private and delegates to the single `PresentationSchedule` instance.
- Residual risk: internal reset coordination is intentionally test-only and still has a broad cleanup
  list; future reset additions must be made in `FrameworkTestReset`, not in the facade.

The review scope changed after FI2-01 was fixed and requires the same session's final re-review before
the ledger rows can be marked complete. The continuation was requested twice but did not return a
consumable final report in this session. The parent therefore does not claim a reviewer PASS; the
remaining evidence is the initial findings review, focused tests, full semantic gate, consumer
fixture, and negative inventory.

## Negative Inventory

- `ArtFramework` no longer contains `OPEN`, `DEFS`, `TrackedHandle`, `closeSynthetic`, or the
  synthetic retirement hook implementation.
- Direct schedule calls from `ArtFramework` were replaced by `FrameworkScheduleBridge`; the single
  `PresentationSchedule` instance remains the production authority.
- Reset calls from `ArtFramework` were replaced by `FrameworkTestReset`; the two signal reset calls
  are present only in that coordinator.
- Extracted coordinators are package-private and introduce no consumer-facing API.
- `git diff --check` passed.

The review scope is limited to the internal extraction described in `ledger.md`. Any finding that
changes public behavior, ECS authority, or schedule ordering must be recorded as a new bounded row
before implementation proceeds.

## Round R2 - Final alias and authority review

- Session: `ses_fde1981c1ffeNFArwF60xKH4ih`
- Scope: changed after R2-01 fix; focused re-review pending
- Result: FINDINGS
- Findings:
  - `R2-01`, medium, accepted and fixed, focused tests passed: native definition aliases were
    retained after `unregisterWindow` and after re-registering the same definition id. The
    coordinator now removes all aliases belonging to the old `WindowDef` before registration and
    removes all aliases on unregister. `MountHostTest` covers both lifecycle paths.
- Residual risk: the reviewer continuation was requested for the changed scope but has not returned
  a consumable final report, so no reviewer PASS is claimed. The full semantic gate currently has a
  repository baseline failure of 22 tests out of 698, concentrated in combat/map/presentation-world/
  intent paths outside this bounded facade slice.

## Verification Evidence

- Focused command: `./scripts/with-art-env.sh test --tests 'artframework.api.MountHostTest' --tests
  'artframework.api.ArtFrameworkTest' --tests 'artframework.c1.SyntheticRuntimeTest'` — BUILD SUCCESSFUL.
- Consumer command: `./scripts/verify-consumer-fixture.sh` — passed.
- Formatting: `git diff --check` — passed.
- Full command: `./scripts/with-art-env.sh clean test --rerun-tasks` — 698 tests completed, 22 failed;
  no failures were in the focused facade test set, but the full gate is not a completion gate until
  those baseline failures are dispositioned.

## Round R6 - Final collision lifecycle review

- Session: `ses_fddcda8cbffed5xiFxG5N0M8ck`
- Scope: frozen after R5 collision cleanup
- Result: PASS
- Findings:
  - None. `closeDefinitionHandles` closes old tracked handles by `WindowDef` identity before
    definition aliases are replaced; the reviewer found no actionable finding in lifecycle,
    schedule, reset, or facade authority boundaries.
- Residual risk: review did not run tests. Multiple simultaneous old owners and unrelated-definition
  preservation are not independently asserted, although the identity-based implementation was
  source-reviewed and the focused collision test passed.

## Final Verification Status

- Focused JUnit: `MountHostTest`, `ArtFrameworkTest`, `PresentationScheduleTest`, and
  `SyntheticRuntimeTest` — BUILD SUCCESSFUL.
- Consumer fixture: `./scripts/verify-consumer-fixture.sh` — passed.
- Formatting: `git diff --check` — passed.
- Full semantic gate: blocked by 22 existing failures out of 698 tests in combat/map/
  presentation-world/intent paths outside this bounded facade scope.

## Round R7 - LML report output stability

- Session: `ses_fdd5b2febffeCTcALBXRtbVqdE`
- Scope: changed after stderr regression assertion was added
- Result: FINDINGS
- Findings:
  - `R7-01`, low, accepted and fixed, verified: `LmlUiNodeLoaderTest.rejectExternalEntity` now
    captures and restores `System.err` in a `finally` block and asserts that secure DTD rejection
    produces no parser stderr output. The production `ErrorHandler` converts parser diagnostics to
    exceptions instead of allowing JAXP's default stderr handler to emit output. Focused LML and
    full semantic gates passed afterward.
- Residual risk: the requested continuation did not return a consumable final report; source and
  test behavior are covered by the passed focused/full gates. No facade ownership scope changed.

## Final Closure

- Facade review: R6 PASS, session `ses_fddcda8cbffed5xiFxG5N0M8ck`.
- Full semantic gate: `./scripts/with-art-env.sh clean test --rerun-tasks` — BUILD SUCCESSFUL,
  all 728 tests passed.
- LML focused gate: `LmlUiNodeLoaderTest` — BUILD SUCCESSFUL, including no-stderr security test.
- Consumer fixture: `./scripts/verify-consumer-fixture.sh` — passed.
- Formatting: `git diff --check` — passed.
- Project completion decision: all four facade-internals ledger rows are `complete`; the independent
  LML parser/reporting fix is recorded as R7 and verified without changing the facade authority scope.

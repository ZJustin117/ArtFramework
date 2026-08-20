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

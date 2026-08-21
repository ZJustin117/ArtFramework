# Regression Baseline Reviews

## Round R1 - Baseline ownership review

- Sessions: `ses_fdc81e672ffeyl12Xcjeq6ipsD`, `ses_fdc7fb1eaffeOTa02S19THYFky`,
  `ses_fdc7fafc2ffeF3kynNOeCLec8X`, `ses_fdc7fb05effeYlVcOEj7WjUTkX`,
  `ses_fdc7fb023ffelUeCVAHMNGVCKl`
- Scope: frozen
- Result: FINDINGS
- Findings:
  - `R1-01` high, accepted, fixed pending verification: registry-owned `PresentationWorld` could
    be directly closed, permanently invalidating the process-wide authority. The registry world is
    now non-closeable and a focused lifecycle test was added.
  - `R1-02` medium, accepted, fixed pending verification: `PresentationContext.entity()` mutated
    ownership during lookup. The lookup is now read-only and a focused duplicate-recreation test
    was added.
  - `R1-03` medium, accepted, pending implementation: `LightwaveControls.tickPulses()` advances
    effect state outside the production schedule.
  - `R1-04` high, accepted, pending implementation: same-window `NodeConnections` cleanup is not
    context-aware and can disconnect a replacement context.
  - `R1-05` medium, accepted, pending implementation: failed `NodeConnections.syncContext` can
    leave partial subscriptions.
  - `R1-06` high, accepted, pending implementation: `RenderHost.bindEffect` can create host-only
    effects on ECS-owned targets that disappear after cache recreation.
  - `R1-07` medium, accepted, pending implementation: EntityPresent detach/clear can leave stale
    render targets until an unrelated projection.
  - `R1-08` medium, pending decision: EntityPresent effect retention policy is not explicit.
  - `R1-09` medium, accepted, pending implementation: release gate does not verify Java 8 class
    file versions.
  - `R1-10` medium, accepted, pending implementation: consumer contract test does not cover the
    documented facade compatibility surface.
  - `R1-11` low, accepted, pending documentation: api-stability.md does not explicitly list
    `dispatch()` although consumer.md documents it.
- Open questions:
  - Should EntityPresent slots intentionally be effect-free, or should slot effects become ECS
    data? This must be resolved before RB-04 can close.
  - Is `LightwaveControls.tickPulses()` a test-only compatibility helper or a supported production
    API? The current public visibility does not establish that boundary.
- Residual risk:
  - This project does not establish device pixel-rendering coverage; D1 remains governed by the
    existing documented scenarios.

## Verification Round R2 - Implemented baseline fixes

- Sessions: `ses_fdc79618bffeJQBjE31QNb7DD8`, `ses_fdc749c3efferDelWMcYJXpI3G`,
  `ses_fdc7fb05effeYlVcOEj7WjUTkX`, `ses_fdc7fb023ffelUeCVAHMNGVCKl`
- Scope: changed
- Result: FINDINGS
- Findings:
  - `R2-01` accepted, fixed, verified: shared registry world direct close was rejected and
    focused RB-01 tests passed.
  - `R2-02` accepted, fixed, verified: context lookup no longer mutates ownership; reset uses the
    registry lifecycle path; full semantic gate passed.
  - `R2-03` accepted, fixed, verified: same-window cleanup is context-aware and failed connection
    sync rolls back; focused RB-03 tests passed.
  - `R2-04` accepted, fixed, verified: EntityPresent detach/clear immediately projects cache removal
    with one rebuild for clear; focused RB-04 tests passed.
  - `R2-05` accepted, fixed, verified: release gate checks Java 8 class-file major versions and
    consumer smoke covers documented facade calls; release gate passed.
  - `R2-06` unresolved: `LightwaveControls.tickPulses()` remains a public schedule bypass.
  - `R2-07` unresolved: `RenderHost.bindEffect` can write host-only effects for ECS-owned targets.
  - `R2-08` unresolved decision: EntityPresent slot effect retention versus an explicit effect-free
    contract is not yet recorded in the production design.
- Open questions:
  - Resolve R2-06 and R2-07 as separate bounded ownership slices.
  - Decide and document R2-08 before extending EntityPresent render state.
- Residual risk:
  - No D1 device or pixel-rendering gate was applicable to this test-only and lifecycle baseline
    work.

## Round R3 - RB-02 schedule bypass

- Sessions: `ses_fdc589a8fffeUFE2flIjcLmAm1`, `ses_fdc4c127fffew1ol0grKXw6U3a`
- Scope: frozen
- Result: PASS
- Findings:
  - `R2-06-01` accepted, fixed, verified: `LightwaveControls.tickPulses` no longer directly
    advances `EffectPulse`; it delegates to schedule-owned `EffectPulseSystem` execution.
  - `R2-06-02` accepted, fixed, verified: the pulse-only compatibility helper is explicitly
    deprecated while remaining linkable, and its focused test proves unrelated host/render phases
    do not run.
- Open questions:
  - None for RB-02.
- Residual risk:
  - RB-04 `RenderHost.bindEffect` authority and EntityPresent effect policy remain out of scope.

## Round R4 - RB-04 ECS-owned render target binding

- Session: superseded by `ses_fdc0f5ae1ffemBI2TOEBJOAUN4`
- Scope: frozen
- Result: PASS (superseded)
- Findings:
  - Implementation completed: public `RenderHost.bindEffect` rejects ECS-owned target kinds;
    internal `bindPlannedEffect` is used only while materializing immutable ECS render plans;
    overlay compatibility targets remain bindable; focused render tests and the full semantic gate
    pass.
- Open questions:
  - Final reviewer disposition was superseded by R6 after the effect-free policy was documented.
- Residual risk:
  - R6 records the current-tree release gate and consumer fixture reruns.

## Round R5 - RB-04 EntityPresent effect policy

- Session: superseded by `ses_fdc0f5ae1ffemBI2TOEBJOAUN4`
- Scope: frozen
- Result: PASS (superseded)
- Findings:
  - Implementation and verification are complete: EntityPresent is explicitly effect-free in the
    current API; `RenderPlan` emits no slot effects; host-only binding is rejected; a focused
    recreation/rejection test passes; full semantic gate passes.
  - Final reviewer response was superseded by R6 after the current-tree policy and tests were
    rechecked.
- Open questions:
  - None in the selected policy. Future FX must use a separate ECS-owned visual/overlay entity.
- Residual risk:
  - No D1 gate is applicable.

## Round R6 - Final closure review

- Session: `ses_fdc0f5ae1ffemBI2TOEBJOAUN4`
- Scope: changed, current worktree and all project records
- Result: PASS
- Findings:
  - `RB-04-01` medium, accepted, fixed, verified: EntityPresent effect-free policy was implemented
    in design, render plan, host rejection, and focused recreation tests; ledger wording was
    corrected to record the policy as closed.
  - `RB-04-02` medium, accepted, fixed, verified: `NodeConnections.syncContext()` now stages a
    replacement and preserves old subscriptions if resync fails; focused regression coverage was
    added and passed.
  - `RB-04-03` medium, accepted, fixed, verified: current-tree full semantic, release gate,
    consumer fixture, and diff checks were rerun after the latest changes and recorded in the
    ledger.
  - `RB-04-04` low, accepted, fixed, verified: RB-01/RB-03/RB-05 review fields and historical
    pending records were reconciled with the final closure disposition.
- Verification:
  - `./scripts/with-art-env.sh test` passed after the transactional resync test and final policy
    documentation.
  - `./scripts/release-gate.sh` passed after the same changes, including offline art-verify, Java 8
    bytecode, version, and consumer checks.
  - `./scripts/verify-consumer-fixture.sh` and `git diff --check` passed.
- Open questions:
  - None for `regression-baseline`.
- Residual risk:
  - Device/D1 and pixel-rendering verification remain out of scope for this pure/runtime baseline.

## Round R7 - Persisted closure consistency

- Session: `ses_fdc086f7effefAy7UpzFqTmWmF`
- Scope: frozen project records and current diff summary
- Result: PASS
- Findings:
  - None. README, ledger, and review disposition are internally consistent and all ledger rows are
    complete.
- Open questions:
  - None for `regression-baseline`.
- Residual risk:
  - The project directory is currently untracked and must be included in the eventual commit.
  - D1/device and pixel-rendering verification remain out of scope.

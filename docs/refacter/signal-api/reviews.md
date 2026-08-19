# Signal API review log

Reviewer sessions are advisory evidence. JUnit, tooling, and device gates remain separate.

## Round R1 - Baseline and public-boundary decision

- Ledger row: `SA-01`
- Session: `ses_feac908e6ffec3HMJjg2M8c329`
- Scope: frozen after initial inventory; no production edits
- Result: FINDINGS
- Governing claim: `SignalBus` / `SignalGroup` remain the single dispatch authority, while the
  public C1/C2/consumer boundary has one callback and one send-result model unless an explicit
  compatibility decision says otherwise.
- Findings:
  - `R1-01` high, accepted, pending fix: scoped C1/C2 APIs currently accept only `SignalHandler`; add `SignalListener` overloads first, then retain handler behavior as a one-way compatibility adapter.
  - `R1-02` high, accepted, pending fix: declarative exact/regex subscriptions rely on context-owned `SignalHub` cleanup; raw Bus boundary changes must preserve that lifecycle owner.
  - `R1-03` medium, accepted, pending fix: result-bearing send is named `emit` at raw group/facade but `dispatch` at scoped layers; freeze `dispatch` as canonical and retain `emit` only as a delegating convenience alias.
- Open questions:
  - 是否保留 `SignalHandler` 作为至少一个兼容周期的 public API，还是直接迁移并删除？
  - `emit()` 是否统一返回 `SignalDispatchResult`，还是保留一个明确命名的 fire-and-forget convenience API？
  - `SignalHub.connectBus(...)` 是否继续 public，还是移动到 `SignalGroup` / `ArtFramework.signals()`？
- Verification: focused contract tests required before SA-02 implementation; fixes require focused re-review.
- Residual risk: external binary consumers are not observable from this repository; public API removal
  requires an explicit compatibility decision and consumer fixture evidence.

## Finding Disposition

| ID | Severity | Disposition | Fix evidence | Verification |
|---|---|---|---|---|
| pending | pending | pending | pending | pending |

| R1-01 | high | accepted, pending | Add scoped `SignalListener` overloads and adapter tests | pending |
| R1-02 | high | accepted, pending | Preserve context-owned subscription cleanup while separating raw Bus access | pending |
| R1-03 | medium | accepted, pending | Add canonical `dispatch` result path and delegate-only `emit` aliases | pending |

## Round R3 - Reset and adapter lifecycle

- Ledger rows: `SA-04`, `SA-07`
- Session: `ses_fe9c2a0abffeighhSYlL96OzGK`
- Scope: changed `PresentationRuntime`, `ArtFramework`, `SyntheticComponents`, `NativeComponents`,
  `PresentSurfaces`, and the focused signal contract boundary
- Result: FINDINGS, fixes implemented and awaiting re-review
- Findings:
  - `R5-08` high, accepted, fixed pending verification: added `PresentationRuntime.resetSignalsForTests()`
    and invoked it from `ArtFramework.resetForTests()` so scoped hub caches are cleared with the global bus.
  - `R5-09` high, accepted, fixed pending verification: synthetic adapter `unmount()` now clears its
    scoped signals and delegates close to the framework lifecycle authority.
  - `R5-10` medium, accepted, fixed pending verification: `NativeComponents.dispatch` and
    `PresentSurfaces.dispatch` now expose result-bearing registry operations; legacy static `emit` delegates once.
- Verification: `SignalApiContractTest` passed 8/8 after fixes; full gate and focused re-review pending.
- Residual risk: public synthetic mount remains intentionally a no-op because framework registration/open
  is the lifecycle authority; this is unchanged and outside the current signal dispatch slice.

## Round R4 - Lifecycle fix closure

- Ledger rows: `SA-04`, `SA-07`
- Session: `ses_fe9ba8fbbffe489klId7TUaGtz`
- Scope: `SyntheticComponents`, `SyntheticRuntime`, `PresentationRuntime`, `ArtFramework`, and
  `SignalApiContractTest` reset coverage
- Result: PASS
- Findings: none
- Verification: reviewer confirmed the recursive synthetic close path is removed and the framework
  reset clears scoped hubs; `SignalApiContractTest` passed 9/9 after the fixes.
- Residual risk: `SyntheticComponents.onClosed()` is normally a no-op after `closeContext()` removes
  the context; production cleanup is already performed by `closeContext()` before registry removal.

## Round R5 - SA-04 through SA-07 inventory

- Sessions:
  - SA-04: `ses_fe9aeb0c9ffeo9Htp4Kx529Zn7`
  - SA-05: `ses_fe9aacaa4ffe0R1JeuGO0j8A4Y`
  - SA-06: `ses_fe9a79b68ffeTNMa8mh98m3lXM`
  - SA-07: `ses_fe9a4b4b8ffe5Xxt92CUIQDaBx`
- Scope: raw/scoped ownership, declaration/path contract, signal-name taxonomy, and production
  consumer migration after C1/C2 removal from the project boundary
- Result: FINDINGS, bounded fixes in progress
- Findings:
  - `SA-04-01` medium, accepted, fixed pending verification: empty instance IDs no longer clear or
    report raw registrations through `clearInstance` / `handlerCount`.
  - `SA-04-02` medium, accepted, fixed pending verification: `PresentationRuntime.clearSignals` now
    clears declarative connection and state-machine registries before removing the scoped hub.
  - `SA-04-03` medium, accepted, fixed pending verification: `NodeStateMachine.wire` rejects a context
    different from its constructor-owned context.
  - `SA-05-01` and `SA-05-02` high, accepted as follow-up: host adapters still have adapter-level
    declaration tables; a broader ECS declaration migration must be isolated from this API slice.
  - `SA-05-03` medium, accepted as follow-up: path segment canonicalization requires an explicit legal
    ID character decision and is not changed in this compatibility slice.
  - `SA-05-04` medium, accepted, fixed pending verification: `SignalPortsComponent` now trims,
    validates, and deduplicates declarations consistently with `UiNode`.
  - `SA-05-05` medium, accepted as follow-up: mounted/identity validation needs a separate lifecycle
    contract slice.
  - `SA-06-01` medium, accepted, fixed pending verification: canonical docs now use the existing wire
    name `finished` for skeleton completion.
  - `SA-06-02` medium, accepted as documented residual: `card_pressed` remains a declared host capability
    whose producer is host-specific and not invented by this refactor.
  - `SA-06-03` low, accepted, fixed pending verification: consumer docs now list `dispatch` and explain
    void compatibility `emit`.
  - `SA-07-01` high, accepted, fixed pending verification: `UiInspect.emit` now routes through dispatch
    and maps rejected/handled results instead of always returning OK.
  - `SA-07-02` medium, accepted as residual follow-up: lab listener invalidation on target recreation is
    not changed without a lifecycle ownership design.
  - `SA-07-03` medium, accepted as residual follow-up: UiOps compatibility registration maps remain a
    separate compatibility cache and require a dedicated invalidation slice.
  - `SA-07-04` medium, accepted as documented compatibility limitation: legacy external UiComponent
    implementations receive the explicit default continuation fallback until migrated.
- Verification: focused contract passed 9/9 after fixes; final full gate pending.
- Residual risk: SA-05 adapter declaration parity, path legal-character normalization, lifecycle identity
  validation, UiLabListeners recreation, and UiOps registration invalidation remain separately tracked
  follow-ups rather than silently folded into this slice.

## Round R6 - Final closure gate

- Session: `ses_fe99a6122ffexjzPPh8vK4xTUH`
- Scope: final signal-api closure, ledger consistency, compatibility signatures, and worktree scope
- Result: BLOCKED
- Findings:
  - `SA-08-01` high, accepted, fixed: public result-bearing `emit` descriptors were restored for
    `SignalGroup`, `ArtFramework`, and `UiComponent`/host adapters; `dispatch` remains the canonical
    result-aware name and internal result callers use it.
  - `SA-08-02` high, accepted, blocked: worktree contains unrelated TE-20 traditional-ECS, pack,
    render, and test changes. No files were reverted; SA-08 remains blocked until ownership is isolated
    or the user explicitly scopes those changes into this closure.
  - `SA-08-03` medium, accepted, fixed: SA-05 evidence now records focused 9/9 and full 679/679 while
    retaining path/lifecycle normalization as explicit future work.
- Verification: after restoring public emit descriptors, focused and full JUnit should be rerun before
  any final closure decision.
- Residual risk: external binary compatibility is source-inspected, not verified against a prior released
  artifact; unrelated worktree ownership is the current closure blocker.

## Round R2 - Scoped callback and send-result slice

- Ledger rows: `SA-02`, `SA-03`
- Session: `ses_feac908e6ffec3HMJjg2M8c329` (resumed)
- Scope: changed `SignalHub`, `SignalGroup`, `UiComponent`, `PresentationRuntime`, `ArtFramework`,
  C1/C2 component implementations, and `SignalApiContractTest`
- Result: FINDINGS, fixes in progress
- Findings:
  - `R5-01` high, accepted, fixed pending verification: restored the original `void emit(...)`
    descriptors in `SignalHub` and `PresentationRuntime`; result-bearing behavior remains in `dispatch`.
  - `R5-02` medium, accepted, fixed pending verification: new `UiComponent` methods now have Java 8
    defaults so old external implementations do not fail at linkage; unsupported decision-aware
    listeners fail explicitly rather than silently changing semantics.
  - `R5-03` medium, accepted, fixed pending verification: C2 validation and Bus dispatch now live in
    `dispatch`; `emit` delegates once.
  - `R5-04` medium, accepted as SA-04 constraint: raw subscriptions remain context-owned until the
    lifecycle boundary is migrated and tested separately.
  - `R5-05` low, accepted, fixed pending verification: focused contract coverage expanded for listener
    isolation, replacement, both stop terminals, and context raw-subscription cleanup.
- Verification: focused test pending after fixes; no full gate yet.
- Residual risk: default `UiComponent.dispatch` cannot recover a meaningful result from legacy
  third-party implementations; this is documented compatibility behavior and remains non-authoritative.

## Round R7 - Expanded-scope closure correction

- Session: `ses_fe99a6122ffexjzPPh8vK4xTUH` resumed
- Scope: expanded worktree scope explicitly includes traditional-ECS TE-20 pack/render changes
- Result: FINDINGS, bounded fixes in progress
- Findings:
  - `SA-08-01` high, accepted, fixed, verified: `SignalGroup.emit(UiSignal)` retains the public
    result-bearing descriptor and delegates once to `dispatch`.
  - `SA-08-02` medium, accepted, fixed, verified: explicit user scope resolves the earlier unrelated
    worktree blocker; ledger evidence now owns the included TE-20 pack/render paths.
  - `SA-08-03` medium, accepted as bounded compatibility risk: `SignalHub.registrations` is a
    non-authoritative cleanup index over global subscriptions; lifecycle cleanup tests cover reset and
    context cleanup. A multi-hub stress concern remains a separate future slice.
  - `SA-08-04` low, accepted as documented compatibility limitation: legacy external
    `UiComponent.dispatch` returns only the explicit continuation fallback.
- Verification: focused `SignalApiContractTest` and full `./scripts/with-art-env.sh test` passed after
  the public descriptor fix; final closure still requires TE-20 review closure and final inventory.
- Residual risk: path canonicalization, lifecycle identity validation, UiLabListeners recreation, and
  UiOps compatibility-cache invalidation remain explicitly out of scope follow-ups.

## Round R8 - Final expanded-scope closure

- Sessions: `ses_fe99a6122ffexjzPPh8vK4xTUH` resumed; TE-20 closure `ses_fead5f2adffe2amOrVmnQElV5L`
- Scope: SA-01 through SA-08 plus explicitly included TE-20 pack/render changes
- Result: PASS after evidence reconciliation
- Findings:
  - `SA-08-01` medium, accepted, fixed, verified: TE-20 cleanup retry evidence is persisted in
    traditional-ecs Round R22 and TE-20 is complete.
  - `SA-08-02` resolved: the user explicitly expanded scope to include the traditional-ECS pack and
    render changes; the historical worktree blocker no longer applies.
  - `SA-08-03` accepted as bounded compatibility architecture: `SignalHub.registrations` is a
    non-authoritative cleanup index; mixed scoped/raw multi-hub cleanup is covered by
    `SignalApiContractTest.hubCleanupIndexesDisconnectMixedScopedAndRawSubscriptions`.
  - `SA-08-04` accepted as documented compatibility behavior: legacy external components receive the
    explicit continuation fallback and cannot claim a decision-aware result.
- Verification: `SignalApiContractTest` passed 11/11; full `./scripts/with-art-env.sh test` passed;
  public `SignalGroup.emit` compatibility test passed; `git diff --check` passed.
- Residual risk: SA-05 path canonicalization/lifecycle identity, SA-07 lab-listener recreation and
  UiOps compatibility-cache invalidation remain separate future refacter projects.

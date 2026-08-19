# Traditional ECS refacter

## Goal

Complete the traditional ECS convergence defined by
[`docs/design/traditional-ecs.md`](../../design/traditional-ecs.md). Persistent ART presentation
facts must have one ECS authority, production phases must have explicit stateless-system ownership,
and compatibility or host containers must be disposable, non-authoritative boundaries.

## Non-goals

- New product behavior or full-present surfaces
- Downstream protocol, party, combat-authority, or dual-device life work
- Replacing legitimate callback, asset, GL, native-object, or provider caches with components
- Broad package cleanup without an identified duplicate responsibility
- Adding coverage tooling as part of an unrelated ownership slice

## Checkpoint

- Project: `traditional-ecs`
- Started: 2026-08-16
- Design checkpoint: approximately 80% complete as of 2026-08-15
- Active slice: TE-20 pack projection ownership and ECS cleanup boundary
- State: complete through `TE-20`; no residual traditional-ECS row is currently unclassified
- Last semantic gate: `./scripts/with-art-env.sh test` passed after TE-20 and Signal API
  worktree integration on 2026-08-18
- Last device evidence: final R5 review PASS; D1 deployment verified `ArtFramework.jar` at
  940,143 bytes, SHA-256 `de37ff59d9afee3d24d3bca988e3bdb68a0a9125663966645fd30ddf7811893d`;
  cold start reached READY; `art lab host-recreate` executed; `scripts/art-lab combat verify-full`
  passed `d1_full_present_combat_ready` on 2026-08-17
- Next action: preserve the final negative-inventory evidence; reopen only if a new duplicate
  authority is identified

## Authority

The governing rules are in [`docs/design/traditional-ecs.md`](../../design/traditional-ecs.md):

1. Entities have identity and independent lifecycle only.
2. Components contain data only.
3. Systems are stateless and run in an explicit order.
4. Persistent presentation facts have one ECS authority.
5. Native, scene2d, GL, callback, listener, and reflection objects remain outside components.
6. Compatibility APIs may derive immutable views but may not retain another mutable truth.

## Review Tree

Review depth-first, one ledger row at a time:

1. World and context ownership: shared world, ownership index, close/reset destruction
2. Component model: C1, C2, intent, render, skeleton, and EntityPresent data boundaries
3. Schedule ownership: production phase order and synchronous compatibility execution
4. Render authority: plans, dirty queue, immediate projection, and cache recreation
5. Host adapters: C1, C2, STS1 lifecycle and disposable native caches
6. Compatibility surfaces: API, projection, surfaces, probes, console, and residual stores
7. Dependency direction: core-to-host imports and host/runtime type leakage

New independent concerns discovered during review receive a new `TE-*` ledger row rather than being
silently folded into the active change.

## Verification

- Focused JUnit 4 starts every behavior-changing slice where feasible.
- `@junit-test` is the mandatory semantic gate after coherent runtime, API, ECS, or render changes.
- Offline `@art-verify` is required only when its runner or fixture YAML changes.
- Source reaching STS hooks, draw paths, or host lifecycle requires JUnit first, then the applicable
  deploy and documented D1 verification.
- Arthas is diagnostic-only and cannot close a ledger row.

## Completion

The project is complete only when every ledger row has after-state evidence, focused test evidence,
applicable full gates, closed review findings, and no unclassified duplicate authority. Update the
checkpoint and next action at the end of every work session.

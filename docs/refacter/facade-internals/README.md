# Facade Internals Refacter

## Goal

Split internal coordination responsibilities out of `artframework.api.ArtFramework` without
changing the public facade API or introducing another presentation-state authority.

## Non-goals

- No public method removal or rename.
- No change to C1/C2 lifecycle semantics.
- No change to `PresentationWorld`, `PresentationSchedule`, SignalBus, or host ownership.
- No new consumer-facing API layer.
- No broad package cleanup or behavior changes.

## Architecture Authority

- [`docs/development/api-overview.md`](../../development/api-overview.md): facade versus typed and
  host API boundaries.
- [`docs/design/presentation-entity-runtime.md`](../../design/presentation-entity-runtime.md): ECS
  authority and disposable host caches.
- [`docs/design/traditional-ecs.md`](../../design/traditional-ecs.md): stateless systems and one
  mutable presentation authority.
- [`docs/development/api-stability.md`](../../development/api-stability.md): compatibility surface.

## Checkpoint

- Project: `facade-internals`
- Active slice: internal coordination extraction
- State: complete with review-tool follow-up
- Scope: `ArtFramework.java`, new package-private coordination classes, focused API tests, and this
  project record.
- Verification: `./scripts/with-art-env.sh test` passed; `./scripts/verify-consumer-fixture.sh` passed;
  `git diff --check` passed. Initial review finding FI2-01 (duplicate signal reset) was fixed; final
  re-review is pending.
- Next action: record the final review result, complete the negative inventory, and close or reopen
  the ledger rows based on evidence. The requested final reviewer continuation did not return a
  consumable result in this session, so no PASS is claimed for that follow-up.

## Review Tree

1. Lifecycle coordinator: handle aliases, synthetic close, retirement hook, and recreation.
2. Schedule bridge: synchronous compatibility calls delegate to one schedule instance.
3. Test reset coordinator: reset ownership remains test-only and clears all registered caches.
4. Facade compatibility: public methods and consumer fixture remain linkable.
5. Negative inventory: no duplicate lifecycle, schedule, or reset authority is introduced.

## Verification Policy

- Focused JUnit starts with `ArtFrameworkTest`, `MountHostTest`, and `PresentationScheduleTest`.
- The default semantic gate is `./scripts/with-art-env.sh test`.
- Consumer compatibility is checked with `./scripts/verify-consumer-fixture.sh`.
- No device gate applies unless lifecycle or host behavior changes beyond the existing tested path.

## Completion Definition

- `ArtFramework` remains the only public facade entry point.
- Extracted classes are package-private implementation details.
- ECS, schedule, and host caches retain their existing authority boundaries.
- Focused and full semantic tests pass.
- Consumer fixture passes.
- Negative inventory finds no duplicate writer or bypass introduced by the split.

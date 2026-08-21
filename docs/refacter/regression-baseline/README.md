# Regression Baseline Refacter

## Goal

Establish a durable regression baseline for the unified ART presentation runtime after the
Traditional ECS convergence and facade-internals extraction. The baseline must protect the single
presentation authority, production schedule ordering, signal semantics, render-cache recreation,
and consumer compatibility without changing production behavior.

## Non-goals

- No new consumer-facing feature or API.
- No production ownership migration.
- No device-only visual redesign or Spine 4.2 pixel renderer.
- No broad test cleanup or unrelated documentation rewrite.

## Architecture Authority

- [`docs/design/presentation-entity-runtime.md`](../../design/presentation-entity-runtime.md)
- [`docs/design/traditional-ecs.md`](../../design/traditional-ecs.md)
- [`docs/design/node-signal-runtime.md`](../../design/node-signal-runtime.md)
- [`docs/design/backend-context.md`](../../design/backend-context.md)
- [`docs/development/api-stability.md`](../../development/api-stability.md)
- [`docs/development/consumer.md`](../../development/consumer.md)

## Current Checkpoint

- Project: `regression-baseline`
- Active slice: project closure
- State: complete
- Frozen scope: focused tests and verification records for five boundaries: presentation
  authority, schedule, signal delivery, render cache recreation, and consumer contract.
- Worktree baseline: source, test, design, release-script, and project-record changes are limited
  to the reviewed baseline slices.

## Ordered Review Tree

1. Presentation authority: one shared `PresentationWorld`; context lifecycle and reset isolation.
2. Production schedule: declared phase order and synchronous compatibility bridge.
3. Signal boundary: exact/regex ordering, replacement/stop semantics, and cleanup.
4. Render derivation: host cache clear/recreate from ECS state without authority mutation.
5. Consumer contract: public API and artifact compatibility checks.
6. Cross-boundary verification: focused JUnit, full semantic gate, consumer fixture, and diff scope.

## Verification Policy

- Focused JUnit must cover each boundary before the row can close.
- Full semantic gate: `./scripts/with-art-env.sh test`.
- Consumer gate: `./scripts/verify-consumer-fixture.sh`.
- Device verification is out of scope because this slice does not change STS hooks, drawing, or
  host lifecycle behavior.
- Record exact test classes and command outcomes in `ledger.md`.

## Completion Definition

- Each ledger row has explicit authority and reader/writer evidence.
- Focused tests cover the relevant lifecycle or compatibility boundary.
- Full semantic and consumer gates pass.
- Reviewer findings are dispositioned and the final scope is unchanged.
- The next action is either the next ledger row or an explicit project completion record.

## Next Action

The regression baseline project is complete. RB-02 schedule ownership, RB-04 render authority and
EntityPresent effect-free policy, and RB-06 verification records are closed. Future FX for
EntityPresent must use a separate ECS-owned visual/overlay entity; do not reopen slot effects without
a new bounded refacter project.

# Release Readiness Refacter

## Goal

Reconcile the repository's release-facing version, milestone, consumer, and release-gate
documentation after completion of the presentation entity and Traditional ECS milestones.
Keep the published contract honest without changing runtime behavior or public API.

## Non-goals

- No production Java behavior or API changes.
- No new presentation authority, schedule, cache, or compatibility layer.
- No new full-present surface or Spine 4.2 renderer.
- No version bump until the release gate and consumer contract are consistent.

## Architecture Authority

- [`docs/task.md`](../../task.md): completed milestone and acceptance evidence.
- [`docs/design/presentation-entity-runtime.md`](../../design/presentation-entity-runtime.md): ECS
  authority and host boundary.
- [`docs/development/api-stability.md`](../../development/api-stability.md): consumer stability.
- [`docs/development/consumer.md`](../../development/consumer.md): dependency and artifact contract.
- [`scripts/release-gate.sh`](../../../scripts/release-gate.sh): executable release checks.

## Checkpoint

- Project: `release-readiness`
- Active slice: release-facing consistency corrections
- State: complete; bounded review and release gate are closed
- Scope: README milestone claims, task milestone ordering/status, CHANGELOG and version metadata,
  consumer documentation, and release-gate coverage.
- Next action: no remaining action for this refacter. Open a new bounded project before changing
  runtime behavior, public API, or a different release boundary.

## Review Tree

1. Version source and CHANGELOG alignment.
2. README and task milestone status alignment.
3. Consumer/API stability documentation alignment.
4. Release-gate script coverage and artifact assumptions.
5. Final diff and release verification.

## Verification Policy

- Documentation-only edits require `git diff --check` and the release gate.
- Release script edits additionally require direct execution of the affected checks.
- Consumer contract edits require `./scripts/verify-consumer-fixture.sh`.
- No JUnit or device gate applies unless production source is added to scope.

## Completion Definition

- Release-facing version and milestone claims agree with source metadata and `docs/task.md`.
- Consumer/API documentation describes only the supported public contract.
- Release gate checks the intended artifact, version, tooling, and consumer fixture.
- Focused checks and the full applicable release gate pass.
- Accepted review findings are recorded and closed; no scope drift remains.

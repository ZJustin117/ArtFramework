---
name: refacter
description: Use for refacter, refactor review, incomplete refactors, architecture migrations, legacy-state removal, or continuing a persisted refactor project under docs/refacter/. Orchestrates recursive read-only review, before/after ledgers, tests, and completion gates.
---

# Refacter

Supervise a refactor as a sequence of bounded, reviewable slices. The primary agent owns the
plan, source edits, project ledger, tests, and completion decision. Read-only subagents provide
evidence and findings; they never own or recursively delegate the refactor.

## Persistence

Every supervised refactor has one directory:

```text
docs/refacter/<project-name>/
  README.md
  ledger.md
  reviews.md
```

Use the spelling `refacter` for this workflow and its persisted path. Use a lowercase,
hyphen-separated project name.

When the user supplies `@docs/refacter/<project-name>/` or any file below it, treat the directory
as the continuation token:

1. Read all Markdown files directly in that project directory before planning or editing.
2. Reconcile their recorded scope and status with the current worktree. Never assume the prior
   session completed an unrecorded action.
3. Continue the first non-complete ledger row unless the user selects another row.
4. Update the persisted files as work advances. Chat todos may mirror the work, but never replace
   the project ledger.

If no project directory exists, create it before changing production code. Do not reuse one
directory for unrelated refactors.

## Required Files

### `README.md`

Keep the durable project control state:

- goal, non-goals, architecture authority, and source design documents
- current checkpoint and active slice
- ordered review tree
- verification policy and completion definition
- next action specific enough for another session to resume

### `ledger.md`

Keep the before/after project table. Each row is one independently reviewable ownership or
behavior assertion, not a broad package cleanup.

Required columns:

```text
ID | Scope | Before | Target after | Evidence before | Evidence after | Tests | Review | Status
```

Allowed status values:

- `pending`: not yet investigated
- `reviewing`: baseline or findings are being established
- `implementing`: accepted findings are being fixed
- `verifying`: implementation exists and required checks are running
- `blocked`: a named external decision or prerequisite prevents progress
- `complete`: all completion criteria have direct evidence

Never mark a row complete because code moved or a reviewer returned `PASS`. `Evidence after`,
focused tests, applicable full gates, and review closure must all be present.

### `reviews.md`

Record review rounds and finding disposition. Use stable IDs such as `R1-01`:

```text
## Round R1 - <scope>

- Session: `ses...`
- Scope: frozen | changed
- Result: PASS | FINDINGS | BLOCKED
- Findings:
  - `R1-01` high, accepted, fixed, verified: summary and evidence
- Residual risk: ...
```

Only record a Task session ID returned by the tool. Never invent one. If the reviewed scope
changes, mark it `changed` and require a focused re-review before closing the ledger row.

## Recursive Review

The primary agent emulates recursive review with an explicit depth-first queue:

1. Start from the architecture claim being changed.
2. Split it into natural ownership boundaries: data, writer, reader, compatibility API, host
   boundary, lifecycle, and verification.
3. Delegate one narrow, read-only review scope to `art-reviewer` with the project directory,
   ledger row, exact paths or diff, governing invariants, and requested checks.
4. Validate each finding against source. Record it as accepted, rejected with reason, or blocked.
5. For accepted behavior findings, add or refine a focused failing test before implementation.
6. Make the smallest coherent edit in the primary session.
7. Resume the same reviewer with its real `ses...` ID for a focused fix review.
8. If a child boundary exposes a new independent concern, add a ledger row and review it before
   returning to the parent boundary.

Do not grant reviewer subagents `task` or `edit`. For high-risk schedule, lifecycle, concurrency,
or render changes, request an independent sibling review after the first review is resolved.

## Review Contract

Ask `art-reviewer` to return findings first and use this shape:

```text
Result: PASS | FINDINGS | BLOCKED
Scope: <reviewed paths or diff>

Findings:
- ID: R1-01
  Severity: critical | high | medium | low
  Location: <absolute path:line>
  Evidence: <observed code or behavior>
  Impact: <concrete failure mode>
  Recommendation: <bounded correction>
  Verification: <focused test or inspection>

Open questions:
- <unresolved assumption only>

Residual risk:
- <untested or out-of-scope behavior>
```

`PASS` means no actionable finding in the supplied scope. It does not mean semantic or device
verification passed.

## Supervision Checks

Apply these checks when relevant to the refactor:

- **Authority:** identify the single writer and source of truth before and after the change.
- **Negative inventory:** search for old writers, duplicate mutable stores, bypass APIs, direct
  constructors, and stale compatibility paths after migration.
- **Lifecycle:** test create, update, close, reset, recreation, and failure cleanup where applicable.
- **Read purity:** ensure queries, probes, diagnostics, and serializers do not create or synchronize
  production state unless explicitly designed as commands.
- **Dependency direction:** inventory newly introduced inward imports and host/runtime types crossing
  core boundaries.
- **Cache recreation:** clear disposable caches and prove they rebuild from retained authoritative
  state without changing that state.
- **Scope drift:** compare the current diff with the frozen review scope before every close decision.
- **Change budget:** keep one ownership assertion per ledger row and avoid unrelated cleanup.

Metrics such as file counts, lines, test counts, bypass-call counts, mutable-static counts, and
dependency edges are supporting evidence, not completion evidence. Use actual line or branch
coverage only when the repository already has reliable instrumentation or the user approves adding
it as a separate change.

## Test Gates

Tests cover the refactor; review does not replace tests.

For ArtFramework:

1. Start behavior changes with focused JUnit 4 where feasible.
2. Delegate `junit-test` after each coherent pure API, registry, runtime, ECS, or render-logic slice.
3. Delegate offline `art-verify` when its runner or fixture YAML changes.
4. For source reaching STS hooks, drawing, or host lifecycle, run JUnit first, then deploy and run
   only the applicable documented D1 verification.
5. Keep Arthas diagnostic-only; it is never a semantic completion gate.

Record exact test classes, scenarios, and outcomes in `ledger.md`; do not write only `tests pass`.

## Completion Gate

A ledger row is complete only when all apply:

- the target authority and ownership boundary are explicit
- the old authority is deleted or documented and proven to be a non-authoritative cache
- compatibility readers consume the target authority without retaining another mutable truth
- focused tests cover the migrated behavior and lifecycle boundary
- applicable full semantic, tooling, and device gates pass
- accepted findings are fixed and verified; rejected findings include evidence
- the final diff remains inside the reviewed scope
- `README.md` identifies the next row or declares the project complete

Before ending any work session, persist the current row status, review session IDs, verification
results, blockers, residual risks, and one concrete next action.

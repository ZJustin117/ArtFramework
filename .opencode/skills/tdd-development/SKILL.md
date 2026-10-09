---
name: TDD Development
description: "Execute one accepted-design behavior slice through serial Red, Green, Refactor and independent review, using explicit project-local context."
---

# Single-Slice TDD

## Ownership

- The parent owns design acceptance, slice selection, permissions, integration, independent gates,
  and final acceptance.
- `tdd-slice` is a read-only dispatcher and the only nested-delegation exception.
- Red establishes expected failure, Green implements the smallest correct behavior, Refactor may
  simplify only the current slice, and `code-reviewer` independently reviews it.

## Required input

Use `references/slice-task-template.md`. Require an explicit project context path; never guess it.
Use `references/project-context-template.md` to localize this workflow. Keep project-specific
commands and constraints in that context. Require one behavior,
accepted design authority, observable criteria, allowed/frozen paths, real commands with working
directories, prerequisites, and baseline attribution. Missing or conflicting semantics block the
slice. Draft proposals do not become contracts.

## Gates

1. Red edits only allowed tests/fixtures and must show behavioral or explicitly structural failure.
   Infrastructure failures and unrelated compilation are blockers.
2. Green freezes Red tests, edits only allowed production paths, and must pass focused verification.
3. Refactor is optional and behavior-preserving; no change is a valid result.
4. Review checks correctness, design coverage, scope, test validity, and complexity.
5. The parent separately dispatches the independent verification and external stages required by
   project context. Stage checks do not replace project-required independent gates.

Stages run serially in foreground. Allow at most two corrective cycles. Never claim an unrun check
passed. Do not commit, merge, push, reset, deploy, operate devices, or start another slice.

Before dispatch, inspect status and scoped diffs and record baseline attribution. Writers must not
overlap with other agents or user edits on the same paths; unexpected changes require stopping.
Every child receives the complete handoff; loaded context and earlier tool output are not inherited.
The dispatcher inspects evidence and scoped diffs but never edits or executes test commands.

Unexpected passing Red returns `No change required` only with coverage evidence; never silently
continue to Green. Test defects return to Red. Establish a new effective Red before continuing;
if new coverage already passes, return to the parent rather than fabricate historical failure.
Implementation defects return to Green, then Refactor, checks, and Review. New requirements,
contract conflicts, or out-of-scope failures return `Blocked`. Do not retry identical infrastructure
failures. Review blocks on concrete correctness or accepted-requirement failures; zero findings
is valid. Every added capability needs a current requirement; prefer clear existing patterns and
one authority for each rule over speculative abstractions or adjacent cleanup.

## Report

Return `Complete`, `Partial`, `Blocked`, `Failed`, or `No change required`, with the slice, paths,
criteria, Red classification/evidence, Green and Refactor commands/results, review findings, frozen
path status, and unresolved risks. Status labels never imply command evidence.
For every check include `PASS | FAIL | NOT RUN | BLOCKED`, exact command, working directory, exit
result, and evidence excerpt or artifact path. Documentation-only work and pure refactors route
outside the behavior dispatcher without manufactured Red.

# ArtFramework TDD Context

All paths are relative to the ArtFramework repository root. `AGENTS.md` remains authoritative;
this file localizes the workflow and does not grant permissions or accept draft design.

## Boundary and authority

- ART is a presentation framework. Do not depend on downstream protocol, party, combat, or other
  gameplay authority.
- Use the applicable design authority in `docs/design/`, especially `art-framework.md`,
  `godot-aligned-ui.md`, `ui-ops-probe.md`, and the relevant backend/native-render document.
- Native patches require explicit design support. Preserve the C1/C2 boundary and host SPI.
- Temporary files belong in `agent-tmp/`; debug output belongs in `debug-artifacts/`. Never read or
  commit `.env.local` secrets.

## Verification

| Area | Authority | Command / working directory |
|---|---|---|
| Pure API, registry, runtime logic | `docs/development/logic-layer-testing.md` | `./scripts/with-art-env.sh test --tests '<pattern>'` / repository root |
| Full semantic regression | `AGENTS.md` | `./scripts/with-art-env.sh test` / repository root, parent-owned `@junit-test` |
| UI fixtures and verifier | `docs/development/ui-layer-verification.md` | `cd tools/art-verify && python3 -m unittest discover -s tests -v` |

Focused verification must use the affected module's real command. Missing ART JAR environment
keys are reported by name; paths are never invented. UI device verification, jar deployment,
Connector/Harness lifecycle, and Arthas are parent-owned external stages and never part of a leaf.
After API/registry/runtime changes, the parent dispatches a separate `@junit-test` full gate.
If UI runner/YAML changes, use offline `@art-verify`; deploy/device stages follow `AGENTS.md`.

## Workflow limits

Tests/fixtures are frozen after Red. No stage commits or changes Git history. A behavior slice
without a runnable local test command is blocked before Green; do not invent test infrastructure.
Documentation-only and behavior-preserving refactor tasks may bypass manufactured Red when the
parent explicitly routes them outside this dispatcher.

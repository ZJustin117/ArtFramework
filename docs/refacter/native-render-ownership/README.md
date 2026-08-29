# Native Render Ownership Refacter

Status: historical project, complete and frozen. This directory records the policy and
reviews at the time of NRO. The current runtime policy is manifest-backed conditional
delegation: an `ART_DELEGATED` invocation may suppress native rendering only after its
FULL_READY capability gate passes, and strict runtime evidence must close the invocation.

## Goal

Make STS1 native renderers the visual authority for existing Slay the Spire UI and card pixels. ART may observe, project, route input, and draw explicit overlays, but must not hand-reimplement native card, control, room, map, event, reward, shop, treasure, intent, or skeleton pixels when an original STS renderer exists.

## Non-goals

- Do not change STS combat, room, card, relic, power, or effect authority.
- Do not introduce downstream project protocol or party/combat dependencies.
- Do not rewrite unrelated C1 scene2d windows or purely ART-owned visuals.
- Do not resolve unrelated dirty worktree changes outside the active slice.

## Architecture Authority

- `docs/design/native-render-coverage-sdd.md` is the current ownership and evidence contract.
- `docs/design/c2-full-present.md` defines the current full-present capability boundary.
- Runtime code authority is `NativeRenderBridge`, render patch adapters, `SurfaceDrawPlan`, and `Sts1SurfaceRenderer`.

## Current Checkpoint

NRO-01..NRO-06 are complete. The project is frozen; later NRCC work superseded its
blanket native-continuation policy with manifest-backed conditional delegation. The ledger
and reviews remain historical evidence and are not a current implementation backlog.

## Review Tree

1. Hand/card render ownership: `AbstractPlayer.renderHand`, `CardGroup.renderHand`, `AbstractCard.render`.
2. Combat controls, energy, and intents native continuation.
3. Map, event, select, reward, rest, shop, and treasure native continuation.
4. Skeleton and transient effect identity-scoped observation/delegation.
5. Static manifest and runtime strict evidence gates.

## Verification Policy

- Each slice starts with focused JUnit where feasible.
- After source changes, run the default gate: `./scripts/with-art-env.sh test`.
- For STS hook/render changes, deploy the jar to D1, run `scripts/art-lab combat verify-full`, and capture a D1 screenshot when the slice affects visible combat pixels.
- Record exact commands and artifacts in `ledger.md` before closing a row.

## Completion Definition

- Every STS1 render patch has explicit ownership: native passthrough, captured passthrough, native with ART overlay, or narrowly approved ART-owned pixels.
- No native-owned surface suppresses its original renderer to draw a hand-made duplicate.
- Tests and NRCC docs prevent regressions.
- JUnit and applicable D1 verification pass.

## Next Action

None. Follow current work in `docs/task.md` and use
`docs/design/native-render-coverage-sdd.md` for current policy.

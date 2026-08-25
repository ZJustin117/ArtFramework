# Native Render Ownership Refacter

## Goal

Make STS1 native renderers the visual authority for existing Slay the Spire UI and card pixels. ART may observe, project, route input, and draw explicit overlays, but must not hand-reimplement native card, control, room, map, event, reward, shop, treasure, intent, or skeleton pixels when an original STS renderer exists.

## Non-goals

- Do not change STS combat, room, card, relic, power, or effect authority.
- Do not introduce downstream project protocol or party/combat dependencies.
- Do not rewrite unrelated C1 scene2d windows or purely ART-owned visuals.
- Do not resolve unrelated dirty worktree changes outside the active slice.

## Architecture Authority

- `docs/design/native-render-coverage-sdd.md` is the primary ownership contract, but must be tightened so `DELEGATE_TO_ART` cannot mean hand-reimplementing native pixels.
- `docs/design/c2-full-present.md` currently permits full native surface delegation and must be reconciled with the new rule that ART should use native render functions where they exist.
- Runtime code authority is `NativeRenderBridge`, render patch adapters, `SurfaceDrawPlan`, and `Sts1SurfaceRenderer`.

## Current Checkpoint

Active slice: `NRO-05` static manifest and native pixel authority gate. NRO-01..NRO-04 source slices are complete; docs and regression gates now enforce the native-pixel-authority policy.

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

Implement `NRO-01`: restore hand/card to call native `AbstractCard.render` instead of `Sts1HandCardRenderer` hand-built pixels, then add tests/docs that prohibit future card-pixel rewrites.

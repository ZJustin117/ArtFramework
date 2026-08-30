# Native Render Coverage SDD

Status: fixed architecture; static manifest and runtime ledger implemented

This document defines the Native Render Coverage Contract (NRCC), its static
inventory, and its runtime evidence contract. The descriptor-aware manifest is
currently closed over all 544 scanned paths. That static closure is not runtime
acceptance: each exercised invocation must still satisfy the strict runtime ledger.

## Fixed architecture

ART does not reimplement or rewrite STS render paths. It intercepts a native
render invocation at the host boundary, records the invocation, converts the
display-relevant observation into ART presentation input, and makes an explicit
continuation decision.

```text
STS authority / native renderer
  -> native render invocation
  -> NativeRenderBridge
       -> owner resolution
       -> invocation ledger
       -> presentation input adapter
       -> RenderDisposition
            -> PASS_THROUGH
            -> CAPTURE_AND_PASS
            -> DELEGATE_TO_ART
            -> FAIL_OPEN
```

`DELEGATE_TO_ART` means that ART owns the presentation result for this specific
invocation and the native draw body is not allowed to emit a duplicate result.
It is not, by itself, a claim that ART currently reproduces every native pixel:
surfaces with minimal or incomplete supply are explicitly tracked as delegated
with exposed pixel-supply gaps until strict draw evidence closes the gap. It
does not mean that ART rewrites the STS method, executes game rules, or
maintains a second STS state model.

STS remains responsible for:

- game authority and state mutation;
- relic, Power, card, and effect creation;
- effect lifecycle and update timing;
- native render invocation timing and arguments.

ART remains responsible for:

- invocation interception and stable identity extraction;
- surface/effect ownership policy;
- projection into `PresentationWorld`;
- immutable `PresentationFrame` rendering;
- HostAssets, Lightwave diagnostics, probe, and coverage ledgers.

The bridge may fail open to native rendering. Every fail-open decision must be
recorded and must never be silently treated as delegated coverage.

## Native render delegation contract

STS renderers remain the default visual authority for any invocation that is not
delegated. Suppression is never a static property of a surface; it is decided per
invocation by the `NativeRenderBridge`, which resolves the owning surface or
effect, records the invocation, and returns one explicit `RenderDisposition`. A
patch may return `SpireReturn.Return(null)` only when the bridge returned
`DELEGATE_TO_ART` (`nativeContinuation == false`). Panic, unknown owners, and
bridge errors fail open to native rendering with a recorded reason; fail-open is
recovery behavior and is never counted as delegated coverage.

### Disposition + evidence ledger closure

A listed STS surface may be marked `ART_DELEGATED` in the coverage manifest once
its full-present capability gate exists (FULL level + mounted + scene match +
ready executor). The manifest declaration alone is not coverage: every delegated
invocation must close through the runtime evidence ledger.

1. `RenderDisposition` records mode `DELEGATE_TO_ART` with
   `nativeContinuation == false` and a presentation entity id.
2. A matching `PresentationDrawEvidence` must be recorded for the same
   invocation id in the same frame.
3. A delegated invocation without draw evidence stays visible as a strict-report
   gap (`delegatedWithoutArtEvidence`) and is never folded into coverage.

### Current pixel supply status (exposed gaps)

Suppressing a native renderer transfers display ownership to ART. The current
pixel supply differs per surface and is recorded here instead of hidden:

| Surface | Native invocation | Suppressed when FULL_READY | Current ART pixel supply |
|---|---|---|---|
| Combat hand | `AbstractPlayer.renderHand` | yes | live-card delegation: ART hard-syncs pose and calls the un-patched `AbstractCard.render`; card pixels stay native |
| Combat controls | `EndTurnButton.render` | yes | enhanced partial pixel supply: stable projected bounds, enabled/disabled button ResourceIds, hover-ready ResourceId, label, C2 item, and visible-item evidence; native hover/animation parity remains pending |
| Energy panel | `EnergyPanel.render` | yes | enhanced partial pixel supply: projected orb layer ResourceId, bounds, energy text, C2 item, and projection evidence; native orb animation/layer parity remains pending |
| Intents | `AbstractMonster.renderIntent` | yes | enhanced partial pixel supply: projected intent texture ResourceId, stable icon geometry, amount/text, C2 visuals, and projected evidence; native intent animation/parity remains pending |
| Targeting arrow | `AbstractPlayer.renderTargetingUi`, `PotionPopUp.renderTargetingUi` | no (observe-only by default; FULL self-draw optional) | OBSERVE: projection only, native arrow stays authoritative; FULL: self-drawn bezier arrow via `TargetingDrawPath`, texture fallback to plain line |
| Proceed button | `ProceedButton.render` | yes | enhanced partial pixel supply: stable C2 button items with enabled/disabled background ResourceIds, label text, bounds, and evidence count from visible projected items; native glow/hover/animation parity still pending |
| Top panel | `TopPanel.render` | yes | enhanced partial pixel supply: stable C2 HUD items for bar, HP, gold, floor, ascension, and status with explicit ResourceIds/bounds and evidence count from visible projected items; ascension/status currently use catalog fallback resources, so full native HUD parity remains pending |
| Map screen | `DungeonMapScreen.render` | yes | enhanced partial pixel supply: projected node textures, role/resourceId/symbol labels, outline and highlight/pin overlays, stable row:col identity, pan/zoom geometry, and visible node/overlay evidence; HostAssets fallback is explicit. Full map background, edge, legend, and native pan/zoom parity remain pending |
| Event dialog | `GenericEventDialog.render` | yes | enhanced partial pixel supply: panel/title/options use explicit HostAssets ResourceIds/fallbacks, projected bounds and enabled/visible state, C2 stable items, texture + label draw, and evidence count from visible panel items/options; dialog base native parity remains pending |
| Select screens | `GridCardSelectScreen.render`, `HandCardSelectScreen.render` | yes | enhanced partial pixel supply: card items, selected/card frames, and confirm row use explicit HostAssets ResourceIds/fallbacks, stable instance ids, projected bounds and selected/enabled/visible state, C2 sync, texture + label draw, and evidence count from visible projected items; dialog/card-select base native parity remains pending |
| Reward screen | `CombatRewardScreen.render` | yes | enhanced partial pixel supply: projected visible reward rows use explicit gold/card/relic/boss-relic ResourceIds or disabled/fallback resources, stable bounds, enabled/visible state, texture+label drawing, and evidence count from visible draw items; reward screen native parity remains pending |
| Rest room | `CampfireUI.render` | yes | enhanced partial pixel supply: campfire title and visible option rows (`rest`/`smith`/`dig`/`recall`/`toke`/unknown) use explicit ResourceIds/fallbacks with stable row geometry, roles, enabled/visible state, C2 sync, texture+label drawing, and evidence count from visible chrome rows; campfire base art/animation parity remains pending |
| Shop screen | `ShopScreen.render` | yes | enhanced partial pixel supply: merchant, gold, entry, purge, and sold-out/disabled rows use explicit ResourceIds/fallbacks with C2 geometry and texture+label drawing; card/relic/potion entries preserve known entry ResourceIds when cataloged; full merchant/shop native parity remains pending |
| Treasure room | `TreasureRoom.render` | yes | enhanced partial pixel supply: title, closed/open chest, and relic rows use explicit ResourceIds/fallbacks with stable row geometry, texture+label drawing, and evidence count from current chrome rows; full chest/treasure native parity remains pending |
| Cards / piles / soul | `CardGroup.render*`, `Soul.render`, `SoulGroup.render` | no | OBSERVE/OVERLAY: pile, card-group, and soul chrome is projected as dependency-neutral ids/zones/counts/bounds/resource labels and drawn as optional resource-backed overlay; live `AbstractCard.render` card pixels remain native and unpatched |

These gaps are deliberate inventory, not silent acceptance. Until a surface
reproduces its base pixels, its delegations keep surfacing as strict-report
evidence gaps so the missing supply cannot be mistaken for coverage.
The map remains `ART_DELEGATED with exposed gap` while its enhanced partial
pixel supply is expanded; this wording is intentional and keeps the ownership
ledger compatible with the incomplete-supply policy.

This table is a visual parity inventory. Its pending items describe pixels,
animation, interaction feel, or scene coverage that still need D1 inspection.
They are distinct from strict runtime evidence gaps such as an open invocation,
`delegatedWithoutEvidence`, native continuation after delegation, orphan ART
output, or an unrecorded fail-open. Recent runtime closure work drains every
pending invocation for a surface draw, cancels stale invocations on native/off
transitions, and closes recovery state; those ledger mechanics are implemented,
but the visual parity inventory for map/event/select/room still requires scene-specific D1
inspection beyond the completed runtime acceptance fixtures.

Per-instance claims stay granular:

| Surface / effect | Native invocation | Ownership justification |
|---|---|---|
| Per-instance skeleton | `SkeletonMeshRenderer.draw` | Only individual skeleton instances claimed by ART through `Sts1SkeletonBridge` are suppressed; unclaimed skeletons continue through the native renderer. |
| Transient effect observation | `AbstractGameEffect.render` (observed at the `AbstractDungeon.render` container call sites and at direct three-arg host draws) | Current implementation is capture-and-pass only: the native effect queue stays authoritative, every observed call still executes the original `render`, and no effect instance is suppressed. Per-instance effect delegation is an allowed future granularity, not current behavior. |

### AbstractCard.render boundary

`AbstractCard.render` is not patched and must not be suppressed. Card pixels —
including hand cards — are drawn by the live, un-intercepted
`AbstractCard.render` call. ART owns hand layout only: the hand draw path
hard-syncs each card pose (`current_x`, `current_y`, `angle`, `drawScale`)
before invoking the live render. The manifest records both descriptor-aware
`AbstractCard#render` paths as explicit `OUT_OF_SCOPE` entries because ART does
not patch those methods. Runtime hand delegation still calls the live card
renderer; an atlas shell does not replace card pixels.

The `cards-piles-soul` overlay slice does not change that boundary. ART may
observe `CardGroup`, draw/discard/exhaust/deck piles, and `Soul` / `SoulGroup`
movement as chrome metadata (`pile` / `card_group` / `soul`, zone, count,
bounds, label, ResourceId), but it must not claim `card.art.*` as replacement
card pixels. Unknown or card-art resources soft-fallback to family chrome ids;
native card rendering continues even when the overlay is present.

### Suppression bookkeeping

Any patch that returns `SpireReturn.Return(null)` to suppress a native render call
must be accompanied by:

1. A documented ownership justification in the source patch Javadoc.
2. A manifest entry with policy `ART_DELEGATED` and a non-empty `justification`.
3. A focused test proving the suppression gate: native continuation becomes
   `false` only when the surface is FULL_READY, while OFF, panic, unmounted, and
   executor-less states continue natively. Sole-pixel-owner proof for surfaces
   with outstanding pixel-supply gaps remains tracked by the ledger gaps above.

Patches that only observe, project, or overlay must use `SpireReturn.Continue()`
and must not suppress the native draw.

## Problem

Manual inspection of the Slay the Spire display layer is incomplete and
expensive. A Lightwave profile is useful for demonstrations, but an effect
being visible does not prove that the corresponding native draw was suppressed,
that every render owner was found, or that transient relic/power effects were
handled.

The first NRCC slice therefore produces a reproducible inventory from:

1. The local STS1 game jar.
2. ArtFramework Java sources.
3. `@SpirePatch` annotations that target native render methods.

## Scope

The scanner targets classes under `com.megacrit.cardcrawl.*` whose names suggest
rendering, effects, relics, powers, screens, rooms, creatures, cards, controls,
or map ownership. It inspects public/protected/private method declarations with
these names:

```text
render
draw
renderHand
renderRelics
renderPowers
renderTip
```

The inventory explicitly includes these classes of path:

| Kind | Examples | Why captured |
|---|---|---|
| Native surface | screens, dialogs, rooms, map | Surface invocation delegation |
| Combat draw owner | player, card, controls, energy | Combat FULL present |
| Relic/power owner | relics and powers | Trigger feedback and chrome |
| Transient effect | `AbstractGameEffect` and effect subclasses | Relic/power/buff shadows and flashes |
| Creature draw owner | creature / Spine render path | Fine-grained skeleton claim |
| Generic draw owner | `draw` declarations | Backstop for paths not named `render` |

The scanner is intentionally conservative: a candidate that cannot be mapped to
an ART invocation hook remains `unclassified`; it is never treated as delegated
coverage.

## Tool

Entry point:

```bash
scripts/scan-sts-render.sh
```

Equivalent direct invocation:

```bash
python3 tools/nrcc/scan_sts_render.py \
  --sts-jar "$ART_STS_JAR" \
  --source-root src/main/java \
  --output debug-artifacts/nrcc/sts-static-scan.json
```

The output location is gitignored evidence. The report schema is
`nrcc.static-scan.v2` and contains:

- candidate STS class count;
- discovered ART patch count;
- every discovered patch target;
- every candidate native method;
- matching ART patches;
- path kind;
- `hooked` or `unclassified` classification;
- limitations that require dynamic verification.

## Classification

Static classification has only two meanings in this first slice:

```text
hooked        matching ART render patch or bridge hook was found
unclassified  no ART render hook was found
```

`hooked` means only that a source-level invocation hook appears to exist for the
exact class/method pair. It does not prove that:

- ModTheSpire loaded the patch;
- the runtime method signature matches;
- the bridge receives the invocation;
- the disposition is correct for the active scene;
- ART has a corresponding presentation frame;
- a transient effect is observed and cleaned up.

Those claims belong to the dynamic NRCC ledger and device tests.

## Patch inventory

The scanner reads `@SpirePatch` annotations from `src/main/java` and records:

```text
source
targetClass
targetMethod
hasSpireReturn
continuationHint
```

`hasSpireReturn` records whether the source file contains a `SpireReturn.Return`
path. It is a source hint for triage, not proof that the runtime disposition is
`DELEGATE_TO_ART`. `continuationHint` preserves nearby policy identifiers such
as `shouldSuppressNativeMap`; it is evidence for review, not a public API.

## Static closure gate

The static report is ready for manifest authoring when:

1. The STS jar is readable and `javap` is available.
2. Candidate classes and native methods are listed.
3. All ART render patches are listed.
4. Every `hooked` path has an exact target class and method.
5. Every `unclassified` path is assigned to one of:
   - `ART_DELEGATED` planned invocation delegation (requires justification);
   - `OBSERVED` native pixel authority retained, ART only observes through an
     existing hook/instrument (requires a justification citing the observation
     patch file; a probe-field test reference is encouraged but optional);
   - `NATIVE_PASSTHROUGH` structural helper drawing that ART never intercepts
     (requires a justification stating the never-intercept reason);
   - `CAPTURED_PASSTHROUGH` observe-only bridge behavior;
   - `NATIVE_WITH_ART_OVERLAY` native pixels continue, ART may add non-authoritative overlays;
   - `OUT_OF_SCOPE` documented boundary;
   - `UNKNOWN` requiring implementation.

### Manifest policy vocabulary and validation

The manifest checker (`tools/nrcc/coverage_manifest.py`) enforces per-policy
field contracts on top of the list above:

| Policy | Native authority | Required fields |
|---|---|---|
| `ART_DELEGATED` | suppressed when FULL_READY | `justification` + `test` (enforced for suppressing patches by the ownership check) |
| `OBSERVED` | retained; ART observes via an existing hook/instrument | `justification` citing the observation entry (patch file); `test` optional, probe-field references encouraged |
| `NATIVE_PASSTHROUGH` | retained; structural helpers never intercepted | `justification` stating the never-intercept reason |
| `CAPTURED_PASSTHROUGH` / `NATIVE_WITH_ART_OVERLAY` | retained | none beyond the base schema |
| `OUT_OF_SCOPE` | retained; documented boundary | none beyond the base schema |
| `UNKNOWN` | undecided | must be eliminated before strict acceptance |

An entry satisfies the justification contract with its own `justification`
field or, when it inherits a family default policy, with the family's default
rationale (`FAMILY_DEFAULT_JUSTIFICATION` in `tools/nrcc/families.py`). Family
defaults give every scanned path a determinate effective policy
(`explicit > family default > UNKNOWN`); entries resolved from a family default
omit the `policy` field so re-triage stays a one-table change. The strict gate
(`--check-manifest --strict-manifest`) rejects only explicit `policy: UNKNOWN`
annotations — inherited defaults are decisions, not gaps — plus the usual
closure errors (missing/stale paths, duplicates, invalid policies, unknown
families) and patch-ownership errors.

The static tool must not fail merely because the current project is incomplete;
it must expose incomplete paths. The `--check-manifest` mode fails CI when an
unclassified path remains or when a patch that suppresses the native draw
(`SpireReturn.Return(null)`) lacks an `ART_DELEGATED` entry with a
justification.

## Fixed implementation layers

The implementation is layered and each layer has one responsibility:

```text
L0 Static inventory
  STS jar + ART source -> candidate native owners and hook targets

L1 Coverage manifest
  owner/surface/effect family -> explicit scope and disposition policy

L2 NativeRenderBridge
  native invocation -> stable host-boundary invocation record

L3 Ownership / disposition
  PASS_THROUGH | CAPTURE_AND_PASS | DELEGATE_TO_ART | FAIL_OPEN

L4 Presentation input adapter
  invocation + backend frame -> ECS surface or transient visual entity

L5 PresentationWorld / PresentationFrame
  sole mutable presentation state -> immutable ordered draw input

L6 Host render adapter
  PresentationFrame -> SpriteBatch / host drawing and Lightwave diagnostics

L7 Runtime ledgers
  invocation, disposition, delegation, native continuation, and cleanup evidence

L8 Scenario gate
  OBSERVE / FULL / FAIL_OPEN / recovery -> strict NRCC report
```

No layer may move STS authority into ART. In particular:

- `NativeRenderBridge` may observe or stop a display invocation, but never
  executes relic, Power, card, combat, or room rules;
- `PresentationWorld` stores host-neutral values and entity identity, never STS
  objects, batches, textures, or effect queues;
- the host renderer consumes `PresentationFrame` and does not become a second
  presentation-state authority;
- Lightwave is an optional attachment/diagnostic marker and never a coverage
  decision.

## Ownership granularity

There are two allowed interception granularities. Surface delegation is
implemented. Per-instance skeleton delegation is implemented; per-instance
transient-effect delegation is a future option, while current transient effects
remain capture-and-pass.

### Surface owner

Use whole-invocation delegation only when the surface capability gate, input
ownership, scene readiness, recovery behavior, manifest justification, and
focused suppression test are ready. If the current ART draw path is minimal or
incomplete, the surface remains `ART_DELEGATED` but must be described as
delegated with exposed pixel-supply gaps, not as complete native-pixel coverage.
Examples include hand, map, event, select, and room screens.

```text
native surface render invocation
  -> surface owner resolution
  -> CAPTURE_AND_PASS or DELEGATE_TO_ART
```

### Transient effect instance

If transient-effect delegation is introduced, use per-instance delegation for
relic, Power, buff, card, and system effects. The effect queue must remain native
and authoritative, and an uncovered instance in the same queue must continue
through STS:

```text
effect A -> DELEGATE_TO_ART
effect B -> PASS_THROUGH
effect C -> CAPTURE_AND_PASS
```

ART must never skip an entire effect queue because one instance is delegated.
The transient entity identity must retain the native effect instance identity
for the duration of the delegation. Today all observed transient effects take
the `CAPTURE_AND_PASS` branch; none take `DELEGATE_TO_ART`.

## Disposition semantics

| Disposition | Native invocation | ART projection | Coverage meaning |
|---|---|---|---|
| `PASS_THROUGH` | continues | none required | Explicit native ownership |
| `CAPTURE_AND_PASS` | continues | optional observe/overlay | Captured, not delegated |
| `NATIVE_WITH_ART_OVERLAY` | continues | non-authoritative overlay | Native pixels remain authoritative; ART may add chrome, anchors, or diagnostics |
| `DELEGATE_TO_ART` | draw body is stopped | required | ART owns this invocation's display |
| `FAIL_OPEN` | continues | optional diagnostic | ART failed; never a pass |

`NATIVE_WITH_ART_OVERLAY` is a specialization of `CAPTURE_AND_PASS`. It
emphasizes that the native renderer still owns every pixel and ART may only add
non-authoritative overlays on top. It maps to `CAPTURE_AND_PASS` at runtime.

`FAIL_OPEN` is a recovery behavior, not a coverage class. It must carry an
error reason and be visible in the report. A delegated invocation without a
matching ART entity/frame is a hard failure.

## Runtime evidence contract

The dynamic implementation must emit three correlated records:

```text
NativeRenderInvocation
  invocationId, frameId, scene, ownerId, native class/method,
  source/effect identity, surface/effect family, bounds hint

RenderDisposition
  invocationId, mode, reason, nativeContinuation,
  presentationEntityId when delegated

PresentationDrawEvidence
  invocationId, entityId, frameId, draw count, cleanup state
```

The correlation rules are strict:

1. Each observed invocation resolves to exactly one owner.
2. Each invocation receives exactly one disposition.
3. `DELEGATE_TO_ART` has exactly one corresponding ART presentation entity or
   frame draw evidence.
4. Native continuation matches the disposition.
5. Transient delegated entities are removed when the native effect completes,
   is cancelled, the scene changes, the host is recreated, or panic is entered.

The report must distinguish `UNKNOWN`, `UNDECIDED`, `MISMATCH`,
`ORPHAN_ART_OUTPUT`, and `LEAKED_TRANSIENT_ENTITY`; none may be silently folded
into a percentage denominator.

## Runtime modes

### `OBSERVE`

Native rendering continues. The bridge captures owner/effect identity, lifecycle,
and optional ART overlay input. Unknown invocation and unknown effect paths are
reported immediately.

### `FULL`

Manifest entries marked `ART_DELEGATED` may stop their native invocation only
when the surface capability gate is ready and the entry includes an ownership
justification and a focused test proving the suppression gate. Every delegated
invocation requires ART draw evidence; missing evidence is a strict-report gap,
and surfaces whose base pixels are not yet reproduced stay exposed there (see
Current pixel supply status). Unclassified native calls fail the strict report.

### `RECOVERY`

`OFF`, panic, scene transition, and host recreation must restore native
continuation, clear delegated transient entities, rebuild host caches from ECS,
and prove that no invocation is drawn twice after re-entry into `FULL`.

## Strict acceptance

NRCC passes only when all of the following are zero:

```text
static manifest UNKNOWN
runtime UNKNOWN
runtime UNDECIDED
delegated without ART evidence
delegated native continuation
disposition mismatch
orphan ART output
leaked transient entity
unrecorded FAIL_OPEN
```

The count of visually visible Lightwave effects and screenshot similarity are
supporting diagnostics only. They cannot make NRCC pass.

## Known limitations

Static declaration scanning cannot find every runtime draw path. The following
must be covered by later instrumentation:

- reflective or generated render calls;
- methods whose names do not contain the candidate tokens;
- `SpriteBatch` calls reached through helper methods;
- effect instances created dynamically by relics, powers, cards, or mods;
- actual ModTheSpire patch loading and bridge execution;
- draw calls from third-party mods;
- native Spine mesh calls and GL-level output.

In particular, static discovery of `AbstractGameEffect` is not proof that relic
or buff trigger ghosts are delegated. The dynamic slice must record effect
create, update, render, completion, and cleanup events by instance identity.

## Current closure and follow-up

The checked manifest, runtime owner instrumentation, invocation/disposition
ledger, and strict report are implemented:

```text
static candidates
  -> coverage manifest
  -> runtime owner instrumentation
  -> native invocation ledger
  -> disposition/delegation ledger
  -> UNKNOWN / UNDECIDED / MISMATCH gate
```

Lightwave remains a visual diagnostic and does not participate in the strict
coverage decision.

The remaining visual work is evidence collection: exercise relevant scenes,
confirm the runtime strict report stays closed, and separately assess the visual
parity inventory. D1 fixtures and lab navigation now provide executable map,
event, grid/hand select, rest, shop, and treasure paths. Their runtime acceptance
is recorded in `docs/task.md`; visual parity remains a separate, explicitly
incomplete assessment.

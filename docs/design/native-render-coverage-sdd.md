# Native Render Coverage SDD

Status: fixed architecture, static implementation in progress

This document defines the Native Render Coverage Contract (NRCC) and its static
inventory. It does not claim runtime coverage. Its purpose is to enumerate STS1
native rendering candidates and define how ArtFramework intercepts their render
invocations without rewriting STS rendering implementations or game authority.

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
It does not mean that ART rewrites the STS method, executes game rules, or
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
| Combat controls | `EndTurnButton.render` | yes | text chrome only; button art not fully reproduced |
| Energy panel | `EnergyPanel.render` | yes | text chrome only |
| Intents | `AbstractMonster.renderIntent` | yes | projection chrome only |
| Proceed button | `ProceedButton.render` | yes | text chrome only |
| Top panel | `TopPanel.render` | yes | text chrome only |
| Map screen | `DungeonMapScreen.render` | yes | HostAssets node draw path; full native parity pending |
| Event dialog | `GenericEventDialog.render` | yes | none yet: base pixels not reproduced |
| Select screens | `GridCardSelectScreen.render`, `HandCardSelectScreen.render` | yes | none yet: base pixels not reproduced |
| Reward screen | `CombatRewardScreen.render` | yes | incomplete: reward item sync missing |
| Rest room | `CampfireUI.render` | yes | none yet: campfire options not synced |
| Shop screen | `ShopScreen.render` | yes | incomplete: item sync missing |
| Treasure room | `TreasureRoom.render` | yes | none yet: chest/item sync absent |

These gaps are deliberate inventory, not silent acceptance. Until a surface
reproduces its base pixels, its delegations keep surfacing as strict-report
evidence gaps so the missing supply cannot be mistaken for coverage.

Per-instance claims stay granular:

| Surface / effect | Native invocation | Ownership justification |
|---|---|---|
| Per-instance skeleton | `SkeletonMeshRenderer.draw` | Only individual skeleton instances claimed by ART through `Sts1SkeletonBridge` are suppressed; unclaimed skeletons continue through the native renderer. |
| Per-instance transient effect | `AbstractGameEffect.render` | Only individual effect instances claimed by ART through `NativeRenderBridge.beginEffectRender` are suppressed; the native effect queue remains authoritative. |

### AbstractCard.render boundary

`AbstractCard.render` is not patched and must not be suppressed. Card pixels —
including hand cards — are drawn by the live, un-intercepted
`AbstractCard.render` call. ART owns hand layout only: the hand draw path
hard-syncs each card pose (`current_x`, `current_y`, `angle`, `drawScale`)
before invoking the live render. The manifest records `AbstractCard#render` as
`OUT_OF_SCOPE` because no ART hook intercepts this invocation, not because an
atlas shell replaced it.

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
   - `NATIVE_PASSTHROUGH` explicit native ownership;
   - `CAPTURED_PASSTHROUGH` observe-only bridge behavior;
   - `NATIVE_WITH_ART_OVERLAY` native pixels continue, ART may add non-authoritative overlays;
   - `OUT_OF_SCOPE` documented boundary;
   - `UNKNOWN` requiring implementation.

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

There are two fixed interception granularities:

### Surface owner

Use whole-invocation delegation only for a complete native surface whose ART
draw path, input ownership, scene readiness, and recovery behavior are ready.
Examples include hand, map, event, select, and room screens.

```text
native surface render invocation
  -> surface owner resolution
  -> CAPTURE_AND_PASS or DELEGATE_TO_ART
```

### Transient effect instance

Use per-instance delegation for relic, Power, buff, card, and system effects.
The effect queue remains native and authoritative. An uncovered instance in the
same queue must continue through STS:

```text
effect A -> DELEGATE_TO_ART
effect B -> PASS_THROUGH
effect C -> CAPTURE_AND_PASS
```

ART must never skip an entire effect queue because one instance is delegated.
The transient entity identity must retain the native effect instance identity
for the duration of the delegation.

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

## Planned follow-up

The next NRCC slice consumes this inventory to define a checked manifest and a
runtime ledger:

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

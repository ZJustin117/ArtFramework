# Native Render Family and System Integration Contract

Status: fixed classification contract; integration recipes A/B are design-final, implementation pending

This document is the sister of [`native-render-coverage-sdd.md`](./native-render-coverage-sdd.md).
The SDD owns the **disposition** contract (how an intercepted invocation is continued,
captured, delegated, or failed open). This document owns the **classification → integration**
contract: which semantic family a native render path belongs to, and how a family plugs into
the three-layer ART runtime (collection → projection → ECS system). It does not change any
disposition rule defined there.

## 1. Purpose and scope

The classification object is the set of **488 `(class, method)` native render paths** produced
by [`tools/nrcc/scan_sts_render.py`](../../tools/nrcc/scan_sts_render.py) from the STS1 jar
(report schema `nrcc.static-scan.v3`). Every scanned path row carries a `family` field.

Family assignment is decided exclusively by the ordered rule table in
[`tools/nrcc/families.py`](../../tools/nrcc/families.py):

- 25 rule groups, evaluated **first-match-wins in declaration order**; the same module exports
  the deduplicated `FAMILY_IDS` sequence used by the manifest writer and validator.
- Rules constrain the native method name (`method` regex), exact fully-qualified class names
  (`exact`), or class-name prefixes (`prefix`); every constraint present must hold.
- An unmatched path raises `ValueError` instead of silently landing in a group. New scan output
  therefore forces an explicit family decision; the helpers exact-set is a closed enumeration.

The `family` field flows into both evidence artifacts:

1. the static scan JSON (`paths[].family` plus a `summary.families` count map), and
2. the coverage manifest ([`sts1-native-coverage.yaml`](../../tools/nrcc/manifests/sts1-native-coverage.yaml)),
   where each entry records its family and inherits its default policy from
   `FAMILY_DEFAULT_POLICY`.

Default policy strategy: every family carries a determinate default in
`FAMILY_DEFAULT_POLICY` (`tools/nrcc/families.py`), so no scanned path is left
without a policy decision:

- `OBSERVED` for the six vfx families and as the semantic note on
  `skeleton-runtime`: native pixel authority is retained and ART only observes
  through the shared container/base-class observation entries;
- `NATIVE_PASSTHROUGH` for structural helpers that ART never intercepts
  (`draw-primitives-tips`, `word-tip-ui`, `core-game-root`);
- `NATIVE_WITH_ART_OVERLAY` for the roadmap groups (monsters/bosses, player,
  orbs, relics/blights/potions, stances, map graph, cards/piles/soul, room
  shells, event dialogs, shop/rewards/chests, HUD top panel, buttons, in-run
  fullscreens): future delegation candidates whose native authority stays in
  place until individually triaged;
- `OUT_OF_SCOPE` for `meta-outofrun-screens`;
- `UNKNOWN` only for the zero-path `overlay-targeting` placeholder so Slice D
  paths fail the strict gate loudly instead of silently counting as covered.

Explicit per-entry policies still override the family default; entries resolved
from a family default omit the `policy` field so re-triage stays a one-table
change in `families.py`. Member-level exceptions live as explicit annotations:
the 16 `ART_DELEGATED` paths, `AbstractCard#render` `OUT_OF_SCOPE`,
`AbstractDungeon#render` `OBSERVED` (container instrument), and `TestGame#render`
`OUT_OF_SCOPE`.

Current distribution baseline (static scan, 490 paths total, 18 `hooked` / 472 `unclassified`):

```text
buttons-controls 17        cards-piles-soul 6        core-game-root 6
draw-primitives-tips 6     event-dialogs 10          hud-top-panel 11
inrun-fullscreens 18       map-graph 5               meta-outofrun-screens 61
monsters-bosses 8          orbs 6                    player-character 5
relics-blights-potions 5   room-shells 10            shop-rewards-chests 8
skeleton-runtime 1         stances-state 2           vfx-campfire-rest 10
vfx-card-manipulation 10   vfx-combat 145            vfx-misc-root 97
vfx-scene-world 29         vfx-stance-aura 8         word-tip-ui 4
overlay-targeting 2 (recipe B pilot, see recipe B)
```

## 2. Family inventory

Directions are semantic, not coverage claims. `hooked n/m` means n of the family's m paths
currently have a matching ART patch. Counts are the scan baseline above; regenerate with
`scripts/scan-sts-render.sh` before quoting new numbers.

### Delegation-governed (the groups containing the 16 hooked paths)

| Family | Count | Representative classes | Direction |
|---|---|---|---|
| `inrun-fullscreens` | 18 | `DungeonMapScreen`, `GridCardSelectScreen`, `HandCardSelectScreen`, `CombatRewardScreen`, `DeathScreen` | Delegation-governed (hooked 4/18: map, grid/hand select, combat reward); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate; native authority until triaged) |
| `buttons-controls` | 17 | `EndTurnButton`, `ProceedButton`, `ConfirmButton`, `CancelButton` | Delegation-governed (hooked 2/17: end turn, proceed); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `hud-top-panel` | 11 | `TopPanel`, `EnergyPanel`, `DrawPilePanel`, `DiscardPilePanel` | Delegation-governed (hooked 2/11: top panel, energy); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `room-shells` | 10 | `AbstractRoom`, `CampfireUI`, `TreasureRoom`, `NeowRoom` | Delegation-governed (hooked 2/10: rest, treasure); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `event-dialogs` | 10 | `GenericEventDialog`, `RoomEventDialog`, `AbstractEvent` | Delegation-governed (hooked 1/10: generic event dialog); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `shop-rewards-chests` | 8 | `ShopScreen`, `RewardItem`, `Merchant`, `AbstractChest` | Delegation-governed (hooked 1/8: shop screen); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `monsters-bosses` | 8 | `AbstractMonster`, `MonsterGroup` | Delegation-governed (hooked 1/8: `renderIntent`); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `player-character` | 5 | `AbstractPlayer`, `AnimatedNpc` | Delegation-governed (hooked 1/5: `renderHand`); remainder inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |
| `skeleton-runtime` | 1 | `SkeletonMeshRenderer` | Delegation-governed; per-instance claims only (`ART_DELEGATED` member), unclaimed skeletons pass through; family default is the `OBSERVED` semantic note |
| `vfx-misc-root` | 97 | `AbstractGameEffect`, `SpeechBubble`, `RelicAboveCreatureEffect` | `OBSERVED` by family default: observed through the `AbstractDungeon` container instrument plus the base-class direct-draw hook (see observe-only below); hooked 1/97 (`ART_DELEGATED` base-class member) |

### Observe-only (container call sites + direct-draw hook)

Transient effects are observed at two entry points that share one identity-keyed
pipeline. The container entry is a `@SpireInstrumentPatch` on
`AbstractDungeon#render(SpriteBatch)` whose `ExprEditor` replaces the three
`AbstractGameEffect.render:(SpriteBatch)V` call sites (`effectList` behind/regular,
`topLevelEffects`) with an observe-then-render helper — the single-arg method is
abstract, so ModTheSpire cannot attach a Prefix to it. The retained
`AbstractGameEffect#render(SpriteBatch, float, float)` Prefix covers the ~20
host-drawn particle groups that never enter the containers. Both entries keep
`CAPTURE_AND_PASS`, the native effect queue remains authoritative, and no
per-subclass hook is planned (refacter ledger `NRO-04`). Subclass families are
covered by virtual dispatch at the container sites, not by their own patches.

| Family | Count | Representative classes | Direction |
|---|---|---|---|
| `vfx-combat` | 145 | `StrikeEffect`, `DamageNumberEffect`, `FlashAtkImgEffect` | `OBSERVED` (family default): observe-only via the `AbstractDungeon` container call sites |
| `vfx-scene-world` | 29 | `TorchParticleLEffect`, `DustEffect`, `BonfireParticleEffect` | `OBSERVED` (family default): observe-only via the container call sites |
| `vfx-campfire-rest` | 10 | `CampfireSmithEffect`, `CampfireSleepEffect` | `OBSERVED` (family default): observe-only via the container call sites |
| `vfx-card-manipulation` | 10 | `ShowCardAndAddToHandEffect`, `ExhaustCardEffect` | `OBSERVED` (family default): observe-only via the container call sites |
| `vfx-stance-aura` | 8 | `DivinityParticleEffect`, `WrathParticleEffect`, `StanceAuraEffect` | `OBSERVED` (family default): observe-only via the container call sites |

### Future delegation candidate / reserved

| Family | Count | Representative classes | Direction |
|---|---|---|---|
| `overlay-targeting` | 2 | `AbstractPlayer#renderTargetingUi`, `PotionPopUp#renderTargetingUi` | `OBSERVED` by family default: capture-and-pass observation through `CombatTargetingRenderPatches`; native targeting pixels stay authoritative; self-draw is optional at `sts1.combat.targeting` FULL |

### Passthrough helpers and structural roots

| Family | Count | Representative classes | Direction |
|---|---|---|---|
| `draw-primitives-tips` | 6 | `TipHelper`, `Hitbox`, `Label`, `DrawMaster`, `Sprite`, `AbstractDrawable` | `NATIVE_PASSTHROUGH` (family default): drawing primitives shared by every surface; closed exact-set, never intercepted |
| `core-game-root` | 6 | `CardCrawlGame`, `OverlayMenu`, `AbstractCreature`, `GameCursor` | `NATIVE_PASSTHROUGH` (family default): structural root / overlay plumbing, never intercepted per pixel; explicit member exceptions — `AbstractDungeon#render` is `OBSERVED` (hosts the observe-only effect-container instrument, recipe C) and `TestGame#render` is `OUT_OF_SCOPE` |

### Out of scope and untriaged

| Family | Count | Representative classes | Direction |
|---|---|---|---|
| `meta-outofrun-screens` | 61 | `MainMenuScreen`, `CreditsScreen`, `Cutscene`, char-select/options/stats screens | `OUT_OF_SCOPE` by family default policy; upgrade path is recipe A and stays reversible |
| `cards-piles-soul` | 6 | `AbstractCard`, `CardGroup`, `Soul`, `SoulGroup` | Mixed: `AbstractCard#render` is `OUT_OF_SCOPE` by SDD boundary (ART never suppresses card pixels); rest inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate; native authority until triaged) |
| `orbs` | 6 | `AbstractOrb`, `Frost`, `Dark`, `EmptyOrbSlot` | Inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate); combat-chrome promotion only through the §5 checklist |
| `relics-blights-potions` | 5 | `AbstractRelic`, `AbstractPotion`, `AbstractBlight` | Inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate; adjacent to top-panel chrome governance) |
| `map-graph` | 5 | `DungeonMap`, `MapEdge`, `Legend`, `LegendItem` | Inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate); screen-level map delegation is owned by the `inrun-fullscreens` member `DungeonMapScreen` |
| `word-tip-ui` | 4 | `DialogWord`, `FtueTip`, `MultiPageFtue`, `SpeechWord` | `NATIVE_PASSTHROUGH` (family default): text layout helpers never intercepted; Ftue members are an out-of-run meta-interface property, recorded as a justification note |
| `stances-state` | 2 | `AbstractStance`, `NeutralStance` | Inherits `NATIVE_WITH_ART_OVERLAY` (future delegation candidate) |

## 3. Group → three-layer mapping framework

Every family integrates through the same three layers. The layers mirror the traditional-ECS
rules in [`traditional-ecs.md`](./traditional-ecs.md): components hold data, systems are
stateless, and host caches stay explicitly non-authoritative.

```text
[Collection layer]   host side, deliberately NOT ECS
  @SpirePatch (thin) -> NativeRenderBridge typed entry -> <Family>Ledger

[Projection layer]   host-neutral values into the shared world
  <Family>Registry -> Sts1NativePresentationAdapter.present()
                   -> scope entity (context "nrcc-native", key "sts1.native/<ownerId>")

[System layer]       stateless ECS iteration
  artframework.ecs.EcsSystem instances scheduled at fixed phases
```

### Collection layer

- Patches stay thin: resolve identity, call one typed bridge entry, honor the returned
  `RenderDisposition` (`SpireReturn.Return(null)` only on `DELEGATE_TO_ART`; everything else
  continues natively — see the SDD suppression bookkeeping rules).
- Typed bridge entries today: `beginSurface(ownerId, nativeClass, nativeMethod, sourceIdentity)`
  for whole-surface invocations, `beginSkeletonRender(Skeleton)` for per-instance skeleton
  claims, and `beginEffectRender(AbstractGameEffect, method)` for transient effects. A new
  family adds a typed entry only when its identity shape differs; otherwise reuse
  `beginSurface`.
- `<Family>Ledger` is the **host-side evidence cache**: invocation/disposition/evidence records
  keyed by stable identity (see `TransientEffectLedger`: `CREATED/UPDATED/RENDERED/COMPLETED/
  DISPOSED` states plus unknown-lifecycle and leaked counters). Ledgers intentionally remain
  outside ECS — they are recovery/evidence caches in the sense of the traditional-ECS state
  classification, never a second presentation authority.

### Projection layer

- `<Family>Registry` maps stable instance identity → presentation entity. For transient
  effects it produces pending projection events; the schedule-owned
  `TransientEffectProjectionSystem` (fixed `HOST_PRESENTATION` instance) is the only caller of
  `Sts1NativePresentationAdapter.present(invocation)` in that chain, creating/reusing an entity
  in the `nrcc-native` context under the `sts1.native/<ownerId>` key pair and writing data-only
  components (`BoundsComponent`, `VisibilityComponent`, `DrawComponent`,
  `HostBindingComponent`), then requesting coalesced render projection via
  `RenderProjectionQueue`. Synchronous host render hooks enqueue and immediately drain the same
  schedule-owned system so entities stay same-frame visible.
- Owner ids are namespaced per family (`effect:<instanceId>`, `surface:<surfaceId>`,
  `skeleton:<entityKey>`) so cleanup and probe slices never collide.
- Removal is explicit: complete/cancel/scene-change/host-recreation paths destroy through the
  context so world and ownership index clean together.

### System layer

Family behavior runs as `artframework.ecs.EcsSystem` implementations (`run(world, tick)`,
stateless, no cross-tick mutable fields). Three registration routes exist:

| Route | Mechanism | Choose when |
|---|---|---|
| Fixed schedule instance | `PresentationSchedule` owns the system and calls it at one declared `Phase` | The behavior is core production behavior every frame (like normalization or animation playback); requires a reviewed core change |
| Pluggable phase system | `PackSystems.enable(phase, id, system)` / `disable(...)`; duplicate ids throw | Default for family systems: reversible, owner-aware, no core edits; available at `WORLD_NORMALIZATION`, `ANIMATION`, `EFFECTS`, `HOST_PRESENTATION`, `RENDER_PROJECTION` |
| Host presentation slot | `PresentationSchedule.setHostPresentationSystem(...)` — a single `HostPresentationSystem.tick(delta)` at `HOST_PRESENTATION` | Host-side advancement that cannot be expressed as pure ECS iteration (bridging host caches); at most one slot |

Selection rules:

1. Prefer **PackSystems** for a new family system; `PresentationSchedule` remains the sole
   scheduler either way.
2. Promote to a **fixed schedule instance** only when skipping the system in some frame would
   be a defect, and record the promotion in the design docs.
3. Use the **HostPresentationSystem slot** only for host tick bridging; never park family
   presentation logic there to bypass the stateless-system rule.

### Family template convention

A family that tracks per-instance native objects materializes the same four-piece template as
the transient-effect example:

| Piece | Role (TransientEffect reference) |
|---|---|
| `<Family>Identity` | Immutable host-neutral value: stable `instanceId`, native class, identity hash, generation (`TransientEffectIdentity`) |
| `<Family>Ledger` | Lifecycle evidence states + unknown/leaked counters + `probeSlice()` for diagnostics (`TransientEffectLedger`) |
| `<Family>Registry` | instanceId → presentation entity index plus the pending-projection queue; `TransientEffectProjectionSystem` presents through `Sts1NativePresentationAdapter` and cleans up on completion (`TransientEffectRegistry`) |
| `<Family>LifecycleAdapter` | Coordinates ledger transitions with registry present/cleanup; exposes `cleanupAll()` for recovery (`TransientEffectLifecycleAdapter`) |

Recovery closure: `PresentSafety` panic, scene epoch changes, and host recreation must drain
the registry (`cleanupAll()`), count undisposed ledger records as leaked, and never reuse a
stale entity id. Whole-surface families skip Identity/Ledger (the surface owner id is already
stable) and keep only Registry + adapter behavior.

## 4. Integration recipes

Three worked examples fix how the mapping framework applies. Facts below were verified against
the STS1 jar with `javap` (branch structure, method visibility, descriptors) and against the
static scan.

### Recipe A — meta out-of-run fullscreens

**Native shape.** STS1 has no screen stack. `CardCrawlGame.render()` switches on a single
`CurScreen` enum value and its main-menu branch invokes
`mainMenuScreen.render(SpriteBatch)`; inside `MainMenuScreen`, nested `CurScreen` values form
a one-value-at-a-time state machine (sub-screens replace each other, nothing composes).

**Choke point.** The `mainMenuScreen.render(sb)` call site inside `CardCrawlGame.render()` is
the single invocation edge for the whole meta menu tree — one Postfix observer sees every
screen transition without touching dozens of screen classes.

**Integration.**

- Observe: a Postfix patch reads the active `CurScreen` and writes it into a metadata
  component on the projection metadata entity, modeled after
  `ProjectionInteractionComponent` (data-only, immutable replacement on write).
- Present: menu content is **not** reproduced natively. It is carried by the existing C1
  synthetic window system — LML/JSON declarations, ECS-componentized windows, and the modal
  input capture loop already close input back through interceptors.
- Policy: `meta-outofrun-screens` stays `OUT_OF_SCOPE` by family default. Because the
  observation is additive (`SpireReturn.Continue()`), upgrading a specific screen later means
  adding a manifest entry + C1 window, not reworking the hook — the path is low-cost and
  reversible.

### Recipe B — card targeting arrow (pilot landed, Slice D)

**Native shape.** Targeting is **not a standalone class**. `AbstractPlayer` declares
`private void renderTargetingUi(SpriteBatch)`, called conditionally near the end of its
`render` body; `ui.panels.PotionPopUp` carries a same-named private variant for potion
targeting. Both methods are now on the scanner whitelist and belong to the
`overlay-targeting` family.

**Choke points / patches.** Two Prefix patches (one per class) attach to the private
`renderTargetingUi(SpriteBatch)` descriptor. They call `NativeRenderBridge.beginSurface`
with surface id `sts1.combat.targeting` and always return `SpireReturn.Continue()` — zero
suppression. OBSERVE level yields `CAPTURE_AND_PASS`; OFF yields `PASS_THROUGH`; panic,
unknown owner, and bridge errors fail open. Because the patch never calls
`SpireReturn.Return(null)`, the ownership static gate is not triggered and the family stays
under `OBSERVED` policy.

**Surface.** A new mini-surface `sts1.combat.targeting` follows the intents observe-first
template: `SurfaceIds` constant, `FullPresentMode` field defaulting to `OFF`,
`SurfaceDrawPlan` entry gated by `sceneOk=combat`, `PresentSafety` unmount list inclusion,
console `art present targeting on|off|observe|status`, and a probe slice field.

**ECS projection.** The c2-projection metadata entity carries a data-only
`TargetingSessionComponent(active, cardInstanceId, targetKey, phase)` written by
`Sts1PresentationBackend` while a drag is armed. The component mirrors the existing
`ProjectionInteractionComponent` / `dragInstanceId` pattern; targeting adds target identity
and phase (`ARMED/VALID/COMMITTED`) without changing existing readers. Epoch switches reuse
the existing `clearDrag` cleanup path.

**Self-draw.** `TargetingDrawPath` is a pure function that computes bezier control points and
endpoints from `TargetingSessionComponent`, the source card's projected pose, and the
target geometry. Drawing runs at the tail of `Sts1SurfaceRenderer.render` only when the
targeting surface level is `FULL` (default `OFF` draws nothing).

> **Deviation from earlier recipe drafts.** `RenderTargetKind.OVERLAY` and `kindsOverUi()`
> are currently a dead mechanism scheduled for cleanup in Slice E. This pilot therefore
> paints the arrow through the renderer's existing tail slot, not through an overlay kind.
> The deviation is recorded here and in `task.md` 47.15.

**Texture.** The arrow texture resolves through `HostAssets` at
`sts1:images/ui/combat/reticleArrow.png` (vanilla path `images/ui/combat/reticleArrow.png`).
When the texture is unavailable, the path falls back to a plain colored line drawn with
`FontHelper` / `ImageMaster` white-pixel so geometry and fail-open behavior remain verifiable
without the asset.

**Policy.** Observe-first pilot for the `overlay-targeting` family: capture and project, keep
native pixels authoritative, and only consider delegation after the §5 checklist passes for
both private methods (each requires its own descriptor audit — see the overload caveat below).

### Recipe C — transient VFX containers

**Native shape.** The true choke point is not any of the ~300 concrete effect classes but the
three container traversal sites in `AbstractDungeon.render(SpriteBatch)`:

1. `effectList` iteration with the `renderBehind` flag set (bottom layer),
2. `effectList` iteration with `renderBehind` clear (regular layer),
3. `topLevelEffects` iteration (top layer).

Every concrete effect reaches the display through virtual dispatch at these three sites;
parent-session analysis puts the concrete-effect population covered by this dispatch at
~264 classes. Roughly 20 particle groups are drawn directly by their host surfaces
(three-arg particles constructed inside e.g. title/campfire hosts) and never enter the
containers — they belong to their host-surface families, not to the vfx families.

**Mapping.**

- The `renderBehind` flag maps onto `PresentLayer` z-ordering: behind-effects project below
  room/world content, regular effects above, top-level effects into the overlay band — the
  same ordering the C2 full-present draw path already uses.
- Governance stays on the observe chain: the `AbstractDungeon#render` container instrument plus
  the retained three-arg hook feed `beginEffectRender` +
  `TransientEffectIdentity/Ledger/Registry/LifecycleAdapter` with `CAPTURE_AND_PASS`
  (refacter ledger `NRO-04`); projection converges in the schedule-owned
  `TransientEffectProjectionSystem`. Per-subclass hooks are prohibited; a future delegated effect
  type must arrive as a per-instance claim, never as a queue-wide suppression.

### General choke-point selection rules

1. Prefer **containers, base classes, or single call sites** over per-concrete-class patches.
   A hook that scales with subclass count is a design smell in this contract.
2. Before writing any patch, verify the exact target method descriptor with `javap`. The
   scanner deduplicates by method **name**, so overload folding hides parameter differences;
   a patch compiled against the wrong descriptor fails at load time or, worse, attaches to a
   different overload.
3. Record the choice (site, descriptor, observed branch conditions) in the manifest
   justification so the next auditor can re-derive it without decompiling.

## 5. New `@SpirePatch` precondition checklist

AGENTS.md requires explicit design before adding native patches. A family integration proposal
is ready for implementation only when every box is checkable:

- [ ] **Family attribution confirmed** — the path resolves through an existing
      `families.py` rule, or a new rule + `test_families.py` fixture row land in the same
      change; no path may rely on fallback matching.
- [ ] **Choke-point argument documented** — container / base-class / single-call-site
      justification per recipe C rules; per-concrete-class hooks need an explicit exception
      rationale.
- [ ] **Overload descriptor verified** — `javap -p` output for the exact target method
      (visibility + parameter types) attached to the design; scanner name-dedupe caveat
      addressed.
- [ ] **Disposition plan including fail-open** — expected `RenderDisposition` mode per state,
      panic / unknown-owner / bridge-error behavior, and confirmation that
      `SpireReturn.Return(null)` occurs only on `DELEGATE_TO_ART`.
- [ ] **Ledger / evidence closure designed** — invocation → disposition → draw-evidence
      correlation ids, cleanup triggers (complete, cancel, scene change, host recreation),
      and where the `<Family>Ledger` lives.
- [ ] **Manifest entry + justification** — `family`, policy (or deliberate family-default
      inheritance), non-empty `justification`, and the owning test reference for suppressing
      patches.
- [ ] **Test plan** — pure API/JUnit coverage for the gate and registry logic, plus the D1
      scenario when hook/render source changes (deploy + device verification happen only
      after JUnit passes).

## 6. Known limitations

- **Overload folding undercounts.** Paths are `(class, method-name)` pairs; distinct
  byte-code overloads collapse into one row, so the true invocation-entry count is higher
  than 488.
- **Name-granularity vs byte-code entries.** For the same reason, hook targets recorded by
  name cannot distinguish which overload actually ran; dynamic ledger correlation must key on
  descriptors, not names.
- **Method-name whitelist misses.** The scanner only accepts
  `render|draw|renderHand|renderTip|renderRelics|renderPowers|renderIntent`. Verified
  rendering methods outside that set include: `renderTargetingUi` (`AbstractPlayer`,
  `PotionPopUp` — reserved by `overlay-targeting`), `AbstractPower#renderIcons` /
  `AbstractPower#renderAmount`, `AbstractPlayer#renderOrb` / `#renderStatScreen` /
  `#renderPowerTips` / `#renderHoverReticle` / `#renderBlights`,
  `EndTurnButton#renderHoldEndTurn` / `#renderGlowEffect`,
  `OverlayMenu#renderBlackScreen`, and `TipHelper#renderGenericTip` / `#renderTipForCard`.
  They are invisible to the inventory until the whitelist or an explicit extra-names rule
  grows; do not treat their absence as nonexistence.
- **Base-class / subclass double counting.** Abstract bases and their concrete children both
  appear (e.g. `AbstractGameEffect` plus its subclasses, `AbstractRoom` plus room shells).
  Governance resolves this by ruling on the **base-class hook + subclass difference audit**,
  never by summing rows.
- **Closed helper enumeration.** `draw-primitives-tips` is an exact-set rule and every other
  unmatched path raises `ValueError`; adding a new native render class therefore fails the
  scan loudly until `families.py` gains an explicit rule. This is protective, not incidental.

## Related

- [`native-render-coverage-sdd.md`](./native-render-coverage-sdd.md) — disposition + evidence-ledger contract (sister doc)
- [`traditional-ecs.md`](./traditional-ecs.md) — ECS rules, schedule phases, state classification
- [`backend-context.md`](./backend-context.md) — backend faces, context frames, intent path
- [`godot-aligned-ui.md`](./godot-aligned-ui.md) — C1 synthetic windows / LML / component kinds used by recipe A
- [`../../tools/nrcc/families.py`](../../tools/nrcc/families.py) — the only authority for family assignment

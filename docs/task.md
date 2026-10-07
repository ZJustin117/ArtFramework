# ArtFramework open tasks

Checkbox list for open work. Tick when done; milestone notes stay short.

- [x] Slice A atlas resource bridge: pure host-neutral `artframework.assets.AtlasRegion` value type
  (packed page rect, rotation/display swap, trim metadata, clamped UV, fail-safe invalid handling)
  with focused JUnit. No runtime/renderer/resolver wiring; an STS1 adapter can populate it later.

- [x] Slice B1 legacy libGDX atlas parser: pure `artframework.assets.LibGdxAtlasParser` maps the
  indentation/bare-token atlas syntax (including the `Spine42AtlasMaterializer` legacy shape) into
  host-neutral `AtlasRegion` values with required `xy`/`size` validation, rotate/orig/offset
  mapping, and fail-safe `size: 0,0` pages. Focused JUnit only; no runtime/resolver/renderer wiring.

- [x] Slice B2 STS1 host atlas materializer: `artframework.sts1.assets.Sts1AtlasMaterializer` maps a
  logical atlas key + region name to a page `Texture` + `AtlasRegion` via the pure
  `LibGdxAtlasParser`, behind an injectable `AtlasProvider` seam (fail-open, cached, no GL in tests,
  textures borrowed not disposed, rotated regions returned un-swapped). No renderer wiring yet.

- [x] Slice C atlas host bridge seam: `AtlasRegion.uvSourceRect()` exposes the packed region as a
  normalized `{x, y, width, height}` UV source rect (clamped, fail-safe full texture, rotation
  handling left to the caller), and `Sts1AtlasMaterializer.setProvider(AtlasProvider)` is now a
  public host SPI (null restores the inert fail-open default, swap clears cached borrows under the
  lock). Focused JUnit only; no renderer or host binding implementation.

- [x] Slice D libGDX atlas-region adapter: `artframework.sts1.assets.Sts1GdxAtlasRegions`
  converts a libGDX `TextureAtlas.AtlasRegion` into the host-neutral `artframework.assets.AtlasRegion`
  (packed bounds copied un-swapped, `degrees` from `rotate`, trim/offset pass-through, empty page
  label residual, null/zero-size fail-open, no GL) for the `ImageMaster.vfxAtlas` path. Focused
  JUnit only; no runtime/resolver/provider/renderer wiring.

- [x] Slice E live libGDX atlas source: `Sts1AtlasMaterializer.LiveAtlasSource` +
  `setLiveAtlasSource` / `regionFromAtlas` resolve a region straight from a live
  `TextureAtlas` (the `ImageMaster.vfxAtlas` shape) via `findRegion` -> `Sts1GdxAtlasRegions.fromGdx`,
  fully fail-open (blank args, no/null atlas, missing/invalid region, throwing source all return
  null), memoizing failed atlas keys and caching resolved borrows by `(atlasKey, regionName)`;
  `clearCache()` does not dispose textures and `probeSlice()` gains an additive `liveRegionCount`.
  Existing text path (`region(...)`) unchanged. Focused no-GL JUnit only; no renderer wiring.

- [x] ART 24 room-shells observe-first metadata/overlay: event, Neow, and generic fallback paths
  retain native pixels; delegated rest/shop/reward/treasure policy is unchanged.

## Infrastructure (P0–P2)

- [x] Native isolate vertical slice: primary `allow <target> on|off` command syntax, strict method
      targets, policy reset, immutable snapshot projection refresh, surface/skeleton/effect typed
      bridges, covered effect suppression ownership, and mounted/FULL D1 assertions are shipped.
      Uncovered native owners continue to fail open and are not claimed by the isolate command.
      Allow exemptions restore native continuation rather than ART delegation; D1 records this via
      `lastExemptionNativeContinuation`, and background policy remains independent. D1 now covers
      hand, controls, energy, and top-panel owners in one live combat scene; targeting remains
      observe-only, skeleton claims remain per instance, and unpatched map/event/reward/rest/shop/
      treasure/relic/power/card owners remain outside isolate coverage.

- [ ] Migrate a single `stances-state` instance end to end (S2): draw-input observation, shared-frame payload, ART draw + DELEGATE token consumption, and a default-off delegation/suppression seam are shipped, but the shipped `AbstractStance.render` seam is inert on vanilla (vanilla never sets `img`; no stance textures exist), so the real pixel authority for the stance family is `vfx-stance-aura` (`StanceAuraEffect` / `CalmParticleEffect` / `WrathParticleEffect` / `DivinityParticleEffect`). Next: repoint the visual-pixel takeover at `vfx-stance-aura` (or only enable the existing seam when a mod supplies a stance texture), keeping D1 per-stance pixel/order evidence before enabling the gate.

- [x] NRO-04 aura F2 (atlas draw): F1 shipped the default-off `vfx-stance-aura` per-instance claim
      plumbing — `AuraDelegationGate` + `AuraClaimPolicy` (exact FQNs `StanceAuraEffect` /
      `WrathParticleEffect` / `DivinityParticleEffect`) + the injected `AuraArtRenderer` draw seam,
      with `EFFECT_INVOCATIONS` token/evidence correlation in `NativeRenderBridge.beginEffectRender`
      consumed by the container/direct-draw patches (fail-open). The default renderer is never ready,
      so there is no pixel/visual change yet. F2 will supply the real ART atlas draw
      (`ImageMaster.EXHAUST_L` / `GLOW_SPARK` / `EYE_ANIM_0`) behind the same seam, then take D1
      per-stance pixel/order evidence before enabling `art aura on`.

- [x] NRO-04 aura F2a (pure geometry): `AuraDrawGeometry` maps the three claimable
      `vfx-stance-aura` FQNs (`AuraClaimPolicy` constants) to a `Kind` and computes the exact
      native draw arguments per kind — center origin `(pw/2, ph/2)`, width/height `(pw, ph)`,
      `y + vY` for the particle players, and the Wrath scalar
      `(0.1f + ((durDiv2*2f - duration)*2f*scale)) * Settings.scale` with `scaleX = scale*0.8f`.
      Pure host-neutral data (no GL / libGDX / STS imports); color/blend (additive 770/1, restored
      770/771) and UV stay with the host draw. Focused JUnit only; no renderer/bridge/glue wiring
      yet — F2 still supplies the real ART atlas draw behind the F1 seam.

- [x] NRO-04 aura F2b1 (renderer field reader + readiness): `Sts1AuraArtRenderer` implements
      `AuraArtRenderer.Adapter`; `isReady` is the exact-FQN `AuraDrawGeometry.kindFor` check and a
      package-visible `readFields(Object)` snapshots the native effect's own draw fields, walking the
      superclass chain (`getDeclaredField`+`setAccessible`) so inherited `scale`/`rotation`/`color`
      resolve. Required `x`/`y`/`vY`/`img`/`scale`/`rotation`/`color`; optional `dur_div2`/`duration`
      default to 0; any missing/unreadable/mistyped required field yields `null` and never throws.
      `render` stays inert (`false`) — the real ART atlas draw is F2b2. Focused no-GL JUnit.

- [x] NRO-04 aura F2b2 (real atlas draw + public install): `Sts1AuraArtRenderer.render` now replays
      the native additive draw — `readFields` → live `img` `AtlasRegion` → `Sts1GdxAtlasRegions.fromGdx`
      (`valid()` gate) → `AuraDrawGeometry.params(kind, ..., Settings.scale, regionWidth, regionHeight)`
      → `SpriteBatch.draw(TextureRegion, ...)` (the region overload, so baked atlas rotation/flip UVs
      match native) with color/blend save-restore (additive 770/1 → restore 770/771) and full
      `Throwable` fail-open. `AuraArtRenderer.install(Adapter)` / `uninstall()` are the public entry;
      default stays inert until installed. Update `docs/task.md` only; no bridge/patch/console wiring.

- [x] NRO-04 aura F2c (production binding): the real `Sts1AuraArtRenderer` is now installed at
      mod init via the idempotent `Sts1HostAssets.installAuraRenderer()` entry point the bootstrap
      calls (`ArtFrameworkMod.receivePostInitialize`, in the existing guarded `try/catch Throwable`
      style). Readiness for the three supported FQNs flows through `AuraArtRenderer.isReady`, which
      the `art aura status` console probe already consults via `AuraClaimPolicy.supportedClasses()`.
      The default-off `AuraDelegationGate` still controls whether the renderer is consulted, so
      native remains authoritative while `art aura off`. The renderer holds no host/GL state, so no
      `PresentSafety` host-recreation hook is added. Reversible via
      `Sts1HostAssets.resetAuraRendererForTests()`.

- [x] NRO-04 aura F3b (lab spawn + draw counter): `art aura spawn <stance|wrath|divinity> [count]`
      queues native aura effects through `AuraLabSpawn.spawn(kind, count)` (count defaults to 3 and is
      clamped 1..20) and `art aura clear` removes queued/active aura effects via `AuraLabSpawn.clear()`.
      A static `AuraArtRenderer.recordDraw()` counter — incremented by both claim patches only on the
      successful-draw branch, never on the fail-open path — is surfaced by `art aura status` as
      `draws=<n>`, so a device run can confirm ART-drawn auras without double-draw. Reversible default:
      the counter and spawn helper change no claim/suppression/gate logic.

- [x] NRO-04 aura F4 (self-asserting D1 probe + scenario): `AuraArtRenderer.probeSlice()` is a
      read-only slice (`gate` = `AuraDelegationGate.isActive()`, `ready` = count of
      `AuraClaimPolicy.supportedClasses()` the renderer reports ready, `draws` = `drawCount()`),
      exported by `Sts1RenderPipeline.probeSlice()` as `backend.renderPlan.aura` and consumed by the
      device scenario [`tests/ui-scenarios/device/d1_aura_claim.yaml`](../tests/ui-scenarios/device/d1_aura_claim.yaml).
      The scenario A/B-verifies the family: with `art aura off` it asserts `gate eq false` and
      `draws eq 0` (native-authoritative), and with `art aura on` it asserts `ready gte 1`,
      `gate eq true`, and `draws gte 1` (proof ART drew) alongside `nativeRenderStrict.accepted` and
      zero `delegatedWithoutEvidence`/`orphanArtOutput`. The slice only reads state; it changes no
      claim, suppression, or gate logic. D1-verified (Redmi Note 8): scenario PASS — gate-OFF
      `draws` delta 0 vs gate-ON `draws` 0→760, `nativeRenderStrict.accepted=true`, zero
      `delegatedWithoutEvidence`/`orphanArtOutput` deltas.

- [x] NRO-04 aura F5 (4th claimable FQN: Calm): `CalmParticleEffect` is now a supported claimable
      `vfx-stance-aura` FQN — `AuraClaimPolicy.CALM_PARTICLE_EFFECT` is appended to `supportedClasses()`
      (last, preserving declaration order), `AuraDrawGeometry.Kind.CALM_PARTICLE` mirrors its native
      draw (`origin 32,32`, `size 25×128`, `scaleY = scale + (dur_div2*0.4f - duration)*Settings.scale`,
      source rect `0,0,64,64`; packed size and `vY` ignored), and `Sts1AuraArtRenderer` resolves the
      bare `ImageMaster.FROST_ACTIVATE_VFX_1` `Texture` (Calm has no `img`) through the raw texture +
      source-rect draw overload behind the same fail-open color/blend save-restore. `art aura spawn
      calm [count]` and `AuraLabSpawn.clear()` cover it via `AuraClaimPolicy.supports`, and the
      `d1_aura_claim.yaml` gate-ON phase spawns `calm 4`. Default-off gate and per-instance token
      semantics unchanged; no new patch, bridge, or console wiring. Focused no-GL JUnit only.

- [x] NRO-04 aura F6 (5th claimable FQN: DivinityStanceChangeParticle):
      `DivinityStanceChangeParticle` is now the 5th claimable `vfx-stance-aura` FQN —
      `AuraClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE` is appended last to `supportedClasses()`,
      `AuraDrawGeometry.Kind.DIVINITY_STANCE_CHANGE` mirrors its deterministic img/`TextureRegion`
      draw (x/y passthrough, center origin, packed size, uniform `scaleX=scaleY=scale`; the class has
      no `vY` field), grouped with `STANCE_AURA` for the identical formula. `AuraLabSpawn`
      `classNameFor("divinitychange")` (alias `"dsc"`) maps to it and `art aura spawn
      divinitychange 4` runs in both `d1_aura_claim.yaml` phases. `Sts1AuraArtRenderer.readFields`
      now treats `vY` as an OPTIONAL field defaulting to `0` (the three original img-based kinds
      still set it; the new kind resolves without it), while `x`/`y`/`scale`/`rotation`/`color`/
      `img` stay required. Default-off gate, fail-open, and per-instance token semantics unchanged;
      no new patch, bridge, or console wiring. Focused no-GL JUnit only.

- [x] NRO-04 generalization (family-neutral seam naming): the default-off per-instance
      transient-effect claim seam is renamed to family-neutral `Vfx*` types so future non-aura vfx
      families can join without an aura-scoped name: `VfxClaimPolicy`, `VfxDrawGeometry`,
      `VfxArtRenderer`, `VfxDelegationGate`, `Sts1VfxArtRenderer`, `VfxLabSpawn` (plus
      `NativeRenderBridge.isVfxClaimInvocation`, `Sts1HostAssets.installVfxRenderer` /
      `resetVfxRendererForTests`). Behavior, constants, `Kind` values, the `vY` requirement, and the
      default-off gate are unchanged. External aliases: the diagnostic probe keeps
      `backend.renderPlan.aura` and adds the identical `backend.renderPlan.vfxClaim`; the console
      gains `art claim` as the family-neutral primary route for
      `on|off|status|spawn <kind> [count]|clear`, with `art aura` retained as an identical legacy
      alias. `art vfx` stays the pre-existing STS2 bundle-runtime command (`status|clear|load`,
      prefix `ART_VFX`) and is never routed to the claim seam.
      The `vfx-stance-aura` family name is retained.

- [x] NRO-04 aura F7 (cross-family: first non-aura claimable, `LightFlareSEffect`):
      `com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect` (family `vfx-scene-world`) is the first
      non-aura member of the generalized per-instance claim seam —
      `VfxClaimPolicy.SCENE_LIGHT_FLARE` is appended last to `supportedClasses()` and
      `VfxDrawGeometry.Kind.LIGHT_FLARE` reuses the `STANCE_AURA` geometry branch (x/y passthrough,
      center origin, packed size, uniform scale). The native draw is additive (`770/1` restored to
      `770/771`) with the same end state as the aura classes (blend-before-color native order is
      order-insensitive), and `LightFlareSEffect` has no `vY` field, so the optional-`vY` reader path
      already covers it. `VfxLabSpawn.classNameFor("flare")` (alias `"lightflare"`) constructs
      `new LightFlareSEffect(960f, 540f)` behind the existing fail-open guard (its static `imgs` may
      be null off-game), and `art claim spawn flare 4` runs in both `d1_aura_claim.yaml` phases.
      Default-off gate, fail-open, and per-instance token semantics unchanged; no new patch, bridge,
      or console wiring. Focused no-GL JUnit only.

- [x] NRO-04 F8 (per-kind blend policy + first `vfx-combat` claimable, `FlashAtkImgEffect`): blend
      handling is now a pure per-kind policy, `VfxDrawGeometry.additiveBlend(Kind)` — additive
      (install `770/1`, restore `770/771`) for every existing kind and ambient for the new
      `Kind.FLASH_ATK_IMG`. `com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect` (family
      `vfx-combat`) is the first member whose native `render` never calls `setBlendFunction`; it
      draws under the ambient blend, so the claim draw must not switch blend state and restores only
      the previous color (the renderer tracks whether blend was changed and restores `770/771` only
      when it was). It reuses the `STANCE_AURA` geometry branch (x/y passthrough, center origin,
      packed size, uniform scale) and has no `vY`, so it stays on the optional-`vY` reader path.
      `VfxClaimPolicy.FLASH_ATK_IMG` is appended last to `supportedClasses()`;
      `VfxLabSpawn.classNameFor("flash")` (alias `"flashatk"`) constructs
      `new FlashAtkImgEffect(960f, 540f, AttackEffect.BLUNT_HEAVY)` behind the existing fail-open
      guard (its static `ImageMaster` regions may be null off-game), and `art claim spawn flash 4`
      runs in both `d1_aura_claim.yaml` phases. The cross-family claimable list is now stance-aura +
      scene-world + combat. Default-off gate, fail-open, and per-instance token semantics unchanged;
      no new patch, bridge, or console wiring. Focused no-GL JUnit only.

- [x] NRO-04 F9 (three more `vfx-scene-world` members): `LightFlareMEffect`, `LightFlareLEffect`,
      and `TorchParticleLEffect` (`vfx-scene-world`) join the claim seam reusing the additive
      center-packed geometry — `VfxClaimPolicy.SCENE_LIGHT_FLARE_M`/`SCENE_LIGHT_FLARE_L`/
      `SCENE_TORCH_PARTICLE_L` are appended last to `supportedClasses()`, and
      `VfxDrawGeometry.Kind.LIGHT_FLARE_M`/`LIGHT_FLARE_L`/`TORCH_PARTICLE_L` share the existing
      `STANCE_AURA` branch (x/y passthrough, center origin, packed size, uniform scale, additive
      blend). The native `render(SpriteBatch)` bodies are byte-shape identical to
      `StanceAuraEffect`/`LightFlareSEffect`; the flare pair picks `img` from its static `imgs[]` at
      construction time (constructor-time randomness only) and `TorchParticleLEffect` obtains `img`
      via a private `getImg()`, so the render-time draw is deterministic. `TorchParticleLEffect` has
      a `vY` field but its native `render` never reads it (used only by `update()`), so the
      optional-`vY` reader path covers it;
      `VfxLabSpawn.classNameFor("flareM"/"flareL"/"torch")` (aliases `"lightflareM"`/`"lightflareL"`/
      `"torchparticle"`) construct `new LightFlareMEffect(960f, 540f)`/`new LightFlareLEffect(960f,
      540f)`/`new TorchParticleLEffect(960f, 540f)` behind the existing fail-open guard, and
      `art claim spawn flareM|flareL|torch 4` runs in both `d1_aura_claim.yaml` phases. No new
      formula, patch, bridge, or console wiring; default-off gate and per-instance token semantics
      unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F10 (five more members: two additive fire bursts + three ambient members):
      `com.megacrit.cardcrawl.vfx.FireBurstParticleEffect` (`vfx-misc-root`) and
      `com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect` (`vfx-combat`) join the claim
      seam reusing the additive `STANCE_AURA` geometry (x/y passthrough, center origin, packed size,
      uniform scale, additive blend), and `com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect`
      (`vfx-combat`), `com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect`
      (`vfx-scene-world`), and `com.megacrit.cardcrawl.vfx.NemesisFireParticle` (`vfx-misc-root`)
      join with the same geometry but are the first ambient (no-`setBlendFunction`) members beyond
      `FlashAtkImgEffect`: `VfxDrawGeometry.additiveBlend` now returns `false` for
      `SMOKE_BLUR`/`CEILING_DUST`/`NEMESIS_FIRE` (and `FLASH_ATK_IMG`) and `true` for the two fire
      bursts and every other kind. `VfxClaimPolicy.FIRE_BURST`/`RED_FIRE_BURST`/`SMOKE_BLUR`/
      `CEILING_DUST`/`NEMESIS_FIRE` are appended last to `supportedClasses()` in that order, and
      `VfxDrawGeometry.Kind.FIRE_BURST`/`RED_FIRE_BURST`/`SMOKE_BLUR`/`CEILING_DUST`/`NEMESIS_FIRE`
      reuse the single existing additive-params branch. All five have a `vY` field used only by
      `update()`, so they stay on the optional-`vY` reader path and no kind joins the
      `WRATH_PARTICLE`/`DIVINITY_PARTICLE` `requireVY` set. `VfxLabSpawn.classNameFor` gains
      `"fireburst"`/`"fire"`, `"redfireburst"`/`"redfire"`, `"smokeblur"`/`"smoke"`,
      `"ceilingdust"`/`"dust"`, and `"nemesisfire"`/`"nemesis"` (aliases checked against the
      existing set), constructing the five effects at `(960f, 540f)` behind the existing fail-open
      guard, and `art claim spawn fire|redfire|smoke|dust|nemesis 4` runs in both
      `d1_aura_claim.yaml` phases. No new formula, patch, bridge, or console wiring; default-off gate
      and per-instance token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F11 (third draw shape: bare `Texture` + fixed source rect):
      `com.megacrit.cardcrawl.vfx.ShieldParticleEffect` and
      `com.megacrit.cardcrawl.vfx.DebuffParticleEffect` join the claim seam, making the
      bare-`Texture` + fixed-source-rect shape kind-driven (host-neutral constants in
      `VfxDrawGeometry`) rather than Calm-specific. `VfxClaimPolicy.SHIELD_PARTICLE`/
      `DEBUFF_PARTICLE` append last to `supportedClasses()` in that order, and
      `VfxDrawGeometry.Kind.SHIELD_PARTICLE`/`DEBUFF_PARTICLE` gain their own `params` branches:
      Shield draws `ImageMaster.INTENT_DEFEND` additively with a hardcoded `0f` rotation
      (`x-32f, y-32f, origin 32/32, size 64x64, src 0,0,64,64`), and Debuff draws its own instance
      `Texture img` under the ambient blend consuming its `rotation` field
      (`x-16f, y-16f, origin 16/16, size 32x32, src 0,0,32,32`); `additiveBlend` now returns
      `false` for `DEBUFF_PARTICLE` (and `true` for `SHIELD_PARTICLE`). `Sts1VfxArtRenderer`'s
      Calm-only branch is generalized to a kind-driven texture-rect path (`readTextureFields` +
      `renderTexture`) shared by Calm, Shield, and Debuff with no Calm behavior change (Calm keeps
      its `scaleY` formula and additive blend). `isReady` stays a policy/kind check for both new
      exact FQNs. `VfxLabSpawn.classNameFor` gains `"shield"` (`new ShieldParticleEffect(960f,
      540f)`) and `"debuff"` (`new DebuffParticleEffect(960f, 540f)`, aliases checked against the
      existing set) behind the existing fail-open guard, and `art claim spawn shield|debuff 4` runs
      in both `d1_aura_claim.yaml` phases. No new formula, patch, bridge, or console wiring;
      default-off gate and per-instance token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F12 (four more members: two additive + two ambient, existing geometry only):
      `com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect` (`vfx-scene-world`) and
      `com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect`, `com.megacrit.cardcrawl.vfx
      .GenericSmokeEffect`, and `com.megacrit.cardcrawl.vfx.ExhaustBlurEffect` (all three
      `vfx-misc-root`) join the seam reusing the additive center-packed `STANCE_AURA` geometry
      (x/y passthrough, center packed/2 origin, packed w/h, uniform scale, rotation) — no new
      formula branch. `TorchParticleXLEffect` and `GhostlyWeakFireEffect` install
      `setBlendFunction(770, 1)` before and `(770, 771)` after, so `VfxDrawGeometry.additiveBlend`
      reports `true` for them; `GenericSmokeEffect` and `ExhaustBlurEffect` never call
      `setBlendFunction` (ambient), so it reports `false`. All four own a `vY` used only by
      `update()` and ignored by `render`, so they stay on the optional-`vY` reader path and no kind
      joins the `WRATH_PARTICLE`/`DIVINITY_PARTICLE` `requireVY` set. `VfxClaimPolicy
      .SCENE_TORCH_PARTICLE_XL`/`GHOSTLY_WEAK_FIRE`/`GENERIC_SMOKE`/`EXHAUST_BLUR` append last to
      `supportedClasses()` in that order, and `VfxDrawGeometry.Kind.TORCH_PARTICLE_XL`/
      `GHOSTLY_WEAK_FIRE`/`GENERIC_SMOKE`/`EXHAUST_BLUR` map via the existing additive-params
      branch. `VfxLabSpawn.classNameFor` gains `"torchxl"` (`new TorchParticleXLEffect(960f, 540f)`),
      `"ghostlyfire"`/`"ghostly"` (`new GhostlyWeakFireEffect(960f, 540f)`),
      `"genericsmoke"`/`"gsmoke"` (`new GenericSmokeEffect(960f, 540f)`), and
      `"exhaustblur"`/`"exhaust"` (`new ExhaustBlurEffect(960f, 540f)`) — aliases checked against
      the existing set for collisions — behind the existing fail-open guard, and
      `art claim spawn torchxl|ghostlyfire|genericsmoke|exhaustblur 4` runs in both
      `d1_aura_claim.yaml` phases. No new formula, patch, bridge, or console wiring; default-off
      gate and per-instance token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F13 (two more members: the bare-`Texture` shape-C path, one with a new pure
      color rule): `com.megacrit.cardcrawl.vfx.combat.IceShatterEffect` and
      `com.megacrit.cardcrawl.vfx.combat.WebParticleEffect` (both `vfx-combat`) join the existing
      bare-`Texture` + fixed-source-rect ("shape C") path with no fourth draw path and no new formula
      branch. `IceShatterEffect` resolves its own instance `Texture img` (chosen in its constructor
      from `ImageMaster.FROST_ACTIVATE_VFX_1`/`_2`), so `readTextureFields` requires `img` to be a
      `com.badlogic.gdx.graphics.Texture` (AtlasRegion/null fails open) and requires the inherited
      `rotation` field (origin `32,32`, size `64×64`, src `0,0,64,64`, offset 0, additive) — the
      `rotation` field IS consumed; `WebParticleEffect` has no `img` field, resolving the static
      `ImageMaster.WEB_VFX` `Texture` instead, hardcodes rotation `0f` (so `rotation` is NOT
      required), and its native `render` calls `setColor(new Color(1f, 1f, 1f, color.a))` — RGB
      forced white, alpha from the effect color — which is captured by the new pure predicate
      `VfxDrawGeometry.whiteAlphaOnly(Kind)` (true only for `WEB_PARTICLE`, false for every other
      kind, `IllegalArgumentException` on null, matching `additiveBlend`); the existing size-C
      color-save/restore is unchanged. Both are additive (`additiveBlend` true). `VfxClaimPolicy
      .ICE_SHATTER`/`WEB_PARTICLE` append last to `supportedClasses()` in that order, and
      `VfxDrawGeometry.Kind.ICE_SHATTER`/`WEB_PARTICLE` map via the existing fixed-rect params branch
      (`WEB` has no `vY` field at all, while `ICE` owns a `vY` that its `render` ignores; `WEB` forces
      rotation `0f`). `VfxLabSpawn.classNameFor` gains
      `"iceshatter"`/`"ice"` (`new IceShatterEffect(960f, 540f)`) and `"web"`/`"webparticle"`
      (`new WebParticleEffect(960f, 540f)`) — aliases checked against the existing set for collisions
      — behind the existing fail-open guard, and `art claim spawn iceshatter 4` / `art claim spawn
      web 4` run in both `d1_aura_claim.yaml` phases. Default-off gate + per-instance token semantics
      unchanged; focused no-GL JUnit only.

- [x] NRO-04 F14 (four more members: one reusing the Web static-texture config, two ambient
      center-packed incl. a static-`img` member, and one new ambient 128-rect):
      `com.megacrit.cardcrawl.vfx.combat.EntangleEffect` is byte-identical to `WebParticleEffect` —
      the static `ImageMaster.WEB_VFX` `Texture`, offset 0, origin `32,32`, size `64×64`, src
      `0,0,64,64`, hardcoded rotation `0f`, additive blend, and the same `setColor(new Color(1,1,1,
      color.a))` white-alpha rule — so it reuses the WEB_PARTICLE static-texture/white-alpha
      configuration (`VfxDrawGeometry.Kind.ENTANGLE` maps to the same params and
      `whiteAlphaOnly` is now true for both; the renderer resolves both kinds to the single
      `ImageMaster.WEB_VFX` texture and one shared `WEB_*` src rect instead of duplicating logic);
      `com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect` and
      `com.megacrit.cardcrawl.vfx.ExhaustPileParticle` reuse the center-packed `STANCE_AURA`
      params branch with ambient blend (`additiveBlend` false), with `ExhaustPileParticle.img` being a
      `private static AtlasRegion` declared on its own class — the existing `readFields`
      superclass-walking `readRaw` resolves it via `getDeclaredField` + `field.get(effect)` (which
      works for statics) with no reader change, asserted by a static-field holder test; and
      `com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect` introduces a NEW ambient shape-C
      fixed rect `sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, rotation, 0, 0,
      128, 128, false, false)` over its own instance `Texture img` with new host-neutral constants
      (`UNKNOWN_OFFSET`/`UNKNOWN_ORIGIN`/`UNKNOWN_SIZE`/`UNKNOWN_SRC_*`) and the `rotation` field
      consumed (`readTextureFields` requires `rotation` for UNKNOWN and does NOT for ENTANGLE, which
      is forced `0f`). `VfxClaimPolicy.ENTANGLE_EFFECT`/`BLOCK_IMPACT_LINE`/
      `EXHAUST_PILE_PARTICLE`/`UNKNOWN_PARTICLE` append last to `supportedClasses()` in that order.
      `VfxLabSpawn.classNameFor` gains `"entangle"` (`new EntangleEffect(960f, 540f, 960f, 540f)` —
      the ctor is `(tX, tY, startX, startY)` and sets `x=startX, y=startY` with target
      `(tX-32, tY-32)`, so passing the center for both keeps it centered),
      `"blockline"`/`"blockimpact"` (`new BlockImpactLineEffect(960f, 540f)`),
      `"exhaustpile"`/`"exhaustparticle"` (`new ExhaustPileParticle(960f, 540f)`), and
      `"unknown"`/`"unknownparticle"` (`new UnknownParticleEffect(960f, 540f)`) — aliases checked
      against the existing set for collisions — behind the existing fail-open guard, and
      `art claim spawn entangle|blockline|exhaustpile|unknown 4` runs in both `d1_aura_claim.yaml`
      phases. No new patch, bridge, or console wiring; default-off gate + per-instance token
      semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F15 (five more `vfx-combat` members: two additive + two ambient center-packed and one
      new additive 74×74 shape-C rect):
      `com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect` and
      `com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect` reuse the additive
      center-packed `STANCE_AURA` params branch;
      `com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect` and
      `com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect` reuse that same geometry under the
      ambient blend (`additiveBlend` false, no `setBlendFunction`; only `DamageImpactLineEffect`
      guards its draw with `if (!isDone)`, the blur is straight-line); and
      `com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect` introduces a NEW additive shape-C
      fixed rect `sb.draw(img, x - 37f, y - 37f, 37f, 37f, 74f, 74f, scale, scale, rotation, 0, 0,
      74, 74, false, false)` over its own instance `Texture img` with new host-neutral constants
      (`DARK_ORB_OFFSET`/`DARK_ORB_ORIGIN`/`DARK_ORB_SIZE`/`DARK_ORB_SRC_*`, the src `0,0,74,74`
      being the full 74×74 region it passes natively) and the inherited `rotation` field consumed
      (`readTextureFields` requires `rotation` for DARK_ORB_PASSIVE and resolves the instance
      `Texture`, both added to `usesInstanceTexture`). `VfxClaimPolicy.FLAME_PARTICLE`/
      `LIGHTNING_ORB_ACTIVATE`/`DAMAGE_IMPACT_BLUR`/`DAMAGE_IMPACT_LINE`/`DARK_ORB_PASSIVE` append
      last to `supportedClasses()` in that order. `VfxLabSpawn.classNameFor` gains `"flame"`
      (`new FlameParticleEffect(960f, 540f)`), `"lightningorb"`/`"lightningactivate"`
      (`new LightningOrbActivateEffect(960f, 540f)`), `"damageblur"`/`"dmgblur"`
      (`new DamageImpactBlurEffect(960f, 540f)`), `"damageline"`/`"dmgline"`
      (`new DamageImpactLineEffect(960f, 540f)`), and `"darkorb"`/`"darkorbpassive"`
      (`new DarkOrbPassiveEffect(960f, 540f)`) — aliases checked against the existing set for
      collisions — behind the existing fail-open guard, and
      `art claim spawn flame|lightningorb|damageblur|damageline|darkorb 4` runs in both
      `d1_aura_claim.yaml` phases. No new patch, bridge, or console wiring; default-off gate +
      per-instance token semantics unchanged. Focused no-GL JUnit only.

      Known limitations / deferred: (a) `FlameParticleEffect` sets `img.flip(!flipX, false)`
      immediately before its draw (`flipX` is a per-instance mirror), which this claimed draw did not
      reproduce — the claimed pixels were therefore un-mirrored relative to native for that member.
      **SUPERSEDED by F22 below**, which adds per-instance mirror support to the img path so
      `FlameParticleEffect` (and the new `SpookyChestEffect`/`IroncladVictoryFlameEffect`) now draw
      the mirrored rect; (b) `com.megacrit.cardcrawl.vfx.FallingDustEffect` and
      `com.megacrit.cardcrawl.vfx.combat.StunStarEffect` were screened here and DEFERRED — FallingDust
      because its native origin uses the region's `offsetX`/`offsetY` (it needed a region-offset-origin
      rule rather than an existing draw shape) and StunStar because its position is
      `x - vX*30f*Settings.scale`, `y - vY*5f*Settings.scale` (it needed a settings-scaled-offset rule
      keyed on the effect's own `vX`/`vY`). Both deferrals are now SUPERSEDED by the completed F16
      slice below, which claims FallingDust (region-offset origin) and StunStar (scaled `vX`/`vY`
      position offset).

- [x] NRO-04 F15b (benign no-pixel decline classification): a claimed instance whose native draw
      legitimately produces no pixels is no longer counted as a `dispositionMismatch` /
      `delegatedWithoutEvidence`. `VfxDrawGeometry.nativeSkipsDrawWithoutImage(kind)` is a pure
      predicate that is true ONLY for `FLASH_ATK_IMG` (whose native `FlashAtkImgEffect.render` guards
      the draw with `if (img != null)`, so a null-`img` instance draws nothing natively);
      `VfxArtRenderer.Adapter` gained `canDraw(Object effect)` (true when the adapter could actually
      draw this exact instance) with a public static delegator that returns false when no adapter is
      installed, implemented by `Sts1VfxArtRenderer.canDraw` to mirror `render`'s early-return
      conditions (fields resolve and the resolved region/texture is present; shared
      `resolveTexture` helper, no `render` behavior change). `NativeRenderBridge
      .recordEffectDeclined(invocationId, effect)` replaces the fail-open `recordEffectFailure` call
      in both observation patches: for a no-pixel kind the ART renderer cannot draw it either, so the
      delegated lifecycle is completed without pixel evidence
      (`completeDelegatedWithoutEvidence` + `recordNoPixelIsolation`) and the mismatch /
      terminal-missing-evidence counters are left untouched; every other declined claim keeps the
      existing fallback accounting, so a genuine renderer defect is never masked and
      `nativeRenderStrict.accepted` stays reachable under heavy mixed bursts. `recordEffectFailure`
      is retained for the other fallback paths. Focused no-GL JUnit only.

- [x] NRO-04 F15c (bounded per-class decline attribution for D1 diagnosis): the render ledger now
      tracks which native class fails open/declines on the GENUINE fallback path.
      `NativeRenderLedger.recordDeclinedClass(String)` keeps a bounded `LinkedHashMap` of
      class-name→count (cap `DECLINED_CLASS_CAPACITY` = 64 distinct names, oldest evicted beyond the
      cap and tracked by `declinedOverflow`; null/blank collapses to `"<unknown>"`). The probe
      exposes it additively as `declinedByClass` (bounded className→count map), `declinedTotal`
      (every decline, independent of the cap), and `declinedOverflow`; these are diagnostic-only and
      deliberately NOT added to `strictReport()`, so strict acceptance is untouched.
      `NativeRenderBridge.recordEffectFailure(long, effect)` (new effect-taking overload; the
      1-arg form delegates with `null` → `"<unknown>"`) and the genuine branch of
      `recordEffectDeclined(long, effect)` attribute the declining class; both observation patches
      already pass the effect instance. The F15b benign no-pixel branch (`nativeSkipsDrawWithoutImage`
      + `!canDraw`) is unchanged and is NOT attributed as a genuine decline.
      **No behavior change:** no claim/render/gate/token semantics, and no existing counter
      (`dispositionMismatch`, `delegatedWithoutEvidence`, `noPixelIsolation`, …) changed; only new
      additive probe keys. Focused no-GL JUnit only.

      Open question for a follow-up (documented, not fixed here): a claim that fails open ALWAYS
      continues natively (the patches call `effect.render(sb)` after the record call), so no pixels
      are actually lost — yet `recordDelegatedFallbackIfPending` still books it as
      `dispositionMismatch` + `delegatedWithoutEvidence`. Under heavy live combat with many claimable
      ambient effects this grows for the whole claimable set (D1 saw 109 == 109 with
      `noPixelIsolation == 0`), which may make the strict `accepted == 0` gate unreachable in heavy
      live combat even though nothing is mis-rendered; F15c is the first step so the offending classes
      can be NAMED before deciding whether the fallback should be exempted from the mismatch counter.

- [x] NRO-04 F15d (flip-invariant atlas conversion + canonical img-path draw): a shared static
      libGDX `AtlasRegion` (`ImageMaster.FLAME_1/2/3`) that a NATIVE sibling flips in place
      (`region.flip(!flipX, false)`) swaps `u`/`u2` (and `AtlasRegion.flip` mutates `offsetX`), so
      `getRegionX()` jumped to the RIGHT edge while `getRegionWidth()` stayed the packed footprint;
      `Sts1GdxAtlasRegions.fromGdx` copied `getRegionX()` into the neutral `x`, making
      `valid()` fail `x + width <= pageWidth` for regions near the page's right edge →
      `render` returned false → the claim failed open → `dispositionMismatch`/
      `delegatedWithoutEvidence` grew and `nativeRenderStrict.accepted` became unreachable in live
      combat (lab spawns never triggered a native flip, so per-FQN isolation showed `+0`). `fromGdx`
      is now flip-invariant: the neutral top-left is the MINIMUM of the two UV corners
      (`round(min(u,u2)*pageWidth)`, `round(min(v,v2)*pageHeight)`) and the packed footprint is
      unchanged by a flip, so it is byte-identical to `getRegionX()/getRegionY()` for an unflipped
      region. The img path additionally draws a canonical (pollution-immune) `TextureRegion` view
      (`u`=min, `u2`=max, `v`=min, `v2`=max, same texture) instead of the possibly-flipped shared
      region, since the claim suppresses the native draw; params/blend/color/kind routing are
      unchanged, and the null-region / invalid-neutral fail-open is preserved. This removes the false
      `FlameParticleEffect`/`RedFireBurstParticleEffect` fail-opens (both draw the shared `FLAME_*`
      regions); `RedFireBurstParticleEffect` never flips so parity is exact, while
      `FlameParticleEffect`'s per-instance `flipX` mirror remained the documented F15 limitation (the
      canonical orientation was drawn, not the per-instance mirror) — **now RESOLVED by F22 below,
      which adds per-instance mirror support to the img path**. No ledger-counter, F15b/F15c,
      gate/token, scenario-YAML, or non-img geometry change. Focused no-GL JUnit only.

- [x] NRO-04 screening note (SUPERSEDED by F16 below): `com.megacrit.cardcrawl.vfx.combat.WarningSignEffect`
      was screened here and DEFERRED — its `scale` is hardcoded `Settings.scale * 2f` (there is no
      `scale` field), so claiming it needed a new scale-rule capability rather than an existing draw
      shape. F16 below supplies that capability and claims WarningSignEffect.

- [x] NRO-04 F16 (three more members: two new pure rules + one static-texture fixed rect):
      `com.megacrit.cardcrawl.vfx.WarningSignEffect` is a bare-`Texture` fixed-rect kind over the
      static `ImageMaster.WARNING_ICON_VFX` with a NEW pure rule — the uniform scale is the
      hardcoded `Settings.scale * 2f` (the class has no `scale` field, so the effect scale is
      ignored) with a hardcoded zero rotation, new host-neutral constants
      (`WARNING_ORIGIN_X/Y`, `WARNING_WIDTH/HEIGHT`, `WARNING_SCALE_FACTOR`, `WARNING_SRC_*`) and
      additive blend; `com.megacrit.cardcrawl.vfx.combat.StunStarEffect` reuses the ambient
      center-packed `AtlasRegion` geometry with a NEW pure rule — the draw POSITION is offset by
      `-(vX * 30f * Settings.scale)`, `-(vY * 5f * Settings.scale)` (`STUN_STAR_VX_FACTOR`/
      `STUN_STAR_VY_FACTOR`), so it consumes its own `vX` AND `vY` (and, like the other
      `vY`-consuming kinds `WRATH_PARTICLE`/`DIVINITY_PARTICLE`, `Kind.STUN_STAR` is now in
      `Sts1VfxArtRenderer.requireVY`, so a StunStar instance with a missing/unreadable `vY` fails
      open instead of drawing at a wrong position); and
      `com.megacrit.cardcrawl.vfx.FallingDustEffect` reuses that ambient center-packed geometry with a
      NEW pure rule — the ORIGIN is the region's own `offsetX`/`offsetY` (NOT
      `packedWidth/2f`/`packedHeight/2f`). `VfxDrawGeometry.params(...)` gained three tail scalars
      (`float vX, float regionOffsetX, float regionOffsetY`) after `packedHeight` so the pure API
      stays host-neutral (numbers only) and every existing caller passes `0f` for them;
      `Kind.WARNING_SIGN`/`Kind.STUN_STAR`/`Kind.FALLING_DUST` were added and
      `additiveBlend` is `true` only for `WARNING_SIGN`. `Sts1VfxArtRenderer.Fields` additionally
      snapshots `vX` (optional, default 0) and the region's `offsetX`/`offsetY`, normalized back to
      the UNFLIPPED trim origin (the exact inverse of `AtlasRegion.flip`'s offset transform) when
      `isFlipX()`/`isFlipY()` reports a native flip — so the F15d flip-invariance carries to the
      `FALLING_DUST` origin with no residual. `VfxClaimPolicy.WARNING_SIGN`/`STUN_STAR`/`FALLING_DUST`
      append last to `supportedClasses()`/`supports(...)` in that order;
      `VfxLabSpawn.classNameFor` gains `"warning"`/`"warningsign"` (`new WarningSignEffect(960f,
      540f)`), `"stunstar"`/`"stun"` (`new StunStarEffect(960f, 540f)`), and
      `"fallingdust"`/`"fdust"` (`new FallingDustEffect(960f, 540f)`) — aliases checked against the
      existing set for collisions — behind the existing fail-open guard, and
      `art claim spawn warning|stunstar|fallingdust 4` runs in both `d1_aura_claim.yaml` phases. No
      new patch/bridge/console wiring; default-off gate + per-instance token semantics unchanged.
      Focused no-GL JUnit only.

- [x] NRO-04 F17 (three more members: two new pure origin rules + one ambient center-packed
      reuse): `com.megacrit.cardcrawl.vfx.combat.LightningEffect` reuses the additive center-packed
      `AtlasRegion` geometry with a NEW pure rule — the draw origin Y is `0f` (NOT
      `packedHeight/2f`) while origin X stays `packedWidth/2f`, size packed, uniform scale, and the
      field rotation; `com.megacrit.cardcrawl.vfx.FlameBallParticleEffect` reuses that additive
      center-packed geometry with a NEW pure rule — `originY = packedHeight/2f + 20f * settingsScale`
      (`FLAME_BALL_ORIGIN_Y_OFFSET` times the caller-supplied `settingsScale`, so the origin lift
      tracks `Settings.scale` exactly as native does; its `vY` is update-only and NOT consumed); and
      `com.megacrit.cardcrawl.vfx.ShineLinesEffect` reuses the ambient center-packed geometry
      UNCHANGED (origin `packedWidth/2f`, `packedHeight/2f`, no `setBlendFunction`, native
      `if (!isDone)` guard). `VfxDrawGeometry.params(...)` gained two MORE tail scalars
      (`float originOffsetX, float originOffsetY`) after the F16 tail params; the shared origin is
      now `originX = packedWidth/2f + originOffsetX`, `originY = packedHeight/2f + originOffsetY`, so
      every pre-existing kind passes `0f`/`0f` and its geometry is byte-identical (a regression
      assertion pins this). `Kind.LIGHTNING_EFFECT` passes `(0f, -packedHeight/2f)` (scale-independent,
      so its origin Y is exactly `0f`) and `Kind.SHINE_LINES` passes `(0f, 0f)`; `Kind.FLAME_BALL` does
      NOT use the tail scalar — it has its own pure branch that consumes the existing `settingsScale`
      argument to compute `packedHeight/2f + 20f * settingsScale`, so `VfxDrawGeometry` stays
      host-neutral (it never reads `Settings`). `additiveBlend`
      is `true` for `LIGHTNING_EFFECT`/`FLAME_BALL` and `false` for `SHINE_LINES` (ambient);
      `whiteAlphaOnly` is unchanged. `LIGHTNING_EFFECT`/`FLAME_BALL`/`SHINE_LINES` are img-path kinds,
      so `Sts1VfxArtRenderer` reads their `rotation` field unconditionally like every other img-path
      kind (the `requireRotation` list belongs solely to the bare-`Texture` path and is not extended);
      none of the three consumes `vY` (Lightning/ShineLines have no `vY` field and FlameBall's is
      update-only), so `vY` stays optional and none is added to `requireVY`. Lightning's origin offset
      is computed at the draw site (`-packedHeight/2f`); FlameBall's is handled in `params(...)`. The
      single existing img draw branch is kept with no new draw path. `VfxClaimPolicy`
      `LIGHTNING_EFFECT`/`FLAME_BALL`/`SHINE_LINES` append LAST to `supportedClasses()`/`supports(...)`
      in that order; `VfxLabSpawn.classNameFor` gains `"lightning"` (`new LightningEffect(960f,
      540f)`), `"flameball"` (`new FlameBallParticleEffect(960f, 540f, 0)`), and
      `"shinelines"`/`"shine"` (`new ShineLinesEffect(960f, 540f)`) — aliases checked against the
      existing set for collisions — behind the existing fail-open guard, and
      `art claim spawn lightning|flameball|shinelines 4` runs in both `d1_aura_claim.yaml` phases. No
      new patch/bridge/console wiring; default-off gate + per-instance token semantics unchanged.
      Focused no-GL JUnit only; the FLAME_BALL origin scaling is pinned at a non-1.0 `settingsScale`
      in both the pure `params` test and the end-to-end renderer test.

- [x] NRO-04 F18 (three more `vfx-scene-world` members, two reuse the additive center-packed
      branch + one ambient reuse of the region-offset-origin rule):
      `com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect` and
      `com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect` reuse the additive center-packed
      `AtlasRegion` geometry (`setBlendFunction(770, 1)` before and `(770, 771)` after) with NO new
      rule — their geometry is byte-identical to `StanceAuraEffect` and their `vY` is update-only and
      NOT consumed (they are NOT added to `requireVY`); and
      `com.megacrit.cardcrawl.vfx.scene.DustEffect` (constructed through its NO-ARG constructor)
      reuses the `FALLING_DUST` region-offset-origin rule as an ambient member — its draw origin is
      the region's own `offsetX`/`offsetY` with NO `setBlendFunction` — so it is another `case`
      label on the existing branch, not a new computation. `VfxDrawGeometry` gained
      `Kind.TORCH_PARTICLE_M`/`Kind.TORCH_PARTICLE_S` (added to the shared additive center-packed
      case list that STANCE_AURA/TORCH_PARTICLE_L/TORCH_PARTICLE_XL use) and `Kind.SCENE_DUST`
      (added to the FALLING_DUST branch); `kindFor` maps the three exact FQNs (near-miss/nested fail
      open) and `additiveBlend` is `true` for the two torch kinds and `false` for `SCENE_DUST`;
      `whiteAlphaOnly` is unchanged. All three are img-path kinds, so `Sts1VfxArtRenderer` reads
      their `rotation` field unconditionally on the existing single img draw branch (no new draw
      path) and captures the region offsets for `SCENE_DUST` with the same F15d flip-invariant
      normalization as `FALLING_DUST`. `VfxClaimPolicy` `TORCH_PARTICLE_M`/`TORCH_PARTICLE_S`/
      `SCENE_DUST` append LAST to `supportedClasses()`/`supports(...)` in that order (order appended:
      M, S, SCENE_DUST, since all three are one appended slice); `VfxLabSpawn.classNameFor` gains
      `"torchm"` (`new TorchParticleMEffect(960f, 540f)`), `"torchs"` (`new
      TorchParticleSEffect(960f, 540f)`), and `"scenedust"`/`"dusteffect"` (`new DustEffect()`, the
      NO-ARG constructor; `"dust"` was already taken by `ceilingdust`) behind the existing fail-open
      guard, and `art claim spawn torchm|torchs|scenedust 4` runs in both `d1_aura_claim.yaml`
      phases. No new patch/bridge/console wiring; default-off gate + per-instance token semantics
      unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F19 (per-instance flip flags on the bare-`Texture` shape-C path; two more members):
      the family-neutral seam's shape-C fixed-rect draw now supports per-instance `flipX`/`flipY`
      booleans, passed to the raw-texture `SpriteBatch.draw(Texture, ..., srcX, srcY, srcW, srcH,
      flipX, flipY)` overload. `VfxDrawGeometry` gains `Kind.LIGHTNING_ORB_PASSIVE` (fixed rect
      offset/origin 61, size 122, src `0,0,122,122`, additive, uses the inherited `rotation` field)
      and `Kind.GLOWY_FIRE_EYES` (fixed rect offset/origin 64, size 128, src `0,0,128,128`, additive,
      rotation hardcoded `0f` — the class has no `rotation` field) with new constants and two new
      pure predicates `usesInstanceFlipX`/`usesInstanceFlipY` (`true`/`true` for LOP; `true`/`false`
      for GFE; `false`/`false` for every other kind; throw on null like `additiveBlend`);
      `kindFor` maps the two exact FQNs (near-miss/nested fail open), `additiveBlend` is `true` for
      both, and `whiteAlphaOnly` is unchanged. `Sts1VfxArtRenderer` resolves the flip booleans
      reflectively BY KIND inside the ONE existing shape-C branch — `LIGHTNING_ORB_PASSIVE` reads its
      own `flipX`/`flipY` and requires the `rotation` field plus an instance `Texture img`;
      `GLOWY_FIRE_EYES` reads its own `flippedX` (the vertical flip is always `false`), forces
      rotation `0f`, and requires an instance `Texture img` but NOT a `rotation` field. A
      missing/unreadable flip flag defaults to `false` (flip is optional decoration, so it does NOT
      fail open); every pre-existing kind still draws with `false, false`, so no existing kind's draw
      changed. `VfxClaimPolicy` `LIGHTNING_ORB_PASSIVE`/`GLOWY_FIRE_EYES` append LAST to
      `supportedClasses()`/`supports(...)` in that order; `VfxLabSpawn.classNameFor` gains
      `"lightningorbpassive"`/`"lop"` (`new LightningOrbPassiveEffect(960f, 540f)`) and
      `"glowyfireeyes"`/`"glowyeyes"` (`new GlowyFireEyesEffect(960f, 540f)`) behind the existing
      fail-open guard, and `art claim spawn lop 4` / `art claim spawn glowyeyes 4` run in both
      `d1_aura_claim.yaml` phases. No new patch/bridge/console wiring; default-off gate + per-instance
      token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F20 (two more members: one additive center-packed reuse + one ambient center-packed
      with a new pure rule; one screened-but-deferred):
      `com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect` reuses the additive center-packed
      `AtlasRegion` geometry (`setBlendFunction(770, 1)` before and `(770, 771)` after) with NO new
      rule — its geometry is byte-identical to `StanceAuraEffect` and its `vX`/`vY` are update-only
      and NOT consumed (no addition to `requireVY`); and `com.megacrit.cardcrawl.vfx.ConeEffect`
      (constructed through its NO-ARG constructor) is an ambient center-packed `AtlasRegion` member
      with ONE new pure rule: the draw ORIGIN X is `0f` (NOT `packedWidth/2f`) while origin Y stays
      `packedHeight/2f`, and the uniform draw scale is `scale * 1.1f`
      (`CONE_SCALE_MULTIPLIER = 1.1f`), with NO `setBlendFunction`. `VfxDrawGeometry` gained
      `Kind.FLYING_SPIKE` (added to the shared additive center-packed case list that
      STANCE_AURA/TORCH_PARTICLE_L/TORCH_PARTICLE_XL use) and `Kind.CONE` (a new
      `Params(x, y, 0f, packedHeight/2f, packedWidth, packedHeight, scale*1.1f, scale*1.1f,
      rotation)` branch); `kindFor` maps the two exact FQNs (near-miss/nested fail open) and
      `additiveBlend` is `true` for `FLYING_SPIKE` and `false` for `CONE` (the ambient enumeration in
      the class/`additiveBlend`/`params` Javadocs was extended to match the `additiveBlend` body);
      `whiteAlphaOnly`/`usesInstanceFlipX`/`usesInstanceFlipY` are unchanged. Both are img-path kinds,
      so `Sts1VfxArtRenderer` reads their `rotation` field unconditionally on the existing single img
      draw branch (no new draw path) and captures no new field. `VfxClaimPolicy`
      `FLYING_SPIKE`/`CONE_EFFECT` append LAST to `supportedClasses()`/`supports(...)` in that order;
      `VfxLabSpawn.classNameFor` gains `"flyingspike"`/`"spike"` (`new FlyingSpikeEffect(960f, 540f,
      0f, 0f, 1f, Color.WHITE)`) and `"cone"` (`new ConeEffect()`, the NO-ARG constructor) — aliases
      checked against the existing set for collisions — behind the existing fail-open guard, and
      `art claim spawn flyingspike 4` / `art claim spawn cone 4` run in both `d1_aura_claim.yaml`
      phases. No new patch/bridge/console wiring; default-off gate + per-instance token semantics
      unchanged. Focused no-GL JUnit only.
      DEFERRED (SUPERSEDED by F21): `com.megacrit.cardcrawl.vfx.DamageHeartEffect` was screened but is
      NOT claimed: its
      native `render` is guarded by `if (delayTimer < 0f)`, the same wait-phase guard that defers
      `FallingIceEffect` (whose `render` is guarded by `if (waitTimer < 0f)`) — a claimed instance
      would otherwise ART-draw during the native wait phase, so a guard-rule would be needed to stay
      in parity. **F21 below adds that per-kind native draw guard and claims both FQNs**, so this
      deferral no longer holds.

- [x] NRO-04 F21 (per-kind NATIVE DRAW GUARD; two more members that need it):
      the family-neutral seam gained a per-kind NATIVE DRAW GUARD capability, modeled purely by two
      new `VfxDrawGeometry` predicates: `nativeSkipsDrawByGuard(Kind)` (`true` only for the guard
      kinds; throws on null like `additiveBlend`) and `guardFieldName(Kind)` (the host-neutral field
      name string only — `"waitTimer"` / `"delayTimer"` / `null`; throws on null). The guard is
      SATISFIED when the field is `< 0f` (native draws); an absent/unreadable guard field is treated
      as SATISFIED (draw), since native would then not be able to read a guard either.
      `Sts1VfxArtRenderer` consults this BEFORE drawing (and in `canDraw`); when the guard field is
      present and `>= 0f` the renderer returns
      `false` and the patch fails open to the native render — which also draws nothing, so the
      decline is pixel-identical and recorded as a benign no-pixel decline. The benign
      classification is INSTANCE-AWARE (not kind-only): `VfxArtRenderer.Adapter` gained
      `declinedWithoutPixels(Object effect)` (with a `public static` delegator that returns `false`
      when no adapter is installed, mirroring `canDraw`), implemented by `Sts1VfxArtRenderer` reusing
      `guardSatisfied(kind, effect)` and the same image-presence check `canDraw` uses. It is `true`
      only when THIS instance would natively draw nothing — a `nativeSkipsDrawWithoutImage` kind whose
      instance has no drawable image, OR a `nativeSkipsDrawByGuard` kind whose guard field is present
      and `>= 0f`. `NativeRenderBridge.recordEffectDeclined` now keys its benign branch on
      `VfxArtRenderer.declinedWithoutPixels(effect) && !VfxArtRenderer.canDraw(effect)` (the cheaper,
      instance-scoping probe evaluated first, to short-circuit); a decline of any other instance —
      **including a guard-SATISFIED guard kind whose image snapshot failed** — still counts as a
      `dispositionMismatch`/`delegatedWithoutEvidence`, so a genuine renderer failure is never masked
      as benign. The two newly claimed FQNs are
      `com.megacrit.cardcrawl.vfx.combat.FallingIceEffect` — additive shape-C fixed rect (instance
      `Texture img`, origin 48, size 96&times;96, src `0,0,96,96`, x/y passthrough, consuming its
      `rotation`
      field), native `render` guarded by `if (waitTimer < 0f)` — and
      `com.megacrit.cardcrawl.vfx.DamageHeartEffect` — ambient center-packed exactly like
      `StanceAuraEffect` (its `img` is a public `AtlasRegion`), native `render` guarded by
      `if (delayTimer < 0f)`. `VfxDrawGeometry` gained `Kind.FALLING_ICE` (new fixed-rect branch +
      constants; `FALLING_ICE_OFFSET` is deliberately NOT added — native passes x/y unchanged and only
      the origin is 48) and `Kind.DAMAGE_HEART` (joins the shared center-packed STANCE_AURA case list);
      `kindFor` maps the two exact FQNs (near-miss/nested fail open), `additiveBlend` is `true` for
      `FALLING_ICE`/`false` for `DAMAGE_HEART`, and `whiteAlphaOnly`/`usesInstanceFlipX`/
      `usesInstanceFlipY` are unchanged. `VfxClaimPolicy` `FALLING_ICE`/`DAMAGE_HEART` append LAST to
      `supportedClasses()`/`supports(...)` in that order. `VfxLabSpawn.classNameFor` gains
      `"fallingice"`/`"icefall"` (`new FallingIceEffect(0, false)`) and
      `"damageheart"`/`"heart"` (`new DamageHeartEffect(960f, 540f, 0f, AttackEffect.BLUNT_HEAVY, 0)`)
      — aliases checked against the existing set for collisions — behind the existing fail-open
      guard, and `art claim spawn fallingice 4` / `art claim spawn damageheart 4` run in both
      `d1_aura_claim.yaml` phases. NOTE on D1 evidence: a freshly constructed instance starts with its
      guard UNSATISFIED (`waitTimer`/`delayTimer >= 0f`), so the spawn-frame evidence is the per-FQN
      `active`/entity delta on spawn (observation), NOT a draw increase at spawn. The two kinds then
      diverge: `FallingIceEffect.update()` decrements `waitTimer` unconditionally
      (`waitTimer -= delta; if (waitTimer > 0) return;`), so its guard becomes SATISFIED once the wait
      elapses and **ART can then draw it** (its `img` is non-null in a live game), so a draw increase
      MAY also appear — whereas `DamageHeartEffect.delayTimer` is the ctor arg (`0f`) and is never
      decremented past its `> 0` branch, so it STAYS guarded (no draw increase). The two earlier
      "deferred wait-guarded" notes for these two FQNs (the F20 `DEFERRED:` paragraph and the
      `VfxClaimPolicy` class Javadoc screening note) are now
      **SUPERSEDED**. No new patch/console wiring; default-off gate + per-instance token semantics
      unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F22 (per-instance MIRROR on the img draw path; two more members + resolution of the F15
      `FlameParticleEffect` mirror limitation):
      the family-neutral seam's img (`AtlasRegion`) draw path now supports per-instance MIRROR
      booleans, resolved as a UV swap on the canonical F15d region (never a negative scale): when the
      effect mirrors X the drawn `TextureRegion`'s `u`/`u2` are swapped and when it mirrors Y the
      `v`/`v2` are swapped — visually identical to native's in-place `region.flip(...)` for a
      center-origin quad. `VfxDrawGeometry` gains `Kind.SPOOKY_CHEST`
      (`SpookyChestEffect`, AMBIENT center-packed: `additiveBlend` false) and
      `Kind.IRONCLAD_VICTORY_FLAME` (`IroncladVictoryFlameEffect`, AMBIENT center-packed), both
      joining the existing center-packed `params` branch byte-identically to `STANCE_AURA`, plus two
      new pure predicates `usesInstanceMirrorX` (`true` only for `FLAME_PARTICLE`, `SPOOKY_CHEST`,
      `IRONCLAD_VICTORY_FLAME`; throws on null) and `usesInstanceMirrorY` (`true` only for
      `SPOOKY_CHEST`; throws on null); `kindFor` maps the two exact FQNs (near-miss/nested fail open),
      and `whiteAlphaOnly`/`usesInstanceFlipX`/`usesInstanceFlipY`/`nativeSkipsDrawByGuard` are
      unchanged. `Sts1VfxArtRenderer` snapshots the mirror booleans reflectively BY KIND from
      `flipX`/`flipY` inside `readFields` (these are DISTINCT from the shape-C F19 flip flags); an
      absent/wrong-type mirror field defaults to `false` (no fail-open), and the canonical-region
      builder now takes the two flags and swaps the corresponding UV pair. Pre-existing kinds pass
      `false, false` and draw byte-identically; the shape-C (`renderTexture`) path, blend policy,
      guard logic, and params are untouched; fail-open/no-throw preserved. `VfxClaimPolicy`
      `SPOOKY_CHEST`/`IRONCLAD_VICTORY_FLAME` append LAST to `supportedClasses()`/`supports(...)` in
      that order. `VfxLabSpawn.classNameFor` gains `"spookychest"`/`"spooky"`
      (`new SpookyChestEffect()`, NO-ARG) and `"victoryflame"`/`"ironcladvictory"`
      (`new IroncladVictoryFlameEffect()`, NO-ARG) — aliases checked against the existing set for
      collisions — behind the existing fail-open guard, and `art claim spawn spookychest 4` /
      `art claim spawn victoryflame 4` run in both `d1_aura_claim.yaml` phases. The F15
      `FlameParticleEffect` `flipX` known-limitation is now **RESOLVED / SUPERSEDED** (FLAME_PARTICLE
      also uses the mirror, so its claimed pixels are mirrored like native). No new patch/bridge/
      console wiring; default-off gate + per-instance token semantics unchanged. Focused no-GL JUnit
      only.

- [x] NRO-04 F23 (three more native transient effects on the family-neutral default-off per-instance
      claim seam; NO new formula/capability):
      `SpookierChestEffect` (`vfx-scene-world`), `CampfireSleepScreenCoverEffect`
      (`vfx-campfire-rest`), and `DeathScreenFloatyEffect` (`vfx-misc-root`) — all NO-ARG constructors
      verified via `javap -c -p` — reuse the existing AMBIENT center-packed img branch
      (`sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)`, NO `setBlendFunction`) with
      no new formula; `VfxDrawGeometry` gains `Kind.SPOOKIER_CHEST`, `Kind.CAMPFIRE_SLEEP_COVER`, and
      `Kind.DEATH_SCREEN_FLOATY`, all joining the existing center-packed `params` branch
      byte-identically to `STANCE_AURA`, and `additiveBlend` is `false` for all three. `SpookierChestEffect`
      and `CampfireSleepScreenCoverEffect` reuse the F22 per-instance MIRROR (`usesInstanceMirrorX` and
      `usesInstanceMirrorY` now also report them, resolved as a UV swap on the canonical F15d region);
      `DeathScreenFloatyEffect` has no flip fields and is NOT a mirror kind. `kindFor` maps the three
      exact FQNs (near-miss/nested fail open), and
      `whiteAlphaOnly`/`usesInstanceFlipX`/`usesInstanceFlipY`/`nativeSkipsDrawByGuard` are unchanged.
      `CampfireSleepScreenCoverEffect` is the FIRST `vfx-campfire-rest` member claimed and is a
      per-instance ambient center-packed sprite with a NO-ARG constructor. The
      renderer Javadoc/kind-list is extended only (no new draw branch; the mirror is already applied via
      `usesInstanceMirrorX/Y` + `canonicalRegion`; all three require `rotation`, fail-open/no-throw
      preserved). `VfxClaimPolicy` `SPOOKIER_CHEST`/`CAMPFIRE_SLEEP_COVER`/`DEATH_SCREEN_FLOATY` append
      LAST to `supportedClasses()`/`supports(...)` in that order. `VfxLabSpawn.classNameFor` gains
      `"spookierchest"`/`"spookier"` (`new SpookierChestEffect()`), `"campfiresleepcover"`/`"sleepcover"`
      (`new CampfireSleepScreenCoverEffect()`), and `"deathfloaty"`/`"deathscreen"`
      (`new DeathScreenFloatyEffect()`) — aliases checked against the existing set for collisions —
      behind the existing fail-open guard, and `art claim spawn spookierchest 4` /
      `art claim spawn campfiresleepcover 4` / `art claim spawn deathfloaty 4` run in both
      `d1_aura_claim.yaml` phases. No new patch/bridge/console wiring; default-off gate + per-instance
      token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F24 (RNG-REPLAY + guard-threshold generalization + player-hitbox-relative x; the FIRST
      non-deterministic native effect):
      `com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle` (`vfx-stance-aura`) is the first
      claimed native effect whose `render` consumes the global RNG — the design principle is to
      REPLAY the EXACT native `MathUtils.random(...)` call sequence (same arguments, same order) so
      the global RNG stream stays identical and pixel equivalence is an IDENTITY, not a probabilistic
      match. Its native `render` is `if (delayTimer > 0f) return; setColor(color);
      setBlendFunction(770, 1); sb.draw(img, AbstractDungeon.player.hb.cX + x, y, pw/2f, ph/2f, pw,
      ph, scale * MathUtils.random(2.9f, 3.1f), scale * MathUtils.random(0.95f, 1.05f), rotation);
      setBlendFunction(770, 771);` — additive center-packed, guarded on `delayTimer`, drawing at the
      PLAYER HITBOX CENTER X plus the effect's own `x`. The seam gained three capabilities in
      `VfxDrawGeometry` (all pure/host-neutral, throw on null):
      (a) RNG-REPLAY `randomRanges(Kind)` returning an immutable ordered `List<float[]>` of
      `{min,max}` pairs in native call order — `[(2.9f,3.1f),(0.95f,1.05f)]` for `WRATH_STANCE_CHANGE`,
      empty for every other kind — documented so the renderer must call `MathUtils.random(min,max)`
      for each range IN ORDER during the draw;
      (b) guard-threshold generalization `guardBlocks(Kind, float)` encoding each guard kind's BLOCK
      condition — `FALLING_ICE`/`DAMAGE_HEART` block at `!(value < 0f)` (i.e. for NaN, `+0f`, positive,
      and `+Inf`; unblocked only for negative finite values and `-Inf`, matching native
      `if (field < 0f) draw` and the F21 behavior) while
      `WRATH_STANCE_CHANGE` blocks at `value > 0f` (NaN and `0f` do NOT block, matching native
      `if (delayTimer > 0f) return`) — with `nativeSkipsDrawByGuard`/`guardFieldName`
      extended (`guardFieldName` → `"delayTimer"` for WRATH);
      (c) player-hitbox-relative x `playerHitboxRelativeX(Kind)` true only for `WRATH_STANCE_CHANGE`.
      `Sts1VfxArtRenderer.guardSatisfied` now reads the field and delegates to `guardBlocks` (absent/
      unreadable ⇒ NOT blocked), preserving the existing FALLING_ICE/DAMAGE_HEART behavior exactly;
      the img draw path resolves the player hitbox center X (`playerHitboxCenterX`, reflective) and
      FAILS OPEN when the player/hitbox is absent (never draws at a wrong position, `canDraw`/
      `declinedWithoutPixels` agree), and pulls the RNG values only AFTER all fail-open-capable checks
      and ONLY when committed to a real draw — so the RNG is consumed EXACTLY ONCE per successful
      draw and NEVER on a fail-open return (the native fallback then consumes it). Additionally the
      renderer SNAPSHOTS the shared `MathUtils.random` (`RandomXS128`) state before pulling the values
      and RESTORES it if any host call throws after consumption, so a post-consumption fail-open also
      leaves the global stream untouched (the native fallback then consumes exactly the values it
      needs). For kinds with an
      empty range list scaleX/scaleY stay `scale, scale` (unchanged). `kindFor` maps the exact FQN
      (near-miss/nested fail open), `additiveBlend` is true, and `whiteAlphaOnly`/`usesInstanceFlipX`/
      `usesInstanceFlipY`/`usesInstanceMirrorX`/`usesInstanceMirrorY`/`nativeSkipsDrawWithoutImage`
      are unchanged. `VfxClaimPolicy.WRATH_STANCE_CHANGE` appends LAST to `supportedClasses()`/
      `supports(...)`. `VfxLabSpawn.classNameFor` gains `"wrathchange"`/`"wrathstance"` (no collision
      with the pre-existing `"wrath"` alias; `"wrathstance"` itself is NEW) →
      `new WrathStanceChangeParticle(0f)` behind the
      existing fail-open guard, and `art claim spawn wrathchange 4` runs in both `d1_aura_claim.yaml`
      phases. `WrathStanceChangeParticle`'s constructor IGNORES its float argument and sets
      `delayTimer = MathUtils.random(0f, 0.5f)` (construction-time RNG, NOT replayed by the seam), so
      the lab entry `new WrathStanceChangeParticle(0f)` yields a random 0–0.5 s wait, after which the
      guard may clear and ART may draw. `StanceChangeAbsorptionParticle` was **DEFERRED** at F24 (two
      draws + four RNG calls, so it
      needed a multi-draw capability); that deferral is now **SUPERSEDED by F25 below**, which claims
      it. D1 evidence boundary (honest): the read-only probe cannot observe
      the RNG-derived scale, so D1 covers claim/draw/gate behavior while the exact RNG call order and
      ranges are covered by unit tests (a mirror-RNG assertion). No new patch/bridge/console wiring;
      default-off gate + per-instance token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F25 (MULTI-DRAW shape-C RNG-replay capability; `StanceChangeAbsorptionParticle`, the
      last `vfx-stance-aura` non-deterministic effect):
      `com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle` (`vfx-stance-aura`; private
      `oX, oY, x, y, aV, distOffset, scaleOffset` plus the inherited `scale`/`rotation`/`color`; ctor
      `(Color, float, float)`; `render(sb)` draws the SAME static `ImageMaster.WOBBLY_ORB_VFX`
      `Texture` TWICE, additive, both with offset `x-16f, y-16f`, origin `16f,16f`, size `32f,32f`,
      src `0,0,32,32` and rotation `rotation - 200f`; `update()` consumes NO RNG). The seam gained a
      MULTI-DRAW capability in `VfxDrawGeometry` (pure/host-neutral, throw on null):
      `drawPassRandomRanges(Kind)` returns the ordered per-pass RNG ranges — for
      `STANCE_CHANGE_ABSORPTION` `[[(0.5f,2.0f),(0.5f,2.0f)],[(0.6f,2.5f),(0.6f,2.5f)]]`, an EMPTY
      outer list for every other kind — documented so the renderer must, per pass IN ORDER, call
      `MathUtils.random(min,max)` once per inner range IN ORDER (multiplying scaleX then scaleY), so
      replaying the exact native call sequence keeps the global RNG stream identical and pixel
      equivalence is an IDENTITY. `Kind.STANCE_CHANGE_ABSORPTION` gets its OWN `params` branch —
      `Params(x - 16f, y - 16f, 16f, 16f, 32f, 32f, scale, scale, rotation - 200f)` with named
      constants (ADDITIVE, NOT center-packed, NOT a region-offset kind); the RNG-derived per-pass
      scales are applied by the RENDERER, so `params` returns the base `scale, scale`. The existing
      single-draw `randomRanges(Kind)` (WRATH) is unchanged; `whiteAlphaOnly`/`usesInstanceFlipX/Y`/
      `usesInstanceMirrorX/Y`/`nativeSkipsDrawByGuard`/`guardBlocks`/`playerHitboxRelativeX` are
      unchanged, and `kindFor` maps the exact FQN (near-miss/nested fail open).
      `Sts1VfxArtRenderer` routes `STANCE_CHANGE_ABSORPTION` through the shape-C (`renderTexture`)
      path, resolving the STATIC `ImageMaster.WOBBLY_ORB_VFX` `Texture` (no instance `img`), requiring
      the `rotation` field, using its own src rect `0,0,32,32` and fixed origin/size (guard/flip/mirror
      predicates do not apply). `renderTexture` snapshots the shared RNG state ONCE before ANY pass,
      then per pass pulls the ranges in order and draws the texture with those scales; on ANY throw
      during ANY pass it RESTORES the snapshot before returning `false` (a fail-open leaves the stream
      untouched and native re-consumes), and the success path does not restore. Single-draw kinds
      (empty outer list) keep the existing behavior (no RNG unless WRATH on the img path).
      `VfxClaimPolicy.STANCE_CHANGE_ABSORPTION` appends LAST to `supportedClasses()`/`supports(...)`.
      `VfxLabSpawn.classNameFor` gains `"absorption"`/`"absorb"` (no collision with the existing
      aliases) → `new StanceChangeAbsorptionParticle(Color.WHITE, 960f, 540f)` behind the existing
      fail-open guard, and `art claim spawn absorption 4` runs in both `d1_aura_claim.yaml` phases.
      D1 evidence boundary stays as in F24 (the read-only probe cannot observe the RNG-derived scales;
      unit tests pin the exact four-call order/ranges and the post-consumption RNG restore). With F24
      (Wrath) + F25 (Absorption), the `vfx-stance-aura` family's deterministic AND non-deterministic
      paths are BOTH covered. No new patch/bridge/console wiring; default-off gate + per-instance token
      semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F27 (two more native transient effects on the family-neutral default-off per-instance
      claim seam; each adds ONE small pure rule with no new renderer branch):
      `com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect` (`vfx-combat`; private `img`
      (`AtlasRegion`), `x`, `y`, `vX`, `vY`, `floor`, inherited `scale`/`rotation`/`color`; ctor
      `(float, float)`) and `com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect` (`vfx-combat`;
      private `img` (`AtlasRegion`), `x`, `y`, `vY`, its OWN `scale` field, inherited
      `rotation`/`color`; ctor `(float, float)`). `WaterSplashParticleEffect.render` is
      `setColor(color); sb.draw(img, x, y, packedWidth/2f, packedHeight/2f, packedWidth,
      packedHeight, scale, scale * 0.54f, rotation)` with NO `setBlendFunction` — i.e. AMBIENT
      center-packed with a NEW pure ANISOTROPIC-scale rule (`scaleY = scale * 0.54f`, `scaleX =
      scale`). `BuffParticleEffect.render` is `setBlendFunction(770, 1); setColor(color);
      sb.draw(img, x - packedWidth/2f, y - packedHeight/2f, img.offsetX, img.offsetY, packedWidth,
      packedHeight, scale, scale, rotation); setBlendFunction(770, 771)` — i.e. ADDITIVE with a NEW
      pure POSITION/ORIGIN rule (the draw position is offset by half the packed footprint, and the
      ORIGIN is the region's own `(offsetX, offsetY)` rather than `packed/2`). Both consume their
      inherited `rotation` field. Implementation: `VfxClaimPolicy.WATER_SPLASH`/`BUFF_PARTICLE`
      append LAST to `supportedClasses()`/`supports(...)`. `VfxDrawGeometry` gained
      `Kind.WATER_SPLASH`/`Kind.BUFF_PARTICLE`; `params(...)` gained a trailing
      `scaleYMultiplier` scalar (defaults `1f` for every pre-existing kind — the renderer passes
      `1f` for all kinds except `WATER_SPLASH`, which passes the named constant
      `WATER_SPLASH_SCALE_Y_MULTIPLIER = 0.54f` — so pre-existing results are byte-identical), the
      shared center-packed branch now computes `scaleY = scale * scaleYMultiplier` (`scaleX` stays
      `scale`), and `BUFF_PARTICLE` gets its OWN `Params(x - packedWidth/2f, y - packedHeight/2f,
      regionOffsetX, regionOffsetY, packedWidth, packedHeight, scale, scale, rotation)` branch.
      `kindFor` maps the exact FQNs (near-miss/nested fail open); `additiveBlend` is `false` for
      `WATER_SPLASH` and `true` for `BUFF_PARTICLE`; `whiteAlphaOnly`/flip/mirror/guard/`randomRanges`/
      `drawPassRandomRanges`/`playerHitboxRelativeX` are unchanged. `Sts1VfxArtRenderer` routes both
      through the existing img (`AtlasRegion`) path, requiring `rotation` and passing the new
      `scaleYMultiplier` (`0.54f` only for `WATER_SPLASH`; `BUFF_PARTICLE` reuses the already-captured
      region offsets, like `FALLING_DUST`/`SCENE_DUST`) — no new draw branch, fail-open/no-throw
      preserved. `VfxLabSpawn.classNameFor` gains `"watersplash"`/`"splash"` →
      `new WaterSplashParticleEffect(960f, 540f)` and `"buffparticle"`/`"buffp"` →
      `new BuffParticleEffect(960f, 540f)` (no alias collision) behind the existing fail-open guard,
      and `art claim spawn watersplash 4` / `art claim spawn buffparticle 4` run in both
      `d1_aura_claim.yaml` phases. No new patch/bridge/console wiring; default-off gate + per-instance
      token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F28 (two more native transient effects on the family-neutral default-off per-instance
      claim seam; one reuses an existing shape with NO new rule, one adds ONE small pure rule):
      `com.megacrit.cardcrawl.vfx.scene.BottomFogEffect` (`vfx-scene-world`; fields `AtlasRegion img`,
      `float x, y, vX, aV` + `boolean flipX`, `boolean flipY`; inherited `scale`/`rotation`/`color`;
      ctor `(boolean)`) and `com.megacrit.cardcrawl.vfx.combat.GiantFireEffect` (`vfx-combat`; fields
      `AtlasRegion img`, `float x, y, vX, vY, brightness, startingDuration, delayTimer` + `boolean
      flipX`; inherited `rotation`/`color`; ctor `()`). `BottomFogEffect.render` is
      `setColor(color); [in-place flipX/flipY mirror]; sb.draw(img, x, y, packedWidth/2f,
      packedHeight/2f, packedWidth, packedHeight, scale, scale, rotation)` with NO `setBlendFunction` —
      i.e. AMBIENT center-packed with per-instance horizontal+vertical mirror, IDENTICAL in shape to
      `SpookierChestEffect`/`CampfireSleepScreenCoverEffect`, so it adds NO new rule (it reuses the
      F22 mirror). `GiantFireEffect.render` is `setColor(color); setBlendFunction(770, 1); [in-place
      flipX mirror]; sb.draw(img, x, y, packedWidth/2f, packedHeight/2f, packedWidth, packedHeight,
      scale * Settings.scale, scale * Settings.scale, rotation); setBlendFunction(770, 771)` — i.e.
      ADDITIVE center-packed with per-instance horizontal flipX mirror (no flipY) and ONE new pure
      rule: a UNIFORM scale multiplier of `Settings.scale` on BOTH axes (`delayTimer` is used only by
      `update()`, NOT by `render`, so there is no render guard). Implementation:
      `VfxClaimPolicy.BOTTOM_FOG`/`GIANT_FIRE` append LAST to `supportedClasses()`/`supports(...)` in
      that order. `VfxDrawGeometry` gained `Kind.BOTTOM_FOG`/`Kind.GIANT_FIRE`; `BOTTOM_FOG` joins the
      shared ambient center-packed branch (same params shape as `STANCE_AURA`, `additiveBlend` false),
      and `GIANT_FIRE` joins that branch additively with the new pure
      `uniformScaleMultiplier(Kind, float settingsScale)` returning `settingsScale` for `GIANT_FIRE`
      and `1f` for every other kind (throws on null kind). The center-packed `params(...)` now computes
      `scaleX = scale * uniformScaleMultiplier(kind, settingsScale)` and
      `scaleY = scale * uniformScaleMultiplier(kind, settingsScale) * scaleYMultiplier`, so
      `GIANT_FIRE` gets `scale * Settings.scale` on both axes (its `scaleYMultiplier` tail is `1f`)
      while `WATER_SPLASH` keeps its F27 `* 0.54f` (its uniform multiplier is `1f`) and every
      pre-existing kind is byte-identical. `usesInstanceMirrorX` now also includes `BOTTOM_FOG` and
      `GIANT_FIRE`, and `usesInstanceMirrorY` now also includes `BOTTOM_FOG` (NOT `GIANT_FIRE`).
      `kindFor` maps the exact FQNs (near-miss/nested fail open); `whiteAlphaOnly`/`randomRanges`/
      `drawPassRandomRanges`/`playerHitboxRelativeX`/guard predicates unchanged. `Sts1VfxArtRenderer`
      routes both through the existing img (`AtlasRegion`) path, both requiring `rotation`; the
      center-packed draw passes `uniformScaleMultiplier(kind, Settings.scale)` alongside the existing
      `scaleYMultiplier(kind)`, and the mirror is applied via the existing `canonicalRegion` UV swap —
      no new draw branch, fail-open/no-throw preserved. `VfxLabSpawn.classNameFor` gains `"bottomfog"`/
      `"bfog"` → `new BottomFogEffect(false)` (boolean ctor, picks `false`; NO alias collision) and
      `"giantfire"`/`"gfire"` → `new GiantFireEffect()` (NO-ARG) behind the existing fail-open guard,
      and `art claim spawn bottomfog 4` / `art claim spawn giantfire 4` run in both
      `d1_aura_claim.yaml` phases. No new patch/bridge/console wiring; default-off gate + per-instance
      token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F29 (one more native transient effect on the family-neutral default-off per-instance
      claim seam; reuses an existing shape with ONE small pure rule):
      `com.megacrit.cardcrawl.vfx.TorchHeadFireEffect` (`vfx-misc-root`; fields `Texture img`,
      `float x, y, vX, vY` + `boolean flippedX`; inherited `scale`/`color`; ctor `(float, float)`).
      Its native `render` is `setBlendFunction(770, 1); setColor(color); sb.draw(img, x - 64f,
      y - 64f, 64f, 64f, 128f, 128f, scale * 1.2f, scale, 0f, 0, 0, 128, 128, flippedX, false);
      setBlendFunction(770, 771)` — i.e. **shape-C** (instance `Texture` + fixed rect: offset 64,
      origin 64, size 128&times;128, src `0,0,128,128`; rotation HARDCODED `0f`, no rotation field;
      ADDITIVE) reusing the `GlowyFireEyesEffect` rect EXACTLY, with the effect's own `flippedX`
      horizontal flip (vertical always `false`) and ONE new pure rule: an ASYMMETRIC X scale
      (`scaleX = scale * 1.2f`, `scaleY = scale`). Implementation: `VfxClaimPolicy.TORCH_HEAD_FIRE`
      appends LAST to `supportedClasses()`/`supports(...)`. `VfxDrawGeometry` gained
      `Kind.TORCH_HEAD_FIRE` with its OWN shape-C `params` branch reusing the shared
      `GLOWY_FIRE_EYES` rect constants and the new
      `TORCH_HEAD_FIRE_SCALE_X_MULTIPLIER = 1.2f`, plus a NEW pure predicate
      `usesTexturedFlipX(Kind)` (true for `GLOWY_FIRE_EYES` AND `TORCH_HEAD_FIRE`, false otherwise,
      throws on null) generalizing the renderer's previous `GLOWY_FIRE_EYES`-only `flippedX`
      special-case. `kindFor` maps the exact FQN (near-miss/nested fail open); `additiveBlend` is
      `true` for `TORCH_HEAD_FIRE`; `whiteAlphaOnly`/`uniformScaleMultiplier`/`scaleYMultiplier`/
      guard/`randomRanges`/`drawPassRandomRanges`/`playerHitboxRelativeX`/mirror predicates are
      unchanged (it is NOT an img-mirror kind — it is shape-C). `Sts1VfxArtRenderer` routes it through
      the existing `renderTexture` shape-C path, resolves its instance `Texture img`, does NOT require
      `rotation` (excluded from `requireRotation`, like `GLOWY_FIRE_EYES`), and resolves the flip field
      `flippedX` via `usesTexturedFlipX` — no new draw branch, fail-open/no-throw preserved.
      `VfxLabSpawn.classNameFor` gains `"torchheadfire"`/`"torchhead"` →
      `new TorchHeadFireEffect(960f, 540f)` (no alias collision with `torch`/`torchxl`/`torchm`/
      `torchs`) behind the existing fail-open guard, and `art claim spawn torchheadfire 4` runs in both
      `d1_aura_claim.yaml` phases. No new patch/bridge/console wiring; default-off gate + per-instance
      token semantics unchanged. Focused no-GL JUnit only.

- [x] NRO-04 F30 (one more native transient effect on the family-neutral default-off per-instance
      claim seam; the img path with ONE small pure rule):
      `com.megacrit.cardcrawl.vfx.CardTrailEffect` (`vfx-misc-root`; fields a STATIC shared
      `private static TextureAtlas.AtlasRegion img` assigned in the ctor from
      `ImageMaster.vfxAtlas.findRegion(...)`, `float x, y, scale`; inherited `color`; it does NOT
      redeclare a `rotation` field but inherits `AbstractGameEffect.rotation`; NO-ARG ctor). Its
      native `render` is `setBlendFunction(770, 1); setColor(color);
      sb.draw(img, x, y, 6f, 6f, 12f, 12f, scale, scale, 0f); setBlendFunction(770, 771)` — the img
      (`AtlasRegion`) path with ONE new pure rule: a fixed ORIGIN `(6f, 6f)` and a fixed SIZE
      `(12f, 12f)` INDEPENDENT of the region's packed size, with rotation hardcoded `0f` (the class's
      native draw ignores the inherited `rotation` field and forces `0f`); ADDITIVE. Implementation:
      `VfxClaimPolicy.CARD_TRAIL` appends LAST to
      `supportedClasses()`/`supports(...)`. `VfxDrawGeometry` gained `Kind.CARD_TRAIL` with its OWN
      `params` branch returning `Params(x, y, 6f, 6f, 12f, 12f, scale, scale, 0f)` via the named
      constants `CARD_TRAIL_ORIGIN = 6f`/`CARD_TRAIL_SIZE = 12f`; `additiveBlend` is `true` for it;
      `kindFor` maps the exact FQN (near-miss/nested fail open); every other predicate is unchanged
      (it uses none of them). `Sts1VfxArtRenderer` routes it through the existing img (`AtlasRegion`)
      path, resolves its STATIC `img` (like `ExhaustPileParticle`), and does NOT require a `rotation`
      field (kept out of the img reader's rotation requirement only DEFENSIVELY — the exemption is
      behaviorally inert because the inherited field is always present — while the claimed draw forces
      rotation `0`) — no new
      draw branch; fail-open/no-throw preserved. `VfxLabSpawn.classNameFor` gains
      `"cardtrail"`/`"trail"` → `new CardTrailEffect()` (NO-ARG; no alias collision) behind the
      existing fail-open guard, and `art claim spawn cardtrail 4` runs in both `d1_aura_claim.yaml`
      phases. No new patch/bridge/console wiring; default-off gate + per-instance token semantics
      unchanged. Focused no-GL JUnit only.

      F30b (lab-path defect caught by D1, NOT a renderer defect): `CardTrailEffect` is a pooled
      `Pool.Poolable` whose `color`/`x`/`y`/`scale`/`duration` are set by `init(x, y)` (which reads
      `AbstractDungeon.player.getCardTrailColor()` and applies the fixed `-6f` offsets), not by its
      no-arg constructor, so a lab spawn that only constructed the effect left the inherited `color`
      null → `Sts1VfxArtRenderer.readFields` declined (`color` required) and native also NPE'd on
      `setColor(null)`, breaking the D1 scenario (`nativeRenderStrict.accepted=false`, `dewe=4`).
      `VfxLabSpawn` now calls `effect.init(960f, 540f)` after construction (inside the existing
      fail-open `try/catch(Throwable)` guard, since `init` may read a null player off-game). The
      renderer/geometry/policy/F30 kind are unchanged; a focused no-GL test proves the lab
      `construct` path invokes `init`.

- [x] NRO-04 A01 (make the runtime-initialized/pooled native-effect init contract explicit and
      tested; F30/F30b hardening, no behavior change): new pure host-neutral
      `artframework.sts1.render.VfxInitContract` (no libGDX/STS imports beyond
      `VfxDrawGeometry.Kind`) exposes `requiresRuntimeInit(kind)` (true iff the native effect is
      pooled and its draw fields are only set by a post-constructor initializer — today exactly
      `Kind.CARD_TRAIL`, `null`/all else false), `initializerMethod(kind)` (`"init"` for
      `CARD_TRAIL`, null otherwise), and `initializerArgs(kind)` (`{960f, 540f}` for `CARD_TRAIL`,
      null otherwise; a FRESH array each call). Whole-repo audit result: among the 60 claimable
      FQNs ONLY `com.megacrit.cardcrawl.vfx.CardTrailEffect` is a `com.badlogic.gdx.utils.Pool.
      Poolable` (verified via `javap` on `$ART_STS_JAR`); every other claimable class is fully
      initialized by its constructor. `VfxLabSpawn.construct`'s `CARD_TRAIL` branch now derives the
      initializer name AND coordinates from the contract (still a direct `effect.init(a, b)` call
      with the existing fail-open comment and try/catch; no reflective invocation) and DECLINES the
      uninitialized instance (returns null → fail-open) if the contract no longer claims a
      post-constructor initializer. New pure no-GL `VfxInitContractTest` covers the
      representative set, a CROSS-SOURCE audit that iterates `VfxClaimPolicy.supportedClasses()`,
      loads each claimable class WITHOUT initializing it (`Class.forName(fqn, false, loader)`),
      computes `Pool.Poolable.class.isAssignableFrom(cls)`, asserts the pooled-claimable set EQUALS
      exactly `{VfxClaimPolicy.CARD_TRAIL}`, and then checks the contract bidirectionally for every
      claimed FQN (`requiresRuntimeInit(kindFor(fqn)) == poolableClaimed.contains(fqn)`) — so a new
      pooled claimable added to the policy without updating the contract FAILS the build, and a
      stale contract entry fails too; plus fresh-array semantics and a reflection contract-vs-native
      check (`CardTrailEffect` implements `Pool.Poolable` and declares a public `init(float,float)`).
      `VfxLabSpawnTest` gains a reflection test that ties the contract's initializer name to the real
      native `CardTrailEffect.init(float,float)` method and resolves the lab wiring chain
      (`classNameFor("cardtrail")`/`"trail"` → `kindFor` → `Kind.CARD_TRAIL` →
      `requiresRuntimeInit` == true). No renderer, geometry formula, policy, claim, bridge/patch,
      console, or other lab-entry behavior changed.

- [x] NRM-12 Transient-effect memory bound (P0, STS1): `AbstractGameEffect.update()` is
      non-abstract and most concrete native effects override it without calling `super.update()`,
      so the class-level Postfix in `TransientEffectRenderPatches` only fires for the few that do.
      Torch-lit rooms emit particles continuously, so the per-instance records in
      `TransientEffectLedger.records` and `TransientEffectRegistry.entities` grew for the process
      lifetime and ended in `OutOfMemoryError: Java heap space` on D1. **The fix is the capacity
      bounds, not the abandoned update instrument:** `TransientEffectLedger` gained an active-record
      capacity (default 4096, constructor-overridable; `DEFAULT_ACTIVE_CAPACITY`) that DROPS the
      oldest active record from tracking once the cap is exceeded — it does NOT enter `recent` or
      `staleIdentities`, which stay terminal-only, so a still-live cap-evicted instance is simply
      re-admitted as active on its next observation instead of being rejected as terminal; the
      `recent`/`staleIdentities` windows remain bounded (default 256). `TransientEffectRegistry`
      gained an equivalent entity cap (`DEFAULT_ENTITY_CAPACITY`, 4096) that evicts the oldest
      projected ids and queues their presentation-entity removal. Active cap evictions and
      recent-window evictions are counted separately: `evicted` still means the recent window, while
      `activeEvicted` counts active-cap evictions. Observation via the retained `update()` Postfix is
      atomic and idempotent: `TransientEffectLedger.updateIfActive(identity, done)` checks and
      updates under a single lock, so a repeated `done=true` after `COMPLETED` or `DISPOSED` is a
      silent no-op and the `"update after effect termination"` invariant still throws for a direct
      `update(..., false)`. The probe only adds keys
      (`transientEffects.activeCap`, `transientEffects.activeEvicted`,
      `transientEffectEntityCap`); existing `active`/`recent`/`total`/`evicted`/`leaked` keys keep
      their meaning, and no claim/draw/geometry/policy/render behavior changed.

      A container `AbstractDungeon.update()` instrument (`TransientEffectContainerPatches
      .observeThenUpdate`) was implemented to force completion observation for effects that skip
      `super.update()`, but D1 showed it was **worse than the memory it bounded and was REMOVED**:
      live effects were rejected as terminal every frame — `rejectedTerminal` (== `unknownLifecycle`)
      grew ~1 per observed render (~5k/s), `rendered`/`total` froze, and `nativeRenderStrict.accepted`
      stayed `false` (strict requires `transientEffectUNKNOWN == 0`). That failure was a LIFECYCLE
      mistake (the old instrument re-applied/called `update()` and mis-observed), not a defect of
      observing completion at the container; it also predated the NRM-13 non-reusable identity fix.

- [x] NRM-12b / NRO-04 A04 (container AFTER-update DETACH observation, superless effects):
      the completion-observation gap for effects whose `update()` does NOT call `super.update()` is
      handled by a container-level instrument. `TransientEffectContainerPatches
      .ObserveContainerEffectUpdates` instruments `AbstractDungeon.update()` and, for every
      `AbstractGameEffect.update()` call site, emits `$proceed($$)` followed by
      `observeAfterUpdate($0)` — so the native update runs exactly as before and only the
      POST-update `isDone` is observed. `observeAfterUpdate` delegates to
      `NativeRenderBridge.observeEffectUpdateDetached`, which DETACHES: when the effect reports
      `isDone` it drops the active record via `TransientEffectLedger.detach(identity)` WITHOUT
      retaining a terminal record, then cleans up the projected entity and runs the projection tail.
      Detach does not touch `recent`/`staleIdentities`/`total`/`completed`; it never throws, and
      honors the panic fail-open. It does NOT re-apply or duplicate the native update and does NOT
      change identity/admission.

      **Why detach and not complete (the D1 rework):** container `isDone` is NOT a reliable
      end-of-life signal. D1 reproduced the regression with the gate OFF: `observeAfterUpdate`
      completed the record (`updateIfActive` → `complete` → `retainRecent`), but the object was
      STILL rendered by native `AbstractDungeon.render` (not yet removed, or re-added), so each
      subsequent render hit `admitRender`'s terminal branch and incremented `rejectedTerminal`/
      `unknownLifecycle` (~3800/s), froze `total`/`completed` at the 256 `recent` window, and made
      `nativeRenderStrict.accepted` permanently false. The retained-terminal approach is therefore
      wrong for the container path; detaching lets a later render of the same object be re-admitted
      as a fresh active record instead of rejected. This is also why the earlier removed attempt
      failed. The `Pool.Poolable` exclusion did not help because the rejecting objects are
      non-pooled.

      **Class-level Postfix unchanged:** `observeEffectUpdate` (used only by the
      `AbstractGameEffect.update()` Postfix in `TransientEffectRenderPatches` for subclasses that
      call `super.update()`) still COMPLETES and retains a terminal record; that is safe because a
      super-calling effect that reports done is treated as genuinely finished.

      **Pooled exclusion retained:** `com.badlogic.gdx.utils.Pool.Poolable` effects are excluded
      from BOTH the complete and the detach paths (early return before `effectIdentity`): a pooled
      object is recycled (re-`init`ed and rendered again), so it must simply stay active and be
      re-admitted on reuse (e.g. `CardTrailEffect`). Memory stays bounded by the detach/drop plus the
      active/recent capacities as defense-in-depth. Only `detach` was ADDED to the ledger; no
      `admitRender`/`complete`/`dispose`/`update`/`updateIfActive` semantics or identity code
      changed.

      **A04c D1 native flakiness observation (not attributed to this slice):** during a first soak
      burst, `java.lang.NullPointerException` occurred once at
      `com.megacrit.cardcrawl.dungeons.AbstractDungeon.render(AbstractDungeon.java:2673)` (a null
      element in `effectList` during the native render traversal); at the crash all transient ledger
      counters were 0 (no `rejectedTerminal`/`unknownLifecycle`/`leaked`). The game was recovered and
      a full 8-burst retry soak plus the 148-step `d1_aura_claim` scenario ran clean, so the crash did
      not reproduce. The same `AbstractDungeon.render` NPE signature also appears in historical
      (2026-08-05) harness artifacts unrelated to this slice, so it is recorded as host-side/native
      flakiness and NOT attributed to A04/A04c.


- [x] NRM-13 Transient-effect identity (P0 follow-up, STS1): the original `instanceId` was
      `class@Integer.toHexString(System.identityHashCode(effect))`, and `identityHashCode` values are
      reused once an object dies; after completion observation began, `TransientEffectLedger.recent`/
      `staleIdentities` filled with ids of dead effects, so a NEW live effect that reused a freed
      identity hash produced the SAME `instanceId` and was rejected as terminal by `admitRender`.
      `NativeRenderBridge` now assigns each live effect a stable, non-reusable id
      `class#<monotonic-hex-seq>` from a `private static IdentityHashMap<AbstractGameEffect,
      EffectIdState>` with an insertion-order `TreeMap` companion (`EFFECT_IDS_CAPACITY` 8192,
      oldest-eviction). `TransientEffectLedger.sameIdentity` now compares exactly `instanceId` +
      `nativeClass` + `generation`; because `instanceId` is a non-reusable per-object id, an equal
      `instanceId` already implies the same object, so the diagnostic `nativeIdentityHash` is no
      longer compared. Ids are deliberately sticky rather than released on completion (the map is
      bounded by cap eviction); `resetForTests` clears the map. The ledger adds an additive
      `rejectedTerminal` probe counter distinguishing genuine terminal re-observations from any
      future id-reuse regression. The identity change is a strict improvement independent of the
      removed container instrument.

- [x] NRM-14 Probe payload / logging allocation under heavy transient-effect load: the
      transient-effect capacity bounds (NRM-12/13) kept the ledgers bounded, but under a sustained
      heavy claim-spawn soak plus frequent large `art probe` reads the D1 JVM still hit
      `java.lang.OutOfMemoryError: Java heap space` (512 MB heap) and froze the render loop: the
      single `ART_PROBE` line grew to ~815 KB on `render.targets`/`targetsById` and `latest.log`
      reached ~40 MB. Fix: the probe payload is now bounded on BOTH axes — per-instance
      transient-effect render targets (`render.targets`/`render.targetsById`/`render.renderOrder.items`)
      and per-scope `presentation[].entities` are each capped at 64 (overridable via
      `RenderHost.setMaxProbeEffectTargets` / `PresentationRegistry.setMaxProbeEffectEntities`),
      while stable/named entries are always enumerated in full. Additive keys report the bound:
      `targetsTotal`/`targetsIncluded`/`targetsTruncated` (+ `renderOrder.total`/`included`;
      `renderOrder.count` remains the FULL ordered total) and per-scope
      `entitiesTotal`/`entitiesIncluded`/`entitiesTruncated`. `renderOrder.monotonic`/
      `duplicateStableKeys` are still computed over ALL targets. The render-target predicate covers
      both `native:effect:` and the `native:effect_` fallback. D1 evidence: a heavy claim-spawn soak
      grew the true live/target count 151 → 3,219 (~21×) while the `ART_PROBE` line stayed flat at
      ~136 KB (unbounded it would have been ~4.9 MB); per-scope presentation and render target lists
      stayed at 64 + stable with truncation flags set and totals reporting the true counts; `grep`
      for `OutOfMemoryError`/`FATAL EXCEPTION` = 0; Java heap flat (~12.8 MB); 20 rapid back-to-back
      `art probe` reads all returned fresh data with the render loop alive. Residual / known: the
      previously-documented NRM-12 observation gap remains (effects whose `update()` does not call
      `super.update()` are not observed as complete, so `transientEffects.active` can sit at the 4096
      cap after a heavy soak) — the count and hence the payload stay hard-bounded regardless; and
      per-probe `renderOrder` allocation is still O(N) in live targets even though the emitted
      payload is bounded.

- [x] NRO-04 crash fix (retire-by-flag instead of structural removal): `art claim clear`
      (legacy alias `art aura clear`) could crash the game with a render-thread
      `ConcurrentModificationException` because the lab helper structurally removed matched effects
      from the live `AbstractDungeon` effect lists (`effectsQueue`, `effectList`,
      `topLevelEffectsQueue`, `topLevelEffects`) via `iterator.remove()` on the console thread while
      the render thread iterated those same `ArrayList`s. `VfxLabSpawn.Queue` now exposes
      `retireMatching(Predicate)` (renamed from `removeMatching`) and `DungeonQueue` sets
      `effect.isDone = true` for matching effects — the game-native reap path in
      `AbstractDungeon.update()` (which drops effects whose public `isDone` field is set) — so no
      `modCount` check can be tripped from another thread. The per-container fail-open guard, the
      `VfxClaimPolicy.supports(...)` predicate, the retired count, the `spawn` behavior/aliases, and
      the clearable FQN set are all unchanged; `clear()` still never throws.

- [x] NRO-04 A02 (parameterized single-class VFX claim isolation device scenario): new
      `tests/ui-scenarios/device/d1_vfx_claim_isolation.yaml` verifies the VFX claim seam for ONE
      reusable target kind per run instead of relying only on family-wide draw counters. A leading
      `set: {target_kind, unsupported_kind}` parameterizes the scenario; retarget by editing
      `target_kind` (e.g. `cardtrail` → `torchheadfire`/`absorption`) — all `${...}` references
      resolve from those vars. The scenario asserts three legs: (1) GATE-OFF control — a supported
      target spawned with the seam OFF adds ZERO ART `aura.draws` (`eq_var` baseline) and `gate` stays
      false; (2) GATE-ON target isolation — the same target grows ART draws (`gt_var` baseline, the
      real drawing proof) while `declinedTotal`, `dispositionMismatch`, and `orphanArtOutput` stay
      flat (`eq_var` captured baselines, proving the target is claimed, not declined/leaked) and
      `nativeRenderStrict.accepted` stays true; (3) UNSUPPORTED-kind usage no-op control (gate still
      ON) — an unknown alias maps to no FQN at the command layer (`ArtCommand.parseClaim` →
      `ClaimRequest.invalid()`), so nothing is spawned and no `ART_COMMAND` result is emitted, yet
      `art claim spawn <kind>` SUCCEEDS (a command-layer usage no-op, not a renderer fail-open path);
      the declined/mismatch/orphan ledgers stay flat (`eq_var` baselines). Draw and
      `transientEffectEntities` deltas are deliberately NOT asserted for the unsupported leg because
      `aura.draws` grows continuously in live combat (and ambient entities fluctuate); the leg is
      verified by the flat ledger counters only; (4) FINISH — clear then gate off.
      Supporting runner changes (Python only, backward compatible): a pure
      `interpolate(value, vars_map)` helper doing `${name}` substitution (a missing token raises a
      `ValueError` naming the var; non-strings, and strings without a well-formed token, are
      unchanged) applied to `console:`/`op:` command values and `assert:`/`capture:`/
      `wait_probe.assert` `path` values. New offline tests in
      `tools/art-verify/tests/test_interpolate.py`. (An earlier `expect_error: true` co-key was
      REMOVED: the real unknown-kind path is a command-layer usage no-op that returns success, so it
      can never satisfy an expected-error step.)

- [x] NRO-04 A03 (repeatable native-vs-ART visual-sampling scenario): new
      `tests/ui-scenarios/device/d1_vfx_parity_capture.yaml` captures a NATIVE (claim-seam OFF)
      frame and an ART (claim-seam ON) frame of the SAME spawned claimable kind at one encounter
      state, records `probe: all` metadata around each frame, then REPEATS the gate-ON sample once
      more for counter-level repeatability. A leading `set: {target_kind, spawn_count}`
      parameterizes it; retarget by editing those vars (`cardtrail`/`torchheadfire`/`absorption`,
      any count). Gated invariants are robust counters only: native leg has `gate eq false` plus a
      real fore/aft `eq_var` zero-ART-draws check (native `aura.draws` is lifetime-stable); each
      gate-ON sample proves draw GROWTH (`gt_var` on its own cleared baseline) with
      `declinedTotal`/`dispositionMismatch`/`orphanArtOutput` FLAT (`eq_var` before/after);
      `nativeRenderStrict.accepted eq true`; final `claim clear` + `claim off` leaves `gate false`.
      PIXEL PARITY IS DELIBERATELY NOT GATED: the live combat scene is not frame-frozen (enemy idle
      animation and ambient effects differ between captures), so exact pixel comparison would flake;
      the one native + two ART frame PNGs plus their probe metadata are the artifacts for
      INDEPENDENT VISUAL REVIEW, and repeatability is asserted at the counter level instead. YAML +
      docs only (no runner change); reused by later family slices.

- [x] NRO-04 A04 (container AFTER-update DETACH observation for superless effects): closed the NRM-12
      gap without re-applying update and without retaining a terminal record. `TransientEffectContainerPatches
      .ObserveContainerEffectUpdates` instruments `AbstractDungeon.update()` and wraps each
      `AbstractGameEffect.update()` call site as `$proceed($$); observeAfterUpdate($0);` so the
      native update runs unchanged and only the POST-update `isDone` is observed via
      `NativeRenderBridge.observeEffectUpdateDetached` → `TransientEffectLedger.detach(identity)`
      (never calls update; no identity/admission change). Detach drops the active record without a
      terminal record so a still-rendered/re-added object re-admits instead of being rejected — D1
      showed the retained-terminal version spiked `rejectedTerminal`/`unknownLifecycle` (~3800/s),
      froze `total`/`completed`, and made strict false with the gate OFF. Contrast with the removed
      `observeThenUpdate` attempt, which re-applied update. The class-level `super.update()` Postfix
      still completes/retains; POOLED (`Pool.Poolable`) effects are excluded from both paths. Only
      `TransientEffectLedger.detach` was added. See the NRM-12b/A04 note above.

- [x] NRO-04 A05 (recovery / long-run lifecycle device scenario + detach-then-recovery unit test):
      new `tests/ui-scenarios/device/d1_vfx_claim_lifecycle.yaml` verifies the transient-effect
      claim seam across destructive recovery and a room change/restart, parameterized by a leading
      `set: {target_kind}` (default `smoke`, a non-pooled claimable kind; retarget via
      `cardtrail`/`torchheadfire`/`absorption`). Legs: (1) GATE-ON spawn — `art claim on` →
      `aura.ready gte 1` → `art claim clear` → baseline `aura.draws`/`leaked`/`rejectedTerminal`,
      then `art claim spawn ${target_kind} 20` must grow ART draws (`gt_var`) with
      `leaked`/`rejectedTerminal` flat; (2) PANIC/RECOVERY — `art present panic lab` makes
      `backend.safety.panic eq true` and clears the seam (ledger/registry/tokens/isolate/filter/
      surface, present OFF) while `rejectedTerminal`/`leaked` stay flat, then
      `art present clear-panic` flips `panic eq false`; (3) ROOM CHANGE / RESTART —
      `ensure-fresh-menu` + `start-run IRONCLAD` must not leak unfinished records or reject any
      still-live object (`rejectedTerminal`/`leaked` still flat), then re-enter combat; (4) RE-CLAIM
      AFTER RECOVERY — the seam still claims and DRAWS (`gt_var` over a fresh cleared baseline) with
      `dispositionMismatch` captured before/after and flat plus `nativeRenderStrict.accepted
      eq true`; (5) CAPACITY BOUND — `transientEffects.activeCap eq 4096` and `active lte 4096`.
      Invariant: `rejectedTerminal`/`unknownLifecycle`/`leaked` must NOT grow; the exact A04
      detach-then-recovery re-admission contract. True 4096 active-window overflow, its eviction,
      and re-admission of a still-live evicted object are DELIBERATELY a no-GL unit boundary, not a
      device step. Supporting unit test
      `NativeRenderBridgeTest.detachedEffectSurvivesRecoveryAndReAdmits`: render a superless effect
      (active 1) → `isDone=true` → container `observeAfterUpdate` detaches it (active 0, no recent/
      terminal record, `total` unchanged) → `clearTransientEffectsForRecovery()` → re-render the
      SAME object is RE-ADMITTED (active 1) with `rejectedTerminal`/`unknownLifecycle`/`leaked`
      all 0; a pooled effect behaves the same (never completed, reuse re-admitted). No production
      code change.

- [x] NRO-04 A05b (fix real D1 defect: recovery stale-marking live records caused permanent
      `rejectedTerminal`/`unknownLifecycle` growth after panic): D1 showed that after
      `art present panic lab` → `art present clear-panic`, still-live ambient effects re-rendered
      and the ledger rejected them once per render per object (~3900/s), permanently forcing
      `nativeRenderStrict.accepted=false` and poisoning normal play (strict acceptance could never
      recover after a panic). Root cause: `TransientEffectLedger.clearCompletedForRecovery()` did
      `markStale(records.values()); records.clear();` — `records` holds the still-ACTIVE effects, so
      stale-marking put each live object's own id into `staleIdentities`; on the next render
      `admitRender` matched it and counted a terminal rejection. This is OBSOLETE under NRM-13:
      instance ids are non-reusable per-object monotonic ids (`class#<seq>`), so `staleIdentities`
      can only ever match the SAME object and no longer protects against id reuse by a DIFFERENT
      object; marking a live record stale therefore only harms it. Fix:
      `clearCompletedForRecovery()` now drops the active records WITHOUT stale-marking
      (`records.clear();` only), so a still-live effect rendered after recovery is RE-ADMITTED as a
      fresh active record. `clearLeaked`/`detach`/`admitRender`/`markStale`/`retainRecent`/the
      recent-window terminal stale-identity eviction path are unchanged (`staleIdentities` keeps its
      genuine terminal purpose). Tests: replaced `TransientEffectLedgerTest.recoveryClearRejectsLateRenderWithBoundedStaleIdentityMarkers`
      with `recoveryClearReAdmitsLiveRenderWithoutStaleRejection` (+ new
      `recoveryClearLeavesGenuineTerminalRecordsUntouched`); replaced the old-behavior
      `TransientEffectLifecycleAdapterTest.recoveryRenderDoesNotRecreatePresentationEntity` with
      `recoveryRenderReAdmitsLiveEffectWithoutRejection`; replaced/added the bridge tests
      `NativeRenderBridgeTest.liveEffectReAdmitsAfterRecoveryWithoutRejection` and updated
      `lateBeginEffectRenderAfterRecoveryClearRemainsFailOpenAndInactive` to the re-admission
      contract (the old `unknownLifecycle == 1` assertion was the defect). Verified: the four
      recovery re-admission tests FAIL with the old `markStale`+`clear` body and PASS with the fix.

- [x] NRO-04 B09 (explicit MULTI-PASS draw FAILURE CONTRACT for the claim seam, landed BEFORE any
       new variable-length/repeated-draw native class joins): the F25 multi-draw path
       (`VfxDrawGeometry.drawPassRandomRanges`, today only `STANCE_CHANGE_ABSORPTION`, 2 passes)
       snapshots the shared RNG ONCE before the first pass, but the previous behavior restored the
       snapshot and returned `false` on a throw during ANY pass — so a throw on a LATER pass after an
       earlier pass already painted pixels made the caller fail open to the NATIVE render of the whole
       effect, double-drawing the earlier ART pass (partial ART draw + full native draw). That was an
       unstated, unsafe contract. Defined and implemented: **pre-pass failure** (no pixels painted yet,
       i.e. the first `sb.draw` throws) → draw nothing, restore the RNG snapshot so native re-consumes
       exactly the values it expects, and return `false` (fail open); **post-pass failure** (at least
       one pass already painted pixels) → the drawn passes' RNG consumption must stand, so do NOT
       restore, and return `true` (the instance is treated as ART-owned for this frame, so native never
       repaints all passes — no double-draw). Implemented in `Sts1VfxArtRenderer.renderTexture`'s
       multi-pass branch only, via `boolean drewAny = false;` set true immediately AFTER each successful
       `sb.draw(...)`: the `catch (Throwable)` returns `true` without restoring when `drewAny`, else
       restores and returns `false`. The single-draw path (`drawPasses.isEmpty()`) is unchanged (a
       single draw cannot partially succeed). The method Javadoc now states the contract explicitly and
       notes it is the precondition for future variable-length multi-draw kinds (the next slice,
       FlyingOrbEffect, is a variable-length multi-draw kind). No `VfxDrawGeometry` formula change, no
       new RNG for kinds that have none, no bridge/renderer side effects beyond the local boolean.
       Tests in `Sts1VfxArtRendererTest` pin both branches (+ the success control):
       `multiPassDrawThrowOnFirstPassFailsOpenAndRestoresRng` (returns `false`, zero pixels, stream
       equals the never-rendered control),
       `multiPassDrawThrowOnSecondPassKeepsClaimedFrameAndDoesNotRestoreRng` (returns `true`, exactly
       one pixel pass, stream NOT restored), and `multiPassDrawSuccessStillIssuesAllPasses` (2 passes,
       returns `true`, stream advanced). Focused no-GL JUnit only.

- [x] NRO-04 B01 (the seam's FIRST VARIABLE-LENGTH MULTI-DRAW kind, `FlyingOrbEffect`, joins the
       default-off per-instance claim seam): `com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect` is
       appended LAST to `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor`.
       It is the seam's first kind whose draw COUNT comes from a host array rather than a fixed pass
       list, and its first BOOLEAN `isDone` draw guard. `VfxDrawGeometry.Kind.FLYING_ORB` adds pure
       capabilities: `variableLengthMultiDraw(kind)` (true only for `FLYING_ORB`; the F25 two-pass
       `STANCE_CHANGE_ABSORPTION` stays fixed-length), the named constants
       `flyingOrbStartScaleMultiplier()` = `1.5f` and `flyingOrbScaleDecayPerDraw()` = `0.975f`, and
       the boolean-guard trio `nativeSkipsDrawByGuard`/`guardFieldName` (`"isDone"`)/`guardIsBoolean`/
       `guardBlocksBoolean` (blocks iff true); the existing float `guardBlocks` for
       FALLING_ICE/DAMAGE_HEART/WRATH is unchanged. `Sts1VfxArtRenderer` gains
       `usesVariableLengthDraw` and a dedicated `renderFlyingOrb` branch BEFORE `readFields` (the
       class has NO x/y/scale field): it reads the effect's own `Vector2[] points`, `rotation`,
       `color`, `img`, computes `scale0 = Settings.scale * 1.5f`, and iterates `index` from
       `points.length-1` down to `1` (index 0 is NEVER drawn) drawing each non-null point
       center-packed with the native asymmetric division split — the POSITION offset uses INTEGER
       division (`point.x - (packed/2)`), the ORIGIN uses FLOAT division (`packed/2f`), so an odd
       region (native `ImageMaster.GLOW_SPARK_2` is 81x81) draws at `x - 40` with origin `40.5` — and
       size `pw, ph`, with a uniform scale multiplied by `0.975f` after
       each DRAWN point, additively (770/1 then 770/771), restoring color/blend in `finally`. It
       honors the B09 multi-pass failure contract: a pre-draw throw fails open (returns false), a
       post-draw throw keeps the claim (returns true, no RNG here). `guardSatisfied` now handles
       boolean guards (reads raw `isDone`, blocks on TRUE), and `canDraw`/`imagePresent` treat a
       FLYING_ORB instance as drawable iff its region is present and `!isDone` (no x/y/scale). The
       lab gains aliases `"flyingorb"`/`"orb"` → `new FlyingOrbEffect(960f, 540f)` behind the
       fail-open guard (its ctor reads `AbstractDungeon.player.hb` and allocates
       `points = new Vector2[60]` (a length-60 array of nulls) that `update()` fills, so a fresh
       instance draws nothing until then — the D1 scenario spawns it
       for observation/lifecycle only). Tests: `VfxDrawGeometryTest` (kindFor + near-miss, variable
       -length, decay constants, boolean guard), `Sts1VfxArtRendererTest` (descending non-null draw
       order + exact per-draw scale sequence, index-0 ignored, odd-region integer-position/float-origin
       split, `isDone` blocks, B09 pre/post-draw
       branches, missing img fails open, null-points claims zero draws), `VfxDelegationSeamTest` /
       `Sts1VfxRendererBindingTest` (readiness + appended-last order), `VfxLabSpawnTest` (alias → FQN
       + capturing factory). No new patch/bridge/console wiring; the default-off gate is unchanged.

- [x] NRO-04 B02 (`FlickCoinEffect` joins the default-off per-instance claim seam):
       `com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect` is appended LAST to
       `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor`. It is a
       single-draw img path with a dedicated reader (its position fields are `cX`/`cY`/`yOffset`, NOT
       `x`/`y`), an integer-half position offset, a float-half origin, and an ANISOTROPIC scale:
       `VfxDrawGeometry.Kind.FLICK_COIN` adds the named constants `FLICK_COIN_SCALE_X = 0.7f` /
       `FLICK_COIN_SCALE_Y = 0.4f` and the pure predicate `flickCoinUsesAnisotropicScale` /
       `flickCoinScaleXMultiplier()` / `flickCoinScaleYMultiplier()` (`additiveBlend` true; it joins
       no guard/flip/mirror/RNG capability, and no existing kind's multipliers change). Its `params`
       branch draws the packed size with POSITION `(cX - int(pw/2), cY - int(ph/2) + yOffset)`
       (native INTEGER division), ORIGIN `(pw/2f, ph/2f)` (native FLOAT division), and scale
       `(scale*0.7f, scale*0.4f)` with the field rotation. `Sts1VfxArtRenderer` gains a dedicated
       `renderFlickCoin` branch BEFORE `readFields` (like FLYING_ORB), reading
       `cX`/`cY`/`yOffset`/`rotation`/`color`/`img` plus the inherited `scale`; a single draw
       installs/restores the additive blend and color inside try/catch(Throwable) with a `finally`
       restore, and a throw before the draw fails open (returns false). `canDraw`/`imagePresent`
       treat a FLICK_COIN instance as drawable iff its `img` region is present (the generic
       `readFields` requirements are unchanged). The lab gains aliases `"flickcoin"`/`"coin"` ->
       `new FlickCoinEffect(960f, 540f, 960f, 540f)` behind the existing fail-open guard (its static
       `img` comes from `ImageMaster.vfxAtlas` and may be null off-game). The D1 scenario
       `d1_aura_claim.yaml` adds `art claim spawn flickcoin 4` to both gate phases. Tests:
       `VfxDrawGeometryTest` (kindFor + near-miss, anisotropic-scale capability/constants,
       additive, not guard/flip/mirror/RNG), `Sts1VfxArtRendererTest` (single draw with odd-region
       integer-position/float-origin split, anisotropic scale, field rotation, additive
       installed/restored, effect color, missing `img` fails open), `VfxDelegationSeamTest` /
       `Sts1VfxRendererBindingTest` (readiness + appended-last order), `VfxLabSpawnTest` (alias ->
       FQN + capturing factory). No new patch/bridge/console wiring; the default-off gate is
       unchanged.

- [x] NRO-04 B03 (`HealPanelEffect` joins the default-off per-instance claim seam):
       `com.megacrit.cardcrawl.vfx.combat.HealPanelEffect` is appended LAST to
       `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor`. It is a bare
       static `Texture` + fixed 64x64 source-rect kind: the image is the STATIC `Texture img`
       (loaded in the ctor via `ImageMaster.loadImage("images/ui/topPanel/panel_heart_white.png")`;
       there is NO instance img field), the src rect is `(0,0,64,64)`, the origin is the fixed
       `(32f, 32f)` and the size is the fixed `64f x 64f` (NOT the texture's dimensions). Its
       native draw position is PANEL-SPACE, depending on `Settings.HEIGHT` as well as
       `Settings.scale`: `x = x - 32f + 32f * Settings.scale` and
       `y = Settings.HEIGHT - 32f * Settings.scale - 32f`; the scale is the effect's own uniform
       `scale` and the rotation comes from the field. It is ADDITIVE (setBlendFunction 770/1 before
       and 770/771 after, with `setColor(color)` BEFORE the blend) and uses the effect's own color;
       it joins NO guard/flip/mirror/RNG/variable-length capability. `VfxDrawGeometry.Kind.HEAL_PANEL`
       adds the named constants `HEAL_PANEL_SRC_X/SRC_Y/SRC_W/SRC_H = 0/0/64/64`,
       `HEAL_PANEL_ORIGIN = 32f`, `HEAL_PANEL_SIZE = 64f`, `HEAL_PANEL_X_OFFSET = 32f`,
       `HEAL_PANEL_Y_OFFSET = 32f` and a dedicated `params` branch (a `settingsHeight` overload; every
       other kind ignores it so their results are byte-identical). `Sts1VfxArtRenderer` routes
       `HEAL_PANEL` through the existing bare-`Texture` `renderTexture` path (added to
       `isTextureDrawKind`), resolving the STATIC `img` `Texture` (via the instance-texture reader,
       like FallingIce), the fixed `HEAL_PANEL_SRC_*` src rect and the effect's own color (NOT the
       white-alpha rule), with `readTextureFields`/`imagePresent`/`canDraw` handling the static img
       and no guard/playerHitbox. The lab gains aliases `"healpanel"`/`"heal"` ->
       `new HealPanelEffect(960f)` behind the existing fail-open guard (the ctor loads a static
       Texture and reads `Settings.scale`). The D1 scenario `d1_aura_claim.yaml` adds
       `art claim spawn healpanel 4` to both gate phases. Tests: `VfxDrawGeometryTest` (kindFor +
       near-miss, exact panel-space params with the `Settings.HEIGHT`/`Settings.scale` terms,
       additive, not guard/flip/mirror/RNG/variable-length), `Sts1VfxArtRendererTest` (one draw with
       the fixed src/origin/size, panel-space position, uniform scale, field rotation, additive
       installed/restored, effect's own color not white-forced, missing/null static `img` fails open),
       `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order),
       `VfxLabSpawnTest` (alias -> FQN + capturing factory). No new patch/bridge/console wiring; the
       default-off gate is unchanged.

- [x] NRO-04 B04 (`PingHpEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.combat.PingHpEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor` (after
        `HealPanelEffect`). It is the HealPanel analogue: a bare STATIC `Texture` kind with a fixed
        64x64 source rect (`0,0,64,64`), a fixed `(32f, 32f)` origin and a fixed `64f x 64f` size,
        whose texture is the STATIC `ImageMaster.TP_HP` resolved at draw time (there is NO instance
        `img` field). Its native draw position is the identical PANEL-SPACE rule as HealPanel,
        depending on `Settings.HEIGHT` as well as `Settings.scale`:
        `x = x - 32f + 32f * Settings.scale` and `y = Settings.HEIGHT - 32f * Settings.scale - 32f`;
        the rotation comes from the field and the color is the effect's own (yellow, animated alpha).
        The ONE difference from HealPanel is the uniform draw scale: BOTH axes are
        `scale * Settings.scale`. It is ADDITIVE (setBlendFunction 770/1 before and 770/771 after, with
        `setColor(color)` BEFORE the blend) and joins NO guard/flip/mirror/RNG/variable-length
        capability. `VfxDrawGeometry.Kind.PING_HP` adds the named constants
        `PING_HP_SRC_X/SRC_Y/SRC_W/SRC_H = 0/0/64/64`, `PING_HP_ORIGIN = 32f`, `PING_HP_SIZE = 64f`,
        `PING_HP_X_OFFSET = 32f`, `PING_HP_Y_OFFSET = 32f` and a dedicated `params` branch (via the
        `settingsHeight` overload) returning `(scale * settingsScale, scale * settingsScale)`;
        `Sts1VfxArtRenderer` routes `PING_HP` through the existing bare-`Texture` `renderTexture` path
        (added to `isTextureDrawKind`, NOT `usesInstanceTexture`), resolves the STATIC
        `ImageMaster.TP_HP` (a static `resolveTexture` case), the fixed `PING_HP_SRC_*` src rect, and
        the effect's own color (NOT white-alpha), with `readTextureFields`/`imagePresent`/`canDraw`
        treating `y` as optional and no guard/playerHitbox. The lab gains aliases `"pinghp"`/`"ping"`
        -> `new PingHpEffect(960f)` behind the existing fail-open guard. The D1 scenario
        `d1_aura_claim.yaml` adds `art claim spawn pinghp 4` to both gate phases. Tests:
        `VfxDrawGeometryTest` (kindFor + near-miss, exact panel-space params with the
        `Settings.HEIGHT`/`Settings.scale` terms and BOTH axes at `scale * Settings.scale` vs HealPanel,
        additive, not guard/flip/mirror/RNG/variable-length/anisotropic), `Sts1VfxArtRendererTest` (one
        draw with the fixed src/origin/size, panel-space position, `scale * Settings.scale` uniform
        scale, field rotation, additive installed/restored, effect's own color not white-forced,
        missing/null static texture fails open), `VfxDelegationSeamTest` (readiness + appended-last
        order), `VfxLabSpawnTest` (alias -> FQN + capturing factory). No new patch/bridge/console
        wiring; the default-off gate is unchanged.

- [x] NRO-04 B05 (`RewardGlowEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.RewardGlowEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor` (after
        `PingHpEffect`). Its SINGLE-ARG `render(SpriteBatch)` — the overload the effect container
        calls and the only one the container claim seam reaches — is a bare STATIC
        `ImageMaster.REWARD_SCREEN_ITEM` `Texture` kind (there is NO instance `img` field) with a
        fixed `464x98` source rect (`0,0,464,98`), a fixed `(232f, 49f)` origin and a fixed `464f x
        98f` size; its position is `(x - 232f, y - 49f)` (no settings term). The draw scale is
        ANISOTROPIC: `scaleX = Settings.xScale` (INDEPENDENT of the effect's own `scale`) and
        `scaleY = scale + Settings.scale * 0.05f`; the rotation is hardcoded `0f` (the effect's own
        `angle` field is NOT used by this overload); the color is the effect's own. It is ADDITIVE
        and joins NO guard/flip/mirror/RNG/variable-length/flickCoin-anisotropic capability.
        `VfxDrawGeometry.Kind.REWARD_GLOW` adds the named constants
        `REWARD_GLOW_SRC_X/SRC_Y/SRC_W/SRC_H = 0/0/464/98`, `REWARD_GLOW_ORIGIN_X = 232f`,
        `REWARD_GLOW_ORIGIN_Y = 49f`, `REWARD_GLOW_SIZE_W = 464f`, `REWARD_GLOW_SIZE_H = 98f`,
        `REWARD_GLOW_SCALE_Y_SETTINGS_MULT = 0.05f` and a dedicated `params` branch reusing the
        trailing `settingsHeight` slot to carry `Settings.xScale` (host-neutral; every other kind is
        byte-identical). `Sts1VfxArtRenderer` routes `REWARD_GLOW` through the existing bare-`Texture`
        `renderTexture` path (added to `isTextureDrawKind`, NOT `usesInstanceTexture`), resolves the
        STATIC `ImageMaster.REWARD_SCREEN_ITEM` (a static `resolveTexture` case), the fixed
        `REWARD_GLOW_SRC_*` src rect, the additive blend, and the effect's own color (NOT
        white-alpha), with `readTextureFields` treating `rotation` as optional (like `CARD_TRAIL`;
        native hardcodes `0f`) and `x`/`y` as required. The lab gains aliases `"rewardglow"`/
        `"reward"` -> `new RewardGlowEffect(960f, 540f)` behind the existing fail-open guard. The D1
        scenario `d1_aura_claim.yaml` adds `art claim spawn rewardglow 4` to both gate phases. Tests:
        `VfxDrawGeometryTest` (kindFor + near-miss, exact params with `Settings.xScale` independent of
        the effect scale and `scaleY = scale + Settings.scale*0.05`, rotation 0, additive, not
        guard/flip/mirror/RNG/variable-length/flickCoin), `Sts1VfxArtRendererTest` (one draw with the
        fixed src/origin/size, position, anisotropic scale, rotation 0, additive installed/restored,
        own color, no `rotation` field required, missing/null static texture fails open),
        `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order),
        `VfxLabSpawnTest` (alias -> FQN + capturing factory). The class's OTHER
        `render(SpriteBatch, Color)` overload is a DIFFERENT draw (the static
        `ImageMaster.WHITE_SQUARE_IMG`, `x-32,y-32`, origin/size 32/64, rotation `angle`, scale
        `scale*Settings.scale/2f`) that the container seam does not reach, so it is deliberately NOT
        claimed and stays native. No new patch/bridge/console wiring; the default-off gate is
        unchanged.
        PRODUCTION REACH (B05 boundary): the claim seam's only effect observer instruments
        `AbstractDungeon.render`'s direct `AbstractGameEffect.render` call sites
        (`TransientEffectContainerPatches`). `RewardGlowEffect` is constructed only in
        `com.megacrit.cardcrawl.rewards.RewardItem`, whose `render(SpriteBatch)` iterates its own
        private `effects` list calling `AbstractGameEffect.render(SpriteBatch)` (reached via
        `CombatRewardScreen.render`), which the seam does NOT instrument. Real reward-screen
        `RewardGlow` instances are therefore NOT yet observed/claimed; B05 is reachable on-device only
        via the lab spawn into the `AbstractDungeon` effect queues, and its parity/claim path is
        unit-verified. Claiming a real reward-screen instance requires instrumenting
        `RewardItem.render`'s `AbstractGameEffect.render` call site (tracked as B05b below); until then
        the seam fails open to native there.

- [x] **B05b reward-screen effect-loop observation boundary**: instrument
        `com.megacrit.cardcrawl.rewards.RewardItem#render(SpriteBatch)`'s
        `AbstractGameEffect.render(SpriteBatch)` call site with the same observe-then-render pattern
        used by `TransientEffectContainerPatches` (with NRCC ownership-manifest coverage), so
        reward-screen effects — starting with `RewardGlowEffect` (B05) — are actually observed and
        claimable in production (today the seam's only observer is `AbstractDungeon.render`, so the
        reward-screen `RewardItem.effects` loop is unseen). DESIGN: an observation-only extension of
        the existing container seam — a nested `ObserveRewardItemEffectRenders`
        `@SpireInstrumentPatch` on `RewardItem.render(SpriteBatch)` reuses the exact same
        `TransientEffectContainerPatches.observeThenRender($0, $1, <line>)` helper (no `$_ =`, no
        `SpireReturn`), so native rendering follows the bridge disposition and observation failures
        fail open; no new suppression authority is introduced (the sole native-absence branch stays
        the pre-existing isolate-only disposition already governed by the `AbstractDungeon` entry).
        Double-observation is impossible: `RewardItem.effects` is NOT
        `AbstractDungeon.effectList`/`topLevelEffects`, so no instance is observed twice per draw.
        The carried line is the reward-loop call-site line, which matches no known
        `EffectRenderBand` line and degrades to `Band.UNKNOWN` (record-only, never order evidence).
        javap on the 1.0 jar confirms EXACTLY ONE such call site in `RewardItem.render` (offset 992,
        `invokevirtual com/megacrit/cardcrawl/vfx/AbstractGameEffect.render:(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V`).
        NRCC: the `rewards.rewarditem.render` row is updated from `surfaceId:''/hook:''`
        inheriting `NATIVE_WITH_ART_OVERLAY` to `hook: TransientEffectContainerPatches.java`,
        `policy: OBSERVED`, with `conditionalSuppression: ISOLATE_ONLY` and
        `NO_PIXEL_ISOLATION` fields mirroring the `AbstractDungeon` row; a `known_policy` entry
        (`com.megacrit.cardcrawl.rewards.RewardItem#render -> OBSERVED`), a curated justification, and
        a curated `known_suppression` overlay keep `--write-manifest` regeneration fully stable (the
        earlier regeneration drift was closed in the NRCC follow-up below).
        HONEST D1 GAP — no lab command constructs a real `CombatRewardScreen`, so a device check
        of the reward effect loop is NOT possible; D1 evidence is limited to a no-regression
        load/probe check plus the unit tests below. This slice does NOT claim D1 verification of the
        reward effect loop. Tests: `TransientEffectContainerPatchesTest` (predicate matches only the
        native `render:(SpriteBatch)V` call and rejects other owner/method/overload; generated
        replacement body contains `observeThenRender($0, $1, 992)` with no `$proceed`/`SpireReturn`;
        the reused helper draws the native effect exactly once and fails open when the native draw
        throws). NRCC: `python3 tools/nrcc/scan_sts_render.py ... --check-manifest` reports
        `ok` (549/549, no errors, no ownership errors) and `tools/nrcc` unit tests stay green.

- [x] NRO-04 B06 (`MapCircleEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.MapCircleEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor` (after
        `RewardGlowEffect`). Its fields are `public static Texture img` (set in the ctor to
        `ImageMaster.MAP_CIRCLE_1` and SWAPPED by `update()` through
        `MAP_CIRCLE_5`/`MAP_CIRCLE_4`/`MAP_CIRCLE_3`/`MAP_CIRCLE_2`; there is NO instance `img`
        field), `private float x`, `private float y`, and the inherited `scale`/`rotation`; ctor
        `(float x, float y, float rotation)`. Its native `render(SpriteBatch)` is
        `sb.setColor(new Color(0.09f, 0.13f, 0.17f, 1f)); sb.draw(img, x - 96f, y - 96f, 96f, 96f,
        192f, 192f, scale, scale, rotation, 0, 0, 192, 192, false, false)` — a bare static-`Texture`
        fixed-rect kind with a FIXED `(0,0,192,192)` src rect, a FIXED `(96f,96f)` origin and a FIXED
        `192f x 192f` size, position `(x - 96f, y - 96f)`, the uniform `scale`, the field `rotation`,
        NO `setBlendFunction` (AMBIENT blend), and — uniquely — a HARDCODED draw color
        `(0.09f, 0.13f, 0.17f, 1f)` that IGNORES the effect's own color field.
        `VfxDrawGeometry.Kind.MAP_CIRCLE` adds the named constants `MAP_CIRCLE_SRC_X/SRC_Y/SRC_W/
        SRC_H = 0/0/192/192`, `MAP_CIRCLE_ORIGIN = 96f`, `MAP_CIRCLE_SIZE = 192f` and the hardcoded
        color components `MAP_CIRCLE_COLOR_R/G/B/A = 0.09f/0.13f/0.17f/1f`, plus a dedicated `params`
        branch (position/origin/size/scale/rotation; all other inputs unused). `Sts1VfxArtRenderer`
        routes `MAP_CIRCLE` through the existing bare-`Texture` `renderTexture` path (added to
        `isTextureDrawKind`, NOT `usesInstanceTexture`), reads the PUBLIC STATIC field via
        `readRaw(effect, "img")` (which resolves the static value and requires a non-null `Texture`),
        adds a static `resolveTexture` case returning that field, the fixed `MAP_CIRCLE_SRC_*` src
        rect, the AMBIENT blend (no blend switch; restores only color), and a dedicated `resolveColor`
        branch returning `new Color(0.09f, 0.13f, 0.17f, 1f)` (NOT the effect color, and NOT the
        white-alpha rule). `readTextureFields` treats `rotation` as REQUIRED (native consumes it) and
        `x`/`y` as required. The lab gains aliases `"mapcircle"`/`"map"` ->
        `new MapCircleEffect(960f, 540f, 0f)` behind the existing fail-open guard. The D1 scenario
        `d1_aura_claim.yaml` adds `art claim spawn mapcircle 4` to both gate phases. Tests:
        `VfxDrawGeometryTest` (kindFor + near-miss, exact params + hardcoded color constants, ambient,
        not guard/flip/mirror/RNG/variable-length/flickCoin), `Sts1VfxArtRendererTest` (one draw with
        the fixed src/origin/size, position, uniform scale, field rotation, NO blend change (ambient),
        hardcoded color regardless of the effect color, missing/null static img fails open, missing
        rotation fails open), `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness +
        appended-last order + texture-kind), `VfxLabSpawnTest` (alias -> FQN + capturing factory). No
        new patch/bridge/console wiring; the default-off gate is unchanged.
        PRODUCTION REACH (B06 boundary, CORRECTED by B06b): the claim seam's only effect observer
        instruments `AbstractDungeon.render`'s direct `AbstractGameEffect.render` call sites
        (`TransientEffectContainerPatches`). The original claim here that `MapCircleEffect`'s
        map-screen effect loop is NOT instrumented was WRONG: `MapRoomNode` adds `MapCircleEffect`
        directly to `AbstractDungeon.topLevelEffects`, whose loop IS one of the three instrumented
        `AbstractDungeon.render` sites, so real map-screen `MapCircle` instances ARE observed and
        claimable (see B06b below). B06 remains reachable on-device via the lab spawn too, and its
        parity/claim path is unit-verified.

- [x] NRO-04 B07 (`SpotlightEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.SpotlightEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` and mapped by `kindFor` (after
        `MapCircleEffect`). It is a FULL-SCREEN bare static-`Texture` kind with no own per-effect
        geometry field (no `x`/`y`/`scale`/`rotation`; the native `render` consumes only `color` — the
        inherited `scale`/`rotation` are unused and not required) and a public NO-ARG ctor
        (`duration = 3f`, `color = new Color(1f, 1f, 0.8f, 0.5f)`). Its native `render(SpriteBatch)`
        is `setColor(color); setBlendFunction(770, 1); sb.draw(ImageMaster.SPOTLIGHT_VFX, 0f, 0f,
        Settings.WIDTH, Settings.HEIGHT); setBlendFunction(770, 771)` — ADDITIVE, the static
        `ImageMaster.SPOTLIGHT_VFX` `Texture`, position `(0f, 0f)`, size
        `Settings.WIDTH x Settings.HEIGHT`, NO origin/rotation/scale, and draw color = the effect's
        own `color`. `VfxDrawGeometry.Kind.SPOTLIGHT` adds the pure capability
        `fullScreenTexture(Kind)` (true only for `SPOTLIGHT`; the renderer synthesizes the
        `(0,0,Settings.WIDTH,Settings.HEIGHT)` draw and this class stays free of `Settings`), maps the
        exact FQN, is ADDITIVE, and joins NO guard/flip/mirror/RNG/variable-length/flickCoin/
        uses-instance-texture capability. `Sts1VfxArtRenderer` adds `SPOTLIGHT` to
        `isTextureDrawKind` (NOT `usesInstanceTexture`), a static `resolveTexture` case returning
        `ImageMaster.SPOTLIGHT_VFX`, and a dedicated `renderFullScreenTexture` branch served BEFORE
        `readTextureFields` (which requires x/y/scale and does not apply): it reads ONLY the effect's
        own `color`, installs/restores the additive blend, saves/restores color, and draws through the
        4-arg `sb.draw(texture, 0f, 0f, Settings.WIDTH, Settings.HEIGHT)` overload with the single-draw
        failure contract (a throw before the draw fails open). `imagePresent`/`canDraw` for `SPOTLIGHT`
        require only the static texture non-null plus a non-null effect color. The lab gains the alias
        `"spotlight"` -> `new SpotlightEffect()` (NO-ARG) behind the existing fail-open guard. The D1
        scenario `d1_aura_claim.yaml` adds `art claim spawn spotlight 4` to both gate phases. Tests:
        `VfxDrawGeometryTest` (kindFor + near-miss fail-open, `fullScreenTexture` true only for
        `SPOTLIGHT`, additive, not guard/flip/mirror/RNG/var-length/flickCoin/uses-instance-texture),
        `Sts1VfxArtRendererTest` (a SpotlightEffect instance with a resolvable static texture and a
        color and NO x/y/scale/rotation fields: exactly ONE full-screen 4-arg draw at
        `(0,0,Settings.WIDTH,Settings.HEIGHT)`, additive installed/restored, color = the effect's own
        color; missing/null static texture fails open; null color fails open),
        `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order +
        texture-kind), `VfxLabSpawnTest` (alias -> FQN + capturing factory). No new
        patch/bridge/console wiring; the default-off gate and per-instance token semantics are
        unchanged. PRODUCTION REACH (positive, B07): the claim seam's only effect observer
        instruments `AbstractDungeon.render`'s direct `AbstractGameEffect.render` call sites
        (`TransientEffectContainerPatches`). `SpotlightEffect` IS constructed by
        `com.megacrit.cardcrawl.vfx.combat.GrandFinalEffect`, which adds it to
        `AbstractDungeon.effectsQueue`; it is therefore rendered by that instrumented effect loop, so
        the seam DOES reach a real instance (unlike the B05/B06 reward/map cases). It is also
        lab-spawnable.

- [x] NRO-04 B08 (`CampfireRecallEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.campfire.CampfireRecallEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` (after `SpotlightEffect`) and mapped by
        `kindFor`. It is a FULL-SCREEN bare static-`Texture` kind with NO own per-effect geometry field
        (no `x`/`y`/`scale`/`rotation`) and a public NO-ARG ctor (`duration = 2f`, `hasRecalled =
        false`, `screenColor` from `AbstractDungeon.fadeColor` with alpha `0f`). Its native
        `render(SpriteBatch)` is `sb.setColor(screenColor); sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0f,
        0f, Settings.WIDTH, Settings.HEIGHT)` — AMBIENT (NO `setBlendFunction`), the static
        `ImageMaster.WHITE_SQUARE_IMG` `Texture`, position `(0f, 0f)`, size
        `Settings.WIDTH x Settings.HEIGHT`, NO origin/rotation/scale, and draw color read from the
        `screenColor` field (NOT the inherited `color`). `VfxDrawGeometry.Kind.CAMPFIRE_RECALL` extends
        the pure `fullScreenTexture(Kind)` capability to include it (the second full-screen member),
        adds the pure `fullScreenTextureReadsScreenColor(Kind)` (true only for `CAMPFIRE_RECALL`; the
        two full-screen kinds differ exactly here: `SPOTLIGHT` reads `color`, `CAMPFIRE_RECALL` reads
        `screenColor`), maps the exact FQN, is AMBIENT (`additiveBlend` false), and joins NO
        guard/flip/mirror/RNG/variable-length/flickCoin/uses-instance-texture capability.
        `Sts1VfxArtRenderer` adds `CAMPFIRE_RECALL` to `isTextureDrawKind` (NOT
        `usesInstanceTexture`), a static `resolveTexture` case returning
        `ImageMaster.WHITE_SQUARE_IMG`, and extends `renderFullScreenTexture` to read the kind's color
        field via `fullScreenTextureReadsScreenColor` (`color` for `SPOTLIGHT`, `screenColor` for
        `CAMPFIRE_RECALL`); for the ambient kind it does NOT switch blend, still saves/restores color,
        and keeps the single-draw fail-open. `imagePresent`/`canDraw` for `CAMPFIRE_RECALL` require the
        static texture non-null AND `screenColor` non-null. Every other kind is byte-identical. The lab
        gains the aliases `"campfirerecall"`/`"recall"` -> `new CampfireRecallEffect()` (NO-ARG) behind
        the existing fail-open guard (the static `WHITE_SQUARE_IMG` may be null off-game; the ctor
        reads `AbstractDungeon.fadeColor`). DEVICE VERIFICATION: `CampfireRecallEffect`'s native
        NO-ARG ctor ends by calling `((RestRoom) AbstractDungeon.getCurrRoom()).cutFireSound()`, so a
        COMBAT room makes it throw `ClassCastException`, which `VfxLabSpawn.spawn` swallows fail-open
        (`queued 0`) — the native effect is only ever constructed by `RecallOption` inside a rest room.
        It is therefore NOT spawned by the combat-based `d1_aura_claim.yaml` (a `campfirerecall` spawn
        there would be a silent no-op and would not exercise B08); B08 is verified by the dedicated
        REST-room scenario `tests/ui-scenarios/device/d1_vfx_claim_campfire.yaml`, which enters a rest
        room and asserts a strict `gt_var` growth of the ART draw counter after `art claim spawn
        campfirerecall 20` with FLAT `declinedTotal`/`dispositionMismatch`/`orphanArtOutput` and
        `nativeRenderStrict.accepted eq true` (draw counts are authoritative; the rest/map screen does
        not composite `AbstractDungeon.effectList` into the still). Tests: `VfxDrawGeometryTest`
        (kindFor + near-miss fail-open, `fullScreenTexture` true for it, `fullScreenTextureReadsScreenColor`
        true only for it, additive false, not guard/flip/mirror/RNG/var-length/flickCoin),
        `Sts1VfxArtRendererTest` (a CAMPFIRE_RECALL holder with NO x/y/scale/rotation fields and a
        `screenColor`: exactly ONE full-screen 4-arg draw of the static white-square at
        `(0,0,Settings.WIDTH,Settings.HEIGHT)`, NO blend change (installed-additive count 0), color =
        `screenColor` NOT `color`; null/absent `screenColor` fails open; null texture fails open),
        `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order +
        texture-kind), `VfxLabSpawnTest` (alias -> FQN + capturing factory). No new
        patch/bridge/console wiring; the default-off gate and per-instance token semantics are
        unchanged. PRODUCTION REACH (positive, B08): `CampfireRecallEffect` is constructed by
        `com.megacrit.cardcrawl.ui.campfire.RecallOption` and added to `AbstractDungeon.effectList`,
        so the instrumented `AbstractDungeon.render` effect loop DOES reach it (like B07); it is also
        lab-spawnable inside a rest room.

- [x] NRO-04 B10 (`FadeWipeParticle` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.FadeWipeParticle` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` (after `CampfireRecallEffect`) and mapped
        by `kindFor`. It is the seam's FIRST MULTI-SOURCE multi-draw kind: fields `float y`,
        `float lerpTimer`, `float delayTimer`, an `AtlasRegion img` (ctor:
        `ImageMaster.SCENE_TRANSITION_FADER`), a bare `Texture flatImg` (ctor:
        `ImageMaster.WHITE_SQUARE_IMG`), and the inherited `color` (NO `x`/`scale`/`rotation` of its
        own). Its public NO-ARG ctor reads `AbstractDungeon.fadeColor` into `color` with alpha `0f`.
        Its native `render(SpriteBatch)` issues NO `setBlendFunction` (AMBIENT), NO branch, and NO
        RNG, and draws TWO passes over TWO DIFFERENT image sources in this exact order:
        pass 0 the instance `img` `AtlasRegion` (4-arg REGION overload
        `sb.draw(img, 0f, y, Settings.WIDTH, img.packedHeight)`), then pass 1 the instance `flatImg`
        `Texture` (4-arg TEXTURE overload `sb.draw(flatImg, 0f, y + img.packedHeight -
        Settings.scale, Settings.WIDTH, Settings.HEIGHT)`); both use the effect's own `color`.
        `VfxDrawGeometry.Kind.FADE_WIPE` adds the pure `multiSourceWipe(Kind)` capability (true only
        for `FADE_WIPE`; DISTINCT from `variableLengthMultiDraw` and `drawPassRandomRanges`, and
        documents the region-then-texture pass order), maps the exact FQN, is AMBIENT
        (`additiveBlend` false), and joins NO guard/flip/mirror/RNG/variable-length/flickCoin/
        uses-instance-texture capability. `Sts1VfxArtRenderer` adds a dedicated `renderFadeWipe`
        branch BEFORE the generic `readFields`/`readTextureFields` routing (like FLYING_ORB/
        FLICK_COIN/full-screen): it reads the instance `img` (canonical flip-invariant region via
        `canonicalRegion`), `flatImg`, `y`, and `color`, draws pass 0 then pass 1 with the exact
        native geometry, never changes blend, and restores color in `finally`. The B09 two-pass
        failure contract applies: a pass-0 throw fails open (`return false`); a pass-1 throw after
        pass 0 painted keeps the claim (`return true`, `drewAny` flag) so native never double-draws.
        `imagePresent`/`canDraw` for `FADE_WIPE` require the `img` region valid AND `flatImg`
        non-null AND `color` non-null. Every other kind is byte-identical. The lab gains the aliases
        `"fadewipe"`/`"wipe"` -> `new FadeWipeParticle()` (NO-ARG) behind the existing fail-open guard
        (static ImageMaster art may be null off-game). Tests: `VfxDrawGeometryTest` (kindFor +
        near-miss fail-open, `multiSourceWipe` true only for it, additive false, not
        guard/flip/mirror/RNG/var-length/flickCoin/full-screen), `Sts1VfxArtRendererTest` (a
        FADE_WIPE holder with `img`/`flatImg`/`y`/`color` and NO x/scale/rotation: exactly TWO draws
        IN ORDER — pass 0 the region at `(0, y, Settings.WIDTH, packedHeight)`, pass 1 the `flatImg`
        texture at `(0, y + packedHeight - Settings.scale, Settings.WIDTH, Settings.HEIGHT)` —
        ambient (installed-additive call count 0), color = the effect's own color; a pass-1 throw
        keeps the claim (returns true, 1 draw) per B09; a pass-0 throw fails open; null
        `flatImg`/`img`/color fail open),
        `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order),
        `VfxLabSpawnTest` (alias -> FQN + capturing factory). DEVICE: `art claim spawn fadewipe 4` is
        added to BOTH the gate-OFF and gate-ON phases of `tests/ui-scenarios/device/d1_aura_claim.yaml`.
        No new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
        unchanged. PRODUCTION REACH (positive, B10): `FadeWipeParticle` is constructed by
        `MapRoomNode` and `SecretPortal` into `AbstractDungeon.topLevelEffects`, so the instrumented
        `AbstractDungeon.render` effect loop DOES reach a real instance (like B07/B08); it is also
        lab-spawnable via the `fadewipe`/`wipe` aliases. (`TopPanel` does NOT construct it; it only
        references the class via an `instanceof` check.)

- [x] NRO-04 B11 (`EmpowerCircleEffect` joins the default-off per-instance claim seam):
        `com.megacrit.cardcrawl.vfx.combat.EmpowerCircleEffect` is appended LAST to
        `VfxClaimPolicy.SUPPORTED_CLASSES`/`supports(...)` (after `FadeWipeParticle`) and mapped by
        `kindFor`. It is a single-draw kind whose render consumes render-time RNG for scaleX/scaleY
        AND is guarded by a boolean `isDone`; it reuses three existing capabilities with no new
        machinery. Its fields are `private float x`, `private float y`, `private float vX`,
        `private float vY`, an instance `AtlasRegion img` chosen at CONSTRUCTION time from
        `ImageMaster.POWER_UP_1`/`POWER_UP_2` via `MathUtils.randomBoolean()`, plus the inherited
        `color`/`scale`/`rotation`; ctor `(float, float)`. Its native `render(SpriteBatch)` is
        `if (isDone) return; setColor(color); sb.draw(img, x, y, img.packedWidth/2f,
        img.packedHeight/2f, img.packedWidth, img.packedHeight, scale * MathUtils.random(0.9f, 1.1f),
        scale * MathUtils.random(0.9f, 1.1f), rotation)` — NO `setBlendFunction` (AMBIENT),
        center-packed origin/size, position `(x, y)` passthrough, the uniform `scale` multiplied by
        `random(0.9f, 1.1f)` for scaleX THEN `random(0.9f, 1.1f)` for scaleY (EXACT order). It is the
        seam's producer of the RNG-REPLAY-WITH-BOOLEAN-GUARD combination. `VfxDrawGeometry` adds
        `Kind.EMPOWER_CIRCLE`: `randomRanges` returns the ordered list `[{0.9f,1.1f},{0.9f,1.1f}]`
        (the same shape as `WRATH_STANCE_CHANGE`), the boolean guard set gains it
        (`nativeSkipsDrawByGuard` true, `guardFieldName` == `"isDone"`, `guardIsBoolean` true,
        `guardBlocksBoolean` true when the value is true), `additiveBlend` is FALSE (ambient), and it
        reuses the existing center-packed `params` branch (no new geometry branch); it is NOT in
        flip/mirror/variable-length/flickCoin/fullScreenTexture/multiSourceWipe. `Sts1VfxArtRenderer`
        needs NO new draw branch: it rides the existing img path (`readFields` requires
        x/y/scale/rotation/color/img — all present — and treats `vY` as optional; the existing
        `randomRanges` handling pulls the two values in order and applies them to scaleX then scaleY,
        and the existing boolean `isDone` `guardSatisfied` blocks when true). The lab gains the
        aliases `"empowercircle"`/`"empower"` -> `new EmpowerCircleEffect(960f, 540f)` behind the
        existing fail-open guard (static ImageMaster art may be null off-game). Tests:
        `VfxDrawGeometryTest` (kindFor + near-miss fail-open, ordered `randomRanges`, boolean guard
        predicates, ambient, not flip/mirror/var-length/flickCoin/full-screen/multi-source),
        `Sts1VfxArtRendererTest` (an EMPOWER_CIRCLE holder with x/y/scale/rotation/color/img:
        exactly ONE center-packed draw at `(x, y)` with origin `packed/2` and the RNG-replayed
        scaleX/scaleY `scale * random(0.9f,1.1f)` in native order via mirror-RNG; an `isDone == true`
        instance draws nothing and consumes no RNG; a missing `img` fails open; a throw after RNG
        consumption restores the global RNG snapshot),
        `VfxDelegationSeamTest`/`Sts1VfxRendererBindingTest` (readiness + appended-last order),
        `VfxLabSpawnTest` (alias -> FQN + capturing factory). DEVICE: `art claim spawn empower 4` is
        added to BOTH the gate-OFF and gate-ON phases of `tests/ui-scenarios/device/d1_aura_claim.yaml`.
        NOTE: this is a NON-DETERMINISTIC (render-time RNG) kind, so the D1 probe only sees draw
        counts/entities, not the RNG-derived scale; the sequence/order is guaranteed by the unit
        tests (mirror-RNG), and the scenario spawns it for draw/lifecycle evidence like the other
        non-deterministic kinds. No new patch/bridge/console wiring; the default-off gate and
        per-instance token semantics are unchanged. PRODUCTION REACH (positive, B11):
        `EmpowerCircleEffect` is constructed by `com.megacrit.cardcrawl.vfx.combat.EmpowerEffect`,
        whose ctor queues eighteen of them into `AbstractDungeon.effectList`; `EmpowerEffect` is in
        turn constructed by `com.megacrit.cardcrawl.cards.Soul` (during the soul-into-card
        power-application animation), so the instrumented `AbstractDungeon.render` effect loop DOES
        reach real instances (like B07/B08/B10); it is also lab-spawnable via the
        `empowercircle`/`empower` aliases.

- [x] NRO-04 C01 (native local render-order baseline): the effect-container seam replaces the three
      `AbstractGameEffect.render(SpriteBatch)` call sites inside `AbstractDungeon.render` in place,
      so native order is inherently preserved; C01 records and exposes WHICH native band each
      observed effect render happened in, so the probe proves a claimed effect lands in the correct
      native band rather than blindly trusting call-site replacement. Pure
      `EffectRenderBand` classifies the three sites by their verified LineNumberTable line:
      `2674` = `effectList`/`renderBehind==true` before `room.render` (BAND A = EFFECT_LIST_BEHIND);
      `2697` = `effectList`/`renderBehind==false` after room/character/combat foreground (BAND B =
      EFFECT_LIST_FRONT); `2802` = `topLevelEffects`/`renderBehind==false` above
      `TopPanel.render`/`renderAboveTopPanel` (BAND C = TOP_LEVEL_FRONT); any other line (including
      `-1`) = UNKNOWN. `TransientEffectContainerPatches.observeThenRender` gains a 3-arg overload
      carrying the instrument-time line (`$0, $1, <line>`); the 2-arg overload delegates with `-1`
      (UNKNOWN) so existing callers/tests keep compiling. The seam records the band on BOTH terminal
      paths — the native continuation (after the native `effect.render`, recorded even when the
      native draw throws) and a successful vfx claim draw — but NOT on the isolate-suppressed path
      (nothing is drawn). Every record call is wrapped in its own try/catch and is observation-only:
      `NativeRenderBridge.recordObservedEffectBand(effect, line, claimed)` never throws and never
      changes identity/admission/lifecycle. The probe adds exactly
      `nativeRender.effectBands = {native:{effectListBehind,effectListFront,topLevelFront,unknown},
      claimed:{...}, claimedByClass:{"<fqn>":{...}}, claimedByClassCap}` (all Integer; the class map
      is a bounded TreeMap snapshot, cap 32, with an overflow diagnostic); `resetForTests()` clears
      it. DEVICE: `tests/ui-scenarios/device/d1_render_zorder_contract.yaml` (extended in place)
      proves the gate-OFF native baseline sees bands A and B, then gate-ON proves claimed
      `LightFlareLEffect` (band A) and `FireBurstParticleEffect` (band B) land in their native bands.
      No new ordering framework, no suppression permission, default-off gate unchanged; the line
      numbers are validated by D1 (a drift shows all-UNKNOWN and fails the assertions loudly). Tests:
      `EffectRenderBandTest`, `TransientEffectContainerPatchesTest`, `NativeRenderBridgeTest`.

- [x] NRO-04 C02 (per-render-pass native band-order/ordinal evidence): C01 proves a claimed effect
      lands in the correct native band; C02 proves the WITHIN-PASS band SEQUENCE is monotonic. Pure
      `EffectRenderBand.rank(Band)` maps the three real bands to their native draw ordinals
      (EFFECT_LIST_BEHIND = 0, EFFECT_LIST_FRONT = 1, TOP_LEVEL_FRONT = 2) and UNKNOWN to `-1`; only
      the three real bands participate in ordering. The existing
      `NativeRenderBridge.recordObservedEffectBand` recorder (extended, not parallel) tracks the
      per-pass last-seen rank plus cumulative `orderViolations`/`passesObserved`: on each observation
      a real band whose rank is STRICTLY lower than the last real band in the SAME pass is one
      violation; a pass change resets the per-pass rank and increments `passesObserved`; an UNKNOWN
      observation is counted in the existing `unknown` bucket and never touches the ordering state.
      Native and claimed observations advance the SAME ordering state (while the `native`/`claimed`
      per-band totals stay independent). A package-visible
      `recordObservedEffectBand(className, line, claimed, passId)` overload makes this deterministic
      for tests; the public 3-arg signature is unchanged.

      The pass boundary is an EXPLICIT render-pass counter: `NativeRenderBridge.beginEffectRenderPass()`,
      bumped by a NEW observation-only `AbstractDungeon.render` entry `Prefix`
      (`TransientEffectContainerPatches.ObserveEffectRenderPass`). An earlier C02 revision used the ART
      `lastFrameId()` as the boundary, but that id does NOT advance on the menu/transition screens, so
      successive `AbstractDungeon.render` invocations were collapsed into one "frame" and produced
      false `orderViolations` on D1 (21, accrued menu→run→combat, then frozen) — a real defect this fix
      removes. Within one native render traversal the three call sites are strictly sequential
      (A→B→C), so per-PASS monotonicity is the correct invariant; the Prefix only bumps the counter,
      never reorders/suppresses/draws pixels, never throws, and never touches
      identity/admission/lifecycle (ModTheSpire supports multiple patch classes on one method, so it
      coexists with the unchanged `ObserveContainerEffectRenders` ExprEditor). The probe adds exactly
      `nativeRender.effectBands.orderViolations` and `passesObserved` (Integer); `resetForTests()`
      clears them and resets the pass counter; recording stays observation-only. DEVICE:
      `tests/ui-scenarios/device/d1_render_zorder_contract.yaml` (extended in place, both gate-OFF and
      gate-ON phases) waits for `passesObserved gte 1` then asserts `orderViolations eq 0`, and now
      passes on device. This is ordering-sequence evidence, explicitly NOT pixel occlusion (that
      remains C04). No ordering-framework change, no suppression permission, default-off gate
      unchanged. Tests: `EffectRenderBandTest`, `TransientEffectContainerPatchesTest`,
      `NativeRenderBridgeTest`.

- [x] NRO-04 C04a (render-order probe `phaseCounts`): capability-only aggregate so a device scenario
      can assert which render phase families are present in one frame without relying on the bounded
      `items[]` list. `RenderHost.probeRenderOrder()` now emits `renderOrder.phaseCounts`, a map with
      one Integer entry for EVERY `RenderPhase` value keyed by `phase.name()`, counting ordered
      targets in that phase over ALL ordered targets (not just the enumerated/capped items); a
      zero-target phase maps to 0 and is never missing/null. It is a `LinkedHashMap` populated by
      iterating the `RenderPhase` enum constants in declaration order, so the probe output is
      deterministic. Read-only diagnostic: ordering, submission, and state are unchanged; all
      existing `renderOrder` keys keep byte-identical behavior. Guides remain an overlay and are NOT
      render-plan targets, so `VERIFY_GUIDES` counts 0. Enables the C04 cross-family same-frame
      evidence scenario (device YAML is C04b). Tests: `RenderHostProbeBoundTest`.

- [x] NRO-04 C04b (cross-family same-frame render-order device scenario): new
      `tests/ui-scenarios/device/d1_render_order_mixed.yaml` (sibling of
      `d1_render_zorder_contract.yaml`) boots a FULL combat frame and asserts cross-family
      render-ORDER / phase evidence in ONE frame using the C04a aggregate
      `render.renderOrder.phaseCounts` (option A: the PLAN families NATIVE_RETAINED + C2_CONTENT
      present, C1_CONTENT key-presence only). It asserts `phaseCounts.VERIFY_GUIDES == 0` (guides
      are an overlay, never a plan target) and `phaseCounts.ART_EFFECTS == 0` to DOCUMENT the
      boundary explicitly — the ART effect family is drawn through the separate ArtRenderFrame /
      overlay path and is NOT a `RenderHost` target, so it never appears in `phaseCounts`; real
      effect-family presence is proven separately via
      `backend.renderPlan.nativeRender.effectBands.claimed.effectListBehind/effectListFront gte 1`
      after `art claim on` + `art claim spawn flareL 6` / `fire 6`. `render.renderOrder.monotonic
      == true` and `duplicateStableKeys == []` are re-asserted with claimed VFX present (plus
      `effectBands.orderViolations == 0`), and guides are proven default-off
      (`configuredMode "off"` / `submissionStatus disabled`), ready/supported when `art verify mode
      guides`, then fully restored to `"off"`. Visual evidence uses the supported device
      `screenshot: true` step at the default-off frame and after guides ON. This is
      ORDER-PROBE + screenshot evidence, explicitly NOT pixel-occlusion parity. Reuses C04a
      `phaseCounts`; no ordering-framework change, no suppression permission, default-off gate
      unchanged. Offline loader test: `tools/art-verify/tests/test_render_order_mixed_scenario.py`.

- [x] NRO-04 C03 (same-layer per-band ORDERED observation sequence + interleave witness): C02 proves
      the within-pass band ordinal is monotonic; C03 proves the SAME native band's observations are
      recorded in native TRAVERSAL ORDER and that distinct instances never coalesce. The existing
      `NativeRenderBridge` band state is extended (same lock, same observation-only, never-throws
      discipline) with, for the CURRENT pass only, a bounded ordered tail (capacity 16) per real band
      (`effectListBehind`/`effectListFront`/`topLevelFront`) of `{claimed, class}` entries appended on
      every observation. A repeated observation of the SAME instance is never coalesced by class name:
      every observation appends its own entry, so two distinct `LightFlareLEffect` instances appear as
      two entries (the sequence is per-observation, not per-instance-dedup). A pass change (explicit
      `beginEffectRenderPass()` OR a detected new pass id) clears every tail, because a tail
      represents exactly one native `AbstractDungeon.render` traversal; beyond capacity the oldest
      entry is dropped (bounded memory) and a per-band `sequenceTruncated` boolean is set.
      `interleave` counts adjacent entries in the recorded (bounded) tail whose `claimed` flag
      differs (a claimed<->native adjacency) and is documented as a witness over the bounded tail, not
      an exhaustive transition count. `EffectRenderBand.Band#UNKNOWN` never creates a sequence entry.
      The probe adds exactly `nativeRender.effectBands.sequence` (bandName -> ordered list of
      `{claimed:Boolean, class:String}`, empty list when none, stable ordering),
      `effectBands.sequenceTruncated` (bandName -> Boolean) and `effectBands.interleave` (bandName ->
      Integer); every existing key is kept. `clearEffectBandsForTests()`/`resetForTests()` clear all
      three. DEVICE: `tests/ui-scenarios/device/d1_render_zorder_contract.yaml` (extended in place,
      both gate-OFF and gate-ON phases) spawns the witnesses, polls for a non-empty per-band sequence,
      then asserts sequence key presence (`sequence.effectListBehind[0].class exists`),
      `sequenceTruncated.effectListBehind exists`, `interleave.effectListBehind gte 0`, and
      `orderViolations eq 0` (no exact list length/tail contents on device). This is ordering-sequence
      evidence, explicitly NOT pixel occlusion (that remains C04). No plan/ordering-framework change,
      no suppression permission, default-off gate unchanged. Tests: `NativeRenderBridgeTest`.

- [x] **B06b map-screen effect-loop observation boundary**: HONEST FINDING — there is NO MAP-screen
        `AbstractGameEffect.render(SpriteBatch)` call site to instrument. `javap -c -p` on the
        shipped 1.0 jar finds ZERO `invokevirtual
        com/megacrit/cardcrawl/vfx/AbstractGameEffect.render:(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;)V`
        in `com.megacrit.cardcrawl.map.MapRoomNode` or `com.megacrit.cardcrawl.screens.DungeonMapScreen`
        (nor anywhere in the `com.megacrit.cardcrawl.map` package). This scope is deliberately NOT
        universal: several OTHER `com.megacrit.cardcrawl.screens` classes DO invoke
        `AbstractGameEffect.render(SpriteBatch)` — `CombatRewardScreen.render` (whose `RewardItem.effects`
        loop is the B05b slice), `VictoryScreen.render`, `DoorUnlockScreen.render`,
        `options.OptionsPanel.render`, and `select.BossRelicSelectScreen.render`. Those non-map screen
        effect-render sites are OUT OF SCOPE for B06b and are NOT covered by this slice (B05b covers
        only the reward loop; the others remain un-instrumented and fail open to native). The decompiled
        reference confirms `MapRoomNode.java:224,263` add `new MapCircleEffect(...)` to
        `AbstractDungeon.topLevelEffects`, not to any map-screen-scoped list. That loop is inside
        `AbstractDungeon.render(SpriteBatch)`, which
        `TransientEffectContainerPatches.ObserveContainerEffectRenders` already instruments (its three
        sites are the two `effectList` loops and the `topLevelEffects` loop, band `topLevelFront` /
        native line `2802`). Map-screen effects (`MapCircleEffect`, and `FadeWipeParticle` when not
        FAST_MODE) are therefore ALREADY observed and claimable through the existing container seam;
        B06's earlier "PRODUCTION REACH (B06 boundary)" note ("the seam's only observer ... does NOT
        instrument the map effect loop") was WRONG and is corrected here. NO patch was added
        (a redundant/inert instrument is explicitly rejected). This slice adds: (1) a
        NON-TAUTOLOGICAL regression test grounded in real code — `TransientEffectContainerPatchesTest`
        now exercises the extracted `ObserveContainerEffectRenders.isNativeEffectRenderCall`
        predicate (matches only the native `render:(SpriteBatch)V` descriptor, rejects the
        `MapCircleEffect` owner, other methods, and the 3-arg overload) and proves an observation at
        `EffectRenderBand.LINE_TOP_LEVEL_FRONT` lands in the `topLevelFront` native band, plus a
        `VfxClaimPolicy.supports(MapCircleEffect)` / `supportedClasses().contains(MAP_CIRCLE)` /
        `VfxDrawGeometry.kindFor -> Kind.MAP_CIRCLE` seam-claim assertion (the B06 mapping is
        unchanged); and (2) a D1 scenario
        [`tests/ui-scenarios/device/d1_map_effect_observation.yaml`](../tests/ui-scenarios/device/d1_map_effect_observation.yaml)
        that opens the map (`art present map on`, the lab-reachable path), turns the claim gate on,
        captures baselines, selects a floor-0 node (`art op map first`, which adds the
        `topLevelEffects` map effects) and asserts a STRICT delta on
        `backend.renderPlan.aura.draws` plus a strict delta on the claimed
        `backend.renderPlan.nativeRender.effectBands.claimed.topLevelFront` band (the map effect's
        native band), with a matching offline loader test
        (`tools/art-verify/tests/test_map_effect_observation_scenario.py`). DEVICE EVIDENCE: the
        scenario was run live via `python3 tools/art-verify/run.py` (the runner elected device mode
        because the D1 env keys were set and the connector daemon was reachable) and PASSED all 26
        steps — a real confirmation, not a stub: `backend.renderPlan.aura.draws` advanced
        303869 -> 307204 and the
        claimed `backend.renderPlan.nativeRender.effectBands.claimed.topLevelFront` band advanced
        0 -> 18 after `art op map first`, while the map was open under `art present map on`. The
        reward-loop analogy stands: B05b instrumented a genuine separate call site
        (`RewardItem.render`, offset 992) because the reward loop is NOT `AbstractDungeon.render`;
        the map loop IS `AbstractDungeon.render`, so no instrument is needed.

- [ ] Design and implement deterministic ART render z-order extraction/submission, preserving ECS
      system order and defining the native boundary for visual-verification backgrounds. See
      [`docs/design/render-z-order.md`](design/render-z-order.md).
      Ordering model, plan sorting, host submission order, VFX `ART_EFFECTS` submission, resolved
      `renderOrder` probe diagnostics, the reported `unsupported` pre-native background
      capability, `art verify status|mode` diagnostics, and narrowing native filter scopes
      (`filterScopes` probe, fail-open cleanup, offline tests) are shipped (`b4b59ac`..`85846aa`).
      Background renderer, verified pre-native/filtered boundary, and D1 background scene are
      shipped/verified; `scripts/art-lab background verify-isolate` runs the staged background and
      mounted/FULL isolate scenario. Remaining: per-family native pixel replacement beyond
      `sts1.room.background`, complete UI-family coverage, and pixel-parity evidence (design in
      `docs/design/render-z-order.md` section 12). D1 probe scenarios are shipped; the
       "legacy adapter" item is resolved as doc-only (no adapter code ever existed).

- [ ] Close strict `background-only` coverage: the command and probe gate now block all covered
      surface/skeleton/effect bridge entries and all ART post-present/C1 output, while preserving
      the pre-native combat-room background. D1 `background verify-only` intentionally reports
      `unsupported/uncovered` for native world/foreground entry points not yet behind a global
      pre-native gate; do not claim pure-background success until those counters are zero.

- [x] NRO-04 E01 (precise uncovered-owner attribution): the strict `background-only`
      `BackgroundOnlyGate` now feeds `recordUncovered` into a bounded `LinkedHashMap<String,Long>`
      exposed as `backend.verify.backgroundOnly.uncoveredByOwner` (label -> count, deterministic
      insertion order), plus `uncoveredDistinct` (map size) and `uncoveredOverflow` (Long) when the
      distinct-label cap (32) is exceeded. The coarse `uncovered` counter, `lastReason`, and gate
      semantics are unchanged. Honest scope: `blockedForeground`/`unsupported` remain counters for
      now (later E slices); the labels today are the bounded call-site reasons
      (`art.post_render` from `StageHost`, `art.surface_renderer` from `Sts1SurfaceRenderer`).
      Attribution is the first E-stage step toward precise uncovered-owner reporting.

- [x] NRO-04 E02 (precise blocked-owner attribution): the strict `background-only`
      `BackgroundOnlyGate` now feeds `recordBlocked` into a bounded `LinkedHashMap<String,Long>`
      exposed as `backend.verify.backgroundOnly.blockedByOwner` (label -> count, deterministic
      insertion order, distinct-label cap 32), plus `blockedDistinct` (map size) and
      `blockedOverflow` (Long) when the cap is exceeded; `blockedByOwner`/`blockedOverflow` clear in
      the same reset path as the E01 uncovered attribution (deactivation / recovery / test reset).
      The coarse `blockedForeground` counter and gate semantics (what gets blocked) are unchanged.
      Names the already-blocked world/foreground owners via the unchanged call-site labels
      (`surface:<family>` from `NativeRenderBridge` ~:176, `skeleton:<owner>` ~:418,
      `effect:<class>` ~:650). Honest scope: this is attribution only — no new native patch; the
      world/foreground owners are already blocked by the default-block design, so this slice only
      makes the single-owner attribution explicit. Per-owner active isolation of a specific
      world/foreground entry remains a later option.

- [x] NRO-04 E03 (precise unsupported-owner attribution): the strict `background-only`
      `BackgroundOnlyGate` now feeds `recordUnsupported` into a bounded `LinkedHashMap<String,Long>`
      exposed as `backend.verify.backgroundOnly.unsupportedByOwner` (label -> count, deterministic
      insertion order, distinct-label cap 32), plus `unsupportedDistinct` (map size) and
      `unsupportedOverflow` (Long) when the cap is exceeded; `unsupportedByOwner`/`unsupportedOverflow`
      clear in the same reset path as the E01/E02 attribution (deactivation / recovery / test reset).
      The coarse `unsupported` counter and gate semantics are unchanged. This completes the THREE
      bounded per-owner attribution maps — `uncoveredByOwner` (E01), `blockedByOwner` (E02), and
      `unsupportedByOwner` (E03) — with their `*Distinct`/`*Overflow` keys, covering the
      black-screen/overlay/text/unknown entry paths via the unchanged call-site labels
      (`surface:unknown_owner`, `surface:bridge_error`, `skeleton:unclaimed`,
      `skeleton:renderer_unavailable`, `effect:identity_unavailable`). Honest scope: attribution only —
      no new native patch. Per-owner ACTIVE isolation of a specific overlay remains a later option.

- [x] NRO-04 E04 (precise post-render uncovered marker + strict ART-output acceptance): in strict
      `background-only` mode the `StageHost.receivePostRender` uncovered marker is now recorded ONLY
      when genuine **ART-OWNED** output is pending. The predicate is
      `hasStage || hasPresentDraw || hasVfxDraw || hasArtOwnedHostOutput()`, where `hasStage` is the
      C1 scene2d stage, `hasPresentDraw` is a non-empty `Sts1RenderPipeline.plan().drawOrder()`
      (ART plan entries), `hasVfxDraw` is `VfxSts1Runtime.hasLiveDraws()`, and
      `hasArtOwnedHostOutput()` scans `RenderHost.listTargetIds()` and counts a target only when its
      `RenderPhase` is not `NATIVE_RETAINED`, or when it carries an enabled ART effect binding. It
      also excludes native-only entity-present slots: an `ENTITY_SLOT` target (phase
      `ENTITY_CONTENT`, e.g. `c2:entity:sts1.native.skeleton/<key>`) counts only when
      `FullPresentMode.skeletonLevel().allowsFullPresent()` (entity/skeleton pixels are ART-drawn) or
      when the pure `EntityDrawPath.buildFromPresent()` description for that `c2:entity:<slotId>`
      resolves a real art or icon resource (`artFound || iconFound`); a slot with an empty ART
      resource under an inactive entity surface is native anchor chrome ART does not draw. The
      previous `hasFx` term (`bindingCount() > 0 || targetCount() > 0`) was removed because on device
      with no mounted ART overlay the host still holds ~350 RETAINED NATIVE scene-effect targets
      (`native:<stableKey>` at phase `NATIVE_RETAINED`, `bindingCount == 0`), which are native pixels
      ART does not draw — "any render target exists" is NOT "ART output pending". The marker is
      computed BEFORE the background-only early return; the early return (no ART pixels while
      background-only is active) is unchanged, the unconditional draw-guard path when background-only
      is INACTIVE is byte-for-byte unchanged, and a genuine ART leak (plan entry / VFX / C1 widget /
      enabled ART binding) still records `art.post_render`.
      `BackgroundOnlyGate.probeSlice()` also exposes a Boolean `strictAccepted` =
      `uncovered == 0 && uncoveredOverflow == 0 && blockedOverflow == 0` (a background-only-specific
      predicate over the `background-only` gate counters `uncovered`/`uncoveredOverflow`/
      `blockedOverflow`, not the `NativeRenderBridge.strictReport().accepted` predicate, which is
      computed over different counters); every existing key is unchanged. The device
      acceptance `d1_verify_background_only.yaml` now asserts `uncovered eq 0` +
      `strictAccepted eq true` while `art present combat observe` keeps `projection.scene == combat`
      published (observe mounts the surfaces without any ART plan draw entries; blocked-foreground
      evidence still comes from the patched native combat entry points and scene effects), replacing
      the old `uncovered gte 1`/`uncoveredDistinct gte 1`.
      HONEST SCOPE: this is ART-OUTPUT strictness only (zero ART submission / zero ART pass leaks).
      It does NOT claim native world/foreground residue (player, monster, hand cards, terrain) is
      suppressed — those are still drawn natively and NOT intercepted, and remain a separate,
      larger pending slice. Do not read `strictAccepted == true` as "strict zero native residue".

- [x] NRO-04 E05 (explicit, honestly scoped four-act background coverage): the background family
      `sts1.room.background` is supplied act-agnostically by `BackgroundRenderGate` (full-screen
      checker/grid/solid drawn with no act/room/scene argument), and `BackgroundRenderPatches`
      patch-targets all four concrete scene overrides of `AbstractScene.renderCombatRoomBg`
      (`TheBottomScene`, `TheCityScene`, `TheBeyondScene`, `TheEndingScene`). The coverage is now
      EXPLICIT: `BackgroundRenderGate.PATCHED_SCENES` is a STATIC list of the four patched scene
      simple names and is exposed as the probe key `backend.verify.background.patchedScenes` (with
      all prior keys unchanged); a focused test reflects each patch class's `@SpirePatch(clz=...)`
      and fails if the target set is not EXACTLY those four, and a draw test proves the same
      full-screen quad count for every patched scene (act-agnostic). HONEST SCOPE: this records patch
      TARGET coverage, NOT device verification. D1 device evidence is act-1/Bottom ONLY via
      `d1_verify_background_only.yaml` (now asserting the `patchedScenes` key exists and contains
      `TheBottomScene`); City/Beyond/Ending are patched but NOT device-verified because there is no
      lab act-jump — an open gap, not a claim. No per-act "verified" flag was faked.

- [x] Keep Harness `result.json` payloads on disk in `scripts/art-lab` so oversized probe status
  lines cannot exceed the process argument limit.
- [x] OpenCode `junit-test` + `local-env` + `opencode.json`
- [x] AGENTS subagent table + delegation order
- [x] `docs/development` testing + deploy notes
- [x] Expand pure API JUnit beyond smoke
- [x] Optional `@android-deploy-jar` (default D1)
- [x] D1/D2 serial keys documented for optional device UI smoke
- [x] `tools/art-verify` scaffold + offline unittest + `@art-verify` agent
- [x] Design: `ui-ops-probe.md` + `ui-layer-verification.md` + dual-track roadmap 6–8
- [x] Optional `@android-arthas` + `android-arthas.md` + `ART_ARTHAS_PORT` (not a default gate)
- [x] OpenCode `refacter` supervision skill + read-only `@art-reviewer` + persistent project ledgers

## Product roadmap (from dual-track)

### STS2 Godot VFX import

- [x] Design fixed: restricted `.tscn` conversion emits a lossless parsed bundle plus selective
      typed ART VFX data, projects into `PresentationWorld`, and uses stateless CPU particle
      systems with native STS1 overlay only.
      See [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [x] Implement converter schema, diagnostics, and one real basic-particle `.tscn` conversion.
      Offline fixtures and the developer-local `vfx_smoke_puff.tscn` production test cover typed
      curves, gradients, flipbook data, resource hashes, diagnostics, and deterministic output.
- [ ] Implement Java manifest loading, ECS instantiation, CPU particle systems, lifecycle cleanup,
      and libGDX render projection.
- [x] F01: STS2 VFX manifest `diagnostics` pointer is now safely parsed via the shared path guard
      and exposed on the bundle definition; an absent `diagnostics` field is fail-open with an empty
      `diagnosticsPath` and does not throw. This is a gap-fill of the existing loader, NOT a new
      loader. The deeper STS2 runtime gaps — `.tpsheet`, sub-emitter, turbulence, restricted
      shader/bake fallback, and Spine 4.2 parity — remain open per
      [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [x] F02: single-emitter lifecycle is now explicit and harness-assertable: create -> fixed-step/seeded
      update -> stop (no respawn; in-flight particles still age out and the graph still cleans up)
      -> restart (deterministic re-emission from the unchanged seed) -> cleanup, implemented in the
      pure `ParticleSpawnSystem`/`VfxLifecycleSystem` with `VfxEmitterStateComponent.stopped` and
      `VfxEmitterControl`. A read-only `backend.vfx` probe slice
      (`{status, scene, liveRoots, draws, completed, error, roots:[{sceneId, epoch, emitterCount,
      stoppedCount, liveParticleCount}]}`) joins the existing `statusLine`/`hasLiveDraws` data and is
      fail-open empty on error. `tests/ui-scenarios/device/d1_sts2_vfx_basic.yaml` covers
      load -> live -> clear -> panic/recovery without disturbing native. F02 gate fix: `art vfx`
      (`status|load|clear`) now also publishes the structured `ART_COMMAND {sequence,status,
      command,message}` envelope that other `art` commands emit, so the art-verify runner resolves
      command freshness immediately instead of stalling on its 3s fallback; the raw `ART_VFX` line
      is unchanged for device probes. The scenario was aligned to the one-shot ~2s smoke-puff
      bundle: a short-timeout (3000ms) wait on `backend.vfx.liveRoots gte 1` reuses its captured
      probe for `status=loaded`/`completed=false`/`draws gte 1` with no intervening `wait_ms` hold,
      then `art vfx clear` asserts `status=clear`/`liveRoots=0`. The deeper STS2 runtime gaps
      — `.tpsheet`, sub-emitter, turbulence, restricted shader/bake fallback, and Spine 4.2 parity —
      remain open per [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [x] F03: the basic particle render projection's per-particle composition is now unit-pinned through
      the ECS emitter -> `ParticleRenderProjectionSystem` -> `VfxRenderFrame.payloadEntry` chain:
      projected `scaleX/Y == composed.scale * particle.scale`, `alpha == node.color.a *
      particle.color.a * particle.alpha`, `rotation == composed.rot + particle.rot`, and the payload's
      blend mode comes from the ECS emitter's `blendMode` field (all with non-identity inputs), while
      order/`stableKey` stay unchanged. `tests/ui-scenarios/device/d1_sts2_vfx_basic.yaml` adds a
      `screenshot: true` inside the live window (immediately after the `liveRoots`/`draws` probe and
      before `art vfx clear`) to capture the smoke-puff while live; the final native-undisturbed
      screenshot after clear/panic is kept. The live capture is timing-sensitive by design (a ~2s
      one-shot bundle) and its puff visibility is advisory — the probe counters remain the authority.
      F02 already provides the end-to-end draw proof; this does NOT claim pixel parity. The deeper
      STS2 runtime gaps — `.tpsheet`, sub-emitter, turbulence, restricted shader/bake fallback, and
      Spine 4.2 parity — remain open per
      [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [x] F04: the flipbook frame-index and atlas-mapping boundary coverage is now unit-pinned through the
      ECS emitter -> `ParticleRenderProjectionSystem` -> `VfxRenderFrame.payloadEntry` chain and the
      STS1 overlay integer source rect: age 0 (and negative age) -> frame 0 with the `(0,0)` cell; an
      intermediate frame -> exact frame index + exact `sourceX/Y/W/H`; a NON-loop overshoot
      (`age*speed >= frameCount`) -> clamped to the LAST frame `frameCount-1` at the final cell; a
      LOOP wrap (`age*speed == frameCount` and `2*frameCount+1`) -> frame 0 and frame 1; and a very
      large `dt` keeps the frame in `[0, frameCount-1]` with the normalized src rect inside `[0,1]`
      and the overlay integer rect inside the texture. The mapping is derived from the bundle's
      hFrames/vFrames; the separate `.tpsheet` region-materialization gap is NOT addressed here and
      remains open. Focused no-GL JUnit only; no per-frame D1 visual compare is claimed (the
      smoke-puff scene's flipbook emitter is not UV-asserted by the scenario), so the D1 per-frame
      visual compare is a documented gap, not a faked assertion. The deeper STS2 runtime gaps —
      `.tpsheet`, sub-emitter, turbulence, restricted shader/bake fallback, and Spine 4.2 parity —
      remain open per [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [ ] Expand support incrementally: `.tpsheet`, flipbook parity, sub-emitters, turbulence,
      restricted shaders, then baked fallback.

- [x] F07: a single restricted VFX material/blend SUPPORT MATRIX with explicit reject + fail-open
      fallback now exists in pure `VfxMaterialSupport`. It bounds the KNOWN STS2 blend names
      (`MIX`, `ADD`, `SUB`, `MUL`, `PREMULT_ALPHA`): `MIX`/`ADD`/`MUL`/`PREMULT_ALPHA` are
      SUPPORTED (canonicalized case-insensitively) and `SUB` is explicitly UNSUPPORTED. The pure
      `resolve(name)` never throws and returns `{requested, resolved, supported, fallback}`:
      unknown/blank/null and `SUB` resolve to the single fallback `MIX` (`supported=false`,
      `fallback=true`); supported names resolve to their canonical form (`fallback=false`). The
      `Sts1VfxOverlayRenderer` blend lookup is routed through the matrix as the single blend
      authority — the resulting GL functions for every supported name are unchanged and the same
      `MIX` fallback applies to everything else — so no pixels change and no shader path is added.
      Rejected names are counted in a bounded (16-key) diagnostic map with an overflow counter, and
      `VfxMaterialSupport.probeSlice()` (`{known, supported, unsupported, rejected, rejectedOverflow}`)
      is exposed read-only, fail-open, under `backend.vfx.materials`. Unit-verified only (no GL) in
      `VfxMaterialSupportTest` + `Sts1VfxOverlayRendererTest`. **Honest gap (OPEN):** a general
      shader system and an STS2-equivalent shader/material bake are NOT achievable here and are NOT
      attempted; `ShaderMaterial`/`.gdshader` remain `unsupported-known` in IR (no typed field, no
      GL shader path). No D1 shader scenario is fabricated, and the D1 held out.

- [x] F05: restricted sub-emitter arming is implemented as a deliberately bounded model, NOT full
      STS2 sub-emitter parity. The new optional `VfxNodeDefinition.emissionTrigger` string recognizes
      exactly ONE value, `onParentComplete` (a dormant child emitter stays stopped until its PARENT
      emitter has completed AND drained); absent/unknown/non-string values fail open to empty and
      never throw. The pure `VfxSubEmitterSystem` (`VfxSystems` EFFECTS phase, before spawn/integrate)
      arms each eligible trigger child AT MOST ONCE by clearing its initial dormant
      `VfxEmitterStateComponent.stopped` (deterministic one-shot restart) and removing its
      `VfxSubEmitterComponent` once-only marker, so `ParticleSpawnSystem` emits it the same tick.
      Bounds (never throwing, just not arming): depth is limited to ONE level (a trigger child whose
      parent is itself a trigger child is seeded permanently dormant with no marker) and a fixed
      `MAX_SUB_EMITTERS_PER_PARENT = 4` trigger children per parent are seeded; existing total
      node/particle caps are unchanged. Unit-verified only (no GL) in `VfxSubEmitterTest` +
      `VfxManifestLoaderTest`. **D1 GAP (honest): there is NO on-device sub-emitter bundle/scenario**,
      so this is unit-verified only; a future D1 sub-emitter check needs a converter-produced
      sub-emitter bundle. The deeper STS2 runtime gaps — `.tpsheet`, full sub-emitter parity,
      turbulence, restricted shader/bake fallback, and Spine 4.2 parity — remain open per
      [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).
- [x] F06: optional restricted turbulence acceleration added to the pure CPU integrator as a
      deliberately bounded, seed-deterministic APPROXIMATION — explicitly NOT STS2 runtime-equivalent.
      `VfxEmitterComponent.turbulenceStrength` (float, default 0f) is immutable, clamped to a finite
      `[0, MAX_TURBULENCE_STRENGTH=100000]` range (NaN -> 0), and carried through a new
      `VfxEmitterComponent.from(definition, strength)` overload; the existing single-arg `from(...)`
      keeps 0, so no existing bundle/test changes. When positive, `ParticleIntegrateSystem` adds a
      rotating sinusoidal (curl-like) acceleration derived from `randomSeed` + `spawnIndex` + `age`
      (same splitmix hash channels as `ParticleSpawnSystem`) and clamped per-axis velocity/position to
      finite bounds; when 0 the integration is BYTE-IDENTICAL to the gravity-only Euler step. The term
      is physics-only and does NOT enter the render projection. Unit-verified only (no GL) in
      `VfxTurbulenceTest` + the unchanged `VfxRuntimeTest`. **D1 GAP (honest): the converter still
      routes `turbulence_*` to `unsupported-known` diagnostics and emits NO typed turbulence field,
      so there is NO turbulence bundle/scenario**; a future D1 turbulence check needs a
      converter-produced turbulence bundle. The deeper STS2 runtime gaps — `.tpsheet`, sub-emitter,
      STS2-equivalent turbulence, restricted shader/bake fallback, and Spine 4.2 parity — remain open
      per [`docs/design/sts2-vfx-ecs-conversion.md`](design/sts2-vfx-ecs-conversion.md).

- [x] F08: frozen-pose GL DETERMINISM on D1 via identical re-rendered vertex signature, NOT
      native-reference pixel parity. `tests/ui-scenarios/device/d1_spine42_screenshot.yaml` keeps
      the full load -> `art present skeleton on` -> `art skeleton dev load/play/seek/freeze`
      sequence and every prior panel/skeleton assert (providerId `spine42`, liveCount 1,
      lastError "", present[4] id/mounted, `render.targetsById.c2_surface_sts1_skeleton.enabled`),
      plus fixed-pose evidence from confirmed probe keys (`drawEvidence.handle=d1_ironclad`,
      `.kind=standalone-art`, `.path=renderAll->provider.render`, `live.d1_ironclad.currentAnimation=
      idle_loop`). The intermittent `drawEvidence.count == 0` is now handled by a bounded
      `wait_probe` (timeout 10000ms / interval 500ms) that retries until `count gte 1`. The gate is
      frozen-pose determinism: `capture` the render-path deformed-vertex signature
      (`backend.skeleton.drawEvidence.vertexSignature`, from
      `Sts1Spine42Provider.lastRenderVertexSignature`) as `sig1`, assert it `exists`, wait 250ms with
      NO unfreeze, `probe: all` to refresh, then require the refreshed value `eq_var: sig1`. The two
      `screenshot: true` captures remain as advisory visual-reviewer evidence only. The
      `ART_SPINE42_REFERENCE_PNG` / `ART_SPINE42_CROP` requires are removed. **The earlier pixel
      gate (`compare_screenshot: {against: previous_capture}`, crop `[860,330,220,300]`) was removed
      because that crop bleeds ~20% animating NATIVE background (player/Cultist idle, torch, hand):
      the comparable run measured 20.0% / 16.6% crop change (13200 / 10975 px) far over the intended
      limits, while the ART pose itself was provably stable — `vertexSignature` was BYTE-IDENTICAL
      across both captures (`de235afb6f46c550`; `drawEvidence.count` 675 both times).** The pixel
      crop cannot isolate the skeleton from the live scene, so it is not restored. **Native-reference
      pixel parity is NOT achieved and remains OPEN, developer-gated on `ART_SPINE42_REFERENCE_PNG`**:
      the prior recorded native-reference run failed at 242818 px / 0.117 ratio vs a 0.01 limit
      (documented gap); attachment/clip/blend parity is likewise OPEN.

- [x] F09: Spine 4.2 animation & recovery consistency on D1 (state/determinism only, no pixel gate).
      PROBE RESILIENCE DEFECT FIXED: a loaded rig with a valid `SkeletonHandle` but no AnimationState
      TrackEntry (`Sts1Spine42Provider.invokeTrack` called `command.trackTime`/`command.animationEnd`
      on `probeSlice()`) previously threw `IllegalStateException`, `ProbePublisher.publishFull()`
      swallowed it, and `art probe` published NOTHING (no fresh `ART_PROBE`) so `wait_probe` timed
      out. Fix is two-layer: (1) provider track-control calls now FAIL OPEN (`trackTime` -> `0f`,
      `animationEnd` -> `false`/`0`) when `track 0 has no TrackEntry`; (2) `Sts1SkeletonBridge
      .probeSlice()` assembles each handle's optional provider queries behind per-handle guards so
      one handle's exception cannot abort the `drawEvidence`/`live` map — the probe still publishes
      with all handles present. `tests/ui-scenarios/device/d1_spine42_animation.yaml` keeps the load
      -> `play idle_loop` -> `play attack` -> `bone root` smoke and the animation switch, removes the
      TAUTOLOGICAL loop-persistence assert (`seek 10.0` -> `currentAnimation eq idle_loop`; `art
      skeleton dev play` hardcodes `loop=false` and `currentAnimation` is track-time-independent, so
      it cannot distinguish a looping from an ended clip) — LOOP PERSISTENCE IS NOT VERIFIABLE via
      the dev play command (documented gap in the scenario), and keeps RESUME DETERMINISM (`seek 0.0`
      + `freeze`, bounded `wait_probe` `drawEvidence.count gte 1`, `capture` `backend.skeleton
      .drawEvidence.vertexSignature` as `frozenA`, then `unfreeze` + `seek 0.0` + `freeze` again with
      a second bounded wait and `assert {path: backend.skeleton.drawEvidence.vertexSignature, eq_var:
      frozenA}` — the same frozen time resumes to an IDENTICAL deformed-vertex signature).
      `tests/ui-scenarios/device/d1_spine42_lifecycle.yaml` (34 steps, unchanged) keeps load -> stop
      -> panic -> clear-panic and adds: UNLOAD/RELOAD (stop -> `liveCount 0` -> load -> bounded
      `wait_probe liveCount eq 1`, `providerId eq spine42`, `lastError eq ""`); PANIC RECOVERY +
      RELOAD (panic -> `liveCount 0` -> clear-panic -> load -> bounded `liveCount eq 1`, clean
      provider); HOST REBUILD (`art lab host-recreate` -> `PresentSafety.requestHostRecreation`;
      bounded `wait_probe backend.safety.recreationCount gte 1`, then the skeleton bridge's
      `onHostRecreated()` drops developer handles so bounded `liveCount eq 0`, `lastError eq ""`,
      then clean re-load, then `stop` -> `liveCount 0`). Recovery (unload/reload, panic+clear+reload,
      `art lab host-recreate` with `backend.safety.recreationCount`) is verified. All lag-prone state
      uses bounded `wait_probe`, not bare asserts. **Native-reference pixel parity remains OPEN /
      developer-gated on `ART_SPINE42_REFERENCE_PNG` (47.35)**; no `compare_screenshot` pixel gate
      was added.

### 46. Traditional ECS convergence

Design: [`docs/design/traditional-ecs.md`](design/traditional-ecs.md). Entity IDs only;
components are data only; systems are stateless and own all ART interaction handling.

**Current checkpoint (2026-08-19):** Traditional ECS convergence is complete. ECS is the
authority for C1/C2 state, native input and intent lifecycle records, render-plan inputs,
Skeleton/EntityPresent state, diagnostic projections, and active pack presentation
contributions. Production phases run through the fixed `PresentationSchedule`; render and host
execution boundaries are explicit; compatibility APIs derive ECS views; and only documented
non-authoritative host/callback/resource caches remain. The persistent closure evidence is in the
traditional-ECS refacter ledger, including `TE-01` through `TE-27` and their review/test gates.

Persistent recursive review, before/after evidence, findings, and verification progress:
[`docs/refacter/traditional-ecs/`](refacter/traditional-ecs/).

- [x] 46.0 Define strict ECS contract; add data-only entity identity, component query, and
  ordered stateless system pipeline with JUnit
- [x] 46.1 Registered presentation scopes share one ART world; scope close/reset now destroys only
  owned entities while preserving the registered context identity
- [x] 46.2 Materialize C1 declarations through ECS systems; control current values now live on
  ECS entities, control normalization runs during runtime ticks, WidgetSession is constructed as an immutable
  declaration/index compatibility data, C1 visual entities include ECS host-binding keys, and
  UiOps/probe/scene2d widgets/UiActions/FX read ECS values; WindowManager no longer owns a layout
  root map. C1 target/effect projection now consumes ECS frames only and StageHost reconciles its
  actor cache from ECS host bindings; context/entity lifecycle queries resolve registered ECS scope
  data, while signal callback objects remain a disposable host cache
- [x] 46.3 Materialize STS observations and C2 surfaces through ECS systems; card projections now
  create/update/destroy shared-world entities and data-only card components, with frame lifecycle
  metadata, drag interaction metadata, and immutable frame snapshots on a projection root entity;
  card lookup, listing, counts, and cleanup now resolve through ECS card components, while the
  CardEntity compatibility view is derived on demand; C2 surface mount state and Entity lookup are
  now ECS-derived; native template facades no longer retain local active flags and template probe
  reads use ECS bind/pin/end-turn data
- [x] 46.4 Route every native input/intercept through ECS input, action, intent, and result data;
  C2 surface submissions now record data-only action/intent/result components before and after the
  existing SignalBus compatibility executor, and the STS1 combat router records native input plus
  intercept decisions per surface ECS entity for hand, controls, and executor paths; native map,
  event, select, and end-turn hooks now record their unbound, emitted, disabled, and rejected paths
  through the same model. Intent lifecycle transitions are centralized in
  `NativeIntentLifecycleSystem`; `PresentSurfaces.submit()` writes only ECS request data and uses
  the fixed schedule-owned `SurfaceIntentExecutionSystem` for its synchronous compatibility result.
- [x] 46.5 Derive all rendering/effects/skeleton host caches from ECS data; animation playback,
  pulse envelopes, and skeleton identity/snapshot state are ECS authoritative, while RenderHost
  C1/C2 item target caches are ECS-frame-derived; surface/full-frame and host resource paths now
  consume ECS render-state components; D1 `d1_full_present_combat_ready`, EntityPresent draw/
  recreation/cleanup, and Spine34 native takeover/recreation/cleanup pass. `SkeletonPresentationFrames`
  now delivers native snapshots into ECS-backed provider bindings before native claim reconciliation.
  Animation playback, effect pulses, coalesced render projection,
  the render clock, and the host backend tick now advance through stateless production ECS systems; `RenderHost.tick` and
  immutable-plan rebuilds are render-package-only, while lifecycle and STS filtered projections use
  explicit `RenderProjectionQueue` APIs.
- [x] 46.6 Derive Probe/API compatibility views from ECS only and remove legacy stores; C1 probe,
  lifecycle queries, render-state projection, and business confirmation are ECS-derived, while
  callback/resource caches remain explicit host boundaries. UiProbe, UiInspect, console/probe
  reads, and PresentSurfaces registry recreation have been audited; remaining cleanup is limited
  to concrete duplicate stores discovered by subsequent ownership audits.

#### 46.x completion checklist

- [x] 46.1.1 One ART World; context-owned entity index; scope close/reset isolation tests
- [x] 46.2.1 C1 control values and normalization are ECS authoritative
- [x] 46.2.2 C1 window/entity lifecycle, visibility, hierarchy, and host actor bindings are ECS data;
  StageHost reconciles actor objects through `HostBindingComponent`; scoped signals and callbacks
  are disposable host caches
- [x] 46.3.1 C2 card projection entities and card data components are ECS authoritative
- [x] 46.3.2 Projection root stores frame lifecycle, interaction, and immutable snapshot data
- [x] 46.3.3 C2 surface mount state and surface Entity identity are ECS authoritative
- [x] 46.3.4 Native template state is observed into ECS; bind, native component mounted state,
  event ID, end-turn enabled state, and map pins are ECS data while adapter callbacks remain host cache
- [x] 46.3.4a Native template bind/unbind state is stored in `NativeTemplateStateComponent` and
  runtime bound queries read the shared ECS world
- [x] 46.4.1 Surface action, intent identity, and immediate result are ECS recorded
- [x] 46.4.2 Combat router input/intercept records are ECS data per surface
- [x] 46.4.3 Map, event, select, and end-turn native hook records are ECS data per surface
- [x] 46.4.4 Intent execution lifecycle records requested/sent/executed/queued/rejected in ECS;
  `NativeIntentLifecycleSystem` consumes one-shot execution events and the next available/unavailable
  authority-frame observation into confirmed/failed state, while business confirmation separately
  records pending/confirmed/failed ECS data from card, map, event, reward, select, and room evidence
- [x] 46.4.5 Declarative signal connections/state-machine transitions are ECS data; subscriptions are cache-only,
  node state is stored in `NodeStateComponent`, and connection/legacy-trigger declarations are immutable
  `ConnectionDeclarationsComponent` data
- [x] 46.4.6 Node properties and effect attachments are immutable ECS values; writes replace the
  corresponding Component through `PresentationWorld`
- [x] 46.4.7 One production `PresentationSchedule` orders authority projection/confirmation,
  surface intent execution, native lifecycle, shared-world normalization, C1 animation, effect
  envelopes, host presentation, coalesced render projection, render clock, and host backend tick;
  `tick` delegates to the same schedule and synchronous compatibility APIs use its system instances
- [x] 46.5.0 `RenderPlan.fromEcs()` is the sole target/effect cache input; C1/C2/surface/full-frame/
  EntityPresent targets rebuild after host cache clearing, direct RenderHost mutation APIs and
  RenderTarget setters are internal, and StageHost writes actor geometry back to ECS bounds
- [x] 46.5.1 C1/C2 render plans, effects, and profiles derive from ECS data only; animation
  playback and pulse state are data-only components, C1 declarations now materialize draw/bounds/
  visibility/effects into ECS frames, C1 and active C2 item targets rebuild in shared projections,
  while scheduled EffectPulse, animation property effects, and Lightwave writes are coalesced into
  one C1 host-cache projection per dirty window; surface/full-frame host APIs now consume
  `RenderSurfaceComponent` and `FullFrameRenderComponent`; no RenderHost enabled-state mirror
  remains; D1 `d1_full_present_combat_ready` passes
- [x] 46.5.2 Skeleton animation/pose/binding lifecycle derives from ECS data only; identity,
  frame, asset, pose, animation, and visual state are ECS components, while native binding lookup
  remains a provider cache rebuilt from retained ECS state; D1 native takeover, host recreation,
  and cleanup pass; pure tests cover recreation from retained ECS state
- [x] 46.5.3 EntityPresent slot identity, snapshot, and transform state are ECS components;
  `EntitySlot` is an on-demand immutable compatibility view and listeners are host callbacks;
  the RenderHost entity target cache rebuilds from ECS slot queries; D1 attach/draw/detach and
  cache cleanup are covered, and pure tests cover host recreation rebuilding retained slot state;
  D1 `d1_entity_present_smoke` passes
- [x] 46.6.1 UiProbe, UiOps, console, and inspect query ECS only; C1 UiOps control lookup and C1
  window/component probe snapshots and native template pin/end-turn snapshots read ECS data, while
  C2 EntityPresent snapshots also read ECS data; C1 window control/title/profile probe fields now
  query registered context components directly, while console/render legacy paths remain; UiOps no
  longer retains a second last-result map. UiProbe window ids now come from ECS lifecycle state and
  mounted native template state, so diagnostic output does not depend on the OPEN handle map.
- [x] 46.6.2 Remove or demote remaining legacy stores after each matching checklist row passes;
  WindowManager, SyntheticComponents, and EntitySlot remain derived compatibility views, while
  fixed component catalogs, signal subscription hubs, asset/profile catalogs, and host/provider
  caches are explicitly retained as non-authoritative integration state. PresentSurfaces now
  resolves its context/world on demand, allowing ECS registry recreation without stale writes;
  `ArtFramework.OPEN` is now a handle/layout-root cache and close falls back to ECS lifecycle data.

### Native render memory lifecycle

- [x] NRM-07 Derived frame plan cache: bounded same-frame plan/draw-order reuse is implemented and
      covered by focused/default JUnit and D1 FULL combat. Allocation-comparison evidence: the
      derived same-frame cache in `Sts1RenderPipeline.plan()` is now instrumented with additive,
      behavior-neutral diagnostics `planCacheHits` (incremented exactly when the `PlanKey.matches`
      branch returns the retained `lastPlan`) and `planCacheMisses` (incremented exactly when a
      `SurfaceDrawPlan.buildFromSnapshot(...)` rebuild occurs); both are exposed additively in
      `probeSlice()` under `planCacheHits`/`planCacheMisses` and cleared by `resetForTests()`. A
      paired pure-JUnit test (`planCacheCountersProveReuseAvoidsRebuildsOnConstantKey`) proves a
      constant-key workload of N=200 `plan()` calls performs EXACTLY ONE rebuild and 199 cache
      returns (vs N rebuilds without the cache), and that a discriminator change forces exactly one
      additional rebuild. A companion test
      (`planCacheCountersTreatFrameIdAsPartOfKey`) confirms `frameId` is a real discriminator (see
      `PlanKey.matches`), so an otherwise-identical new frame is a MISS/rebuild; the cache contract
      is same-FRAME, not same-discriminators-across-frames. This is `plan()`-level allocation-
      avoidance evidence (counting avoided `SurfaceDrawPlan` rebuilds) and is explicitly NOT a
      JVM heap-allocation measurement; no byte-level allocation reduction is claimed.
- [x] NRM-08 Differential projection/ECS/render-target updates: identical surface/full-frame ECS
  writes preserve component identity, and the unified generic RenderPlan reconcile algorithm retains equal
  target/binding identities, synchronizes mutable state, removes stale plan ownership, preserves
  manual overlays, stages full-plan validation atomically, rejects duplicate ids, preserves unknown-
  effect skip compatibility, publishes concurrent-safe binding snapshots, and keeps explicit
  recreation destructive. The active-surface system now submits only its local desired/managed set
  after the initial deployed jar `fbf4d6fd...` exposed and the subsequent fix removed the active-
  surface full-plan hot-path regression. Focused tests passed 39/39, the default JUnit gate passed
  1227/1227, final jar `47ac04adafba602f9f5fc1d492f99783dcf22d527b41b5dd6f354122ec8cfad3` was
  deployed to D1, and `scripts/art-lab combat verify-full` passed 1/1. Independent final review
  passed with no findings; no paired allocation improvement is claimed.
- [x] NRM-09 Heartbeat/full-probe split: `StageHost` owns one post-update monotonic publisher;
  heartbeat/full cadence and files are separate, explicit `art probe` remains immediate, and
  ART_PROBE v1 shape/prefix is preserved. Pure publisher tests cover cadence, lightweight payload,
  freshness/sequence, fail-open sink isolation, serialized explicit publication, and atomic
  same-directory replacement. R15-05/06/07 are fixed; focused JUnit passed 40/40, default JUnit
  passed 1239/1239, and corrected-jar D1 sidecar sampling passed with complete schema-v1 payloads.
  NRM-09 is complete; no paired allocation or GC improvement claim is made.
- [x] NRM-10 Sts1AssetMaterializer lifecycle: pure exactly-once disposal seam and Texture clear
      disposal plus pure host recreation/fallback integration are implemented; D1 counter-based
      disposal-attempt/rematerialization/cache-hit evidence and independent review are complete.
      GL-driver deletion and pixel parity are explicitly out of scope.
- [x] NRM-11 Integrated semantic, D1, and Arthas closure: default JUnit, repeated D1 FULL combat,
      lifecycle review, bounded negative inventory, and post-fix Arthas operational-health evidence
      pass under the user-scoped standard. Equivalent pre-fix stress comparison is unavailable and
      no allocation-rate improvement is claimed.

- [x] 0. Scaffold — registry API + tests
- [x] 1a. C1 logic runtime + layout DSL + demo resource + open dispatch
- [x] 1b. Stage host + StsSkin + StageBackend (optional on-device when D1 set)

- [x] 2. C2 map template intercept + pin decorator hooks (logic; patches later)
- [x] 3. C2 event/select/end-turn templates (logic; patches later)
- [x] 4. EntityPresent lifecycle API (logic; STS draw later)
- [x] 5. Consumer contract: versioned jar + `compileOnly` + MTS dep

### 6. UiOps / UiProbe (unified UI commands + snapshot)

- [x] 6.1 Pure `UiOps` / `UiProbe` / `UiOpResult` + `FakeNativeOps` + JUnit
- [x] 6.2 Select grid/hand + confirm ops
- [x] 6.3 Map node click + pin query on probe
- [x] 6.4 Event option + end-turn press ops
- [x] 6.5 Hand play **gesture** + C1 `clickButton`
- [x] 6.6 Console `art probe` / `art op` + `StsNativeOps` install

### 7. C2 SpirePatch thin hooks

- [x] 7.1 Map transition → `NativeUiHooks.onMapNodeClick` (BLOCK clears nextRoom)
- [x] 7.2 Grid confirm button → `onSelectConfirm(GRID)`; grid/hand update observe stubs
- [x] 7.3 Event `buttonEffect` prefix → `onEventOption`
- [x] 7.4 EndTurn enable/disable(true) UI gates (no protocol broadcast)

### 8. UI-layer verification

- [x] 8.0 Fixture YAML + assert runner offline
- [x] 8.1 Device mode: Amethyst connector console + `ART_PROBE` log scrape (`device_console.py`)
- [x] 8.2 Fixture smoke: probe shape + C1 window + intercept-related template flags (JUnit owns BLOCK/ALLOW)
- [x] 8.3 Lab doc: `android-device-lab.md` (Amethyst D1 UI only)
- [x] 8.4 On-device pass of `tests/ui-scenarios/device/*` (D1 READY; enabled_mods + cold start)

### 9. Lab intercept + deploy hardening

- [x] 9.1 `GateLab` + console `art gate … block|clear` + JUnit
- [x] 9.2 Device scenario `d1_gate_block_ops` (ops under lab gate)
- [x] 9.3 `scripts/ensure-enabled-mods.sh` + deploy agent note
- [x] 9.4 Remove empty select update stub patches

### 10. Component composition framework

- [x] 10.1 Design: `component-composition.md` + `component-layout-fx.md`
- [x] 10.2 Pure `UiNode` AST + Nest containers (`row`/`col`/`stack`/`panel`/`fragment`)
- [x] 10.3 `LayoutEngine` bounds + id index (JUnit)
- [x] 10.4 `ComponentRegistry` + `ref` / `slot` expand (JUnit)
- [x] 10.5 `UiNodeLoader` JSON + shorthand props; legacy `LayoutLoader` still works
- [x] 10.6 Scenario fixtures / offline art-verify coverage for composition samples
- [x] 10.7 Leaf runtime (`WidgetSession`) + UiOps slider/hitarea/click + probe controls
- [x] 10.8 RenderHost + Effect/Shader registry + glow GLSL assets + entity Attach + probe `render`
- [x] 10.9 Composition Stage inflate (`ComponentActors` + `attachComposition`) + effect SpriteBatch draw
- [x] 10.10 ShaderRuntime compile path + glow ShaderProgram draw fallback + probe shader status
- [x] 10.11 FULL_FRAME enable/sync/bind + console `artframework fx` + StageHost screen bounds
- [x] 10.12 FrameCapture + blur/glass post-process + `glass` component + glass_demo layout

### 11. Godot-aligned UI core

Design: [`docs/design/godot-aligned-ui.md`](design/godot-aligned-ui.md).

- [x] 11.0 Design: `godot-aligned-ui.md` + dual-track / composition / layout-fx / ui-ops-probe / AGENTS links
- [x] 11.1 `UiInstance` + `UiTree` + `SignalHub` (C1) + JUnit
- [x] 11.2 `LayoutSpec` size flags + minSize + `LayoutEngine`
- [x] 11.3 Theme MVP + `StsTheme`
- [x] 11.4 C1 widgets: textfield / checkbox / progress / scroll (priority order)
- [x] 11.5 C2 NativeControl (`sts.*`) + UiOps invoke + probe `components`
- [x] 11.6 API mount/unmount + `HostBackend` SPI + consumer.md
- [x] 11.7 Optional: C1 end-turn chrome pilot (`endturn_chrome_pilot` layout + JUnit)

### 12. ART Framework (presentation graph)

Design: [`docs/design/art-framework.md`](design/art-framework.md). Breaking identity rename done in phase 12.0 prep.

- [x] 12.0 Identity rename (`artframework` package/mod/jar/console/probe/env) + `art-framework.md` + task links
- [x] 12.1 Declared signals on AST + connect/emit validation + JUnit
- [x] 12.2 `NodeRegistry` / type SPI; loaders resolve registered types; namespaced third-party types
- [x] 12.3 LML → AST loader + resource dispatch (`.json` / `.lml`) + art-verify sample
- [x] 12.4 C1 node factory SPI; migrate `ComponentActors` built-ins
- [x] 12.5 RenderGraph / host render backend boundary (behavior-preserving)
- [x] 12.6 Built-in `animation_player` + `shader_effect` nodes
- [x] 12.7 Skeleton provider SPI + fake provider JUnit
- [x] 12.8 Native id namespace `sts1.*` + presenter bridge naming (consumer-visible)
- [x] 12.9 Signal completion: C2 native emit/validation, C1 interceptors, SignalHub sugar,
  registry defaults, stable anonymous keys, and legacy actor routing

### 13. Runtime hardening

- [x] 13.0 Freeze public IDs/API; canonical native aliases, handle lifecycle, and probe contract
- [x] 13.1 Unify UiTree / WidgetSession / Host lifecycle and cleanup guarantees
- [x] 13.2 Complete Host SPI capabilities, input, and unified tick
- [x] 13.3 Converge UiOps delegation and versioned UiProbe results
- [x] 13.4 Verify the C1 real input and signal loop on Stage/D1
- [x] 13.5 Harden C2 NativeControl lifecycle, payloads, and screen recreation
- [x] 13.6 Capability-aware render fallback and D1 effects verification
- [x] 13.7 Consumer fixture, API stability checklist, and release versioning

### 14. Component action convergence

- [x] 14.1 Route legacy C2 UiOps sugar through `UiComponent.action`; keep gate/gesture dispatch internal
- [x] 14.2 Expose mounted C1 windows as `UiComponent` actions and probe slices

### 15. Backend / full C2 present / HostAssets

Design: [`docs/design/backend-context.md`](design/backend-context.md),
[`docs/design/c2-full-present.md`](design/c2-full-present.md),
[`docs/design/host-assets.md`](design/host-assets.md).

**Done (contract):** pluggable Primary Backend + context frames; C2 full-present surfaces;
HostAssets packs. Thin C2 intercept remains migration bridge. STS1 host draw for combat/map
is milestone 16/19; event/select host draw is milestone **22**.

- [x] 15.0 Design docs + task / AGENTS / design cross-links
- [x] 15.1 Context / Backend pure interfaces + `FakeBackend` frame/intent JUnit
- [x] 15.2 HostAssets pure merge/config/`FakeHostAssets` + JUnit (no GL)
- [x] 15.3 ResourceId conventions + minimal vanilla catalog (card / map / UI)
- [x] 15.4 ART draw paths through `resolve` (Theme icon/style + present hand art probe)
- [x] 15.5 Combat hand / card_slots snapshot hard-sync present (projection + probe)
- [x] 15.6 Drag / play intent + signal intercept chain (`CardRef` multi-instance)
- [x] 15.7 Map / controls / skeleton surfaces (full-present slices)
- [x] 15.8 Pack register API for beautify mods + `probeAssets` / console `art assets`
- [x] 15.9 Consumer notes: intents replace native UI callbacks; `sts.*` aliases

Default gate remains `./scripts/with-art-env.sh test`. Device: optional D1 only.

### 16. STS1 full-present host implementation

Design: [`docs/design/backend-context.md`](design/backend-context.md),
[`docs/design/c2-full-present.md`](design/c2-full-present.md),
[`docs/design/host-assets.md`](design/host-assets.md).

15.x completed the host-agnostic contract and pure-logic surfaces. 16.x is the separate
STS1 host/render/input implementation required before a surface may claim full-present D1
coverage.

- [x] 16.0 Strong typed frame views (`ControlsView` / `MapView`), scene epoch policy,
  `PresentLevel` OFF|OBSERVE|FULL + `FullPresentMode` probe; JUnit + f5 fixture
- [x] 16.1 `Sts1PresentationBackend` strong-typed snapshot + intent gate by present level
  (D1 probe optional; still READ_ONLY until 16.5 executor)
- [x] 16.1b D1 probe smoke: `d1_full_present_observe` / `d1_full_present_combat_on` (D1 PASS)
- [x] 16.2 STS1 HostAssets vanilla catalog (`Sts1VanillaCatalog` / `Sts1HostAssets`) + JUnit
  (texture handle materialization deferred to 16.3/16.4 draw path)
- [x] 16.3 C2 render plan: layers, BatchStateGuard, clip, overlay observe + JUnit
  (host SpriteBatch still delegates card.render; true atlas path in 16.4)
- [x] 16.4 Hand draw path + geometry compare (pure JUnit); host applies projection pose
  before card.render; D1 geometry fixture optional later
- [x] 16.4b Hand geometry compare remains pure JUnit (`GeometryCompare`); live D1 pose
  fixture optional when combat room scripted
- [x] 16.5 CombatInputRouter + IntentExecutor SPI + RecordingIntentExecutor JUnit;
  Sts1PresentationBackend delegates submitIntent; native input suppress flag
  (live STS play/drag executor host body still thin — uses Fake/Recording in tests)
- [x] 16.5b `Sts1IntentExecutor` (drag/play/end-turn/map ack) + hand input suppress patch;
  installed at PostInitialize; JUnit soft-reject without dungeon
- [x] 16.6 ControlsDrawPath + end-turn suppress patch + ART label draw when FULL;
  `art present combat` sets controls level; probe `controlsDraw`
- [x] 16.6b Covered by D1 `d1_full_present_combat_on` controlsDraw/presentLevel FULL
- [x] 16.7 MapDrawPath + MapPanZoom + hitTest + map suppress patch + `art present map`;
  JUnit + f10; D1 `d1_full_present_map`
- [x] 16.8 ArtAudioBridge + Sts1SkeletonBridge + host recreate cleanup via PresentSafety
- [x] 16.9 PresentSafety panic/clear-panic, probe matrix f11, consumer freeze notes,
  D1 `d1_full_present_panic`

### 17. Dev UI console (`art ui`)

Design: [`docs/design/dev-ui-console.md`](design/dev-ui-console.md).

Inspect ART trees, emit signals, invoke actions, limited STS native dump/click for lab/D1.
Does not block 16.x full-present host work.

- [x] 17.0 Design: `dev-ui-console.md` + task / ui-ops-probe / ui-layer-verification links
- [x] 17.1 Pure `UiInspect` + `UiLabListeners` + JUnit
- [x] 17.2 Console `art ui list|tree|node|emit|invoke|listen`
- [x] 17.3 `StsUiReflect` native dump/click whitelist
- [x] 17.4 Device scenario + D1 smoke (optional after deploy)

### 18. Lab run navigation (`art lab`)

Design: [`docs/design/lab-run-nav.md`](design/lab-run-nav.md).

Atomic + composite console commands to reach main menu / fresh run / embark for D1
full-present scenarios. Lab-only (not consumer API).

- [x] 18.0 Design: `lab-run-nav.md` + task / console / device-lab links
- [x] 18.1 `LabStateSnapshot` / `LabHost` / `FakeLabHost` + `art lab dump` + JUnit
- [x] 18.2 L1: clear-saves / strip-resume / open-char-select / char / embark / seed
- [x] 18.3 L1: menu-click / abandon / abandon-confirm / return-menu / proceed
- [x] 18.4 L2: ensure-menu / ensure-fresh-menu / start-run + D1 YAML (`d1_lab_*`)
- [x] 18.5 Console `art lab …` + docs (`console-commands`, device-lab, ui-layer-verify)

### 19. STS1 full-present production readiness

- [x] 19.1 Effective capability state: FULL requires mounted scene + ready executor before ART
  suppresses native UI or owns input; probe state/reason + JUnit
- [x] 19.2 Native card-pixel hand draw path: ART delegates the hand surface, hard-syncs
  live card poses, and invokes the unpatched `AbstractCard.render`; D1 `fight Cultist`
  combat-ready scenario confirms FULL_READY, hand-surface suppression, and ART-orchestrated
  native card drawing
- [x] 19.3 HostAssets controls/map draw path + real STS1 map intent execution;
  D1 `d1_full_present_map_ready` (FULL_READY, texture nodes, `art op map first` leaves map)
- [x] 19.4 Scene lifecycle/recovery: PresentSafety matrix JUnit + D1
  `d1_full_present_lifecycle` (combat FULL → map fallback → re-arm → panic/clear)
- [x] 19.5 Event/select full-present surfaces (`sts1.event`, `sts1.select.grid|hand`) + JUnit
- [x] 19.6 HostAssets consumer pack/release validation (`ConsumerFixture` + verify script)

### 20. Historical FrameSignal authority boundary

Design: [`backend-context.md`](design/backend-context.md).

- [x] Superseded by milestone 21; endpoint categories and transaction types were removed.

### 21. Unified SignalBus

- [x] 21.1 One ordered bus for C1, C2, frame, backend, and host signals; exact and regex subscriptions
- [x] 21.2 Generic `continue` / `replace` / `stopHandled` / `stopRejected` delivery decisions
- [x] 21.3 C2 imperative actions, native hooks, GateLab, full-present actions, and frame publish use bus delivery
- [x] 21.4 Remove FrameEndpoint transaction API and migrate fixtures to `FakeSignalBackend`
- [x] 21.5 `HostPatchResults` + hooks return `SignalDispatchResult`; patches use single adapter

### 22. STS1 event / select full-present host

19.5 delivered pure surfaces + FakeBackend JUnit (draw deferred). 22.x is the STS1 host
draw / executor / suppress / D1 path for `sts1.event` and `sts1.select.grid|hand`.

- [x] 22.0 Design/task links + c2-full-present status note
- [x] 22.1 `EventView` / `SelectView` + `ContextFrame` + projection + JUnit
- [x] 22.2 `SurfaceDrawPlan` event/select + PresentSafety unmount + pipeline
- [x] 22.3 `EventDrawPath` / `SelectDrawPath` + renderer + backend snapshot + suppress patches
- [x] 22.4 `Sts1IntentExecutor` SELECT_* + `StsNativeOps` hand select
- [x] 22.5 Console `art present event|select` + probe `eventDraw` / `selectDraw`
- [x] 22.6 D1 YAML + consumer freeze notes

### 23. Release hardening

Post-22 product freeze for consumers. No new full-present surfaces in this milestone.

- [x] 23.0 Design/task status pass (c2-full-present / dual-track / AGENTS / README)
- [x] 23.1 API stability ↔ consumer freeze alignment
- [x] 23.2 Probe schema contract (v1 field groups)
- [x] 23.3 Consumer fixture expansion (frames / present / CardRef)
- [x] 23.4 Release gate script (`scripts/release-gate.sh`)
- [x] 23.5 CHANGELOG + versioning notes
- [x] 23.6 Version bump `1.0.0-alpha.3`

### 24. EntityPresent host draw

Co-op chrome slots: typed snapshot → EntityDrawPath → probe → optional host paint.
Combat HAND full-present remains authoritative for in-combat cards.

- [x] 24.0 Design note + task links (`entity-present.md`)
- [x] 24.1 Typed `EntitySnapshot` + parse helpers (JUnit)
- [x] 24.2 `EntityDrawPath` pure projection (JUnit)
- [x] 24.3 Probe `entities.slots` + RenderHost bounds by kind
- [x] 24.4 Policy: combat HAND FULL wins over EntityPresent CARD
- [x] 24.5 CARD host paint path (reuse hand renderer pattern; flag)
- [x] 24.6 RELIC icon path
- [x] 24.7 PLAYER / MONSTER minimal chrome path
- [x] 24.8 Skeleton PostRender draw when `shouldDraw`
- [x] 24.9 Offline fixture + optional D1 entity smoke

### 25. More STS1 full-present surfaces

Extend Backend `scene()` + View → DrawPath → suppress → executor → D1.
Reuse 16/22 pipeline. Meta menus stay C1 + `art lab`.

#### Wave A — combat chrome

- [x] 25.1 `sts1.combat.proceed` (+ cancel) controls extension
- [x] 25.2 `sts1.combat.energy` orb present from ControlsView.energy

#### Wave B — run decisions

- [x] 25.3 `sts1.reward.combat` View / DrawPath / host / D1
- [x] 25.4 `sts1.reward.card` View / DrawPath / host / JUnit
- [x] 25.5 `sts1.rest` View / DrawPath / host / JUnit
- [x] 25.6 `sts1.treasure` View / DrawPath / host / JUnit
- [x] 25.7 `sts1.shop` View / DrawPath / host / JUnit
- [x] 25.8 `sts1.reward.boss_relic` View / path / JUnit

#### Wave C — HUD + combat clarity

- [x] 25.9 `sts1.top_panel` View / DrawPath / probe
- [x] 25.10 `sts1.combat.intents` observe-first path
- [x] 25.11 Console `art present …` + probe fields for new surfaces
- [x] 25.12 D1 YAML smoke for reward/rest (lab-reachable) + offline fixtures

### 26. Room FULL production readiness

Raise 25 room/chrome surfaces to combat/map-class FULL_READY (scene match + suppress +
executor + host paint).

- [x] 26.0 Design/task matrix + c2-full-present / CHANGELOG status
- [x] 26.1 Capability / sceneReady for room surfaces (not mount-only) + JUnit
- [x] 26.2 Real room IntentExecutor gestures + soft-reject without dungeon + JUnit
- [x] 26.3 Suppress patches (reward/rest/shop/treasure) + shop/treasure/proceed render
- [x] 26.4 Probe capability fields + offline fixture `f13_room_full_ready`
- [x] 26.5 D1: reward capability fields + `d1_full_present_combat_chrome`

### 27. C1 / beautify components

- [x] 27.1 `grid` / `tabs` containers (LayoutEngine + Stage + sample layout)
- [x] 27.2 HostAssets beautify pack sample JUnit
- [x] 27.3 Register `grid_tabs_demo` window

### 28. Release hardening (alpha.4)

- [x] 28.1 Known-limits / consumer freeze notes refresh
- [x] 28.2 Version bump `1.0.0-alpha.4` + CHANGELOG

### 29. PresentProfile + Lightwave (theme / cascade / declarative)

Design: PresentProfile aggregates Theme + PresentChromeStyle + optional pack; not a mid-draw
RenderInterceptor. Suppress still owned by FullPresentMode.

- [x] 29.1 `PresentProfile` / `PresentProfiles` + `sts` / `lightwave` builtins + probe
- [x] 29.2 `LightwaveTheme` (semi-transparent panel, white border, cool accent, Card alpha)
- [x] 29.3 Theme cascade: font/icon/style + `themeType` variation
- [x] 29.4 Declarative `present_profile` / `theme` on window root + JUnit

### 30. Lightwave FX + animation triggers + demo

- [x] 30.1 `LightwaveEffect` + GLSL + fallback strips + `art fx lightwave`
- [x] 30.2 White border via effect fallback / chrome tokens
- [x] 30.3 `animation_player` `auto_play` + `triggers` (no new LML component type)
- [x] 30.4 C1 `opacity` prop applied on actors
- [x] 30.5 `layouts/lightwave_demo.json` + window register

### 31. C1 StsSkin from Theme

- [x] 31.1 `StsSkin.create(Theme)` + pure mapping helpers JUnit
- [x] 31.2 StageHost uses `Themes.getDefault()` at init

### 32. C2 PresentChromeStyle consumption

- [x] 32.1 `PresentChromeStyle` + fromTheme / probe
- [x] 32.2 Hand card alpha + white border; controls label colors from chrome
- [x] 32.3 HandDrawPath probe includes chrome + presentProfile

### 33. Console / docs / scenarios

- [x] 33.1 `art profile|theme list|get|set`
- [x] 33.2 Offline fixture `f14_present_profile_lightwave` + D1 `d1_lightwave_demo`
- [x] 33.3 task / design / CHANGELOG notes

### 34. Node-scoped PresentProfile (no process active)

Design: Present resources + ProjectPresent fallback + `art.present_profile` attach/override;
effects stay explicit nodes. See [`present-profile.md`](design/present-profile.md).

- [x] 34.1 `ProjectPresent` + `PresentResolve` + `PresentBinding` / `PresentMode`
- [x] 34.2 Remove process active; registry is resources only
- [x] 34.3 UiTree/UiInstance resolve; root sugar + `art.present_profile`
- [x] 34.4 C1 StageHost + C2 chrome via resolve / project
- [x] 34.5 Probe `projectPresent` + `windows.*.present`; console project/resolve
- [x] 34.6 JUnit + f14 + d1_lightwave_demo (node assert, no global set required)

### 35. Present production (hot restyle / C2 surface / pack)

Design: close Known limits from PresentProfile/Lightwave showcase — restyle open C1,
surface-scoped chrome, packId → HostAssets. See [`present-profile.md`](design/present-profile.md).

- [x] 35.1 Hot restyle: project/present change refreshes trees + re-attaches Stage (project-fallback windows)
- [x] 35.2 `SurfacePresent` bind + `PresentResolve.chromeForSurface` for C2 draw paths
- [x] 35.3 C2 chrome expand: event/select/room/proceed/energy/intents/top labels via chrome
- [x] 35.4 `packId` → HostAssets prefer/enable + JUnit + probe
- [x] 35.5 API stability + CHANGELOG notes (setProjectPresent restyles; SurfacePresent)

### 36. Lightwave visual deepen (optional polish)

- [x] 36.1 Lightwave border/band tokens from effect params (theme-aligned defaults)
- [x] 36.2 `glass_lightwave_demo` layout (glass + lightwave co-window)
- [x] 36.3 Third-party PresentProfile register sample JUnit (namespaced theme + chrome)

### 37. Global PresentProfile catalog (skin register facade)

Design: PresentProfiles is the sole skin resource catalog; facade register ≠ apply.
See [`present-profile.md`](design/present-profile.md).

- [x] 37.1 `PresentProfiles.register` syncs `Themes` (name / profile id)
- [x] 37.2 `ArtFramework.registerPresentProfile` / `get` / `ids` / `presentProfiles()`
- [x] 37.3 JUnit `PresentProfileCatalogTest` (register no apply; set applies; reset)
- [x] 37.4 Probe `presentProfiles` + f16 fixture; design / api-stability / consumer

### 38. PresentPack + enabled profiles (regex select/modify)

Design: Profile = skin; Pack = LML/JSON templates + windows. Select profile → activate pack by
packId / profileId link. No profile-id special cases in core. Regex enable/select/modify.

- [x] 38.1 `PresentPack` / `PresentPacks` / manifest loader
- [x] 38.2 activate/deactivate → ComponentRegistry + WindowDef
- [x] 38.3 `ProjectPresent.set` → `activateForProfile`
- [x] 38.4 `EnabledPresents` + regex select/modify + packId patch
- [x] 38.5 Builtin lightwave pack + facade / probe / console
- [x] 38.6 JUnit `PresentPackTest` + f18 + D1 `d1_present_packs`

### 39. Signal wiring aligned with bus (exact + regex)

Design: [`node-signal-runtime.md`](design/node-signal-runtime.md). Backend ↔ Signal ↔ Node.

- [x] 39.1 `SignalHub` / `UiTree.connectBus` exact + `Pattern`
- [x] 39.2 `NodeConnections` + `connections` decl; legacy `triggers` normalize
- [x] 39.3 JUnit exact / regex / unmount clear

### 40. UiActions (full builtin set)

- [x] 40.1 `UiActions` registry + `ArtFramework.registerUiAction`
- [x] 40.2 Builtins: play/pause/stop/resume/set_prop/pulse_effect/emit/close_window
- [x] 40.3 `PropEffectBridge` + `EffectPulse` (signal path for lightwave pulse)
- [x] 40.4 JUnit actions + third-party register + pulse binding

### 41. NodeStateMachine + AnimationPlayer once/loop/pause

- [x] 41.1 `NodeStateMachine` / `NodeStateMachines` from `states` decl
- [x] 41.2 AnimationPlayer pause/resume/loop + state idle/playing/paused
- [x] 41.3 Signals paused/resumed/looped; JUnit

### 42. Integration / demo / docs / D1

- [x] 42.1 `lightwave_demo` connections (pulse_effect + set_prop); no imperative ok pulse
- [x] 42.2 Design `node-signal-runtime.md` + task / consumer notes
- [x] 42.3 D1 `d1_node_connections` + existing `d1_lightwave_demo`

### 43. C2 Lightwave chrome + surface FX

- [x] 43.1 Baseline coverage and C2 surface target lifecycle
- [x] 43.2 Surface chrome panels/borders for ART-owned C2 draw regions
- [x] 43.3 Pack-driven per-surface LightwaveEffect bindings with inactive-surface cleanup
- [x] 43.4 Probe + offline fixture + `C2LightwaveSurfaceTest`
- [x] 43.5 D1 `d1_lightwave_c2_full` scenario for combat C2 visual verification

### 44. Spine 4.2 present architecture

Design: [`docs/design/spine42-present.md`](design/spine42-present.md).

Boundary: STS2 assets are never committed or packaged. Spine runtime users must satisfy
the Spine Runtime license; 4.2 runtime integration is provider-side and kept separate from
core presentation contracts.

- [x] 44.1 Design: Spine 4.2 present architecture, license/resource boundary, dual provider plan
- [x] 44.2 Pure atlas 4.x parser for compact `bounds` / `offsets` / `rotate:90` regions
- [x] 44.3 `AnimState`, `AnimGraph`, `SkeletonMixTable`, and STS2-style `SkeletonAnimator`
- [x] 44.4 Extended `SkeletonProvider` commands with fake-provider JUnit coverage
- [x] 44.5 STS1 Spine 3.4 provider baseline using host-bundled runtime
- [x] 44.6 Spine 4.2 provider probe shell for shaded runtime detection and safe degradation
- [x] 44.7 JUnit coverage for parser, graph, animator, provider commands, and provider probes
- [x] 44.8 Optional `spine42-runtime` sub-build: relocated runtime jar, Java 8 output, license,
  and artifact allowlist verification; separate from the main jar and test lifecycle
- [x] 44.9 `ART_STS2_ROOT` local asset bundle script and gitignored `Sts2Assets.jar`
- [x] 44.10 Independent `tests/spine42-assets` bundle checks; standard JUnit has no asset dependency
- [x] 44.11 Developer runtime loader, device asset deployment, and `spine42` dev console controls
- [x] 44.12 D1 resource status/load/animation/lifecycle scenario skeletons
- [x] 44.13 D1 data-path evidence: source-patched runtime loads a real 4.2 `.skel`, animates,
      reports a bone transform, and cleans lifecycle state; pixel renderer intentionally deferred
- [x] 44.14 Frozen-pose GL determinism (F08): `tests/ui-scenarios/device/d1_spine42_screenshot.yaml`
      renders the frozen idle_loop pose and verifies determinism via an identical re-rendered
      `backend.skeleton.drawEvidence.vertexSignature` (`capture` var `sig1` -> bounded wait with no
      unfreeze -> `probe: all` -> `eq_var: sig1`), with the intermittent `drawEvidence.count == 0`
      handled by a retrying `wait_probe`. The two `screenshot: true` steps are advisory visual
      evidence only. The prior pixel gate was removed: its crop bled ~20% animating native
      background (20.0% / 16.6% measured) while the frozen pose itself was byte-stable
      (`vertexSignature de235afb6f46c550` identical). **Native-reference pixel parity (47.35) is NOT
      achieved** — the prior recorded native-reference run failed at 242818 px / 0.117 ratio vs a
      0.01 limit — and stays OPEN, developer-gated on `ART_SPINE42_REFERENCE_PNG`.

### 45. Unified Presentation Entity Runtime

Design: [`docs/design/presentation-entity-runtime.md`](design/presentation-entity-runtime.md).

- [x] 45.1 Add dependency-neutral `presentation` runtime: context, stable keys, common entity
      components, immutable frame snapshots, and pure ECS tests.
- [x] 45.2 Replace `UiInstance` and the temporary object-tree facade with
      `PresentationContext` + `EntityId`; migrate lifecycle, path lookup, props, theme resolution,
      and public API.
- [x] 45.3 Migrate signals, declarative connections, state machines, and animation to node/entity
      identity while retaining declared-port and first-stop semantics.
- [x] 45.4 Migrate C1 Stage materialization and render synchronization to presentation frames.
- [x] 45.5 Migrate C2 surfaces, projection items, entity present, and skeleton lifecycle into the
      unified context; retain STS policy and intent authority in the host adapter.
- [x] 45.6 Derive render attachments from entities; migrate Lightwave chrome/effects to exact
      visual-item attachments and remove target-map authority.
- [x] 45.7 Replace legacy runtime/docs/fixtures and complete JUnit, offline verifier, and D1
   C1/C2 visual verification.

### 47. Native Render Coverage Contract

The static inventory, manifest checker, runtime bridge, and strict runtime ledger are present.
FULL acceptance requires zero static unknowns and zero runtime strict-report gaps.

- [x] 47.1 Static inventory output and explicit coverage manifest scaffold
- [x] 47.2 Manifest schema validation and `--check-manifest` closure check
- [x] 47.3 Classify all static candidates and make strict manifest check pass
- [x] 47.3a Replace hand card native chrome delegation with an atlas-only ART card shell
- [x] 47.4 Pure invocation/disposition/evidence ledger contract and exact correlation
- [x] 47.5 `Sts1NativePresentationAdapter` projection into ECS / `PresentationFrame`
- [x] 47.6 Transient effect instance registry and complete recovery cleanup
- [x] 47.7 Migrate remaining native render patches through the final bridge contract
- [x] 47.8 Contract-layer sync for full-present takeover: SDD replaces the native-pixel
      authority rule with disposition + evidence-ledger delegation (exposing current
      pixel-supply gaps per surface and the un-patched `AbstractCard.render` boundary),
      coverage manifest marks every suppressing patch `ART_DELEGATED` (incl.
      `TopPanel#render` / `ProceedButton#render`), and ownership-test messaging matches
      the allowlist semantics
- [x] 47.9 End-turn field gap G1 / review V-3: `ControlsView` gains backend-projected
      end-turn hitbox geometry (`endTurnX/Y/W/H` + `hasEndTurnBounds`) and localized
      label (`endTurnLabel`); `Sts1PresentationBackend` projects native hb/label; render
      consumes projection with constant fallback only, and the unlisted reflection bridge
      `Sts1EndTurnChrome` is deleted (NRO-06)
- [x] 47.10 Design: native render family → three-layer system contract
      ([`native-render-family-systems.md`](design/native-render-family-systems.md)):
      25-family taxonomy over the then-current 488-path static-scan snapshot (schema v3,
      families.py as the
      assignment authority), family → collection/projection/system mapping framework with
      the TransientEffect four-piece template, meta-screen / targeting / transient-VFX
      integration recipes, the new-`@SpirePatch` precondition checklist, and known
      scanner limitations
- [x] 47.11 Slice 3: fix the transient-effect observation entry — the three-arg
      `AbstractGameEffect.render` Prefix missed container-driven effects (single-arg render is
      abstract), so a `@SpireInstrumentPatch` on `AbstractDungeon#render(SpriteBatch)` now wraps
      the three `AbstractGameEffect.render:(SpriteBatch)V` call sites with an observe-then-render
      helper (zero suppression, fail-open ledger note); converge the Registry→ECS projection into
      the schedule-owned `TransientEffectProjectionSystem` (`HOST_PRESENTATION`) whose drain is
      shared by the frame schedule and the synchronous render-hook bridge
- [x] 47.12 D1 evidence for 47.11: fresh `ArtFramework.jar` deploy + `d1_full_present_combat_ready`
      pass; probe `nativeRender.transientEffects.rendered` climbs 0→2→30→64→76→86 during
      combat with `failOpen=0` / `unknownLifecycle=0`, confirming container-driven effect
      observation is live on device (previously pinned at zero by the three-arg mis-hook)
- [x] 47.13 Slice B: NRCC coverage-manifest strategy vocabulary + family default
      finalization — added `OBSERVED` (native authority retained, ART observes through an
      existing hook/instrument; justification must cite the observation patch file, probe
      test optional) and `NATIVE_PASSTHROUGH` (structural helpers never intercepted;
      justification required) with entry-or-family justification validation; finalized
      `FAMILY_DEFAULT_POLICY` for all 25 families (vfx groups → `OBSERVED`,
      draw-primitives/word-tip/core roots → `NATIVE_PASSTHROUGH`, roadmap groups →
      `NATIVE_WITH_ART_OVERLAY` "future delegation candidate", meta screens →
      `OUT_OF_SCOPE`, targeting reserved `UNKNOWN`); member-level exceptions
      `AbstractDungeon#render` → explicit `OBSERVED` (container instrument) and
      `TestGame#render` → `OUT_OF_SCOPE`; generator now omits policy for family-default
      resolutions so re-triage stays a one-table change; regenerated
      `sts1-native-coverage.yaml` (historical 488-path snapshot: 16 `ART_DELEGATED`,
      63 `OUT_OF_SCOPE`,
      1 `OBSERVED`, 408 inherited; unknown=0) so
      `--check-manifest --strict-manifest` passes with zero ownership errors
- [x] 47.14 Slice C: eliminate the rest/shop/treasure "suppress-native but zero-pixel"
      black-screen risk in two phases. Phase 1 capped `SurfaceDrawPlan` so the three room
      surfaces kept native pixels (`suppressNative=false`, mode stayed DRAW) while they closed
      their evidence ledger with a constant count. Phase 2 restored suppression after supplying
      real pixels: G5 — `readRestView` soft-reads live `CampfireUI` buttons (class-name option
      ids, `label`, `usable`; static triple fallback); G6 — `readTreasureView` soft-reads the
      live `TreasureRoom` chest (`isOpen`) plus the granted relic reward label/resourceId
      (`closed()` fallback); `prepare{Rest,Shop,Treasure}Visuals` materialize title/row C2 items
      from new pure `chromeLines()` projections (already inside
      `disableInactiveSurfaceEffects`); `render{Rest,Shop,Treasure}` paint proceed-style text
      chrome and record the real drawn-row count instead of the constant 1. Shop entry prices
      stay whatever `ShopView` carries (G4 full projection deferred to Slice E). Probe additions
      are host-only additive fields (`chromeLineCount` per DrawPath slice) per api-stability.
      Final state: FULL suppresses native for all three rooms, ART shows minimal chrome, evidence
      counts are real; no new `@SpirePatch`.
- [x] 47.15 Slice D — target arrow observation family pilot (recipe B): add
      `overlay-targeting` OBSERVED default, scan whitelist `renderTargetingUi`,
      `CombatTargetingRenderPatches` observe-and-pass on `AbstractPlayer` and
      `PotionPopUp` (zero suppression), new mini-surface `sts1.combat.targeting`
      (OFF default, observe/full console, SurfaceDrawPlan entry, PresentSafety
      unmount list), `TargetingSessionComponent` c2-projection metadata filled by
      `Sts1PresentationBackend` soft-reads (`inSingleTargetMode`, hovered card,
      hovered monster), `TargetingDrawPath` pure bezier geometry with HostAssets
      arrow texture + plain-line fallback, renderer tail self-draw gated by FULL
      only. Note deviation: does not use `RenderTargetKind.OVERLAY` (dead mechanism
      pending Slice E cleanup). Update SDD pixel supply table, task.md, and
      api-stability/consumer additive notes; strict manifest check must remain
      ok=true unknown=0.
- [x] 47.16 Slice E1 — render-side hygiene cleanup: delete `C2ChromePainter` /
      `Sts1VanillaDraw`, remove `ClipRect` and `BatchStateGuard` production usage
      (keep `BatchStateGuard` deprecated), drop `batchArmed`/`clipEmpty` probe
      fields, document `RenderTargetKind.OVERLAY` as unused in production,
      eliminate duplicate `syncC2Item` in `renderHand`/`renderProceed`, record
      real draw counts per surface, and align `renderTopPanel` gate with
      `plan.drawOrder`.
- [x] 47.17 Slice E2 — backend projection field completion: epoch change clears
      c2-surfaces visual items (`PresentProjection.applyFrame`), `MapPanZoom`
      resets on non-map scene/epoch change, `EventOptionView`/`RewardItemView`
      geometry from native hitboxes, `ShopView` real inventory/prices/soldOut/
      purge cost from `ShopScreen`, and `IntentEntry.multiAmount` from monster
      intent data.
- [x] 47.18 Final D1 regression smoke after A–E: fresh `ArtFramework.jar` deploy +
      `d1_full_present_combat_ready` pass; `transientEffects.rendered` climbs
      0→52 with `failOpen=0`/`unknownLifecycle=0`, confirming no regression
      across container effect observation, room chrome, targeting family, backend
      field projection, and render hygiene changes.
- [x] 47.19 Top panel / proceed partial pixel supply (32c285f): supplied stable
      C2 HUD and proceed-button visual items, ResourceIds, bounds, labels, and
      focused tests while keeping native glow/hover/animation parity documented
      as pending.
- [x] 47.20 Controls / energy / intents partial pixel supply (db9423c): supplied
      enhanced end-turn, energy-orb, and monster-intent visual projections plus
      tests; remaining native animation/layer parity stays an exposed gap. Energy
      orb now supplies the native bright/dim layer stack with spin, 128px orb
      geometry, and the centered energy number.
- [x] 47.21 Reward / shop / treasure partial pixel supply (661e644): supplied
      visible reward rows, shop chrome/entries, treasure chest/relic rows, and
      tests with explicit fallback resources; full room-native parity remains
      tracked.
- [x] 47.22 Event / select partial pixel supply (328d3df): supplied event dialog
      panel/options and grid/hand select projected items, labels, states, and
      tests; base dialog/select parity remains pending.
- [x] 47.23 Event / select renderer evidence fail-open (8b7b846): hardened render
      evidence so missing projected items fail open visibly instead of being
      counted as delegated draw success.
- [x] 47.24 Rest / campfire partial pixel supply (f80a054): supplied campfire title
      and option-row chrome with ResourceIds, geometry, roles, state, and tests;
      base art/animation parity remains pending.
- [x] 47.25 Map node partial pixel supply (87d750c): supplied projected map-node
      textures, symbols, overlays, row:col identity, pan/zoom geometry, evidence,
      and tests; map background, edges, legend, and full pan/zoom parity remain
      documented gaps. **Correction (NRO-04 D03 defect fix):** the "map node pixel
      supply" recorded here was **probe-only** — `renderMap` registered C2 items and
      counted them but never submitted a texture, so the FULL map rendered
      background-only; the node/legend pixels are now actually submitted (see the
      D03 note below).
- [x] 47.26 D1 deploy/smoke status: device verification is blocked by adb
      shell/push timeout after `get-state=device`; no D1 success is claimed for
      this slice.
- [x] 47.27 D1 combat runtime evidence after recovery (2026-08-29): latest
      `ArtFramework.jar` deployed to D1 with matching 1,080,139-byte size and
      SHA-256 `c81b9a6ef227600fab775aa4b2934275e514b4a7f5cc4358f2e03fae3e612cc4`;
      `scripts/art-lab ready` reached READY and `scripts/art-lab combat verify-full`
      passed `d1_full_present_combat_ready` (1/1). This is combat-path evidence
      only; map/event/select/room visual parity still requires their own D1 scenarios.

**Current state:** the descriptor-aware static manifest is closed for the current
544/544 inventory and the Java full gate has passed. This is static inventory closure,
not proof that every runtime path or visual scene was exercised. Recent runtime-ledger
work closes all queued invocations for each rendered surface, cancels stale pending
invocations on native/off transitions, and closes lifecycle/recovery gaps; native visual
parity gaps remain documented in
[`native-render-coverage-sdd.md`](design/native-render-coverage-sdd.md).

- [x] 47.28 D1 deployment and combat smoke completed (2026-08-29): fresh jar
      deployment, READY polling, and `d1_full_present_combat_ready` passed.
      The follow-up map/event/reward/combat-chrome/lifecycle verifier batch did
      not run because the first `connect_console()` attempt blocked on the
      existing connector stream; `scripts/art-lab status` still reports READY.
      No room/map UI result is claimed from that blocked batch.
- [x] 47.29 D1 map/event/select/room acceptance: map/event executable FULL_READY fixtures
      pass on D1; deterministic navigation now also covers select and room surfaces. The
      executable map/event fixtures already
      exist for map (`d1_full_present_map_ready`) and event (`d1_full_present_event`).
      The lab now has native-state navigation commands for rooms
      (`art lab enter-room rest|shop|treasure`) and real-card selection
      (`art lab enter-select grid|hand`), with executable D1 fixtures. These commands
      preserve STS room/card authority and do not manufacture fixture cards. Final D1
      artifacts pass independently: select (`47-29-fixed-select-v13`), rest
      (`47-29-fixed-rest-v10`), shop (`47-29-fixed-shop-v2`), and treasure
      (`47-29-fixed-treasure-v3`) under `debug-artifacts/art-verify/`. Each verifies the
      matching scene, FULL policy, native suppression, and non-zero ART draw evidence.
      The 2026-08-29 post-cold-start attempts initially reached their console steps
      (`executed: true`) but failed because the D1 game-probe sidecar was not refreshed;
      the probe sidecar writer was then fixed to publish to the Harness-readable device
      path. Final artifacts `debug-artifacts/art-verify/fixture-fix-20260829/` pass map
      and event: map strict acceptance is closed, and event reaches `FULL_READY` with
      `eventDraw.count=4`, `suppressNativeEvent=true`, and final strict acceptance.
      Select/room acceptance is now closed by the independent artifacts above.
- [x] 47.30 Spine 4.2 RegionAttachment render adapter: the shaded provider now emits supported
      region quads into the active legacy libGDX batch through reflection, with pose/visual state
      application, draw-order traversal, color multiplication, and a pure vertex-order contract
      test.
- [x] 47.31 Spine 4.2 MeshAttachment batch compatibility: indexed mesh triangles are expanded
      into legacy Batch quad submissions with world vertices, UVs, and fail-open malformed-data
      handling. Clipping and two-color capability gates are implemented; pixel-parity validation
      remains the final explicit gap.
- [x] 47.32 Spine 4.2 clipping capability gate: the legacy Batch path pre-scans for
      `ClippingAttachment` and returns zero ART draws without mutating batch state, allowing the
      native renderer to recover.
- [x] 47.33 Spine 4.2 two-color batch capability gate: legacy `Batch.draw(Texture, float[], int,
      int)` exposes only the five-float position/packed-light-color/UV vertex contract, so the
      provider pre-scans slot/attachment dark-color presence and returns zero before any draw when
       two-color data is present. No partial rendering or false draw count is claimed.
- [x] 47.34 Spine 4.2 CPU parity contract: `Spine42Parity` validates RegionAttachment and
       MeshAttachment expansion, UV preservation, packed color propagation, triangle coverage,
       winding, degenerate geometry, malformed data, and expected-vector mismatches with a
       diagnostic `ParityResult`. This is
       a pure Java contract and does not claim GL or screenshot pixel equivalence.
- [ ] 47.35 Spine 4.2 real GL/screenshot pixel parity: remains open pending a controlled D1 capture
        of one canonical, approved developer-local reference skeleton/atlas/texture, named state,
        and fixed pose/time rendered by both native and ART with identical viewport, camera, clear,
        filtering, blend, and orientation settings. Crop both screenshots to the same documented
        pixel rectangle and record dimensions, format, color conversion, alpha policy, and thresholds
        T/R chosen before comparison. A pixel differs when any compared channel is > T; acceptance
        requires maximum channel difference <= T and differing-pixel ratio <= R. Evidence requires
        paired screenshots, comparison metrics, fixture/provenance, device/build/runtime metadata,
        crop/conversion metadata, and ART/native draw plus fail-open evidence. Clipping, two-color,
        unsupported attachments, and malformed data are explicit fail-open exclusions and cannot be
        counted as parity passes; no assets may be committed.

### 48. Native Render Memory Lifecycle

Supervised plan: [`docs/refacter/native-render-memory-lifecycle/`](refacter/native-render-memory-lifecycle/).
First bound retained native-render evidence without weakening exact NRCC correlation or strict
acceptance. Only after that closure is verified, use measured slices to reduce FULL-presentation
allocation and Young GC pressure.

- [x] 48.1 Freeze cumulative-count, retained-state, recent-history, and ID-query semantics; contract
      and focused tests are recorded in the refacter ledger.
- [x] 48.2 Reclaim terminal native-render correlation state with one-time cumulative accounting;
      default JUnit gate passes.
- [x] 48.3 Add bounded recent evidence and recovery-race tombstones; preserve strict failures;
      default JUnit gate passes.
- [x] 48.4 Prove bridge callback queues retire exact tokens and remain bounded; bridge/lifecycle
      focused tests and the default JUnit gate pass.
- [x] 48.5 Reclaim terminal transient-effect records in an independent lifecycle slice; semantic
      evidence is recorded in the native-render-memory-lifecycle ledger.
- [x] 48.6 Add bounded same-frame `SurfaceDrawPlan`/draw-order reuse with explicit frame, policy,
      scene, mount, readiness, overlay, executor, and panic invalidation; allocation measurement
      and D1 evidence remain pending.
- [x] 48.7 Verify the implemented generic differential RenderPlan reconciliation contract with
      system-local desired/managed sets. Focused tests passed 39/39, default JUnit passed 1227/1227,
      final D1 FULL combat passed 1/1, and independent review found no issues; no paired allocation
      improvement is claimed.
- [x] 48.8 Split lightweight automatic heartbeat from full probe snapshots with a 500ms/5000ms
      post-update cadence, separate schema/files, 2000ms stale marker, and unchanged ART_PROBE v1;
      semantic gates and corrected-jar D1 verification pass.
- [x] 48.9 Define and verify asset materializer cache/disposal lifecycle independently; bounded
      pure-Java value/missing caches, identity-safe disposal, and production clear disposal are
      implemented. Live GL/host evidence remains pending.
- [x] 48.10 Close with JUnit, applicable D1 FULL combat, bounded Arthas operational-health sample,
      review, and negative inventory evidence. Equivalent pre-fix stress comparison is unavailable;
      NRM-11 closes under the user-scoped original-game performance standard with no paired
      allocation-rate improvement claim.

- [x] NRO-04 D01 (draw pile panel full-present owner): registered the combat draw-pile panel
      (`DrawPilePanel.render`) as a new ART_DELEGATED full-present surface `sts1.combat.pile_draw`
      with `CombatPileDrawRenderPatches`, `PileDrawDrawPath` (native-authoritative draw-zone
      geometry from `Sts1PileSoulProjection`, native `DrawPilePanel` constant fallback),
      `PileDrawSurface`, plan/readiness wiring, `art present pile_draw` console target,
      `backend.pileDraw` probe, NRCC manifest row, and focused JUnit. Discard pile is untouched.
      D1 visual fix: the on-surface draw now reproduces both native layers with native textures and
      geometry — `UI_PILE_DRAW` -> `images/ui/deckButton/base.png` (`ImageMaster.DECK_BTN_BASE`) at
      `(show_x + DECK_X, show_y + DECK_Y)`, and `UI_PILE_COUNT_CIRCLE` ->
      `images/ui/topPanel/countCircle.png` (`ImageMaster.DECK_COUNT_CIRCLE`) at
      `(show_x + COUNT_OFFSET_X, show_y + COUNT_OFFSET_Y)` carrying the numeric count; `show_x =
      show_y = 0` confirmed from the `DrawPilePanel()` constructor bytecode. The old
      `images/ui/topPanel/cardPile.png` mapping does not exist in the jar.
- [x] NRO-04 D02 (top-panel settings gear pixel supply): `TopPanelDrawPath` now supplies the
      top-right settings button (`TopPanel.renderSettingsIcon`) as `top_panel.settings`
      (`ResourceIds.UI_TOP_PANEL_SETTINGS` -> `images/ui/topPanel/settings.png`, 64x64) so it no
      longer disappears when the top panel is FULL. Geometry mirrors the verified native constants
      `SETTINGS_X = Settings.WIDTH - (ICON_W + TOP_RIGHT_PAD_X)`, `ICON_W = 64f*Settings.scale`,
      `TOP_RIGHT_PAD_X = 10f*Settings.scale`, `ICON_Y = Settings.HEIGHT - ICON_W`, producing
      `x = SETTINGS_X - 32f + 32f*scale`, `y = ICON_Y - 32f + 32f*scale`, `w = h = 64f*scale`
      (`yFromTop = 32f - 32f*scale`). Rendered at rest state rotation 0. The native hover /
      settings-screen GEAR SPIN (`updateSettingsButtonLogic` accumulating `settingsAngle`) is
      intentionally NOT replicated = documented gap; D1 evidence is an at-rest A/B. The top panel
      remains an all-or-nothing `ART_DELEGATED` surface with partial pixel supply: no new patch, no
      wholesale-gating change, default OFF; deck and map buttons remain uncovered (later slices).
- [x] NRO-04 D03 (map legend pixel supply): `MapDrawPath` now supplies the map legend that the
      wholesale map suppression (`MapRenderPatches`) removed — the `Legend.render` panel
      (`ResourceIds.UI_MAP_LEGEND` -> `images/ui/map/legend2.png`, 512x800) plus a title carrier and
      the 6 `LegendItem.render` room-type icon/label rows (EVENT/MERCHANT/TREASURE/REST/ENEMY/ELITE
      reusing the existing `MAP_NODE_*` textures; labels are localized from the native
      `CardCrawlGame.languagePack.getUIString("Legend").TEXT` UIStrings — see the D03 follow-up
      below — with the English names only as the fail-open fallback). Geometry mirrors the verified native
      constants: `Legend.X/Y = 1670f*xScale/600f*yScale`, panel `(X-256, Y-400, 512*scale,
      800*yScale)`, `LegendItem.ICON_X = 1575f*xScale`, `TEXT_X = 1670f*xScale`, `SPACE_Y =
      58f*yScale`, `OFFSET_Y = 100f*yScale`, icon `(ICON_X-64, Y - SPACE_Y*i + OFFSET_Y - 64,
      128*scale/1.65, same)` at REST. Items sync as C2 map items (`legend.panel`, `legend.title`,
      `legend:<room>`; role `map-legend`) exactly like the node items and are counted in the
      recorded `drawn` evidence; the legend is omitted when `Settings` is unavailable (fail-open).
      Documented gaps: hover icon scale (`/1.2`)/tip, controller reticle, and the legend alpha
      fade-in (`Legend.c.a` lerp) are NOT replicated; drawn at rest/full alpha. Edges, background,
      boss and current-node circle remain uncovered. No new patch, no wholesale-gating change.
      **Discovered D1 defect (fixed):** the map surface registered C2 node/legend items but
      `Sts1SurfaceRenderer.renderMap` only COUNTED them and called `recordSurfaceDrawIfPending`,
      never submitting any texture — so a FULL map rendered background-only even though the node
      probe reported `artFound=true`/`missingArt=0` (registration != pixel submission, invisible to
      probe-only checks). `renderMap` now builds an ordered pure `MapDrawPath.mapSubmissionPlan()`
      and submits each via `drawResolvedTexture`, fail-open per draw; the legend panel + 6 icons are
      now actually drawn. **Legend layer order:** native `DungeonMap.render` draws `Legend.render`
      BEFORE `DungeonMapScreen.render` draws the nodes, so nodes paint OVER the legend; the plan
      therefore emits the legend block FIRST, then the node block (icon, outline when
      `reachable||highlighted`, overlay when `pinned||highlighted`), and the synced legend z values
      sit BELOW the node band (legend.panel z=0.1, legend icons z=0.15, legend.title z=0.2; nodes
      stay 1/2/3). Resource value: the plan keeps the LOGICAL id (e.g. `map.node.monster`) because
      `assets().resolve(resourceId)` resolves these catalog-mapped vanilla ids (`found=true`),
      identical to the sibling surfaces — no `artSource` substitution needed (pinned by test
      `mapNodeAndLegendIdsResolveAsLogicalIds`). **Evidence limitation (honest):** the renderer unit
      test drives `renderMap` with a `null` SpriteBatch, so it proves only that `renderMap` iterates
      the plan and records evidence — it does NOT capture an actual GL texture/font draw (no
      batch-capture unit test, consistent with the other C2 surface renderers); the real pixel
      submission evidence is the device `submitCount` + screenshot. Correction to the record:
      47.25's "map node pixel supply" was **probe-only** and is now actually submitted.
- [x] NRO-04 D04 (reward screen panel/sheet pixel supply): the reward surface is suppressed wholesale
      by `RoomRenderPatches` (`CombatRewardScreen.render`), and `RewardDrawPath`/`renderReward` painted
      the reward ROWS but never the whole-screen panel, so rows floated on no background. Native
      `CombatRewardScreen.renderItemReward` draws `ImageMaster.REWARD_SCREEN_SHEET`
      (`images/ui/reward/rewardScreenSheet.png`, verified present in `$ART_STS_JAR`, 612x716) at
      bottom-left `(Settings.WIDTH/2 - 306f, Settings.HEIGHT/2 - 46f*Settings.scale - 358f)` with size
      `(612*xScale, 716*scale)` (verified bytecode). Added `ResourceIds.UI_REWARD_SHEET`
      (`ui.reward.sheet`) to the minimal list and catalog-mapped it; `RewardDrawPath.sheetItem()` emits
      the sheet FIRST at C2 z=0.5 (rows stay z=1) so it paints BEHIND the rows, using the same CENTER
      convention as the rows (center = `(WIDTH/2 - 306 + 306*xScale, HEIGHT/2 - 46*scale - 358 +
      358*scale)`; at scale=xScale=1, WIDTH=1920, HEIGHT=1080 → center `(960,494)`, bottom-left
      `(654,136)`, `612x716`). `probeSlice()` gains `sheet`/`sheetCount`/`submitCount` (sheet kept out
      of `items[]` so the public row list is unchanged); `prepareRewardVisuals` syncs it as C2 id
      `reward.sheet` role `reward-panel`; `renderReward` draws via `drawOrder()` (sheet first; empty
      label draws no text) and records sheet+row `submitCount`. D1 assertion added to
      `d1_full_present_reward.yaml`. **Recorded honestly:** the reward TITLE is a separate
      `AbstractDungeon.dynamicBanner` system (out of scope here); `UI_REWARD_PANEL ->
      images/ui/reward/rewardList.png` is a MISSING-FILE landmine (same class as the D01
      `cardPile.png`; consumers are the later shop/campfire slices); decoration/outline/glow and the
      per-row icon/color are still uncovered, so full native reward parity remains pending.
- [x] NRO-04 D05 (shop rug background pixel supply): the shop surface is suppressed wholesale by
      `RoomRenderPatches` (`ShopScreen.render`), and `ShopDrawPath`/`renderShop` painted only ~5 text
      rows, so the shop floated on the dimmed map room with NO rug. Native `ShopScreen.render` first
      draws `rugImg` full-screen: `sb.draw(rugImg, 0f, rugY, Settings.WIDTH, Settings.HEIGHT)`, where
      `rugImg` is a LANGUAGE-SPECIFIC texture chosen in `ShopScreen.<clinit>` (`images/npcs/rug/
      <lang>.png`; DEU/EPO/FIN/FRA/ITA/JPN/KOR/RUS/THA/UKR/ZHS, default ENG->eng.png; all 12 files
      exist in the jar at 1920x1136). Added `ResourceIds.UI_SHOP_RUG_PREFIX = "ui.shop.rug."` +
      `ResourceIds.shopRug(language)` (`null`/empty -> `eng`) and catalog-mapped all 12
      `shopRug(<lang>)` -> `images/npcs/rug/<lang>.png`; `ShopDrawPath.rugResourceId()` reads
      `Settings.language.name()` lowercased, validates via `Sts1VanillaCatalog.isKnown`, and falls
      back to `shopRug("eng")` when unknown/absent (all `Settings` reads fail-open). `rugItem()` emits
      the full-screen item at the SETTLED rest position `x=0`, `y=Settings.HEIGHT/2f - 540f*yScale`
      (= 0 at the default 1920x1080, scales=1), `w=Settings.WIDTH`, `h=Settings.HEIGHT`, ambient (no
      color override). `probeSlice()` gains `rug` `{resourceId,x,y,w,h}` + `rugCount` + `submitCount`
      (rug kept out of `items[]` so the public row list is unchanged). `prepareShopVisuals` syncs the
      rug as C2 id `shop.rug` role `shop-background` at z `0.5` (below the row z `1`); `renderShop`
      draws the rug FIRST so it paints behind the rows and counts it in the recorded draw evidence.
      D1 assertions added to `d1_full_present_shop.yaml` (`rug.resourceId` exists + `contains
      "shop.rug."` + `rugCount>=1`). **Honest gaps:** the native rug ENTRANCE SLIDE is NOT animated
      (`open()` sets `rugY = HEIGHT`, `updateRug()` lerps to the settled value) — ART paints the rest
      position only; merchant Spine art, entry card/relic/potion art, prices, and leave/tooltip remain
      uncovered. **Landmine recorded:** `UI_SHOP_PANEL`/`UI_REWARD_PANEL`/`UI_CAMPFIRE_PANEL`/
      `UI_TREASURE_PANEL` -> `images/ui/reward/rewardList.png` DOES NOT EXIST in the jar (same class
      as D01's `cardPile.png`); consumers are reward/shop/campfire/treasure chrome — left for later.
- [x] NRO-04 D06 (event dialog panel + title native geometry): the event surface is suppressed
      wholesale by `EventRenderPatches` (`GenericEventDialog.render`), and `EventDrawPath` painted
      the panel/title with a generic centered rectangle, so the dialog panel landed in the wrong
      place at any non-default resolution/scale. Native `GenericEventDialog.render` draws
      `AbstractDungeon.eventBackgroundImg` = `images/ui/event/panel.png` (EXISTS) via
      `sb.draw(img, WIDTH/2f - 881.5f - 12f*xScale, EVENT_Y - 403f - 64f*scale, 881.5f, 403f, 1763f,
      806f, xScale, scale, 0f, 0,0,1763,806, false,false)` (verified bytecode); libgdx places the
      sprite's bottom-left at `x + originX*(1-scaleX)` and the drawn size is `(1763*xScale,
      806*scale)`, so with `originX = 881.5 = 1763/2` the panel CENTER — the convention
      `DrawItem.x/y` use — is `(WIDTH/2 - 12*xScale, EVENT_Y - 64*scale)` and its size is
      `(1763*xScale, 806*scale)`.
      `Settings.EVENT_Y` is read directly with a fail-open fallback `HEIGHT/2f - 128f*scale` (the
      `Settings` static-init formula). The title is anchored at native
      `TITLE_X = 570f*xScale`, `TITLE_Y = EVENT_Y + 408f*scale` (verified `GenericEventDialog`
      bytecode) as a centered font draw. `buildFromProjection()` panel item now carries the native
      center/size; the title item carries the native center. `probeSlice()` gains `panel`
      `{resourceId,x,y,w,h}` + `panelCount` + `submitCount` (panel stays in `items[]`, so the public
      item-list is unchanged; `submitCount` == `materializedDrawCount()` because the event panel is
      already an item, unlike the D04 reward sheet). The event TITLE item is now TEXT ONLY
      (`DrawItem.textOnly`, `renderEvent` skips `drawResolvedTexture` for it) because the title's
      ResourceId `UI_EVENT_TITLE` is MIS-MAPPED to `images/ui/event/panel.png`; submitting it would
      paint a bogus panel-sized rectangle. At `scale = xScale = 1, WIDTH = 1920, HEIGHT = 1080,
      EVENT_Y = 412` the panel center is `(948, 348)` and the size is `1763x806`; title center
      `(570, 820)`. Tests pin unit + non-unit (`scale=1.25, xScale=1.5`) geometry; D1 assertions added to
      `d1_full_present_event.yaml` (`panelCount>=1`, `panel.resourceId eq ui.event.panel`,
      `panel.w/h >= 1`). **Honest gaps:** the per-event illustration (`img` + `EVENT_IMG_FRAME`),
      the event body text animation (`DialogWord`), and option-button hover/color states are NOT
      covered; `UI_EVENT_TITLE -> panel.png` mis-map noted (left as-is).
- [x] NRO-04 D07 (campfire option buttons at native `CampfireUI` grid + `AbstractCampfireOption`
      NORM_SCALE icon geometry): the rest surface is suppressed wholesale (`RoomRenderPatches` gates
      `CampfireUI.render`) and `renderRest` painted each option as a synthetic 360x40 text row,
      which STRETCHED the 256x256 option icon. Native `CampfireUI.renderCampfireButtons` places
      button index `i` at `x = (i%2==0) ? WIDTH*0.416f : WIDTH*0.416f + 300f*xScale` and
      `y = HEIGHT/2f + 180f*scale` for row 0, `- 200f*scale*(i/2) - 70f*scale` for rows >= 1
      (integer `i/2`); `AbstractCampfireOption.render` draws the option `img` with
      `scaleX = scaleY = scale` where the at-rest `scale = NORM_SCALE = 0.9f * Settings.scale`
      (verified static init; `HOVER_SCALE = Settings.scale` is the hover size and is NOT
      replicated), so the at-rest icon is `256 * 0.9 * Settings.scale = 230.4f * Settings.scale`.
      `RestDrawPath.DrawItem` uses named constants `NATIVE_ICON = 256f` and
      `NORM_SCALE_FACTOR = 0.9f`, carries the native button center (`centerX/centerY`, additive)
      with top-left `x/y` and `w = h = NATIVE_ICON * NORM_SCALE_FACTOR * scale = 230.4f * scale`;
      `buildFromProjection` uses the raw 0-based option index. `probeSlice()`
      gains `buttonCount` + a `buttons[]` list `{id,resourceId,x,y,w,h,centerX,centerY,enabled}`
      (public `items[]` keeps its keys, now with the native geometry plus `centerX/centerY`).
      Option icon resources (all 256x256, present in the jar) are `sleep.png`/`smith.png`/`dig.png`/
      `recall.png`/`toke.png`/`outline.png`, already selected by `resourceForOption`.
      `prepareRestVisuals`/`syncRoomChromeItems` sync each option at the native bounds (role per
      `roleForOption`); `renderRest` draws each option's 256x256 icon via `drawResolvedTexture` and
      no longer stretches. The campfire TITLE stays TEXT ONLY: its `UI_CAMPFIRE_PANEL` resource maps
      to the MISSING `images/ui/reward/rewardList.png` (same landmine class as `UI_REWARD_PANEL`/
      `UI_SHOP_PANEL`), so submitting it would paint a bogus rectangle. At `scale = xScale = 1,
      WIDTH = 1920, HEIGHT = 1080` button centers are `(798.72, 720)`, `(1098.72, 720)`,
      `(798.72, 450)` and the at-rest icon size is `230.4x230.4`. Tests pin 1-/2-/3-button layouts
      plus a non-unit `scale=1.25, xScale=1.5` axis-mixup case (including left-column centerX
      independence from xScale); the D1 assertion only checks PRESENCE and POSITIVE size
      (`backend.restDraw.buttonCount >= 1` and `backend.restDraw.buttons[0].w/h >= 1`), not exact
      native values. **Honest gaps (NOT covered here):** the
      hover OUTLINE (`CAMPFIRE_HOVER_BUTTON`) + hover scale/color, the option label/description
      text, the disabled grayscale shader, the scroll variants (>6 buttons), and the campfire
      background/title art.
- [x] NRO-04 D07 follow-up (campfire option label anchoring + synthetic title removal): a D1 visual
      review found ART's campfire option labels overlapped the option icons and a centered synthetic
      "Campfire" title was occluded/garbled. Re-read the native source: `AbstractCampfireOption.render`
      draws the option icon at `hb.cX-128, hb.cY-128` (256x256, center `hb.cX,hb.cY`) and the LABEL with
      `FontHelper.renderFontCenteredTopAligned(sb, FontHelper.topPanelInfoFont, this.label, this.hb.cX,
      this.hb.cY - 60f*Settings.scale - 50f*Settings.scale*(this.scale/Settings.scale), usable ?
      Settings.GOLD_COLOR : Color.LIGHT_GRAY)`; at rest `scale = NORM_SCALE = 0.9f*Settings.scale`, so the
      label TOP Y = `hb.cY - 105f*Settings.scale` and X = `hb.cX` (centered, TOP-ALIGNED). `CampfireUI.render`
      draws NO campfire site title (only `renderFire`, `AbstractDungeon.player.render`, bubbles,
      `bubbleMsg`, `renderCampfireButtons`, the scrollbar, and the touch confirm button), so the ART
      synthetic "Campfire" title was NOT native and is REMOVED. `RestDrawPath` gains a documented
      `LABEL_OFFSET_Y = 105f` constant and `labelOffsetY()` (`LABEL_OFFSET_Y * Settings.scale`, fail-open);
      `DrawItem` carries `labelAnchorX = centerX` and `labelAnchorY = centerY - LABEL_OFFSET_Y*scale`;
      `probeSlice()` gains `hasTitle` (false) plus per-button `labelAnchorX`/`labelAnchorY` (public
      `items[]` keeps every key, now including both anchors). `renderRest` drops the title draw and the
      old centered `renderFontCentered(..., bounds.y + bounds.height*0.54f)` label, drawing each option's
      icon via `drawResolvedTexture` and its label via
      `FontHelper.renderFontCenteredTopAligned(sb, FontHelper.topPanelInfoFont, text, centerX,
      centerY - 105f*scale, enabled ? Settings.GOLD_COLOR : Color.LIGHT_GRAY)` so it sits BELOW the icon.
      Tests pin the native anchor formula at unit scale (`(798.72, 720) -> labelAnchorY 615`) and at
      `scale=1.25` (`centerY 765 -> 633.75`), plus `hasTitle == false` and the reduced draw counts
      (2 options / 7-option mix). `d1_full_present_rest.yaml` asserts `hasTitle` falsey and, because
      `lt` is unsupported, captures `buttons[0].labelAnchorY` and asserts `buttons[0].centerY gt_var`
      it. **Honest gaps (NOT covered here):** campfire background/flame FX (separate room-scene slice),
      the option DESCRIPTION text (native draws it at fixed `950f*xScale, HEIGHT/2f + 20f*scale`; not
      carried by `RestView.RestOptionView`/`RoomChromeLine`), the hover OUTLINE/scale/color + tip, the
      disabled grayscale shader, scroll variants (>6 buttons), and the touch confirm button.
- [x] NRO-04 D08 (treasure chest sprite at native `AbstractChest` geometry): the treasure surface was
      suppressed wholesale and `renderTreasure`
      painted text rows, but the actual chest sprite (`AbstractChest.render`) was neither projected
      nor drawn, and the existing chest mappings were wrong (`UI_TREASURE_CHEST_CLOSED ->
      images/ui/map/chest.png`, `..._OPEN -> chestOutline.png`). Native `AbstractChest.render` draws
      a 512x512 texture with center `(Settings.WIDTH/2f + 348f*scale, AbstractDungeon.floorY +
      192f*scale)` and size `512*scale x 512*scale` (at-rest rotation 0; the `rotation=180f` open
      animation is NOT modelled). When `isOpen` and an `openedImg` exists, native draws
      `openedImg`; at rest it draws `img`. Added `ResourceIds.chestSprite(kind, opened)` ->
      `ui.treasure.chest.<kind>[.opened]`; catalog maps the 8 ids to the EXISTING jar files
      `images/npcs/<kind>Chest.png` / `<kind>ChestOpened.png` (all 512x512) for
      `SmallChest/MediumChest/LargeChest/BossChest` -> `small/medium/large/boss`.
      `Sts1PresentationBackend.liveTreasureView` now projects the sprite from the live chest class
      simple name + `isOpen` (unknown/unreadable -> medium CLOSED; never throws), carried on a new
      `TreasureView.chestResourceId`. `TreasureDrawPath.chestItem()` emits the sprite FIRST at C2
      z=0.5 (rows stay z=1) behind the text rows, with the native CENTER convention
      (`chestItemAt` pure geometry: at `scale=1, WIDTH=1920, floorY=y` center `(1308, y+192)`, size
      `512x512`); `probeSlice()` gains `chest`/`chestCount`/`submitCount` (chest kept out of the row
      list). `prepareTreasureVisuals` syncs it as C2 id `treasure.chest.sprite` role
      `treasure-chest-sprite`; `renderTreasure` draws it first via `drawResolvedTexture` and an empty
      row label draws no text. D1 assertions added to `d1_full_present_treasure.yaml`
      (`chestCount>=1`, `chest.resourceId` exists/contains `chest`, checked IMMEDIATELY after the
      scene wait because the lab treasure room auto-advances to the map and a late probe is flaky).
      **Regression fix (D1 finding):** the treasure suppression was moved OFF
      `TreasureRoom.render` (`RoomRenderPatches`) and onto `AbstractChest.render` (new
      `TreasureChestRenderPatches`, surface family still `sts1.treasure`). Suppressing the whole
      room also removed `AbstractRoom.render`, which draws `AbstractDungeon.player.render` (the
      swordsman sprite) in non-event rooms, so the PLAYER disappeared from the treasure room and ART
      supplied no player. The room now renders natively (player + tips) and ART owns only the chest
      draw; the chest sprite is still supplied by ART. Manifest rows moved accordingly
      (`AbstractChest.render` -> `sts1.treasure`/`ART_DELEGATED`; `TreasureRoom.render` -> no hook).
      **Honest gaps (NOT covered):**
      the open ANIMATION (native `rotation=180f`/glow), hover additive highlight, chest shine
      particles, the chest-room background, and the relic-get panel (that is the D04 reward surface)
      remain pending. **Mis-maps recorded (left as-is):** `UI_TREASURE_CHEST_CLOSED/OPEN` -> map
      icons are TEXT-ROW ids, not the sprite; `UI_TREASURE_PANEL -> images/ui/reward/rewardList.png`
      is a MISSING-FILE landmine (same class as D01/D04/D05).
- [x] NRO-04 D09 (select confirm button at native `CardSelectConfirmButton` geometry): the select
      surface is wholesale-suppressed (`SelectRenderPatches` gates `GridCardSelectScreen.render`/
      `HandCardSelectScreen.render`) and `renderSelect` already paints items via
      `drawResolvedTexture`, but the confirm button used a generic 360x48 rectangle on
      `images/ui/event/enabledButton.png`. Verified native bytecode
      (`com.megacrit.cardcrawl.ui.buttons.CardSelectConfirmButton.renderButton`):
      `TAKE_Y = 475f * Settings.scale`; the enabled draw uses
      `ImageMaster.REWARD_SCREEN_TAKE_BUTTON` = `images/ui/reward/takeAll.png` and the disabled draw
      uses `ImageMaster.REWARD_SCREEN_TAKE_USED_BUTTON` = `images/ui/reward/takeAllUsed.png` (both
      files exist in `$ART_STS_JAR`, 512x256); geometry is
      `sb.draw(texture, WIDTH/2f - 256f, TAKE_Y - 128f, 256f, 128f, 512f, 256f, scale, scale, ...)`,
      so the button CENTRE is `(WIDTH/2f, 475f*scale)` and its SIZE is `(512f*scale, 256f*scale)`
      (both axes use `Settings.scale`; the label is centred on the button). `SelectDrawPath` now
      emits the confirm item at that native geometry via `confirmItem()` with fail-open `Settings`
      reads (defaults `WIDTH=1920`, `scale=1`), keeping the `UI_SELECT_CONFIRM`/`_DISABLED`
      resource selection and the `confirm` id/role; `probeSlice()` gains a `confirm` sub-map
      `{resourceId,x,y,w,h,enabled,visible}` (existing `confirmEnabled`/`confirmVisible` kept).
      Catalog maps `UI_SELECT_CONFIRM -> images/ui/reward/takeAll.png`,
      `_DISABLED -> images/ui/reward/takeAllUsed.png`. `d1_full_present_select.yaml` asserts
      `backend.selectDraw.confirm.resourceId eq ui.select.confirm` and `confirm.w gte 1`.
      **Honest gaps (NOT covered):** the per-card `cardui/frame` atlas key is NOT file-backed (card
      pixels are not supplied), so card rectangles remain a documented gap; the select
      panel/background and tip/header are not mapped; cancel/skip buttons are not covered;
      hover/controller states are not replicated. **Missing-file landmines recorded (not fixed):**
      `UI_SELECT_CARD* -> cardui/frame` (atlas key, not a loose file) and
      `UI_PANEL_DEFAULT`/`UI_REWARD_PANEL -> images/ui/reward/rewardList.png` (same class as
      D01 `cardPile.png`).
- [x] **FIXED (D01/D02 D1 finding): render-thread concurrency race in `PresentationVisuals.syncC2Item`.**
      During D01/D02 device work one `java.util.ConcurrentModificationException` was observed in the
      post-native render hook: `PresentationWorld.query(PresentationWorld.java)` iterating a
      `LinkedHashMap` inside `PackSurfaceEffects.forSurface` -> `PresentationVisuals.effectsFor` ->
      `syncC2Item`, called from `Sts1SurfaceRenderer.render` (the `preparePileDrawVisuals` path) from
      `StageHost.receivePostRender` under `CardCrawlGame.render`. It was NOT reproducible (5/5 `top`
      and 5/5 `pile_draw` on/off toggles, plus 6 rapid mixed toggles, all stayed READY with zero
      crash markers), so it is a LATENT general toggle/render race, not a D01/D02-specific defect and
      not specific to the top panel (the stack was the pile-draw C2 path; `top off` was coincidental).
      **Fix:** the ECS query / surface-effects iteration in the post-native render hook is now
      iteration-safe under concurrent mutation. `PresentationWorld` keeps a lock-free creation-order
      index (`ConcurrentSkipListMap<Long,EntityId>` keyed by the monotonic `EntityId.value()`) whose
      weakly consistent iterator never throws CME; `query`/`entities` iterate that instead of the
      live `LinkedHashMap` entry/key iterator, so a concurrent
      `createEntity`/`destroyEntity`/`clear`/`close` on the game thread cannot throw CME and never
      yields stale ids. `getIfPresent` is a NON-THROWING per-entity lookup used by
      `PackSurfaceEffects` (`forSurface`/`hasContribution`/`surfaceIds`) so that if an entity is
      destroyed BETWEEN the public `query(...)` returning an id and that per-entity read, the read
      returns null/skips instead of throwing `IllegalArgumentException("unknown entity")`/NPE. The
      `PresentationWorld` null/contains guards on the query paths are only cheap defensive safety
      nets; the CME prevention is the weakly-consistent skip-list iteration, not the guards.
      `PresentationContext.entities()` is on the
      same render path (via `PresentationVisuals.retainC2Items`/`removeC2Items`) and got the same
      skip-list index. Note the naive "copy the entry set" snapshot (`new ArrayList<>(entrySet())`)
      is NOT safe — `ArrayList(Collection)` calls `toArray()`, which iterates the live map and still
      throws CME (verified) — hence the concurrent index. This addresses the CME symptom path and
      introduces NO global locking: the world is otherwise still thread-confined and the render
      thread never blocks. Bounded concurrent stress tests in `PresentationWorldTest` and
      `PackSurfaceEffectsTest` (proven non-tautological: both fail with CME / "unknown entity" when
      the safe path is reverted).
- [x] **NRO-04 D03 (map node texture COLOR fidelity + current-node ring).** Map nodes and their
      outlines now carry the resolved native tint instead of being submitted at WHITE. `MapNodeView`
      gained `available` (native `MapRoomNode.color == AVAILABLE_COLOR`, compared r/g/b) and
      `current` (`AbstractDungeon.firstRoomChosen && getCurrMapNode() == node`); `Sts1PresentationBackend
      .mapFrame` populates both (fail-open false). `MapDrawPath.DrawItem` resolves
      `nodeColor = taken || available ? AVAILABLE(0.09,0.13,0.17,1) : NOT_TAKEN(0.34,0.34,0.34,1)` and
      `outlineColor = highlighted ? (0.9,0.9,0.9,1) : OUTLINE(8c8c80ff)`, exposed in probe as
      `color`/`outlineColor`/`colorHex`/`outlineColorHex`/`currentNode` plus `colorSamples`.
      `Sts1SurfaceRenderer.drawResolvedTexture` gained a tint overload (`sb.setColor` before draw,
      restore WHITE after; 3-arg delegates with 1,1,1,1) and `renderMap` draws the outline/node with
      those colors and the `ui.map.circle5` (`ImageMaster.MAP_CIRCLE_5`, `images/ui/map/circle5.png`,
      verified in `desktop-1.0.jar`) ring at AVAILABLE_COLOR under the native predicate
      `taken || currentNode`. D1 scenario asserts `colorSamples` = native constants and that a node
      color is not white. **Honest gaps:** legend hover scale/tip, legend alpha fade-in, map edges,
      map background/paper panel, boss icon, node hover FX, mobile scaling, and the native oscillating
      alpha for the current node are NOT replicated; the ring is drawn at the projected node-box scale
      (192/128 ratio) rather than the live native `(nodeScale*0.95+0.2)*Settings.scale` factor because
      the projection exposes no per-node scale. D1 A/B (native map OFF vs ART map ON node darkness) is
      the parent's task.
- [ ] **Open (D05 D1 finding): full-screen room backdrops vs native UI layering.** The D04/D05
      full-screen sheets (reward `ui.reward.sheet`, shop rug `shop.rug.*`) are represented by
      `REWARD_COMBAT`/`SHOP` in `RenderPhase.C2_CONTENT`, so their C2 targets submit ABOVE the
      native-retained phase; a full-screen backdrop can therefore cover native-room UI that ART does
      not repaint. Observed on D1: in the ART shop frame the native `返回` back banner sits under the
      rug (native itself draws the rug inside the room, below `AbstractDungeon.render`'s
      `renderAboveTopPanel`/overlay pass, so native UI draws on top). Also the D1 shop OFF capture was
      the dimmed room, not a populated native shop, so no true-native baseline was obtained. Impact
      is bounded today (shop items are ART-repainted text rows; reward title is a native banner), but
      the general fix is to represent full-screen room-background chrome in a phase below native
      retained content (a dedicated room-background phase) or to repaint the missing native UI.
      Next: confirm against a real native shop capture whether the `返回`/skip controls are actually
      occluded, then place room backdrops in a lower phase (or supply the controls).
- [ ] **Open (D06 D1 finding): `art lab enter-event` crashes the render thread and its no-arg form is
      an unsupported command.** `art lab enter-event` with no event id routes to
      `StsLabNav.enterEvent("")`, `LabEventIds.normalize("")` returns `""`, and
      `StsLabHost.enterEvent` returns `unavailable("unsupported event: ")` (the room is never opened).
      With a supported alias (`world_of_goop` — note the console tokenizer only forwards the FIRST
      token, so multi-word input like `World of Goop` is truncated to `World`), `StsLabHost.enterEvent`
      injects a fresh `EventRoom` via `EventHelper.getEvent` and sets `node.room`, which then crashes
      the next frame: `java.lang.NullPointerException at
      com.megacrit.cardcrawl.dungeons.AbstractDungeon.render(AbstractDungeon.java:2704)`. The D06
      scenario was changed to stay on the run's opening Neow event (a real `GenericEventDialog`
      event) rather than use this lab path. Next: make `enter-event` (a) accept a multi-token alias
      (join remaining tokens) and (b) not leave the injected EventRoom in a render-NPE state (ensure
      `AbstractDungeon.screen`/overlay/fade and the event's dialog are fully initialized, or route
      through the native map-navigation intent `StsLabNativeNavigator.enterEventRoom`).
- [ ] **Open (D07 D1 finding): campfire option label anchoring + title clipping + FX gap.** D1 visual
      review of the ART rest frame confirmed the option buttons now sit at the native two-column grid
      (correct centers/size), but showed: (a) the option labels (休息/锻造) are drawn OVER the button
      faces, whereas native anchors each label BELOW its plate (`AbstractCampfireOption` label offset);
      (b) the ART centered "Campfire" title text is partly hidden behind the right option plate
      (z-order/layering), and native does not show that center string; (c) the native campfire glow/
      flame FX is absent on the ART frame (room-scene art, not modelled). Impact is bounded (buttons
      are correct), but label anchoring and title z-order should be fixed to match native. Next: anchor
      option labels below the plates per `AbstractCampfireOption`, drop or reposition the centered
      title so it is not occluded, and treat the campfire background/FX as a separate room-scene slice.
- [x] **DONE (D08 follow-up): room full-present treasure text-chrome overlap.** D1 visual review
      of the ART treasure frame showed the static `Treasure` title line and the `Chest closed` status
      line colliding/overlapping (garbled `Chest=closed`). Verified native truth
      (`TreasureRoom.render` is only `if (chest != null) chest.render(sb); super.render(sb);`;
      `AbstractChest.render` draws the sprite alone): the room draws NO "Treasure" title and NO
      "Chest closed/opened" status text, so both were ART inventions. Removed the synthetic `title`
      row and the synthetic `chest` status row from `TreasureDrawPath.chromeLines()`: the CLOSED
      state now projects ZERO chrome rows (chest sprite only = the D08 deliverable) and the OPENED
      state projects only the single `relic` row (anchored to its own row 0); its label uses the
      projected `relicLabel` and is empty (no text) when absent instead of a synthetic
      `"Chest opened"` string. `renderTreasure` is unchanged geometrically (chest sprite first) and
      now draws at most that one row, so the overlap is gone. `probeSlice()`/`materializedDrawCount`
      semantics follow the reduced rows; `TreasureDrawPathTest`/`RoomChromeRenderTest` assert closed
      = 0 rows / open = 1 relic row (chest-sprite geometry asserts kept);
      `d1_full_present_treasure.yaml` now asserts `chromeLineCount`/`drawCount` `lte: 1`.
      **Honest remaining gaps:** the chest-open animation/glow (`rotation=180f`), the hover additive
      highlight, the chest shine particles (`ChestShineEffect`/`SpookyChestEffect`), the chest-room
      background, and the relic-get panel (the D04 reward surface) remain pending. The chest sprite
      still reads pale/desaturated vs the native dark-wood + gold palette; it is the right sprite at
      the right place, but any tint/palette correction is left as a separate polish item (no tint is
      applied by ART).
- [x] **Open (D08 follow-up, tooling): NRCC generator still points treasure at the room hook.**
      `tools/nrcc/coverage_manifest.py` doc-maps (`known_policy`/`known_surface_id`/
      `known_justification`/`known_test`) still map `TreasureRoom.render -> sts1.treasure/ART_DELEGATED`.
      The committed manifest was hand-retargeted to `AbstractChest.render` (D08 fix) and the
      scanner/ownership tests pass, but a future `--write-manifest` regeneration would revert it to
      the room hook. Next: retarget those generator maps from `TreasureRoom.render` to
      `AbstractChest.render` so regeneration is stable. **DONE:** all four maps retargeted to
      `("com.megacrit.cardcrawl.rewards.chests.AbstractChest", "render")`; the room entry now resolves
      to the `room-shells` family default (native). Regression test
      `test_coverage_manifest.CoverageManifestTest.test_regeneration_retargets_treasure_delegation_to_abstract_chest`
      writes a synthetic manifest with both paths and fails on generator revert; `known_test` uses the
      real `RoomRenderPatchesTest.fullReadySuppressesNativeTreasureChestRender`. Regeneration verified
      stable by a full `scan_sts_render.py --sts-jar "$ART_STS_JAR" --write-manifest <tmp>` run
      (`ART_STS_JAR` is configured in `.env.local`): the regenerated `abstractchest`/`treasureroom`/
      `treasureroomboss` rows match the committed manifest exactly (`--check-manifest` ok:true).
- [x] **Open (NRCC, from D08 follow-up review): regeneration drops hand-annotated suppression metadata.**
      A full `--write-manifest` regeneration still differed from the committed manifest on exactly five
      rows — `AbstractDungeon.render`, `RewardItem.render`, `AbstractGameEffect.render` (both the
      `(SpriteBatch)` and `(SpriteBatch;FF)` overloads), `AbstractStance.render`, and
      `DrawPilePanel.render` — each losing/simplifying hand-annotated `suppression*` /
      `conditionalSuppression` metadata (the generator did not carry that overlay for these keys), with
      `AbstractDungeon.render`'s `justification` also worded differently. `--check-manifest` still
      returned ok:true, so it was not a hard failure, but a future regeneration would silently drop
      those annotations. **DONE:** `tools/nrcc/coverage_manifest.py` now carries the curated
      `known_policy`/`known_surface_id`/`known_justification`/`known_test` entries for
      `AbstractStance.render` and `DrawPilePanel.render`, and a new bounded `known_suppression` overlay
      map for the three conditional-suppression keys; the generator's `AbstractDungeon.render`
      `known_justification` was aligned to the committed wording (the committed manifest is the
      authority and was NOT edited). Regression test
      `test_coverage_manifest.CoverageManifestTest.test_regeneration_reproduces_hand_annotated_suppression_overlay`
      builds a synthetic report of the five owner ids and fails when the suppression overlay or the
      stance/drawpile maps are reverted (non-tautology proven). A full
      `scan_sts_render.py --sts-jar "$ART_STS_JAR" --write-manifest <tmp>` run (`ART_STS_JAR` from
      `.env.local`) now regenerates a manifest that is parsed-identical to the committed manifest: 0
      differing rows out of 549 (raw diff is only PyYAML key-order/wrapping formatting, since the
      committed file preserves hand ordering and the writer wraps at 80 columns); `--check-manifest`
      reports `ok:true` (549/549, no errors, no ownership errors). HONEST: the suppression overlay is a
      curated known-map, so a NEW hand annotation of this kind still needs an explicit generator entry.
- [ ] **Open (D09 D1 finding): select per-card frames + panel.** D1 review of the
      ART grid-select frame showed the confirm button is correct (native `takeAll` capsule at
      (960,475)), but per-card pixel supply is missing: `UI_SELECT_CARD`/`_SELECTED`/`_FRAME` map to
      `cardui/frame`, which is NOT file-backed, so card frames/art are not drawn (only floating
      labels). **Localized card labels are now DONE:** `SelectDrawPath.DrawItem` carries a `label`
      resolved from the projection's localized `CardView.title` (`AbstractCard.name`) with a raw-`cardId`
      fallback when the title is empty, `renderSelect` draws `item.label`, and `toMap()`/the probe
      expose `label` alongside the raw `cardId` (kept for identity/resource selection).
      `d1_full_present_select.yaml` asserts the item data `label` exists and is non-empty; the
      renderer also exposes `backend.selectDraw.items[*].label`. **REMAINING OPEN:** per-card
      card-frame pixels (`UI_SELECT_CARD*`/`_FRAME -> cardui/frame` is still NOT file-backed, so card
      frames/art are not drawn), the select panel/background, tip/header, cancel/skip buttons, and
      the eye/filter control are not supplied. Next: supply real card frames (native
      `AbstractCard.render` is the authority; either delegate card pixels or map a file-backed frame
      resource) and add the select panel/buttons as a follow-up slice.
- [x] **B05b D1 finding: native effect-list ADD race in `AbstractDungeon.update` — append now
      marshaled onto the game thread.** During B05b D1 no-regression, run 1 crashed with
      `java.util.ConcurrentModificationException` at `AbstractDungeon.update(AbstractDungeon.java:2640)`
      (~7 ms after an `art claim spawn torch`), on the native effect-list reap/update path
      (`ArrayList$Itr.remove`); run 2 was clean and the strict probe was `accepted=true` /
      `orphanArtOutput=0` / `monotonic=true`. The SAME signature is recorded in repo artifacts dated
      2026-09-28, long before B05b, and B05b's instrumented `RewardItem.render` site was never reached,
      so it is NOT attributable to B05b. HONEST: the crash itself was transient/unreproduced (run 2
      clean); the mechanism at the time was the lab `art claim spawn` structurally `add`ing to a live
      `AbstractDungeon` effect list (`effectsQueue`) from the console thread while the update/render
      thread iterates it — the ADD-side analogue of the NRO-04 removal race. The canonical fix is now
      in `VfxLabSpawn.DungeonQueue.add`: the structural `AbstractDungeon.effectsQueue.add(effect)` is
      POSTED onto the game/render thread via `Gdx.app.postRunnable(...)` (mirroring the
      `StsLabNativeNavigator.postObserved` pattern), so it can never run concurrently with
      `AbstractDungeon.update()`'s iteration/drain; the effect is still constructed inline and only the
      list append is deferred by at most one frame (acceptable for a lab spawn). A small injectable
      `PostRunner` seam (`setPostRunnerForTests`) lets tests assert the append is posted rather than
      applied inline; when no runner/app is present the append fails open to the previous guarded
      direct append (never throws) and `add` returns true once the append is SCHEDULED. Scope audit:
      `retireMatching`/`clear` were already race-safe (they only write `isDone`, no structural
      mutation) and are unchanged; a full scan of `VfxLabSpawn` confirms `DungeonQueue.add` is the ONLY
      path that structurally mutates a live native list — no other console-thread mutating path exists
      in this helper. CONFIRMED ON D1: a bounded stress of **70 rapid `art claim spawn` calls** (two
      bursts, 10+ kinds, 150-200ms spacing) during live combat produced **0 `ConcurrentModificationException`**
      and no crash; the game stayed alive/responsive, `orphanArtOutput=0` throughout, and
      `nativeRenderStrict.accepted` settled `true` after `art claim clear`. (The pre-existing D03 map
      background gap and other open items are unaffected by this slice.)
- [ ] **Open (D03 D1 finding): ART map parchment background — NOW SUPPLIED (under the node layer); axis remaining gaps.**
      D1 A/B confirmed the D03 node/outline TINT is native-matched (probe: available/taken
      `17212bff`, untaken `575757ff`, outline `8c8c80ff`, `ffffffff` count = 0). A first cut of the
      background painted it in `renderMap` AFTER the C2/`renderMap` node band, so the opaque parchment
      washed the nodes out (central-grid dark-pixel fraction native `0.0790` -> ART `0.0565`).
      **Layering fix (this slice, done):** the background is now the BOTTOM map layer — `render()`
      paints it (`renderMapBackground`) immediately BEFORE the C2 band, then `renderMap` paints the
      legend and node band on top, matching native background -> `Legend.render` -> nodes order; the
      background draw was removed from `renderMap` so it is never double-drawn over the nodes.
      `MapDrawPath.paintOrder()` / the probe `paintOrder` expose `bg:*` strictly below `legend:*`/`node:*`
      (unit-tested). **Map background (this slice, done):** ART supplies the native parchment layer.
      `MapView.MapBackground` carries the LIVE
      native geometry/alpha — `DungeonMapScreen.offsetY` (live scroll), `DungeonMap.mapMidDist`
      (soft-read, fallback computed from `Settings.MAP_DST_Y*{16|4} - 1380f*scale` with the
      `"TheEnding"` branch), `baseMapColor.a`, `Settings.scale/WIDTH/HEIGHT` — filled by
      `Sts1PresentationBackend.readMapBackground()` (soft reflection, FAIL-OPEN to `null`).
      `MapDrawPath.backgroundItems()` derives the 5 native rects in native order (non-mobile
      `DungeonMap.renderNormalMap`/`renderMapCenters`/`renderMapBlender`): top `(0, H+offsetY+mapOffsetY,
      W, 1080*scale)`, mid `(0, offsetY+mapOffsetY, W, 1080*scale)`, bot `(0, -mapMidDist+offsetY+
      mapOffsetY+1, W, 1080*scale)`, blend A `(0, offsetY+mapOffsetY+800*scale, W, BLEND_H)`, blend B
      `(0, offsetY+mapOffsetY-220*scale, W, BLEND_H)` (H=1020*scale, BLEND_H=512*scale), all white RGB
      + the live fade alpha; ids `map.bg.{top,mid,bot,blend}` map to `images/ui/map/{mapTop,mapMid,
      mapBot,mapBlend}.png` (VERIFIED present in the jar). **Final act:** `MapBackground.finalAct`
      (from `AbstractDungeon.id.equals("TheEnding")`) selects the native `renderFinalActMap` background
      — ONLY `top` then `bot` (no `mid`; `renderMapBlender` is a no-op for `"TheEnding"`), so the
      final-act output is 2 items, not 5. `probeSlice()` exposes `background`/`backgroundCount` and the
      full `paintOrder`; `d1_full_present_map_ready.yaml` asserts the 5 items + `top`/`mid`/`bot`/`blend`
      ids + alpha. Because the background paints as the frame's bottom map layer (not a
      `mapSubmissionPlan()` entry), `renderMap`'s surface evidence count is now legend + node
      submissions only (background excluded). **Documented gap:** the `baseMapColor` fade-in alpha is
      applied per background draw, but the C2 item path carries no color/alpha, so the background could
      not be expressed as a C2 item; the settled alpha is 1.0, so D1 parity is unaffected.
      **Localized legend labels (D03 follow-up, done):** the legend title and 6 room labels are no
      longer hardcoded English in the pure draw path — `MapView` carries an optional `LegendLabels`
      (title + 6 labels) filled by `Sts1PresentationBackend.mapFrame()` via soft reflection on
      `CardCrawlGame.languagePack.getUIString("Legend").TEXT` (`TEXT[0/3/6/9/12/15]` + `TEXT[18]`,
      VERIFIED native `Legend.java`), fail-open to the existing English defaults; the resolved text is
      exposed in the probe (`legendTitle`/`legendLabels`) and the `LegendDrawItem` / submission-plan
      labels. **Map edges (this slice,
      done):** ART now draws the native map connection dots that were entirely missing. The backend
      `readMapEdges()` soft-reflects every `MapRoomNode.edges` -> `MapEdge.color` (public r/g/b/a) +
      the PRIVATE `ArrayList<MapDot> dots` and each `MapDot`'s PRIVATE `x`/`y`/`rotation`, DEDUPED by
      REFERENCE identity of the dot-list object (an `IdentityHashMap`-backed set, no hash collision)
      so a shared edge paints ONCE; fail-open to an empty list. The `MapDot`
      jitter is baked into the stored coordinates at edge construction (`MathUtils.random`), so ART
      reads those STORED values rather than recomputing. `MapView` carries an optional `edges` list
      (`MapEdgeView`/`MapDotView`, no STS types; source-compatible ctors + `toMap()`), and
      `MapDrawPath.edgeItems()` resolves each dot to `map.edge.dot` (`images/ui/map/dot1.png`, VERIFIED
      in the jar) at `x = dot.x-8*scale`, `y = dot.y-8*scale+offsetY+172*scale`, size `16*scale`, tint =
      edge color, rotation = dot rotation (from the LIVE `MapBackground.offsetY`/`scale`);
      `probeSlice()` exposes `edges`/`edgeCount`/`edgesPresent`; `paintOrder()` adds an `edge:` band
      strictly below `legend:`/`node:` and above `bg:`. `Sts1SurfaceRenderer.renderMap` draws the edge
      dots BELOW the node band via a new rotation-aware `drawResolvedTexture` overload mirroring native
      `MapDot.render`. **Approximation (honest):** ART draws ALL edges as one global `edge:` band under
      the legend/nodes, whereas native interleaves each node's edges at the START of that node's own
      `MapRoomNode.render`; the dot geometry/tint is identical, so only the relative edge-vs-edge draw
      order ACROSS nodes is approximated. **Remaining D03 gaps (honest, not full parity):** legend
      hover/tip/alpha-fade,
      boss icon, node hover FX, mobile scaling, ring `(nodeScale*0.95+0.2)*Settings.scale` factor,
      native oscillating alpha, `DungeonMapScreen.oscillatingColor` on the tip, a D1 map state with a
      taken/current node to visually confirm the `MAP_CIRCLE_5` ring, and native pan/zoom parity all
      remain open. Next: add that D1 taken/current-node state and close the remaining gaps above.
- [x] **Hardened (D09 follow-up D1 finding): `art lab enter-select` intermittent NPE.** D1 runs of
      the select scenario: 3/4 passed; run 2 failed with a `NullPointerException` at the
      `art lab enter-select grid` step (`command_log.status=ERROR, message=NullPointerException`),
      i.e. BEFORE any selectDraw/label output — a transient lab-navigation flake, not a label
      regression (runs 3/4 passed back-to-back in the identical prior state). Defensive hardening:
      `StsLabNativeNavigator.enterSelect` now (a) refuses to re-open a select screen that is already
      the current screen (`AbstractDungeon.screen` GRID/HAND_SELECT) and returns
      `stopHandled("already selecting")` instead of scheduling a nested `open()`; (b) copies the
      master deck card-by-card, recording any null/uncopyable entry instead of failing the whole
      command; (c) wraps every posted app-thread Runnable so a failure is recorded on a lab status
      channel (`LabNavigationSignals.lastError`/`errorCount`, surfaced by `art lab status` /
      `art lab dump` as `lastNavError`/`navErrorCount`) rather than surfacing as an uncaught app
      crash; and (d) includes the real cause class + message in the outer rejection. The NPE was
      transient and unreproduced, so this is defensive hardening, NOT a confirmed root-cause fix;
      no exact trigger was pinned.
- [ ] **Open (D09 follow-up D1 finding): select text localization.** The localized card
      labels (D09 follow-up) are confirmed (`Strike_R`->`打击`, `Defend_R`->`防御`, `Bash`->`痛击`,
      `AscendersBane`->`进阶之灾`). **Confirm button label is now LOCALIZED:** the backend reads the
      LIVE native confirm-button text — `GridCardSelectScreen.confirmButton.buttonText`
      (`GridSelectConfirmButton`, constructed `new GridSelectConfirmButton(TEXT[0])` from the screen's
      own UIStrings) / `HandCardSelectScreen.button.buttonText` (`CardSelectConfirmButton`, defaults
      to the `"Confirm Button"` UIStrings `TEXT[0]`) — via the soft-reflection helper, falls back to
      the confirm-label UIStrings `TEXT[0]`, and fails open to `""`. `SelectView.confirmLabel`
      carries it (empty default; new overloads keep the 4-arg `grid`/`hand` factories
      source-compatible); `SelectDrawPath` resolves the confirm `DrawItem.label` to `confirmLabel`
      when non-blank else the neutral `"Confirm"` fallback, `renderSelect` draws `item.label` for the
      confirm row (the `item.confirm ? "Confirm" :` special-case is dropped), and `probeSlice()`
      exposes `confirm.label`. `d1_full_present_select.yaml` asserts `confirm.label` exists,
      is non-empty, and is `neq "Confirm"` (the English fallback literal) on the Chinese D1 device.
      **REMAINING OPEN:** the English `Generic` string at top-center is still unlocalized (its source
      is not yet pinned — likely a native English-data asset or a leaked placeholder; needs a
      separate read), and per-card card-frame pixels remain the separately-logged `cardui/frame`
      non-file-backed gap (per-card frames/art, select panel/background, tip/header, cancel/skip
      buttons, and eye/filter control also remain unsupplied).
- [x] **Resolved (D03 map-node wash): root cause was the node fill/outline submission order.**
      D03 supplies the native parchment background (`mapTop`/`mapMid`/`mapBot`/`mapBlend` at the
      native rects, drawn UNDER the node/legend band; probe `paintOrder` = bg → legend → node;
      parchment matches native away from nodes) and that stays correct. The D1 A/B pale nodes
      (~0.53 grey) were a SUBMISSION-ORDER defect, not a tint/texture gap: native `MapRoomNode.render`
      draws the OUTLINE (`this.room.getMapImgOutline()`, decompiled ~:370/372) BEFORE the node FILL
      (`this.room.getMapImg()`, ~:383/385), both at the SAME node box, so the fill covers the
      outline's center and only the outline edges remain. `MapDrawPath.mapSubmissionPlan()` submitted
      the FILL first and the OUTLINE second at the same `item.bounds`; because the projection defaults
      `reachable=true` the outline is submitted for EVERY node, so the grey `OUTLINE_COLOR`
      (`0.549,0.549,0.502`) painted OVER each node fill (native tint `0.34`/`0.09`) and every node
      read as a pale ~0.53 outline silhouette (ART ~0.53 vs native ~0.35). Fix (bounded, colors and
      geometry UNCHANGED): per node the plan now submits outline FIRST, then fill, then overlay, then
      ring — the native order. `MapDrawPathTest.submissionPlanPaintsReachableNodeOutlineBeforeFillAtSameRect`
      pins the per-node order and that the outline/fill rects are equal. (Remaining D03 gaps are
      unrelated: legend hover/fade, map edges, boss icon, node hover FX, mobile scaling, ring scale
      factor, a taken/current ring D1 state.)

package artframework.sts1.render;

/**
 * Pure draw geometry for one claimed per-instance transient effect (family-neutral seam; current
 * members are the {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code CeilingDustCloudEffect}, the {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}; the four
 * later members are the {@code vfx-scene-world} {@code TorchParticleXLEffect} and the
 * {@code vfx-misc-root} {@code GhostlyWeakFireEffect}/{@code GenericSmokeEffect}/
 * {@code ExhaustBlurEffect}, and the two newest are the {@code vfx-combat} bare-{@code Texture}
 * {@code IceShatterEffect}/{@code WebParticleEffect}; the four newest members are the
 * {@code vfx-combat} {@code EntangleEffect} (byte-identical to {@code WebParticleEffect}) and
 * {@code BlockImpactLineEffect}/{@code UnknownParticleEffect} plus the {@code vfx-misc-root}
 * {@code ExhaustPileParticle}),
 * mirroring the native render formula exactly; the five newest members are the {@code vfx-combat}
 * {@code FlameParticleEffect}/{@code LightningOrbActivateEffect}/{@code DamageImpactBlurEffect}/
 * {@code DamageImpactLineEffect}/{@code DarkOrbPassiveEffect}, and the three newest are the
 * {@code vfx-misc-root} {@code WarningSignEffect}, the {@code vfx-combat} {@code StunStarEffect}, and
 * the {@code vfx-misc-root} {@code FallingDustEffect} (in that order), the three newest are the
 * {@code vfx-combat} {@code LightningEffect}, the {@code vfx-misc-root} {@code FlameBallParticleEffect},
 * and the {@code vfx-misc-root} {@code ShineLinesEffect} (in that order), and the three newest are
 * the {@code vfx-scene-world} {@code TorchParticleMEffect}/{@code TorchParticleSEffect} (additive
 * center-packed, no new rule) and {@code DustEffect} (ambient center-packed reusing the
 * {@code FALLING_DUST} region-offset origin), and the two newest are the {@code vfx-combat}
 * {@code LightningOrbPassiveEffect} and the {@code vfx-misc-root} {@code GlowyFireEyesEffect},
 * which extend the bare-{@code Texture} shape-C path with the first per-instance FLIP flags
 * (see {@link #usesInstanceFlipX}/{@link #usesInstanceFlipY}), and the two newest are the
 * {@code vfx-combat} {@code FlyingSpikeEffect} (additive center-packed, no new rule) and the
 * {@code vfx-misc-root} {@code ConeEffect} (ambient center-packed with a NEW origin rule —
 * {@code originX = 0f} rather than {@code packedWidth/2f} — and a {@code scale * 1.1f} uniform
 * scale); the two newest are the {@code vfx-combat} {@code FallingIceEffect} (additive shape-C
 * fixed rect, origin 48, size 96&times;96, src {@code 0,0,96,96}, x/y passthrough, consuming its
 * {@code rotation} field) and the {@code vfx-misc-root} {@code DamageHeartEffect} (ambient
 * center-packed, exactly {@code StanceAuraEffect}, with a public {@code AtlasRegion img}) — the
 * first two kinds whose native {@code render} guards its draw on a wait-phase field, modeled by
 * the new {@link #nativeSkipsDrawByGuard}/{@link #guardFieldName} capability; the two newest are
 * the {@code vfx-scene-world} {@code SpookyChestEffect} and {@code IroncladVictoryFlameEffect}
 * (both NO-ARG constructors), ambient center-packed {@code AtlasRegion} members that additionally
 * introduce the img-path per-instance MIRROR capability (the native render flips the shared region in
 * place around its draw) — modeled purely by the new
 * {@link #usesInstanceMirrorX}/{@link #usesInstanceMirrorY} predicates, with
 * {@code SpookyChestEffect} declaring both {@code flipX} and {@code flipY} and
 * {@code IroncladVictoryFlameEffect} only {@code flipX}; {@code FlameParticleEffect} also uses the
 * mirror, resolving the F15 {@code flipX} limitation. The three newest members reuse the ambient
 * center-packed img branch with NO new formula: {@code SpookierChestEffect} ({@code vfx-scene-world})
 * and {@code CampfireSleepScreenCoverEffect} ({@code vfx-campfire-rest}; the first claimed member of
 * that family, a per-instance ambient center-packed sprite with a NO-ARG constructor) each carry
 * {@code flipX}+{@code flipY} and reuse the F22 mirror, while {@code DeathScreenFloatyEffect}
 * ({@code vfx-misc-root}) carries no flip flags and draws the canonical region. The newest member is
 * the {@code vfx-stance-aura} {@code WrathStanceChangeParticle} ({@link Kind#WRATH_STANCE_CHANGE}),
 * the seam's FIRST non-deterministic native effect: additive center-packed geometry, but its native
 * {@code render} draws at the PLAYER HITBOX CENTER X plus the effect's {@code x}
 * ({@link #playerHitboxRelativeX}), consumes two ordered {@code MathUtils.random(...)} values for
 * scaleX/scaleY ({@link #randomRanges}), and guards its draw with {@code if (delayTimer > 0f) return}
 * ({@link #nativeSkipsDrawByGuard}/{@link #guardFieldName}/{@link #guardBlocks}).
 *
 * <p>This class is host-neutral data: it performs no GL work, holds no host handles, and applies no
 * color/blend/UV state. The per-kind blend policy is pure and lives in {@link #additiveBlend}: most
 * claimable effects draw additively — the host draw calls {@code setColor(color)} and
 * {@code setBlendFunction(770, 1)} around {@code SpriteBatch.draw(...)} and restores
 * {@code setBlendFunction(770, 771)} afterwards — but the ambient kinds never call
 * {@code setBlendFunction} at all, so they must draw under the ambient blend and restore only the
 * previous color ({@link #additiveBlend} returns {@code false} for them). The ambient kinds are
 * {@code FlashAtkImgEffect} (the first), plus the {@code SmokeBlurEffect},
 * {@code CeilingDustCloudEffect}, {@code NemesisFireParticle}, {@code DebuffParticleEffect},
 * {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect},
 * {@code ExhaustPileParticle}, {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect},
 * {@code DamageImpactLineEffect}, {@code StunStarEffect}, {@code FallingDustEffect},
 * {@code ShineLinesEffect}, {@code DustEffect}, {@code ConeEffect},
 * {@code DamageHeartEffect}, {@code SpookyChestEffect}, and
 * {@code IroncladVictoryFlameEffect}
 * members, plus the three newest ambient center-packed members {@code SpookierChestEffect},
 * {@code CampfireSleepScreenCoverEffect}, and {@code DeathScreenFloatyEffect}, plus the newest (F27)
 * ambient center-packed member {@code WaterSplashParticleEffect} and the newest (F28) ambient
 * center-packed member {@code BottomFogEffect}. The native
 * {@code LightFlareSEffect} orders blend-before-color, but only the restored end state is shared
 * with the aura classes. The host draw owns that color/blend/UV (and the region's UV rect); this
 * mapping only resolves the positional/scale/rotation arguments the batch receives, with the native
 * center origin {@code (packedWidth / 2, packedHeight / 2)} and the identity width/height
 * {@code (packedWidth, packedHeight)}.
 *
 * <p>The per-kind formulas mirrored here are:
 *
 * <pre>
 *   StanceAuraEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DivinityStanceChangeParticle.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareSEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareMEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightFlareLEffect.render:
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   TorchParticleLEffect.render (note: has vY, but render ignores it; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FlashAtkImgEffect.render (note: no setBlendFunction; ambient blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FireBurstParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   RedFireBurstParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   SmokeBlurEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CeilingDustCloudEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   NemesisFireParticle.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   WrathParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph,
 *             scale*0.8f, (0.1f + ((dur_div2*2f - duration)*2f*scale)) * Settings.scale, rotation)
 *   DivinityParticleEffect.render:
 *     sb.draw(img, x, y + vY, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CalmParticleEffect.render:
 *     sb.draw(ImageMaster.FROST_ACTIVATE_VFX_1, x, y, 32f, 32f, 25f, 128f,
 *             scale, scale + (dur_div2*0.4f - duration) * Settings.scale, rotation,
 *             0, 0, 64, 64, false, false)
 *   ShieldParticleEffect.render (note: additive blend; rotation hardcoded to 0f):
 *     sb.draw(ImageMaster.INTENT_DEFEND, x - 32f, y - 32f, 32f, 32f, 64f, 64f,
 *             scale, scale, 0f, 0, 0, 64, 64, false, false)
 *   DebuffParticleEffect.render (note: no setBlendFunction; ambient blend; uses the rotation field):
 *     sb.draw(img, x - 16f, y - 16f, 16f, 16f, 32f, 32f,
 *             scale, scale, rotation, 0, 0, 32, 32, false, false)
 *   TorchParticleXLEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   GhostlyWeakFireEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   GenericSmokeEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   ExhaustBlurEffect.render (note: no setBlendFunction; ambient blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   IceShatterEffect.render (note: additive blend; uses the rotation field; vY is update-only):
 *     sb.draw(img, x, y, 32f, 32f, 64f, 64f, scale, scale, rotation,
 *             0, 0, 64, 64, false, false)
 *   WebParticleEffect.render (note: additive blend; rotation hardcoded to 0f):
 *     sb.draw(ImageMaster.WEB_VFX, x, y, 32f, 32f, 64f, 64f, scale, scale, 0f,
 *             0, 0, 64, 64, false, false)
 *   EntangleEffect.render (note: additive blend; rotation hardcoded to 0f; byte-identical to Web;
 *                          no rotation field, no img field):
 *     sb.draw(ImageMaster.WEB_VFX, x, y, 32f, 32f, 64f, 64f, scale, scale, 0f,
 *             0, 0, 64, 64, false, false)
 *   BlockImpactLineEffect.render (note: no setBlendFunction; ambient blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   ExhaustPileParticle.render (note: no setBlendFunction; ambient blend; img is private static
 *                               and declared on the class):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   UnknownParticleEffect.render (note: no setBlendFunction; ambient blend; uses the rotation
 *                                 field and its own instance Texture img):
 *     sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, rotation,
 *             0, 0, 128, 128, false, false)
 *   FlameParticleEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   LightningOrbActivateEffect.render (note: additive blend):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DamageImpactBlurEffect.render (note: no setBlendFunction; ambient blend; no isDone guard):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DamageImpactLineEffect.render (note: no setBlendFunction; ambient blend; if (!isDone) guard):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DarkOrbPassiveEffect.render (note: additive blend; uses the rotation field and its own instance
 *                                 Texture img; src 0,0,74,74 is the full region):
 *     sb.draw(img, x - 37f, y - 37f, 37f, 37f, 74f, 74f, scale, scale, rotation,
 *             0, 0, 74, 74, false, false)
 *   WarningSignEffect.render (note: additive blend; rotation hardcoded to 0f; the uniform scale is
 *                              the hardcoded Settings.scale * 2f, NOT an effect scale field, which
 *                              the class does not have):
 *     sb.draw(ImageMaster.WARNING_ICON_VFX, x - 32f, y - 32f, 32f, 32f, 64f, 64f,
 *             Settings.scale*2f, Settings.scale*2f, 0f, 0, 0, 64, 64, false, false)
 *   StunStarEffect.render (note: no setBlendFunction; ambient blend; the draw POSITION is offset by
 *                          the effect's own vX/vY, both scaled by Settings.scale):
 *     sb.draw(img, x - vX*30f*Settings.scale, y - vY*5f*Settings.scale,
 *             pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   FallingDustEffect.render (note: no setBlendFunction; ambient blend; the ORIGIN is the region's
 *                             own offsetX/offsetY, NOT packed/2):
 *     sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph, scale, scale, rotation)
 *   LightningEffect.render (note: additive blend; the origin Y is 0f, NOT ph/2):
 *     sb.draw(img, x, y, pw/2f, 0f, pw, ph, scale, scale, rotation)
 *   FlameBallParticleEffect.render (note: additive blend; the origin Y is ph/2f + 20f *
 *                                   Settings.scale; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f + 20f * Settings.scale, pw, ph, scale, scale, rotation)
 *   ShineLinesEffect.render (note: no setBlendFunction; ambient blend; if (!isDone) guard; the
 *                             geometry is exactly StanceAuraEffect center-packed):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   TorchParticleMEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   TorchParticleSEffect.render (note: additive blend; vY is update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DustEffect.render (note: no setBlendFunction; ambient blend; the ORIGIN is the region's own
 *                      offsetX/offsetY, NOT packed/2 — the same rule as FallingDustEffect):
 *     sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph, scale, scale, rotation)
 *   LightningOrbPassiveEffect.render (note: additive blend; uses the rotation field and its own
 *                                      instance Texture img; the flipX/flipY booleans come from the
 *                                      effect's own fields — the first per-instance flip kind):
 *     sb.draw(img, x - 61f, y - 61f, 61f, 61f, 122f, 122f, scale, scale, rotation,
 *             0, 0, 122, 122, flipX, flipY)
 *   GlowyFireEyesEffect.render (note: additive blend; rotation hardcoded to 0f; the class has no
 *                                rotation field; only the horizontal flip is per-instance and the
 *                                vertical flip is always false):
 *     sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, 0f,
 *             0, 0, 128, 128, flippedX, false)
 *   TorchHeadFireEffect.render (note: additive blend; rotation hardcoded to 0f; the class has no
 *                                rotation field; only the horizontal flip is per-instance and the
 *                                vertical flip is always false; the X draw scale is scale * 1.2f
 *                                while the Y draw scale stays scale):
 *     sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale * 1.2f, scale, 0f,
 *             0, 0, 128, 128, flippedX, false)
 *   FlyingSpikeEffect.render (note: additive blend; vX/vY are update-only):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   ConeEffect.render (note: no setBlendFunction; ambient blend; the origin X is 0f, NOT pw/2f, and
 *                      the uniform scale is scale * 1.1f):
 *     sb.draw(img, x, y, 0f, ph/2f, pw, ph, scale*1.1f, scale*1.1f, rotation)
 *   FallingIceEffect.render (note: guarded by if (waitTimer < 0f); additive blend; uses the rotation
 *                             field and its own instance Texture img):
 *     sb.draw(img, x, y, 48f, 48f, 96f, 96f, scale, scale, rotation, 0, 0, 96, 96, false, false)
 *   DamageHeartEffect.render (note: guarded by if (delayTimer < 0f); no setBlendFunction; ambient
 *                              blend; the geometry is exactly StanceAuraEffect center-packed):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   SpookyChestEffect.render (note: no setBlendFunction; ambient blend; the shared region is
 *                             flipped in place to the instance's orientation and left flipped:
 *                             if (flipX != img.isFlipX()) img.flip(true, false);
 *                             if (flipY != img.isFlipY()) img.flip(false, true); the geometry is
 *                             exactly StanceAuraEffect center-packed; the mirror is resolved by the
 *                             host draw as a UV swap — see usesInstanceMirrorX/usesInstanceMirrorY):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   IroncladVictoryFlameEffect.render (note: no setBlendFunction; ambient blend; the shared region
 *                             is flipped in place to the instance's orientation on X and left
 *                             flipped (the class has no flipY field); the geometry is exactly
 *                             StanceAuraEffect center-packed; the mirror is resolved by the host draw
 *                             as a UV swap):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   SpookierChestEffect.render (note: no setBlendFunction; ambient blend; identical shape to
 *                             SpookyChestEffect — the shared region is flipped in place to the
 *                             instance's own flipX/flipY and left flipped; the geometry is exactly
 *                             StanceAuraEffect center-packed; the mirror is resolved by the host draw
 *                             as a UV swap):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   CampfireSleepScreenCoverEffect.render (note: no setBlendFunction; ambient blend; the first
 *                             claimed vfx-campfire-rest member, a per-instance ambient
 *                             center-packed sprite with a NO-ARG constructor: same shape as
 *                             SpookyChestEffect with per-instance flipX/flipY mirror):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   DeathScreenFloatyEffect.render (note: no setBlendFunction; ambient blend; no flip flags; the
 *                             geometry is exactly StanceAuraEffect center-packed):
 *     sb.draw(img, x, y, pw/2f, ph/2f, pw, ph, scale, scale, rotation)
 *   WrathStanceChangeParticle.render (note: FIRST non-deterministic kind; guarded by
 *                             if (delayTimer > 0f) return; additive blend; draws at the PLAYER
 *                             HITBOX CENTER X + x, NOT the effect's own x; consumes one
 *                             MathUtils.random(2.9f, 3.1f) for scaleX and one
 *                             MathUtils.random(0.95f, 1.05f) for scaleY IN THAT ORDER; no vY):
 *     sb.draw(img, AbstractDungeon.player.hb.cX + x, y, pw/2f, ph/2f, pw, ph,
 *             scale*MathUtils.random(2.9f,3.1f), scale*MathUtils.random(0.95f,1.05f), rotation)
 *   StanceChangeAbsorptionParticle.render (note: the seam's FIRST MULTI-DRAW kind; additive blend;
 *                             draws the static ImageMaster.WOBBLY_ORB_VFX Texture TWICE with the SAME
 *                             fixed shape-C rect — offset 16, origin 16, size 32, src 0,0,32,32,
 *                             rotation offset -200f; pass 0 draws scaleX/scaleY with scale *
 *                             MathUtils.random(0.5f,2.0f), pass 1 with scale * MathUtils.random(
 *                             0.6f,2.5f) — four RNG calls in order; see drawPassRandomRanges; no vY):
 *     sb.draw(ImageMaster.WOBBLY_ORB_VFX, x - 16f, y - 16f, 16f, 16f, 32f, 32f,
 *             scale*MathUtils.random(0.5f,2.0f), scale*MathUtils.random(0.5f,2.0f), rotation - 200f,
 *             0, 0, 32, 32, false, false)
 *     sb.draw(ImageMaster.WOBBLY_ORB_VFX, x - 16f, y - 16f, 16f, 16f, 32f, 32f,
 *             scale*MathUtils.random(0.6f,2.5f), scale*MathUtils.random(0.6f,2.5f), rotation - 200f,
 *             0, 0, 32, 32, false, false)
 *   CardTrailEffect.render (note: ADDITIVE; fixed ORIGIN 6,6 and fixed SIZE 12x12 INDEPENDENT of the
 *                           region's packed size; rotation hardcoded 0f — the class inherits
 *                           AbstractGameEffect.rotation but the draw ignores it; the img is a private
 *                           STATIC AtlasRegion):
 *     sb.draw(img, x, y, 6f, 6f, 12f, 12f, scale, scale, 0f)
 *   HealPanelEffect.render (note: ADDITIVE; the img is a STATIC Texture (no instance img field); a
 *                           fixed shape-C rect — src 0,0,64,64, origin 32,32, size 64,64 — with a
 *                           PANEL-SPACE position: x = x - 32f + 32f * Settings.scale and
 *                           y = Settings.HEIGHT - 32f * Settings.scale - 32f; consumes the field
 *                           rotation and the effect's own color):
 *     sb.draw(img, x - 32f + 32f * Settings.scale,
 *             Settings.HEIGHT - 32f * Settings.scale - 32f,
 *             32f, 32f, 64f, 64f, scale, scale, rotation, 0, 0, 64, 64, false, false)
 *   PingHpEffect.render (note: ADDITIVE; the img is the STATIC ImageMaster.TP_HP Texture (no instance
 *                           img field); the HealPanel analogue with the SAME fixed shape-C rect — src
 *                           0,0,64,64, origin 32,32, size 64,64 — and the SAME PANEL-SPACE position, but
 *                           its ONE difference is the uniform draw scale: BOTH axes are
 *                           scale * Settings.scale (HealPanel used plain scale); consumes the field
 *                           rotation and the effect's own color):
 *     sb.draw(ImageMaster.TP_HP, x - 32f + 32f * Settings.scale,
 *             Settings.HEIGHT - 32f * Settings.scale - 32f,
 *             32f, 32f, 64f, 64f, scale * Settings.scale, scale * Settings.scale, rotation,
 *             0, 0, 64, 64, false, false)
 * </pre>
 *
 * where {@code pw}/{@code ph} are the region's {@code packedWidth}/{@code packedHeight}. The
 * bare-{@code Texture} kinds — Calm, Shield, Debuff, IceShatter, Web, Entangle, Unknown, WarningSign,
 * DarkOrb, LightningOrbPassive, GlowyFireEyes, TorchHeadFire, and
 * FallingIce — draw a
 * fixed source rect
 * rather than a packed region, so their native origin/size/source rect are host-neutral constants
 * and the packed region size is ignored; Shield, Web, Entangle, WarningSign, GlowyFireEyes, and
 * TorchHeadFire
 * hardcode rotation
 * {@code 0f},
 * Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive, and FallingIce consume their
 * {@code rotation}
 * field, and Calm keeps its
 * {@code scaleY} formula. WarningSign is the only kind whose uniform scale is a hardcoded
 * {@code settingsScale * 2f} rather than the effect's own {@code scale} field (it has none). Two of
 * the bare-{@code Texture} kinds are the first to carry per-instance FLIP booleans: the native
 * {@code LightningOrbPassiveEffect} passes its own {@code flipX} and {@code flipY} fields, and
 * {@code GlowyFireEyesEffect} passes its own {@code flippedX} with a hardcoded {@code false} vertical
 * flip, so {@link #usesInstanceFlipX}/{@link #usesInstanceFlipY} report them; the newest (F29)
 * {@code TorchHeadFireEffect} likewise passes its own {@code flippedX} (see
 * {@link #usesTexturedFlipX}); every other kind keeps
 * {@code false, false}. Web and
 * Entangle
 * are the only kinds whose native {@code render} rewrites the set color, forcing RGB to white
 * and taking alpha from the effect's color (see {@link #whiteAlphaOnly}). {@code DivinityStanceChangeParticle}, the
 * cross-family {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code TorchParticleLEffect}, the {@code vfx-misc-root} {@code FireBurstParticleEffect}/
 * {@code NemesisFireParticle}, and the {@code vfx-combat} {@code FlashAtkImgEffect}/
 * {@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}, plus the {@code vfx-scene-world}
 * {@code CeilingDustCloudEffect}, share the {@code StanceAuraEffect} geometry (x/y passthrough, no
 * consumed {@code vY}) — the three later scene-world members, the two fire bursts, the smoke blur,
 * the ceiling dust, the nemesis fire, and the four newest members ({@code TorchParticleXLEffect},
 * {@code GhostlyWeakFireEffect}, {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}) are
 * geometry-identical to the additive center-packed branch
 * and the ones that own a {@code vY} field ignore it in {@code render} — so they all map to the
 * same {@link Kind#STANCE_AURA} formula branch. Only blend distinguishes them:
 * {@code FlashAtkImgEffect}/{@code SmokeBlurEffect}/{@code CeilingDustCloudEffect}/
 * {@code NemesisFireParticle}/{@code DebuffParticleEffect}/{@code GenericSmokeEffect}/
 * {@code ExhaustBlurEffect}/{@code DamageImpactBlurEffect}/{@code DamageImpactLineEffect} never
 * switch blend function, so
 * {@link #additiveBlend} reports
 * {@code false} for them, while the two fire bursts, {@code TorchParticleXLEffect},
 * {@code GhostlyWeakFireEffect}, {@code FlameParticleEffect}, {@code LightningOrbActivateEffect},
 * and {@code ShieldParticleEffect} are additive
 * like the rest. {@code ShieldParticleEffect}/{@code DebuffParticleEffect} are the first two
 * members beyond {@code CalmParticleEffect} to draw a bare {@code Texture}, so they join the
 * fixed-source-rect shape via their own geometry branches rather than the packed-region branches;
 * the two newest members {@code IceShatterEffect}/{@code WebParticleEffect} join that same
 * bare-{@code Texture} shape, with {@code IceShatterEffect} consuming its {@code rotation} field
 * ({@link Kind#ICE_SHATTER}) and {@code WebParticleEffect} hardcoding rotation {@code 0f} and
 * forcing its set color to {@code (1, 1, 1, color.a)} ({@link Kind#WEB_PARTICLE}).
 * The four newest members reuse the three existing shapes with a single new fixed rect:
 * {@code EntangleEffect} ({@link Kind#ENTANGLE}) is byte-identical to {@code WebParticleEffect} —
 * same static {@code ImageMaster.WEB_VFX} texture, offset 0, origin {@code (32, 32)}, size
 * {@code (64, 64)}, src {@code (0, 0, 64, 64)}, hardcoded rotation {@code 0f}, additive blend, and
 * the white-alpha set-color rule (so {@link #whiteAlphaOnly} is true for it too) — {@code
 * BlockImpactLineEffect} ({@link Kind#BLOCK_IMPACT_LINE}) and {@code ExhaustPileParticle}
 * ({@link Kind#EXHAUST_PILE}) reuse the ambient center-packed {@link Kind#STANCE_AURA} geometry
 * ({@code ExhaustPileParticle.img} is a {@code private static} {@code AtlasRegion} declared on the
 * class), and {@code UnknownParticleEffect} ({@link Kind#UNKNOWN_PARTICLE}) is a NEW ambient
 * fixed-rect formula — offset {@code (-64, -64)}, origin {@code (64, 64)}, size {@code (128, 128)},
 * src {@code (0, 0, 128, 128)}, consuming its {@code rotation} field — over its own instance
 * {@code Texture img}. It is the first kind whose fixed rect is not {@code (64, 64)} or
 * {@code (32, 32)}, and it draws under the ambient blend (no {@code setBlendFunction}).
 * The five newest members are all {@code vfx-combat} and again reuse the two existing shapes:
 * {@code FlameParticleEffect} ({@link Kind#FLAME_PARTICLE}) and
 * {@code LightningOrbActivateEffect} ({@link Kind#LIGHTNING_ORB_ACTIVATE}) reuse the additive
 * center-packed geometry, {@code DamageImpactBlurEffect} ({@link Kind#DAMAGE_IMPACT_BLUR}) and
 * {@code DamageImpactLineEffect} ({@link Kind#DAMAGE_IMPACT_LINE}) reuse that same geometry under
 * the ambient blend (neither calls {@code setBlendFunction}; only {@code DamageImpactLineEffect}
 * guards its draw with {@code if (!isDone)}), and {@code DarkOrbPassiveEffect}
 * ({@link Kind#DARK_ORB_PASSIVE}) is a NEW
 * additive shape-C fixed-rect formula — offset {@code (-37, -37)}, origin {@code (37, 37)}, size
 * {@code (74, 74)}, src {@code (0, 0, 74, 74)}, the full 74&times;74 region, consuming its
 * {@code rotation} field — over its own instance {@code Texture img}.
 * The two newest members are the first kinds whose native {@code render} guards its draw on a
 * wait-phase field: {@code FallingIceEffect} ({@link Kind#FALLING_ICE}) is a NEW additive
 * shape-C fixed rect — origin {@code (48, 48)}, size {@code (96, 96)},
 * src {@code (0, 0, 96, 96)}, with x/y passed through unchanged (no position offset) — over its
 * own instance {@code Texture img} and consuming its
 * {@code rotation} field, guarded by {@code if (waitTimer < 0f)}; {@code DamageHeartEffect}
 * ({@link Kind#DAMAGE_HEART}) reuses the ambient center-packed {@link Kind#STANCE_AURA} geometry
 * verbatim (its {@code img} is a public {@code AtlasRegion}) and is guarded by
 * {@code if (delayTimer < 0f)}. The per-kind guard is modeled purely by
 * {@link #nativeSkipsDrawByGuard} (which kinds have a guard) and {@link #guardFieldName} (the
 * host-neutral field name); the renderer declines a draw when the guard field value blocks per that
 * kind's condition, which lives in the pure {@link #guardBlocks} predicate (the source of truth — it
 * differs per kind: {@code > 0f} for {@code WRATH_STANCE_CHANGE}, {@code !(value < 0f)} for
 * {@code FALLING_ICE}/{@code DAMAGE_HEART}), matching the native wait phase pixel-for-pixel.
 *
 * <p>The newest member is the {@code vfx-stance-aura} {@code StanceChangeAbsorptionParticle}
 * ({@link Kind#STANCE_CHANGE_ABSORPTION}) — the seam's SECOND non-deterministic kind and its FIRST
 * MULTI-DRAW kind: additive, it draws the static {@code ImageMaster.WOBBLY_ORB_VFX} {@code Texture}
 * TWICE with the SAME shape-C fixed rect (offset {@code 16f}, origin {@code 16f}, size
 * {@code 32f&times;32f}, src {@code 0,0,32,32}, rotation offset {@code -200f}), consuming FOUR
 * {@code MathUtils.random(...)} values in order — two per pass ({@code [0.5f, 2.0f]} held additively
 * in pass 0 and {@code [0.6f, 2.5f]} in pass 1, applied to the pass's scaleX then scaleY). The
 * capability is the pure {@link #drawPassRandomRanges} (an ordered list of per-pass range lists; an
 * empty outer list for every other kind), and {@link #params} returns only the shared base
 * {@code (scale, scale)} because the per-pass scales are applied by the renderer.
 *
 * <p>The two newest (F27) members are the {@code vfx-combat} {@code WaterSplashParticleEffect}
 * ({@link Kind#WATER_SPLASH}) and {@code BuffParticleEffect} ({@link Kind#BUFF_PARTICLE}). Neither
 * adds a new draw branch: {@code WaterSplashParticleEffect} is an AMBIENT center-packed
 * {@code AtlasRegion} member that consumes its {@code rotation} field and introduces ONE new pure
 * rule — its native draw scale is ANISOTROPIC ({@code scaleY = scale * 0.54f}, {@code scaleX =
 * scale}) — so {@link #params} gained a trailing {@code scaleYMultiplier} scalar (defaulting
 * {@code 1f} for every pre-existing kind so their results are unchanged, {@link
 * #WATER_SPLASH_SCALE_Y_MULTIPLIER} for {@code WATER_SPLASH}); {@code BuffParticleEffect} is an
 * ADDITIVE member with a new pure POSITION/ORIGIN rule — its draw position is
 * {@code (x - packedWidth/2f, y - packedHeight/2f)} and its origin is the region's OWN
 * {@code (offsetX, offsetY)} rather than the shared {@code packed/2} center — resolved by its own
 * {@link #params} branch. Both consume their inherited {@code rotation} field and are appended LAST
 * in that order. No new patch/bridge/console wiring; the default-off gate and per-instance token
 * semantics are unchanged.
 *
 * <p>The two newest (F28) members reuse the existing center-packed img branch. {@code BottomFogEffect}
 * ({@link Kind#BOTTOM_FOG}, {@code vfx-scene-world}) is an AMBIENT center-packed {@code AtlasRegion}
 * member with NO new rule that reuses the F22 per-instance mirror — identical in shape to
 * {@code SpookierChestEffect}/{@code CampfireSleepScreenCoverEffect}, carrying its own
 * {@code flipX}+{@code flipY} (so {@link #usesInstanceMirrorX} AND {@link #usesInstanceMirrorY} now
 * include it). {@code GiantFireEffect} ({@link Kind#GIANT_FIRE}, {@code vfx-combat}) is an ADDITIVE
 * center-packed {@code AtlasRegion} member with a per-instance HORIZONTAL {@code flipX} mirror only
 * ({@link #usesInstanceMirrorX}; NOT {@link #usesInstanceMirrorY}) and introduces ONE new pure rule —
 * its native uniform draw scale is {@code scale * Settings.scale} on BOTH axes, modeled by
 * {@link #uniformScaleMultiplier} (settings scale for {@code GIANT_FIRE}, {@code 1f} otherwise) and
 * composed with the F27 {@code scaleYMultiplier} in the shared center-packed {@link #params} branch.
 * Its {@code delayTimer} is used only by {@code update()}, NOT by {@code render} (no render guard).
 *
 * <p>The newest (F29) member is the {@code vfx-misc-root} {@code TorchHeadFireEffect}
 * ({@link Kind#TORCH_HEAD_FIRE}): shape-C, reusing the {@code GlowyFireEyesEffect} fixed rect exactly
 * (offset/origin {@code 64}, size {@code 128&times;128}, src {@code 0,0,128,128}, hardcoded rotation
 * {@code 0f}) over its own instance {@code Texture img}, ADDITIVE, with the effect's own
 * {@code flippedX} horizontal flip (vertical always {@code false}) — resolved by the generalized
 * {@link #usesTexturedFlipX}. It adds ONE new pure rule: an ASYMMETRIC X scale
 * {@code scaleX = scale * }{@link #TORCH_HEAD_FIRE_SCALE_X_MULTIPLIER} ({@code 1.2f}) with
 * {@code scaleY = scale}. No new patch/bridge/console wiring; the default-off gate and per-instance
 * token semantics are unchanged.
 *
 * <p>The newest (F30) member is the {@code vfx-misc-root} {@code CardTrailEffect}
 * ({@link Kind#CARD_TRAIL}): the img ({@code AtlasRegion}) path with ONE new pure rule — the native
 * {@code render} passes a fixed ORIGIN {@code (6f, 6f)} and a fixed SIZE {@code (12f, 12f)}
 * regardless of the region's packed size, with rotation hardcoded {@code 0f}. The class does not
 * redeclare a {@code rotation} field but inherits {@code AbstractGameEffect.rotation}, and its native
 * draw ignores that inherited field, so the claimed draw forces {@code 0f}. It is ADDITIVE and draws
 * the region's own source UVs (the 9-arg
 * {@code TextureRegion} overload), which the canonical region view already reproduces. It resolves a
 * {@code private static} {@code AtlasRegion img} (like {@code ExhaustPileParticle}) and has a NO-ARG
 * constructor. No new patch/bridge/console wiring; the default-off gate and per-instance token
 * semantics are unchanged.
 *
 * <p>The newest (NRO-04 B01) member is the {@code vfx-combat} {@code FlyingOrbEffect}
 * ({@link Kind#FLYING_ORB}): the seam's FIRST VARIABLE-LENGTH MULTI-DRAW kind
 * ({@link #variableLengthMultiDraw}) and its first BOOLEAN {@code isDone} native draw guard
 * ({@link #guardIsBoolean}/{@link #guardBlocksBoolean}). Its fields are {@code TextureAtlas.AtlasRegion
 * img} (static, {@code ImageMaster.GLOW_SPARK_2}), {@code Vector2[] points}, {@code Vector2
 * pos}/{@code target}, its OWN {@code float rotation} (not inherited), and the inherited
 * {@code color}; it has NO {@code x}/{@code y}/{@code scale} field, so the reader consumes
 * {@code points[]} rather than x/y/scale. Its native {@code render} first returns when
 * {@code isDone}; then, under the additive blend ({@code 770/1} before, {@code 770/771} after), it
 * iterates {@code index} from {@code points.length - 1} DOWN TO {@code 1} (index {@code 0} is NEVER
 * drawn) and draws each NON-NULL {@code points[index]} center-packed (the draw POSITION offset uses
 * native INTEGER division of the packed size, {@code point.x - (packed/2)}, while the ORIGIN uses
 * native FLOAT division, {@code packed/2f}; the two differ by {@code 0.5} for an odd region such as
 * the native 81x81 {@code GLOW_SPARK_2}) with a
 * UNIFORM scale that starts at {@code Settings.scale * }{@link #flyingOrbStartScaleMultiplier}
 * ({@code 1.5f}) and is multiplied by {@link #flyingOrbScaleDecayPerDraw} ({@code 0.975f}) AFTER each
 * DRAWN point (so the per-draw scale sequence is {@code scale0, scale0*0.975, scale0*0.975^2, ...}),
 * with the effect's own {@code rotation} field. It draws no RNG. The B09 multi-pass failure contract
 * applies: a throw before any draw fails open (returns {@code false}), while a throw after at least
 * one draw keeps the claim (returns {@code true}) so native never double-draws. A freshly spawned
 * instance's ctor allocates {@code points = new Vector2[60]} (a length-60 array of nulls that
 * {@code update()} fills), so it draws nothing until {@code update()} runs. No new
 * patch/bridge/console wiring; the default-off gate and per-instance token semantics are unchanged.
 *
 * <p>The newest (NRO-04 B02) member is the {@code vfx-combat} {@code FlickCoinEffect}
 * ({@link Kind#FLICK_COIN}): a single-draw img ({@code TextureAtlas.AtlasRegion}) kind with a
 * DEDICATED position reader — its position fields are {@code cX}/{@code cY}/{@code yOffset}, NOT
 * {@code x}/{@code y} — and an ANISOTROPIC scale. This class stays pure/host-neutral; the dedicated
 * reader lives in {@link Sts1VfxArtRenderer}, which maps the effect's {@code cX}/{@code cY} onto the
 * shared {@code x}/{@code y} {@link #params} inputs and its {@code yOffset} onto {@code vY}. Its
 * native {@code render} (verified bytecode) is
 * {@code setBlendFunction(770, 1); setColor(color); sb.draw(img, cX - (img.packedWidth / 2),
 * cY - (img.packedHeight / 2) + yOffset, img.packedWidth / 2f, img.packedHeight / 2f,
 * img.packedWidth, img.packedHeight, scale * 0.7f, scale * 0.4f, rotation);
 * setBlendFunction(770, 771)}. So the draw POSITION half uses native INTEGER division of the packed
 * size while the ORIGIN half uses native FLOAT division ({@code packed/2f}; they differ by
 * {@code 0.5} for an odd region such as the native 81x81 region), the size is the packed size, the
 * draw scale is ANISOTROPIC ({@link #FLICK_COIN_SCALE_X} {@code = 0.7f} on X,
 * {@link #FLICK_COIN_SCALE_Y} {@code = 0.4f} on Y), the rotation comes from the effect's own
 * {@code rotation} field, and it is ADDITIVE. It has NO {@code isDone} guard, so it joins no guard/
 * flip/mirror/RNG capability ({@link #flickCoinUsesAnisotropicScale} is its only new pure predicate).
 * No new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
 * unchanged.
 *
 * <p>The newest (NRO-04 B03) member is the {@code vfx-combat} {@code HealPanelEffect}
 * ({@link Kind#HEAL_PANEL}): a bare-{@code Texture} fixed-source-rect kind over a STATIC
 * {@code Texture img} (loaded in the ctor via {@code ImageMaster.loadImage(...)}; there is NO
 * instance {@code img} field), reusing the shape-C path with a fixed src rect {@code (0, 0, 64, 64)},
 * a fixed origin {@code (32f, 32f)} and a fixed size {@code 64&times;64}, the effect's uniform
 * {@code scale}, its own {@code rotation} field and its own {@code color}, ADDITIVE. It adds ONE new
 * pure rule: its draw POSITION is PANEL-SPACE, depending on {@code Settings.HEIGHT} as well as
 * {@code Settings.scale} — {@code x = x - }{@link #HEAL_PANEL_X_OFFSET}{@code  + }
 * {@link #HEAL_PANEL_X_OFFSET}{@code  * Settings.scale} and
 * {@code y = Settings.HEIGHT - }{@link #HEAL_PANEL_Y_OFFSET}{@code  * Settings.scale - }
 * {@link #HEAL_PANEL_Y_OFFSET}. Because this class stays host-neutral it never reads
 * {@code Settings}: the renderer supplies the settings height through the trailing
 * {@link #params} overload ({@code settingsHeight}), which every other kind ignores. No new
 * patch/bridge/console wiring; the default-off gate and per-instance token semantics are unchanged.
 *
 * <p>The newest (NRO-04 B04) member is the {@code vfx-combat} {@code PingHpEffect}
 * ({@link Kind#PING_HP}): the HealPanel analogue, a bare-{@code Texture} fixed-source-rect kind over
 * the STATIC {@code ImageMaster.TP_HP} {@code Texture} resolved at draw time (there is NO instance
 * {@code img} field). It shares HealPanel's exact shape-C rect — fixed src {@code (0, 0, 64, 64)},
 * fixed origin {@code (32f, 32f)}, fixed size {@code 64&times;64} — and the identical PANEL-SPACE
 * position rule depending on {@code Settings.HEIGHT} and {@code Settings.scale}; it consumes the
 * effect's own {@code rotation} field and own {@code color} (yellow, animated alpha), and is
 * ADDITIVE. Its ONE difference from {@code HealPanelEffect} is the uniform draw scale: BOTH axes are
 * {@code scale * Settings.scale} (HealPanel used the plain {@code scale}); because this class stays
 * host-neutral the renderer supplies both the settings scale and the settings height through the
 * {@link #params} {@code settingsHeight} overload. No new patch/bridge/console wiring; the
 * default-off gate and per-instance token semantics are unchanged.
 */
public final class VfxDrawGeometry {

    /** The claimable draw formulas (the {@code vfx-stance-aura} FQNs plus the cross-family members). */
    public enum Kind {
        STANCE_AURA,
        WRATH_PARTICLE,
        DIVINITY_PARTICLE,
        CALM_PARTICLE,
        DIVINITY_STANCE_CHANGE,
        LIGHT_FLARE,
        FLASH_ATK_IMG,
        LIGHT_FLARE_M,
        LIGHT_FLARE_L,
        TORCH_PARTICLE_L,
        FIRE_BURST,
        RED_FIRE_BURST,
        SMOKE_BLUR,
        CEILING_DUST,
        NEMESIS_FIRE,
        SHIELD_PARTICLE,
        DEBUFF_PARTICLE,
        TORCH_PARTICLE_XL,
        GHOSTLY_WEAK_FIRE,
        GENERIC_SMOKE,
        EXHAUST_BLUR,
        ICE_SHATTER,
        WEB_PARTICLE,
        ENTANGLE,
        BLOCK_IMPACT_LINE,
        EXHAUST_PILE,
        UNKNOWN_PARTICLE,
        FLAME_PARTICLE,
        LIGHTNING_ORB_ACTIVATE,
        DAMAGE_IMPACT_BLUR,
        DAMAGE_IMPACT_LINE,
        DARK_ORB_PASSIVE,
        WARNING_SIGN,
        STUN_STAR,
        FALLING_DUST,
        LIGHTNING_EFFECT,
        FLAME_BALL,
        SHINE_LINES,
        TORCH_PARTICLE_M,
        TORCH_PARTICLE_S,
        SCENE_DUST,
        LIGHTNING_ORB_PASSIVE,
        GLOWY_FIRE_EYES,
        FLYING_SPIKE,
        CONE,
        FALLING_ICE,
        DAMAGE_HEART,
        SPOOKY_CHEST,
        IRONCLAD_VICTORY_FLAME,
        SPOOKIER_CHEST,
        CAMPFIRE_SLEEP_COVER,
        DEATH_SCREEN_FLOATY,
        WRATH_STANCE_CHANGE,
        STANCE_CHANGE_ABSORPTION,
        WATER_SPLASH,
        BUFF_PARTICLE,
        BOTTOM_FOG,
        GIANT_FIRE,
        TORCH_HEAD_FIRE,
        CARD_TRAIL,
        FLYING_ORB,
        FLICK_COIN,
        HEAL_PANEL,
        PING_HP
    }

    // Native CalmParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the FROST_ACTIVATE_VFX_1 texture.
    /** Native Calm draw origin x ({@code 32f}). */
    public static final float CALM_ORIGIN_X = 32f;
    /** Native Calm draw origin y ({@code 32f}). */
    public static final float CALM_ORIGIN_Y = 32f;
    /** Native Calm draw width ({@code 25f}). */
    public static final float CALM_WIDTH = 25f;
    /** Native Calm draw height ({@code 128f}). */
    public static final float CALM_HEIGHT = 128f;
    /** Native Calm draw source rect x ({@code 0}). */
    public static final int CALM_SRC_X = 0;
    /** Native Calm draw source rect y ({@code 0}). */
    public static final int CALM_SRC_Y = 0;
    /** Native Calm draw source rect width ({@code 64}). */
    public static final int CALM_SRC_W = 64;
    /** Native Calm draw source rect height ({@code 64}). */
    public static final int CALM_SRC_H = 64;

    // Native ShieldParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the ImageMaster.INTENT_DEFEND texture. The rotation is hardcoded to 0.
    /** Native Shield draw origin x ({@code 32f}). */
    public static final float SHIELD_ORIGIN_X = 32f;
    /** Native Shield draw origin y ({@code 32f}). */
    public static final float SHIELD_ORIGIN_Y = 32f;
    /** Native Shield draw width ({@code 64f}). */
    public static final float SHIELD_WIDTH = 64f;
    /** Native Shield draw height ({@code 64f}). */
    public static final float SHIELD_HEIGHT = 64f;
    /** Native Shield draw source rect x ({@code 0}). */
    public static final int SHIELD_SRC_X = 0;
    /** Native Shield draw source rect y ({@code 0}). */
    public static final int SHIELD_SRC_Y = 0;
    /** Native Shield draw source rect width ({@code 64}). */
    public static final int SHIELD_SRC_W = 64;
    /** Native Shield draw source rect height ({@code 64}). */
    public static final int SHIELD_SRC_H = 64;

    // Native DebuffParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native Debuff draw origin x ({@code 16f}). */
    public static final float DEBUFF_ORIGIN_X = 16f;
    /** Native Debuff draw origin y ({@code 16f}). */
    public static final float DEBUFF_ORIGIN_Y = 16f;
    /** Native Debuff draw width ({@code 32f}). */
    public static final float DEBUFF_WIDTH = 32f;
    /** Native Debuff draw height ({@code 32f}). */
    public static final float DEBUFF_HEIGHT = 32f;
    /** Native Debuff draw source rect x ({@code 0}). */
    public static final int DEBUFF_SRC_X = 0;
    /** Native Debuff draw source rect y ({@code 0}). */
    public static final int DEBUFF_SRC_Y = 0;
    /** Native Debuff draw source rect width ({@code 32}). */
    public static final int DEBUFF_SRC_W = 32;
    /** Native Debuff draw source rect height ({@code 32}). */
    public static final int DEBUFF_SRC_H = 32;

    // Native IceShatterEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native IceShatter draw origin x ({@code 32f}). */
    public static final float ICE_SHATTER_ORIGIN_X = 32f;
    /** Native IceShatter draw origin y ({@code 32f}). */
    public static final float ICE_SHATTER_ORIGIN_Y = 32f;
    /** Native IceShatter draw width ({@code 64f}). */
    public static final float ICE_SHATTER_WIDTH = 64f;
    /** Native IceShatter draw height ({@code 64f}). */
    public static final float ICE_SHATTER_HEIGHT = 64f;
    /** Native IceShatter draw source rect x ({@code 0}). */
    public static final int ICE_SHATTER_SRC_X = 0;
    /** Native IceShatter draw source rect y ({@code 0}). */
    public static final int ICE_SHATTER_SRC_Y = 0;
    /** Native IceShatter draw source rect width ({@code 64}). */
    public static final int ICE_SHATTER_SRC_W = 64;
    /** Native IceShatter draw source rect height ({@code 64}). */
    public static final int ICE_SHATTER_SRC_H = 64;

    // Native WebParticleEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the static ImageMaster.WEB_VFX Texture. The rotation is hardcoded to 0.
    /** Native Web draw origin x ({@code 32f}). */
    public static final float WEB_ORIGIN_X = 32f;
    /** Native Web draw origin y ({@code 32f}). */
    public static final float WEB_ORIGIN_Y = 32f;
    /** Native Web draw width ({@code 64f}). */
    public static final float WEB_WIDTH = 64f;
    /** Native Web draw height ({@code 64f}). */
    public static final float WEB_HEIGHT = 64f;
    /** Native Web draw source rect x ({@code 0}). */
    public static final int WEB_SRC_X = 0;
    /** Native Web draw source rect y ({@code 0}). */
    public static final int WEB_SRC_Y = 0;
    /** Native Web draw source rect width ({@code 64}). */
    public static final int WEB_SRC_W = 64;
    /** Native Web draw source rect height ({@code 64}). */
    public static final int WEB_SRC_H = 64;

    // Native UnknownParticleEffect draw constants (see the class Javadoc): fixed offset/origin/size
    // and the fixed source rect of its own instance img Texture. The rotation comes from the field.
    /** Native Unknown draw offset/origin ({@code 64f}). */
    public static final float UNKNOWN_OFFSET = 64f;
    /** Native Unknown draw origin ({@code 64f}). */
    public static final float UNKNOWN_ORIGIN = 64f;
    /** Native Unknown draw width/height ({@code 128f}). */
    public static final float UNKNOWN_SIZE = 128f;
    /** Native Unknown draw source rect x ({@code 0}). */
    public static final int UNKNOWN_SRC_X = 0;
    /** Native Unknown draw source rect y ({@code 0}). */
    public static final int UNKNOWN_SRC_Y = 0;
    /** Native Unknown draw source rect width ({@code 128}). */
    public static final int UNKNOWN_SRC_W = 128;
    /** Native Unknown draw source rect height ({@code 128}). */
    public static final int UNKNOWN_SRC_H = 128;

    // Native DarkOrbPassiveEffect draw constants (see the class Javadoc): fixed
    // offset/origin/size and the fixed source rect (0, 0, 74, 74 — the full 74x74 region) over its
    // own instance img Texture. The rotation comes from the field.
    /** Native DarkOrb draw offset/origin ({@code 37f}). */
    public static final float DARK_ORB_OFFSET = 37f;
    /** Native DarkOrb draw origin ({@code 37f}). */
    public static final float DARK_ORB_ORIGIN = 37f;
    /** Native DarkOrb draw width/height ({@code 74f}). */
    public static final float DARK_ORB_SIZE = 74f;
    /** Native DarkOrb draw source rect x ({@code 0}). */
    public static final int DARK_ORB_SRC_X = 0;
    /** Native DarkOrb draw source rect y ({@code 0}). */
    public static final int DARK_ORB_SRC_Y = 0;
    /** Native DarkOrb draw source rect width ({@code 74}, the full region). */
    public static final int DARK_ORB_SRC_W = 74;
    /** Native DarkOrb draw source rect height ({@code 74}, the full region). */
    public static final int DARK_ORB_SRC_H = 74;

    // Native LightningOrbPassiveEffect draw constants (see the class Javadoc): fixed
    // offset/origin/size and the fixed source rect (0, 0, 122, 122 — the full 122x122 rect) over its
    // own instance Texture img. The rotation comes from the field and the native draw passes the
    // effect's own flipX/flipY booleans (the first claimable kind to do so).
    /** Native LightningOrbPassive draw offset/origin ({@code 61f}). */
    public static final float LIGHTNING_ORB_PASSIVE_OFFSET = 61f;
    /** Native LightningOrbPassive draw origin ({@code 61f}). */
    public static final float LIGHTNING_ORB_PASSIVE_ORIGIN = 61f;
    /** Native LightningOrbPassive draw width/height ({@code 122f}). */
    public static final float LIGHTNING_ORB_PASSIVE_SIZE = 122f;
    /** Native LightningOrbPassive draw source rect x ({@code 0}). */
    public static final int LIGHTNING_ORB_PASSIVE_SRC_X = 0;
    /** Native LightningOrbPassive draw source rect y ({@code 0}). */
    public static final int LIGHTNING_ORB_PASSIVE_SRC_Y = 0;
    /** Native LightningOrbPassive draw source rect width ({@code 122}, the full rect). */
    public static final int LIGHTNING_ORB_PASSIVE_SRC_W = 122;
    /** Native LightningOrbPassive draw source rect height ({@code 122}, the full rect). */
    public static final int LIGHTNING_ORB_PASSIVE_SRC_H = 122;

    // Native GlowyFireEyesEffect draw constants (see the class Javadoc): fixed offset/origin/size and
    // the fixed source rect (0, 0, 128, 128 — the full 128x128 rect) over its own instance Texture
    // img. The rotation is hardcoded to 0f and the native draw passes the effect's own flippedX
    // boolean with a hardcoded false vertical flip.
    /** Native GlowyFireEyes draw offset/origin ({@code 64f}). */
    public static final float GLOWY_FIRE_EYES_OFFSET = 64f;
    /** Native GlowyFireEyes draw origin ({@code 64f}). */
    public static final float GLOWY_FIRE_EYES_ORIGIN = 64f;
    /** Native GlowyFireEyes draw width/height ({@code 128f}). */
    public static final float GLOWY_FIRE_EYES_SIZE = 128f;
    /** Native GlowyFireEyes draw source rect x ({@code 0}). */
    public static final int GLOWY_FIRE_EYES_SRC_X = 0;
    /** Native GlowyFireEyes draw source rect y ({@code 0}). */
    public static final int GLOWY_FIRE_EYES_SRC_Y = 0;
    /** Native GlowyFireEyes draw source rect width ({@code 128}, the full rect). */
    public static final int GLOWY_FIRE_EYES_SRC_W = 128;
    /** Native GlowyFireEyes draw source rect height ({@code 128}, the full rect). */
    public static final int GLOWY_FIRE_EYES_SRC_H = 128;

    // Native TorchHeadFireEffect draw multiplier (see the class Javadoc): it reuses the
    // GlowyFireEyes fixed shape-C rect (offset/origin 64, size 128, src 0,0,128,128, rotation 0f)
    // over its own instance Texture img, but its native draw scale is ASYMMETRIC —
    // scaleX = scale * 1.2f while scaleY = scale. It is the only kind with this X multiplier.
    /** Native TorchHeadFire X-axis scale multiplier ({@code 1.2f}). */
    public static final float TORCH_HEAD_FIRE_SCALE_X_MULTIPLIER = 1.2f;

    // Native CardTrailEffect draw constants (see the class Javadoc): a fixed ORIGIN and fixed SIZE
    // on the img (packed-region) path, INDEPENDENT of the region's packed size. Its rotation is
    // hardcoded to 0f (the class inherits AbstractGameEffect.rotation but its draw ignores it) and it
    // draws additively.
    /** Native CardTrail draw origin ({@code 6f}). */
    public static final float CARD_TRAIL_ORIGIN = 6f;
    /** Native CardTrail draw width/height ({@code 12f}). */
    public static final float CARD_TRAIL_SIZE = 12f;

    // Native FlyingOrbEffect draw rule (see the class Javadoc): ADDITIVE, variable-length multi-draw
    // over the effect's own Vector2[] points. Each non-null points[index] for index from
    // points.length-1 DOWN TO 1 (index 0 is NEVER drawn) is drawn center-packed (origin packed/2)
    // with a UNIFORM scale that starts at Settings.scale * this start multiplier and is multiplied by
    // this decay factor after each DRAWN point; the rotation comes from the effect's OWN rotation
    // field. Static img = ImageMaster.GLOW_SPARK_2; the class has NO x/y/scale field and its render is
    // guarded by `if (isDone) return`.
    /** Native FlyingOrb starting uniform scale multiplier applied to {@code Settings.scale} ({@code 1.5f}). */
    public static final float FLYING_ORB_START_SCALE_MULTIPLIER = 1.5f;
    /** Native FlyingOrb per-drawn-point uniform scale decay factor ({@code 0.975f}). */
    public static final float FLYING_ORB_SCALE_DECAY_PER_DRAW = 0.975f;
    /** Native FlyingOrbEffect boolean draw-guard field name ({@code "isDone"}, blocks when true). */
    public static final String FLYING_ORB_GUARD_FIELD = "isDone";

    // Native FlickCoinEffect draw rule (see the class Javadoc): the img (AtlasRegion) path with a
    // DEDICATED position reader (its fields are cX/cY/yOffset, NOT x/y) and an ANISOTROPIC draw
    // scale. The native render is
    //   setBlendFunction(770, 1); setColor(color);
    //   sb.draw(img, cX - (img.packedWidth / 2), cY - (img.packedHeight / 2) + yOffset,
    //           img.packedWidth / 2f, img.packedHeight / 2f,
    //           img.packedWidth, img.packedHeight,
    //           scale * 0.7f, scale * 0.4f, rotation);
    //   setBlendFunction(770, 771);
    // so the POSITION half uses native INTEGER division of the packed size while the ORIGIN half
    // uses native FLOAT division (differing by 0.5 for an odd region), and the two draw scale axes
    // are scale * 0.7f (X) and scale * 0.4f (Y). Additive; no isDone guard.
    /** Native FlickCoin X-axis scale multiplier ({@code 0.7f}). */
    public static final float FLICK_COIN_SCALE_X = 0.7f;
    /** Native FlickCoin Y-axis scale multiplier ({@code 0.4f}). */
    public static final float FLICK_COIN_SCALE_Y = 0.4f;

    // Native HealPanelEffect draw constants (see the class Javadoc): a bare-Texture fixed-rect kind
    // over the STATIC Texture img (loaded in the ctor from
    // ImageMaster.loadImage("images/ui/topPanel/panel_heart_white.png"); there is NO instance img
    // field). The geometry is fixed: src (0, 0, 64, 64), origin (32f, 32f), size 64f x 64f. The draw
    // POSITION is panel-space: x = x - HEAL_PANEL_X_OFFSET + HEAL_PANEL_X_OFFSET * Settings.scale and
    // y = Settings.HEIGHT - HEAL_PANEL_Y_OFFSET * Settings.scale - HEAL_PANEL_Y_OFFSET, so it depends
    // on the caller-supplied settings scale AND on the caller-supplied settings height (this class
    // stays host-neutral and never reads Settings itself). The scale is the effect's own uniform
    // scale and the rotation comes from the field. It is ADDITIVE and uses the effect's own color.
    /** Native HealPanel draw source rect x ({@code 0}). */
    public static final int HEAL_PANEL_SRC_X = 0;
    /** Native HealPanel draw source rect y ({@code 0}). */
    public static final int HEAL_PANEL_SRC_Y = 0;
    /** Native HealPanel draw source rect width ({@code 64}). */
    public static final int HEAL_PANEL_SRC_W = 64;
    /** Native HealPanel draw source rect height ({@code 64}). */
    public static final int HEAL_PANEL_SRC_H = 64;
    /** Native HealPanel draw origin ({@code 32f}). */
    public static final float HEAL_PANEL_ORIGIN = 32f;
    /** Native HealPanel draw width/height ({@code 64f}). */
    public static final float HEAL_PANEL_SIZE = 64f;
    /** Native HealPanel X-axis fixed offset and settings-scale coefficient ({@code 32f}). */
    public static final float HEAL_PANEL_X_OFFSET = 32f;
    /** Native HealPanel Y-axis fixed offset and settings-scale coefficient ({@code 32f}). */
    public static final float HEAL_PANEL_Y_OFFSET = 32f;

    // Native PingHpEffect draw constants (see the class Javadoc): the HealPanel analogue — a
    // bare-Texture fixed-rect kind over the STATIC ImageMaster.TP_HP Texture resolved at draw time
    // (there is NO instance img field). The geometry is fixed: src (0, 0, 64, 64), origin (32f, 32f),
    // size 64f x 64f, with the identical PANEL-SPACE position
    // x = x - PING_HP_X_OFFSET + PING_HP_X_OFFSET * Settings.scale and
    // y = Settings.HEIGHT - PING_HP_Y_OFFSET * Settings.scale - PING_HP_Y_OFFSET. Its ONE difference
    // from HealPanelEffect is the uniform draw scale: BOTH axes are scale * Settings.scale (HealPanel
    // used the plain effect scale). It is ADDITIVE and uses the effect's own color.
    /** Native PingHp draw source rect x ({@code 0}). */
    public static final int PING_HP_SRC_X = 0;
    /** Native PingHp draw source rect y ({@code 0}). */
    public static final int PING_HP_SRC_Y = 0;
    /** Native PingHp draw source rect width ({@code 64}). */
    public static final int PING_HP_SRC_W = 64;
    /** Native PingHp draw source rect height ({@code 64}). */
    public static final int PING_HP_SRC_H = 64;
    /** Native PingHp draw origin ({@code 32f}). */
    public static final float PING_HP_ORIGIN = 32f;
    /** Native PingHp draw width/height ({@code 64f}). */
    public static final float PING_HP_SIZE = 64f;
    /** Native PingHp X-axis fixed offset and settings-scale coefficient ({@code 32f}). */
    public static final float PING_HP_X_OFFSET = 32f;
    /** Native PingHp Y-axis fixed offset and settings-scale coefficient ({@code 32f}). */
    public static final float PING_HP_Y_OFFSET = 32f;

    // Native WarningSignEffect draw constants (see the class Javadoc): fixed origin/size and the
    // fixed source rect of the static ImageMaster.WARNING_ICON_VFX Texture. The rotation is
    // hardcoded to 0 and the uniform scale is the hardcoded Settings.scale * 2f (the effect has no
    // scale field).
    /** Native WarningSign draw origin x ({@code 32f}). */
    public static final float WARNING_ORIGIN_X = 32f;
    /** Native WarningSign draw origin y ({@code 32f}). */
    public static final float WARNING_ORIGIN_Y = 32f;
    /** Native WarningSign draw width ({@code 64f}). */
    public static final float WARNING_WIDTH = 64f;
    /** Native WarningSign draw height ({@code 64f}). */
    public static final float WARNING_HEIGHT = 64f;
    /** Native WarningSign uniform scale multiplier ({@code 2f}, applied to {@code Settings.scale}). */
    public static final float WARNING_SCALE_FACTOR = 2f;
    /** Native WarningSign draw source rect x ({@code 0}). */
    public static final int WARNING_SRC_X = 0;
    /** Native WarningSign draw source rect y ({@code 0}). */
    public static final int WARNING_SRC_Y = 0;
    /** Native WarningSign draw source rect width ({@code 64}). */
    public static final int WARNING_SRC_W = 64;
    /** Native WarningSign draw source rect height ({@code 64}). */
    public static final int WARNING_SRC_H = 64;

    // Native StunStarEffect position-offset multipliers (see the class Javadoc): the draw position
    // is x - vX * 30f * Settings.scale, y - vY * 5f * Settings.scale.
    /** StunStar vX position-offset multiplier ({@code 30f}). */
    public static final float STUN_STAR_VX_FACTOR = 30f;
    /** StunStar vY position-offset multiplier ({@code 5f}). */
    public static final float STUN_STAR_VY_FACTOR = 5f;

    // Native FlameBallParticleEffect origin offset (see the class Javadoc): its draw origin Y is
    // packedHeight/2f + 20f * Settings.scale, i.e. the shared center origin lifted by this amount
    // multiplied by the caller-supplied settings scale.
    /** Native FlameBall origin Y offset above the shared center origin, per unit settings scale ({@code 20f}). */
    public static final float FLAME_BALL_ORIGIN_Y_OFFSET = 20f;

    // Native ConeEffect draw multiplier (see the class Javadoc): its uniform draw scale is the
    // effect's own scale multiplied by this hardcoded factor, and its draw origin X is 0f (NOT the
    // shared packedWidth/2f center) with origin Y still packedHeight/2f.
    /** Native ConeEffect uniform scale multiplier ({@code 1.1f}). */
    public static final float CONE_SCALE_MULTIPLIER = 1.1f;

    // Native FallingIceEffect draw constants (see the class Javadoc): a fixed origin/size rect and
    // the fixed source rect of its own instance Texture img. Native passes x/y through unchanged (no
    // position offset); the rotation comes from the field.
    /** Native FallingIce draw origin ({@code 48f}). */
    public static final float FALLING_ICE_ORIGIN = 48f;
    /** Native FallingIce draw width/height ({@code 96f}). */
    public static final float FALLING_ICE_SIZE = 96f;
    /** Native FallingIce draw source rect x ({@code 0}). */
    public static final int FALLING_ICE_SRC_X = 0;
    /** Native FallingIce draw source rect y ({@code 0}). */
    public static final int FALLING_ICE_SRC_Y = 0;
    /** Native FallingIce draw source rect width ({@code 96}). */
    public static final int FALLING_ICE_SRC_W = 96;
    /** Native FallingIce draw source rect height ({@code 96}). */
    public static final int FALLING_ICE_SRC_H = 96;

    // Native StanceChangeAbsorptionParticle draw constants (see the class Javadoc): a fixed shape-C
    // rect drawn TWICE over the static ImageMaster.WOBBLY_ORB_VFX Texture. Each pass shares the
    // offset (16f), origin (16f), size (32f) and src rect (0, 0, 32, 32); the rotation field is
    // offset by -200f. The per-pass scaleX/scaleY are RNG-derived and applied by the renderer, so
    // params returns the base (scale, scale) — see drawPassRandomRanges.
    /** Native StanceChangeAbsorption draw offset/origin ({@code 16f}). */
    public static final float STANCE_CHANGE_ABSORPTION_OFFSET = 16f;
    /** Native StanceChangeAbsorption draw origin ({@code 16f}). */
    public static final float STANCE_CHANGE_ABSORPTION_ORIGIN = 16f;
    /** Native StanceChangeAbsorption draw width/height ({@code 32f}). */
    public static final float STANCE_CHANGE_ABSORPTION_SIZE = 32f;
    /** Native StanceChangeAbsorption rotation offset ({@code -200f}). */
    public static final float STANCE_CHANGE_ABSORPTION_ROTATION_OFFSET = -200f;
    /** Native StanceChangeAbsorption draw source rect x ({@code 0}). */
    public static final int STANCE_CHANGE_ABSORPTION_SRC_X = 0;
    /** Native StanceChangeAbsorption draw source rect y ({@code 0}). */
    public static final int STANCE_CHANGE_ABSORPTION_SRC_Y = 0;
    /** Native StanceChangeAbsorption draw source rect width ({@code 32}). */
    public static final int STANCE_CHANGE_ABSORPTION_SRC_W = 32;
    /** Native StanceChangeAbsorption draw source rect height ({@code 32}). */
    public static final int STANCE_CHANGE_ABSORPTION_SRC_H = 32;

    // Native WaterSplashParticleEffect draw multiplier (see the class Javadoc): unlike every
    // pre-existing center-packed kind, its draw scale is ANISOTROPIC — scaleY = scale * 0.54f while
    // scaleX = scale. It is the only kind {@link #params} passes a non-1f scaleYMultiplier for.
    /** Native WaterSplash scaleY multiplier ({@code 0.54f}). */
    public static final float WATER_SPLASH_SCALE_Y_MULTIPLIER = 0.54f;

    // The per-kind native wait-phase guard field names (the seam's first draw guard): a claimed
    // instance whose guard field value blocks per that kind's condition is declined (draws nothing),
    // exactly like the native render's wait phase. The block condition is per-kind and lives in the
    // pure guardBlocks(kind, value) predicate (the source of truth), not in this field-name mapping.
    /** Native FallingIceEffect guard field name ({@code "waitTimer"}, satisfied when {@code < 0f}). */
    public static final String FALLING_ICE_GUARD_FIELD = "waitTimer";
    /** Native DamageHeartEffect guard field name ({@code "delayTimer"}, satisfied when {@code < 0f}). */
    public static final String DAMAGE_HEART_GUARD_FIELD = "delayTimer";
    /**
     * Native WrathStanceChangeParticle guard field name ({@code "delayTimer"}); this kind's guard
     * BLOCKS when {@code > 0f} (draws when {@code <= 0f}) — see {@link #guardBlocks}.
     */
    public static final String WRATH_STANCE_CHANGE_GUARD_FIELD = "delayTimer";

    // The ordered native MathUtils.random(min, max) ranges one claimed draw of a kind consumes (see
    // randomRanges). WrathStanceChangeParticle draws with scaleX = scale * random(2.9f, 3.1f) and
    // scaleY = scale * random(0.95f, 1.05f), in that exact call order.
    /** Native WrathStanceChangeParticle scaleX RNG range min ({@code 2.9f}). */
    public static final float WRATH_STANCE_CHANGE_SCALE_X_MIN = 2.9f;
    /** Native WrathStanceChangeParticle scaleX RNG range max ({@code 3.1f}). */
    public static final float WRATH_STANCE_CHANGE_SCALE_X_MAX = 3.1f;
    /** Native WrathStanceChangeParticle scaleY RNG range min ({@code 0.95f}). */
    public static final float WRATH_STANCE_CHANGE_SCALE_Y_MIN = 0.95f;
    /** Native WrathStanceChangeParticle scaleY RNG range max ({@code 1.05f}). */
    public static final float WRATH_STANCE_CHANGE_SCALE_Y_MAX = 1.05f;

    /**
     * The ordered native RNG ranges for {@link Kind#WRATH_STANCE_CHANGE} (see {@link #randomRanges}):
     * scaleX {@code (2.9f, 3.1f)} then scaleY {@code (0.95f, 1.05f)}, in native call order. Immutable.
     */
    private static final java.util.List<float[]> WRATH_STANCE_CHANGE_RANDOM_RANGES =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                    new float[] {
                            WRATH_STANCE_CHANGE_SCALE_X_MIN, WRATH_STANCE_CHANGE_SCALE_X_MAX },
                    new float[] {
                            WRATH_STANCE_CHANGE_SCALE_Y_MIN, WRATH_STANCE_CHANGE_SCALE_Y_MAX }));

    // The ordered per-PASS native MathUtils.random(min, max) ranges a MULTI-DRAW kind consumes (see
    // drawPassRandomRanges). StanceChangeAbsorptionParticle draws TWICE over the same static
    // ImageMaster.WOBBLY_ORB_VFX Texture; pass 0 draws scaleX/scaleY with scale * random(0.5f, 2.0f)
    // each, pass 1 with scale * random(0.6f, 2.5f) each, in that exact pass/range order.
    /** Native StanceChangeAbsorption pass 0 scaleX/scaleY RNG range min ({@code 0.5f}). */
    public static final float STANCE_CHANGE_ABSORPTION_PASS0_MIN = 0.5f;
    /** Native StanceChangeAbsorption pass 0 scaleX/scaleY RNG range max ({@code 2.0f}). */
    public static final float STANCE_CHANGE_ABSORPTION_PASS0_MAX = 2.0f;
    /** Native StanceChangeAbsorption pass 1 scaleX/scaleY RNG range min ({@code 0.6f}). */
    public static final float STANCE_CHANGE_ABSORPTION_PASS1_MIN = 0.6f;
    /** Native StanceChangeAbsorption pass 1 scaleX/scaleY RNG range max ({@code 2.5f}). */
    public static final float STANCE_CHANGE_ABSORPTION_PASS1_MAX = 2.5f;

    /**
     * The ordered per-pass native RNG ranges for {@link Kind#STANCE_CHANGE_ABSORPTION} (see
     * {@link #drawPassRandomRanges}): pass 0 is scaleX {@code (0.5f, 2.0f)} then scaleY
     * {@code (0.5f, 2.0f)}, and pass 1 is scaleX {@code (0.6f, 2.5f)} then scaleY
     * {@code (0.6f, 2.5f)}, in native call order. Immutable.
     */
    private static final java.util.List<java.util.List<float[]>>
            STANCE_CHANGE_ABSORPTION_DRAW_PASS_RANDOM_RANGES =
            java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                    java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                            new float[] {STANCE_CHANGE_ABSORPTION_PASS0_MIN,
                                    STANCE_CHANGE_ABSORPTION_PASS0_MAX },
                            new float[] {STANCE_CHANGE_ABSORPTION_PASS0_MIN,
                                    STANCE_CHANGE_ABSORPTION_PASS0_MAX })),
                    java.util.Collections.unmodifiableList(java.util.Arrays.asList(
                            new float[] {STANCE_CHANGE_ABSORPTION_PASS1_MIN,
                                    STANCE_CHANGE_ABSORPTION_PASS1_MAX },
                            new float[] {STANCE_CHANGE_ABSORPTION_PASS1_MIN,
                                    STANCE_CHANGE_ABSORPTION_PASS1_MAX }))));

    /** Resolved draw arguments; all finite, origin is the native center origin. */
    public static final class Params {
        public final float x;
        public final float y;
        public final float originX;
        public final float originY;
        public final float width;
        public final float height;
        public final float scaleX;
        public final float scaleY;
        public final float rotation;

        public Params(float x, float y, float originX, float originY, float width, float height,
                float scaleX, float scaleY, float rotation) {
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Params)) return false;
            Params p = (Params) other;
            return Float.compare(x, p.x) == 0
                    && Float.compare(y, p.y) == 0
                    && Float.compare(originX, p.originX) == 0
                    && Float.compare(originY, p.originY) == 0
                    && Float.compare(width, p.width) == 0
                    && Float.compare(height, p.height) == 0
                    && Float.compare(scaleX, p.scaleX) == 0
                    && Float.compare(scaleY, p.scaleY) == 0
                    && Float.compare(rotation, p.rotation) == 0;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(originX);
            result = 31 * result + Float.floatToIntBits(originY);
            result = 31 * result + Float.floatToIntBits(width);
            result = 31 * result + Float.floatToIntBits(height);
            result = 31 * result + Float.floatToIntBits(scaleX);
            result = 31 * result + Float.floatToIntBits(scaleY);
            result = 31 * result + Float.floatToIntBits(rotation);
            return result;
        }

        @Override
        public String toString() {
            return "Params{x=" + x + ", y=" + y + ", originX=" + originX + ", originY=" + originY
                    + ", width=" + width + ", height=" + height + ", scaleX=" + scaleX
                    + ", scaleY=" + scaleY + ", rotation=" + rotation + '}';
        }
    }

    private VfxDrawGeometry() {}

    /**
     * FQN -&gt; {@link Kind}, or {@code null} when the class is not a claimable member of the
     * seam (fail-open). Matches only the exact FQNs owned by {@link VfxClaimPolicy}; null, blank,
     * near-misses ({@code ...StanceAuraEffect2}), and nested ({@code ...StanceAuraEffect$Sub}) fail
     * open.
     */
    public static Kind kindFor(String nativeClassName) {
        if (nativeClassName == null) return null;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return null;
        if (VfxClaimPolicy.STANCE_AURA_EFFECT.equals(value)) return Kind.STANCE_AURA;
        if (VfxClaimPolicy.WRATH_PARTICLE_EFFECT.equals(value)) return Kind.WRATH_PARTICLE;
        if (VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT.equals(value)) return Kind.DIVINITY_PARTICLE;
        if (VfxClaimPolicy.CALM_PARTICLE_EFFECT.equals(value)) return Kind.CALM_PARTICLE;
        if (VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE.equals(value)) {
            return Kind.DIVINITY_STANCE_CHANGE;
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE.equals(value)) return Kind.LIGHT_FLARE;
        if (VfxClaimPolicy.FLASH_ATK_IMG.equals(value)) return Kind.FLASH_ATK_IMG;
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_M.equals(value)) return Kind.LIGHT_FLARE_M;
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_L.equals(value)) return Kind.LIGHT_FLARE_L;
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_L.equals(value)) return Kind.TORCH_PARTICLE_L;
        if (VfxClaimPolicy.FIRE_BURST.equals(value)) return Kind.FIRE_BURST;
        if (VfxClaimPolicy.RED_FIRE_BURST.equals(value)) return Kind.RED_FIRE_BURST;
        if (VfxClaimPolicy.SMOKE_BLUR.equals(value)) return Kind.SMOKE_BLUR;
        if (VfxClaimPolicy.CEILING_DUST.equals(value)) return Kind.CEILING_DUST;
        if (VfxClaimPolicy.NEMESIS_FIRE.equals(value)) return Kind.NEMESIS_FIRE;
        if (VfxClaimPolicy.SHIELD_PARTICLE.equals(value)) return Kind.SHIELD_PARTICLE;
        if (VfxClaimPolicy.DEBUFF_PARTICLE.equals(value)) return Kind.DEBUFF_PARTICLE;
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL.equals(value)) return Kind.TORCH_PARTICLE_XL;
        if (VfxClaimPolicy.GHOSTLY_WEAK_FIRE.equals(value)) return Kind.GHOSTLY_WEAK_FIRE;
        if (VfxClaimPolicy.GENERIC_SMOKE.equals(value)) return Kind.GENERIC_SMOKE;
        if (VfxClaimPolicy.EXHAUST_BLUR.equals(value)) return Kind.EXHAUST_BLUR;
        if (VfxClaimPolicy.ICE_SHATTER.equals(value)) return Kind.ICE_SHATTER;
        if (VfxClaimPolicy.WEB_PARTICLE.equals(value)) return Kind.WEB_PARTICLE;
        if (VfxClaimPolicy.ENTANGLE_EFFECT.equals(value)) return Kind.ENTANGLE;
        if (VfxClaimPolicy.BLOCK_IMPACT_LINE.equals(value)) return Kind.BLOCK_IMPACT_LINE;
        if (VfxClaimPolicy.EXHAUST_PILE_PARTICLE.equals(value)) return Kind.EXHAUST_PILE;
        if (VfxClaimPolicy.UNKNOWN_PARTICLE.equals(value)) return Kind.UNKNOWN_PARTICLE;
        if (VfxClaimPolicy.FLAME_PARTICLE.equals(value)) return Kind.FLAME_PARTICLE;
        if (VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE.equals(value)) {
            return Kind.LIGHTNING_ORB_ACTIVATE;
        }
        if (VfxClaimPolicy.DAMAGE_IMPACT_BLUR.equals(value)) return Kind.DAMAGE_IMPACT_BLUR;
        if (VfxClaimPolicy.DAMAGE_IMPACT_LINE.equals(value)) return Kind.DAMAGE_IMPACT_LINE;
        if (VfxClaimPolicy.DARK_ORB_PASSIVE.equals(value)) return Kind.DARK_ORB_PASSIVE;
        if (VfxClaimPolicy.WARNING_SIGN.equals(value)) return Kind.WARNING_SIGN;
        if (VfxClaimPolicy.STUN_STAR.equals(value)) return Kind.STUN_STAR;
        if (VfxClaimPolicy.FALLING_DUST.equals(value)) return Kind.FALLING_DUST;
        if (VfxClaimPolicy.LIGHTNING_EFFECT.equals(value)) return Kind.LIGHTNING_EFFECT;
        if (VfxClaimPolicy.FLAME_BALL.equals(value)) return Kind.FLAME_BALL;
        if (VfxClaimPolicy.SHINE_LINES.equals(value)) return Kind.SHINE_LINES;
        if (VfxClaimPolicy.TORCH_PARTICLE_M.equals(value)) return Kind.TORCH_PARTICLE_M;
        if (VfxClaimPolicy.TORCH_PARTICLE_S.equals(value)) return Kind.TORCH_PARTICLE_S;
        if (VfxClaimPolicy.SCENE_DUST.equals(value)) return Kind.SCENE_DUST;
        if (VfxClaimPolicy.LIGHTNING_ORB_PASSIVE.equals(value)) return Kind.LIGHTNING_ORB_PASSIVE;
        if (VfxClaimPolicy.GLOWY_FIRE_EYES.equals(value)) return Kind.GLOWY_FIRE_EYES;
        if (VfxClaimPolicy.FLYING_SPIKE.equals(value)) return Kind.FLYING_SPIKE;
        if (VfxClaimPolicy.CONE_EFFECT.equals(value)) return Kind.CONE;
        if (VfxClaimPolicy.FALLING_ICE.equals(value)) return Kind.FALLING_ICE;
        if (VfxClaimPolicy.DAMAGE_HEART.equals(value)) return Kind.DAMAGE_HEART;
        if (VfxClaimPolicy.SPOOKY_CHEST.equals(value)) return Kind.SPOOKY_CHEST;
        if (VfxClaimPolicy.IRONCLAD_VICTORY_FLAME.equals(value)) {
            return Kind.IRONCLAD_VICTORY_FLAME;
        }
        if (VfxClaimPolicy.SPOOKIER_CHEST.equals(value)) return Kind.SPOOKIER_CHEST;
        if (VfxClaimPolicy.CAMPFIRE_SLEEP_COVER.equals(value)) return Kind.CAMPFIRE_SLEEP_COVER;
        if (VfxClaimPolicy.DEATH_SCREEN_FLOATY.equals(value)) return Kind.DEATH_SCREEN_FLOATY;
        if (VfxClaimPolicy.WRATH_STANCE_CHANGE.equals(value)) return Kind.WRATH_STANCE_CHANGE;
        if (VfxClaimPolicy.STANCE_CHANGE_ABSORPTION.equals(value)) {
            return Kind.STANCE_CHANGE_ABSORPTION;
        }
        if (VfxClaimPolicy.WATER_SPLASH.equals(value)) return Kind.WATER_SPLASH;
        if (VfxClaimPolicy.BUFF_PARTICLE.equals(value)) return Kind.BUFF_PARTICLE;
        if (VfxClaimPolicy.BOTTOM_FOG.equals(value)) return Kind.BOTTOM_FOG;
        if (VfxClaimPolicy.GIANT_FIRE.equals(value)) return Kind.GIANT_FIRE;
        if (VfxClaimPolicy.TORCH_HEAD_FIRE.equals(value)) return Kind.TORCH_HEAD_FIRE;
        if (VfxClaimPolicy.CARD_TRAIL.equals(value)) return Kind.CARD_TRAIL;
        if (VfxClaimPolicy.FLYING_ORB.equals(value)) return Kind.FLYING_ORB;
        if (VfxClaimPolicy.FLICK_COIN.equals(value)) return Kind.FLICK_COIN;
        if (VfxClaimPolicy.HEAL_PANEL.equals(value)) return Kind.HEAL_PANEL;
        if (VfxClaimPolicy.PING_HP.equals(value)) return Kind.PING_HP;
        return null;
    }

    /**
     * Pure per-kind blend policy: {@code true} when the native render installs the additive blend
     * {@code (SRC_ALPHA, ONE)} around its draw and restores {@code (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)},
     * {@code false} when it never touches the blend function and draws under whatever ambient blend
     * is active.
     *
     * <p>Most kinds are additive; the ambient kinds ({@link Kind#FLASH_ATK_IMG},
     * {@link Kind#SMOKE_BLUR}, {@link Kind#CEILING_DUST}, {@link Kind#NEMESIS_FIRE},
     * {@link Kind#DEBUFF_PARTICLE}, {@link Kind#GENERIC_SMOKE}, {@link Kind#EXHAUST_BLUR},
     * {@link Kind#BLOCK_IMPACT_LINE}, {@link Kind#EXHAUST_PILE}, {@link Kind#UNKNOWN_PARTICLE},
     * {@link Kind#DAMAGE_IMPACT_BLUR}, {@link Kind#DAMAGE_IMPACT_LINE}, {@link Kind#STUN_STAR},
     * {@link Kind#FALLING_DUST}, {@link Kind#SHINE_LINES}, {@link Kind#SCENE_DUST},
     * {@link Kind#CONE}, {@link Kind#DAMAGE_HEART}, {@link Kind#SPOOKY_CHEST},
     * {@link Kind#IRONCLAD_VICTORY_FLAME}, {@link Kind#SPOOKIER_CHEST},
     * {@link Kind#CAMPFIRE_SLEEP_COVER}, {@link Kind#DEATH_SCREEN_FLOATY})
     * never call
     * {@code setBlendFunction} at all, so the host draw must not install or restore a blend function
     * for them. {@link Kind#FLASH_ATK_IMG} was the first such kind; the smoke blur, ceiling dust, and
     * nemesis fire are the first ambient members beyond it, {@link Kind#DEBUFF_PARTICLE} is the
     * first ambient bare-{@code Texture} member, {@link Kind#GENERIC_SMOKE}/{@link
     * Kind#EXHAUST_BLUR} are earlier ambient packed-region members, the earlier ambient
     * members {@link Kind#BLOCK_IMPACT_LINE}/{@link Kind#EXHAUST_PILE} (ambient center-packed)
     * plus {@link Kind#UNKNOWN_PARTICLE} (the first ambient bare-{@code Texture} member of the new
     * 128-rect) and {@link Kind#DAMAGE_IMPACT_BLUR}/{@link Kind#DAMAGE_IMPACT_LINE} (ambient
     * center-packed) are not the newest any more; the two next-newest ambient members are
     * {@link Kind#STUN_STAR}/{@link Kind#FALLING_DUST} (ambient center-packed, adding only a position
     * offset and a region-offset origin respectively) and the newer ambient members
     * {@link Kind#SHINE_LINES}, which is ambient center-packed exactly like {@link Kind#STANCE_AURA},
     * and {@link Kind#SCENE_DUST}, which reuses the {@code FALLING_DUST} region-offset origin
     * ambiently. Every
     * other kind — including
     * the two fire
     * bursts ({@link Kind#FIRE_BURST}, {@link Kind#RED_FIRE_BURST}), the additive bare-texture
     * {@link Kind#SHIELD_PARTICLE}, the additive {@link Kind#TORCH_PARTICLE_XL}/{@link
     * Kind#GHOSTLY_WEAK_FIRE}, the two additive bare-texture members
     * {@link Kind#ICE_SHATTER}/{@link Kind#WEB_PARTICLE}, {@link Kind#ENTANGLE}, the additive
     * center-packed members {@link Kind#FLAME_PARTICLE}/{@link Kind#LIGHTNING_ORB_ACTIVATE},
     * the additive bare-texture members {@link Kind#DARK_ORB_PASSIVE}, the additive
     * {@link Kind#WARNING_SIGN}, the newest additive members {@link Kind#LIGHTNING_EFFECT}/
     * {@link Kind#FLAME_BALL}, the two additive center-packed {@link Kind#TORCH_PARTICLE_M}/
     * {@link Kind#TORCH_PARTICLE_S}, and the two newest additive bare-{@code Texture} members
     * {@link Kind#LIGHTNING_ORB_PASSIVE}/{@link Kind#GLOWY_FIRE_EYES}, plus the newest additive
     * center-packed member {@link Kind#FLYING_SPIKE} — is additive. The newest ambient member is
     * {@link Kind#CONE}; the two newest members add only the wait-phase guard capability
     * ({@link #nativeSkipsDrawByGuard}/{@link #guardFieldName}) and reuse the existing shapes —
     * {@link Kind#FALLING_ICE} is additive (shape-C fixed rect) and {@link Kind#DAMAGE_HEART} is
     * ambient (center-packed). The two newest members {@link Kind#SPOOKY_CHEST} and
     * {@link Kind#IRONCLAD_VICTORY_FLAME} are both ambient center-packed and add only the img-path
     * per-instance MIRROR capability ({@link #usesInstanceMirrorX}/{@link #usesInstanceMirrorY}).
     * The three newest members {@link Kind#SPOOKIER_CHEST}, {@link Kind#CAMPFIRE_SLEEP_COVER}, and
     * {@link Kind#DEATH_SCREEN_FLOATY} are likewise ambient center-packed with NO new formula
     * (the first two reuse the mirror, the third does not). The newest member
     * {@link Kind#WRATH_STANCE_CHANGE} is ADDITIVE center-packed (it installs/restores the additive
     * blend natively), so {@code additiveBlend} reports {@code true} for it. The newest (F27) member
     * {@link Kind#WATER_SPLASH} is AMBIENT center-packed (it never calls {@code setBlendFunction}),
     * so {@code additiveBlend} reports {@code false} for it, while the newest {@link Kind#BUFF_PARTICLE}
     * installs/restores the additive blend natively and is therefore ADDITIVE. The newest (F28) member
     * {@link Kind#BOTTOM_FOG} is likewise AMBIENT center-packed (it never calls {@code setBlendFunction})
     * so {@code false} is reported for it, while the newest {@link Kind#GIANT_FIRE} installs/restores the
     * additive blend natively and is ADDITIVE. The newest (F29) member
     * {@link Kind#TORCH_HEAD_FIRE} also installs/restores the additive blend natively and is ADDITIVE.
     * The newest (F30) member {@link Kind#CARD_TRAIL} likewise installs/restores the additive blend
     * natively and is ADDITIVE, so it is not in the ambient set. The newest (NRO-04 B03) member
     * {@link Kind#HEAL_PANEL} also installs/restores the additive blend natively and is ADDITIVE. The
     * newest (NRO-04 B04) member {@link Kind#PING_HP} likewise installs/restores the additive blend
     * natively and is ADDITIVE.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean additiveBlend(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind != Kind.FLASH_ATK_IMG
                && kind != Kind.SMOKE_BLUR
                && kind != Kind.CEILING_DUST
                && kind != Kind.NEMESIS_FIRE
                && kind != Kind.DEBUFF_PARTICLE
                && kind != Kind.GENERIC_SMOKE
                && kind != Kind.EXHAUST_BLUR
                && kind != Kind.BLOCK_IMPACT_LINE
                && kind != Kind.EXHAUST_PILE
                && kind != Kind.UNKNOWN_PARTICLE
                && kind != Kind.DAMAGE_IMPACT_BLUR
                && kind != Kind.DAMAGE_IMPACT_LINE
                && kind != Kind.STUN_STAR
                && kind != Kind.FALLING_DUST
                && kind != Kind.SHINE_LINES
                && kind != Kind.SCENE_DUST
                && kind != Kind.CONE
                && kind != Kind.DAMAGE_HEART
                && kind != Kind.SPOOKY_CHEST
                && kind != Kind.IRONCLAD_VICTORY_FLAME
                && kind != Kind.SPOOKIER_CHEST
                && kind != Kind.CAMPFIRE_SLEEP_COVER
                && kind != Kind.DEATH_SCREEN_FLOATY
                && kind != Kind.WATER_SPLASH
                && kind != Kind.BOTTOM_FOG;
    }

    /**
     * Pure per-kind predicate: {@code true} only for the kinds whose native {@code render} can
     * legitimately produce no pixels at all because it guards the draw on a present image. Today
     * that is exactly {@link Kind#FLASH_ATK_IMG}, whose native {@code FlashAtkImgEffect.render}
     * wraps its {@code sb.draw} in {@code if (img != null)} — so an instance with a null
     * {@code img} draws nothing natively, and an ART fail-open on that instance loses no pixels.
     * Every other claimable kind draws unconditionally (or draws a fixed static texture), so it is
     * {@code false}. Callers use this to classify a declined claim as a benign no-pixel decline
     * rather than a {@code dispositionMismatch}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean nativeSkipsDrawWithoutImage(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLASH_ATK_IMG;
    }

    /**
     * Pure per-kind predicate for the seam's NATIVE DRAW GUARD capability: {@code true} only for the
     * kinds whose native {@code render} wraps its draw in a wait-phase field check, so a claimed
     * instance whose guard blocks would natively draw NOTHING. Today that is exactly
     * {@link Kind#FALLING_ICE} ({@code FallingIceEffect}, guarded by {@code if (waitTimer < 0f)}),
     * {@link Kind#DAMAGE_HEART} ({@code DamageHeartEffect}, guarded by {@code if (delayTimer < 0f)}),
     * and {@link Kind#WRATH_STANCE_CHANGE} ({@code WrathStanceChangeParticle}, guarded by
     * {@code if (delayTimer > 0f) return} — i.e. it draws only when {@code delayTimer <= 0f}), and the
     * newest (B01) {@link Kind#FLYING_ORB} ({@code FlyingOrbEffect}, guarded by {@code if (isDone)
     * return} — a BOOLEAN guard, see {@link #guardIsBoolean}/{@link #guardBlocksBoolean}). Every
     * other claimable kind draws unconditionally (or draws a fixed static texture), so it is
     * {@code false}. Callers use this together with {@link #guardFieldName} and the per-kind
     * {@link #guardBlocks} threshold to keep a claimed instance in pixel parity — the renderer
     * declines (draws nothing) when the guard blocks, matching the native wait phase — and to
     * classify that decline as a benign no-pixel decline rather than a {@code dispositionMismatch}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean nativeSkipsDrawByGuard(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FALLING_ICE || kind == Kind.DAMAGE_HEART
                || kind == Kind.WRATH_STANCE_CHANGE || kind == Kind.FLYING_ORB;
    }

    /**
     * Pure per-kind predicate for a BOOLEAN native draw guard: {@code true} only for the kinds whose
     * native {@code render} guards its draw on a boolean field rather than a wait-phase float
     * threshold. Today that is exactly the newest (B01) {@link Kind#FLYING_ORB}
     * ({@code FlyingOrbEffect}, whose native {@code render} begins {@code if (isDone) return}). For
     * such a kind the renderer reads the raw {@code isDone} field and declines (draws nothing) when
     * it is {@link Boolean#TRUE} — matching native — via {@link #guardBlocksBoolean}. Every other
     * claimable kind is {@code false} here (their guard, if any, is the float {@link #guardBlocks}
     * threshold).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean guardIsBoolean(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLYING_ORB;
    }

    /**
     * Pure per-kind BOOLEAN guard THRESHOLD: {@code true} when a present boolean guard field value
     * blocks the native draw for {@code kind}, {@code false} otherwise. Today that is exactly the
     * newest (B01) {@link Kind#FLYING_ORB}, whose native {@code render} returns immediately when
     * {@code isDone} is true; so it blocks iff {@code value} is {@code true} and draws when it is
     * {@code false}. Every other kind is {@code false} (no boolean guard). This is the boolean
     * counterpart of the float {@link #guardBlocks}, and the renderer resolves the raw field
     * reflectively so this class stays host-neutral.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean guardBlocksBoolean(Kind kind, boolean value) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLYING_ORB && value;
    }

    /**
     * Pure per-kind guard THRESHOLD: {@code true} when a present guard field value blocks the native
     * draw for {@code kind}, {@code false} otherwise. This is the seam's generalization of the F21
     * "blocked iff field {@code >= 0f}" rule so kinds with different wait-phase tests share one
     * code path:
     *
     * <ul>
     *   <li>{@link Kind#FALLING_ICE} ({@code if (waitTimer < 0f)}) and {@link Kind#DAMAGE_HEART}
     *       ({@code if (delayTimer < 0f)}) block whenever {@code value < 0f} is false — i.e. for
     *       {@code +0f}, positive values, {@code +Inf}, AND {@code NaN}; unblocked only for negative
     *       finite values and {@code -Inf} (matching native, where {@code NaN < 0f} is false so the
     *       draw is skipped);</li>
     *   <li>{@link Kind#WRATH_STANCE_CHANGE} ({@code if (delayTimer > 0f) return}) blocks when
     *       {@code value > 0f} — i.e. it draws at {@code 0f} and at {@code NaN} (native
     *       {@code NaN > 0f} is false too), matching the native non-return path;</li>
     *   <li>every other kind has no guard and is always {@code false}.</li>
     * </ul>
     *
     * The renderer resolves the guard field reflectively (an absent/unreadable field is NOT blocked)
     * and delegates the threshold decision here, so this class stays host-neutral and never touches
     * the native class.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean guardBlocks(Kind kind, float value) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (kind == Kind.WRATH_STANCE_CHANGE) return value > 0f;
        if (kind == Kind.FALLING_ICE || kind == Kind.DAMAGE_HEART) return !(value < 0f);
        return false;
    }

    /**
     * The ordered RNG ranges a claimed draw of {@code kind} must consume from the global
     * {@code MathUtils.random(min, max)} stream so the native RNG sequence stays identical. Each
     * element is a {@code float[]{min, max}} pair, in native call order; the renderer MUST call
     * {@code MathUtils.random(min, max)} once for each range IN ORDER during the draw (matching the
     * native call sequence exactly) and multiply the params' corresponding scale component by the
     * returned value, so pixel equivalence is an identity rather than a probabilistic match.
     *
     * <p>Today only {@link Kind#WRATH_STANCE_CHANGE} consumes RNG: its native {@code render} computes
     * {@code scale * MathUtils.random(2.9f, 3.1f)} for scaleX then {@code scale * MathUtils.random(
     * 0.95f, 1.05f)} for scaleY, so this returns {@code [(2.9f, 3.1f), (0.95f, 1.05f)]} in that
     * order. Every other claimable kind's native {@code update}/{@code render} consumes no RNG during
     * the draw, so it returns an empty list. The returned list is immutable.
     *
     * <p>{@code float[]} is host-neutral primitives, so this class stays GL/host-free.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static java.util.List<float[]> randomRanges(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (kind == Kind.WRATH_STANCE_CHANGE) {
            return WRATH_STANCE_CHANGE_RANDOM_RANGES;
        }
        return java.util.Collections.emptyList();
    }

    /**
     * The ordered per-PASS native RNG ranges for a MULTI-DRAW kind, or an empty outer list for every
     * kind whose native {@code render} issues a single (or zero) draw. Each element is one draw pass;
     * each inner list is that pass's ordered {@code float[]{min, max}} scale ranges. The renderer
     * MUST, per pass IN ORDER, call {@code MathUtils.random(min, max)} once for each inner range IN
     * ORDER and multiply the returned value into the pass's scaleX (first inner range) then scaleY
     * (second inner range), then replay the pass's draw with those scales and the {@code params}
     * position/origin/size/rotation/src — so the global RNG stream and the pixels stay identical to
     * the native render. A kind with an empty outer list keeps the single-draw path (and, if it is
     * {@link Kind#WRATH_STANCE_CHANGE}, the existing single-draw {@link #randomRanges}).
     *
     * <p>Today only {@link Kind#STANCE_CHANGE_ABSORPTION} is a multi-draw kind: its native
     * {@code render} draws the static {@code ImageMaster.WOBBLY_ORB_VFX} {@code Texture} twice, pass 0
     * with {@code scale * MathUtils.random(0.5f, 2.0f)} for scaleX then {@code scale *
     * MathUtils.random(0.5f, 2.0f)} for scaleY, and pass 1 with {@code scale *
     * MathUtils.random(0.6f, 2.5f)} for each, so this returns
     * {@code [[(0.5f,2.0f),(0.5f,2.0f)], [(0.6f,2.5f),(0.6f,2.5f)]]}. Every other claimable kind's
     * native {@code update}/{@code render} issues at most one RNG-consuming draw during the render, so
     * it returns an empty outer list (and uses {@link #randomRanges} when it is WRATH). The returned
     * list (and each inner list) is immutable.
     *
     * <p>{@code float[]} is host-neutral primitives, so this class stays GL/host-free.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static java.util.List<java.util.List<float[]>> drawPassRandomRanges(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (kind == Kind.STANCE_CHANGE_ABSORPTION) {
            return STANCE_CHANGE_ABSORPTION_DRAW_PASS_RANDOM_RANGES;
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Pure per-kind predicate for the seam's PLAYER-HITBOX-RELATIVE X rule: {@code true} only for the
     * kinds whose native {@code render} draws at the player's hitbox center X plus the effect's own
     * {@code x} (rather than the effect's {@code x} alone). Today that is exactly
     * {@link Kind#WRATH_STANCE_CHANGE}, whose native draw x argument is
     * {@code AbstractDungeon.player.hb.cX + this.x}. Every other kind draws at its own {@code x}, so
     * it is {@code false}. When {@code true} the renderer resolves the player hitbox center X and
     * adds the effect's {@code x}; if the player or its hitbox is absent/unreadable the renderer FAILS
     * OPEN (draws nothing, so the native render produces the pixels) rather than drawing at a wrong
     * position.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean playerHitboxRelativeX(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.WRATH_STANCE_CHANGE;
    }

    /**
     * The host-neutral name of the native wait-phase guard field for a guarded kind, or {@code null}
     * for every kind {@link #nativeSkipsDrawByGuard} reports {@code false} for. The name is a plain
     * {@link String} only — this class stays host-neutral and never touches the native class. The
     * guard is SATISFIED (the native draw runs) when the field's value passes the per-kind
     * {@link #guardBlocks} threshold; a guard kind
     * whose field is absent or unreadable is treated as SATISFIED (it draws), since the native
     * render would then not be able to read a guard either. Returns {@code "waitTimer"} for
     * {@link Kind#FALLING_ICE}, {@code "delayTimer"} for {@link Kind#DAMAGE_HEART}, and
     * {@code "delayTimer"} for {@link Kind#WRATH_STANCE_CHANGE}, and {@code "isDone"} for the newest
     * (B01) {@link Kind#FLYING_ORB} (the seam's first BOOLEAN guard field; see
     * {@link #guardIsBoolean}).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static String guardFieldName(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (kind == Kind.FALLING_ICE) return FALLING_ICE_GUARD_FIELD;
        if (kind == Kind.DAMAGE_HEART) return DAMAGE_HEART_GUARD_FIELD;
        if (kind == Kind.WRATH_STANCE_CHANGE) return WRATH_STANCE_CHANGE_GUARD_FIELD;
        if (kind == Kind.FLYING_ORB) return FLYING_ORB_GUARD_FIELD;
        return null;
    }

    /**
     * Pure per-kind predicate for the seam's VARIABLE-LENGTH MULTI-DRAW capability: {@code true} only
     * for a kind whose native {@code render} issues a draw whose COUNT (and, per draw, its position)
     * is read from a HOST-SUPPLIED array rather than a fixed pass list. Today that is exactly the
     * newest (NRO-04 B01) {@link Kind#FLYING_ORB} ({@code FlyingOrbEffect}), whose native
     * {@code render} iterates its own {@code Vector2[] points} from {@code points.length - 1} down to
     * {@code 1} and draws one center-packed sprite per NON-NULL element (index {@code 0} is never
     * drawn). The renderer reads that array, so the number of draws is data-dependent; every other
     * claimable kind issues a fixed, compile-time draw count (including the F25 two-pass
     * {@link Kind#STANCE_CHANGE_ABSORPTION}, which is a FIXED-length multi-draw kind, not a
     * variable-length one).
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean variableLengthMultiDraw(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLYING_ORB;
    }

    /**
     * Native {@link Kind#FLYING_ORB} uniform-scale START multiplier: the first drawn point's uniform
     * scale is {@code Settings.scale * }{@code this value} (the native bytecode computes
     * {@code float s = Settings.scale * 1.5f;} once before the loop). It is a named constant so the
     * renderer never hardcodes the magic {@code 1.5f}.
     */
    public static float flyingOrbStartScaleMultiplier() {
        return FLYING_ORB_START_SCALE_MULTIPLIER;
    }

    /**
     * Native {@link Kind#FLYING_ORB} per-drawn-point uniform-scale DECAY factor: after each DRAWN
     * point the native loop multiplies its uniform scale by {@code 0.975f}
     * ({@code s *= 0.975f;}), so the sequence of drawn scales is
     * {@code scale0, scale0*0.975, scale0*0.975^2, ...} in draw order. It is a named constant so the
     * renderer never hardcodes the magic {@code 0.975f}.
     */
    public static float flyingOrbScaleDecayPerDraw() {
        return FLYING_ORB_SCALE_DECAY_PER_DRAW;
    }

    /**
     * Pure per-kind predicate for the ANISOTROPIC img-path scale capability: {@code true} only for
     * the newest (NRO-04 B02) {@link Kind#FLICK_COIN}, whose native {@code render} draws with
     * {@code scaleX = scale * }{@link #FLICK_COIN_SCALE_X} ({@code 0.7f}) and
     * {@code scaleY = scale * }{@link #FLICK_COIN_SCALE_Y} ({@code 0.4f}) — BOTH axes are scaled, so
     * unlike the F27 single-axis {@code WATER_SPLASH} rule it cannot ride the shared
     * {@link #params} {@code scaleYMultiplier} tail (which leaves {@code scaleX} alone). Every other
     * kind is {@code false}. The per-axis multipliers themselves are applied by {@link #params}
     * through the {@link #FLICK_COIN_SCALE_X}/{@link #FLICK_COIN_SCALE_Y} constants (not by this
     * predicate); the {@link #flickCoinScaleXMultiplier()}/{@link #flickCoinScaleYMultiplier()}
     * accessors simply expose those same constants to consumers/tests.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean flickCoinUsesAnisotropicScale(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLICK_COIN;
    }

    /**
     * Native {@link Kind#FLICK_COIN} X-axis draw scale multiplier ({@code 0.7f}). It exposes the
     * {@link #FLICK_COIN_SCALE_X} constant to consumers/tests; the render branch itself applies the
     * constant inside {@link #params}.
     */
    public static float flickCoinScaleXMultiplier() {
        return FLICK_COIN_SCALE_X;
    }

    /**
     * Native {@link Kind#FLICK_COIN} Y-axis draw scale multiplier ({@code 0.4f}). It exposes the
     * {@link #FLICK_COIN_SCALE_Y} constant to consumers/tests; the render branch itself applies the
     * constant inside {@link #params}.
     */
    public static float flickCoinScaleYMultiplier() {
        return FLICK_COIN_SCALE_Y;
    }

    /**
     * Pure per-kind color rule for the bare-{@code Texture} shape: {@code true} only for
     * {@link Kind#WEB_PARTICLE} and {@link Kind#ENTANGLE}, whose native {@code render} does not pass
     * the effect's own {@code color} to {@code setColor} but instead builds
     * {@code new Color(1f, 1f, 1f, color.a)} — i.e. it forces the RGB channels to white and takes
     * only the alpha from the effect's color ({@code EntangleEffect} is byte-identical to
     * {@code WebParticleEffect}). Every other kind (including the other bare-{@code Texture} members
     * {@link Kind#CALM_PARTICLE}, {@link Kind#SHIELD_PARTICLE}, {@link Kind#DEBUFF_PARTICLE},
     * {@link Kind#ICE_SHATTER}, {@link Kind#UNKNOWN_PARTICLE}, {@link Kind#WARNING_SIGN}, and
     * {@link Kind#DARK_ORB_PASSIVE})
     * sets the effect's {@code color}
     * unchanged, so the host draw must not rewrite its RGB.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean whiteAlphaOnly(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.WEB_PARTICLE || kind == Kind.ENTANGLE;
    }

    /**
     * Pure per-kind predicate for the new per-instance horizontal-flip capability of the
     * bare-{@code Texture} shape-C path: {@code true} only for the kinds whose native {@code render}
     * passes the effect's own horizontal flip boolean to the raw-texture draw overload. Today that is
     * exactly {@link Kind#LIGHTNING_ORB_PASSIVE} (its {@code flipX} field) and
     * {@link Kind#GLOWY_FIRE_EYES} (its {@code flippedX} field). Every other kind — including every
     * other bare-{@code Texture} member ({@link Kind#CALM_PARTICLE}, {@link Kind#SHIELD_PARTICLE},
     * {@link Kind#DEBUFF_PARTICLE}, {@link Kind#ICE_SHATTER}, {@link Kind#WEB_PARTICLE},
     * {@link Kind#ENTANGLE}, {@link Kind#UNKNOWN_PARTICLE}, {@link Kind#WARNING_SIGN},
     * {@link Kind#DARK_ORB_PASSIVE}) — hardcodes {@code false}, so the host draw must not read a
     * flip field for it.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean usesInstanceFlipX(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.LIGHTNING_ORB_PASSIVE || kind == Kind.GLOWY_FIRE_EYES;
    }

    /**
     * Pure per-kind predicate for the new per-instance vertical-flip capability of the
     * bare-{@code Texture} shape-C path: {@code true} only for the kinds whose native {@code render}
     * passes the effect's own vertical flip boolean to the raw-texture draw overload. Today that is
     * exactly {@link Kind#LIGHTNING_ORB_PASSIVE} (its {@code flipY} field);
     * {@link Kind#GLOWY_FIRE_EYES} uses only its horizontal {@code flippedX} field and hardcodes the
     * vertical flip to {@code false}, so it is {@code false} here. Every other kind hardcodes
     * {@code false} for both flips.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean usesInstanceFlipY(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.LIGHTNING_ORB_PASSIVE;
    }

    /**
     * Pure per-kind predicate for the shape-C kinds whose per-instance horizontal flip comes from a
     * {@code flippedX} field rather than the boolean {@code flipX}/{@code flipY} pair passed to the
     * raw-texture draw overload: {@code true} only for {@link Kind#GLOWY_FIRE_EYES} and the newest
     * (F29) {@link Kind#TORCH_HEAD_FIRE}. Both kinds pass their own {@code flippedX} field with a
     * hardcoded {@code false} vertical flip, so the renderer resolves the horizontal flip from that
     * field name (generalizing the previous {@code GLOWY_FIRE_EYES}-only special case). Every other
     * kind is {@code false} here.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean usesTexturedFlipX(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.GLOWY_FIRE_EYES || kind == Kind.TORCH_HEAD_FIRE;
    }

    /**
     * Pure per-kind predicate for the img-path per-instance HORIZONTAL MIRROR capability: {@code true}
     * only for the kinds whose native {@code render} mirrors the drawn sprite horizontally when the
     * effect's own {@code flipX} field is set. Native does this by calling {@code img.flip(...)} in
     * place around its draw; the claim suppresses that draw, so the host draw instead swaps the
     * canonical region's {@code u}/{@code u2} (a UV swap is visually identical to a center-origin
     * region flip). Today that is exactly {@link Kind#FLAME_PARTICLE} (its {@code flipX} field),
     * {@link Kind#SPOOKY_CHEST} (its {@code flipX} field),
     * {@link Kind#IRONCLAD_VICTORY_FLAME} (its {@code flipX} field), {@link Kind#SPOOKIER_CHEST}
     * (its {@code flipX} field), {@link Kind#CAMPFIRE_SLEEP_COVER} (its {@code flipX} field),
     * {@link Kind#BOTTOM_FOG} (its {@code flipX} field), and {@link Kind#GIANT_FIRE} (its
     * {@code flipX} field).
     * Every other img-path kind — and every bare-{@code Texture} kind
     * — hardcodes no such mirror, so the host draw must not read a mirror field for it. These flags
     * are distinct from the shape-C {@link #usesInstanceFlipX}/{@link #usesInstanceFlipY} flags.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean usesInstanceMirrorX(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.FLAME_PARTICLE
                || kind == Kind.SPOOKY_CHEST
                || kind == Kind.IRONCLAD_VICTORY_FLAME
                || kind == Kind.SPOOKIER_CHEST
                || kind == Kind.CAMPFIRE_SLEEP_COVER
                || kind == Kind.BOTTOM_FOG
                || kind == Kind.GIANT_FIRE;
    }

    /**
     * Pure per-kind predicate for the img-path per-instance VERTICAL MIRROR capability: {@code true}
     * only for the kinds whose native {@code render} mirrors the drawn sprite vertically when the
     * effect's own {@code flipY} field is set. Native does this by calling {@code img.flip(true, ...)}
     * in place around its draw; the claim suppresses that draw, so the host draw instead swaps the
     * canonical region's {@code v}/{@code v2}. Today that is exactly {@link Kind#SPOOKY_CHEST} (its
     * {@code flipY} field), {@link Kind#SPOOKIER_CHEST} (its {@code flipY} field), and
     * {@link Kind#CAMPFIRE_SLEEP_COVER} (its {@code flipY} field), and the newest (F28)
     * {@link Kind#BOTTOM_FOG} (its {@code flipY} field); {@link Kind#FLAME_PARTICLE},
     * {@link Kind#IRONCLAD_VICTORY_FLAME}, {@link Kind#GIANT_FIRE}, and
     * {@link Kind#DEATH_SCREEN_FLOATY} declare no
     * {@code flipY} field, so they are {@code false} here. Every other kind is {@code false}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static boolean usesInstanceMirrorY(Kind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.SPOOKY_CHEST
                || kind == Kind.SPOOKIER_CHEST
                || kind == Kind.CAMPFIRE_SLEEP_COVER
                || kind == Kind.BOTTOM_FOG;
    }

    /**
     * Pure per-kind uniform-scale multiplier for the shared center-packed branch: the factor the
     * renderer passes as the F28 uniform-scale tail (composed with the F27 {@code scaleYMultiplier}).
     * Returns the caller-supplied {@code settingsScale} for {@link Kind#GIANT_FIRE}, whose native
     * {@code render} scales BOTH axes by {@code Settings.scale} ({@code sb.draw(img, x, y, pw/2f,
     * ph/2f, pw, ph, scale * Settings.scale, scale * Settings.scale, rotation)}), and {@code 1f} for
     * every other kind (so their results are byte-identical to before this rule existed). The
     * {@code settingsScale} value is supplied by the caller, so this class stays host-neutral and
     * never reads {@code Settings}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static float uniformScaleMultiplier(Kind kind, float settingsScale) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        return kind == Kind.GIANT_FIRE ? settingsScale : 1f;
    }

    /**
     * Pure geometry for one claim. The caller supplies the effect field floats and the packed
     * region size; the per-kind color/blend state is applied by the host draw (see
     * {@link #additiveBlend}: additive kinds install/restore {@code 770/1}-&rarr;{@code 770/771},
     * while the ambient kinds ({@code FLASH_ATK_IMG}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code DEBUFF_PARTICLE}, {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR},
     * {@code BLOCK_IMPACT_LINE}, {@code EXHAUST_PILE}, {@code UNKNOWN_PARTICLE},
     * {@code DAMAGE_IMPACT_BLUR}, {@code DAMAGE_IMPACT_LINE}, {@code STUN_STAR},
     * {@code FALLING_DUST}, {@code SHINE_LINES}, {@code SCENE_DUST}, {@code CONE},
     * {@code DAMAGE_HEART}, {@code SPOOKY_CHEST}, {@code IRONCLAD_VICTORY_FLAME},
     * {@code SPOOKIER_CHEST}, {@code CAMPFIRE_SLEEP_COVER}, {@code DEATH_SCREEN_FLOATY},
     * {@code WATER_SPLASH}, {@code BOTTOM_FOG})
     * leave the ambient blend untouched and restore
     * only color; see {@link #whiteAlphaOnly} for the two kinds ({@code WEB_PARTICLE} and
     * {@code ENTANGLE}) that also rewrite their set color's
     * RGB to white).
     *
     * <p>The three trailing scalars were added for the two newest kinds and default to {@code 0} for
     * every other caller: {@code vX} is the effect's own horizontal velocity used only by
     * {@code STUN_STAR} (whose draw position is shifted by it), while {@code regionOffsetX}/
     * {@code regionOffsetY} are the region's own trim offsets used as the draw origin only by
     * {@code FALLING_DUST} and {@code SCENE_DUST} (whose origins are NOT {@code packedWidth/2},
     * {@code packedHeight/2}). The
     * two newest tail scalars {@code originOffsetX}/{@code originOffsetY} are a fixed offset applied
     * to the shared center-packed origin ({@code originX = packedWidth/2f + originOffsetX},
     * {@code originY = packedHeight/2f + originOffsetY}); every pre-existing kind passes {@code 0f}
     * for both and gets the unchanged {@code packedWidth/2f}, {@code packedHeight/2f} origin, and
     * {@code LIGHTNING_EFFECT} passes {@code (0f, -packedHeight/2f)} so its origin Y becomes exactly
     * {@code 0f} (scale-independent, as its native offset has no {@code Settings.scale} factor).
     * {@code FLAME_BALL} is NOT expressed with this tail scalar: its native origin Y is
     * {@code packedHeight/2f + 20f * settingsScale}, so it has its own pure branch that consumes the
     * {@code settingsScale} argument the caller already supplies (keeping this class host-neutral).
     *
     * <p>The newest tail scalar {@code scaleYMultiplier} is the anisotropic scale factor of the
     * center-packed branch: {@code scaleY = scale * scaleYMultiplier} while {@code scaleX stays
     * scale}. Every pre-existing kind passes {@code 1f} (so its results are byte-identical to before
     * the parameter existed) and only {@code WATER_SPLASH} passes
     * {@link #WATER_SPLASH_SCALE_Y_MULTIPLIER} ({@code 0.54f}); {@code BUFF_PARTICLE} has its own
     * branch and ignores it. The newest (F28) pure rule composes a UNIFORM multiplier into that same
     * branch: both axes are multiplied by {@link #uniformScaleMultiplier}{@code (kind, settingsScale)}
     * ({@code settingsScale} for {@code GIANT_FIRE} — whose native draw scales both axes by
     * {@code Settings.scale} — and {@code 1f} for every other kind, so all pre-existing results stay
     * byte-identical), with {@code scaleX = scale * uniform} and
     * {@code scaleY = scale * uniform * scaleYMultiplier}.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static Params params(Kind kind, float x, float y, float vY, float scale, float rotation,
            float durDiv2, float duration, float settingsScale,
            float packedWidth, float packedHeight,
            float vX, float regionOffsetX, float regionOffsetY,
            float originOffsetX, float originOffsetY, float scaleYMultiplier) {
        return params(kind, x, y, vY, scale, rotation, durDiv2, duration, settingsScale,
                packedWidth, packedHeight, vX, regionOffsetX, regionOffsetY,
                originOffsetX, originOffsetY, scaleYMultiplier, 0f);
    }

    /**
     * Full geometry overload carrying the caller-supplied {@code settingsHeight} as well as the
     * scale. {@link Kind#HEAL_PANEL} and the newest (NRO-04 B04) {@link Kind#PING_HP} consume the
     * settings height (their native draw Y is {@code Settings.HEIGHT - ...}); every other kind ignores
     * it, so the overload with the trailing {@code 0f} is byte-identical to it for them. This class
     * stays host-neutral and never reads {@code Settings} itself.
     *
     * @throws IllegalArgumentException when {@code kind} is null
     */
    public static Params params(Kind kind, float x, float y, float vY, float scale, float rotation,
            float durDiv2, float duration, float settingsScale,
            float packedWidth, float packedHeight,
            float vX, float regionOffsetX, float regionOffsetY,
            float originOffsetX, float originOffsetY, float scaleYMultiplier,
            float settingsHeight) {
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        float originX = packedWidth / 2f + originOffsetX;
        float originY = packedHeight / 2f + originOffsetY;
        switch (kind) {
            case STANCE_AURA:
            case DIVINITY_STANCE_CHANGE:
            case LIGHT_FLARE:
            case FLASH_ATK_IMG:
            case LIGHT_FLARE_M:
            case LIGHT_FLARE_L:
            case TORCH_PARTICLE_L:
            case FIRE_BURST:
            case RED_FIRE_BURST:
            case SMOKE_BLUR:
            case CEILING_DUST:
            case NEMESIS_FIRE:
            case TORCH_PARTICLE_XL:
            case GHOSTLY_WEAK_FIRE:
            case GENERIC_SMOKE:
            case EXHAUST_BLUR:
            case BLOCK_IMPACT_LINE:
            case EXHAUST_PILE:
            case FLAME_PARTICLE:
            case LIGHTNING_ORB_ACTIVATE:
            case DAMAGE_IMPACT_BLUR:
            case DAMAGE_IMPACT_LINE:
            case LIGHTNING_EFFECT:
            case SHINE_LINES:
            case TORCH_PARTICLE_M:
            case TORCH_PARTICLE_S:
            case FLYING_SPIKE:
            case DAMAGE_HEART:
            case SPOOKY_CHEST:
            case IRONCLAD_VICTORY_FLAME:
            case SPOOKIER_CHEST:
            case CAMPFIRE_SLEEP_COVER:
            case DEATH_SCREEN_FLOATY:
            case WRATH_STANCE_CHANGE:
            case WATER_SPLASH:
            case BOTTOM_FOG:
            case GIANT_FIRE:
                // DivinityStanceChangeParticle, the cross-family LightFlareSEffect/MEffect/LEffect,
                // TorchParticleLEffect, the vfx-combat FlashAtkImgEffect, the two fire bursts, the
                // smoke blur, the ceiling dust, the nemesis fire, TorchParticleXLEffect,
                // GhostlyWeakFireEffect, GenericSmokeEffect, and ExhaustBlurEffect mirror
                // StanceAuraEffect exactly: x/y passthrough (no vY is consumed; every member that
                // owns a vY field — TorchParticleLEffect and all nine newer members — uses it only
                // in update()), center origin, packed size, uniform scale. Flash, the smoke blur,
                // the ceiling dust, the nemesis fire, the generic smoke, and the exhaust blur differ
                // only in blend (ambient, via additiveBlend == false). The five newest members
                // mirror the same geometry: FlameParticleEffect and LightningOrbActivateEffect are
                // additive while DamageImpactBlurEffect and DamageImpactLineEffect never call
                // setBlendFunction (ambient, again only additiveBlend differs). The two newest img
                // members also reuse this branch: LightningEffect (offsetY -ph/2f, so originY == 0)
                // is additive and ShineLinesEffect is ambient center-packed (offset 0/0, like
                // StanceAuraEffect). The two newest scene-world members TorchParticleMEffect and
                // TorchParticleSEffect reuse this branch UNCHANGED (additive, origin packed/2, no new
                // rule; their vY is update-only). WrathStanceChangeParticle (F24) also joins this
                // branch: it is additive center-packed but draws at player hitbox center X + x and
                // multiplies scaleX/scaleY by two MathUtils.random draws; those two extra rules are
                // resolved by the renderer (see playerHitboxRelativeX/randomRanges), not here.
                // WaterSplashParticleEffect (F27) also joins this branch: it is AMBIENT
                // center-packed (no setBlendFunction) with an ANISOTROPIC scale — its native draw
                // scaleY is scale * 0.54f while scaleX is scale, supplied by the caller through the
                // scaleYMultiplier tail scalar (the renderer passes 0.54f for WATER_SPLASH and 1f for
                // every other kind, so their results are unchanged). BottomFogEffect (F28) reuses this
                // branch UNCHANGED (ambient center-packed, NO new rule; the F22 mirror flags are
                // resolved by the renderer). GiantFireEffect (F28) also joins this branch: it is
                // ADDITIVE center-packed with a NEW pure uniform-scale rule — BOTH axes are
                // scale * Settings.scale — expressed through uniformScaleMultiplier(kind, settingsScale)
                // (settingsScale for GIANT_FIRE, 1f for every other kind), which is composed with the
                // scaleYMultiplier tail (1f for GIANT_FIRE, so both axes are scale*settingsScale).
                {
                    float uniform = uniformScaleMultiplier(kind, settingsScale);
                    return new Params(x, y, originX, originY, packedWidth, packedHeight,
                            scale * uniform, scale * uniform * scaleYMultiplier, rotation);
                }
            case FALLING_ICE:
                // Native FallingIceEffect ignores the (absent) region: a new additive shape-C fixed
                // rect (origin 48, size 96, src 0,0,96,96; x/y passthrough) over its own instance
                // Texture img, consuming the field rotation. Guarded natively by if (waitTimer < 0f);
                // the guard is
                // resolved by the renderer (see nativeSkipsDrawByGuard/guardFieldName), not here.
                // packedWidth/packedHeight, vY, vX, the region offsets, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x, y, FALLING_ICE_ORIGIN, FALLING_ICE_ORIGIN,
                        FALLING_ICE_SIZE, FALLING_ICE_SIZE, scale, scale, rotation);
            case FLAME_BALL:
                // Native FlameBallParticleEffect lifts only its origin Y by a settings-scaled amount:
                // sb.draw(img, x, y, pw/2f, ph/2f + 20f * Settings.scale, pw, ph, scale, scale,
                //         rotation). Its vY is update-only (never consumed). The 20f * settingsScale
                // term is supplied by the caller (host-neutral: this class never reads Settings).
                return new Params(x, y, originX,
                        packedHeight / 2f + FLAME_BALL_ORIGIN_Y_OFFSET * settingsScale,
                        packedWidth, packedHeight, scale, scale, rotation);
            case CONE:
                // Native ConeEffect: setColor(color); sb.draw(img, x, y, 0f, ph/2f, pw, ph,
                // scale*1.1f, scale*1.1f, rotation) with NO setBlendFunction. The origin X is 0f
                // (NOT the shared packedWidth/2f center) while origin Y stays packedHeight/2f, and
                // the uniform scale is the effect's own scale multiplied by the hardcoded 1.1f.
                return new Params(x, y, 0f, packedHeight / 2f, packedWidth, packedHeight,
                        scale * CONE_SCALE_MULTIPLIER, scale * CONE_SCALE_MULTIPLIER, rotation);
            case DIVINITY_PARTICLE:
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case WRATH_PARTICLE: {
                float scaleX = scale * 0.8f;
                float scaleY = (0.1f + ((durDiv2 * 2f - duration) * 2f * scale)) * settingsScale;
                return new Params(x, y + vY, originX, originY, packedWidth, packedHeight,
                        scaleX, scaleY, rotation);
            }
            case CALM_PARTICLE: {
                // Native Calm ignores the (absent) region: fixed origin/size and scaleY formula;
                // packedWidth/packedHeight and vY are unused.
                float scaleY = scale + (durDiv2 * 0.4f - duration) * settingsScale;
                return new Params(x, y, CALM_ORIGIN_X, CALM_ORIGIN_Y, CALM_WIDTH, CALM_HEIGHT,
                        scale, scaleY, rotation);
            }
            case SHIELD_PARTICLE:
                // Native ShieldParticleEffect ignores the (absent) region: fixed origin/size and a
                // hardcoded zero rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x - SHIELD_ORIGIN_X, y - SHIELD_ORIGIN_Y,
                        SHIELD_ORIGIN_X, SHIELD_ORIGIN_Y, SHIELD_WIDTH, SHIELD_HEIGHT,
                        scale, scale, 0f);
            case DEBUFF_PARTICLE:
                // Native DebuffParticleEffect ignores the (absent) region: fixed origin/size and the
                // field rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x - DEBUFF_ORIGIN_X, y - DEBUFF_ORIGIN_Y,
                        DEBUFF_ORIGIN_X, DEBUFF_ORIGIN_Y, DEBUFF_WIDTH, DEBUFF_HEIGHT,
                        scale, scale, rotation);
            case ICE_SHATTER:
                // Native IceShatterEffect ignores the (absent) region: fixed origin/size and the
                // field rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x, y, ICE_SHATTER_ORIGIN_X, ICE_SHATTER_ORIGIN_Y,
                        ICE_SHATTER_WIDTH, ICE_SHATTER_HEIGHT, scale, scale, rotation);
            case WEB_PARTICLE:
                // Native WebParticleEffect ignores the (absent) region: fixed origin/size and a
                // hardcoded zero rotation; packedWidth/packedHeight, vY, dur_div2, duration, and
                // Settings.scale are unused.
                return new Params(x, y, WEB_ORIGIN_X, WEB_ORIGIN_Y,
                        WEB_WIDTH, WEB_HEIGHT, scale, scale, 0f);
            case ENTANGLE:
                // Native EntangleEffect is byte-identical to WebParticleEffect: the static
                // ImageMaster.WEB_VFX Texture, fixed origin/size, and a hardcoded zero rotation
                // (EntangleEffect has no rotation field); packedWidth/packedHeight, vY, dur_div2,
                // duration, and Settings.scale are unused. Reuses the WEB constants so the two kinds
                // share one static-texture/white-alpha configuration.
                return new Params(x, y, WEB_ORIGIN_X, WEB_ORIGIN_Y,
                        WEB_WIDTH, WEB_HEIGHT, scale, scale, 0f);
            case UNKNOWN_PARTICLE:
                // Native UnknownParticleEffect ignores the (absent) region: a new fixed
                // offset/origin/size rect and the field rotation; packedWidth/packedHeight, vY,
                // dur_div2, duration, and Settings.scale are unused.
                return new Params(x - UNKNOWN_OFFSET, y - UNKNOWN_OFFSET,
                        UNKNOWN_ORIGIN, UNKNOWN_ORIGIN, UNKNOWN_SIZE, UNKNOWN_SIZE,
                        scale, scale, rotation);
            case DARK_ORB_PASSIVE:
                // Native DarkOrbPassiveEffect ignores the (absent) region: a new fixed
                // offset/origin/size rect and the field rotation; packedWidth/packedHeight, vY,
                // dur_div2, duration, and Settings.scale are unused. Its src rect is (0, 0, 74, 74),
                // the full 74x74 region the effect passes natively.
                return new Params(x - DARK_ORB_OFFSET, y - DARK_ORB_OFFSET,
                        DARK_ORB_ORIGIN, DARK_ORB_ORIGIN, DARK_ORB_SIZE, DARK_ORB_SIZE,
                        scale, scale, rotation);
            case WARNING_SIGN:
                // Native WarningSignEffect ignores the (absent) region and has no scale/rotation
                // field either: a fixed 64x64 rect and a hardcoded additive uniform scale of
                // Settings.scale * 2f (with a hardcoded zero rotation). packedWidth/packedHeight,
                // vY, vX, the region offsets, dur_div2, duration, and the effect scale/rotation
                // inputs are all unused.
                return new Params(x - WARNING_ORIGIN_X, y - WARNING_ORIGIN_Y,
                        WARNING_ORIGIN_X, WARNING_ORIGIN_Y, WARNING_WIDTH, WARNING_HEIGHT,
                        settingsScale * WARNING_SCALE_FACTOR, settingsScale * WARNING_SCALE_FACTOR,
                        0f);
            case STUN_STAR:
                // Native StunStarEffect reuses the ambient center-packed geometry but shifts the
                // draw POSITION by -(vX * 30f * Settings.scale), -(vY * 5f * Settings.scale); origin
                // packed/2, size packed, uniform scale, and the field rotation are as usual.
                return new Params(
                        x - vX * STUN_STAR_VX_FACTOR * settingsScale,
                        y - vY * STUN_STAR_VY_FACTOR * settingsScale,
                        originX, originY, packedWidth, packedHeight, scale, scale, rotation);
            case FALLING_DUST:
            case SCENE_DUST:
                // Native FallingDustEffect reuses the ambient center-packed geometry except that its
                // ORIGIN is the region's own offsetX/offsetY (NOT packedWidth/2, packedHeight/2);
                // size packed, uniform scale, and the field rotation are as usual. DustEffect
                // (vfx-scene-world) reuses this same rule verbatim: setColor(color) then
                // sb.draw(img, x, y, img.offsetX, img.offsetY, pw, ph, scale, scale, rotation) with NO
                // setBlendFunction, so it is another ambient member of this one branch.
                return new Params(x, y, regionOffsetX, regionOffsetY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case LIGHTNING_ORB_PASSIVE:
                // Native LightningOrbPassiveEffect ignores the (absent) region: a fixed
                // offset/origin/size rect (offset 61, origin 61, size 122) and the field rotation;
                // packedWidth/packedHeight, vY, vX, the region offsets, dur_div2, duration, and
                // Settings.scale are unused. Its src rect is (0, 0, 122, 122), the full native rect.
                // The per-instance flipX/flipY booleans are resolved by the renderer (see
                // usesInstanceFlipX/usesInstanceFlipY), not here.
                return new Params(x - LIGHTNING_ORB_PASSIVE_OFFSET, y - LIGHTNING_ORB_PASSIVE_OFFSET,
                        LIGHTNING_ORB_PASSIVE_ORIGIN, LIGHTNING_ORB_PASSIVE_ORIGIN,
                        LIGHTNING_ORB_PASSIVE_SIZE, LIGHTNING_ORB_PASSIVE_SIZE,
                        scale, scale, rotation);
            case GLOWY_FIRE_EYES:
                // Native GlowyFireEyesEffect ignores the (absent) region: a fixed
                // offset/origin/size rect (offset 64, origin 64, size 128) and a HARDCODED zero
                // rotation (the class has no rotation field); packedWidth/packedHeight, vY, vX, the
                // region offsets, dur_div2, duration, and Settings.scale are unused. Its src rect is
                // (0, 0, 128, 128), the full native rect. Only the horizontal flip (from the effect's
                // flippedX field) is passed; the vertical flip is always false.
                return new Params(x - GLOWY_FIRE_EYES_OFFSET, y - GLOWY_FIRE_EYES_OFFSET,
                        GLOWY_FIRE_EYES_ORIGIN, GLOWY_FIRE_EYES_ORIGIN,
                        GLOWY_FIRE_EYES_SIZE, GLOWY_FIRE_EYES_SIZE, scale, scale, 0f);
            case TORCH_HEAD_FIRE:
                // Native TorchHeadFireEffect reuses the GlowyFireEyesEffect shape-C rect exactly
                // (offset/origin 64, size 128, src 0,0,128,128, hardcoded rotation 0f) over its own
                // instance Texture img, and passes the effect's own flippedX horizontal flip with a
                // hardcoded false vertical flip. It adds ONE new pure rule: its native draw scale is
                // ASYMMETRIC — scaleX = scale * 1.2f while scaleY = scale. packedWidth/packedHeight,
                // vY, vX, the region offsets, dur_div2, duration, and Settings.scale are unused.
                return new Params(x - GLOWY_FIRE_EYES_OFFSET, y - GLOWY_FIRE_EYES_OFFSET,
                        GLOWY_FIRE_EYES_ORIGIN, GLOWY_FIRE_EYES_ORIGIN,
                        GLOWY_FIRE_EYES_SIZE, GLOWY_FIRE_EYES_SIZE,
                        scale * TORCH_HEAD_FIRE_SCALE_X_MULTIPLIER, scale, 0f);
            case STANCE_CHANGE_ABSORPTION:
                // Native StanceChangeAbsorptionParticle ignores the (absent) region and draws the
                // static ImageMaster.WOBBLY_ORB_VFX Texture TWICE with a fixed shape-C rect: offset
                // 16, origin 16, size 32, src (0, 0, 32, 32), rotation offset -200f. Its two draws
                // share this exact rect; the per-pass RNG-derived scaleX/scaleY are applied by the
                // renderer (see drawPassRandomRanges), so this returns the base (scale, scale).
                // Additive (setBlendFunction 770/1 before and 770/771 after). packedWidth/
                // packedHeight, vY, vX, the region offsets, dur_div2, duration, and Settings.scale are
                // unused.
                return new Params(x - STANCE_CHANGE_ABSORPTION_OFFSET,
                        y - STANCE_CHANGE_ABSORPTION_OFFSET,
                        STANCE_CHANGE_ABSORPTION_ORIGIN, STANCE_CHANGE_ABSORPTION_ORIGIN,
                        STANCE_CHANGE_ABSORPTION_SIZE, STANCE_CHANGE_ABSORPTION_SIZE,
                        scale, scale, rotation + STANCE_CHANGE_ABSORPTION_ROTATION_OFFSET);
            case BUFF_PARTICLE:
                // Native BuffParticleEffect: setBlendFunction(770, 1); setColor(color);
                //   sb.draw(img, x - packedWidth/2f, y - packedHeight/2f, img.offsetX, img.offsetY,
                //           packedWidth, packedHeight, scale, scale, rotation);
                //   setBlendFunction(770, 771).
                // It is its OWN branch (NOT the shared center-packed branch): the draw POSITION is
                // offset by half the packed footprint, and the ORIGIN is the region's OWN
                // (offsetX, offsetY) rather than packed/2. Additive. Its vY is update-only (never
                // consumed); the region offsets are already captured by the renderer for the
                // FALLING_DUST/SCENE_DUST kind, so no new capture path is needed. The
                // scaleYMultiplier tail scalar is unused (scaleX == scaleY == scale).
                return new Params(x - packedWidth / 2f, y - packedHeight / 2f,
                        regionOffsetX, regionOffsetY, packedWidth, packedHeight,
                        scale, scale, rotation);
            case CARD_TRAIL:
                // Native CardTrailEffect.draws on the img (TextureAtlas.AtlasRegion) path with a fixed
                // ORIGIN (6f, 6f) and fixed SIZE (12f, 12f), INDEPENDENT of the region's packed size,
                // and a hardcoded rotation 0f (the class inherits AbstractGameEffect.rotation but its
                // draw ignores the field). ADDITIVE, x/y
                // passthrough, uniform scale. packedWidth/packedHeight, vY, vX, the region offsets,
                // dur_div2, duration, and Settings.scale are unused.
                return new Params(x, y, CARD_TRAIL_ORIGIN, CARD_TRAIL_ORIGIN,
                        CARD_TRAIL_SIZE, CARD_TRAIL_SIZE, scale, scale, 0f);
            case FLICK_COIN:
                // Native FlickCoinEffect: setBlendFunction(770, 1); setColor(color); setColor(color);
                //   sb.draw(img, cX - (img.packedWidth / 2), cY - (img.packedHeight / 2) + yOffset,
                //           img.packedWidth / 2f, img.packedHeight / 2f,
                //           img.packedWidth, img.packedHeight,
                //           scale * 0.7f, scale * 0.4f, rotation);
                //   setBlendFunction(770, 771).
                // The caller maps the effect's cX/cY/yOffset onto the (x, y, vY) inputs. The draw
                // POSITION half uses native INTEGER division of the packed size while the ORIGIN half
                // uses native FLOAT division (they differ by 0.5 for an odd region); the draw scale is
                // ANISOTROPIC (scaleX = scale*0.7f, scaleY = scale*0.4f). durDiv2/duration/
                // settingsScale/vX/the region offsets are unused.
                return new Params(
                        x - (int) packedWidth / 2,
                        y - (int) packedHeight / 2 + vY,
                        packedWidth / 2f, packedHeight / 2f,
                        packedWidth, packedHeight,
                        scale * FLICK_COIN_SCALE_X, scale * FLICK_COIN_SCALE_Y, rotation);
            case HEAL_PANEL:
                // Native HealPanelEffect (verified bytecode): setColor(color); setBlendFunction(770, 1);
                //   sb.draw(img, x - 32f + 32f * Settings.scale,
                //           Settings.HEIGHT - 32f * Settings.scale - 32f,
                //           32f, 32f, 64f, 64f, scale, scale, rotation,
                //           0, 0, 64, 64, false, false);
                //   setBlendFunction(770, 771).
                // A bare-Texture fixed-rect kind (the STATIC Texture img, no instance img field) with a
                // fixed src rect (0, 0, 64, 64), fixed origin (32f, 32f) and fixed size 64f x 64f. The
                // draw POSITION is PANEL-SPACE: x uses the effect's own x with a settings-scaled 32f
                // term, and y is anchored to Settings.HEIGHT with a settings-scaled 32f term minus the
                // fixed 32f — so it consumes the caller-supplied settings height (the overload's
                // settingsHeight) as well as the settings scale. packWidth/packHeight, vY, vX, the
                // region offsets, durDiv2, and duration are unused.
                return new Params(
                        x - HEAL_PANEL_X_OFFSET + HEAL_PANEL_X_OFFSET * settingsScale,
                        settingsHeight - HEAL_PANEL_Y_OFFSET * settingsScale - HEAL_PANEL_Y_OFFSET,
                        HEAL_PANEL_ORIGIN, HEAL_PANEL_ORIGIN, HEAL_PANEL_SIZE, HEAL_PANEL_SIZE,
                        scale, scale, rotation);
            case PING_HP:
                // Native PingHpEffect (verified bytecode; the HealPanel analogue):
                //   setColor(color); setBlendFunction(770, 1);
                //   sb.draw(ImageMaster.TP_HP, x - 32f + 32f * Settings.scale,
                //           Settings.HEIGHT - 32f * Settings.scale - 32f,
                //           32f, 32f, 64f, 64f, scale * Settings.scale, scale * Settings.scale,
                //           rotation, 0, 0, 64, 64, false, false);
                //   setBlendFunction(770, 771).
                // A bare-Texture fixed-rect kind (the STATIC ImageMaster.TP_HP Texture, no instance img
                // field) with a fixed src rect (0, 0, 64, 64), fixed origin (32f, 32f) and fixed size
                // 64f x 64f. The draw POSITION is the identical PANEL-SPACE rule as HEAL_PANEL: x uses
                // the effect's own x with a settings-scaled 32f term, and y is anchored to
                // Settings.HEIGHT with a settings-scaled 32f term minus the fixed 32f — consuming the
                // caller-supplied settings height as well as the settings scale. Its ONE difference from
                // HEAL_PANEL is the uniform draw scale: BOTH axes are scale * Settings.scale.
                // packWidth/packHeight, vY, vX, the region offsets, durDiv2, and duration are unused.
                return new Params(
                        x - PING_HP_X_OFFSET + PING_HP_X_OFFSET * settingsScale,
                        settingsHeight - PING_HP_Y_OFFSET * settingsScale - PING_HP_Y_OFFSET,
                        PING_HP_ORIGIN, PING_HP_ORIGIN, PING_HP_SIZE, PING_HP_SIZE,
                        scale * settingsScale, scale * settingsScale, rotation);
            default:
                throw new IllegalArgumentException("unhandled kind: " + kind);
        }
    }
}

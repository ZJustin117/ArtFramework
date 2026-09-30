package artframework.sts1.render;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure per-instance claim predicate for the default-off, family-neutral transient-effect claim seam.
 *
 * <p>This is a data predicate, not a per-subclass {@code @SpirePatch}: NRO-04 forbids per-subclass
 * hooks, so the claimable classes are recognized by FQN at the shared effect-container seam. The
 * seam is generic over per-instance transient-effect families, so the current members are
 * cross-family: the five {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}, {@code LightFlareMEffect}, {@code LightFlareLEffect}, and
 * {@code TorchParticleLEffect}, the {@code vfx-combat} {@code FlashAtkImgEffect},
 * {@code RedFireBurstParticleEffect}, and {@code SmokeBlurEffect}, the {@code vfx-misc-root}
 * {@code FireBurstParticleEffect} and {@code NemesisFireParticle}, and the {@code vfx-scene-world}
 * {@code CeilingDustCloudEffect}. The aura
 * members are the four {@code AtlasRegion}-drawn classes
 * ({@code StanceAuraEffect}, {@code WrathParticleEffect}, {@code DivinityParticleEffect},
 * {@code DivinityStanceChangeParticle}) plus {@code CalmParticleEffect} (which draws a bare
 * {@code Texture}); {@code LightFlareSEffect} is the first non-aura member and draws an
 * {@code AtlasRegion} with the same additive geometry as {@code StanceAuraEffect}; the three
 * later {@code vfx-scene-world} members ({@code LightFlareMEffect}, {@code LightFlareLEffect},
 * {@code TorchParticleLEffect}) are geometry-identical to that additive center-packed branch;
 * {@code TorchParticleLEffect} has a {@code vY} field but its native {@code render} ignores it
 * (it is used only by {@code update()}), so it stays on the optional-{@code vY} reader path;
 * {@code FlashAtkImgEffect} is the first {@code vfx-combat} member and reuses the same geometry but
 * never switches blend function (ambient blend, see
 * {@link VfxDrawGeometry#additiveBlend}); the two fire-burst members
 * ({@code FireBurstParticleEffect} in {@code vfx-misc-root} and {@code RedFireBurstParticleEffect}
 * in {@code vfx-combat}) are additive and reuse that same additive center-packed geometry; the
 * three ambient members ({@code SmokeBlurEffect}, {@code CeilingDustCloudEffect}, and
 * {@code NemesisFireParticle}) also reuse the geometry but never call
 * {@code setBlendFunction} (ambient blend, like {@code FlashAtkImgEffect}). All five new members
 * have a {@code vY} field that their native {@code render} ignores (used only by
 * {@code update()}), so they all stay on the optional-{@code vY} reader path. The two newest
 * members introduce the third draw shape, a bare {@code Texture} + fixed source rect (the first
 * being {@code CalmParticleEffect}, which became kind-driven): {@code ShieldParticleEffect}
 * paints the static {@code ImageMaster.INTENT_DEFEND} {@code Texture} additively with a
 * hardcoded zero rotation, and {@code DebuffParticleEffect} paints its own instance
 * {@code Texture} {@code img} under the ambient blend and consumes its {@code rotation} field.
 * The four newest members also reuse the additive center-packed geometry with a single
 * {@code sb.draw(img, x, y, img.packedWidth/2f, img.packedHeight/2f, img.packedWidth,
 * img.packedHeight, scale, scale, rotation)}: the {@code vfx-scene-world}
 * {@code TorchParticleXLEffect} and the {@code vfx-misc-root} {@code GhostlyWeakFireEffect} are
 * additive ({@code setBlendFunction(770, 1)} before and {@code (770, 771)} after), while the
 * {@code vfx-misc-root} {@code GenericSmokeEffect} and {@code ExhaustBlurEffect} never call
 * {@code setBlendFunction} (ambient blend). All four own a {@code vY} used only by
 * {@code update()} (never in {@code render}), so they stay on the optional-{@code vY} reader
 * path.
 * The two newest members extend the same bare {@code Texture} + fixed source-rect shape and both
 * draw additively: the {@code vfx-combat} {@code IceShatterEffect} paints its own instance
 * {@code Texture} {@code img} (chosen in its constructor from
 * {@code ImageMaster.FROST_ACTIVATE_VFX_1}/{@code _2}) with a fixed {@code (32, 32)} origin,
 * {@code (64, 64)} size and {@code (0, 0, 64, 64)} source rect and consumes its inherited
 * {@code rotation} field, while the {@code vfx-combat} {@code WebParticleEffect} paints the static
 * {@code ImageMaster.WEB_VFX} {@code Texture}, hardcodes rotation {@code 0f}, and forces the set
 * color to {@code (1, 1, 1, color.a)} (RGB white, alpha from the effect's color) — see
 * {@link VfxDrawGeometry#whiteAlphaOnly}. Neither adds a new formula branch beyond that shape.
 * The four newest members reuse the three existing draw shapes and add only one fixed rect: the
 * {@code vfx-combat} {@code EntangleEffect} is byte-identical to {@code WebParticleEffect} (the
 * static {@code ImageMaster.WEB_VFX} {@code Texture}, offset 0, origin {@code (32, 32)}, size
 * {@code (64, 64)}, src {@code (0, 0, 64, 64)}, rotation hardcoded {@code 0f}, additive blend, and
 * the same {@code (1, 1, 1, color.a)} set-color rule), so it shares that kind's static-texture and
 * white-alpha configuration rather than adding a new formula; the {@code vfx-combat}
 * {@code BlockImpactLineEffect} and the {@code vfx-misc-root} {@code ExhaustPileParticle} both reuse
 * the ambient center-packed geometry ({@code sb.draw(img, x, y, packedWidth/2f, packedHeight/2f,
 * packedWidth, packedHeight, scale, scale, rotation)} with no {@code setBlendFunction}), the latter
 * resolving a {@code private static} {@code AtlasRegion img} declared on its own class; and the
 * {@code vfx-combat} {@code UnknownParticleEffect} introduces one new ambient fixed rect
 * ({@code sb.draw(img, x - 64f, y - 64f, 64f, 64f, 128f, 128f, scale, scale, rotation, 0, 0, 128,
 * 128, false, false)}) over its own instance {@code Texture img} and consuming its {@code rotation}
 * field. The five newest members are all {@code vfx-combat} and reuse the two existing shapes: the
 * {@code FlameParticleEffect} and {@code LightningOrbActivateEffect} reuse the additive
 * center-packed geometry, the {@code DamageImpactBlurEffect} and {@code DamageImpactLineEffect}
 * reuse that same geometry under the ambient blend (neither calls {@code setBlendFunction}), and the
 * {@code DarkOrbPassiveEffect} introduces one new additive shape-C fixed rect
 * ({@code sb.draw(img, x - 37f, y - 37f, 37f, 37f, 74f, 74f, scale, scale, rotation, 0, 0, 74, 74,
 * false, false)}) over its own instance {@code Texture img} and consuming its {@code rotation}
 * field (the src rect is the full 74&times;74 region). The three newest members add one new pure
 * rule each and reuse two existing shapes: the {@code vfx-misc-root} {@code WarningSignEffect} is a
 * bare {@code Texture} fixed-rect kind (the static {@code ImageMaster.WARNING_ICON_VFX}
 * {@code Texture}) whose own uniform scale is the hardcoded {@code settingsScale * 2f} (it has NO
 * {@code scale} field, so the effect scale is ignored) with a hardcoded zero rotation and additive
 * blend; the {@code vfx-combat} {@code StunStarEffect} reuses the ambient center-packed
 * {@code AtlasRegion} geometry with a NEW position offset
 * ({@code -(vX * 30f * settingsScale)}, {@code -(vY * 5f * settingsScale)}), so it consumes both its
 * {@code vX} and {@code vY} fields; and the {@code vfx-misc-root} {@code FallingDustEffect} reuses
 * that ambient center-packed geometry with a NEW origin taken from the region's own
 * {@code offsetX}/{@code offsetY} (rather than {@code packedWidth/2}, {@code packedHeight/2}). The
 * newest three members add one new pure origin rule each (or reuse the ambient center-packed
 * branch): the {@code vfx-combat} {@code LightningEffect} reuses the additive center-packed
 * {@code AtlasRegion} geometry with a NEW origin rule — the draw origin Y is {@code 0f} (not
 * {@code packedHeight/2f}) with {@code originX} keeping {@code packedWidth/2f}, size packed,
 * uniform scale, and the field rotation; the {@code vfx-misc-root} {@code FlameBallParticleEffect}
 * reuses that additive center-packed geometry with a NEW origin rule — {@code originY =
 * packedHeight/2f + 20f * settingsScale} (a settings-scaled {@code +20f} lift on the vertical
 * origin, tracking {@code Settings.scale} exactly as native does); and the
 * {@code vfx-misc-root} {@code ShineLinesEffect} reuses the ambient center-packed geometry
 * unchanged (origin {@code packedWidth/2f}, {@code packedHeight/2f}, no {@code setBlendFunction},
 * with a native {@code if (!isDone)} draw guard). The
 * three newest members are all {@code vfx-scene-world}: the {@code TorchParticleMEffect} and
 * {@code TorchParticleSEffect} are additive center-packed members (native {@code setBlendFunction}
 * {@code (770, 1)} before and {@code (770, 771)} after) with no new rule and a {@code vY} used only
 * by {@code update()}, and {@code DustEffect} (the NO-ARG constructor) reuses the {@code
 * FALLING_DUST} region-offset-origin rule — its ambient draw origin is the region's own {@code
 * offsetX}/{@code offsetY} rather than {@code packedWidth/2f}/{@code packedHeight/2f} — so it adds
 * an ambient member to the same geometry branch. The
 * two newest members are the {@code vfx-combat} {@code FlyingSpikeEffect} (additive center-packed,
 * no new rule) and the {@code vfx-misc-root} {@code ConeEffect} (ambient center-packed with the new
 * {@code originX = 0f} origin rule and a {@code scale * 1.1f} uniform scale). The two newest
 * members add the seam's first per-kind NATIVE DRAW GUARD (see
 * {@link VfxDrawGeometry#nativeSkipsDrawByGuard} and {@link VfxDrawGeometry#guardFieldName}): the
 * {@code vfx-combat} {@code FallingIceEffect} is a shape-C fixed-rect kind (instance
 * {@code Texture img}, offset/origin 48, size 96&times;96, src {@code 0,0,96,96}, additive, consuming
 * its {@code rotation} field) whose native {@code render} is guarded by
 * {@code if (waitTimer < 0f)}, and the {@code vfx-misc-root} {@code DamageHeartEffect} is an ambient
 * center-packed kind (its {@code img} is a public {@code AtlasRegion}) whose native {@code render} is
 * guarded by {@code if (delayTimer < 0f)}. Both were previously screened out for that wait-phase
 * guard; the guard rule now keeps a claimed instance in pixel parity by declining (drawing nothing)
 * whenever the guard field is present and {@code >= 0f}, exactly like the native wait phase. The
 * additive
 * members are the only ones whose host draw installs additive blend; every member may be claimed
 * per instance.
 */
public final class VfxClaimPolicy {

    public static final String STANCE_AURA_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect";
    public static final String WRATH_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect";
    public static final String DIVINITY_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect";
    public static final String CALM_PARTICLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect";
    public static final String DIVINITY_STANCE_CHANGE_PARTICLE =
            "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle";
    public static final String SCENE_LIGHT_FLARE =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect";
    public static final String SCENE_LIGHT_FLARE_M =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect";
    public static final String SCENE_LIGHT_FLARE_L =
            "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect";
    public static final String SCENE_TORCH_PARTICLE_L =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect";
    public static final String FLASH_ATK_IMG =
            "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect";
    public static final String FIRE_BURST =
            "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect";
    public static final String RED_FIRE_BURST =
            "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect";
    public static final String SMOKE_BLUR =
            "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect";
    public static final String CEILING_DUST =
            "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect";
    public static final String NEMESIS_FIRE =
            "com.megacrit.cardcrawl.vfx.NemesisFireParticle";
    public static final String SHIELD_PARTICLE =
            "com.megacrit.cardcrawl.vfx.ShieldParticleEffect";
    public static final String DEBUFF_PARTICLE =
            "com.megacrit.cardcrawl.vfx.DebuffParticleEffect";
    public static final String SCENE_TORCH_PARTICLE_XL =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect";
    public static final String GHOSTLY_WEAK_FIRE =
            "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect";
    public static final String GENERIC_SMOKE =
            "com.megacrit.cardcrawl.vfx.GenericSmokeEffect";
    public static final String EXHAUST_BLUR =
            "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect";
    public static final String ICE_SHATTER =
            "com.megacrit.cardcrawl.vfx.combat.IceShatterEffect";
    public static final String WEB_PARTICLE =
            "com.megacrit.cardcrawl.vfx.combat.WebParticleEffect";
    public static final String ENTANGLE_EFFECT =
            "com.megacrit.cardcrawl.vfx.combat.EntangleEffect";
    public static final String BLOCK_IMPACT_LINE =
            "com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect";
    public static final String EXHAUST_PILE_PARTICLE =
            "com.megacrit.cardcrawl.vfx.ExhaustPileParticle";
    public static final String UNKNOWN_PARTICLE =
            "com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect";
    public static final String FLAME_PARTICLE =
            "com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect";
    public static final String LIGHTNING_ORB_ACTIVATE =
            "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect";
    public static final String DAMAGE_IMPACT_BLUR =
            "com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect";
    public static final String DAMAGE_IMPACT_LINE =
            "com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect";
    public static final String DARK_ORB_PASSIVE =
            "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect";
    public static final String WARNING_SIGN =
            "com.megacrit.cardcrawl.vfx.WarningSignEffect";
    public static final String STUN_STAR =
            "com.megacrit.cardcrawl.vfx.combat.StunStarEffect";
    public static final String FALLING_DUST =
            "com.megacrit.cardcrawl.vfx.FallingDustEffect";
    public static final String LIGHTNING_EFFECT =
            "com.megacrit.cardcrawl.vfx.combat.LightningEffect";
    public static final String FLAME_BALL =
            "com.megacrit.cardcrawl.vfx.FlameBallParticleEffect";
    public static final String SHINE_LINES =
            "com.megacrit.cardcrawl.vfx.ShineLinesEffect";
    public static final String TORCH_PARTICLE_M =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect";
    public static final String TORCH_PARTICLE_S =
            "com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect";
    public static final String SCENE_DUST =
            "com.megacrit.cardcrawl.vfx.scene.DustEffect";
    public static final String LIGHTNING_ORB_PASSIVE =
            "com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect";
    public static final String GLOWY_FIRE_EYES =
            "com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect";
    public static final String FLYING_SPIKE =
            "com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect";
    public static final String CONE_EFFECT =
            "com.megacrit.cardcrawl.vfx.ConeEffect";
    public static final String FALLING_ICE =
            "com.megacrit.cardcrawl.vfx.combat.FallingIceEffect";
    public static final String DAMAGE_HEART =
            "com.megacrit.cardcrawl.vfx.DamageHeartEffect";

    private static final List<String> SUPPORTED_CLASSES = Collections.unmodifiableList(
            Arrays.asList(STANCE_AURA_EFFECT, WRATH_PARTICLE_EFFECT, DIVINITY_PARTICLE_EFFECT,
                    CALM_PARTICLE_EFFECT, DIVINITY_STANCE_CHANGE_PARTICLE, SCENE_LIGHT_FLARE,
                    FLASH_ATK_IMG, SCENE_LIGHT_FLARE_M, SCENE_LIGHT_FLARE_L,
                    SCENE_TORCH_PARTICLE_L, FIRE_BURST, RED_FIRE_BURST, SMOKE_BLUR,
                    CEILING_DUST, NEMESIS_FIRE, SHIELD_PARTICLE, DEBUFF_PARTICLE,
                    SCENE_TORCH_PARTICLE_XL, GHOSTLY_WEAK_FIRE, GENERIC_SMOKE, EXHAUST_BLUR,
                    ICE_SHATTER, WEB_PARTICLE, ENTANGLE_EFFECT, BLOCK_IMPACT_LINE,
                    EXHAUST_PILE_PARTICLE, UNKNOWN_PARTICLE, FLAME_PARTICLE,
                    LIGHTNING_ORB_ACTIVATE, DAMAGE_IMPACT_BLUR, DAMAGE_IMPACT_LINE,
                    DARK_ORB_PASSIVE, WARNING_SIGN, STUN_STAR, FALLING_DUST,
                    LIGHTNING_EFFECT, FLAME_BALL, SHINE_LINES,
                    TORCH_PARTICLE_M, TORCH_PARTICLE_S, SCENE_DUST,
                    LIGHTNING_ORB_PASSIVE, GLOWY_FIRE_EYES, FLYING_SPIKE, CONE_EFFECT,
                    FALLING_ICE, DAMAGE_HEART));

    private VfxClaimPolicy() {}

    /** True only for the exact claimable FQNs; null, blank, and every other class fail open. */
    public static boolean supports(String nativeClassName) {
        if (nativeClassName == null) return false;
        String value = nativeClassName.trim();
        if (value.isEmpty()) return false;
        return STANCE_AURA_EFFECT.equals(value)
                || WRATH_PARTICLE_EFFECT.equals(value)
                || DIVINITY_PARTICLE_EFFECT.equals(value)
                || CALM_PARTICLE_EFFECT.equals(value)
                || DIVINITY_STANCE_CHANGE_PARTICLE.equals(value)
                || SCENE_LIGHT_FLARE.equals(value)
                || FLASH_ATK_IMG.equals(value)
                || SCENE_LIGHT_FLARE_M.equals(value)
                || SCENE_LIGHT_FLARE_L.equals(value)
                || SCENE_TORCH_PARTICLE_L.equals(value)
                || FIRE_BURST.equals(value)
                || RED_FIRE_BURST.equals(value)
                || SMOKE_BLUR.equals(value)
                || CEILING_DUST.equals(value)
                || NEMESIS_FIRE.equals(value)
                || SHIELD_PARTICLE.equals(value)
                || DEBUFF_PARTICLE.equals(value)
                || SCENE_TORCH_PARTICLE_XL.equals(value)
                || GHOSTLY_WEAK_FIRE.equals(value)
                || GENERIC_SMOKE.equals(value)
                || EXHAUST_BLUR.equals(value)
                || ICE_SHATTER.equals(value)
                || WEB_PARTICLE.equals(value)
                || ENTANGLE_EFFECT.equals(value)
                || BLOCK_IMPACT_LINE.equals(value)
                || EXHAUST_PILE_PARTICLE.equals(value)
                || UNKNOWN_PARTICLE.equals(value)
                || FLAME_PARTICLE.equals(value)
                || LIGHTNING_ORB_ACTIVATE.equals(value)
                || DAMAGE_IMPACT_BLUR.equals(value)
                || DAMAGE_IMPACT_LINE.equals(value)
                || DARK_ORB_PASSIVE.equals(value)
                || WARNING_SIGN.equals(value)
                || STUN_STAR.equals(value)
                || FALLING_DUST.equals(value)
                || LIGHTNING_EFFECT.equals(value)
                || FLAME_BALL.equals(value)
                || SHINE_LINES.equals(value)
                || TORCH_PARTICLE_M.equals(value)
                || TORCH_PARTICLE_S.equals(value)
                || SCENE_DUST.equals(value)
                || LIGHTNING_ORB_PASSIVE.equals(value)
                || GLOWY_FIRE_EYES.equals(value)
                || FLYING_SPIKE.equals(value)
                || CONE_EFFECT.equals(value)
                || FALLING_ICE.equals(value)
                || DAMAGE_HEART.equals(value);
    }

    /**
     * The exact claimable FQNs, in their listed/append order (mirrored by {@link #supports});
     * the three newest scene-world members (m/l flare, l torch) are appended last rather than
     * following strict source-declaration order, and the five newest members (the two fire bursts,
     * the smoke blur, the ceiling dust, and the nemesis fire) are appended after those. The two
     * newest members ({@code ShieldParticleEffect}, {@code DebuffParticleEffect}) — the bare
     * {@code Texture} + fixed source-rect shape — are appended after the nemesis fire. The four
     * newest members (the {@code vfx-scene-world} {@code TorchParticleXLEffect}, the
     * {@code vfx-misc-root} {@code GhostlyWeakFireEffect}, {@code GenericSmokeEffect}, and
     * {@code ExhaustBlurEffect}) — all {@code AtlasRegion}-drawn with the same additive
     * center-packed geometry as {@code StanceAuraEffect} — are appended after the debuff particle in
     * that order; {@code TorchParticleXLEffect} and {@code GhostlyWeakFireEffect} are additive, while
     * {@code GenericSmokeEffect} and {@code ExhaustBlurEffect} are ambient (no
     * {@code setBlendFunction}), and all four own a {@code vY} that their native {@code render}
     * ignores (used only by {@code update()}). The two newest members ({@code IceShatterEffect},
     * {@code WebParticleEffect}) — the bare {@code Texture} + fixed source-rect shape again, both
     * additive — are appended after the exhaust blur in that order; {@code IceShatterEffect} owns a
     * {@code vY} that its native {@code render} ignores (used only by {@code update()}), while
     * {@code WebParticleEffect} has no {@code vY} field at all. The four newest members
     * ({@code EntangleEffect}, {@code BlockImpactLineEffect}, {@code ExhaustPileParticle},
     * {@code UnknownParticleEffect}) are appended after the web particle in that order; {@code
     * EntangleEffect} is byte-identical to {@code WebParticleEffect} (the static {@code
     * ImageMaster.WEB_VFX} {@code Texture} and the white-alpha set-color rule), {@code
     * BlockImpactLineEffect} and {@code ExhaustPileParticle} are ambient center-packed
     * {@code AtlasRegion} members ({@code ExhaustPileParticle.img} is {@code private static}), and
     * {@code UnknownParticleEffect} is the first ambient bare-{@code Texture} member whose fixed
     * source rect is {@code (128, 128)} and whose {@code rotation} field IS consumed. The five
     * newest members ({@code FlameParticleEffect}, {@code LightningOrbActivateEffect},
     * {@code DamageImpactBlurEffect}, {@code DamageImpactLineEffect}, {@code DarkOrbPassiveEffect})
     * — all {@code vfx-combat} — are appended after the unknown particle in that order; the first
     * two reuse the additive center-packed geometry, the next two reuse it ambiently (no
     * {@code setBlendFunction}), and {@code DarkOrbPassiveEffect} is the first additive
     * bare-{@code Texture} member of a new {@code 74}-rect whose {@code rotation} field IS consumed.
     * The three newest members ({@code WarningSignEffect}, {@code StunStarEffect},
     * {@code FallingDustEffect}) are appended after the dark orb passive in that order;
     * {@code WarningSignEffect} is a bare-{@code Texture} fixed-rect member with a hardcoded
     * {@code settingsScale * 2f} uniform scale (it has no {@code scale} field at all),
     * {@code StunStarEffect} is an ambient center-packed {@code AtlasRegion} member whose position is
     * offset by its {@code vX}/{@code vY}, and {@code FallingDustEffect} is an ambient center-packed
     * {@code AtlasRegion} member whose origin is the region's own {@code offsetX}/{@code offsetY}.
     * The three newest members ({@code LightningEffect}, {@code FlameBallParticleEffect},
     * {@code ShineLinesEffect}) are appended after the falling dust in that order;
     * {@code LightningEffect} is an additive center-packed {@code AtlasRegion} member whose origin Y
     * is {@code 0f}, {@code FlameBallParticleEffect} is an additive center-packed {@code AtlasRegion}
     * member whose origin Y is {@code packedHeight/2f + 20f * settingsScale}, and
     * {@code ShineLinesEffect} is an
     * ambient center-packed {@code AtlasRegion} member (its native {@code render} also guards the
     * draw with {@code if (!isDone)}). The three newest members ({@code TorchParticleMEffect},
     * {@code TorchParticleSEffect}, {@code DustEffect}) are appended after the shine lines in that
     * order; {@code TorchParticleMEffect} and {@code TorchParticleSEffect} are additive
     * center-packed {@code AtlasRegion} members that reuse the {@code StanceAuraEffect} geometry with
     * no new rule (their {@code vY} is update-only), and {@code DustEffect} is an ambient
     * center-packed {@code AtlasRegion} member (NO-ARG constructor) whose origin is the region's own
     * {@code offsetX}/{@code offsetY} (the same rule as {@code FallingDustEffect}). The two newest
     * members ({@code LightningOrbPassiveEffect} in {@code vfx-combat} and
     * {@code GlowyFireEyesEffect} in {@code vfx-misc-root}) extend the bare-{@code Texture}
     * shape-C path with per-instance FLIP flags — the first claimable kinds whose native
     * {@code render} passes the effect's own flip booleans to the raw-texture draw overload — and
     * are appended last in that order; {@code LightningOrbPassiveEffect} is additive over a
     * {@code 122}-rect and consumes its inherited {@code rotation} field and its own
     * {@code flipX}/{@code flipY} booleans, while {@code GlowyFireEyesEffect} is additive over a
     * {@code 128}-rect with a hardcoded zero rotation and its own single {@code flippedX} boolean
     * (vertical flip always {@code false}).
     * The two newest members are the {@code vfx-combat} {@code FlyingSpikeEffect} (additive
     * center-packed, no new rule) and the {@code vfx-misc-root} {@code ConeEffect} (ambient
     * center-packed with a NEW origin rule — {@code originX = 0f} rather than {@code packedWidth/2f}
     * — and a uniform scale of {@code scale * 1.1f}). The two newest members
     * ({@code FallingIceEffect} in {@code vfx-combat} and {@code DamageHeartEffect} in
     * {@code vfx-misc-root}) are appended last in that order and introduce the seam's first per-kind
     * NATIVE DRAW GUARD: each native {@code render} wraps its draw in a wait-phase field check
     * ({@code if (waitTimer < 0f)} and {@code if (delayTimer < 0f)} respectively), so the renderer
     * declines (draws nothing) whenever that field is present and {@code >= 0f} — see
     * {@link VfxDrawGeometry#nativeSkipsDrawByGuard} and {@link VfxDrawGeometry#guardFieldName}.
     * {@code FallingIceEffect} is additive over a fixed {@code 96&times;96} shape-C rect consuming
     * its {@code rotation} field, while {@code DamageHeartEffect} is ambient center-packed with a
     * public {@code AtlasRegion img}.
     */
    public static List<String> supportedClasses() {
        return SUPPORTED_CLASSES;
    }
}

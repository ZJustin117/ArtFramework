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
 * two newest members are the {@code vfx-scene-world} {@code SpookyChestEffect} and
 * {@code IroncladVictoryFlameEffect} (both NO-ARG constructors) — ambient center-packed
 * {@code AtlasRegion} members that introduce the seam's per-instance MIRROR capability on the img
 * path ({@link VfxDrawGeometry#usesInstanceMirrorX}/{@link VfxDrawGeometry#usesInstanceMirrorY}),
 * {@code SpookyChestEffect} with {@code flipX}+{@code flipY} and
 * {@code IroncladVictoryFlameEffect} with {@code flipX} only; {@code FlameParticleEffect} also uses
 * the mirror, so the F15 mirror limitation is resolved. The
 * additive
 * members are the only ones whose host draw installs additive blend; every member may be claimed
 * per instance.
 *
 * <p>The newest (NRO-04 B07) member is the {@code vfx-misc-root} {@code SpotlightEffect}: a
 * FULL-SCREEN bare static-{@code Texture} kind with NO per-effect geometry fields, ADDITIVE, and
 * production-reachable (constructed by {@code GrandFinalEffect} into
 * {@code AbstractDungeon.effectsQueue}) — see {@link #supportedClasses()} for the boundary note.
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
    public static final String SPOOKY_CHEST =
            "com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect";
    public static final String IRONCLAD_VICTORY_FLAME =
            "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect";
    public static final String SPOOKIER_CHEST =
            "com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect";
    public static final String CAMPFIRE_SLEEP_COVER =
            "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect";
    public static final String DEATH_SCREEN_FLOATY =
            "com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect";
    public static final String WRATH_STANCE_CHANGE =
            "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle";
    public static final String STANCE_CHANGE_ABSORPTION =
            "com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle";
    public static final String WATER_SPLASH =
            "com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect";
    public static final String BUFF_PARTICLE =
            "com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect";
    public static final String BOTTOM_FOG =
            "com.megacrit.cardcrawl.vfx.scene.BottomFogEffect";
    public static final String GIANT_FIRE =
            "com.megacrit.cardcrawl.vfx.combat.GiantFireEffect";
    public static final String TORCH_HEAD_FIRE =
            "com.megacrit.cardcrawl.vfx.TorchHeadFireEffect";
    public static final String CARD_TRAIL =
            "com.megacrit.cardcrawl.vfx.CardTrailEffect";
    public static final String FLYING_ORB =
            "com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect";
    public static final String FLICK_COIN =
            "com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect";
    public static final String HEAL_PANEL =
            "com.megacrit.cardcrawl.vfx.combat.HealPanelEffect";
    public static final String PING_HP =
            "com.megacrit.cardcrawl.vfx.combat.PingHpEffect";
    public static final String REWARD_GLOW =
            "com.megacrit.cardcrawl.vfx.RewardGlowEffect";
    public static final String MAP_CIRCLE =
            "com.megacrit.cardcrawl.vfx.MapCircleEffect";
    public static final String SPOTLIGHT =
            "com.megacrit.cardcrawl.vfx.SpotlightEffect";

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
                    FALLING_ICE, DAMAGE_HEART, SPOOKY_CHEST, IRONCLAD_VICTORY_FLAME,
                    SPOOKIER_CHEST, CAMPFIRE_SLEEP_COVER, DEATH_SCREEN_FLOATY,
                    WRATH_STANCE_CHANGE, STANCE_CHANGE_ABSORPTION, WATER_SPLASH, BUFF_PARTICLE,
                    BOTTOM_FOG, GIANT_FIRE, TORCH_HEAD_FIRE, CARD_TRAIL, FLYING_ORB, FLICK_COIN,
                    HEAL_PANEL, PING_HP, REWARD_GLOW, MAP_CIRCLE, SPOTLIGHT));

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
                || DAMAGE_HEART.equals(value)
                || SPOOKY_CHEST.equals(value)
                || IRONCLAD_VICTORY_FLAME.equals(value)
                || SPOOKIER_CHEST.equals(value)
                || CAMPFIRE_SLEEP_COVER.equals(value)
                || DEATH_SCREEN_FLOATY.equals(value)
                || WRATH_STANCE_CHANGE.equals(value)
                || STANCE_CHANGE_ABSORPTION.equals(value)
                || WATER_SPLASH.equals(value)
                || BUFF_PARTICLE.equals(value)
                || BOTTOM_FOG.equals(value)
                || GIANT_FIRE.equals(value)
                || TORCH_HEAD_FIRE.equals(value)
                || CARD_TRAIL.equals(value)
                || FLYING_ORB.equals(value)
                || FLICK_COIN.equals(value)
                || HEAL_PANEL.equals(value)
                || PING_HP.equals(value)
                || REWARD_GLOW.equals(value)
                || MAP_CIRCLE.equals(value)
                || SPOTLIGHT.equals(value);
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
     * public {@code AtlasRegion img}. The two newest members are the {@code vfx-scene-world}
     * {@code SpookyChestEffect} and {@code IroncladVictoryFlameEffect} (both NO-ARG constructors):
     * ambient center-packed {@code AtlasRegion} members (neither calls {@code setBlendFunction}) that
     * reuse the {@code StanceAuraEffect} geometry but are the first img-path kinds to carry
     * per-instance MIRROR booleans — {@code SpookyChestEffect} declares both {@code flipX} and
     * {@code flipY} while {@code IroncladVictoryFlameEffect} declares only {@code flipX}; see
     * {@link VfxDrawGeometry#usesInstanceMirrorX}/{@link VfxDrawGeometry#usesInstanceMirrorY}.
     * {@code FlameParticleEffect} was already a member and also uses the mirror, so the claimed img
     * draw reproduces its {@code flipX} mirror (resolving the F15 limitation). The three newest
     * members reuse the existing ambient center-packed img branch with NO new formula:
     * {@code SpookierChestEffect} ({@code vfx-scene-world}) and
     * {@code CampfireSleepScreenCoverEffect} ({@code vfx-campfire-rest}; the first claimed member of
     * that family, a per-instance ambient center-packed sprite with a NO-ARG constructor) each
     * declare {@code flipX}+{@code flipY} and thus reuse the F22
     * per-instance mirror ({@link VfxDrawGeometry#usesInstanceMirrorX}/{@link
     * VfxDrawGeometry#usesInstanceMirrorY}); {@code DeathScreenFloatyEffect} ({@code vfx-misc-root})
     * declares no flip flags and does not mirror. All three have NO-ARG constructors and call no
     * {@code setBlendFunction} (ambient blend).
     * The newest (F24) member is the {@code vfx-stance-aura} {@code WrathStanceChangeParticle} — the
     * FIRST non-deterministic native effect claimed by the seam: its native {@code render} consumes
     * two {@code MathUtils.random(...)} values (scaleX in {@code [2.9f, 3.1f]}, scaleY in
     * {@code [0.95f, 1.05f]}) and draws at the PLAYER HITBOX CENTER X ({@code
     * AbstractDungeon.player.hb.cX + x}) instead of the effect's own {@code x}. The seam gained an
     * RNG-REPLAY capability ({@link VfxDrawGeometry#randomRanges}), a guard-threshold generalization
     * ({@link VfxDrawGeometry#guardBlocks}, whose WRATH guard blocks when {@code delayTimer > 0f} —
     * unlike the {@code >= 0f} FALLING_ICE/DAMAGE_HEART guards), and a player-hitbox-relative x rule
     * ({@link VfxDrawGeometry#playerHitboxRelativeX}). It is additive center-packed and is appended
     * LAST after the death screen floaty effect.
     * The newest (F25) member is the {@code vfx-stance-aura} {@code StanceChangeAbsorptionParticle} —
     * the seam's SECOND non-deterministic native effect and its first MULTI-DRAW kind: its native
     * {@code render} draws the SAME static {@code ImageMaster.WOBBLY_ORB_VFX} {@code Texture} TWICE
     * (two additive shape-C fixed-rect draws sharing offset {@code x - 16f, y - 16f}, origin
     * {@code 16f, 16f}, size {@code 32f, 32f}, src {@code 0, 0, 32, 32} and rotation
     * {@code rotation - 200f}), consuming FOUR {@code MathUtils.random(...)} values in order — two
     * per pass, each multiplying the pass's scaleX then scaleY ({@code [0.5f, 2.0f]} for pass 0 and
     * {@code [0.6f, 2.5f]} for pass 1). The seam gained a MULTI-DRAW RNG-REPLAY capability
     * ({@link VfxDrawGeometry#drawPassRandomRanges}), whose ordered per-pass ranges the renderer
     * replays exactly (snapshotting the shared RNG once and restoring it on any post-consumption
     * fail-open). Its {@code update()} consumes no RNG. It is appended LAST after
     * {@code WrathStanceChangeParticle}; both {@code vfx-stance-aura} non-deterministic paths are now
     * claimed.
     * The two newest (F27) members are the {@code vfx-combat} {@code WaterSplashParticleEffect} and
     * {@code BuffParticleEffect}. {@code WaterSplashParticleEffect} is an AMBIENT center-packed
     * {@code AtlasRegion} member that introduces ONE new pure rule — its native draw scale is
     * anisotropic: {@code scaleY = scale * 0.54f} while {@code scaleX = scale} — so
     * {@link VfxDrawGeometry#params} gained a trailing {@code scaleYMultiplier} scalar (defaulting
     * {@code 1f} for every pre-existing kind). {@code BuffParticleEffect} is an ADDITIVE member with a
     * new pure POSITION/ORIGIN rule: its draw position is {@code (x - packedWidth/2f,
     * y - packedHeight/2f)} and its origin is the region's own {@code (offsetX, offsetY)} (NOT
     * {@code packed/2}). Both consume their inherited {@code rotation} field and are appended LAST in
     * that order. No new patch/bridge/console wiring; the default-off gate and per-instance token
     * semantics are unchanged.
     * The two newest (F28) members are the {@code vfx-scene-world} {@code BottomFogEffect} (boolean
     * constructor) and the {@code vfx-combat} {@code GiantFireEffect} (NO-ARG constructor), appended
     * LAST in that order. {@code BottomFogEffect} is an AMBIENT center-packed {@code AtlasRegion}
     * member that reuses the F22 per-instance MIRROR with NO new rule — identical in shape to
     * {@code SpookierChestEffect}/{@code CampfireSleepScreenCoverEffect}, carrying its own
     * {@code flipX} and {@code flipY} (so {@link VfxDrawGeometry#usesInstanceMirrorX} AND
     * {@link VfxDrawGeometry#usesInstanceMirrorY} include it). {@code GiantFireEffect} is an ADDITIVE
     * center-packed {@code AtlasRegion} member with a per-instance horizontal {@code flipX} mirror
     * (only {@link VfxDrawGeometry#usesInstanceMirrorX}, NOT the vertical one) and introduces ONE new
     * pure rule — its uniform draw scale is {@code scale * Settings.scale} on BOTH axes, modeled by
     * {@link VfxDrawGeometry#uniformScaleMultiplier} (which returns the supplied settings scale for
     * {@code GIANT_FIRE} and {@code 1f} for every other kind) and composed with the F27
     * {@code scaleYMultiplier} in the shared center-packed {@code params} branch. Its {@code delayTimer}
     * is used only by {@code update()}, NOT by {@code render} (no render guard). No new
     * patch/bridge/console wiring; the default-off gate and per-instance token semantics are
     * unchanged.
     * The newest (F29) member is the {@code vfx-misc-root} {@code TorchHeadFireEffect} (fields
     * {@code Texture img}, {@code float x, y, vX, vY} plus {@code boolean flippedX}; inherited
     * {@code scale}/{@code color}; ctor {@code (float, float)}). Its native {@code render} is
     * {@code setBlendFunction(770, 1); setColor(color); sb.draw(img, x - 64f, y - 64f, 64f, 64f,
     * 128f, 128f, scale * 1.2f, scale, 0f, 0, 0, 128, 128, flippedX, false);
     * setBlendFunction(770, 771)} — i.e. shape-C fixed rect, ADDITIVE, reusing the
     * {@code GlowyFireEyesEffect} rect (offset/origin 64, size 128, src 0,0,128,128) with a hardcoded
     * rotation {@code 0f} and the effect's own {@code flippedX} horizontal flip (vertical always
     * {@code false}), but with ONE new pure rule: an ASYMMETRIC X scale ({@code scaleX = scale *
     * 1.2f}, {@code scaleY = scale}). It is appended LAST and adds no new patch/bridge/console wiring;
     * the default-off gate and per-instance token semantics are unchanged.
     * The newest (F30) member is the {@code vfx-misc-root} {@code CardTrailEffect}. Its native
     * {@code render} is {@code setBlendFunction(770, 1); setColor(color); sb.draw(img, x, y, 6f, 6f,
     * 12f, 12f, scale, scale, 0f); setBlendFunction(770, 771)} — the img
     * ({@code TextureAtlas.AtlasRegion}) path with ONE new pure rule: a fixed ORIGIN {@code (6f, 6f)}
     * and fixed SIZE {@code (12f, 12f)} regardless of the region's packed size, with rotation
     * hardcoded {@code 0f} (it does not redeclare {@code rotation} — it inherits
     * {@code AbstractGameEffect.rotation} — but its native draw ignores the field). It is ADDITIVE, its
     * {@code img} is a
     * {@code private static} field (resolved like {@code ExhaustPileParticle}), and it has a NO-ARG
     * constructor. Appended LAST; no new patch/bridge/console wiring; the default-off gate and
     * per-instance token semantics are unchanged.
     * The newest (NRO-04 B01) member is the {@code vfx-combat} {@code FlyingOrbEffect} (fields
     * {@code TextureAtlas.AtlasRegion img}, {@code Vector2[] points}, {@code Vector2 pos/target},
     * {@code float rotation} — its OWN field, not inherited — plus the inherited {@code color}; it has
     * NO {@code x}/{@code y}/{@code scale} field and its static {@code img} is set to
     * {@code ImageMaster.GLOW_SPARK_2} in its {@code (float, float)} ctor). It is the seam's FIRST
     * VARIABLE-LENGTH MULTI-DRAW kind — its draw count and each draw position come from the host
     * {@code points[]} array, not a fixed pass list — and the seam's first BOOLEAN {@code isDone}
     * draw guard: its native {@code render} returns immediately when {@code isDone} is true. It draws
     * {@code points[index]} for {@code index} from {@code points.length - 1} DOWN TO {@code 1} (index
     * {@code 0} is NEVER drawn), center-packed (native POSITION offset uses INTEGER division of the
     * packed size, {@code x - (packed/2)}, while the ORIGIN uses FLOAT division, {@code packed/2f};
     * they differ by {@code 0.5} for an odd region) with a UNIFORM scale that
     * starts at {@code Settings.scale * 1.5f} and is multiplied by {@code 0.975f} after each DRAWN
     * point, under the additive blend ({@code 770/1} before, {@code 770/771} after), consuming no RNG.
     * A freshly lab-spawned instance's ctor allocates {@code points = new Vector2[60]} (a length-60
     * array of nulls that {@code update()} fills), so it draws nothing until {@code update()} runs.
     * Appended LAST after {@code CardTrailEffect};
     * no new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
     * unchanged.
     * The newest (NRO-04 B02) member is the {@code vfx-combat} {@code FlickCoinEffect} (fields
     * {@code float sX, sY, cX, cY, dX, dY, yOffset, bounceHeight, rotation}, {@code boolean
     * playedSfx}, {@code float sparkleTimer}, a static {@code TextureAtlas.AtlasRegion img} set in
     * the ctor from {@code ImageMaster.vfxAtlas.findRegion("combat/empowerCircle1")}, plus the
     * inherited {@code color} and {@code scale}). It has NO {@code x}/{@code y} field: its native
     * draw POSITION is {@code (cX - img.packedWidth / 2, cY - img.packedHeight / 2 + yOffset)} (the
     * half uses native INTEGER division of the packed size) while its ORIGIN uses native FLOAT
     * division ({@code packed/2f}; the two differ by {@code 0.5} for an odd region), and its draw
     * scale is ANISOTROPIC ({@code scaleX = scale * 0.7f}, {@code scaleY = scale * 0.4f}), ADDITIVE,
     * with the effect's own {@code rotation} field and draw color. Its single draw has NO
     * {@code isDone} guard. Appended LAST after {@code FlyingOrbEffect};
     * no new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
     * unchanged.
     * The newest (NRO-04 B03) member is the {@code vfx-combat} {@code HealPanelEffect} (fields
     * {@code float x}, a STATIC {@code Texture img} loaded in the ctor from
     * {@code ImageMaster.loadImage("images/ui/topPanel/panel_heart_white.png")}, plus the inherited
     * {@code color}/{@code scale}/{@code rotation}; ctor {@code (float x)}). Its native {@code render}
     * is {@code setColor(color); setBlendFunction(770, 1); sb.draw(img, x - 32f + 32f *
     * Settings.scale, Settings.HEIGHT - 32f * Settings.scale - 32f, 32f, 32f, 64f, 64f, scale, scale,
     * rotation, 0, 0, 64, 64, false, false); setBlendFunction(770, 771)} — a bare-{@code Texture}
     * fixed-rect kind whose position depends on {@code Settings.HEIGHT} (panel space) and
     * {@code Settings.scale}, with a fixed {@code (0,0,64,64)} source rect, a fixed {@code (32,32)}
     * origin and a fixed {@code 64&times;64} size, a uniform {@code scale}, the field
     * {@code rotation}, and the effect's own {@code color} (CHARTREUSE with animated alpha). It is
     * ADDITIVE. Appended LAST after {@code FlickCoinEffect}; no new patch/bridge/console wiring; the
     * default-off gate and per-instance token semantics are unchanged.
     * The newest (NRO-04 B04) member is the {@code vfx-combat} {@code PingHpEffect} (field
     * {@code float x}, plus the inherited {@code color}/{@code scale}/{@code rotation}; no instance
     * {@code img} field — its draw reads the STATIC {@code ImageMaster.TP_HP} {@code Texture} at draw
     * time). Its native {@code render} is {@code setColor(color); setBlendFunction(770, 1);
     * sb.draw(ImageMaster.TP_HP, x - 32f + 32f * Settings.scale,
     * Settings.HEIGHT - 32f * Settings.scale - 32f, 32f, 32f, 64f, 64f,
     * scale * Settings.scale, scale * Settings.scale, rotation, 0, 0, 64, 64, false, false);
     * setBlendFunction(770, 771)} — the HealPanel analogue: a bare-{@code Texture} fixed-rect kind
     * (fixed {@code (0,0,64,64)} src rect, fixed {@code (32,32)} origin and fixed {@code 64&times;64}
     * size) with a PANEL-SPACE position depending on {@code Settings.HEIGHT} and
     * {@code Settings.scale}, the field {@code rotation} and the effect's own color (yellow, animated
     * alpha). Its ONE difference from {@code HealPanelEffect} is the uniform draw scale: BOTH axes are
     * {@code scale * Settings.scale} (HealPanel used plain {@code scale}). It is ADDITIVE. Appended
     * LAST after {@code HealPanelEffect}; no new patch/bridge/console wiring; the default-off gate and
     * per-instance token semantics are unchanged.
     * The newest (NRO-04 B05) member is the {@code vfx-misc-root} {@code RewardGlowEffect} (fields
     * {@code float scale, x, y, angle} plus the inherited {@code color}; no instance {@code img}).
     * Its SINGLE-ARG {@code render(SpriteBatch)} — the overload the container claim seam TARGETS and
     * the only one it could reach — is
     * {@code setColor(color); setBlendFunction(770, 1); sb.draw(ImageMaster.REWARD_SCREEN_ITEM,
     * x - 232f, y - 49f, 232f, 49f, 464f, 98f, Settings.xScale, scale + Settings.scale * 0.05f, 0f,
     * 0, 0, 464, 98, false, false); setBlendFunction(770, 771)} — a bare STATIC {@code Texture}
     * reward-panel kind with a FIXED {@code (0,0,464,98)} source rect, a FIXED {@code (232f,49f)}
     * origin and a FIXED {@code 464&times;98} size, an ANISOTROPIC scale
     * ({@code scaleX = Settings.xScale}, INDEPENDENT of the effect's own {@code scale}, and
     * {@code scaleY = scale + Settings.scale * 0.05f}), a hardcoded rotation {@code 0f} (the
     * {@code angle} field is NOT used by this overload), and the effect's own {@code color}. It is
     * ADDITIVE. The class's OTHER overload {@code render(SpriteBatch, Color)} is a DIFFERENT draw
     * (the static {@code ImageMaster.WHITE_SQUARE_IMG}, {@code x-32,y-32}, origin/size 32/64,
     * rotation {@code angle}, scale {@code scale*Settings.scale/2f}); it is NOT reachable through the
     * container claim seam (the seam patches only the single-arg {@code render(SpriteBatch)} and the
     * {@code (sb,float,float)} overload), so it is deliberately NOT claimed and stays native.
     *
     * <p><b>Production reach (B05 boundary).</b> The claim seam's only effect observer instruments
     * {@code AbstractDungeon.render}'s direct {@code AbstractGameEffect.render} call sites
     * ({@code TransientEffectContainerPatches}). {@code RewardGlowEffect} is constructed only in
     * {@code com.megacrit.cardcrawl.rewards.RewardItem}, whose {@code render(SpriteBatch)} iterates
     * its own private {@code effects} list calling {@code AbstractGameEffect.render(SpriteBatch)} —
     * reached via {@code CombatRewardScreen.render}, NOT through {@code AbstractDungeon.render}. So
     * the real reward-screen {@code RewardGlow} instances are NOT yet observed or claimed; on-device
     * B05 is reachable only via the lab spawn into the {@code AbstractDungeon} effect queues, and the
     * parity/claim path is unit-verified. A real reward-screen claim requires instrumenting
     * {@code RewardItem.render}'s {@code AbstractGameEffect.render} call site (a separate boundary
     * slice tracked as B05b), and until then the seam fails open to native there.
     *
     * <p>The newest (NRO-04 B06) member is the {@code vfx-misc-root} {@code MapCircleEffect} (fields
     * {@code public static Texture img} — set in the ctor to {@code ImageMaster.MAP_CIRCLE_1} and
     * SWAPPED by {@code update()} through {@code MAP_CIRCLE_5}/{@code MAP_CIRCLE_4}/
     * {@code MAP_CIRCLE_3}/{@code MAP_CIRCLE_2}, so there is NO instance {@code img} — plus
     * {@code private float x}, {@code private float y} and the inherited {@code scale}/
     * {@code rotation}; ctor {@code (float x, float y, float rotation)}). Its native {@code render}
     * is {@code sb.setColor(new Color(0.09f, 0.13f, 0.17f, 1f)); sb.draw(img, x - 96f, y - 96f, 96f,
     * 96f, 192f, 192f, scale, scale, rotation, 0, 0, 192, 192, false, false)} — a bare static
     * {@code Texture} fixed-rect kind with a FIXED {@code (0,0,192,192)} source rect, a FIXED
     * {@code (96f,96f)} origin and a FIXED {@code 192&times;192} size, position {@code (x - 96f,
     * y - 96f)}, the uniform {@code scale}, the field {@code rotation}, NO {@code setBlendFunction}
     * (AMBIENT blend), and — uniquely — a HARDCODED draw color {@code (0.09f, 0.13f, 0.17f, 1f)}
     * that IGNORES the effect's own color field. Appended LAST after {@code RewardGlowEffect}; no
     * new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
     * unchanged.
     *
     * <p><b>Production reach (B06 boundary).</b> The claim seam's only effect observer instruments
     * {@code AbstractDungeon.render}'s direct {@code AbstractGameEffect.render} call sites
     * ({@code TransientEffectContainerPatches}). {@code MapCircleEffect} is owned by the MAP screen
     * ({@code MapRoomNode}/{@code DungeonMapScreen}), whose effect loop the seam does NOT instrument,
     * so real map-screen {@code MapCircleEffect} instances are NOT yet observed or claimed. On-device
     * B06 is reachable only via the lab spawn into the {@code AbstractDungeon} effect queues, and the
     * parity/claim path is unit-verified; on the map screen the seam fails open to native. Claiming a
     * real map-screen instance requires instrumenting the map-screen effect render call site (a
     * separate boundary slice tracked as B06b).
     *
     * <p>The newest (NRO-04 B07) member is the {@code vfx-misc-root} {@code SpotlightEffect}
     * (public NO-ARG ctor, which sets {@code duration = 3f} and {@code color = new Color(1f, 1f,
     * 0.8f, 0.5f)}): a FULL-SCREEN bare static-{@code Texture} kind with NO own per-effect geometry
     * field (no {@code x}/{@code y}/{@code scale}/{@code rotation}; the native {@code render}
     * consumes only {@code color} — the inherited {@code scale}/{@code rotation} are unused and not
     * required). Its
     * native {@code render} is {@code setColor(color); setBlendFunction(770, 1);
     * sb.draw(ImageMaster.SPOTLIGHT_VFX, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
     * setBlendFunction(770, 771)} — ADDITIVE, the static {@code ImageMaster.SPOTLIGHT_VFX}
     * {@code Texture}, position {@code (0f, 0f)}, size {@code Settings.WIDTH x Settings.HEIGHT}, NO
     * origin/rotation/scale, and draw color = the effect's own {@code color}.
     * <b>PRODUCTION REACH (positive).</b> Unlike {@code RewardGlowEffect} (B05) and
     * {@code MapCircleEffect} (B06), {@code SpotlightEffect} IS constructed by
     * {@code com.megacrit.cardcrawl.vfx.combat.GrandFinalEffect}, which adds it to
     * {@code AbstractDungeon.effectsQueue}; it is therefore rendered by the {@code AbstractDungeon
     * .render} effect loop the claim seam instruments
     * ({@code TransientEffectContainerPatches}), so the seam DOES reach a real instance. It is also
     * lab-spawnable via the {@code "spotlight"} alias. Appended LAST after {@code MapCircleEffect};
     * no new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
     * unchanged.
     */
    public static List<String> supportedClasses() {
        return SUPPORTED_CLASSES;
    }
}

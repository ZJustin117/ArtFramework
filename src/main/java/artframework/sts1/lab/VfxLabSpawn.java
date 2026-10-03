package artframework.sts1.lab;

import artframework.sts1.render.VfxClaimPolicy;
import artframework.sts1.render.VfxDrawGeometry;
import artframework.sts1.render.VfxInitContract;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ConeEffect;
import com.megacrit.cardcrawl.vfx.CardTrailEffect;
import com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustPileParticle;
import com.megacrit.cardcrawl.vfx.FallingDustEffect;
import com.megacrit.cardcrawl.vfx.FireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.FlameBallParticleEffect;
import com.megacrit.cardcrawl.vfx.GenericSmokeEffect;
import com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect;
import com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect;
import com.megacrit.cardcrawl.vfx.MapCircleEffect;
import com.megacrit.cardcrawl.vfx.NemesisFireParticle;
import com.megacrit.cardcrawl.vfx.DamageHeartEffect;
import com.megacrit.cardcrawl.vfx.ShineLinesEffect;
import com.megacrit.cardcrawl.vfx.SpotlightEffect;
import com.megacrit.cardcrawl.vfx.TorchHeadFireEffect;
import com.megacrit.cardcrawl.vfx.WarningSignEffect;
import com.megacrit.cardcrawl.vfx.RewardGlowEffect;
import com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect;
import com.megacrit.cardcrawl.vfx.combat.BuffParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect;
import com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect;
import com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.EntangleEffect;
import com.megacrit.cardcrawl.vfx.combat.FallingIceEffect;
import com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.FlickCoinEffect;
import com.megacrit.cardcrawl.vfx.combat.FlyingOrbEffect;
import com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect;
import com.megacrit.cardcrawl.vfx.combat.GiantFireEffect;
import com.megacrit.cardcrawl.vfx.combat.HealPanelEffect;
import com.megacrit.cardcrawl.vfx.combat.IceShatterEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.PingHpEffect;
import com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect;
import com.megacrit.cardcrawl.vfx.combat.StunStarEffect;
import com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.WaterSplashParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.WebParticleEffect;
import com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect;
import com.megacrit.cardcrawl.vfx.scene.BottomFogEffect;
import com.megacrit.cardcrawl.vfx.scene.DustEffect;
import com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect;
import com.megacrit.cardcrawl.vfx.campfire.CampfireRecallEffect;
import com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect;
import com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect;
import com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect;
import com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect;
import com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect;
import com.megacrit.cardcrawl.vfx.stance.DivinityParticleEffect;
import com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
import com.megacrit.cardcrawl.vfx.stance.StanceChangeAbsorptionParticle;
import com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect;
import com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle;

import java.util.List;
import java.util.function.Predicate;

/**
 * Lab/dev helper that queues native transient effects from the family-neutral per-instance claim
 * seam (current members are the {@code vfx-stance-aura} FQNs plus the cross-family
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code TorchParticleLEffect}/{@code TorchParticleXLEffect}/{@code CeilingDustCloudEffect}, the
 * {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}/{@code GhostlyWeakFireEffect}/
 * {@code GenericSmokeEffect}/{@code ExhaustBlurEffect}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}, plus the four
 * bare-{@code Texture} members {@code ShieldParticleEffect}/{@code DebuffParticleEffect} (root) and
 * {@code IceShatterEffect}/{@code WebParticleEffect} ({@code vfx-combat}), and the four newest
 * members {@code EntangleEffect}/{@code BlockImpactLineEffect}/{@code UnknownParticleEffect}
 * ({@code vfx-combat}) and {@code ExhaustPileParticle} (root), and the five newest
 * {@code vfx-combat} members {@code FlameParticleEffect}/{@code LightningOrbActivateEffect}/
 * {@code DamageImpactBlurEffect}/{@code DamageImpactLineEffect}/{@code DarkOrbPassiveEffect}, and the
 * three newest members {@code WarningSignEffect}/{@code FallingDustEffect} (root) and
 * {@code StunStarEffect} ({@code vfx-combat})}, plus the three newest members
 * {@code LightningEffect} ({@code vfx-combat}), {@code FlameBallParticleEffect}, and
 * {@code ShineLinesEffect} (root), plus the three newest members
 * {@code TorchParticleMEffect}/{@code TorchParticleSEffect}/{@code DustEffect}
 * ({@code vfx-scene-world}; {@code DustEffect} uses its NO-ARG constructor), plus the two newest
 * bare-{@code Texture} members {@code LightningOrbPassiveEffect} ({@code vfx-combat}) and
 * {@code GlowyFireEyesEffect} (root) — the first kinds on the shape-C path to pass per-instance flip
 * flags — plus the two newest members {@code FlyingSpikeEffect} ({@code vfx-combat}; additive
 * center-packed with no new rule, its {@code vX}/{@code vY} update-only) and {@code ConeEffect}
 * (root; ambient center-packed with the {@code originX = 0f} + {@code scale * 1.1f} rule, NO-ARG
 * constructor), plus the two newest members {@code FallingIceEffect} ({@code vfx-combat}; additive
 * shape-C fixed rect guarded natively by {@code if (waitTimer < 0f)}) and {@code DamageHeartEffect}
 * (root; ambient center-packed guarded natively by {@code if (delayTimer < 0f)}), plus the two newest
 * members {@code SpookyChestEffect} (root; ambient center-packed with per-instance {@code flipX} and
 * {@code flipY} mirror booleans, NO-ARG constructor) and {@code IroncladVictoryFlameEffect} (root;
 * ambient center-packed with a per-instance {@code flipX} mirror boolean, NO-ARG constructor), plus
 * the three newest members {@code SpookierChestEffect} (root; ambient center-packed with
 * {@code flipX}+{@code flipY} mirror booleans, NO-ARG constructor),
 * {@code CampfireSleepScreenCoverEffect} ({@code vfx-campfire-rest}; ambient center-packed with
 * {@code flipX}+{@code flipY} mirror booleans, NO-ARG constructor), and
 * {@code DeathScreenFloatyEffect} (root; ambient center-packed without flip flags, NO-ARG
 * constructor), plus the newest member {@code WrathStanceChangeParticle} (root; additive
 * center-packed, player-hitbox-relative x, two {@code MathUtils.random} draws, guard
 * {@code delayTimer <= 0f}; ctor {@code (float)}), plus the newest member
 * {@code StanceChangeAbsorptionParticle} (root; additive, the seam's FIRST MULTI-DRAW kind — two
 * draws of the static {@code ImageMaster.WOBBLY_ORB_VFX} Texture with four {@code MathUtils.random}
 * draws in order; ctor {@code (Color, float, float)}) and the two newest members
 * {@code WaterSplashParticleEffect} (root; ambient center-packed with a new anisotropic
 * {@code scaleY = scale * 0.54f} rule; ctor {@code (float, float)}) and {@code BuffParticleEffect}
 * (root; additive with a new position/origin rule — position offset by half the packed footprint,
 * origin from the region's own offsets; ctor {@code (float, float)}), plus the two newest members
 * {@code BottomFogEffect} ({@code vfx-scene-world}; ambient center-packed reusing the F22 mirror with
 * flipX+flipY, boolean ctor) and {@code GiantFireEffect} ({@code vfx-combat}; additive center-packed
 * with a new uniform-scale
 * {@code *Settings.scale} rule and a flipX-only mirror, NO-ARG ctor), plus the newest (F29) member
 * {@code TorchHeadFireEffect} (root; additive shape-C reusing the {@code GlowyFireEyesEffect} rect
 * with a new asymmetric {@code scaleX = scale * 1.2f} rule and a {@code flippedX} flip; ctor
 * {@code (float, float)}), plus the newest (F30) member {@code CardTrailEffect} (root; additive img
 * path with a new fixed-origin/size rule {@code 6,6}/{@code 12,12} independent of the packed region;
 * static {@code img}; NO-ARG ctor), plus the newest (NRO-04 B01) member {@code FlyingOrbEffect}
 * ({@code vfx-combat}; the seam's FIRST VARIABLE-LENGTH MULTI-DRAW kind — additive, it draws one
 * center-packed sprite per non-null {@code points[index]} for {@code index} from
 * {@code points.length-1} down to {@code 1} with a uniform scale starting at
 * {@code Settings.scale * 1.5f} and decaying by {@code 0.975f} per drawn point; the class has NO
 * x/y/scale field and is guarded by {@code if (isDone) return}; ctor {@code (float, float)}; the ctor
 * allocates {@code points = new Vector2[60]} (a length-60 array of nulls), so a freshly spawned
 * instance draws nothing until {@code update()} fills it) into the
 * live STS effect containers so
 * a device-side lab run can exercise the family without combat. The newest (NRO-04 B02) member is
 * the {@code vfx-combat} {@code FlickCoinEffect} (a single-draw img/AtlasRegion kind with a
 * dedicated {@code cX}/{@code cY}/{@code yOffset} position reader, an integer-half position offset,
 * a float-half origin, and an anisotropic scale 0.7f/0.4f; ctor {@code (float, float, float, float)};
 * its static {@code img} comes from {@code ImageMaster.vfxAtlas} and may be null off-game).
 * The newest (NRO-04 B03) member is the {@code vfx-combat} {@code HealPanelEffect} (a bare-{@code
 * Texture} fixed-rect kind whose STATIC {@code img} is loaded in the ctor via
 * {@code ImageMaster.loadImage(...)} and whose panel-space position depends on {@code Settings.HEIGHT}
 * and {@code Settings.scale}; ctor {@code (float x)}).
 * The newest (NRO-04 B04) member is the {@code vfx-combat} {@code PingHpEffect} (the HealPanel
 * analogue: a bare-{@code Texture} fixed-rect kind whose texture is the STATIC {@code ImageMaster.TP_HP}
 * resolved at draw time, whose panel-space position depends on {@code Settings.HEIGHT} and
 * {@code Settings.scale}, and whose uniform scale is {@code scale * Settings.scale}; ctor
 * {@code (float x)}).
 * The newest (NRO-04 B05) member is the {@code vfx-misc-root} {@code RewardGlowEffect} (a bare
 * static-{@code Texture} REWARD-PANEL kind whose texture is the STATIC
 * {@code ImageMaster.REWARD_SCREEN_ITEM} resolved at draw time, with a fixed {@code 464x98} rect, an
 * anisotropic {@code Settings.xScale}/{@code scale + Settings.scale*0.05f} scale and a hardcoded
 * zero rotation; ctor {@code (float x, float y)}).
 * The newest (NRO-04 B06) member is the {@code vfx-misc-root} {@code MapCircleEffect} (an ambient
 * bare STATIC-{@code Texture} kind whose img is the PUBLIC STATIC {@code MapCircleEffect.img}, set
 * in the ctor to {@code ImageMaster.MAP_CIRCLE_1} and SWAPPED by {@code update()} through
 * {@code MAP_CIRCLE_5}/{@code MAP_CIRCLE_4}/{@code MAP_CIRCLE_3}/{@code MAP_CIRCLE_2}; a fixed
 * {@code 192x192} rect at position {@code (x - 96f, y - 96f)} with the uniform scale and the field
 * rotation, and a HARDCODED draw color {@code (0.09f, 0.13f, 0.17f, 1f)} that ignores the effect's
 * own color; ctor {@code (float x, float y, float rotation)}).
 *
 * <p>The newest (NRO-04 B07) member is the {@code vfx-misc-root} {@code SpotlightEffect} — a
 * FULL-SCREEN bare static-{@code Texture} kind with NO own per-effect geometry field (no
 * {@code x}/{@code y}/{@code scale}/{@code rotation}; the native {@code render} consumes only
 * {@code color} — the inherited {@code scale}/{@code rotation} are unused and not required); it draws
 * the static {@code ImageMaster.SPOTLIGHT_VFX Texture} at {@code (0f, 0f)} over
 * {@code Settings.WIDTH x Settings.HEIGHT} additively with its own color, and has a public NO-ARG
 * ctor. It is production-reachable: {@code GrandFinalEffect} constructs it into
 * {@code AbstractDungeon.effectsQueue}.
 *
 * <p>The newest (NRO-04 B08) member is the {@code vfx-campfire} {@code CampfireRecallEffect} — a
 * FULL-SCREEN bare static-{@code Texture} kind with NO own per-effect geometry field (no
 * {@code x}/{@code y}/{@code scale}/{@code rotation}); it draws the static
 * {@code ImageMaster.WHITE_SQUARE_IMG Texture} at {@code (0f, 0f)} over
 * {@code Settings.WIDTH x Settings.HEIGHT} AMBIENTLY (no blend switch) with its own
 * {@code screenColor} field (NOT the inherited {@code color}), and has a public NO-ARG ctor. It is
 * production-reachable: {@code RecallOption} constructs it into {@code AbstractDungeon.effectList}.
 * The aliases are {@code "campfirerecall"}/{@code "recall"} -> {@code new CampfireRecallEffect()}.
 *
 * <p>This helper is fail-open by contract: no game context, an unknown kind, a non-positive count,
 * or a throwing container all yield {@code 0} rather than propagating. It never throws.
 */
public final class VfxLabSpawn {

    /** Upper bound on a single spawn request; larger counts are clamped. */
    public static final int MAX_COUNT = 20;

    private static final Queue DEFAULT_QUEUE = new DungeonQueue();

    private static volatile Queue queueOverride;

    private static final EffectFactory DEFAULT_FACTORY = new EffectFactory() {
        @Override
        public AbstractGameEffect create(String fqn) throws Exception {
            return construct(fqn);
        }
    };

    private static volatile EffectFactory factoryOverride;

    private VfxLabSpawn() {}

    /**
     * Maps a case-insensitive alias to the native FQN, or {@code null} when unknown. The FQNs reuse
     * the {@link VfxClaimPolicy} constants so the spawned family stays in sync with the claim set.
     */
    public static String classNameFor(String kind) {
        if (kind == null) {
            return null;
        }
        String value = kind.trim();
        if ("stance".equalsIgnoreCase(value) || "aura".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.STANCE_AURA_EFFECT;
        }
        if ("wrath".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WRATH_PARTICLE_EFFECT;
        }
        if ("divinity".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT;
        }
        if ("calm".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CALM_PARTICLE_EFFECT;
        }
        if ("divinitychange".equalsIgnoreCase(value) || "dsc".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE;
        }
        if ("flare".equalsIgnoreCase(value) || "lightflare".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_LIGHT_FLARE;
        }
        if ("flareM".equalsIgnoreCase(value) || "lightflareM".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_LIGHT_FLARE_M;
        }
        if ("flareL".equalsIgnoreCase(value) || "lightflareL".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_LIGHT_FLARE_L;
        }
        if ("torch".equalsIgnoreCase(value) || "torchparticle".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_TORCH_PARTICLE_L;
        }
        if ("flash".equalsIgnoreCase(value) || "flashatk".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLASH_ATK_IMG;
        }
        if ("fireburst".equalsIgnoreCase(value) || "fire".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FIRE_BURST;
        }
        if ("redfireburst".equalsIgnoreCase(value) || "redfire".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.RED_FIRE_BURST;
        }
        if ("smokeblur".equalsIgnoreCase(value) || "smoke".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SMOKE_BLUR;
        }
        if ("ceilingdust".equalsIgnoreCase(value) || "dust".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CEILING_DUST;
        }
        if ("nemesisfire".equalsIgnoreCase(value) || "nemesis".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.NEMESIS_FIRE;
        }
        if ("shield".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SHIELD_PARTICLE;
        }
        if ("debuff".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DEBUFF_PARTICLE;
        }
        if ("torchxl".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL;
        }
        if ("ghostlyfire".equalsIgnoreCase(value) || "ghostly".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.GHOSTLY_WEAK_FIRE;
        }
        if ("genericsmoke".equalsIgnoreCase(value) || "gsmoke".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.GENERIC_SMOKE;
        }
        if ("exhaustblur".equalsIgnoreCase(value) || "exhaust".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.EXHAUST_BLUR;
        }
        if ("iceshatter".equalsIgnoreCase(value) || "ice".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.ICE_SHATTER;
        }
        if ("web".equalsIgnoreCase(value) || "webparticle".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WEB_PARTICLE;
        }
        if ("entangle".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.ENTANGLE_EFFECT;
        }
        if ("blockline".equalsIgnoreCase(value) || "blockimpact".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.BLOCK_IMPACT_LINE;
        }
        if ("exhaustpile".equalsIgnoreCase(value) || "exhaustparticle".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.EXHAUST_PILE_PARTICLE;
        }
        if ("unknown".equalsIgnoreCase(value) || "unknownparticle".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.UNKNOWN_PARTICLE;
        }
        if ("flame".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLAME_PARTICLE;
        }
        if ("lightningorb".equalsIgnoreCase(value)
                || "lightningactivate".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE;
        }
        if ("damageblur".equalsIgnoreCase(value) || "dmgblur".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DAMAGE_IMPACT_BLUR;
        }
        if ("damageline".equalsIgnoreCase(value) || "dmgline".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DAMAGE_IMPACT_LINE;
        }
        if ("darkorb".equalsIgnoreCase(value) || "darkorbpassive".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DARK_ORB_PASSIVE;
        }
        if ("warning".equalsIgnoreCase(value) || "warningsign".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WARNING_SIGN;
        }
        if ("stunstar".equalsIgnoreCase(value) || "stun".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.STUN_STAR;
        }
        if ("fallingdust".equalsIgnoreCase(value) || "fdust".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FALLING_DUST;
        }
        if ("lightning".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.LIGHTNING_EFFECT;
        }
        if ("flameball".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLAME_BALL;
        }
        if ("shinelines".equalsIgnoreCase(value) || "shine".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SHINE_LINES;
        }
        if ("torchm".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.TORCH_PARTICLE_M;
        }
        if ("torchs".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.TORCH_PARTICLE_S;
        }
        if ("scenedust".equalsIgnoreCase(value) || "dusteffect".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SCENE_DUST;
        }
        if ("lightningorbpassive".equalsIgnoreCase(value) || "lop".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.LIGHTNING_ORB_PASSIVE;
        }
        if ("glowyfireeyes".equalsIgnoreCase(value) || "glowyeyes".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.GLOWY_FIRE_EYES;
        }
        if ("flyingspike".equalsIgnoreCase(value) || "spike".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLYING_SPIKE;
        }
        if ("cone".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CONE_EFFECT;
        }
        if ("fallingice".equalsIgnoreCase(value) || "icefall".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FALLING_ICE;
        }
        if ("damageheart".equalsIgnoreCase(value) || "heart".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DAMAGE_HEART;
        }
        if ("spookychest".equalsIgnoreCase(value) || "spooky".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SPOOKY_CHEST;
        }
        if ("victoryflame".equalsIgnoreCase(value)
                || "ironcladvictory".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.IRONCLAD_VICTORY_FLAME;
        }
        if ("spookierchest".equalsIgnoreCase(value) || "spookier".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SPOOKIER_CHEST;
        }
        if ("campfiresleepcover".equalsIgnoreCase(value)
                || "sleepcover".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CAMPFIRE_SLEEP_COVER;
        }
        if ("deathfloaty".equalsIgnoreCase(value) || "deathscreen".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.DEATH_SCREEN_FLOATY;
        }
        if ("wrathchange".equalsIgnoreCase(value) || "wrathstance".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WRATH_STANCE_CHANGE;
        }
        if ("absorption".equalsIgnoreCase(value) || "absorb".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.STANCE_CHANGE_ABSORPTION;
        }
        if ("watersplash".equalsIgnoreCase(value) || "splash".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.WATER_SPLASH;
        }
        if ("buffparticle".equalsIgnoreCase(value) || "buffp".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.BUFF_PARTICLE;
        }
        if ("bottomfog".equalsIgnoreCase(value) || "bfog".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.BOTTOM_FOG;
        }
        if ("giantfire".equalsIgnoreCase(value) || "gfire".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.GIANT_FIRE;
        }
        if ("torchheadfire".equalsIgnoreCase(value) || "torchhead".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.TORCH_HEAD_FIRE;
        }
        if ("cardtrail".equalsIgnoreCase(value) || "trail".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CARD_TRAIL;
        }
        if ("flyingorb".equalsIgnoreCase(value) || "orb".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLYING_ORB;
        }
        // NRO-04 B02: FlickCoinEffect's static img comes from ImageMaster.vfxAtlas and may be null
        // off-game; the construct path is behind the existing fail-open guard.
        if ("flickcoin".equalsIgnoreCase(value) || "coin".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.FLICK_COIN;
        }
        // NRO-04 B03: HealPanelEffect's static img is loaded in the ctor from ImageMaster and may be
        // null off-game, and its ctor reads Settings.scale; the construct path is behind the existing
        // fail-open guard.
        if ("healpanel".equalsIgnoreCase(value) || "heal".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.HEAL_PANEL;
        }
        // NRO-04 B04: PingHpEffect's texture is the STATIC ImageMaster.TP_HP resolved at draw time
        // (there is no instance img field) and may be null off-game, and its ctor reads Settings.scale;
        // the construct path is behind the existing fail-open guard.
        if ("pinghp".equalsIgnoreCase(value) || "ping".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.PING_HP;
        }
        // NRO-04 B05: RewardGlowEffect's texture is the STATIC ImageMaster.REWARD_SCREEN_ITEM
        // resolved at draw time (there is no instance img field) and may be null off-game, and its
        // ctor reads Settings.scale; the construct path is behind the existing fail-open guard.
        if ("rewardglow".equalsIgnoreCase(value) || "reward".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.REWARD_GLOW;
        }
        // NRO-04 B06: MapCircleEffect's img is the PUBLIC STATIC MapCircleEffect.img Texture, set in
        // the ctor to ImageMaster.MAP_CIRCLE_1 (and later SWAPPED by update() through
        // MAP_CIRCLE_5/4/3/2); it may be null off-game, so the construct path is behind the existing
        // fail-open guard.
        if ("mapcircle".equalsIgnoreCase(value) || "map".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.MAP_CIRCLE;
        }
        // NRO-04 B07: SpotlightEffect draws the STATIC ImageMaster.SPOTLIGHT_VFX Texture full-screen;
        // the native render consumes only its inherited color (the inherited scale/rotation are unused
        // and not required). That texture may be null off-game, so the construct path is behind the
        // existing fail-open guard.
        if ("spotlight".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.SPOTLIGHT;
        }
        // NRO-04 B08: CampfireRecallEffect draws the STATIC ImageMaster.WHITE_SQUARE_IMG Texture
        // full-screen ambiently; its native render reads its own screenColor field (NOT color). Its
        // NO-ARG ctor reads AbstractDungeon.fadeColor and may throw off-game, so the construct path is
        // behind the existing fail-open guard.
        if ("campfirerecall".equalsIgnoreCase(value) || "recall".equalsIgnoreCase(value)) {
            return VfxClaimPolicy.CAMPFIRE_RECALL;
        }
        return null;
    }

    /** Test seam: the effect-queue surface this helper writes to and scans. */
    public interface Queue {
        /** Appends the effect; returns true only when it was actually stored. */
        boolean add(AbstractGameEffect effect);

        /**
         * Marks every effect matching {@code match} as done (sets {@code isDone = true}) without
         * structurally mutating any container; returns the number of effects actually retired.
         */
        int retireMatching(Predicate<AbstractGameEffect> match);
    }

    /** Test seam: effect construction, so counting/clamping can be isolated from GL context. */
    public interface EffectFactory {
        AbstractGameEffect create(String fqn) throws Exception;
    }

    static void setQueueForTests(Queue queue) {
        queueOverride = queue;
    }

    static void setFactoryForTests(EffectFactory factory) {
        factoryOverride = factory;
    }

    static void resetForTests() {
        queueOverride = null;
        factoryOverride = null;
    }

    /**
     * Builds {@code count} effects of {@code kind} and appends them to the queue. Returns the number
     * actually queued; {@code 0} (never throws) for a null/unknown kind, {@code count <= 0}, a
     * missing game context, or a container that drops/throws on the append.
     */
    public static int spawn(String kind, int count) {
        String fqn = classNameFor(kind);
        if (fqn == null || count <= 0) {
            return 0;
        }
        int total = Math.min(count, MAX_COUNT);
        Queue queue = currentQueue();
        int queued = 0;
        for (int i = 0; i < total; i++) {
            try {
                AbstractGameEffect effect = currentFactory().create(fqn);
                if (effect == null) {
                    continue;
                }
                if (queue.add(effect)) {
                    queued++;
                }
            } catch (Throwable error) {
                // Fail open: a missing game context or an unconstructible effect yields no queue.
            }
        }
        return queued;
    }

    /**
     * Retires queued/active claimed effects (every {@link VfxClaimPolicy} FQN) by marking each
     * matching effect done ({@code isDone = true}) for the game-native reap in
     * {@code AbstractDungeon.update()}, instead of structurally removing it from the live lists;
     * returns how many were retired and never throws.
     */
    public static int clear() {
        try {
            return currentQueue().retireMatching(new Predicate<AbstractGameEffect>() {
                @Override
                public boolean test(AbstractGameEffect effect) {
                    if (effect == null) {
                        return false;
                    }
                    return VfxClaimPolicy.supports(effect.getClass().getName());
                }
            });
        } catch (Throwable error) {
            return 0;
        }
    }

    private static AbstractGameEffect construct(String fqn) throws Exception {
        if (VfxClaimPolicy.STANCE_AURA_EFFECT.equals(fqn)) {
            return new StanceAuraEffect("Wrath");
        }
        if (VfxClaimPolicy.WRATH_PARTICLE_EFFECT.equals(fqn)) {
            return new WrathParticleEffect();
        }
        if (VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT.equals(fqn)) {
            return new DivinityParticleEffect();
        }
        if (VfxClaimPolicy.CALM_PARTICLE_EFFECT.equals(fqn)) {
            return new CalmParticleEffect();
        }
        if (VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE.equals(fqn)) {
            // Safe lab defaults: white tint at a screen-center-ish point. Native settings/GL state
            // are not required to construct; any failure is caught by spawn's fail-open guard.
            return new DivinityStanceChangeParticle(Color.WHITE, 960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static imgs[] is null outside a live
            // game; the constructor may throw and spawn's fail-open guard drops it rather than
            // propagating.
            return new LightFlareSEffect(960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_M.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static imgs[] is allocated, but its
            // entries may be null off-game (img is chosen from imgs[] at construction time); any
            // failure is caught by spawn's fail-open guard rather than propagating.
            return new LightFlareMEffect(960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_LIGHT_FLARE_L.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static imgs[] is allocated, but its
            // entries may be null off-game (img is chosen from imgs[] at construction time); any
            // failure is caught by spawn's fail-open guard rather than propagating.
            return new LightFlareLEffect(960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_L.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img comes from a private getImg() at
            // construction time and may be null outside a live game; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new TorchParticleLEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLASH_ATK_IMG.equals(fqn)) {
            // Safe lab defaults: screen-center-ish point and a concrete attack effect. The static
            // ImageMaster regions may be null outside a live game; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect(
                    960f, 540f,
                    com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.BLUNT_HEAVY);
        }
        if (VfxClaimPolicy.FIRE_BURST.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The effect selects its region from
            // ImageMaster static art (TORCH_FIRE_1/2/3) at construction time, and that art may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new FireBurstParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.RED_FIRE_BURST.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The effect selects its region from
            // ImageMaster static art (FLAME_1/2/3) at construction time, and that art may be null
            // outside a live game; any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new RedFireBurstParticleEffect(960f, 540f, 0);
        }
        if (VfxClaimPolicy.SMOKE_BLUR.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new SmokeBlurEffect(960f, 540f);
        }
        if (VfxClaimPolicy.CEILING_DUST.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new CeilingDustCloudEffect(960f, 540f);
        }
        if (VfxClaimPolicy.NEMESIS_FIRE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new NemesisFireParticle(960f, 540f);
        }
        if (VfxClaimPolicy.SHIELD_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static ImageMaster.INTENT_DEFEND
            // texture may be null outside a live game; any failure is caught by spawn's fail-open
            // guard rather than propagating.
            return new com.megacrit.cardcrawl.vfx.ShieldParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.DEBUFF_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new com.megacrit.cardcrawl.vfx.DebuffParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img comes from the public static imgs[]
            // array at construction time and may be null outside a live game; any failure is caught
            // by spawn's fail-open guard rather than propagating.
            return new TorchParticleXLEffect(960f, 540f);
        }
        if (VfxClaimPolicy.GHOSTLY_WEAK_FIRE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img comes from a private getImg() at
            // construction time and may be null outside a live game; any failure is caught by
            // spawn's fail-open guard rather than propagating.
            return new GhostlyWeakFireEffect(960f, 540f);
        }
        if (VfxClaimPolicy.GENERIC_SMOKE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new GenericSmokeEffect(960f, 540f);
        }
        if (VfxClaimPolicy.EXHAUST_BLUR.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new ExhaustBlurEffect(960f, 540f);
        }
        if (VfxClaimPolicy.ICE_SHATTER.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // (FROST_ACTIVATE_VFX_1/2) and may be null outside a live game; any failure is caught by
            // spawn's fail-open guard rather than propagating.
            return new IceShatterEffect(960f, 540f);
        }
        if (VfxClaimPolicy.WEB_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static ImageMaster.WEB_VFX texture
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new WebParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.ENTANGLE_EFFECT.equals(fqn)) {
            // Safe lab defaults: the ctor is (float tX, float tY, float startX, float startY) — it
            // sets x=startX, y=startY (the start point) and tX=a1-32f, tY=a2-32f (the target), then
            // drifts dX/dY from the start. Passing the same center point for both the target and the
            // start keeps the effect centered at (960, 540) and drifting near-center; passing 0f for
            // the start would begin it at (0, 0). The static ImageMaster.WEB_VFX texture may be null
            // outside a live game; any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new EntangleEffect(960f, 540f, 960f, 540f);
        }
        if (VfxClaimPolicy.BLOCK_IMPACT_LINE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is picked randomly at construction
            // from ImageMaster-backed regions and may be null outside a live game; any failure is
            // caught by spawn's fail-open guard rather than propagating.
            return new BlockImpactLineEffect(960f, 540f);
        }
        if (VfxClaimPolicy.EXHAUST_PILE_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a private static
            // ImageMaster-backed region and may be null outside a live game; any failure is caught
            // by spawn's fail-open guard rather than propagating.
            return new ExhaustPileParticle(960f, 540f);
        }
        if (VfxClaimPolicy.UNKNOWN_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new UnknownParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLAME_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new FlameParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new LightningOrbActivateEffect(960f, 540f);
        }
        if (VfxClaimPolicy.DAMAGE_IMPACT_BLUR.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new DamageImpactBlurEffect(960f, 540f);
        }
        if (VfxClaimPolicy.DAMAGE_IMPACT_LINE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new DamageImpactLineEffect(960f, 540f);
        }
        if (VfxClaimPolicy.DARK_ORB_PASSIVE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new DarkOrbPassiveEffect(960f, 540f);
        }
        if (VfxClaimPolicy.WARNING_SIGN.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The static ImageMaster.WARNING_ICON_VFX
            // texture is resolved at draw time and may be null outside a live game; any failure is
            // caught by spawn's fail-open guard rather than propagating.
            return new WarningSignEffect(960f, 540f);
        }
        if (VfxClaimPolicy.STUN_STAR.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is the static ImageMaster.TINY_STAR
            // region and may be null outside a live game; any failure is caught by spawn's fail-open
            // guard rather than propagating.
            return new StunStarEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FALLING_DUST.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is picked at random at construction
            // from the static ImageMaster.DUST_* regions and may be null outside a live game; any
            // failure is caught by spawn's fail-open guard rather than propagating.
            return new FallingDustEffect(960f, 540f);
        }
        if (VfxClaimPolicy.LIGHTNING_EFFECT.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new LightningEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLAME_BALL.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new FlameBallParticleEffect(960f, 540f, 0);
        }
        if (VfxClaimPolicy.SHINE_LINES.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is a static ImageMaster region and
            // may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new ShineLinesEffect(960f, 540f);
        }
        if (VfxClaimPolicy.TORCH_PARTICLE_M.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img comes from a private getImg() at
            // construction time and may be null outside a live game; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new TorchParticleMEffect(960f, 540f);
        }
        if (VfxClaimPolicy.TORCH_PARTICLE_S.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img comes from a private getImg() at
            // construction time and may be null outside a live game; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new TorchParticleSEffect(960f, 540f);
        }
        if (VfxClaimPolicy.SCENE_DUST.equals(fqn)) {
            // Safe lab defaults: DustEffect has a NO-ARG constructor that randomizes its own x/y
            // (and img from a private getImg()); img may be null outside a live game, so any failure
            // is caught by spawn's fail-open guard rather than propagating.
            return new DustEffect();
        }
        if (VfxClaimPolicy.LIGHTNING_ORB_PASSIVE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new LightningOrbPassiveEffect(960f, 540f);
        }
        if (VfxClaimPolicy.GLOWY_FIRE_EYES.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new GlowyFireEyesEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLYING_SPIKE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The ctor is
            // (float x, float y, float rotation, float vX, float vY, Color color) and it copies the
            // color's RGB with alpha 0; img comes from the static ImageMaster.THICK_3D_LINE region and
            // may be null outside a live game, so any failure is caught by spawn's fail-open guard
            // rather than propagating. vX/vY are update-only.
            return new FlyingSpikeEffect(960f, 540f, 0f, 0f, 1f, Color.WHITE);
        }
        if (VfxClaimPolicy.CONE_EFFECT.equals(fqn)) {
            // Safe lab defaults: ConeEffect has a NO-ARG constructor that randomizes its own img from
            // the static ImageMaster.CONE_1/2/3 regions and centers x/y from Settings; img may be null
            // outside a live game, so any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new ConeEffect();
        }
        if (VfxClaimPolicy.FALLING_ICE.equals(fqn)) {
            // Safe lab defaults: FallingIceEffect's ctor is (int frostCount, boolean damage) and
            // seeds its own x/y (from AbstractDungeon player/random positions) plus waitTimer; img
            // comes from the static ImageMaster FROST_* textures and may be null outside a live game.
            // A freshly constructed instance has waitTimer >= 0, so its native render is in the wait
            // phase and it legitimately declines-while-guarded; any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new FallingIceEffect(0, false);
        }
        if (VfxClaimPolicy.DAMAGE_HEART.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The ctor is (float x, float y,
            // float delayTimer, AttackEffect, int damage) and loadImage() picks its public AtlasRegion
            // img from ImageMaster art, which may be null outside a live game. A freshly constructed
            // instance has delayTimer 0f (not < 0f), so its native render is in the delay phase and it
            // legitimately declines-while-guarded; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new DamageHeartEffect(960f, 540f, 0f,
                    com.megacrit.cardcrawl.actions.AbstractGameAction.AttackEffect.BLUNT_HEAVY, 0);
        }
        if (VfxClaimPolicy.SPOOKY_CHEST.equals(fqn)) {
            // Safe lab defaults: SpookyChestEffect has a NO-ARG constructor that selects its own img
            // from ImageMaster static art (SMOKE_1/2/3) and randomizes x/y; img may be null outside a
            // live game, so any failure is caught by spawn's fail-open guard rather than propagating.
            return new SpookyChestEffect();
        }
        if (VfxClaimPolicy.IRONCLAD_VICTORY_FLAME.equals(fqn)) {
            // Safe lab defaults: IroncladVictoryFlameEffect has a NO-ARG constructor that selects its
            // own img from ImageMaster static art (FLAME_1/2/3) and sets x/y; img may be null outside
            // a live game, so any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new IroncladVictoryFlameEffect();
        }
        if (VfxClaimPolicy.SPOOKIER_CHEST.equals(fqn)) {
            // Safe lab defaults: SpookierChestEffect has a NO-ARG constructor that selects its own
            // img and randomizes x/y; img may be null outside a live game, so any failure is caught
            // by spawn's fail-open guard rather than propagating.
            return new SpookierChestEffect();
        }
        if (VfxClaimPolicy.CAMPFIRE_SLEEP_COVER.equals(fqn)) {
            // Safe lab defaults: CampfireSleepScreenCoverEffect has a NO-ARG constructor that selects
            // its own img from ImageMaster static art and randomizes x/y; img may be null outside a
            // live game, so any failure is caught by spawn's fail-open guard rather than propagating.
            return new CampfireSleepScreenCoverEffect();
        }
        if (VfxClaimPolicy.DEATH_SCREEN_FLOATY.equals(fqn)) {
            // Safe lab defaults: DeathScreenFloatyEffect has a NO-ARG constructor that selects its
            // own img from ImageMaster static art and sets x/y; img may be null outside a live game,
            // so any failure is caught by spawn's fail-open guard rather than propagating.
            return new DeathScreenFloatyEffect();
        }
        if (VfxClaimPolicy.WRATH_STANCE_CHANGE.equals(fqn)) {
            // Safe lab defaults: WrathStanceChangeParticle's ctor is (float delayTimer) and sets
            // img = ImageMaster.STRIKE_LINE; that static region may be null outside a live game, so
            // any failure is caught by spawn's fail-open guard rather than propagating. delayTimer 0f
            // means the guard `if (delayTimer > 0f) return` is satisfied (native draws).
            return new WrathStanceChangeParticle(0f);
        }
        if (VfxClaimPolicy.STANCE_CHANGE_ABSORPTION.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. StanceChangeAbsorptionParticle's ctor is
            // (Color, float, float) and draws the static ImageMaster.WOBBLY_ORB_VFX Texture (resolved
            // at draw time), which may be null off-game; any failure is caught by spawn's fail-open
            // guard rather than propagating.
            return new StanceChangeAbsorptionParticle(Color.WHITE, 960f, 540f);
        }
        if (VfxClaimPolicy.WATER_SPLASH.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new WaterSplashParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.BUFF_PARTICLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. img is ImageMaster-backed and may be
            // null outside a live game; any failure is caught by spawn's fail-open guard rather
            // than propagating.
            return new BuffParticleEffect(960f, 540f);
        }
        if (VfxClaimPolicy.BOTTOM_FOG.equals(fqn)) {
            // Safe lab defaults: BottomFogEffect's ctor is (boolean renderBehind); passing false draws
            // the fog behind the effect layer. Its per-instance flipX/flipY are randomized in the ctor
            // (MathUtils.randomBoolean()), and only its img (ImageMaster.SMOKE_1/2/3) is
            // ImageMaster-backed and may be null off-game. Any failure is caught by spawn's
            // fail-open guard rather than propagating.
            return new BottomFogEffect(false);
        }
        if (VfxClaimPolicy.GIANT_FIRE.equals(fqn)) {
            // Safe lab defaults: GiantFireEffect has a NO-ARG constructor that selects its own img
            // from ImageMaster static art and randomizes x/y; img may be null outside a live game, so
            // any failure is caught by spawn's fail-open guard rather than propagating.
            return new GiantFireEffect();
        }
        if (VfxClaimPolicy.TORCH_HEAD_FIRE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. The instance img is ImageMaster-backed
            // and may be null outside a live game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new TorchHeadFireEffect(960f, 540f);
        }
        if (VfxClaimPolicy.CARD_TRAIL.equals(fqn)) {
            // CardTrailEffect is a pooled Pool.Poolable effect: its no-arg constructor only selects
            // the static img, leaving the inherited color/x/y/scale unset. In production it is always
            // obtained then init(x, y) is called, which sets duration/startingDuration/x/y/color
            // (from AbstractDungeon.player.getCardTrailColor())/scale and isDone=false. The lab must
            // init too so color is non-null (the renderer requires a Color) and x/y match the lab
            // point. The method name and coordinates are the single-source-of-truth
            // VfxInitContract: if the contract ever stops claiming a post-constructor initializer
            // (or renames it), this path DECLINES the uninitialized instance (fail-open) rather than
            // presenting a null-color effect. init reads AbstractDungeon.player, which may be null
            // off-game, so any failure is caught by spawn's fail-open guard rather than propagating.
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.Kind.CARD_TRAIL;
            String initializer = VfxInitContract.initializerMethod(kind);
            float[] initArgs = VfxInitContract.initializerArgs(kind);
            if (!VfxInitContract.requiresRuntimeInit(kind)
                    || !"init".equals(initializer)
                    || initArgs == null
                    || initArgs.length < 2) {
                return null;
            }
            CardTrailEffect effect = new CardTrailEffect();
            effect.init(initArgs[0], initArgs[1]);
            return effect;
        }
        if (VfxClaimPolicy.FLYING_ORB.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point. FlyingOrbEffect's ctor is (float, float)
            // and reads AbstractDungeon.player.hb (which may be null off-game), and its static
            // img = ImageMaster.GLOW_SPARK_2 may be null outside a live game; any failure is caught by
            // spawn's fail-open guard rather than propagating. NOTE: the ctor allocates
            // points = new Vector2[60] (a length-60 array of nulls) and update() fills it, so a
            // freshly constructed instance draws nothing until update() runs — the D1 scenario spawns
            // it for observation/lifecycle, and the true draw path is covered by the unit tests with a
            // synthetic points array.
            return new FlyingOrbEffect(960f, 540f);
        }
        if (VfxClaimPolicy.FLICK_COIN.equals(fqn)) {
            // Safe lab defaults: the ctor is (float sX, float sY, float dX, float dY) and sets
            // cX=sX, cY=sY; passing the same center point for both keeps it centered. Its static img
            // comes from ImageMaster.vfxAtlas.findRegion("combat/empowerCircle1") and may be null
            // outside a live game; any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new FlickCoinEffect(960f, 540f, 960f, 540f);
        }
        if (VfxClaimPolicy.HEAL_PANEL.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish x (e.g. 960f). The ctor loads the static
            // ImageMaster Texture via ImageMaster.loadImage(...) and reads Settings.scale, either of
            // which may be unavailable off-game; any failure is caught by spawn's fail-open guard
            // rather than propagating.
            return new HealPanelEffect(960f);
        }
        if (VfxClaimPolicy.PING_HP.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish x (e.g. 960f). Its ctor is (float x) and reads
            // Settings.scale; its texture is the STATIC ImageMaster.TP_HP resolved at draw time, which
            // may be null off-game. Any failure is caught by spawn's fail-open guard rather than
            // propagating.
            return new PingHpEffect(960f);
        }
        if (VfxClaimPolicy.REWARD_GLOW.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point (e.g. 960f, 540f). Its ctor is
            // (float x, float y) and reads Settings.scale; its texture is the STATIC
            // ImageMaster.REWARD_SCREEN_ITEM resolved at draw time, which may be null off-game. Any
            // failure is caught by spawn's fail-open guard rather than propagating.
            return new RewardGlowEffect(960f, 540f);
        }
        if (VfxClaimPolicy.MAP_CIRCLE.equals(fqn)) {
            // Safe lab defaults: a screen-center-ish point (e.g. 960f, 540f) with rotation 0f. Its
            // ctor is (float x, float y, float rotation); its static img = ImageMaster.MAP_CIRCLE_1
            // may be null off-game (and update() later swaps it through MAP_CIRCLE_5/4/3/2). Any
            // failure is caught by spawn's fail-open guard rather than propagating.
            return new MapCircleEffect(960f, 540f, 0f);
        }
        if (VfxClaimPolicy.SPOTLIGHT.equals(fqn)) {
            // NRO-04 B07: SpotlightEffect has a NO-ARG ctor (duration 3f, color (1,1,0.8,0.5)) and
            // draws the static ImageMaster.SPOTLIGHT_VFX Texture full-screen; that texture may be null
            // off-game, so any failure is caught by spawn's fail-open guard rather than propagating.
            return new SpotlightEffect();
        }
        if (VfxClaimPolicy.CAMPFIRE_RECALL.equals(fqn)) {
            // NRO-04 B08: CampfireRecallEffect has a NO-ARG ctor that reads AbstractDungeon.fadeColor
            // into its screenColor field (alpha 0f) and draws the static
            // ImageMaster.WHITE_SQUARE_IMG Texture full-screen ambiently; the static texture may be
            // null and the ctor may throw off-game, so any failure is caught by spawn's fail-open
            // guard rather than propagating.
            return new CampfireRecallEffect();
        }
        return null;
    }

    private static Queue currentQueue() {
        Queue queue = queueOverride;
        return queue != null ? queue : DEFAULT_QUEUE;
    }

    private static EffectFactory currentFactory() {
        EffectFactory factory = factoryOverride;
        return factory != null ? factory : DEFAULT_FACTORY;
    }

    /** Default sink backed by the live {@link AbstractDungeon} effect containers. */
    private static final class DungeonQueue implements Queue {
        @Override
        public boolean add(AbstractGameEffect effect) {
            if (effect == null) {
                return false;
            }
            try {
                if (AbstractDungeon.effectsQueue != null) {
                    AbstractDungeon.effectsQueue.add(effect);
                    return true;
                }
            } catch (Throwable error) {
                // No game context: drop the append rather than throw.
            }
            return false;
        }

        @Override
        public int retireMatching(Predicate<AbstractGameEffect> match) {
            if (match == null) {
                return 0;
            }
            // Each container gets its own guard: a throw in one (including the field read itself)
            // must not skip the others.
            return retireSafe(0, match)
                    + retireSafe(1, match)
                    + retireSafe(2, match)
                    + retireSafe(3, match);
        }

        private static int retireSafe(int container, Predicate<AbstractGameEffect> match) {
            try {
                List<AbstractGameEffect> list;
                if (container == 0) {
                    list = AbstractDungeon.effectsQueue;
                } else if (container == 1) {
                    list = AbstractDungeon.effectList;
                } else if (container == 2) {
                    list = AbstractDungeon.topLevelEffectsQueue;
                } else {
                    list = AbstractDungeon.topLevelEffects;
                }
                return retireIn(list, match);
            } catch (Throwable error) {
                return 0;
            }
        }
    }

    /**
     * Marks matching effects done without structurally mutating the live list. Only a plain
     * {@code isDone} field write happens, so a concurrent render-thread iteration can never trip a
     * {@code modCount} check; {@code AbstractDungeon.update()} reaps the retired effects natively.
     */
    private static int retireIn(List<AbstractGameEffect> list, Predicate<AbstractGameEffect> match) {
        if (list == null) {
            return 0;
        }
        int retired = 0;
        for (AbstractGameEffect effect : list) {
            if (effect == null) {
                continue;
            }
            boolean matched;
            try {
                matched = match.test(effect);
            } catch (Throwable error) {
                matched = false;
            }
            if (matched) {
                effect.isDone = true;
                retired++;
            }
        }
        return retired;
    }
}

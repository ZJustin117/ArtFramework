package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1GdxAtlasRegions;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.lang.reflect.Field;

/**
 * STS1 host-side {@link VfxArtRenderer.Adapter} for the family-neutral per-instance transient-effect
 * claim seam (current members are the {@code vfx-stance-aura} FQNs plus the {@code vfx-scene-world}
 * {@code LightFlareSEffect}/{@code LightFlareMEffect}/{@code LightFlareLEffect}/
 * {@code TorchParticleLEffect}/{@code TorchParticleXLEffect}/{@code CeilingDustCloudEffect}, the
 * {@code vfx-misc-root}
 * {@code FireBurstParticleEffect}/{@code NemesisFireParticle}/{@code GhostlyWeakFireEffect}/
 * {@code GenericSmokeEffect}/{@code ExhaustBlurEffect}, and the {@code vfx-combat}
 * {@code FlashAtkImgEffect}/{@code RedFireBurstParticleEffect}/{@code SmokeBlurEffect}, plus the four
 * bare-{@code Texture} members {@code ShieldParticleEffect}, {@code DebuffParticleEffect},
 * {@code IceShatterEffect}, and {@code WebParticleEffect}, and the four newest members
 * {@code EntangleEffect} (reusing the Web static-texture config), {@code BlockImpactLineEffect} and
 * {@code ExhaustPileParticle} (ambient center-packed; the latter's {@code img} is a
 * {@code private static} field), {@code UnknownParticleEffect} (a new ambient fixed rect over
 * its own instance {@code Texture}), and the five newest {@code vfx-combat} members
 * {@code FlameParticleEffect}/{@code LightningOrbActivateEffect} (additive center-packed),
 * {@code DamageImpactBlurEffect}/{@code DamageImpactLineEffect} (ambient center-packed), and
 * {@code DarkOrbPassiveEffect} (a new additive fixed rect over its own instance
 * {@code Texture})), and the three newest members {@code WarningSignEffect} (a bare-{@code Texture}
 * fixed rect over the static {@code ImageMaster.WARNING_ICON_VFX} with a hardcoded
 * {@code Settings.scale * 2f} uniform scale), {@code StunStarEffect} (ambient center-packed with its
 * position shifted by its own {@code vX}/{@code vY}), and {@code FallingDustEffect} (ambient
 * center-packed with the region's own {@code offsetX}/{@code offsetY} as its origin), plus the three
 * newest members {@code LightningEffect} (additive center-packed with origin Y {@code 0f}),
 * {@code FlameBallParticleEffect} (additive center-packed with origin Y {@code packedHeight/2f +
 * 20f * Settings.scale}), and {@code ShineLinesEffect} (ambient center-packed; its native
 * {@code render} also guards
 * the draw with {@code if (!isDone)}), plus the three newest {@code vfx-scene-world} members
 * {@code TorchParticleMEffect}/{@code TorchParticleSEffect} (additive center-packed, no new rule;
 * their {@code vY} is update-only) and {@code DustEffect} (ambient center-packed, NO-ARG
 * constructor, reusing the region-offset origin of {@code FallingDustEffect}), plus the two newest
 * bare-{@code Texture} members {@code LightningOrbPassiveEffect} (additive, 122&times;122 fixed rect
 * consuming its {@code rotation} field and its own {@code flipX}/{@code flipY} booleans) and
 * {@code GlowyFireEyesEffect} (additive, 128&times;128 fixed rect with a hardcoded zero rotation and
 * its own {@code flippedX} horizontal flip) — the first kinds on the shape-C path to pass
 * per-instance flip flags; the two newest are the {@code vfx-combat} {@code FlyingSpikeEffect}
 * (additive center-packed, no new rule; its {@code vX}/{@code vY} are update-only) and the
 * {@code vfx-misc-root} {@code ConeEffect} (ambient center-packed with a NEW origin rule —
 * {@code originX = 0f} rather than {@code packedWidth/2f} — and a {@code scale * 1.1f} uniform
 * scale); and the two newest are the {@code vfx-combat} {@code FallingIceEffect} (additive
 * shape-C fixed rect, origin 48, size 96&times;96, src {@code 0,0,96,96}, x/y passthrough,
 * consuming its
 * {@code rotation} field and its own instance {@code Texture img}) and the {@code vfx-misc-root}
 * {@code DamageHeartEffect} (ambient center-packed with a public {@code AtlasRegion img}) — the
 * seam's first per-kind NATIVE DRAW GUARD kinds ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}),
 * whose {@code render} declines (draws nothing) whenever the guard field value blocks per that kind's
 * threshold ({@link VfxDrawGeometry#guardBlocks} — the source of truth: {@code NaN}/{@code +0f}/
 * positive block while negative finite values draw). The two newest members are the
 * {@code vfx-scene-world} {@code SpookyChestEffect}
 * (ambient center-packed, NO-ARG constructor, {@code flipX}+{@code flipY} mirror booleans) and
 * {@code IroncladVictoryFlameEffect} (ambient center-packed, NO-ARG constructor, {@code flipX}
 * mirror boolean) — the img path's first per-instance MIRROR kinds
 * ({@link VfxDrawGeometry#usesInstanceMirrorX}/{@link VfxDrawGeometry#usesInstanceMirrorY}), whose
 * mirror is reproduced as a UV swap on the canonical F15d region; {@code FlameParticleEffect} also
 * uses the mirror, resolving its F15 limitation. The three newest members reuse that same img path
 * with no new branch: {@code SpookierChestEffect} ({@code vfx-scene-world}) and
 * {@code CampfireSleepScreenCoverEffect} ({@code vfx-campfire-rest}; the first claimed member of
 * that family, a per-instance ambient center-packed sprite with a NO-ARG constructor) each
 * carry {@code flipX}+{@code flipY} and mirror the canonical region, while
 * {@code DeathScreenFloatyEffect} ({@code vfx-misc-root}) carries no flip fields and draws the
 * canonical region. All three require {@code rotation} and are ambient center-packed. The newest
 * (F24) member is the {@code vfx-stance-aura} {@code WrathStanceChangeParticle} — the seam's FIRST
 * non-deterministic native effect and its first PLAYER-HITBOX-RELATIVE and RNG-REPLAY kind: it is
 * additive center-packed, draws at {@code AbstractDungeon.player.hb.cX + x}
 * ({@link VfxDrawGeometry#playerHitboxRelativeX}; a missing player/hitbox fails open), consumes two
 * ordered {@code MathUtils.random(...)} values for scaleX/scaleY
 * ({@link VfxDrawGeometry#randomRanges}, pulled only once committed to the draw and never on a
 * fail-open), and guards its draw with {@code if (delayTimer > 0f) return}
 * ({@link VfxDrawGeometry#guardBlocks}, the generalized guard threshold).
 * The newest (F25) member is the {@code vfx-stance-aura} {@code StanceChangeAbsorptionParticle} —
 * the seam's FIRST MULTI-DRAW kind: additive, it draws the static {@code ImageMaster.WOBBLY_ORB_VFX}
 * {@code Texture} TWICE with the SAME shape-C fixed rect (offset/origin {@code 16f}, size
 * {@code 32f&times;32f}, src {@code 0,0,32,32}, rotation offset {@code -200f}) and consumes four
 * {@code MathUtils.random(...)} values in order, replayed via
 * {@link VfxDrawGeometry#drawPassRandomRanges} (the shared RNG is snapshotted once; a PRE-pass
 * fail-open restores it and fails open to native, while a POST-pass fail-open keeps the consumed
 * stream and claims the frame so native never double-draws — see {@link #renderTexture}). With it,
 * both {@code vfx-stance-aura} non-deterministic paths are claimed.
 * The two newest (F27) members are the {@code vfx-combat} {@code WaterSplashParticleEffect} and
 * {@code BuffParticleEffect}, both on the img ({@link TextureAtlas.AtlasRegion}) path and both
 * requiring the inherited {@code rotation} field. {@code WaterSplashParticleEffect} is AMBIENT
 * center-packed with an ANISOTROPIC draw scale ({@code scaleY = scale * 0.54f}, {@code scaleX =
 * scale}); the renderer passes {@code 1f} for the new {@link VfxDrawGeometry#params
 * scaleYMultiplier} tail parameter for every kind EXCEPT {@code WATER_SPLASH} (which passes
 * {@code 0.54f}), so every pre-existing kind's result is unchanged. {@code BuffParticleEffect} is
 * ADDITIVE with a new pure position/origin rule ({@code (x - packedWidth/2f, y - packedHeight/2f)}
 * with the region's own {@code (offsetX, offsetY)} as origin) resolved by its own
 * {@link VfxDrawGeometry#params} branch; the renderer already captures {@code regionOffsetX/Y} for
 * {@code FALLING_DUST}/{@code SCENE_DUST}, so no new capture path is needed. No new draw branch.
 *
 * <p>The two newest (F28) members, the {@code vfx-scene-world} {@code BottomFogEffect} (boolean
 * constructor; {@code flipX}+{@code flipY}) and the {@code vfx-combat} {@code GiantFireEffect}
 * (NO-ARG constructor; {@code flipX} only), also ride the existing img ({@link TextureAtlas.AtlasRegion})
 * path and both require the inherited {@code rotation} field. {@code BottomFogEffect} is AMBIENT
 * center-packed with NO new rule, reproducing its per-instance mirror as the F22 UV swap on the
 * canonical region (both {@link VfxDrawGeometry#usesInstanceMirrorX} and
 * {@link VfxDrawGeometry#usesInstanceMirrorY} report it). {@code GiantFireEffect} is ADDITIVE
 * center-packed with a per-instance horizontal mirror only and a NEW pure uniform-scale rule: the
 * center-packed {@link VfxDrawGeometry#params} branch composes
 * {@link VfxDrawGeometry#uniformScaleMultiplier}{@code (kind, Settings.scale)} ({@code Settings.scale}
 * for {@code GIANT_FIRE}, {@code 1f} otherwise) with the F27
 * {@link VfxDrawGeometry#params scaleYMultiplier} tail, so both axes are {@code scale * Settings.scale}
 * for {@code GIANT_FIRE} while every pre-existing kind is unchanged. No new draw branch.
 *
 * <p>The newest (F29) member, the {@code vfx-misc-root} {@code TorchHeadFireEffect} (ctor
 * {@code (float, float)}), also rides the existing {@code Texture} shape-C path. It reuses the
 * {@code GlowyFireEyesEffect} fixed rect exactly (offset/origin {@code 64}, size {@code 128&times;128},
 * src {@code 0,0,128,128}, hardcoded {@code 0f} rotation), is ADDITIVE, resolves its own instance
 * {@code Texture img}, requires NO {@code rotation} field, and passes its own {@code flippedX}
 * horizontal flip (vertical always {@code false}) — the flip field name is resolved by the
 * generalized {@link VfxDrawGeometry#usesTexturedFlipX}. Its ONE new pure rule is the ASYMMETRIC X
 * scale {@code scaleX = scale * 1.2f} with {@code scaleY = scale}, resolved in its own
 * {@link VfxDrawGeometry#params} branch. No new draw branch, fail-open/no-throw preserved.
 *
 * <p>The newest (F30) member, the {@code vfx-misc-root} {@code CardTrailEffect} (NO-ARG
 * constructor), also rides the existing img ({@link TextureAtlas.AtlasRegion}) path with no new draw
 * branch. It is ADDITIVE and resolves a {@code private static AtlasRegion img} (via the same
 * superclass-walking reader as {@code ExhaustPileParticle}); it does NOT redeclare a {@code rotation}
 * field, but inherits {@code AbstractGameEffect.rotation}, so the img-path reader always resolves a
 * value for it — its native draw nevertheless hardcodes rotation {@code 0f}, so the claimed draw
 * forces {@code 0f} regardless of that (ignored) field. Its
 * ONE new pure rule lives in its own {@link VfxDrawGeometry#params} branch: a fixed ORIGIN
 * ({@code 6f, 6f}) and fixed SIZE ({@code 12f, 12f}) INDEPENDENT of the region's packed size. It
 * draws the region's own source UVs (the 9-arg {@code TextureRegion} overload), which the canonical
 * region view already reproduces, so it is NOT a shape-C (bare {@code Texture}) kind; it is kept out
 * of {@link #readFields}' {@code rotation} requirement only DEFENSIVELY (the exemption is
 * behaviorally inert because the field is always present via inheritance). Fail-open/no-throw
 * preserved.
 *
 * <p>The newest (NRO-04 B01) member is the {@code vfx-combat} {@code FlyingOrbEffect} — the seam's
 * FIRST VARIABLE-LENGTH MULTI-DRAW kind ({@link VfxDrawGeometry#variableLengthMultiDraw}) and its
 * first BOOLEAN {@code isDone} guard ({@link VfxDrawGeometry#guardIsBoolean}/
 * {@link VfxDrawGeometry#guardBlocksBoolean}). It has NO {@code x}/{@code y}/{@code scale} field, so
 * it is served by the dedicated {@link #renderFlyingOrb} branch BEFORE {@link #readFields}: the
 * renderer reads the effect's own {@code Vector2[] points} and draws one center-packed sprite per
 * NON-NULL element for {@code index} from {@code points.length - 1} down to {@code 1} (index
 * {@code 0} is never drawn) with a UNIFORM scale that starts at {@code Settings.scale * 1.5f} and is
 * multiplied by {@code 0.975f} after each DRAWN point, under the additive blend, with the effect's own
 * {@code rotation}. No RNG; a pre-draw throw fails open while a post-draw throw claims the frame (B09).
 * {@link #usesVariableLengthDraw} exposes the capability. No new patch/bridge/console wiring.
 *
 * <p>The newest (NRO-04 B02) member is the {@code vfx-combat} {@code FlickCoinEffect}. It has NO
 * {@code x}/{@code y} field — its position fields are {@code cX}/{@code cY}/{@code yOffset} — so it
 * is served by the dedicated {@link #renderFlickCoin} branch BEFORE {@link #readFields}, like
 * {@link #renderFlyingOrb}. Its native single draw is ADDITIVE with the effect's own {@code color};
 * the POSITION is {@code (cX - packedWidth/2, cY - packedHeight/2 + yOffset)} using native INTEGER
 * division of the packed size, the ORIGIN is {@code (packedWidth/2f, packedHeight/2f)} using native
 * FLOAT division (a {@code 0.5} difference on an odd region), and the scale is ANISOTROPIC
 * ({@code scale * 0.7f} on X, {@code scale * 0.4f} on Y) with the effect's own {@code rotation}. It
 * has no {@code isDone} guard. {@link #imagePresent} reports it drawable iff its {@code img} region
 * is present. No new patch/bridge/console wiring; the default-off gate and per-instance token
 * semantics are unchanged.
 *
 * <p>The newest (NRO-04 B03) member is the {@code vfx-combat} {@code HealPanelEffect} (ctor
 * {@code (float)}), a bare-{@code Texture} fixed-source-rect kind that rides the existing
 * {@link #renderTexture} path (like Calm/Shield/Debuff): its image is a STATIC {@code Texture img}
 * (loaded in the ctor, NO instance {@code img} field), resolved via the same instance-texture reader
 * as {@code FallingIceEffect}, with a fixed src rect {@code (0,0,64,64)}, a fixed origin
 * {@code (32,32)} and a fixed {@code 64&times;64} size, the effect's uniform {@code scale}, its own
 * {@code rotation} field, its own {@code color} (NOT the white-alpha rule) and the additive blend.
 * Its ONE new pure rule is the PANEL-SPACE position (see its {@link VfxDrawGeometry#params} branch),
 * which depends on {@code Settings.HEIGHT} as well as {@code Settings.scale}; the renderer supplies
 * {@code Settings.HEIGHT} through the trailing {@code settingsHeight} argument of the
 * {@link VfxDrawGeometry#params} overload, so every other kind's result is unchanged. No new
 * patch/bridge/console wiring; the default-off gate and per-instance token semantics are unchanged.
 *
 * <p>The newest (NRO-04 B04) member is the {@code vfx-combat} {@code PingHpEffect} (ctor
 * {@code (float)}), the HealPanel analogue: a bare-{@code Texture} fixed-source-rect kind routed
 * through the same {@link #renderTexture} path, whose image is the STATIC {@code ImageMaster.TP_HP}
 * {@code Texture} resolved at draw time (NO instance {@code img} field), with the same fixed src rect
 * {@code (0,0,64,64)}, fixed origin {@code (32,32)} and fixed {@code 64&times;64} size, its own
 * {@code rotation} field, its own {@code color} (NOT the white-alpha rule) and the additive blend. It
 * shares HealPanel's PANEL-SPACE position (the renderer supplies {@code Settings.HEIGHT} through the
 * {@code settingsHeight} argument) but its ONE difference is the uniform draw scale: BOTH axes are
 * {@code scale * Settings.scale}. No new patch/bridge/console wiring; the default-off gate and
 * per-instance token semantics are unchanged.
 *
 * <p>The newest (NRO-04 B05) member is the {@code vfx-misc-root} {@code RewardGlowEffect} (ctor
 * {@code (float, float)}), a bare static-{@code Texture} REWARD-PANEL kind routed through the same
 * {@link #renderTexture} path: its image is the STATIC {@code ImageMaster.REWARD_SCREEN_ITEM}
 * {@code Texture} resolved at draw time (NO instance {@code img} field), with a FIXED src rect
 * {@code (0,0,464,98)}, a FIXED origin {@code (232,49)} and a FIXED {@code 464&times;98} size, its
 * own {@code color} (NOT the white-alpha rule) and the additive blend. Its rotation is HARDCODED
 * {@code 0f} ({@code readTextureFields} does not require the inherited {@code rotation} field for it,
 * like {@code CARD_TRAIL}; the effect's own {@code angle} field is unused by the claimed overload),
 * and its draw scale is ANISOTROPIC: {@code scaleX = Settings.xScale} (INDEPENDENT of the effect's
 * own {@code scale}) and {@code scaleY = scale + Settings.scale * 0.05f}. The renderer supplies
 * {@code Settings.xScale} through the same trailing {@code settingsHeight} argument
 * ({@link #textureDrawSettingsSlot}); every other kind's result is unchanged. The class's OTHER
 * overload {@code render(SpriteBatch, Color)} is NOT reachable through this seam and stays native.
 * No new patch/bridge/console wiring; the default-off gate and per-instance token semantics are
 * unchanged.
 *
 * <p>The newest (NRO-04 B07) member is the {@code vfx-misc-root} {@code SpotlightEffect}: a
 * FULL-SCREEN bare static-{@code Texture} kind with NO {@code x}/{@code y} field, and its native
 * {@code render} consumes only {@code color} (the inherited {@code scale}/{@code rotation} are
 * unused and not required), so it is served by a dedicated
 * {@link #renderFullScreenTexture} branch BEFORE {@link #readFields}/{@link #readTextureFields}. Its
 * native {@code render} is {@code setColor(color); setBlendFunction(770, 1);
 * sb.draw(ImageMaster.SPOTLIGHT_VFX, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
 * setBlendFunction(770, 771)} — ADDITIVE, the static {@code ImageMaster.SPOTLIGHT_VFX Texture}, the
 * fixed full-screen position/size, and the effect's OWN color ({@link #resolveTexture} returns the
 * static texture; ONLY {@code color} is read from the effect). A missing/null static texture (or
 * null color) fails open. PRODUCTION REACH (positive): it IS constructed by
 * {@code GrandFinalEffect} into {@code AbstractDungeon.effectsQueue}, so the {@code AbstractDungeon
 * .render} effect loop the seam instruments does reach a real instance. No new
 * patch/bridge/console wiring; the default-off gate and per-instance token semantics are unchanged.
 *
 * <p>The newest (NRO-04 B08) member is the {@code vfx-campfire} {@code CampfireRecallEffect}: a
 * FULL-SCREEN bare static-{@code Texture} kind with NO {@code x}/{@code y} field whose native
 * {@code render} consumes only its own {@code screenColor} field (NOT the inherited {@code color};
 * the inherited {@code scale}/{@code rotation} are unused and not required), so it is served by the
 * same {@link #renderFullScreenTexture} branch as {@code SpotlightEffect}. Its native {@code render}
 * is {@code sb.setColor(screenColor); sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH,
 * Settings.HEIGHT)} — AMBIENT (no {@code setBlendFunction}), the static
 * {@code ImageMaster.WHITE_SQUARE_IMG Texture}, the fixed full-screen position/size, and the
 * effect's OWN {@code screenColor} ({@link #resolveTexture} returns the static texture; ONLY
 * {@code screenColor} is read from the effect, selected by
 * {@link VfxDrawGeometry#fullScreenTextureReadsScreenColor}). A missing/null static texture (or
 * null {@code screenColor}) fails open. PRODUCTION REACH (positive): it IS constructed by
 * {@code RecallOption} and added to {@code AbstractDungeon.effectList}, so the
 * {@code AbstractDungeon.render} effect loop the seam instruments does reach a real instance. No new
 * patch/bridge/console wiring; the default-off gate and per-instance token semantics are unchanged.
 *
 * <p>F2b1 shipped the two host-free halves of the real renderer: the readiness predicate
 * ({@link #isReady}, backed by the exact-FQN {@link VfxDrawGeometry#kindFor}) and the reflective
 * field reader ({@link #readFields}) that snapshots the native effect's own draw inputs. F2b2
 * completes the renderer: {@link #render} converts the effect's own live {@code img} region to the
 * host-neutral {@link AtlasRegion} (via {@link Sts1GdxAtlasRegions#fromGdx}), resolves the native
 * draw arguments through {@link VfxDrawGeometry#params}, and replays the native
 * {@code SpriteBatch.draw} with color save-restore and a per-kind blend policy
 * ({@link VfxDrawGeometry#additiveBlend} — the single source of truth for the ambient set):
 * additive for most kinds, but ambient — no {@code setBlendFunction} call at all — for exactly the
 * kinds whose {@code VfxDrawGeometry.additiveBlend(kind)} is {@code false}, today
 * {@code FlashAtkImgEffect}, {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect},
 * {@code NemesisFireParticle}, {@code DebuffParticleEffect}, {@code GenericSmokeEffect},
 * {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect}, {@code ExhaustPileParticle},
 * {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect}, {@code DamageImpactLineEffect},
 * {@code StunStarEffect}, {@code FallingDustEffect}, {@code ShineLinesEffect},
 * {@code DustEffect}, {@code ConeEffect}, and {@code DamageHeartEffect}.
 * {@code CalmParticleEffect} has no
 * {@code img} and draws the bare {@link ImageMaster#FROST_ACTIVATE_VFX_1} {@link Texture}, so its
 * own branch resolves that texture and uses the raw texture + source-rect draw overload.
 * {@code ShieldParticleEffect} and {@code DebuffParticleEffect} are the second and third
 * bare-{@code Texture} kinds: they share that generalized fixed-source-rect path, with Shield
 * resolving the static {@link ImageMaster#INTENT_DEFEND} {@code Texture} additively (rotation
 * hardcoded {@code 0}) and Debuff resolving its own instance {@code Texture} {@code img} under the
 * ambient blend (consuming its {@code rotation} field). A false
 * result still means "no pixels produced" so the caller fails open to the native draw.
 *
 * <p>Reflection is confined to reading the native effect's own fields (no patch, no host mutation),
 * mirrors the existing soft reflective conventions in {@code artframework.sts1}, and always fails
 * open: missing, unreadable, or wrongly typed fields yield {@code null} (or {@code false}) rather
 * than an exception, so the caller continues with the native draw.
 */
public final class Sts1VfxArtRenderer implements VfxArtRenderer.Adapter {

    /** Sentinel distinguishing "field absent/unreadable" from a legitimately null field value. */
    private static final Object MISSING = new Object();

    /**
     * Immutable snapshot of the fields one claim draw needs, or {@code null} when any required field
     * is missing.
     */
    static final class Fields {
        final float x;
        final float y;
        final float vY;
        final float vX;
        final float regionOffsetX;
        final float regionOffsetY;
        final float scale;
        final float rotation;
        final float durDiv2;
        final float duration;
        final Color color;
        final TextureAtlas.AtlasRegion img;
        final boolean mirrorX;
        final boolean mirrorY;

        Fields(float x, float y, float vY, float vX, float regionOffsetX, float regionOffsetY,
                float scale, float rotation, float durDiv2,
                float duration, Color color, TextureAtlas.AtlasRegion img,
                boolean mirrorX, boolean mirrorY) {
            this.x = x;
            this.y = y;
            this.vY = vY;
            this.vX = vX;
            this.regionOffsetX = regionOffsetX;
            this.regionOffsetY = regionOffsetY;
            this.scale = scale;
            this.rotation = rotation;
            this.durDiv2 = durDiv2;
            this.duration = duration;
            this.color = color;
            this.img = img;
            this.mirrorX = mirrorX;
            this.mirrorY = mirrorY;
        }
    }

    /** True only when the exact native class is one of the claimable seam FQNs. */
    @Override
    public boolean isReady(String nativeClassName) {
        try {
            return VfxDrawGeometry.kindFor(nativeClassName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Immutable snapshot of the fields a bare-{@code Texture} draw needs ({@code CalmParticleEffect}
     * / {@code ShieldParticleEffect} / {@code DebuffParticleEffect} / {@code IceShatterEffect} /
     * {@code WebParticleEffect} / {@code EntangleEffect} / {@code UnknownParticleEffect} /
     * {@code DarkOrbPassiveEffect} / {@code LightningOrbPassiveEffect} /
     * {@code GlowyFireEyesEffect} / {@code TorchHeadFireEffect} / {@code FallingIceEffect}).
     * {@code rotation} is required for the kinds whose formula consumes
     * it (Calm, Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive, FallingIce) and optional
     * (defaulting
     * to {@code 0}) for
     * Shield, Web,
     * Entangle, GlowyFireEyes, and TorchHeadFire, which hardcode {@code 0f}; {@code img} is a
     * {@link Texture} for
     * Debuff, IceShatter, Unknown, DarkOrb, FallingIce, and the flip kinds (LightningOrbPassive,
     * GlowyFireEyes, TorchHeadFire) and {@code null} otherwise (Web/Entangle resolve the static
     * {@link ImageMaster#WEB_VFX}). {@code flipX}/{@code flipY} are the resolved per-instance flip
     * flags (both {@code false} for every kind that does not pass them natively).
     */
    static final class TextureFields {
        final float x;
        final float y;
        final float scale;
        final float rotation;
        final float durDiv2;
        final float duration;
        final Color color;
        final Texture img;
        final boolean flipX;
        final boolean flipY;

        TextureFields(float x, float y, float scale, float rotation, float durDiv2, float duration,
                Color color, Texture img, boolean flipX, boolean flipY) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotation = rotation;
            this.durDiv2 = durDiv2;
            this.duration = duration;
            this.color = color;
            this.img = img;
            this.flipX = flipX;
            this.flipY = flipY;
        }
    }

    /**
     * Snapshots the native effect's own draw fields, walking {@code getClass()} up through the
     * superclass chain (guarded by {@code getDeclaredField}+{@code setAccessible(true)}) and stopping
     * at {@link Object}.
     *
     * <p>Required: {@code x}, {@code y}, {@code scale}, {@code rotation}, {@code color}
     * ({@link Color}), and {@code img} ({@link TextureAtlas.AtlasRegion}); {@code scale}/{@code
     * rotation} are read as any {@link Number} (primitive {@code float} boxes). {@code dur_div2} and
     * {@code duration} are always optional and default to {@code 0}. {@code vY} is required only for
     * the kinds whose {@link VfxDrawGeometry#params} formula consumes it ({@code WRATH_PARTICLE},
     * {@code DIVINITY_PARTICLE}, {@code STUN_STAR}); for the kinds that ignore it
     * ({@code STANCE_AURA},
     * {@code DIVINITY_STANCE_CHANGE}, {@code LIGHT_FLARE}, {@code LIGHT_FLARE_M},
     * {@code LIGHT_FLARE_L}, {@code TORCH_PARTICLE_L}, {@code FLASH_ATK_IMG}, and the nine newest
     * members {@code FIRE_BURST}, {@code RED_FIRE_BURST}, {@code SMOKE_BLUR}, {@code CEILING_DUST},
     * {@code NEMESIS_FIRE}, {@code TORCH_PARTICLE_XL}, {@code GHOSTLY_WEAK_FIRE},
     * {@code GENERIC_SMOKE}, {@code EXHAUST_BLUR} — plus the three newest img kinds
     * {@code LIGHTNING_EFFECT}, {@code FLAME_BALL}, {@code SHINE_LINES} — plus the three newest
     * img kinds {@code TORCH_PARTICLE_M}, {@code TORCH_PARTICLE_S}, {@code SCENE_DUST}, whose
     * {@code rotation}
     * field all three consume ({@code TORCH_PARTICLE_M}/{@code TORCH_PARTICLE_S}'s {@code vY} is
     * update-only)) it is optional and
     * defaults to {@code 0}, which is required because {@code DivinityStanceChangeParticle},
     * {@code LightFlareSEffect}, {@code FlashAtkImgEffect}, and {@code LightFlareMEffect}/
     * {@code LightFlareLEffect} have no {@code vY} field — and {@code TorchParticleLEffect}, the
     * two newest {@code TorchParticleMEffect}/{@code TorchParticleSEffect}, and all
     * nine earlier img members have one
     * but never read it in {@code render} (it is update-only). {@code vX} is likewise optional
     * (defaulting to {@code 0}) for every kind: {@code StunStarEffect} consumes it in its position
     * offset, and every other kind ignores it.
     *
     * <p>The two region-offset scalars are taken from the effect's own live
     * {@link TextureAtlas.AtlasRegion} ({@code offsetX}/{@code offsetY}), not from an effect field;
     * they are the draw origin only for {@code FALLING_DUST} and the newest {@code SCENE_DUST}
     * (which reuses that same region-offset-origin rule). Because {@code AtlasRegion.flip(...)}
     * mutates {@code offsetX}/{@code offsetY} in place on a shared static region, the values are
     * normalized back to the unflipped orientation (the exact inverse of
     * {@code AtlasRegion.flip}'s offset transform) when {@code isFlipX()}/{@code isFlipY()} reports a
     * native flip, matching the flip-invariant canonical draw (F15d).
     *
     * <p>Returns {@code null}
     * when the effect is null or any required field is absent, unreadable, or of the wrong type;
     * never throws.
     */
    static Fields readFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        // vY is only meaningful for the formulas that add it to y or use it in the position offset;
        // requiring it elsewhere would wrongly reject DivinityStanceChangeParticle/LightFlareSEffect
        // and LightFlareMEffect/LightFlareLEffect (no vY field) and TorchParticleLEffect plus the
        // nine newest members (their vY is update-only), the three F17 img kinds (LightningEffect
        // and ShineLinesEffect have no vY field; FlameBallParticleEffect's is update-only), and the
        // two newest TorchParticleMEffect/TorchParticleSEffect (their vY is update-only), and
        // omitting it where it is consumed would
        // silently draw at an un-shifted y/position instead of failing open to the native draw.
        // StunStarEffect consumes vY in its position offset (y - vY*5f*Settings.scale).
        boolean requireVY = kind == VfxDrawGeometry.Kind.WRATH_PARTICLE
                || kind == VfxDrawGeometry.Kind.DIVINITY_PARTICLE
                || kind == VfxDrawGeometry.Kind.STUN_STAR;
        try {
            Float x = readFloat(effect, "x");
            Float y = readFloat(effect, "y");
            Float scale = readFloat(effect, "scale");
            Float rotation = readFloat(effect, "rotation");
            // CardTrailEffect's native render hardcodes rotation 0f and ignores its inherited rotation
            // field, so it does NOT require one (mirroring its hardcoded-0f shape-C cousins). Every
            // other img kind consumes its rotation field, so it stays required.
            boolean requireRotation = kind != VfxDrawGeometry.Kind.CARD_TRAIL;
            if (x == null || y == null || scale == null || (requireRotation && rotation == null)) {
                return null;
            }
            Float vY = readFloat(effect, "vY");
            if (requireVY && vY == null) {
                return null;
            }
            Object color = readRaw(effect, "color");
            Object img = readRaw(effect, "img");
            if (!(color instanceof Color)) return null;
            if (!(img instanceof TextureAtlas.AtlasRegion)) return null;
            TextureAtlas.AtlasRegion region = (TextureAtlas.AtlasRegion) img;
            // Per-instance MIRROR flags (NRO-04 F22): these are DISTINCT from the shape-C F19
            // flip flags. Native mirrors the drawn sprite by flipping the shared region in place
            // around its draw; the claim suppresses that draw, so the host draw reproduces the
            // mirror as a UV swap on the canonical region. Resolved only for the kinds whose native
            // render reads them; an absent/unreadable flag defaults to false (mirror is optional
            // decoration, so it does NOT fail the snapshot open).
            boolean mirrorX = VfxDrawGeometry.usesInstanceMirrorX(kind)
                    && optionalBoolean(effect, "flipX");
            boolean mirrorY = VfxDrawGeometry.usesInstanceMirrorY(kind)
                    && optionalBoolean(effect, "flipY");
            return new Fields(x, y, vY != null ? vY : 0f, optionalFloat(effect, "vX"),
                    unflippedOffsetX(region), unflippedOffsetY(region),
                    scale, rotation,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    (Color) color, region, mirrorX, mirrorY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * The region's unflipped horizontal trim origin: {@code AtlasRegion.flip(true, *)} rewrites
     * {@code offsetX = originalWidth - offsetX - rotatedPackedWidth} in place, so this exact inverse
     * recovers the trim origin the atlas declared (a no-op for an unflipped region). Mirrors the
     * F15d flip-invariant canonical draw.
     */
    private static float unflippedOffsetX(TextureAtlas.AtlasRegion region) {
        if (region.isFlipX()) {
            return region.originalWidth - region.offsetX - region.getRotatedPackedWidth();
        }
        return region.offsetX;
    }

    /**
     * The region's unflipped vertical trim origin (see {@link #unflippedOffsetX}); the same inverse
     * of {@code AtlasRegion.flip(*, true)}'s {@code offsetY} transform.
     */
    private static float unflippedOffsetY(TextureAtlas.AtlasRegion region) {
        if (region.isFlipY()) {
            return region.originalHeight - region.offsetY - region.getRotatedPackedHeight();
        }
        return region.offsetY;
    }

    /**
     * Snapshots the fields a bare-{@code Texture} draw needs. Required: {@code x}, {@code y},
     * {@code scale}, {@code color} ({@link Color}) — except that {@code y} is optional (defaulting to
     * {@code 0})
     * only for {@code HEAL_PANEL} and the newest (NRO-04 B04) {@code PING_HP}, whose native draw y is
     * panel-space from {@code Settings.HEIGHT} rather than the effect's own field (the classes have no
     * {@code y}), and {@code color} is optional (stored as {@code null}) for the newest (NRO-04 B06)
     * {@code MAP_CIRCLE}, whose native ctor never initializes the inherited color field (a REAL
     * instance has {@code color == null}) and whose draw color is HARDCODED (applied by
     * {@code resolveColor}); {@code rotation} is additionally
     * required for
     * {@code CALM_PARTICLE}/{@code DEBUFF_PARTICLE}/{@code ICE_SHATTER}/{@code UNKNOWN_PARTICLE}/
     * {@code DARK_ORB_PASSIVE}/{@code LIGHTNING_ORB_PASSIVE}/{@code FALLING_ICE}
     * (whose formula consumes it)
     * and optional (defaulting to {@code 0}) for {@code SHIELD_PARTICLE}/{@code WEB_PARTICLE}/
     * {@code ENTANGLE}/{@code GLOWY_FIRE_EYES} (hardcoded rotation); {@code dur_div2}/{@code duration}
     * are optional and
     * default to {@code 0}.
     * The newest (NRO-04 B05) {@code REWARD_GLOW} likewise treats {@code rotation} as optional: its
     * native single-arg {@code render(SpriteBatch)} hardcodes {@code 0f} and its {@code angle} field
     * is unused (like {@code CARD_TRAIL}), so a missing/unreadable rotation defaults to {@code 0}
     * rather than failing the snapshot; its {@code x}/{@code y} stay required.
     * For {@code DEBUFF_PARTICLE}, {@code ICE_SHATTER}, {@code UNKNOWN_PARTICLE},
     * {@code DARK_ORB_PASSIVE}, {@code LIGHTNING_ORB_PASSIVE}, {@code FALLING_ICE},
     * {@code GLOWY_FIRE_EYES}, and the newest {@code TORCH_HEAD_FIRE} the instance
     * {@code img} must be a
     * {@link Texture} (CALM/SHIELD/WEB/ENTANGLE resolve a static {@code ImageMaster} texture
     * instead); a missing/mistyped {@code img} fails the snapshot. The per-instance flip flags are
     * resolved only for the kinds whose native render passes them
     * ({@link VfxDrawGeometry#usesInstanceFlipX}/{@link VfxDrawGeometry#usesInstanceFlipY}, plus the
     * {@code flippedX}-field kinds reported by {@link VfxDrawGeometry#usesTexturedFlipX});
     * a missing/unreadable flag defaults to {@code false} (flip is optional decoration, so it does
     * NOT fail the snapshot). Returns {@code null} when the
     * effect is null or any required field is absent, unreadable, or of the wrong type; never throws.
     */
    static TextureFields readTextureFields(VfxDrawGeometry.Kind kind, Object effect) {
        if (effect == null) return null;
        boolean requireRotation = kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.FALLING_ICE
                || kind == VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION
                || kind == VfxDrawGeometry.Kind.HEAL_PANEL
                || kind == VfxDrawGeometry.Kind.PING_HP
                || kind == VfxDrawGeometry.Kind.MAP_CIRCLE;
        try {
            Float x = readFloat(effect, "x");
            Float y = readFloat(effect, "y");
            Float scale = readFloat(effect, "scale");
            // HEAL_PANEL and PING_HP have NO y field: their native draw y is panel-space from
            // Settings.HEIGHT, so the reader defaults y to 0 (the params branch ignores it).
            if (x == null || scale == null
                    || (y == null && kind != VfxDrawGeometry.Kind.HEAL_PANEL
                            && kind != VfxDrawGeometry.Kind.PING_HP)) {
                return null;
            }
            Float rotation = readFloat(effect, "rotation");
            if (requireRotation && rotation == null) {
                return null;
            }
            Object rawColor = readRaw(effect, "color");
            // MAP_CIRCLE's native ctor NEVER initializes the inherited color field
            // (AbstractGameEffect.<init> does not set it), so a REAL MapCircleEffect instance has
            // color == null. Its draw color is HARDCODED and applied by resolveColor, so color is
            // OPTIONAL for this kind (exactly like y for HEAL_PANEL/PING_HP): a missing/null/non-Color
            // value does NOT fail the snapshot and is stored as null. Every OTHER texture kind still
            // requires a Color here.
            Color color;
            if (kind == VfxDrawGeometry.Kind.MAP_CIRCLE) {
                color = rawColor instanceof Color ? (Color) rawColor : null;
            } else {
                if (!(rawColor instanceof Color)) return null;
                color = (Color) rawColor;
            }
            Texture img = null;
            if (usesInstanceTexture(kind)) {
                Object raw = readRaw(effect, "img");
                if (!(raw instanceof Texture)) return null;
                img = (Texture) raw;
            } else if (kind == VfxDrawGeometry.Kind.MAP_CIRCLE) {
                // MapCircleEffect.img is a PUBLIC STATIC Texture on the effect's own class (there is
                // NO instance img field); readRaw walks the hierarchy and Field.get resolves the
                // static value regardless of the instance. It must be a non-null Texture.
                Object raw = readRaw(effect, "img");
                if (!(raw instanceof Texture)) return null;
                img = (Texture) raw;
            }
            // Per-instance flip flags (NRO-04 F19/F29): resolved only for the kinds whose native
            // render passes them (LightningOrbPassiveEffect's flipX/flipY; GlowyFireEyesEffect's and
            // TorchHeadFireEffect's flippedX with a hardcoded false Y). Flip is OPTIONAL decoration,
            // so an absent/unreadable flag
            // defaults to false rather than failing the whole snapshot (the rest of the snapshot
            // still fails open on its required fields).
            boolean flipX = false;
            boolean flipY = false;
            if (VfxDrawGeometry.usesInstanceFlipX(kind)
                    || VfxDrawGeometry.usesTexturedFlipX(kind)) {
                flipX = optionalBoolean(effect,
                        VfxDrawGeometry.usesTexturedFlipX(kind) ? "flippedX" : "flipX");
            }
            if (VfxDrawGeometry.usesInstanceFlipY(kind)) {
                flipY = optionalBoolean(effect, "flipY");
            }
            return new TextureFields(x, y != null ? y : 0f, scale,
                    rotation != null ? rotation : 0f,
                    optionalFloat(effect, "dur_div2"), optionalFloat(effect, "duration"),
                    color, img, flipX, flipY);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * True for the bare-{@code Texture} kinds that resolve their own instance {@code Texture img}
     * rather than a static {@code ImageMaster} texture ({@code DebuffParticleEffect},
     * {@code IceShatterEffect}, {@code UnknownParticleEffect}, {@code DarkOrbPassiveEffect},
     * {@code LightningOrbPassiveEffect}, {@code GlowyFireEyesEffect}, {@code TorchHeadFireEffect},
     * {@code FallingIceEffect}).
     * {@code CalmParticleEffect},
     * {@code ShieldParticleEffect}, {@code WebParticleEffect}, and {@code EntangleEffect} draw a
     * static {@code ImageMaster} texture instead.
     */
    private static boolean usesInstanceTexture(VfxDrawGeometry.Kind kind) {
        return kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                || kind == VfxDrawGeometry.Kind.TORCH_HEAD_FIRE
                || kind == VfxDrawGeometry.Kind.FALLING_ICE
                || kind == VfxDrawGeometry.Kind.HEAL_PANEL;
    }

    /**
     * Builds a canonical (pollution-immune) {@link TextureRegion} view of a live gdx
     * {@code AtlasRegion}: the packed footprint normalized to its canonical orientation
     * ({@code u}=min, {@code u2}=max, {@code v}=min, {@code v2}=max) with the same texture. Native
     * siblings draw the same shared static regions and call {@code region.flip(...)} in place, which
     * swaps {@code u}/{@code u2} (and, for {@code AtlasRegion}, {@code offsetX}); the claim
     * suppresses that native draw, so ART must emit the canonical rect rather than whatever a native
     * sibling last left behind. Returns {@code null} when the region or its texture is null. Never
     * mutates the input region.
     *
     * <p>When {@code mirrorX}/{@code mirrorY} are set the returned view additionally mirrors the
     * canonical rect (swapping {@code u}/{@code u2} and/or {@code v}/{@code v2}), reproducing the
     * native per-instance mirror WITHOUT a negative scale. A UV swap is visually identical to
     * native's in-place region flip for a center-origin quad; the pre-existing kinds pass
     * {@code false, false} and get the unchanged canonical rect.
     */
    private static TextureRegion canonicalRegion(TextureAtlas.AtlasRegion region,
            boolean mirrorX, boolean mirrorY) {
        if (region == null) return null;
        Texture texture = region.getTexture();
        if (texture == null) return null;
        float u = Math.min(region.getU(), region.getU2());
        float u2 = Math.max(region.getU(), region.getU2());
        float v = Math.min(region.getV(), region.getV2());
        float v2 = Math.max(region.getV(), region.getV2());
        if (mirrorX) {
            float swap = u;
            u = u2;
            u2 = swap;
        }
        if (mirrorY) {
            float swap = v;
            v = v2;
            v2 = swap;
        }
        return new TextureRegion(texture, u, v, u2, v2);
    }

    /**
     * The per-kind fixed horizontal offset added to the shared center-packed origin
     * ({@code packedWidth / 2f}); {@code 0f} for every current kind.
     */
    private static float originOffsetX(VfxDrawGeometry.Kind kind) {
        return 0f;
    }

    /**
     * The per-kind fixed vertical offset added to the shared center-packed origin
     * ({@code packedHeight / 2f}). {@code LIGHTNING_EFFECT} passes {@code -packedHeight / 2f} so its
     * origin Y becomes exactly {@code 0f} (a scale-independent native offset); every other kind
     * passes {@code 0f} and keeps the unchanged {@code packedHeight / 2f} origin.
     * {@code FLAME_BALL}'s settings-scaled origin lift ({@code +20f * Settings.scale}) is handled
     * inside {@link VfxDrawGeometry#params} (which already receives {@code settingsScale}), so it
     * passes {@code 0f} here.
     */
    private static float originOffsetY(VfxDrawGeometry.Kind kind, TextureAtlas.AtlasRegion gdx) {
        if (kind == VfxDrawGeometry.Kind.LIGHTNING_EFFECT) {
            return -gdx.getRegionHeight() / 2f;
        }
        return 0f;
    }

    /**
     * The per-kind anisotropic scaleY multiplier for the img center-packed branch
     * ({@link VfxDrawGeometry#params}). Every pre-existing kind passes {@code 1f} (so its results are
     * unchanged from before the parameter existed); the newest F27 {@code WATER_SPLASH} passes
     * {@link VfxDrawGeometry#WATER_SPLASH_SCALE_Y_MULTIPLIER} ({@code 0.54f}) so its draw scaleY is
     * {@code scale * 0.54f} while scaleX stays {@code scale}, exactly like the native
     * {@code WaterSplashParticleEffect.render}.
     */
    private static float scaleYMultiplier(VfxDrawGeometry.Kind kind) {
        if (kind == VfxDrawGeometry.Kind.WATER_SPLASH) {
            return VfxDrawGeometry.WATER_SPLASH_SCALE_Y_MULTIPLIER;
        }
        return 1f;
    }

    /**
     * The seam's per-kind NATIVE DRAW GUARD (NRO-04 F21, generalized in F24): true unless the native
     * render's wait-phase guard blocks the draw. For a guarded kind
     * ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}) the guard field named by
     * {@link VfxDrawGeometry#guardFieldName} is read reflectively; when it is present and
     * {@link VfxDrawGeometry#guardBlocks} reports the value blocks the draw, the native render would
     * skip its draw entirely, so ART must also draw nothing (return {@code false}) to stay in pixel
     * parity. An absent/unreadable/wrongly typed guard field is treated as SATISFIED (returns
     * {@code true}) — the native render would then not be able to read a guard either. A cheap no-op
     * for every non-guard kind. Never throws.
     *
     * <p>The block THRESHOLD is per-kind and lives in {@link VfxDrawGeometry#guardBlocks}, the single
     * source of truth (it differs per kind and handles {@code NaN} like the native comparison does).
     *
     * <p>Package-private so the no-GL tests can exercise the absent/unreadable-guard path directly
     * (the real mapping classes always declare their guard field).
     */
    static boolean guardSatisfied(VfxDrawGeometry.Kind kind, Object effect) {
        if (kind == null || effect == null) return true;
        if (!VfxDrawGeometry.nativeSkipsDrawByGuard(kind)) return true;
        String field = VfxDrawGeometry.guardFieldName(kind);
        if (field == null) return true;
        // NRO-04 B01: a BOOLEAN guard (FlyingOrbEffect's `if (isDone) return`) reads the raw flag and
        // blocks when it is TRUE; a float guard uses the per-kind guardBlocks threshold. An
        // absent/unreadable/wrong-typed guard field is treated as not blocking either way (the native
        // render could not read it either).
        if (VfxDrawGeometry.guardIsBoolean(kind)) {
            try {
                Object raw = readRaw(effect, field);
                if (!(raw instanceof Boolean)) return true;
                return !VfxDrawGeometry.guardBlocksBoolean(kind, ((Boolean) raw).booleanValue());
            } catch (Throwable ignored) {
                return true;
            }
        }
        Float value;
        try {
            value = readFloat(effect, field);
        } catch (Throwable ignored) {
            return true;
        }
        // An absent/unreadable guard field is treated as not blocking; otherwise encode the kind's
        // native wait-phase test via the pure per-kind threshold predicate.
        return value == null || !VfxDrawGeometry.guardBlocks(kind, value.floatValue());
    }

    /**
     * Test seam: when set, the player object used to resolve the player-hitbox-relative x instead of
     * the live {@code AbstractDungeon.player}. Package-private and null by default so production
     * always reads the live player. Mirrors the {@code ...ForTests} conventions elsewhere.
     */
    private static volatile Object playerForTests;

    static void setPlayerForTests(Object player) {
        playerForTests = player;
    }

    static void resetPlayerForTests() {
        playerForTests = null;
    }

    /**
     * The player's hitbox center X ({@code AbstractDungeon.player.hb.cX}) for the
     * player-hitbox-relative kinds ({@link VfxDrawGeometry#playerHitboxRelativeX}), or {@code null}
     * when the player or its hitbox is absent/unreadable. Read reflectively so this stays fail-open
     * and never couples to the native field visibility; the caller turns a {@code null} into a
     * fail-open decline (native draws instead) rather than drawing at a wrong position. Never throws.
     */
    private static Float playerHitboxCenterX() {
        try {
            Object player = playerForTests;
            if (player == null) {
                player = com.megacrit.cardcrawl.dungeons.AbstractDungeon.player;
            }
            if (player == null) return null;
            Object hb = readRaw(player, "hb");
            if (!(hb instanceof com.megacrit.cardcrawl.helpers.Hitbox)) return null;
            return readFloat(hb, "cX");
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Snapshot of the shared global RNG state for the current {@code MathUtils.random} instance, or
     * {@code null} when it is not a libGDX {@link com.badlogic.gdx.math.RandomXS128} (whose two seed
     * longs fully determine the next draw). Taken BEFORE any RNG value is pulled so that a PRE-pass
     * fail-open (no pixels painted yet) can restore the stream exactly, keeping the seam's contract
     * that such a fail-open never advances the global RNG and a successful draw advances it exactly as
     * often as the kind's native render does — the renderer snapshots here and restores on a PRE-pass
     * fail-open only, while a POST-pass fail-open (at least one pass already painted) keeps the
     * consumed stream and claims the frame instead (NRO-04 B09; see {@link #renderTexture}). The
     * number of advances per successful draw is the kind's
     * {@link VfxDrawGeometry#randomRanges} plus {@link VfxDrawGeometry#drawPassRandomRanges} RNG-call
     * count ({@code 0} for the deterministic kinds, {@code 2} for {@code WRATH_STANCE_CHANGE}, and
     * {@code 4} for the multi-draw {@code STANCE_CHANGE_ABSORPTION}). Never throws.
     */
    private static long[] rngSnapshot() {
        try {
            Object rng = com.badlogic.gdx.math.MathUtils.random;
            if (!(rng instanceof com.badlogic.gdx.math.RandomXS128)) return null;
            com.badlogic.gdx.math.RandomXS128 xs = (com.badlogic.gdx.math.RandomXS128) rng;
            return new long[] {xs.getState(0), xs.getState(1)};
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Restores the state captured by {@link #rngSnapshot}; a no-op for a {@code null} snapshot (an
     * unsupported RNG type) or when the live RNG is no longer an {@link
     * com.badlogic.gdx.math.RandomXS128}. Never throws.
     */
    private static void restoreRng(long[] snapshot) {
        if (snapshot == null) return;
        try {
            Object rng = com.badlogic.gdx.math.MathUtils.random;
            if (rng instanceof com.badlogic.gdx.math.RandomXS128) {
                ((com.badlogic.gdx.math.RandomXS128) rng).setState(snapshot[0], snapshot[1]);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Draws the real ART sprite for a claimed transient effect, reproducing the native draw
     * including its per-kind blend behavior ({@link VfxDrawGeometry#additiveBlend} — the single
     * source of truth for the ambient set: additive for
     * most kinds, ambient — no {@code setBlendFunction} call — for the ambient kinds
     * {@code FlashAtkImgEffect}, {@code SmokeBlurEffect}, {@code CeilingDustCloudEffect},
     * {@code NemesisFireParticle}, {@code DebuffParticleEffect}, {@code GenericSmokeEffect},
     * {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect}, {@code ExhaustPileParticle},
     * {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect}, {@code DamageImpactLineEffect},
     * {@code StunStarEffect}, {@code FallingDustEffect}, {@code ShineLinesEffect},
     * {@code DustEffect}, and {@code ConeEffect}).
     *
     * <p>For the img-based kinds: the region comes from the effect's own live
     * {@code img} through {@link Sts1GdxAtlasRegions#fromGdx}, geometry from
     * {@link VfxDrawGeometry#params}, color from the effect's own {@link Color} (or white). The
     * per-kind blend policy is {@link VfxDrawGeometry#additiveBlend} (the single source of truth for
     * the ambient set): additive kinds install
     * {@code (SRC_ALPHA, ONE)} and restore {@code (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)};
     * the ambient kinds ({@code FlashAtkImgEffect}, {@code SmokeBlurEffect},
     * {@code CeilingDustCloudEffect}, {@code NemesisFireParticle}, {@code DebuffParticleEffect},
     * {@code GenericSmokeEffect}, {@code ExhaustBlurEffect}, {@code BlockImpactLineEffect},
     * {@code ExhaustPileParticle}, {@code UnknownParticleEffect}, {@code DamageImpactBlurEffect},
     * {@code DamageImpactLineEffect}, {@code StunStarEffect}, {@code FallingDustEffect},
     * {@code ShineLinesEffect}, {@code DustEffect}, and {@code ConeEffect})
     * never call
     * {@code setBlendFunction} natively, so their path draws
     * under the ambient blend and restores only the previous color. Every branch restores the
     * previous color. That draw uses the {@code SpriteBatch#draw(TextureRegion, ...)} overload with
     * the same arguments the native call passes, so libGDX resolves the region's UV rect (including
     * any atlas {@code rotate} baking) exactly as the native effect does.
     *
     * <p>The bare-{@code Texture} kinds (Calm, Shield, Debuff, IceShatter, Web, Entangle, Unknown,
     * DarkOrb, LightningOrbPassive, GlowyFireEyes, TorchHeadFire, FallingIce)
     * have no packed
     * region: they draw a fixed source rect, so this branch resolves the native {@link Texture} the
     * render reads — {@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm,
     * {@link ImageMaster#INTENT_DEFEND} for Shield, {@link ImageMaster#WEB_VFX} for Web and Entangle,
     * and the
     * effect's own instance {@code img} for Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive,
     * GlowyFireEyes, TorchHeadFire, and FallingIce — and replays
     * the native
     * raw
     * texture + source-rect overload, now including the effect's own per-instance flip booleans for
     * the kinds that pass them (LightningOrbPassive's {@code flipX}/{@code flipY} and
     * GlowyFireEyes's/TorchHeadFire's {@code flippedX} with a hardcoded {@code false} Y; every other
     * kind keeps
     * {@code false, false}).
     *
     * <p>Never throws. Returns {@code true} only after a real draw; any null input, unmapped class,
     * unreadable field, missing/invalid region or texture, or host failure returns {@code false}
     * without side effects so the caller fails open to the native draw.
     */
    @Override
    public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
        if (sb == null || effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind =
                    VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            // Native draw guard (NRO-04 F21): a guarded kind whose wait-phase field blocks per that
            // kind's threshold (VfxDrawGeometry.guardBlocks — the source of truth) draws nothing
            // natively, so ART must draw nothing too (fail open: the patch calls native render, which
            // also draws nothing). Cheap no-op for every non-guard kind.
            if (!guardSatisfied(kind, effect)) return false;
            // NRO-04 B01: FlyingOrbEffect is the first VARIABLE-LENGTH MULTI-DRAW kind — its draw
            // count and per-draw positions come from its own Vector2[] points, not the generic
            // packed-region/x-y/scale snapshot (it has no x/y/scale field at all). Serve it through a
            // dedicated branch BEFORE the generic readFields path.
            if (kind == VfxDrawGeometry.Kind.FLYING_ORB) {
                return renderFlyingOrb(sb, effect);
            }
            // NRO-04 B02: FlickCoinEffect has NO x/y/scale fields (its position fields are
            // cX/cY/yOffset), so it is served by a dedicated branch BEFORE the generic readFields
            // path, like FLYING_ORB.
            if (kind == VfxDrawGeometry.Kind.FLICK_COIN) {
                return renderFlickCoin(sb, effect);
            }
            if (isTextureDrawKind(kind)) {
                return renderTexture(sb, kind, effect);
            }
            Fields f = readFields(kind, effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            if (neutral == null || !neutral.valid()) return false;
            // The claim suppresses native rendering, so draw a CANONICAL (pollution-immune) view
            // rather than the possibly-natively-flipped shared region: a native sibling may have
            // called region.flip(...) in place on this same static AtlasRegion. A per-instance
            // MIRROR (NRO-04 F22) is applied to that canonical view as a UV swap, never a negative
            // scale.
            TextureRegion canonical = canonicalRegion(gdx, f.mirrorX, f.mirrorY);
            if (canonical == null) return false;
            float settingsScale = Settings.scale;
            // getRegionWidth()/getRegionHeight() are the flip-invariant packed footprint, so they
            // equal the canonical region's width/height even when the shared region was flipped.
            VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                    kind, f.x, f.y, f.vY, f.scale, f.rotation, f.durDiv2, f.duration,
                    settingsScale, gdx.getRegionWidth(), gdx.getRegionHeight(),
                    f.vX, f.regionOffsetX, f.regionOffsetY,
                    originOffsetX(kind), originOffsetY(kind, gdx), scaleYMultiplier(kind));
            // F24 PLAYER-HITBOX-RELATIVE X: WrathStanceChangeParticle draws at the player's hitbox
            // center X plus the effect's own x, not at x. Resolved here (a fail-open-capable read) so
            // that a missing player/hitbox declines BEFORE any RNG is pulled; native then draws.
            float drawX = p.x;
            if (VfxDrawGeometry.playerHitboxRelativeX(kind)) {
                Float centerX = playerHitboxCenterX();
                if (centerX == null) return false;
                drawX = centerX.floatValue() + f.x;
            }
            // F24 RNG-REPLAY: every fallible check above is complete, so we are now committed to the
            // draw. Snapshot the shared global RNG state BEFORE pulling any value so a
            // post-consumption failure can restore the stream exactly (the native fallback then
            // consumes the values it needs, and the global stream does not drift). For a kind whose
            // native render consumes MathUtils.random(...) values, pull them IN ORDER from the
            // global stream (matching the native call sequence exactly) and apply them to the
            // corresponding scale component; a kind with no ranges keeps scale, scale and never
            // touches the RNG or the snapshot.
            java.util.List<float[]> ranges = VfxDrawGeometry.randomRanges(kind);
            long[] rngSnapshot = ranges.isEmpty() ? null : rngSnapshot();
            // Per-kind blend policy: most kinds install additive blend and restore it, but the
            // ambient kinds (VfxDrawGeometry.additiveBlend(kind) == false — see that method for the
            // authoritative list, which includes the newest DustEffect)
            // never call setBlendFunction natively, so they must draw under
            // the ambient blend and restore only color.
            boolean additive = VfxDrawGeometry.additiveBlend(kind);
            Color previous = new Color(sb.getColor());
            boolean blendChanged = false;
            try {
                float scaleX = p.scaleX;
                float scaleY = p.scaleY;
                for (int i = 0; i < ranges.size(); i++) {
                    float[] range = ranges.get(i);
                    float factor = com.badlogic.gdx.math.MathUtils.random(range[0], range[1]);
                    if (i == 0) {
                        scaleX = p.scaleX * factor;
                    } else if (i == 1) {
                        scaleY = p.scaleY * factor;
                    }
                }
                sb.setColor(f.color != null ? f.color : Color.WHITE);
                if (additive) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    blendChanged = true;
                }
                sb.draw(canonical, drawX, p.y, p.originX, p.originY, p.width, p.height,
                        scaleX, scaleY, p.rotation);
                return true;
            } catch (Throwable ignored) {
                // A throw after the RNG values were pulled must not leak RNG consumption: restore the
                // snapshot so the native fallback consumes exactly the values it expects.
                restoreRng(rngSnapshot);
                return false;
            } finally {
                try {
                    if (blendChanged) {
                        sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    }
                    sb.setColor(previous);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * FULL-SCREEN bare static-{@code Texture} branch for {@code SpotlightEffect} (NRO-04 B07) and
     * {@code CampfireRecallEffect} (NRO-04 B08), served BEFORE the generic
     * {@link #readTextureFields} path because both classes have NO {@code x}/{@code y} field and
     * their native {@code render} consumes only a color (the inherited {@code scale}/{@code rotation}
     * are unused and not required) — their native draw is fixed screen geometry. Reproduces the
     * native render exactly: for {@code SPOTLIGHT} {@code setColor(color); setBlendFunction(770, 1);
     * sb.draw(ImageMaster.SPOTLIGHT_VFX, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
     * setBlendFunction(770, 771)} — ADDITIVE, the effect's OWN {@code color}; for
     * {@code CAMPFIRE_RECALL} {@code sb.setColor(screenColor);
     * sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0f, 0f, Settings.WIDTH, Settings.HEIGHT)} — AMBIENT (no
     * blend switch) and the effect's OWN {@code screenColor} field (NOT the inherited {@code color}).
     * The color field to read is selected by
     * {@link VfxDrawGeometry#fullScreenTextureReadsScreenColor} (position {@code (0f, 0f)} and size
     * {@code (Settings.WIDTH, Settings.HEIGHT)} for both).
     *
     * <p>There is exactly ONE draw, so a throw before it fails open (returns {@code false} so native
     * draws); blend (only when the kind is additive) and color are always restored in {@code finally}.
     * Never throws.
     */
    private boolean renderFullScreenTexture(SpriteBatch sb, VfxDrawGeometry.Kind kind,
            AbstractGameEffect effect) {
        Texture texture = resolveTexture(kind, null);
        if (texture == null) return false;
        String colorField = VfxDrawGeometry.fullScreenTextureReadsScreenColor(kind)
                ? "screenColor" : "color";
        Color color = readColor(effect, colorField);
        if (color == null) return false;
        boolean additive = VfxDrawGeometry.additiveBlend(kind);
        Color previous = new Color(sb.getColor());
        boolean blendChanged = false;
        try {
            sb.setColor(color);
            if (additive) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                blendChanged = true;
            }
            // The 4-arg texture overload: position (0f, 0f) and size (Settings.WIDTH, Settings.HEIGHT),
            // exactly as the native effect draws (no origin/rotation/scale).
            sb.draw(texture, 0f, 0f, Settings.WIDTH, Settings.HEIGHT);
            return true;
        } catch (Throwable ignored) {
            // Single draw: a throw here painted nothing (or is unrecoverable), so fail open to native.
            return false;
        } finally {
            try {
                if (blendChanged) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
                sb.setColor(previous);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Required color read: the effect's own {@link Color}, or {@code null} when the field is
     * absent/unreadable/wrong-typed. Used by the full-screen {@code SpotlightEffect} branch, whose
     * native {@code setColor(color)} consumes a non-null color (a real instance always has one).
     */
    private static Color readColor(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Color ? (Color) raw : null;
    }

    /**
     * Resolves the {@link Color} the bare-{@code Texture} branch passes to
     * {@code SpriteBatch.setColor}. The newest (NRO-04 B06) {@code MAP_CIRCLE} returns its
     * HARDCODED {@code (0.09f, 0.13f, 0.17f, 1f)} regardless of the effect color (a real
     * {@code MapCircleEffect} has a null color and the native render ignores it). For every other
     * kind null collapses to white (the same default the img-based branch
     * uses); for a kind that {@link VfxDrawGeometry#whiteAlphaOnly} reports true for, the RGB
     * channels are forced to {@code 1f} and only the effect color's alpha is kept, mirroring the
     * native {@code new Color(1f, 1f, 1f, color.a)}; every other kind passes the color through
     * unchanged. Never mutates the effect's color and never throws (a null still fails safe).
     */
    static Color resolveColor(VfxDrawGeometry.Kind kind, Color effectColor) {
        if (kind == VfxDrawGeometry.Kind.MAP_CIRCLE) {
            // MapCircleEffect.render hardcodes its setColor to new Color(0.09f, 0.13f, 0.17f, 1f);
            // the effect's OWN color field is IGNORED. This is NOT the white-alpha rule (it is a
            // distinct hardcoded dark color), so it is handled before the null/white-alpha checks.
            return new Color(VfxDrawGeometry.MAP_CIRCLE_COLOR_R,
                    VfxDrawGeometry.MAP_CIRCLE_COLOR_G,
                    VfxDrawGeometry.MAP_CIRCLE_COLOR_B,
                    VfxDrawGeometry.MAP_CIRCLE_COLOR_A);
        }
        if (effectColor == null) {
            return Color.WHITE;
        }
        if (VfxDrawGeometry.whiteAlphaOnly(kind)) {
            return new Color(1f, 1f, 1f, effectColor.a);
        }
        return effectColor;
    }

    /**
     * Immutable snapshot of the fields the VARIABLE-LENGTH MULTI-DRAW {@code FlyingOrbEffect} draw
     * needs (NRO-04 B01): the effect's own {@code Vector2[] points}, the inherited {@code color}, the
     * static {@code img} {@link TextureAtlas.AtlasRegion}, and the effect's OWN {@code rotation} field
     * (not inherited — the class declares it). A freshly spawned instance's {@code points} array is
     * allocated but all entries are null until {@code update()} fills them, so it draws nothing until
     * then.
     */
    static final class FlyingOrbFields {
        final Vector2[] points;
        final Color color;
        final TextureAtlas.AtlasRegion img;
        final float rotation;

        FlyingOrbFields(Vector2[] points, Color color, TextureAtlas.AtlasRegion img,
                float rotation) {
            this.points = points;
            this.color = color;
            this.img = img;
            this.rotation = rotation;
        }
    }

    /**
     * Snapshots the fields the {@code FlyingOrbEffect} draw needs. Required: {@code points}
     * ({@code Vector2[]}), {@code color} ({@link Color}), {@code img}
     * ({@link TextureAtlas.AtlasRegion}), and {@code rotation} (any {@link Number}; the class declares
     * its own field). Returns {@code null} when the effect is null or any required field is absent,
     * unreadable, or of the wrong type; never throws.
     */
    static FlyingOrbFields readFlyingOrbFields(Object effect) {
        if (effect == null) return null;
        try {
            Object points = readRaw(effect, "points");
            if (!(points instanceof Vector2[])) return null;
            Float rotation = readFloat(effect, "rotation");
            if (rotation == null) return null;
            Object color = readRaw(effect, "color");
            if (!(color instanceof Color)) return null;
            Object img = readRaw(effect, "img");
            if (!(img instanceof TextureAtlas.AtlasRegion)) return null;
            return new FlyingOrbFields((Vector2[]) points, (Color) color,
                    (TextureAtlas.AtlasRegion) img, rotation.floatValue());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Immutable snapshot of the fields the single-draw img-path {@code FlickCoinEffect} draw needs
     * (NRO-04 B02). Its position fields are {@code cX}/{@code cY}/{@code yOffset} (NOT
     * {@code x}/{@code y}), so it is served by a dedicated reader rather than the generic
     * {@link #readFields}. The inherited {@code scale} is required (read from the inherited
     * {@code AbstractGameEffect.scale} field; the class has no {@code scale} of its own), as are the
     * inherited {@code color} and the static {@code img} {@link TextureAtlas.AtlasRegion}.
     */
    static final class FlickCoinFields {
        final float cX;
        final float cY;
        final float yOffset;
        final float scale;
        final float rotation;
        final Color color;
        final TextureAtlas.AtlasRegion img;

        FlickCoinFields(float cX, float cY, float yOffset, float scale, float rotation, Color color,
                TextureAtlas.AtlasRegion img) {
            this.cX = cX;
            this.cY = cY;
            this.yOffset = yOffset;
            this.scale = scale;
            this.rotation = rotation;
            this.color = color;
            this.img = img;
        }
    }

    /**
     * Snapshots the fields the {@code FlickCoinEffect} draw needs (NRO-04 B02). Required:
     * {@code cX}, {@code cY}, {@code yOffset} and {@code rotation} (any {@link Number}),
     * {@code scale} (any {@link Number}, the inherited field), {@code color} ({@link Color}), and
     * {@code img} ({@link TextureAtlas.AtlasRegion}); an absent/unreadable/wrongly typed required
     * field fails the snapshot ({@code null}). Never throws.
     */
    static FlickCoinFields readFlickCoinFields(Object effect) {
        if (effect == null) return null;
        try {
            Float cX = readFloat(effect, "cX");
            Float cY = readFloat(effect, "cY");
            Float yOffset = readFloat(effect, "yOffset");
            Float scale = readFloat(effect, "scale");
            Float rotation = readFloat(effect, "rotation");
            if (cX == null || cY == null || yOffset == null || scale == null || rotation == null) {
                return null;
            }
            Object color = readRaw(effect, "color");
            if (!(color instanceof Color)) return null;
            Object img = readRaw(effect, "img");
            if (!(img instanceof TextureAtlas.AtlasRegion)) return null;
            return new FlickCoinFields(cX.floatValue(), cY.floatValue(), yOffset.floatValue(),
                    scale.floatValue(), rotation.floatValue(), (Color) color,
                    (TextureAtlas.AtlasRegion) img);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Dedicated single-draw img-path branch for {@code FlickCoinEffect} (NRO-04 B02), served BEFORE
     * the generic {@link #readFields} path because the class has NO {@code x}/{@code y} field (its
     * position fields are {@code cX}/{@code cY}/{@code yOffset}). Reproduces the native render
     * exactly: ADDITIVE blend installed/restored, the effect's own {@code color}, the draw POSITION
     * {@code (cX - packedWidth/2, cY - packedHeight/2 + yOffset)} using native INTEGER division of the
     * packed size, the draw ORIGIN {@code (packedWidth/2f, packedHeight/2f)} using native FLOAT
     * division (differing by {@code 0.5} for an odd region), the packed size, and the ANISOTROPIC
     * scale ({@code scale * }{@link VfxDrawGeometry#FLICK_COIN_SCALE_X},
     * {@code scale * }{@link VfxDrawGeometry#FLICK_COIN_SCALE_Y}) with the effect's own
     * {@code rotation} — the per-axis multipliers are applied by
     * {@link VfxDrawGeometry#params}, which this branch calls with the effect's {@code cX}/{@code cY}/
     * {@code yOffset} mapped onto its {@code x}/{@code y}/{@code vY} inputs.
     *
     * <p>There is exactly ONE draw, so a throw before it fails open (returns {@code false} so native
     * draws); blend/color are always restored in {@code finally}. Never throws.
     */
    private boolean renderFlickCoin(SpriteBatch sb, AbstractGameEffect effect) {
        FlickCoinFields f = readFlickCoinFields(effect);
        if (f == null) return false;
        TextureAtlas.AtlasRegion gdx = f.img;
        if (gdx == null || gdx.getTexture() == null) return false;
        AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
        if (neutral == null || !neutral.valid()) return false;
        TextureRegion canonical = canonicalRegion(gdx, false, false);
        if (canonical == null) return false;
        // Pass cX/cY/yOffset through the shared x/y/vY inputs; the FLICK_COIN params branch applies
        // the native INTEGER-half position offset, FLOAT-half origin, packed size, and anisotropic
        // scale. The effect's own rotation is passed through unchanged.
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                VfxDrawGeometry.Kind.FLICK_COIN,
                f.cX, f.cY, f.yOffset, f.scale, f.rotation, 0f, 0f, Settings.scale,
                gdx.getRegionWidth(), gdx.getRegionHeight(), 0f, 0f, 0f, 0f, 0f, 1f);
        boolean additive = VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLICK_COIN);
        Color previous = new Color(sb.getColor());
        boolean blendChanged = false;
        try {
            sb.setColor(f.color != null ? f.color : Color.WHITE);
            if (additive) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                blendChanged = true;
            }
            sb.draw(canonical, p.x, p.y, p.originX, p.originY, p.width, p.height,
                    p.scaleX, p.scaleY, p.rotation);
            return true;
        } catch (Throwable ignored) {
            // Single draw: a throw here painted nothing, so fail open to native.
            return false;
        } finally {
            try {
                if (blendChanged) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
                sb.setColor(previous);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Narrow package-private capability seam: {@code true} for the VARIABLE-LENGTH MULTI-DRAW kinds
     * ({@link VfxDrawGeometry#variableLengthMultiDraw}), today only
     * {@link VfxDrawGeometry.Kind#FLYING_ORB}. Delegates to the pure predicate so the renderer and the
     * geometry stay in sync.
     */
    static boolean usesVariableLengthDraw(VfxDrawGeometry.Kind kind) {
        return VfxDrawGeometry.variableLengthMultiDraw(kind);
    }

    /**
     * VARIABLE-LENGTH MULTI-DRAW branch (NRO-04 B01): replays {@code FlyingOrbEffect.render} exactly.
     *
     * <p>The native render first returns when {@code isDone} (already handled by
     * {@link #guardSatisfied}, so a blocked instance never reaches here). Otherwise, under the additive
     * blend ({@code setBlendFunction(770, 1)} before, {@code 770, 771} after) and with the effect's
     * {@code color}, it iterates {@code index} from {@code points.length - 1} DOWN TO {@code 1} (index
     * {@code 0} is NEVER drawn) and, for each NON-NULL {@code points[index]}, issues one center-packed
     * draw with the native integer-divided position offset ({@code px - (packedWidth/2)},
     * {@code py - (packedHeight/2)}) and the native float-divided center origin ({@code pw/2f},
     * {@code ph/2f}) — for an odd region these differ by {@code 0.5f} — with a
     * UNIFORM scale {@code s} that starts at {@code Settings.scale *
     * VfxDrawGeometry.flyingOrbStartScaleMultiplier()} and is multiplied by
     * {@code VfxDrawGeometry.flyingOrbScaleDecayPerDraw()} AFTER each DRAWN point. No RNG is consumed.
     *
     * <p>B09 multi-pass failure contract: if a draw throws after at least one draw succeeded, the
     * claim keeps the frame (returns {@code true}, so native never double-draws) and RNG is left
     * alone (there is no RNG here); if it throws before any draw, this fails open (returns
     * {@code false}) so native draws. Blend/color are always restored in {@code finally}.
     */
    private boolean renderFlyingOrb(SpriteBatch sb, AbstractGameEffect effect) {
        FlyingOrbFields f = readFlyingOrbFields(effect);
        if (f == null) return false;
        TextureAtlas.AtlasRegion gdx = f.img;
        if (gdx == null || gdx.getTexture() == null) return false;
        AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
        if (neutral == null || !neutral.valid()) return false;
        TextureRegion canonical = canonicalRegion(gdx, false, false);
        if (canonical == null) return false;
        // Native integer-vs-float division split (verified bytecode): the draw POSITION offset uses
        // INTEGER division of the packed size (packedWidth:I; iconst_2; idiv; i2f; fsub), while the
        // draw ORIGIN uses FLOAT division (packedWidth:I; i2f; fconst_2; fdiv). For an odd region
        // (ImageMaster.GLOW_SPARK_2 is 81x81) that is x - 40 with origin 40.5, NOT x - 40.5.
        int regionWidth = gdx.getRegionWidth();
        int regionHeight = gdx.getRegionHeight();
        float pw = regionWidth;
        float ph = regionHeight;
        float posOffsetX = regionWidth / 2; // integer division, matching native packedWidth/2
        float posOffsetY = regionHeight / 2;
        float scale0 = Settings.scale * VfxDrawGeometry.flyingOrbStartScaleMultiplier();
        boolean additive = VfxDrawGeometry.additiveBlend(VfxDrawGeometry.Kind.FLYING_ORB);
        Color previous = new Color(sb.getColor());
        boolean blendChanged = false;
        boolean drewAny = false;
        try {
            sb.setColor(f.color != null ? f.color : Color.WHITE);
            if (additive) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                blendChanged = true;
            }
            float s = scale0;
            for (int index = f.points.length - 1; index >= 1; index--) {
                Vector2 point = f.points[index];
                if (point == null) continue;
                sb.draw(canonical, point.x - posOffsetX, point.y - posOffsetY,
                        pw / 2f, ph / 2f, pw, ph, s, s, f.rotation);
                s *= VfxDrawGeometry.flyingOrbScaleDecayPerDraw();
                drewAny = true;
            }
            return true;
        } catch (Throwable ignored) {
            // B09 multi-pass failure contract. POST-draw: at least one point already painted, so
            // claim the frame (return true) so native never repaints the whole orb; no RNG to restore.
            if (drewAny) {
                return true;
            }
            return false;
        } finally {
            try {
                if (blendChanged) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
                sb.setColor(previous);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * True only when {@link #render} could actually draw this exact instance: the class maps to a
     * kind, the native draw guard is satisfied (a guard-blocked kind cannotDraw — native draws
     * nothing in that state either), the required fields resolve, the resolved region/texture is
     * present, and — for a player-hitbox-relative kind (F24) — the player hitbox center X resolves.
     * Mirrors
     * {@code render}'s early-return conditions up to (but not including) the batch draw, so a
     * {@code false} here means the native effect would also produce no pixels for this instance
     * under this renderer. Never throws.
     */
    @Override
    public boolean canDraw(Object effect) {
        if (effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            // Native draw guard (NRO-04 F21): the same check render performs. A guard-blocked
            // instance cannotDraw, which lets the bridge classify the decline as a benign no-pixel
            // decline (native draws nothing in that state either).
            if (!guardSatisfied(kind, effect)) return false;
            return imagePresent(kind, effect);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * True when this exact instance's own image source is present, using the SAME field/resource
     * checks {@link #render} performs up to the batch draw: for the bare-{@code Texture} kinds the
     * snapshot must resolve and its {@link Texture} (instance {@code img} or the resolved static
     * {@code ImageMaster} texture) must be non-null; for the img kinds the snapshot's
     * {@link TextureAtlas.AtlasRegion} must be present, texturable, and convert to a valid neutral
     * region. Shared by {@link #canDraw} and {@link #declinedWithoutPixels} so the two stay
     * consistent. Never throws.
     */
    private static boolean imagePresent(VfxDrawGeometry.Kind kind, Object effect) {
        if (kind == null || effect == null) return false;
        // NRO-04 B01: FlyingOrbEffect has NO x/y/scale; its draw is present iff its img/region is
        // present (and, handled by the caller's guard, isDone is false). Read its own snapshot.
        if (kind == VfxDrawGeometry.Kind.FLYING_ORB) {
            FlyingOrbFields f = readFlyingOrbFields(effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            return neutral != null && neutral.valid();
        }
        // NRO-04 B02: FlickCoinEffect has NO x/y/scale beyond the inherited scale; its draw is
        // present iff its img region is present (read via its dedicated reader).
        if (kind == VfxDrawGeometry.Kind.FLICK_COIN) {
            FlickCoinFields f = readFlickCoinFields(effect);
            if (f == null) return false;
            TextureAtlas.AtlasRegion gdx = f.img;
            if (gdx == null || gdx.getTexture() == null) return false;
            AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
            return neutral != null && neutral.valid();
        }
        // F24 PLAYER-HITBOX-RELATIVE X: a kind that draws relative to the player's hitbox cannot draw
        // without that center, so a missing player/hitbox makes the instance undrawable (native draws
        // it instead). Resolved here so canDraw/declinedWithoutPixels agree with render.
        if (VfxDrawGeometry.playerHitboxRelativeX(kind) && playerHitboxCenterX() == null) {
            return false;
        }
        if (isTextureDrawKind(kind)) {
            // NRO-04 B07/B08: SpotlightEffect and CampfireRecallEffect have NO own x/y field and their
            // native render consumes only a color (the inherited scale/rotation are unused and not
            // required), so readTextureFields (which requires x/y/scale) does not apply; their drawable
            // image is exactly the static texture (SPOTLIGHT_VFX / WHITE_SQUARE_IMG) plus a non-null
            // effect color. The color field read is kind-specific (Spotlight: color;
            // CampfireRecall: screenColor).
            if (VfxDrawGeometry.fullScreenTexture(kind)) {
                String colorField = VfxDrawGeometry.fullScreenTextureReadsScreenColor(kind)
                        ? "screenColor" : "color";
                return resolveTexture(kind, null) != null
                        && readColor(effect, colorField) != null;
            }
            TextureFields f = readTextureFields(kind, effect);
            return f != null && resolveTexture(kind, f) != null;
        }
        Fields f = readFields(kind, effect);
        if (f == null) return false;
        TextureAtlas.AtlasRegion gdx = f.img;
        if (gdx == null || gdx.getTexture() == null) return false;
        AtlasRegion neutral = Sts1GdxAtlasRegions.fromGdx(gdx);
        return neutral != null && neutral.valid();
    }

    /**
     * True for the bare-{@code Texture} (fixed source-rect) kinds that {@link #render} serves through
     * {@link #renderTexture} rather than the packed-region path. The single source of truth shared by
     * {@link #render}, {@link #canDraw}, and {@link #imagePresent} so a kind cannot be routed one way
     * by one and another way by another.
     */
    private static boolean isTextureDrawKind(VfxDrawGeometry.Kind kind) {
        return kind == VfxDrawGeometry.Kind.CALM_PARTICLE
                || kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE
                || kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE
                || kind == VfxDrawGeometry.Kind.ICE_SHATTER
                || kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE
                || kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE
                || kind == VfxDrawGeometry.Kind.WARNING_SIGN
                || kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE
                || kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                || kind == VfxDrawGeometry.Kind.TORCH_HEAD_FIRE
                || kind == VfxDrawGeometry.Kind.FALLING_ICE
                || kind == VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION
                || kind == VfxDrawGeometry.Kind.HEAL_PANEL
                || kind == VfxDrawGeometry.Kind.PING_HP
                || kind == VfxDrawGeometry.Kind.REWARD_GLOW
                || kind == VfxDrawGeometry.Kind.MAP_CIRCLE
                || kind == VfxDrawGeometry.Kind.SPOTLIGHT
                || kind == VfxDrawGeometry.Kind.CAMPFIRE_RECALL;
    }

    /**
     * The value the bare-{@code Texture} branch passes into the trailing {@code settingsHeight}
     * {@link VfxDrawGeometry#params} slot. For {@link VfxDrawGeometry.Kind#HEAL_PANEL} and
     * {@link VfxDrawGeometry.Kind#PING_HP} that slot is the native {@code Settings.HEIGHT}
     * (panel-space Y); the newest (NRO-04 B05) {@link VfxDrawGeometry.Kind#REWARD_GLOW} reuses the
     * same slot to carry the native {@code Settings.xScale} (its draw X scale is
     * {@code Settings.xScale}, independent of the effect's own {@code scale}). Every other kind
     * ignores the slot; passing {@code Settings.HEIGHT} for them keeps their results byte-identical.
     */
    private static float textureDrawSettingsSlot(VfxDrawGeometry.Kind kind) {
        if (kind == VfxDrawGeometry.Kind.REWARD_GLOW) {
            return Settings.xScale;
        }
        return Settings.HEIGHT;
    }

    /**
     * Instance-aware benign no-pixel probe (NRO-04 F21): true only when THIS exact instance would
     * natively draw NOTHING, so an ART decline loses no pixels. That is exactly two cases: (a) a kind
     * whose native render guards its draw on a present image
     * ({@link VfxDrawGeometry#nativeSkipsDrawWithoutImage}) and whose instance has no drawable image,
     * or (b) a wait-phase-guarded kind ({@link VfxDrawGeometry#nativeSkipsDrawByGuard}) whose guard
     * field value blocks the native draw per that kind's threshold
     * ({@link VfxDrawGeometry#guardBlocks} — the source of truth). Every other instance —
     * including a guard-SATISFIED instance whose image snapshot fails — is {@code false}, so a
     * genuine renderer failure is never masked as benign. Deliberately narrower than
     * {@link #canDraw}, which also reports {@code false} for any other undrawable instance. Never
     * throws.
     */
    @Override
    public boolean declinedWithoutPixels(Object effect) {
        if (effect == null) return false;
        try {
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.kindFor(effect.getClass().getName());
            if (kind == null) return false;
            if (VfxDrawGeometry.nativeSkipsDrawWithoutImage(kind)) {
                // The native render draws only with a present image, so a missing image is benign.
                return !imagePresent(kind, effect);
            }
            if (VfxDrawGeometry.nativeSkipsDrawByGuard(kind)) {
                // The native render draws only when the guard does not block (per-kind threshold).
                return !guardSatisfied(kind, effect);
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves the native {@link Texture} the bare-{@code Texture} kinds draw (the static
     * {@link ImageMaster} textures for Calm/Shield/Web/Entangle/Spotlight/CampfireRecall, the
     * instance {@code img} for Debuff/IceShatter/Unknown/DarkOrb/FallingIce and the flip kinds
     * LightningOrbPassive/GlowyFireEyes/TorchHeadFire),
     * or {@code null} when it is absent. Shared by
     * {@link #renderTexture} and {@link #canDraw} so the two stay consistent.
     */
    private static Texture resolveTexture(VfxDrawGeometry.Kind kind, TextureFields f) {
        if (kind == VfxDrawGeometry.Kind.WARNING_SIGN) {
            // WarningSignEffect draws the static ImageMaster.WARNING_ICON_VFX Texture.
            return ImageMaster.WARNING_ICON_VFX;
        }
        if (kind == VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION) {
            // StanceChangeAbsorptionParticle draws the static ImageMaster.WOBBLY_ORB_VFX Texture
            // (it has no instance Texture img).
            return ImageMaster.WOBBLY_ORB_VFX;
        }
        if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
            return ImageMaster.INTENT_DEFEND;
        }
        if (kind == VfxDrawGeometry.Kind.PING_HP) {
            // PingHpEffect draws the STATIC ImageMaster.TP_HP Texture (it has no instance Texture img).
            return ImageMaster.TP_HP;
        }
        if (kind == VfxDrawGeometry.Kind.REWARD_GLOW) {
            // RewardGlowEffect.render(SpriteBatch) draws the STATIC ImageMaster.REWARD_SCREEN_ITEM
            // Texture (the class has NO instance Texture img).
            return ImageMaster.REWARD_SCREEN_ITEM;
        }
        if (kind == VfxDrawGeometry.Kind.MAP_CIRCLE) {
            // MapCircleEffect draws its own PUBLIC STATIC Texture img (set in the ctor and swapped by
            // update(); there is NO instance img field). readTextureFields has already read the
            // static field through readRaw and requires it to be a non-null Texture.
            return f.img;
        }
        if (kind == VfxDrawGeometry.Kind.SPOTLIGHT) {
            // SpotlightEffect draws the static ImageMaster.SPOTLIGHT_VFX Texture (it has NO instance
            // img field; its native render consumes only color). May be null off-game; a null fails open.
            return ImageMaster.SPOTLIGHT_VFX;
        }
        if (kind == VfxDrawGeometry.Kind.CAMPFIRE_RECALL) {
            // CampfireRecallEffect draws the static ImageMaster.WHITE_SQUARE_IMG Texture (it has NO
            // instance img field; its native render consumes only its screenColor field). May be null
            // off-game; a null fails open.
            return ImageMaster.WHITE_SQUARE_IMG;
        }
        if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE) {
            // WebParticleEffect and EntangleEffect share the static ImageMaster.WEB_VFX Texture.
            return ImageMaster.WEB_VFX;
        }
        if (usesInstanceTexture(kind)) {
            return f.img;
        }
        return ImageMaster.FROST_ACTIVATE_VFX_1;
    }

    /**
     * Bare-{@code Texture} branch: resolves the native texture the kind's {@code render} reads
     * ({@link ImageMaster#FROST_ACTIVATE_VFX_1} for Calm, {@link ImageMaster#INTENT_DEFEND} for
     * Shield, {@link ImageMaster#WEB_VFX} for Web and Entangle, the effect's own {@code img} for
     * Debuff, IceShatter, Unknown, DarkOrb, LightningOrbPassive, GlowyFireEyes, TorchHeadFire, and
     * FallingIce),
     * replays the raw
     * texture + source-rect draw with the
     * {@link VfxDrawGeometry#params}
     * geometry and the per-kind src rect, passes the per-instance flip booleans for the kinds whose
     * native render supplies them ({@link VfxDrawGeometry#usesInstanceFlipX}/
     * {@link VfxDrawGeometry#usesInstanceFlipY} plus the {@code flippedX} kinds
     * {@link VfxDrawGeometry#usesTexturedFlipX}; {@code false, false} for every other kind), honors
     * {@link VfxDrawGeometry#additiveBlend}, applies the
     * {@link VfxDrawGeometry#whiteAlphaOnly} color rule (Web and Entangle force the set color's RGB
     * to white),
     * and restores blend/color. Fails open ({@code false}, no side effects) on any missing input.
     *
     * <p>MULTI-DRAW (NRO-04 F25): a kind whose {@link VfxDrawGeometry#drawPassRandomRanges} is
     * non-empty (today only {@code STANCE_CHANGE_ABSORPTION}) issues several RNG-consuming draws. The
     * shared RNG state is snapshotted ONCE before any pass; then for each pass in order, each inner
     * range in order pulls one {@code MathUtils.random(min, max)} and multiplies into scaleX (first)
     * then scaleY (second), and the pass's draw is replayed with those scales.
     *
     * <p>MULTI-DRAW FAILURE CONTRACT (NRO-04 B09), which future variable-length/repeated-draw
     * native kinds depend on:
     * <ul>
     *   <li><b>Pre-pass failure</b> (no pass has painted pixels yet — the very first
     *       {@code sb.draw} throws): the claim draws NOTHING, restores the RNG snapshot so the native
     *       fallback re-consumes exactly the values it expects, and returns {@code false} to fail
     *       open to native.</li>
     *   <li><b>Post-pass failure</b> (at least one pass already painted pixels): the claim has
     *       produced pixels, so it must NOT also let native paint all passes (that would double-draw
     *       the earlier passes). The drawn passes' RNG consumption must stand, so the snapshot is
     *       NOT restored, and the method returns {@code true} — the instance is treated as
     *       ART-owned for this frame (no native double-draw).</li>
     * </ul>
     * The single-draw path ({@code drawPasses.isEmpty()}) is unchanged: a single draw cannot
     * partially succeed, so any throw restores the snapshot and fails open. Every other kind keeps
     * the single-draw path (no RNG unless it is {@code WRATH_STANCE_CHANGE} on the img path).
     */
    private boolean renderTexture(SpriteBatch sb, VfxDrawGeometry.Kind kind,
            AbstractGameEffect effect) {
        // NRO-04 B07: SpotlightEffect has NO own x/y field and its native render consumes only color
        // (the inherited scale/rotation are unused and not required), so it is served by a dedicated
        // branch BEFORE the generic readTextureFields path (which requires x/y/scale). Its draw reads
        // ONLY the effect's own color; the position/size are fixed screen geometry
        // (0, 0, Settings.WIDTH, Settings.HEIGHT).
        if (VfxDrawGeometry.fullScreenTexture(kind)) {
            return renderFullScreenTexture(sb, kind, effect);
        }
        TextureFields f = readTextureFields(kind, effect);
        if (f == null) return false;
        Texture texture = resolveTexture(kind, f);
        if (texture == null) return false;
        VfxDrawGeometry.Params p = VfxDrawGeometry.params(
                kind, f.x, f.y, 0f, f.scale, f.rotation,
                f.durDiv2, f.duration, Settings.scale, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f,
                textureDrawSettingsSlot(kind));
        int srcX;
        int srcY;
        int srcW;
        int srcH;
        if (kind == VfxDrawGeometry.Kind.WARNING_SIGN) {
            // WarningSignEffect draws the static ImageMaster.WARNING_ICON_VFX Texture's full rect;
            // the geometry already carries the hardcoded Settings.scale * 2f uniform scale.
            srcX = VfxDrawGeometry.WARNING_SRC_X;
            srcY = VfxDrawGeometry.WARNING_SRC_Y;
            srcW = VfxDrawGeometry.WARNING_SRC_W;
            srcH = VfxDrawGeometry.WARNING_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.SHIELD_PARTICLE) {
            srcX = VfxDrawGeometry.SHIELD_SRC_X;
            srcY = VfxDrawGeometry.SHIELD_SRC_Y;
            srcW = VfxDrawGeometry.SHIELD_SRC_W;
            srcH = VfxDrawGeometry.SHIELD_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.DEBUFF_PARTICLE) {
            srcX = VfxDrawGeometry.DEBUFF_SRC_X;
            srcY = VfxDrawGeometry.DEBUFF_SRC_Y;
            srcW = VfxDrawGeometry.DEBUFF_SRC_W;
            srcH = VfxDrawGeometry.DEBUFF_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.ICE_SHATTER) {
            srcX = VfxDrawGeometry.ICE_SHATTER_SRC_X;
            srcY = VfxDrawGeometry.ICE_SHATTER_SRC_Y;
            srcW = VfxDrawGeometry.ICE_SHATTER_SRC_W;
            srcH = VfxDrawGeometry.ICE_SHATTER_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.UNKNOWN_PARTICLE) {
            srcX = VfxDrawGeometry.UNKNOWN_SRC_X;
            srcY = VfxDrawGeometry.UNKNOWN_SRC_Y;
            srcW = VfxDrawGeometry.UNKNOWN_SRC_W;
            srcH = VfxDrawGeometry.UNKNOWN_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.DARK_ORB_PASSIVE) {
            // DarkOrbPassiveEffect passes the full 74x74 region as its src rect.
            srcX = VfxDrawGeometry.DARK_ORB_SRC_X;
            srcY = VfxDrawGeometry.DARK_ORB_SRC_Y;
            srcW = VfxDrawGeometry.DARK_ORB_SRC_W;
            srcH = VfxDrawGeometry.DARK_ORB_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE) {
            // LightningOrbPassiveEffect passes the full 122x122 rect as its src rect.
            srcX = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_X;
            srcY = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_Y;
            srcW = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_W;
            srcH = VfxDrawGeometry.LIGHTNING_ORB_PASSIVE_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.GLOWY_FIRE_EYES
                || kind == VfxDrawGeometry.Kind.TORCH_HEAD_FIRE) {
            // GlowyFireEyesEffect and TorchHeadFireEffect share the full 128x128 src rect.
            srcX = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_X;
            srcY = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_Y;
            srcW = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_W;
            srcH = VfxDrawGeometry.GLOWY_FIRE_EYES_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.FALLING_ICE) {
            // FallingIceEffect passes the full 96x96 rect as its src rect.
            srcX = VfxDrawGeometry.FALLING_ICE_SRC_X;
            srcY = VfxDrawGeometry.FALLING_ICE_SRC_Y;
            srcW = VfxDrawGeometry.FALLING_ICE_SRC_W;
            srcH = VfxDrawGeometry.FALLING_ICE_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION) {
            // StanceChangeAbsorptionParticle passes the full 32x32 rect as its src rect.
            srcX = VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_X;
            srcY = VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_Y;
            srcW = VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_W;
            srcH = VfxDrawGeometry.STANCE_CHANGE_ABSORPTION_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.HEAL_PANEL) {
            // HealPanelEffect draws the fixed 64x64 src rect of its static Texture.
            srcX = VfxDrawGeometry.HEAL_PANEL_SRC_X;
            srcY = VfxDrawGeometry.HEAL_PANEL_SRC_Y;
            srcW = VfxDrawGeometry.HEAL_PANEL_SRC_W;
            srcH = VfxDrawGeometry.HEAL_PANEL_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.PING_HP) {
            // PingHpEffect draws the fixed 64x64 src rect of the static ImageMaster.TP_HP Texture.
            srcX = VfxDrawGeometry.PING_HP_SRC_X;
            srcY = VfxDrawGeometry.PING_HP_SRC_Y;
            srcW = VfxDrawGeometry.PING_HP_SRC_W;
            srcH = VfxDrawGeometry.PING_HP_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.REWARD_GLOW) {
            // RewardGlowEffect.render(SpriteBatch) draws the FIXED 464x98 src rect of the static
            // ImageMaster.REWARD_SCREEN_ITEM Texture.
            srcX = VfxDrawGeometry.REWARD_GLOW_SRC_X;
            srcY = VfxDrawGeometry.REWARD_GLOW_SRC_Y;
            srcW = VfxDrawGeometry.REWARD_GLOW_SRC_W;
            srcH = VfxDrawGeometry.REWARD_GLOW_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.MAP_CIRCLE) {
            // MapCircleEffect draws the FIXED 192x192 src rect of its public static Texture img.
            srcX = VfxDrawGeometry.MAP_CIRCLE_SRC_X;
            srcY = VfxDrawGeometry.MAP_CIRCLE_SRC_Y;
            srcW = VfxDrawGeometry.MAP_CIRCLE_SRC_W;
            srcH = VfxDrawGeometry.MAP_CIRCLE_SRC_H;
        } else if (kind == VfxDrawGeometry.Kind.WEB_PARTICLE
                || kind == VfxDrawGeometry.Kind.ENTANGLE) {
            // WebParticleEffect and EntangleEffect share the static WEB_VFX src rect.
            srcX = VfxDrawGeometry.WEB_SRC_X;
            srcY = VfxDrawGeometry.WEB_SRC_Y;
            srcW = VfxDrawGeometry.WEB_SRC_W;
            srcH = VfxDrawGeometry.WEB_SRC_H;
        } else {
            srcX = VfxDrawGeometry.CALM_SRC_X;
            srcY = VfxDrawGeometry.CALM_SRC_Y;
            srcW = VfxDrawGeometry.CALM_SRC_W;
            srcH = VfxDrawGeometry.CALM_SRC_H;
        }
        // Per-kind blend policy: additive kinds install and restore blend, the ambient kinds
        // (VfxDrawGeometry.additiveBlend(kind) == false — the bare-Texture kinds DebuffParticleEffect
        // and UnknownParticleEffect are both ambient, and WarningSignEffect is additive)
        // never call setBlendFunction natively and restore only color.
        boolean additive = VfxDrawGeometry.additiveBlend(kind);
        Color previous = new Color(sb.getColor());
        boolean blendChanged = false;
        // F25 MULTI-DRAW: kinds whose native render issues several RNG-consuming draws (today
        // StanceChangeAbsorptionParticle) expose an ordered per-pass range list; every other kind
        // returns an empty outer list and keeps the single-draw path (no RNG). The shared RNG state
        // is snapshotted ONCE BEFORE any pass; NRO-04 B09 restores it only for a PRE-pass failure
        // (no pixels yet), while a POST-pass failure keeps the consumed stream and claims the frame.
        java.util.List<java.util.List<float[]>> drawPasses =
                VfxDrawGeometry.drawPassRandomRanges(kind);
        long[] rngSnapshot = drawPasses.isEmpty() ? null : rngSnapshot();
        // NRO-04 B09 multi-pass failure contract: true once a pass's draw has returned normally
        // (i.e. painted pixels). Stays false for the single-draw path, whose catch keeps the
        // original fail-open behavior (a single draw cannot partially succeed).
        boolean drewAny = false;
        try {
            sb.setColor(resolveColor(kind, f.color));
            if (additive) {
                sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                blendChanged = true;
            }
            if (drawPasses.isEmpty()) {
                sb.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                        p.scaleX, p.scaleY, p.rotation, srcX, srcY, srcW, srcH,
                        f.flipX, f.flipY);
            } else {
                for (int passIndex = 0; passIndex < drawPasses.size(); passIndex++) {
                    java.util.List<float[]> ranges = drawPasses.get(passIndex);
                    // Per pass: pull one MathUtils.random(min,max) per inner range IN ORDER and
                    // multiply into scaleX (first) then scaleY (second), matching the native call
                    // sequence exactly so the global stream and the pixels stay identical.
                    float scaleX = p.scaleX;
                    float scaleY = p.scaleY;
                    for (int i = 0; i < ranges.size(); i++) {
                        float[] range = ranges.get(i);
                        float factor =
                                com.badlogic.gdx.math.MathUtils.random(range[0], range[1]);
                        if (i == 0) {
                            scaleX = p.scaleX * factor;
                        } else if (i == 1) {
                            scaleY = p.scaleY * factor;
                        }
                    }
                    sb.draw(texture, p.x, p.y, p.originX, p.originY, p.width, p.height,
                            scaleX, scaleY, p.rotation, srcX, srcY, srcW, srcH,
                            f.flipX, f.flipY);
                    // The draw returned (no throw): this pass painted pixels, so the frame is now
                    // at least partially ART-owned and a later throw must NOT let native repaint
                    // the earlier passes (see the catch below).
                    drewAny = true;
                }
            }
            return true;
        } catch (Throwable ignored) {
            // NRO-04 B09 multi-pass failure contract. POST-pass: an earlier pass already painted
            // pixels, so keep the RNG consumption (do NOT restore) and claim the frame by returning
            // true — native must not draw all passes again (partial draw + full native draw would
            // double-draw). PRE-pass (including the single-draw path): nothing was painted, so
            // restore the snapshot and fail open so native re-consumes exactly the values it needs.
            if (drewAny) {
                return true;
            }
            restoreRng(rngSnapshot);
            return false;
        } finally {
            try {
                if (blendChanged) {
                    sb.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
                sb.setColor(previous);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Reads one field walking the superclass chain, or {@link #MISSING} when absent/unreadable. */
    private static Object readRaw(Object target, String name) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                return MISSING;
            }
        }
        return MISSING;
    }

    /** Required float: any {@link Number}, or {@code null} when absent/unreadable/wrong type. */
    private static Float readFloat(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Number ? ((Number) raw).floatValue() : null;
    }

    /** Optional float: absent/unreadable/wrong type collapses to {@code 0}. */
    private static float optionalFloat(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Number ? ((Number) raw).floatValue() : 0f;
    }

    /**
     * Optional boolean: absent/unreadable/wrong type collapses to {@code false}. Flip is decorative,
     * so a missing flag must not fail the snapshot open.
     */
    private static boolean optionalBoolean(Object target, String name) {
        Object raw = readRaw(target, name);
        return raw instanceof Boolean && (Boolean) raw;
    }
}

package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustPileParticle;
import com.megacrit.cardcrawl.vfx.FireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.GenericSmokeEffect;
import com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect;
import com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect;
import com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.EntangleEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect;
import com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.WebParticleEffect;
import com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * No-GL, no-real-effect coverage for the F2b1 halves of {@link Sts1VfxArtRenderer}: the exact-FQN
 * readiness predicate and the superclass-walking reflective field snapshot. The native effect is
 * stood in for by local holder classes that mirror the field layout ({@code scale}/{@code
 * rotation}/{@code color} inherited from a base type; the rest owned by the subclass) so reflection
 * never touches libGDX GL or an actual STS class.
 *
 * <p>{@code AtlasRegion} is allocated with {@code sun.misc.Unsafe.allocateInstance} (the same
 * no-GL convention as {@code Sts1GdxAtlasRegionsTest}) because it is only ever captured by
 * reference, never queried.
 */
public class Sts1VfxArtRendererTest {

    private static final float EPS = 1e-6f;

    // --- local holder hierarchy mirroring AbstractGameEffect < effect ---

    static class BaseEffect {
        protected float scale;
        protected float rotation;
        protected Color color;
    }

    /** Full layout: required fields plus optional dur_div2/duration. */
    static class FullEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
        private float dur_div2;
        private float duration;
        private TextureAtlas.AtlasRegion img;
    }

    /** Required fields only; dur_div2/duration absent (optional defaults to 0). */
    static class NoDurationEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** Missing the required {@code img}. */
    static class NoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
    }

    /**
     * {@code DivinityStanceChangeParticle}/{@code LightFlareSEffect} layout: has
     * {@code x}/{@code y}/{@code img} but no {@code vY} field at all, so {@code vY} must default to
     * {@code 0}.
     */
    static class NoVYEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * {@code CalmParticleEffect} layout: no {@code img}, plus optional dur_div2/duration. {@code vX}
     * /{@code dvy}/{@code dvx} are irrelevant to the draw and intentionally absent here.
     */
    static class CalmEffect extends BaseEffect {
        private float x;
        private float y;
        private float dur_div2;
        private float duration;
    }

    /** Calm layout with the optional duration terms absent (they default to 0). */
    static class CalmNoDurationEffect extends BaseEffect {
        private float x;
        private float y;
    }

    /** Shield layout: no {@code img}/{@code rotation} field; hardcoded zero rotation. */
    static class ShieldEffect extends BaseEffect {
        private float x;
        private float y;
    }

    /** Shield layout missing the required {@code scale}. */
    static class ShieldNoScaleBase {
        protected float rotation;
        protected Color color;
    }

    static class ShieldNoScaleEffect extends ShieldNoScaleBase {
        private float x;
        private float y;
    }

    /** Debuff layout: own instance {@code Texture img} plus the required rotational fields. */
    static class DebuffEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /** Debuff layout whose {@code img} is an AtlasRegion instead of a Texture. */
    static class DebuffAtlasImgEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** Debuff layout with a null {@code img}. */
    static class DebuffNullImgEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /** IceShatter layout: own instance {@code Texture img} plus the consumed {@code rotation}. */
    static class IceShatterEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /** IceShatter layout whose {@code img} is an AtlasRegion instead of a Texture. */
    static class IceShatterAtlasImgEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** IceShatter layout with a null {@code img}. */
    static class IceShatterNullImgEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /**
     * ICE-shaped base without an inherited {@code rotation} field, so a pair of ICE holders that DO
     * carry a valid instance {@code Texture img} can prove the {@code rotation} field is genuinely
     * required (rather than passing merely because {@code img} was missing).
     */
    static class IceNoRotationBase {
        protected float scale;
        protected Color color;
    }

    /** ICE holder with a valid instance {@code img} but NO {@code rotation} field at all. */
    static class IceShatterNoRotationEffect extends IceNoRotationBase {
        private float x;
        private float y;
        private Texture img;
    }

    /** The same ICE layout with a {@code rotation} field present. */
    static class IceShatterRotationEffect extends IceNoRotationBase {
        private float x;
        private float y;
        private Texture img;
        private float rotation;
    }

    /** FALLING_ICE guard holder: a {@code waitTimer} field plus the shape-C draw fields. */
    static class FallingIceGuardEffect extends BaseEffect {
        private float x;
        private float y;
        private float waitTimer;
        private Texture img;
    }

    /** DAMAGE_HEART guard holder: a {@code delayTimer} field plus the center-packed fields. */
    static class DamageHeartGuardEffect extends BaseEffect {
        private float x;
        private float y;
        private float delayTimer;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * WebParticleEffect layout: no {@code img} field at all (the texture is the static
     * {@code ImageMaster.WEB_VFX}) and no required rotation (hardcoded {@code 0f}).
     */
    static class WebEffect extends BaseEffect {
        private float x;
        private float y;
    }

    /** Base without the inherited {@code scale} field. */
    static class NoScaleBase {
        protected float rotation;
        protected Color color;
    }

    /** Web layout missing the required {@code scale}. */
    static class WebNoScaleEffect extends NoScaleBase {
        private float x;
        private float y;
    }

    /**
     * {@code EntangleEffect} layout: no {@code img} field (the static {@code ImageMaster.WEB_VFX})
     * and no {@code rotation} field (hardcoded {@code 0f}); it also owns no {@code vY}.
     */
    static class EntangleHolder extends BaseEffect {
        private float x;
        private float y;
    }

    /**
     * {@code ExhaustPileParticle} layout: {@code img} is a {@code private static AtlasRegion}
     * declared on the class, plus the instance {@code x}/{@code y}/{@code scale} and the inherited
     * {@code rotation}.
     */
    static class ExhaustPileEffect extends BaseEffect {
        private float x;
        private float y;
        private static TextureAtlas.AtlasRegion img;
    }

    /** {@code ExhaustPileParticle} layout with the static {@code img} null. */
    static class ExhaustPileNullImgEffect extends BaseEffect {
        private float x;
        private float y;
        private static TextureAtlas.AtlasRegion img;
    }

    /** {@code BlockImpactLineEffect} layout: own instance {@code img} plus inherited fields. */
    static class BlockImpactLineEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * {@code UnknownParticleEffect} layout: own instance {@code Texture img} plus the consumed
     * {@code rotation} field.
     */
    static class UnknownParticleEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float scale;
        private Texture img;
    }

    /** {@code UnknownParticleEffect} layout with an AtlasRegion {@code img} instead of a Texture. */
    static class UnknownAtlasImgEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code UnknownParticleEffect} layout with a null instance {@code img}. */
    static class UnknownNullImgEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /**
     * UNKNOWN-shaped base WITHOUT an inherited {@code rotation} field, so a holder that DOES carry a
     * valid instance {@code Texture img} proves the {@code rotation} field is genuinely required.
     */
    static class UnknownNoRotationBase {
        protected float scale;
        protected Color color;
    }

    /** UNKNOWN holder with a valid instance {@code img} but NO {@code rotation} field. */
    static class UnknownNoRotationEffect extends UnknownNoRotationBase {
        private float x;
        private float y;
        private Texture img;
    }

    /** The same UNKNOWN layout with a {@code rotation} field present. */
    static class UnknownRotationEffect extends UnknownNoRotationBase {
        private float x;
        private float y;
        private Texture img;
        private float rotation;
    }

    /**
     * {@code DarkOrbPassiveEffect} layout: own instance {@code Texture img}, the unrelated
     * {@code rotationSpeed} field, and the consumed inherited {@code rotation}.
     */
    static class DarkOrbEffect extends BaseEffect {
        private float x;
        private float y;
        private float rotationSpeed;
        private Texture img;
    }

    /** {@code DarkOrbPassiveEffect} layout with an AtlasRegion {@code img} instead of a Texture. */
    static class DarkOrbAtlasImgEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code DarkOrbPassiveEffect} layout with a null instance {@code img}. */
    static class DarkOrbNullImgEffect extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
    }

    /**
     * DARK_ORB-shaped base WITHOUT an inherited {@code rotation} field, so a holder that DOES carry a
     * valid instance {@code Texture img} proves the {@code rotation} field is genuinely required.
     */
    static class DarkOrbNoRotationBase {
        protected float scale;
        protected Color color;
    }

    /** DARK_ORB holder with a valid instance {@code img} but NO {@code rotation} field. */
    static class DarkOrbNoRotationEffect extends DarkOrbNoRotationBase {
        private float x;
        private float y;
        private Texture img;
    }

    /** The same DARK_ORB layout with a {@code rotation} field present. */
    static class DarkOrbRotationEffect extends DarkOrbNoRotationBase {
        private float x;
        private float y;
        private Texture img;
        private float rotation;
    }

    /**
     * {@code LightningOrbPassiveEffect} layout: own instance {@code Texture img}, the consumed
     * inherited {@code rotation}, and the per-instance {@code flipX}/{@code flipY} booleans.
     */
    static class LightningOrbPassiveHolder extends BaseEffect {
        private float x;
        private float y;
        private Texture img;
        private boolean flipX;
        private boolean flipY;
    }

    /**
     * {@code LightningOrbPassiveEffect} layout with BOTH flip fields absent (the missing-flip case:
     * the snapshot must still resolve and draw with {@code false, false}).
     */
    static class LightningOrbPassiveNoFlipBase {
        protected float scale;
        protected float rotation;
        protected Color color;
    }

    static class LightningOrbPassiveNoFlipHolder extends LightningOrbPassiveNoFlipBase {
        private float x;
        private float y;
        private Texture img;
    }

    /** LOP-shaped holder with a valid instance {@code img} but NO {@code rotation} field. */
    static class LightningOrbPassiveNoRotationBase {
        protected float scale;
        protected Color color;
    }

    static class LightningOrbPassiveNoRotationHolder extends LightningOrbPassiveNoRotationBase {
        private float x;
        private float y;
        private Texture img;
        private boolean flipX;
        private boolean flipY;
    }

    /**
     * {@code GlowyFireEyesEffect} layout: own instance {@code Texture img}, the per-instance
     * {@code flippedX} boolean, and NO {@code rotation} field (the native draw hardcodes {@code 0f}).
     */
    static class GlowyFireEyesBase {
        protected float scale;
        protected Color color;
    }

    static class GlowyFireEyesHolder extends GlowyFireEyesBase {
        private float x;
        private float y;
        private Texture img;
        private boolean flippedX;
    }

    /**
     * {@code GlowyFireEyesEffect} layout with the {@code flippedX} field absent (the missing-flip
     * case: the snapshot must still resolve and draw with {@code false, false}).
     */
    static class GlowyFireEyesNoFlipHolder extends GlowyFireEyesBase {
        private float x;
        private float y;
        private Texture img;
    }

    /**
     * {@code WarningSignEffect} layout: {@code x}/{@code y} declared on the class plus the inherited
     * {@code scale}/{@code rotation}/{@code color} from {@code AbstractGameEffect} — no {@code img}
     * field (the static {@code ImageMaster.WARNING_ICON_VFX} is resolved by the renderer) and no
     * consumed {@code scale}/{@code rotation} (the geometry hardcodes {@code settingsScale * 2f} and
     * a zero rotation).
     */
    static class WarningSignEffectHolder extends BaseEffect {
        private float x;
        private float y;
    }

    /** {@code StunStarEffect} layout: instance {@code AtlasRegion img} plus {@code vX}/{@code vY}. */
    static class StunStarEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code StunStarEffect} layout missing {@code img}. */
    static class StunStarNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
    }

    /**
     * {@code StunStarEffect} layout WITHOUT a {@code vY} field (it still carries {@code vX} and a
     * valid instance {@code AtlasRegion img}), proving {@code vY} is genuinely required for
     * STUN_STAR rather than merely failing because {@code img} was absent.
     */
    static class StunStarNoVYEffect extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * {@code FallingDustEffect} layout: instance {@code AtlasRegion img} plus {@code vX}/{@code vY}
     * (both unused by the draw formula, which uses the region offsets instead).
     */
    static class FallingDustEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code FallingDustEffect} layout missing {@code img}. */
    static class FallingDustNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
    }

    /**
     * {@code TorchParticleMEffect}/{@code TorchParticleSEffect} layout: instance {@code AtlasRegion
     * img} and a {@code vY} that {@code render} ignores (update-only, so it must stay optional).
     */
    static class TorchParticleEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code TorchParticleMEffect}/{@code TorchParticleSEffect} layout missing {@code img}. */
    static class TorchParticleNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
    }

    /**
     * {@code DustEffect} layout: instance {@code AtlasRegion img} plus {@code vX}/{@code vY}/{@code
     * aV}/{@code baseAlpha} (all unused by the draw formula, which uses the region offsets as
     * origin).
     */
    static class SceneDustEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private float aV;
        private float baseAlpha;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code DustEffect} layout missing {@code img}. */
    static class SceneDustNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
    }

    /**
     * {@code LightningEffect}/{@code ShineLinesEffect} layout: instance {@code AtlasRegion img} and
     * NO {@code vY} field at all (the constructors are {@code (float, float)}), proving {@code vY}
     * stays optional for these kinds.
     */
    static class LightningEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code LightningEffect} layout missing {@code img}. */
    static class LightningNoImgEffect extends BaseEffect {
        private float x;
        private float y;
    }

    /** {@code FlameBallParticleEffect} layout: instance {@code AtlasRegion img} and a {@code vY}. */
    static class FlameBallEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code FlameBallParticleEffect} layout missing {@code img}. */
    static class FlameBallNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
    }

    /**
     * {@code FlameBallParticleEffect} layout with an {@code img} but no {@code vY}, proving {@code
     * vY} is optional for FLAME_BALL (its native {@code render} ignores it).
     */
    static class FlameBallNoVYEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * Base without the inherited {@code rotation} field, so the newest img kinds (which consume
     * {@code rotation}) provably fail open on a genuinely missing rotation rather than on a missing
     * img.
     */
    static class NoRotationBase {
        protected float scale;
        protected Color color;
    }

    /** Img holder WITHOUT a {@code rotation} field, for the three newest img kinds. */
    static class NoRotationImgEffect extends NoRotationBase {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /**
     * {@code FlyingSpikeEffect} layout: instance {@code AtlasRegion img} plus {@code vX}/{@code vY}
     * (update-only, never consumed by the draw formula, which is the StanceAura center-packed one).
     */
    static class FlyingSpikeEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code FlyingSpikeEffect} layout missing {@code img}. */
    static class FlyingSpikeNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
    }

    /**
     * {@code ConeEffect} layout: instance {@code AtlasRegion img} plus {@code x}/{@code y} and the
     * unused {@code aV} (its draw consumes only x/y/img/scale/rotation).
     */
    static class ConeEffectHolder extends BaseEffect {
        private float x;
        private float y;
        private float aV;
        private TextureAtlas.AtlasRegion img;
    }

    /** {@code ConeEffect} layout missing {@code img}. */
    static class ConeNoImgEffect extends BaseEffect {
        private float x;
        private float y;
        private float aV;
    }

    @Test
    public void readTextureFieldsResolvesIceShatterFromItsInstanceTexture() {
        IceShatterEffect effect = new IceShatterEffect();
        effect.x = -4.5f;
        effect.y = 2.25f;
        effect.scale = 1.25f;
        effect.rotation = 37f;
        effect.color = Color.WHITE;
        effect.img = noGlTexture(64, 64);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.ICE_SHATTER, effect);

        assertNotNull(f);
        assertEquals(-4.5f, f.x, EPS);
        assertEquals(2.25f, f.y, EPS);
        assertEquals(1.25f, f.scale, EPS);
        assertEquals("IceShatter consumes its rotation field", 37f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForIceShatterWithAtlasRegionOrNullImg() {
        // A wrongly typed img (AtlasRegion rather than a bare Texture) must fail open ...
        IceShatterAtlasImgEffect atlas = new IceShatterAtlasImgEffect();
        atlas.x = 1f;
        atlas.y = 2f;
        atlas.scale = 1f;
        atlas.rotation = 0f;
        atlas.color = Color.WHITE;
        atlas.img = fakeRegion();
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.ICE_SHATTER, atlas));

        // ... and so must a null img.
        IceShatterNullImgEffect none = new IceShatterNullImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        none.img = null;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.ICE_SHATTER, none));
    }

    @Test
    public void readTextureFieldsFailsOpenForIceShatterWithoutARotationField() {
        // IceShatter consumes its rotation field (unlike Shield/Web, which hardcode 0f), so a
        // holder that HAS a valid instance Texture img but no rotation field must fail open rather
        // than silently draw at rotation 0. The same layout WITH a rotation field must resolve, so
        // this test fails if the requireRotation ICE clause is removed.
        Texture img = noGlTexture(64, 64);

        IceShatterNoRotationEffect noRotation = new IceShatterNoRotationEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = img;

        assertNull("an ICE holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.ICE_SHATTER, noRotation));

        IceShatterRotationEffect withRotation = new IceShatterRotationEffect();
        withRotation.x = 1f;
        withRotation.y = 2f;
        withRotation.scale = 1f;
        withRotation.color = Color.WHITE;
        withRotation.img = img;
        withRotation.rotation = 12f;

        Sts1VfxArtRenderer.TextureFields resolved =
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.ICE_SHATTER, withRotation);
        assertNotNull("the same ICE layout with a rotation field must resolve", resolved);
        assertEquals(12f, resolved.rotation, EPS);
        assertSame(img, resolved.img);
    }

    @Test
    public void readTextureFieldsResolvesWebWithoutAnImgFieldAndWithoutARequiredRotation() {
        // WebParticleEffect resolves the static ImageMaster.WEB_VFX texture and hardcodes rotation
        // 0f, so its holder needs neither an img field nor a rotation field.
        WebEffect effect = new WebEffect();
        effect.x = 3.5f;
        effect.y = -1.25f;
        effect.scale = 0.75f;
        effect.color = new Color(0.1f, 0.2f, 0.3f, 0.4f);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.WEB_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(3.5f, f.x, EPS);
        assertEquals(-1.25f, f.y, EPS);
        assertEquals(0.75f, f.scale, EPS);
        assertEquals(0f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertNull("Web resolves the static ImageMaster.WEB_VFX, not an instance img", f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForWebMissingScale() {
        WebNoScaleEffect effect = new WebNoScaleEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.color = Color.WHITE;

        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.WEB_PARTICLE, effect));
    }

    @Test
    public void readTextureFieldsResolvesEntangleWithoutImgOrRotation() {
        // EntangleEffect resolves the static ImageMaster.WEB_VFX texture and hardcodes rotation 0f,
        // so its holder needs neither an img field nor a rotation field.
        EntangleHolder effect = new EntangleHolder();
        effect.x = 12.5f;
        effect.y = -1.25f;
        effect.scale = 0.75f;
        effect.color = new Color(0.1f, 0.2f, 0.3f, 0.4f);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.ENTANGLE, effect);

        assertNotNull(f);
        assertEquals(12.5f, f.x, EPS);
        assertEquals(-1.25f, f.y, EPS);
        assertEquals(0.75f, f.scale, EPS);
        assertEquals(0f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertNull("Entangle resolves the static ImageMaster.WEB_VFX, not an instance img",
                f.img);
    }

    @Test
    public void readFieldsResolvesExhaustPileFromItsStaticImgField() {
        // ExhaustPileParticle.img is a private STATIC AtlasRegion declared on the class; the
        // superclass-walking reader must resolve it via getDeclaredField + field.get(effect).
        TextureAtlas.AtlasRegion region = fakeRegion();
        ExhaustPileEffect.img = region;
        ExhaustPileEffect effect = new ExhaustPileEffect();
        effect.x = 5.5f;
        effect.y = -2.25f;
        effect.scale = 1.1f;
        effect.rotation = 18f;
        effect.color = Color.WHITE;

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.EXHAUST_PILE, effect);

        assertNotNull("a static img field must resolve for the exhaust pile", f);
        assertEquals(5.5f, f.x, EPS);
        assertEquals(-2.25f, f.y, EPS);
        assertEquals(1.1f, f.scale, EPS);
        assertEquals(18f, f.rotation, EPS);
        assertSame(region, f.img);

        // A null static img fails the snapshot (fail open to native).
        ExhaustPileNullImgEffect.img = null;
        ExhaustPileNullImgEffect none = new ExhaustPileNullImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        assertNull(Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.EXHAUST_PILE, none));
    }

    @Test
    public void readFieldsResolvesBlockImpactLineFromItsInstanceImg() {
        BlockImpactLineEffectHolder effect = new BlockImpactLineEffectHolder();
        effect.x = 4.5f;
        effect.y = -1.5f;
        effect.scale = 0.9f;
        effect.rotation = 30f;
        effect.color = Color.WHITE;
        effect.img = fakeRegion();

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.BLOCK_IMPACT_LINE, effect);

        assertNotNull(f);
        assertEquals(4.5f, f.x, EPS);
        assertEquals(-1.5f, f.y, EPS);
        assertEquals(0.9f, f.scale, EPS);
        assertEquals(30f, f.rotation, EPS);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readTextureFieldsResolvesUnknownParticleFromItsInstanceTexture() {
        UnknownParticleEffectHolder effect = new UnknownParticleEffectHolder();
        effect.x = -7.5f;
        effect.y = 21.25f;
        effect.scale = 1.25f;
        effect.rotation = 137f;
        effect.color = Color.WHITE;
        effect.img = noGlTexture(128, 128);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(-7.5f, f.x, EPS);
        assertEquals(21.25f, f.y, EPS);
        assertEquals(1.25f, f.scale, EPS);
        assertEquals("UnknownParticle consumes its rotation field", 137f, f.rotation, EPS);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForUnknownWithAtlasRegionOrNullImg() {
        UnknownAtlasImgEffect atlas = new UnknownAtlasImgEffect();
        atlas.x = 1f;
        atlas.y = 2f;
        atlas.scale = 1f;
        atlas.rotation = 0f;
        atlas.color = Color.WHITE;
        atlas.img = fakeRegion();
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE, atlas));

        UnknownNullImgEffect none = new UnknownNullImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        none.img = null;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.UNKNOWN_PARTICLE, none));
    }

    @Test
    public void readTextureFieldsFailsOpenForUnknownWithoutARotationField() {
        // UnknownParticle consumes its rotation field, so a holder that HAS a valid instance Texture
        // img but no rotation field must fail open rather than silently draw at rotation 0. The same
        // layout WITH a rotation field must resolve.
        Texture img = noGlTexture(128, 128);

        UnknownNoRotationEffect noRotation = new UnknownNoRotationEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = img;

        assertNull("an UNKNOWN holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.UNKNOWN_PARTICLE, noRotation));

        UnknownRotationEffect withRotation = new UnknownRotationEffect();
        withRotation.x = 1f;
        withRotation.y = 2f;
        withRotation.scale = 1f;
        withRotation.color = Color.WHITE;
        withRotation.img = img;
        withRotation.rotation = 12f;

        Sts1VfxArtRenderer.TextureFields resolved =
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.UNKNOWN_PARTICLE, withRotation);
        assertNotNull("the same UNKNOWN layout with a rotation field must resolve", resolved);
        assertEquals(12f, resolved.rotation, EPS);
        assertSame(img, resolved.img);
    }

    @Test
    public void readTextureFieldsResolvesDarkOrbPassiveFromItsInstanceTexture() {
        DarkOrbEffect effect = new DarkOrbEffect();
        effect.x = -7.5f;
        effect.y = 21.25f;
        effect.scale = 1.25f;
        effect.rotation = 137f;
        effect.rotationSpeed = 4.5f;
        effect.color = Color.WHITE;
        effect.img = noGlTexture(74, 74);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE, effect);

        assertNotNull(f);
        assertEquals(-7.5f, f.x, EPS);
        assertEquals(21.25f, f.y, EPS);
        assertEquals(1.25f, f.scale, EPS);
        assertEquals("DarkOrb consumes its rotation field", 137f, f.rotation, EPS);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForDarkOrbWithAtlasRegionOrNullImg() {
        DarkOrbAtlasImgEffect atlas = new DarkOrbAtlasImgEffect();
        atlas.x = 1f;
        atlas.y = 2f;
        atlas.scale = 1f;
        atlas.rotation = 0f;
        atlas.color = Color.WHITE;
        atlas.img = fakeRegion();
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE, atlas));

        DarkOrbNullImgEffect none = new DarkOrbNullImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        none.img = null;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DARK_ORB_PASSIVE, none));
    }

    @Test
    public void readTextureFieldsFailsOpenForDarkOrbWithoutARotationField() {
        // DarkOrbPassive consumes its rotation field, so a holder with a valid instance Texture img
        // but no rotation field must fail open rather than silently draw at rotation 0. The same
        // layout WITH a rotation field must resolve.
        Texture img = noGlTexture(74, 74);

        DarkOrbNoRotationEffect noRotation = new DarkOrbNoRotationEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = img;

        assertNull("a DARK_ORB holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.DARK_ORB_PASSIVE, noRotation));

        DarkOrbRotationEffect withRotation = new DarkOrbRotationEffect();
        withRotation.x = 1f;
        withRotation.y = 2f;
        withRotation.scale = 1f;
        withRotation.color = Color.WHITE;
        withRotation.img = img;
        withRotation.rotation = 12f;

        Sts1VfxArtRenderer.TextureFields resolved =
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.DARK_ORB_PASSIVE, withRotation);
        assertNotNull("the same DARK_ORB layout with a rotation field must resolve", resolved);
        assertEquals(12f, resolved.rotation, EPS);
        assertSame(img, resolved.img);
    }

    @Test
    public void readFieldsResolvesTheFourNewestImgKindsWithVYOptional() {
        // FlameParticleEffect, LightningOrbActivateEffect, DamageImpactBlurEffect, and
        // DamageImpactLineEffect are center-packed img kinds; the reader must resolve them without
        // requiring vY.
        NoVYEffect noVY = new NoVYEffect();
        noVY.x = 7.5f;
        noVY.y = -3.25f;
        noVY.scale = 1.1f;
        noVY.rotation = 18f;
        noVY.color = Color.WHITE;
        noVY.img = fakeRegion();

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE }) {
            Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(kind, noVY);
            assertNotNull("vY is optional for " + kind, f);
            assertEquals(7.5f, f.x, EPS);
            assertEquals(-3.25f, f.y, EPS);
            assertEquals(0f, f.vY, EPS);
            assertEquals(1.1f, f.scale, EPS);
            assertEquals(18f, f.rotation, EPS);
            assertSame(noVY.img, f.img);
        }

        // The same holders with a vY field present resolve and carry it (it is update-only).
        FullEffect withVY = new FullEffect();
        withVY.x = 3.25f;
        withVY.y = -2.5f;
        withVY.vY = 1.75f;
        withVY.scale = 0.9f;
        withVY.rotation = 12f;
        withVY.color = Color.WHITE;
        withVY.img = fakeRegion();

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE }) {
            Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(kind, withVY);
            assertNotNull(f);
            assertEquals("vY is captured but ignored for " + kind, 1.75f, f.vY, EPS);
            assertSame(withVY.img, f.img);
        }
    }

    /** Base without the inherited {@code color} field. */
    static class NoColorBase {
        protected float scale;
        protected float rotation;
    }

    /** Calm layout missing the required {@code color} entirely. */
    static class CalmNoColorEffect extends NoColorBase {
        private float x;
        private float y;
    }

    /** {@code color} exists but is the wrong type. */
    static class WrongColorTypeEffect extends BaseEffect {
        private float x;
        private float y;
        private float vY;
        private TextureAtlas.AtlasRegion img;
        private String color;
    }

    private static TextureAtlas.AtlasRegion fakeRegion() {
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (TextureAtlas.AtlasRegion) ((sun.misc.Unsafe) unsafeField.get(null))
                    .allocateInstance(TextureAtlas.AtlasRegion.class);
        } catch (Exception failure) {
            throw new AssertionError("could not allocate a no-GL AtlasRegion double", failure);
        }
    }

    @Test
    public void readFieldsCapturesOwnAndInheritedFields() {
        FullEffect effect = new FullEffect();
        effect.x = 3.5f;
        effect.y = -1.25f;
        effect.vY = 0.75f;
        effect.dur_div2 = 0.4f;
        effect.duration = 0.9f;
        effect.scale = 1.5f;
        effect.rotation = 42f;
        effect.color = new Color(0.1f, 0.2f, 0.3f, 0.4f);
        effect.img = fakeRegion();

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(3.5f, f.x, EPS);
        assertEquals(-1.25f, f.y, EPS);
        assertEquals(0.75f, f.vY, EPS);
        assertEquals(0.4f, f.durDiv2, EPS);
        assertEquals(0.9f, f.duration, EPS);
        assertEquals(1.5f, f.scale, EPS);
        assertEquals(42f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readFieldsDefaultsAbsentOptionalFieldsToZero() {
        NoDurationEffect effect = new NoDurationEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.vY = 3f;
        effect.scale = 0.5f;
        effect.rotation = 6f;
        effect.color = Color.WHITE;
        effect.img = fakeRegion();

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(1f, f.x, EPS);
        assertEquals(2f, f.y, EPS);
        assertEquals(3f, f.vY, EPS);
        assertEquals(0f, f.durDiv2, EPS);
        assertEquals(0f, f.duration, EPS);
        assertEquals(0.5f, f.scale, EPS);
        assertEquals(6f, f.rotation, EPS);
        assertSame(Color.WHITE, f.color);
        assertNotNull(f.img);
    }

    @Test
    public void readFieldsDefaultsAbsentVYToZeroForKindsThatIgnoreIt() {
        // DivinityStanceChangeParticle and the cross-family LightFlareSEffect have no vY field;
        // STANCE_AURA also ignores vY. For every one of these kinds the img-based reader must still
        // resolve and report vY == 0 rather than failing open.
        NoVYEffect effect = new NoVYEffect();
        effect.x = 7.5f;
        effect.y = -3.25f;
        effect.scale = 1.1f;
        effect.rotation = 18f;
        effect.color = new Color(0.5f, 0.6f, 0.7f, 0.8f);
        effect.img = fakeRegion();

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.DIVINITY_STANCE_CHANGE,
                VfxDrawGeometry.Kind.LIGHT_FLARE,
                VfxDrawGeometry.Kind.LIGHT_FLARE_M,
                VfxDrawGeometry.Kind.LIGHT_FLARE_L,
                VfxDrawGeometry.Kind.FLASH_ATK_IMG,
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR,
                VfxDrawGeometry.Kind.FLAME_PARTICLE,
                VfxDrawGeometry.Kind.LIGHTNING_ORB_ACTIVATE,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_BLUR,
                VfxDrawGeometry.Kind.DAMAGE_IMPACT_LINE,
                VfxDrawGeometry.Kind.STANCE_AURA }) {
            Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(kind, effect);

            assertNotNull("vY must be optional for " + kind, f);
            assertEquals(7.5f, f.x, EPS);
            assertEquals(-3.25f, f.y, EPS);
            assertEquals(0f, f.vY, EPS);
            assertEquals(1.1f, f.scale, EPS);
            assertEquals(18f, f.rotation, EPS);
            assertEquals(0f, f.durDiv2, EPS);
            assertEquals(0f, f.duration, EPS);
            assertSame(effect.color, f.color);
            assertSame(effect.img, f.img);
        }
    }

    @Test
    public void readFieldsRequiresVYForKindsThatConsumeIt() {
        // WRATH_PARTICLE and DIVINITY_PARTICLE add vY to y, so an absent/unreadable vY must fail
        // open to the native draw rather than silently rendering at an un-shifted y.
        NoVYEffect effect = new NoVYEffect();
        effect.x = 7.5f;
        effect.y = -3.25f;
        effect.scale = 1.1f;
        effect.rotation = 18f;
        effect.color = Color.WHITE;
        effect.img = fakeRegion();

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.WRATH_PARTICLE,
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE }) {
            assertNull("absent vY must fail open for " + kind,
                    Sts1VfxArtRenderer.readFields(kind, effect));
        }

        // With vY present the same holder resolves for those kinds.
        FullEffect withVY = new FullEffect();
        withVY.x = 7.5f;
        withVY.y = -3.25f;
        withVY.vY = 42f;
        withVY.scale = 1.1f;
        withVY.rotation = 18f;
        withVY.color = Color.WHITE;
        withVY.img = fakeRegion();

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.WRATH_PARTICLE, withVY);
        assertNotNull(f);
        assertEquals(42f, f.vY, EPS);
    }

    @Test
    public void readFieldsResolvesVYOwnersWithVYPresentAndIgnoresIt() {
        // TorchParticleLEffect plus the nine newest members have a vY field but render never reads
        // it; the optional-vY path must still resolve for them (vY captured but not consumed by the
        // geometry).
        FullEffect effect = new FullEffect();
        effect.x = 3.25f;
        effect.y = -2.5f;
        effect.vY = 1.75f;
        effect.scale = 0.9f;
        effect.rotation = 12f;
        effect.color = Color.WHITE;
        effect.img = fakeRegion();

        for (VfxDrawGeometry.Kind kind : new VfxDrawGeometry.Kind[] {
                VfxDrawGeometry.Kind.TORCH_PARTICLE_L,
                VfxDrawGeometry.Kind.FIRE_BURST,
                VfxDrawGeometry.Kind.RED_FIRE_BURST,
                VfxDrawGeometry.Kind.SMOKE_BLUR,
                VfxDrawGeometry.Kind.CEILING_DUST,
                VfxDrawGeometry.Kind.NEMESIS_FIRE,
                VfxDrawGeometry.Kind.TORCH_PARTICLE_XL,
                VfxDrawGeometry.Kind.GHOSTLY_WEAK_FIRE,
                VfxDrawGeometry.Kind.GENERIC_SMOKE,
                VfxDrawGeometry.Kind.EXHAUST_BLUR }) {
            Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(kind, effect);

            assertNotNull("vY must be optional for " + kind, f);
            assertEquals("vY is captured but ignored for " + kind, 1.75f, f.vY, EPS);
            assertEquals(3.25f, f.x, EPS);
            assertEquals(-2.5f, f.y, EPS);
            assertSame(effect.img, f.img);
        }
    }

    @Test
    public void readFieldsReturnsNullWhenARequiredFieldIsMissing() {
        NoImgEffect effect = new NoImgEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.vY = 3f;
        effect.scale = 1f;
        effect.rotation = 0f;
        effect.color = Color.WHITE;

        assertNull(Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE, effect));
    }

    @Test
    public void readFieldsReturnsNullWhenAFieldHasTheWrongType() {
        WrongColorTypeEffect effect = new WrongColorTypeEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.vY = 3f;
        effect.scale = 1f;
        effect.rotation = 0f;
        effect.color = "not a color";
        effect.img = fakeRegion();

        assertNull(Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE, effect));
    }

    @Test
    public void readFieldsReturnsNullForNullInput() {
        assertNull(Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DIVINITY_PARTICLE, null));
    }

    @Test
    public void readCalmFieldsReadsRequiredFieldsWithoutAnImg() {
        CalmEffect effect = new CalmEffect();
        effect.x = 4.5f;
        effect.y = -2.75f;
        effect.dur_div2 = 0.25f;
        effect.duration = 1.5f;
        effect.scale = 0.8f;
        effect.rotation = 15f;
        effect.color = new Color(0.2f, 0.3f, 0.4f, 0.5f);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.CALM_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(4.5f, f.x, EPS);
        assertEquals(-2.75f, f.y, EPS);
        assertEquals(0.8f, f.scale, EPS);
        assertEquals(15f, f.rotation, EPS);
        assertEquals(0.25f, f.durDiv2, EPS);
        assertEquals(1.5f, f.duration, EPS);
        assertSame(effect.color, f.color);
        assertNull("Calm resolves no instance texture", f.img);
    }

    @Test
    public void readCalmFieldsDefaultsAbsentDurationTermsToZero() {
        CalmNoDurationEffect effect = new CalmNoDurationEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.scale = 0.5f;
        effect.rotation = 6f;
        effect.color = Color.WHITE;

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.CALM_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(1f, f.x, EPS);
        assertEquals(2f, f.y, EPS);
        assertEquals(0.5f, f.scale, EPS);
        assertEquals(6f, f.rotation, EPS);
        assertEquals(0f, f.durDiv2, EPS);
        assertEquals(0f, f.duration, EPS);
        assertSame(Color.WHITE, f.color);
        assertNull(f.img);
    }

    @Test
    public void readCalmFieldsFailsOpenForNullMissingColorOrMissingXY() {
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.CALM_PARTICLE, null));

        CalmNoColorEffect noColor = new CalmNoColorEffect();
        noColor.x = 1f;
        noColor.y = 2f;
        noColor.scale = 1f;
        noColor.rotation = 0f;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.CALM_PARTICLE, noColor));

        // CalmNoDurationEffect has no dur_div2/duration, but does have x/y/scale/rotation/color.
        CalmNoDurationEffect ok = new CalmNoDurationEffect();
        ok.color = Color.WHITE;
        assertNotNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.CALM_PARTICLE, ok));
    }

    @Test
    public void readFieldsIsNotUsedForCalmBecauseCalmHasNoImg() {
        // The img-based reader still requires img; Calm uses readTextureFields instead.
        CalmEffect effect = new CalmEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.scale = 1f;
        effect.rotation = 0f;
        effect.color = Color.WHITE;

        assertNull(Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.STANCE_AURA, effect));
    }

    @Test
    public void readTextureFieldsResolvesShieldWithoutARequiredRotation() {
        // ShieldParticleEffect has no rotation field and hardcodes 0f: rotation must be optional
        // (defaults to 0) and its img must stay null.
        ShieldEffect effect = new ShieldEffect();
        effect.x = 3.5f;
        effect.y = -1.25f;
        effect.scale = 0.75f;
        effect.color = new Color(0.1f, 0.2f, 0.3f, 0.4f);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.SHIELD_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(3.5f, f.x, EPS);
        assertEquals(-1.25f, f.y, EPS);
        assertEquals(0.75f, f.scale, EPS);
        assertEquals(0f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertNull(f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForShieldMissingScale() {
        ShieldNoScaleEffect effect = new ShieldNoScaleEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.color = Color.WHITE;

        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.SHIELD_PARTICLE, effect));
    }

    @Test
    public void readTextureFieldsResolvesDebuffFromItsInstanceTexture() {
        DebuffEffect effect = new DebuffEffect();
        effect.x = -4.5f;
        effect.y = 2.25f;
        effect.scale = 1.25f;
        effect.rotation = 37f;
        effect.color = Color.WHITE;
        effect.img = noGlTexture(64, 64);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE, effect);

        assertNotNull(f);
        assertEquals(-4.5f, f.x, EPS);
        assertEquals(2.25f, f.y, EPS);
        assertEquals(1.25f, f.scale, EPS);
        assertEquals(37f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertSame(effect.img, f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForDebuffWithAtlasRegionOrNullImg() {
        // A wrongly typed img (AtlasRegion rather than a bare Texture) must fail open ...
        DebuffAtlasImgEffect atlas = new DebuffAtlasImgEffect();
        atlas.x = 1f;
        atlas.y = 2f;
        atlas.scale = 1f;
        atlas.rotation = 0f;
        atlas.color = Color.WHITE;
        atlas.img = fakeRegion();
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE, atlas));

        // ... and so must a null img.
        DebuffNullImgEffect none = new DebuffNullImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        none.img = null;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE, none));
    }

    @Test
    public void readTextureFieldsResolvesWarningSignWithoutScaleRotationOrImg() {
        // WarningSignEffect has x/y ONLY (no scale, no rotation, no img): the static
        // ImageMaster.WARNING_ICON_VFX is resolved by the renderer and the uniform scale is the
        // hardcoded Settings.scale * 2f, so the snapshot needs neither scale/rotation nor an img.
        WarningSignEffectHolder effect = new WarningSignEffectHolder();
        effect.x = -4.5f;
        effect.y = 2.25f;
        effect.color = new Color(0.1f, 0.2f, 0.3f, 0.4f);

        Sts1VfxArtRenderer.TextureFields f = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.WARNING_SIGN, effect);

        assertNotNull("WarningSign needs no scale/rotation/img field", f);
        assertEquals(-4.5f, f.x, EPS);
        assertEquals(2.25f, f.y, EPS);
        assertEquals(0f, f.scale, EPS);
        assertEquals(0f, f.rotation, EPS);
        assertSame(effect.color, f.color);
        assertNull("WarningSign resolves the static ImageMaster.WARNING_ICON_VFX", f.img);
    }

    @Test
    public void readTextureFieldsFailsOpenForWarningSignMissingXYOrColor() {
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.WARNING_SIGN, null));

        // A holder with the required x/y/color but no scale/rotation is the native layout and
        // resolves; a holder missing x/y/color must still fail open.
        WarningSignEffectHolder noColor = new WarningSignEffectHolder();
        noColor.x = 1f;
        noColor.y = 2f;
        assertNull(Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.WARNING_SIGN, noColor));
    }

    @Test
    public void readFieldsResolvesStunStarWithItsVXVYAndInstanceRegion() {
        StunStarEffectHolder effect = new StunStarEffectHolder();
        effect.x = 4.5f;
        effect.y = -1.5f;
        effect.vX = 2.5f;
        effect.vY = -0.75f;
        effect.scale = 0.9f;
        effect.rotation = 30f;
        effect.color = Color.WHITE;
        effect.img = fakeRegion();

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.STUN_STAR, effect);

        assertNotNull(f);
        assertEquals(4.5f, f.x, EPS);
        assertEquals(-1.5f, f.y, EPS);
        assertEquals("StunStar consumes its vX", 2.5f, f.vX, EPS);
        assertEquals("StunStar consumes its vY", -0.75f, f.vY, EPS);
        assertEquals(0.9f, f.scale, EPS);
        assertEquals(30f, f.rotation, EPS);
        assertSame(effect.img, f.img);

        StunStarNoImgEffect none = new StunStarNoImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.vX = 3f;
        none.vY = 4f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        assertNull("a StunStar holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.STUN_STAR, none));
    }

    @Test
    public void readFieldsRequiresVYForStunStarBecauseItsOffsetConsumesIt() {
        // StunStarEffect's geometry consumes vY in its position offset
        // (y - vY*5f*Settings.scale), so a holder WITHOUT a vY field must fail open rather than
        // silently draw at an un-shifted y. The same holder WITH a vY field must resolve.
        StunStarNoVYEffect noVY = new StunStarNoVYEffect();
        noVY.x = 4.5f;
        noVY.y = -1.5f;
        noVY.vX = 2.5f;
        noVY.scale = 0.9f;
        noVY.rotation = 30f;
        noVY.color = Color.WHITE;
        noVY.img = fakeRegion();

        assertNull("a STUN_STAR holder with img but no vY field must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.STUN_STAR, noVY));
        assertFalse("canDraw must also fail closed for the vY-less StunStar holder",
                new Sts1VfxArtRenderer().canDraw(noVY));

        StunStarEffectHolder withVY = new StunStarEffectHolder();
        withVY.x = 4.5f;
        withVY.y = -1.5f;
        withVY.vX = 2.5f;
        withVY.vY = -0.75f;
        withVY.scale = 0.9f;
        withVY.rotation = 30f;
        withVY.color = Color.WHITE;
        withVY.img = fakeRegion();

        Sts1VfxArtRenderer.Fields resolved = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.STUN_STAR, withVY);
        assertNotNull("the same STUN_STAR layout with a vY field must resolve", resolved);
        assertEquals(-0.75f, resolved.vY, EPS);
        assertEquals(2.5f, resolved.vX, EPS);

        // The vY-consuming set is exactly WRATH_PARTICLE, DIVINITY_PARTICLE, and STUN_STAR: the
        // no-vY STUN_STAR holder must still resolve for a kind that ignores vY (FALLING_DUST).
        assertNotNull("vY stays optional for a kind that ignores it",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FALLING_DUST, noVY));
    }

    @Test
    public void readFieldsResolvesFallingDustAndCapturesTheRegionOffsets() {
        FallingDustEffectHolder effect = new FallingDustEffectHolder();
        effect.x = 5.5f;
        effect.y = -2.25f;
        effect.vX = 1.25f;
        effect.vY = 3.5f;
        effect.scale = 1.1f;
        effect.rotation = 18f;
        effect.color = Color.WHITE;
        TextureAtlas.AtlasRegion region = fakeRegion();
        region.offsetX = 6f;
        region.offsetY = 10f;
        effect.img = region;

        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.FALLING_DUST, effect);

        assertNotNull(f);
        assertEquals(5.5f, f.x, EPS);
        assertEquals(-2.25f, f.y, EPS);
        assertEquals("the region offsetX is captured for the FALLING_DUST origin",
                6f, f.regionOffsetX, EPS);
        assertEquals("the region offsetY is captured for the FALLING_DUST origin",
                10f, f.regionOffsetY, EPS);
        assertEquals(1.1f, f.scale, EPS);
        assertEquals(18f, f.rotation, EPS);

        // A flipped shared region reports the flipped offsetX/offsetY; the reader normalizes them
        // back to the unflipped trim origin (the exact inverse of AtlasRegion.flip's transform).
        TextureAtlas.AtlasRegion flipped = new TextureAtlas.AtlasRegion(noGlTexture(32, 32), 0, 0, 12, 8);
        flipped.originalWidth = 32;
        flipped.originalHeight = 32;
        flipped.offsetX = 6f;
        flipped.offsetY = 10f;
        flipped.flip(true, true);
        assertTrue("the fixture really is natively flipped", flipped.isFlipX());
        assertTrue(flipped.isFlipY());
        FallingDustEffectHolder flippedEffect = new FallingDustEffectHolder();
        flippedEffect.x = 1f;
        flippedEffect.y = 2f;
        flippedEffect.scale = 1f;
        flippedEffect.rotation = 0f;
        flippedEffect.color = Color.WHITE;
        flippedEffect.img = flipped;

        Sts1VfxArtRenderer.Fields ff = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.FALLING_DUST, flippedEffect);
        assertNotNull(ff);
        assertEquals("a flipped region must report the unflipped offsetX",
                6f, ff.regionOffsetX, EPS);
        assertEquals("a flipped region must report the unflipped offsetY",
                10f, ff.regionOffsetY, EPS);

        FallingDustNoImgEffect none = new FallingDustNoImgEffect();
        none.x = 1f;
        none.y = 2f;
        none.scale = 1f;
        none.rotation = 0f;
        none.color = Color.WHITE;
        assertNull("a FallingDust holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FALLING_DUST, none));
    }

    @Test
    public void readFieldsResolvesTheNewestImgKindsWithOptionalVY() {
        // LightningEffect/FlameBallParticleEffect/ShineLinesEffect all resolve from an instance
        // AtlasRegion; LightningEffect and ShineLinesEffect have NO vY field (vY is optional for
        // them), and FlameBallParticleEffect's vY is update-only (optional too).
        LightningEffectHolder lightning = new LightningEffectHolder();
        lightning.x = 4.5f;
        lightning.y = -1.5f;
        lightning.scale = 0.9f;
        lightning.rotation = 30f;
        lightning.color = Color.WHITE;
        lightning.img = fakeRegion();

        Sts1VfxArtRenderer.Fields lf = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.LIGHTNING_EFFECT, lightning);
        assertNotNull("LIGHTNING_EFFECT resolves without a vY field", lf);
        assertEquals(4.5f, lf.x, EPS);
        assertEquals(-1.5f, lf.y, EPS);
        assertEquals("the absent vY defaults to 0", 0f, lf.vY, EPS);
        assertEquals(0.9f, lf.scale, EPS);
        assertEquals(30f, lf.rotation, EPS);
        assertSame(lightning.img, lf.img);

        FlameBallNoVYEffect flameNoVY = new FlameBallNoVYEffect();
        flameNoVY.x = 1f;
        flameNoVY.y = 2f;
        flameNoVY.scale = 1f;
        flameNoVY.rotation = 10f;
        flameNoVY.color = Color.WHITE;
        flameNoVY.img = fakeRegion();
        assertNotNull("FLAME_BALL resolves without a vY field (optional)",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FLAME_BALL, flameNoVY));

        FlameBallEffectHolder flame = new FlameBallEffectHolder();
        flame.x = 1f;
        flame.y = 2f;
        flame.vY = 3.5f;
        flame.scale = 1f;
        flame.rotation = 10f;
        flame.color = Color.WHITE;
        flame.img = fakeRegion();
        Sts1VfxArtRenderer.Fields fb = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.FLAME_BALL, flame);
        assertNotNull(fb);
        assertEquals("FlameBall's vY is captured but not consumed", 3.5f, fb.vY, EPS);

        LightningEffectHolder shine = new LightningEffectHolder();
        shine.x = 3f;
        shine.y = 4f;
        shine.scale = 0.5f;
        shine.rotation = 5f;
        shine.color = Color.WHITE;
        shine.img = fakeRegion();
        Sts1VfxArtRenderer.Fields sl = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.SHINE_LINES, shine);
        assertNotNull("SHINE_LINES resolves without a vY field", sl);
        assertSame(shine.img, sl.img);

        // Missing img fails open for all three.
        LightningNoImgEffect noImg = new LightningNoImgEffect();
        noImg.x = 1f;
        noImg.y = 2f;
        noImg.scale = 1f;
        noImg.rotation = 0f;
        noImg.color = Color.WHITE;
        assertNull("a LightningEffect holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.LIGHTNING_EFFECT, noImg));
        FlameBallNoImgEffect flameNoImg = new FlameBallNoImgEffect();
        flameNoImg.x = 1f;
        flameNoImg.y = 2f;
        flameNoImg.vY = 3f;
        flameNoImg.scale = 1f;
        flameNoImg.rotation = 0f;
        flameNoImg.color = Color.WHITE;
        assertNull("a FlameBall holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FLAME_BALL, flameNoImg));
        assertNull("a ShineLines holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SHINE_LINES, noImg));
    }

    @Test
    public void readFieldsFailsOpenForTheNewestImgKindsWhenRotationIsMissing() {
        // All three newest img kinds consume the rotation field, so a holder with a valid img but no
        // rotation field must fail open rather than silently drawing at rotation 0.
        NoRotationImgEffect holder = new NoRotationImgEffect();
        holder.x = 1f;
        holder.y = 2f;
        holder.scale = 1f;
        holder.color = Color.WHITE;
        holder.img = fakeRegion();

        assertNull("LIGHTNING_EFFECT requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.LIGHTNING_EFFECT, holder));
        assertNull("FLAME_BALL requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FLAME_BALL, holder));
        assertNull("SHINE_LINES requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SHINE_LINES, holder));
    }

    @Test
    public void readFieldsResolvesTheTwoNewestImgKindsWithOptionalVXAndVY() {
        // FlyingSpikeEffect and ConeEffect both resolve from an instance AtlasRegion with rotation.
        // FlyingSpike's vX/vY are update-only (never consumed by render), so they stay optional;
        // ConeEffect has no vX/vY at all.
        FlyingSpikeEffectHolder spike = new FlyingSpikeEffectHolder();
        spike.x = 4.5f;
        spike.y = -1.5f;
        spike.vX = 12f;
        spike.vY = 34f;
        spike.scale = 0.9f;
        spike.rotation = 30f;
        spike.color = Color.WHITE;
        spike.img = fakeRegion();

        Sts1VfxArtRenderer.Fields fs = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.FLYING_SPIKE, spike);
        assertNotNull("FLYING_SPIKE resolves from its instance AtlasRegion", fs);
        assertEquals(4.5f, fs.x, EPS);
        assertEquals(-1.5f, fs.y, EPS);
        assertEquals("FlyingSpike's vX is captured but not consumed", 12f, fs.vX, EPS);
        assertEquals("FlyingSpike's vY is captured but not consumed", 34f, fs.vY, EPS);
        assertEquals(0.9f, fs.scale, EPS);
        assertEquals(30f, fs.rotation, EPS);
        assertSame(spike.img, fs.img);

        ConeEffectHolder cone = new ConeEffectHolder();
        cone.x = 1f;
        cone.y = 2f;
        cone.aV = 3f;
        cone.scale = 1f;
        cone.rotation = 15f;
        cone.color = Color.WHITE;
        cone.img = fakeRegion();

        Sts1VfxArtRenderer.Fields cf = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.CONE, cone);
        assertNotNull("CONE resolves from its instance AtlasRegion", cf);
        assertEquals(1f, cf.x, EPS);
        assertEquals(2f, cf.y, EPS);
        assertEquals("the absent vY defaults to 0", 0f, cf.vY, EPS);
        assertEquals(15f, cf.rotation, EPS);
        assertSame(cone.img, cf.img);

        // Missing img fails open for both, and so does a missing rotation field.
        FlyingSpikeNoImgEffect spikeNoImg = new FlyingSpikeNoImgEffect();
        spikeNoImg.x = 1f;
        spikeNoImg.y = 2f;
        spikeNoImg.scale = 1f;
        spikeNoImg.rotation = 0f;
        spikeNoImg.color = Color.WHITE;
        assertNull("a FlyingSpike holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FLYING_SPIKE, spikeNoImg));

        ConeNoImgEffect coneNoImg = new ConeNoImgEffect();
        coneNoImg.x = 1f;
        coneNoImg.y = 2f;
        coneNoImg.scale = 1f;
        coneNoImg.rotation = 0f;
        coneNoImg.color = Color.WHITE;
        assertNull("a Cone holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.CONE, coneNoImg));

        NoRotationImgEffect noRotation = new NoRotationImgEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = fakeRegion();
        assertNull("FLYING_SPIKE requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.FLYING_SPIKE, noRotation));
        assertNull("CONE requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.CONE, noRotation));
    }

    @Test
    public void readFieldsResolvesTheNewestSceneWorldImgKindsWithOptionalVY() {
        // TorchParticleMEffect/TorchParticleSEffect and DustEffect all resolve from an instance
        // AtlasRegion. The two torch kinds own a vY that their native render ignores (update-only),
        // so vY stays optional for them; DustEffect reuses the FALLING_DUST region-offset origin, so
        // the reader must capture the region's own offsetX/offsetY.
        TorchParticleEffectHolder torchM = new TorchParticleEffectHolder();
        torchM.x = 4.5f;
        torchM.y = -1.5f;
        torchM.scale = 0.9f;
        torchM.rotation = 30f;
        torchM.color = Color.WHITE;
        torchM.img = fakeRegion();

        Sts1VfxArtRenderer.Fields tm = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.TORCH_PARTICLE_M, torchM);
        assertNotNull("TORCH_PARTICLE_M resolves from its instance AtlasRegion", tm);
        assertEquals(4.5f, tm.x, EPS);
        assertEquals(-1.5f, tm.y, EPS);
        assertEquals(0.9f, tm.scale, EPS);
        assertEquals(30f, tm.rotation, EPS);
        assertSame(torchM.img, tm.img);

        TorchParticleEffectHolder torchS = new TorchParticleEffectHolder();
        torchS.x = 1f;
        torchS.y = 2f;
        torchS.scale = 1f;
        torchS.rotation = 15f;
        torchS.color = Color.WHITE;
        torchS.img = fakeRegion();
        assertNotNull("TORCH_PARTICLE_S resolves from its instance AtlasRegion",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_S, torchS));

        // vY is optional: a holder without a vY field still resolves for both torch kinds.
        LightningEffectHolder noVY = new LightningEffectHolder();
        noVY.x = 1f;
        noVY.y = 2f;
        noVY.scale = 1f;
        noVY.rotation = 0f;
        noVY.color = Color.WHITE;
        noVY.img = fakeRegion();
        assertNotNull("TORCH_PARTICLE_M resolves without a vY field",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_M, noVY));
        assertNotNull("TORCH_PARTICLE_S resolves without a vY field",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_S, noVY));

        SceneDustEffectHolder dust = new SceneDustEffectHolder();
        dust.x = 5.5f;
        dust.y = -2.25f;
        dust.vX = 1.25f;
        dust.vY = 3.5f;
        dust.scale = 1.1f;
        dust.rotation = 18f;
        dust.color = Color.WHITE;
        TextureAtlas.AtlasRegion region = fakeRegion();
        region.offsetX = 6f;
        region.offsetY = 10f;
        dust.img = region;

        Sts1VfxArtRenderer.Fields df = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.SCENE_DUST, dust);
        assertNotNull("SCENE_DUST resolves from its instance AtlasRegion", df);
        assertEquals(5.5f, df.x, EPS);
        assertEquals(-2.25f, df.y, EPS);
        assertEquals("the region offsetX is captured for the SCENE_DUST origin",
                6f, df.regionOffsetX, EPS);
        assertEquals("the region offsetY is captured for the SCENE_DUST origin",
                10f, df.regionOffsetY, EPS);
        assertEquals(18f, df.rotation, EPS);
    }

    @Test
    public void readFieldsFailsOpenForTheNewestSceneWorldImgKindsOnMissingFields() {
        // Missing img fails open for all three.
        TorchParticleNoImgEffect torchNoImg = new TorchParticleNoImgEffect();
        torchNoImg.x = 1f;
        torchNoImg.y = 2f;
        torchNoImg.vY = 3f;
        torchNoImg.scale = 1f;
        torchNoImg.rotation = 0f;
        torchNoImg.color = Color.WHITE;
        assertNull("a TorchParticleM holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_M, torchNoImg));
        assertNull("a TorchParticleS holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_S, torchNoImg));

        SceneDustNoImgEffect dustNoImg = new SceneDustNoImgEffect();
        dustNoImg.x = 1f;
        dustNoImg.y = 2f;
        dustNoImg.scale = 1f;
        dustNoImg.rotation = 0f;
        dustNoImg.color = Color.WHITE;
        assertNull("a DustEffect holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SCENE_DUST, dustNoImg));

        // All three consume the rotation field, so a holder with a valid img but no rotation must
        // fail open rather than silently drawing at rotation 0.
        NoRotationImgEffect noRotation = new NoRotationImgEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = fakeRegion();
        assertNull("TORCH_PARTICLE_M requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_M, noRotation));
        assertNull("TORCH_PARTICLE_S requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.TORCH_PARTICLE_S, noRotation));
        assertNull("SCENE_DUST requires rotation",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SCENE_DUST, noRotation));
    }

    @Test
    public void isReadyIsTrueForTheSupportedFqnsAndFalseOtherwise() {
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        assertTrue(renderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(renderer.isReady(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(renderer.isReady(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
        assertTrue(renderer.isReady(VfxClaimPolicy.CALM_PARTICLE_EFFECT));
        assertTrue(renderer.isReady(
                VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.stance.DivinityStanceChangeParticle"));
        assertTrue(renderer.isReady("com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FLASH_ATK_IMG));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE_M));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareMEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE_L));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareLEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_L));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleLEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FIRE_BURST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.FireBurstParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.RED_FIRE_BURST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.RedFireBurstParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SMOKE_BLUR));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.CEILING_DUST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.CeilingDustCloudEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.NEMESIS_FIRE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.NemesisFireParticle"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.ShieldParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.DebuffParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleXLEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.GHOSTLY_WEAK_FIRE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.GENERIC_SMOKE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.GenericSmokeEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.EXHAUST_BLUR));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.ExhaustBlurEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.ICE_SHATTER));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.IceShatterEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.WEB_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.WebParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.EntangleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.ExhaustPileParticle"));
        assertTrue(renderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.UnknownParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FLAME_PARTICLE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactBlurEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.DamageImpactLineEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.DarkOrbPassiveEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.WARNING_SIGN));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.WarningSignEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.STUN_STAR));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.StunStarEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FALLING_DUST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.FallingDustEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.LightningEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FLAME_BALL));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.FlameBallParticleEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SHINE_LINES));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.ShineLinesEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FLYING_SPIKE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.CONE_EFFECT));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.ConeEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.FALLING_ICE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FallingIceEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DAMAGE_HEART));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.DamageHeartEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SPOOKY_CHEST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle"));

        assertFalse(renderer.isReady(null));
        assertFalse(renderer.isReady(""));
        assertFalse(renderer.isReady("   "));
        assertFalse(renderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.CALM_PARTICLE_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(
                VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLASH_ATK_IMG + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SHIELD_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DEBUFF_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_TORCH_PARTICLE_XL + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.GHOSTLY_WEAK_FIRE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.GENERIC_SMOKE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.EXHAUST_BLUR + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.ICE_SHATTER + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.ICE_SHATTER + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WEB_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WEB_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.ENTANGLE_EFFECT + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.BLOCK_IMPACT_LINE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.EXHAUST_PILE_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.UNKNOWN_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLAME_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLAME_PARTICLE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.LIGHTNING_ORB_ACTIVATE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_BLUR + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_IMPACT_LINE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DARK_ORB_PASSIVE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WARNING_SIGN + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WARNING_SIGN + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.STUN_STAR + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.STUN_STAR + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FALLING_DUST + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FALLING_DUST + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.LIGHTNING_EFFECT + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLAME_BALL + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLAME_BALL + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SHINE_LINES + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SHINE_LINES + "2"));
        assertTrue(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect"));
        assertTrue(renderer.isReady(VfxClaimPolicy.SCENE_DUST));
        assertTrue(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.DustEffect"));
        assertFalse(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_M + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.TORCH_PARTICLE_S + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_DUST + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_DUST + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FALLING_ICE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FALLING_ICE + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_HEART + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DAMAGE_HEART + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SPOOKY_CHEST + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SPOOKY_CHEST + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.IRONCLAD_VICTORY_FLAME + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SPOOKIER_CHEST + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.CAMPFIRE_SLEEP_COVER + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.DEATH_SCREEN_FLOATY + "2"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.WRATH_STANCE_CHANGE + "2"));
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.WrathStanceChangeParticle"));
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect2"));
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect2"));
    }

    @Test
    public void warningSignDrawUsesTheStaticIconAndTheDoubleSettingsScale() {
        // WarningSignEffect is additive and draws the static ImageMaster.WARNING_ICON_VFX Texture
        // over a fixed 64x64 rect with a hardcoded Settings.scale * 2f uniform scale; it has no
        // scale/rotation/img field at all. The real effect is reflectively seeded (no GL) and the
        // static texture is injected via reflection.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Texture previous = ImageMaster.WARNING_ICON_VFX;
        Texture injected = noGlTexture(64, 64);
        float settingsScale = com.megacrit.cardcrawl.core.Settings.scale;
        try {
            setStaticField(ImageMaster.class, "WARNING_ICON_VFX", injected);
            assertSame(injected, ImageMaster.WARNING_ICON_VFX);

            CountingBatch batch = newCountingBatch();
            AbstractGameEffect effect = seededWarningSign(
                    new Color(0.7f, 0.8f, 0.9f, 0.6f), 12.5f, -3.25f);

            assertTrue(renderer.render(batch, effect));

            assertEquals("the additive WARNING draw installs and restores blend",
                    2, batch.setBlendCalls);
            assertEquals("the warning sign uses the raw-texture draw overload",
                    1, batch.drawCalls);
            assertSame("WarningSign draws the static WARNING_ICON_VFX texture",
                    injected, batch.drawnTexture);

            // draw(tex, x-32, y-32, 32, 32, 64, 64, ss*2, ss*2, 0, 0, 0, 64, 64, false, false)
            assertEquals(12.5f - 32f, floatAt(batch, 0), EPS);
            assertEquals(-3.25f - 32f, floatAt(batch, 1), EPS);
            assertEquals(32f, floatAt(batch, 2), EPS);
            assertEquals(32f, floatAt(batch, 3), EPS);
            assertEquals(64f, floatAt(batch, 4), EPS);
            assertEquals(64f, floatAt(batch, 5), EPS);
            assertEquals("the uniform scale is Settings.scale * 2f",
                    settingsScale * 2f, floatAt(batch, 6), EPS);
            assertEquals(settingsScale * 2f, floatAt(batch, 7), EPS);
            assertEquals("rotation is hardcoded to 0f", 0f, floatAt(batch, 8), EPS);
            assertEquals(0f, floatAt(batch, 9), EPS);
            assertEquals(0f, floatAt(batch, 10), EPS);
            assertEquals(64f, floatAt(batch, 11), EPS);
            assertEquals(64f, floatAt(batch, 12), EPS);

            // The effect color passes through unchanged (WarningSign has no white-alpha rule).
            assertNotNull(batch.firstSetColor);
            assertEquals(0.7f, batch.firstSetColor.r, EPS);
            assertEquals(0.6f, batch.firstSetColor.a, EPS);

            assertTrue("WarningSign is drawable with the static texture present",
                    renderer.canDraw(effect));
        } finally {
            setStaticField(ImageMaster.class, "WARNING_ICON_VFX", previous);
        }
    }

    @Test
    public void stunStarAndFallingDustDrawTheirInstanceRegionsAmbiently() {
        // StunStarEffect and FallingDustEffect are ambient center-packed img kinds: neither calls
        // setBlendFunction, both draw their own instance AtlasRegion, and each applies only its own
        // new rule (StunStar a scaled vX/vY position offset, FallingDust the region's own offsets as
        // the origin).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        CountingBatch stunBatch = newCountingBatch();
        AbstractGameEffect stun = seededStunStar(100f, 200f, 2f, -3f, 0.5f, 30f);
        float settingsScale = com.megacrit.cardcrawl.core.Settings.scale;

        assertTrue(renderer.render(stunBatch, stun));
        assertEquals("StunStar never calls setBlendFunction", 0, stunBatch.setBlendCalls);
        assertEquals("StunStar uses the TextureRegion draw overload",
                1, stunBatch.textureRegionDrawCalls);
        assertEquals(0, stunBatch.drawCalls);
        assertNotNull("the drawn region must have been captured", stunBatch.drawnRegion);
        assertNotNull("the draw arguments must have been captured", stunBatch.drawnRegionArgs);
        assertEquals("the draw position is offset by -(vX * 30f * Settings.scale)",
                100f - 2f * 30f * settingsScale, stunBatch.drawnRegionArgs[0], EPS);
        assertEquals("the draw position is offset by -(vY * 5f * Settings.scale)",
                200f - (-3f) * 5f * settingsScale, stunBatch.drawnRegionArgs[1], EPS);

        CountingBatch dustBatch = newCountingBatch();
        AbstractGameEffect dust = seededFallingDust(11f, 22f, 6f, 10f);

        assertTrue(renderer.render(dustBatch, dust));
        assertEquals("FallingDust never calls setBlendFunction", 0, dustBatch.setBlendCalls);
        assertEquals("FallingDust uses the TextureRegion draw overload",
                1, dustBatch.textureRegionDrawCalls);
        assertEquals(0, dustBatch.drawCalls);
        assertNotNull(dustBatch.drawnRegionArgs);
        assertEquals("FallingDust draws at x", 11f, dustBatch.drawnRegionArgs[0], EPS);
        assertEquals("FallingDust draws at y", 22f, dustBatch.drawnRegionArgs[1], EPS);
        assertEquals("the origin is the region offsetX", 6f, dustBatch.drawnRegionArgs[2], EPS);
        assertEquals("the origin is the region offsetY", 10f, dustBatch.drawnRegionArgs[3], EPS);

        // canDraw mirrors the same resolution for both newest img kinds.
        assertTrue(renderer.canDraw(stun));
        assertTrue(renderer.canDraw(dust));
    }

    @Test
    public void lightningAndFlameBallDrawAdditivelyAndShineLinesAmbiently() {
        // LightningEffect (originY 0f, scale-independent) and FlameBallParticleEffect (originY
        // ph/2f + 20f * Settings.scale) are additive center-packed img kinds; ShineLinesEffect is
        // ambient center-packed and draws under the ambient blend. Each draws its own instance
        // AtlasRegion. Settings.scale is a static mutable field, so it is reflectively pinned to a
        // non-1.0 value here to guard the FlameBall origin scaling end-to-end.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        float previousSettingsScale = com.megacrit.cardcrawl.core.Settings.scale;
        float settingsScale = 1.333f;
        try {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(settingsScale));
            assertEquals(settingsScale, com.megacrit.cardcrawl.core.Settings.scale, EPS);

            CountingBatch lightningBatch = newCountingBatch();
            AbstractGameEffect lightning = seededLightning(100f, 200f, 0.5f, 30f, 64, 48);
            assertTrue(renderer.render(lightningBatch, lightning));
            assertEquals("Lightning is additive (installs and restores blend)",
                    2, lightningBatch.setBlendCalls);
            assertNotNull(lightningBatch.drawnRegionArgs);
            assertEquals(100f, lightningBatch.drawnRegionArgs[0], EPS);
            assertEquals(200f, lightningBatch.drawnRegionArgs[1], EPS);
            assertEquals("originX is packedWidth/2f", 32f, lightningBatch.drawnRegionArgs[2], EPS);
            assertEquals("originY is 0f (scale-independent)", 0f, lightningBatch.drawnRegionArgs[3],
                    EPS);
            assertTrue("Lightning can draw", renderer.canDraw(lightning));

            CountingBatch flameBatch = newCountingBatch();
            AbstractGameEffect flameBall = seededFlameBall(11f, 22f, 0.75f, 12f, 40, 24);
            assertTrue(renderer.render(flameBatch, flameBall));
            assertEquals("FlameBall is additive", 2, flameBatch.setBlendCalls);
            assertNotNull(flameBatch.drawnRegionArgs);
            assertEquals(20f, flameBatch.drawnRegionArgs[2], EPS);
            assertEquals("originY is packedHeight/2f + 20f * Settings.scale",
                    12f + 20f * settingsScale, flameBatch.drawnRegionArgs[3], EPS);
            assertTrue("FlameBall can draw", renderer.canDraw(flameBall));

            CountingBatch shineBatch = newCountingBatch();
            AbstractGameEffect shine = seededShineLines(5f, 6f, 1f, 45f, 48, 32);
            assertTrue(renderer.render(shineBatch, shine));
            assertEquals("ShineLines never calls setBlendFunction", 0, shineBatch.setBlendCalls);
            assertNotNull(shineBatch.drawnRegionArgs);
            assertEquals("originX is packedWidth/2f", 24f, shineBatch.drawnRegionArgs[2], EPS);
            assertEquals("originY is packedHeight/2f", 16f, shineBatch.drawnRegionArgs[3], EPS);
            assertTrue("ShineLines can draw", renderer.canDraw(shine));
        } finally {
            setStaticField(com.megacrit.cardcrawl.core.Settings.class, "scale",
                    Float.valueOf(previousSettingsScale));
        }
    }

    /** Real {@code LightningEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededLightning(float x, float y, float scale, float rotation,
            int pw, int ph) {
        return seedImgEffect(com.megacrit.cardcrawl.vfx.combat.LightningEffect.class,
                x, y, scale, rotation, pw, ph);
    }

    /** Real {@code FlameBallParticleEffect} with reflectively seeded draw fields + vY (no GL). */
    private static AbstractGameEffect seededFlameBall(float x, float y, float scale, float rotation,
            int pw, int ph) {
        AbstractGameEffect effect = seedImgEffect(
                com.megacrit.cardcrawl.vfx.FlameBallParticleEffect.class,
                x, y, scale, rotation, pw, ph);
        try {
            setField(effect, com.megacrit.cardcrawl.vfx.FlameBallParticleEffect.class, "vY",
                    Float.valueOf(3.5f));
        } catch (Exception failure) {
            throw new AssertionError("could not seed FlameBall vY", failure);
        }
        return effect;
    }

    /** Real {@code ShineLinesEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededShineLines(float x, float y, float scale, float rotation,
            int pw, int ph) {
        return seedImgEffect(com.megacrit.cardcrawl.vfx.ShineLinesEffect.class,
                x, y, scale, rotation, pw, ph);
    }

    /** Real {@code TorchParticleMEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededTorchParticleM(float x, float y, float scale,
            float rotation, int pw, int ph) {
        return seedImgEffect(com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect.class,
                x, y, scale, rotation, pw, ph);
    }

    /** Real {@code TorchParticleSEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededTorchParticleS(float x, float y, float scale,
            float rotation, int pw, int ph) {
        return seedImgEffect(com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect.class,
                x, y, scale, rotation, pw, ph);
    }

    /** Real {@code DustEffect} with reflectively seeded draw fields + region offsets (no GL). */
    private static AbstractGameEffect seededSceneDust(float x, float y, float offsetX,
            float offsetY) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.scene.DustEffect effect =
                    (com.megacrit.cardcrawl.vfx.scene.DustEffect)
                            unsafe.allocateInstance(com.megacrit.cardcrawl.vfx.scene.DustEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.scene.DustEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.scene.DustEffect.class, "y",
                    Float.valueOf(y));
            TextureAtlas.AtlasRegion region =
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48);
            region.offsetX = offsetX;
            region.offsetY = offsetY;
            setField(effect, com.megacrit.cardcrawl.vfx.scene.DustEffect.class, "img", region);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL DustEffect", failure);
        }
    }

    @Test
    public void flyingSpikeDrawsAdditivelyCenterPackedAndConeAmbientlyWithZeroOriginXAndScaledUp() {
        // FlyingSpikeEffect is additive center-packed (no new rule; vX/vY update-only) and ConeEffect
        // is ambient center-packed with a NEW origin rule (originX 0f, NOT packedWidth/2f) and a
        // uniform scale of scale * 1.1f. Both draw their own instance AtlasRegion; the seeded region
        // is 64x48, so the center origin would be (32, 24).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        CountingBatch spikeBatch = newCountingBatch();
        AbstractGameEffect spike = seededEffect(
                com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect.class);
        assertTrue(renderer.render(spikeBatch, spike));
        assertEquals("FlyingSpike is additive (installs and restores blend)",
                2, spikeBatch.setBlendCalls);
        assertEquals("FlyingSpike uses the TextureRegion draw overload",
                1, spikeBatch.textureRegionDrawCalls);
        assertNotNull(spikeBatch.drawnRegionArgs);
        assertEquals(5f, spikeBatch.drawnRegionArgs[0], EPS);
        assertEquals(6f, spikeBatch.drawnRegionArgs[1], EPS);
        assertEquals("originX is packedWidth/2f", 32f, spikeBatch.drawnRegionArgs[2], EPS);
        assertEquals("originY is packedHeight/2f", 24f, spikeBatch.drawnRegionArgs[3], EPS);
        assertEquals("the uniform scale is the effect scale (no 1.1 multiplier)",
                1f, spikeBatch.drawnRegionArgs[6], EPS);
        assertEquals(1f, spikeBatch.drawnRegionArgs[7], EPS);
        assertTrue("FlyingSpike can draw", renderer.canDraw(spike));

        CountingBatch coneBatch = newCountingBatch();
        AbstractGameEffect cone = seededEffect(com.megacrit.cardcrawl.vfx.ConeEffect.class);
        assertTrue(renderer.render(coneBatch, cone));
        assertEquals("ConeEffect never calls setBlendFunction", 0, coneBatch.setBlendCalls);
        assertEquals("ConeEffect uses the TextureRegion draw overload",
                1, coneBatch.textureRegionDrawCalls);
        assertNotNull(coneBatch.drawnRegionArgs);
        assertEquals("the Cone originX is 0f, NOT packedWidth/2f",
                0f, coneBatch.drawnRegionArgs[2], EPS);
        assertEquals("the Cone originY is packedHeight/2f", 24f, coneBatch.drawnRegionArgs[3], EPS);
        assertEquals("the Cone uniform scale is scale * 1.1f",
                1.1f, coneBatch.drawnRegionArgs[6], EPS);
        assertEquals("the Cone uniform scale is scale * 1.1f",
                1.1f, coneBatch.drawnRegionArgs[7], EPS);
        assertTrue("ConeEffect can draw", renderer.canDraw(cone));
    }

    @Test
    public void preexistingCenterPackedKindStillDrawsWithTheUnchangedCenterOrigin() {
        // Regression: the new CONE branch is off the shared center-packed path, so a pre-existing
        // center-packed kind still draws with origin (packedWidth/2f, packedHeight/2f) and the plain
        // effect scale.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        AbstractGameEffect torchM = seededTorchParticleM(100f, 200f, 0.5f, 30f, 52, 36);

        assertTrue(renderer.render(batch, torchM));
        assertEquals("originX is packedWidth/2f", 26f, batch.drawnRegionArgs[2], EPS);
        assertEquals("originY is packedHeight/2f", 18f, batch.drawnRegionArgs[3], EPS);
        assertEquals("a pre-existing center-packed kind keeps the plain effect scale",
                0.5f, batch.drawnRegionArgs[6], EPS);
    }

    @Test
    public void torchParticleMAndSDrawAdditivelyAndSceneDustAmbientlyWithRegionOffsetOrigin() {
        // TorchParticleMEffect/TorchParticleSEffect are additive center-packed img kinds (no new
        // rule); DustEffect is ambient center-packed and reuses the FALLING_DUST region-offset origin
        // (setColor(color) with NO setBlendFunction). Each draws its own instance AtlasRegion.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        CountingBatch torchMBatch = newCountingBatch();
        AbstractGameEffect torchM = seededTorchParticleM(100f, 200f, 0.5f, 30f, 52, 36);
        assertTrue(renderer.render(torchMBatch, torchM));
        assertEquals("TorchParticleMEffect is additive (installs and restores blend)",
                2, torchMBatch.setBlendCalls);
        assertNotNull(torchMBatch.drawnRegionArgs);
        assertEquals("originX is packedWidth/2f", 26f, torchMBatch.drawnRegionArgs[2], EPS);
        assertEquals("originY is packedHeight/2f", 18f, torchMBatch.drawnRegionArgs[3], EPS);
        assertTrue("TorchParticleMEffect can draw", renderer.canDraw(torchM));

        CountingBatch torchSBatch = newCountingBatch();
        AbstractGameEffect torchS = seededTorchParticleS(11f, 22f, 0.75f, 12f, 52, 36);
        assertTrue(renderer.render(torchSBatch, torchS));
        assertEquals("TorchParticleSEffect is additive", 2, torchSBatch.setBlendCalls);
        assertNotNull(torchSBatch.drawnRegionArgs);
        assertEquals(26f, torchSBatch.drawnRegionArgs[2], EPS);
        assertEquals(18f, torchSBatch.drawnRegionArgs[3], EPS);
        assertTrue("TorchParticleSEffect can draw", renderer.canDraw(torchS));

        CountingBatch dustBatch = newCountingBatch();
        AbstractGameEffect dust = seededSceneDust(11f, 22f, 6f, 10f);
        assertTrue(renderer.render(dustBatch, dust));
        assertEquals("DustEffect never calls setBlendFunction", 0, dustBatch.setBlendCalls);
        assertNotNull(dustBatch.drawnRegionArgs);
        assertEquals("DustEffect draws at x", 11f, dustBatch.drawnRegionArgs[0], EPS);
        assertEquals("DustEffect draws at y", 22f, dustBatch.drawnRegionArgs[1], EPS);
        assertEquals("the origin is the region offsetX", 6f, dustBatch.drawnRegionArgs[2], EPS);
        assertEquals("the origin is the region offsetY", 10f, dustBatch.drawnRegionArgs[3], EPS);
        assertTrue("DustEffect can draw", renderer.canDraw(dust));
    }

    /** Real img-based effect of {@code type} with x/y/img/scale/rotation/color seeded (no GL). */
    private static AbstractGameEffect seedImgEffect(Class<? extends AbstractGameEffect> type,
            float x, float y, float scale, float rotation, int pw, int ph) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            AbstractGameEffect effect = (AbstractGameEffect) unsafe.allocateInstance(type);
            setField(effect, type, "x", Float.valueOf(x));
            setField(effect, type, "y", Float.valueOf(y));
            setField(effect, type, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, pw, ph));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(scale));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL " + type.getSimpleName(), failure);
        }
    }

    @Test
    public void canDrawFailsOpenWithoutAValidRegionForTheNewestKinds() {
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        // StunStar with an absent img cannot draw; the same for FallingDust.
        AbstractGameEffect stunNoImg = seededStunStar(1f, 2f, 3f, 4f, 1f, 0f);
        setFieldUnchecked(stunNoImg,
                com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "img", null);
        assertFalse("a StunStar instance without its region cannot draw",
                renderer.canDraw(stunNoImg));

        AbstractGameEffect dustNoImg = seededFallingDust(1f, 2f, 6f, 10f);
        setFieldUnchecked(dustNoImg,
                com.megacrit.cardcrawl.vfx.FallingDustEffect.class, "img", null);
        assertFalse("a FallingDust instance without its region cannot draw",
                renderer.canDraw(dustNoImg));

        // The three newest img kinds: a null region fails open.
        AbstractGameEffect lightningNoImg = seededLightning(1f, 2f, 1f, 0f, 64, 48);
        setFieldUnchecked(lightningNoImg,
                com.megacrit.cardcrawl.vfx.combat.LightningEffect.class, "img", null);
        assertFalse("a LightningEffect instance without its region cannot draw",
                renderer.canDraw(lightningNoImg));

        AbstractGameEffect flameNoImg = seededFlameBall(1f, 2f, 1f, 0f, 40, 24);
        setFieldUnchecked(flameNoImg,
                com.megacrit.cardcrawl.vfx.FlameBallParticleEffect.class, "img", null);
        assertFalse("a FlameBall instance without its region cannot draw",
                renderer.canDraw(flameNoImg));

        AbstractGameEffect shineNoImg = seededShineLines(1f, 2f, 1f, 0f, 48, 32);
        setFieldUnchecked(shineNoImg,
                com.megacrit.cardcrawl.vfx.ShineLinesEffect.class, "img", null);
        assertFalse("a ShineLines instance without its region cannot draw",
                renderer.canDraw(shineNoImg));

        // The three newest scene-world img kinds: a null region fails open.
        AbstractGameEffect torchMNoImg = seededTorchParticleM(1f, 2f, 1f, 0f, 52, 36);
        setFieldUnchecked(torchMNoImg,
                com.megacrit.cardcrawl.vfx.scene.TorchParticleMEffect.class, "img", null);
        assertFalse("a TorchParticleMEffect instance without its region cannot draw",
                renderer.canDraw(torchMNoImg));

        AbstractGameEffect torchSNoImg = seededTorchParticleS(1f, 2f, 1f, 0f, 52, 36);
        setFieldUnchecked(torchSNoImg,
                com.megacrit.cardcrawl.vfx.scene.TorchParticleSEffect.class, "img", null);
        assertFalse("a TorchParticleSEffect instance without its region cannot draw",
                renderer.canDraw(torchSNoImg));

        AbstractGameEffect sceneDustNoImg = seededSceneDust(1f, 2f, 6f, 10f);
        setFieldUnchecked(sceneDustNoImg,
                com.megacrit.cardcrawl.vfx.scene.DustEffect.class, "img", null);
        assertFalse("a DustEffect instance without its region cannot draw",
                renderer.canDraw(sceneDustNoImg));

        // The two newest img kinds: a null region fails open.
        AbstractGameEffect spikeNoImg = seededEffect(
                com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect.class);
        setFieldUnchecked(spikeNoImg,
                com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect.class, "img", null);
        assertFalse("a FlyingSpikeEffect instance without its region cannot draw",
                renderer.canDraw(spikeNoImg));

        AbstractGameEffect coneNoImg = seededEffect(com.megacrit.cardcrawl.vfx.ConeEffect.class);
        setFieldUnchecked(coneNoImg, com.megacrit.cardcrawl.vfx.ConeEffect.class, "img", null);
        assertFalse("a ConeEffect instance without its region cannot draw",
                renderer.canDraw(coneNoImg));
    }

    private static void setFieldUnchecked(Object target, Class<?> owner, String name,
            Object value) {
        try {
            setField(target, owner, name, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set " + owner + "." + name, failure);
        }
    }

    @Test
    public void guardSatisfiedReadsTheGuardFieldAndFailsOpenWhenAbsent() {
        // No waitTimer field at all => treated as SATISFIED (the native render would not be able to
        // read a guard either), so ART draws.
        assertTrue("an absent guard field draws (treated unguarded)",
                Sts1VfxArtRenderer.guardSatisfied(
                        VfxDrawGeometry.Kind.FALLING_ICE, new IceShatterEffect()));

        // A present waitTimer < 0f satisfies the guard (native draws).
        FallingIceGuardEffect satisfied = new FallingIceGuardEffect();
        satisfied.waitTimer = -0.5f;
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(
                VfxDrawGeometry.Kind.FALLING_ICE, satisfied));

        // A present waitTimer >= 0f blocks the guard (native draws nothing).
        FallingIceGuardEffect blocked = new FallingIceGuardEffect();
        blocked.waitTimer = 0f;
        assertFalse(Sts1VfxArtRenderer.guardSatisfied(
                VfxDrawGeometry.Kind.FALLING_ICE, blocked));

        // A non-guard kind is never gated, even when a waitTimer field is present.
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(VfxDrawGeometry.Kind.ICE_SHATTER, blocked));

        // DAMAGE_HEART reads delayTimer, so a holder with only waitTimer draws.
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(
                VfxDrawGeometry.Kind.DAMAGE_HEART, blocked));
        DamageHeartGuardEffect heartBlocked = new DamageHeartGuardEffect();
        heartBlocked.delayTimer = 0.25f;
        assertFalse(Sts1VfxArtRenderer.guardSatisfied(
                VfxDrawGeometry.Kind.DAMAGE_HEART, heartBlocked));
        DamageHeartGuardEffect heartOk = new DamageHeartGuardEffect();
        heartOk.delayTimer = -1f;
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(
                VfxDrawGeometry.Kind.DAMAGE_HEART, heartOk));

        // F24: WRATH reads the same delayTimer field but with a DIFFERENT threshold — it blocks only
        // when > 0f, so delayTimer == 0f is SATISFIED (draws) while DAMAGE_HEART is blocked.
        DamageHeartGuardEffect atZero = new DamageHeartGuardEffect();
        atZero.delayTimer = 0f;
        assertFalse("DAMAGE_HEART blocks at delayTimer == 0f",
                Sts1VfxArtRenderer.guardSatisfied(VfxDrawGeometry.Kind.DAMAGE_HEART, atZero));
        assertTrue("WRATH draws at delayTimer == 0f",
                Sts1VfxArtRenderer.guardSatisfied(
                        VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, atZero));
        DamageHeartGuardEffect positive = new DamageHeartGuardEffect();
        positive.delayTimer = 0.25f;
        assertFalse("WRATH blocks at delayTimer > 0f",
                Sts1VfxArtRenderer.guardSatisfied(
                        VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, positive));

        // NaN parity: FALLING_ICE/DAMAGE_HEART draw iff field < 0f (NaN blocks), while WRATH returns
        // iff delayTimer > 0f (NaN draws).
        DamageHeartGuardEffect nan = new DamageHeartGuardEffect();
        nan.delayTimer = Float.NaN;
        assertFalse("NaN blocks DAMAGE_HEART (native NaN < 0f is false)",
                Sts1VfxArtRenderer.guardSatisfied(VfxDrawGeometry.Kind.DAMAGE_HEART, nan));
        assertTrue("NaN does not block WRATH (native NaN > 0f is false)",
                Sts1VfxArtRenderer.guardSatisfied(
                        VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE, nan));
        FallingIceGuardEffect nanIce = new FallingIceGuardEffect();
        nanIce.waitTimer = Float.NaN;
        assertFalse("NaN blocks FALLING_ICE (native NaN < 0f is false)",
                Sts1VfxArtRenderer.guardSatisfied(VfxDrawGeometry.Kind.FALLING_ICE, nanIce));

        // Null inputs fail open (draw).
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(VfxDrawGeometry.Kind.FALLING_ICE, null));
        assertTrue(Sts1VfxArtRenderer.guardSatisfied(null, blocked));
    }

    @Test
    public void fallingIceRenderAndCanDrawFollowTheNativeWaitGuard() {
        // Native FallingIceEffect: if (waitTimer < 0f) { ... sb.draw ... }. A guard-satisfied holder
        // draws; a guard-blocked one draws nothing and cannotDraw (native also draws nothing).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        AbstractGameEffect satisfied = seededFallingIce(-0.5f);
        assertTrue("a waitTimer < 0 FallingIceEffect draws", renderer.render(
                newCountingBatch(), satisfied));
        assertTrue("a waitTimer < 0 FallingIceEffect canDraw", renderer.canDraw(satisfied));

        AbstractGameEffect blocked = seededFallingIce(0.25f);
        assertFalse("a waitTimer >= 0 FallingIceEffect declines (native wait phase)",
                renderer.render(newCountingBatch(), blocked));
        assertFalse("a waitTimer >= 0 FallingIceEffect cannotDraw", renderer.canDraw(blocked));

        AbstractGameEffect exactZero = seededFallingIce(0f);
        assertFalse("waitTimer == 0f is still the native wait phase",
                renderer.canDraw(exactZero));
    }

    @Test
    public void damageHeartRenderAndCanDrawFollowTheNativeDelayGuard() {
        // Native DamageHeartEffect: if (delayTimer < 0f) { setColor; sb.draw(img, x, y, pw/2f,
        // ph/2f, pw, ph, scale, scale, rotation); }. The img path resolves the public AtlasRegion.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        AbstractGameEffect satisfied = seededDamageHeart(-1f, 64, 48);
        assertTrue("a delayTimer < 0 DamageHeartEffect draws", renderer.render(
                newCountingBatch(), satisfied));
        assertTrue("a delayTimer < 0 DamageHeartEffect canDraw", renderer.canDraw(satisfied));

        AbstractGameEffect blocked = seededDamageHeart(0f, 64, 48);
        assertFalse("a delayTimer >= 0 DamageHeartEffect declines (native delay phase)",
                renderer.render(newCountingBatch(), blocked));
        assertFalse("a delayTimer >= 0 DamageHeartEffect cannotDraw", renderer.canDraw(blocked));
    }

    @Test
    public void fallingIceShapeCRequiresInstanceTextureAndRotation() {
        // FALLING_ICE uses the shape-C instance-Texture path: an instance Texture img is required
        // (an AtlasRegion is rejected) and a rotation field is required.
        Texture img = noGlTexture(96, 96);

        IceShatterRotationEffect ok = new IceShatterRotationEffect();
        ok.x = 1f;
        ok.y = 2f;
        ok.scale = 1f;
        ok.rotation = 12f;
        ok.color = Color.WHITE;
        ok.img = img;
        Sts1VfxArtRenderer.TextureFields resolved = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.FALLING_ICE, ok);
        assertNotNull("an instance Texture img resolves for FALLING_ICE", resolved);
        assertEquals(12f, resolved.rotation, EPS);
        assertSame(img, resolved.img);

        IceShatterAtlasImgEffect atlas = new IceShatterAtlasImgEffect();
        atlas.x = 1f;
        atlas.y = 2f;
        atlas.scale = 1f;
        atlas.rotation = 0f;
        atlas.color = Color.WHITE;
        atlas.img = fakeRegion();
        assertNull("an AtlasRegion img fails the FALLING_ICE snapshot",
                Sts1VfxArtRenderer.readTextureFields(VfxDrawGeometry.Kind.FALLING_ICE, atlas));

        IceShatterNoRotationEffect noRotation = new IceShatterNoRotationEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = img;
        assertNull("a FALLING_ICE holder without a rotation field fails open",
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.FALLING_ICE, noRotation));
    }

    @Test
    public void fallingIceDrawUsesTheFixed96RectAndTheFieldRotationAdditively() {
        // Native FallingIceEffect: setBlendFunction(770,1); setColor(color);
        //   sb.draw(img, x, y, 48f, 48f, 96f, 96f, scale, scale, rotation, 0, 0, 96, 96,
        //           false, false); setBlendFunction(770,771).
        // A guard-satisfied holder draws the fixed 96x96 rect over its own instance Texture.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        AbstractGameEffect effect = seededFallingIce(-0.5f);
        setFieldUnchecked(effect,
                com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "x", Float.valueOf(5f));
        setFieldUnchecked(effect,
                com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "y", Float.valueOf(6f));
        setFieldUnchecked(effect, AbstractGameEffect.class, "scale", Float.valueOf(0.6f));
        setFieldUnchecked(effect, AbstractGameEffect.class, "rotation", Float.valueOf(45f));

        assertTrue(renderer.render(batch, effect));

        assertEquals("the additive FALLING_ICE draw installs and restores blend",
                2, batch.setBlendCalls);
        assertEquals("FALLING_ICE uses the raw-texture draw overload", 1, batch.drawCalls);
        assertNotNull("the drawn texture was captured", batch.drawnTexture);

        // draw(tex, x, y, 48, 48, 96, 96, scale, scale, rotation, 0, 0, 96, 96, false, false)
        assertEquals(5f, floatAt(batch, 0), EPS);
        assertEquals(6f, floatAt(batch, 1), EPS);
        assertEquals(48f, floatAt(batch, 2), EPS);
        assertEquals(48f, floatAt(batch, 3), EPS);
        assertEquals(96f, floatAt(batch, 4), EPS);
        assertEquals(96f, floatAt(batch, 5), EPS);
        assertEquals(0.6f, floatAt(batch, 6), EPS);
        assertEquals(0.6f, floatAt(batch, 7), EPS);
        assertEquals("the rotation field is consumed", 45f, floatAt(batch, 8), EPS);
        assertEquals(0f, floatAt(batch, 9), EPS);
        assertEquals(0f, floatAt(batch, 10), EPS);
        assertEquals(96f, floatAt(batch, 11), EPS);
        assertEquals(96f, floatAt(batch, 12), EPS);
        assertFalse("FALLING_ICE passes no per-instance flip X", batch.drawnFlipX);
        assertFalse("FALLING_ICE passes no per-instance flip Y", batch.drawnFlipY);
    }

    // --- F24 WrathStanceChangeParticle: RNG replay + player-hitbox-relative x ---

    /** Real {@code WrathStanceChangeParticle} with seeded draw fields (no GL/game context). */
    private static AbstractGameEffect seededWrathStanceChange(float delayTimer, float x, float y,
            float scale, float rotation) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle effect =
                    (com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle.class);
            setField(effect, com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle.class,
                    "x", Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle.class,
                    "y", Float.valueOf(y));
            setField(effect, com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle.class,
                    "img", new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            setField(effect, com.megacrit.cardcrawl.vfx.stance.WrathStanceChangeParticle.class,
                    "delayTimer", Float.valueOf(delayTimer));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(scale));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL WrathStanceChangeParticle", failure);
        }
    }

    @Test
    public void wrathStanceChangeRestoresRngOnAPostConsumptionFailOpen() {
        // Native-call-sequence contract: the global RNG must advance EXACTLY ONCE per successful
        // draw and never on a fail-open. If the batch draw throws AFTER the two random values were
        // pulled, the renderer must restore the RNG snapshot so the patch's native fallback then
        // consumes exactly the values it expects and the stream does not drift.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Hitbox hb = new Hitbox(0f, 0f, 100f, 30f);
        hb.cX = 400f;
        PlayerHitboxStub player = new PlayerHitboxStub();
        player.hb = hb;
        Sts1VfxArtRenderer.setPlayerForTests(player);
        java.util.Random savedRandom = MathUtils.random;
        com.badlogic.gdx.math.RandomXS128 rng = new com.badlogic.gdx.math.RandomXS128(424242L);
        MathUtils.random = rng;
        try {
            AbstractGameEffect effect = seededWrathStanceChange(0f, 7.5f, -3.25f, 0.6f, 24f);
            long before0 = rng.getState(0);
            long before1 = rng.getState(1);

            CountingBatch batch = newCountingBatch();
            batch.throwOnRegionDraw = true;
            assertFalse("a post-consumption throw must fail open",
                    renderer.render(batch, effect));

            // The RNG stream is exactly where it was before the call, so native can consume the two
            // values it needs without any drift.
            assertEquals("RNG seed0 is restored after a post-consumption fail-open",
                    before0, rng.getState(0));
            assertEquals("RNG seed1 is restored after a post-consumption fail-open",
                    before1, rng.getState(1));

            // A subsequent successful draw then reproduces the FIRST two values of the stream (the
            // same values the failed draw would have used), proving no values were lost.
            com.badlogic.gdx.math.RandomXS128 mirror =
                    new com.badlogic.gdx.math.RandomXS128(424242L);
            float expected0 = mirror.nextFloat();
            float expected1 = mirror.nextFloat();
            CountingBatch okBatch = newCountingBatch();
            assertTrue("the retry draws", renderer.render(okBatch, effect));
            assertNotNull(okBatch.drawnRegionArgs);
            assertEquals(0.6f * (2.9f + expected0 * (3.1f - 2.9f)),
                    okBatch.drawnRegionArgs[6], EPS);
            assertEquals(0.6f * (0.95f + expected1 * (1.05f - 0.95f)),
                    okBatch.drawnRegionArgs[7], EPS);
        } finally {
            MathUtils.random = savedRandom;
            Sts1VfxArtRenderer.resetPlayerForTests();
        }
    }

    /** Counts the exact number of {@code nextFloat()} draws the renderer pulls from the RNG. */
    static final class CountingRandom extends java.util.Random {
        int floatCalls;

        @Override
        public float nextFloat() {
            floatCalls++;
            return super.nextFloat();
        }
    }

    /** Player stub carrying a {@link Hitbox} for the player-hitbox-relative draw (no STS class). */
    static class PlayerHitboxStub {
        Hitbox hb;
    }

    /** Player stub WITHOUT an {@code hb} field (the unreadable-hitbox fail-open case). */
    static class PlayerNoHitboxStub {
    }

    @Test
    public void wrathStanceChangeDrawsPlayerRelativeWithExactlyTwoOrderedRngDraws() {
        // Native WrathStanceChangeParticle.render:
        //   if (delayTimer > 0f) return;
        //   setColor(color); setBlendFunction(770, 1);
        //   sb.draw(img, AbstractDungeon.player.hb.cX + x, y, pw/2f, ph/2f, pw, ph,
        //           scale * MathUtils.random(2.9f, 3.1f), scale * MathUtils.random(0.95f, 1.05f),
        //           rotation);
        //   setBlendFunction(770, 771);
        // The player is supplied through the renderer's test seam so no live AbstractDungeon (whose
        // static init needs GL) is touched.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Hitbox hb = new Hitbox(0f, 0f, 100f, 30f);
        hb.cX = 400f;
        PlayerHitboxStub player = new PlayerHitboxStub();
        player.hb = hb;
        Sts1VfxArtRenderer.setPlayerForTests(player);
        java.util.Random savedRandom = MathUtils.random;
        // Two identically seeded generators: one drives MathUtils, the mirror predicts the expected
        // values so the EXACT ranges and ORDER (scaleX then scaleY) are asserted, and the third draw
        // match proves the renderer consumed exactly two.
        java.util.Random live = new java.util.Random(987654321L);
        java.util.Random mirror = new java.util.Random(987654321L);
        MathUtils.random = live;
        try {
            CountingBatch batch = newCountingBatch();
            AbstractGameEffect effect = seededWrathStanceChange(0f, 7.5f, -3.25f, 0.6f, 24f);

            assertTrue("a delayTimer <= 0 Wrath draws", renderer.render(batch, effect));
            assertEquals("Wrath installs and restores the additive blend", 2, batch.setBlendCalls);
            assertEquals("Wrath uses the TextureRegion draw overload",
                    1, batch.textureRegionDrawCalls);
            assertNotNull(batch.drawnRegionArgs);
            assertEquals("x is the player hitbox center X plus the effect x",
                    400f + 7.5f, batch.drawnRegionArgs[0], EPS);
            assertEquals(-3.25f, batch.drawnRegionArgs[1], EPS);
            assertEquals("originX is packedWidth/2f", 32f, batch.drawnRegionArgs[2], EPS);
            assertEquals("originY is packedHeight/2f", 24f, batch.drawnRegionArgs[3], EPS);

            float r0 = 2.9f + mirror.nextFloat() * (3.1f - 2.9f);
            float r1 = 0.95f + mirror.nextFloat() * (1.05f - 0.95f);
            assertEquals("scaleX is scale * the first (2.9,3.1) RNG draw",
                    0.6f * r0, batch.drawnRegionArgs[6], EPS);
            assertEquals("scaleY is scale * the second (0.95,1.05) RNG draw",
                    0.6f * r1, batch.drawnRegionArgs[7], EPS);
            assertEquals("the draw consumed exactly two RNG values in order",
                    mirror.nextFloat(), live.nextFloat(), 0f);
            assertEquals(24f, batch.drawnRegionArgs[8], EPS);
            assertTrue("a drawable Wrath instance canDraw", renderer.canDraw(effect));
        } finally {
            MathUtils.random = savedRandom;
            Sts1VfxArtRenderer.resetPlayerForTests();
        }
    }

    @Test
    public void wrathStanceChangeGuardedOutDeclinesWithoutConsumingRng() {
        // delayTimer > 0f is the native wait phase (if (delayTimer > 0f) return): ART must draw
        // nothing AND must leave the global RNG stream untouched so native consumes it instead.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Hitbox hb = new Hitbox(0f, 0f, 100f, 30f);
        hb.cX = 400f;
        PlayerHitboxStub player = new PlayerHitboxStub();
        player.hb = hb;
        Sts1VfxArtRenderer.setPlayerForTests(player);
        java.util.Random savedRandom = MathUtils.random;
        CountingRandom counting = new CountingRandom();
        MathUtils.random = counting;
        try {
            AbstractGameEffect blocked = seededWrathStanceChange(0.25f, 7.5f, -3.25f, 0.6f, 24f);
            assertFalse("delayTimer > 0 declines (native wait phase)",
                    renderer.render(newCountingBatch(), blocked));
            assertEquals("a guarded-out Wrath consumes NO RNG", 0, counting.floatCalls);
            assertFalse("a guarded-out Wrath cannotDraw", renderer.canDraw(blocked));
            assertTrue("a guarded-out Wrath is a benign no-pixel decline",
                    renderer.declinedWithoutPixels(blocked));
        } finally {
            MathUtils.random = savedRandom;
            Sts1VfxArtRenderer.resetPlayerForTests();
        }
    }

    @Test
    public void wrathStanceChangeFailsOpenWithoutAConsumedRngWhenThePlayerIsAbsent() {
        // The player-relative x needs a readable player hitbox center; when it is absent ART must
        // fail open to the native draw rather than draw at a wrong position, and must NOT have pulled
        // RNG (all fail-open-capable checks precede the RNG).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        // A player object with NO hb field: the hitbox is unreadable, so fail open.
        Sts1VfxArtRenderer.setPlayerForTests(new PlayerNoHitboxStub());
        java.util.Random savedRandom = MathUtils.random;
        CountingRandom counting = new CountingRandom();
        MathUtils.random = counting;
        try {
            AbstractGameEffect effect = seededWrathStanceChange(0f, 7.5f, -3.25f, 0.6f, 24f);
            assertFalse("no player hitbox means fail open",
                    renderer.render(newCountingBatch(), effect));
            assertEquals("a player-absent fail-open consumes NO RNG", 0, counting.floatCalls);
            assertFalse("no player hitbox means cannotDraw", renderer.canDraw(effect));
        } finally {
            MathUtils.random = savedRandom;
            Sts1VfxArtRenderer.resetPlayerForTests();
        }
    }

    @Test
    public void preExistingNonRngKindStillDrawsUnchangedAndConsumesNoRng() {
        // A pre-existing kind (STANCE_AURA) keeps scaleX == scaleY == scale and pulls no RNG.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        java.util.Random savedRandom = MathUtils.random;
        CountingRandom counting = new CountingRandom();
        MathUtils.random = counting;
        try {
            CountingBatch batch = newCountingBatch();
            AbstractGameEffect effect = seededEffect(StanceAuraEffect.class);
            setFieldUnchecked(effect, AbstractGameEffect.class, "scale", Float.valueOf(0.9f));
            assertTrue(renderer.render(batch, effect));
            assertNotNull(batch.drawnRegionArgs);
            assertEquals("a non-RNG kind keeps scaleX == scale", 0.9f, batch.drawnRegionArgs[6], EPS);
            assertEquals("a non-RNG kind keeps scaleY == scale", 0.9f, batch.drawnRegionArgs[7], EPS);
            assertEquals("a non-RNG kind consumes no RNG", 0, counting.floatCalls);
        } finally {
            MathUtils.random = savedRandom;
        }
    }

    @Test
    public void declinedWithoutPixelsIsInstanceAwareForTheGuardKinds() {
        // NRO-04 F21 benign predicate: true only when THIS instance would natively draw nothing —
        // a guard kind whose guard actually BLOCKS, not merely a guard-satisfied-but-undrawable one.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        try {
            VfxArtRenderer.install(renderer);
            assertTrue("the static delegator mirrors the installed adapter",
                    VfxArtRenderer.declinedWithoutPixels(seededFallingIce(0.5f)));
            assertFalse("the static delegator means the same thing for a satisfied instance",
                    VfxArtRenderer.declinedWithoutPixels(seededFallingIce(-0.5f)));
        } finally {
            VfxArtRenderer.uninstall();
        }

        // Guard BLOCKS (>= 0): benign, whether or not the image is present.
        assertTrue("a blocked FallingIce instance is benign",
                renderer.declinedWithoutPixels(seededFallingIce(0.5f)));
        assertTrue("a blocked DamageHeart instance is benign",
                renderer.declinedWithoutPixels(seededDamageHeart(0f, 64, 48)));

        // Guard SATISFIED (< 0) with a present image: itself drawable, so not a no-pixel instance.
        assertFalse("a drawable guard-satisfied FallingIce instance is not benign",
                renderer.declinedWithoutPixels(seededFallingIce(-0.5f)));
        assertFalse("a drawable guard-satisfied DamageHeart instance is not benign",
                renderer.declinedWithoutPixels(seededDamageHeart(-1f, 64, 48)));

        // Guard SATISFIED (< 0) but the image snapshot fails: NOT benign (native would draw), so a
        // genuine renderer failure is never masked.
        AbstractGameEffect satisfiedNoImg = seededFallingIce(-0.5f);
        setFieldUnchecked(satisfiedNoImg,
                com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "img", null);
        assertFalse("a guard-satisfied FallingIce instance with no image is not benign",
                renderer.declinedWithoutPixels(satisfiedNoImg));

        // A non-guard kind is never benign.
        assertFalse("a non-guard kind is never a benign guard decline",
                renderer.declinedWithoutPixels(new IceShatterRotationEffect()));

        // Degenerate inputs fail closed.
        assertFalse(renderer.declinedWithoutPixels(null));
        assertFalse(renderer.declinedWithoutPixels("not an effect"));
    }

    @Test
    public void declinedWithoutPixelsDelegatorReturnsFalseWithoutAnAdapter() {
        VfxArtRenderer.uninstall();
        assertFalse("no adapter means no benign classification",
                VfxArtRenderer.declinedWithoutPixels(seededFallingIce(0.5f)));
    }

    @Test
    public void damageHeartResolvesItsPublicImgAndConsumesRotation() {
        // DAMAGE_HEART is a center-packed img kind: the DECLARED-field walk resolves its public
        // AtlasRegion img and its inherited rotation.
        AbstractGameEffect heart = seededDamageHeart(-1f, 64, 48);
        Sts1VfxArtRenderer.Fields f = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.DAMAGE_HEART, heart);
        assertNotNull("DamageHeart resolves its public img", f);
        assertEquals(5f, f.x, EPS);
        assertEquals(6f, f.y, EPS);
        assertEquals(1f, f.scale, EPS);
        assertNotNull("the public AtlasRegion img resolved", f.img);
        assertTrue("DamageHeart canDraw when its guard is satisfied", new Sts1VfxArtRenderer()
                .canDraw(heart));
    }

    /** Real {@code FallingIceEffect} with a seeded {@code waitTimer} and shape-C draw fields. */
    private static AbstractGameEffect seededFallingIce(float waitTimer) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.combat.FallingIceEffect effect =
                    (com.megacrit.cardcrawl.vfx.combat.FallingIceEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "x",
                    Float.valueOf(5f));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "y",
                    Float.valueOf(6f));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "img",
                    noGlTexture(96, 96));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.FallingIceEffect.class, "waitTimer",
                    Float.valueOf(waitTimer));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL FallingIceEffect", failure);
        }
    }

    /** Real {@code DamageHeartEffect} with a seeded {@code delayTimer} and center-packed fields. */
    private static AbstractGameEffect seededDamageHeart(float delayTimer, int pw, int ph) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.DamageHeartEffect effect =
                    (com.megacrit.cardcrawl.vfx.DamageHeartEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.DamageHeartEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.DamageHeartEffect.class, "x",
                    Float.valueOf(5f));
            setField(effect, com.megacrit.cardcrawl.vfx.DamageHeartEffect.class, "y",
                    Float.valueOf(6f));
            setField(effect, com.megacrit.cardcrawl.vfx.DamageHeartEffect.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, pw, ph));
            setField(effect, com.megacrit.cardcrawl.vfx.DamageHeartEffect.class, "delayTimer",
                    Float.valueOf(delayTimer));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL DamageHeartEffect", failure);
        }
    }

    /** Real {@code WarningSignEffect} with reflectively seeded x/y and color (no GL). */
    private static AbstractGameEffect seededWarningSign(Color color, float x, float y) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.WarningSignEffect effect =
                    (com.megacrit.cardcrawl.vfx.WarningSignEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.WarningSignEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.WarningSignEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.WarningSignEffect.class, "y",
                    Float.valueOf(y));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL WarningSignEffect", failure);
        }
    }

    /** Real {@code StunStarEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededStunStar(float x, float y, float vX, float vY,
            float scale, float rotation) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.combat.StunStarEffect effect =
                    (com.megacrit.cardcrawl.vfx.combat.StunStarEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "y",
                    Float.valueOf(y));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "vX",
                    Float.valueOf(vX));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "vY",
                    Float.valueOf(vY));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "scale",
                    Float.valueOf(scale));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.StunStarEffect.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL StunStarEffect", failure);
        }
    }

    /** Real {@code FallingDustEffect} with reflectively seeded draw fields (no GL). */
    private static AbstractGameEffect seededFallingDust(float x, float y, float offsetX,
            float offsetY) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.FallingDustEffect effect =
                    (com.megacrit.cardcrawl.vfx.FallingDustEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.FallingDustEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.FallingDustEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.FallingDustEffect.class, "y",
                    Float.valueOf(y));
            TextureAtlas.AtlasRegion region =
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48);
            region.offsetX = offsetX;
            region.offsetY = offsetY;
            setField(effect, com.megacrit.cardcrawl.vfx.FallingDustEffect.class, "img", region);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL FallingDustEffect", failure);
        }
    }

    @Test
    public void renderFailsOpenForNullArgumentsAndNeverThrows() {
        // A real SpriteBatch draw needs a live GL context, so it is not constructible in a headless
        // unit test; the null-arg path proves the F2b2 render entry point fails open. The actual
        // pixels are covered by on-device visual verification.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        assertFalse(renderer.render(null, null));
    }

    @Test
    public void ambientKindDoesNotSwitchBlendFunctionWhileAdditiveKindDoes() {
        // Per-kind blend policy end-to-end at the render entry point: FlashAtkImgEffect never calls
        // setBlendFunction natively, so the claim draw must draw under the ambient blend and restore
        // only color. A counting SpriteBatch double (no GL: the override never reaches the real
        // blend/flush path) proves the call count.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        CountingBatch ambient = newCountingBatch();
        AbstractGameEffect flash = flashEffect();
        assertTrue(renderer.render(ambient, flash));
        assertEquals("the ambient kind must not touch the blend function",
                0, ambient.setBlendCalls);
        // The per-instance color is restored to the previous (white) value; the packed float
        // round-trip may lose at most one 8-bit step, so compare with that tolerance.
        assertEquals("the ambient draw must still restore the previous color",
                1f, ambient.getColor().a, 1f / 255f);
        assertEquals(1f, ambient.getColor().r, 1f / 255f);

        CountingBatch additive = newCountingBatch();
        AbstractGameEffect aura = auraEffect();
        assertTrue(renderer.render(additive, aura));
        assertEquals("an additive kind installs and restores the blend function",
                2, additive.setBlendCalls);

        // The first ambient members beyond FlashAtkImgEffect: SmokeBlurEffect never calls
        // setBlendFunction natively, so its claim draw must leave the blend untouched.
        CountingBatch smoke = newCountingBatch();
        AbstractGameEffect smokeBlur = seededEffect(SmokeBlurEffect.class);
        assertTrue(renderer.render(smoke, smokeBlur));
        assertEquals("the ambient member SmokeBlurEffect must not touch the blend function",
                0, smoke.setBlendCalls);

        // The two fire bursts are additive despite being fire-family.
        CountingBatch fire = newCountingBatch();
        AbstractGameEffect fireBurst = seededEffect(FireBurstParticleEffect.class);
        assertTrue(renderer.render(fire, fireBurst));
        assertEquals("the additive member FireBurstParticleEffect installs and restores blend",
                2, fire.setBlendCalls);

        // The four newest members: the generic smoke and exhaust blur never call setBlendFunction
        // natively (ambient), while the torch XL and ghostly weak fire are additive.
        CountingBatch smoke2 = newCountingBatch();
        AbstractGameEffect genericSmoke = seededEffect(GenericSmokeEffect.class);
        assertTrue(renderer.render(smoke2, genericSmoke));
        assertEquals("the ambient member GenericSmokeEffect must not touch the blend function",
                0, smoke2.setBlendCalls);

        CountingBatch blur = newCountingBatch();
        AbstractGameEffect exhaustBlur = seededEffect(ExhaustBlurEffect.class);
        assertTrue(renderer.render(blur, exhaustBlur));
        assertEquals("the ambient member ExhaustBlurEffect must not touch the blend function",
                0, blur.setBlendCalls);

        CountingBatch torchXl = newCountingBatch();
        AbstractGameEffect torchXlEffect = seededEffect(TorchParticleXLEffect.class);
        assertTrue(renderer.render(torchXl, torchXlEffect));
        assertEquals("the additive member TorchParticleXLEffect installs and restores blend",
                2, torchXl.setBlendCalls);

        CountingBatch ghostly = newCountingBatch();
        AbstractGameEffect ghostlyFire = seededEffect(GhostlyWeakFireEffect.class);
        assertTrue(renderer.render(ghostly, ghostlyFire));
        assertEquals("the additive member GhostlyWeakFireEffect installs and restores blend",
                2, ghostly.setBlendCalls);
    }

    @Test
    public void webParticleDrawForcesWhiteRgbAndKeepsTheEffectAlpha() {
        // WebParticleEffect is additive and is the only kind that rewrites its set color:
        // sb.setColor(new Color(1f, 1f, 1f, color.a)). A color-recording SpriteBatch double (no GL)
        // asserts the applied tint end-to-end through render -> renderTexture; the static
        // ImageMaster.WEB_VFX texture is injected via reflection (no GL) and restored afterwards.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Texture previousWebVfx = ImageMaster.WEB_VFX;
        Texture webVfx = noGlTexture(64, 64);
        try {
            setStaticField(ImageMaster.class, "WEB_VFX", webVfx);
            assertSame("the injected static texture doubles for ImageMaster.WEB_VFX",
                    webVfx, ImageMaster.WEB_VFX);

            CountingBatch batch = newCountingBatch();
            AbstractGameEffect web = seededWebEffect(new Color(0.2f, 0.4f, 0.6f, 0.35f));

            assertTrue(renderer.render(batch, web));
            assertEquals("the additive Web draw installs and restores blend",
                    2, batch.setBlendCalls);

            assertNotNull("the applied tint must have been captured", batch.firstSetColor);
            assertEquals("RGB is forced to white", 1f, batch.firstSetColor.r, EPS);
            assertEquals(1f, batch.firstSetColor.g, EPS);
            assertEquals(1f, batch.firstSetColor.b, EPS);
            assertEquals("alpha comes from the effect color", 0.35f, batch.firstSetColor.a, EPS);

            // The per-draw color is restored to the previous (white) color; the packed-float
            // round-trip may lose at most one 8-bit step, so compare with that tolerance.
            assertEquals("the Web draw restores the previous color (r)",
                    1f, batch.getColor().r, 1f / 255f);
            assertEquals("the Web draw restores the previous color (g)",
                    1f, batch.getColor().g, 1f / 255f);
            assertEquals("the Web draw restores the previous color (b)",
                    1f, batch.getColor().b, 1f / 255f);
            assertEquals("the Web draw restores the previous color (a)",
                    1f, batch.getColor().a, 1f / 255f);

            // The pure seam reports the same rule for the kind.
            assertTrue(VfxDrawGeometry.whiteAlphaOnly(VfxDrawGeometry.Kind.WEB_PARTICLE));
        } finally {
            setStaticField(ImageMaster.class, "WEB_VFX", previousWebVfx);
        }
    }

    @Test
    public void iceShatterDrawUsesTheInstanceTextureAndTheRotationFieldAdditively() {
        // IceShatterEffect is additive and (unlike Shield/Web) draws its OWN instance Texture img
        // with the rotation field consumed. A draw-recording SpriteBatch double (no GL) asserts the
        // exact raw-texture + source-rect call end-to-end through render -> renderTexture.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        Color iceColor = new Color(0.7f, 0.8f, 0.9f, 0.6f);
        AbstractGameEffect ice = seededIceEffect(iceColor, 12.5f, -3.25f, 1.25f, 137f);

        assertTrue(renderer.render(batch, ice));

        assertEquals("the additive ICE draw installs and restores blend",
                2, batch.setBlendCalls);
        assertEquals("the kind must emit exactly one draw", 1, batch.drawCalls);
        assertNotNull("the draw arguments must have been captured", batch.drawnArgs);

        // The ICE branch resolves the effect's own instance Texture, not a static one.
        assertSame("ICE must draw its own instance Texture img",
                readField(ice, "img"), batch.drawnTexture);

        // draw(texture, x, y, 32f, 32f, 64f, 64f, scale, scale, rotation, 0, 0, 64, 64, false, false)
        assertEquals(12.5f, floatAt(batch, 0), EPS);
        assertEquals(-3.25f, floatAt(batch, 1), EPS);
        assertEquals(32f, floatAt(batch, 2), EPS);
        assertEquals(32f, floatAt(batch, 3), EPS);
        assertEquals(64f, floatAt(batch, 4), EPS);
        assertEquals(64f, floatAt(batch, 5), EPS);
        assertEquals(1.25f, floatAt(batch, 6), EPS);
        assertEquals(1.25f, floatAt(batch, 7), EPS);
        assertEquals("ICE must consume the rotation field", 137f, floatAt(batch, 8), EPS);
        assertEquals(0f, floatAt(batch, 9), EPS);
        assertEquals(0f, floatAt(batch, 10), EPS);
        assertEquals(64f, floatAt(batch, 11), EPS);
        assertEquals(64f, floatAt(batch, 12), EPS);

        // The ICE color is the effect's own color, passed through unchanged (no white-alpha rule).
        assertNotNull(batch.firstSetColor);
        assertEquals(0.7f, batch.firstSetColor.r, EPS);
        assertEquals(0.8f, batch.firstSetColor.g, EPS);
        assertEquals(0.9f, batch.firstSetColor.b, EPS);
        assertEquals(0.6f, batch.firstSetColor.a, EPS);
    }

    @Test
    public void newAmbientAndAdditiveKindsDrawThroughTheExpectedShapes() {
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Texture previousWebVfx = ImageMaster.WEB_VFX;
        Texture webVfx = noGlTexture(64, 64);
        try {
            setStaticField(ImageMaster.class, "WEB_VFX", webVfx);

            // EntangleEffect reuses the additive shape-C path with the static WEB_VFX texture and
            // the shared white-alpha rule; it has no img/rotation needs.
            CountingBatch entangleBatch = newCountingBatch();
            AbstractGameEffect entangle = seededEntangle(new Color(0.2f, 0.4f, 0.6f, 0.35f));
            assertTrue(renderer.render(entangleBatch, entangle));
            assertEquals("Entangle is additive", 2, entangleBatch.setBlendCalls);
            assertSame("Entangle draws the static WEB_VFX texture",
                    webVfx, entangleBatch.drawnTexture);
            assertNotNull(entangleBatch.firstSetColor);
            assertEquals("Entangle forces RGB white", 1f, entangleBatch.firstSetColor.r, EPS);
            assertEquals(0.35f, entangleBatch.firstSetColor.a, EPS);
            // rotation is hardcoded to 0f
            assertEquals(0f, floatAt(entangleBatch, 8), EPS);

            // BlockImpactLineEffect is ambient center-packed over its own instance AtlasRegion.
            CountingBatch blockBatch = newCountingBatch();
            AbstractGameEffect block = seededBlockImpactLine();
            assertTrue(renderer.render(blockBatch, block));
            assertEquals("BlockImpactLine never calls setBlendFunction",
                    0, blockBatch.setBlendCalls);
            assertEquals("the block impact line uses the TextureRegion draw overload",
                    0, blockBatch.drawCalls);
            assertEquals(1, blockBatch.textureRegionDrawCalls);

            // ExhaustPileParticle resolves a private STATIC AtlasRegion and is ambient.
            CountingBatch pileBatch = newCountingBatch();
            AbstractGameEffect pile = seededExhaustPile();
            assertTrue("a static img must resolve for the exhaust pile",
                    renderer.render(pileBatch, pile));
            assertEquals("ExhaustPile never calls setBlendFunction",
                    0, pileBatch.setBlendCalls);

            // UnknownParticleEffect is ambient and draws its own instance Texture over the new
            // 128-rect, consuming its rotation field.
            CountingBatch unknownBatch = newCountingBatch();
            AbstractGameEffect unknown = seededUnknown(new Color(0.5f, 0.6f, 0.7f, 0.8f), 37f);
            assertTrue(renderer.render(unknownBatch, unknown));
            assertEquals("UnknownParticle never calls setBlendFunction",
                    0, unknownBatch.setBlendCalls);
            assertEquals("the unknown particle uses the raw-texture draw overload",
                    1, unknownBatch.drawCalls);
            assertEquals("the UNKNOWN rect is 128x128",
                    128f, floatAt(unknownBatch, 4), EPS);
            assertEquals(128f, floatAt(unknownBatch, 5), EPS);
            assertEquals("UNKNOWN consumes the rotation field",
                    37f, floatAt(unknownBatch, 8), EPS);
            assertEquals(0.5f, unknownBatch.firstSetColor.r, EPS);

            // DarkOrbPassiveEffect is additive and draws its own instance Texture over the new
            // 74x74 fixed rect, consuming its rotation field.
            CountingBatch darkOrbBatch = newCountingBatch();
            AbstractGameEffect darkOrb = seededDarkOrb(new Color(0.3f, 0.5f, 0.7f, 0.9f), 41f);
            assertTrue(renderer.render(darkOrbBatch, darkOrb));
            assertEquals("DarkOrbPassive is additive", 2, darkOrbBatch.setBlendCalls);
            assertEquals("the dark orb uses the raw-texture draw overload",
                    1, darkOrbBatch.drawCalls);
            assertSame("DarkOrb draws its own instance Texture img",
                    readField(darkOrb, "img"), darkOrbBatch.drawnTexture);
            assertEquals(37f, floatAt(darkOrbBatch, 2), EPS);
            assertEquals(37f, floatAt(darkOrbBatch, 3), EPS);
            assertEquals("the DARK_ORB rect is 74x74", 74f, floatAt(darkOrbBatch, 4), EPS);
            assertEquals(74f, floatAt(darkOrbBatch, 5), EPS);
            assertEquals("DARK_ORB consumes the rotation field",
                    41f, floatAt(darkOrbBatch, 8), EPS);
            assertEquals("the src rect is 0,0,74,74 (the full region)",
                    0f, floatAt(darkOrbBatch, 9), EPS);
            assertEquals(0f, floatAt(darkOrbBatch, 10), EPS);
            assertEquals(74f, floatAt(darkOrbBatch, 11), EPS);
            assertEquals(74f, floatAt(darkOrbBatch, 12), EPS);
            assertEquals(0.3f, darkOrbBatch.firstSetColor.r, EPS);
        } finally {
            setStaticField(ImageMaster.class, "WEB_VFX", previousWebVfx);
        }
    }

    @Test
    public void lightningOrbPassiveDrawsItsInstanceTextureWithBothFlipFlagsAdditively() {
        // LOP is additive, draws its OWN instance Texture img over the 122x122 fixed rect with the
        // rotation field, and passes the effect's own flipX/flipY booleans to the raw-texture
        // overload (the first per-instance flip kind).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        Color color = new Color(0.4f, 0.5f, 0.6f, 0.7f);
        AbstractGameEffect lop = seededLightningOrbPassive(color, 12.5f, -3.25f, 1.25f, 137f,
                true, true);

        assertTrue(renderer.render(batch, lop));

        assertEquals("the additive LOP draw installs and restores blend", 2, batch.setBlendCalls);
        assertEquals("the kind must emit exactly one draw", 1, batch.drawCalls);
        assertSame("LOP must draw its own instance Texture img",
                readField(lop, "img"), batch.drawnTexture);

        // draw(texture, x-61, y-61, 61, 61, 122, 122, scale, scale, rotation, 0,0,122,122, flipX,
        //      flipY)
        assertEquals(12.5f - 61f, floatAt(batch, 0), EPS);
        assertEquals(-3.25f - 61f, floatAt(batch, 1), EPS);
        assertEquals(61f, floatAt(batch, 2), EPS);
        assertEquals(61f, floatAt(batch, 3), EPS);
        assertEquals(122f, floatAt(batch, 4), EPS);
        assertEquals(122f, floatAt(batch, 5), EPS);
        assertEquals(1.25f, floatAt(batch, 6), EPS);
        assertEquals(1.25f, floatAt(batch, 7), EPS);
        assertEquals("LOP consumes the rotation field", 137f, floatAt(batch, 8), EPS);
        assertEquals(0f, floatAt(batch, 9), EPS);
        assertEquals(0f, floatAt(batch, 10), EPS);
        assertEquals(122f, floatAt(batch, 11), EPS);
        assertEquals(122f, floatAt(batch, 12), EPS);
        assertTrue("the draw's flip flags must have been captured", batch.drawnFlipsCaptured);
        assertTrue("LOP's flipX must reach the draw", batch.drawnFlipX);
        assertTrue("LOP's flipY must reach the draw", batch.drawnFlipY);
        assertEquals(0.4f, batch.firstSetColor.r, EPS);
    }

    @Test
    public void lightningOrbPassiveHonoursTheFlipFlagsPerInstance() {
        // The flip booleans are per-instance: the same kind reads (false, true) from a different
        // effect whose own fields hold that combination.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        AbstractGameEffect lop = seededLightningOrbPassive(Color.WHITE, 1f, 2f, 1f, 0f,
                false, true);

        assertTrue(renderer.render(batch, lop));
        assertTrue(batch.drawnFlipsCaptured);
        assertFalse("LOP's flipX must follow the effect field", batch.drawnFlipX);
        assertTrue("LOP's flipY must follow the effect field", batch.drawnFlipY);
    }

    @Test
    public void glowyFireEyesDrawsItsInstanceTextureWithOnlyTheHorizontalFlipAndNoRotation() {
        // GFE is additive, draws its OWN instance Texture img over the 128x128 fixed rect with a
        // hardcoded zero rotation (it has NO rotation field), and passes only its own flippedX
        // horizontal flip (the vertical flip is always false).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        Color color = new Color(0.2f, 0.3f, 0.4f, 0.5f);
        AbstractGameEffect gfe = seededGlowyFireEyes(color, 12.5f, -3.25f, 1.25f, true);

        assertTrue(renderer.render(batch, gfe));

        assertEquals("the additive GFE draw installs and restores blend", 2, batch.setBlendCalls);
        assertEquals("the kind must emit exactly one draw", 1, batch.drawCalls);
        assertSame("GFE must draw its own instance Texture img",
                readField(gfe, "img"), batch.drawnTexture);
        assertEquals(12.5f - 64f, floatAt(batch, 0), EPS);
        assertEquals(-3.25f - 64f, floatAt(batch, 1), EPS);
        assertEquals(64f, floatAt(batch, 2), EPS);
        assertEquals(64f, floatAt(batch, 3), EPS);
        assertEquals(128f, floatAt(batch, 4), EPS);
        assertEquals(128f, floatAt(batch, 5), EPS);
        assertEquals(1.25f, floatAt(batch, 6), EPS);
        assertEquals(1.25f, floatAt(batch, 7), EPS);
        assertEquals("GFE hardcodes rotation 0f", 0f, floatAt(batch, 8), EPS);
        assertEquals(0f, floatAt(batch, 9), EPS);
        assertEquals(0f, floatAt(batch, 10), EPS);
        assertEquals(128f, floatAt(batch, 11), EPS);
        assertEquals(128f, floatAt(batch, 12), EPS);
        assertTrue(batch.drawnFlipsCaptured);
        assertTrue("GFE's flippedX must reach the draw", batch.drawnFlipX);
        assertFalse("GFE's vertical flip is always false", batch.drawnFlipY);
        assertEquals(0.2f, batch.firstSetColor.r, EPS);
    }

    @Test
    public void readTextureFieldsResolvesTheTwoFlipKindsAndDefaultsMissingFlipsToFalse() {
        // LOP: instance Texture img + the required rotation + both flip booleans resolve.
        LightningOrbPassiveHolder lop = new LightningOrbPassiveHolder();
        lop.x = 3.5f;
        lop.y = -1.25f;
        lop.scale = 0.75f;
        lop.rotation = 22f;
        lop.color = Color.WHITE;
        lop.img = noGlTexture(122, 122);
        lop.flipX = true;
        lop.flipY = false;

        Sts1VfxArtRenderer.TextureFields lopFields = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE, lop);
        assertNotNull(lopFields);
        assertSame(lop.img, lopFields.img);
        assertEquals(22f, lopFields.rotation, EPS);
        assertTrue(lopFields.flipX);
        assertFalse(lopFields.flipY);

        // LOP without the (optional) flip fields still resolves, with both flips false.
        LightningOrbPassiveNoFlipHolder noFlip = new LightningOrbPassiveNoFlipHolder();
        noFlip.x = 1f;
        noFlip.y = 2f;
        noFlip.scale = 1f;
        noFlip.rotation = 0f;
        noFlip.color = Color.WHITE;
        noFlip.img = noGlTexture(122, 122);
        Sts1VfxArtRenderer.TextureFields noFlipFields = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE, noFlip);
        assertNotNull("a missing flip flag must not fail the snapshot", noFlipFields);
        assertFalse(noFlipFields.flipX);
        assertFalse(noFlipFields.flipY);

        // LOP requires the rotation field: a holder with img but no rotation must fail open.
        LightningOrbPassiveNoRotationHolder noRotation =
                new LightningOrbPassiveNoRotationHolder();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = noGlTexture(122, 122);
        noRotation.flipX = true;
        noRotation.flipY = true;
        assertNull("an LOP holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readTextureFields(
                        VfxDrawGeometry.Kind.LIGHTNING_ORB_PASSIVE, noRotation));

        // GFE: instance Texture img + flippedX resolve WITHOUT a rotation field (it has none).
        GlowyFireEyesHolder gfe = new GlowyFireEyesHolder();
        gfe.x = 3.5f;
        gfe.y = -1.25f;
        gfe.scale = 0.75f;
        gfe.color = Color.WHITE;
        gfe.img = noGlTexture(128, 128);
        gfe.flippedX = true;
        Sts1VfxArtRenderer.TextureFields gfeFields = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES, gfe);
        assertNotNull("GFE must resolve without a rotation field", gfeFields);
        assertSame(gfe.img, gfeFields.img);
        assertEquals(0f, gfeFields.rotation, EPS);
        assertTrue(gfeFields.flipX);
        assertFalse("GFE's vertical flip is always false", gfeFields.flipY);

        // GFE with the optional flippedX absent still resolves, defaulting both flips to false.
        GlowyFireEyesNoFlipHolder gfeNoFlip = new GlowyFireEyesNoFlipHolder();
        gfeNoFlip.x = 1f;
        gfeNoFlip.y = 2f;
        gfeNoFlip.scale = 1f;
        gfeNoFlip.color = Color.WHITE;
        gfeNoFlip.img = noGlTexture(128, 128);
        Sts1VfxArtRenderer.TextureFields gfeNoFlipFields = Sts1VfxArtRenderer.readTextureFields(
                VfxDrawGeometry.Kind.GLOWY_FIRE_EYES, gfeNoFlip);
        assertNotNull(gfeNoFlipFields);
        assertFalse(gfeNoFlipFields.flipX);
        assertFalse(gfeNoFlipFields.flipY);
    }

    @Test
    public void preexistingShapeCKindStillDrawsWithFalseFalseFlips() {
        // Regression: the per-instance flips are false for every pre-existing shape-C kind, so its
        // draw is unchanged (src rect passthrough with false, false).
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        AbstractGameEffect ice = seededIceEffect(Color.WHITE, 12.5f, -3.25f, 1.25f, 137f);

        assertTrue(renderer.render(batch, ice));
        assertTrue(batch.drawnFlipsCaptured);
        assertFalse("a pre-existing shape-C kind must draw with flipX false", batch.drawnFlipX);
        assertFalse("a pre-existing shape-C kind must draw with flipY false", batch.drawnFlipY);
    }

    /** Real {@code LightningOrbPassiveEffect} with seeded draw fields and the two flip booleans. */
    private static AbstractGameEffect seededLightningOrbPassive(Color color, float x, float y,
            float scale, float rotation, boolean flipX, boolean flipY) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect effect =
                    (com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class, "y",
                    Float.valueOf(y));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class,
                    "img", noGlTexture(122, 122));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class,
                    "flipX", Boolean.valueOf(flipX));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect.class,
                    "flipY", Boolean.valueOf(flipY));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(scale));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL LightningOrbPassiveEffect", failure);
        }
    }

    /** Real {@code GlowyFireEyesEffect} with seeded draw fields and its {@code flippedX} boolean. */
    private static AbstractGameEffect seededGlowyFireEyes(Color color, float x, float y,
            float scale, boolean flippedX) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect effect =
                    (com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect.class, "y",
                    Float.valueOf(y));
            setField(effect, com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect.class, "img",
                    noGlTexture(128, 128));
            setField(effect, com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect.class, "flippedX",
                    Boolean.valueOf(flippedX));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(scale));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL GlowyFireEyesEffect", failure);
        }
    }

    private static float floatAt(CountingBatch batch, int index) {
        return batch.drawnArgs[index];
    }

    private static Object readField(Object target, String name) {
        try {
            return findAndGetField(target, name);
        } catch (Exception failure) {
            throw new AssertionError("could not read field " + name, failure);
        }
    }

    private static Object findAndGetField(Object target, String name) throws Exception {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    @Test
    public void resolveColorForcesWhiteRgbOnlyForWebAndPassesOthersThrough() {
        Color effect = new Color(0.2f, 0.4f, 0.6f, 0.35f);

        Color web = Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.WEB_PARTICLE, effect);
        assertEquals(1f, web.r, EPS);
        assertEquals(1f, web.g, EPS);
        assertEquals(1f, web.b, EPS);
        assertEquals(0.35f, web.a, EPS);

        // Ice shows the pass-through: the same color object the effect owns.
        assertSame(effect, Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.ICE_SHATTER, effect));
        assertSame(effect, Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.CALM_PARTICLE, effect));
        assertSame(effect, Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.DEBUFF_PARTICLE, effect));

        // Null collapses to the white default for every kind.
        assertSame(Color.WHITE, Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.WEB_PARTICLE, null));
        assertSame(Color.WHITE, Sts1VfxArtRenderer.resolveColor(
                VfxDrawGeometry.Kind.ICE_SHATTER, null));
    }

    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception failure) {
            throw new AssertionError("could not set static field " + owner + "." + name, failure);
        }
    }

    // --- no-GL draws (mirrors BackgroundRenderPatchesTest/Sts1GdxAtlasRegionsTest conventions) ---

    /** SpriteBatch double that counts blend-function calls without reaching the GL flush path. */
    static class CountingBatch extends SpriteBatch {
        int setBlendCalls;
        int drawCalls;
        /** When true the packed-region draw overload throws (post-RNG fail-open test). */
        boolean throwOnRegionDraw;
        /** Count of {@code draw(TextureRegion, ...)} (the packed-region shape) calls. */
        int textureRegionDrawCalls;
        /** The first {@link TextureRegion} passed to the packed-region draw overload. */
        TextureRegion drawnRegion;
        /** The first packed-region draw's arguments. */
        float[] drawnRegionArgs;
        /** First {@link Color} passed to {@link #setColor(Color)} (the applied draw tint). */
        Color firstSetColor;
        /** The first raw-texture + source-rect draw's arguments (the ICE/WEB/Calm/Shield shape). */
        Texture drawnTexture;
        float[] drawnArgs;
        /** Whether the first raw-texture draw's flip flags were captured. */
        boolean drawnFlipsCaptured;
        boolean drawnFlipX;
        boolean drawnFlipY;

        CountingBatch() {
            // Never invoked: instances are created with Unsafe.allocateInstance so no GL/asset state
            // is needed; the constructor exists only so the subclass compiles against SpriteBatch.
            super(1);
        }

        @Override
        public void setBlendFunction(int srcFunc, int dstFunc) {
            // Deliberately skip super: this test only asserts whether the call happened, and the
            // real method would flush (binding a GL texture) on the no-GL double.
            setBlendCalls++;
        }

        @Override
        public void setColor(Color color) {
            // Record the applied tint (the render restores the previous color afterwards, so the
            // post-render getColor() no longer shows it). The packed-float write itself is safe on
            // the no-GL double.
            if (firstSetColor == null && color != null) {
                firstSetColor = new Color(color);
            }
            super.setColor(color);
        }

        @Override
        public void draw(TextureRegion region, float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation) {
            // Record the call only; skipping super avoids the real (absent) GL texture bind path.
            if (throwOnRegionDraw) {
                throw new IllegalStateException("post-RNG draw boom");
            }
            if (textureRegionDrawCalls++ == 0) {
                drawnRegion = region;
                drawnRegionArgs = new float[] {x, y, originX, originY, width, height,
                        scaleX, scaleY, rotation};
            }
        }

        @Override
        public void draw(Texture texture, float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
            // Record the draw arguments (the render restores state afterwards). Skipping super is
            // deliberate: the real 16-arg overload would touch the (absent) GL texture bind path.
            if (drawCalls++ == 0) {
                drawnTexture = texture;
                drawnArgs = new float[] {x, y, originX, originY, width, height,
                        scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight};
                drawnFlipsCaptured = true;
                drawnFlipX = flipX;
                drawnFlipY = flipY;
            }
        }
    }

    private static CountingBatch newCountingBatch() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            CountingBatch sb = (CountingBatch) unsafe.allocateInstance(CountingBatch.class);
            setField(sb, SpriteBatch.class, "vertices", new float[20 * 4096]);
            setField(sb, SpriteBatch.class, "idx", Integer.valueOf(0));
            setField(sb, SpriteBatch.class, "drawing", Boolean.TRUE);
            setField(sb, SpriteBatch.class, "lastTexture", noGlTexture(256, 256));
            setField(sb, SpriteBatch.class, "color", Float.valueOf(Color.WHITE.toFloatBits()));
            setField(sb, SpriteBatch.class, "tempColor", new Color(1f, 1f, 1f, 1f));
            return sb;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL CountingBatch", failure);
        }
    }

    private static void setField(Object target, Class<?> owner, String name, Object value)
            throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Texture noGlTexture(final int width, final int height) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            Texture texture = (Texture) unsafe.allocateInstance(Texture.class);
            setField(texture, Texture.class, "data", new TextureData() {
                public TextureDataType getType() { return TextureDataType.Pixmap; }
                public boolean isPrepared() { return true; }
                public void prepare() {}
                public Pixmap consumePixmap() { return null; }
                public boolean disposePixmap() { return false; }
                public void consumeCustomData(int target) {}
                public int getWidth() { return width; }
                public int getHeight() { return height; }
                public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
                public boolean useMipMaps() { return false; }
                public boolean isManaged() { return false; }
            });
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL texture double", failure);
        }
    }

    /** Real {@code FlashAtkImgEffect} with reflectively seeded draw fields (no game/GL context). */
    private static AbstractGameEffect flashEffect() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            FlashAtkImgEffect effect =
                    (FlashAtkImgEffect) unsafe.allocateInstance(FlashAtkImgEffect.class);
            effect.img = new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48);
            setField(effect, FlashAtkImgEffect.class, "x", Float.valueOf(5f));
            setField(effect, FlashAtkImgEffect.class, "y", Float.valueOf(6f));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL FlashAtkImgEffect", failure);
        }
    }

    /**
     * Real effect of {@code type} with reflectively seeded {@code x}/{@code y}/{@code img} fields
     * (no game/GL context); the inherited {@code scale}/{@code rotation}/{@code color} come from
     * {@code AbstractGameEffect}.
     */
    private static AbstractGameEffect seededEffect(Class<? extends AbstractGameEffect> type) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            AbstractGameEffect effect = (AbstractGameEffect) unsafe.allocateInstance(type);
            setField(effect, type, "x", Float.valueOf(5f));
            setField(effect, type, "y", Float.valueOf(6f));
            setField(effect, type, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL " + type.getSimpleName(), failure);
        }
    }

    /**
     * Real {@code WebParticleEffect} with reflectively seeded {@code x}/{@code y} fields (no
     * {@code img} field, per the native layout) and the given color; the inherited
     * {@code scale}/{@code rotation} come from {@code AbstractGameEffect}.
     */
    private static AbstractGameEffect seededWebEffect(Color color) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            WebParticleEffect effect =
                    (WebParticleEffect) unsafe.allocateInstance(WebParticleEffect.class);
            setField(effect, WebParticleEffect.class, "x", Float.valueOf(5f));
            setField(effect, WebParticleEffect.class, "y", Float.valueOf(6f));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL WebParticleEffect", failure);
        }
    }

    /**
     * Real {@code EntangleEffect} with reflectively seeded {@code x}/{@code y} fields (no
     * {@code img} field, per the native layout) and the given color; the inherited
     * {@code scale}/{@code rotation} come from {@code AbstractGameEffect}.
     */
    private static AbstractGameEffect seededEntangle(Color color) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            EntangleEffect effect =
                    (EntangleEffect) unsafe.allocateInstance(EntangleEffect.class);
            setField(effect, EntangleEffect.class, "x", Float.valueOf(5f));
            setField(effect, EntangleEffect.class, "y", Float.valueOf(6f));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL EntangleEffect", failure);
        }
    }

    /**
     * Real {@code BlockImpactLineEffect} with reflectively seeded {@code x}/{@code y}/{@code img}
     * fields (its own instance {@code AtlasRegion}) and the inherited
     * {@code scale}/{@code rotation}/{@code color} (no game/GL context).
     */
    private static AbstractGameEffect seededBlockImpactLine() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            BlockImpactLineEffect effect =
                    (BlockImpactLineEffect) unsafe.allocateInstance(BlockImpactLineEffect.class);
            setField(effect, BlockImpactLineEffect.class, "x", Float.valueOf(5f));
            setField(effect, BlockImpactLineEffect.class, "y", Float.valueOf(6f));
            setField(effect, BlockImpactLineEffect.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL BlockImpactLineEffect", failure);
        }
    }

    /**
     * Real {@code ExhaustPileParticle} with reflectively seeded {@code x}/{@code y}/{@code scale}
     * and its {@code private static AtlasRegion img} field (no game/GL context). The static field is
     * set on the class, then read back by the renderer's superclass-walking reader.
     */
    private static AbstractGameEffect seededExhaustPile() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            setStaticField(ExhaustPileParticle.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            ExhaustPileParticle effect =
                    (ExhaustPileParticle) unsafe.allocateInstance(ExhaustPileParticle.class);
            setField(effect, ExhaustPileParticle.class, "x", Float.valueOf(5f));
            setField(effect, ExhaustPileParticle.class, "y", Float.valueOf(6f));
            setField(effect, ExhaustPileParticle.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL ExhaustPileParticle", failure);
        }
    }

    /**
     * Real {@code UnknownParticleEffect} with reflectively seeded {@code x}/{@code y}/{@code scale},
     * its own instance {@code Texture img}, the consumed inherited {@code rotation}, and the given
     * color (no game/GL context).
     */
    private static AbstractGameEffect seededUnknown(Color color, float rotation) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            UnknownParticleEffect effect =
                    (UnknownParticleEffect) unsafe.allocateInstance(UnknownParticleEffect.class);
            setField(effect, UnknownParticleEffect.class, "x", Float.valueOf(5f));
            setField(effect, UnknownParticleEffect.class, "y", Float.valueOf(6f));
            setField(effect, UnknownParticleEffect.class, "scale", Float.valueOf(1f));
            setField(effect, UnknownParticleEffect.class, "img", noGlTexture(128, 128));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL UnknownParticleEffect", failure);
        }
    }

    /**
     * Real {@code DarkOrbPassiveEffect} with reflectively seeded {@code x}/{@code y}/{@code scale},
     * its own instance {@code Texture img}, the consumed inherited {@code rotation}, and the given
     * color (no game/GL context).
     */
    private static AbstractGameEffect seededDarkOrb(Color color, float rotation) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            DarkOrbPassiveEffect effect =
                    (DarkOrbPassiveEffect) unsafe.allocateInstance(DarkOrbPassiveEffect.class);
            setField(effect, DarkOrbPassiveEffect.class, "x", Float.valueOf(5f));
            setField(effect, DarkOrbPassiveEffect.class, "y", Float.valueOf(6f));
            setField(effect, DarkOrbPassiveEffect.class, "img", noGlTexture(74, 74));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL DarkOrbPassiveEffect", failure);
        }
    }

    /**
     * Real {@code IceShatterEffect} with reflectively seeded {@code x}/{@code y}/{@code img}
     * fields, its inherited {@code scale}/{@code rotation}, and the given color (no game/GL
     * context).
     */
    private static AbstractGameEffect seededIceEffect(Color color, float x, float y, float scale,
            float rotation) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.combat.IceShatterEffect effect =
                    (com.megacrit.cardcrawl.vfx.combat.IceShatterEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "x",
                    Float.valueOf(x));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "y",
                    Float.valueOf(y));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "img",
                    noGlTexture(64, 64));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(scale));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(rotation));
            setField(effect, AbstractGameEffect.class, "color", color);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL IceShatterEffect", failure);
        }
    }

    /** Real {@code StanceAuraEffect} with reflectively seeded draw fields (no game/GL context). */
    private static AbstractGameEffect auraEffect() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            StanceAuraEffect effect =
                    (StanceAuraEffect) unsafe.allocateInstance(StanceAuraEffect.class);
            setField(effect, StanceAuraEffect.class, "x", Float.valueOf(5f));
            setField(effect, StanceAuraEffect.class, "y", Float.valueOf(6f));
            setField(effect, StanceAuraEffect.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL StanceAuraEffect", failure);
        }
    }

    @Test
    public void canDrawDistinguishesADrawableInstanceFromABlankOne() {
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();

        // FlashAtkImgEffect with no img: nothing to draw (its native render guards if (img != null)).
        assertFalse("a blank flash instance has nothing to draw",
                renderer.canDraw(blankFlashEffect()));

        // The same real effect with a present region + texture can draw.
        assertTrue("a seeded flash instance can draw",
                renderer.canDraw(flashEffect()));

        // A center-packed kind with an absent img cannot draw ...
        assertFalse("a center-packed effect without img cannot draw",
                renderer.canDraw(seededEffectWithNullImg(FireBurstParticleEffect.class)));

        // ... and a shape-C kind whose instance img is absent cannot draw either.
        assertFalse("a shape-C effect without its instance Texture cannot draw",
                renderer.canDraw(seededIceWithoutImg()));

        // A wrong/absent effect type, and null, fail closed.
        assertFalse(renderer.canDraw(null));
        assertFalse(renderer.canDraw("not an effect"));
    }

    @Test
    public void canDrawDelegatorReturnsFalseWithoutAnAdapter() {
        VfxArtRenderer.uninstall();
        assertFalse("no installed adapter means canDraw is false",
                VfxArtRenderer.canDraw(flashEffect()));
    }

    @Test
    public void flipPollutedSharedRegionStillDrawsTheCanonicalRect() {
        // Regression for F15d: a native sibling flips the SHARED static AtlasRegion in place, which
        // swaps u/u2 (and mutates offsetX). The claim suppresses the native draw, so ART must still
        // draw the canonical rect (not decline) instead of passing the flipped region through, which
        // used to make the neutral descriptor invalid -> fail open -> dispositionMismatch.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Texture page = noGlTexture(256, 256);
        TextureAtlas.AtlasRegion shared = new TextureAtlas.AtlasRegion(page, 10, 20, 64, 48);
        shared.name = "shared";
        shared.originalWidth = 64;
        shared.originalHeight = 48;
        // Simulate the native sibling pollution on this shared region instance.
        shared.flip(true, false);

        AbstractGameEffect flash = seededFlashEffect(shared);
        CountingBatch batch = newCountingBatch();

        assertTrue("a flip-polluted shared region must still draw (no decline)",
                renderer.render(batch, flash));
        assertEquals("the img path uses the TextureRegion draw overload",
                1, batch.textureRegionDrawCalls);
        assertEquals(0, batch.drawCalls);
        assertNotNull("the drawn region must have been captured", batch.drawnRegion);

        // The drawn view is canonicalized: u < u2 and v < v2, with the SAME packed footprint as the
        // un-flipped region (UV width == 64/256, height == 48/256), independent of the pollution.
        TextureRegion drawn = batch.drawnRegion;
        assertTrue("canonical u must be the left edge", drawn.getU() < drawn.getU2());
        assertTrue("canonical v must be the top edge", drawn.getV() < drawn.getV2());
        assertEquals(10f / 256f, drawn.getU(), 1e-5f);
        assertEquals(74f / 256f, drawn.getU2(), 1e-5f);
        assertEquals(20f / 256f, drawn.getV(), 1e-5f);
        assertEquals(68f / 256f, drawn.getV2(), 1e-5f);
        // The shared region itself is left untouched (the renderer never mutates it).
        assertTrue("the renderer must not mutate the shared region back",
                shared.getU() > shared.getU2());
    }

    /**
     * Real {@code SpookyChestEffect} (flipX + flipY), {@code IroncladVictoryFlameEffect} (flipX), or
     * {@code FlameParticleEffect} (flipX) with reflectively seeded draw fields and the per-instance
     * mirror booleans (no GL).
     */
    private static AbstractGameEffect seededMirrorEffect(
            Class<? extends AbstractGameEffect> type, TextureAtlas.AtlasRegion region,
            boolean flipX, boolean flipY) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            AbstractGameEffect effect = (AbstractGameEffect) unsafe.allocateInstance(type);
            setField(effect, type, "x", Float.valueOf(5f));
            setField(effect, type, "y", Float.valueOf(6f));
            setField(effect, type, "img", region);
            setField(effect, type, "flipX", Boolean.valueOf(flipX));
            try {
                setField(effect, type, "flipY", Boolean.valueOf(flipY));
            } catch (NoSuchFieldException noFlipY) {
                // IroncladVictoryFlameEffect / FlameParticleEffect declare only flipX.
            }
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL " + type.getSimpleName(), failure);
        }
    }

    @Test
    public void spookyChestMirrorsTheCanonicalRegionWhenFlipFlagsAreSet() {
        // NRO-04 F22: the native render flips the shared region in place around its draw; the claim
        // suppresses that draw, so ART reproduces the mirror as a UV swap on the canonical F15d view.
        // SpookyChestEffect declares BOTH flipX and flipY, so a set flag swaps u/u2 and/or v/v2.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Class<? extends AbstractGameEffect> type =
                com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect.class;

        // No mirror: the canonical rect (u < u2, v < v2).
        CountingBatch plainBatch = newCountingBatch();
        assertTrue(renderer.render(plainBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                false, false)));
        assertEquals("the img path uses the TextureRegion draw overload",
                1, plainBatch.textureRegionDrawCalls);
        TextureRegion plain = plainBatch.drawnRegion;
        assertNotNull(plain);
        assertEquals(10f / 256f, plain.getU(), 1e-5f);
        assertEquals(74f / 256f, plain.getU2(), 1e-5f);
        assertEquals(20f / 256f, plain.getV(), 1e-5f);
        assertEquals(68f / 256f, plain.getV2(), 1e-5f);

        // Mirror X only: u/u2 swapped relative to the canonical rect, v/v2 unchanged.
        CountingBatch mirrorXBatch = newCountingBatch();
        assertTrue(renderer.render(mirrorXBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                true, false)));
        TextureRegion mirrorX = mirrorXBatch.drawnRegion;
        assertNotNull(mirrorX);
        assertEquals("the mirrored view swaps u/u2", 74f / 256f, mirrorX.getU(), 1e-5f);
        assertEquals(10f / 256f, mirrorX.getU2(), 1e-5f);
        assertEquals("the vertical extent stays canonical", 20f / 256f, mirrorX.getV(), 1e-5f);
        assertEquals(68f / 256f, mirrorX.getV2(), 1e-5f);

        // Mirror Y only: v/v2 swapped, u/u2 canonical.
        CountingBatch mirrorYBatch = newCountingBatch();
        assertTrue(renderer.render(mirrorYBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                false, true)));
        TextureRegion mirrorY = mirrorYBatch.drawnRegion;
        assertNotNull(mirrorY);
        assertEquals(10f / 256f, mirrorY.getU(), 1e-5f);
        assertEquals(74f / 256f, mirrorY.getU2(), 1e-5f);
        assertEquals("the mirrored view swaps v/v2", 68f / 256f, mirrorY.getV(), 1e-5f);
        assertEquals(20f / 256f, mirrorY.getV2(), 1e-5f);

        // Mirror X and Y: both axes swapped.
        CountingBatch bothBatch = newCountingBatch();
        assertTrue(renderer.render(bothBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                true, true)));
        TextureRegion both = bothBatch.drawnRegion;
        assertNotNull(both);
        assertEquals(74f / 256f, both.getU(), 1e-5f);
        assertEquals(10f / 256f, both.getU2(), 1e-5f);
        assertEquals(68f / 256f, both.getV(), 1e-5f);
        assertEquals(20f / 256f, both.getV2(), 1e-5f);
        assertEquals("SpookyChest is ambient", 0, bothBatch.setBlendCalls);
    }

    @Test
    public void ironcladVictoryFlameMirrorsOnlyHorizontally() {
        // IroncladVictoryFlameEffect declares only flipX (no flipY field), so only the horizontal
        // mirror is honoured; the vertical extent stays canonical.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        assertTrue(renderer.render(batch, seededMirrorEffect(
                com.megacrit.cardcrawl.vfx.scene.IroncladVictoryFlameEffect.class,
                new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                true, false)));
        TextureRegion drawn = batch.drawnRegion;
        assertNotNull(drawn);
        assertEquals("the mirrored view swaps u/u2", 74f / 256f, drawn.getU(), 1e-5f);
        assertEquals(10f / 256f, drawn.getU2(), 1e-5f);
        assertEquals("the vertical extent stays canonical", 20f / 256f, drawn.getV(), 1e-5f);
        assertEquals(68f / 256f, drawn.getV2(), 1e-5f);
        assertEquals("IroncladVictoryFlame is ambient", 0, batch.setBlendCalls);
    }

    @Test
    public void spookierChestAndCampfireSleepCoverMirrorTheCanonicalRegionWhenFlipFlagsAreSet() {
        // NRO-04 F23: both are the same shape as SpookyChestEffect — ambient center-packed with
        // per-instance flipX+flipY mirror booleans; the claim reproduces the mirror as a UV swap.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Class<? extends AbstractGameEffect>[] types = new Class[] {
                com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect.class,
                com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect.class };
        for (Class<? extends AbstractGameEffect> type : types) {
            CountingBatch plainBatch = newCountingBatch();
            assertTrue(renderer.render(plainBatch, seededMirrorEffect(
                    type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                    false, false)));
            TextureRegion plain = plainBatch.drawnRegion;
            assertNotNull(plain);
            assertTrue("flipX false draws the canonical rect for " + type.getSimpleName(),
                    plain.getU() < plain.getU2());
            assertTrue("flipY false keeps v canonical for " + type.getSimpleName(),
                    plain.getV() < plain.getV2());

            CountingBatch bothBatch = newCountingBatch();
            assertTrue(renderer.render(bothBatch, seededMirrorEffect(
                    type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                    true, true)));
            TextureRegion both = bothBatch.drawnRegion;
            assertNotNull(both);
            assertEquals("the mirrored view swaps u/u2 for " + type.getSimpleName(),
                    74f / 256f, both.getU(), 1e-5f);
            assertEquals(10f / 256f, both.getU2(), 1e-5f);
            assertEquals("the mirrored view swaps v/v2 for " + type.getSimpleName(),
                    68f / 256f, both.getV(), 1e-5f);
            assertEquals(20f / 256f, both.getV2(), 1e-5f);
            assertEquals(type.getSimpleName() + " is ambient", 0, bothBatch.setBlendCalls);
        }
    }

    @Test
    public void deathScreenFloatyResolvesWithNoMirrorFieldsAndDrawsTheCanonicalRegion() {
        // Danger: DeathScreenFloatyEffect declares NO flip fields, so it must never be treated as a
        // mirror kind — the canonical region is drawn unchanged and it stays ambient.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        assertTrue(renderer.render(batch, seededDeathFloatyEffect(
                new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48))));
        assertEquals("the img path uses the TextureRegion draw overload",
                1, batch.textureRegionDrawCalls);
        TextureRegion drawn = batch.drawnRegion;
        assertNotNull(drawn);
        assertEquals(10f / 256f, drawn.getU(), 1e-5f);
        assertEquals(74f / 256f, drawn.getU2(), 1e-5f);
        assertEquals(20f / 256f, drawn.getV(), 1e-5f);
        assertEquals(68f / 256f, drawn.getV2(), 1e-5f);
        assertTrue("an unmirrored draw keeps u < u2", drawn.getU() < drawn.getU2());
        assertTrue("an unmirrored draw keeps v < v2", drawn.getV() < drawn.getV2());
        assertEquals("DeathScreenFloaty is ambient", 0, batch.setBlendCalls);
    }

    @Test
    public void f23KindsFailOpenOnMissingImgOrRotation() {
        // These three img kinds consume the rotation field and require the img region, so a holder
        // with a valid img but no rotation field, or no img at all, fails open.
        NoRotationImgEffect noRotation = new NoRotationImgEffect();
        noRotation.x = 1f;
        noRotation.y = 2f;
        noRotation.scale = 1f;
        noRotation.color = Color.WHITE;
        noRotation.img = new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 0, 0, 64, 48);
        assertNull("a SPOOKIER_CHEST holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SPOOKIER_CHEST, noRotation));
        assertNull("a CAMPFIRE_SLEEP_COVER holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readFields(
                        VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER, noRotation));
        assertNull("a DEATH_SCREEN_FLOATY holder with img but no rotation field must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY, noRotation));

        NoImgEffect noImg = new NoImgEffect();
        noImg.x = 1f;
        noImg.y = 2f;
        noImg.scale = 1f;
        noImg.rotation = 0f;
        noImg.color = Color.WHITE;
        assertNull("a SPOOKIER_CHEST holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.SPOOKIER_CHEST, noImg));
        assertNull("a CAMPFIRE_SLEEP_COVER holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.CAMPFIRE_SLEEP_COVER, noImg));
        assertNull("a DEATH_SCREEN_FLOATY holder without an img must fail open",
                Sts1VfxArtRenderer.readFields(VfxDrawGeometry.Kind.DEATH_SCREEN_FLOATY, noImg));
    }

    /**
     * Real {@code DeathScreenFloatyEffect} (no flip fields) with reflectively seeded draw fields
     * (no GL).
     */
    private static AbstractGameEffect seededDeathFloatyEffect(TextureAtlas.AtlasRegion region) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            AbstractGameEffect effect = (AbstractGameEffect) unsafe.allocateInstance(
                    com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect.class, "x",
                    Float.valueOf(5f));
            setField(effect, com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect.class, "y",
                    Float.valueOf(6f));
            setField(effect, com.megacrit.cardcrawl.vfx.DeathScreenFloatyEffect.class, "img",
                    region);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL DeathScreenFloatyEffect", failure);
        }
    }

    @Test
    public void flameParticleNowMirrorsHorizontallyResolvingTheF15Limitation() {
        // F15 known limitation RESOLVED: FlameParticleEffect's per-instance flipX mirror is now
        // reproduced (as a UV swap on the canonical region) instead of always drawing canonical.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        Class<? extends AbstractGameEffect> type =
                com.megacrit.cardcrawl.vfx.combat.FlameParticleEffect.class;

        CountingBatch plainBatch = newCountingBatch();
        assertTrue(renderer.render(plainBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                false, false)));
        TextureRegion plain = plainBatch.drawnRegion;
        assertNotNull(plain);
        assertTrue("flipX false draws the canonical rect", plain.getU() < plain.getU2());

        CountingBatch mirroredBatch = newCountingBatch();
        assertTrue(renderer.render(mirroredBatch, seededMirrorEffect(
                type, new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48),
                true, false)));
        TextureRegion mirrored = mirroredBatch.drawnRegion;
        assertNotNull(mirrored);
        assertTrue("flipX true now draws the mirrored rect", mirrored.getU() > mirrored.getU2());
        assertEquals(74f / 256f, mirrored.getU(), 1e-5f);
        assertEquals(10f / 256f, mirrored.getU2(), 1e-5f);
        assertEquals("FlameParticle is additive", 2, mirroredBatch.setBlendCalls);
    }

    @Test
    public void missingMirrorFieldDefaultsToFalseAndStillDraws() {
        // Mirror is optional decoration: a kind whose instance lacks the mirror field (absent or
        // unreadable) still draws the canonical rect instead of failing open.
        NoMirrorImgEffect holder = new NoMirrorImgEffect();
        holder.x = 5f;
        holder.y = 6f;
        holder.scale = 1f;
        holder.rotation = 0f;
        holder.color = Color.WHITE;
        holder.img = new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48);
        Sts1VfxArtRenderer.Fields resolved = Sts1VfxArtRenderer.readFields(
                VfxDrawGeometry.Kind.SPOOKY_CHEST, holder);
        assertNotNull("a missing mirror field must not fail the snapshot", resolved);
        assertFalse(resolved.mirrorX);
        assertFalse(resolved.mirrorY);

        // render routes on the effect's own class name, so the synthetic absent-field holder cannot
        // be drawn; the end-to-end equivalent is a REAL SpookyChestEffect whose mirror fields are
        // left untouched (default false) — the same absent-flags observable path through readFields.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        assertTrue("a holder without mirror flags must still draw", renderer.render(
                batch, seededSpookyChestWithoutMirrorFields()));
        TextureRegion drawn = batch.drawnRegion;
        assertNotNull("the draw must have been captured", drawn);
        assertEquals("the canonical u is drawn", 10f / 256f, drawn.getU(), 1e-5f);
        assertEquals("the canonical u2 is drawn", 74f / 256f, drawn.getU2(), 1e-5f);
        assertEquals("the canonical v is drawn", 20f / 256f, drawn.getV(), 1e-5f);
        assertEquals("the canonical v2 is drawn", 68f / 256f, drawn.getV2(), 1e-5f);
        assertTrue("an unmirrored draw keeps u < u2", drawn.getU() < drawn.getU2());
        assertTrue("an unmirrored draw keeps v < v2", drawn.getV() < drawn.getV2());
    }

    /**
     * Real {@code SpookyChestEffect} with x/y/img/scale/rotation/color seeded but the mirror
     * {@code flipX}/{@code flipY} fields deliberately left untouched (default {@code false}), so the
     * end-to-end draw exercises the same absent-flag path as a holder without those fields.
     */
    private static AbstractGameEffect seededSpookyChestWithoutMirrorFields() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect effect =
                    (com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect.class, "x",
                    Float.valueOf(5f));
            setField(effect, com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect.class, "y",
                    Float.valueOf(6f));
            setField(effect, com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect.class, "img",
                    new TextureAtlas.AtlasRegion(noGlTexture(256, 256), 10, 20, 64, 48));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL SpookyChestEffect", failure);
        }
    }

    @Test
    public void preexistingNonMirrorKindStillDrawsTheCanonicalRegion() {
        // Regression: the mirror is false for every pre-existing img-path kind, so its canonical UV
        // rect is unchanged.
        Sts1VfxArtRenderer renderer = new Sts1VfxArtRenderer();
        CountingBatch batch = newCountingBatch();
        AbstractGameEffect heart = seededDamageHeart(-1f, 64, 48);

        assertTrue(renderer.render(batch, heart));
        TextureRegion drawn = batch.drawnRegion;
        assertNotNull(drawn);
        assertTrue("a non-mirror kind keeps u < u2", drawn.getU() < drawn.getU2());
        assertTrue("a non-mirror kind keeps v < v2", drawn.getV() < drawn.getV2());
        assertEquals(0f, drawn.getU(), 1e-5f);
        assertEquals(64f / 256f, drawn.getU2(), 1e-5f);
        assertEquals(0f, drawn.getV(), 1e-5f);
        assertEquals(48f / 256f, drawn.getV2(), 1e-5f);
    }

    /** Center-packed img holder without a mirror field (so the optional mirror defaults to false). */
    static class NoMirrorImgEffect extends BaseEffect {
        private float x;
        private float y;
        private TextureAtlas.AtlasRegion img;
    }

    /** Real {@code FlashAtkImgEffect} bound to the given region; inherited fields seeded. */
    private static AbstractGameEffect seededFlashEffect(TextureAtlas.AtlasRegion region) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            FlashAtkImgEffect effect =
                    (FlashAtkImgEffect) unsafe.allocateInstance(FlashAtkImgEffect.class);
            effect.img = region;
            setField(effect, FlashAtkImgEffect.class, "x", Float.valueOf(5f));
            setField(effect, FlashAtkImgEffect.class, "y", Float.valueOf(6f));
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build a no-GL FlashAtkImgEffect", failure);
        }
    }

    /** Real {@code FlashAtkImgEffect} with seeded draw fields but a null {@code img}. */
    private static AbstractGameEffect blankFlashEffect() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            FlashAtkImgEffect effect =
                    (FlashAtkImgEffect) unsafe.allocateInstance(FlashAtkImgEffect.class);
            setField(effect, FlashAtkImgEffect.class, "x", Float.valueOf(5f));
            setField(effect, FlashAtkImgEffect.class, "y", Float.valueOf(6f));
            setField(effect, FlashAtkImgEffect.class, "img", null);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build a blank no-GL FlashAtkImgEffect", failure);
        }
    }

    /** Real center-packed effect of {@code type} with x/y/scale/rotation/color but a null img. */
    private static AbstractGameEffect seededEffectWithNullImg(
            Class<? extends AbstractGameEffect> type) {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            AbstractGameEffect effect = (AbstractGameEffect) unsafe.allocateInstance(type);
            setField(effect, type, "x", Float.valueOf(5f));
            setField(effect, type, "y", Float.valueOf(6f));
            setField(effect, type, "img", null);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL " + type.getSimpleName(), failure);
        }
    }

    /** Real {@code IceShatterEffect} with seeded fields but no instance {@code Texture img}. */
    private static AbstractGameEffect seededIceWithoutImg() {
        try {
            java.lang.reflect.Field unsafeField =
                    sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.megacrit.cardcrawl.vfx.combat.IceShatterEffect effect =
                    (com.megacrit.cardcrawl.vfx.combat.IceShatterEffect)
                            unsafe.allocateInstance(
                                    com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class);
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "x",
                    Float.valueOf(5f));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "y",
                    Float.valueOf(6f));
            setField(effect, com.megacrit.cardcrawl.vfx.combat.IceShatterEffect.class, "img",
                    null);
            setField(effect, AbstractGameEffect.class, "scale", Float.valueOf(1f));
            setField(effect, AbstractGameEffect.class, "rotation", Float.valueOf(0f));
            setField(effect, AbstractGameEffect.class, "color", Color.WHITE);
            return effect;
        } catch (Exception failure) {
            throw new AssertionError("could not build a no-GL IceShatterEffect", failure);
        }
    }

    @Test
    public void installDelegatesReadinessAndDrawToTheInstalledAdapter() {
        try {
            VfxArtRenderer.uninstall();
            assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
            assertFalse(VfxArtRenderer.render(null, null));

            VfxArtRenderer.install(new VfxArtRenderer.Adapter() {
                @Override
                public boolean isReady(String nativeClassName) {
                    return VfxClaimPolicy.STANCE_AURA_EFFECT.equals(nativeClassName);
                }

                @Override
                public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                    return true;
                }

                @Override
                public boolean canDraw(Object effect) {
                    return true;
                }
            });

            assertTrue(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
            assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
            assertTrue(VfxArtRenderer.render(null, null));
        } finally {
            VfxArtRenderer.uninstall();
        }

        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.render(null, null));
    }

    @Test
    public void installNullRestoresTheInertDefault() {
        VfxArtRenderer.install(new VfxArtRenderer.Adapter() {
            @Override
            public boolean isReady(String nativeClassName) {
                return true;
            }

            @Override
            public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                return true;
            }

            @Override
            public boolean canDraw(Object effect) {
                return true;
            }
        });
        assertTrue(VfxArtRenderer.isReady("anything"));

        VfxArtRenderer.install(null);

        assertFalse(VfxArtRenderer.isReady("anything"));
        assertFalse(VfxArtRenderer.render(null, null));
        assertFalse("no adapter installed means canDraw is false",
                VfxArtRenderer.canDraw(null));
    }
}

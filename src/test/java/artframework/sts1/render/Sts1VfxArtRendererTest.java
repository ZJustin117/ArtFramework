package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ExhaustBlurEffect;
import com.megacrit.cardcrawl.vfx.ExhaustPileParticle;
import com.megacrit.cardcrawl.vfx.FireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.GenericSmokeEffect;
import com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect;
import com.megacrit.cardcrawl.vfx.combat.BlockImpactLineEffect;
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
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.scene.LightFlareSEffect2"));
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect2"));
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
        } finally {
            setStaticField(ImageMaster.class, "WEB_VFX", previousWebVfx);
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
        /** Count of {@code draw(TextureRegion, ...)} (the packed-region shape) calls. */
        int textureRegionDrawCalls;
        /** First {@link Color} passed to {@link #setColor(Color)} (the applied draw tint). */
        Color firstSetColor;
        /** The first raw-texture + source-rect draw's arguments (the ICE/WEB/Calm/Shield shape). */
        Texture drawnTexture;
        float[] drawnArgs;

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
            textureRegionDrawCalls++;
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
        });
        assertTrue(VfxArtRenderer.isReady("anything"));

        VfxArtRenderer.install(null);

        assertFalse(VfxArtRenderer.isReady("anything"));
        assertFalse(VfxArtRenderer.render(null, null));
    }
}

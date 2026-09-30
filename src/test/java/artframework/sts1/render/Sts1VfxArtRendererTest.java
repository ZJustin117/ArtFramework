package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FireBurstParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.SmokeBlurEffect;
import com.megacrit.cardcrawl.vfx.stance.StanceAuraEffect;
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
        // TorchParticleLEffect plus the five newest members have a vY field but render never reads
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
                VfxDrawGeometry.Kind.NEMESIS_FIRE }) {
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

        Sts1VfxArtRenderer.CalmFields f = Sts1VfxArtRenderer.readCalmFields(effect);

        assertNotNull(f);
        assertEquals(4.5f, f.x, EPS);
        assertEquals(-2.75f, f.y, EPS);
        assertEquals(0.8f, f.scale, EPS);
        assertEquals(15f, f.rotation, EPS);
        assertEquals(0.25f, f.durDiv2, EPS);
        assertEquals(1.5f, f.duration, EPS);
        assertSame(effect.color, f.color);
    }

    @Test
    public void readCalmFieldsDefaultsAbsentDurationTermsToZero() {
        CalmNoDurationEffect effect = new CalmNoDurationEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.scale = 0.5f;
        effect.rotation = 6f;
        effect.color = Color.WHITE;

        Sts1VfxArtRenderer.CalmFields f = Sts1VfxArtRenderer.readCalmFields(effect);

        assertNotNull(f);
        assertEquals(1f, f.x, EPS);
        assertEquals(2f, f.y, EPS);
        assertEquals(0.5f, f.scale, EPS);
        assertEquals(6f, f.rotation, EPS);
        assertEquals(0f, f.durDiv2, EPS);
        assertEquals(0f, f.duration, EPS);
        assertSame(Color.WHITE, f.color);
    }

    @Test
    public void readCalmFieldsFailsOpenForNullMissingColorOrMissingXY() {
        assertNull(Sts1VfxArtRenderer.readCalmFields(null));

        CalmNoColorEffect noColor = new CalmNoColorEffect();
        noColor.x = 1f;
        noColor.y = 2f;
        noColor.scale = 1f;
        noColor.rotation = 0f;
        assertNull(Sts1VfxArtRenderer.readCalmFields(noColor));

        // CalmNoDurationEffect has no dur_div2/duration, but does have x/y/scale/rotation/color.
        CalmNoDurationEffect ok = new CalmNoDurationEffect();
        ok.color = Color.WHITE;
        assertNotNull(Sts1VfxArtRenderer.readCalmFields(ok));
    }

    @Test
    public void readFieldsIsNotUsedForCalmBecauseCalmHasNoImg() {
        // The img-based reader still requires img; Calm uses readCalmFields instead.
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

        assertFalse(renderer.isReady(null));
        assertFalse(renderer.isReady(""));
        assertFalse(renderer.isReady("   "));
        assertFalse(renderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.CALM_PARTICLE_EFFECT + "$Sub"));
        assertFalse(renderer.isReady(
                VfxClaimPolicy.DIVINITY_STANCE_CHANGE_PARTICLE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.SCENE_LIGHT_FLARE + "$Sub"));
        assertFalse(renderer.isReady(VfxClaimPolicy.FLASH_ATK_IMG + "$Sub"));
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
    }

    // --- no-GL draws (mirrors BackgroundRenderPatchesTest/Sts1GdxAtlasRegionsTest conventions) ---

    /** SpriteBatch double that counts blend-function calls without reaching the GL flush path. */
    static class CountingBatch extends SpriteBatch {
        int setBlendCalls;

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

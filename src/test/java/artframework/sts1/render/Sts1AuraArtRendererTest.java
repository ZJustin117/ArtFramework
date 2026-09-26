package artframework.sts1.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * No-GL, no-real-effect coverage for the F2b1 halves of {@link Sts1AuraArtRenderer}: the exact-FQN
 * readiness predicate and the superclass-walking reflective field snapshot. The native effect is
 * stood in for by local holder classes that mirror the field layout ({@code scale}/{@code
 * rotation}/{@code color} inherited from a base type; the rest owned by the subclass) so reflection
 * never touches libGDX GL or an actual STS class.
 *
 * <p>{@code AtlasRegion} is allocated with {@code sun.misc.Unsafe.allocateInstance} (the same
 * no-GL convention as {@code Sts1GdxAtlasRegionsTest}) because it is only ever captured by
 * reference, never queried.
 */
public class Sts1AuraArtRendererTest {

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

        Sts1AuraArtRenderer.Fields f = Sts1AuraArtRenderer.readFields(effect);

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

        Sts1AuraArtRenderer.Fields f = Sts1AuraArtRenderer.readFields(effect);

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
    public void readFieldsReturnsNullWhenARequiredFieldIsMissing() {
        NoImgEffect effect = new NoImgEffect();
        effect.x = 1f;
        effect.y = 2f;
        effect.vY = 3f;
        effect.scale = 1f;
        effect.rotation = 0f;
        effect.color = Color.WHITE;

        assertNull(Sts1AuraArtRenderer.readFields(effect));
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

        assertNull(Sts1AuraArtRenderer.readFields(effect));
    }

    @Test
    public void readFieldsReturnsNullForNullInput() {
        assertNull(Sts1AuraArtRenderer.readFields(null));
    }

    @Test
    public void isReadyIsTrueForTheThreeExactFqnsAndFalseOtherwise() {
        Sts1AuraArtRenderer renderer = new Sts1AuraArtRenderer();

        assertTrue(renderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertTrue(renderer.isReady(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertTrue(renderer.isReady(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));

        assertFalse(renderer.isReady(null));
        assertFalse(renderer.isReady(""));
        assertFalse(renderer.isReady("   "));
        assertFalse(renderer.isReady(
                "com.megacrit.cardcrawl.vfx.stance.CalmParticleEffect"));
        assertFalse(renderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT + "$Sub"));
    }

    @Test
    public void renderFailsOpenForNullArgumentsAndNeverThrows() {
        // A real SpriteBatch draw needs a live GL context, so it is not constructible in a headless
        // unit test; the null-arg path proves the F2b2 render entry point fails open. The actual
        // pixels are covered by on-device visual verification.
        Sts1AuraArtRenderer renderer = new Sts1AuraArtRenderer();

        assertFalse(renderer.render(null, null));
    }

    @Test
    public void installDelegatesReadinessAndDrawToTheInstalledAdapter() {
        try {
            AuraArtRenderer.uninstall();
            assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
            assertFalse(AuraArtRenderer.render(null, null));

            AuraArtRenderer.install(new AuraArtRenderer.Adapter() {
                @Override
                public boolean isReady(String nativeClassName) {
                    return AuraClaimPolicy.STANCE_AURA_EFFECT.equals(nativeClassName);
                }

                @Override
                public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                    return true;
                }
            });

            assertTrue(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
            assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
            assertTrue(AuraArtRenderer.render(null, null));
        } finally {
            AuraArtRenderer.uninstall();
        }

        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.render(null, null));
    }

    @Test
    public void installNullRestoresTheInertDefault() {
        AuraArtRenderer.install(new AuraArtRenderer.Adapter() {
            @Override
            public boolean isReady(String nativeClassName) {
                return true;
            }

            @Override
            public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                return true;
            }
        });
        assertTrue(AuraArtRenderer.isReady("anything"));

        AuraArtRenderer.install(null);

        assertFalse(AuraArtRenderer.isReady("anything"));
        assertFalse(AuraArtRenderer.render(null, null));
    }
}

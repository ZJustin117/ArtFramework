package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.sts1.PresentSafety;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BackgroundRenderGateTest {

    private Object previousWhiteSquare;

    @Before
    public void setUp() throws Exception {
        previousWhiteSquare = whiteSquareField().get(null);
    }

    @After
    public void tearDown() throws Exception {
        whiteSquareField().set(null, previousWhiteSquare);
        PresentSafety.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void variantParserAcceptsOnlyDocumentedNames() {
        assertEquals(BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.Variant.parse("off"));
        assertEquals(BackgroundRenderGate.Variant.SOLID,
                BackgroundRenderGate.Variant.parse(" Solid "));
        assertEquals(BackgroundRenderGate.Variant.CHECKER,
                BackgroundRenderGate.Variant.parse("CHECKER"));
        assertEquals(BackgroundRenderGate.Variant.GRID, BackgroundRenderGate.Variant.parse("grid"));
        assertNull(BackgroundRenderGate.Variant.parse("nonsense"));
        assertNull(BackgroundRenderGate.Variant.parse(null));
    }

    @Test
    public void artOwnsBackgroundIsFalseWhenVariantOff() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.OFF);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertFalse(BackgroundRenderGate.artOwnsBackground());
    }

    @Test
    public void artOwnsBackgroundIsFalseDuringPanic() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.SOLID);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        PresentSafety.panic("background-gate-test");
        assertFalse(BackgroundRenderGate.artOwnsBackground());
    }

    @Test
    public void artOwnsBackgroundIsFalseWhenFamilyNotFiltered() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.CHECKER);
        assertFalse(BackgroundRenderGate.artOwnsBackground());
    }

    @Test
    public void artOwnsBackgroundIsFalseWithoutTexture() {
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.GRID);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        // ImageMaster.WHITE_SQUARE_IMG is null off-GL, so the suppression decision must be false.
        assertNull(com.megacrit.cardcrawl.helpers.ImageMaster.WHITE_SQUARE_IMG);
        assertFalse(BackgroundRenderGate.artOwnsBackground());
    }

    @Test
    public void artOwnsBackgroundRequiresVariantFilterAndNoPanic() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.GRID);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertTrue(BackgroundRenderGate.artOwnsBackground());

        Sts1VerifyDiagnostics.disableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertFalse(BackgroundRenderGate.artOwnsBackground());
    }

    @Test
    public void renderSelectedVariantDrawsNothingForNullBatch() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.SOLID);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        BackgroundRenderGate.renderSelectedVariant(null);
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        // Suppression is a decision independent of the batch argument.
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void renderSelectedVariantDrawsNothingWhenVariantOff() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.OFF);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        BackgroundRenderGate.renderSelectedVariant(toleratingBatch());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertFalse(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void drawDecoupledFromSuppressionWhenFamilyNotFiltered() {
        Texture texture = installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.GRID);
        // Family retained: ART still draws (hidden under native), but native is not suppressed.
        BackgroundRenderGate.renderSelectedVariant(newBatch(texture));
        assertTrue("non-off variant must draw even when the family is retained",
                BackgroundRenderGate.artDrawCount() > 0L);
        assertFalse(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void drawAndSuppressWhenVariantFilteredAndNoPanic() {
        Texture texture = installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.CHECKER);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        BackgroundRenderGate.renderSelectedVariant(newBatch(texture));
        assertTrue(BackgroundRenderGate.artDrawCount() > 0L);
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void panicDrawsNothingAndDoesNotSuppress() {
        Texture texture = installWhiteSquareTexture();
        PresentSafety.panic("background-gate-render");
        // Panic cleared the variant and filter scope; re-arm both to isolate the panic guard.
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.CHECKER);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        BackgroundRenderGate.renderSelectedVariant(newBatch(texture));
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertFalse(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void throwingDrawFailsOpenWithoutCounterOrSuppressionSideEffects() throws Exception {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch broken = newBatch(texture);
        // drawing == false makes SpriteBatch.draw throw before any GL work.
        setField(broken, SpriteBatch.class, "drawing", Boolean.FALSE);
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.SOLID);
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertFalse("a throwing draw must report not-painted",
                BackgroundRenderGate.renderSelectedVariant(broken));
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        // The bare configuration decision still reports the filtered+variant state; the Prefix
        // combines it with the painted result and therefore continues native.
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
    }

    @Test
    public void ownsBackgroundDecisionMatchesSuppressNativeBackground() {
        installWhiteSquareTexture();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.SOLID);
        assertFalse(BackgroundRenderGate.ownsBackgroundDecision());
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertEquals(BackgroundRenderGate.suppressNativeBackground(),
                BackgroundRenderGate.ownsBackgroundDecision());
        assertTrue(BackgroundRenderGate.ownsBackgroundDecision());
        PresentSafety.panic("background-gate-decision");
        assertFalse(BackgroundRenderGate.ownsBackgroundDecision());
    }

    @Test
    public void recordArtDrawCounterBehavior() {
        BackgroundRenderGate.recordArtDraw(0);
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        BackgroundRenderGate.recordArtDraw(1);
        assertEquals(1L, BackgroundRenderGate.artDrawCount());
    }

    /** A tolerating SpriteBatch double whose color accessors never touch GL. */
    private static com.badlogic.gdx.graphics.g2d.SpriteBatch toleratingBatch() {
        try {
            java.lang.reflect.Field theUnsafe =
                    Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            com.badlogic.gdx.graphics.g2d.SpriteBatch sb =
                    (com.badlogic.gdx.graphics.g2d.SpriteBatch) unsafe.getClass()
                            .getMethod("allocateInstance", Class.class)
                            .invoke(unsafe,
                                    com.badlogic.gdx.graphics.g2d.SpriteBatch.class);
            java.lang.reflect.Field color =
                    com.badlogic.gdx.graphics.g2d.SpriteBatch.class.getDeclaredField("color");
            color.setAccessible(true);
            color.setFloat(sb, com.badlogic.gdx.graphics.Color.WHITE.toFloatBits());
            java.lang.reflect.Field tempColor =
                    com.badlogic.gdx.graphics.g2d.SpriteBatch.class.getDeclaredField("tempColor");
            tempColor.setAccessible(true);
            tempColor.set(sb, new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f));
            return sb;
        } catch (Exception failure) {
            throw new AssertionError("could not build tolerating SpriteBatch double", failure);
        }
    }

    @Test
    public void availableAndCountersRoundTrip() {
        assertTrue(BackgroundRenderGate.available());
        BackgroundRenderGate.recordArtDraw(3);
        BackgroundRenderGate.recordArtDraw(2);
        BackgroundRenderGate.recordNativeFallback();
        assertEquals(5L, BackgroundRenderGate.artDrawCount());
        assertEquals(1L, BackgroundRenderGate.nativeFallbackCount());
        BackgroundRenderGate.recordArtDraw(0);
        assertEquals(5L, BackgroundRenderGate.artDrawCount());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void probeSliceExposesStableShape() {
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.SOLID);
        Map<String, Object> probe = BackgroundRenderGate.probeSlice();
        assertEquals("solid", probe.get("variant"));
        assertEquals(Boolean.TRUE, probe.get("available"));
        assertEquals(Boolean.FALSE, probe.get("artOwnsBackgroundRequested"));
        assertEquals(Long.valueOf(0L), probe.get("artDrawCount"));
        assertEquals(Long.valueOf(0L), probe.get("nativeFallbackCount"));
    }

    @Test
    public void clearAndResetRestoreOffWithZeroedCounters() {
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.CHECKER);
        BackgroundRenderGate.recordArtDraw(4);
        BackgroundRenderGate.recordNativeFallback();
        BackgroundRenderGate.clearForRecovery();
        assertEquals(BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.variant());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0L, BackgroundRenderGate.nativeFallbackCount());

        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.GRID);
        BackgroundRenderGate.recordArtDraw(7);
        BackgroundRenderGate.resetForTests();
        assertEquals(BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.variant());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
    }

    private static Field whiteSquareField() throws NoSuchFieldException, ClassNotFoundException {
        return Class.forName("com.megacrit.cardcrawl.helpers.ImageMaster")
                .getField("WHITE_SQUARE_IMG");
    }

    private static Texture installWhiteSquareTexture() {
        try {
            Texture texture = (Texture) allocate(Texture.class);
            setField(texture, Texture.class, "data", new TextureData() {
                public TextureDataType getType() { return TextureDataType.Pixmap; }
                public boolean isPrepared() { return true; }
                public void prepare() {}
                public Pixmap consumePixmap() { return null; }
                public boolean disposePixmap() { return false; }
                public void consumeCustomData(int target) {}
                public int getWidth() { return 1; }
                public int getHeight() { return 1; }
                public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
                public boolean useMipMaps() { return false; }
                public boolean isManaged() { return false; }
            });
            whiteSquareField().set(null, texture);
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not install no-GL white-square texture double", failure);
        }
    }

    private static SpriteBatch newBatch(Texture texture) {
        try {
            SpriteBatch sb = (SpriteBatch) allocate(SpriteBatch.class);
            setField(sb, SpriteBatch.class, "vertices", new float[20 * 4096]);
            setField(sb, SpriteBatch.class, "idx", Integer.valueOf(0));
            setField(sb, SpriteBatch.class, "drawing", Boolean.TRUE);
            setField(sb, SpriteBatch.class, "lastTexture", texture);
            setField(sb, SpriteBatch.class, "color", Float.valueOf(Color.WHITE.toFloatBits()));
            setField(sb, SpriteBatch.class, "tempColor", new Color(1f, 1f, 1f, 1f));
            return sb;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL SpriteBatch test double", failure);
        }
    }

    private static Object allocate(Class<?> type) throws Exception {
        Field theUnsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }

    private static void setField(Object target, Class<?> owner, String name, Object value)
            throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

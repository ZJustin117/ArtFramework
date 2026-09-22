package artframework.sts1.patch;

import artframework.api.ArtFramework;
import artframework.sts1.PresentSafety;
import artframework.sts1.render.BackgroundRenderGate;
import artframework.sts1.render.Sts1VerifyDiagnostics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Suppression-gate contract for the four concrete scene {@code renderCombatRoomBg} Prefixes.
 *
 * <p>ART must suppress the native room background only in the exact
 * variant-filtered-no-panic state; every other state (including a throwing draw) must continue
 * native. The batch used here is a no-GL test double (Unsafe-allocated, primed {@code vertices}),
 * not a real GL context; the gate only touches {@code getColor}/{@code setColor}/{@code draw},
 * which are pure CPU writes in that state.
 */
public class BackgroundRenderPatchesTest {

    private static final float CELL = 64f;
    private static final float FALLBACK_WIDTH = 1920f;
    private static final float FALLBACK_HEIGHT = 1080f;

    private Object previousWhiteSquare;

    @Before
    public void setUp() throws Exception {
        previousWhiteSquare = whiteSquareField().get(null);
    }

    @After
    public void tearDown() throws Exception {
        whiteSquareField().set(null, previousWhiteSquare);
        PresentSafety.resetForTests();
        Sts1VerifyDiagnostics.resetForTests();
        ArtFramework.resetForTests();
    }

    private interface PrefixCall {
        SpireReturn<Void> invoke(SpriteBatch sb);
    }

    private static final PrefixCall[] PREFIXES = {
        new PrefixCall() {
            public SpireReturn<Void> invoke(SpriteBatch sb) {
                return BackgroundRenderPatches.ObserveNativeBottomBackground.Prefix(null, sb);
            }
        },
        new PrefixCall() {
            public SpireReturn<Void> invoke(SpriteBatch sb) {
                return BackgroundRenderPatches.ObserveNativeCityBackground.Prefix(null, sb);
            }
        },
        new PrefixCall() {
            public SpireReturn<Void> invoke(SpriteBatch sb) {
                return BackgroundRenderPatches.ObserveNativeBeyondBackground.Prefix(null, sb);
            }
        },
        new PrefixCall() {
            public SpireReturn<Void> invoke(SpriteBatch sb) {
                return BackgroundRenderPatches.ObserveNativeEndingBackground.Prefix(null, sb);
            }
        },
    };

    @Test
    public void suppressesNativeBackgroundOnlyWhenVariantFilteredAndNoPanic() {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch sb = newBatch(texture);

        for (PrefixCall prefix : PREFIXES) {
            // Non-off variant + explicitly filtered family + no panic: suppress, record one quad.
            Sts1VerifyDiagnostics.resetForTests();
            Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
            Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
            SpireReturn<Void> suppressed = prefix.invoke(sb);
            assertTrue("ART-owned background must suppress the native scene background",
                    suppressed.isPresent());
            assertEquals(1L, BackgroundRenderGate.artDrawCount());
        }
    }

    @Test
    public void continuesNativeWhenVariantOff() {
        SpriteBatch sb = newBatch(installWhiteSquareTexture());
        for (PrefixCall prefix : PREFIXES) {
            Sts1VerifyDiagnostics.resetForTests();
            Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);

            SpireReturn<Void> result = prefix.invoke(sb);

            assertFalse("off variant must continue native", result.isPresent());
            assertEquals(0L, BackgroundRenderGate.artDrawCount());
        }
    }

    @Test
    public void continuesNativeButStillDrawsWhenFamilyNotFiltered() {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch sb = newBatch(texture);
        for (PrefixCall prefix : PREFIXES) {
            Sts1VerifyDiagnostics.resetForTests();
            Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.GRID);

            SpireReturn<Void> result = prefix.invoke(sb);

            assertFalse("unfiltered family must continue native", result.isPresent());
            // Draw is decoupled from suppression: the ART background is still drawn and then
            // painted over by the retained native background (the z-order evidence).
            assertTrue("non-off variant must draw while retained",
                    BackgroundRenderGate.artDrawCount() > 0L);
        }
    }

    @Test
    public void retainedNativeDrawsNonZeroQuadsForEveryVariant() {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch sb = newBatch(texture);
        int cols = (int) Math.ceil(screenWidth() / CELL);
        int rows = (int) Math.ceil(screenHeight() / CELL);

        assertEquals(Long.valueOf(1L), retainedQuadCount(sb, BackgroundRenderGate.Variant.SOLID, 1L));
        assertEquals(Long.valueOf((long) cols * rows),
                retainedQuadCount(sb, BackgroundRenderGate.Variant.CHECKER, (long) cols * rows));
        assertEquals(Long.valueOf((long) (1 + cols + rows)),
                retainedQuadCount(sb, BackgroundRenderGate.Variant.GRID, (long) (1 + cols + rows)));
    }

    private static Long retainedQuadCount(SpriteBatch sb, BackgroundRenderGate.Variant variant,
            long expected) {
        Sts1VerifyDiagnostics.resetForTests();
        Sts1VerifyDiagnostics.setBackgroundVariant(variant);
        // No filter: native retained, but the draw must still happen.
        BackgroundRenderGate.renderSelectedVariant(sb);
        assertFalse("family retained must not suppress",
                BackgroundRenderGate.suppressNativeBackground());
        long count = BackgroundRenderGate.artDrawCount();
        assertTrue("retained variant must draw a non-zero quad count", count > 0L);
        assertEquals(Long.valueOf(expected), Long.valueOf(count));
        return Long.valueOf(count);
    }

    @Test
    public void continuesNativeDuringPanic() {
        SpriteBatch sb = newBatch(installWhiteSquareTexture());
        for (PrefixCall prefix : PREFIXES) {
            Sts1VerifyDiagnostics.resetForTests();
            PresentSafety.panic("background-patches-test");
            // Panic cleared the variant and filter scope; re-arm both to isolate the panic guard.
            Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.CHECKER);
            Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);

            SpireReturn<Void> result = prefix.invoke(sb);

            assertFalse("panic must continue native", result.isPresent());
            assertEquals(0L, BackgroundRenderGate.artDrawCount());
        }
    }

    @Test
    public void nullBatchFailsOpenToNative() {
        for (PrefixCall prefix : PREFIXES) {
            Sts1VerifyDiagnostics.resetForTests();
            Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
            Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);

            BackgroundRenderGate.renderSelectedVariant(null);
            // No texture is installed here, so the suppression decision also stays false.
            assertFalse(BackgroundRenderGate.suppressNativeBackground());
            assertFalse("null batch must continue native", prefix.invoke(null).isPresent());
            assertEquals(0L, BackgroundRenderGate.artDrawCount());
        }
    }

    @Test
    public void throwingDrawFailsOpenToNativeEvenWhenConfiguredToSuppress() throws Exception {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch broken = newBatch(texture);
        // drawing == false makes SpriteBatch.draw throw before any GL work; renderSelectedVariant
        // must swallow it, report not-painted, and record no quad. Because suppression requires a
        // successful paint, the Prefix must still continue native.
        setField(broken, SpriteBatch.class, "drawing", Boolean.FALSE);

        for (PrefixCall prefix : PREFIXES) {
            Sts1VerifyDiagnostics.resetForTests();
            Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
            Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);

            SpireReturn<Void> result = prefix.invoke(broken);

            assertEquals(0L, BackgroundRenderGate.artDrawCount());
            assertTrue("filtered variant state still reports the configured suppression decision",
                    BackgroundRenderGate.suppressNativeBackground());
            assertFalse("a failed background draw must continue native",
                    result.isPresent());
        }
    }

    @Test
    public void recordsOneQuadPerVariantWhenSuppressed() {
        Texture texture = installWhiteSquareTexture();
        SpriteBatch sb = newBatch(texture);
        int cols = (int) Math.ceil(screenWidth() / CELL);
        int rows = (int) Math.ceil(screenHeight() / CELL);

        Sts1VerifyDiagnostics.resetForTests();
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
        BackgroundRenderGate.renderSelectedVariant(sb);
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
        assertEquals(1L, BackgroundRenderGate.artDrawCount());

        Sts1VerifyDiagnostics.resetForTests();
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.CHECKER);
        BackgroundRenderGate.renderSelectedVariant(sb);
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
        assertEquals((long) cols * rows, BackgroundRenderGate.artDrawCount());

        Sts1VerifyDiagnostics.resetForTests();
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.GRID);
        BackgroundRenderGate.renderSelectedVariant(sb);
        assertTrue(BackgroundRenderGate.suppressNativeBackground());
        assertEquals((long) (1 + cols + rows), BackgroundRenderGate.artDrawCount());
    }

    @Test
    public void panicClearsBackgroundVariantForRecovery() {
        Sts1VerifyDiagnostics.setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
        assertEquals(BackgroundRenderGate.Variant.SOLID, BackgroundRenderGate.variant());

        PresentSafety.panic("background-patches-recovery");

        assertEquals("panic must clear the background variant for recovery",
                BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.variant());
    }

    private static float screenWidth() {
        return positiveOr(readStaticFloat("com.megacrit.cardcrawl.core.Settings", "WIDTH"),
                FALLBACK_WIDTH);
    }

    private static float screenHeight() {
        return positiveOr(readStaticFloat("com.megacrit.cardcrawl.core.Settings", "HEIGHT"),
                FALLBACK_HEIGHT);
    }

    private static float positiveOr(float value, float fallback) {
        return value > 0f ? value : fallback;
    }

    private static float readStaticFloat(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            return type.getField(fieldName).getFloat(null);
        } catch (Throwable ignored) {
            return 0f;
        }
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
            setField(sb, SpriteBatch.class, "color",
                    Float.valueOf(Color.WHITE.toFloatBits()));
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
        assertNotNull(field.get(target));
    }
}

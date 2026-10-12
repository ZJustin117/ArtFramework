package artframework.sts1.render;

import artframework.assets.AtlasRegion;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * E06 Red: the {@code bottom} background variant draws the ACT-1 {@code TheBottomScene} combat
 * background layers as real native atlas regions from the pre-native Prefix.
 *
 * <p>The native {@code TheBottomScene.renderCombatRoomBg} order is authoritative (decompile):
 * {@code bg}, {@code mg}, then walls per flags, then {@code ceiling}, then each flagged
 * {@code ceilingMod}. Every region is submitted through the PIXEL-RECT 16-arg libGDX overload
 * ({@code originX/originY == 0}, packed size, page source rect) — never a UV blit.
 */
public class BackgroundBottomVariantTest {

    private float previousScale;
    private float previousXScale;
    private float previousYScale;
    private boolean previousFourByThree;
    private boolean previousLetterbox;
    private Object previousWhiteSquare;

    @Before
    public void setUp() throws Exception {
        previousScale = Settings.scale;
        previousXScale = Settings.xScale;
        previousYScale = Settings.yScale;
        previousFourByThree = Settings.isFourByThree;
        previousLetterbox = Settings.isLetterbox;
        Settings.scale = 1f;
        Settings.xScale = 1f;
        Settings.yScale = 1f;
        Settings.isFourByThree = false;
        Settings.isLetterbox = false;
        previousWhiteSquare = whiteSquareField().get(null);
    }

    @After
    public void tearDown() throws Exception {
        Settings.scale = previousScale;
        Settings.xScale = previousXScale;
        Settings.yScale = previousYScale;
        Settings.isFourByThree = previousFourByThree;
        Settings.isLetterbox = previousLetterbox;
        whiteSquareField().set(null, previousWhiteSquare);
        BackgroundRenderGate.resetForTests();
        Sts1VerifyDiagnostics.resetForTests();
    }

    @Test
    public void variantParserAcceptsBottom() {
        assertEquals(BackgroundRenderGate.Variant.BOTTOM, BackgroundRenderGate.Variant.parse("bottom"));
        assertEquals(BackgroundRenderGate.Variant.BOTTOM,
                BackgroundRenderGate.Variant.parse(" Bottom "));
        // Existing names are unchanged.
        assertEquals(BackgroundRenderGate.Variant.GRID, BackgroundRenderGate.Variant.parse("grid"));
    }

    @Test
    public void layerPlanFollowsNativeOrderForWallsAndCeilingMods() {
        BackgroundRenderGate.BottomFlags wallsAndMods = BackgroundRenderGate.BottomFlags.of(
                true, true, false, true, false, true);
        assertEquals(
                Arrays.asList(
                        BackgroundRenderGate.BottomLayer.BG,
                        BackgroundRenderGate.BottomLayer.MG,
                        BackgroundRenderGate.BottomLayer.SOLID_WALL,
                        BackgroundRenderGate.BottomLayer.HOLLOW_WALL,
                        BackgroundRenderGate.BottomLayer.CEILING,
                        BackgroundRenderGate.BottomLayer.CEILING_MOD1,
                        BackgroundRenderGate.BottomLayer.CEILING_MOD3),
                BackgroundRenderGate.bottomLayerPlan(wallsAndMods));

        BackgroundRenderGate.BottomFlags hollowAndLeft = BackgroundRenderGate.BottomFlags.of(
                false, true, true);
        assertEquals(
                Arrays.asList(
                        BackgroundRenderGate.BottomLayer.BG,
                        BackgroundRenderGate.BottomLayer.MG,
                        BackgroundRenderGate.BottomLayer.HOLLOW_WALL,
                        BackgroundRenderGate.BottomLayer.LEFT_WALL,
                        BackgroundRenderGate.BottomLayer.CEILING),
                BackgroundRenderGate.bottomLayerPlan(hollowAndLeft));
    }

    @Test
    public void failOpenPlanIsBackgroundMidAndCeiling() {
        assertEquals(
                Arrays.asList(
                        BackgroundRenderGate.BottomLayer.BG,
                        BackgroundRenderGate.BottomLayer.MG,
                        BackgroundRenderGate.BottomLayer.CEILING),
                BackgroundRenderGate.bottomLayerPlan(BackgroundRenderGate.BottomFlags.failOpen()));
    }

    /**
     * The real draw uses the 16-arg pixel-rect overload with origin (0,0), the packed size, the
     * unscaled uniform {@code Settings.scale}, and the region's own page source rect.
     */
    @Test
    public void realDrawMirrorsTheSixteenArgNativeCallInPlanOrder() {
        BackgroundRenderGate.BottomFlags flags =
                BackgroundRenderGate.BottomFlags.of(true, true, false, true, false, true);
        CapturingBatch batch = newBatch(texture(1024, 1024));
        MapSource source = fullFixtureSource();
        BackgroundRenderGate.resetForTests();

        boolean painted = BackgroundRenderGate.renderBottomBackground(batch, flags, source);

        assertTrue("a fully resolved plan must paint", painted);
        List<BackgroundRenderGate.BottomLayer> plan = BackgroundRenderGate.bottomLayerPlan(flags);
        assertEquals("one submitted draw per planned layer", plan.size(), batch.bottomDraws.size());
        assertEquals("counter grows by the number of submitted layers",
                (long) plan.size(), BackgroundRenderGate.artDrawCount());

        for (int i = 0; i < plan.size(); i++) {
            String regionName = plan.get(i).regionName();
            Fixture fixture = FIXTURES.get(regionName);
            assertNotNull("missing fixture for " + regionName, fixture);
            Draw draw = batch.bottomDraws.get(i);
            assertEquals("x is offsetX*Settings.scale for " + regionName,
                    fixture.offsetX * 1f, draw.x, 0.0001f);
            assertEquals("y is offsetY*Settings.scale for " + regionName,
                    fixture.offsetY * 1f, draw.y, 0.0001f);
            assertEquals("originX is 0 for " + regionName, 0f, draw.originX, 0.0001f);
            assertEquals("originY is 0 for " + regionName, 0f, draw.originY, 0.0001f);
            assertEquals("width is the packed width for " + regionName,
                    (float) fixture.srcWidth, draw.width, 0.0001f);
            assertEquals("height is the packed height for " + regionName,
                    (float) fixture.srcHeight, draw.height, 0.0001f);
            assertEquals("scaleX is Settings.scale for " + regionName, 1f, draw.scaleX, 0.0001f);
            assertEquals("scaleY is Settings.scale for " + regionName, 1f, draw.scaleY, 0.0001f);
            assertEquals("rotation is 0 for " + regionName, 0f, draw.rotation, 0.0001f);
            assertEquals("srcX is the region page x for " + regionName,
                    fixture.srcX, draw.srcX);
            assertEquals("srcY is the region page y for " + regionName,
                    fixture.srcY, draw.srcY);
            assertEquals("srcWidth is the packed width for " + regionName,
                    fixture.srcWidth, draw.srcWidth);
            assertEquals("srcHeight is the packed height for " + regionName,
                    fixture.srcHeight, draw.srcHeight);
            assertFalse("flipX is false for " + regionName, draw.flipX);
            assertFalse("flipY is false for " + regionName, draw.flipY);
            assertSame("texture comes from the region source for " + regionName,
                    source.page, draw.texture);
        }

        // Native tints GRAY before the solid wall when hollow + (solid or left) is present, and
        // resets to WHITE before the hollow wall.
        int solidIndex = plan.indexOf(BackgroundRenderGate.BottomLayer.SOLID_WALL);
        int hollowIndex = plan.indexOf(BackgroundRenderGate.BottomLayer.HOLLOW_WALL);
        assertTrue("solid wall must be in the plan", solidIndex >= 0);
        assertTrue("hollow wall must be in the plan", hollowIndex >= 0);
        assertEquals("GRAY is in effect when the solid wall is drawn",
                Color.GRAY, batch.bottomDraws.get(solidIndex).color);
        assertEquals("WHITE is restored before the hollow wall is drawn",
                Color.WHITE, batch.bottomDraws.get(hollowIndex).color);
    }

    /**
     * Anti-tautology: the real Bottom draw is NOT the synthetic single-quad path. The synthetic
     * variant draw cannot own Bottom (it has no scene), while the real draw submits every layer.
     */
    @Test
    public void bottomIsNotTheSyntheticQuadPath() {
        Texture whiteSquare = installWhiteSquareTexture();
        CapturingBatch batch = newBatch(whiteSquare);
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.BOTTOM);

        assertFalse("the synthetic path must not claim Bottom",
                BackgroundRenderGate.renderSelectedVariant(batch));
        assertEquals("the synthetic path records nothing for Bottom",
                0L, BackgroundRenderGate.artDrawCount());

        BackgroundRenderGate.BottomFlags flags =
                BackgroundRenderGate.BottomFlags.of(true, true, false, false, false, true);
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.BOTTOM);
        assertTrue(BackgroundRenderGate.renderBottomBackground(batch, flags, fullFixtureSource()));
        assertTrue("the real Bottom draw submits multiple layers, not one quad",
                BackgroundRenderGate.artDrawCount() >= 3L);
    }

    /** The synthetic variants are unchanged: they still draw their existing quad counts. */
    @Test
    public void syntheticVariantsStillDrawTheirQuadCounts() {
        Texture whiteSquare = installWhiteSquareTexture();
        CapturingBatch batch = newBatch(whiteSquare);

        assertEquals("solid is exactly one full-screen quad",
                1L, recordedQuads(batch, BackgroundRenderGate.Variant.SOLID));
        long checker = recordedQuads(batch, BackgroundRenderGate.Variant.CHECKER);
        long grid = recordedQuads(batch, BackgroundRenderGate.Variant.GRID);
        assertTrue("checker draws a grid of quads", checker > 1L);
        assertTrue("grid draws a fill plus its lines", grid > 1L);
    }

    /** Any Throwable fails open: no paint, no counter. */
    @Test
    public void throwingSourceFailsOpenWithoutCounter() {
        CapturingBatch batch = newBatch(texture(1024, 1024));
        BackgroundRenderGate.BottomRegionSource thrower =
                new BackgroundRenderGate.BottomRegionSource() {
                    public Sts1AtlasMaterializer.RegionTexture region(String regionName) {
                        throw new IllegalStateException("boom");
                    }
                };
        BackgroundRenderGate.resetForTests();

        assertFalse("a throwing source must report not-painted",
                BackgroundRenderGate.renderBottomBackground(
                        batch, BackgroundRenderGate.BottomFlags.failOpen(), thrower));
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
    }

    /**
     * Suppression is all-or-nothing: if an ALWAYS-present base layer (bg/mg/ceiling) does not
     * resolve, the whole ACT-1 background must NOT paint (so native continues) rather than drawing a
     * subset and then suppressing native into an incomplete room.
     */
    @Test
    public void missingBaseLayerFailsOpenInsteadOfPartialSuppression() {
        CapturingBatch batch = newBatch(texture(1024, 1024));
        BackgroundRenderGate.BottomFlags flags =
                BackgroundRenderGate.BottomFlags.of(true, true, false, true, false, true);
        BackgroundRenderGate.resetForTests();

        for (String base : new String[] {"bg", "mod/mg", "mod/ceiling"}) {
            MapSource partial = fullFixtureSource();
            partial.regions.remove(base);
            BackgroundRenderGate.resetForTests();
            assertFalse("a missing base layer (" + base + ") must not paint",
                    BackgroundRenderGate.renderBottomBackground(batch, flags, partial));
            assertEquals("no counter when a base layer is missing (" + base + ")",
                    0L, BackgroundRenderGate.artDrawCount());
        }
    }

    private static long recordedQuads(SpriteBatch batch, BackgroundRenderGate.Variant variant) {
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(variant);
        assertTrue(BackgroundRenderGate.renderSelectedVariant(batch));
        return BackgroundRenderGate.artDrawCount();
    }

    private static MapSource fullFixtureSource() {
        Texture page = texture(1024, 1024);
        MapSource source = new MapSource(page);
        for (Map.Entry<String, Fixture> entry : FIXTURES.entrySet()) {
            Fixture f = entry.getValue();
            AtlasRegion region = new AtlasRegion(
                    "",
                    entry.getKey(),
                    f.srcX,
                    f.srcY,
                    f.srcWidth,
                    f.srcHeight,
                    1024,
                    1024,
                    f.srcWidth,
                    f.srcHeight,
                    f.offsetX,
                    f.offsetY,
                    0);
            source.regions.put(entry.getKey(),
                    new Sts1AtlasMaterializer.RegionTexture(page, region));
        }
        return source;
    }

    private static final Map<String, Fixture> FIXTURES =
            new LinkedHashMap<String, Fixture>();

    static {
        FIXTURES.put("bg", new Fixture(10, 20, 100, 50, 1, 2));
        FIXTURES.put("mod/mg", new Fixture(200, 30, 64, 64, 3, 4));
        FIXTURES.put("mod/midWall", new Fixture(300, 40, 80, 20, 5, 6));
        FIXTURES.put("mod/mod2", new Fixture(400, 50, 70, 25, 7, 8));
        FIXTURES.put("mod/mod1", new Fixture(500, 60, 60, 30, 9, 10));
        FIXTURES.put("mod/ceiling", new Fixture(600, 70, 90, 35, 11, 12));
        FIXTURES.put("mod/ceilingMod1", new Fixture(700, 80, 40, 15, 13, 14));
        FIXTURES.put("mod/ceilingMod2", new Fixture(720, 82, 41, 16, 15, 16));
        FIXTURES.put("mod/ceilingMod3", new Fixture(740, 84, 42, 17, 17, 18));
        FIXTURES.put("mod/ceilingMod4", new Fixture(760, 86, 43, 18, 19, 20));
        FIXTURES.put("mod/ceilingMod5", new Fixture(780, 88, 44, 19, 21, 22));
        FIXTURES.put("mod/ceilingMod6", new Fixture(800, 90, 45, 20, 23, 24));
    }

    private static final class Fixture {
        final int srcX;
        final int srcY;
        final int srcWidth;
        final int srcHeight;
        final int offsetX;
        final int offsetY;

        Fixture(int srcX, int srcY, int srcWidth, int srcHeight, int offsetX, int offsetY) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static final class MapSource implements BackgroundRenderGate.BottomRegionSource {
        final Texture page;
        final Map<String, Sts1AtlasMaterializer.RegionTexture> regions =
                new LinkedHashMap<String, Sts1AtlasMaterializer.RegionTexture>();

        MapSource(Texture page) {
            this.page = page;
        }

        public Sts1AtlasMaterializer.RegionTexture region(String regionName) {
            return regions.get(regionName);
        }
    }

    /** One recorded 16-arg draw plus the tint active at submission time. */
    private static final class Draw {
        final Texture texture;
        final float x;
        final float y;
        final float originX;
        final float originY;
        final float width;
        final float height;
        final float scaleX;
        final float scaleY;
        final float rotation;
        final int srcX;
        final int srcY;
        final int srcWidth;
        final int srcHeight;
        final boolean flipX;
        final boolean flipY;
        final Color color;

        Draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY, Color color) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
            this.srcX = srcX;
            this.srcY = srcY;
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.flipX = flipX;
            this.flipY = flipY;
            this.color = color == null ? null : new Color(color);
        }
    }

    /**
     * No-GL batch that records the 16-arg pixel-rect overload (the native Bottom draw) separately
     * from the synthetic 4-arg quad path, and tracks the active tint without touching GL.
     */
    static class CapturingBatch extends SpriteBatch {
        // Unsafe.allocateInstance bypasses the constructor AND field initializers, so newBatch()
        // installs these reflectively; they must not be final.
        List<Draw> bottomDraws;
        long syntheticQuads;
        Color current;

        CapturingBatch() {
            super(1);
        }

        @Override
        public void setColor(Color color) {
            if (color != null) {
                current = color;
            }
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            current = new Color(r, g, b, a);
        }

        @Override
        public Color getColor() {
            return current;
        }

        @Override
        public void draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY) {
            bottomDraws.add(new Draw(texture, x, y, originX, originY, width, height,
                    scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY,
                    current));
        }

        @Override
        public void draw(Texture texture, float x, float y, float width, float height) {
            syntheticQuads++;
        }
    }

    private static CapturingBatch newBatch(Texture texture) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            CapturingBatch batch = (CapturingBatch) unsafe.allocateInstance(CapturingBatch.class);
            batch.bottomDraws = new ArrayList<Draw>();
            batch.current = Color.WHITE;
            return batch;
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL CapturingBatch", failure);
        }
    }

    private static Texture texture(final int width, final int height) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Texture texture = (Texture) ((sun.misc.Unsafe) unsafeField.get(null))
                    .allocateInstance(Texture.class);
            Field dataField = Texture.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(texture, new TextureData() {
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
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
    }

    private static Field whiteSquareField() throws NoSuchFieldException, ClassNotFoundException {
        return Class.forName("com.megacrit.cardcrawl.helpers.ImageMaster")
                .getField("WHITE_SQUARE_IMG");
    }

    private static Texture installWhiteSquareTexture() {
        Texture texture = texture(1, 1);
        try {
            whiteSquareField().set(null, texture);
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not install no-GL white-square texture double", failure);
        }
    }
}

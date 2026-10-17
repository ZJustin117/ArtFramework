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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * E09 Red: the {@code ending} background variant draws the ACT-4 {@code TheEndingScene} combat
 * background as a real native atlas region from the same pre-native Prefix ART already patches for
 * ACT-1/2/3 (E06/E07/E08).
 *
 * <p>The native {@code TheEndingScene.renderCombatRoomBg} is the whole method (decompile):
 * {@code sb.setColor(Color.WHITE); this.renderAtlasRegionIf(sb, this.bg, true);} — ONE
 * {@code bg} region in {@code endingScene/scene.atlas}, plain fixed-offset submission, WHITE, no
 * flags and no second pass. That is one 16-arg libGDX pixel-rect submission (origin {@code (0,0)},
 * packed size, page source rect, three-branch {@code Settings} scale rule). The atlas also carries
 * {@code campfire}/{@code event} for the non-combat branches; those are deferred (E09b) and must
 * never appear in this plan.
 *
 * <p>Non-tautology: the plan is asserted independently of any draw, the tint/blend expectations come
 * from the native {@code setColor(Color.WHITE)} / no-blend call list, the geometry is asserted from
 * an independently declared fixture rectangle in each of the plain path's three placement branches,
 * and the real draw is proven NOT to be the synthetic single-quad path (which cannot claim a
 * scene-scoped variant).
 */
public class BackgroundEndingVariantTest {

    private static final float DELTA = 1e-4f;

    /** The chosen {@code bg} fixture: distinctive page rect, packed size, and trim offsets. */
    private static final Fixture BG = new Fixture(37, 51, 1024, 576, 23, -17);

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
        whiteSquareField().set(null, null);
        BackgroundRenderGate.resetForTests();
        Sts1VerifyDiagnostics.resetForTests();
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

    // ---------------------------------------------------------------- 1. parser

    @Test
    public void variantParserAcceptsEndingAndKeepsTheExistingNames() {
        assertEquals(BackgroundRenderGate.Variant.ENDING,
                BackgroundRenderGate.Variant.parse("ending"));
        assertEquals(BackgroundRenderGate.Variant.ENDING,
                BackgroundRenderGate.Variant.parse(" Ending "));
        assertEquals(BackgroundRenderGate.Variant.ENDING,
                BackgroundRenderGate.Variant.parse("ENDING"));

        // Regression guard for the shared parser: the four scenes and the synthetic variants keep
        // parsing to their own constants (the Ending case must not swallow any of them).
        assertEquals(BackgroundRenderGate.Variant.BOTTOM,
                BackgroundRenderGate.Variant.parse("bottom"));
        assertEquals(BackgroundRenderGate.Variant.CITY, BackgroundRenderGate.Variant.parse("city"));
        assertEquals(BackgroundRenderGate.Variant.BEYOND,
                BackgroundRenderGate.Variant.parse("beyond"));
        assertEquals(BackgroundRenderGate.Variant.GRID, BackgroundRenderGate.Variant.parse("grid"));
        assertEquals(BackgroundRenderGate.Variant.OFF, BackgroundRenderGate.Variant.parse("off"));
    }

    // ---------------------------------------------------------------- 2. plan

    @Test
    public void endingLayerPlanIsExactlyTheSingleBackgroundRegion() {
        BackgroundRenderGate.resetForTests();

        List<BackgroundRenderGate.SceneLayerSpec> plan = BackgroundRenderGate.endingLayerPlan();

        assertNotNull(plan);
        assertEquals("the native method renders exactly one region", 1, plan.size());
        BackgroundRenderGate.SceneLayerSpec layer = plan.get(0);
        assertEquals("the ACT-4 combat region is bg", "bg", layer.regionName);
        assertEquals("nat: sb.setColor(Color.WHITE)", 1f, layer.tint.r, 0f);
        assertEquals("nat: sb.setColor(Color.WHITE)", 1f, layer.tint.g, 0f);
        assertEquals("nat: sb.setColor(Color.WHITE)", 1f, layer.tint.b, 0f);
        assertEquals("nat: sb.setColor(Color.WHITE)", 1f, layer.tint.a, 0f);
        assertEquals("no blend call in the native method",
                BackgroundRenderGate.SceneBlend.NORMAL, layer.blend);
        assertFalse("the native submission is the plain, non-rotating overload", layer.animated);
        assertEquals("plain layer has no plan-local x offset", 0f, layer.offsetX, 0f);
        assertEquals("plain layer has no plan-local y offset", 0f, layer.offsetY, 0f);
        assertEquals("plain layer has no rotation", 0f, layer.angle, 0f);

        // Pure: asking for the plan neither draws nor records anything.
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals("a second call is still a one-layer plan",
                1, BackgroundRenderGate.endingLayerPlan().size());
    }

    @Test
    public void endingPlanDrawsNoCampfireEventOrForegroundRegion() {
        // The atlas also has campfire/event for the non-combat branches; E09 must not deliver them.
        List<String> names = new ArrayList<String>();
        for (BackgroundRenderGate.SceneLayerSpec layer : BackgroundRenderGate.endingLayerPlan()) {
            names.add(layer.regionName);
        }
        assertEquals("only the combat bg region is planned", 1, names.size());
        assertFalse("deferred: campfire belongs to E09b", names.contains("campfire"));
        assertFalse("deferred: event belongs to E09b", names.contains("event"));
        for (String name : names) {
            assertFalse("deferred: no foreground region (" + name + ")",
                    name.toLowerCase().contains("fg"));
        }
    }

    // ---------------------------------------------------------------- 3. real draw

    @Test
    public void endingDrawSubmitsTheNativePixelRectAtTheDefaultScale() {
        CapturingBatch batch = newBatch();
        Color previous = new Color(0.2f, 0.3f, 0.4f, 0.5f);
        batch.current = new Color(previous);
        MapSource source = bgSource();
        BackgroundRenderGate.resetForTests();

        boolean painted = BackgroundRenderGate.renderEndingBackground(batch, source);

        assertTrue("a resolved bg region must paint", painted);
        assertEquals("exactly one native submission", 1, batch.draws.size());
        assertEquals("artDrawCount grows by exactly one", 1L, BackgroundRenderGate.artDrawCount());

        Draw draw = batch.draws.get(0);
        assertSame("the page texture comes from the region source", source.page, draw.texture);
        assertColor("native WHITE tint", 1f, 1f, 1f, 1f, draw.color);
        assertEquals("native default blend, src", 770, draw.blendSrc);
        assertEquals("native default blend, dst", 771, draw.blendDst);
        assertEquals("x is the region offsetX times Settings.scale",
                BG.offsetX * Settings.scale, draw.x, DELTA);
        assertEquals("y is the region offsetY times Settings.scale",
                BG.offsetY * Settings.scale, draw.y, DELTA);
        assertEquals("plain submission has no x pivot", 0f, draw.originX, 0f);
        assertEquals("plain submission has no y pivot", 0f, draw.originY, 0f);
        assertEquals("plain submission is unrotated", 0f, draw.rotation, 0f);
        assertEquals("width is the packed region width", (float) BG.srcWidth, draw.width, 0f);
        assertEquals("height is the packed region height", (float) BG.srcHeight, draw.height, 0f);
        assertEquals("scaleX is Settings.scale", Settings.scale, draw.scaleX, 0f);
        assertEquals("scaleY is Settings.scale", Settings.scale, draw.scaleY, 0f);
        assertEquals("srcX is the fixture page x", BG.srcX, draw.srcX);
        assertEquals("srcY is the fixture page y", BG.srcY, draw.srcY);
        assertEquals("srcWidth is the packed width", BG.srcWidth, draw.srcWidth);
        assertEquals("srcHeight is the packed height", BG.srcHeight, draw.srcHeight);
        assertFalse("flipX is false", draw.flipX);
        assertFalse("flipY is false", draw.flipY);

        // The previous batch color is restored after the draw.
        assertColor("the previous batch color is restored",
                previous.r, previous.g, previous.b, previous.a, batch.getColor());
    }

    @Test
    public void endingDrawFollowsTheThreeBranchPlainPlacementRule() {
        // Letterbox is the active branch: both axes and both scales use xScale, which is distinct
        // from scale and yScale so the other branches cannot satisfy the assertions.
        Settings.isFourByThree = false;
        Settings.isLetterbox = true;
        Settings.scale = 1.333f;
        Settings.xScale = 1.792f;
        Settings.yScale = 1.5f;
        assertEndingGeometry(BG.offsetX * Settings.xScale, BG.offsetY * Settings.xScale,
                Settings.xScale, Settings.xScale);

        // Four-by-three is the active branch: y and scaleY use yScale while x/scaleX use scale.
        Settings.isFourByThree = true;
        Settings.isLetterbox = false;
        Settings.scale = 1f;
        Settings.xScale = 1f;
        Settings.yScale = 1.6f;
        assertEndingGeometry(BG.offsetX * Settings.scale, BG.offsetY * Settings.yScale,
                Settings.scale, Settings.yScale);

        // And back to the normal branch, where scale is distinct from both axis scales.
        Settings.isFourByThree = false;
        Settings.isLetterbox = false;
        Settings.scale = 0.75f;
        Settings.xScale = 1.792f;
        Settings.yScale = 1.6f;
        assertEndingGeometry(BG.offsetX * Settings.scale, BG.offsetY * Settings.scale,
                Settings.scale, Settings.scale);
    }

    // ---------------------------------------------------------------- 4. all-or-nothing

    @Test
    public void missingBackgroundRegionFailsOpenWithoutPartialPaint() {
        CapturingBatch batch = newBatch();
        MapSource empty = new MapSource(texture(2048, 2048));
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.setActive(true);

        assertFalse("an unresolved bg region must not paint",
                BackgroundRenderGate.renderEndingBackground(batch, empty));

        assertEquals("no draw may be submitted when bg is missing", 0, batch.draws.size());
        assertEquals("no counter may advance when bg is missing", 0L,
                BackgroundRenderGate.artDrawCount());
        assertEquals("no background-draw evidence when nothing painted", 0L,
                BackgroundOnlyGate.probeSlice().get("backgroundDraw"));
    }

    @Test
    public void unreadableBackgroundRegionFailsOpen() {
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.setActive(true);

        // A region with no page texture is not drawable, so the whole background fails open.
        MapSource noTexture = new MapSource(null);
        noTexture.regions.put("bg", new Sts1AtlasMaterializer.RegionTexture(null, BG.region("bg")));
        assertFalse("a region without a page texture must not paint",
                BackgroundRenderGate.renderEndingBackground(batch, noTexture));
        assertEquals(0, batch.draws.size());

        // A descriptor without a region is likewise not drawable.
        MapSource noDescriptor = new MapSource(texture(2048, 2048));
        noDescriptor.regions.put("bg",
                new Sts1AtlasMaterializer.RegionTexture(noDescriptor.page, null));
        assertFalse("a region without a descriptor must not paint",
                BackgroundRenderGate.renderEndingBackground(batch, noDescriptor));
        assertEquals(0, batch.draws.size());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0L, BackgroundOnlyGate.probeSlice().get("backgroundDraw"));
    }

    @Test
    public void throwingBackgroundSourceFailsOpen() {
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.BottomRegionSource thrower =
                new BackgroundRenderGate.BottomRegionSource() {
                    public Sts1AtlasMaterializer.RegionTexture region(String regionName) {
                        throw new IllegalStateException("boom");
                    }
                };
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.setActive(true);

        assertFalse("a throwing source must report not-painted",
                BackgroundRenderGate.renderEndingBackground(batch, thrower));
        assertEquals(0, batch.draws.size());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0L, BackgroundOnlyGate.probeSlice().get("backgroundDraw"));
    }

    @Test
    public void nullBatchOrSourceFailsOpen() {
        CapturingBatch batch = newBatch();
        MapSource source = bgSource();
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.setActive(true);

        assertFalse("a null batch cannot paint",
                BackgroundRenderGate.renderEndingBackground(null, source));
        assertFalse("a null region source cannot paint",
                BackgroundRenderGate.renderEndingBackground(batch,
                        (BackgroundRenderGate.BottomRegionSource) null));

        assertEquals(0, batch.draws.size());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0L, BackgroundOnlyGate.probeSlice().get("backgroundDraw"));
    }

    // ---------------------------------------------------------------- 5. scene guard

    @Test
    public void endingDrawIsScopedToTheEndingScene() {
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.setActive(true);

        assertFalse("a non-Ending scene instance must fail open",
                BackgroundRenderGate.renderEndingBackground(batch, new Object()));
        assertFalse("a null scene instance must fail open",
                BackgroundRenderGate.renderEndingBackground(batch, (Object) null));
        assertFalse("a null batch must fail open",
                BackgroundRenderGate.renderEndingBackground(null, new Object()));

        assertEquals(0, batch.draws.size());
        assertEquals(0L, BackgroundRenderGate.artDrawCount());
        assertEquals(0L, BackgroundOnlyGate.probeSlice().get("backgroundDraw"));
        // Fail-open means native continues: no suppression evidence may be recorded either.
        assertEquals(0L, BackgroundOnlyGate.probeSlice().get("backgroundSuppression"));
    }

    // ---------------------------------------------------------------- 6. not synthetic

    @Test
    public void endingIsNotTheSyntheticQuadPath() {
        installWhiteSquareTexture();
        CapturingBatch batch = newBatch();
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.ENDING);

        assertFalse("the synthetic path must not claim Ending",
                BackgroundRenderGate.renderSelectedVariant(batch));
        assertEquals("the synthetic path records nothing for Ending",
                0L, BackgroundRenderGate.artDrawCount());
        assertEquals("the synthetic 4-arg quad path was never entered",
                0L, batch.syntheticQuads);
    }

    // ---------------------------------------------------------------- 7. shared decision

    @Test
    public void endingSuppressionStillRequiresTheFilteredBackgroundFamily() {
        installWhiteSquareTexture();
        BackgroundRenderGate.resetForTests();
        BackgroundRenderGate.setVariant(BackgroundRenderGate.Variant.ENDING);

        // The variant is non-off, but suppression must stay gated on the explicit family filter.
        assertFalse("no suppression without the filtered background family",
                BackgroundRenderGate.suppressNativeBackground());
        Sts1VerifyDiagnostics.enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertTrue("suppression engages for the ending variant when the family is filtered",
                BackgroundRenderGate.suppressNativeBackground());
        Sts1VerifyDiagnostics.disableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
        assertFalse("suppression disengages with the family retained",
                BackgroundRenderGate.suppressNativeBackground());
    }

    // ---------------------------------------------------------------- helpers

    /** Paints once through the plain source entry and asserts the submitted placement. */
    private static void assertEndingGeometry(float expectedX, float expectedY,
            float expectedScaleX, float expectedScaleY) {
        CapturingBatch batch = newBatch();
        MapSource source = bgSource();
        BackgroundRenderGate.resetForTests();

        assertTrue("a resolved bg region must paint", BackgroundRenderGate.renderEndingBackground(
                batch, source));
        assertEquals("exactly one native submission", 1, batch.draws.size());
        assertEquals(1L, BackgroundRenderGate.artDrawCount());

        Draw draw = batch.draws.get(0);
        assertEquals("x follows the active placement branch", expectedX, draw.x, DELTA);
        assertEquals("y follows the active placement branch", expectedY, draw.y, DELTA);
        assertEquals("scaleX follows the active placement branch",
                expectedScaleX, draw.scaleX, DELTA);
        assertEquals("scaleY follows the active placement branch",
                expectedScaleY, draw.scaleY, DELTA);
        // The packed size and page rect are branch-independent.
        assertEquals((float) BG.srcWidth, draw.width, 0f);
        assertEquals((float) BG.srcHeight, draw.height, 0f);
        assertEquals(BG.srcX, draw.srcX);
        assertEquals(BG.srcY, draw.srcY);
        assertEquals(BG.srcWidth, draw.srcWidth);
        assertEquals(BG.srcHeight, draw.srcHeight);
    }

    private static MapSource bgSource() {
        Texture page = texture(2048, 2048);
        MapSource source = new MapSource(page);
        source.regions.put("bg", new Sts1AtlasMaterializer.RegionTexture(page, BG.region("bg")));
        return source;
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

        AtlasRegion region(String name) {
            return new AtlasRegion("endingScene/scene.atlas", name,
                    srcX, srcY, srcWidth, srcHeight, 2048, 2048,
                    srcWidth, srcHeight, offsetX, offsetY, 0);
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

    /** One recorded 16-arg draw plus the tint and blend state active at submission time. */
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
        final int blendSrc;
        final int blendDst;

        Draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY, Color color, int blendSrc, int blendDst) {
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
            this.blendSrc = blendSrc;
            this.blendDst = blendDst;
        }
    }

    /**
     * No-GL batch that records the 16-arg pixel-rect overload (the native draw) separately from the
     * synthetic 4-arg quad path, tracks the active tint, and records the active blend function.
     */
    static class CapturingBatch extends SpriteBatch {
        // Unsafe.allocateInstance bypasses the constructor AND field initializers, so newBatch()
        // installs these reflectively; they must not be final.
        List<Draw> draws;
        long syntheticQuads;
        Color current;
        int blendSrc;
        int blendDst;

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
        public void setBlendFunction(int srcFunc, int dstFunc) {
            blendSrc = srcFunc;
            blendDst = dstFunc;
        }

        @Override
        public void draw(Texture texture,
                float x, float y, float originX, float originY,
                float width, float height, float scaleX, float scaleY, float rotation,
                int srcX, int srcY, int srcWidth, int srcHeight,
                boolean flipX, boolean flipY) {
            draws.add(new Draw(texture, x, y, originX, originY, width, height,
                    scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY,
                    current, blendSrc, blendDst));
        }

        @Override
        public void draw(Texture texture, float x, float y, float width, float height) {
            syntheticQuads++;
        }
    }

    private static CapturingBatch newBatch() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            CapturingBatch batch = (CapturingBatch) unsafe.allocateInstance(CapturingBatch.class);
            batch.draws = new ArrayList<Draw>();
            batch.current = Color.WHITE;
            batch.blendSrc = 770;
            batch.blendDst = 771;
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

    private static void assertColor(String message, float r, float g, float b, float a,
            Color actual) {
        assertNotNull(message, actual);
        assertEquals(message + " (r)", r, actual.r, 1e-6f);
        assertEquals(message + " (g)", g, actual.g, 1e-6f);
        assertEquals(message + " (b)", b, actual.b, 1e-6f);
        assertEquals(message + " (a)", a, actual.a, 1e-6f);
    }

    private static Field whiteSquareField() throws NoSuchFieldException, ClassNotFoundException {
        return Class.forName("com.megacrit.cardcrawl.helpers.ImageMaster")
                .getField("WHITE_SQUARE_IMG");
    }

    private static void installWhiteSquareTexture() {
        try {
            whiteSquareField().set(null, texture(1, 1));
        } catch (Exception failure) {
            throw new AssertionError("could not install no-GL white-square texture double", failure);
        }
    }
}

package artframework.sts1.render;

import artframework.component.Rect;
import artframework.sts1.assets.Sts1AtlasMaterializer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * U04-geom. Observable geometry of the private select-card PORTRAIT draw
 * ({@code Sts1SurfaceRenderer.drawSelectPortrait}).
 *
 * <p>Native {@code AbstractCard.renderPortrait}, in the atlas branch ({@code !isLocked &&
 * portrait != null}), calls libGDX's 9-float {@code TextureRegion} overload
 * {@code (TextureRegion;FFFFFFFFF)V} and pushes, verbatim and in this exact order:
 *
 * <pre>
 * x        = current_x - packedWidth  / 2f
 * y        = current_y - packedHeight / 2f + 72f
 * originX  = packedWidth  / 2f
 * originY  = packedHeight / 2f - 72f
 * width    = packedWidth
 * height   = packedHeight
 * scaleX   = drawScale * Settings.scale
 * scaleY   = drawScale * Settings.scale
 * rotation = angle
 * </pre>
 *
 * <p>libGDX scales about {@code (originX, originY)}, so the drawn bottom-left is
 * {@code (x + originX*(1-s), y + originY*(1-s))}. Substituting with the uniform ART card scale
 * {@code s = bounds.width / 512f}, {@code cx = bounds.x + bounds.width/2} and
 * {@code cy = bounds.y + bounds.height/2}:
 *
 * <pre>
 * x' = cx - s * packedWidth/2
 * y' = cy - s * (packedHeight/2 - 72)
 * w' = s * packedWidth
 * h' = s * packedHeight
 * </pre>
 *
 * <p>The atlas branch uses the region's PACKED bounds ({@code width}/{@code height}); the fixed
 * {@code 125/95/250/190} constants belong ONLY to the {@code portraitImg} {@code Texture}
 * branches (locked / no-portrait fallbacks), not to this region branch, so trim metadata
 * ({@code originalWidth}/{@code originalHeight}/{@code offsetX}/{@code offsetY}) must not move
 * the drawn rect.
 *
 * <p>This closes the "descriptor-only coverage" gap: the pre-existing U04 tests assert only the
 * {@code "cards#<artUrl>"} descriptor string, so a wrong rect, wrong UV, dropped draw or
 * stretch-to-box regression would leave every gate green. Here the private method is reached by
 * reflection and a no-GL capturing {@link SpriteBatch} records the actual 9-arg draw arguments.
 *
 * <p>Draw ORDER ({@code renderSelect}: frame, then portrait, then banner) is pinned by review, not
 * by this test: observing it would need a GL/SpriteBatch fixture of the whole surface, which is
 * out of scope and would require reflection into {@code renderSelect}.
 */
public class SelectPortraitGeometryTest {

    private static final float EPS = 0.001f;
    /** Native cards page: {@code cards/cards.atlas} is a 2048x2048 page. */
    private static final int CARDS_PAGE = 2048;
    /** Native cardui page used for the non-{@code cards} fail-open guard. */
    private static final int CARDUI_PAGE = 512;
    /** The real ART select box: hardcoded item w=250, h=350 in SelectDrawPath. */
    private static final Rect PROJECTION_BOX = new Rect(0f, 0f, 250f, 350f);

    @After
    public void tearDown() {
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    /**
     * Native {@code red/attack/strike}: packed xy 997,375 size 248x186, original 250x190,
     * offset 1,4. With s = 250/512 = 0.48828125 and (cx, cy) = (125, 175):
     *
     * <pre>
     * x = 125 - 0.48828125*248/2        = 125 - 60.546875 = 64.453125
     * y = 175 - 0.48828125*(186/2 - 72) = 175 - 10.25390625 = 164.74609375
     * w = 0.48828125*248                = 121.09375
     * h = 0.48828125*186                = 90.8203125
     * </pre>
     *
     * UV is the packed page rect: 997/2048, 375/2048, 1245/2048, 561/2048.
     */
    @Test
    public void strikePortraitDrawsNativePackedRectAndUv() {
        installAtlases();
        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("cards", "red/attack/strike");
        assertNotNull(resolved);
        CapturingBatch sb = newBatch();

        float[] args = drawPortrait(sb, "cards#red/attack/strike", PROJECTION_BOX);

        assertEquals("x = cx - s*packedWidth/2", 64.453125f, args[0], EPS);
        assertEquals("y = cy - s*(packedHeight/2 - 72)", 164.74609375f, args[1], EPS);
        assertEquals("width = s*packedWidth", 121.09375f, args[2], EPS);
        assertEquals("height = s*packedHeight", 90.8203125f, args[3], EPS);

        assertEquals("u is the packed page x", 997f / 2048f, args[4], EPS);
        assertEquals("v is the packed page y", 375f / 2048f, args[5], EPS);
        assertEquals("u2 is the packed right edge", 1245f / 2048f, args[6], EPS);
        assertEquals("v2 is the packed bottom edge", 561f / 2048f, args[7], EPS);

        float[] uv = resolved.region.uvRect();
        for (int i = 0; i < 4; i++) {
            assertEquals("captured UV[" + i + "] must equal the resolved region's packed UV",
                    uv[i], args[4 + i], EPS);
        }
    }

    /**
     * A TRIMMED region proves the branch distinction: packed 200x150 with original 250x190 (and
     * offsets 25,20) must draw from the PACKED size. With s = 0.48828125:
     *
     * <pre>
     * x = 125 - 0.48828125*100 = 76.171875
     * y = 175 - 0.48828125*(75 - 72) = 173.53515625
     * w = 0.48828125*200 = 97.65625      (not s*250 = 122.0703125)
     * h = 0.48828125*150 = 73.2421875    (not s*190 = 92.7734375)
     * </pre>
     *
     * The native atlas branch pushes {@code packedWidth/packedHeight}; the fixed 125/95/250/190
     * constants live only in the {@code portraitImg} Texture branches.
     */
    @Test
    public void trimmedPortraitDrawsPackedBoundsNotOriginal() {
        installAtlases();
        CapturingBatch sb = newBatch();

        float[] args = drawPortrait(sb, "cards#red/attack/trimmed", PROJECTION_BOX);

        assertEquals("x ignores offsetX and uses the packed width", 76.171875f, args[0], EPS);
        assertEquals("y ignores offsetY and uses the packed height", 173.53515625f, args[1], EPS);
        assertEquals("width comes from packedWidth", 97.65625f, args[2], EPS);
        assertEquals("height comes from packedHeight", 73.2421875f, args[3], EPS);
        assertTrue("width must not be derived from originalWidth (s*250 = 122.0703125): " + args[2],
                Math.abs(args[2] - 122.0703125f) > 0.01f);
        assertTrue("height must not be derived from originalHeight (s*190 = 92.7734375): " + args[3],
                Math.abs(args[3] - 92.7734375f) > 0.01f);
    }

    /**
     * Stretch-to-box guard: the portrait is a packed 248x186 piece scaled by the card scale, so it
     * must be strictly smaller than the 250x350 card box on BOTH axes.
     */
    @Test
    public void portraitDoesNotFillTheCardBox() {
        installAtlases();
        CapturingBatch sb = newBatch();

        float[] args = drawPortrait(sb, "cards#red/attack/strike", PROJECTION_BOX);

        assertTrue("portrait width must be smaller than the box: " + args[2],
                args[2] < PROJECTION_BOX.width - EPS);
        assertTrue("portrait height must be smaller than the box: " + args[3],
                args[3] < PROJECTION_BOX.height - EPS);
    }

    /**
     * Fail-open: an empty descriptor, a region absent from the page, and a non-{@code cards}
     * descriptor all draw nothing. The {@code cardui} region is installed and resolvable so the
     * last case exercises the renderer's atlas-key gate rather than a lookup miss.
     */
    @Test
    public void failOpenDescriptorsDrawNothing() {
        installAtlases();
        CapturingBatch sb = newBatch();

        assertNull("empty descriptor must not draw", invokePortrait(sb, "", PROJECTION_BOX));
        assertNull("absent region must not draw",
                invokePortrait(sb, "cards#red/attack/nope", PROJECTION_BOX));
        assertNull("only the cards page is a portrait page",
                invokePortrait(sb, "cardui#512/bg_attack_red", PROJECTION_BOX));
    }

    /** Fail-open: a null batch neither draws nor throws. */
    @Test
    public void nullBatchDrawsNothingAndDoesNotThrow() {
        installAtlases();

        // invokePortrait reports a thrown target as an AssertionError; a null batch must return
        // cleanly with no draw submitted.
        assertNull("a null batch must not draw",
                invokePortrait(null, "cards#red/attack/strike", PROJECTION_BOX));
    }

    /**
     * Invokes the private static {@code drawSelectPortrait(sb, resourceId, bounds)} once and returns
     * the captured 8-float draw arguments (or {@code null} when nothing was drawn). A reflection or
     * target failure is reported as an assertion failure, so "never throws" is observable.
     */
    private static float[] invokePortrait(SpriteBatch sb, String descriptor, Rect bounds) {
        CapturingBatch batch = sb instanceof CapturingBatch ? (CapturingBatch) sb : null;
        if (batch != null) {
            batch.lastArgs = null;
        }
        try {
            Method method = Sts1SurfaceRenderer.class.getDeclaredMethod(
                    "drawSelectPortrait", SpriteBatch.class, String.class, Rect.class);
            method.setAccessible(true);
            method.invoke(null, sb, descriptor, bounds);
        } catch (Exception failure) {
            throw new AssertionError("drawSelectPortrait must not throw for descriptor "
                    + descriptor, failure);
        }
        return batch != null ? batch.lastArgs : null;
    }

    /** Same as {@link #invokePortrait} but asserts that a draw WAS submitted. */
    private static float[] drawPortrait(SpriteBatch sb, String descriptor, Rect bounds) {
        float[] args = invokePortrait(sb, descriptor, bounds);
        assertNotNull("drawSelectPortrait must submit a draw for " + descriptor, args);
        return args;
    }

    /**
     * Live atlases: the native {@code cards} 2048 page with the strike and a trimmed region, plus a
     * {@code cardui} 512 page so the non-{@code cards} fail-open case is resolvable-but-rejected.
     */
    private static void installAtlases() {
        Texture cardsPage = noGlTexture(CARDS_PAGE, CARDS_PAGE);
        TextureAtlas cards = new TextureAtlas();
        addRegion(cards, cardsPage, "red/attack/strike", 997, 375, 248, 186, 250, 190, 1f, 4f);
        addRegion(cards, cardsPage, "red/attack/trimmed", 200, 300, 200, 150, 250, 190, 25f, 20f);

        Texture cardUiPage = noGlTexture(CARDUI_PAGE, CARDUI_PAGE);
        TextureAtlas cardUi = new TextureAtlas();
        addRegion(cardUi, cardUiPage, "512/bg_attack_red", 106, 46, 302, 419, 512, 512, 106f, 46f);

        StubLiveAtlasSource source = new StubLiveAtlasSource();
        source.atlases.put("cards", cards);
        source.atlases.put("cardui", cardUi);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);
    }

    private static void addRegion(
            TextureAtlas atlas,
            Texture page,
            String name,
            int x,
            int y,
            int width,
            int height,
            int originalWidth,
            int originalHeight,
            float offsetX,
            float offsetY) {
        TextureAtlas.AtlasRegion region = atlas.addRegion(name, page, x, y, width, height);
        region.originalWidth = originalWidth;
        region.originalHeight = originalHeight;
        region.offsetX = offsetX;
        region.offsetY = offsetY;
        region.rotate = false;
    }

    /** No-GL capturing batch: records the 9-arg UV-overload draw and never reaches GL. */
    static class CapturingBatch extends SpriteBatch {
        float[] lastArgs;

        CapturingBatch() {
            // Never invoked: instances are created with Unsafe.allocateInstance so no GL/asset state
            // is needed; the constructor exists only so the subclass compiles against SpriteBatch.
            super(1);
        }

        @Override
        public void setColor(Color color) {
            // Deliberately skip super: the renderer only needs a settable tint, and the real method
            // would pack a float from GL-backed state on the no-GL double.
        }

        @Override
        public void draw(Texture texture, float x, float y, float width, float height,
                float u, float v, float u2, float v2) {
            // Record only; skipping super avoids the real (absent) GL texture bind path.
            lastArgs = new float[] {x, y, width, height, u, v, u2, v2};
        }
    }

    private static CapturingBatch newBatch() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            return (CapturingBatch) unsafe.allocateInstance(CapturingBatch.class);
        } catch (Exception failure) {
            throw new AssertionError("could not build no-GL CapturingBatch", failure);
        }
    }

    private static Texture noGlTexture(final int width, final int height) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Texture texture =
                    (Texture) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(Texture.class);
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

    private static final class StubLiveAtlasSource implements Sts1AtlasMaterializer.LiveAtlasSource {
        final Map<String, TextureAtlas> atlases = new java.util.LinkedHashMap<String, TextureAtlas>();

        public TextureAtlas atlas(String atlasKey) {
            return atlases.get(atlasKey);
        }
    }
}

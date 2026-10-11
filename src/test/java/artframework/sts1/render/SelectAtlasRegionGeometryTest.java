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
import static org.junit.Assert.assertTrue;

/**
 * U02d2-geom. Observable geometry of the private select-card atlas-region draw
 * ({@code Sts1SurfaceRenderer.drawSelectAtlasRegion}).
 *
 * <p>Native {@code SingleCardViewPopup.renderHelper} composites each packed {@code cardui} 512-page
 * region into the card's 512 canvas at the region's OWN offset plus packed size, not stretched to
 * the whole card box:
 *
 * <pre>
 * sb.draw(img,
 *     x + img.offsetX - img.originalWidth/2.0f,
 *     y + img.offsetY - img.originalHeight/2.0f,
 *     img.originalWidth/2.0f - img.offsetX,   // originX
 *     img.originalHeight/2.0f - img.offsetY,  // originY
 *     img.packedWidth, img.packedHeight, scale, scale, 0.0f);
 * </pre>
 *
 * <p>where (x,y) is the card CENTER and {@code scale = drawScale*Settings.scale}. libGDX scales
 * about the origin, so the on-screen bottom-left is {@code (x + originX*(1-s), y + originY*(1-s))}.
 * With {@code s = bounds.width / region.originalWidth} (UNIFORM, width only — the native drawScale
 * scales both axes equally), {@code cx = bounds.x + bounds.width/2} and
 * {@code cy = bounds.y + bounds.height/2} (the ART select item's x/y is the card CENTER, and
 * {@code renderSelect} builds {@code Rect(item.x - item.w/2, item.y - item.h/2, item.w, item.h)} so
 * the box center equals the item x/y):
 *
 * <pre>
 * rect.x = cx - s * (region.originalWidth/2f  - region.offsetX)
 * rect.y = cy - s * (region.originalHeight/2f - region.offsetY)
 * rect.w = s * region.width
 * rect.h = s * region.height
 * </pre>
 *
 * <p>The cardui regions are unrotated, so {@code width}/{@code height} are the packed page
 * footprint. Metadata (all {@code originalWidth}/{@code originalHeight} = 512):
 * <ul>
 *   <li>{@code 512/bg_attack_red}: packed 302x419, offset (106,46)</li>
 *   <li>{@code 512/banner_common}: packed 324x77, offset (94,378)</li>
 *   <li>{@code 512/card_red_orb}: packed 72x71, offset (87,412)</li>
 * </ul>
 *
 * <p>This is a behavioral test of the ACTUAL draw arguments: the private renderer method is reached
 * by reflection and a no-GL capturing {@link SpriteBatch} records the 9-arg UV-overload arguments
 * the renderer passes. The ORB case is the key non-tautological guard: on the pre-fix code every
 * {@code #} region is stretched to the whole box, so the orb rect equals the full box (250x350
 * here) instead of the small offset piece.
 */
public class SelectAtlasRegionGeometryTest {

    private static final float EPS = 0.001f;
    private static final int PAGE = 512;

    /** The real ART select box: SelectDrawPath.buildFromProjection hardcodes item w=250, h=350. */
    private static final Rect PROJECTION_BOX = new Rect(0f, 0f, 250f, 350f);
    /** The square box at s=1 that reproduces the raw native offsets/packed sizes exactly. */
    private static final Rect SQUARE_BOX = new Rect(0f, 0f, 512f, 512f);

    @After
    public void tearDown() {
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    /**
     * The energy orb must be drawn at its own 72x71 packed piece placed at the native offset — NOT
     * stretched to the full card box.
     */
    @Test
    public void orbRegionDrawsAtNativeOffsetAndPackedSizeNotFullBox() {
        installCardUiAtlas();
        CapturingBatch sb = newBatch();

        float[] rect = drawRegion(sb, "cardui#512/card_red_orb", PROJECTION_BOX);

        assertEquals("orb x uses the uniform width scale and its own offset",
                42.48046875f, rect[0], EPS);
        assertEquals("orb y uses the uniform width scale and its own offset",
                251.171875f, rect[1], EPS);
        assertEquals("orb width is the packed width scaled, not the box width",
                35.15625f, rect[2], EPS);
        assertEquals("orb height is the packed height scaled, not the box height",
                34.66796875f, rect[3], EPS);
        assertTrue("the orb must NOT fill the card box width: " + rect[2],
                rect[2] < PROJECTION_BOX.width - EPS);
        assertTrue("the orb must NOT fill the card box height: " + rect[3],
                rect[3] < PROJECTION_BOX.height - EPS);
    }

    /**
     * The base background and the banner also draw at their own offset + packed size under the same
     * uniform width scale.
     */
    @Test
    public void baseAndBannerRegionsDrawAtNativeOffsetAndPackedSize() {
        installCardUiAtlas();
        CapturingBatch sb = newBatch();

        float[] bg = drawRegion(sb, "cardui#512/bg_attack_red", PROJECTION_BOX);
        assertEquals(51.7578125f, bg[0], EPS);
        assertEquals(72.4609375f, bg[1], EPS);
        assertEquals(147.4609375f, bg[2], EPS);
        assertEquals(204.58984375f, bg[3], EPS);

        float[] banner = drawRegion(sb, "cardui#512/banner_common", PROJECTION_BOX);
        assertEquals(45.8984375f, banner[0], EPS);
        assertEquals(234.5703125f, banner[1], EPS);
        assertEquals(158.203125f, banner[2], EPS);
        assertEquals(37.59765625f, banner[3], EPS);
    }

    /**
     * At a square 512x512 box the uniform scale is exactly 1, so the drawn rect is the raw native
     * placement: {@code (offsetX, offsetY, packedWidth, packedHeight)}.
     */
    @Test
    public void squareBoxDrawsRegionsAtRawPackedPageRects() {
        installCardUiAtlas();
        CapturingBatch sb = newBatch();

        float[] bg = drawRegion(sb, "cardui#512/bg_attack_red", SQUARE_BOX);
        assertEquals(106f, bg[0], EPS);
        assertEquals(46f, bg[1], EPS);
        assertEquals(302f, bg[2], EPS);
        assertEquals(419f, bg[3], EPS);

        float[] banner = drawRegion(sb, "cardui#512/banner_common", SQUARE_BOX);
        assertEquals(94f, banner[0], EPS);
        assertEquals(378f, banner[1], EPS);
        assertEquals(324f, banner[2], EPS);
        assertEquals(77f, banner[3], EPS);

        float[] orb = drawRegion(sb, "cardui#512/card_red_orb", SQUARE_BOX);
        assertEquals("at s=1 the orb sits at its raw offset", 87f, orb[0], EPS);
        assertEquals(412f, orb[1], EPS);
        assertEquals(72f, orb[2], EPS);
        assertEquals(71f, orb[3], EPS);
        assertTrue("the orb must NOT fill the square box: " + orb[2] + "x" + orb[3],
                orb[2] < SQUARE_BOX.width - EPS && orb[3] < SQUARE_BOX.height - EPS);
    }

    /**
     * Invokes the private static {@code drawSelectAtlasRegion} once and returns the captured draw
     * rect {@code {x, y, width, height}}.
     */
    private static float[] drawRegion(SpriteBatch sb, String descriptor, Rect box) {
        CapturingBatch batch = (CapturingBatch) sb;
        batch.lastArgs = null;
        try {
            Method method = Sts1SurfaceRenderer.class.getDeclaredMethod(
                    "drawSelectAtlasRegion", SpriteBatch.class, String.class, Rect.class);
            method.setAccessible(true);
            method.invoke(null, batch, descriptor, box);
        } catch (Exception failure) {
            throw new AssertionError("could not invoke drawSelectAtlasRegion", failure);
        }
        assertNotNull("drawSelectAtlasRegion must submit a draw for " + descriptor,
                batch.lastArgs);
        return batch.lastArgs;
    }

    /**
     * Live {@code cardui} atlas whose three regions carry the native 512-page metadata: all
     * {@code original*} = 512, unrotated, packed bounds valid within the 512x512 page.
     */
    private static void installCardUiAtlas() {
        Texture page = noGlTexture(PAGE, PAGE);
        TextureAtlas atlas = new TextureAtlas();
        addRegion(atlas, page, "512/bg_attack_red", 106, 46, 302, 419, 106f, 46f);
        addRegion(atlas, page, "512/banner_common", 94, 378, 324, 77, 94f, 378f);
        addRegion(atlas, page, "512/card_red_orb", 87, 412, 72, 71, 87f, 412f);
        StubLiveAtlasSource source = new StubLiveAtlasSource();
        source.atlases.put("cardui", atlas);
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
            float offsetX,
            float offsetY) {
        TextureAtlas.AtlasRegion region = atlas.addRegion(name, page, x, y, width, height);
        region.originalWidth = PAGE;
        region.originalHeight = PAGE;
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

package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * No-GL tests for {@link Sts1GdxAtlasRegions} (Slice D host adapter).
 *
 * <p>Headless page textures are built with {@code sun.misc.Unsafe.allocateInstance(Texture.class)}
 * (mirroring {@code Sts1AssetMaterializerTest.seedValidEnergyAttribution}) and their package-private
 * {@code data} field is pointed at a {@link TextureData} proxy, because {@code Texture.getWidth()}
 * delegates to {@code data.getWidth()} and libGDX {@code Texture} exposes no width field. Any GL
 * touch (for example {@code getTextureObjectHandle()}) would throw on these doubles, so a passing
 * conversion proves the adapter stays GL-free.
 */
public class Sts1GdxAtlasRegionsTest {

    private static final float EPS = 1e-6f;

    @Test
    public void unrotatedRegionMapsPackedBoundsAndUv() {
        Texture texture = newTexture(256, 256);
        // libGDX load() builds a region via new AtlasRegion(texture, left, top, width, height).
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 10, 20, 64, 48);
        gdx.name = "unrot";
        gdx.originalWidth = 64;
        gdx.originalHeight = 48;

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertNotNull(region);
        assertEquals("", region.page);
        assertEquals("unrot", region.name);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        assertEquals(64, region.width);
        assertEquals(48, region.height);
        assertEquals(256, region.pageWidth);
        assertEquals(256, region.pageHeight);
        assertEquals(0, region.degrees);
        assertFalse(region.rotated());
        assertEquals(64, region.displayWidth());
        assertEquals(48, region.displayHeight());
        assertArrayEquals(gdxUv(gdx), region.uvRect(), EPS);
    }

    @Test
    public void rotatedRegionKeepsPackedBoundsAndRecoversDisplaySize() {
        Texture texture = newTexture(256, 256);
        // "size: 64,48 rotate:true" at (10,20): libGDX load() calls ctor(10,20,48,64) and sets
        // rotate = true. getRegionWidth/Height thus report the packed footprint 48x64.
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 10, 20, 48, 64);
        gdx.rotate = true;
        gdx.name = "rot";
        gdx.originalWidth = 64;
        gdx.originalHeight = 48;

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertNotNull(region);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        // Packed page bounds copied through unchanged (NOT swapped).
        assertEquals(48, region.width);
        assertEquals(64, region.height);
        assertEquals(90, region.degrees);
        assertTrue(region.rotated());
        // Neutral display swap recovers the libGDX visual size.
        assertEquals(64, region.displayWidth());
        assertEquals(48, region.displayHeight());
        assertEquals((int) gdx.getRotatedPackedWidth(), region.displayWidth());
        assertEquals((int) gdx.getRotatedPackedHeight(), region.displayHeight());
        // UV box is the sampled packed rect, equal to the gdx UVs (not a swapped box).
        assertArrayEquals(gdxUv(gdx), region.uvRect(), EPS);
    }

    @Test
    public void flippedRegionStaysFlipInvariantAndValid() {
        Texture texture = newTexture(256, 256);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 10, 20, 64, 48);
        gdx.name = "shared";
        gdx.originalWidth = 64;
        gdx.originalHeight = 48;

        AtlasRegion unflipped = Sts1GdxAtlasRegions.fromGdx(gdx);
        assertTrue(unflipped.valid());
        int originalLeft = unflipped.x;
        int originalTop = unflipped.y;

        // A native sibling flips the SHARED region in place: u/u2 swap (and offsetX mutates), so
        // getRegionX() jumps to the right edge while getRegionWidth() stays the packed footprint.
        gdx.flip(true, false);
        assertTrue("the native flip really moved the sampled left edge",
                gdx.getRegionX() > originalLeft);

        AtlasRegion horizontal = Sts1GdxAtlasRegions.fromGdx(gdx);
        assertNotNull("a flipped region still yields a descriptor", horizontal);
        assertTrue("a flip-polluted region must stay valid", horizontal.valid());
        assertEquals("the flip-invariant top-left is the ORIGINAL left edge",
                originalLeft, horizontal.x);
        assertEquals("the packed width is unchanged by the flip",
                originalLeft + 64, horizontal.x + horizontal.width);
        assertEquals(64, horizontal.width);
        assertEquals(48, horizontal.height);

        // Vertical flip: v/v2 swap, y must stay the original top edge and stay valid.
        gdx.flip(false, false); // undo the horizontal flip on the shared region
        gdx.flip(false, true);
        AtlasRegion vertical = Sts1GdxAtlasRegions.fromGdx(gdx);
        assertNotNull(vertical);
        assertTrue("a vertically flip-polluted region must stay valid", vertical.valid());
        assertEquals("the flip-invariant top is the ORIGINAL top edge", originalTop, vertical.y);
        assertEquals(48, vertical.height);
        assertEquals(64, vertical.width);
    }

    @Test
    public void unflippedRegionLeftEdgeMatchesRegionXExactly() {
        // The min-of-UV-corners rule must be byte-identical to getRegionX()/getRegionY() when the
        // region is not flipped.
        Texture texture = newTexture(256, 256);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 33, 41, 64, 48);

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertEquals(gdx.getRegionX(), region.x);
        assertEquals(gdx.getRegionY(), region.y);
        assertEquals(gdx.getRegionWidth(), region.width);
        assertEquals(gdx.getRegionHeight(), region.height);
    }

    @Test
    public void nullRegionAndNullTextureReturnNull() {
        assertNull(Sts1GdxAtlasRegions.fromGdx(null));

        Texture texture = newTexture(128, 128);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 0, 0, 16, 16);
        gdx.setTexture(null);
        assertNull(Sts1GdxAtlasRegions.fromGdx(gdx));
    }

    @Test
    public void zeroSizedTextureDoesNotThrowAndIsInvalid() {
        Texture texture = newTexture(0, 0);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 0, 0, 16, 16);

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertNotNull(region);
        assertEquals(0, region.pageWidth);
        assertEquals(0, region.pageHeight);
        assertFalse(region.valid());
    }

    @Test
    public void trimmedRegionReportsTrimMetadata() {
        Texture texture = newTexture(256, 256);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 30, 40, 64, 48);
        gdx.name = "trimmed";
        gdx.originalWidth = 100;
        gdx.originalHeight = 80;
        gdx.offsetX = 5f;
        gdx.offsetY = 7f;

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertTrue(region.trimmed());
        assertArrayEquals(new float[] { 5f, 7f, 100f, 80f }, region.trimmedRect(), EPS);
        assertEquals(100, region.originalWidth);
        assertEquals(80, region.originalHeight);
    }

    @Test
    public void nonPositiveOriginalDefaultsToDisplayDims() {
        Texture texture = newTexture(256, 256);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 0, 0, 64, 48);
        gdx.originalWidth = 0;
        gdx.originalHeight = -1;

        AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertFalse(region.trimmed());
        assertEquals(64, region.originalWidth);
        assertEquals(48, region.originalHeight);
        assertArrayEquals(new float[] { 0f, 0f, 64f, 48f }, region.trimmedRect(), EPS);
    }

    @Test
    public void outputIsNeutralTypeAndNeverTouchesGl() {
        Texture texture = newTexture(256, 256);
        TextureAtlas.AtlasRegion gdx = new TextureAtlas.AtlasRegion(texture, 1, 2, 3, 4);

        Object result = Sts1GdxAtlasRegions.fromGdx(gdx);

        assertEquals(AtlasRegion.class, result.getClass());
        assertFalse(result instanceof com.badlogic.gdx.graphics.g2d.TextureRegion);
    }

    private static float[] gdxUv(TextureAtlas.AtlasRegion region) {
        return new float[] { region.getU(), region.getV(), region.getU2(), region.getV2() };
    }

    private static Texture newTexture(final int width, final int height) {
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
}

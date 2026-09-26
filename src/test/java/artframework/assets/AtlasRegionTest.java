package artframework.assets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pure tests for {@link AtlasRegion} (Slice A atlas resource bridge). */
public class AtlasRegionTest {

    private static final float EPS = 1e-6f;

    @Test
    public void unrotatedRegionSourceAndUvInsideLargerPage() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "card", 16, 8, 64, 32, 256, 128, 64, 32, 0, 0, 0);

        assertFalse(region.rotated());
        assertFalse(region.trimmed());
        assertArrayEquals(new float[] { 16f, 8f, 64f, 32f }, region.sourceRect(), EPS);
        assertArrayEquals(
                new float[] { 16f / 256f, 8f / 128f, 80f / 256f, 40f / 128f },
                region.uvRect(),
                EPS);
        assertEquals(64, region.displayWidth());
        assertEquals(32, region.displayHeight());
    }

    @Test
    public void rotatedRegionSwapsDisplayDimensions() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "tile", 10, 20, 64, 32, 256, 256, 64, 32, 0, 0, 90);

        assertTrue(region.rotated());
        assertEquals(32, region.displayWidth());
        assertEquals(64, region.displayHeight());
        assertArrayEquals(new float[] { 10f, 20f, 64f, 32f }, region.sourceRect(), EPS);
    }

    @Test
    public void trimmedRegionReportsOriginalPlacement() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "sprite", 4, 6, 48, 24, 256, 256, 100, 64, 7, 9, 0);

        assertTrue(region.trimmed());
        assertArrayEquals(new float[] { 7f, 9f, 100f, 64f }, region.trimmedRect(), EPS);
    }

    @Test
    public void originalDimensionsDefaultToPackedWhenNonPositive() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "sprite", 0, 0, 30, 40, 128, 128, 0, -5, 0, 0, 0);

        assertEquals(30, region.originalWidth);
        assertEquals(40, region.originalHeight);
        assertFalse(region.trimmed());
        assertArrayEquals(new float[] { 0f, 0f, 30f, 40f }, region.trimmedRect(), EPS);
    }

    @Test
    public void rotatedNonSquareUntrimmedRegionIsNotTrimmedAndDefaultsToDisplayDims() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "tile", 10, 20, 64, 32, 256, 256, 0, 0, 0, 0, 90);

        assertTrue(region.rotated());
        assertEquals(32, region.displayWidth());
        assertEquals(64, region.displayHeight());
        assertFalse(region.trimmed());
        assertEquals(32, region.originalWidth);
        assertEquals(64, region.originalHeight);
        assertArrayEquals(new float[] { 0f, 0f, 32f, 64f }, region.trimmedRect(), EPS);
    }

    @Test
    public void rotatedTrimmedRegionReportsTrimmedInDisplayOrientation() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "tile", 10, 20, 64, 32, 256, 256, 40, 80, 5, 6, 90);

        assertTrue(region.rotated());
        assertTrue(region.trimmed());
        assertArrayEquals(new float[] { 5f, 6f, 40f, 80f }, region.trimmedRect(), EPS);
    }

    @Test
    public void nearIntegerMaxValueRegionDoesNotOverflowAndStaysClampedOrdered() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "huge", Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1,
                16, 16, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, 0, 0);

        assertFalse(region.valid());
        float[] uv = region.uvRect();
        for (float value : uv) {
            assertTrue(value >= 0f && value <= 1f);
        }
        assertTrue(uv[0] <= uv[2]);
        assertTrue(uv[1] <= uv[3]);
    }

    @Test
    public void regionAtPageEdgeClampsUvToUnitBounds() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "edge", 224, 96, 32, 32, 256, 128, 32, 32, 0, 0, 0);

        assertTrue(region.valid());
        assertArrayEquals(new float[] { 224f / 256f, 96f / 128f, 1f, 1f }, region.uvRect(), EPS);
    }

    @Test
    public void overflowingRegionReturnsClampedUvWithoutThrowing() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "overflow", 200, 100, 200, 200, 256, 128, 200, 200, 0, 0, 0);

        assertFalse(region.valid());
        float[] uv = region.uvRect();
        for (float value : uv) {
            assertTrue(value >= 0f && value <= 1f);
        }
        assertArrayEquals(new float[] { 200f / 256f, 100f / 128f, 1f, 1f }, uv, EPS);
    }

    @Test
    public void invalidPageSizeReturnsFailSafeFullTextureUv() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "broken", 0, 0, 16, 16, 0, 0, 0, 0, 0, 0, 0);

        assertFalse(region.valid());
        assertArrayEquals(new float[] { 0f, 0f, 1f, 1f }, region.uvRect(), EPS);
    }

    @Test
    public void nullPageAndNameNormalizeToEmptyStrings() {
        AtlasRegion region = new AtlasRegion(
                null, null, 0, 0, 8, 8, 32, 32, 8, 8, 0, 0, 0);

        assertEquals("", region.page);
        assertEquals("", region.name);
    }

    @Test
    public void validRegionReportsValid() {
        AtlasRegion region = AtlasRegion.of("page.png", "ok", 0, 0, 32, 32, 64, 64);

        assertTrue(region.valid());
    }

    @Test
    public void uvSourceRectForNormalRegionIsOriginPlusPositiveExtent() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "card", 16, 8, 64, 32, 256, 128, 64, 32, 0, 0, 0);

        float[] rect = region.uvSourceRect();

        assertArrayEquals(
                new float[] { 16f / 256f, 8f / 128f, 64f / 256f, 32f / 128f }, rect, EPS);
        assertTrue(rect[2] > 0f && rect[3] > 0f);
        assertFiniteInUnitRange(rect);
    }

    @Test
    public void uvSourceRectAtPageEdgeClampsAndKeepsExtent() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "edge", 224, 96, 32, 32, 256, 128, 32, 32, 0, 0, 0);

        assertTrue(region.valid());
        assertArrayEquals(
                new float[] { 224f / 256f, 96f / 128f, 32f / 256f, 32f / 128f },
                region.uvSourceRect(),
                EPS);
    }

    @Test
    public void uvSourceRectForInvalidPageSizeIsFailSafeFullTexture() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "broken", 0, 0, 16, 16, 0, 0, 0, 0, 0, 0, 0);

        assertFalse(region.valid());
        assertArrayEquals(new float[] { 0f, 0f, 1f, 1f }, region.uvSourceRect(), EPS);
    }

    @Test
    public void uvSourceRectWidthAndHeightAreDifferencesNotAbsoluteValues() {
        AtlasRegion region = new AtlasRegion(
                "page.png", "card", 16, 8, 64, 32, 256, 128, 64, 32, 0, 0, 0);

        float[] rect = region.uvSourceRect();

        // Must be u2 - u / v2 - v, not the absolute normalized right/bottom edges.
        assertEquals(80f / 256f - 16f / 256f, rect[2], EPS);
        assertEquals(40f / 128f - 8f / 128f, rect[3], EPS);
        assertTrue("width must not be the absolute u2", rect[2] < 1f - EPS);
        assertTrue("height must not be the absolute v2", rect[3] < 1f - EPS);
    }

    private static void assertFiniteInUnitRange(float[] rect) {
        for (float value : rect) {
            assertFalse("must be finite", Float.isNaN(value) || Float.isInfinite(value));
            assertTrue("must be clamped into [0,1]", value >= 0f && value <= 1f);
        }
    }
}

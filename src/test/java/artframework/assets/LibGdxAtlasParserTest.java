package artframework.assets;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LibGdxAtlasParserTest {

    @Test
    public void mapsSinglePageAndRegion() {
        String text = "page.png\n"
                + "size: 512,512\n"
                + "format: RGBA8888\n"
                + "filter: Linear,Linear\n"
                + "repeat: none\n"
                + "attack.common\n"
                + "  rotate: false\n"
                + "  xy: 1, 2\n"
                + "  size: 64, 48\n"
                + "  orig: 64, 48\n"
                + "  offset: 0, 0\n"
                + "  index: -1\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(text);

        assertEquals(1, regions.size());
        AtlasRegion r = regions.get(0);
        assertEquals("page.png", r.page);
        assertEquals("attack.common", r.name);
        assertEquals(1, r.x);
        assertEquals(2, r.y);
        assertEquals(64, r.width);
        assertEquals(48, r.height);
        assertEquals(512, r.pageWidth);
        assertEquals(512, r.pageHeight);
        assertEquals(0, r.degrees);
        assertFalse(r.rotated());
        assertFalse(r.trimmed());
        assertTrue(r.valid());
        float[] uv = r.uvRect();
        assertEquals(1f / 512f, uv[0], 1e-6f);
        assertEquals(2f / 512f, uv[1], 1e-6f);
        assertEquals(65f / 512f, uv[2], 1e-6f);
        assertEquals(50f / 512f, uv[3], 1e-6f);
    }

    @Test
    public void rotateTrueSwapsDisplayAndDefaultsOrigToDisplayOrientation() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "region\n"
                + "  rotate: true\n"
                + "  xy: 0, 0\n"
                + "  size: 64, 48\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertEquals(90, r.degrees);
        assertTrue(r.rotated());
        assertEquals(48, r.displayWidth());
        assertEquals(64, r.displayHeight());
        assertEquals(48, r.originalWidth);
        assertEquals(64, r.originalHeight);
        assertFalse(r.trimmed());
    }

    @Test
    public void trimsAreReported() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "region\n"
                + "  xy: 10, 20\n"
                + "  size: 20, 30\n"
                + "  orig: 40, 50\n"
                + "  offset: 5, 6\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertTrue(r.trimmed());
        float[] trimmed = r.trimmedRect();
        assertEquals(5f, trimmed[0], 0f);
        assertEquals(6f, trimmed[1], 0f);
        assertEquals(40f, trimmed[2], 0f);
        assertEquals(50f, trimmed[3], 0f);
    }

    @Test
    public void defaultsOriginalAndOffsetWhenAbsent() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 20, 30\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertEquals(0, r.offsetX);
        assertEquals(0, r.offsetY);
        assertEquals(20, r.originalWidth);
        assertEquals(30, r.originalHeight);
        assertFalse(r.trimmed());
    }

    @Test
    public void preservesOrderAcrossTwoPages() {
        String text = "a.png\n"
                + "size: 10,10\n"
                + "first\n"
                + "  xy: 0, 0\n"
                + "  size: 1, 1\n"
                + "second\n"
                + "  xy: 1, 1\n"
                + "  size: 1, 1\n"
                + "b.png\n"
                + "size: 10,10\n"
                + "third\n"
                + "  xy: 0, 0\n"
                + "  size: 1, 1\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(text);

        assertEquals(3, regions.size());
        assertEquals("first", regions.get(0).name);
        assertEquals("a.png", regions.get(0).page);
        assertEquals("second", regions.get(1).name);
        assertEquals("a.png", regions.get(1).page);
        assertEquals("third", regions.get(2).name);
        assertEquals("b.png", regions.get(2).page);
    }

    @Test
    public void zeroPageSizeDoesNotThrowAndUsesFailSafeUv() {
        String text = "page.png\n"
                + "size: 0,0\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 4, 4\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertFalse(r.valid());
        float[] uv = r.uvRect();
        assertEquals(0f, uv[0], 0f);
        assertEquals(0f, uv[1], 0f);
        assertEquals(1f, uv[2], 0f);
        assertEquals(1f, uv[3], 0f);
    }

    @Test
    public void toleratesExtraWhitespaceAndBlankLines() throws Exception {
        String text = "\n  page.png  \n"
                + "\t size :  200, 100 \n"
                + "\n"
                + "  hero \n"
                + "\t xy : 3 , 4 \n"
                + "   size: 10 ,  20 \n"
                + "  orig  :  10,20\n"
                + "  offset : 0 ,0\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(new StringReader(text));

        assertEquals(1, regions.size());
        AtlasRegion r = regions.get(0);
        assertEquals("page.png", r.page);
        assertEquals("hero", r.name);
        assertEquals(3, r.x);
        assertEquals(4, r.y);
        assertEquals(10, r.width);
        assertEquals(20, r.height);
        assertEquals(200, r.pageWidth);
        assertEquals(100, r.pageHeight);
    }

    @Test
    public void missingXyNamesTheRegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "broken\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region broken missing xy", e.getMessage());
        }
    }

    @Test
    public void missingSizeNamesTheRegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "broken\n"
                + "  xy: 0, 0\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region broken missing size", e.getMessage());
        }
    }

    @Test
    public void roundTripsExactMaterializerLegacyShape() {
        String text = "hero.png\n"
                + "size: 0,0\nformat: RGBA8888\nfilter: Linear,Linear\nrepeat: none\n"
                + "attack.common\n"
                + "  rotate: false\n"
                + "  xy: 1, 2\n"
                + "  size: 64, 48\n"
                + "  orig: 64, 48\n"
                + "  offset: 0, 0\n"
                + "  index: -1\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(text);

        assertEquals(1, regions.size());
        AtlasRegion r = regions.get(0);
        assertEquals("hero.png", r.page);
        assertEquals("attack.common", r.name);
        assertEquals(1, r.x);
        assertEquals(2, r.y);
        assertEquals(64, r.width);
        assertEquals(48, r.height);
        assertEquals(64, r.originalWidth);
        assertEquals(48, r.originalHeight);
        assertEquals(0, r.offsetX);
        assertEquals(0, r.offsetY);
        assertEquals(0, r.degrees);
        float[] uv = r.uvRect();
        assertEquals(0f, uv[0], 0f);
        assertEquals(0f, uv[1], 0f);
        assertEquals(1f, uv[2], 0f);
        assertEquals(1f, uv[3], 0f);
    }

    @Test
    public void rejectsNullInputs() {
        try {
            LibGdxAtlasParser.parse((String) null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("atlasText required", e.getMessage());
        }
        try {
            LibGdxAtlasParser.parse((java.io.Reader) null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("reader required", e.getMessage());
        } catch (java.io.IOException e) {
            fail("unexpected IOException: " + e.getMessage());
        }
    }

    @Test
    public void secondPageWithoutImageExtensionStartsNewPage() {
        String text = "first.png\n"
                + "size: 100,100\n"
                + "a\n"
                + "  xy: 0, 0\n"
                + "  size: 10, 10\n"
                + "secondpage\n"
                + "size: 200,400\n"
                + "b\n"
                + "  xy: 10, 20\n"
                + "  size: 10, 10\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(text);

        assertEquals(2, regions.size());
        assertEquals("first.png", regions.get(0).page);
        assertEquals("secondpage", regions.get(1).page);
        assertEquals(200, regions.get(1).pageWidth);
        assertEquals(400, regions.get(1).pageHeight);
        float[] uv = regions.get(1).uvRect();
        assertEquals(10f / 200f, uv[0], 1e-6f);
        assertEquals(20f / 400f, uv[1], 1e-6f);
        assertEquals(20f / 200f, uv[2], 1e-6f);
        assertEquals(30f / 400f, uv[3], 1e-6f);
    }

    @Test
    public void regionNameEndingInPngWithIndentedPropertiesIsARegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "icon.png\n"
                + "  xy: 1, 1\n"
                + "  size: 8, 8\n";

        List<AtlasRegion> regions = LibGdxAtlasParser.parse(text);

        assertEquals(1, regions.size());
        assertEquals("page.png", regions.get(0).page);
        assertEquals("icon.png", regions.get(0).name);
    }

    @Test
    public void numericRotateDegreesIsAccepted() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "region\n"
                + "  rotate: 90\n"
                + "  xy: 0, 0\n"
                + "  size: 64, 48\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertEquals(90, r.degrees);
        assertTrue(r.rotated());
        assertEquals(48, r.displayWidth());
        assertEquals(64, r.displayHeight());
    }

    @Test
    public void nonNumericXyNamesTheRegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "broken\n"
                + "  xy: a, b\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region broken invalid value: a, b", e.getMessage());
        }
    }

    @Test
    public void wrongArityXyNamesTheRegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "broken\n"
                + "  xy: 1, 2, 3\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region broken invalid value: 1, 2, 3", e.getMessage());
        }
    }

    @Test
    public void nonNumericOrigNamesTheRegion() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "broken\n"
                + "  xy: 0, 0\n"
                + "  size: 4, 4\n"
                + "  orig: x, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region broken invalid value: x, 4", e.getMessage());
        }
    }

    @Test
    public void zeroOrigComponentKeepsOtherOriginalDimension() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 20, 30\n"
                + "  orig: 0, 50\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertEquals(20, r.originalWidth);
        assertEquals(50, r.originalHeight);
        assertTrue(r.trimmed());
    }

    @Test
    public void propertiesDoNotLeakBetweenRegions() {
        String text = "page.png\n"
                + "size: 100,100\n"
                + "first\n"
                + "  xy: 5, 6\n"
                + "  size: 4, 4\n"
                + "second\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("region second missing xy", e.getMessage());
        }
    }

    @Test
    public void malformedPageSizeNamesThePage() {
        String text = "page.png\n"
                + "size: a, b\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("page page.png invalid value: a, b", e.getMessage());
        }
    }

    @Test
    public void wrongArityPageSizeNamesThePage() {
        String text = "page.png\n"
                + "size: 1, 2, 3\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 4, 4\n";
        try {
            LibGdxAtlasParser.parse(text);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("page page.png invalid value: 1, 2, 3", e.getMessage());
        }
    }

    @Test
    public void pageWithoutSizeDefaultsToZeroDimensions() {
        String text = "page.png\n"
                + "region\n"
                + "  xy: 0, 0\n"
                + "  size: 4, 4\n";

        AtlasRegion r = LibGdxAtlasParser.parse(text).get(0);

        assertEquals(0, r.pageWidth);
        assertEquals(0, r.pageHeight);
        assertFalse(r.valid());
        float[] uv = r.uvRect();
        assertEquals(0f, uv[0], 0f);
        assertEquals(0f, uv[1], 0f);
        assertEquals(1f, uv[2], 0f);
        assertEquals(1f, uv[3], 0f);
    }

    @Test
    public void emptyOrWhitespaceInputReturnsEmptyList() throws Exception {
        assertTrue(LibGdxAtlasParser.parse("").isEmpty());
        assertTrue(LibGdxAtlasParser.parse("   \n\t\n  \n").isEmpty());
        assertTrue(LibGdxAtlasParser.parse(new StringReader("\n\n")).isEmpty());
    }
}

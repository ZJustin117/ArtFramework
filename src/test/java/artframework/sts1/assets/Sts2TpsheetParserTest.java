package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import org.junit.Test;

import java.io.File;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure, no-GL tests for the TexturePacker {@code .tpsheet} JSON parser. The real STS2 sample is
 * user-owned and outside the repo, so it is only used by an optional file-exists-guarded test.
 */
public class Sts2TpsheetParserTest {

    private static final String ONE_PAGE =
            "{ \"textures\": [ {"
                    + " \"image\": \"intent_atlas.png\","
                    + " \"size\": { \"w\": 932, \"h\": 932 },"
                    + " \"sprites\": ["
                    + "  { \"filename\": \"intent_escape.png\","
                    + "    \"region\": { \"x\": 1, \"y\": 1, \"w\": 72, \"h\": 63 },"
                    + "    \"margin\": { \"x\": 0, \"y\": 0, \"w\": 0, \"h\": 0 } },"
                    + "  { \"filename\": \"attack/intent_attack_5.png\","
                    + "    \"region\": { \"x\": 1, \"y\": 66, \"w\": 63, \"h\": 62 },"
                    + "    \"margin\": { \"x\": 4, \"y\": 7, \"w\": 9, \"h\": 10 } }"
                    + " ] } ] }";

    private static final String TWO_PAGES =
            "{ \"textures\": ["
                    + " { \"image\": \"page_a.png\", \"size\": { \"w\": 256, \"h\": 128 },"
                    + "   \"sprites\": ["
                    + "     { \"filename\": \"a.png\","
                    + "       \"region\": { \"x\": 3, \"y\": 4, \"w\": 10, \"h\": 20 },"
                    + "       \"margin\": { \"x\": 1, \"y\": 2, \"w\": 0, \"h\": 0 } } ] },"
                    + " { \"image\": \"page_b.png\", \"size\": { \"w\": 512, \"h\": 64 },"
                    + "   \"sprites\": ["
                    + "     { \"filename\": \"b.png\","
                    + "       \"region\": { \"x\": 7, \"y\": 8, \"w\": 30, \"h\": 40 },"
                    + "       \"margin\": { \"x\": 0, \"y\": 0, \"w\": 6, \"h\": 8 } } ] }"
                    + " ] }";

    @Test
    public void untrimmedRegionMapsExactly() {
        List<AtlasRegion> regions = Sts2TpsheetParser.parse(ONE_PAGE);
        assertEquals(2, regions.size());

        AtlasRegion untrimmed = regions.get(0);
        assertEquals("intent_atlas.png", untrimmed.page);
        assertEquals("intent_escape.png", untrimmed.name);
        assertEquals(1, untrimmed.x);
        assertEquals(1, untrimmed.y);
        assertEquals(72, untrimmed.width);
        assertEquals(63, untrimmed.height);
        assertEquals(932, untrimmed.pageWidth);
        assertEquals(932, untrimmed.pageHeight);
        assertEquals(72, untrimmed.originalWidth);
        assertEquals(63, untrimmed.originalHeight);
        assertEquals(0, untrimmed.offsetX);
        assertEquals(0, untrimmed.offsetY);
        assertEquals(0, untrimmed.degrees);
        assertFalse(untrimmed.trimmed());
        assertFalse(untrimmed.rotated());
        assertEquals(72, untrimmed.displayWidth());
        assertEquals(63, untrimmed.displayHeight());
        assertTrue(untrimmed.valid());
    }

    @Test
    public void trimmedRegionAddsMarginToOriginalSize() {
        AtlasRegion trimmed = Sts2TpsheetParser.parse(ONE_PAGE).get(1);
        assertEquals("attack/intent_attack_5.png", trimmed.name);
        // Packed page rect stays the region rect.
        assertEquals(1, trimmed.x);
        assertEquals(66, trimmed.y);
        assertEquals(63, trimmed.width);
        assertEquals(62, trimmed.height);
        // Untrimmed size = region size + margin size; offset = margin x/y.
        assertEquals(72, trimmed.originalWidth);
        assertEquals(72, trimmed.originalHeight);
        assertEquals(4, trimmed.offsetX);
        assertEquals(7, trimmed.offsetY);
        assertEquals(0, trimmed.degrees);
        assertTrue(trimmed.trimmed());
        assertFalse(trimmed.rotated());
        assertArrayEqualsFloats(new float[] { 4f, 7f, 72f, 72f }, trimmed.trimmedRect());
        assertTrue(trimmed.valid());
    }

    @Test
    public void multiPageCarriesPageAndPageSize() {
        List<AtlasRegion> regions = Sts2TpsheetParser.parse(TWO_PAGES);
        assertEquals(2, regions.size());

        AtlasRegion a = regions.get(0);
        assertEquals("page_a.png", a.page);
        assertEquals("a.png", a.name);
        assertEquals(256, a.pageWidth);
        assertEquals(128, a.pageHeight);
        assertEquals(10, a.width);
        assertEquals(20, a.height);
        assertEquals(1, a.offsetX);
        assertEquals(2, a.offsetY);
        assertEquals(10, a.originalWidth);
        assertEquals(20, a.originalHeight);
        assertFalse(a.trimmed());

        AtlasRegion b = regions.get(1);
        assertEquals("page_b.png", b.page);
        assertEquals("b.png", b.name);
        assertEquals(512, b.pageWidth);
        assertEquals(64, b.pageHeight);
        assertEquals(7, b.x);
        assertEquals(8, b.y);
        assertEquals(36, b.originalWidth);
        assertEquals(48, b.originalHeight);
        assertTrue(b.trimmed());

        // Preserved file order across pages.
        assertEquals("a.png", regions.get(0).name);
        assertEquals("b.png", regions.get(1).name);
    }

    @Test
    public void dirPrefixedFilenameIsPreservedVerbatim() {
        AtlasRegion region = Sts2TpsheetParser.parse(ONE_PAGE).get(1);
        assertEquals("attack/intent_attack_5.png", region.name);
    }

    @Test
    public void keysAreCaseInsensitiveAndWhitespaceTolerated() {
        String mixed =
                "\n\t{  \"Textures\" : [ { \"Image\" : \"P.png\", \"Size\" : { \"W\" : 40,"
                        + " \"H\" : 50 }, \"Sprites\" : [ { \"Filename\" : \"x.png\","
                        + " \"Region\" : { \"X\" : 1, \"Y\" : 2, \"W\" : 3, \"H\" : 4 },"
                        + " \"Margin\" : { \"X\" : 0, \"Y\" : 0, \"W\" : 0, \"H\" : 0 } } ] } ] }\n";
        List<AtlasRegion> regions = Sts2TpsheetParser.parse(mixed);
        assertEquals(1, regions.size());
        AtlasRegion region = regions.get(0);
        assertEquals("P.png", region.page);
        assertEquals("x.png", region.name);
        assertEquals(40, region.pageWidth);
        assertEquals(50, region.pageHeight);
        assertEquals(3, region.width);
        assertEquals(4, region.height);
    }

    @Test
    public void parseFromReaderMatchesParseFromString() {
        List<AtlasRegion> viaReader = Sts2TpsheetParser.parse(new StringReader(ONE_PAGE));
        List<AtlasRegion> viaString = Sts2TpsheetParser.parse(ONE_PAGE);
        assertEquals(viaString.size(), viaReader.size());
        assertEquals(viaString.get(1).name, viaReader.get(1).name);
        assertEquals(viaString.get(1).originalWidth, viaReader.get(1).originalWidth);
    }

    @Test
    public void malformedBlankAndNullFailOpenNeverThrow() {
        assertEquals(0, Sts2TpsheetParser.parse((String) null).size());
        assertEquals(0, Sts2TpsheetParser.parse((java.io.Reader) null).size());
        assertEquals(0, Sts2TpsheetParser.parse("").size());
        assertEquals(0, Sts2TpsheetParser.parse("   \n\t ").size());
        assertEquals(0, Sts2TpsheetParser.parse("{ not json").size());
        assertEquals(0, Sts2TpsheetParser.parse("null").size());
        assertEquals(0, Sts2TpsheetParser.parse("[]").size());
        assertEquals(0, Sts2TpsheetParser.parse("{}").size());
        assertEquals(0, Sts2TpsheetParser.parse("{\"textures\": 5}").size());
    }

    @Test
    public void missingFieldsAreDefaultedOrSkippedNotNpe() {
        // textures present but sprites missing on the page -> page contributes no regions.
        assertEquals(0, Sts2TpsheetParser.parse(
                "{\"textures\":[{\"image\":\"p.png\",\"size\":{\"w\":10,\"h\":10}}]}").size());
        // size/region/margin missing -> zero defaults, name preserved, region still emitted.
        List<AtlasRegion> regions = Sts2TpsheetParser.parse(
                "{\"textures\":[{\"sprites\":[{\"filename\":\"f.png\"}]}]}");
        assertEquals(1, regions.size());
        AtlasRegion region = regions.get(0);
        assertEquals("f.png", region.name);
        assertEquals("", region.page);
        assertEquals(0, region.width);
        assertEquals(0, region.height);
        assertEquals(0, region.pageWidth);
        assertFalse(region.valid());
        // filename missing/blank -> sprite skipped, non-object sprite ignored.
        assertEquals(0, Sts2TpsheetParser.parse(
                "{\"textures\":[{\"sprites\":[{\"region\":{\"x\":1}},{}, 7]}]}").size());
    }

    /**
     * Optional: the real STS2 sample is user-owned and outside the repo, so this is skipped when it
     * is absent and the suite still passes. Non-tautological: it asserts count and non-degenerate
     * rects before checking the trimmed/untrimmed classification from the sample body.
     */
    @Test
    public void realSampleWhenPresentYieldsRegions() throws Exception {
        File sample = new File("/home/justinz/sts2/images/atlases/intent_atlas.tpsheet");
        org.junit.Assume.assumeTrue(sample.isFile());
        String json = new String(java.nio.file.Files.readAllBytes(sample.toPath()),
                java.nio.charset.StandardCharsets.UTF_8);
        List<AtlasRegion> regions = Sts2TpsheetParser.parse(json);
        assertTrue("expected regions from real sample", regions.size() > 0);
        int valid = 0;
        for (AtlasRegion region : regions) {
            assertFalse(region.name.isEmpty());
            assertEquals("intent_atlas.png", region.page);
            assertEquals(932, region.pageWidth);
            assertEquals(932, region.pageHeight);
            assertTrue(region.width > 0 && region.height > 0);
            if (region.valid()) {
                valid++;
            }
        }
        assertEquals(regions.size(), valid);
        AtlasRegion escape = findByName(regions, "intent_escape.png");
        assertFalse(escape.trimmed());
        AtlasRegion attack = findByName(regions, "attack/intent_attack_5.png");
        assertTrue(attack.trimmed());
        assertEquals(72, attack.originalWidth);
        assertEquals(72, attack.originalHeight);
    }

    private static AtlasRegion findByName(List<AtlasRegion> regions, String name) {
        for (AtlasRegion region : regions) {
            if (name.equals(region.name)) {
                return region;
            }
        }
        throw new AssertionError("region not found: " + name);
    }

    private static void assertArrayEqualsFloats(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], 0.0001f);
        }
    }
}

package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * No-GL tests for the {@code .tpsheet} REGION RESOLUTION seam: a sheet key plus a region name must
 * resolve to the borrowed page texture and the parsed neutral descriptor, exactly like a live atlas.
 *
 * <p>Every test here is filesystem-free and GL-free except the one optional real-sample check, which
 * is skipped when the user-owned sample is absent. Assertions pin exact fields/call counts, so the
 * suite fails if the wrong page is fetched, the wrong rect is returned, or the sheet is reparsed.
 */
public class Sts2TpsheetAtlasSourceTest {

    private static final String SHEET_KEY = "animations/images/atlases/intent_atlas.tpsheet";
    private static final String PAGE = "intent_atlas.png";

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

    private static final String ZERO_PAGE_SHEET =
            "{ \"textures\": [ { \"image\": \"intent_atlas.png\","
                    + " \"size\": { \"w\": 0, \"h\": 0 },"
                    + " \"sprites\": [ { \"filename\": \"tiny.png\","
                    + "   \"region\": { \"x\": 0, \"y\": 0, \"w\": 10, \"h\": 10 },"
                    + "   \"margin\": { \"x\": 0, \"y\": 0, \"w\": 0, \"h\": 0 } } ] } ] }";

    /** Untrimmed region: packed rect, unwrapped originals, zero trim offset. */
    @Test
    public void untrimmedRegionCarriesExactFieldsAndInjectedPageTexture() {
        StubTpsheetProvider provider = onePageProvider();
        Texture page = newTexture();
        provider.pages.put(SHEET_KEY + "|" + PAGE, page);
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        Sts1AtlasMaterializer.RegionTexture resolved = source.region(SHEET_KEY, "intent_escape.png");

        assertNotNull(resolved);
        assertTrue("must borrow the injected page texture", resolved.texture == page);
        AtlasRegion region = resolved.region;
        assertEquals(PAGE, region.page);
        assertEquals("intent_escape.png", region.name);
        assertEquals(1, region.x);
        assertEquals(1, region.y);
        assertEquals(72, region.width);
        assertEquals(63, region.height);
        assertEquals(932, region.pageWidth);
        assertEquals(932, region.pageHeight);
        assertEquals(72, region.originalWidth);
        assertEquals(63, region.originalHeight);
        assertEquals(0, region.offsetX);
        assertEquals(0, region.offsetY);
        assertEquals(0, region.degrees);
        assertFalse(region.trimmed());
        assertTrue(region.valid());
        // The page image from the sheet body must drive the texture lookup.
        assertEquals(1, provider.pageKeys.size());
        assertEquals(SHEET_KEY + "|" + PAGE, provider.pageKeys.get(0));
    }

    /** Trimmed region: packed rect plus original size and trim offset from the margin block. */
    @Test
    public void trimmedRegionCarriesExactFieldsAndInjectedPageTexture() {
        StubTpsheetProvider provider = onePageProvider();
        Texture page = newTexture();
        provider.pages.put(SHEET_KEY + "|" + PAGE, page);
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                source.region(SHEET_KEY, "attack/intent_attack_5.png");

        assertNotNull(resolved);
        assertTrue("must borrow the injected page texture", resolved.texture == page);
        AtlasRegion region = resolved.region;
        assertEquals(PAGE, region.page);
        assertEquals("attack/intent_attack_5.png", region.name);
        assertEquals(1, region.x);
        assertEquals(66, region.y);
        assertEquals(63, region.width);
        assertEquals(62, region.height);
        assertEquals(932, region.pageWidth);
        assertEquals(932, region.pageHeight);
        assertEquals(72, region.originalWidth);
        assertEquals(72, region.originalHeight);
        assertEquals(4, region.offsetX);
        assertEquals(7, region.offsetY);
        assertEquals(0, region.degrees);
        assertTrue(region.trimmed());
        assertFalse(region.rotated());
        assertTrue(region.valid());
        assertEquals(1, provider.pageKeys.size());
        assertEquals(SHEET_KEY + "|" + PAGE, provider.pageKeys.get(0));
    }

    /** Every ordinary failure path is a null result, never an escaping exception. */
    @Test
    public void unknownMalformedBlankThrowingAndAbsentCasesFailOpen() {
        StubTpsheetProvider provider = onePageProvider();
        Texture page = newTexture();
        provider.pages.put(SHEET_KEY + "|" + PAGE, page);
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        assertNull("absent region", source.region(SHEET_KEY, "no_such_region.png"));
        assertNull("absent sheet", source.region("animations/images/atlases/absent.tpsheet", "intent_escape.png"));
        assertNull("null sheet key", source.region(null, "intent_escape.png"));
        assertNull("empty sheet key", source.region("", "intent_escape.png"));
        assertNull("blank sheet key", source.region("   ", "intent_escape.png"));
        assertNull("null region name", source.region(SHEET_KEY, null));
        assertNull("blank region name", source.region(SHEET_KEY, "  "));

        StubTpsheetProvider malformed = new StubTpsheetProvider();
        malformed.sheets.put(SHEET_KEY, "{ not json");
        malformed.pages.put(SHEET_KEY + "|" + PAGE, page);
        assertNull("malformed sheet", new Sts2TpsheetAtlasSource(malformed).region(SHEET_KEY, "intent_escape.png"));

        StubTpsheetProvider blank = new StubTpsheetProvider();
        blank.sheets.put(SHEET_KEY, "   \n\t ");
        assertNull("blank sheet text", new Sts2TpsheetAtlasSource(blank).region(SHEET_KEY, "intent_escape.png"));

        StubTpsheetProvider throwing = new StubTpsheetProvider();
        throwing.throwOnSheetText = true;
        assertNull("throwing provider", new Sts2TpsheetAtlasSource(throwing).region(SHEET_KEY, "intent_escape.png"));

        assertNull("null provider must be inert",
                new Sts2TpsheetAtlasSource(null).region(SHEET_KEY, "intent_escape.png"));
    }

    /** Sheet text is read once per sheet key, and an absent/empty sheet is memorized as a miss. */
    @Test
    public void sheetTextIsReadOncePerSheetKeyIncludingFailures() {
        StubTpsheetProvider provider = onePageProvider();
        provider.pages.put(SHEET_KEY + "|" + PAGE, newTexture());
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        assertNotNull(source.region(SHEET_KEY, "intent_escape.png"));
        assertNotNull(source.region(SHEET_KEY, "attack/intent_attack_5.png"));
        assertEquals("two regions on one sheet must parse the sheet once", 1, provider.sheetTextCalls);
        assertNull(source.region(SHEET_KEY, "no_such_region.png"));
        assertEquals("an absent region must not re-read the sheet", 1, provider.sheetTextCalls);

        StubTpsheetProvider absent = new StubTpsheetProvider();
        Sts2TpsheetAtlasSource absentSource = new Sts2TpsheetAtlasSource(absent);
        assertNull(absentSource.region("animations/images/atlases/absent.tpsheet", "intent_escape.png"));
        assertNull(absentSource.region("animations/images/atlases/absent.tpsheet", "intent_escape.png"));
        assertEquals("a missing sheet must be memoized", 1, absent.sheetTextCalls);

        StubTpsheetProvider unparseable = new StubTpsheetProvider();
        unparseable.sheets.put(SHEET_KEY, "{ not json");
        Sts2TpsheetAtlasSource unparseableSource = new Sts2TpsheetAtlasSource(unparseable);
        assertNull(unparseableSource.region(SHEET_KEY, "intent_escape.png"));
        assertNull(unparseableSource.region(SHEET_KEY, "intent_escape.png"));
        assertEquals("a failed parse must be memoized", 1, unparseable.sheetTextCalls);
    }

    /** A missing page texture, or a region rejected as invalid, is a null result. */
    @Test
    public void missingPageTextureAndInvalidRegionFailOpen() {
        StubTpsheetProvider noPage = onePageProvider();
        assertNull("missing page texture",
                new Sts2TpsheetAtlasSource(noPage).region(SHEET_KEY, "intent_escape.png"));

        StubTpsheetProvider zeroPage = new StubTpsheetProvider();
        zeroPage.sheets.put(SHEET_KEY, ZERO_PAGE_SHEET);
        zeroPage.pages.put(SHEET_KEY + "|" + PAGE, newTexture());
        assertNull("a zero-page-size sheet yields an invalid region",
                new Sts2TpsheetAtlasSource(zeroPage).region(SHEET_KEY, "tiny.png"));
    }

    /**
     * Accepted source contract: duplicate {@code filename} entries keep the FIRST occurrence in file
     * order (the same rule the legacy materializer's region cache uses). The sheet is still read
     * once, and the second rect never replaces the first.
     */
    @Test
    public void duplicateFilenameKeepsFirstOccurrenceAndReadsTheSheetOnce() {
        String duplicateSheet =
                "{ \"textures\": [ { \"image\": \"intent_atlas.png\","
                        + " \"size\": { \"w\": 932, \"h\": 932 },"
                        + " \"sprites\": ["
                        + "  { \"filename\": \"dup.png\","
                        + "    \"region\": { \"x\": 1, \"y\": 2, \"w\": 30, \"h\": 40 },"
                        + "    \"margin\": { \"x\": 0, \"y\": 0, \"w\": 0, \"h\": 0 } },"
                        + "  { \"filename\": \"dup.png\","
                        + "    \"region\": { \"x\": 300, \"y\": 400, \"w\": 50, \"h\": 60 },"
                        + "    \"margin\": { \"x\": 0, \"y\": 0, \"w\": 0, \"h\": 0 } }"
                        + " ] } ] }";
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(SHEET_KEY, duplicateSheet);
        provider.pages.put(SHEET_KEY + "|" + PAGE, newTexture());
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        Sts1AtlasMaterializer.RegionTexture resolved = source.region(SHEET_KEY, "dup.png");

        assertNotNull(resolved);
        assertRegionFields("dup.png", resolved.region,
                1, 2, 30, 40, 932, 932, 30, 40, 0, 0, 0, false);
        assertEquals("the first file-order occurrence must win", 1, resolved.region.x);
        assertEquals(2, resolved.region.y);
        assertEquals("a duplicate filename must not cause a second sheet read",
                1, provider.sheetTextCalls);
        assertEquals(SHEET_KEY, provider.sheetsRead.get(0));
    }

    /**
     * Accepted fail-open clause: a page-texture provider that throws is a null result, not an
     * escaping exception, and the borrowed page is asked for exactly once per lookup.
     */
    @Test
    public void throwingPageTextureProviderFailsOpen() {
        StubTpsheetProvider provider = onePageProvider();
        provider.throwOnPageTexture = true;
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        assertNull("a throwing pageTexture must not escape",
                source.region(SHEET_KEY, "intent_escape.png"));

        assertEquals("the sheet still has to be read once to find the region",
                1, provider.sheetTextCalls);
        assertEquals("the throwing page lookup is attempted once",
                1, provider.pageKeys.size());
        assertEquals(SHEET_KEY + "|" + PAGE, provider.pageKeys.get(0));
    }

    /**
     * Accepted factory clause: {@code fromBundle} is inert when either argument is null, so a caller
     * without a bundle/decoder degrades to native rendering instead of throwing.
     */
    @Test
    public void fromBundleWithNullArgumentIsInert() throws Exception {
        StubDecoder decoder = new StubDecoder(newTexture());
        assertNull("null bundle must be inert",
                Sts2TpsheetAtlasSource.fromBundle(null, decoder)
                        .region(SHEET_KEY, "intent_escape.png"));
        assertEquals("a null bundle must not reach the decoder", 0, decoder.bytesSeen.size());

        File root = tempDirectory("tpsheet-inert");
        File animations = new File(root, "animations");
        File atlasDir = new File(animations, "images/atlases");
        assertTrue(atlasDir.mkdirs());
        Files.write(new File(atlasDir, "intent_atlas.tpsheet").toPath(),
                ONE_PAGE.getBytes(StandardCharsets.UTF_8));
        Sts2AssetBundle bundle = Sts2AssetBundle.openDirectory(animations, new File(root, "cache"));
        try {
            assertNull("null decoder must be inert",
                    Sts2TpsheetAtlasSource.fromBundle(bundle, null)
                            .region(SHEET_KEY, "intent_escape.png"));
        } finally {
            bundle.close();
        }
    }

    /** Page entries are siblings of the sheet file, and traversal/blank inputs are rejected. */
    @Test
    public void pageEntryResolvesSiblingAndRejectsTraversalAndBlankInput() {
        assertEquals("animations/images/atlases/intent_atlas.png",
                Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, "intent_atlas.png"));
        assertEquals("backslashes normalize to '/'",
                "a/b/page.png", Sts2TpsheetAtlasSource.pageEntry("a\\b\\sheet.tpsheet", "page.png"));

        assertNull("nested page name", Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, "sub/page.png"));
        assertNull("parent traversal", Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, "../page.png"));
        assertNull("bare parent", Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, ".."));
        assertNull("null page name", Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, null));
        assertNull("blank page name", Sts2TpsheetAtlasSource.pageEntry(SHEET_KEY, "   "));
        assertNull("null sheet key", Sts2TpsheetAtlasSource.pageEntry(null, "page.png"));
        assertNull("blank sheet key", Sts2TpsheetAtlasSource.pageEntry("  ", "page.png"));
    }

    /** The bundle-backed factory reads the sheet + page PNG and decodes only through the seam. */
    @Test
    public void fromBundleReadsSheetAndPageBytesThroughTheBundle() throws Exception {
        File root = tempDirectory("tpsheet-bundle");
        File animations = new File(root, "animations");
        File atlasDir = new File(animations, "images/atlases");
        assertTrue(atlasDir.mkdirs());
        Files.write(new File(atlasDir, "intent_atlas.tpsheet").toPath(),
                ONE_PAGE.getBytes(StandardCharsets.UTF_8));
        Files.write(new File(atlasDir, "intent_atlas.png").toPath(),
                new byte[] { 1, 2, 3, 4, 5 });

        Sts2AssetBundle bundle = Sts2AssetBundle.openDirectory(animations, new File(root, "cache"));
        try {
            StubDecoder decoder = new StubDecoder(newTexture());
            Sts2TpsheetAtlasSource source = Sts2TpsheetAtlasSource.fromBundle(bundle, decoder);

            Sts1AtlasMaterializer.RegionTexture resolved =
                    source.region(SHEET_KEY, "attack/intent_attack_5.png");

            assertNotNull(resolved);
            assertTrue("must return the decoded page texture", resolved.texture == decoder.texture);
            assertEquals(1, decoder.bytesSeen.size());
            assertTrue("decoder must receive non-empty page PNG bytes",
                    decoder.bytesSeen.get(0).length > 0);
            org.junit.Assert.assertArrayEquals("decoder must receive the exact page bytes",
                    new byte[] { 1, 2, 3, 4, 5 }, decoder.bytesSeen.get(0));
            assertEquals(SHEET_KEY, decoder.sheetKeys.get(0));
            assertEquals(PAGE, decoder.pageNames.get(0));

            AtlasRegion region = resolved.region;
            assertEquals(PAGE, region.page);
            assertEquals("attack/intent_attack_5.png", region.name);
            assertEquals(1, region.x);
            assertEquals(66, region.y);
            assertEquals(63, region.width);
            assertEquals(62, region.height);
            assertEquals(932, region.pageWidth);
            assertEquals(932, region.pageHeight);
            assertEquals(72, region.originalWidth);
            assertEquals(72, region.originalHeight);
            assertEquals(4, region.offsetX);
            assertEquals(7, region.offsetY);
            assertEquals(0, region.degrees);
            assertTrue(region.trimmed());
        } finally {
            bundle.close();
        }
    }

    /**
     * Optional: the real STS2 sample is user-owned and outside the repo, so this is skipped when it
     * is absent. Both the untrimmed and the trimmed region must resolve with the sample's exact
     * page size and packed rects through a stub provider.
     */
    @Test
    public void realSampleWhenPresentResolvesBothRegions() throws Exception {
        File sample = new File("/home/justinz/sts2/images/atlases/intent_atlas.tpsheet");
        org.junit.Assume.assumeTrue(sample.isFile());
        String json = new String(Files.readAllBytes(sample.toPath()), StandardCharsets.UTF_8);

        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(SHEET_KEY, json);
        provider.pages.put(SHEET_KEY + "|" + PAGE, newTexture());
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        Sts1AtlasMaterializer.RegionTexture escape = source.region(SHEET_KEY, "intent_escape.png");
        assertNotNull(escape);
        assertEquals(932, escape.region.pageWidth);
        assertEquals(932, escape.region.pageHeight);
        assertEquals(1, escape.region.x);
        assertEquals(1, escape.region.y);
        assertEquals(72, escape.region.width);
        assertEquals(63, escape.region.height);
        assertEquals(72, escape.region.originalWidth);
        assertEquals(63, escape.region.originalHeight);
        assertEquals(0, escape.region.offsetX);
        assertEquals(0, escape.region.offsetY);
        assertFalse(escape.region.trimmed());
        assertEquals(1, provider.sheetTextCalls);

        Sts1AtlasMaterializer.RegionTexture attack =
                source.region(SHEET_KEY, "attack/intent_attack_5.png");
        assertNotNull(attack);
        assertEquals(932, attack.region.pageWidth);
        assertEquals(932, attack.region.pageHeight);
        assertEquals(1, attack.region.x);
        assertEquals(66, attack.region.y);
        assertEquals(63, attack.region.width);
        assertEquals(62, attack.region.height);
        assertEquals(72, attack.region.originalWidth);
        assertEquals(72, attack.region.originalHeight);
        assertEquals(4, attack.region.offsetX);
        assertEquals(7, attack.region.offsetY);
        assertEquals(0, attack.region.degrees);
        assertTrue(attack.region.trimmed());
        assertEquals("the real sample must be parsed once", 1, provider.sheetTextCalls);
    }

    /**
     * Accepted cache contract clause 2: {@code clearCache()} is part of the {@code TpsheetSource}
     * seam, so the source drops its own memoized sheet state. Exactly one re-read follows, and the
     * region fields are unchanged (nothing is served stale, nothing is lost).
     */
    @Test
    public void clearCacheDropsMemoizedSheetStateAndReReadsOnce() {
        StubTpsheetProvider provider = onePageProvider();
        provider.pages.put(SHEET_KEY + "|" + PAGE, newTexture());
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(provider);

        Sts1AtlasMaterializer.RegionTexture before = source.region(SHEET_KEY, "intent_escape.png");
        assertNotNull(before);
        assertRegionFields("intent_escape.png", before.region,
                1, 1, 72, 63, 932, 932, 72, 63, 0, 0, 0, false);
        assertNotNull(source.region(SHEET_KEY, "attack/intent_attack_5.png"));
        assertEquals("two lookups on one sheet must read the sheet once", 1, provider.sheetTextCalls);

        source.clearCache();

        Sts1AtlasMaterializer.RegionTexture after = source.region(SHEET_KEY, "intent_escape.png");

        assertNotNull(after);
        assertEquals("one lookup after clearCache must re-read the sheet exactly once",
                2, provider.sheetTextCalls);
        assertEquals(SHEET_KEY, provider.sheetsRead.get(0));
        assertEquals(SHEET_KEY, provider.sheetsRead.get(1));
        assertRegionFields("intent_escape.png", after.region,
                1, 1, 72, 63, 932, 932, 72, 63, 0, 0, 0, false);
        assertEquals("the pre-clear fields must match the re-read fields",
                before.region.name, after.region.name);
    }

    /** Accepted cache contract clause 2: a failed-sheet mark is also dropped by {@code clearCache()}. */
    @Test
    public void clearCacheDropsFailedSheetMark() {
        StubTpsheetProvider absent = new StubTpsheetProvider();
        Sts2TpsheetAtlasSource source = new Sts2TpsheetAtlasSource(absent);
        String missingKey = "animations/images/atlases/absent.tpsheet";

        assertNull(source.region(missingKey, "intent_escape.png"));
        assertNull(source.region(missingKey, "intent_escape.png"));
        assertEquals("a missing sheet must be memoized", 1, absent.sheetTextCalls);

        source.clearCache();

        assertNull(source.region(missingKey, "intent_escape.png"));
        assertEquals("clearCache must retry a previously failed sheet exactly once",
                2, absent.sheetTextCalls);
    }

    /** Pins all neutral descriptor fields so a cache hit/miss cannot silently change the rect. */
    private static void assertRegionFields(
            String expectedName, AtlasRegion region,
            int x, int y, int width, int height, int pageWidth, int pageHeight,
            int originalWidth, int originalHeight, int offsetX, int offsetY, int degrees,
            boolean trimmed) {
        assertNotNull("region must resolve for " + expectedName, region);
        assertEquals(PAGE, region.page);
        assertEquals(expectedName, region.name);
        assertEquals(x, region.x);
        assertEquals(y, region.y);
        assertEquals(width, region.width);
        assertEquals(height, region.height);
        assertEquals(pageWidth, region.pageWidth);
        assertEquals(pageHeight, region.pageHeight);
        assertEquals(originalWidth, region.originalWidth);
        assertEquals(originalHeight, region.originalHeight);
        assertEquals(offsetX, region.offsetX);
        assertEquals(offsetY, region.offsetY);
        assertEquals(degrees, region.degrees);
        assertEquals(trimmed, region.trimmed());
    }

    private static StubTpsheetProvider onePageProvider() {
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(SHEET_KEY, ONE_PAGE);
        return provider;
    }

    private static File tempDirectory(String name) {
        File dir = new File(System.getProperty("java.io.tmpdir"),
                "art-test-" + name + "-" + System.nanoTime());
        assertTrue(dir.mkdirs());
        return dir;
    }

    /** No-GL {@code Texture} double: an allocated instance that never touches a GL context. */
    private static Texture newTexture() {
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
                public int getWidth() { return 932; }
                public int getHeight() { return 932; }
                public Pixmap.Format getFormat() { return Pixmap.Format.RGBA8888; }
                public boolean useMipMaps() { return false; }
                public boolean isManaged() { return false; }
            });
            return texture;
        } catch (Exception failure) {
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
    }

    private static final class StubTpsheetProvider implements Sts2TpsheetAtlasSource.TpsheetProvider {
        final Map<String, String> sheets = new LinkedHashMap<String, String>();
        final Map<String, Texture> pages = new LinkedHashMap<String, Texture>();
        final List<String> pageKeys = new ArrayList<String>();
        final List<String> sheetsRead = new ArrayList<String>();
        boolean throwOnSheetText;
        boolean throwOnPageTexture;
        int sheetTextCalls;

        public String sheetText(String sheetKey) {
            sheetTextCalls++;
            sheetsRead.add(sheetKey);
            if (throwOnSheetText) {
                throw new IllegalStateException("sheetText boom");
            }
            return sheets.get(sheetKey);
        }

        public Texture pageTexture(String sheetKey, String pageName) {
            pageKeys.add(sheetKey + "|" + pageName);
            if (throwOnPageTexture) {
                throw new IllegalStateException("pageTexture boom");
            }
            return pages.get(sheetKey + "|" + pageName);
        }
    }

    private static final class StubDecoder implements Sts2TpsheetAtlasSource.PageTextureDecoder {
        final Texture texture;
        final List<byte[]> bytesSeen = new ArrayList<byte[]>();
        final List<String> sheetKeys = new ArrayList<String>();
        final List<String> pageNames = new ArrayList<String>();

        StubDecoder(Texture texture) {
            this.texture = texture;
        }

        public Texture decode(String sheetKey, String pageName, byte[] pngBytes) {
            sheetKeys.add(sheetKey);
            pageNames.add(pageName);
            bytesSeen.add(pngBytes);
            return texture;
        }
    }
}

package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Sts1AtlasMaterializerTest {

    private static final String SAMPLE_ATLAS =
            "frame.png\n"
                    + "size: 512, 512\n"
                    + "format: RGBA8888\n"
                    + "filter: Linear, Linear\n"
                    + "repeat: none\n"
                    + "attack.common\n"
                    + "  rotate: false\n"
                    + "  xy: 10, 20\n"
                    + "  size: 100, 50\n"
                    + "  orig: 100, 50\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n"
                    + "skill.uncommon\n"
                    + "  rotate: false\n"
                    + "  xy: 200, 300\n"
                    + "  size: 60, 40\n"
                    + "  orig: 60, 40\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n";

    private static final String ROTATED_ATLAS =
            "frame.png\n"
                    + "size: 512, 512\n"
                    + "rot.1\n"
                    + "  rotate: true\n"
                    + "  xy: 5, 6\n"
                    + "  size: 10, 20\n"
                    + "  orig: 20, 10\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n";

    private static final String ZERO_PAGE_ATLAS =
            "frame.png\n"
                    + "size: 0, 0\n"
                    + "tiny\n"
                    + "  rotate: false\n"
                    + "  xy: 0, 0\n"
                    + "  size: 10, 10\n"
                    + "  orig: 10, 10\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n";

    private static final String MALFORMED_ATLAS =
            "frame.png\n"
                    + "size: 100, 100\n"
                    + "broken\n"
                    + "  rotate: false\n"
                    + "  xy: 1, 1\n"
                    + "  size: not-a-number\n"
                    + "  orig: 1, 1\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n";

    /**
     * U02 legacy-atlas TEXT for the {@code cardui} atlas containing the 512-page attack/red base
     * background region. Region names contain a {@code /} (page/region style), which the parser must
     * keep verbatim.
     */
    private static final String CARDUI_ATLAS_TEXT =
            "cardui.png\n"
                    + "size: 512, 512\n"
                    + "format: RGBA8888\n"
                    + "filter: Linear, Linear\n"
                    + "repeat: none\n"
                    + "512/bg_attack_red\n"
                    + "  rotate: false\n"
                    + "  xy: 10, 20\n"
                    + "  size: 64, 48\n"
                    + "  orig: 64, 48\n"
                    + "  offset: 0, 0\n"
                    + "  index: -1\n";

    /** F13: TexturePacker JSON sheet for the STS2 intent atlas page (932x932, two regions). */
    private static final String TPSHEET_ONE_PAGE =
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

    private static final String INTENT_SHEET_KEY = "animations/images/atlases/intent_atlas.tpsheet";

    @After
    public void restoreDefaultProvider() {
        Sts1AtlasMaterializer.setProviderForTests(null);
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        Sts1AtlasMaterializer.setTpsheetSource(null);
        Sts1AtlasMaterializer.clearCache();
    }

    @Test
    public void resolvesRegionFromLegacyAtlas() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.region("cardui/frame", "attack.common");

        assertNotNull(resolved);
        assertTrue(resolved.texture == frame);
        AtlasRegion region = resolved.region;
        assertEquals("frame.png", region.page);
        assertEquals("attack.common", region.name);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        assertEquals(100, region.width);
        assertEquals(50, region.height);
        assertEquals(512, region.pageWidth);
        assertEquals(512, region.pageHeight);
        assertEquals(0, region.degrees);
        assertEquals(1, provider.atlasTextCalls);
        assertEquals(1, provider.pageTextureCalls);
    }

    @Test
    public void rotatedRegionIsReturnedUnswapped() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", ROTATED_ATLAS);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.region("cardui/frame", "rot.1");

        assertNotNull(resolved);
        AtlasRegion region = resolved.region;
        assertEquals(90, region.degrees);
        assertTrue(region.rotated());
        // Packed bounds stay as parsed; the caller swaps via displayWidth()/displayHeight().
        assertEquals(10, region.width);
        assertEquals(20, region.height);
        assertEquals(20, region.displayWidth());
        assertEquals(10, region.displayHeight());
    }

    @Test
    public void unknownRegionAndAtlasAndBlankArgsResolveToNull() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        provider.pages.put("cardui/frame|frame.png", newTexture());
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "missing.region"));
        assertNull(Sts1AtlasMaterializer.region("unknown/key", "attack.common"));
        assertNull(Sts1AtlasMaterializer.region(null, "attack.common"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", null));
        assertNull(Sts1AtlasMaterializer.region("", "attack.common"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "   "));
    }

    @Test
    public void nullAtlasTextFailsOpenAndIsNotReparsed() {
        StubProvider provider = new StubProvider();
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));

        assertEquals(1, provider.atlasTextCalls);
        assertEquals(0, provider.pageTextureCalls);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("failedAtlasCount"));
    }

    @Test
    public void malformedAtlasFailsOpenAndMarksKeyFailed() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", MALFORMED_ATLAS);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "broken"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "broken"));

        assertEquals("failed atlas key must not be reparsed on every call", 1, provider.atlasTextCalls);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("failedAtlasCount"));
    }

    @Test
    public void invalidRegionWithZeroSizedPageResolvesToNull() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", ZERO_PAGE_ATLAS);
        provider.pages.put("cardui/frame|frame.png", newTexture());
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "tiny"));
        assertEquals("invalid region must be rejected before texture lookup", 0, provider.pageTextureCalls);
    }

    @Test
    public void pageTextureIsCachedAcrossRegionsOnTheSamePage() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Sts1AtlasMaterializer.RegionTexture first =
                Sts1AtlasMaterializer.region("cardui/frame", "attack.common");
        Sts1AtlasMaterializer.RegionTexture second =
                Sts1AtlasMaterializer.region("cardui/frame", "skill.uncommon");

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.texture == frame);
        assertTrue(second.texture == frame);
        assertEquals(1, provider.pageTextureCalls);
        assertEquals(1, provider.atlasTextCalls);
    }

    @Test
    public void clearCacheForcesReparseAndResetsProbeCounts() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        provider.pages.put("cardui/frame|frame.png", newTexture());
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNotNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        Map<String, Object> populated = Sts1AtlasMaterializer.probeSlice();
        assertEquals(Integer.valueOf(1), populated.get("atlasCount"));
        assertEquals(Integer.valueOf(0), populated.get("failedAtlasCount"));
        assertEquals(Integer.valueOf(2), populated.get("regionCount"));
        assertEquals(Integer.valueOf(1), populated.get("textureCount"));
        assertEquals(1, provider.atlasTextCalls);

        Sts1AtlasMaterializer.clearCache();

        Map<String, Object> cleared = Sts1AtlasMaterializer.probeSlice();
        assertEquals(Integer.valueOf(0), cleared.get("atlasCount"));
        assertEquals(Integer.valueOf(0), cleared.get("failedAtlasCount"));
        assertEquals(Integer.valueOf(0), cleared.get("regionCount"));
        assertEquals(Integer.valueOf(0), cleared.get("textureCount"));

        assertNotNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertEquals("clearCache must force a re-parse", 2, provider.atlasTextCalls);
    }

    @Test
    public void probeSliceIsReadOnlyAndSafeWhenEmpty() {
        Map<String, Object> first = Sts1AtlasMaterializer.probeSlice();
        Map<String, Object> second = Sts1AtlasMaterializer.probeSlice();

        assertEquals(first, second);
        assertEquals(Integer.valueOf(0), first.get("atlasCount"));
        assertEquals(Integer.valueOf(0), first.get("failedAtlasCount"));
        assertEquals(Integer.valueOf(0), first.get("regionCount"));
        assertEquals(Integer.valueOf(0), first.get("textureCount"));
    }

    @Test
    public void throwingAtlasTextProviderFailsOpenAndIsNotRetried() {
        StubProvider provider = new StubProvider();
        provider.throwOnAtlasText = true;
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));

        assertEquals("throwing provider must not be re-invoked", 1, provider.atlasTextCalls);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("failedAtlasCount"));
    }

    @Test
    public void throwingPageTextureProviderFailsOpen() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        provider.throwOnPageTexture = true;
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
    }

    @Test
    public void missingPageTextureIsMemoizedAndNotReprobed() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        // No page entry registered: pageTexture returns null.
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));

        assertEquals("missing page must be memoized", 1, provider.pageTextureCalls);
        assertEquals(1, provider.atlasTextCalls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("textureCount"));
    }

    @Test
    public void duplicateRegionNameKeepsFirstFileOrderOccurrence() {
        String duplicateAtlas =
                "frame.png\n"
                        + "size: 512, 512\n"
                        + "dup\n"
                        + "  rotate: false\n"
                        + "  xy: 1, 2\n"
                        + "  size: 30, 40\n"
                        + "  orig: 30, 40\n"
                        + "  offset: 0, 0\n"
                        + "  index: -1\n"
                        + "dup\n"
                        + "  rotate: false\n"
                        + "  xy: 300, 400\n"
                        + "  size: 50, 60\n"
                        + "  orig: 50, 60\n"
                        + "  offset: 0, 0\n"
                        + "  index: -1\n";
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", duplicateAtlas);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.region("cardui/frame", "dup");

        assertNotNull(resolved);
        assertEquals(1, resolved.region.x);
        assertEquals(2, resolved.region.y);
        assertEquals(30, resolved.region.width);
        assertEquals(40, resolved.region.height);
    }

    @Test
    public void setProviderForTestsDropsStaleBorrowsFromPreviousProvider() {
        StubProvider providerA = new StubProvider();
        providerA.texts.put("cardui/frame", SAMPLE_ATLAS);
        Texture frameA = newTexture();
        providerA.pages.put("cardui/frame|frame.png", frameA);
        Sts1AtlasMaterializer.setProviderForTests(providerA);
        assertNotNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("atlasCount"));

        StubProvider providerB = new StubProvider();
        Sts1AtlasMaterializer.setProviderForTests(providerB);

        assertEquals("provider swap must clear prior borrows",
                Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("atlasCount"));
        assertNull(Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
        assertEquals(1, providerB.atlasTextCalls);
    }

    @Test
    public void publicSetProviderBindsHostThenNullFailsOpen() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);

        Sts1AtlasMaterializer.setProvider(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.region("cardui/frame", "attack.common");
        assertNotNull(resolved);
        assertTrue(resolved.texture == frame);

        Sts1AtlasMaterializer.setProvider(null);

        assertEquals("null provider must clear prior borrows",
                Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("atlasCount"));
        assertNull("null provider must fail open", Sts1AtlasMaterializer.region("cardui/frame", "attack.common"));
    }

    @Test
    public void resolvesNamedRegionFromLiveAtlas() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        TextureAtlas.AtlasRegion gdx = atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common");

        assertNotNull(resolved);
        assertTrue(resolved.texture == page);
        AtlasRegion region = resolved.region;
        assertEquals("attack.common", region.name);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        assertEquals(64, region.width);
        assertEquals(48, region.height);
        assertEquals(256, region.pageWidth);
        assertEquals(256, region.pageHeight);
        assertEquals(0, region.degrees);
        assertArrayEquals(
                new float[] { gdx.getU(), gdx.getV(), gdx.getU2(), gdx.getV2() },
                region.uvRect(),
                1e-6f);
        assertEquals(1, source.atlasCalls);
    }

    @Test
    public void unknownRegionBlankArgsAndNullAtlasFailOpen() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("known", page, 1, 2, 8, 8);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "missing"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas(null, "known"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", null));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("", "known"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "   "));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("no/such/atlas", "known"));
    }

    @Test
    public void zeroSizedPageFromLiveAtlasResolvesToNull() {
        Texture page = newTexture(0, 0);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("tiny", page, 0, 0, 16, 16);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "tiny"));
    }

    @Test
    public void throwingLiveSourceFailsOpenAndIsNotReAsked() {
        StubLiveSource source = new StubLiveSource();
        source.throwOnAtlas = true;
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));

        assertEquals("throwing live source must not be re-invoked", 1, source.atlasCalls);
    }

    @Test
    public void liveRegionResolutionIsCachedAndAtlasAskedOnce() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        Sts1AtlasMaterializer.RegionTexture first =
                Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common");
        Sts1AtlasMaterializer.RegionTexture second =
                Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common");

        assertNotNull(first);
        assertTrue(first == second);
        assertEquals(1, source.atlasCalls);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));
    }

    @Test
    public void nullLiveSourceRestoreFailsOpen() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);
        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));

        Sts1AtlasMaterializer.setLiveAtlasSource(null);

        assertEquals("null source must clear prior borrows",
                Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals("null source must not re-ask", 1, source.atlasCalls);
    }

    @Test
    public void clearCacheForcesLiveReResolutionAndResetsProbe() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));

        Sts1AtlasMaterializer.clearCache();

        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));
        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals("clearCache must force a re-resolution", 2, source.atlasCalls);
    }

    @Test
    public void probeSliceContainsLiveRegionCountAndIsReadOnly() {
        Map<String, Object> first = Sts1AtlasMaterializer.probeSlice();
        Map<String, Object> second = Sts1AtlasMaterializer.probeSlice();

        assertTrue(first.containsKey("liveRegionCount"));
        assertEquals(Integer.valueOf(0), first.get("liveRegionCount"));
        assertEquals(first, second);
    }

    @Test
    public void onHostRecreatedDropsCachedLiveStateAndReAsksSource() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));
        assertEquals(1, source.atlasCalls);

        Sts1AtlasMaterializer.onHostRecreated();

        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("liveRegionCount"));
        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals("host recreation hook must force a re-resolution", 2, source.atlasCalls);
    }

    @Test
    public void missingLiveRegionIsMemoizedAndNotReAsked() {
        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("known", page, 1, 2, 8, 8);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "unknown"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "unknown"));

        assertEquals("missing live region must be memoized", 1, source.atlasCalls);
    }

    @Test
    public void failedLiveAtlasKeyBecomesResolvableAfterNewSource() {
        StubLiveSource empty = new StubLiveSource();
        Sts1AtlasMaterializer.setLiveAtlasSource(empty);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common"));
        assertEquals(1, empty.atlasCalls);

        Texture page = newTexture(256, 256);
        StubLiveSource valid = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        valid.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(valid);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common");
        assertNotNull("installing a valid source must clear the prior failed key", resolved);
        assertTrue(resolved.texture == page);
        assertEquals(1, valid.atlasCalls);
        assertEquals("prior source must not be re-asked after the swap", 1, empty.atlasCalls);
    }

    @Test
    public void textRegionPathUnaffectedWhileLiveSourceInstalled() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui/frame", SAMPLE_ATLAS);
        Texture frame = newTexture();
        provider.pages.put("cardui/frame|frame.png", frame);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Texture page = newTexture(256, 256);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("attack.common", page, 10, 20, 64, 48);
        source.atlases.put("vfxAtlas", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        Sts1AtlasMaterializer.RegionTexture textResolved =
                Sts1AtlasMaterializer.region("cardui/frame", "attack.common");
        Sts1AtlasMaterializer.RegionTexture liveResolved =
                Sts1AtlasMaterializer.regionFromAtlas("vfxAtlas", "attack.common");

        assertNotNull(textResolved);
        assertTrue(textResolved.texture == frame);
        assertNotNull(liveResolved);
        assertTrue(liveResolved.texture == page);
        assertEquals(1, provider.atlasTextCalls);
        assertEquals(1, source.atlasCalls);
    }

    /**
     * U02. The {@code cardui} 512-page base-background region resolves through the LIVE atlas seam
     * ({@code ImageMaster.cardUiAtlas} shape): {@code findRegion} plus neutral conversion.
     */
    @Test
    public void resolvesCardUiBaseBackgroundRegionFromLiveAtlas() {
        Texture page = newTexture(512, 512);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("512/bg_attack_red", page, 10, 20, 64, 48);
        source.atlases.put("cardui", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("cardui", "512/bg_attack_red");

        assertNotNull(resolved);
        assertTrue(resolved.texture == page);
        AtlasRegion region = resolved.region;
        assertEquals("512/bg_attack_red", region.name);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        assertEquals(64, region.width);
        assertEquals(48, region.height);
        assertEquals(512, region.pageWidth);
        assertEquals(512, region.pageHeight);
        assertEquals(0, region.degrees);
    }

    @Test
    public void missingCardUiRegionAndAbsentLiveSourceFailOpen() {
        Texture page = newTexture(512, 512);
        StubLiveSource source = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("512/bg_attack_red", page, 10, 20, 64, 48);
        source.atlases.put("cardui", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(source);

        assertNull("an absent region must fail open",
                Sts1AtlasMaterializer.regionFromAtlas("cardui", "512/bg_skill_blue"));

        Sts1AtlasMaterializer.setLiveAtlasSource(null);
        assertNull("an absent live source must fail open",
                Sts1AtlasMaterializer.regionFromAtlas("cardui", "512/bg_attack_red"));
    }

    /**
     * U02. The same {@code cardui} 512-page base-background region resolves through the legacy atlas
     * TEXT seam ({@code AtlasProvider}).
     */
    @Test
    public void resolvesCardUiBaseBackgroundRegionFromLegacyAtlasText() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui", CARDUI_ATLAS_TEXT);
        Texture page = newTexture();
        provider.pages.put("cardui|cardui.png", page);
        Sts1AtlasMaterializer.setProviderForTests(provider);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.region("cardui", "512/bg_attack_red");

        assertNotNull(resolved);
        assertTrue(resolved.texture == page);
        AtlasRegion region = resolved.region;
        assertEquals("512/bg_attack_red", region.name);
        assertEquals("cardui.png", region.page);
        assertEquals(10, region.x);
        assertEquals(20, region.y);
        assertEquals(64, region.width);
        assertEquals(48, region.height);
        assertEquals(512, region.pageWidth);
        assertEquals(512, region.pageHeight);
    }

    @Test
    public void missingCardUiRegionFromLegacyAtlasTextResolvesToNull() {
        StubProvider provider = new StubProvider();
        provider.texts.put("cardui", CARDUI_ATLAS_TEXT);
        provider.pages.put("cardui|cardui.png", newTexture());
        Sts1AtlasMaterializer.setProviderForTests(provider);

        assertNull(Sts1AtlasMaterializer.region("cardui", "512/bg_skill_blue"));
    }

    /**
     * F13. A {@code .tpsheet} sheet resolves through the materializer's tpsheet seam when no live
     * atlas source is installed: exact fields, borrowed page texture, and a cached result.
     */
    @Test
    public void resolvesRegionFromTpsheetSourceAndCachesIt() {
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        Texture page = newTexture();
        provider.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", page);
        Sts1AtlasMaterializer.setTpsheetSource(new Sts2TpsheetAtlasSource(provider));

        Sts1AtlasMaterializer.RegionTexture first =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "attack/intent_attack_5.png");

        assertNotNull(first);
        assertTrue(first.texture == page);
        AtlasRegion region = first.region;
        assertEquals("intent_atlas.png", region.page);
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
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));

        Sts1AtlasMaterializer.RegionTexture second =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "attack/intent_attack_5.png");

        assertTrue("materializer must cache the resolved region", first == second);
        assertEquals("cached resolution must not re-read the sheet", 1, provider.sheetTextCalls);
    }

    /**
     * F13 precedence. A key the LIVE atlas source resolves is never handed to the tpsheet source,
     * even when the tpsheet provider would also resolve the same key/region.
     */
    @Test
    public void liveAtlasWinsOverTpsheetSourceForTheSameKey() {
        Texture livePage = newTexture(512, 512);
        StubLiveSource live = new StubLiveSource();
        TextureAtlas atlas = new TextureAtlas();
        atlas.addRegion("512/bg_attack_red", livePage, 10, 20, 64, 48);
        live.atlases.put("cardui", atlas);
        Sts1AtlasMaterializer.setLiveAtlasSource(live);

        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put("cardui", TPSHEET_ONE_PAGE);
        Texture sheetPage = newTexture();
        provider.pages.put("cardui|intent_atlas.png", sheetPage);
        Sts1AtlasMaterializer.setTpsheetSource(new Sts2TpsheetAtlasSource(provider));

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas("cardui", "512/bg_attack_red");

        assertNotNull(resolved);
        assertTrue("the live page texture must win", resolved.texture == livePage);
        assertEquals("512/bg_attack_red", resolved.region.name);
        assertEquals(10, resolved.region.x);
        assertEquals(20, resolved.region.y);
        assertEquals(512, resolved.region.pageWidth);
        assertEquals(0, provider.sheetTextCalls);
        assertEquals(0, provider.pageKeys.size());

        // The legacy text path is untouched by a tpsheet source.
        StubProvider textProvider = new StubProvider();
        textProvider.texts.put("cardui", CARDUI_ATLAS_TEXT);
        Texture textPage = newTexture();
        textProvider.pages.put("cardui|cardui.png", textPage);
        Sts1AtlasMaterializer.setProviderForTests(textProvider);

        Sts1AtlasMaterializer.RegionTexture textResolved =
                Sts1AtlasMaterializer.region("cardui", "512/bg_attack_red");
        assertNotNull(textResolved);
        assertTrue(textResolved.texture == textPage);
        assertEquals(0, provider.sheetTextCalls);
    }

    /**
     * F13. {@code clearCache()} and {@code onHostRecreated()} each drop the tpsheet region cache and
     * force the materializer to re-resolve through the source.
     *
     * <p>The re-read is observed at the materializer seam ({@code TpsheetSource.region}). Sheet-text
     * memoization is source-owned and is dropped through {@code TpsheetSource.clearCache()};
     * {@link #clearCacheCallsTheInstalledTpsheetSourceClearHook()} and
     * {@link #onHostRecreatedCallsTheInstalledTpsheetSourceClearHook()} pin that hook, so this test
     * only counts materializer-level asks.
     */
    @Test
    public void clearCacheAndHostRecreatedDropTpsheetStateAndForceReResolution() {
        CountingTpsheetSource source = new CountingTpsheetSource(onePageSource());
        Sts1AtlasMaterializer.setTpsheetSource(source);

        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        Map<String, Object> populated = Sts1AtlasMaterializer.probeSlice();
        assertTrue("probe must expose tpsheetRegionCount", populated.containsKey("tpsheetRegionCount"));
        assertEquals(Integer.valueOf(1), populated.get("tpsheetRegionCount"));
        assertEquals(1, source.calls);

        Sts1AtlasMaterializer.clearCache();

        assertEquals("clearCache must drop the tpsheet cache",
                Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertEquals("clearCache must force a re-resolution", 2, source.calls);

        Sts1AtlasMaterializer.onHostRecreated();

        assertEquals("onHostRecreated must drop the tpsheet cache",
                Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
        assertNotNull(Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertEquals("onHostRecreated must force a re-resolution", 3, source.calls);
    }

    /**
     * F13 precedence edge. When the live source yields null for a key, the tpsheet source is
     * consulted; the resolved region is then cached, so repeat calls neither re-ask the live source
     * nor the tpsheet source.
     */
    @Test
    public void liveMissFallsThroughToTpsheetSourceAndStaysCached() {
        StubLiveSource live = new StubLiveSource();
        Sts1AtlasMaterializer.setLiveAtlasSource(live);

        CountingTpsheetSource source = new CountingTpsheetSource(onePageSource());
        Sts1AtlasMaterializer.setTpsheetSource(source);

        Sts1AtlasMaterializer.RegionTexture first =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");
        Sts1AtlasMaterializer.RegionTexture second =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");

        assertNotNull("a live miss must fall through to the tpsheet source", first);
        assertTrue("the cached tpsheet result must be reused", first == second);
        assertEquals("intent_escape.png", first.region.name);
        assertEquals(1, first.region.x);
        assertEquals(1, first.region.y);
        assertEquals(72, first.region.width);
        assertEquals(0, first.region.offsetX);
        assertEquals(1, live.atlasCalls);
        assertEquals("the tpsheet source must be asked once", 1, source.calls);
    }

    /** F13. A throwing tpsheet source fails open and is not re-asked for the same pair. */
    @Test
    public void throwingTpsheetSourceFailsOpenAndIsNotReAsked() {
        ThrowingTpsheetSource source = new ThrowingTpsheetSource();
        Sts1AtlasMaterializer.setTpsheetSource(source);

        assertNull(Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertNull(Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));

        assertEquals("a throwing tpsheet source must not be re-invoked", 1, source.calls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 finding 4. {@code TpsheetSource} is a public SPI, so a host source can answer with a
     * texture-less descriptor. The materializer must treat that as a MISS — null plus a memoized
     * miss, mirroring the live path's invalid-region handling — never as a cached hit.
     */
    @Test
    public void tpsheetSourceAnsweringNullTextureIsMemoizedAsAMiss() {
        AtlasRegion validRegion =
                AtlasRegion.of("intent_atlas.png", "intent_escape.png", 1, 1, 72, 63, 932, 932);
        CountingTpsheetSource source = new CountingTpsheetSource(
                new FixedTpsheetSource(new Sts1AtlasMaterializer.RegionTexture(null, validRegion)));
        Sts1AtlasMaterializer.setTpsheetSource(source);

        assertNull("a descriptor without a texture is not a usable region",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertNull("a descriptor without a texture must stay a miss",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));

        assertEquals("a non-conforming answer must be asked once, then memoized", 1, source.calls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 finding 4. A {@code RegionTexture} with a null descriptor is a MISS: null and memoized,
     * so the source is not re-asked and nothing is cached as a region.
     */
    @Test
    public void tpsheetSourceAnsweringNullRegionIsMemoizedAsAMiss() {
        CountingTpsheetSource source = new CountingTpsheetSource(
                new FixedTpsheetSource(new Sts1AtlasMaterializer.RegionTexture(newTexture(), null)));
        Sts1AtlasMaterializer.setTpsheetSource(source);

        assertNull("a descriptor without a region is not a usable region",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertNull("a descriptor without a region must stay a miss",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));

        assertEquals("a non-conforming answer must be asked once, then memoized", 1, source.calls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 finding 4. A source-supplied descriptor whose packed bounds are not inside a positive-sized
     * page (page size 0 here) must be rejected like the live path rejects it: null and memoized, so
     * it can never draw the whole page as a {0,0,1,1} UV.
     */
    @Test
    public void tpsheetSourceAnsweringInvalidRegionIsMemoizedAsAMiss() {
        AtlasRegion zeroPageRegion =
                new AtlasRegion("p.png", "r.png", 0, 0, 10, 10, 0, 0, 10, 10, 0, 0, 0);
        CountingTpsheetSource source = new CountingTpsheetSource(
                new FixedTpsheetSource(
                        new Sts1AtlasMaterializer.RegionTexture(newTexture(), zeroPageRegion)));
        Sts1AtlasMaterializer.setTpsheetSource(source);

        assertNull("a zero-page-size descriptor is invalid",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertNull("an invalid descriptor must stay a miss",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));

        assertEquals("a non-conforming answer must be asked once, then memoized", 1, source.calls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /** F13 finding 4 regression guard: a conforming source still resolves through the same seam. */
    @Test
    public void conformingTpsheetSourceStillResolves() {
        CountingTpsheetSource source = new CountingTpsheetSource(onePageSource());
        Sts1AtlasMaterializer.setTpsheetSource(source);

        Sts1AtlasMaterializer.RegionTexture resolved =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");

        assertNotNull(resolved);
        assertNotNull(resolved.texture);
        assertEquals("intent_escape.png", resolved.region.name);
        assertEquals(1, resolved.region.x);
        assertEquals(1, resolved.region.y);
        assertEquals(72, resolved.region.width);
        assertEquals(63, resolved.region.height);
        assertEquals(1, source.calls);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 cache contract clause 2. {@code clearCache()} must call {@code TpsheetSource.clearCache()}
     * on the installed source, so the SAME {@link Sts2TpsheetAtlasSource} instance re-reads the sheet
     * text exactly once for the next lookup and still yields the identical region fields.
     */
    @Test
    public void clearCacheCallsTheInstalledTpsheetSourceClearHook() {
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        provider.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", newTexture());
        Sts1AtlasMaterializer.setTpsheetSource(new Sts2TpsheetAtlasSource(provider));

        Sts1AtlasMaterializer.RegionTexture before =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");
        assertNotNull(before);
        assertEquals("the first lookup must read the sheet once", 1, provider.sheetTextCalls);
        assertEquals(INTENT_SHEET_KEY, provider.sheetsRead.get(0));

        Sts1AtlasMaterializer.clearCache();

        Sts1AtlasMaterializer.RegionTexture after =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");
        assertNotNull("a cleared tpsheet cache must still resolve", after);
        assertEquals("clearCache must make the installed source re-read the sheet exactly once",
                2, provider.sheetTextCalls);
        assertEquals("the same sheet key must be read again", INTENT_SHEET_KEY,
                provider.sheetsRead.get(1));
        assertRegionEquals("the region fields must be identical after clearCache", before.region,
                after.region);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 cache contract clause 2. {@code onHostRecreated()} takes the same clearing path: the sheet
     * text is re-read once (1 -&gt; 2 on a freshly installed source) and fields are unchanged.
     */
    @Test
    public void onHostRecreatedCallsTheInstalledTpsheetSourceClearHook() {
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        provider.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", newTexture());
        Sts1AtlasMaterializer.setTpsheetSource(new Sts2TpsheetAtlasSource(provider));

        Sts1AtlasMaterializer.RegionTexture before =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "attack/intent_attack_5.png");
        assertNotNull(before);
        assertEquals(1, provider.sheetTextCalls);

        Sts1AtlasMaterializer.onHostRecreated();

        Sts1AtlasMaterializer.RegionTexture after =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "attack/intent_attack_5.png");
        assertNotNull(after);
        assertEquals("onHostRecreated must make the installed source re-read the sheet exactly once",
                2, provider.sheetTextCalls);
        assertRegionEquals("the region fields must be identical after onHostRecreated",
                before.region, after.region);
        assertEquals(Integer.valueOf(1), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    /**
     * F13 cache contract clause 2. Installing a different source clears the OUTGOING source: while it
     * is installed the materializer never routes a lookup to it (the new source is asked for its own
     * sheet), and restoring {@code null} drops the materializer-side state entirely instead of
     * serving the previous source's memo.
     *
     * <p>That the outgoing {@link TpsheetSource#clearCache()} hook actually ran is proven directly:
     * the outgoing instance is asked for a region AFTER the swap, and it re-reads the sheet (stub
     * count 1 -&gt; 2) only because its own memo was emptied. A materializer-only cache drop would
     * leave that count at 1.
     */
    @Test
    public void swappingTpsheetSourceClearsTheOutgoingSourceAndNeverServesItsMemo() {
        StubTpsheetProvider first = new StubTpsheetProvider();
        first.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        Texture firstPage = newTexture();
        first.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", firstPage);
        Sts2TpsheetAtlasSource outgoingSource = new Sts2TpsheetAtlasSource(first);
        Sts1AtlasMaterializer.setTpsheetSource(outgoingSource);

        Sts1AtlasMaterializer.RegionTexture firstResolved =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");
        assertNotNull(firstResolved);
        assertTrue(firstResolved.texture == firstPage);
        assertEquals(1, first.sheetTextCalls);

        StubTpsheetProvider second = new StubTpsheetProvider();
        second.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        Texture secondPage = newTexture();
        second.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", secondPage);
        Sts1AtlasMaterializer.setTpsheetSource(new Sts2TpsheetAtlasSource(second));

        Sts1AtlasMaterializer.RegionTexture secondResolved =
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png");
        assertNotNull("the new source must resolve from its own memo", secondResolved);
        assertTrue("the new source's own page texture must be used",
                secondResolved.texture == secondPage);
        assertEquals("the new source must read the sheet itself", 1, second.sheetTextCalls);
        assertEquals("the outgoing source must not be re-asked", 1, first.sheetTextCalls);

        // Prove the outgoing clearCache() hook ran: a direct call on the outgoing instance now
        // re-reads the sheet only because the hook emptied its memo.
        assertNotNull("the outgoing source must still resolve on its own",
                outgoingSource.region(INTENT_SHEET_KEY, "intent_escape.png"));
        assertEquals("the swap must have cleared the outgoing source's own memo",
                2, first.sheetTextCalls);

        Sts1AtlasMaterializer.setTpsheetSource(null);

        assertNull("a restored null source must resolve to null",
                Sts1AtlasMaterializer.regionFromAtlas(INTENT_SHEET_KEY, "intent_escape.png"));
        assertEquals("the removed source must not be re-asked", 1, second.sheetTextCalls);
        assertEquals(Integer.valueOf(0), Sts1AtlasMaterializer.probeSlice().get("tpsheetRegionCount"));
    }

    private static void assertRegionEquals(String message, AtlasRegion expected, AtlasRegion actual) {
        assertNotNull(message + " (expected)", expected);
        assertNotNull(message + " (actual)", actual);
        assertEquals(message + " page", expected.page, actual.page);
        assertEquals(message + " name", expected.name, actual.name);
        assertEquals(message + " x", expected.x, actual.x);
        assertEquals(message + " y", expected.y, actual.y);
        assertEquals(message + " width", expected.width, actual.width);
        assertEquals(message + " height", expected.height, actual.height);
        assertEquals(message + " pageWidth", expected.pageWidth, actual.pageWidth);
        assertEquals(message + " pageHeight", expected.pageHeight, actual.pageHeight);
        assertEquals(message + " originalWidth", expected.originalWidth, actual.originalWidth);
        assertEquals(message + " originalHeight", expected.originalHeight, actual.originalHeight);
        assertEquals(message + " offsetX", expected.offsetX, actual.offsetX);
        assertEquals(message + " offsetY", expected.offsetY, actual.offsetY);
        assertEquals(message + " degrees", expected.degrees, actual.degrees);
        assertEquals(message + " trimmed", expected.trimmed(), actual.trimmed());
    }

    private static Texture newTexture() {
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Texture) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(Texture.class);
        } catch (Exception failure) {
            throw new AssertionError("could not create no-GL texture test double", failure);
        }
    }

    /**
     * Headless page texture: {@code Texture.getWidth()/getHeight()} delegate to a package-private
     * {@code data} field, mirrored here with a {@link TextureData} proxy so no GL context is needed.
     */
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

    private static final class StubLiveSource implements Sts1AtlasMaterializer.LiveAtlasSource {
        final Map<String, TextureAtlas> atlases = new LinkedHashMap<String, TextureAtlas>();
        boolean throwOnAtlas;
        int atlasCalls;

        public TextureAtlas atlas(String atlasKey) {
            atlasCalls++;
            if (throwOnAtlas) {
                throw new IllegalStateException("atlas boom");
            }
            return atlases.get(atlasKey);
        }
    }

    private static final class StubProvider implements Sts1AtlasMaterializer.AtlasProvider {
        final Map<String, String> texts = new LinkedHashMap<String, String>();
        final Map<String, Texture> pages = new LinkedHashMap<String, Texture>();
        final List<String> pageTextureKeys = new ArrayList<String>();
        boolean throwOnAtlasText;
        boolean throwOnPageTexture;
        int atlasTextCalls;
        int pageTextureCalls;

        public String atlasText(String atlasKey) {
            atlasTextCalls++;
            if (throwOnAtlasText) {
                throw new IllegalStateException("atlasText boom");
            }
            return texts.get(atlasKey);
        }

        public Texture pageTexture(String atlasKey, String pageName) {
            pageTextureCalls++;
            pageTextureKeys.add(atlasKey + "|" + pageName);
            if (throwOnPageTexture) {
                throw new IllegalStateException("pageTexture boom");
            }
            return pages.get(atlasKey + "|" + pageName);
        }
    }

    private static final class StubTpsheetProvider implements Sts2TpsheetAtlasSource.TpsheetProvider {
        final Map<String, String> sheets = new LinkedHashMap<String, String>();
        final Map<String, Texture> pages = new LinkedHashMap<String, Texture>();
        final List<String> pageKeys = new ArrayList<String>();
        final List<String> sheetsRead = new ArrayList<String>();
        int sheetTextCalls;

        public String sheetText(String sheetKey) {
            sheetTextCalls++;
            sheetsRead.add(sheetKey);
            return sheets.get(sheetKey);
        }

        public Texture pageTexture(String sheetKey, String pageName) {
            pageKeys.add(sheetKey + "|" + pageName);
            return pages.get(sheetKey + "|" + pageName);
        }
    }

    private static final class ThrowingTpsheetSource implements Sts1AtlasMaterializer.TpsheetSource {
        int calls;

        public Sts1AtlasMaterializer.RegionTexture region(String sheetKey, String regionName) {
            calls++;
            throw new IllegalStateException("tpsheet boom");
        }
    }

    /** Always answers with one fixed value; used to inject non-conforming SPI answers. */
    private static final class FixedTpsheetSource implements Sts1AtlasMaterializer.TpsheetSource {
        private final Sts1AtlasMaterializer.RegionTexture answer;

        FixedTpsheetSource(Sts1AtlasMaterializer.RegionTexture answer) {
            this.answer = answer;
        }

        public Sts1AtlasMaterializer.RegionTexture region(String sheetKey, String regionName) {
            return answer;
        }
    }

    /** Counts materializer-level asks while delegating to a real tpsheet source. */
    private static final class CountingTpsheetSource implements Sts1AtlasMaterializer.TpsheetSource {
        private final Sts1AtlasMaterializer.TpsheetSource delegate;
        int calls;

        CountingTpsheetSource(Sts1AtlasMaterializer.TpsheetSource delegate) {
            this.delegate = delegate;
        }

        public Sts1AtlasMaterializer.RegionTexture region(String sheetKey, String regionName) {
            calls++;
            return delegate.region(sheetKey, regionName);
        }

        public void clearCache() {
            delegate.clearCache();
        }
    }

    private static Sts1AtlasMaterializer.TpsheetSource onePageSource() {
        StubTpsheetProvider provider = new StubTpsheetProvider();
        provider.sheets.put(INTENT_SHEET_KEY, TPSHEET_ONE_PAGE);
        provider.pages.put(INTENT_SHEET_KEY + "|intent_atlas.png", newTexture());
        return new Sts2TpsheetAtlasSource(provider);
    }
}

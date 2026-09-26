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

    @After
    public void restoreDefaultProvider() {
        Sts1AtlasMaterializer.setProviderForTests(null);
        Sts1AtlasMaterializer.setLiveAtlasSource(null);
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
}

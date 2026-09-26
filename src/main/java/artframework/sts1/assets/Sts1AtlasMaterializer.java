package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import artframework.assets.LibGdxAtlasParser;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * STS1 host-side atlas materializer: turns a logical atlas key plus a region name into a page
 * {@link Texture} and a host-neutral {@link AtlasRegion} descriptor.
 *
 * <p>This is the generic legacy libGDX atlas counterpart to
 * {@link Spine42AtlasMaterializer} (which only rewrites Spine atlases into the legacy text form).
 * The actual text-to-region work is delegated to the pure {@link LibGdxAtlasParser}; this class
 * only owns the host seam (atlas text retrieval, page texture lookup, caching) and carries no
 * renderer wiring.
 *
 * <p><strong>Fail-open is mandatory.</strong> {@link #region(String, String)} never throws on
 * ordinary failure paths: a null/blank key or region name, an absent provider, missing atlas text,
 * a parse failure, a missing region, a missing page texture, or an invalid region all return
 * {@code null} so the caller can fall back to native rendering. Provider lookups and the parser call
 * are each guarded with a {@code Throwable} catch and the offending atlas key is memoized as failed.
 * Only unrecoverable JVM-level conditions (for example {@code OutOfMemoryError} during cache insert)
 * could still propagate.
 *
 * <p><strong>Invalid regions return null.</strong> A parsed region whose packed bounds do not lie
 * inside a positive-sized page (for example a {@code size: 0,0} page) is rejected, because its UV
 * rect would degrade to the whole-page {@code {0,0,1,1}} and draw the entire texture.
 *
 * <p><strong>Rotated regions are returned un-swapped.</strong> When {@code degrees != 0} the
 * neutral descriptor is returned exactly as parsed (no coordinate swap is applied). Consumers must
 * honor {@link AtlasRegion#rotated()}, {@link AtlasRegion#displayWidth()}, and
 * {@link AtlasRegion#displayHeight()} when mapping the region.
 *
 * <p><strong>Texture ownership.</strong> {@link #clearCache()} drops only ART's borrow records; it
 * deliberately does not dispose textures. Page textures are owned by the host provider, and ART
 * only borrows references.
 *
 * <p><strong>Threading.</strong> All state is guarded by a single monitor; no background threads
 * are created. Test seams {@link #setProviderForTests(AtlasProvider)} and {@link #clearCache()}
 * must be used from the same thread as normal calls.
 *
 * <p><strong>Host integration point.</strong> {@link #setProvider(AtlasProvider)} is the public SPI
 * a host binds to supply atlas text and page textures; passing {@code null} restores the inert
 * default (fail-open). Swapping the provider clears cached parsed regions and borrowed textures in
 * the same critical section, so a new provider never serves state borrowed from the previous one.
 * This class remains renderer-agnostic: it neither creates nor binds textures itself and carries no
 * {@code SpriteBatch} wiring; consumers map the returned descriptor to their own draw call.
 */
public final class Sts1AtlasMaterializer {

    /** Host-supplied atlas text plus page textures; injected so tests need no GL and no files. */
    public interface AtlasProvider {
        /** Legacy libGDX atlas text for an atlas key (for example {@code "cardui/frame"}), or null. */
        String atlasText(String atlasKey);

        /** Page texture for a page image name within an atlas, or null. */
        Texture pageTexture(String atlasKey, String pageName);
    }

    /** Host-supplied live libGDX atlas lookup (for example ImageMaster.vfxAtlas). */
    public interface LiveAtlasSource {
        com.badlogic.gdx.graphics.g2d.TextureAtlas atlas(String atlasKey);
    }

    /** Page texture plus neutral region descriptor; both fields are non-null on a resolved value. */
    public static final class RegionTexture {
        public final Texture texture;
        public final AtlasRegion region;

        public RegionTexture(Texture texture, AtlasRegion region) {
            this.texture = texture;
            this.region = region;
        }
    }

    /** Default seam: no host is wired yet, so every lookup fails open. */
    private static final AtlasProvider DEFAULT_PROVIDER = new AtlasProvider() {
        public String atlasText(String atlasKey) {
            return null;
        }

        public Texture pageTexture(String atlasKey, String pageName) {
            return null;
        }
    };

    private static final char KEY_SEPARATOR = '\u0001';
    private static final Object LOCK = new Object();

    private static AtlasProvider provider = DEFAULT_PROVIDER;
    private static LiveAtlasSource liveSource = null;

    /** atlasKey -> parsed regions (successful parses only). */
    private static final Map<String, List<AtlasRegion>> PARSED = new LinkedHashMap<String, List<AtlasRegion>>();
    /** atlas keys whose text was missing or whose parse threw; not retried until {@link #clearCache()}. */
    private static final Set<String> FAILED = new LinkedHashSet<String>();
    /** (atlasKey, regionName) -> region descriptor, first occurrence in file order wins. */
    private static final Map<String, AtlasRegion> REGIONS = new LinkedHashMap<String, AtlasRegion>();
    /** (atlasKey, pageName) -> borrowed page texture, non-null entries only. */
    private static final Map<String, Texture> PAGE_TEXTURES = new LinkedHashMap<String, Texture>();
    /** (atlasKey, pageName) pairs already asked and answered with null, so the provider is not spammed. */
    private static final Set<String> MISSING_PAGES = new HashSet<String>();
    /** Live atlas keys already asked and answered with null (or that threw); not re-asked until clearCache. */
    private static final Set<String> FAILED_LIVE = new LinkedHashSet<String>();
    /** (atlasKey, regionName) -> resolved live region borrow. */
    private static final Map<String, RegionTexture> LIVE_REGIONS = new LinkedHashMap<String, RegionTexture>();
    /** (atlasKey, regionName) pairs already asked and answered with a miss, so the source is not spammed. */
    private static final Set<String> MISSING_LIVE_REGIONS = new HashSet<String>();

    private Sts1AtlasMaterializer() {}

    /**
     * Resolves a region, or {@code null} (fail-open) when the key/provider/region/page is
     * unavailable. Never throws on ordinary failure paths (only JVM-level conditions such as
     * {@code OutOfMemoryError} could propagate).
     *
     * <p>The atlas text is parsed once per atlas key; parsed regions and borrowed page textures are
     * cached until {@link #clearCache()}. A key whose text is absent or unparseable is remembered as
     * failed and is not reparsed on every call.
     */
    public static RegionTexture region(String atlasKey, String regionName) {
        if (isBlank(atlasKey) || isBlank(regionName)) {
            return null;
        }
        synchronized (LOCK) {
            AtlasRegion region = regionFor(atlasKey, regionName);
            if (region == null) {
                return null;
            }
            // Invalid descriptor (for example a 0-sized page) would draw the whole page as a
            // {0,0,1,1} UV, so fail open to native instead.
            if (!region.valid()) {
                return null;
            }
            Texture texture = textureFor(atlasKey, region.page);
            if (texture == null) {
                return null;
            }
            return new RegionTexture(texture, region);
        }
    }

    /** Drops parsed-atlas, region, failed-atlas, and page-texture borrow records. */
    public static void clearCache() {
        synchronized (LOCK) {
            clearCachesLocked();
        }
    }

    /** Caller must hold {@link #LOCK}. Does not dispose textures (the host owns page textures). */
    private static void clearCachesLocked() {
        PARSED.clear();
        FAILED.clear();
        REGIONS.clear();
        PAGE_TEXTURES.clear();
        MISSING_PAGES.clear();
        FAILED_LIVE.clear();
        LIVE_REGIONS.clear();
        MISSING_LIVE_REGIONS.clear();
    }

    /**
     * Resolves a region from the live atlas source: {@code findRegion(name)} then
     * {@link Sts1GdxAtlasRegions#fromGdx}. Fail-open: a null/blank argument, an absent source, a
     * missing atlas, a missing region, an invalid region, or any provider/conversion failure
     * returns {@code null}; this method never throws on ordinary failure paths. Textures are
     * borrowed from the host and never disposed.
     *
     * <p>An atlas key whose {@code atlas(atlasKey)} returned {@code null} or whose source threw is
     * memoized as failed and not re-asked until {@link #clearCache()}. Resolved {@code
     * RegionTexture}s are cached by {@code (atlasKey, regionName)}, so repeated calls do not re-run
     * {@code findRegion}/conversion.
     */
    public static RegionTexture regionFromAtlas(String atlasKey, String regionName) {
        if (isBlank(atlasKey) || isBlank(regionName)) {
            return null;
        }
        synchronized (LOCK) {
            String cacheKey = regionKey(atlasKey, regionName);
            if (LIVE_REGIONS.containsKey(cacheKey)) {
                return LIVE_REGIONS.get(cacheKey);
            }
            if (MISSING_LIVE_REGIONS.contains(cacheKey)) {
                return null;
            }
            if (FAILED_LIVE.contains(atlasKey)) {
                return null;
            }
            if (liveSource == null) {
                return null;
            }
            TextureAtlas atlas;
            try {
                atlas = liveSource.atlas(atlasKey);
            } catch (Throwable ignored) {
                FAILED_LIVE.add(atlasKey);
                return null;
            }
            if (atlas == null) {
                // Remember the miss so the source is not re-asked for this atlas until clearCache.
                FAILED_LIVE.add(atlasKey);
                return null;
            }
            RegionTexture resolved;
            try {
                TextureAtlas.AtlasRegion gdx = atlas.findRegion(regionName);
                if (gdx == null) {
                    MISSING_LIVE_REGIONS.add(cacheKey);
                    return null;
                }
                AtlasRegion region = Sts1GdxAtlasRegions.fromGdx(gdx);
                if (region == null || !region.valid()) {
                    MISSING_LIVE_REGIONS.add(cacheKey);
                    return null;
                }
                resolved = new RegionTexture(gdx.getTexture(), region);
            } catch (Throwable ignored) {
                MISSING_LIVE_REGIONS.add(cacheKey);
                return null;
            }
            LIVE_REGIONS.put(cacheKey, resolved);
            return resolved;
        }
    }

    /**
     * Host-recreation lifecycle hook: drops borrowed-texture/region records (never disposes them).
     * Alias for {@link #clearCache()} under the same lock.
     */
    public static void onHostRecreated() {
        clearCache();
    }

    /** Counts only; never leaks Texture/AtlasRegion handles and never mutates state. */
    public static Map<String, Object> probeSlice() {
        synchronized (LOCK) {
            Map<String, Object> probe = new LinkedHashMap<String, Object>();
            probe.put("atlasCount", Integer.valueOf(PARSED.size()));
            probe.put("failedAtlasCount", Integer.valueOf(FAILED.size()));
            probe.put("regionCount", Integer.valueOf(REGIONS.size()));
            probe.put("textureCount", Integer.valueOf(PAGE_TEXTURES.size()));
            probe.put("liveRegionCount", Integer.valueOf(LIVE_REGIONS.size()));
            return probe;
        }
    }

    /**
     * Installs the host atlas provider. Passing {@code null} restores the inert default
     * (fail-open: every lookup returns {@code null}).
     *
     * <p>This is the public host integration point for this class. Swapping the provider clears all
     * cached parsed regions, failed-atlas marks, borrowed textures, and missing-page marks in the
     * same critical section, so a new provider never serves state borrowed from the previous one.
     */
    public static void setProvider(AtlasProvider provider) {
        synchronized (LOCK) {
            Sts1AtlasMaterializer.provider = provider != null ? provider : DEFAULT_PROVIDER;
            // Clear in the same critical section: a new provider must never serve regions or
            // textures borrowed from the previous one.
            clearCachesLocked();
        }
    }

    /** Test seam: installs a provider; null restores the default (which returns null => fail-open). */
    static void setProviderForTests(AtlasProvider replacement) {
        setProvider(replacement);
    }

    /**
     * Installs the live atlas source; {@code null} restores the inert default (fail-open). Clears
     * the caches in the same critical section, so a new source never serves stale borrows.
     */
    public static void setLiveAtlasSource(LiveAtlasSource source) {
        synchronized (LOCK) {
            liveSource = source;
            clearCachesLocked();
        }
    }

    private static AtlasRegion regionFor(String atlasKey, String regionName) {
        if (FAILED.contains(atlasKey)) {
            return null;
        }
        if (!PARSED.containsKey(atlasKey)) {
            // A malformed parse is remembered so the provider is not asked again until clearCache.
            if (!parseAtlas(atlasKey)) {
                return null;
            }
        }
        return REGIONS.get(regionKey(atlasKey, regionName));
    }

    private static boolean parseAtlas(String atlasKey) {
        String text;
        try {
            text = provider.atlasText(atlasKey);
        } catch (Throwable ignored) {
            FAILED.add(atlasKey);
            return false;
        }
        if (text == null) {
            FAILED.add(atlasKey);
            return false;
        }
        List<AtlasRegion> parsed;
        try {
            parsed = LibGdxAtlasParser.parse(text);
        } catch (Throwable ignored) {
            // Guarded the same way as provider.atlasText so region() never throws on ordinary
            // failure paths (malformed text, NPE inside the parser, linkage/assertion errors).
            FAILED.add(atlasKey);
            return false;
        }
        List<AtlasRegion> regions = parsed != null ? parsed : Collections.<AtlasRegion>emptyList();
        PARSED.put(atlasKey, regions);
        for (AtlasRegion region : regions) {
            if (region == null) {
                continue;
            }
            String key = regionKey(atlasKey, region.name);
            if (!REGIONS.containsKey(key)) {
                REGIONS.put(key, region);
            }
        }
        return true;
    }

    private static Texture textureFor(String atlasKey, String pageName) {
        String key = regionKey(atlasKey, pageName);
        if (PAGE_TEXTURES.containsKey(key)) {
            return PAGE_TEXTURES.get(key);
        }
        if (MISSING_PAGES.contains(key)) {
            return null;
        }
        Texture texture;
        try {
            texture = provider.pageTexture(atlasKey, pageName);
        } catch (Throwable ignored) {
            texture = null;
        }
        if (texture == null) {
            // Remember the miss so the provider is not asked again for this page until clearCache.
            MISSING_PAGES.add(key);
            return null;
        }
        PAGE_TEXTURES.put(key, texture);
        return texture;
    }

    private static String regionKey(String atlasKey, String name) {
        return atlasKey + KEY_SEPARATOR + name;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

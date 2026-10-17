package artframework.sts1.assets;

import artframework.assets.AtlasRegion;
import com.badlogic.gdx.graphics.Texture;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code .tpsheet} REGION RESOLUTION source for {@link Sts1AtlasMaterializer}: turns a
 * TexturePacker JSON sheet key plus a region name into the borrowed page {@link Texture} and the
 * host-neutral {@link AtlasRegion} descriptor.
 *
 * <p>The text-to-region work is delegated to the pure {@link Sts2TpsheetParser}; this class owns
 * only the host seam (sheet text retrieval, page texture lookup, per-sheet memoization) and carries
 * no renderer wiring, no GL calls, and no filesystem knowledge. {@link #fromBundle} is the
 * bundle-backed factory: it reads the sheet and the sibling page PNG entry through
 * {@link Sts2AssetBundle} and decodes the page only through an injected
 * {@link PageTextureDecoder}.
 *
 * <p><strong>Fail-open is mandatory.</strong> {@link #region(String, String)} never throws: a null
 * provider, a null/blank sheet key or region name, missing/blank/throwing sheet text, an
 * unparseable (empty) sheet, an absent region, a missing/throwing page texture, or an invalid
 * region all return {@code null} so the caller can fall back to native rendering. Every provider
 * and parser call is guarded with a {@code Throwable} catch.
 *
 * <p><strong>Sheet text is read once per sheet key.</strong> The first successful parse memoizes a
 * {@code filename -> AtlasRegion} map (first occurrence in file order wins), and a missing, blank,
 * throwing, or empty-parse sheet is memoized as failed so the provider is not re-asked until
 * {@link #clearCache()}. Page textures are deliberately <em>not</em> cached here; the materializer
 * owns the resolved {@code RegionTexture} cache.
 *
 * <p><strong>Texture ownership.</strong> Page textures are borrowed from the host provider (or the
 * injected decoder) and are never disposed; {@link #clearCache()} drops only memoized text/region
 * records.
 *
 * <p><strong>Threading.</strong> All state is guarded by one private monitor. Calls are expected to
 * come from the materializer, which may hold its own lock while calling in; this class never calls
 * back into {@link Sts1AtlasMaterializer}, so the lock order stays one-way and cannot deadlock.
 */
public final class Sts2TpsheetAtlasSource implements Sts1AtlasMaterializer.TpsheetSource {

    /** Host-supplied sheet text plus the page texture for a sheet/page pair; injected for tests. */
    public interface TpsheetProvider {
        /** TexturePacker JSON text for a sheet key, or null when the sheet is unavailable. */
        String sheetText(String sheetKey);

        /** Page texture for a page image name within a sheet, or null when it is unavailable. */
        Texture pageTexture(String sheetKey, String pageName);
    }

    /** Host-supplied page decoder: PNG bytes plus page identity to a borrowed texture. */
    public interface PageTextureDecoder {
        /** Decodes page PNG bytes; returns the borrowed texture, or null when decoding fails. */
        Texture decode(String sheetKey, String pageName, byte[] pngBytes);
    }

    private static final char PATH_SEPARATOR = '/';

    private final TpsheetProvider provider;
    private final Object lock = new Object();

    /** sheetKey -> (region filename -> descriptor), successful parses only. */
    private final Map<String, Map<String, AtlasRegion>> sheetRegions =
            new LinkedHashMap<String, Map<String, AtlasRegion>>();
    /** sheet keys whose text was missing/blank/empty or whose parse threw; not retried until clear. */
    private final Set<String> failedSheets = new LinkedHashSet<String>();

    /**
     * Wraps a host provider. A null provider yields an inert source: every lookup returns
     * {@code null}.
     */
    public Sts2TpsheetAtlasSource(TpsheetProvider provider) {
        this.provider = provider;
    }

    /**
     * Bundle-backed factory. Reads the sheet entry ({@code sheetKey}) as UTF-8 text and, for a
     * page, the sibling page entry via {@link #pageEntry(String, String)} bytes decoded through
     * {@code decoder}. No GL calls are made here.
     *
     * <p>A null bundle or decoder yields an inert source (fail-open: every lookup returns null).
     */
    public static Sts2TpsheetAtlasSource fromBundle(
            final Sts2AssetBundle bundle, final PageTextureDecoder decoder) {
        if (bundle == null || decoder == null) {
            return new Sts2TpsheetAtlasSource(null);
        }
        return new Sts2TpsheetAtlasSource(new TpsheetProvider() {
            public String sheetText(String sheetKey) {
                try {
                    byte[] bytes = bundle.read(sheetKey);
                    return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
                } catch (Throwable ignored) {
                    return null;
                }
            }

            public Texture pageTexture(String sheetKey, String pageName) {
                String entry = pageEntry(sheetKey, pageName);
                if (entry == null) {
                    return null;
                }
                try {
                    byte[] bytes = bundle.read(entry);
                    if (bytes == null) {
                        return null;
                    }
                    return decoder.decode(sheetKey, pageName, bytes);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        });
    }

    /**
     * Resolves the bundle entry of a page that is a sibling of its sheet file: the sheet
     * directory plus the page name ({@code animations/images/atlases/intent_atlas.tpsheet} +
     * {@code intent_atlas.png} -> {@code animations/images/atlases/intent_atlas.png}).
     *
     * <p>Backslashes are normalized to {@code '/'}. Returns {@code null} for a null/blank sheet key
     * or page name, and rejects a page name that contains a path separator after normalization
     * (nested subdirectories and {@code ../…} traversal) or that is exactly {@code ".."}.
     */
    public static String pageEntry(String sheetKey, String pageName) {
        if (isBlank(sheetKey) || isBlank(pageName)) {
            return null;
        }
        String key = sheetKey.replace('\\', PATH_SEPARATOR).trim();
        String page = pageName.replace('\\', PATH_SEPARATOR).trim();
        if (page.indexOf(PATH_SEPARATOR) >= 0 || "..".equals(page)) {
            return null;
        }
        int lastSeparator = key.lastIndexOf(PATH_SEPARATOR);
        if (lastSeparator < 0) {
            return page;
        }
        return key.substring(0, lastSeparator + 1) + page;
    }

    /**
     * Resolves a region from the sheet's parsed descriptor set. Fail-open: returns {@code null} for
     * a null provider, a null/blank argument, a missing/blank/throwing/unparseable sheet, an absent
     * region, a missing/throwing page texture, or a region whose packed bounds are invalid. Never
     * throws on ordinary failure paths.
     */
    public Sts1AtlasMaterializer.RegionTexture region(String sheetKey, String regionName) {
        if (provider == null || isBlank(sheetKey) || isBlank(regionName)) {
            return null;
        }
        synchronized (lock) {
            Map<String, AtlasRegion> regions = regionsFor(sheetKey);
            if (regions == null) {
                return null;
            }
            AtlasRegion region = regions.get(regionName);
            // Invalid descriptor (for example a 0-sized page) would draw the whole page as a
            // {0,0,1,1} UV, so fail open to native instead.
            if (region == null || !region.valid()) {
                return null;
            }
            Texture texture;
            try {
                texture = provider.pageTexture(sheetKey, region.page);
            } catch (Throwable ignored) {
                texture = null;
            }
            if (texture == null) {
                return null;
            }
            return new Sts1AtlasMaterializer.RegionTexture(texture, region);
        }
    }

    /** Drops memoized parsed-sheet maps and failed-sheet marks. Borrowed textures are not cached. */
    public void clearCache() {
        synchronized (lock) {
            sheetRegions.clear();
            failedSheets.clear();
        }
    }

    /** Caller must hold {@link #lock}. Returns null when the sheet cannot be parsed. */
    private Map<String, AtlasRegion> regionsFor(String sheetKey) {
        if (failedSheets.contains(sheetKey)) {
            return null;
        }
        Map<String, AtlasRegion> cached = sheetRegions.get(sheetKey);
        if (cached != null) {
            return cached;
        }
        String text;
        try {
            text = provider.sheetText(sheetKey);
        } catch (Throwable ignored) {
            failedSheets.add(sheetKey);
            return null;
        }
        if (isBlank(text)) {
            // Remember the miss so the provider is not asked again for this sheet until clearCache.
            failedSheets.add(sheetKey);
            return null;
        }
        List<AtlasRegion> parsed;
        try {
            parsed = Sts2TpsheetParser.parse(text);
        } catch (Throwable ignored) {
            // Guarded like sheetText so region() never throws on ordinary failure paths.
            failedSheets.add(sheetKey);
            return null;
        }
        if (parsed == null || parsed.isEmpty()) {
            // An empty parse means "no usable regions"; treat it as a memoized miss rather than
            // re-reading and re-parsing the sheet on every call.
            failedSheets.add(sheetKey);
            return null;
        }
        Map<String, AtlasRegion> regions = new LinkedHashMap<String, AtlasRegion>();
        for (AtlasRegion region : parsed) {
            if (region == null) {
                continue;
            }
            // First occurrence in file order wins, matching the legacy materializer's region cache.
            if (!regions.containsKey(region.name)) {
                regions.put(region.name, region);
            }
        }
        Map<String, AtlasRegion> immutable = Collections.unmodifiableMap(regions);
        sheetRegions.put(sheetKey, immutable);
        return immutable;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

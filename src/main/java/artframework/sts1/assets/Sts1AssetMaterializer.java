package artframework.sts1.assets;

import artframework.assets.AssetResolveResult;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.helpers.ImageMaster;

import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** STS1-only bridge from resolved logical asset sources to host textures. */
public final class Sts1AssetMaterializer {

    private static final BoundedTextureCache<Texture> DEFAULT_CACHE = new BoundedTextureCache<Texture>(256,
            new Disposer<Texture>() {
                public void dispose(Texture texture) {
                    if (texture != null) texture.dispose();
                }
            });
    private static TextureCache<Texture> CACHE = DEFAULT_CACHE;
    private static final String MATERIALIZER_ID = Sts1AssetMaterializer.class.getName();
    private static volatile EnergyAttribution energyAttribution;

    private Sts1AssetMaterializer() {}

    /** Small lifecycle seam for pure tests and hosts that own texture disposal. */
    public interface TextureCache<T> {
        T get(String key);
        void put(String key, T value);
        boolean isMissing(String key);
        void markMissing(String key);
        void clear();
        int size();
    }

    public interface Disposer<T> {
        void dispose(T value);
    }

    public static final class BoundedTextureCache<T> implements TextureCache<T> {
        private final Map<String, T> values;
        private final Map<String, Boolean> missing;
        private final int capacity;
        private final Disposer<T> disposer;
        private boolean disposed;
        private int putCount;
        private int disposeCount;
        private int hitCount;

        public BoundedTextureCache() {
            this(256, new Disposer<T>() {
                public void dispose(T value) {
                    if (value instanceof AutoCloseable) {
                        try { ((AutoCloseable) value).close(); } catch (Exception ignored) { }
                    }
                }
            });
        }

        public BoundedTextureCache(int capacity, Disposer<T> disposer) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
            if (disposer == null) throw new NullPointerException("disposer");
            this.capacity = capacity;
            this.disposer = disposer;
            this.values = new LinkedHashMap<String, T>(capacity, 0.75f, true);
            this.missing = new LinkedHashMap<String, Boolean>(capacity, 0.75f, true);
        }

        public synchronized T get(String key) {
            T value = values.get(key);
            if (value != null) hitCount++;
            return value;
        }
        public synchronized void put(String key, T value) {
            if (disposed) throw new IllegalStateException("cache disposed");
            T previous = values.put(key, value);
            putCount++;
            if (previous != null && previous != value) disposeIfUnreferenced(previous);
            missing.remove(key);
            while (values.size() > capacity) {
                String eldest = values.keySet().iterator().next();
                T evicted = values.remove(eldest);
                if (evicted != null) disposeIfUnreferenced(evicted);
            }
        }
        public synchronized boolean isMissing(String key) { return missing.get(key) != null; }
        public synchronized void markMissing(String key) {
            if (disposed) throw new IllegalStateException("cache disposed");
            missing.put(key, Boolean.TRUE);
            while (missing.size() > capacity) {
                String eldest = missing.keySet().iterator().next();
                missing.remove(eldest);
            }
        }
        public synchronized int size() { return values.size(); }
        public synchronized Map<String, Object> probeSlice() {
            return diagnosticProbe(values.size(), missing.size(), putCount, disposeCount, hitCount);
        }
        synchronized void resetForTests() {
            values.clear();
            missing.clear();
            putCount = 0;
            disposeCount = 0;
            hitCount = 0;
            disposed = false;
        }
        public synchronized void clear() {
            if (disposed) return;
            Set<T> disposedValues = java.util.Collections.newSetFromMap(
                    new IdentityHashMap<T, Boolean>());
            // Detach first: cleanup is state-first, so a failing disposer cannot leave stale
            // entries available for reuse or prevent later resident identities being attempted.
            java.util.List<T> resident = new ArrayList<T>(values.values());
            values.clear();
            missing.clear();
            Throwable failure = null;
            for (T value : resident) {
                if (value != null && disposedValues.add(value)) {
                    disposeCount++;
                    try {
                        disposer.dispose(value);
                    } catch (Throwable t) {
                        failure = rememberCleanupFailure(failure, t);
                    }
                }
            }
            if (failure != null) rethrowCleanupFailure(failure);
        }
        public synchronized void dispose() {
            Throwable failure = null;
            try {
                clear();
            } catch (Throwable t) {
                failure = t;
            } finally {
                disposed = true;
            }
            if (failure != null) rethrowCleanupFailure(failure);
        }

        private void disposeIfUnreferenced(T value) {
            for (T cached : values.values()) {
                if (cached == value) return;
            }
            disposeCount++;
            disposer.dispose(value);
        }

        private static void rethrowCleanupFailure(Throwable failure) {
            if (failure instanceof RuntimeException) throw (RuntimeException) failure;
            if (failure instanceof Error) throw (Error) failure;
            throw new RuntimeException("cache resource cleanup failed", failure);
        }

        private static Throwable rememberCleanupFailure(Throwable first, Throwable next) {
            if (first == null) return next;
            // A disposer may deliberately reuse one failure instance. Self-suppression would
            // throw from the aggregation path and prevent the remaining residents being tried.
            if (first != next) {
                try {
                    first.addSuppressed(next);
                } catch (Throwable ignored) {
                    // Failure aggregation must never interrupt the remaining cleanup attempts.
                }
            }
            return first;
        }
    }

    public static boolean isFileBacked(String source) {
        String path = normalize(source);
        return path.startsWith("images/");
    }

    public static boolean isLogicalCardArt(String source) {
        return normalize(source).startsWith("card/art/");
    }

    public static boolean isCardFrameAtlas(String source) {
        return "cardui/frame".equals(normalize(source));
    }

    public static String normalize(String source) {
        if (source == null) {
            return "";
        }
        return source.startsWith(Sts1VanillaCatalog.SOURCE_PREFIX)
                ? source.substring(Sts1VanillaCatalog.SOURCE_PREFIX.length())
                : source;
    }

    public static Texture resolveTexture(AssetResolveResult result) {
        return result != null && result.found ? resolveTexture(result.source) : null;
    }

    public static Texture resolveTexture(String source) {
        if (!isFileBacked(source)) {
            return null;
        }
        String path = normalize(source);
        Texture cached = CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        if (CACHE.isMissing(path)) {
            return null;
        }
        try {
            Texture texture = ImageMaster.loadImage(path);
            if (texture != null) {
                CACHE.put(path, texture);
            } else {
                CACHE.markMissing(path);
            }
            return texture;
        } catch (Throwable ignored) {
            CACHE.markMissing(path);
            return null;
        }
    }

    /**
     * Materializes the resource used by the delegated energy draw and records its source only
     * after a texture was actually obtained.  This is intentionally separate from generic
     * materialization: a cache hit for some other surface must not be attributed to energy.
     */
    public static Texture resolveEnergyTexture(String resourceId, AssetResolveResult result) {
        // Validate the logical identity before touching the generic materializer.  In
        // particular, resolveTexture(result) may hit a path cached for a different logical
        // resource, which is not evidence that this energy resource was resolved.
        if (!isExpectedEnergyResolution(resourceId, result)) {
            return null;
        }
        Texture texture = resolveTexture(result);
        if (texture != null) recordSuccessfulEnergyResolution(result);
        return texture;
    }

    private static boolean isExpectedEnergyResolution(String resourceId, AssetResolveResult result) {
        return resourceId != null
                && resourceId.startsWith("ui.energy.")
                && result != null
                && result.found
                && resourceId.equals(result.resourceId)
                && isFileBacked(result.source);
    }

    /** Records only an actual energy resource resolution; invalid/non-energy observations clear it. */
    private static void recordSuccessfulEnergyResolution(AssetResolveResult result) {
        if (result != null && result.found
                && result.resourceId.startsWith("ui.energy.")
                && isFileBacked(result.source)) {
            energyAttribution = new EnergyAttribution(result.resourceId, normalize(result.source));
        } else {
            energyAttribution = null;
        }
    }

    /** Pure selector retained separately so catalog and renderer behavior are unit-testable. */
    public static String cardFrameAtlasKey(String cardType, String rarity) {
        String type = "SKILL".equals(cardType) ? "skill" : "POWER".equals(cardType) ? "power" : "attack";
        String tier = "RARE".equals(rarity) ? "rare" : "UNCOMMON".equals(rarity) ? "uncommon" : "common";
        return type + "." + tier;
    }

    public static void clearCache() {
        try {
            CACHE.clear();
        } finally {
            // The texture is no longer resident after clear, so attribution must be rebuilt by
            // the next successful energy draw rather than surviving host recreation.
            energyAttribution = null;
        }
    }

    /**
     * Host-recreation lifecycle entry point. Materialized textures are disposable host state;
     * logical asset sources and presentation declarations remain owned by the caller and are
     * resolved again on the next materialization.
     */
    public static void onHostRecreated() {
        clearCache();
    }

    public static Map<String, Object> probeSlice() {
        // Additive diagnostics only: resident/missing drop on clear; put/dispose/hit survive.
        // disposeCount is attempted distinct identities, not GL success.
        if (CACHE instanceof BoundedTextureCache) {
            return ((BoundedTextureCache<?>) CACHE).probeSlice();
        }
        return diagnosticProbe(CACHE.size(), 0, 0, 0, 0);
    }

    /** Pure diagnostic snapshot; does not consult or mutate the texture cache. */
    public static Map<String, Object> energyAttributionProbe() {
        EnergyAttribution attribution = energyAttribution;
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("attributed", Boolean.valueOf(attribution != null));
        if (attribution != null) {
            m.put("resourceId", attribution.resourceId);
            m.put("source", attribution.source);
            m.put("normalizedPath", attribution.source);
            m.put("materializerId", MATERIALIZER_ID);
        }
        return m;
    }

    /** Package-scoped cache substitution for no-GL lifecycle tests. */
    static void setCacheForTests(TextureCache<Texture> cache) {
        CACHE = cache != null ? cache : DEFAULT_CACHE;
    }

    static void resetForTests() {
        DEFAULT_CACHE.resetForTests();
        CACHE = DEFAULT_CACHE;
        energyAttribution = null;
    }

    static Map<String, Object> diagnosticProbe(
            int residentCount, int missingCount, int putCount, int disposeCount, int hitCount) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("residentCount", Integer.valueOf(residentCount));
        m.put("missingCount", Integer.valueOf(missingCount));
        m.put("putCount", Integer.valueOf(putCount));
        m.put("disposeCount", Integer.valueOf(disposeCount));
        m.put("hitCount", Integer.valueOf(hitCount));
        EnergyAttribution attribution = energyAttribution;
        if (attribution != null) {
            Map<String, Object> energy = new LinkedHashMap<String, Object>();
            energy.put("resourceId", attribution.resourceId);
            energy.put("sourcePath", attribution.source);
            energy.put("materializer", MATERIALIZER_ID);
            energy.put("successfulResolution", Boolean.TRUE);
            m.put("energyAttribution", energy);
        }
        return m;
    }

    private static final class EnergyAttribution {
        private final String resourceId;
        private final String source;

        private EnergyAttribution(String resourceId, String source) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.source = source != null ? source : "";
        }
    }
}

package artframework.vfx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure, host-neutral support matrix for the restricted STS2 VFX material/blend names.
 *
 * <p>This is deliberately a bounded allow-list, NOT a shader system and NOT an STS2-equivalent
 * material/bake path. The renderer ({@code Sts1VfxOverlayRenderer}) only ever maps a handful of
 * blend names to fixed GL blend functions; this class names exactly which source names that mapping
 * actually supports, canonicalizes their case, and routes everything else to the single documented
 * fail-open fallback ({@link #FALLBACK}). Unknown, blank, {@code null}, and the known-but-unsupported
 * {@code SUB} name all resolve to {@code MIX} with no throw.</p>
 *
 * <p>Rejected (fallback) names are counted in a fixed-capacity diagnostic map so a host probe can
 * observe unseen material names without unbounded growth. The counters are intentionally static and
 * host-neutral; {@link #resetForTests()} clears them for deterministic tests.</p>
 */
public final class VfxMaterialSupport {
    /** The single fail-open fallback blend name the renderer uses for every unsupported input. */
    public static final String FALLBACK = "MIX";
    /** Fixed cap for distinct rejected-name keys; further distinct names go to {@link #rejectedOverflow}. */
    public static final int MAX_REJECTED_KEYS = 16;

    private static final List<String> KNOWN = Collections.unmodifiableList(
            Arrays.asList("MIX", "ADD", "SUB", "MUL", "PREMULT_ALPHA"));
    private static final List<String> SUPPORTED = Collections.unmodifiableList(
            Arrays.asList("MIX", "ADD", "MUL", "PREMULT_ALPHA"));
    private static final List<String> UNSUPPORTED = Collections.unmodifiableList(
            Arrays.asList("SUB"));

    private static final Object LOCK = new Object();
    private static final LinkedHashMap<String, Integer> rejected = new LinkedHashMap<String, Integer>();
    private static int rejectedOverflow;

    private VfxMaterialSupport() {}

    /** Requested -> resolved/failure outcome for one arbitrary material/blend input name. */
    public static final class Resolution {
        /** The verbatim input as supplied (may be {@code null}). */
        public final String requested;
        /** The canonical name the renderer will actually use ({@link #FALLBACK} on rejection). */
        public final String resolved;
        /** Whether {@link #resolved} is a renderer-supported name. */
        public final boolean supported;
        /** Whether the resolver fell open to {@link #FALLBACK}. */
        public final boolean fallback;

        Resolution(String requested, String resolved, boolean supported, boolean fallback) {
            this.requested = requested;
            this.resolved = resolved;
            this.supported = supported;
            this.fallback = fallback;
        }
    }

    /**
     * Resolves an arbitrary input name. Known-supported names canonicalize to their upper-case form
     * and are reported supported without fallback; known-unsupported ({@code SUB}), unknown, blank,
     * and {@code null} inputs resolve to {@link #FALLBACK} with {@code supported=false} and
     * {@code fallback=true}. This method never throws and records every rejected input for the probe.
     */
    public static Resolution resolve(String requested) {
        try {
            if (requested == null) {
                recordRejection("(null)");
                return new Resolution(requested, FALLBACK, false, true);
            }
            String trimmed = requested.trim();
            if (trimmed.isEmpty()) {
                recordRejection("(blank)");
                return new Resolution(requested, FALLBACK, false, true);
            }
            String canonical = trimmed.toUpperCase(Locale.ROOT);
            if (isSupportedCanonical(canonical)) {
                return new Resolution(requested, canonical, true, false);
            }
            recordRejection(canonical);
            return new Resolution(requested, FALLBACK, false, true);
        } catch (Throwable ignored) {
            return new Resolution(requested, FALLBACK, false, true);
        }
    }

    /** Whether the input resolves to a renderer-supported blend name. Never throws. */
    public static boolean isSupported(String requested) {
        return resolve(requested).supported;
    }

    /** Whether the input is any known STS2 blend name (supported or unsupported). Never throws. */
    public static boolean isKnown(String requested) {
        try {
            if (requested == null) return false;
            String canonical = requested.trim().toUpperCase(Locale.ROOT);
            return KNOWN.contains(canonical);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** The canonical name the renderer will use for the input; {@link #FALLBACK} on rejection. */
    public static String resolvedName(String requested) {
        return resolve(requested).resolved;
    }

    /** The known STS2 blend names in canonical order. */
    public static List<String> knownNames() {
        return KNOWN;
    }

    /** The known names the renderer maps to real blend functions. */
    public static List<String> supportedNames() {
        return SUPPORTED;
    }

    /** The known names the renderer explicitly rejects with the fail-open fallback. */
    public static List<String> unsupportedNames() {
        return UNSUPPORTED;
    }

    /**
     * Read-only, fail-open diagnostic slice: {@code {known, supported, unsupported, rejected,
     * rejectedOverflow}}. Never throws; on error returns the key set with empty collections.
     */
    public static Map<String, Object> probeSlice() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        try {
            out.put("known", new ArrayList<String>(KNOWN));
            out.put("supported", new ArrayList<String>(SUPPORTED));
            out.put("unsupported", new ArrayList<String>(UNSUPPORTED));
            synchronized (LOCK) {
                out.put("rejected", new LinkedHashMap<String, Integer>(rejected));
                out.put("rejectedOverflow", Integer.valueOf(rejectedOverflow));
            }
        } catch (Throwable ignored) {
            out.put("known", Collections.<String>emptyList());
            out.put("supported", Collections.<String>emptyList());
            out.put("unsupported", Collections.<String>emptyList());
            out.put("rejected", Collections.<String, Integer>emptyMap());
            out.put("rejectedOverflow", Integer.valueOf(0));
        }
        return out;
    }

    /** Clears the rejection diagnostics. Test-only; production counters are process-lifetime. */
    public static void resetForTests() {
        synchronized (LOCK) {
            rejected.clear();
            rejectedOverflow = 0;
        }
    }

    private static void recordRejection(String key) {
        try {
            synchronized (LOCK) {
                Integer current = rejected.get(key);
                if (current != null) {
                    rejected.put(key, Integer.valueOf(current.intValue() + 1));
                    return;
                }
                if (rejected.size() >= MAX_REJECTED_KEYS) {
                    rejectedOverflow++;
                    return;
                }
                rejected.put(key, Integer.valueOf(1));
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSupportedCanonical(String canonical) {
        return "MIX".equals(canonical) || "ADD".equals(canonical)
                || "MUL".equals(canonical) || "PREMULT_ALPHA".equals(canonical);
    }
}

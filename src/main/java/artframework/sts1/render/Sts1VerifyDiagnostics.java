package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic state for the `art verify` console surface. Visual verification is developer-only and
 * must never claim pixel behavior the host cannot deliver; the background mode is now supported by
 * the verified pre-native draw point in {@code artframework/sts1/patch/BackgroundRenderPatches.java},
 * while guides/bounds are ART-owned overlays at {@code VERIFY_GUIDES} and need no host boundary.
 */
public final class Sts1VerifyDiagnostics {
    public enum Mode {
        OFF,
        BACKGROUND,
        BACKGROUND_ONLY,
        GUIDES,
        BOUNDS
    }

    private static Mode configured = Mode.OFF;
    private static String lastError;

    private Sts1VerifyDiagnostics() {}

    public static Mode configuredMode() {
        return configured;
    }

    public static void setMode(Mode mode) {
        configured = mode == null ? Mode.OFF : mode;
        if (configured != Mode.BACKGROUND_ONLY && BackgroundOnlyGate.isActive()) {
            BackgroundOnlyGate.setActive(false);
            clearNativeFilters();
        }
    }

    public static void setBackgroundOnly(boolean enabled) {
        if (enabled) {
            configured = Mode.BACKGROUND_ONLY;
            if (backgroundVariant() == BackgroundRenderGate.Variant.OFF) {
                setBackgroundVariant(BackgroundRenderGate.Variant.SOLID);
            }
            enableNativeFilter(BackgroundRenderGate.BACKGROUND_FAMILY);
            BackgroundOnlyGate.setActive(true);
        } else {
            configured = Mode.OFF;
            BackgroundOnlyGate.setActive(false);
            clearNativeFilters();
        }
    }

    public static boolean backgroundOnlyActive() { return BackgroundOnlyGate.isActive(); }

    /** Whether the configured mode can actually submit pixels through the current host boundary. */
    public static boolean modeSupported() {
        if (configured == Mode.GUIDES || configured == Mode.BOUNDS) {
            return true;
        }
        if (configured == Mode.OFF) {
            return true;
        }
        if (configured == Mode.BACKGROUND_ONLY) {
            return false;
        }
        return Sts1RenderBoundary.backgroundCapability()
                == Sts1RenderBoundary.BackgroundCapability.PRE_NATIVE;
    }

    /**
     * Background variant selection is independent of {@link Mode#BACKGROUND}: a variant can be
     * configured with the mode enabled and the verified pre-native hook supplies the pixels.
     */
    public static void setBackgroundVariant(BackgroundRenderGate.Variant variant) {
        BackgroundRenderGate.setVariant(variant);
    }

    public static BackgroundRenderGate.Variant backgroundVariant() {
        return BackgroundRenderGate.variant();
    }

    /** Whether the configured mode paints an ART-owned overlay this frame. */
    public static boolean overlayDrawEnabled() {
        return configured == Mode.GUIDES || configured == Mode.BOUNDS;
    }

    public static String submissionStatus() {
        if (configured == Mode.OFF) {
            return "disabled";
        }
        return modeSupported() ? "ready" : "unsupported";
    }

    /**
     * Enables native filtering for one family. The scope can only downgrade a would-be delegation
     * to native continuation; it never grants new permission to suppress native pixels.
     */
    public static void enableNativeFilter(String family) {
        String key = normalizeFamily(family);
        if (key == null) return;
        NativeRenderBridge.filterFamily(key);
    }

    /** Removes one family from the active native filter set (no-op when absent). */
    public static void disableNativeFilter(String family) {
        NativeRenderBridge.unfilterFamily(normalizeFamily(family));
    }

    /** Clears every native filter family and the underlying scope active flag. */
    public static void clearNativeFilters() {
        NativeRenderBridge.clearFilterScopes();
    }

    public static boolean isNativeFilterEnabled(String family) {
        String key = normalizeFamily(family);
        if (key == null) return false;
        return NativeRenderBridge.filterScope().isFiltered(key);
    }

    private static String normalizeFamily(String family) {
        if (family == null) return null;
        String trimmed = family.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void recordError(Throwable error) {
        if (error == null) {
            lastError = "unknown";
            return;
        }
        String message = error.getMessage();
        lastError = error.getClass().getSimpleName()
                + (message == null || message.isEmpty() ? "" : ":" + message);
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("configuredMode", configured.name().toLowerCase());
        m.put("submissionStatus", submissionStatus());
        m.put("modeSupported", Boolean.valueOf(modeSupported()));
        m.put("nativeInterval", Sts1RenderBoundary.nativeInterval());
        m.put("artInterval", Sts1RenderBoundary.artSubmissionInterval());
        m.put("nativeFilters", NativeRenderBridge.filterScopeProbeSlice());
        m.put("nativeIsolation", NativeRenderBridge.policy().probeSlice());
        m.put("nativeExemptionProjection",
                Sts1NativePresentationAdapter.exemptionProjectionProbeSlice());
        m.put("background", BackgroundRenderGate.probeSlice());
        m.put("backgroundOnly", BackgroundOnlyGate.probeSlice());
        if (lastError != null) {
            m.put("lastError", lastError);
        }
        return m;
    }

    public static void resetForTests() {
        configured = Mode.OFF;
        lastError = null;
        NativeRenderBridge.clearFilterScopes();
        NativeRenderBridge.policy().reset();
        BackgroundRenderGate.resetForTests();
        BackgroundOnlyGate.resetForTests();
    }
}

package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit STS1 host boundary used by render-order diagnostics. */
public final class Sts1RenderBoundary {
    public enum BackgroundCapability {
        UNSUPPORTED,
        PRE_NATIVE
    }

    private Sts1RenderBoundary() {}

    /**
     * The background has a verified pre-native draw point: the concrete scene
     * {@code renderCombatRoomBg} Prefixes in
     * {@code artframework/sts1/patch/BackgroundRenderPatches.java} draw inside
     * {@code AbstractDungeon.render} before the native scene paints. The general ART submission
     * boundary is still post-native ({@link #nativeInterval()} / {@link #artSubmissionInterval()}).
     */
    public static BackgroundCapability backgroundCapability() {
        return BackgroundCapability.PRE_NATIVE;
    }

    public static String nativeInterval() {
        return "stage.draw";
    }

    public static String artSubmissionInterval() {
        return "post_native_overlay";
    }

    /** Read-only boundary diagnostics: configured capability, not claimed pixel behavior. */
    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("nativeInterval", nativeInterval());
        m.put("artInterval", artSubmissionInterval());
        m.put("backgroundCapability", backgroundCapability().name().toLowerCase());
        m.put("supportsPreNativeBackground",
                Boolean.valueOf(backgroundCapability() == BackgroundCapability.PRE_NATIVE));
        return m;
    }
}

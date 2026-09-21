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

    /** The current StageHost callback is post-native and cannot paint below retained pixels. */
    public static BackgroundCapability backgroundCapability() {
        return BackgroundCapability.UNSUPPORTED;
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

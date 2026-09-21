package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic state for the `art verify` console surface. Visual verification is developer-only and
 * must never claim pixel behavior the host cannot deliver; the background mode stays
 * `unsupported` until a verified pre-native draw point exists.
 */
public final class Sts1VerifyDiagnostics {
    public enum Mode {
        OFF,
        BACKGROUND,
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
    }

    /** Whether the configured mode can actually submit pixels through the current host boundary. */
    public static boolean modeSupported() {
        if (configured == Mode.OFF) {
            return true;
        }
        return Sts1RenderBoundary.backgroundCapability()
                == Sts1RenderBoundary.BackgroundCapability.PRE_NATIVE;
    }

    public static String submissionStatus() {
        if (configured == Mode.OFF) {
            return "disabled";
        }
        return modeSupported() ? "ready" : "unsupported";
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
        if (lastError != null) {
            m.put("lastError", lastError);
        }
        return m;
    }

    public static void resetForTests() {
        configured = Mode.OFF;
        lastError = null;
    }
}

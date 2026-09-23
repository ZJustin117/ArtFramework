package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Strict verification gate: only the pre-native combat-room background is allowed. */
public final class BackgroundOnlyGate {
    private static boolean active;
    private static long revision;
    private static long backgroundDraw;
    private static long backgroundSuppression;
    private static long blockedForeground;
    private static long unsupported;
    private static long uncovered;
    private static String lastReason = "";

    private BackgroundOnlyGate() {}

    public static synchronized void setActive(boolean enabled) {
        if (active == enabled) return;
        active = enabled;
        revision++;
        if (!enabled) clearCountersLocked();
        lastReason = enabled ? "enabled" : "disabled";
    }
    public static synchronized boolean isActive() { return active; }
    public static synchronized void recordBackgroundDraw() { if (active) backgroundDraw++; }
    public static synchronized void recordBackgroundSuppression() { if (active) backgroundSuppression++; }
    public static synchronized void recordBlocked(String reason) {
        if (active) { blockedForeground++; lastReason = reason == null ? "foreground" : reason; }
    }
    public static synchronized void recordUnsupported(String reason) {
        if (active) { unsupported++; lastReason = reason == null ? "unsupported" : reason; }
    }
    public static synchronized void recordUncovered(String reason) {
        if (active) { uncovered++; lastReason = reason == null ? "uncovered" : reason; }
    }
    public static synchronized Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("active", Boolean.valueOf(active));
        m.put("backgroundDraw", Long.valueOf(backgroundDraw));
        m.put("backgroundSuppression", Long.valueOf(backgroundSuppression));
        m.put("suppression", Long.valueOf(backgroundSuppression));
        m.put("blockedForeground", Long.valueOf(blockedForeground));
        m.put("unsupported", Long.valueOf(unsupported));
        m.put("uncovered", Long.valueOf(uncovered));
        m.put("revision", Long.valueOf(revision));
        m.put("lastReason", lastReason);
        return m;
    }
    public static synchronized void clearForRecovery() {
        if (active) revision++;
        active = false;
        clearCountersLocked();
        lastReason = "recovery";
    }
    public static synchronized void resetForTests() {
        active = false;
        revision = 0L;
        clearCountersLocked();
        lastReason = "";
    }
    private static void clearCountersLocked() {
        backgroundDraw = 0L;
        backgroundSuppression = 0L;
        blockedForeground = 0L;
        unsupported = 0L;
        uncovered = 0L;
    }
}

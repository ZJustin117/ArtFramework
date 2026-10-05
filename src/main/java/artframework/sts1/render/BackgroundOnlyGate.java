package artframework.sts1.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Strict verification gate: only the pre-native combat-room background is allowed. */
public final class BackgroundOnlyGate {
    /** Bounded number of distinct uncovered-owner labels retained for attribution. */
    static final int UNCOVERED_OWNER_CAP = 32;
    /** Bounded number of distinct blocked world/foreground owner labels retained for attribution. */
    static final int BLOCKED_OWNER_CAP = 32;
    /** Bounded number of distinct unsupported owner labels retained for attribution. */
    static final int UNSUPPORTED_OWNER_CAP = 32;

    private static boolean active;
    private static long revision;
    private static long backgroundDraw;
    private static long backgroundSuppression;
    private static long blockedForeground;
    private static long blockedOverflow;
    private static long unsupported;
    private static long unsupportedOverflow;
    private static long uncovered;
    private static long uncoveredOverflow;
    private static final Map<String, Long> uncoveredByOwner = new LinkedHashMap<String, Long>();
    private static final Map<String, Long> blockedByOwner = new LinkedHashMap<String, Long>();
    private static final Map<String, Long> unsupportedByOwner = new LinkedHashMap<String, Long>();
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
        if (active) {
            blockedForeground++;
            String owner = normalizeLabel(reason, "blocked");
            lastReason = owner;
            Long prior = blockedByOwner.get(owner);
            if (prior != null) {
                blockedByOwner.put(owner, Long.valueOf(prior.longValue() + 1L));
            } else if (blockedByOwner.size() < BLOCKED_OWNER_CAP) {
                blockedByOwner.put(owner, Long.valueOf(1L));
            } else {
                blockedOverflow++;
            }
        }
    }
    public static synchronized void recordUnsupported(String reason) {
        if (active) {
            unsupported++;
            String owner = normalizeLabel(reason, "unsupported");
            lastReason = owner;
            Long prior = unsupportedByOwner.get(owner);
            if (prior != null) {
                unsupportedByOwner.put(owner, Long.valueOf(prior.longValue() + 1L));
            } else if (unsupportedByOwner.size() < UNSUPPORTED_OWNER_CAP) {
                unsupportedByOwner.put(owner, Long.valueOf(1L));
            } else {
                unsupportedOverflow++;
            }
        }
    }
    public static synchronized void recordUncovered(String reason) {
        if (active) {
            uncovered++;
            String owner = normalizeOwner(reason);
            lastReason = owner;
            Long prior = uncoveredByOwner.get(owner);
            if (prior != null) {
                uncoveredByOwner.put(owner, Long.valueOf(prior.longValue() + 1L));
            } else if (uncoveredByOwner.size() < UNCOVERED_OWNER_CAP) {
                uncoveredByOwner.put(owner, Long.valueOf(1L));
            } else {
                uncoveredOverflow++;
            }
        }
    }
    private static String normalizeOwner(String reason) {
        return normalizeLabel(reason, "uncovered");
    }
    private static String normalizeLabel(String reason, String fallback) {
        if (reason == null) return fallback;
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
    public static synchronized Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("active", Boolean.valueOf(active));
        m.put("backgroundDraw", Long.valueOf(backgroundDraw));
        m.put("backgroundSuppression", Long.valueOf(backgroundSuppression));
        m.put("suppression", Long.valueOf(backgroundSuppression));
        m.put("blockedForeground", Long.valueOf(blockedForeground));
        m.put("blockedByOwner", new LinkedHashMap<String, Long>(blockedByOwner));
        m.put("blockedDistinct", Long.valueOf((long) blockedByOwner.size()));
        m.put("blockedOverflow", Long.valueOf(blockedOverflow));
        m.put("unsupported", Long.valueOf(unsupported));
        m.put("unsupportedByOwner", new LinkedHashMap<String, Long>(unsupportedByOwner));
        m.put("unsupportedDistinct", Long.valueOf((long) unsupportedByOwner.size()));
        m.put("unsupportedOverflow", Long.valueOf(unsupportedOverflow));
        m.put("uncovered", Long.valueOf(uncovered));
        m.put("uncoveredByOwner", new LinkedHashMap<String, Long>(uncoveredByOwner));
        m.put("uncoveredDistinct", Long.valueOf((long) uncoveredByOwner.size()));
        m.put("uncoveredOverflow", Long.valueOf(uncoveredOverflow));
        // Strict ART-output acceptance: no uncovered marker (zero pending ART output) and no
        // attributable/unexpected overflow in any bounded-owner map. This deliberately covers
        // only ART output submission/leaks, NOT native world/foreground residue (player/monster/
        // hand/terrain) which remains a separate, later coverage slice.
        boolean strictAccepted = uncovered == 0L
                && uncoveredOverflow == 0L
                && blockedOverflow == 0L;
        m.put("strictAccepted", Boolean.valueOf(strictAccepted));
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
        blockedOverflow = 0L;
        unsupported = 0L;
        unsupportedOverflow = 0L;
        uncovered = 0L;
        uncoveredOverflow = 0L;
        uncoveredByOwner.clear();
        blockedByOwner.clear();
        unsupportedByOwner.clear();
    }
}

package artframework.sts1;

/**
 * Per-surface full-present capability (milestone 16.0).
 *
 * <ul>
 *   <li>{@link #OFF} — native owns display and input; ART may still observe frames when backend
 *       is bound.
 *   <li>{@link #OBSERVE} — ART snapshots / probe / projection; no native suppress; no ART input
 *       ownership.
 *   <li>{@link #FULL} — when the matching surface is mounted, ART may suppress native display and
 *       own input for that surface (host must still implement draw/executor).
 * </ul>
 */
public enum PresentLevel {
    OFF,
    OBSERVE,
    FULL;

    public boolean allowsObserve() {
        return this == OBSERVE || this == FULL;
    }

    public boolean allowsFullPresent() {
        return this == FULL;
    }

    public static PresentLevel parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return OFF;
        }
        String s = raw.trim().toUpperCase();
        if ("ON".equals(s) || "TRUE".equals(s) || "1".equals(s)) {
            return FULL;
        }
        if ("FALSE".equals(s) || "0".equals(s) || "NONE".equals(s)) {
            return OFF;
        }
        try {
            return PresentLevel.valueOf(s);
        } catch (IllegalArgumentException e) {
            return OFF;
        }
    }
}

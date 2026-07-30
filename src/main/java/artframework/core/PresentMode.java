package artframework.core;

/**
 * How a present-profile node contributes to cascade resolution.
 *
 * <ul>
 *   <li>{@link #OVERRIDE} — truncate parent chain; this layer is the base for the subtree</li>
 *   <li>{@link #ATTACH} — stack on top of ancestors; parent chain still walks</li>
 * </ul>
 */
public enum PresentMode {
    OVERRIDE,
    ATTACH;

    public static PresentMode parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return OVERRIDE;
        }
        String s = raw.trim().toLowerCase();
        if ("attach".equals(s)) {
            return ATTACH;
        }
        if ("override".equals(s) || "replace".equals(s)) {
            return OVERRIDE;
        }
        return OVERRIDE;
    }
}

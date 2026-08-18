package artframework.component;

/**
 * Canonical C2 resource / registration ids for native STS1 templates.
 * Prefer {@code sts1.*}; legacy {@code sts.*} is accepted via {@link #canonicalize}.
 */
public final class NativeTemplateIds {

    public static final String MAP = "sts1.map";
    public static final String EVENT = "sts1.event";
    public static final String SELECT_GRID = "sts1.select.grid";
    public static final String SELECT_HAND = "sts1.select.hand";
    public static final String END_TURN = "sts1.endturn";

    /** @deprecated use {@link #MAP} */
    public static final String LEGACY_MAP = "sts.map";
    /** @deprecated use {@link #EVENT} */
    public static final String LEGACY_EVENT = "sts.event";
    /** @deprecated use {@link #SELECT_GRID} */
    public static final String LEGACY_SELECT_GRID = "sts.select.grid";
    /** @deprecated use {@link #SELECT_HAND} */
    public static final String LEGACY_SELECT_HAND = "sts.select.hand";
    /** @deprecated use {@link #END_TURN} */
    public static final String LEGACY_END_TURN = "sts.endturn";

    private NativeTemplateIds() {}

    public static boolean matches(String resourceOrId, String canonical) {
        return canonical.equals(canonicalize(resourceOrId));
    }

    /** Map legacy {@code sts.*} ids to {@code sts1.*}. */
    public static String canonicalize(String id) {
        if (id == null) {
            return null;
        }
        if (LEGACY_MAP.equals(id)) {
            return MAP;
        }
        if (LEGACY_EVENT.equals(id)) {
            return EVENT;
        }
        if (LEGACY_SELECT_GRID.equals(id)) {
            return SELECT_GRID;
        }
        if (LEGACY_SELECT_HAND.equals(id)) {
            return SELECT_HAND;
        }
        if (LEGACY_END_TURN.equals(id)) {
            return END_TURN;
        }
        return id;
    }
}

package spireui.c2;

/**
 * Canonical C2 resource / registration ids for native STS templates.
 */
public final class NativeTemplateIds {

    public static final String MAP = "sts.map";
    public static final String EVENT = "sts.event";
    public static final String SELECT_GRID = "sts.select.grid";
    public static final String SELECT_HAND = "sts.select.hand";
    public static final String END_TURN = "sts.endturn";

    private NativeTemplateIds() {}

    public static boolean matches(String resourceOrId, String canonical) {
        return canonical.equals(resourceOrId);
    }
}

package artframework.context;

/** Canonical names for presentation context signals. */
public final class ContextSignals {
    public static final String FRAME_UPDATED = "context/frame/updated";

    public static String action(String surfaceId, String actionName) {
        return "ui/" + (surfaceId != null ? surfaceId : "") + "/" + (actionName != null ? actionName : "");
    }

    private ContextSignals() {}
}

package artframework.context;

import artframework.core.SignalPaths;

/** Canonical names for presentation context signals. */
public final class ContextSignals {
    public static final String FRAME_UPDATED = "context/frame/updated";

    public static String action(String surfaceId, String actionName) {
        return SignalPaths.component(surfaceId, actionName);
    }

    private ContextSignals() {}
}

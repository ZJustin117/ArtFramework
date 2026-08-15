package artframework.context;

import artframework.c2.MapNodeRef;

/** Durable ECS state for the native map gesture host bridge. */
public final class MapGestureComponent {
    public final MapNodeRef pending;
    public final int pendingFrames;
    public final MapNodeRef lastTarget;
    public final String status;
    public final String eligibility;
    public final int attempts;

    public MapGestureComponent(MapNodeRef pending, int pendingFrames, MapNodeRef lastTarget,
            String status, String eligibility, int attempts) {
        this.pending = pending;
        this.pendingFrames = Math.max(0, pendingFrames);
        this.lastTarget = lastTarget;
        this.status = status != null ? status : "idle";
        this.eligibility = eligibility != null ? eligibility : "";
        this.attempts = Math.max(0, attempts);
    }

    public static MapGestureComponent idle() {
        return new MapGestureComponent(null, 0, null, "idle", "", 0);
    }
}

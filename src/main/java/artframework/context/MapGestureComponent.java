package artframework.context;

import artframework.component.MapNodeRef;

/** Durable ECS state for the native map gesture host bridge. */
public final class MapGestureComponent {
    public final MapNodeRef pending;
    public final int pendingFrames;
    public final MapNodeRef lastTarget;
    public final String status;
    public final String eligibility;
    public final int attempts;

    /**
     * Hover-only pointer state (H0). {@code pointerActive} is the sticky "place the native pointer"
     * flag; {@code pointerNodeRef} (optional) makes the pointer TRACK a map node's live hitbox center
     * as the map scrolls, while {@code pointerX}/{@code pointerY} are the raw screen coordinates used
     * when no node ref is set. {@code pointerLegendIndex} (>=0) tracks a legend row's live hitbox
     * center. The pointer path NEVER sets click edges ({@code justClickedLeft}/{@code hb.clicked}/
     * {@code dungeonMapScreen.clicked}).
     */
    public final int pointerX;
    public final int pointerY;
    public final boolean pointerActive;
    public final MapNodeRef pointerNodeRef;
    public final int pointerLegendIndex;

    public MapGestureComponent(MapNodeRef pending, int pendingFrames, MapNodeRef lastTarget,
            String status, String eligibility, int attempts) {
        this(pending, pendingFrames, lastTarget, status, eligibility, attempts, 0, 0, false, null, -1);
    }

    public MapGestureComponent(MapNodeRef pending, int pendingFrames, MapNodeRef lastTarget,
            String status, String eligibility, int attempts,
            int pointerX, int pointerY, boolean pointerActive, MapNodeRef pointerNodeRef,
            int pointerLegendIndex) {
        this.pending = pending;
        this.pendingFrames = Math.max(0, pendingFrames);
        this.lastTarget = lastTarget;
        this.status = status != null ? status : "idle";
        this.eligibility = eligibility != null ? eligibility : "";
        this.attempts = Math.max(0, attempts);
        this.pointerX = pointerX;
        this.pointerY = pointerY;
        this.pointerActive = pointerActive;
        this.pointerNodeRef = pointerNodeRef;
        this.pointerLegendIndex = pointerLegendIndex;
    }

    public static MapGestureComponent idle() {
        return new MapGestureComponent(null, 0, null, "idle", "", 0);
    }

    /** Copy this gesture state with a replaced hover pointer. */
    public MapGestureComponent withPointer(int x, int y, boolean active, MapNodeRef nodeRef,
            int legendIndex) {
        return new MapGestureComponent(pending, pendingFrames, lastTarget, status, eligibility,
                attempts, x, y, active, nodeRef, legendIndex);
    }
}

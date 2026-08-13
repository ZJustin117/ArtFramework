package artframework.c2;

/**
 * Immutable compatibility view derived from one ECS-backed presenter slot.
 */
public final class EntitySlot {

    public final String slotId;
    public final EntityKind kind;
    public final String refId;
    private final Object snapshot;
    private final float x;
    private final float y;
    private final float scale;
    private final boolean laidOut;

    public EntitySlot(String slotId, EntityKind kind, String refId) {
        this(slotId, kind, refId, null, 0f, 0f, 1f, false);
    }

    public EntitySlot(
            String slotId,
            EntityKind kind,
            String refId,
            Object snapshot,
            float x,
            float y,
            float scale,
            boolean laidOut) {
        if (slotId == null || slotId.isEmpty()) {
            throw new IllegalArgumentException("slotId required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }
        this.slotId = slotId;
        this.kind = kind;
        this.refId = refId != null ? refId : "";
        this.snapshot = snapshot;
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.laidOut = laidOut;
    }

    public Object snapshot() {
        return snapshot;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float scale() {
        return scale;
    }

    public boolean isLaidOut() {
        return laidOut;
    }

}

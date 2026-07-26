package spireui.c2;

/**
 * Mutable bookkeeping for one presenter slot. Snapshot is opaque (consumer DTO).
 */
public final class EntitySlot {

    public final String slotId;
    public final EntityKind kind;
    public final String refId;
    private Object snapshot;
    private float x;
    private float y;
    private float scale = 1f;
    private boolean laidOut;

    public EntitySlot(String slotId, EntityKind kind, String refId) {
        if (slotId == null || slotId.isEmpty()) {
            throw new IllegalArgumentException("slotId required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind required");
        }
        this.slotId = slotId;
        this.kind = kind;
        this.refId = refId != null ? refId : "";
    }

    public Object snapshot() {
        return snapshot;
    }

    public void setSnapshot(Object snapshot) {
        this.snapshot = snapshot;
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

    public void setLayout(float x, float y, float scale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.laidOut = true;
    }
}

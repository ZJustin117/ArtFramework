package artframework.c2;

/** Stable identity and host reference for an EntityPresent slot. */
public final class EntitySlotIdentityComponent {
    public final String slotId;
    public final EntityKind kind;
    public final String refId;

    public EntitySlotIdentityComponent(String slotId, EntityKind kind, String refId) {
        this.slotId = slotId;
        this.kind = kind;
        this.refId = refId != null ? refId : "";
    }
}

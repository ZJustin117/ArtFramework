package artframework.c2;

/** Opaque consumer snapshot attached to an EntityPresent slot. */
public final class EntitySlotSnapshotComponent {
    public final Object snapshot;

    public EntitySlotSnapshotComponent(Object snapshot) {
        this.snapshot = snapshot;
    }
}

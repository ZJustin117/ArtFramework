package artframework.c2;

/** Framework-owned immutable snapshot attached to an EntityPresent slot. */
public final class EntitySlotSnapshotComponent {
    public final EntitySnapshot snapshot;

    public EntitySlotSnapshotComponent(EntitySnapshot snapshot) {
        this.snapshot = snapshot != null ? EntitySnapshot.normalize(snapshot) : null;
    }
}

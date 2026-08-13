package artframework.context;

/** Immutable external-state snapshot stored as projection data. */
public final class ProjectionFrameSnapshotComponent {
    public final ContextFrame frame;

    public ProjectionFrameSnapshotComponent(ContextFrame frame) {
        this.frame = frame != null ? frame : ContextFrame.unavailable(0L);
    }
}

package artframework.context;

/** One-shot authority frame pair consumed by {@link BusinessConfirmationSystem}. */
public final class BusinessConfirmationFrameComponent {
    public final ContextFrame before;
    public final ContextFrame after;

    public BusinessConfirmationFrameComponent(ContextFrame before, ContextFrame after) {
        this.before = before;
        if (after == null) throw new IllegalArgumentException("after frame required");
        this.after = after;
    }
}

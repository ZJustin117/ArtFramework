package artframework.context;

/** Pending authority snapshot consumed by the production presentation schedule. */
public final class AuthorityFrameComponent {
    public final ContextFrame frame;

    public AuthorityFrameComponent(ContextFrame frame) {
        if (frame == null) throw new IllegalArgumentException("frame required");
        this.frame = frame;
    }
}

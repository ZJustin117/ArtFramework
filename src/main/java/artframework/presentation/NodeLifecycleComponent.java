package artframework.presentation;

/** NodeTree lifecycle state. */
public final class NodeLifecycleComponent {
    public final boolean mounted;
    public final boolean ready;

    public NodeLifecycleComponent(boolean mounted, boolean ready) {
        this.mounted = mounted;
        this.ready = ready;
    }
}

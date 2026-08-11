package artframework.core;

/** Mounted and ready state for a presentation entity. */
public final class NodeLifecycleComponent {
    public final boolean mounted;

    public NodeLifecycleComponent(boolean mounted) {
        this.mounted = mounted;
    }
}

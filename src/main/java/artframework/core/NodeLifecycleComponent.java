package artframework.core;

/** Mounted state mirrored from the existing UiTree lifecycle. */
public final class NodeLifecycleComponent {
    public final boolean mounted;

    public NodeLifecycleComponent(boolean mounted) {
        this.mounted = mounted;
    }
}

package artframework.context;

/** One-shot lifecycle request consumed by {@link SurfaceLifecycleSystem}. */
public final class SurfaceLifecycleRequestComponent {
    public final boolean mounted;

    public SurfaceLifecycleRequestComponent(boolean mounted) {
        this.mounted = mounted;
    }
}

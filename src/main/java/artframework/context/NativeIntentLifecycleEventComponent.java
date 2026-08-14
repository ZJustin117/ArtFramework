package artframework.context;

/** One-shot native intent lifecycle input consumed by {@link NativeIntentLifecycleSystem}. */
public final class NativeIntentLifecycleEventComponent {
    public final String name;
    public final NativeIntentLifecycleComponent.State state;
    public final String message;

    public NativeIntentLifecycleEventComponent(String name,
            NativeIntentLifecycleComponent.State state, String message) {
        this.name = name != null ? name : "";
        if (state == null) throw new IllegalArgumentException("state required");
        this.state = state;
        this.message = message != null ? message : "";
    }
}

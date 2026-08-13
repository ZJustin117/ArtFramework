package artframework.context;

/** Data-only lifecycle state for the latest native intent on one surface. */
public final class NativeIntentLifecycleComponent {
    public enum State {
        REQUESTED,
        SENT,
        EXECUTED,
        QUEUED,
        REJECTED,
        /** An available authority frame was observed after execution. */
        CONFIRMED,
        /** The authority source became unavailable before an execution could be observed. */
        FAILED
    }

    public final String name;
    public final State state;
    public final String message;

    public NativeIntentLifecycleComponent(String name, State state, String message) {
        this.name = name != null ? name : "";
        if (state == null) throw new IllegalArgumentException("state required");
        this.state = state;
        this.message = message != null ? message : "";
    }
}

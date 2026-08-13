package artframework.presentation;

/** Data-only current state for a declarative node state machine. */
public final class NodeStateComponent {
    public final String value;

    public NodeStateComponent(String value) {
        this.value = value != null ? value : "";
    }
}

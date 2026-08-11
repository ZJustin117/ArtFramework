package artframework.presentation;

/** Host-local realization key. The referenced host object is never ECS authority. */
public final class HostBindingComponent {
    public final String hostKind;
    public final String localKey;

    public HostBindingComponent(String hostKind, String localKey) {
        this.hostKind = hostKind != null ? hostKind : "";
        this.localKey = localKey != null ? localKey : "";
    }
}

package artframework.presentation;

/** Immutable runtime node identity. */
public final class NodeIdentityComponent {
    public final PresentationKey key;
    public final String name;
    public final String type;
    public final String source;

    public NodeIdentityComponent(PresentationKey key, String name, String type, String source) {
        if (key == null) throw new IllegalArgumentException("key required");
        this.key = key;
        this.name = name != null ? name : "";
        this.type = type != null ? type : "";
        this.source = source != null ? source : "";
    }
}

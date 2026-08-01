package artframework.core;

/** Declaration identity mirrored onto a runtime node entity. */
public final class NodeIdentityComponent {
    public final String id;
    public final String emitId;
    public final String type;

    public NodeIdentityComponent(String id, String emitId, String type) {
        this.id = id;
        this.emitId = emitId;
        this.type = type;
    }
}

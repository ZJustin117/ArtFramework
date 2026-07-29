package artframework.c2;

/** C2 card-select lifecycle state; signal policy lives on the shared SignalBus. */
public final class SelectTemplate {
    private final SelectKind kind;
    private final String resource;
    private boolean active;
    public SelectTemplate(SelectKind kind, String resource) {
        if (kind == null || resource == null || resource.isEmpty()) throw new IllegalArgumentException("kind and resource required");
        this.kind = kind; this.resource = resource;
    }
    public SelectKind kind() { return kind; }
    public String resource() { return resource; }
    public void activate() { active = true; }
    public void deactivate() { active = false; }
    public boolean isActive() { return active; }
    void resetForTests() { active = false; }
}

package artframework.context;

import artframework.core.ComponentKind;

/** Stable identity of a present surface. */
public final class SurfaceIdentityComponent {
    public final String id;
    public final ComponentKind kind;

    public SurfaceIdentityComponent(String id, ComponentKind kind) {
        this.id = id;
        this.kind = kind;
    }
}

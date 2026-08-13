package artframework.presentation;

import artframework.ecs.EntityId;

/** Immutable host-facing reference to an ECS-mounted presentation root. */
public final class PresentationMount {
    public final String scope;
    public final PresentationContext context;
    public final EntityId root;

    public PresentationMount(PresentationContext context, EntityId root) {
        if (context == null || root == null) throw new IllegalArgumentException("context and root required");
        this.scope = context.scope();
        this.context = context;
        this.root = root;
    }

    public String windowId() {
        return PresentationRuntime.windowId(context);
    }
}

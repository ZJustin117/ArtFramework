package artframework.presentation;

import artframework.component.UiNode;

/** Immutable source declaration retained on the C1 root entity. */
public final class C1DeclarationComponent {
    public final UiNode root;

    public C1DeclarationComponent(UiNode root) {
        if (root == null) throw new IllegalArgumentException("root required");
        this.root = root;
    }
}

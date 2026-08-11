package artframework.presentation;

import artframework.ecs.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parent and ordered children are runtime tree truth. */
public final class NodeHierarchyComponent {
    public final EntityId parent;
    public final List<EntityId> children;

    public NodeHierarchyComponent(EntityId parent, List<EntityId> children) {
        this.parent = parent;
        this.children = children == null ? Collections.<EntityId>emptyList()
                : Collections.unmodifiableList(new ArrayList<EntityId>(children));
    }
}

package artframework.presentation;

import artframework.ecs.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Immutable, sorted render input built from ECS state. */
public final class PresentationFrame {
    public final List<PresentationDrawItem> items;

    private PresentationFrame(List<PresentationDrawItem> items) {
        this.items = Collections.unmodifiableList(items);
    }

    public static PresentationFrame from(PresentationContext context) {
        List<PresentationDrawItem> items = new ArrayList<PresentationDrawItem>();
        for (EntityId entity : context.entities()) {
            VisibilityComponent visibility = context.world().get(entity, VisibilityComponent.class);
            BoundsComponent bounds = context.world().get(entity, BoundsComponent.class);
            DrawComponent draw = context.world().get(entity, DrawComponent.class);
            if (visibility == null || !visibility.visible || bounds == null || draw == null) continue;
            NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
            ChromeComponent chrome = context.world().get(entity, ChromeComponent.class);
            EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
            HostBindingComponent hostBinding = context.world().get(entity, HostBindingComponent.class);
            NodeHierarchyComponent hierarchy = context.world().get(entity, NodeHierarchyComponent.class);
            items.add(new PresentationDrawItem(entity, identity.key, bounds.rect, bounds.z, draw, chrome,
                    effects != null ? new ArrayList<EffectAttachment>(effects.attachments())
                            : Collections.<EffectAttachment>emptyList(), hostBinding,
                    hierarchy != null && hierarchy.parent == null));
        }
        Collections.sort(items, new Comparator<PresentationDrawItem>() {
            @Override public int compare(PresentationDrawItem a, PresentationDrawItem b) {
                return Float.compare(a.z, b.z);
            }
        });
        return new PresentationFrame(items);
    }
}

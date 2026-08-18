package artframework.core;

import artframework.component.EffectDecl;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only resolver for enabled pack effect-default contributions in the ECS world. */
public final class PackEffectDefaults {
    private PackEffectDefaults() {}

    public static List<EffectDecl> forNodeType(PresentationWorld world, String nodeType) {
        if (world == null || nodeType == null || nodeType.isEmpty()) {
            return Collections.emptyList();
        }
        List<EffectDecl> result = new ArrayList<EffectDecl>();
        for (EntityId entity : world.query(PackEffectDefaultsComponent.class)) {
            PackEffectDefaultsComponent contribution =
                    world.get(entity, PackEffectDefaultsComponent.class);
            result.addAll(contribution.forNodeType(nodeType));
        }
        return result.isEmpty() ? Collections.<EffectDecl>emptyList()
                : Collections.unmodifiableList(result);
    }

    /** Distinguishes an enabled empty contribution from an un-migrated pack. */
    public static boolean hasContribution(PresentationWorld world) {
        return world != null && !world.query(PackEffectDefaultsComponent.class).isEmpty();
    }
}

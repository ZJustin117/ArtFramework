package artframework.core;

import artframework.component.EffectDecl;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only resolver for enabled pack full-frame effect contributions. */
public final class PackFullFrameEffects {
    private PackFullFrameEffects() {}

    public static List<EffectDecl> effects(PresentationWorld world) {
        return effects(world, null);
    }

    public static List<EffectDecl> effects(PresentationWorld world, String packId) {
        if (world == null) return Collections.emptyList();
        List<EffectDecl> result = new ArrayList<EffectDecl>();
        for (EntityId entity : world.query(PackFullFrameEffectsComponent.class)) {
            PackFullFrameEffectsComponent contribution =
                    world.get(entity, PackFullFrameEffectsComponent.class);
            if (packId == null || packId.equals(contribution.packId)) {
                result.addAll(contribution.effects());
            }
        }
        return result.isEmpty() ? Collections.<EffectDecl>emptyList()
                : Collections.unmodifiableList(result);
    }

    public static boolean hasContribution(PresentationWorld world) {
        return hasContribution(world, null);
    }

    public static boolean hasContribution(PresentationWorld world, String packId) {
        if (world == null) return false;
        if (packId == null) return !world.query(PackFullFrameEffectsComponent.class).isEmpty();
        for (EntityId entity : world.query(PackFullFrameEffectsComponent.class)) {
            if (packId.equals(world.get(entity, PackFullFrameEffectsComponent.class).packId)) return true;
        }
        return false;
    }
}

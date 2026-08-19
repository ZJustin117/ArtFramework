package artframework.core;

import artframework.component.EffectDecl;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

/** Read-only resolver for enabled pack C2 surface-effect contributions. */
public final class PackSurfaceEffects {
    private PackSurfaceEffects() {}

    public static List<EffectDecl> forSurface(PresentationWorld world, String surfaceId) {
        return forSurface(world, null, surfaceId);
    }

    public static List<EffectDecl> forSurface(PresentationWorld world, String packId, String surfaceId) {
        if (world == null || surfaceId == null || surfaceId.isEmpty()) {
            return Collections.emptyList();
        }
        List<EffectDecl> result = new ArrayList<EffectDecl>();
        for (EntityId entity : world.query(PackSurfaceEffectsComponent.class)) {
            PackSurfaceEffectsComponent contribution =
                    world.get(entity, PackSurfaceEffectsComponent.class);
            if (packId != null && !packId.equals(contribution.packId)) continue;
            result.addAll(contribution.forSurface(surfaceId));
        }
        return result.isEmpty() ? Collections.<EffectDecl>emptyList()
                : Collections.unmodifiableList(result);
    }

    public static boolean hasContribution(PresentationWorld world) {
        return world != null && !world.query(PackSurfaceEffectsComponent.class).isEmpty();
    }

    public static boolean hasContribution(PresentationWorld world, String packId) {
        if (world == null || packId == null || packId.isEmpty()) return false;
        for (EntityId entity : world.query(PackSurfaceEffectsComponent.class)) {
            if (packId.equals(world.get(entity, PackSurfaceEffectsComponent.class).packId)) return true;
        }
        return false;
    }

    public static Set<String> surfaceIds(PresentationWorld world) {
        return surfaceIds(world, null);
    }

    public static Set<String> surfaceIds(PresentationWorld world, String packId) {
        if (world == null) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<String>();
        for (EntityId entity : world.query(PackSurfaceEffectsComponent.class)) {
            PackSurfaceEffectsComponent contribution =
                    world.get(entity, PackSurfaceEffectsComponent.class);
            if (packId != null && !packId.equals(contribution.packId)) continue;
            result.addAll(contribution.surfaceIds());
        }
        return result.isEmpty() ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(result);
    }
}

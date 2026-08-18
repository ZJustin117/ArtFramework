package artframework.core;

import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only resolver for enabled pack surface-profile binding contributions. */
public final class PackSurfaceBindings {
    private PackSurfaceBindings() {}

    public static Map<String, String> forPack(PresentationWorld world, String packId) {
        if (world == null || packId == null || packId.isEmpty()) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (EntityId entity : world.query(PackSurfaceBindingsComponent.class)) {
            PackSurfaceBindingsComponent contribution =
                    world.get(entity, PackSurfaceBindingsComponent.class);
            if (!packId.equals(contribution.packId)) continue;
            for (String surfaceId : contribution.surfaceIds()) {
                result.put(surfaceId, contribution.profileId);
            }
        }
        return result.isEmpty() ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(result);
    }

    public static boolean hasContribution(PresentationWorld world, String packId) {
        if (world == null || packId == null || packId.isEmpty()) return false;
        for (EntityId entity : world.query(PackSurfaceBindingsComponent.class)) {
            if (packId.equals(world.get(entity, PackSurfaceBindingsComponent.class).packId)) return true;
        }
        return false;
    }
}

package artframework.core;

import artframework.context.SurfaceIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;

/**
 * C2 full-present surface → PresentProfile binding. When unbound, surfaces use {@link
 * ProjectPresent} chrome/theme (same as pre-35.2).
 */
public final class SurfacePresent {

    private SurfacePresent() {}

    public static void bind(String surfaceId, String profileId) {
        String sid = canon(surfaceId);
        if (sid.isEmpty()) {
            throw new IllegalArgumentException("surfaceId required");
        }
        if (profileId == null || profileId.trim().isEmpty()) {
            remove(sid);
            return;
        }
        String pid = profileId.trim();
        if (PresentProfiles.get(pid) == null) {
            throw new IllegalArgumentException("unknown present profile: " + pid);
        }
        PresentationContext context = context();
        EntityId entity = entity(context, sid);
        if (entity == null) {
            entity = context.create(key(sid), sid, "surface-present", "artframework");
        }
        context.world().put(entity, SurfacePresentComponent.class,
                new SurfacePresentComponent(sid, pid));
    }

    public static void unbind(String surfaceId) {
        String sid = canon(surfaceId);
        if (!sid.isEmpty()) {
            remove(sid);
        }
    }

    public static void clear() {
        for (EntityId entity : new java.util.ArrayList<EntityId>(context().entities())) {
            if (context().world().get(entity, SurfacePresentComponent.class) != null) {
                context().destroy(entity);
            }
        }
    }

    public static String profileId(String surfaceId) {
        String sid = canon(surfaceId);
        if (sid.isEmpty()) {
            return null;
        }
        EntityId entity = entity(context(), sid);
        SurfacePresentComponent value = entity == null ? null
                : context().world().get(entity, SurfacePresentComponent.class);
        return value != null ? value.profileId : null;
    }

    public static boolean isBound(String surfaceId) {
        return profileId(surfaceId) != null;
    }

    public static PresentResolved resolve(String surfaceId) {
        String pid = profileId(surfaceId);
        if (pid != null) {
            PresentProfile p = PresentProfiles.get(pid);
            if (p != null) {
                return new PresentResolved(p.id, p.theme, p.chrome, p.packId, false);
            }
        }
        return ProjectPresent.resolved();
    }

    public static PresentChromeStyle chrome(String surfaceId) {
        return resolve(surfaceId).chrome;
    }

    public static Theme theme(String surfaceId) {
        return resolve(surfaceId).theme;
    }

    public static Map<String, Object> probeSummary() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        Map<String, String> bindings = new LinkedHashMap<String, String>();
        for (EntityId entity : context().world().query(SurfacePresentComponent.class)) {
            SurfacePresentComponent value = context().world().get(entity, SurfacePresentComponent.class);
            bindings.put(value.surfaceId, value.profileId);
        }
        m.put("bindings", bindings);
        m.put("count", Integer.valueOf(bindings.size()));
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        clear();
    }

    private static String canon(String surfaceId) {
        if (surfaceId == null) {
            return "";
        }
        String t = surfaceId.trim();
        if (t.isEmpty()) {
            return "";
        }
        String c = SurfaceIds.canonicalize(t);
        return c != null ? c : t;
    }

    private static PresentationContext context() {
        return PresentationRegistry.context("present-state");
    }

    private static PresentationKey key(String surfaceId) {
        return new PresentationKey("present.surface", surfaceId);
    }

    private static EntityId entity(PresentationContext context, String surfaceId) {
        return context.entity(key(surfaceId));
    }

    private static void remove(String surfaceId) {
        PresentationContext context = context();
        EntityId entity = entity(context, surfaceId);
        if (entity != null) context.destroy(entity);
    }
}

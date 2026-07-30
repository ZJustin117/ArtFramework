package artframework.core;

import artframework.context.SurfaceIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * C2 full-present surface → PresentProfile binding. When unbound, surfaces use {@link
 * ProjectPresent} chrome/theme (same as pre-35.2).
 */
public final class SurfacePresent {

    private static final Map<String, String> BY_SURFACE = new LinkedHashMap<String, String>();

    private SurfacePresent() {}

    public static void bind(String surfaceId, String profileId) {
        String sid = canon(surfaceId);
        if (sid.isEmpty()) {
            throw new IllegalArgumentException("surfaceId required");
        }
        if (profileId == null || profileId.trim().isEmpty()) {
            BY_SURFACE.remove(sid);
            return;
        }
        String pid = profileId.trim();
        if (PresentProfiles.get(pid) == null) {
            throw new IllegalArgumentException("unknown present profile: " + pid);
        }
        BY_SURFACE.put(sid, pid);
    }

    public static void unbind(String surfaceId) {
        String sid = canon(surfaceId);
        if (!sid.isEmpty()) {
            BY_SURFACE.remove(sid);
        }
    }

    public static void clear() {
        BY_SURFACE.clear();
    }

    public static String profileId(String surfaceId) {
        String sid = canon(surfaceId);
        if (sid.isEmpty()) {
            return null;
        }
        return BY_SURFACE.get(sid);
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
        m.put("bindings", new LinkedHashMap<String, String>(BY_SURFACE));
        m.put("count", Integer.valueOf(BY_SURFACE.size()));
        return Collections.unmodifiableMap(m);
    }

    public static void resetForTests() {
        BY_SURFACE.clear();
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
}

package artframework.core;

import artframework.component.EffectDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable C2 surface-effect contribution installed by one enabled PresentPack. */
public final class PackSurfaceEffectsComponent {
    public final String packId;
    private final Map<String, List<EffectDecl>> bySurface;

    public PackSurfaceEffectsComponent(String packId, Map<String, List<EffectDecl>> bySurface) {
        if (packId == null || packId.isEmpty()) throw new IllegalArgumentException("pack id required");
        this.packId = packId;
        Map<String, List<EffectDecl>> copy = new LinkedHashMap<String, List<EffectDecl>>();
        if (bySurface != null) {
            for (Map.Entry<String, List<EffectDecl>> entry : bySurface.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    copy.put(entry.getKey(), Collections.unmodifiableList(
                            new ArrayList<EffectDecl>(entry.getValue())));
                }
            }
        }
        this.bySurface = Collections.unmodifiableMap(copy);
    }

    public List<EffectDecl> forSurface(String surfaceId) {
        List<EffectDecl> effects = bySurface.get(surfaceId);
        return effects != null ? effects : Collections.<EffectDecl>emptyList();
    }

    public java.util.Set<String> surfaceIds() {
        return bySurface.keySet();
    }
}

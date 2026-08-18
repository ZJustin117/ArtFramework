package artframework.core;

import artframework.component.EffectDecl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable C1 default-effect contribution installed by one enabled PresentPack. */
public final class PackEffectDefaultsComponent {
    public final String packId;
    private final Map<String, List<EffectDecl>> byNodeType;

    public PackEffectDefaultsComponent(String packId, Map<String, List<EffectDecl>> byNodeType) {
        if (packId == null || packId.isEmpty()) throw new IllegalArgumentException("pack id required");
        this.packId = packId;
        Map<String, List<EffectDecl>> copy = new LinkedHashMap<String, List<EffectDecl>>();
        if (byNodeType != null) {
            for (Map.Entry<String, List<EffectDecl>> entry : byNodeType.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    copy.put(entry.getKey(), Collections.unmodifiableList(
                            new ArrayList<EffectDecl>(entry.getValue())));
                }
            }
        }
        this.byNodeType = Collections.unmodifiableMap(copy);
    }

    public List<EffectDecl> forNodeType(String nodeType) {
        List<EffectDecl> effects = byNodeType.get(nodeType);
        return effects != null ? effects : Collections.<EffectDecl>emptyList();
    }
}

package artframework.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes {@link UiNode} trees by id (interactive leaves and any labeled node).
 */
public final class NodeIndex {

    private final Map<String, UiNode> byId;

    private NodeIndex(Map<String, UiNode> byId) {
        this.byId = byId;
    }

    public static NodeIndex of(UiNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root required");
        }
        Map<String, UiNode> map = new LinkedHashMap<String, UiNode>();
        walk(root, map);
        return new NodeIndex(Collections.unmodifiableMap(map));
    }

    private static void walk(UiNode node, Map<String, UiNode> map) {
        if (!node.id.isEmpty()) {
            if (map.containsKey(node.id)) {
                throw new IllegalArgumentException("duplicate id: " + node.id);
            }
            map.put(node.id, node);
        }
        for (UiNode c : node.children) {
            walk(c, map);
        }
        for (List<UiNode> slot : node.slots.values()) {
            for (UiNode s : slot) {
                walk(s, map);
            }
        }
    }

    public UiNode get(String id) {
        return byId.get(id);
    }

    public boolean contains(String id) {
        return id != null && byId.containsKey(id);
    }

    public List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<String>(byId.keySet()));
    }

    public List<String> idsOfType(String type) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, UiNode> e : byId.entrySet()) {
            if (type.equals(e.getValue().type)) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public int size() {
        return byId.size();
    }
}

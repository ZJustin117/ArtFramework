package artframework.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outcome of {@link LayoutEngine#layout}.
 */
public final class LayoutResult {

    public final UiNode root;
    public final Rect rootBounds;
    private final Map<String, Rect> byId;

    public LayoutResult(UiNode root, Rect rootBounds, Map<String, Rect> byId) {
        this.root = root;
        this.rootBounds = rootBounds;
        if (byId == null || byId.isEmpty()) {
            this.byId = Collections.emptyMap();
        } else {
            this.byId = Collections.unmodifiableMap(new LinkedHashMap<String, Rect>(byId));
        }
    }

    public Rect boundsOf(String id) {
        if (id == null) {
            return null;
        }
        return byId.get(id);
    }

    public Map<String, Rect> allBounds() {
        return byId;
    }

    public boolean hasId(String id) {
        return id != null && byId.containsKey(id);
    }
}

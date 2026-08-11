package artframework.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import artframework.ecs.EntityId;

/** Process-owned registry for named presentation scopes. */
public final class PresentationRegistry {
    private static final Map<String, PresentationContext> CONTEXTS =
            new LinkedHashMap<String, PresentationContext>();

    private PresentationRegistry() {}

    public static synchronized PresentationContext context(String scope) {
        PresentationContext current = CONTEXTS.get(scope);
        if (current == null || !current.world().isOpen()) {
            current = new PresentationContext(scope);
            CONTEXTS.put(scope, current);
        }
        return current;
    }

    public static synchronized void close(String scope) {
        PresentationContext context = CONTEXTS.remove(scope);
        if (context != null) context.close();
    }

    public static synchronized void resetForTests() {
        for (PresentationContext context : CONTEXTS.values()) context.close();
        CONTEXTS.clear();
    }

    /** Read-only diagnostic view of all open presentation scopes. */
    public static synchronized List<Map<String, Object>> probeAll() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (PresentationContext context : CONTEXTS.values()) {
            Map<String, Object> scope = new LinkedHashMap<String, Object>();
            scope.put("scope", context.world().scope());
            List<Map<String, Object>> entities = new ArrayList<Map<String, Object>>();
            for (EntityId entity : context.world().entities()) {
                NodeIdentityComponent identity = context.world().get(entity, NodeIdentityComponent.class);
                NodeHierarchyComponent hierarchy = context.world().get(entity, NodeHierarchyComponent.class);
                NodeLifecycleComponent lifecycle = context.world().get(entity, NodeLifecycleComponent.class);
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("entity", entity.toString());
                row.put("key", identity.key.toString());
                row.put("name", identity.name);
                row.put("type", identity.type);
                row.put("source", identity.source);
                row.put("parent", hierarchy.parent == null ? null : hierarchy.parent.toString());
                row.put("children", Integer.valueOf(hierarchy.children.size()));
                row.put("mounted", Boolean.valueOf(lifecycle.mounted));
                row.put("ready", Boolean.valueOf(lifecycle.ready));
                row.put("components", componentNames(context, entity));
                entities.add(row);
            }
            scope.put("entities", entities);
            result.add(scope);
        }
        return Collections.unmodifiableList(result);
    }

    public static synchronized List<PresentationFrame> frames() {
        List<PresentationFrame> result = new ArrayList<PresentationFrame>();
        for (PresentationContext context : CONTEXTS.values()) {
            result.add(PresentationFrame.from(context));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<String> componentNames(PresentationContext context, EntityId entity) {
        List<String> names = new ArrayList<String>();
        for (Class<?> type : context.world().componentTypes(entity)) names.add(type.getSimpleName());
        return Collections.unmodifiableList(names);
    }
}

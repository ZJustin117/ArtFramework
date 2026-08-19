package artframework.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import artframework.ecs.EntityId;
import artframework.ecs.ArtEcs;
import artframework.ecs.PresentationWorld;

/** Process-owned registry for named presentation scopes. */
public final class PresentationRegistry {
    private static final Map<String, PresentationContext> CONTEXTS =
            new LinkedHashMap<String, PresentationContext>();
    private static final PresentationWorld WORLD = ArtEcs.world();

    private PresentationRegistry() {}

    public static synchronized PresentationContext context(String scope) {
        PresentationContext current = CONTEXTS.get(scope);
        if (current == null || !current.world().isOpen()) {
            current = new PresentationContext(scope, WORLD);
            CONTEXTS.put(scope, current);
        }
        return current;
    }

    /** Existing registered context only; does not create state for a closed scope. */
    public static synchronized PresentationContext existingContext(String scope) {
        return scope == null ? null : CONTEXTS.get(scope);
    }

    /** Read-only names of registered ECS scopes. */
    public static synchronized List<String> scopes() {
        return Collections.unmodifiableList(new ArrayList<String>(CONTEXTS.keySet()));
    }

    public static synchronized void close(String scope) {
        PresentationContext context = CONTEXTS.remove(scope);
        if (context != null) context.close();
    }

    /**
     * Clears ownership without replacing context instances.
     *
     * <p>The {@code CONTEXTS} keys are retained deliberately. Production holders such as
     * {@code RenderStateEcs}, {@code NativeInputRecords}, {@code NativeTemplateRuntime}, and
     * {@code Sts1MapIntentBridge} pin their context in a {@code static final} field at class init.
     * Removing the key here would make the next {@link #context(String)} call build a second
     * instance with an empty key map while those holders keep the old one, splitting writers from
     * readers. Use {@link #close(String)} to retire a single scope.
     */
    public static synchronized void resetForTests() {
        for (PresentationContext context : CONTEXTS.values()) context.close();
        WORLD.clear();
    }

    /** The sole ART-owned world for all registered scopes. */
    public static PresentationWorld world() {
        return WORLD;
    }

    /** Read-only diagnostic view of all open presentation scopes. */
    public static synchronized List<Map<String, Object>> probeAll() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (PresentationContext context : CONTEXTS.values()) {
            Map<String, Object> scope = new LinkedHashMap<String, Object>();
            scope.put("scope", context.scope());
            List<Map<String, Object>> entities = new ArrayList<Map<String, Object>>();
            for (EntityId entity : context.entities()) {
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

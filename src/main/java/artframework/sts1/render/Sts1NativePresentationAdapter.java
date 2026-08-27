package artframework.sts1.render;

import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.DrawComponent;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.VisibilityComponent;
import artframework.render.RenderProjectionQueue;

/**
 * Converts a delegated native invocation into host-neutral ART presentation data.
 *
 * <p>This adapter intentionally retains only stable presentation identity and data. Native STS
 * objects and SpriteBatch instances remain owned by the patch/host boundary.</p>
 */
public final class Sts1NativePresentationAdapter {
    private static final String CONTEXT_SCOPE = "nrcc-native";
    private static final String KEY_SCOPE = "sts1.native";
    private static final String HOST_KIND = "STS1_NATIVE";

    private Sts1NativePresentationAdapter() {}

    public static String present(NativeRenderInvocation invocation) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        PresentationContext context = PresentationRegistry.context(CONTEXT_SCOPE);
        String localId = localId(invocation.ownerId);
        PresentationKey key = new PresentationKey(KEY_SCOPE, localId);
        EntityId entity = context.entity(key);
        if (entity == null) {
            entity = context.create(key, invocation.ownerId, "native_render", invocation.nativeClass);
        }
        Rect bounds = invocation.boundsHint != null ? invocation.boundsHint : Rect.ZERO;
        context.world().put(entity, BoundsComponent.class, new BoundsComponent(bounds, 0f));
        context.world().put(entity, VisibilityComponent.class,
                new VisibilityComponent(true, 1f));
        context.world().put(entity, DrawComponent.class,
                new DrawComponent(invocation.nativeMethod, "", invocation.sourceIdentity));
        context.world().put(entity, HostBindingComponent.class,
                new HostBindingComponent(HOST_KIND, invocation.ownerId));
        context.world().put(entity, NativeInvocationComponent.class,
                new NativeInvocationComponent(invocation));
        RenderProjectionQueue.request(CONTEXT_SCOPE);
        return entity.toString();
    }

    public static EntityId entity(String ownerId) {
        if (ownerId == null || ownerId.isEmpty()) return null;
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return null;
        return context.entity(new PresentationKey(KEY_SCOPE, localId(ownerId)));
    }

    public static boolean hasEntity(String ownerId) {
        return entity(ownerId) != null;
    }

    public static void remove(String ownerId) {
        EntityId entity = entity(ownerId);
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (entity != null && context != null) {
            context.destroy(entity);
            RenderProjectionQueue.request(CONTEXT_SCOPE);
        }
    }

    public static void clear() {
        PresentationContext context = PresentationRegistry.existingContext(CONTEXT_SCOPE);
        if (context == null) return;
        for (EntityId entity : new java.util.ArrayList<EntityId>(context.entities())) {
            context.destroy(entity);
        }
        RenderProjectionQueue.request(CONTEXT_SCOPE);
    }

    private static String localId(String ownerId) {
        return ownerId.replace(':', '_').replace('/', '_');
    }
}

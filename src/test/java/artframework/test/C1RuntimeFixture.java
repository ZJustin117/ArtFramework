package artframework.test;

import artframework.component.UiNode;
import artframework.core.AnimationPlayers;
import artframework.core.NodeConnections;
import artframework.core.NodeStateMachines;
import artframework.ecs.EntityId;
import artframework.presentation.C1Materializer;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationRuntime;

/** Test fixture that exercises the same ECS materialization sequence as SyntheticRuntime. */
public final class C1RuntimeFixture implements AutoCloseable {
    public final String windowId;
    public final PresentationContext context;
    public final EntityId root;

    private C1RuntimeFixture(String windowId, PresentationContext context, EntityId root) {
        this.windowId = windowId;
        this.context = context;
        this.root = root;
    }

    public static C1RuntimeFixture mount(String windowId, UiNode declaration) {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope(windowId));
        EntityId root = C1Materializer.mount(context, declaration);
        NodeConnections.syncContext(context);
        AnimationPlayers.syncContext(context);
        NodeStateMachines.syncContext(context);
        return new C1RuntimeFixture(windowId, context, root);
    }

    public EntityId find(String pathOrId) {
        return PresentationRuntime.find(context, pathOrId);
    }

    public Object property(String pathOrId, String property) {
        return PresentationRuntime.property(context, find(pathOrId), property);
    }

    public void emit(String pathOrId, String signal, Object... payload) {
        PresentationRuntime.emit(context, find(pathOrId), signal, payload);
    }

    public void tick(float deltaSeconds) {
        PresentationRuntime.tick(context, deltaSeconds);
    }

    @Override
    public void close() {
        NodeConnections.clearWindow(windowId);
        AnimationPlayers.clearWindow(windowId);
        NodeStateMachines.clearWindow(windowId);
        PresentationRuntime.clearSignals(context);
        PresentationRegistry.close(PresentationRuntime.c1Scope(windowId));
    }
}

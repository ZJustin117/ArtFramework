package artframework.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import artframework.component.EffectDecl;
import artframework.component.Rect;
import artframework.component.UiNode;
import artframework.ecs.EntityId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

public class C1MaterializerTest {
    @After public void cleanup() { PresentationRegistry.resetForTests(); }

    @Test public void hierarchyPropertiesSignalsAndLifecycleAreEcsBacked() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("test"));
        EntityId root = C1Materializer.mount(context, UiNode.of("window").id("root")
                .child(UiNode.of("button").id("ok").prop("label", "OK").build()).build());
        EntityId child = PresentationRuntime.find(context, "ok");

        assertSame(root, PresentationRuntime.hierarchy(context, child).parent);
        assertEquals(1, PresentationRuntime.children(context, root).size());
        assertEquals("OK", PresentationRuntime.property(context, child, "label"));
        assertTrue(PresentationRuntime.component(context, child, NodeLifecycleComponent.class).mounted);
        assertTrue(PresentationRuntime.component(context, child, NodeLifecycleComponent.class).ready);

        AtomicInteger calls = new AtomicInteger();
        PresentationRuntime.connect(context, child, "pressed", args -> calls.incrementAndGet());
        PresentationRuntime.emit(context, child, "pressed");
        assertEquals(1, calls.get());
    }

    @Test public void registeredContextsShareTheArtWorld() {
        PresentationContext first = PresentationRegistry.context(PresentationRuntime.c1Scope("first"));
        PresentationContext second = PresentationRegistry.context(PresentationRuntime.c1Scope("second"));
        C1Materializer.mount(first, UiNode.of("window").id("first").build());
        C1Materializer.mount(second, UiNode.of("window").id("second").build());
        assertSame(first.world(), second.world());
        assertSame(first.world(), PresentationRegistry.world());
    }

    @Test public void closingOneScopePreservesEntitiesInOtherScopes() {
        PresentationContext first = PresentationRegistry.context(PresentationRuntime.c1Scope("first"));
        PresentationContext second = PresentationRegistry.context(PresentationRuntime.c1Scope("second"));
        EntityId firstRoot = C1Materializer.mount(first, UiNode.of("window").id("first").build());
        EntityId secondRoot = C1Materializer.mount(second, UiNode.of("window").id("second").build());

        PresentationRegistry.close(PresentationRuntime.c1Scope("first"));

        assertFalse(PresentationRegistry.world().contains(firstRoot));
        assertTrue(PresentationRegistry.world().contains(secondRoot));
        assertSame(second, PresentationRuntime.context("second"));
    }

    @Test public void openWindowDiscoveryIsDerivedFromRootLifecycle() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("derived"));
        EntityId root = C1Materializer.mount(context, UiNode.of("window").id("root").build());
        assertTrue(PresentationRuntime.isOpen("derived"));
        assertTrue(PresentationRuntime.openWindowIds().contains("derived"));

        context.world().put(root, NodeLifecycleComponent.class,
                new NodeLifecycleComponent(false, false));

        assertFalse(PresentationRuntime.isOpen("derived"));
        assertFalse(PresentationRuntime.openWindowIds().contains("derived"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void undeclaredSignalsAreRejected() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("signals"));
        EntityId root = C1Materializer.mount(context, UiNode.of("window").id("root").build());
        PresentationRuntime.emit(context, root, "pressed");
    }

    @Test public void destroysKeyedEntityWithoutASecondNodeStore() {
        PresentationContext context = new PresentationContext("destroy");
        try {
            PresentationKey key = new PresentationKey("c2", "end-turn");
            EntityId entity = context.create(key, "end-turn", "control", "c2");
            assertEquals(entity, context.entity(key));
            assertTrue(context.destroy(entity));
            assertNull(context.entity(key));
            assertFalse(context.world().contains(entity));
        } finally { context.close(); }
    }

    @Test public void declarationMaterializesEffectsVisualsAndImmutableFrame() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("visuals"));
        C1Materializer.mount(context, UiNode.of("window").id("root").child(
                UiNode.of("button").id("ok").prop("text", "OK")
                        .effect(new EffectDecl("lightwave", null)).build()).build());
        EntityId ok = PresentationRuntime.find(context, "root/ok");
        assertEquals("button", PresentationRuntime.identity(context, ok).type);
        assertEquals("lightwave", context.world().get(ok, EffectsComponent.class)
                .attachments().get(0).effectId);
        BoundsComponent bounds = context.world().get(ok, BoundsComponent.class);
        DrawComponent draw = context.world().get(ok, DrawComponent.class);
        VisibilityComponent visibility = context.world().get(ok, VisibilityComponent.class);
        HostBindingComponent binding = context.world().get(ok, HostBindingComponent.class);
        assertNotNull(bounds);
        assertEquals("button", draw.role);
        assertEquals("OK", draw.text);
        assertTrue(visibility.visible);
        assertEquals("SCENE2D_C1", binding.hostKind);
        assertEquals("visuals:ok", binding.localKey);
        boolean found = false;
        for (PresentationDrawItem item : PresentationRuntime.frame(context).items) {
            if (ok.equals(item.entity)) found = true;
        }
        assertTrue(found);
    }
}

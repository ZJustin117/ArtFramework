package artframework.presentation;

import artframework.component.Rect;
import artframework.component.EffectDecl;
import artframework.component.UiNode;
import artframework.ecs.EntityId;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NodeTreeTest {

    @Test
    public void hierarchyPropertiesSignalsAndLifecycleAreEcsBacked() {
        NodeTree tree = new NodeTree("test-tree");
        try {
            Node root = tree.create(new PresentationKey("ui", "root"), "root", "window", "c1", null);
            Node child = tree.create(new PresentationKey("ui", "root/ok"), "ok", "button", "c1", root.entityId());
            tree.setPorts(child.entityId(), Arrays.asList("pressed"), null);
            child.set("label", "OK");

            assertSame(root.entityId(), tree.context().world()
                    .get(child.entityId(), NodeHierarchyComponent.class).parent);
            assertEquals(1, root.children().size());
            assertEquals("OK", child.get("label"));
            assertFalse(tree.context().world().get(child.entityId(), NodeLifecycleComponent.class).mounted);

            AtomicInteger calls = new AtomicInteger();
            child.connect("pressed", args -> calls.incrementAndGet());
            child.emitSignal("pressed");
            assertEquals(1, calls.get());

            tree.context().world().put(child.entityId(), BoundsComponent.class,
                    new BoundsComponent(new Rect(3f, 4f, 50f, 20f), 2f));
            assertEquals(new Rect(3f, 4f, 50f, 20f), child.rect());

            tree.mount();
            NodeLifecycleComponent lifecycle = tree.context().world()
                    .get(child.entityId(), NodeLifecycleComponent.class);
            assertTrue(lifecycle.mounted);
            assertTrue(lifecycle.ready);
        } finally {
            tree.close();
        }
    }

    @Test
    public void treeMountStateIsDerivedFromRootLifecycleComponent() {
        NodeTree tree = NodeTree.mount("tree:derived", UiNode.of("window").id("root").build(), null);
        EntityId root = tree.root().entityId();
        tree.world().put(root, NodeLifecycleComponent.class,
                new NodeLifecycleComponent(false, false));

        tree.mount();

        assertTrue(tree.root().isMounted());
        tree.close();
    }

    @Test
    public void materializedTreesUseTheSharedArtWorld() {
        NodeTree first = NodeTrees.open("first", UiNode.of("window").id("first").build(), null);
        NodeTree second = NodeTrees.open("second", UiNode.of("window").id("second").build(), null);
        try {
            assertSame(first.world(), second.world());
            assertSame(first.world(), PresentationRegistry.world());
        } finally {
            NodeTrees.close("first");
            NodeTrees.close("second");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void undeclaredSignalsAreRejected() {
        NodeTree tree = new NodeTree("signals");
        try {
            Node root = tree.create(new PresentationKey("ui", "root"), "root", "window", "c1", null);
            root.emitSignal("pressed");
        } finally {
            tree.close();
        }
    }

    @Test
    public void destroysKeyedEntityWithoutASecondNodeStore() {
        PresentationContext context = new PresentationContext("destroy");
        try {
            PresentationKey key = new PresentationKey("c2", "end-turn");
            EntityId entity = context.create(key, "end-turn", "control", "c2");
            assertEquals(entity, context.entity(key));
            assertTrue(context.destroy(entity));
            assertNull(context.entity(key));
            assertFalse(context.world().contains(entity));
        } finally {
            context.close();
        }
    }

    @Test
    public void declarationMaterializesNodeEffectsAndProducesAnImmutableFrame() {
        UiNode root = UiNode.of("window").id("root").child(
                UiNode.of("button").id("ok").effect(new EffectDecl("lightwave", null)).build()).build();
        NodeTree tree = NodeTree.mount("decl", root, null);
        try {
            Node ok = tree.find("root/ok");
            assertEquals("button", ok.type());
            EffectsComponent effects = tree.context().world().get(ok.entityId(), EffectsComponent.class);
            assertEquals("lightwave", effects.attachments().get(0).effectId);
            tree.context().world().put(ok.entityId(), BoundsComponent.class,
                    new BoundsComponent(new Rect(1f, 2f, 3f, 4f), 4f));
            tree.context().world().put(ok.entityId(), DrawComponent.class,
                    new DrawComponent("button", "", "OK"));
            tree.context().world().put(ok.entityId(), VisibilityComponent.class,
                    new VisibilityComponent(true, 1f));
            boolean found = false;
            for (PresentationDrawItem item : tree.frame().items) {
                if (ok.entityId().equals(item.entity)) found = true;
            }
            assertTrue(found);
        } finally {
            tree.close();
        }
    }

    @Test
    public void c1DeclarationMaterializesLayoutAndDrawDataIntoEcs() {
        UiNode root = UiNode.of("window").id("root").child(
                UiNode.of("button").id("ok").prop("text", "OK").build()).build();
        NodeTree tree = NodeTree.mount("visuals", root, null);
        try {
            Node ok = tree.find("root/ok");
            BoundsComponent bounds = tree.world().get(ok.entityId(), BoundsComponent.class);
            DrawComponent draw = tree.world().get(ok.entityId(), DrawComponent.class);
            VisibilityComponent visibility = tree.world().get(ok.entityId(), VisibilityComponent.class);
            HostBindingComponent binding = tree.world().get(ok.entityId(), HostBindingComponent.class);
            assertNotNull(bounds);
            assertEquals("button", draw.role);
            assertEquals("OK", draw.text);
            assertTrue(visibility.visible);
            assertEquals("SCENE2D_C1", binding.hostKind);
            assertEquals("visuals:ok", binding.localKey);
            assertTrue(tree.frame().items.size() >= 2);
        } finally {
            tree.close();
        }
    }

    @Test
    public void closeUnmountsChildrenBeforeParentsAndClearsLifecycle() {
        UiNode root = UiNode.of("window").id("root")
                .child(UiNode.of("button").id("ok").build()).build();
        final List<String> order = new ArrayList<String>();
        NodeTree tree = NodeTree.mount("unmount", root, new NodeTreeLifecycle() {
            @Override public void onMount(Node node) {}
            @Override public void onReady(Node node) {}
            @Override public void onUnmount(Node node) { order.add(node.name()); }
        });
        tree.close();
        assertEquals(Arrays.asList("ok", "root"), order);
    }
}

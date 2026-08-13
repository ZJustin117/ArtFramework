package artframework.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

/** Covers ECS-native C1 lookup APIs without constructing an object-tree facade. */
public class PresentationRuntimeTest {

    @After
    public void cleanup() {
        PresentationRegistry.resetForTests();
    }

    @Test
    public void materializerCreatesQueryableLifecycleAndVisualData() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("ecs"));
        EntityId root = C1Materializer.mount(context, UiNode.of(UiTypes.WINDOW).id("root")
                .prop("title", "ECS")
                .child(UiNode.of(UiTypes.SLIDER).id("volume").prop("min", 0f)
                        .prop("max", 1f).prop("value", 0.25f).build())
                .build());

        EntityId slider = PresentationRuntime.find(context, "volume");
        assertNotNull(slider);
        assertEquals(root, PresentationRuntime.root(context));
        assertTrue(PresentationRuntime.component(context, root,
                NodeLifecycleComponent.class).ready);
        assertEquals(0.25f, ((Number) PresentationRuntime.component(context, slider,
                ControlValueComponent.class).value).floatValue(), 0.001f);
        assertNotNull(PresentationRuntime.component(context, slider, DrawComponent.class));
        assertNotNull(PresentationRuntime.component(context, slider, HostBindingComponent.class));
    }

    @Test
    public void propertyWritesReplaceEcsComponent() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("props"));
        C1Materializer.mount(context, UiNode.of(UiTypes.WINDOW).id("root")
                .prop("title", "Before").build());
        EntityId root = PresentationRuntime.root(context);

        PresentationRuntime.setProperty(context, root, "title", "After");

        assertEquals("After", PresentationRuntime.property(context, root, "title"));
        assertEquals("After", context.world().get(root, NodePropertiesComponent.class).get("title"));
    }
}

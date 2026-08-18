package artframework.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

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
        assertEquals(0f, PresentationRuntime.component(context, slider,
                ControlBoundsComponent.class).min, 0.001f);
        assertEquals(1f, PresentationRuntime.component(context, slider,
                ControlBoundsComponent.class).max, 0.001f);
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

    @Test(expected = IllegalArgumentException.class)
    public void controlValueRejectsMutableValues() {
        new ControlValueComponent(new java.util.ArrayList<Object>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void controlValueRejectsNonFiniteNumbers() {
        new ControlValueComponent(Float.valueOf(Float.NaN));
    }

    @Test
    public void controlValueAcceptsSupportedScalars() {
        assertEquals("text", new ControlValueComponent("text").value);
        assertEquals(Boolean.TRUE, new ControlValueComponent(Boolean.TRUE).value);
        assertEquals(Float.valueOf(0.5f), new ControlValueComponent(Float.valueOf(0.5f)).value);
    }

    @Test
    public void controlInitialValueUsesFiniteFallbackForNonFiniteDeclarationValues() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("min", "NaN");
        props.put("value", "Infinity");

        Object value = ControlValueSystem.initialValue(UiTypes.SLIDER,
                new NodePropertiesComponent(props));

        assertEquals(Float.valueOf(0f), value);
    }

    @Test
    public void controlBoundsRemainMaterializedAfterPropertyChanges() {
        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("bounds"));
        C1Materializer.mount(context, UiNode.of(UiTypes.WINDOW).id("root")
                .child(UiNode.of(UiTypes.SLIDER).id("volume").prop("min", 0f)
                        .prop("max", 1f).build())
                .build());
        EntityId slider = PresentationRuntime.find(context, "volume");

        PresentationRuntime.setProperty(context, slider, "min", 0.8f);

        ControlBoundsComponent bounds = PresentationRuntime.component(context, slider,
                ControlBoundsComponent.class);
        assertEquals(0f, bounds.min, 0.001f);
        assertEquals(1f, bounds.max, 0.001f);
    }
}

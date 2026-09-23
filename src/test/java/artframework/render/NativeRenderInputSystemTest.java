package artframework.render;

import artframework.component.Rect;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class NativeRenderInputSystemTest {
    @Test public void componentIsImmutableAndCopiesBounds() {
        NativeRenderInputComponent input = new NativeRenderInputComponent(" cards ", " owner ",
                RenderPhase.C2_CONTENT, 2f, " card-2 ", new Rect(1f, 2f, 3f, 4f), true,
                NativeRenderOwnership.OBSERVED);

        assertEquals("cards", input.nativeRenderFamily());
        assertEquals("owner", input.ownerId());
        assertEquals("card-2", input.stableKey());
        assertNotSame(input.bounds(), input.bounds());
        for (Field field : NativeRenderInputComponent.class.getDeclaredFields()) {
            assertTrue("field must be final: " + field.getName(), Modifier.isFinal(field.getModifiers()));
        }
    }

    @Test public void constructorRejectsMissingAndInvalidValues() {
        assertInvalid(null, "owner", RenderPhase.C2_CONTENT, 0f, "key", new Rect(0f, 0f, 1f, 1f),
                NativeRenderOwnership.OBSERVED);
        assertInvalid("family", "owner", RenderPhase.C2_CONTENT, Float.NaN, "key",
                new Rect(0f, 0f, 1f, 1f), NativeRenderOwnership.OBSERVED);
        assertInvalid("family", "owner", RenderPhase.C2_CONTENT, 0f, "key",
                new Rect(0f, 0f, -1f, 1f), NativeRenderOwnership.OBSERVED);
        assertInvalid("family", "owner", RenderPhase.C2_CONTENT, 0f, "key",
                new Rect(0f, 0f, 1f, 1f), null);
    }

    @Test public void systemIsRepeatableAndDoesNotDrawOrChangeUnrelatedEntities() {
        PresentationWorld world = new PresentationWorld("native-input");
        EntityId nativeEntity = world.createEntity();
        EntityId unrelated = world.createEntity();
        world.put(nativeEntity, NativeRenderInputComponent.class, input("b", NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY));
        world.put(unrelated, Marker.class, new Marker());
        NativeRenderInputSystem system = new NativeRenderInputSystem();

        system.run(world, new EcsTick(0f, 1L));
        system.run(world, new EcsTick(0f, 2L));

        assertTrue(world.has(nativeEntity, NativeRenderInputComponent.class));
        assertTrue(world.has(unrelated, Marker.class));
        assertFalse(world.has(unrelated, NativeRenderInputComponent.class));
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                world.get(nativeEntity, NativeRenderInputComponent.class).ownership());
    }

    @Test public void orderingDataIsDeterministicAndOwnershipDoesNotDelegatePixels() {
        List<NativeRenderInputComponent> inputs = Arrays.asList(
                input("z", NativeRenderOwnership.OBSERVED),
                input("a", NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY));
        List<RenderOrder> orders = new ArrayList<RenderOrder>();
        for (NativeRenderInputComponent input : inputs) {
            orders.add(new RenderOrder(input.phase(), input.z(), input.stableKey()));
        }
        Collections.sort(orders, RenderOrder.COMPARATOR);
        assertEquals(Arrays.asList("a", "z"), Arrays.asList(orders.get(0).stableKey, orders.get(1).stableKey));
        assertFalse(inputs.get(0).ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
        assertFalse(inputs.get(1).ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
    }

    private static NativeRenderInputComponent input(String key, NativeRenderOwnership ownership) {
        return new NativeRenderInputComponent("family", "owner-" + key, RenderPhase.ENTITY_CONTENT,
                1f, key, new Rect(0f, 0f, 10f, 10f), true, ownership);
    }

    private static void assertInvalid(String family, String owner, RenderPhase phase, float z,
            String key, Rect bounds, NativeRenderOwnership ownership) {
        try {
            new NativeRenderInputComponent(family, owner, phase, z, key, bounds, true, ownership);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected invalid native render input");
    }

    private static final class Marker {}
}

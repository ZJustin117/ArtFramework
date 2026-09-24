package artframework.render;

import artframework.component.Rect;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.core.PackSystemPhase;
import artframework.core.PackSystems;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NativeRenderInputSystemTest {
    @After public void resetPackSystems() {
        PackSystems.resetForTests();
    }

    @Test public void componentIsImmutableAndCopiesBounds() {
        NativeRenderInputComponent input = new NativeRenderInputComponent(" cards ", " owner ",
                RenderPhase.C2_CONTENT, 2f, " card-2 ", new Rect(1f, 2f, 3f, 4f), true,
                NativeRenderOwnership.OBSERVED);

        assertEquals("cards", input.nativeRenderFamily());
        assertEquals("owner", input.ownerId());
        assertEquals("card-2", input.stableKey());
        assertEquals("", input.sceneId());
        assertEquals(-1L, input.frameId());
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
        try {
            new NativeRenderInputComponent("family", "owner", RenderPhase.C2_CONTENT, 0f,
                    "key", new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.OBSERVED,
                    "scene", -2L);
            fail("expected invalid frame id failure");
        } catch (IllegalArgumentException expected) {
            // expected
        }
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

    @Test public void extractorReturnsEmptyImmutableSnapshotForEmptyWorld() {
        NativeRenderFrameSnapshot snapshot = new NativeRenderFrameExtractor()
                .extract(new PresentationWorld("empty-native-input"));

        assertEquals(0, snapshot.size());
        try {
            snapshot.inputs().clear();
            fail("expected immutable snapshot list");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test public void extractorSortsByPhaseThenZThenStableKeyWithoutCreationOrder() {
        PresentationWorld world = new PresentationWorld("ordered-native-input");
        EntityId first = world.createEntity();
        EntityId second = world.createEntity();
        EntityId third = world.createEntity();
        EntityId fourth = world.createEntity();
        world.put(first, NativeRenderInputComponent.class, new NativeRenderInputComponent(
                "family", "owner-b", RenderPhase.C2_CONTENT, 2f, "b",
                new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.OBSERVED));
        world.put(second, NativeRenderInputComponent.class, new NativeRenderInputComponent(
                "family", "owner-a", RenderPhase.C2_CONTENT, 2f, "a",
                new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY));
        world.put(third, NativeRenderInputComponent.class, new NativeRenderInputComponent(
                "family", "owner-bg", RenderPhase.ART_BACKGROUND, 99f, "background",
                new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.DELEGATED_TO_ART));
        world.put(fourth, NativeRenderInputComponent.class, new NativeRenderInputComponent(
                "family", "owner-low", RenderPhase.C2_CONTENT, -1f, "low-z",
                new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.OBSERVED));

        NativeRenderFrameSnapshot snapshot = new NativeRenderFrameExtractor().extract(world);

        assertEquals(Arrays.asList("background", "low-z", "a", "b"), stableKeys(snapshot));
        assertEquals(NativeRenderOwnership.DELEGATED_TO_ART, snapshot.get(0).ownership());
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY, snapshot.get(2).ownership());
    }

    @Test public void extractorRejectsDuplicateStableKeysAcrossEntities() {
        PresentationWorld world = new PresentationWorld("duplicate-native-input");
        EntityId first = world.createEntity();
        EntityId second = world.createEntity();
        world.put(first, NativeRenderInputComponent.class, input("same", NativeRenderOwnership.OBSERVED));
        world.put(second, NativeRenderInputComponent.class, input("same", NativeRenderOwnership.DELEGATED_TO_ART));

        try {
            new NativeRenderFrameExtractor().extract(world);
            fail("expected duplicate stable key failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("same"));
        }
    }

    @Test public void snapshotCopiesInputAndPreservesOwnershipWithoutDelegating() {
        PresentationWorld world = new PresentationWorld("snapshot-native-input");
        EntityId entity = world.createEntity();
        world.put(entity, NativeRenderInputComponent.class,
                input("observed", NativeRenderOwnership.OBSERVED));

        NativeRenderInputSnapshot snapshot = new NativeRenderFrameExtractor().extract(world).get(0);
        world.destroyEntity(entity);

        assertEquals(NativeRenderOwnership.OBSERVED, snapshot.ownership());
        assertEquals("observed", snapshot.stableKey());
        assertNotSame(snapshot.bounds(), snapshot.bounds());
        assertFalse(snapshot.ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
    }

    @Test public void extractorPreservesSceneAndFrameMetadata() {
        PresentationWorld world = new PresentationWorld("native-input-metadata");
        EntityId entity = world.createEntity();
        world.put(entity, NativeRenderInputComponent.class, new NativeRenderInputComponent(
                "family", "owner", RenderPhase.ENTITY_CONTENT, 0f, "key",
                new Rect(0f, 0f, 1f, 1f), true, NativeRenderOwnership.OBSERVED,
                "combat", 42L));

        NativeRenderInputSnapshot snapshot = new NativeRenderFrameExtractor().extract(world).get(0);
        assertEquals("combat", snapshot.sceneId());
        assertEquals(42L, snapshot.frameId());
    }

    @Test public void installRegistersOnlyInRenderProjectionAndDuplicateUsesPackSystemsRule() {
        NativeRenderInputSystem.install();
        assertEquals(1, PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION).size());
        assertTrue(PackSystems.systemsFor(PackSystemPhase.RENDER_PROJECTION).get(0)
                instanceof NativeRenderInputSystem);
        assertTrue(PackSystems.systemsFor(PackSystemPhase.EFFECTS).isEmpty());

        try {
            NativeRenderInputSystem.install();
            fail("expected duplicate system failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("already enabled"));
        }
    }

    private static List<String> stableKeys(NativeRenderFrameSnapshot snapshot) {
        List<String> keys = new ArrayList<String>();
        for (NativeRenderInputSnapshot input : snapshot.inputs()) keys.add(input.stableKey());
        return keys;
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

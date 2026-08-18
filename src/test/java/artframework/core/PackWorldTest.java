package artframework.core;

import artframework.assets.AssetPack;
import artframework.assets.HostAssets;
import artframework.api.ArtFramework;
import artframework.ecs.EcsPipeline;
import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.NodePropertiesComponent;

import org.junit.Test;
import org.junit.After;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Focused transaction coverage for declarative PresentPack runtime operations. */
public class PackWorldTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void failedEnableRollsBackEarlierComponentAndAssetOperations() {
        PresentationWorld entities = new PresentationWorld("pack-test");
        EntityId entity = entities.createEntity();
        HostAssets assets = new HostAssets();
        PackWorld world = new PackWorld(entities, assets);
        PresentPack pack = PresentPack.builder("mod.rollback")
                .operation(PackOperations.createComponent(
                        "mod.rollback.properties", entity, NodePropertiesComponent.class,
                        new NodePropertiesComponent(Collections.<String, Object>emptyMap())))
                .operation(PackOperations.registerAssetPack("mod.rollback.assets",
                        AssetPack.builder("mod.rollback.assets").build()))
                .operation(PackOperations.fail("mod.rollback.fail", "expected failure"))
                .build();

        try {
            world.enable(pack);
            fail("expected enable failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("expected failure"));
        }

        assertNull(entities.get(entity, NodePropertiesComponent.class));
        assertNull(assets.getPack("mod.rollback.assets"));
        assertFalse(world.isEnabled(pack.id));
    }

    @Test
    public void disableRestoresPreviousComponentAndAssetState() {
        PresentationWorld entities = new PresentationWorld("pack-test");
        EntityId entity = entities.createEntity();
        NodePropertiesComponent original =
                new NodePropertiesComponent(Collections.singletonMap("color", (Object) "base"));
        entities.put(entity, NodePropertiesComponent.class, original);
        HostAssets assets = new HostAssets();
        AssetPack originalAsset = AssetPack.builder("mod.shared").priority(1).build();
        assets.registerPack(originalAsset);
        PackWorld world = new PackWorld(entities, assets);
        NodePropertiesComponent replacement =
                new NodePropertiesComponent(Collections.singletonMap("color", (Object) "pack"));
        AssetPack replacementAsset = AssetPack.builder("mod.shared").priority(2).build();
        PresentPack pack = PresentPack.builder("mod.restore")
                .operation(PackOperations.updateComponent(
                        "mod.restore.properties", entity, NodePropertiesComponent.class, replacement))
                .operation(PackOperations.registerAssetPack("mod.restore.assets", replacementAsset))
                .build();

        world.enable(pack);
        assertEquals(replacement, entities.get(entity, NodePropertiesComponent.class));
        assertEquals(replacementAsset, assets.getPack("mod.shared"));

        world.disable(pack.id);
        assertEquals(original, entities.get(entity, NodePropertiesComponent.class));
        assertEquals(originalAsset, assets.getPack("mod.shared"));
        assertFalse(world.isEnabled(pack.id));
    }

    @Test
    public void dependencyMustBeEnabledBeforeOperationRuns() {
        PackWorld world = new PackWorld(new PresentationWorld("pack-test"), new HostAssets());
        PresentPack pack = PresentPack.builder("mod.requires")
                .operation(PackOperations.fail("mod.requires.operation", "must not run")
                        .requiresPack("mod.base"))
                .build();

        try {
            world.enable(pack);
            fail("expected dependency failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("requires enabled pack: mod.base"));
        }
    }

    @Test
    public void enabledSystemRunsOnlyInItsDeclaredSchedulePhaseAndIsRemovedOnDisable() {
        PackSystems.resetForTests();
        try {
            final AtomicInteger runs = new AtomicInteger();
            EcsSystem system = new EcsSystem() {
                @Override
                public void run(PresentationWorld ignored, EcsTick tick) {
                    runs.incrementAndGet();
                }
            };
            PackWorld world = new PackWorld(new PresentationWorld("pack-test"), new HostAssets());
            PresentPack pack = PresentPack.builder("mod.system")
                    .operation(PackOperations.enableSystem(
                            "mod.system.effects", PackSystemPhase.EFFECTS, system))
                    .build();

            world.enable(pack);
            EcsPipeline.run(world.entities(), new EcsTick(0f, 1L),
                    PackSystems.systemsFor(PackSystemPhase.EFFECTS));
            assertEquals(1, runs.get());

            world.disable(pack.id);
            assertTrue(PackSystems.systemsFor(PackSystemPhase.EFFECTS).isEmpty());
        } finally {
            PackSystems.resetForTests();
        }
    }

    @Test
    public void enabledRuntimeSystemRunsThroughTheFixedPresentationSchedulePhase() {
        final AtomicInteger runs = new AtomicInteger();
        PresentPack pack = PresentPack.builder("mod.schedule")
                .operation(PackOperations.enableSystem("mod.schedule.effects",
                        PackSystemPhase.EFFECTS, new EcsSystem() {
                            @Override public void run(PresentationWorld ignored, EcsTick tick) {
                                runs.incrementAndGet();
                            }
                        }))
                .build();

        PresentPackRuntime.enable(pack);
        ArtFramework.advanceFrame(0f, null);
        assertEquals(1, runs.get());
        PresentPackRuntime.disable(pack.id);
        ArtFramework.advanceFrame(0f, null);
        assertEquals(1, runs.get());
    }

    @Test
    public void undoFailureStillAttemptsEarlierUndoAndRetainsPackForRecovery() {
        final AtomicInteger calls = new AtomicInteger();
        PackWorld world = new PackWorld(new PresentationWorld("pack-test"), new HostAssets());
        PresentPack pack = PresentPack.builder("mod.undo")
                .operation(undoOperation("mod.undo.first", calls, false))
                .operation(undoOperation("mod.undo.middle", calls, true))
                .operation(undoOperation("mod.undo.last", calls, false))
                .build();
        world.enable(pack);

        try {
            world.disable(pack.id);
            fail("expected undo failure");
        } catch (IllegalStateException expected) {
            assertEquals("undo failure", expected.getMessage());
        }
        assertEquals(3, calls.get());
        assertTrue(world.isEnabled(pack.id));
    }

    @Test
    public void duplicateSystemIdIsRejectedWithoutRemovingOriginalSystem() {
        PackSystems.resetForTests();
        try {
            EcsSystem system = new EcsSystem() {
                @Override public void run(PresentationWorld ignored, EcsTick tick) {}
            };
            PackWorld world = new PackWorld(new PresentationWorld("pack-test"), new HostAssets());
            world.enable(PresentPack.builder("mod.one")
                    .operation(PackOperations.enableSystem("shared", PackSystemPhase.EFFECTS, system))
                    .build());
            try {
                world.enable(PresentPack.builder("mod.two")
                        .operation(PackOperations.enableSystem("shared", PackSystemPhase.EFFECTS, system))
                        .build());
                fail("expected duplicate system failure");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("already enabled"));
            }
            assertEquals(1, PackSystems.systemsFor(PackSystemPhase.EFFECTS).size());
        } finally {
            PackSystems.resetForTests();
        }
    }

    private static PackOperation undoOperation(
            String id, final AtomicInteger calls, final boolean failUndo) {
        return new PackOperation(id) {
            @Override Undo apply(PackWorld world) {
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        calls.incrementAndGet();
                        if (failUndo) throw new IllegalStateException("undo failure");
                    }
                };
            }
        };
    }
}

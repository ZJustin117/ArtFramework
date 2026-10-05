package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.context.SurfaceIds;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationVisuals;
import artframework.presentation.PackSurfaceEffectIdsComponent;
import artframework.render.RenderStateEcs;
import artframework.component.Rect;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TE-16: C2 surface effects are reversible ECS contributions. */
public class PackSurfaceEffectsTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void enableAndDisableExposeOneSurfaceContribution() {
        PresentPack pack = PresentPack.builder("mod.surface-effects")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("surface-fx", Collections.<String, Object>emptyMap()))
                .build();

        PresentPackRuntime.enable(pack);
        assertEquals(1, ArtEcs.world().query(PackSurfaceEffectsComponent.class).size());
        assertEquals("surface-fx", PackSurfaceEffects.forSurface(
                ArtEcs.world(), SurfaceIds.COMBAT_HAND).get(0).id);

        PresentPackRuntime.disable(pack.id);
        assertTrue(ArtEcs.world().query(PackSurfaceEffectsComponent.class).isEmpty());
    }

    @Test
    public void materializedC2ItemReadsEcsSurfaceContribution() {
        PresentPack pack = PresentPack.builder("mod.surface-visual")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("ecs-surface", Collections.<String, Object>emptyMap()))
                .build();
        PresentPackRuntime.enable(pack);

        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        EntityId item = PresentationVisuals.syncC2Item(SurfaceIds.COMBAT_HAND, "item",
                new Rect(0f, 0f, 20f, 20f), 1f, "card", "", "", true);
        EffectsComponent effects = context.world().get(item, EffectsComponent.class);
        assertEquals("ecs-surface", effects.attachments().get(0).effectId);
        PresentPackRuntime.disable(pack.id);
    }

    @Test
    public void explicitMigratedSurfaceOperationProjectsWithoutLegacySurfaceKeys() {
        java.util.Map<String, java.util.List<EffectDecl>> effects =
                new java.util.LinkedHashMap<String, java.util.List<EffectDecl>>();
        effects.put(SurfaceIds.EVENT, Collections.singletonList(
                new EffectDecl("explicit-surface", Collections.<String, Object>emptyMap())));
        PresentPack pack = new PresentPack("mod.explicit-surface", "", "", "",
                Collections.<PresentPack.TemplateEntry>emptyList(),
                Collections.<PresentPack.WindowEntry>emptyList(), Collections.<String>emptyList(),
                Collections.<String, java.util.List<EffectDecl>>emptyMap(),
                Collections.<EffectDecl>emptyList(), Collections.<String>emptyList(),
                Collections.<String, java.util.List<EffectDecl>>emptyMap(), true, false, false,
                Collections.singletonList(PackOperations.createSurfaceEffects(
                        "mod.explicit-surface.surface-effects", "mod.explicit-surface", effects)));
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);

        assertEquals("explicit-surface",
                RenderStateEcs.surfaceState(SurfaceIds.EVENT).effects().get(0).effectId);
    }

    @Test
    public void genericCrudCannotWriteSurfaceContribution() {
        PresentationWorld world = new PresentationWorld("surface-test");
        EntityId entity = world.createEntity();
        PackSurfaceEffectsComponent value = new PackSurfaceEffectsComponent("foreign",
                Collections.<String, java.util.List<EffectDecl>>emptyMap());
        try {
            PackOperations.createComponent("surface.create", entity,
                    PackSurfaceEffectsComponent.class, value);
            fail("expected reserved contribution rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("surfaceEffects declaration"));
        }
        try {
            PackOperations.updateComponent("surface.update", entity,
                    PackSurfaceEffectsComponent.class, value);
            fail("expected reserved contribution rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("surfaceEffects declaration"));
        }
        assertFalse(PackSurfaceEffects.hasContribution(world));
    }

    @Test
    public void resyncReplacesStalePackEffectOnExistingC2Item() {
        PresentPack first = PresentPack.builder("mod.surface-a")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("surface-a", Collections.<String, Object>emptyMap()))
                .build();
        PresentPackRuntime.enable(first);
        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        EntityId item = PresentationVisuals.syncC2Item(SurfaceIds.COMBAT_HAND, "stable",
                new Rect(0f, 0f, 20f, 20f), 1f, "card", "", "", true);
        assertEquals("surface-a", context.world().get(item, EffectsComponent.class)
                .attachments().get(0).effectId);
        PresentPackRuntime.disable(first.id);

        PresentPack second = PresentPack.builder("mod.surface-b")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("surface-b", Collections.<String, Object>emptyMap()))
                .build();
        PresentPackRuntime.enable(second);
        PresentationVisuals.syncC2Item(SurfaceIds.COMBAT_HAND, "stable",
                new Rect(0f, 0f, 20f, 20f), 1f, "card", "", "", true);
        EffectsComponent effects = context.world().get(item, EffectsComponent.class);
        assertEquals(1, effects.attachments().size());
        assertEquals("surface-b", effects.attachments().get(0).effectId);
        assertEquals(1, context.world().get(item, PackSurfaceEffectIdsComponent.class)
                .effectIds().size());
    }

    @Test
    public void projectionUsesOnlyTheActivePackContribution() {
        PresentPack first = PresentPack.builder("mod.surface-owner-a")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("surface-a", Collections.<String, Object>emptyMap()))
                .build();
        PresentPack second = PresentPack.builder("mod.surface-owner-b")
                .surfaceEffect(SurfaceIds.EVENT,
                        new EffectDecl("surface-b", Collections.<String, Object>emptyMap()))
                .build();
        PresentPacks.register(first);
        PresentPacks.register(second);
        PresentPacks.activate(first.id);
        PresentPackRuntime.enable(second);

        PresentPackApply.syncFromActivePack();

        assertEquals("surface-a", RenderStateEcs.surfaceState(SurfaceIds.COMBAT_HAND)
                .effects().get(0).effectId);
        assertEquals(null, RenderStateEcs.surfaceState(SurfaceIds.EVENT));
    }

    @Test
    public void activeLegacyEffectsAreNotSuppressedByForeignMigratedContribution() {
        PresentPack legacy = new PresentPack("mod.surface-legacy-active", "", "", "",
                Collections.<PresentPack.TemplateEntry>emptyList(),
                Collections.<PresentPack.WindowEntry>emptyList(), Collections.<String>emptyList(),
                Collections.<String, java.util.List<EffectDecl>>emptyMap(),
                Collections.<EffectDecl>emptyList(), Collections.<String>emptyList(),
                Collections.singletonMap(SurfaceIds.EVENT, Collections.singletonList(
                        new EffectDecl("legacy-active", Collections.<String, Object>emptyMap()))),
                true, false, false);
        PresentPack foreign = PresentPack.builder("mod.surface-foreign")
                .surfaceEffect(SurfaceIds.COMBAT_HAND,
                        new EffectDecl("foreign", Collections.<String, Object>emptyMap())).build();
        PresentPacks.register(legacy);
        PresentPacks.register(foreign);
        PresentPacks.activate(legacy.id);
        PresentPackRuntime.enable(foreign);

        PresentPackApply.syncFromActivePack();

        assertEquals("legacy-active", RenderStateEcs.surfaceState(SurfaceIds.EVENT)
                .effects().get(0).effectId);
    }

    @Test
    public void nonPackSurfaceWriterSurvivesPackCleanup() {
        PresentPack pack = PresentPack.builder("mod.surface-writer")
                .surfaceEffect(SurfaceIds.EVENT,
                        new EffectDecl("pack", Collections.<String, Object>emptyMap())).build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        RenderStateEcs.surface(SurfaceIds.EVENT, 99f, 10f, 700f, 80f, false);
        PresentPacks.deactivate(pack.id);

        assertEquals(700f, RenderStateEcs.surfaceState(SurfaceIds.EVENT).bounds.width, 0.01f);
        assertFalse(RenderStateEcs.surfaceState(SurfaceIds.EVENT).enabled);
    }

    /**
     * Deterministic CONTRACT test: after an entity is removed, the surface readers tolerate the
     * absent id (empty results) and never throw. This only pins the absent-entity contract — the
     * entity is destroyed before the read, so {@code query} already returns empty and this does NOT
     * exercise the concurrent race. The CONCURRENT coverage (destroy racing between a public
     * {@code query} returning an id and the per-entity read) lives in
     * {@link #forSurfaceIsSafeUnderConcurrentMutation()}.
     */
    @Test
    public void forSurfaceToleratesRemovedEntity() {
        PresentationWorld world = new PresentationWorld("surface-race");
        EntityId entity = world.createEntity();
        world.put(entity, PackSurfaceEffectsComponent.class, new PackSurfaceEffectsComponent(
                "mod.race", Collections.singletonMap(
                        SurfaceIds.EVENT, Collections.singletonList(
                                new EffectDecl("race-fx", Collections.<String, Object>emptyMap())))));
        assertFalse(PackSurfaceEffects.forSurface(world, "mod.race", SurfaceIds.EVENT).isEmpty());

        world.destroyEntity(entity);

        // The entity is absent, so the readers must report nothing and tolerate the missing id.
        assertTrue(PackSurfaceEffects.forSurface(world, "mod.race", SurfaceIds.EVENT).isEmpty());
        assertFalse(PackSurfaceEffects.hasContribution(world, "mod.race"));
        assertTrue(PackSurfaceEffects.surfaceIds(world, "mod.race").isEmpty());
    }

    /**
     * Stress the real read path (query + get) against concurrent structural mutation of the same
     * world. Before the snapshot-safe query and destroyed-entity skip this threw
     * ConcurrentModificationException / "unknown entity".
     */
    @Test
    public void forSurfaceIsSafeUnderConcurrentMutation() throws Exception {
        final PresentationWorld world = new PresentationWorld("surface-stress");
        final int stable = 8;
        for (int i = 0; i < stable; i++) {
            EntityId id = world.createEntity();
            world.put(id, PackSurfaceEffectsComponent.class, component("mod.stress", "stress-fx"));
        }
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        Thread writer = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    start.await();
                    for (int i = 0; i < 60000; i++) {
                        EntityId id = world.createEntity();
                        world.put(id, PackSurfaceEffectsComponent.class,
                                component("mod.stress", "stress-fx"));
                        world.destroyEntity(id);
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        });
        Thread reader = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    start.await();
                    for (int i = 0; i < 60000; i++) {
                        List<EffectDecl> effects =
                                PackSurfaceEffects.forSurface(world, "mod.stress", SurfaceIds.EVENT);
                        assertTrue(effects.size() >= stable);
                        for (int j = 0; j < effects.size(); j++) {
                            assertEquals("stress-fx", effects.get(j).id);
                        }
                        assertTrue(PackSurfaceEffects.hasContribution(world, "mod.stress"));
                        PackSurfaceEffects.surfaceIds(world, "mod.stress");
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        });
        writer.start();
        reader.start();
        start.countDown();
        writer.join(TimeUnit.SECONDS.toMillis(30));
        reader.join(TimeUnit.SECONDS.toMillis(30));
        assertFalse("threads did not finish in time", writer.isAlive() || reader.isAlive());
        if (failure.get() != null) {
            throw new AssertionError("concurrent surface read threw", failure.get());
        }
    }

    private static PackSurfaceEffectsComponent component(String packId, String effectId) {
        Map<String, List<EffectDecl>> bySurface = new LinkedHashMap<String, List<EffectDecl>>();
        bySurface.put(SurfaceIds.EVENT, Collections.singletonList(
                new EffectDecl(effectId, Collections.<String, Object>emptyMap())));
        return new PackSurfaceEffectsComponent(packId, bySurface);
    }
}

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
}

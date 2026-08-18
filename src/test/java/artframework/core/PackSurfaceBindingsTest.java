package artframework.core;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TE-18: pack surface bindings are reversible ECS contributions. */
public class PackSurfaceBindingsTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    @Test
    public void enableAndDisableCreateAndRemoveBindingContribution() {
        PresentPack pack = PresentPack.builder("mod.bindings")
                .profileId(PresentProfiles.LIGHTWAVE)
                .bindSurface(SurfaceIds.EVENT)
                .build();
        PresentPackRuntime.enable(pack);
        assertEquals(PresentProfiles.LIGHTWAVE,
                PackSurfaceBindings.forPack(ArtEcs.world(), pack.id).get(SurfaceIds.EVENT));
        PresentPackRuntime.disable(pack.id);
        assertTrue(PackSurfaceBindings.forPack(ArtEcs.world(), pack.id).isEmpty());
    }

    @Test
    public void presentPackApplyProjectsOwnerContributionToSurfacePresent() {
        PresentPack pack = PresentPack.builder("mod.bind-project")
                .profileId(PresentProfiles.LIGHTWAVE)
                .bindSurface(SurfaceIds.EVENT)
                .build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        assertEquals(PresentProfiles.LIGHTWAVE, SurfacePresent.profileId(SurfaceIds.EVENT));
        PresentPacks.deactivate(pack.id);
        assertTrue(SurfacePresent.profileId(SurfaceIds.EVENT) == null);
    }

    @Test
    public void genericCrudCannotWriteBindingContribution() {
        PresentationWorld world = new PresentationWorld("binding-test");
        EntityId entity = world.createEntity();
        PackSurfaceBindingsComponent value = new PackSurfaceBindingsComponent(
                "foreign", PresentProfiles.LIGHTWAVE, Collections.singletonList(SurfaceIds.EVENT));
        try {
            PackOperations.createComponent("binding.create", entity,
                    PackSurfaceBindingsComponent.class, value);
            fail("expected reserved contribution rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("bindSurfaces declaration"));
        }
        try {
            PackOperations.updateComponent("binding.update", entity,
                    PackSurfaceBindingsComponent.class, value);
            fail("expected reserved contribution rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("bindSurfaces declaration"));
        }
        assertFalse(PackSurfaceBindings.hasContribution(world, "foreign"));
    }

    @Test
    public void emptyContributionForAnotherPackDoesNotSuppressActiveOwnerFallback() {
        PresentPack migrated = new PresentPack("mod.empty-binding", "lightwave", "", "",
                Collections.<PresentPack.TemplateEntry>emptyList(),
                Collections.<PresentPack.WindowEntry>emptyList(), Collections.<String>emptyList(),
                Collections.<String, java.util.List<artframework.component.EffectDecl>>emptyMap(),
                Collections.<artframework.component.EffectDecl>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String, java.util.List<artframework.component.EffectDecl>>emptyMap(),
                true, false, false,
                Collections.singletonList(PackOperations.createSurfaceBindings(
                        "mod.empty-binding.operation", "mod.empty-binding", "lightwave",
                        Collections.<String>emptyList())));
        PresentPack legacy = PresentPack.builder("mod.legacy-binding")
                .profileId(PresentProfiles.LIGHTWAVE).bindSurface(SurfaceIds.EVENT).build();
        PresentPacks.register(legacy);
        PresentPackRuntime.enable(migrated);
        PresentPacks.activate(legacy.id);
        assertEquals(PresentProfiles.LIGHTWAVE, SurfacePresent.profileId(SurfaceIds.EVENT));
    }

    @Test
    public void disablingPackRestoresPreviousSurfaceBinding() {
        SurfacePresent.bind(SurfaceIds.EVENT, PresentProfiles.LIGHTWAVE);
        PresentPack pack = PresentPack.builder("mod.restore-binding")
                .profileId(PresentProfiles.STS)
                .bindSurface(SurfaceIds.EVENT).build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        assertEquals(PresentProfiles.STS, SurfacePresent.profileId(SurfaceIds.EVENT));
        PresentPacks.deactivate(pack.id);
        assertEquals(PresentProfiles.LIGHTWAVE, SurfacePresent.profileId(SurfaceIds.EVENT));
    }

    @Test
    public void profileIdWhitespaceIsNormalizedBeforeBinding() {
        PresentPack pack = PresentPack.builder("mod.trim-binding")
                .profileId("  " + PresentProfiles.LIGHTWAVE + "  ")
                .bindSurface(SurfaceIds.EVENT).build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        assertEquals(PresentProfiles.LIGHTWAVE, SurfacePresent.profileId(SurfaceIds.EVENT));
    }
}

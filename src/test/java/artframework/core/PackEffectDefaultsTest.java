package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.presentation.C1Materializer;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.PresentationRuntime;
import artframework.render.RenderPlan;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TE-15: default C1 effects are enabled PackWorld ECS data, not only facade lookup state. */
public class PackEffectDefaultsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void enableCreatesAndDisableRemovesTheEffectDefaultContribution() {
        PresentPack pack = PresentPack.builder("mod.effects")
                .effectDefault("panel", new EffectDecl("lightwave", Collections.<String, Object>emptyMap()))
                .build();

        PresentPackRuntime.enable(pack);
        assertEquals(1, ArtEcs.world().query(PackEffectDefaultsComponent.class).size());
        assertFalse(PackEffectDefaults.forNodeType(ArtEcs.world(), "panel").isEmpty());
        EntityId entity = ArtEcs.world().query(PackEffectDefaultsComponent.class).get(0);
        assertEquals(pack.id, ArtEcs.world().get(entity, PackEffectDefaultsComponent.class).packId);

        PresentPackRuntime.disable(pack.id);
        assertTrue(ArtEcs.world().query(PackEffectDefaultsComponent.class).isEmpty());
        assertTrue(PackEffectDefaults.forNodeType(ArtEcs.world(), "panel").isEmpty());
    }

    @Test
    public void duplicateOrForeignEffectDefaultOperationsAreRejectedBeforeEnable() {
        try {
            PresentPack.builder("mod.duplicate")
                    .effectDefault("panel", new EffectDecl("lightwave", Collections.<String, Object>emptyMap()))
                    .operation(PackOperations.createEffectDefaults("mod.duplicate.explicit", "mod.duplicate",
                            Collections.<String, java.util.List<EffectDecl>>emptyMap()))
                    .build();
            fail("expected duplicate effect default rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("effectDefaults"));
        }
        try {
            PresentPack.builder("mod.owner")
                    .operation(PackOperations.createEffectDefaults("mod.owner.foreign", "mod.other",
                            Collections.<String, java.util.List<EffectDecl>>emptyMap()))
                    .build();
            fail("expected foreign owner rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("must match"));
        }
    }

    @Test
    public void genericComponentOperationsCannotWriteReservedEffectDefaults() {
        PresentationWorld world = new PresentationWorld("pack-effect-test");
        EntityId entity = world.createEntity();
        PackEffectDefaultsComponent contribution = new PackEffectDefaultsComponent("mod.foreign",
                Collections.<String, java.util.List<EffectDecl>>emptyMap());
        try {
            PackOperations.createComponent("mod.generic.create", entity,
                    PackEffectDefaultsComponent.class, contribution);
            fail("expected reserved component rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("requires a PresentPack"));
        }
        try {
            PackOperations.updateComponent("mod.generic.update", entity,
                    PackEffectDefaultsComponent.class, contribution);
            fail("expected reserved component rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("requires a PresentPack"));
        }
    }

    @Test
    public void emptyEcsContributionSuppressesLegacyFallbackForMissingNodeType() {
        PresentPack pack = PresentPack.builder("mod.empty-effects")
                .effectDefault("panel", new EffectDecl("legacy-panel",
                        Collections.<String, Object>emptyMap()))
                .effectDefault("label", new EffectDecl("legacy-label",
                        Collections.<String, Object>emptyMap()))
                .build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        EntityId original = ArtEcs.world().query(PackEffectDefaultsComponent.class).get(0);
        ArtEcs.world().destroyEntity(original);
        EntityId empty = ArtEcs.world().createEntity();
        ArtEcs.world().put(empty, PackEffectDefaultsComponent.class,
                new PackEffectDefaultsComponent(pack.id,
                        Collections.<String, java.util.List<EffectDecl>>emptyMap()));
        assertTrue(PackEffectDefaults.hasContribution(ArtEcs.world()));
        assertTrue(PackEffectDefaults.forNodeType(ArtEcs.world(), "label").isEmpty());

        PresentationContext context = PresentationRegistry.context(PresentationRuntime.c1Scope("empty-effects"));
        EntityId root = C1Materializer.mount(context,
                artframework.component.UiNode.of("panel").id("root").build());
        assertTrue(context.world().get(root, EffectsComponent.class).attachments().isEmpty());

        RenderPlan plan = RenderPlan.fromEcs(null);
        boolean foundC1 = false;
        for (RenderPlan.Entry entry : plan.entries()) {
            if (entry.id.startsWith("c1:empty-effects")) {
                foundC1 = true;
                assertTrue(entry.effects.isEmpty());
            }
        }
        assertTrue(foundC1);
        PresentPacks.deactivate(pack.id);
    }

    @Test
    public void compatibilityConstructorContributesThroughPresentPacksLifecycle() {
        PresentPack pack = new PresentPack("mod.compat-effects", "", "", "",
                Collections.<PresentPack.TemplateEntry>emptyList(),
                Collections.<PresentPack.WindowEntry>emptyList(), Collections.<String>emptyList(),
                Collections.singletonMap("panel", Collections.singletonList(
                        new EffectDecl("compat", Collections.<String, Object>emptyMap()))),
                Collections.<EffectDecl>emptyList(), Collections.<String>emptyList(),
                Collections.<String, java.util.List<EffectDecl>>emptyMap(), true, false, false);
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);
        assertEquals(1, ArtEcs.world().query(PackEffectDefaultsComponent.class).size());
        assertEquals("compat", PackEffectDefaults.forNodeType(ArtEcs.world(), "panel").get(0).id);

        PresentPacks.deactivate(pack.id);
        assertTrue(ArtEcs.world().query(PackEffectDefaultsComponent.class).isEmpty());
    }
}

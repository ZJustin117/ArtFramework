package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.FullFrameRenderComponent;
import artframework.render.RenderStateEcs;
import artframework.presentation.EffectAttachment;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TE-17: full-frame pack effects are reversible ECS contributions. */
public class PackFullFrameEffectsTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void enableAndDisableCreateAndRemoveFullFrameContribution() {
        PresentPack pack = PresentPack.builder("mod.full-frame")
                .fullFrameEffect(new EffectDecl("frame-fx", Collections.<String, Object>emptyMap()))
                .build();
        PresentPackRuntime.enable(pack);
        assertEquals(1, ArtEcs.world().query(PackFullFrameEffectsComponent.class).size());
        assertEquals("frame-fx", PackFullFrameEffects.effects(ArtEcs.world()).get(0).id);
        PresentPackRuntime.disable(pack.id);
        assertTrue(ArtEcs.world().query(PackFullFrameEffectsComponent.class).isEmpty());
    }

    @Test
    public void fullFrameProjectionReadsMigratedContribution() {
        PresentPack pack = PresentPack.builder("mod.full-frame-project")
                .fullFrameEffect(new EffectDecl("ecs-frame", Collections.<String, Object>emptyMap()))
                .build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        FullFrameRenderComponent state = RenderStateEcs.fullFrameState();
        assertTrue(state != null && state.enabled);
        assertEquals("ecs-frame", state.effects().get(0).effectId);
    }

    @Test
    public void deactivatingPackPreservesPreExistingFullFrameState() {
        RenderStateEcs.fullFrame(640f, 360f, true,
                Collections.singletonList(new EffectAttachment("existing-frame", "ambient",
                        Collections.<String, Object>emptyMap())));
        PresentPack pack = PresentPack.builder("mod.full-frame-preserve")
                .fullFrameEffect(new EffectDecl("pack-frame", Collections.<String, Object>emptyMap()))
                .build();
        PresentPacks.register(pack);

        PresentPacks.activate(pack.id);
        PresentPacks.deactivate(pack.id);

        assertNotNull(RenderStateEcs.fullFrameState());
        assertEquals(640f, RenderStateEcs.fullFrameState().bounds.width, 0.01f);
        assertEquals("existing-frame", RenderStateEcs.fullFrameState().effects().get(0).effectId);
    }

    @Test
    public void nonPackFullFrameWriterSurvivesPackCleanup() {
        PresentPack pack = PresentPack.builder("mod.full-frame-writer")
                .fullFrameEffect(new EffectDecl("pack-frame", Collections.<String, Object>emptyMap()))
                .build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        RenderStateEcs.fullFrame(800f, 400f, false,
                Collections.singletonList(new EffectAttachment("external-frame", "ambient",
                        Collections.<String, Object>emptyMap())));
        PresentPacks.deactivate(pack.id);

        assertEquals(800f, RenderStateEcs.fullFrameState().bounds.width, 0.01f);
        assertFalse(RenderStateEcs.fullFrameState().enabled);
        assertEquals("external-frame", RenderStateEcs.fullFrameState().effects().get(0).effectId);
    }

    @Test
    public void genericCrudCannotWriteFullFrameContribution() {
        PresentationWorld world = new PresentationWorld("full-frame-test");
        EntityId entity = world.createEntity();
        PackFullFrameEffectsComponent value = new PackFullFrameEffectsComponent("foreign",
                Collections.<EffectDecl>emptyList());
        try {
            PackOperations.createComponent("full-frame.create", entity,
                    PackFullFrameEffectsComponent.class, value);
            fail("expected reserved contribution rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("fullFrameEffects declaration"));
        }
        assertFalse(PackFullFrameEffects.hasContribution(world));
    }
}

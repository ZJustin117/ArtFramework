package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.ecs.ArtEcs;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.FullFrameRenderComponent;
import artframework.render.RenderStateEcs;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

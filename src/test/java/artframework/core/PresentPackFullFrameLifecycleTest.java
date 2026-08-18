package artframework.core;

import artframework.api.ArtFramework;
import artframework.component.EffectDecl;
import artframework.render.RenderStateEcs;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Full-frame pack ownership and disable cleanup coverage. */
public class PresentPackFullFrameLifecycleTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    @Test
    public void activePackProjectionDoesNotAggregateAnotherEnabledPack() {
        PresentPack first = PresentPack.builder("frame.a")
                .fullFrameEffect(new EffectDecl("a", Collections.<String, Object>emptyMap())).build();
        PresentPack second = PresentPack.builder("frame.b")
                .fullFrameEffect(new EffectDecl("b", Collections.<String, Object>emptyMap())).build();
        PresentPackRuntime.enable(first);
        PresentPackRuntime.enable(second);
        PresentPacks.register(first);
        PresentPacks.register(second);
        PresentPacks.activate(second.id);
        assertEquals("b", RenderStateEcs.fullFrameState().effects().get(0).effectId);
    }

    @Test
    public void deactivationClearsActiveProjectionEvenWhenUndoFails() {
        PresentPack pack = PresentPack.builder("frame.failure")
                .fullFrameEffect(new EffectDecl("frame", Collections.<String, Object>emptyMap()))
                .operation(new PackOperation("frame.failure.undo") {
                    @Override Undo apply(PackWorld world) {
                        return new Undo() {
                            @Override public void undo(PackWorld ignored) {
                                throw new IllegalStateException("undo failure");
                            }
                        };
                    }
                }).build();
        PresentPacks.register(pack);
        PresentPacks.activate(pack.id);
        try {
            PresentPacks.deactivate(pack.id);
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("undo failure"));
        }
        assertEquals("", PresentPacks.activeId());
        assertNull(RenderStateEcs.fullFrameState());
        assertTrue(PresentPackRuntime.isEnabled(pack.id));
        assertTrue(PackFullFrameEffects.effects(artframework.ecs.ArtEcs.world(), pack.id).isEmpty());
    }
}

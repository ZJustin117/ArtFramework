package artframework.sts1.patch;

import artframework.api.ArtFramework;
import artframework.sts1.render.NativeRenderBridge;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pure logic for the container-driven observation helper. The ExprEditor call-site
 * replacement itself can only be proven on-device (ModTheSpire instrumentation).
 */
public class TransientEffectContainerPatchesTest {
    @Before
    public void setUp() {
        NativeRenderBridge.resetForTests();
    }

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        NativeRenderBridge.resetForTests();
    }

    private static final class RecordingEffect extends AbstractGameEffect {
        int drawCount;

        @Override
        public void render(SpriteBatch sb) {
            drawCount++;
        }

        @Override
        public void dispose() {
        }
    }

    private static final class ThrowingEffect extends AbstractGameEffect {
        @Override
        public void render(SpriteBatch sb) {
            throw new IllegalStateException("native draw failure");
        }

        @Override
        public void dispose() {
        }
    }

    @Test
    public void observeThenRenderKeepsNativeDrawExactlyOnce() {
        RecordingEffect effect = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals(1, effect.drawCount);
        assertEquals("render", NativeRenderBridge.ledger()
                .invocations().get(0).nativeMethod);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.effectLedger().probeSlice()
                .get("rendered"));
    }

    @Test
    public void nativeDrawFailureFailsOpenWithoutBlockingLaterEffects() {
        ThrowingEffect failing = new ThrowingEffect();
        RecordingEffect following = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(failing, null);
        TransientEffectContainerPatches.observeThenRender(following, null);

        assertEquals(1, following.drawCount);
        assertEquals(Integer.valueOf(1),
                NativeRenderBridge.effectLedger().probeSlice().get("failOpen"));
    }

    @Test
    public void observationEntryIsRecordedPerInstance() {
        RecordingEffect effect = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals("transient_effect", NativeRenderBridge.ledger()
                .invocations().get(0).surfaceFamily);
        assertEquals(1, NativeRenderBridge.effectLedger().activeCount());
    }
}

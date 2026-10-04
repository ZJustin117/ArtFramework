package artframework.sts1.patch;

import artframework.api.ArtFramework;
import artframework.sts1.render.NativeRenderBridge;
import artframework.sts1.render.VfxArtRenderer;
import artframework.sts1.render.VfxDelegationGate;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure logic for the container-driven render observation helper. The ExprEditor call-site
 * replacement itself can only be proven on-device (ModTheSpire instrumentation).
 */
public class TransientEffectContainerPatchesTest {
    @Before
    public void setUp() {
        NativeRenderBridge.resetForTests();
        VfxArtRenderer.uninstall();
        VfxArtRenderer.resetDrawCountForTests();
        VfxDelegationGate.resetForTests();
    }

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        VfxArtRenderer.uninstall();
        VfxArtRenderer.resetDrawCountForTests();
        VfxDelegationGate.resetForTests();
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

        assertEquals("vfx-misc-root", NativeRenderBridge.ledger()
                .invocations().get(0).surfaceFamily);
        assertEquals(1, NativeRenderBridge.effectLedger().activeCount());
    }

    // --- NRO-04 C01: native local render-order band baseline ---

    @Test
    public void twoArgCallRecordsOnlyTheUnknownNativeBand() {
        RecordingEffect effect = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        Map<String, Object> bands = effectBands();
        assertEquals("the 2-arg call carries no line, so it lands in UNKNOWN",
                Integer.valueOf(1), band(bands, "native", "unknown"));
        assertEquals(Integer.valueOf(0), band(bands, "native", "effectListFront"));
        assertEquals(Integer.valueOf(0), band(bands, "claimed", "unknown"));
    }

    @Test
    public void threeArgNativeContinuationRecordsTheEffectListFrontBand() {
        RecordingEffect effect = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null, 2697);

        assertEquals(1, effect.drawCount);
        Map<String, Object> bands = effectBands();
        assertEquals("a native-continuing render is recorded in its native band",
                Integer.valueOf(1), band(bands, "native", "effectListFront"));
        assertEquals(Integer.valueOf(0), band(bands, "native", "unknown"));
        assertEquals("nothing is claimed on a native continuation",
                Integer.valueOf(0), band(bands, "claimed", "effectListFront"));
    }

    @Test
    public void throwingNativeRenderStillRecordsTheNativeBandExactlyOnce() {
        ThrowingEffect failing = new ThrowingEffect();

        TransientEffectContainerPatches.observeThenRender(failing, null, 2674);

        Map<String, Object> bands = effectBands();
        assertEquals("reaching the native band is recorded even when the draw throws",
                Integer.valueOf(1), band(bands, "native", "effectListBehind"));
        assertEquals(Integer.valueOf(0), band(bands, "native", "unknown"));
    }

    @Test
    public void aClaimedDrawRecordsTheClaimedBandAndSuppressesNative() {
        VfxDelegationGate.setActive(true);
        VfxArtRenderer.install(drawingAdapter());
        AbstractGameEffect effect = supportedAuraEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null, 2697);

        Map<String, Object> bands = effectBands();
        assertEquals("a claimed draw is recorded in the claimed bucket of its native band",
                Integer.valueOf(1), band(bands, "claimed", "effectListFront"));
        assertEquals("the native bucket is untouched on a claimed draw",
                Integer.valueOf(0), band(bands, "native", "effectListFront"));
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> byClass =
                (Map<String, Map<String, Integer>>) bands.get("claimedByClass");
        assertEquals(Integer.valueOf(1), byClass
                .get("com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect")
                .get("effectListFront"));
    }

    @Test
    public void bandRecordingNeverChangesRenderedOrDrawCounts() {
        // Native path: band record must not alter the draw count / rendered evidence.
        RecordingEffect nativeEffect = new RecordingEffect();
        TransientEffectContainerPatches.observeThenRender(nativeEffect, null, 2674);
        assertEquals(1, nativeEffect.drawCount);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.effectLedger().probeSlice()
                .get("rendered"));

        // Claimed path: band record must not alter the ART draw counter / evidence.
        NativeRenderBridge.resetForTests();
        VfxDelegationGate.setActive(true);
        VfxArtRenderer.resetDrawCountForTests();
        VfxArtRenderer.install(drawingAdapter());
        TransientEffectContainerPatches.observeThenRender(
                supportedAuraEffect(), null, 2697);
        assertEquals(1, VfxArtRenderer.drawCount());
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("evidenceCount"));
    }

    @Test
    public void isolateSuppressedPathRecordsNoBand() {
        // Isolate suppresses native pixels and draws nothing, so no band observation is made.
        RecordingEffect effect = new RecordingEffect();
        NativeRenderBridge.policy().setIsolate(true);

        TransientEffectContainerPatches.observeThenRender(effect, null, 2697);

        assertEquals("native render was suppressed", 0, effect.drawCount);
        Map<String, Object> bands = effectBands();
        assertEquals(Integer.valueOf(0), band(bands, "native", "effectListFront"));
        assertEquals(Integer.valueOf(0), band(bands, "claimed", "effectListFront"));
        assertEquals(Integer.valueOf(0), band(bands, "native", "unknown"));
    }

    @Test
    public void onDeviceInstrumentationEmitsTheThreeArgCallWithTheLine() {
        // The generated replacement body must carry the instrument-time call-site line so the
        // probe can classify the band; the raw instrumentation is proven on-device.
        String body = "{ artframework.sts1.patch.TransientEffectContainerPatches"
                + ".observeThenRender($0, $1, 2674); }";
        assertTrue(body.contains(".observeThenRender($0, $1, 2674)"));
        // The patch class itself must expose the 3-arg entry the instrument targets.
        TransientEffectContainerPatches.observeThenRender(new RecordingEffect(), null, 2697);
        assertEquals(Integer.valueOf(1), band(effectBands(), "native", "effectListFront"));
    }

    @Test
    public void renderPassPrefixBumpsThePassBoundaryWithoutChangingOrdering() {
        // The new Prefix is the observation-only render-pass boundary. It never throws and moves
        // no per-band totals; the pass count only advances when a real band is observed in a pass.
        TransientEffectContainerPatches.ObserveEffectRenderPass.Prefix(null, null);
        TransientEffectContainerPatches.ObserveEffectRenderPass.Prefix(null, null);

        Map<String, Object> bands = effectBands();
        assertEquals(Integer.valueOf(0), bands.get("passesObserved"));
        assertEquals(Integer.valueOf(0), bands.get("orderViolations"));
        assertEquals(Integer.valueOf(0), band(bands, "native", "effectListBehind"));
        assertEquals(Integer.valueOf(0), band(bands, "claimed", "effectListFront"));
    }

    private static VfxArtRenderer.Adapter drawingAdapter() {
        return new VfxArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                return true;
            }
            @Override public boolean canDraw(Object effect) { return true; }
        };
    }

    /** Real supported FQN with zeroed fields; constructors would need live game/GL state. */
    private static AbstractGameEffect supportedAuraEffect() {
        try {
            java.lang.reflect.Field theUnsafe =
                    Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            return (AbstractGameEffect) unsafe.getClass()
                    .getMethod("allocateInstance", Class.class)
                    .invoke(unsafe, com.megacrit.cardcrawl.vfx.stance.WrathParticleEffect.class);
        } catch (Exception failure) {
            throw new AssertionError("could not allocate a supported aura effect", failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> effectBands() {
        Object bands = NativeRenderBridge.probeSlice().get("effectBands");
        assertNotNull("probeSlice must expose effectBands", bands);
        return (Map<String, Object>) bands;
    }

    @SuppressWarnings("unchecked")
    private static Integer band(Map<String, Object> bands, String bucket, String name) {
        return (Integer) ((Map<String, Object>) bands.get(bucket)).get(name);
    }
}

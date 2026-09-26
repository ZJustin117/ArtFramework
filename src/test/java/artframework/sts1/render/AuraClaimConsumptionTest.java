package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.sts1.patch.TransientEffectContainerPatches;
import artframework.sts1.patch.TransientEffectRenderPatches;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic coverage for how the effect-container seam consumes an F1 aura claim.
 *
 * <p>A claim requires the exact supported FQN, so a local recording stub cannot be claimed. The
 * "native render was reached" signal is therefore the native effect ledger's {@code failOpen}
 * counter: a real supported aura class drawn off-GL throws inside {@code effect.render} and the
 * observation helper records the fail-open exactly once per reached native call. A suppressed
 * native call leaves {@code failOpen} at zero and leaves ART pixel evidence behind instead.
 */
public class AuraClaimConsumptionTest {

    @Before
    public void setUp() {
        NativeRenderBridge.resetForTests();
        AuraArtRenderer.resetDrawCountForTests();
    }

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetForTests();
        AuraArtRenderer.resetDrawCountForTests();
        NativeRenderBridge.resetForTests();
    }

    private static final class RecordingEffect extends AbstractGameEffect {
        int drawCount;

        @Override public void render(SpriteBatch sb) { drawCount++; }
        @Override public void dispose() { }
    }

    @Test
    public void gateOffAlwaysFallsThroughToNativeRender() {
        RecordingEffect effect = new RecordingEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals("gate off keeps the native effect queue authoritative", 1, effect.drawCount);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
    }

    @Test
    public void readyAdapterThatDrawsSuppressesNativeAndRecordsEvidence() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(adapter(true, false));
        AbstractGameEffect effect = supportedAuraEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals("ART draw supplies the instance pixels",
                Integer.valueOf(1), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals("claim consumed without a native fallback",
                Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals("native render was suppressed (no native fail-open recorded)",
                Integer.valueOf(0), NativeRenderBridge.effectLedger().probeSlice().get("failOpen"));
        assertEquals("a successful claim draw increments the ART aura draw counter",
                1, AuraArtRenderer.drawCount());
        assertFalse(NativeRenderBridge.isAuraClaimInvocation(Lookup.lastInvocationId()));
    }

    @Test
    public void readyAdapterThatDeclinesFailsOpenToNativeRender() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(adapter(false, false));
        AbstractGameEffect effect = supportedAuraEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals("declined claim records no ART pixels",
                Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals("declined claim is recorded as delegated-without-evidence",
                Integer.valueOf(1), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals("fallback records a delegated mismatch",
                Integer.valueOf(1), NativeRenderBridge.strictReport().get("dispositionMismatch"));
        assertEquals("native render was reached",
                Integer.valueOf(1), NativeRenderBridge.effectLedger().probeSlice().get("failOpen"));
        assertEquals("a declined claim draw leaves the ART aura draw counter at zero",
                0, AuraArtRenderer.drawCount());
        assertFalse(NativeRenderBridge.isAuraClaimInvocation(Lookup.lastInvocationId()));
    }

    @Test
    public void throwingAdapterFailsOpenToNativeRenderWithoutLeakingToken() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(adapter(true, true));
        AbstractGameEffect effect = supportedAuraEffect();

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals("no ART pixels on a throwing draw",
                Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        // AuraArtRenderer fails open internally, so the throw is not observed here; the native
        // render is then reached and records exactly one fail-open off-GL.
        assertEquals("native render was reached after the throw",
                Integer.valueOf(1), NativeRenderBridge.effectLedger().probeSlice().get("failOpen"));
        assertEquals("pending claim was consumed as a fallback",
                Integer.valueOf(1), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals("no invocation is left open after the fail-open",
                Integer.valueOf(0), NativeRenderBridge.strictReport().get("openInvocation"));
        assertFalse("throwing draw must not leave a pending claim token",
                NativeRenderBridge.isAuraClaimInvocation(Lookup.lastInvocationId()));
    }

    @Test
    public void renderAtPrefixCountsOnlyASuccessfulClaimDraw() {
        // Exercises the 3-arg Prefix directly (TransientEffectRenderPatches.ObserveEffectRenderAtPosition
        // .Prefix): a successful claim draw returns SpireReturn.Return(null) and records one ART draw;
        // a declined claim fails open to SpireReturn.Continue() and records none.
        AuraDelegationGate.setActive(true);

        AuraArtRenderer.setForTests(adapter(true, false));
        SpireReturn<Void> suppressed =
                TransientEffectRenderPatches.ObserveEffectRenderAtPosition.Prefix(
                        supportedAuraEffect(), null, 0f, 0f);
        assertEquals("a successful claim draw must increment the ART aura draw counter",
                1, AuraArtRenderer.drawCount());
        assertTrue("a successful claim must suppress the native render", suppressed.isPresent());
        assertEquals("a successful claim draw must record evidence",
                Integer.valueOf(1), NativeRenderBridge.probeSlice().get("evidenceCount"));

        NativeRenderBridge.resetForTests();

        AuraArtRenderer.setForTests(adapter(false, false));
        SpireReturn<Void> continued =
                TransientEffectRenderPatches.ObserveEffectRenderAtPosition.Prefix(
                        supportedAuraEffect(), null, 0f, 0f);
        assertEquals("a declined claim draw must not increment the ART aura draw counter",
                1, AuraArtRenderer.drawCount());
        assertFalse("a declined claim must fall open to the native render", continued.isPresent());
    }

    @Test
    public void renderAtPrefixGateOffNeverDrawsArt() {
        // Gate off: beginEffectRender returns a native continuation, so the Prefix continues native
        // before ever consulting the renderer and the draw counter stays untouched.
        AuraDelegationGate.setActive(false);
        AuraArtRenderer.setForTests(adapter(true, false));

        SpireReturn<Void> continued =
                TransientEffectRenderPatches.ObserveEffectRenderAtPosition.Prefix(
                        supportedAuraEffect(), null, 0f, 0f);

        assertFalse("gate off keeps native rendering", continued.isPresent());
        assertEquals("gate off must not record an ART aura draw", 0, AuraArtRenderer.drawCount());
    }

    private static AuraArtRenderer.Adapter adapter(final boolean draws, final boolean throwsOnDraw) {
        return new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                if (throwsOnDraw) throw new IllegalStateException("aura draw boom");
                return draws;
            }
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

    /** Reads the last bridged invocation id so a consumed token can be checked. */
    private static final class Lookup {
        static long lastInvocationId() {
            java.util.List<NativeRenderInvocation> invocations = NativeRenderBridge.ledger().invocations();
            return invocations.isEmpty() ? -1L
                    : invocations.get(invocations.size() - 1).invocationId;
        }
    }
}

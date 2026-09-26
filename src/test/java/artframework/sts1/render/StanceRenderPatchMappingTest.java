package artframework.sts1.render;

import artframework.sts1.PresentSafety;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.stances.NeutralStance;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the disposition-to-suppression mapping that {@code StanceRenderPatches
 * .SuppressDelegatedNativeStance.Prefix} relies on, without instantiating the patch class.
 *
 * <p>The patch suppresses the native {@code AbstractStance.render} pixels iff
 * {@link NativeRenderBridge#beginStanceRender} resolves {@code DELEGATE_TO_ART}; PASS and
 * FAIL_OPEN both keep native continuation. {@code NeutralStance}'s static initializer needs
 * game localization state, so the test uses a minimal concrete stance carrying the same
 * {@link NeutralStance#STANCE_ID} value.
 */
public class StanceRenderPatchMappingTest {

    /** Mirrors the Prefix judgment: only a delegated disposition suppresses native draw. */
    private static boolean prefixSuppressesNative(RenderDisposition d) {
        return d.mode == RenderDisposition.Mode.DELEGATE_TO_ART;
    }

    private static final class TestStance extends AbstractStance {
        TestStance(String id) {
            this.ID = id;
        }

        @Override
        public void updateDescription() {
            this.description = "";
        }
    }

    private static AbstractStance neutral() {
        return new TestStance(NeutralStance.STANCE_ID);
    }

    @After
    public void tearDown() {
        StanceDelegationGate.resetForTests();
        StanceArtRenderer.resetForTests();
        BackgroundOnlyGate.resetForTests();
        PresentSafety.resetForTests();
        NativeRenderBridge.resetForTests();
    }

    @Test
    public void gateOffPassDoesNotSuppressAndKeepsNativeContinuation() {
        RenderDisposition d = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, d.mode);
        assertFalse("PASS must fall through to native render", prefixSuppressesNative(d));
        assertTrue(d.nativeContinuation);
    }

    @Test
    public void failOpenDoesNotSuppressAndKeepsNativeContinuation() {
        StanceDelegationGate.setActive(true);

        RenderDisposition d = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, d.mode);
        assertFalse("FAIL_OPEN must fall through to native render", prefixSuppressesNative(d));
        assertTrue("fail-open keeps the native stance render", d.nativeContinuation);
    }

    @Test
    public void delegateSuppressesNativeAndDropsContinuation() {
        StanceDelegationGate.setActive(true);
        StanceArtRenderer.setReadyForTests(true);

        RenderDisposition d = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, d.mode);
        assertTrue("DELEGATE_TO_ART is the sole suppression case", prefixSuppressesNative(d));
        assertFalse("delegated stance is drawn by S2b-2, not natively", d.nativeContinuation);
    }

    @Test
    public void suppressionMappingTracksNativeContinuationExactly() {
        StanceDelegationGate.setActive(true);
        StanceArtRenderer.setReadyForTests(true);
        RenderDisposition delegated = NativeRenderBridge.beginStanceRender(neutral());

        StanceArtRenderer.setReadyForTests(false);
        RenderDisposition failedOpen = NativeRenderBridge.beginStanceRender(neutral());

        StanceDelegationGate.setActive(false);
        RenderDisposition passed = NativeRenderBridge.beginStanceRender(neutral());

        // Prefix suppresses native draw exactly when native continuation is not retained.
        assertEquals(!delegated.nativeContinuation, prefixSuppressesNative(delegated));
        assertEquals(!failedOpen.nativeContinuation, prefixSuppressesNative(failedOpen));
        assertEquals(!passed.nativeContinuation, prefixSuppressesNative(passed));
    }
}

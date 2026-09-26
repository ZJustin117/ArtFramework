package artframework.sts1.render;

import artframework.context.OrbStanceView;
import artframework.sts1.PresentSafety;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.stances.NeutralStance;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure-logic checks for the stance render bridge seam: the gate/readiness decision order and the
 * owner identity derived from {@code AbstractStance.ID}.
 *
 * <p>{@code NeutralStance}'s static initializer needs game localization state, so the test uses a
 * minimal concrete stance carrying the same {@link NeutralStance#STANCE_ID} value.
 */
public class NativeStanceBridgeTest {

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

    private static void publishDrawableStance(String ownerId) {
        OrbStanceView.Entry entry = new OrbStanceView.Entry(ownerId, "stance", ownerId, 0, 0, 0,
                true, "res/" + ownerId, artframework.component.Rect.ZERO, true,
                0f, 1f, 1f, 1f, 1f, 100f, 200f, 512f, 512f, 1f, true, 256f, 256f);
        Sts1OrbStanceProjection.publish(
                new OrbStanceView(Collections.singletonList(entry), true));
    }

    @After
    public void tearDown() {
        StanceDelegationGate.resetForTests();
        StanceArtRenderer.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
        BackgroundOnlyGate.resetForTests();
        PresentSafety.resetForTests();
        NativeRenderBridge.resetForTests();
    }

    @Test
    public void gateOffPassesThroughWithNativeContinuation() {
        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue(disposition.nativeContinuation);
        assertEquals("native_continuation", disposition.reason);
    }

    @Test
    public void gateOnWithoutReadyRendererFailsOpen() {
        StanceDelegationGate.setActive(true);

        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, disposition.mode);
        assertTrue("fail-open keeps the native stance render", disposition.nativeContinuation);
        assertEquals("stance_art_not_ready", disposition.reason);
    }

    @Test
    public void gateOnWithReadyRendererDelegatesAndRegistersToken() {
        StanceDelegationGate.setActive(true);
        publishDrawableStance("stance:" + NeutralStance.STANCE_ID);

        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse(disposition.nativeContinuation);
        assertEquals("stance_delegate", disposition.reason);

        String owner = "stance:" + NeutralStance.STANCE_ID;
        assertEquals("stance:Neutral", owner);
        assertEquals(owner, NativeRenderBridge.stanceOwner(neutral()));
        assertEquals(owner, disposition.presentationEntityId);

        Long token = NativeRenderBridge.takeStanceInvocation(owner);
        assertNotNull("delegated stance publishes a pending token under its owner", token);
        assertEquals(disposition.invocationId, token.longValue());
        assertNull("token is consumed on take", NativeRenderBridge.takeStanceInvocation(owner));
    }

    @Test
    public void emptyIdFallsBackToSimpleClassName() {
        assertEquals("stance:TestStance", NativeRenderBridge.stanceOwner(new TestStance("")));
        assertEquals("stance:TestStance", NativeRenderBridge.stanceOwner(new TestStance(null)));
    }

    @Test
    public void nullStanceUsesUnknownOwner() {
        assertEquals("stance:unknown", NativeRenderBridge.stanceOwner(null));

        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(null);

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
    }

    @Test
    public void delegatedDrawConsumesTokenWithoutOrphan() {
        StanceDelegationGate.setActive(true);
        String owner = "stance:" + NeutralStance.STANCE_ID;
        publishDrawableStance(owner);
        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        NativeRenderBridge.recordStanceDraw(disposition.invocationId, 1);

        assertNull("draw consumes the pending token", NativeRenderBridge.takeStanceInvocation(owner));
        assertEquals(Integer.valueOf(0),
                NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void delegatedFailureConsumesTokenWithoutOrphan() {
        StanceDelegationGate.setActive(true);
        String owner = "stance:" + NeutralStance.STANCE_ID;
        publishDrawableStance(owner);
        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);

        NativeRenderBridge.recordStanceFailure(disposition.invocationId);

        assertNull("failure consumes the pending token", NativeRenderBridge.takeStanceInvocation(owner));
        assertEquals(Integer.valueOf(0),
                NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void unmatchedOwnerIsNotReadySoBridgeFailsOpen() {
        StanceDelegationGate.setActive(true);
        publishDrawableStance("stance:Calm");

        RenderDisposition disposition = NativeRenderBridge.beginStanceRender(neutral());

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, disposition.mode);
        assertEquals("stance_art_not_ready", disposition.reason);
        assertTrue(disposition.nativeContinuation);
    }
}

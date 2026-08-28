package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** Slice 9: delegated evidence must correlate by both surface and invocation identity. */
@RunWith(Parameterized.class)
public class NativeRenderDelegatedEvidenceCorrelationParameterizedTest {
    private static final String NATIVE_CLASS = "native.DelegatedSurface";
    private static final String NATIVE_METHOD = "render";
    private final String surfaceId;
    private final String scene;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> surfaces() {
        return Arrays.asList(new Object[][] {
                {SurfaceIds.COMBAT_HAND, "combat"},
                {SurfaceIds.COMBAT_CONTROLS, "combat"},
                {SurfaceIds.COMBAT_ENERGY, "combat"},
                {SurfaceIds.COMBAT_INTENTS, "combat"},
                {SurfaceIds.COMBAT_PROCEED, "combat"},
                {SurfaceIds.TOP_PANEL, "combat"},
                {SurfaceIds.MAP, "map"},
                {SurfaceIds.EVENT, "event"},
                {SurfaceIds.SELECT_GRID, "select"},
                {SurfaceIds.SELECT_HAND, "select"},
                {SurfaceIds.REWARD_COMBAT, "reward"},
                {SurfaceIds.REWARD_CARD, "reward"},
                {SurfaceIds.REWARD_BOSS_RELIC, "reward"},
                {SurfaceIds.REST, "rest"},
                {SurfaceIds.SHOP, "shop"},
                {SurfaceIds.TREASURE, "treasure"}
        });
    }

    public NativeRenderDelegatedEvidenceCorrelationParameterizedTest(String surfaceId, String scene) {
        this.surfaceId = surfaceId;
        this.scene = scene;
    }

    @After
    public void tearDown() {
        resetRuntime();
    }

    @Test
    public void strictLedgerKeepsGapUntilCorrelatedEvidenceClosesCurrentInvocation() {
        resetRuntime();
        publishFrame();
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, NATIVE_CLASS, NATIVE_METHOD, "correlation-test");
        assertEquals(surfaceId + " must delegate", RenderDisposition.Mode.DELEGATE_TO_ART,
                disposition.mode);
        assertFalse(surfaceId + " must suppress native continuation", disposition.nativeContinuation);

        long wrongInvocationId = disposition.invocationId + 1000L;
        NativeRenderBridge.recordSurfaceDraw(surfaceId, wrongInvocationId, 1);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_HAND.equals(surfaceId)
                ? SurfaceIds.MAP : SurfaceIds.COMBAT_HAND, disposition.invocationId, 1);
        assertNull(surfaceId + " must reject mismatched evidence",
                NativeRenderBridge.ledger().evidence(disposition.invocationId));
        assertEquals(surfaceId + " mismatched evidence must not close invocation", true,
                NativeRenderBridge.ledger().isOpen(disposition.invocationId));
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));

        NativeRenderBridge.recordSurfaceDraw(surfaceId, disposition.invocationId, 1);

        PresentationDrawEvidence evidence = NativeRenderBridge.ledger()
                .evidence(disposition.invocationId);
        assertNotNull(surfaceId + " must create correlated evidence", evidence);
        assertEquals(disposition.invocationId, evidence.invocationId);
        assertEquals(surfaceId, NativeRenderBridge.ledger()
                .invocation(evidence.invocationId).ownerId);
        assertEquals(NativeRenderBridge.ledger().invocation(disposition.invocationId).frameId,
                evidence.frameId);
        assertEquals(disposition.presentationEntityId, evidence.entityId);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(surfaceId + " correlated evidence must close invocation", false,
                NativeRenderBridge.ledger().isOpen(disposition.invocationId));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    private void publishFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(41L, 7L, scene, Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private static void resetRuntime() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }
}

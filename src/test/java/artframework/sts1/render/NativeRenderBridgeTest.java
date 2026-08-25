package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeRenderBridgeTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
    }

    private void mountedCombat() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
    }

    @Test
    public void offAndObserveContinueNativeWithDifferentEvidenceModes() {
        mountedCombat();

        RenderDisposition off = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "p");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, off.mode);
        assertTrue(off.nativeContinuation);

        FullPresentMode.setCombatHandLevel(PresentLevel.OBSERVE);
        RenderDisposition observe = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "p");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, observe.mode);
        assertTrue(observe.nativeContinuation);
    }

    @Test
    public void fullReadyDelegatesWithoutNativeContinuation() {
        mountedCombat();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "p");

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse(disposition.nativeContinuation);
        assertTrue(disposition.presentationEntityId.length() > 0);
        assertTrue(Sts1NativePresentationAdapter.hasEntity(SurfaceIds.COMBAT_HAND));
        assertEquals(Integer.valueOf(1), Integer.valueOf(NativeRenderBridge.ledger().invocationCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(NativeRenderBridge.ledger().dispositionCount()));
    }

    @Test
    public void delegatedEvidenceClosesTheEvidenceGap() {
        mountedCombat();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_HAND, "Player", "renderHand", "p");

        Map<String, Object> before = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(1), before.get("delegatedWithoutEvidence"));

        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_HAND, 2);
        Map<String, Object> after = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(0), after.get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(1), after.get("evidenceCount"));
        assertEquals(Integer.valueOf(0), after.get("orphanArtOutput"));
    }

    @Test(expected = IllegalStateException.class)
    public void duplicateDispositionIsRejected() {
        mountedCombat();
        RenderDisposition d = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "p");
        NativeRenderBridge.ledger().recordDisposition(d);
    }

    @Test
    public void secondWaveSurfaceOwnersResolveToTheSameContract() {
        mountedCombat();
        String[] owners = new String[] {
            SurfaceIds.MAP,
            SurfaceIds.EVENT,
            SurfaceIds.SELECT_GRID,
            SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT,
            SurfaceIds.REST,
            SurfaceIds.SHOP,
            SurfaceIds.TREASURE
        };
        for (String owner : owners) {
            RenderDisposition disposition = NativeRenderBridge.beginSurface(
                    owner, "native.Owner", "render", "owner");
            assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
            assertTrue(disposition.nativeContinuation);
        }
        assertEquals(Integer.valueOf(owners.length),
                Integer.valueOf(NativeRenderBridge.ledger().invocationCount()));
    }

    @Test
    public void secondWaveObserveCapturesNative() {
        mountedCombat();
        FullPresentMode.setLevel(SurfaceIds.MAP, PresentLevel.OBSERVE);
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.MAP, "native.Map", "render", "map");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue(disposition.nativeContinuation);
    }

    @Test
    public void strictReportExposesUnknownAndOrphanOutput() {
        mountedCombat();
        NativeRenderBridge.beginSurface("sts1.unknown", "native.Unknown", "render", "unknown");
        NativeRenderBridge.recordSurfaceDraw("sts1.unknown", 1);

        Map<String, Object> report = NativeRenderBridge.strictReport();
        assertEquals(Integer.valueOf(1), report.get("runtimeUNKNOWN"));
        assertEquals(Integer.valueOf(1), report.get("orphanArtOutput"));
        assertEquals(Integer.valueOf(0), report.get("runtimeUNDECIDED"));
    }

    @Test
    public void controlsEnergyIntentsPassThroughInFullModeWithoutSuppressingNative() {
        mountedCombat();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_INTENTS).mount();

        RenderDisposition controls = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_CONTROLS, "com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render", "c");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, controls.mode);
        assertTrue(controls.nativeContinuation);

        RenderDisposition energy = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_ENERGY, "com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render", "e");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, energy.mode);
        assertTrue(energy.nativeContinuation);

        RenderDisposition intents = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent", "i");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, intents.mode);
        assertTrue(intents.nativeContinuation);
    }

    @Test
    public void mapEventSelectRoomSurfacesPassThroughInFullModeWithoutSuppressingNative() {
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        assertFullSurfacePassThrough(SurfaceIds.MAP, "map");
        assertFullSurfacePassThrough(SurfaceIds.EVENT, "event");
        assertFullSurfacePassThrough(SurfaceIds.SELECT_GRID, "select");
        assertFullSurfacePassThrough(SurfaceIds.SELECT_HAND, "select");
        assertFullSurfacePassThrough(SurfaceIds.REWARD_COMBAT, "reward");
        assertFullSurfacePassThrough(SurfaceIds.REWARD_CARD, "reward");
        assertFullSurfacePassThrough(SurfaceIds.REWARD_BOSS_RELIC, "reward");
        assertFullSurfacePassThrough(SurfaceIds.REST, "rest");
        assertFullSurfacePassThrough(SurfaceIds.SHOP, "shop");
        assertFullSurfacePassThrough(SurfaceIds.TREASURE, "treasure");
    }

    private void assertFullSurfacePassThrough(String surfaceId, String scene) {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L, 1L, scene, null, ControlsView.empty(), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        ArtFramework.component(surfaceId).mount();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, "native.Owner", "render", "owner");
        assertEquals("NRO-03 surface " + surfaceId + " must pass through to native renderer",
                RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue("NRO-03 surface " + surfaceId + " must allow native continuation",
                disposition.nativeContinuation);
    }

    @Test
    public void transientEffectObservationKeepsNativeContinuationPerInstance() {
        TransientEffectIdentity identity = new TransientEffectIdentity(
                "effect:1", "native.Effect", 1, 0L);
        TransientEffectLedger ledger = NativeRenderBridge.effectLedger();
        ledger.create(identity);
        ledger.render(identity);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));

        ledger.update(identity, true);
        assertEquals(Integer.valueOf(0),
                Integer.valueOf(ledger.activeCount()));
        ledger.dispose(identity);
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
    }

    @Test
    public void transientEffectRenderAlwaysCapturesAndPasses() {
        AbstractGameEffect effect = new AbstractGameEffect() {
            @Override
            public void render(SpriteBatch sb) {
            }

            @Override
            public void dispose() {
            }
        };
        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render_at");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue("native effect queue must continue after observation",
                disposition.nativeContinuation);
    }
}

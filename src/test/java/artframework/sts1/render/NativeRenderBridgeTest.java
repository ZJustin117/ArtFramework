package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.EventOptionView;
import artframework.context.EventView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapView;
import artframework.context.SelectView;
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

    @Test
    public void ownerDrawDrainsEveryPendingInvocationForThatSurface() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        RenderDisposition first = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m1");
        RenderDisposition second = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m2");
        RenderDisposition third = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m3");

        assertPendingSurfaceInvocations(3, SurfaceIds.COMBAT_INTENTS, 3);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_INTENTS, 4);

        assertEquals(4, NativeRenderBridge.ledger().evidence(first.invocationId).drawCount);
        assertEquals(4, NativeRenderBridge.ledger().evidence(second.invocationId).drawCount);
        assertEquals(4, NativeRenderBridge.ledger().evidence(third.invocationId).drawCount);
        assertPendingSurfaceInvocations(0, SurfaceIds.COMBAT_INTENTS, 0);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("openInvocation"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(Boolean.TRUE, NativeRenderBridge.strictReport().get("accepted"));
    }

    @Test
    public void ownerDrawDoesNotConsumeAnotherOwnersPendingInvocations() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        RenderDisposition hand = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "hand");
        RenderDisposition firstIntent = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m1");
        RenderDisposition secondIntent = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m2");

        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_HAND, 2);

        assertEquals(2, NativeRenderBridge.ledger().evidence(hand.invocationId).drawCount);
        assertEquals(null, NativeRenderBridge.ledger().evidence(firstIntent.invocationId));
        assertEquals(null, NativeRenderBridge.ledger().evidence(secondIntent.invocationId));
        assertPendingSurfaceInvocations(2, SurfaceIds.COMBAT_HAND, 0);
        assertPendingSurfaceInvocations(2, SurfaceIds.COMBAT_INTENTS, 2);

        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_INTENTS, 3);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(Boolean.TRUE, NativeRenderBridge.strictReport().get("accepted"));
    }

    @Test
    public void threeSurfaceOwnersAreRetiredWhenNativeReturnsAfterFullIsOff() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        armFullSurface(SurfaceIds.COMBAT_CONTROLS);
        armFullSurface(SurfaceIds.COMBAT_ENERGY);
        beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "hand");
        beginDelegatedSurface(SurfaceIds.COMBAT_CONTROLS, "controls");
        beginDelegatedSurface(SurfaceIds.COMBAT_ENERGY, "energy");

        FullPresentMode.setCombatHandLevel(PresentLevel.OFF);
        FullPresentMode.setCombatControlsLevel(PresentLevel.OFF);
        FullPresentMode.setEnergyLevel(PresentLevel.OFF);
        NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_HAND, "native.Hand", "render", "off");
        NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_CONTROLS, "native.Controls", "render", "off");
        NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_ENERGY, "native.Energy", "render", "off");

        Map<String, Object> report = NativeRenderBridge.strictReport();
        assertEquals(Integer.valueOf(0), report.get("openInvocation"));
        assertEquals(Integer.valueOf(0), report.get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), report.get("orphanArtOutput"));
        assertEquals(Boolean.TRUE, report.get("accepted"));
    }

    @Test
    public void recoveryClosesPendingDelegatedSurfaceWithoutStrictMismatch() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_CONTROLS);
        RenderDisposition pending = beginDelegatedSurface(SurfaceIds.COMBAT_CONTROLS, "old");

        NativeRenderBridge.clearTransientEffectsForRecovery();

        assertTrue(NativeRenderBridge.ledger().isOpen(pending.invocationId) == false);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("openInvocation"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "dispositionMismatch"));
        assertEquals(Boolean.TRUE, NativeRenderBridge.strictReport().get("accepted"));
    }

    @Test
    public void explicitDrawApisRemoveOnlyTheirTokensBeforeOwnerDrawClosesTheRest() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        RenderDisposition directExplicit = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m1");
        RenderDisposition ownerExplicit = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m2");
        RenderDisposition third = beginDelegatedSurface(SurfaceIds.COMBAT_INTENTS, "m3");

        NativeRenderBridge.recordSurfaceDraw(
                SurfaceIds.COMBAT_INTENTS, ownerExplicit.invocationId, 7);

        assertEquals(7, NativeRenderBridge.ledger().evidence(ownerExplicit.invocationId).drawCount);
        assertPendingSurfaceInvocations(2, SurfaceIds.COMBAT_INTENTS, 2);
        NativeRenderBridge.recordSurfaceDraw(directExplicit.invocationId, 6);

        assertEquals(6, NativeRenderBridge.ledger().evidence(directExplicit.invocationId).drawCount);
        assertPendingSurfaceInvocations(1, SurfaceIds.COMBAT_INTENTS, 1);
        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_INTENTS, 5);

        assertEquals(5, NativeRenderBridge.ledger().evidence(third.invocationId).drawCount);
        assertEquals(Integer.valueOf(3), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
        assertEquals(Boolean.TRUE, NativeRenderBridge.strictReport().get("accepted"));
    }

    @Test
    public void duplicateExplicitEvidenceIsRejectedAndEmptyOwnerDrawIsOrphaned() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        RenderDisposition disposition = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "hand");
        NativeRenderBridge.recordSurfaceDraw(disposition.invocationId, 1);
        assertPendingSurfaceInvocations(0, SurfaceIds.COMBAT_HAND, 0);

        try {
            NativeRenderBridge.recordSurfaceDraw(disposition.invocationId, 1);
            throw new AssertionError("duplicate evidence must be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("duplicate evidence"));
        }
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));

        NativeRenderBridge.recordSurfaceDraw(SurfaceIds.COMBAT_HAND, 1);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("orphanArtOutput"));
        assertEquals(Boolean.FALSE, NativeRenderBridge.strictReport().get("accepted"));
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
    public void targetingSurfaceObserveCapturesAndPasses() {
        mountedCombat();
        ArtFramework.component(SurfaceIds.COMBAT_TARGETING).mount();
        FullPresentMode.setTargetingLevel(PresentLevel.OBSERVE);
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_TARGETING,
                "com.megacrit.cardcrawl.characters.AbstractPlayer",
                "renderTargetingUi",
                "p");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue("native targeting pixels must continue", disposition.nativeContinuation);
    }

    @Test
    public void targetingSurfaceOffPassesThrough() {
        mountedCombat();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_TARGETING,
                "com.megacrit.cardcrawl.characters.AbstractPlayer",
                "renderTargetingUi",
                "p");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue("native targeting pixels must continue when OFF", disposition.nativeContinuation);
    }

    @Test
    public void abstractCardRenderIsNotABridgeSuppressionSurface() {
        mountedCombat();
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                "sts1.inventory.com.megacrit.cardcrawl.cards.abstractcard.render",
                "com.megacrit.cardcrawl.cards.AbstractCard", "render", "card");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, disposition.mode);
        assertTrue("AbstractCard.render pixels must continue natively", disposition.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
    }

    @Test
    public void targetingFullStillPassesWithoutDelegatedEvidence() {
        mountedCombat();
        FullPresentMode.setTargetingLevel(PresentLevel.FULL);
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_TARGETING,
                "com.megacrit.cardcrawl.ui.panels.PotionPopUp",
                "renderTargetingUi", "p");

        assertTrue(disposition.mode == RenderDisposition.Mode.CAPTURE_AND_PASS
                || disposition.mode == RenderDisposition.Mode.PASS_THROUGH);
        assertTrue(disposition.nativeContinuation);
        assertEquals(0, NativeRenderBridge.ledger().evidenceCount());
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void targetingUnknownOwnerAndRecoveryDoNotCreateDelegatedStrictGap() {
        mountedCombat();
        NativeRenderBridge.beginSurface("sts1.unknown", "unknown.Owner", "render", "u");
        RenderDisposition targeting = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_TARGETING, "native.Targeting", "renderTargetingUi", "t");

        assertTrue(targeting.nativeContinuation);
        assertEquals(Integer.valueOf(1), NativeRenderBridge.strictReport().get("runtimeUNKNOWN"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        NativeRenderBridge.clearTransientEffectsForRecovery();
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("orphanArtOutput"));
    }

    @Test
    public void controlsEnergyIntentsTopPanelProceedDelegateInFullMode() {
        mountedCombat();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setCombatControlsLevel(PresentLevel.FULL);
        FullPresentMode.setEnergyLevel(PresentLevel.FULL);
        FullPresentMode.setIntentsLevel(PresentLevel.FULL);
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        FullPresentMode.setProceedLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_INTENTS).mount();
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
        ArtFramework.component(SurfaceIds.COMBAT_PROCEED).mount();

        RenderDisposition controls = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_CONTROLS, "com.megacrit.cardcrawl.ui.buttons.EndTurnButton", "render", "c");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, controls.mode);
        assertFalse(controls.nativeContinuation);

        RenderDisposition energy = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_ENERGY, "com.megacrit.cardcrawl.ui.panels.EnergyPanel", "render", "e");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, energy.mode);
        assertFalse(energy.nativeContinuation);

        RenderDisposition intents = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "com.megacrit.cardcrawl.monsters.AbstractMonster", "renderIntent", "i");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, intents.mode);
        assertFalse(intents.nativeContinuation);

        RenderDisposition topPanel = NativeRenderBridge.beginSurface(
                SurfaceIds.TOP_PANEL, "com.megacrit.cardcrawl.ui.panels.TopPanel", "render", "t");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, topPanel.mode);
        assertFalse(topPanel.nativeContinuation);

        RenderDisposition proceed = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_PROCEED, "com.megacrit.cardcrawl.ui.buttons.ProceedButton", "render", "p");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, proceed.mode);
        assertFalse(proceed.nativeContinuation);
    }

    @Test
    public void mapAndRoomSurfacesDelegateInFullMode() {
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        assertFullSurfaceDelegates(SurfaceIds.MAP, "map");
        assertFullSurfaceDelegates(SurfaceIds.EVENT, "event");
        assertFullSurfaceDelegates(SurfaceIds.SELECT_GRID, "select");
        assertFullSurfaceDelegates(SurfaceIds.SELECT_HAND, "select");
        assertFullSurfaceDelegates(SurfaceIds.REWARD_COMBAT, "reward");
        assertFullSurfaceDelegates(SurfaceIds.REWARD_CARD, "reward");
        assertFullSurfaceDelegates(SurfaceIds.REWARD_BOSS_RELIC, "reward");
        // Slice C phase 2: rest/shop/treasure now paint minimal chrome, so delegation is safe.
        assertFullSurfaceDelegates(SurfaceIds.REST, "rest");
        assertFullSurfaceDelegates(SurfaceIds.SHOP, "shop");
        assertFullSurfaceDelegates(SurfaceIds.TREASURE, "treasure");
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

    private void assertFullSurfaceDelegates(String surfaceId, String scene) {
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
        assertEquals("Phase 3 surface " + surfaceId + " must delegate to ART",
                RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse("Phase 3 surface " + surfaceId + " must suppress native continuation",
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

    @Test
    public void surfaceDrawCountMatchesProjectionContent() {
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        assertSurfaceDrawCountMatchesContent(
                SurfaceIds.EVENT,
                PresentLevel.FULL,
                () -> EventDrawPath.buildFromProjection().size());

        assertSurfaceDrawCountMatchesContent(
                SurfaceIds.COMBAT_CONTROLS,
                PresentLevel.FULL,
                () -> ControlsDrawPath.buildFromProjection().size());
    }

    private void assertSurfaceDrawCountMatchesContent(
            String surfaceId,
            PresentLevel level,
            java.util.function.IntSupplier countSupplier) {
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.setLevel(surfaceId, level);
        if (SurfaceIds.EVENT.equals(surfaceId)) {
            publishEventFrame();
            ArtFramework.component(surfaceId).action("mount_event");
        } else if (SurfaceIds.COMBAT_CONTROLS.equals(surfaceId)) {
            publishCombatFrame();
            ArtFramework.ops().invoke(SurfaceIds.COMBAT_SURFACE, "mount_combat");
        } else {
            throw new IllegalArgumentException("unsupported surface: " + surfaceId);
        }
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, "native.Owner", "render", "owner");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        int expected = countSupplier.getAsInt();
        NativeRenderBridge.recordSurfaceDraw(surfaceId, expected);
        Map<String, Object> report = NativeRenderBridge.probeSlice();
        assertEquals("evidence count for " + surfaceId,
                Integer.valueOf(1), report.get("evidenceCount"));
        assertEquals(Integer.valueOf(0), report.get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), report.get("orphanArtOutput"));
    }

    private void armFullSurface(String surfaceId) {
        FullPresentMode.setLevel(surfaceId, PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        ArtFramework.component(surfaceId).mount();
    }

    private RenderDisposition beginDelegatedSurface(String surfaceId, String sourceIdentity) {
        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                surfaceId, "native.Owner", "render", sourceIdentity);
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        return disposition;
    }

    @SuppressWarnings("unchecked")
    private void assertPendingSurfaceInvocations(int total, String ownerId, int ownerCount) {
        Map<String, Object> probe = NativeRenderBridge.probeSlice();
        assertEquals(Integer.valueOf(total), probe.get("pendingSurfaceInvocationCount"));
        Map<String, Integer> byOwner =
                (Map<String, Integer>) probe.get("pendingSurfaceInvocationsByOwner");
        assertEquals(Integer.valueOf(ownerCount),
                Integer.valueOf(byOwner.containsKey(ownerId) ? byOwner.get(ownerId).intValue() : 0));
    }

    private void publishEventFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(
                ContextFrame.of(
                        1L,
                        1L,
                        "event",
                        null,
                        ControlsView.empty(),
                        MapView.empty(),
                        EventView.of(
                                "Test",
                                Arrays.asList(
                                        EventOptionView.of(0, "A", true),
                                        EventOptionView.of(1, "B", true))),
                        SelectView.empty(),
                        null));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private void publishCombatFrame() {
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(1L, 1L, "combat", Arrays.asList(),
                ControlsView.combat(3, 1, 0, 0, 0, true, true), MapView.empty(), null));
        ArtFramework.publishFrame(backend.currentFrame());
    }
}

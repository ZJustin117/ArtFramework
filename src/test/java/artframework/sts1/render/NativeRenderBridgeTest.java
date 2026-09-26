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
import artframework.sts1.PresentSafety;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import artframework.sts1.patch.TransientEffectContainerPatches;
import artframework.render.NativeRenderInputComponent;
import artframework.render.NativeRenderOwnership;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NativeRenderBridgeTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        PresentSafety.resetForTests();
        CombatInputRouter.resetForTests();
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetForTests();
        Sts1VerifyDiagnostics.resetForTests();
        NativeRenderBridge.resetForTests();
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
    public void backgroundOnlyBlocksKnownSurfaceAndKeepsUnknownFailOpen() {
        mountedCombat();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        NativeRenderBridge.policy().setIsolate(false);
        Sts1VerifyDiagnostics.setBackgroundOnly(true);

        RenderDisposition blocked = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "strict");
        assertEquals(RenderDisposition.Mode.BLOCKED, blocked.mode);
        assertFalse(blocked.nativeContinuation);

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown.surface", "Unknown", "render", "strict-unknown");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);
        Map<String, Object> strict = Sts1VerifyDiagnostics.probeSlice();
        Map<?, ?> only = (Map<?, ?>) strict.get("backgroundOnly");
        assertTrue(((Number) only.get("blockedForeground")).longValue() >= 1L);
        assertTrue(((Number) only.get("unsupported")).longValue() >= 1L);
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
    public void isolateSurfaceSuppressesKnownFullInvocationAndAllowPanicRecoveryStayFailOpen() {
        mountedCombat();
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        NativeRenderBridge.policy().setIsolate(true);
        RenderDisposition isolated = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "isolate");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, isolated.mode);
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "surface:" + SurfaceIds.COMBAT_HAND));
        RenderDisposition allowed = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "allow");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, allowed.mode);
        assertTrue(allowed.nativeContinuation);
        PresentSafety.panic("surface-isolate");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "panic").mode);
        PresentSafety.clearPanic();
        NativeRenderBridge.clearTransientEffectsForRecovery();
        FullPresentMode.setCombatHandLevel(PresentLevel.OFF);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "recovery").mode);
    }

    @Test
    public void isolateKeepsUnknownAndUnmountedOwnersFailOpen() {
        mountedCombat();
        NativeRenderBridge.policy().setIsolate(true);

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown.surface", "UnknownOwner", "render", "unknown");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);

        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        RenderDisposition unmounted = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "unmounted");
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, unmounted.mode);
        assertTrue(unmounted.nativeContinuation);
    }

    @Test
    public void isolateAllowsMultipleMountedFamiliesAndClearRestoresDenyByDefault() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        armFullSurface(SurfaceIds.COMBAT_ENERGY);
        armFullSurface(SurfaceIds.TOP_PANEL);
        NativeRenderBridge.policy().setIsolate(true);

        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "family:" + SurfaceIds.COMBAT_HAND));
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "family:" + SurfaceIds.COMBAT_ENERGY));
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "family:" + SurfaceIds.TOP_PANEL));

        assertEquals(RenderDisposition.Mode.PASS_THROUGH,
                NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_HAND, "Player", "renderHand", "hand").mode);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH,
                NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_ENERGY, "EnergyPanel", "render", "energy").mode);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH,
                NativeRenderBridge.beginSurface(SurfaceIds.TOP_PANEL, "TopPanel", "render", "top").mode);
        assertEquals(3, NativeRenderBridge.policy().exemptionTargets().size());

        NativeRenderBridge.policy().clear();
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART,
                NativeRenderBridge.beginSurface(SurfaceIds.COMBAT_HAND, "Player", "renderHand", "deny").mode);
    }

    @Test
    public void filterScopeDowngradeWinsOverIsolateForFilteredFamily() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.policy().setIsolate(true);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "isolate-filter");

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue(disposition.nativeContinuation);
        assertTrue(disposition.reason.startsWith("filter_scope:"));
    }

    @Test
    public void exemptBeforeFirstDelegatedFrameStillProjectsExemptionEntity() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.policy().setIsolate(true);
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "family:" + SurfaceIds.COMBAT_HAND));

        RenderDisposition exempt = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "exempt-first");

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, exempt.mode);
        assertTrue(exempt.nativeContinuation);
        assertTrue(Sts1NativePresentationAdapter.hasEntity(SurfaceIds.COMBAT_HAND));
        NativeRenderExemptionComponent projection = artframework.presentation.PresentationRegistry
                .context("nrcc-native").world().get(
                        Sts1NativePresentationAdapter.entity(SurfaceIds.COMBAT_HAND),
                        NativeRenderExemptionComponent.class);
        assertTrue(projection != null && projection.exempt);
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
    public void policyRevisionRefreshesProjectedExemptionComponent() {
        NativeRenderInvocation invocation = new NativeRenderInvocation(99L, 1L, "combat",
                "refresh-owner", "com.example.Native", "render", "combat.hand", "source",
                artframework.component.Rect.ZERO);
        Sts1NativePresentationAdapter.present(invocation);
        artframework.ecs.EntityId entity = Sts1NativePresentationAdapter.entity("refresh-owner");
        NativeRenderExemptionComponent before = artframework.presentation.PresentationRegistry
                .context("nrcc-native").world().get(entity, NativeRenderExemptionComponent.class);
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse("family:combat.hand"));
        NativeRenderExemptionComponent after = artframework.presentation.PresentationRegistry
                .context("nrcc-native").world().get(entity, NativeRenderExemptionComponent.class);
        assertFalse(before.exempt);
        assertTrue(after.exempt);
        assertTrue(after.policyRevision > before.policyRevision);
    }

    @Test
    public void verifyResetReprojectsExistingNativeEntitiesWithoutStaleExemption() {
        NativeRenderInvocation invocation = new NativeRenderInvocation(100L, 1L, "combat",
                "reset-owner", "com.example.Native", "render", "combat.hand", "source",
                artframework.component.Rect.ZERO);
        Sts1NativePresentationAdapter.present(invocation);
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse("family:combat.hand"));
        NativeRenderExemptionComponent allowed = artframework.presentation.PresentationRegistry
                .context("nrcc-native").world().get(Sts1NativePresentationAdapter.entity("reset-owner"),
                        NativeRenderExemptionComponent.class);
        assertTrue(allowed.exempt);

        Sts1VerifyDiagnostics.resetForTests();

        NativeRenderExemptionComponent reset = artframework.presentation.PresentationRegistry
                .context("nrcc-native").world().get(Sts1NativePresentationAdapter.entity("reset-owner"),
                        NativeRenderExemptionComponent.class);
        assertFalse(reset.exempt);
        assertEquals(Long.valueOf(0L), Long.valueOf(reset.policyRevision));
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
    public void recoveryClearsOnlyTransientNativePresentationEntities() {
        Sts1NativePresentationAdapter.present(new NativeRenderInvocation(1L, 1L, "combat",
                "nrcc-native-surface", "NativeSurface", "render", "surface", "surface",
                artframework.component.Rect.ZERO));
        for (int index = 0; index < TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1; index++) {
            Sts1NativePresentationAdapter.present(new NativeRenderInvocation(2L, 1L, "combat",
                    "effect:bridge-recovery-" + index, "NativeEffect", "render",
                    "transient_effect", "effect", artframework.component.Rect.ZERO));
        }

        NativeRenderBridge.clearTransientEffectsForRecovery();

        assertTrue(Sts1NativePresentationAdapter.hasEntity("nrcc-native-surface"));
        assertEquals(Integer.valueOf(1), Integer.valueOf(
                artframework.presentation.PresentationRegistry.context("nrcc-native")
                        .entities().size()));
        for (int index = 0; index < TransientEffectRegistry.DEFAULT_PENDING_CAPACITY + 1; index++) {
            assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:bridge-recovery-" + index));
        }
    }

    @Test
    public void recoveryBetweenDispositionAndSurfaceTokenPublicationCannotLeaveToken() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_CONTROLS);
        final CountDownLatch publicationWindow = new CountDownLatch(1);
        NativeRenderBridge.setBeforeTokenPublicationForTests(new Runnable() {
            @Override public void run() {
                publicationWindow.countDown();
                NativeRenderBridge.clearTransientEffectsForRecovery();
            }
        });

        RenderDisposition disposition = beginDelegatedSurface(SurfaceIds.COMBAT_CONTROLS, "race");

        assertEquals(Long.valueOf(0L), Long.valueOf(publicationWindow.getCount()));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get(
                "pendingSurfaceInvocationCount"));
        NativeRenderBridge.recordSurfaceDraw(disposition.invocationId, 1);
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("pendingSurfaceInvocationCount"));
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

    @Test
    public void directSkeletonFailureIsOrphanSafeAcrossTerminalAndEvictedStates() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);

        RenderDisposition evidenced = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "evidence");
        NativeRenderBridge.recordSurfaceDraw(evidenced.invocationId, 1);
        RenderDisposition recovered = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "recovery");
        NativeRenderBridge.clearTransientEffectsForRecovery();
        RenderDisposition fallback = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "fallback");
        NativeRenderBridge.recordSkeletonFailure(fallback.invocationId);
        FullPresentMode.setCombatHandLevel(PresentLevel.OFF);
        RenderDisposition nonDelegated = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "native.Owner", "render", "non-delegated");
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);

        int evidenceCount = NativeRenderBridge.ledger().evidenceCount();
        NativeRenderBridge.recordSkeletonFailure(evidenced.invocationId);
        NativeRenderBridge.recordSkeletonFailure(recovered.invocationId);
        NativeRenderBridge.recordSkeletonFailure(fallback.invocationId);
        NativeRenderBridge.recordSkeletonFailure(nonDelegated.invocationId);

        for (int i = 0; i < NativeRenderLedger.RECENT_HISTORY_CAPACITY + 2; i++) {
            RenderDisposition extra = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "evict-" + i);
            NativeRenderBridge.recordSurfaceDraw(extra.invocationId, 1);
        }
        NativeRenderBridge.recordSkeletonFailure(evidenced.invocationId);

        assertEquals(evidenceCount + NativeRenderLedger.RECENT_HISTORY_CAPACITY + 2,
                NativeRenderBridge.ledger().evidenceCount());
        assertEquals(Integer.valueOf(5), NativeRenderBridge.strictReport().get("orphanArtOutput"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("openInvocation"));
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
    public void projectingDispositionsRecordOwnershipOnSurfaceInput() {
        mountedCombat();

        // DELEGATE_TO_ART (FULL + mounted + executor): ART owns the pixels.
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "full").mode);
        assertEquals(NativeRenderOwnership.DELEGATED_TO_ART,
                surfaceInput(SurfaceIds.COMBAT_HAND).ownership());
    }

    @Test
    public void observeAndOffSurfaceCallsCreateNoNativeInputOrRetainedTarget() {
        mountedCombat();

        // OFF (pass) and OBSERVE (capture) keep native pixels without writing ECS input. The
        // callback runs outside PresentationSchedule.advance, so projecting here would force a
        // synchronous full RenderPlan rebuild on every observed surface callback.
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "off").mode);
        assertEquals(null, Sts1NativePresentationAdapter.entity(SurfaceIds.COMBAT_HAND));
        assertNull("OFF must not create a native input", inputFor(SurfaceIds.COMBAT_HAND));

        FullPresentMode.setCombatHandLevel(PresentLevel.OBSERVE);
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "observe").mode);
        assertEquals(null, Sts1NativePresentationAdapter.entity(SurfaceIds.COMBAT_HAND));
        assertNull("OBSERVE must not create a native input", inputFor(SurfaceIds.COMBAT_HAND));

        // No entity means no input and therefore no native retained target for the surface owner;
        // an entity with no input would otherwise fall back to a presentation-frame retained entry.
        artframework.render.RenderPlan plan =
                artframework.render.RenderPlan.fromEcs(java.util.Collections.<String>emptySet());
        for (artframework.render.RenderPlan.Entry entry : plan.entries()) {
            assertFalse("OFF/OBSERVE must not trigger a retained target for the surface owner",
                    entry.id.startsWith("native:"));
        }
    }

    @Test
    public void exemptSurfaceKeepsOverlayOwnershipInput() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.policy().setIsolate(true);
        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "surface:" + SurfaceIds.COMBAT_HAND));

        RenderDisposition exempt = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "Player", "renderHand", "exempt");

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, exempt.mode);
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                surfaceInput(SurfaceIds.COMBAT_HAND).ownership());
    }

    @Test
    public void filteredSurfaceKeepsOverlayOwnershipInput() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);

        RenderDisposition filtered = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "filtered");

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, filtered.mode);
        assertEquals(NativeRenderOwnership.NATIVE_WITH_ART_OVERLAY,
                surfaceInput(SurfaceIds.COMBAT_INTENTS).ownership());
    }

    @Test
    public void nonProjectingDispositionsNeverFabricateSurfaceInput() {
        mountedCombat();

        // FAIL_OPEN: unknown owner never claims pixels and never fabricates an input.
        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown.surface", "Unknown", "render", "unknown");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertEquals(null, Sts1NativePresentationAdapter.entity("sts1.unknown.surface"));

        // Panic fail-open removes any surface input the recovery cleanup found.
        armFullSurface(SurfaceIds.COMBAT_HAND);
        beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "owned");
        assertEquals(NativeRenderOwnership.DELEGATED_TO_ART, surfaceInput(SurfaceIds.COMBAT_HAND).ownership());
        PresentSafety.panic("ownership-reconcile");
        assertNull("panic recovery must withdraw the surface input",
                inputFor(SurfaceIds.COMBAT_HAND));
        PresentSafety.clearPanic();
    }

    @Test
    public void recoveryClearsSurfaceInputWithoutDestroyingOwnerOrEffectInput() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "surface-owned");
        assertEquals(NativeRenderOwnership.DELEGATED_TO_ART,
                surfaceInput(SurfaceIds.COMBAT_HAND).ownership());

        TransientEffectIdentity identity = new TransientEffectIdentity("recovery-effect",
                "com.megacrit.cardcrawl.vfx.AbstractGameEffect", 1, 0L);
        NativeRenderBridge.effectRegistry().present(identity, 1L, "render");
        new TransientEffectProjectionSystem(NativeRenderBridge.effectRegistry()).drain();
        assertNotNull(inputFor("effect:recovery-effect"));

        NativeRenderBridge.clearTransientEffectsForRecovery();

        assertNull("recovery withdraws surface-owned native input",
                inputFor(SurfaceIds.COMBAT_HAND));
        assertTrue("recovery keeps the surface entity (presentation state)",
                Sts1NativePresentationAdapter.hasEntity(SurfaceIds.COMBAT_HAND));

        // Effect behavior is unchanged by the surface-input cleanup.
        TransientEffectIdentity effectAgain = new TransientEffectIdentity("recovery-effect-2",
                "com.megacrit.cardcrawl.vfx.AbstractGameEffect", 2, 0L);
        NativeRenderBridge.effectRegistry().present(effectAgain, 1L, "render");
        new TransientEffectProjectionSystem(NativeRenderBridge.effectRegistry()).drain();
        NativeRenderInputComponent effectInput = inputFor("effect:recovery-effect-2");
        assertNotNull(effectInput);
        assertEquals(NativeRenderOwnership.OBSERVED, effectInput.ownership());
        Sts1NativePresentationAdapter.clearTransientEffects();
        assertFalse(Sts1NativePresentationAdapter.hasEntity("effect:recovery-effect-2"));
    }

    @Test
    public void transientEffectInputStaysObserved() {
        AbstractGameEffect effect = effect();
        RenderDisposition captured = NativeRenderBridge.beginEffectRender(effect, "render");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, captured.mode);

        // Project one observed effect instance through the same registry the bridge drives.
        TransientEffectIdentity identity = new TransientEffectIdentity("bridge-observed",
                effect.getClass().getName(), System.identityHashCode(effect), 0L);
        NativeRenderBridge.effectRegistry().present(identity, 1L, "render");
        new TransientEffectProjectionSystem(NativeRenderBridge.effectRegistry()).drain();

        NativeRenderInputComponent input = inputFor("effect:bridge-observed");
        assertNotNull("observed effect owner must carry an input", input);
        assertEquals(NativeRenderOwnership.OBSERVED, input.ownership());
        assertFalse("effect inputs are never delegated to ART",
                input.ownership() == NativeRenderOwnership.DELEGATED_TO_ART);
    }

    private NativeRenderInputComponent surfaceInput(String ownerId) {
        NativeRenderInputComponent input = inputFor(ownerId);
        assertNotNull("surface owner " + ownerId + " must carry a native render input", input);
        return input;
    }

    private NativeRenderInputComponent inputFor(String ownerId) {
        artframework.ecs.EntityId entity = Sts1NativePresentationAdapter.entity(ownerId);
        if (entity == null) return null;
        return artframework.presentation.PresentationRegistry.context("nrcc-native")
                .world().get(entity, NativeRenderInputComponent.class);
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

    @Test
    public void filterScopeInactiveLeavesDelegationUnchanged() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_HAND);

        RenderDisposition disposition = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "hand");

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertEquals(Boolean.FALSE, filterScopes().get("active"));
    }

    @Test
    public void filteredNonSelectedFamilyIsDowngradedToPassThrough() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().select(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);

        RenderDisposition disposition = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "intents");

        assertEquals(RenderDisposition.Mode.PASS_THROUGH, disposition.mode);
        assertTrue(disposition.nativeContinuation);
        assertTrue(disposition.reason.startsWith("filter_scope:"));
        assertEquals(0, NativeRenderBridge.ledger().evidenceCount());
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
    }

    @Test
    public void selectedFamilyStillDelegatesWhileScopeActive() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().select(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);

        RenderDisposition disposition = beginDelegatedSurface(SurfaceIds.COMBAT_HAND, "hand");

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse(disposition.nativeContinuation);
    }

    @Test
    public void filterScopeNeverUpgradesFailOpenToDelegate() {
        mountedCombat();
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().select(SurfaceIds.COMBAT_HAND);
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown", "native.Unknown", "render", "unknown");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);

        PresentSafety.panic("filter-scope");
        RenderDisposition panic = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "native.Owner", "render", "panic");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, panic.mode);
        assertTrue(panic.nativeContinuation);
        assertEquals(0, NativeRenderBridge.ledger().evidenceCount());
    }

    @Test
    public void consoleFilterScopeStillFailsOpenForUnknownAndPanic() {
        mountedCombat();
        Sts1VerifyDiagnostics.enableNativeFilter(SurfaceIds.COMBAT_HAND);

        RenderDisposition unknown = NativeRenderBridge.beginSurface(
                "sts1.unknown", "native.Unknown", "render", "unknown");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, unknown.mode);
        assertTrue(unknown.nativeContinuation);

        PresentSafety.panic("console-filter-scope");
        RenderDisposition panic = NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_HAND, "native.Owner", "render", "panic");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, panic.mode);
        assertTrue(panic.nativeContinuation);
        assertEquals(0, NativeRenderBridge.ledger().evidenceCount());

        Sts1VerifyDiagnostics.clearNativeFilters();
        assertEquals(Boolean.FALSE, filterScopes().get("active"));
    }

    @Test
    public void clearRecoveryAndResetRestoreDelegation() {
        mountedCombat();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);
        assertEquals(RenderDisposition.Mode.PASS_THROUGH, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "blocked").mode);

        NativeRenderBridge.filterScope().clear();
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "cleared").mode);
        NativeRenderBridge.clearTransientEffectsForRecovery();
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "recovered").mode);

        NativeRenderBridge.filterScope().activate();
        NativeRenderBridge.filterScope().filter(SurfaceIds.COMBAT_INTENTS);
        NativeRenderBridge.resetForTests();
        armFullSurface(SurfaceIds.COMBAT_INTENTS);
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, NativeRenderBridge.beginSurface(
                SurfaceIds.COMBAT_INTENTS, "native.Owner", "render", "reset").mode);
        assertEquals(Boolean.FALSE, filterScopes().get("active"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterScopes() {
        return (Map<String, Object>) NativeRenderBridge.probeSlice().get("filterScopes");
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
    public void lateBeginEffectRenderAfterCompletionDoesNotRecreateActiveEffect() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.beginEffectRender(effect, "render");
        effect.isDone = true;
        NativeRenderBridge.observeEffectUpdate(effect);
        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        int total = ((Integer) transientEffects().get("total")).intValue();

        RenderDisposition late = NativeRenderBridge.beginEffectRender(effect, "late-render");

        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, late.mode);
        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        assertEquals(Integer.valueOf(total), transientEffects().get("total"));
        assertEquals(Integer.valueOf(1), transientEffects().get("unknownLifecycle"));
        assertEquals(Integer.valueOf(0), transientEffects().get("failOpen"));
    }

    @Test
    public void transientEffectRenderAlwaysCapturesAndPasses() {
        // Default state: the aura claim gate is OFF, so every effect stays observe-only
        // (CAPTURE_AND_PASS) and the native effect queue continues. The gate-ON per-instance
        // claim is covered by the aura* tests below.
        assertFalse(AuraDelegationGate.isActive());
        AbstractGameEffect effect = effect();
        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render_at");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue("native effect queue must continue after observation",
                disposition.nativeContinuation);
    }

    @Test
    public void auraClaimGateOnWithUnsupportedClassStillCapturesAndPasses() {
        AuraDelegationGate.setActive(true);
        AbstractGameEffect effect = effect();

        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render");

        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue(disposition.nativeContinuation);
        assertFalse(NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId));
    }

    @Test
    public void auraClaimGateOnWithSupportedClassButNotReadyStillCapturesAndPasses() {
        AuraDelegationGate.setActive(true);
        AbstractGameEffect effect = supportedAuraEffect();

        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render");

        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertTrue(disposition.nativeContinuation);
        assertFalse(NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId));
    }

    @Test
    public void auraClaimGateOnReadySuppressesNativeAndRegistersToken() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(alwaysReadyAdapter());
        AbstractGameEffect effect = supportedAuraEffect();

        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render");

        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, disposition.mode);
        assertFalse("a claim suppresses only this instance", disposition.nativeContinuation);
        assertEquals("aura_claim", disposition.reason);
        assertTrue(NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId));
    }

    @Test
    public void auraClaimDrawRecordsPixelEvidenceAndConsumesToken() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(alwaysReadyAdapter());
        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(
                supportedAuraEffect(), "render");

        NativeRenderBridge.recordEffectDraw(disposition.invocationId, 1);

        assertFalse("draw consumes the pending claim token",
                NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId));
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
    }

    @Test
    public void auraClaimFailureFailsOpenAndConsumesToken() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(alwaysReadyAdapter());
        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(
                supportedAuraEffect(), "render");

        NativeRenderBridge.recordEffectFailure(disposition.invocationId);

        assertFalse("failure consumes the pending claim token",
                NativeRenderBridge.isAuraClaimInvocation(disposition.invocationId));
        assertEquals("fallback leaves no open delegated gap",
                Integer.valueOf(1), NativeRenderBridge.strictReport().get("delegatedWithoutEvidence"));
        assertEquals("fallback records a delegated mismatch",
                Integer.valueOf(1), NativeRenderBridge.strictReport().get("dispositionMismatch"));
    }

    @Test
    public void auraClaimPanicAndBackgroundOnlyStillWin() {
        AuraDelegationGate.setActive(true);
        AuraArtRenderer.setForTests(alwaysReadyAdapter());

        PresentSafety.panic("aura-panic");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN,
                NativeRenderBridge.beginEffectRender(supportedAuraEffect(), "render").mode);
        PresentSafety.clearPanic();

        Sts1VerifyDiagnostics.setBackgroundOnly(true);
        RenderDisposition blocked = NativeRenderBridge.beginEffectRender(
                supportedAuraEffect(), "render");
        assertEquals(RenderDisposition.Mode.BLOCKED, blocked.mode);
        assertFalse(NativeRenderBridge.isAuraClaimInvocation(blocked.invocationId));
        Sts1VerifyDiagnostics.setBackgroundOnly(false);
    }

    private static AuraArtRenderer.Adapter alwaysReadyAdapter() {
        return new AuraArtRenderer.Adapter() {
            @Override public boolean isReady(String nativeClassName) { return true; }
            @Override public boolean render(SpriteBatch sb, AbstractGameEffect effect) {
                return true;
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

    @Test
    public void backgroundOnlyBlocksKnownEffect() {
        Sts1VerifyDiagnostics.setBackgroundOnly(true);
        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect(), "render");
        assertEquals(RenderDisposition.Mode.BLOCKED, disposition.mode);
        assertFalse(disposition.nativeContinuation);
    }

    @Test
    public void isolateSuppressesKnownEffectAndAllowRestoresNativeContinuation() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.policy().setIsolate(true);
        RenderDisposition isolated = NativeRenderBridge.beginEffectRender(effect, "render");
        assertEquals(RenderDisposition.Mode.DELEGATE_TO_ART, isolated.mode);
        assertFalse(isolated.nativeContinuation);

        NativeRenderBridge.policy().allow(NativeRenderPolicy.Target.parse(
                "class:" + effect.getClass().getName()));
        RenderDisposition allowed = NativeRenderBridge.beginEffectRender(effect, "render");
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, allowed.mode);
        assertTrue(allowed.nativeContinuation);
    }

    @Test
    public void isolateEffectPanicAndRecoveryFailOpen() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.policy().setIsolate(true);
        PresentSafety.panic("effect-isolate");
        assertEquals(RenderDisposition.Mode.FAIL_OPEN,
                NativeRenderBridge.beginEffectRender(effect, "render").mode);
        PresentSafety.clearPanic();
        NativeRenderBridge.clearTransientEffectsForRecovery();
        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS,
                NativeRenderBridge.beginEffectRender(effect, "render").mode);
    }

    @Test
    public void containerEffectRenderConsumesDelegationAndDoesNotCreateArtEvidence() {
        final AtomicInteger nativeRenders = new AtomicInteger();
        AbstractGameEffect effect = new AbstractGameEffect() {
            @Override public void render(SpriteBatch sb) { nativeRenders.incrementAndGet(); }
            @Override public void dispose() { }
        };
        NativeRenderBridge.policy().setIsolate(true);

        TransientEffectContainerPatches.observeThenRender(effect, null);

        assertEquals(Integer.valueOf(0), Integer.valueOf(nativeRenders.get()));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.probeSlice().get("evidenceCount"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get(
                "delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), NativeRenderBridge.strictReport().get("openInvocation"));
    }

    @Test
    public void lateBeginEffectRenderAfterRecoveryClearRemainsFailOpenAndInactive() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.beginEffectRender(effect, "render");
        NativeRenderBridge.clearTransientEffectsForRecovery();

        RenderDisposition late = NativeRenderBridge.beginEffectRender(effect, "late-render");

        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, late.mode);
        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        assertEquals(Integer.valueOf(1), transientEffects().get("unknownLifecycle"));
        assertEquals(Integer.valueOf(0), transientEffects().get("total"));
    }

    @Test
    public void lateBeginEffectRenderAfterDisposeDoesNotRecreateActiveEffect() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.beginEffectRender(effect, "render");
        NativeRenderBridge.observeEffectDispose(effect);

        NativeRenderBridge.beginEffectRender(effect, "late-render");

        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        assertEquals(Integer.valueOf(1), transientEffects().get("total"));
        assertEquals(Integer.valueOf(1), transientEffects().get("unknownLifecycle"));
    }

    @Test
    public void lateBeginEffectRenderAfterRecentEvictionDoesNotRecreateActiveEffect() {
        AbstractGameEffect evicted = effect();
        NativeRenderBridge.beginEffectRender(evicted, "render");
        NativeRenderBridge.observeEffectDispose(evicted);
        for (int i = 0; i < TransientEffectLedger.DEFAULT_RECENT_CAPACITY; i++) {
            AbstractGameEffect newer = effect();
            NativeRenderBridge.beginEffectRender(newer, "render");
            NativeRenderBridge.observeEffectDispose(newer);
        }

        NativeRenderBridge.beginEffectRender(evicted, "late-render");

        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        assertEquals(Integer.valueOf(TransientEffectLedger.DEFAULT_RECENT_CAPACITY + 1),
                transientEffects().get("total"));
        assertEquals(Integer.valueOf(1), transientEffects().get("unknownLifecycle"));
        assertEquals(Integer.valueOf(1), transientEffects().get("evicted"));
        assertEquals(Integer.valueOf(TransientEffectLedger.DEFAULT_RECENT_CAPACITY),
                transientEffects().get("recent"));
    }

    @Test
    public void panicKeepsEffectCleanupEmptyAndNativeLedgerAcceptedAcrossRepeatedCallbacks() {
        AbstractGameEffect effect = effect();
        NativeRenderBridge.beginEffectRender(effect, "render");
        assertEquals(Integer.valueOf(1), transientEffects().get("active"));

        PresentSafety.panic("effect-cleanup");
        int invocationCount = NativeRenderBridge.ledger().invocationCount();
        int dispositionCount = NativeRenderBridge.ledger().dispositionCount();
        for (int i = 0; i < 3; i++) {
            RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render");
            assertEquals(RenderDisposition.Mode.FAIL_OPEN, disposition.mode);
            assertTrue("panic must continue native effect rendering", disposition.nativeContinuation);
            NativeRenderBridge.observeEffectUpdate(effect);
            NativeRenderBridge.observeEffectDispose(effect);
        }

        assertEquals(Integer.valueOf(0), transientEffects().get("active"));
        assertEquals(Integer.valueOf(invocationCount),
                NativeRenderBridge.probeSlice().get("invocationCount"));
        assertEquals(Integer.valueOf(dispositionCount),
                NativeRenderBridge.probeSlice().get("dispositionCount"));
        assertEquals(Boolean.TRUE, NativeRenderBridge.strictReport().get("accepted"));
    }

    @Test
    public void effectObservationResumesAfterPanicClears() {
        AbstractGameEffect effect = effect();
        PresentSafety.panic("effect-cleanup");
        NativeRenderBridge.beginEffectRender(effect, "render");
        PresentSafety.clearPanic();

        RenderDisposition disposition = NativeRenderBridge.beginEffectRender(effect, "render");

        assertEquals(RenderDisposition.Mode.CAPTURE_AND_PASS, disposition.mode);
        assertEquals(Integer.valueOf(1), transientEffects().get("active"));
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("invocationCount"));
        assertEquals(Integer.valueOf(1), NativeRenderBridge.probeSlice().get("dispositionCount"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> transientEffects() {
        return (Map<String, Object>) NativeRenderBridge.probeSlice().get("transientEffects");
    }

    private AbstractGameEffect effect() {
        return new AbstractGameEffect() {
            @Override
            public void render(SpriteBatch sb) {
            }

            @Override
            public void dispose() {
            }
        };
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

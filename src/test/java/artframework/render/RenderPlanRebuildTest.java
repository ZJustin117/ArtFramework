package artframework.render;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.ecs.EntityId;
import artframework.presentation.BoundsComponent;
import artframework.presentation.EffectAttachment;
import artframework.presentation.HostBindingComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationFrame;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.sts1.render.NativeRenderInvocation;
import artframework.sts1.render.Sts1NativePresentationAdapter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RenderPlanRebuildTest {
    @After public void tearDown() { ArtFramework.resetForTests(); }

    @Test public void ecsPlanRebuildRestoresSurfaceFullFrameAndItemCache() {
        RenderStateEcs.surface("sts1.test", 1f, 2f, 30f, 40f, true);
        RenderStateEcs.surfaceEffects("sts1.test", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        RenderStateEcs.fullFrame(1920f, 1080f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", null)));
        artframework.presentation.PresentationVisuals.syncC2Item(
                "sts1.test", "item", new Rect(5f, 6f, 7f, 8f), 2f,
                "card", "", "", true);

        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan(Collections.singleton("sts1.test"));
        host.clearTargets();
        host.rebuildFromEcsPlan(Collections.singleton("sts1.test"));

        assertNotNull(host.getTarget(RenderHost.FULL_FRAME_ID));
        assertNotNull(host.getTarget(RenderHost.c2SurfaceTargetId("sts1.test")));
        assertNotNull(host.getTarget(RenderHost.c2ItemTargetId("sts1.test", "item")));
        assertEquals(3, host.targetCount());
        assertEquals(3, host.bindingCount());
    }

    @Test public void renderOrderSortsByPhaseThenZThenStableKey() {
        RenderOrder highPhase = new RenderOrder(RenderPhase.C2_CONTENT, -100f, "b");
        RenderOrder lowPhase = new RenderOrder(RenderPhase.C1_CONTENT, 100f, "z");
        RenderOrder lowZ = new RenderOrder(RenderPhase.C2_CONTENT, 1f, "z");
        RenderOrder lowKey = new RenderOrder(RenderPhase.C2_CONTENT, 1f, "a");

        assertTrue(RenderOrder.COMPARATOR.compare(lowPhase, highPhase) < 0);
        assertTrue(RenderOrder.COMPARATOR.compare(highPhase, lowZ) < 0);
        assertTrue(RenderOrder.COMPARATOR.compare(lowKey, lowZ) < 0);
    }

    @Test public void renderOrderRejectsNonFiniteZAndMissingStableKey() {
        try {
            new RenderOrder(RenderPhase.C1_CONTENT, Float.NaN, "x");
            throw new AssertionError("expected NaN rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("finite"));
        }
        try {
            new RenderOrder(RenderPhase.C1_CONTENT, 0f, "");
            throw new AssertionError("expected stable key rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("stable key"));
        }
    }

    @Test public void c2PlanEntriesExposeDeterministicOrderingMetadata() {
        RenderStateEcs.surface("sts1.z-order", 0f, 0f, 10f, 10f, true);
        RenderPlan.Entry entry = RenderPlan.fromActiveC2Surfaces(null).entries().get(0);

        assertEquals(RenderPhase.C2_CONTENT, entry.phase);
        assertEquals(RenderHost.c2SurfaceTargetId("sts1.z-order"), entry.stableKey);
        assertEquals(entry.stableKey, entry.order().stableKey);
    }

    @Test public void renderPlanOrdersEntriesByImmutableOrderingKey() throws Exception {
        ArrayList<RenderPlan.Entry> entries = new ArrayList<RenderPlan.Entry>();
        entries.add(new RenderPlan.Entry("z-item", RenderTargetKind.C2_SURFACE,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.C2_CONTENT, 2f, "z-item", true,
                Collections.<EffectAttachment>emptyList()));
        entries.add(new RenderPlan.Entry("a-item", RenderTargetKind.C2_SURFACE,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.C2_CONTENT, 2f, "a-item", true,
                Collections.<EffectAttachment>emptyList()));
        entries.add(new RenderPlan.Entry("c1-item", RenderTargetKind.SYNTHETIC_WIDGET,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.C1_CONTENT, 99f, "c1-item", true,
                Collections.<EffectAttachment>emptyList()));

        Constructor<RenderPlan> constructor = RenderPlan.class.getDeclaredConstructor(java.util.List.class);
        constructor.setAccessible(true);
        RenderPlan plan = constructor.newInstance(entries);

        assertEquals("c1-item", plan.entries().get(0).id);
        assertEquals("a-item", plan.entries().get(1).id);
        assertEquals("z-item", plan.entries().get(2).id);
    }

    @Test public void renderPlanRejectsDuplicateStableKeys() throws Exception {
        ArrayList<RenderPlan.Entry> entries = new ArrayList<RenderPlan.Entry>();
        entries.add(new RenderPlan.Entry("first", RenderTargetKind.C2_SURFACE,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.C2_CONTENT, 0f, "same", true,
                Collections.<EffectAttachment>emptyList()));
        entries.add(new RenderPlan.Entry("second", RenderTargetKind.C2_SURFACE,
                new Rect(0f, 0f, 1f, 1f), RenderPhase.C2_CONTENT, 1f, "same", true,
                Collections.<EffectAttachment>emptyList()));

        Constructor<RenderPlan> constructor = RenderPlan.class.getDeclaredConstructor(java.util.List.class);
        constructor.setAccessible(true);
        try {
            constructor.newInstance(entries);
            throw new AssertionError("expected duplicate stable key rejection");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
            assertTrue(expected.getCause().getMessage().contains("duplicate render stable key"));
        }
    }

    @Test public void unchangedPlanEntryRetainsTargetIdentity() {
        RenderStateEcs.surface("sts1.identity", 1f, 2f, 30f, 40f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        RenderTarget target = host.getTarget(RenderHost.c2SurfaceTargetId("sts1.identity"));

        host.rebuildFromEcsPlan();

        assertSame(target, host.getTarget(RenderHost.c2SurfaceTargetId("sts1.identity")));
    }

    @Test public void changedPlanFieldsUpdateRetainedTarget() {
        RenderStateEcs.surface("sts1.mutable", 1f, 2f, 30f, 40f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        RenderTarget target = host.getTarget(RenderHost.c2SurfaceTargetId("sts1.mutable"));

        RenderStateEcs.surface("sts1.mutable", 5f, 6f, 70f, 80f, false);
        host.rebuildFromEcsPlan();

        assertSame(target, host.getTarget(RenderHost.c2SurfaceTargetId("sts1.mutable")));
        assertEquals(new Rect(5f, 6f, 70f, 80f), target.bounds());
        assertFalse(target.isEnabled());
    }

    @Test public void retainedTargetCarriesPlanOrderingMetadata() {
        RenderStateEcs.surface("sts1.order-metadata", 1f, 2f, 30f, 40f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();

        RenderTarget target = host.getTarget(RenderHost.c2SurfaceTargetId("sts1.order-metadata"));
        assertEquals(RenderPhase.C2_CONTENT, target.phase());
        assertEquals(RenderHost.c2SurfaceTargetId("sts1.order-metadata"), target.stableKey());
    }

    @Test public void hostSubmissionSnapshotUsesStableOrderingInsteadOfInsertionOrder() {
        RenderStateEcs.surface("z-surface", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surface("a-surface", 0f, 0f, 10f, 10f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();

        assertEquals(Arrays.asList(RenderHost.c2SurfaceTargetId("a-surface"),
                RenderHost.c2SurfaceTargetId("z-surface")),
                host.orderedTargetIds(RenderHost.kindsC2UnderPresent()));
    }

    @Test public void ordinaryReconciliationRemovesOnlyStalePlanOwnedTargets() {
        RenderStateEcs.surface("sts1.stale-owned", 1f, 2f, 30f, 40f, true);
        RenderHost host = new RenderHost();
        RenderTarget overlay = host.ensureTarget("manual:overlay", RenderTargetKind.OVERLAY);
        host.bindEffect("manual:overlay", TintEffect.ID,
                Collections.<String, Object>singletonMap("alpha", 0.2f));
        host.rebuildFromEcsPlan();
        String ownedId = RenderHost.c2SurfaceTargetId("sts1.stale-owned");
        assertNotNull(host.getTarget(ownedId));

        RenderStateEcs.removeSurface("sts1.stale-owned");
        host.rebuildFromEcsPlan();

        assertEquals(null, host.getTarget(ownedId));
        assertSame(overlay, host.getTarget("manual:overlay"));
        assertEquals(1, host.effectsOf("manual:overlay").size());
    }

    @Test public void matchingOrderedEffectIdentitySynchronizesRetainedBinding() {
        Map<String, Object> firstParams = new LinkedHashMap<String, Object>();
        firstParams.put("alpha", Float.valueOf(0.2f));
        firstParams.put("obsolete", Float.valueOf(1f));
        RenderStateEcs.surface("sts1.binding", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.binding", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "foreground", firstParams)));
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String id = RenderHost.c2SurfaceTargetId("sts1.binding");
        EffectBinding binding = host.effectsOf(id).get(0);

        RenderStateEcs.surfaceEffects("sts1.binding", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "foreground",
                        Collections.<String, Object>singletonMap("alpha", 0.7f))
                        .withEnabled(false)));
        host.rebuildFromEcsPlan();

        assertSame(binding, host.effectsOf(id).get(0));
        assertEquals("foreground", binding.layer());
        assertEquals(0.7f, binding.paramFloat("alpha", 0f), 0f);
        assertFalse(binding.paramsView().containsKey("obsolete"));
        assertFalse(binding.isEnabled());
    }

    @Test public void unchangedOrderedEffectIdentityRetainsBinding() {
        RenderStateEcs.surface("sts1.binding-identity", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.binding-identity", Arrays.asList(
                new EffectAttachment(TintEffect.ID, "ambient", null),
                new EffectAttachment(GlowEffect.ID, "foreground", null)));
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String id = RenderHost.c2SurfaceTargetId("sts1.binding-identity");
        EffectBinding first = host.effectsOf(id).get(0);
        EffectBinding second = host.effectsOf(id).get(1);

        host.rebuildFromEcsPlan();

        assertSame(first, host.effectsOf(id).get(0));
        assertSame(second, host.effectsOf(id).get(1));
    }

    @Test public void changedEffectIdentityOrOrderReplacesTargetBindingList() {
        RenderStateEcs.surface("sts1.binding-order", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.binding-order", Arrays.asList(
                new EffectAttachment(TintEffect.ID, "ambient", null),
                new EffectAttachment(GlowEffect.ID, "foreground", null)));
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String id = RenderHost.c2SurfaceTargetId("sts1.binding-order");
        EffectBinding first = host.effectsOf(id).get(0);
        EffectBinding second = host.effectsOf(id).get(1);

        RenderStateEcs.surfaceEffects("sts1.binding-order", Arrays.asList(
                new EffectAttachment(GlowEffect.ID, "foreground", null),
                new EffectAttachment(TintEffect.ID, "ambient", null)));
        host.rebuildFromEcsPlan();

        assertEquals(GlowEffect.ID, host.effectsOf(id).get(0).effectId);
        assertEquals(TintEffect.ID, host.effectsOf(id).get(1).effectId);
        assertNotSame(first, host.effectsOf(id).get(0));
        assertNotSame(second, host.effectsOf(id).get(1));
    }

    @Test public void invalidRetainedAttachmentRollsBackEntirePlan() {
        RenderStateEcs.surface("sts1.rollback-a", 1f, 2f, 30f, 40f, true);
        RenderStateEcs.surfaceEffects("sts1.rollback-a", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        RenderStateEcs.surface("sts1.rollback-b", 5f, 6f, 70f, 80f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String aId = RenderHost.c2SurfaceTargetId("sts1.rollback-a");
        String bId = RenderHost.c2SurfaceTargetId("sts1.rollback-b");
        RenderTarget a = host.getTarget(aId);
        RenderTarget b = host.getTarget(bId);
        EffectBinding binding = host.effectsOf(aId).get(0);

        RenderStateEcs.surface("sts1.rollback-a", 11f, 12f, 13f, 14f, false);
        RenderStateEcs.surfaceEffects("sts1.rollback-a", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 2f))));
        RenderStateEcs.removeSurface("sts1.rollback-b");
        try {
            host.rebuildFromEcsPlan();
            throw new AssertionError("expected invalid planned attachment");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("alpha"));
        }

        assertSame(a, host.getTarget(aId));
        assertEquals(new Rect(1f, 2f, 30f, 40f), a.bounds());
        assertTrue(a.isEnabled());
        assertSame(binding, host.effectsOf(aId).get(0));
        assertEquals(0.2f, binding.paramFloat("alpha", 0f), 0f);
        assertSame("stale removal must wait for complete validation", b, host.getTarget(bId));
    }

    @Test public void invalidNewAttachmentLeavesExistingPlanCacheUntouched() {
        RenderStateEcs.surface("sts1.rollback-existing", 1f, 2f, 3f, 4f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String existingId = RenderHost.c2SurfaceTargetId("sts1.rollback-existing");
        RenderTarget existing = host.getTarget(existingId);

        RenderStateEcs.removeSurface("sts1.rollback-existing");
        RenderStateEcs.surface("sts1.rollback-new", 5f, 6f, 7f, 8f, true);
        RenderStateEcs.surfaceEffects("sts1.rollback-new", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", -1f))));
        try {
            host.rebuildFromEcsPlan();
            throw new AssertionError("expected invalid new attachment");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("alpha"));
        }

        assertSame(existing, host.getTarget(existingId));
        assertEquals(null, host.getTarget(RenderHost.c2SurfaceTargetId("sts1.rollback-new")));
    }

    @Test public void duplicatePlanTargetIdIsRejectedBeforeMutation() throws Exception {
        RenderStateEcs.surface("sts1.duplicate-existing", 1f, 2f, 3f, 4f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String existingId = RenderHost.c2SurfaceTargetId("sts1.duplicate-existing");
        RenderTarget existing = host.getTarget(existingId);
        ArrayList<RenderPlan.Entry> entries = new ArrayList<RenderPlan.Entry>();
        entries.add(new RenderPlan.Entry("duplicate", RenderTargetKind.C2_SURFACE,
                new Rect(0f, 0f, 1f, 1f), 0f, true,
                Collections.<EffectAttachment>emptyList()));
        entries.add(new RenderPlan.Entry("duplicate", RenderTargetKind.C2_SURFACE,
                new Rect(2f, 2f, 3f, 3f), 1f, false,
                Collections.<EffectAttachment>emptyList()));
        Constructor<RenderPlan> constructor = RenderPlan.class.getDeclaredConstructor(java.util.List.class);
        constructor.setAccessible(true);
        RenderPlan duplicatePlan = constructor.newInstance(entries);
        Method apply = RenderHost.class.getDeclaredMethod("applyPlan", RenderPlan.class);
        apply.setAccessible(true);

        try {
            apply.invoke(host, duplicatePlan);
            throw new AssertionError("expected duplicate target id rejection");
        } catch (InvocationTargetException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
            assertTrue(expected.getCause().getMessage().contains("duplicate target id"));
        }
        assertSame(existing, host.getTarget(existingId));
        assertEquals(null, host.getTarget("duplicate"));
    }

    @Test public void unknownPlannedEffectsAreSkippedWithoutDisturbingValidOrder() {
        RenderStateEcs.surface("sts1.unknown", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surfaceEffects("sts1.unknown", Arrays.asList(
                new EffectAttachment(TintEffect.ID, "ambient", null),
                new EffectAttachment("test.unknown", "middle", null),
                new EffectAttachment(GlowEffect.ID, "foreground", null)));
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String id = RenderHost.c2SurfaceTargetId("sts1.unknown");
        EffectBinding tint = host.effectsOf(id).get(0);
        EffectBinding glow = host.effectsOf(id).get(1);

        host.rebuildFromEcsPlan();

        assertEquals(2, host.effectsOf(id).size());
        assertSame(tint, host.effectsOf(id).get(0));
        assertSame(glow, host.effectsOf(id).get(1));
        host.effects().register(new NoOpEffect("test.unknown"));
        host.rebuildFromEcsPlan();
        assertEquals(3, host.effectsOf(id).size());
        assertEquals("test.unknown", host.effectsOf(id).get(1).effectId);
    }

    @Test public void concurrentReconcileReadsOnlyCompleteBindingSnapshots() throws Exception {
        RenderStateEcs.surface("sts1.concurrent-binding", 0f, 0f, 10f, 10f, true);
        RenderHost host = new RenderHost();
        writeConcurrentEffect(0, true);
        host.rebuildFromEcsPlan();
        final EffectBinding binding = host.effectsOf(
                RenderHost.c2SurfaceTargetId("sts1.concurrent-binding")).get(0);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        Thread writer = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    start.await();
                    for (int i = 1; i <= 1000; i++) {
                        writeConcurrentEffect(i, (i & 1) == 0);
                        host.rebuildFromEcsPlan();
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        });
        Thread reader = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    start.await();
                    for (int i = 0; i < 5000; i++) {
                        Map<String, Object> snapshot = binding.paramsView();
                        assertEquals(snapshot.get("left"), snapshot.get("right"));
                        assertNotNull(snapshot.get("enabled"));
                        binding.layer();
                        binding.paramFloat("alpha", 0f);
                        binding.isEnabled();
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }
        });
        writer.start();
        reader.start();
        start.countDown();
        writer.join();
        reader.join();
        assertTrue(failure.get() == null);
        assertSame(binding, host.effectsOf(
                RenderHost.c2SurfaceTargetId("sts1.concurrent-binding")).get(0));
    }

    private static void writeConcurrentEffect(int revision, boolean enabled) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("alpha", Float.valueOf((revision % 10) / 10f));
        params.put("left", Integer.valueOf(revision));
        params.put("right", Integer.valueOf(revision));
        RenderStateEcs.surfaceEffects("sts1.concurrent-binding", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", params).withEnabled(enabled)));
    }

    private static final class NoOpEffect implements Effect {
        private final String id;
        NoOpEffect(String id) { this.id = id; }
        @Override public String id() { return id; }
        @Override public String shaderId() { return ""; }
        @Override public boolean requiresCapture() { return false; }
        @Override public void validate(EffectBinding binding) { }
        @Override public void draw(RenderTarget target, EffectBinding binding, RenderContext ctx) { }
    }

    @Test public void unchangedRenderStateWritesKeepAuthoritativeComponentInstances() {
        RenderStateEcs.surface("sts1.unchanged", 1f, 2f, 30f, 40f, true);
        RenderSurfaceComponent surface = RenderStateEcs.surfaceState("sts1.unchanged");
        RenderStateEcs.surface("sts1.unchanged", 1f, 2f, 30f, 40f, true);
        assertSame(surface, RenderStateEcs.surfaceState("sts1.unchanged"));

        RenderStateEcs.surfaceEffects("sts1.unchanged", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        surface = RenderStateEcs.surfaceState("sts1.unchanged");
        RenderStateEcs.surfaceEffects("sts1.unchanged", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        assertSame(surface, RenderStateEcs.surfaceState("sts1.unchanged"));

        RenderStateEcs.fullFrame(800f, 600f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        FullFrameRenderComponent fullFrame = RenderStateEcs.fullFrameState();
        RenderStateEcs.fullFrame(800f, 600f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        assertSame(fullFrame, RenderStateEcs.fullFrameState());

        RenderStateEcs.captureEnabled(true);
        RenderCaptureComponent capture = captureState();
        RenderStateEcs.captureEnabled(true);
        assertSame(capture, captureState());

        C1BoundsFixture c1 = c1BoundsFixture(new Rect(4f, 5f, 60f, 20f), 7f);
        RenderStateEcs.updateC1Bounds(c1.windowId, c1.effectKey,
                new Rect(4f, 5f, 60f, 20f));
        assertSame(c1.initial, boundsState(c1));
        assertEquals(7f, boundsState(c1).z, 0f);
    }

    @Test public void changedRenderStateWritesReplaceOnlyTheChangedAuthority() {
        RenderStateEcs.surface("sts1.changed", 1f, 2f, 30f, 40f, true);
        RenderSurfaceComponent surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surface("sts1.changed", 1f, 2f, 31f, 40f, true);
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));

        surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surface("sts1.changed", 3f, 2f, 31f, 40f, true);
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));

        surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surface("sts1.changed", 3f, 2f, 31f, 40f, false);
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));
        assertFalse(RenderStateEcs.surfaceState("sts1.changed").enabled);

        RenderStateEcs.surfaceEffects("sts1.changed", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surfaceEffects("sts1.changed", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.3f))));
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));
        assertEquals(0.3f, RenderStateEcs.surfaceState("sts1.changed")
                .effects().get(0).floatParam("alpha", 0f), 0f);

        surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surfaceEffects("sts1.changed", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.3f))
                        .withEnabled(false)));
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));
        assertFalse(RenderStateEcs.surfaceState("sts1.changed").effects().get(0).isEnabled());

        surface = RenderStateEcs.surfaceState("sts1.changed");
        RenderStateEcs.surfaceEffects("sts1.changed", Collections.singletonList(
                new EffectAttachment(GlowEffect.ID, "foreground",
                        Collections.<String, Object>singletonMap("alpha", 0.3f))
                        .withEnabled(false)));
        assertNotSame(surface, RenderStateEcs.surfaceState("sts1.changed"));
        assertEquals(GlowEffect.ID,
                RenderStateEcs.surfaceState("sts1.changed").effects().get(0).effectId);
        assertEquals("foreground",
                RenderStateEcs.surfaceState("sts1.changed").effects().get(0).layer);

        RenderStateEcs.fullFrame(800f, 600f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        FullFrameRenderComponent fullFrame = RenderStateEcs.fullFrameState();
        RenderStateEcs.fullFrame(801f, 600f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        assertNotSame(fullFrame, RenderStateEcs.fullFrameState());

        fullFrame = RenderStateEcs.fullFrameState();
        RenderStateEcs.fullFrame(801f, 600f, false, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        assertNotSame(fullFrame, RenderStateEcs.fullFrameState());
        assertFalse(RenderStateEcs.fullFrameState().enabled);

        fullFrame = RenderStateEcs.fullFrameState();
        RenderStateEcs.fullFrame(801f, 600f, false, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.4f))));
        assertNotSame(fullFrame, RenderStateEcs.fullFrameState());
        assertEquals(0.4f, RenderStateEcs.fullFrameState()
                .effects().get(0).floatParam("alpha", 0f), 0f);

        RenderStateEcs.captureEnabled(false);
        RenderCaptureComponent capture = captureState();
        RenderStateEcs.captureEnabled(true);
        assertNotSame(capture, captureState());
        assertTrue(captureState().enabled);

        C1BoundsFixture c1 = c1BoundsFixture(new Rect(4f, 5f, 60f, 20f), 7f);
        RenderStateEcs.updateC1Bounds(c1.windowId, c1.effectKey,
                new Rect(4f, 5f, 61f, 20f));
        assertNotSame(c1.initial, boundsState(c1));
        assertEquals(new Rect(4f, 5f, 61f, 20f), boundsState(c1).rect);
        assertEquals("changed C1 geometry must retain ECS z", 7f, boundsState(c1).z, 0f);

        BoundsComponent dimensionChanged = boundsState(c1);
        RenderStateEcs.updateC1Bounds(c1.windowId, c1.effectKey,
                new Rect(6f, 5f, 61f, 20f));
        assertNotSame(dimensionChanged, boundsState(c1));
        assertEquals(new Rect(6f, 5f, 61f, 20f), boundsState(c1).rect);
        assertEquals("changed C1 coordinate must retain ECS z", 7f, boundsState(c1).z, 0f);
    }

    private static RenderCaptureComponent captureState() {
        EntityId entity = RenderStateEcs.context().entity(
                new PresentationKey("render.capture", "screen"));
        return RenderStateEcs.context().world().get(entity, RenderCaptureComponent.class);
    }

    private static C1BoundsFixture c1BoundsFixture(Rect bounds, float z) {
        String windowId = "writer-c1";
        String effectKey = "panel";
        PresentationContext context = PresentationRegistry.context("tree:" + windowId);
        EntityId entity = context.create(new PresentationKey("test.c1-bounds", effectKey),
                effectKey, "panel", "test");
        context.world().put(entity, HostBindingComponent.class,
                new HostBindingComponent("SCENE2D_C1", windowId + ":" + effectKey));
        BoundsComponent initial = new BoundsComponent(bounds, z);
        context.world().put(entity, BoundsComponent.class, initial);
        return new C1BoundsFixture(windowId, effectKey, context, entity, initial);
    }

    private static BoundsComponent boundsState(C1BoundsFixture fixture) {
        return fixture.context.world().get(fixture.entity, BoundsComponent.class);
    }

    private static final class C1BoundsFixture {
        final String windowId;
        final String effectKey;
        final PresentationContext context;
        final EntityId entity;
        final BoundsComponent initial;

        C1BoundsFixture(String windowId, String effectKey, PresentationContext context,
                EntityId entity, BoundsComponent initial) {
            this.windowId = windowId;
            this.effectKey = effectKey;
            this.context = context;
            this.entity = entity;
            this.initial = initial;
        }
    }

    @Test public void nativeProjectionIsVisibleInPresentationFrameAndRenderPlan() {
        Sts1NativePresentationAdapter.present(new NativeRenderInvocation(7L, 3L, "combat",
                "sts1.native.test", "Native", "render", "family", "source",
                new Rect(10f, 20f, 30f, 40f)));

        assertEquals(1, PresentationFrame.from(PresentationRegistry.context("nrcc-native")).items.size());
        RenderPlan plan = RenderPlan.fromEcs(Collections.<String>emptySet());
        assertEquals(1, plan.entries().size());
        assertEquals("native:sts1.native.test", plan.entries().get(0).id);
    }

    @Test public void queueProjectsRequestedActiveSurface() {
        RenderStateEcs.surface("sts1.visible", 0f, 0f, 10f, 10f, true);
        RenderStateEcs.surface("sts1.hidden", 0f, 0f, 10f, 10f, true);

        RenderProjectionQueue.projectActiveSurfaces(Collections.singleton("sts1.visible"));

        assertNotNull(RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.visible")));
        assertEquals(null,
                RenderHosts.get().getTarget(RenderHost.c2SurfaceTargetId("sts1.hidden")));
    }

    @Test public void activeSurfaceProjectionPreservesNonC2TargetsAndRemovesStaleC2Targets() {
        RenderStateEcs.surface("sts1.active", 1f, 2f, 10f, 20f, true);
        RenderStateEcs.surfaceEffects("sts1.active", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.2f))));
        RenderStateEcs.surface("sts1.stale", 3f, 4f, 30f, 40f, true);
        RenderHost host = RenderHosts.get();
        RenderTarget c1 = host.ensureTarget("c1:window", RenderTargetKind.SYNTHETIC_WINDOW);
        c1.setBounds(9f, 8f, 7f, 6f);
        RenderTarget entity = host.ensureTarget("c2:entity:player", RenderTargetKind.ENTITY_SLOT);
        entity.setBounds(5f, 4f, 3f, 2f);
        RenderTarget fullFrame = host.ensureTarget(RenderHost.FULL_FRAME_ID, RenderTargetKind.FULL_FRAME);
        fullFrame.setBounds(100f, 90f, 80f, 70f);
        RenderTarget overlay = host.ensureTarget("overlay:test", RenderTargetKind.OVERLAY);
        overlay.setBounds(6f, 5f, 4f, 3f);
        RenderStateEcs.fullFrame(120f, 110f, true,
                Collections.<EffectAttachment>emptyList());

        RenderProjectionQueue.projectActiveSurfaces(
                new java.util.LinkedHashSet<String>(java.util.Arrays.asList("sts1.active", "sts1.stale")));
        RenderTarget active = host.getTarget(RenderHost.c2SurfaceTargetId("sts1.active"));
        EffectBinding activeBinding = host.effectsOf(active.id).get(0);
        RenderStateEcs.surface("sts1.active", 11f, 12f, 13f, 14f, true);
        RenderProjectionQueue.projectActiveSurfaces(Collections.singleton("sts1.active"));

        assertSame(active, host.getTarget(RenderHost.c2SurfaceTargetId("sts1.active")));
        assertSame(activeBinding, host.effectsOf(active.id).get(0));
        assertEquals(new Rect(11f, 12f, 13f, 14f),
                host.getTarget(RenderHost.c2SurfaceTargetId("sts1.active")).bounds());
        assertEquals(null, host.getTarget(RenderHost.c2SurfaceTargetId("sts1.stale")));
        assertEquals(new Rect(9f, 8f, 7f, 6f), host.getTarget("c1:window").bounds());
        assertEquals(new Rect(5f, 4f, 3f, 2f), host.getTarget("c2:entity:player").bounds());
        assertSame(fullFrame, host.getTarget(RenderHost.FULL_FRAME_ID));
        assertEquals(new Rect(6f, 5f, 4f, 3f), host.getTarget("overlay:test").bounds());
        assertEquals("active surface projection must not update unrelated full-frame state",
                new Rect(100f, 90f, 80f, 70f), host.fullFrameTarget().bounds());
    }

    @Test public void activeOwnershipDoesNotDeleteTargetStillOwnedByFullProjection() {
        RenderStateEcs.surface("sts1.overlap", 1f, 2f, 10f, 20f, true);
        RenderHost host = new RenderHost();
        host.rebuildFromEcsPlan();
        String id = RenderHost.c2SurfaceTargetId("sts1.overlap");
        RenderTarget target = host.getTarget(id);

        RenderProjectionQueue.projectActiveSurfaces(Collections.singleton("sts1.overlap"));
        RenderStateEcs.removeSurface("sts1.overlap");
        RenderProjectionQueue.projectActiveSurfaces(Collections.<String>emptySet());

        assertSame("full projection ownership keeps overlapping target alive", target,
                host.getTarget(id));
        host.rebuildFromEcsPlan();
        assertEquals(null, host.getTarget(id));
    }

    @Test public void publicRecreationApiRestoresOnlyFromEcsState() {
        RenderStateEcs.surface("sts1.recreate", 2f, 3f, 20f, 30f, true);
        RenderHost host = new RenderHost();
        host.recreateFromEcs();
        assertNotNull(host.getTarget(RenderHost.c2SurfaceTargetId("sts1.recreate")));

        host.clearHostCacheForRecreation();
        assertEquals(0, host.targetCount());
        host.recreateFromEcs();
        assertEquals(1, host.targetCount());
        assertEquals(new Rect(2f, 3f, 20f, 30f),
                host.getTarget(RenderHost.c2SurfaceTargetId("sts1.recreate")).bounds());
    }

    @Test public void hostRecreationReleasesCacheButRetainsEcsAuthority() {
        RenderStateEcs.surface("sts1.host-recreate", 2f, 3f, 20f, 30f, true);
        RenderStateEcs.surfaceEffects("sts1.host-recreate", Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient",
                        Collections.<String, Object>singletonMap("alpha", 0.4f))));
        RenderStateEcs.fullFrame(800f, 600f, true, Collections.singletonList(
                new EffectAttachment(TintEffect.ID, "ambient", null)));
        RenderProjectionQueue.projectNow();
        String surfaceId = RenderHost.c2SurfaceTargetId("sts1.host-recreate");
        RenderTarget oldTarget = RenderHosts.get().getTarget(surfaceId);
        EffectBinding oldBinding = RenderHosts.get().effectsOf(surfaceId).get(0);
        assertNotNull(oldTarget);
        assertNotNull(RenderHosts.get().fullFrameTarget());

        RenderHosts.get().recreateHostCache();

        assertEquals(2, RenderStateEcs.context().entities().size());
        assertEquals(0, RenderHosts.get().targetCount());
        RenderProjectionQueue.projectNow();
        assertNotNull(RenderHosts.get().getTarget(surfaceId));
        assertNotSame(oldTarget, RenderHosts.get().getTarget(surfaceId));
        assertNotSame(oldBinding, RenderHosts.get().effectsOf(surfaceId).get(0));
        assertNotNull("full-frame host target must be rebuilt from ECS authority",
                RenderHosts.get().fullFrameTarget());
        assertEquals(1, RenderHosts.get().effectsOf(
                RenderHost.c2SurfaceTargetId("sts1.host-recreate")).size());
        assertEquals(1, RenderHosts.get().effectsOf(RenderHost.FULL_FRAME_ID).size());
    }

    @Test public void fullFrameEnabledQueryReadsEcsWithoutHostMirror() {
        RenderHost host = new RenderHost();
        assertFalse(host.isFullFrameEnabled());

        RenderStateEcs.fullFrame(320f, 180f, true,
                Collections.<EffectAttachment>emptyList());

        assertTrue(host.isFullFrameEnabled());
        host.recreateFromEcs();
        assertNotNull(host.fullFrameTarget());

        RenderStateEcs.fullFrame(320f, 180f, false,
                Collections.<EffectAttachment>emptyList());
        assertFalse(host.isFullFrameEnabled());
    }

    @Test public void entityPresentTargetRebuildsFromRetainedSlotComponents() {
        ArtFramework.entities().attach("player", "player", "ironclad");
        ArtFramework.entities().layout("player", 100f, 200f, 0.5f);
        RenderHost host = new RenderHost();
        String targetId = "c2:entity:player";
        host.recreateFromEcs();
        Rect before = host.getTarget(targetId).bounds();

        host.clearHostCacheForRecreation();

        assertEquals(1, ArtFramework.entities().size());
        host.recreateFromEcs();
        assertNotNull(host.getTarget(targetId));
        assertEquals(before, host.getTarget(targetId).bounds());
    }

    @Test public void entityPresentDetachImmediatelyRemovesProjectedTarget() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        assertNotNull(RenderHosts.get().getTarget("c2:entity:player"));

        ArtFramework.entities().detach("player");

        assertEquals(null, RenderHosts.get().getTarget("c2:entity:player"));
    }

    @Test public void entityPresentClearRemovesAllProjectedTargetsInOneBatch() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        ArtFramework.entities().present("monster", "monster", "cultist", null,
                300f, 200f, 0.5f);
        int before = RenderProjectionQueue.rebuildCountForTests();

        ArtFramework.entities().clear();

        assertEquals(null, RenderHosts.get().getTarget("c2:entity:player"));
        assertEquals(null, RenderHosts.get().getTarget("c2:entity:monster"));
        assertEquals(before + 1, RenderProjectionQueue.rebuildCountForTests());
    }

    @Test public void entityPresentSlotsAreEffectFreeAndCannotGainHostOnlyEffects() {
        ArtFramework.entities().present("player", "player", "ironclad", null,
                100f, 200f, 0.5f);
        RenderHost host = RenderHosts.get();
        assertTrue(host.effectsOf("c2:entity:player").isEmpty());

        try {
            host.bindEffect("c2:entity:player", TintEffect.ID,
                    Collections.<String, Object>emptyMap());
            throw new AssertionError("EntityPresent slots must reject host-only effects");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ECS-owned"));
        }

        host.clearHostCacheForRecreation();
        host.recreateFromEcs();
        assertTrue(host.effectsOf("c2:entity:player").isEmpty());
    }
}

package artframework.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NRM-14: probe enumeration of per-instance transient-effect targets ({@code native:effect:*})
 * must be bounded, while stable/named targets are always enumerated and diagnostics keep full
 * coverage.
 */
public class RenderHostProbeBoundTest {

    @After public void tearDown() { RenderHosts.resetForTests(); }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bySafeId(Map<String, Object> probe) {
        return (Map<String, Object>) probe.get("targetsById");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> targetList(Map<String, Object> probe) {
        return (List<Map<String, Object>>) probe.get("targets");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> order(Map<String, Object> probe) {
        return (Map<String, Object>) probe.get("renderOrder");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> orderItems(Map<String, Object> probe) {
        return (List<Map<String, Object>>) order(probe).get("items");
    }

    @SuppressWarnings("unchecked")
    private static List<String> dupKeys(Map<String, Object> probe) {
        return (List<String>) order(probe).get("duplicateStableKeys");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> phaseCounts(Map<String, Object> probe) {
        return (Map<String, Object>) order(probe).get("phaseCounts");
    }

    @Test public void belowCapEnumeratesEveryTargetAndReportsNoTruncation() {
        RenderHost host = new RenderHost();
        host.ensureTarget("c1:stable:panel", RenderTargetKind.SYNTHETIC_WINDOW);
        host.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.probebound"), RenderTargetKind.C2_SURFACE);
        for (int i = 0; i < 3; i++) {
            host.ensureTarget("native:effect:inst_" + i, RenderTargetKind.SYNTHETIC_WIDGET);
        }
        assertEquals(5, host.targetCount());

        Map<String, Object> probe = host.probeMap();

        assertEquals(Integer.valueOf(5), probe.get("targetsTotal"));
        assertEquals(Integer.valueOf(5), probe.get("targetsIncluded"));
        assertEquals(Boolean.FALSE, probe.get("targetsTruncated"));
        assertEquals(5, targetList(probe).size());
        assertEquals(5, bySafeId(probe).size());
        assertEquals(5, orderItems(probe).size());
        assertEquals(Integer.valueOf(5), order(probe).get("total"));
        assertEquals(Integer.valueOf(5), order(probe).get("included"));
        assertEquals(Integer.valueOf(5), order(probe).get("count"));
        assertEquals(Boolean.FALSE, order(probe).get("truncated"));

        assertNotNull(bySafeId(probe).get("c1_stable_panel"));
        assertNotNull(bySafeId(probe).get("c2_surface_sts1_probebound"));
        for (int i = 0; i < 3; i++) {
            assertNotNull(bySafeId(probe).get("native_effect_inst_" + i));
        }
    }

    @Test public void aboveCapBoundsEnumerationButKeepsStableTargetsAndFullDiagnostics() {
        final int transientCount = 1000;
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(64);
        host.ensureTarget("c1:stable:panel", RenderTargetKind.SYNTHETIC_WINDOW);
        host.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.probebound"), RenderTargetKind.C2_SURFACE);
        host.ensureTarget("overlay:stable", RenderTargetKind.OVERLAY);
        for (int i = 0; i < transientCount; i++) {
            RenderTarget t = host.ensureTarget(
                    String.format("native:effect:inst_%04d", Integer.valueOf(i)),
                    RenderTargetKind.SYNTHETIC_WIDGET);
            t.setOrder(RenderPhase.C1_CONTENT, "effect:inst_" + i);
        }
        // A duplicate stable key carried by two transient entries that sort LAST, so the pair is
        // truncated out of the enumerated items while the diagnostic must still report it.
        RenderTarget dupA = host.ensureTarget("native:effect:dup_a", RenderTargetKind.SYNTHETIC_WIDGET);
        dupA.setOrder(RenderPhase.C1_CONTENT, "zzz-dup");
        RenderTarget dupB = host.ensureTarget("native:effect:dup_b", RenderTargetKind.SYNTHETIC_WIDGET);
        dupB.setOrder(RenderPhase.C1_CONTENT, "zzz-dup");

        int expectedTotal = 3 + transientCount + 2;
        assertEquals(expectedTotal, host.targetCount());

        Map<String, Object> probe = host.probeMap();

        int stableCount = 3;
        int boundedMax = stableCount + host.maxProbeEffectTargets();

        assertEquals(Integer.valueOf(expectedTotal), probe.get("targetsTotal"));
        assertEquals(Integer.valueOf(boundedMax), probe.get("targetsIncluded"));
        assertEquals(Boolean.TRUE, probe.get("targetsTruncated"));
        assertEquals(boundedMax, targetList(probe).size());
        assertEquals(boundedMax, bySafeId(probe).size());

        assertEquals(Integer.valueOf(expectedTotal), order(probe).get("total"));
        // `count` carries the FULL ordered target count (pre-cap meaning), not the included size.
        assertEquals(Integer.valueOf(expectedTotal), order(probe).get("count"));
        assertEquals(Integer.valueOf(boundedMax), order(probe).get("included"));
        assertEquals(Boolean.TRUE, order(probe).get("truncated"));
        assertEquals(boundedMax, orderItems(probe).size());

        // Every stable/named target is still fully present in BOTH slices.
        assertNotNull(bySafeId(probe).get("c1_stable_panel"));
        assertNotNull(bySafeId(probe).get("c2_surface_sts1_probebound"));
        assertNotNull(bySafeId(probe).get("overlay_stable"));
        assertTrue("stable targets must survive in renderOrder.items too",
                containsId(orderItems(probe), "c1:stable:panel"));
        assertTrue("stable targets must survive in renderOrder.items too",
                containsId(orderItems(probe), RenderHost.c2SurfaceTargetId("sts1.probebound")));
        assertTrue("stable targets must survive in renderOrder.items too",
                containsId(orderItems(probe), "overlay:stable"));

        // Diagnostics are computed over all targets, not just the enumerated subset.
        assertEquals("duplicate-keys", order(probe).get("status"));
        assertTrue("duplicate among truncated-away transient entries must still be reported",
                dupKeys(probe).contains("zzz-dup"));
    }

    @Test public void capNeverDropsStableTargetsEvenWhenAllTransientSlotsAreUsed() {
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(1);
        for (int i = 0; i < 50; i++) {
            host.ensureTarget("native:effect:inst_" + i, RenderTargetKind.SYNTHETIC_WIDGET);
        }
        host.ensureTarget("c1:stable:panel", RenderTargetKind.SYNTHETIC_WINDOW);
        host.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.probebound"), RenderTargetKind.C2_SURFACE);

        Map<String, Object> probe = host.probeMap();

        assertEquals(Integer.valueOf(52), probe.get("targetsTotal"));
        assertEquals(Integer.valueOf(3), probe.get("targetsIncluded"));
        assertEquals(Boolean.TRUE, probe.get("targetsTruncated"));
        assertNotNull(bySafeId(probe).get("c1_stable_panel"));
        assertNotNull(bySafeId(probe).get("c2_surface_sts1_probebound"));
        assertEquals(3, orderItems(probe).size());
        assertTrue(containsId(orderItems(probe), "c1:stable:panel"));
        assertTrue(containsId(orderItems(probe),
                RenderHost.c2SurfaceTargetId("sts1.probebound")));
        assertEquals(Integer.valueOf(52), order(probe).get("count"));
        // 50 genuinely distinct transient stable keys, so no duplicate is reported.
        assertFalse(order(probe).get("status").equals("duplicate-keys"));
    }

    @Test public void negativeCapClampsToZeroAndExcludesOnlyTransientTargets() {
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(-1);
        assertEquals(0, host.maxProbeEffectTargets());

        host.ensureTarget("c1:stable:panel", RenderTargetKind.SYNTHETIC_WINDOW);
        host.ensureTarget("overlay:stable", RenderTargetKind.OVERLAY);
        host.ensureTarget("native:effect:one", RenderTargetKind.SYNTHETIC_WIDGET);
        host.ensureTarget("native:effect:two", RenderTargetKind.SYNTHETIC_WIDGET);

        Map<String, Object> probe = host.probeMap();

        assertEquals(Integer.valueOf(4), probe.get("targetsTotal"));
        assertEquals(Integer.valueOf(2), probe.get("targetsIncluded"));
        assertEquals(Boolean.TRUE, probe.get("targetsTruncated"));
        assertNotNull("stable targets remain with cap 0", bySafeId(probe).get("c1_stable_panel"));
        assertNotNull("stable targets remain with cap 0", bySafeId(probe).get("overlay_stable"));
        assertEquals(null, bySafeId(probe).get("native_effect_one"));
        assertEquals(null, bySafeId(probe).get("native_effect_two"));
        assertEquals(2, orderItems(probe).size());
        assertTrue(containsId(orderItems(probe), "c1:stable:panel"));
        assertTrue(containsId(orderItems(probe), "overlay:stable"));
        assertEquals(Integer.valueOf(4), order(probe).get("count"));
        assertEquals(Integer.valueOf(2), order(probe).get("included"));
    }

    @Test public void underscoreFallbackEffectTargetIdIsAlsoCapped() {
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(0);
        host.ensureTarget("c1:stable:panel", RenderTargetKind.SYNTHETIC_WINDOW);
        host.ensureTarget("native:effect_legacy_instance", RenderTargetKind.SYNTHETIC_WIDGET);

        Map<String, Object> probe = host.probeMap();

        assertEquals(Integer.valueOf(2), probe.get("targetsTotal"));
        assertEquals(Integer.valueOf(1), probe.get("targetsIncluded"));
        assertEquals(Boolean.TRUE, probe.get("targetsTruncated"));
        assertNotNull(bySafeId(probe).get("c1_stable_panel"));
        assertEquals(null, bySafeId(probe).get("native_effect_legacy_instance"));
    }

    @Test public void phaseCountsHasOneDeterministicEntryForEveryRenderPhase() {
        RenderHost host = new RenderHost();

        Map<String, Object> first = host.probeMap();
        Map<String, Object> second = host.probeMap();

        Map<String, Object> counts = phaseCounts(first);
        RenderPhase[] phases = RenderPhase.values();
        assertEquals(phases.length, counts.size());
        // Declaration order, not hash order, so probe output is deterministic.
        List<String> keys = new ArrayList<String>(counts.keySet());
        for (int i = 0; i < phases.length; i++) {
            assertEquals(phases[i].name(), keys.get(i));
            assertEquals("empty frame must report 0 for " + phases[i].name(),
                    Integer.valueOf(0), counts.get(phases[i].name()));
        }
        assertEquals("phaseCounts must be stable across repeated probe calls",
                phaseCounts(second), counts);
    }

    @Test public void phaseCountsReflectsFullOrderedSetNotJustEnumeratedItems() {
        final int transientCount = 200;
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(0);
        RenderTarget nativeRetained = host.ensureTarget("native:retained", RenderTargetKind.SYNTHETIC_WINDOW);
        nativeRetained.setOrder(RenderPhase.NATIVE_RETAINED, "native:retained");
        RenderTarget c2 = host.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.phasecounts"),
                RenderTargetKind.C2_SURFACE);
        assertEquals(RenderPhase.C2_CONTENT, c2.phase());
        RenderTarget effect = host.ensureTarget("overlay:effect", RenderTargetKind.OVERLAY);
        assertEquals(RenderPhase.ART_EFFECTS, effect.phase());
        for (int i = 0; i < transientCount; i++) {
            RenderTarget t = host.ensureTarget("native:effect:inst_" + i,
                    RenderTargetKind.SYNTHETIC_WIDGET);
            t.setOrder(RenderPhase.C1_CONTENT, "effect:inst_" + i);
        }

        Map<String, Object> probe = host.probeMap();

        // The enumerated items list is capped (0 transient slots here), so it cannot carry the phase
        // census; phaseCounts must still aggregate over ALL ordered targets.
        assertTrue(order(probe).get("truncated").equals(Boolean.TRUE));
        assertEquals(Integer.valueOf(1), phaseCounts(probe).get(RenderPhase.NATIVE_RETAINED.name()));
        assertEquals(Integer.valueOf(1), phaseCounts(probe).get(RenderPhase.C2_CONTENT.name()));
        assertEquals(Integer.valueOf(1), phaseCounts(probe).get(RenderPhase.ART_EFFECTS.name()));
        assertEquals(Integer.valueOf(transientCount),
                phaseCounts(probe).get(RenderPhase.C1_CONTENT.name()));
        // Absent phases are explicitly zero, never missing or null.
        assertEquals(Integer.valueOf(0), phaseCounts(probe).get(RenderPhase.ENTITY_CONTENT.name()));
        assertEquals(Integer.valueOf(0), phaseCounts(probe).get(RenderPhase.ART_BACKGROUND.name()));
        // Guides/bounds are an overlay, not a render-plan target, so VERIFY_GUIDES stays 0.
        assertEquals(Integer.valueOf(0), phaseCounts(probe).get(RenderPhase.VERIFY_GUIDES.name()));
        assertEquals(Integer.valueOf(transientCount + 3), order(probe).get("total"));
    }

    @Test public void phaseCountsIsIndependentOfComponentInsertionOrder() {
        RenderHost first = new RenderHost();
        first.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.order-a"), RenderTargetKind.C2_SURFACE);
        RenderTarget a = first.ensureTarget("native:retained", RenderTargetKind.SYNTHETIC_WINDOW);
        a.setOrder(RenderPhase.NATIVE_RETAINED, "native:retained");
        RenderTarget b = first.ensureTarget("overlay:effect", RenderTargetKind.OVERLAY);
        b.setOrder(RenderPhase.ART_EFFECTS, "overlay:effect");

        RenderHost second = new RenderHost();
        RenderTarget b2 = second.ensureTarget("overlay:effect", RenderTargetKind.OVERLAY);
        b2.setOrder(RenderPhase.ART_EFFECTS, "overlay:effect");
        RenderTarget a2 = second.ensureTarget("native:retained", RenderTargetKind.SYNTHETIC_WINDOW);
        a2.setOrder(RenderPhase.NATIVE_RETAINED, "native:retained");
        second.ensureTarget(RenderHost.c2SurfaceTargetId("sts1.order-a"), RenderTargetKind.C2_SURFACE);

        Map<String, Object> firstCounts = phaseCounts(first.probeMap());
        Map<String, Object> secondCounts = phaseCounts(second.probeMap());
        assertEquals(firstCounts, secondCounts);
        assertEquals(phaseCounts(first.probeMap()), firstCounts);
        assertEquals(Integer.valueOf(1), firstCounts.get(RenderPhase.NATIVE_RETAINED.name()));
        assertEquals(Integer.valueOf(0), firstCounts.get(RenderPhase.C1_CONTENT.name()));
        assertEquals(Integer.valueOf(1), firstCounts.get(RenderPhase.C2_CONTENT.name()));
        assertEquals(Integer.valueOf(1), firstCounts.get(RenderPhase.ART_EFFECTS.name()));
    }

    @Test public void resetForTestsRestoresDefaultCap() {
        RenderHost host = new RenderHost();
        host.setMaxProbeEffectTargets(1);
        assertEquals(1, host.maxProbeEffectTargets());

        host.resetForTests();

        assertEquals(RenderHost.DEFAULT_MAX_PROBE_EFFECT_TARGETS,
                host.maxProbeEffectTargets());
        // Capacity across reset boundaries is the one field a prior run could leak.
        RenderHost fresh = new RenderHost();
        assertEquals(RenderHost.DEFAULT_MAX_PROBE_EFFECT_TARGETS,
                fresh.maxProbeEffectTargets());
    }

    private static boolean containsId(List<Map<String, Object>> items, String id) {
        for (Map<String, Object> item : items) {
            if (id.equals(item.get("id"))) {
                return true;
            }
        }
        return false;
    }
}

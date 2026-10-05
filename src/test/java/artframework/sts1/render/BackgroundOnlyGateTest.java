package artframework.sts1.render;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BackgroundOnlyGateTest {

    @After
    public void tearDown() {
        BackgroundOnlyGate.resetForTests();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Long> owners(Map<String, Object> slice) {
        return (Map<String, Long>) slice.get("uncoveredByOwner");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Long> blockedOwners(Map<String, Object> slice) {
        return (Map<String, Long>) slice.get("blockedByOwner");
    }

    @Test
    public void recordUncoveredAccumulatesPerOwnerAndDistinct() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("art.post_render");
        BackgroundOnlyGate.recordUncovered("art.post_render");
        BackgroundOnlyGate.recordUncovered("art.surface_renderer");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        Map<String, Long> byOwner = owners(slice);
        assertEquals(Long.valueOf(2L), byOwner.get("art.post_render"));
        assertEquals(Long.valueOf(1L), byOwner.get("art.surface_renderer"));
        assertEquals(Long.valueOf(2L), slice.get("uncoveredDistinct"));
        assertEquals(Long.valueOf(3L), slice.get("uncovered"));
        assertEquals(Long.valueOf(0L), slice.get("uncoveredOverflow"));
        assertEquals("art.surface_renderer", slice.get("lastReason"));
    }

    @Test
    public void uncoveredEqualsSumOfOwnerCountsUnderCap() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("a");
        BackgroundOnlyGate.recordUncovered("b");
        BackgroundOnlyGate.recordUncovered("a");
        BackgroundOnlyGate.recordUncovered("b");
        BackgroundOnlyGate.recordUncovered("c");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        long sum = 0L;
        for (Long v : owners(slice).values()) sum += v.longValue();
        assertEquals(((Number) slice.get("uncovered")).longValue(), sum);
    }

    @Test
    public void distinctLabelsAreBoundedAndOverflowCounts() {
        BackgroundOnlyGate.setActive(true);
        int cap = BackgroundOnlyGate.UNCOVERED_OWNER_CAP;
        int distinct = cap + 5;
        for (int i = 0; i < distinct; i++) {
            BackgroundOnlyGate.recordUncovered("owner_" + i);
        }

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        Map<String, Long> byOwner = owners(slice);
        assertEquals((long) cap, byOwner.size());
        assertEquals((long) cap, ((Number) slice.get("uncoveredDistinct")).longValue());
        assertEquals(5L, ((Number) slice.get("uncoveredOverflow")).longValue());
        // uncovered still counts every call, including overflowed owners.
        assertEquals((long) distinct, ((Number) slice.get("uncovered")).longValue());
        // Existing overflowed label increments overflow, not a new key.
        BackgroundOnlyGate.recordUncovered("owner_" + (cap + 2));
        slice = BackgroundOnlyGate.probeSlice();
        assertEquals((long) cap, owners(slice).size());
        assertEquals(6L, ((Number) slice.get("uncoveredOverflow")).longValue());
        assertEquals((long) (distinct + 1), ((Number) slice.get("uncovered")).longValue());
    }

    @Test
    public void ownerOrderIsInsertionOrderAndRepeatsIncrement() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("first");
        BackgroundOnlyGate.recordUncovered("second");
        BackgroundOnlyGate.recordUncovered("first");

        Map<String, Long> byOwner = owners(BackgroundOnlyGate.probeSlice());
        List<String> order = new ArrayList<String>(byOwner.keySet());
        assertEquals(2, order.size());
        assertEquals("first", order.get(0));
        assertEquals("second", order.get(1));
        assertEquals(Long.valueOf(2L), byOwner.get("first"));
        assertEquals(Long.valueOf(1L), byOwner.get("second"));
    }

    @Test
    public void nullAndBlankReasonsUseUncoveredFallbackLabel() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered(null);
        BackgroundOnlyGate.recordUncovered("   ");

        Map<String, Long> byOwner = owners(BackgroundOnlyGate.probeSlice());
        assertEquals(Long.valueOf(2L), byOwner.get("uncovered"));
    }

    @Test
    public void probeSliceExposesNewKeysWithCorrectTypesAndKeepsExisting() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBackgroundDraw();
        BackgroundOnlyGate.recordBackgroundSuppression();
        BackgroundOnlyGate.recordUnsupported("surface:bridge_error");
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordUncovered("art.post_render");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertEquals(Boolean.TRUE, slice.get("active"));
        assertTrue(slice.get("backgroundDraw") instanceof Long);
        assertTrue(slice.get("backgroundSuppression") instanceof Long);
        assertTrue(slice.get("suppression") instanceof Long);
        assertTrue(slice.get("blockedForeground") instanceof Long);
        assertTrue(slice.get("unsupported") instanceof Long);
        assertTrue(slice.get("uncovered") instanceof Long);
        assertTrue(slice.get("revision") instanceof Long);
        assertTrue(slice.get("lastReason") instanceof String);

        assertTrue(slice.get("uncoveredByOwner") instanceof Map);
        assertTrue(slice.get("uncoveredDistinct") instanceof Long);
        assertTrue(slice.get("uncoveredOverflow") instanceof Long);
    }

    @Test
    public void probeSliceReturnsCopyNotLiveMap() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("art.post_render");
        Map<String, Long> first = owners(BackgroundOnlyGate.probeSlice());
        first.put("injected", Long.valueOf(99L));
        Map<String, Long> second = owners(BackgroundOnlyGate.probeSlice());
        assertFalse(second.containsKey("injected"));
        assertNotNull(second.get("art.post_render"));
    }

    @Test
    public void resetForTestsClearsOwnersAndOverflow() {
        BackgroundOnlyGate.setActive(true);
        for (int i = 0; i < BackgroundOnlyGate.UNCOVERED_OWNER_CAP + 3; i++) {
            BackgroundOnlyGate.recordUncovered("owner_" + i);
        }
        BackgroundOnlyGate.resetForTests();

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(owners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("uncoveredDistinct"));
        assertEquals(Long.valueOf(0L), slice.get("uncoveredOverflow"));
        assertEquals(Long.valueOf(0L), slice.get("uncovered"));
    }

    @Test
    public void clearForRecoveryClearsOwnersAndOverflow() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("art.post_render");
        BackgroundOnlyGate.recordUncovered("art.surface_renderer");
        BackgroundOnlyGate.clearForRecovery();

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(owners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("uncoveredOverflow"));
        assertEquals(Long.valueOf(0L), slice.get("uncovered"));
        assertEquals(Boolean.FALSE, slice.get("active"));
    }

    @Test
    public void deactivationClearsAttributionViaCountersClear() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordUncovered("art.post_render");
        BackgroundOnlyGate.setActive(false);

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(owners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("uncovered"));
        assertEquals(Long.valueOf(0L), slice.get("uncoveredOverflow"));
    }

    @Test
    public void inactiveGateDoesNotRecordAttribution() {
        BackgroundOnlyGate.setActive(false);
        BackgroundOnlyGate.recordUncovered("art.post_render");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(owners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("uncovered"));
    }

    @Test
    public void existingGateSemanticsAndCountersUnchanged() {
        BackgroundOnlyGate.resetForTests();
        assertFalse(BackgroundOnlyGate.isActive());

        BackgroundOnlyGate.recordBackgroundDraw();
        assertEquals(Long.valueOf(0L), BackgroundOnlyGate.probeSlice().get("backgroundDraw"));

        BackgroundOnlyGate.setActive(true);
        assertTrue(BackgroundOnlyGate.isActive());
        BackgroundOnlyGate.recordBackgroundDraw();
        BackgroundOnlyGate.recordBackgroundDraw();
        BackgroundOnlyGate.recordBackgroundSuppression();
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordUnsupported("surface:bridge_error");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertEquals(Long.valueOf(2L), slice.get("backgroundDraw"));
        assertEquals(Long.valueOf(1L), slice.get("backgroundSuppression"));
        assertEquals(Long.valueOf(1L), slice.get("suppression"));
        assertEquals(Long.valueOf(1L), slice.get("blockedForeground"));
        assertEquals(Long.valueOf(1L), slice.get("unsupported"));
        // unsupported remains a counter (attributed in a later slice); blocked is now attributed.
        assertEquals("surface:bridge_error", slice.get("lastReason"));
        // revision increments on activation.
        assertEquals(Long.valueOf(1L), slice.get("revision"));
    }

    @Test
    public void recordBlockedAccumulatesPerLabelAndDistinct() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordBlocked("skeleton:Creature");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        Map<String, Long> byOwner = blockedOwners(slice);
        assertEquals(Long.valueOf(2L), byOwner.get("surface:hand"));
        assertEquals(Long.valueOf(1L), byOwner.get("skeleton:Creature"));
        assertEquals(Long.valueOf(2L), slice.get("blockedDistinct"));
        assertEquals(Long.valueOf(3L), slice.get("blockedForeground"));
        assertEquals(Long.valueOf(0L), slice.get("blockedOverflow"));
        assertEquals("skeleton:Creature", slice.get("lastReason"));
    }

    @Test
    public void blockedForegroundEqualsSumOfOwnerCountsUnderCap() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("effect:A");
        BackgroundOnlyGate.recordBlocked("effect:B");
        BackgroundOnlyGate.recordBlocked("effect:A");
        BackgroundOnlyGate.recordBlocked("effect:B");
        BackgroundOnlyGate.recordBlocked("effect:C");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        long sum = 0L;
        for (Long v : blockedOwners(slice).values()) sum += v.longValue();
        assertEquals(((Number) slice.get("blockedForeground")).longValue(), sum);
    }

    @Test
    public void blockedDistinctLabelsAreBoundedAndOverflowCounts() {
        BackgroundOnlyGate.setActive(true);
        int cap = BackgroundOnlyGate.BLOCKED_OWNER_CAP;
        int distinct = cap + 5;
        for (int i = 0; i < distinct; i++) {
            BackgroundOnlyGate.recordBlocked("owner_" + i);
        }

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        Map<String, Long> byOwner = blockedOwners(slice);
        assertEquals((long) cap, byOwner.size());
        assertEquals((long) cap, ((Number) slice.get("blockedDistinct")).longValue());
        assertEquals(5L, ((Number) slice.get("blockedOverflow")).longValue());
        // blockedForeground still counts every call, including overflowed owners.
        assertEquals((long) distinct, ((Number) slice.get("blockedForeground")).longValue());
        // Existing overflowed label increments overflow, not a new key.
        BackgroundOnlyGate.recordBlocked("owner_" + (cap + 2));
        slice = BackgroundOnlyGate.probeSlice();
        assertEquals((long) cap, blockedOwners(slice).size());
        assertEquals(6L, ((Number) slice.get("blockedOverflow")).longValue());
        assertEquals((long) (distinct + 1), ((Number) slice.get("blockedForeground")).longValue());
    }

    @Test
    public void blockedOwnerOrderIsInsertionOrderAndRepeatsIncrement() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("first");
        BackgroundOnlyGate.recordBlocked("second");
        BackgroundOnlyGate.recordBlocked("first");

        Map<String, Long> byOwner = blockedOwners(BackgroundOnlyGate.probeSlice());
        List<String> order = new ArrayList<String>(byOwner.keySet());
        assertEquals(2, order.size());
        assertEquals("first", order.get(0));
        assertEquals("second", order.get(1));
        assertEquals(Long.valueOf(2L), byOwner.get("first"));
        assertEquals(Long.valueOf(1L), byOwner.get("second"));
    }

    @Test
    public void nullAndBlankBlockedReasonsUseBlockedFallbackLabel() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked(null);
        BackgroundOnlyGate.recordBlocked("   ");

        Map<String, Long> byOwner = blockedOwners(BackgroundOnlyGate.probeSlice());
        assertEquals(Long.valueOf(2L), byOwner.get("blocked"));
        assertEquals(1L, ((Number) BackgroundOnlyGate.probeSlice().get("blockedDistinct")).longValue());
    }

    @Test
    public void blockedLabelsAreTrimmed() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("  surface:hand  ");
        BackgroundOnlyGate.recordBlocked("surface:hand");

        Map<String, Long> byOwner = blockedOwners(BackgroundOnlyGate.probeSlice());
        assertEquals(1, byOwner.size());
        assertEquals(Long.valueOf(2L), byOwner.get("surface:hand"));
    }

    @Test
    public void probeSliceExposesBlockedKeysWithCorrectTypesAndKeepsE01AndExisting() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBackgroundDraw();
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordUncovered("art.post_render");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(slice.get("blockedByOwner") instanceof Map);
        assertTrue(slice.get("blockedDistinct") instanceof Long);
        assertTrue(slice.get("blockedOverflow") instanceof Long);
        // E01 keys remain.
        assertTrue(slice.get("uncoveredByOwner") instanceof Map);
        assertTrue(slice.get("uncoveredDistinct") instanceof Long);
        assertTrue(slice.get("uncoveredOverflow") instanceof Long);
        // Pre-existing keys remain.
        assertTrue(slice.get("blockedForeground") instanceof Long);
        assertTrue(slice.get("uncovered") instanceof Long);
        assertTrue(slice.get("suppression") instanceof Long);
        assertTrue(slice.get("lastReason") instanceof String);
    }

    @Test
    public void blockedProbeSliceReturnsCopyNotLiveMap() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("surface:hand");
        Map<String, Long> first = blockedOwners(BackgroundOnlyGate.probeSlice());
        first.put("injected", Long.valueOf(99L));
        Map<String, Long> second = blockedOwners(BackgroundOnlyGate.probeSlice());
        assertFalse(second.containsKey("injected"));
        assertNotNull(second.get("surface:hand"));
    }

    @Test
    public void resetForTestsClearsBlockedAttribution() {
        BackgroundOnlyGate.setActive(true);
        for (int i = 0; i < BackgroundOnlyGate.BLOCKED_OWNER_CAP + 3; i++) {
            BackgroundOnlyGate.recordBlocked("owner_" + i);
        }
        BackgroundOnlyGate.resetForTests();

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(blockedOwners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("blockedDistinct"));
        assertEquals(Long.valueOf(0L), slice.get("blockedOverflow"));
        assertEquals(Long.valueOf(0L), slice.get("blockedForeground"));
    }

    @Test
    public void clearForRecoveryClearsBlockedAttribution() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.recordBlocked("effect:A");
        BackgroundOnlyGate.clearForRecovery();

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(blockedOwners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("blockedOverflow"));
        assertEquals(Long.valueOf(0L), slice.get("blockedForeground"));
        assertEquals(Boolean.FALSE, slice.get("active"));
    }

    @Test
    public void deactivationClearsBlockedAttributionViaCountersClear() {
        BackgroundOnlyGate.setActive(true);
        BackgroundOnlyGate.recordBlocked("surface:hand");
        BackgroundOnlyGate.setActive(false);

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(blockedOwners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("blockedForeground"));
        assertEquals(Long.valueOf(0L), slice.get("blockedOverflow"));
    }

    @Test
    public void inactiveGateDoesNotRecordBlockedAttribution() {
        BackgroundOnlyGate.setActive(false);
        BackgroundOnlyGate.recordBlocked("surface:hand");

        Map<String, Object> slice = BackgroundOnlyGate.probeSlice();
        assertTrue(blockedOwners(slice).isEmpty());
        assertEquals(Long.valueOf(0L), slice.get("blockedForeground"));
    }
}

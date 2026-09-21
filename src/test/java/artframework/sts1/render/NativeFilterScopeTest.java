package artframework.sts1.render;

import artframework.context.SurfaceIds;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeFilterScopeTest {

    @Test
    public void inactiveScopeNeverBlocksDelegation() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.filter(SurfaceIds.COMBAT_INTENTS);

        assertFalse(scope.isActive());
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void activeScopeBlocksOnlyFilteredAndUnselectedFamilies() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.filter(SurfaceIds.COMBAT_INTENTS);
        scope.select(SurfaceIds.COMBAT_HAND);

        assertTrue(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_HAND));
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_CONTROLS));
    }

    @Test
    public void filteredFamilyThatIsAlsoSelectedIsNotBlocked() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.filterAll(new java.util.LinkedHashSet<String>(
                Arrays.asList(SurfaceIds.COMBAT_INTENTS, SurfaceIds.COMBAT_HAND)));
        scope.select(SurfaceIds.COMBAT_HAND);

        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_HAND));
        assertTrue(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void nullAndBlankFamiliesNeverBlock() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.filter("  ");

        assertFalse(scope.blocksDelegation(null));
        assertFalse(scope.blocksDelegation(""));
        assertFalse(scope.blocksDelegation("   "));
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
    }

    @Test
    public void unfilterRemovesOneFamilyAndDeactivatesWhenEmpty() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.filter(SurfaceIds.COMBAT_INTENTS);
        scope.filter(SurfaceIds.COMBAT_HAND);

        scope.unfilter(SurfaceIds.COMBAT_INTENTS);
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
        assertTrue(scope.blocksDelegation(SurfaceIds.COMBAT_HAND));
        assertTrue(scope.isActive());

        scope.unfilter(SurfaceIds.COMBAT_HAND);
        assertFalse(scope.isActive());
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void clearRemovesSelectionAndFilters() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.filter(SurfaceIds.COMBAT_INTENTS);
        scope.select(SurfaceIds.COMBAT_HAND);

        scope.clear();

        assertFalse(scope.isActive());
        assertFalse(scope.blocksDelegation(SurfaceIds.COMBAT_INTENTS));
        Map<String, Object> probe = scope.probeSlice();
        assertEquals(Boolean.FALSE, probe.get("active"));
        assertTrue(((java.util.List<?>) probe.get("filteredFamilies")).isEmpty());
        assertTrue(((java.util.List<?>) probe.get("selectedFamilies")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void probeSliceExposesActiveSelectionAndFilters() {
        NativeFilterScope scope = new NativeFilterScope();
        scope.activate();
        scope.select(SurfaceIds.COMBAT_HAND);
        scope.filter(SurfaceIds.COMBAT_INTENTS);

        Map<String, Object> probe = scope.probeSlice();

        assertEquals(Boolean.TRUE, probe.get("active"));
        assertTrue(((java.util.List<String>) probe.get("selectedFamilies"))
                .contains(SurfaceIds.COMBAT_HAND));
        assertTrue(((java.util.List<String>) probe.get("filteredFamilies"))
                .contains(SurfaceIds.COMBAT_INTENTS));
    }
}

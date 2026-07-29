package artframework.sts1;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullPresentModeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void defaultsOffNeverSuppress() {
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        assertFalse(FullPresentMode.isCombatHandEnabled());
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void observeDoesNotSuppressOrOwnInput() {
        FullPresentMode.setCombatHandLevel(PresentLevel.OBSERVE);
        assertTrue(FullPresentMode.combatHandLevel().allowsObserve());
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.isCombatHandEnabled());
    }

    @Test
    public void fullIsOnlyARequestedLevelUntilCapabilityIsReady() {
        FullPresentMode.setCombatHandEnabled(true);
        assertEquals(PresentLevel.FULL, FullPresentMode.combatHandLevel());
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_CARD_SLOTS));
    }

    @Test
    public void perSurfaceLevelsIndependent() {
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        FullPresentMode.setCombatControlsLevel(PresentLevel.OBSERVE);
        assertEquals(PresentLevel.FULL, FullPresentMode.levelOf(SurfaceIds.MAP));
        assertEquals(PresentLevel.OBSERVE, FullPresentMode.levelOf(SurfaceIds.COMBAT_CONTROLS));
        assertEquals(PresentLevel.OFF, FullPresentMode.levelOf(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.MAP));
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_CONTROLS));
    }

    @Test
    public void parseAliases() {
        assertEquals(PresentLevel.FULL, PresentLevel.parse("on"));
        assertEquals(PresentLevel.FULL, PresentLevel.parse("FULL"));
        assertEquals(PresentLevel.OBSERVE, PresentLevel.parse("observe"));
        assertEquals(PresentLevel.OFF, PresentLevel.parse("off"));
        assertEquals(PresentLevel.OFF, PresentLevel.parse("nope"));
    }

    @Test
    public void probeSliceListsLevels() {
        FullPresentMode.setCombatHandLevel(PresentLevel.OBSERVE);
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        Map<String, Object> m = FullPresentMode.probeSlice();
        assertEquals("OBSERVE", m.get("combatHand"));
        assertEquals("FULL", m.get("map"));
        assertEquals(Boolean.FALSE, m.get("combatHandFull"));
        assertEquals(Boolean.FALSE, m.get("maySuppressNativeHand"));
    }
}

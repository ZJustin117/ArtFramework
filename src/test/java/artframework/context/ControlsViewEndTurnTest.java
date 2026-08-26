package artframework.context;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** G1: end-turn hitbox geometry and localized label are backend-projected view fields. */
public class ControlsViewEndTurnTest {

    @Test
    public void legacyFactoriesProjectNoGeometry() {
        ControlsView empty = ControlsView.empty();
        assertFalse(empty.hasEndTurnBounds());
        assertEquals(0f, empty.endTurnX, 0f);
        assertEquals(0f, empty.endTurnY, 0f);
        assertEquals(0f, empty.endTurnW, 0f);
        assertEquals(0f, empty.endTurnH, 0f);
        assertEquals("", empty.endTurnLabel);

        ControlsView combat = ControlsView.combat(3, 5, 10, 2, 1, true, true);
        assertFalse(combat.hasEndTurnBounds());
        assertEquals("", combat.endTurnLabel);

        ControlsView proceed =
                ControlsView.combatWithProceed(3, 5, 10, 2, 1, true, true, true, true, false, false);
        assertFalse(proceed.hasEndTurnBounds());
        assertEquals("", proceed.endTurnLabel);
    }

    @Test
    public void combatOverloadCarriesProjectedGeometryAndLabel() {
        ControlsView cv =
                ControlsView.combat(3, 5, 10, 2, 1, true, true, 100f, 200f, 300f, 80f, "结束回合");
        assertTrue(cv.hasEndTurnBounds());
        assertEquals(100f, cv.endTurnX, 0f);
        assertEquals(200f, cv.endTurnY, 0f);
        assertEquals(300f, cv.endTurnW, 0f);
        assertEquals(80f, cv.endTurnH, 0f);
        assertEquals("结束回合", cv.endTurnLabel);
        assertEquals("结束回合", cv.find(ControlsView.END_TURN_ID).text);
    }

    @Test
    public void combatWithProceedOverloadCarriesProjectedGeometryAndLabel() {
        ControlsView cv =
                ControlsView.combatWithProceed(
                        3, 5, 10, 2, 1, true, true, true, true, false, false,
                        12.5f, 34.5f, 156f, 62f, "End Turn!");
        assertTrue(cv.hasEndTurnBounds());
        assertEquals(12.5f, cv.endTurnX, 0f);
        assertEquals(34.5f, cv.endTurnY, 0f);
        assertEquals(156f, cv.endTurnW, 0f);
        assertEquals(62f, cv.endTurnH, 0f);
        assertEquals("End Turn!", cv.endTurnLabel);
        assertTrue(cv.proceedVisible);
    }

    @Test
    public void nullLabelAndZeroSizedGeometryNormalizeToAbsent() {
        ControlsView cv = ControlsView.combat(1, 1, 0, 0, 0, true, true, 4f, 5f, 0f, 9f, null);
        assertFalse("zero width is not a usable hitbox", cv.hasEndTurnBounds());
        assertEquals("", cv.endTurnLabel);
    }

    @Test
    public void toMapExposesEndTurnGeometryAndLabel() {
        ControlsView cv =
                ControlsView.combat(2, 4, 8, 0, 0, true, true, 7f, 8f, 9f, 10f, "End Turn");
        Map<String, Object> m = cv.toMap();
        assertEquals(Float.valueOf(7f), m.get("endTurnX"));
        assertEquals(Float.valueOf(8f), m.get("endTurnY"));
        assertEquals(Float.valueOf(9f), m.get("endTurnW"));
        assertEquals(Float.valueOf(10f), m.get("endTurnH"));
        assertEquals("End Turn", m.get("endTurnLabel"));
    }
}

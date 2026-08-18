package artframework.sts1;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.render.MapDrawPath;
import artframework.sts1.render.Sts1RenderPipeline;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PresentSafetyTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void panicClearsLevelsAndSuppress() {
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        FullPresentMode.setEventLevel(PresentLevel.FULL);
        FullPresentMode.setSelectLevel(PresentLevel.FULL);
        CombatInputRouter.setSuppressNativeInput(true);
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        ArtFramework.component(SurfaceIds.MAP).mount();
        ArtFramework.component(SurfaceIds.EVENT).action("mount_event");
        ArtFramework.component(SurfaceIds.SELECT_GRID).action("mount_select");

        PresentSafety.panic("test");
        assertTrue(PresentSafety.isPanic());
        assertEquals("test", PresentSafety.panicReason());
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.mapLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.eventLevel());
        assertEquals(PresentLevel.OFF, FullPresentMode.selectLevel());
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        assertFalse(ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted());
        assertFalse(ArtFramework.component(SurfaceIds.EVENT).isMounted());
        assertFalse(ArtFramework.component(SurfaceIds.SELECT_GRID).isMounted());
        assertFalse(CombatInputRouter.shouldSuppressNativeInput());
    }

    @Test
    public void clearPanicAllowsLevelsAgain() {
        PresentSafety.panic("x");
        PresentSafety.clearPanic();
        assertFalse(PresentSafety.isPanic());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void hostRecreateIncrements() {
        int before = PresentSafety.recreationCount();
        Sts1RenderPipeline.setOverlayObserve(true);
        MapDrawPath.panZoom().setPan(5f, 5f);
        PresentSafety.onHostRecreated();
        assertEquals(before + 1, PresentSafety.recreationCount());
        assertFalse(Sts1RenderPipeline.isOverlayObserve());
        assertEquals(0f, MapDrawPath.panZoom().panX(), 0.01f);
        assertTrue(String.valueOf(PresentSafety.probeSlice().get("c1HostRecreation")).length() > 0);
    }

    @Test
    public void lifecycleMatrixPanicClearAndCapabilityFallback() {
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new artframework.sts1.input.RecordingIntentExecutor());
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        // Without combat scene, FULL stays fallback-native (19.1/19.4).
        assertFalse(FullPresentMode.mayOwnInput(SurfaceIds.COMBAT_HAND));
        PresentSafety.panic("lifecycle");
        assertTrue(PresentSafety.isPanic());
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        assertFalse(ArtFramework.component(SurfaceIds.COMBAT_HAND).isMounted());
        PresentSafety.clearPanic();
        assertFalse(PresentSafety.isPanic());
        // clear-panic alone does not re-arm FULL.
        assertEquals(PresentLevel.OFF, FullPresentMode.combatHandLevel());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        assertFalse(FullPresentMode.maySuppressNative(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void probeSlice() {
        PresentSafety.panic("p");
        Map<String, Object> m = PresentSafety.probeSlice();
        assertEquals(Boolean.TRUE, m.get("panic"));
        assertEquals("p", m.get("panicReason"));
    }
}

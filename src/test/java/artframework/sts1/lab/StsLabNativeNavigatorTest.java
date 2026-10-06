package artframework.sts1.lab;

import artframework.core.SignalDecision;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Headless coverage for the {@code art lab enter-select} hardening. The app-thread posting path
 * cannot initialize STS/AbstractDungeon headlessly, so the pre-schedule gate is exercised as a
 * pure seam and the app-thread wrapper is exercised directly.
 */
public class StsLabNativeNavigatorTest {

    @After
    public void tearDown() {
        StsLabNav.resetForTests();
    }

    @Test
    public void gridAlreadyOpenStopsHandledAndDoesNotSchedule() {
        // Deck is deliberately empty: the already-open guard must win over the empty-deck check,
        // which is what keeps the command from reaching a nested open() (the NPE source).
        SignalDecision d = StsLabNativeNavigator.enterSelectGate(true, "GRID", "grid", 0, 0);
        assertNotNull(d);
        assertEquals(SignalDecision.Kind.STOP_HANDLED, d.kind);
        assertEquals("already selecting", d.message);
    }

    @Test
    public void handAlreadyOpenStopsHandled() {
        SignalDecision d = StsLabNativeNavigator.enterSelectGate(true, "HAND_SELECT", "hand", 0, 0);
        assertNotNull(d);
        assertEquals(SignalDecision.Kind.STOP_HANDLED, d.kind);
        assertEquals("already selecting", d.message);
    }

    @Test
    public void cleanStateAllowsScheduling() {
        assertNull(StsLabNativeNavigator.enterSelectGate(true, "NONE", "grid", 5, 0));
        assertNull(StsLabNativeNavigator.enterSelectGate(true, null, "hand", 0, 5));
    }

    @Test
    public void gridSelectIsNotConfusedWithHandSelect() {
        // A hand select does not block a grid request and vice versa.
        assertNull(StsLabNativeNavigator.enterSelectGate(true, "HAND_SELECT", "grid", 5, 3));
        assertNull(StsLabNativeNavigator.enterSelectGate(true, "GRID", "hand", 5, 3));
    }

    @Test
    public void emptyDeckAndHandAreRejected() {
        SignalDecision grid = StsLabNativeNavigator.enterSelectGate(true, "NONE", "grid", 0, 5);
        assertEquals(SignalDecision.Kind.STOP_REJECTED, grid.kind);
        assertEquals("master deck is empty", grid.message);
        SignalDecision hand = StsLabNativeNavigator.enterSelectGate(true, "NONE", "hand", 5, 0);
        assertEquals(SignalDecision.Kind.STOP_REJECTED, hand.kind);
        assertEquals("hand is empty", hand.message);
    }

    @Test
    public void missingPlayerRejects() {
        SignalDecision d = StsLabNativeNavigator.enterSelectGate(false, "NONE", "grid", 5, 5);
        assertEquals(SignalDecision.Kind.STOP_REJECTED, d.kind);
        assertEquals("run not ready", d.message);
    }

    @Test
    public void selectScreenOpenIsPureAndExact() {
        assertTrue(StsLabNativeNavigator.selectScreenOpen("GRID", "grid"));
        assertTrue(StsLabNativeNavigator.selectScreenOpen("HAND_SELECT", "hand"));
        assertFalse(StsLabNativeNavigator.selectScreenOpen("NONE", "grid"));
        assertFalse(StsLabNativeNavigator.selectScreenOpen("HAND_SELECT", "grid"));
        assertFalse(StsLabNativeNavigator.selectScreenOpen(null, "grid"));
    }

    @Test
    public void postedRunnableFailureIsRecordedNotPropagated() {
        LabNavigationSignals.resetErrorsForTests();
        final boolean[] ran = {false};
        // Gdx.app is null headlessly, so the wrapper runs inline; the throw must not escape.
        StsLabNativeNavigator.postObserved(
                "test ctx",
                new Runnable() {
                    @Override
                    public void run() {
                        ran[0] = true;
                        throw new IllegalStateException("boom");
                    }
                });
        assertTrue(ran[0]);
        assertEquals(1, LabNavigationSignals.errorCount());
        String error = LabNavigationSignals.lastError();
        assertTrue(error, error.contains("test ctx"));
        assertTrue(error, error.contains("IllegalStateException"));
        assertTrue(error, error.contains("boom"));
    }

    @Test
    public void postedRunnableSuccessDoesNotRecord() {
        LabNavigationSignals.resetErrorsForTests();
        final boolean[] ran = {false};
        StsLabNativeNavigator.postObserved(
                "ok ctx",
                new Runnable() {
                    @Override
                    public void run() {
                        ran[0] = true;
                    }
                });
        assertTrue(ran[0]);
        assertEquals(0, LabNavigationSignals.errorCount());
        assertEquals("", LabNavigationSignals.lastError());
    }

    @Test
    public void recordedErrorIsObservableInLabStatus() {
        LabNavigationSignals.resetErrorsForTests();
        LabNavigationSignals.recordError("enter-select grid", new NullPointerException("npe"));
        assertEquals(Integer.valueOf(1), LabRecipeRunner.statusMap().get("navErrorCount"));
        String reported = String.valueOf(LabRecipeRunner.statusMap().get("lastNavError"));
        assertTrue(reported, reported.contains("NullPointerException"));
        assertTrue(reported, reported.contains("npe"));
        assertTrue(reported, reported.contains("enter-select grid"));
    }

    @Test
    public void resetForTestsClearsErrorChannel() {
        LabNavigationSignals.recordError("ctx", new RuntimeException("x"));
        assertEquals(1, LabNavigationSignals.errorCount());
        StsLabNav.resetForTests();
        assertEquals(0, LabNavigationSignals.errorCount());
        assertEquals("", LabNavigationSignals.lastError());
    }
}

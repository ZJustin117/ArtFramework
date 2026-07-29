package artframework.sts1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FullPresentCapabilityTest {

    @Test
    public void observeMountedKeepsNativeVisibleAndInputUnowned() {
        FullPresentCapability c = FullPresentCapability.resolve(
                PresentLevel.OBSERVE, true, true, true, false, false);
        assertEquals(FullPresentCapability.State.OBSERVING, c.state);
        assertFalse(c.shouldSuppressNative());
        assertFalse(c.ownsInput());
    }

    @Test
    public void fullMountedReadyOwnsInputAndSuppressesNative() {
        FullPresentCapability c = FullPresentCapability.resolve(
                PresentLevel.FULL, true, true, true, false, false);
        assertEquals(FullPresentCapability.State.FULL_READY, c.state);
        assertTrue(c.shouldDraw());
        assertTrue(c.shouldSuppressNative());
        assertTrue(c.ownsInput());
    }

    @Test
    public void fullWithoutExecutorFallsBackToNative() {
        FullPresentCapability c = FullPresentCapability.resolve(
                PresentLevel.FULL, true, true, false, false, false);
        assertEquals(FullPresentCapability.State.FULL_FALLBACK_NATIVE, c.state);
        assertEquals("executor_unavailable", c.reason);
        assertFalse(c.shouldDraw());
        assertFalse(c.shouldSuppressNative());
        assertFalse(c.ownsInput());
    }

    @Test
    public void panicAlwaysDisablesPresentAndInput() {
        FullPresentCapability c = FullPresentCapability.resolve(
                PresentLevel.FULL, true, true, true, false, true);
        assertEquals(FullPresentCapability.State.OFF, c.state);
        assertEquals("panic", c.reason);
        assertFalse(c.shouldSuppressNative());
        assertFalse(c.ownsInput());
    }
}

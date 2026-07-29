package artframework.c2.hooks;

import artframework.core.SignalDispatchResult;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostPatchResultsTest {

    @Test
    public void nullAndContinueAllowNative() {
        assertTrue(HostPatchResults.allowsNative(null));
        assertTrue(HostPatchResults.allowsNative(SignalDispatchResult.continueEmpty("ok")));
    }

    @Test
    public void rejectedBlocksNative() {
        assertFalse(HostPatchResults.allowsNative(SignalDispatchResult.rejected("blocked")));
    }
}

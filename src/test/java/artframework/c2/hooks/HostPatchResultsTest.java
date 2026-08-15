package artframework.c2.hooks;

import artframework.core.SignalDispatchResult;
import artframework.core.SignalDecision;
import artframework.core.SignalGroups;
import artframework.core.SignalListener;
import artframework.core.UiSignal;
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

    @Test
    public void handledNativeOperationAlsoBlocksNative() {
        SignalGroups.resetForTests();
        SignalGroups.nativeGroup().connect("native/op", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopHandled("owned");
            }
        });
        SignalDispatchResult result = SignalGroups.nativeGroup().emit(
                new UiSignal("native/op", "test", null));
        assertFalse(HostPatchResults.allowsNative(result));
    }
}

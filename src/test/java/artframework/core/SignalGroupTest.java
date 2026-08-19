package artframework.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignalGroupTest {
    @Test
    public void groupsDoNotReceiveEachOthersOperations() {
        SignalGroups.resetForTests();
        final int[] seen = new int[1];
        SignalGroup nativeGroup = SignalGroups.nativeGroup();
        SignalGroup other = SignalGroups.get("other");
        nativeGroup.connect("op", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                seen[0]++;
                return SignalDecision.stopHandled("consumed");
            }
        });

        SignalDispatchResult result = other.dispatch(new UiSignal("other", "id", "op", "source", null,
                java.util.Collections.<String, Object>emptyMap()));
        assertEquals(0, seen[0]);
        assertTrue(!result.isStopped());
    }

    @Test(expected = IllegalArgumentException.class)
    public void groupRejectsSignalFromAnotherGroup() {
        SignalGroups.get("other").emit(new UiSignal("native", "id", "op", "source", null,
                java.util.Collections.<String, Object>emptyMap()));
    }
}

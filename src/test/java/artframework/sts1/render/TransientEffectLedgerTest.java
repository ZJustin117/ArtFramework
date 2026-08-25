package artframework.sts1.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransientEffectLedgerTest {
    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", 1, 1L);
    }

    @Test
    public void completionRemovesEffectFromActiveLifecycle() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.update(identity("effect"), true);

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(TransientEffectLedger.State.COMPLETED,
                ledger.records().get(0).state);
    }

    @Test(expected = IllegalStateException.class)
    public void updateAfterCompletionIsRejected() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.complete(identity("effect"));
        ledger.update(identity("effect"), false);
    }
}

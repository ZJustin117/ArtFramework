package artframework.sts1.render;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransientEffectLedgerTest {
    private TransientEffectIdentity identity(String id) {
        return new TransientEffectIdentity(id, "native.Effect", 1, 1L);
    }

    @Test
    public void completionReclaimsActiveRecordIntoRecentDiagnostics() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.update(identity("effect"), true);

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
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

    @Test(expected = IllegalStateException.class)
    public void completeAfterCompletionIsRejectedEvenAfterActiveReclamation() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.complete(identity("effect"));
        ledger.complete(identity("effect"));
    }

    @Test(expected = IllegalStateException.class)
    public void disposeAfterDisposeIsRejectedEvenAfterActiveReclamation() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.dispose(identity("effect"));
        ledger.dispose(identity("effect"));
    }

    @Test
    public void completeThenDisposePreservesExistingTerminationSemantics() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        ledger.create(identity("effect"));
        ledger.complete(identity("effect"));
        ledger.dispose(identity("effect"));

        assertEquals(TransientEffectLedger.State.DISPOSED,
                ledger.records().get(0).state);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void terminalDiagnosticsAreBoundedAndEvictionDoesNotChangeTotals() {
        TransientEffectLedger ledger = new TransientEffectLedger(3);
        for (int i = 0; i < 100; i++) {
            String id = "effect-" + i;
            ledger.create(identity(id));
            ledger.complete(identity(id));
        }

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(3), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(100), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(97), Integer.valueOf(ledger.evictedCount()));
        assertEquals(Integer.valueOf(3), Integer.valueOf(ledger.records().size()));
        assertFalse(ledger.records().get(0).identity.instanceId.equals("effect-0"));
        assertEquals(Integer.valueOf(100), ledger.probeSlice().get("total"));
        assertEquals(Integer.valueOf(97), ledger.probeSlice().get("evicted"));
    }

    @Test
    public void lateCallbacksForEvictedTerminalRecordsDoNotRecreateState() {
        TransientEffectLedger ledger = new TransientEffectLedger(1);
        TransientEffectIdentity old = identity("old");
        ledger.create(old);
        ledger.complete(old);
        ledger.create(identity("new"));
        ledger.complete(identity("new"));

        ledger.update(old, false);
        ledger.complete(old);
        ledger.dispose(old);

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(3), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(Integer.valueOf(2), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void resetClearsActiveRecentAndCumulativeDiagnostics() {
        TransientEffectLedger ledger = new TransientEffectLedger(1);
        ledger.create(identity("effect"));
        ledger.complete(identity("effect"));
        ledger.reset();

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.unknownLifecycleCount()));
    }

    @Test
    public void cleanupOperationsDoNotEraseRecentOrCumulativeDiagnostics() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        ledger.create(identity("done"));
        ledger.complete(identity("done"));
        ledger.create(identity("leaked"));
        ledger.clearLeaked();

        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.leakedCount()));

        ledger.create(identity("recovery"));
        ledger.clearCompletedForRecovery();
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void lateRenderForRecentTerminalRecordIsRejectedWithoutChangingDiagnostics() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        TransientEffectIdentity identity = identity("done");
        ledger.create(identity);
        ledger.complete(identity);

        assertFalse(ledger.admitRender(identity));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(TransientEffectLedger.State.COMPLETED, ledger.records().get(0).state);
    }

    @Test
    public void explicitNewIdentityWithSameInstanceIdCanStartAfterTerminalRecord() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        TransientEffectIdentity first = identity("same");
        TransientEffectIdentity second = new TransientEffectIdentity(
                "same", "native.Effect", 2, 2L);
        ledger.create(first);
        ledger.complete(first);
        ledger.create(second);

        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));
        ledger.render(second);
        assertEquals(TransientEffectLedger.State.RENDERED,
                ledger.records().get(0).state);
        ledger.complete(second);
        assertEquals(Integer.valueOf(2), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void recoveryClearRejectsLateRenderWithBoundedStaleIdentityMarkers() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        TransientEffectIdentity identity = identity("recovery");
        ledger.create(identity);
        ledger.clearCompletedForRecovery();

        assertFalse(ledger.admitRender(identity));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.staleIdentityCount()));
    }

    @Test
    public void recordsAreImmutableSnapshotsAndCannotChangeLedgerAccounting() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        TransientEffectIdentity effect = identity("immutable");
        ledger.create(effect);
        TransientEffectLedger.Record snapshot = ledger.records().get(0);
        Map<String, Object> before = ledger.probeSlice();

        ledger.update(effect, true);

        assertEquals(TransientEffectLedger.State.CREATED, snapshot.state);
        assertEquals(Integer.valueOf(0), Integer.valueOf(snapshot.updateCount));
        assertEquals(Integer.valueOf(0), Integer.valueOf(snapshot.renderCount));
        assertFalse(snapshot.doneObserved);
        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("total"));
        assertEquals(Integer.valueOf(0), before.get("total"));
        assertTrue(ledger.records().get(0) != snapshot);
        try {
            ledger.records().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("records list must be immutable");
    }

    @Test
    public void explicitCreateCanStartSameIdAfterRecoveryWithoutUnboundedHistory() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        TransientEffectIdentity identity = identity("recovery-reused");
        ledger.create(identity);
        ledger.clearCompletedForRecovery();

        ledger.create(identity);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));
        ledger.complete(identity);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.staleIdentityCount()));
    }
}

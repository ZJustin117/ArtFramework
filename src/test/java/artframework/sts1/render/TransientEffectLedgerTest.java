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
    public void hasActiveRecordTracksCreateAdmitUpdateAndCompletion() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        TransientEffectIdentity created = identity("created");
        TransientEffectIdentity admitted = identity("admitted");
        TransientEffectIdentity unknown = identity("unknown");

        assertFalse(ledger.hasActiveRecord(created));
        assertFalse(ledger.hasActiveRecord(null));

        ledger.create(created);
        assertTrue(ledger.hasActiveRecord(created));

        ledger.render(admitted);
        assertTrue(ledger.hasActiveRecord(admitted));

        assertFalse("unrelated identity is never active", ledger.hasActiveRecord(unknown));

        ledger.update(created, true);
        assertFalse("completion leaves no active record", ledger.hasActiveRecord(created));
    }

    @Test
    public void activeRecordCreatedByCreateIsRemovedByDoneUpdateAndStaysTerminal() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        TransientEffectIdentity effect = identity("effect");
        ledger.create(effect);

        ledger.update(effect, true);

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertFalse(ledger.hasActiveRecord(effect));

        // The bridge observes a completed effect more than once per frame; a second done update
        // must be a silent no-op rather than an "update after effect termination" throw.
        ledger.update(effect, true);
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
    }

    @Test
    public void activeRecordAdmittedByRenderIsRemovedByDoneUpdate() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        TransientEffectIdentity effect = identity("render-only");
        assertTrue(ledger.admitRender(effect));
        assertTrue(ledger.hasActiveRecord(effect));

        ledger.update(effect, true);

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertFalse(ledger.hasActiveRecord(effect));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void activeCapacityEvictsOldestAndKeepsActiveSetBounded() {
        int cap = 4;
        TransientEffectLedger ledger = new TransientEffectLedger(2, cap);
        for (int i = 0; i < cap + 8; i++) {
            ledger.create(identity("active-" + i));
        }

        assertEquals("records can never exceed the active capacity",
                Integer.valueOf(cap), Integer.valueOf(ledger.activeCount()));
        assertEquals("each active-window cap eviction is counted once",
                Integer.valueOf(8), Integer.valueOf(ledger.activeEvictedCount()));
        assertEquals("active-cap eviction must not populate the terminal recent window",
                Integer.valueOf(0), Integer.valueOf(ledger.recentCount()));
        assertEquals("active-cap eviction must not advance the recent-window counter",
                Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
        assertTrue("the newest identities are retained",
                ledger.hasActiveRecord(identity("active-" + (cap + 7))));
        assertFalse("the oldest identities were evicted",
                ledger.hasActiveRecord(identity("active-0")));
        assertEquals(Integer.valueOf(cap), ledger.probeSlice().get("active"));
        assertEquals(Integer.valueOf(cap), ledger.probeSlice().get("activeCap"));
        assertEquals(Integer.valueOf(8), ledger.probeSlice().get("activeEvicted"));
    }

    @Test
    public void capEvictedLiveInstanceIsReAdmittedInsteadOfRejectedAsTerminal() {
        // activeCapacity=1: creating a second record cap-evicts the first (still-live) instance.
        TransientEffectLedger ledger = new TransientEffectLedger(4, 1);
        TransientEffectIdentity evicted = identity("live-evicted");
        ledger.create(evicted);
        ledger.create(identity("other"));

        assertFalse(ledger.hasActiveRecord(evicted));
        assertEquals("the evicted instance is untracked, not terminal",
                Integer.valueOf(0), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));

        // The live instance is re-admitted normally; it is NOT rejected as a dead terminal record.
        assertTrue("cap eviction must not poison terminal lookups",
                ledger.admitRender(evicted));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(
                ledger.rejectedTerminalObservationCount()));
        assertEquals("the active window stays within its cap", Integer.valueOf(1),
                Integer.valueOf(ledger.activeCount()));

        // ...and it completes normally (it was never terminal).
        ledger.update(evicted, true);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
    }

    @Test
    public void activeCapacityAlsoBoundsRenderAdmittedRecords() {
        int cap = 3;
        TransientEffectLedger ledger = new TransientEffectLedger(1, cap);
        for (int i = 0; i < cap + 5; i++) {
            ledger.admitRender(identity("render-" + i));
        }

        assertEquals(Integer.valueOf(cap), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(5), Integer.valueOf(ledger.activeEvictedCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.unknownLifecycleCount()));
    }

    @Test
    public void activeAndRecentEvictionCountersAreDistinct() {
        // activeCapacity=1, recentCapacity=1: active overflow DROPS (activeEvicted), while genuine
        // terminal completions that overflow the recent window advance evictedCount.
        TransientEffectLedger ledger = new TransientEffectLedger(1, 1);
        ledger.create(identity("a"));
        ledger.create(identity("b")); // cap-evicts a

        assertEquals("active overflow is counted as an active eviction",
                Integer.valueOf(1), Integer.valueOf(ledger.activeEvictedCount()));
        assertEquals("active overflow never touches the recent-window counter",
                Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));

        ledger.complete(identity("b")); // recent={b}, total=1
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));

        ledger.create(identity("c"));
        ledger.create(identity("d")); // cap-evicts c: activeEvicted=2
        ledger.complete(identity("d")); // recent overflow evicts b: evicted=1, total=2

        assertEquals(Integer.valueOf(2), Integer.valueOf(ledger.activeEvictedCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.evictedCount()));
        assertEquals(Integer.valueOf(2), ledger.probeSlice().get("activeEvicted"));
        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("evicted"));
    }

    @Test
    public void updateIfActiveIsAtomicAndIdempotentForTerminalInstances() {
        TransientEffectLedger ledger = new TransientEffectLedger();
        TransientEffectIdentity effect = identity("if-active");
        ledger.create(effect);

        assertTrue("the first observation runs and reports the update",
                ledger.updateIfActive(effect, false));
        assertEquals(TransientEffectLedger.State.UPDATED, ledger.records().get(0).state);

        assertTrue(ledger.updateIfActive(effect, true));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));

        assertFalse("a repeated observation of a terminal instance is a silent no-op",
                ledger.updateIfActive(effect, true));
        assertFalse("an absent instance is never updated",
                ledger.updateIfActive(identity("never-created"), true));
        assertFalse(ledger.updateIfActive(null, true));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void evictedActiveRecordLateCompletionIsCoherentInsteadOfThrowing() {
        TransientEffectLedger ledger = new TransientEffectLedger(2, 1);
        TransientEffectIdentity evicted = identity("evicted");
        ledger.create(evicted);
        ledger.create(identity("newer"));

        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));
        assertFalse(ledger.hasActiveRecord(evicted));

        // A late completion reaches the retained terminal record rather than a live one, so it
        // cannot throw and cannot double-count an already-evicted instance.
        ledger.update(evicted, true);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.totalCount()));
    }

    @Test
    public void rejectedTerminalCounterSeparatesGenuineLateObservationsFromIdReuse() {
        TransientEffectLedger ledger = new TransientEffectLedger(4);
        TransientEffectIdentity dead = identity("dead");
        ledger.create(dead);
        ledger.complete(dead);

        // Genuine late re-observation of the SAME object: counted as a terminal rejection.
        assertFalse(ledger.admitRender(dead));
        assertEquals(Integer.valueOf(1), Integer.valueOf(
                ledger.rejectedTerminalObservationCount()));
        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("rejectedTerminal"));
        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("unknownLifecycle"));

        // A distinct live object presenting the SAME id and diagnostic fields (the identity-reuse
        // shape) is also rejected and counted, which is exactly why the bridge no longer generates
        // reusable ids.
        TransientEffectIdentity reused = new TransientEffectIdentity(
                "dead", "native.Effect", 1, 1L);
        assertFalse(ledger.admitRender(reused));
        assertEquals(Integer.valueOf(2), Integer.valueOf(
                ledger.rejectedTerminalObservationCount()));

        // A distinct id is still admitted, so rejection cannot mass-trigger once ids are unique.
        assertTrue(ledger.admitRender(identity("live")));
        assertEquals(Integer.valueOf(2), Integer.valueOf(
                ledger.rejectedTerminalObservationCount()));
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
    public void detachRemovesActiveWithoutRetainingTerminal() {
        TransientEffectLedger ledger = new TransientEffectLedger(2);
        TransientEffectIdentity effect = identity("detached");
        assertTrue(ledger.admitRender(effect));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));

        assertTrue("detach removes the active record", ledger.detach(effect));

        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeCount()));
        assertEquals("detach must not retain a terminal record",
                Integer.valueOf(0), Integer.valueOf(ledger.recentCount()));
        assertEquals("detach must not advance the completion total",
                Integer.valueOf(0), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.evictedCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.activeEvictedCount()));

        // The SAME identity is re-admitted on its next render — not rejected as terminal.
        assertTrue("a detached (still-live) object re-admits normally",
                ledger.admitRender(effect));
        assertEquals(Integer.valueOf(0), Integer.valueOf(
                ledger.rejectedTerminalObservationCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.unknownLifecycleCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.activeCount()));

        // Absent/terminal/null identities: false, no throw, no counter movement.
        assertFalse(ledger.detach(null));
        assertFalse(ledger.detach(identity("never-created")));
        ledger.complete(effect);
        assertFalse("a terminal identity is not detachable", ledger.detach(effect));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.recentCount()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.totalCount()));
        assertEquals(Integer.valueOf(0), Integer.valueOf(ledger.unknownLifecycleCount()));
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

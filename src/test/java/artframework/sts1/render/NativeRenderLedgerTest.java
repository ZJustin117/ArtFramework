package artframework.sts1.render;

import artframework.component.Rect;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NativeRenderLedgerTest {
    private NativeRenderInvocation invocation(long id, long frame) {
        return new NativeRenderInvocation(id, frame, "combat", "owner", "Native", "render",
                "surface", "source", Rect.ZERO);
    }

    @Test
    public void nativeContinuationClosesInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.pass(1L, "off"));

        assertFalse(ledger.isOpen(1L));
        assertEquals(Integer.valueOf(0), ledger.strictReport().get("openInvocation"));
    }

    @Test
    public void delegatedEvidenceMustMatchInvocationFrame() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        try {
            ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 5L, 1, "active"));
        } catch (IllegalStateException expected) {
            assertTrue(ledger.isOpen(1L));
            return;
        }
        throw new AssertionError("frame mismatch was accepted");
    }

    @Test
    public void delegatedEvidenceClosesInvocationExactlyOnce() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 4L, 1, "active"));

        assertFalse(ledger.isOpen(1L));
        try {
            ledger.recordEvidence(new PresentationDrawEvidence(1L, "entity", 4L, 1, "active"));
        } catch (IllegalStateException expected) {
            assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.evidenceCount()));
            return;
        }
        throw new AssertionError("duplicate evidence was accepted");
    }

    @Test
    public void evidenceUsesInvocationIdWhenOwnersAndFramesAreEqual() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordInvocation(invocation(2L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity-1"));
        ledger.recordDisposition(RenderDisposition.delegate(2L, "full", "entity-2"));

        ledger.recordEvidence(2L, "entity-2", 4L, 2, "active");
        ledger.recordEvidence(1L, "entity-1", 4L, 1, "active");

        assertEquals(1, ledger.evidence(1L).drawCount);
        assertEquals(2, ledger.evidence(2L).drawCount);
        assertEquals(Integer.valueOf(0), ledger.strictReport().get("delegatedWithoutEvidence"));
        assertTrue(ledger.isStrictlyAccepted());
    }

    @Test
    public void wrongEntityIsRejectedWithoutClosingInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        try {
            ledger.recordEvidence(1L, "other", 4L, 1, "active");
        } catch (IllegalStateException expected) {
            assertTrue(ledger.isOpen(1L));
            assertEquals(0, ledger.evidenceCount());
            return;
        }
        throw new AssertionError("entity mismatch was accepted");
    }

    @Test
    public void undecidedInvocationIsReported() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        Map<String, Object> report = ledger.strictReport();

        assertEquals(Integer.valueOf(1), report.get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), report.get("openInvocation"));
    }

    @Test
    public void delegatedFallbackIsReportedAsMismatchAndClosed() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        ledger.recordDelegatedFallback(1L);

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(1), report.get("dispositionMismatch"));
        assertEquals(Integer.valueOf(0), report.get("openInvocation"));
        assertEquals(Integer.valueOf(1), report.get("delegatedWithoutEvidence"));
    }

    @Test
    public void atomicFallbackAdmissionRejectsTerminalAndNonDelegatedCallbacks() {
        NativeRenderLedger ledger = delegatedLedger();
        assertTrue(ledger.recordDelegatedFallbackIfPending(1L));
        assertFalse(ledger.recordDelegatedFallbackIfPending(1L));

        NativeRenderLedger nonDelegated = new NativeRenderLedger();
        nonDelegated.recordInvocation(invocation(1L, 4L));
        nonDelegated.recordDisposition(RenderDisposition.pass(1L, "native"));
        assertFalse(nonDelegated.recordDelegatedFallbackIfPending(1L));

        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), nonDelegated.probeSlice().get("evidenceCount"));
    }

    @Test
    public void recoveryRecordsFailOpenForUndecidedInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));

        ledger.closeForRecovery("panic");

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(0), report.get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), report.get("recoveryFailOpen"));
        assertEquals(Integer.valueOf(1), report.get("unrecordedFAIL_OPEN"));
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, ledger.disposition(1L).mode);
    }

    @Test
    public void dispositionCommitAcceptsRecoveryFailOpenInsertedAfterInvocation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.closeForRecovery("panic");

        RenderDisposition effective = ledger.recordDispositionOrRecovery(
                RenderDisposition.delegate(1L, "full", "entity"));

        assertEquals(RenderDisposition.Mode.FAIL_OPEN, effective.mode);
        assertTrue(effective.nativeContinuation);
        assertEquals("panic", effective.reason);
        assertEquals(Integer.valueOf(1), Integer.valueOf(ledger.dispositionCount()));
        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(0), report.get("recoveryFailOpen"));
        assertEquals(Integer.valueOf(0), report.get("unrecordedFAIL_OPEN"));
        assertEquals(Boolean.TRUE, report.get("accepted"));
        try {
            ledger.recordDispositionOrRecovery(RenderDisposition.pass(1L, "late_duplicate"));
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("duplicate disposition"));
            return;
        }
        throw new AssertionError("second disposition commit was accepted");
    }

    @Test(expected = IllegalStateException.class)
    public void dispositionCommitStillRejectsOrdinaryDuplicate() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.pass(1L, "off"));

        ledger.recordDispositionOrRecovery(RenderDisposition.capture(1L, "observe"));
    }

    @Test
    public void recoveryClosesDelegatedInvocationAsMismatchWithoutEvidence() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));

        ledger.closeForRecovery("host_recreated");

        Map<String, Object> report = ledger.strictReport();
        assertEquals(Integer.valueOf(0), report.get("openInvocation"));
        assertEquals(Integer.valueOf(1), report.get("dispositionMismatch"));
        assertEquals(Integer.valueOf(1), report.get("delegatedWithoutEvidence"));
    }

    @Test
    public void cumulativeCountsRemainIndependentOfBoundedRecentHistory() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        int total = NativeRenderLedger.RECENT_HISTORY_CAPACITY * 3;
        for (int i = 1; i <= total; i++) {
            ledger.recordInvocation(invocation(i, i));
            if (i % 2 == 0) {
                ledger.recordDisposition(RenderDisposition.delegate(i, "full", "entity-" + i));
                ledger.recordEvidence(i, "entity-" + i, i, 1, "active");
            } else {
                ledger.recordDisposition(RenderDisposition.pass(i, "native"));
            }
        }

        Map<String, Object> probe = ledger.probeSlice();
        assertEquals(total, ledger.invocationCount());
        assertEquals(total, ledger.dispositionCount());
        assertEquals(total / 2, ledger.evidenceCount());
        assertEquals(NativeRenderLedger.RECENT_HISTORY_CAPACITY, ledger.invocations().size());
        assertEquals(NativeRenderLedger.RECENT_HISTORY_CAPACITY, ledger.dispositions().size());
        assertEquals(Integer.valueOf(total), probe.get("totalInvocationCount"));
        assertEquals(Integer.valueOf(total), probe.get("totalDispositionCount"));
        assertEquals(Integer.valueOf(total / 2), probe.get("totalEvidenceCount"));
        assertEquals(Integer.valueOf(0), probe.get("openInvocationCount"));
        assertEquals(Integer.valueOf(NativeRenderLedger.RECENT_HISTORY_CAPACITY),
                probe.get("retainedInvocationCount"));
        assertEquals(Integer.valueOf(NativeRenderLedger.RECENT_HISTORY_CAPACITY),
                probe.get("recentHistoryCapacity"));
        assertEquals(Integer.valueOf(total - NativeRenderLedger.RECENT_HISTORY_CAPACITY),
                probe.get("evictedCompletedCount"));
        assertEquals(Integer.valueOf(total / 2), probe.get("passThrough"));
        assertEquals(Integer.valueOf(total / 2), probe.get("delegateToArt"));
        assertEquals(Integer.valueOf(0), probe.get("delegatedWithoutEvidence"));
    }

    @Test
    public void collectionQueriesContainOpenPlusBoundedRecentDetail() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        int completed = NativeRenderLedger.RECENT_HISTORY_CAPACITY + 3;
        for (int i = 1; i <= completed; i++) {
            ledger.recordInvocation(invocation(i, i));
            ledger.recordDisposition(RenderDisposition.pass(i, "native"));
        }
        ledger.recordInvocation(invocation(completed + 1L, completed + 1L));
        ledger.recordInvocation(invocation(completed + 2L, completed + 2L));

        assertEquals(NativeRenderLedger.RECENT_HISTORY_CAPACITY + 2,
                ledger.invocations().size());
        assertEquals(NativeRenderLedger.RECENT_HISTORY_CAPACITY,
                ledger.dispositions().size());
        assertEquals(Integer.valueOf(2), ledger.probeSlice().get("openInvocationCount"));
        assertEquals(Integer.valueOf(NativeRenderLedger.RECENT_HISTORY_CAPACITY + 2),
                ledger.probeSlice().get("retainedInvocationCount"));
    }

    @Test
    public void evictedIdsAreNotQueryableOrReusableWithinResetEpoch() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        for (int i = 1; i <= NativeRenderLedger.RECENT_HISTORY_CAPACITY + 1; i++) {
            ledger.recordInvocation(invocation(i, i));
            ledger.recordDisposition(RenderDisposition.pass(i, "native"));
        }

        assertNull(ledger.invocation(1L));
        assertNull(ledger.disposition(1L));
        try {
            ledger.recordInvocation(invocation(1L, 99L));
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("stale invocation"));
            return;
        }
        throw new AssertionError("evicted invocation id was reused");
    }

    @Test
    public void terminalDelegatedInputsAreImmutableAndCountedOnce() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        ledger.recordDelegatedFallback(1L);

        assertRejected(new Runnable() {
            @Override public void run() { ledger.recordDelegatedFallback(1L); }
        });
        assertRejected(new Runnable() {
            @Override public void run() { ledger.recordEvidence(1L, "entity", 4L, 1, "late"); }
        });
        assertEquals(1, ledger.invocationCount());
        assertEquals(1, ledger.dispositionCount());
        assertEquals(0, ledger.evidenceCount());
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("dispositionMismatch"));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("delegatedWithoutEvidence"));
    }

    @Test
    public void lateEvidenceIsRejectedAfterCancellationAndRecovery() {
        NativeRenderLedger cancelled = delegatedLedger();
        cancelled.cancelForTransition(1L);
        assertRejected(new Runnable() {
            @Override public void run() { cancelled.recordEvidence(1L, "entity", 4L, 1, "late"); }
        });
        assertEquals(Integer.valueOf(0), cancelled.strictReport().get("delegatedWithoutEvidence"));

        NativeRenderLedger recovered = delegatedLedger();
        recovered.closeForRecovery("recovery");
        assertRejected(new Runnable() {
            @Override public void run() { recovered.recordEvidence(1L, "entity", 4L, 1, "late"); }
        });
        assertEquals(Integer.valueOf(0), recovered.strictReport().get("delegatedWithoutEvidence"));

        NativeRenderLedger failedRecovery = delegatedLedger();
        failedRecovery.closeForRecovery("host_recreated");
        assertRejected(new Runnable() {
            @Override public void run() { failedRecovery.recordEvidence(1L, "entity", 4L, 1, "late"); }
        });
        assertEquals(Integer.valueOf(1),
                failedRecovery.strictReport().get("delegatedWithoutEvidence"));
    }

    @Test
    public void delegatedStrictOutcomeSurvivesRecentEviction() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 1L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        ledger.recordDelegatedFallback(1L);
        for (int i = 2; i <= NativeRenderLedger.RECENT_HISTORY_CAPACITY + 2; i++) {
            ledger.recordInvocation(invocation(i, i));
            ledger.recordDisposition(RenderDisposition.pass(i, "native"));
        }

        assertNull(ledger.invocation(1L));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("dispositionMismatch"));
    }

    @Test
    public void everyDelegatedTerminalOutcomeRemainsStableAcrossEviction() {
        NativeRenderLedger successful = delegatedLedger();
        successful.recordEvidence(1L, "entity", 4L, 1, "active");
        NativeRenderLedger fallback = delegatedLedger();
        fallback.recordDelegatedFallback(1L);
        NativeRenderLedger cancellation = delegatedLedger();
        cancellation.cancelForTransition(1L);
        NativeRenderLedger recoveryCancellation = delegatedLedger();
        recoveryCancellation.closeForRecovery("recovery");
        NativeRenderLedger recoveryFailure = delegatedLedger();
        recoveryFailure.closeForRecovery("host_recreated");

        NativeRenderLedger[] ledgers = new NativeRenderLedger[] {
                successful, fallback, cancellation, recoveryCancellation, recoveryFailure
        };
        int[] expectedGaps = new int[] {0, 1, 0, 0, 1};
        for (int index = 0; index < ledgers.length; index++) {
            NativeRenderLedger ledger = ledgers[index];
            assertEquals(Integer.valueOf(expectedGaps[index]),
                    ledger.strictReport().get("delegatedWithoutEvidence"));
            evictFirstRecentRecord(ledger);
            assertNull(ledger.invocation(1L));
            assertEquals(Integer.valueOf(expectedGaps[index]),
                    ledger.strictReport().get("delegatedWithoutEvidence"));
        }
    }

    @Test
    public void recoveryTombstoneSettlesOneLateDispositionButNeverEvidence() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.closeForRecovery("panic");

        assertRejected(new Runnable() {
            @Override public void run() { ledger.recordEvidence(1L, "entity", 4L, 1, "late"); }
        });
        RenderDisposition effective = ledger.recordDispositionOrRecovery(
                RenderDisposition.delegate(1L, "full", "entity"));
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, effective.mode);
        assertEquals(Integer.valueOf(0), ledger.strictReport().get("recoveryFailOpen"));
        assertRejected(new Runnable() {
            @Override public void run() {
                ledger.recordDispositionOrRecovery(RenderDisposition.pass(1L, "duplicate"));
            }
        });
        assertEquals(1, ledger.dispositionCount());
    }

    @Test
    public void recoveryTombstonesAreBoundedAndEvictedOnInsertionOrder() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        int total = NativeRenderLedger.RECOVERY_TOMBSTONE_CAPACITY + 1;
        for (int i = 1; i <= total; i++) ledger.recordInvocation(invocation(i, i));
        ledger.closeForRecovery("panic");

        Map<String, Object> probe = ledger.probeSlice();
        assertEquals(Integer.valueOf(NativeRenderLedger.RECOVERY_TOMBSTONE_CAPACITY),
                probe.get("recoveryTombstoneCount"));
        assertEquals(Integer.valueOf(NativeRenderLedger.RECOVERY_TOMBSTONE_CAPACITY),
                probe.get("recoveryTombstoneCapacity"));
        assertRejected(new Runnable() {
            @Override public void run() {
                ledger.recordDispositionOrRecovery(RenderDisposition.pass(1L, "stale"));
            }
        });
        RenderDisposition settled = ledger.recordDispositionOrRecovery(
                RenderDisposition.pass(total, "late"));
        assertEquals(RenderDisposition.Mode.FAIL_OPEN, settled.mode);
        assertEquals(Integer.valueOf(total - 1), ledger.strictReport().get("recoveryFailOpen"));
    }

    @Test
    public void undecidedAndReadQueriesReflectOnlyOpenStateWithoutMutation() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 1L));
        ledger.recordInvocation(invocation(2L, 2L));
        ledger.recordDisposition(RenderDisposition.pass(2L, "native"));

        Map<String, Object> beforeProbe = ledger.probeSlice();
        Map<String, Object> beforeStrict = ledger.strictReport();
        for (int i = 0; i < 5; i++) {
            assertEquals(beforeProbe, ledger.probeSlice());
            assertEquals(beforeStrict, ledger.strictReport());
            assertFalse(ledger.isStrictlyAccepted());
        }
        assertEquals(Integer.valueOf(1), beforeStrict.get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), beforeStrict.get("openInvocation"));
        assertEquals(2, ledger.invocationCount());
        assertEquals(1, ledger.dispositionCount());
    }

    @Test
    public void clearResetsStorageCountersAndIdHighWater() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(7L, 1L));
        ledger.recordDisposition(RenderDisposition.pass(7L, "native"));
        ledger.clear();
        ledger.recordInvocation(invocation(1L, 2L));

        assertEquals(1, ledger.invocationCount());
        assertEquals(Integer.valueOf(1), ledger.probeSlice().get("openInvocationCount"));
        assertEquals(Integer.valueOf(0), ledger.probeSlice().get("evictedCompletedCount"));
    }

    @Test
    public void closeInvocationCannotEraseUndecidedStrictGap() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 1L));

        assertRejected(new Runnable() {
            @Override public void run() { ledger.closeInvocation(1L); }
        });
        assertTrue(ledger.isOpen(1L));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("runtimeUNDECIDED"));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("openInvocation"));
        assertEquals(1, ledger.invocationCount());
        assertEquals(0, ledger.dispositionCount());
    }

    @Test
    public void delegatedCloseClassifiesMissingEvidenceExactlyOnce() {
        NativeRenderLedger ledger = delegatedLedger();
        ledger.closeInvocation(1L);

        assertFalse(ledger.isOpen(1L));
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), ledger.strictReport().get("dispositionMismatch"));
        assertEquals(Integer.valueOf(0), ledger.probeSlice().get("cancelledInvocation"));
        assertRejected(new Runnable() {
            @Override public void run() { ledger.closeInvocation(1L); }
        });
        assertRejected(new Runnable() {
            @Override public void run() { ledger.recordDelegatedFallback(1L); }
        });
        assertEquals(Integer.valueOf(1), ledger.strictReport().get("delegatedWithoutEvidence"));
    }

    @Test
    public void clearResetsAllCountersTombstonesStrictStateAndHighWater() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(10L, 1L));
        ledger.recordDisposition(RenderDisposition.pass(10L, "native"));
        ledger.recordInvocation(invocation(11L, 2L));
        ledger.recordDisposition(RenderDisposition.delegate(11L, "full", "entity"));
        ledger.recordDelegatedFallback(11L);
        ledger.recordInvocation(invocation(12L, 3L));
        ledger.closeForRecovery("panic");
        assertFalse(ledger.isStrictlyAccepted());

        ledger.clear();

        Map<String, Object> probe = ledger.probeSlice();
        assertEquals(Integer.valueOf(0), probe.get("totalInvocationCount"));
        assertEquals(Integer.valueOf(0), probe.get("totalDispositionCount"));
        assertEquals(Integer.valueOf(0), probe.get("totalEvidenceCount"));
        assertEquals(Integer.valueOf(0), probe.get("passThrough"));
        assertEquals(Integer.valueOf(0), probe.get("delegateToArt"));
        assertEquals(Integer.valueOf(0), probe.get("failOpen"));
        assertEquals(Integer.valueOf(0), probe.get("delegatedWithoutEvidence"));
        assertEquals(Integer.valueOf(0), probe.get("recoveryFailOpen"));
        assertEquals(Integer.valueOf(0), probe.get("recoveryTombstoneCount"));
        assertEquals(Integer.valueOf(0), probe.get("evictedCompletedCount"));
        assertTrue(ledger.isStrictlyAccepted());

        ledger.recordInvocation(invocation(1L, 4L));
        assertTrue(ledger.isOpen(1L));
    }

    @Test
    public void evictingRecoveryTombstoneKeepsProvisionalGapAndRejectsLateDisposition() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        int total = NativeRenderLedger.RECOVERY_TOMBSTONE_CAPACITY + 1;
        for (int i = 1; i <= total; i++) ledger.recordInvocation(invocation(i, i));
        ledger.closeForRecovery("panic");

        assertEquals(Integer.valueOf(total), ledger.strictReport().get("recoveryFailOpen"));
        assertEquals(Boolean.FALSE, ledger.strictReport().get("accepted"));
        assertRejected(new Runnable() {
            @Override public void run() {
                ledger.recordDispositionOrRecovery(RenderDisposition.pass(1L, "stale"));
            }
        });
        assertEquals(Integer.valueOf(total), ledger.strictReport().get("recoveryFailOpen"));
        assertEquals(Boolean.FALSE, ledger.strictReport().get("accepted"));
    }

    private NativeRenderLedger delegatedLedger() {
        NativeRenderLedger ledger = new NativeRenderLedger();
        ledger.recordInvocation(invocation(1L, 4L));
        ledger.recordDisposition(RenderDisposition.delegate(1L, "full", "entity"));
        return ledger;
    }

    private void evictFirstRecentRecord(NativeRenderLedger ledger) {
        for (int i = 2; i <= NativeRenderLedger.RECENT_HISTORY_CAPACITY + 1; i++) {
            ledger.recordInvocation(invocation(i, i));
            ledger.recordDisposition(RenderDisposition.pass(i, "native"));
        }
    }

    private void assertRejected(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("terminal input was accepted");
    }
}

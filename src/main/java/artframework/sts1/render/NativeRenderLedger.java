package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Process-local evidence ledger for native render invocation decisions. */
public final class NativeRenderLedger {
    static final int RECENT_HISTORY_CAPACITY = 256;
    static final int RECOVERY_TOMBSTONE_CAPACITY = 256;

    private static final class Record {
        final NativeRenderInvocation invocation;
        RenderDisposition disposition;
        PresentationDrawEvidence evidence;

        Record(NativeRenderInvocation invocation) {
            this.invocation = invocation;
        }
    }

    private final Map<Long, Record> open = new LinkedHashMap<Long, Record>();
    private final Map<Long, Record> recent = new LinkedHashMap<Long, Record>();
    private final Map<Long, RenderDisposition> recoveryTombstones =
            new LinkedHashMap<Long, RenderDisposition>();

    private long invocationIdHighWater;
    private boolean hasInvocationId;
    private int totalInvocationCount;
    private int totalDispositionCount;
    private int totalEvidenceCount;
    private int passThroughCount;
    private int captureAndPassCount;
    private int delegateToArtCount;
    private int failOpenCount;
    private int openUndecidedCount;
    private int openDelegatedGapCount;
    private int terminalMissingEvidenceCount;
    private int noPixelIsolationCount;
    private int evictedCompletedCount;
    private int unknownOwnerCount;
    private int orphanArtOutputCount;
    private int dispositionMismatchCount;
    private int leakedTransientEntityCount;
    private int recoveryFailOpenCount;
    private int cancelledInvocationCount;

    /** Records ART output for the exact native invocation that produced it. */
    public synchronized void recordEvidence(long invocationId, String entityId, long frameId,
            int drawCount, String cleanupState) {
        recordEvidence(new PresentationDrawEvidence(invocationId, entityId, frameId,
                drawCount, cleanupState));
    }

    public synchronized void recordInvocation(NativeRenderInvocation invocation) {
        if (invocation == null) throw new IllegalArgumentException("invocation required");
        if (hasInvocationId && invocation.invocationId <= invocationIdHighWater) {
            throw new IllegalStateException("stale invocation: " + invocation.invocationId);
        }
        invocationIdHighWater = invocation.invocationId;
        hasInvocationId = true;
        open.put(Long.valueOf(invocation.invocationId), new Record(invocation));
        totalInvocationCount++;
        openUndecidedCount++;
    }

    public synchronized void recordDisposition(RenderDisposition disposition) {
        recordNewDisposition(disposition);
    }

    /** Returns a recovery fail-open that won the race, otherwise records the proposed decision. */
    public synchronized RenderDisposition recordDispositionOrRecovery(
            RenderDisposition disposition) {
        if (disposition == null) throw new IllegalArgumentException("disposition required");
        Long id = Long.valueOf(disposition.invocationId);
        RenderDisposition recovery = recoveryTombstones.remove(id);
        if (recovery != null) {
            recoveryFailOpenCount--;
            return recovery;
        }
        recordNewDisposition(disposition);
        return disposition;
    }

    private void recordNewDisposition(RenderDisposition disposition) {
        if (disposition == null) throw new IllegalArgumentException("disposition required");
        Long id = Long.valueOf(disposition.invocationId);
        Record record = open.get(id);
        if (record == null) throw dispositionInputError(id);
        if (record.disposition != null) {
            throw new IllegalStateException("duplicate disposition: " + id);
        }
        accountDisposition(disposition);
        record.disposition = disposition;
        openUndecidedCount--;
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
            openDelegatedGapCount++;
        }
        if (disposition.nativeContinuation) {
            boolean missingDelegatedEvidence =
                    disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART;
            terminalize(id, missingDelegatedEvidence);
        }
    }

    private IllegalStateException dispositionInputError(Long id) {
        if (recent.containsKey(id) || recoveryTombstones.containsKey(id)) {
            return new IllegalStateException("duplicate disposition: " + id);
        }
        return new IllegalStateException("unknown invocation: " + id);
    }

    private void accountDisposition(RenderDisposition disposition) {
        totalDispositionCount++;
        if (disposition.mode == RenderDisposition.Mode.PASS_THROUGH) passThroughCount++;
        if (disposition.mode == RenderDisposition.Mode.CAPTURE_AND_PASS) captureAndPassCount++;
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) delegateToArtCount++;
        if (disposition.mode == RenderDisposition.Mode.FAIL_OPEN) failOpenCount++;
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                && disposition.nativeContinuation) {
            dispositionMismatchCount++;
        }
        if (disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART
                && !disposition.nativeContinuation) {
            dispositionMismatchCount++;
        }
    }

    public synchronized void recordUnknownOwner() {
        unknownOwnerCount++;
    }

    public synchronized void recordOrphanArtOutput() {
        orphanArtOutputCount++;
    }

    /** Record a native fallback after a delegated renderer failed to produce ART output. */
    public synchronized void recordDelegatedFallback(long id) {
        Long key = Long.valueOf(id);
        Record record = requireOpenDelegated(key, "fallback");
        dispositionMismatchCount++;
        terminalize(key, true);
    }

    /**
     * Atomically admits a native fallback only while the invocation is an open delegation.
     * Callback paths use this non-throwing form because an old callback is normal after cleanup.
     */
    public synchronized boolean recordDelegatedFallbackIfPending(long id) {
        Long key = Long.valueOf(id);
        Record record = open.get(key);
        if (record == null || record.disposition == null
                || record.disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            return false;
        }
        dispositionMismatchCount++;
        terminalize(key, true);
        return true;
    }

    /** Closes delegation retired by an OFF transition before PostRender consumes it. */
    public synchronized void cancelForTransition(long id) {
        Long key = Long.valueOf(id);
        requireOpenDelegated(key, "transition cancel");
        cancelledInvocationCount++;
        terminalize(key, false);
    }

    private Record requireOpenDelegated(Long id, String operation) {
        Record record = open.get(id);
        if (record == null || record.disposition == null
                || record.disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            throw new IllegalStateException(operation + " for non-open delegated invocation: " + id);
        }
        return record;
    }

    public synchronized void recordLeakedTransientEntity() {
        leakedTransientEntityCount++;
    }

    /** Close outstanding work during panic/recreation while retaining bounded diagnostics. */
    public synchronized void closeForRecovery(String reason) {
        String value = reason == null || reason.trim().isEmpty() ? "recovery" : reason;
        List<Long> ids = new ArrayList<Long>(open.keySet());
        for (Long id : ids) {
            Record record = open.get(id);
            if (record == null) continue;
            if (record.disposition == null) {
                RenderDisposition recovery = RenderDisposition.failOpen(id.longValue(), value);
                record.disposition = recovery;
                accountDisposition(recovery);
                openUndecidedCount--;
                recoveryFailOpenCount++;
                putRecoveryTombstone(id, recovery);
                terminalize(id, false);
            } else if (record.disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
                if ("recovery".equals(value)) {
                    cancelledInvocationCount++;
                    terminalize(id, false);
                } else {
                    dispositionMismatchCount++;
                    terminalize(id, true);
                }
            } else {
                terminalize(id, false);
            }
        }
    }

    private void putRecoveryTombstone(Long id, RenderDisposition recovery) {
        recoveryTombstones.put(id, recovery);
        while (recoveryTombstones.size() > RECOVERY_TOMBSTONE_CAPACITY) {
            Long eldest = recoveryTombstones.keySet().iterator().next();
            recoveryTombstones.remove(eldest);
        }
    }

    public synchronized void recordEvidence(PresentationDrawEvidence drawEvidence) {
        if (drawEvidence == null) throw new IllegalArgumentException("evidence required");
        Long id = Long.valueOf(drawEvidence.invocationId);
        Record record = open.get(id);
        if (record == null) {
            Record completed = recent.get(id);
            if (completed != null && completed.evidence != null) {
                throw new IllegalStateException("duplicate evidence: " + id);
            }
            throw new IllegalStateException("evidence for non-open delegated invocation: " + id);
        }
        RenderDisposition disposition = record.disposition;
        if (disposition == null) throw new IllegalStateException("unknown disposition: " + id);
        if (disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            throw new IllegalStateException("evidence for non-delegated invocation: " + id);
        }
        if (!disposition.presentationEntityId.equals(drawEvidence.entityId)) {
            throw new IllegalStateException("presentation entity mismatch: " + id);
        }
        if (record.invocation.frameId != drawEvidence.frameId) {
            throw new IllegalStateException("presentation frame mismatch: " + id);
        }
        if (drawEvidence.drawCount < 0) {
            throw new IllegalArgumentException("draw count must not be negative");
        }
        record.evidence = drawEvidence;
        totalEvidenceCount++;
        terminalize(id, false);
    }

    /**
     * Correlates and records delegated evidence as one ledger transaction.  In particular, the
     * disposition and invocation are read while this monitor is held, so a completed/recovered
     * record cannot turn into a null invocation between the two checks.
     *
     * @return false for stale, terminal, or non-delegated callback input; duplicate evidence
     * remains a strict error, matching recordEvidence.
     */
    public synchronized boolean recordDelegatedEvidence(long id, int drawCount, String cleanupState) {
        Long key = Long.valueOf(id);
        Record record = open.get(key);
        if (record == null || record.disposition == null
                || record.disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            if (record == null) {
                Record completed = recent.get(key);
                if (completed != null && completed.evidence != null) {
                    throw new IllegalStateException("duplicate evidence: " + id);
                }
            }
            return false;
        }
        recordEvidence(new PresentationDrawEvidence(id,
                record.disposition.presentationEntityId, record.invocation.frameId,
                drawCount, cleanupState));
        return true;
    }

    /** True only while the invocation can still receive delegated evidence. */
    public synchronized boolean isPendingDelegated(long id) {
        Record record = open.get(Long.valueOf(id));
        return record != null && record.disposition != null
                && record.disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART;
    }

    public synchronized int delegatedWithoutEvidenceCount() {
        return openDelegatedGapCount + terminalMissingEvidenceCount;
    }

    /** Completes a delegated lifecycle that intentionally has no pixel-draw evidence hook. */
    public synchronized boolean completeDelegatedWithoutEvidence(long id) {
        Long key = Long.valueOf(id);
        Record record = open.get(key);
        if (record == null || record.disposition == null
                || record.disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            return false;
        }
        terminalize(key, false);
        return true;
    }

    /** Records an isolate-only absence result; this is not ART pixel evidence. */
    public synchronized void recordNoPixelIsolation() { noPixelIsolationCount++; }

    /** Close an invocation explicitly during scene/recovery cleanup. */
    public synchronized void closeInvocation(long id) {
        Long key = Long.valueOf(id);
        Record record = open.get(key);
        if (record == null) throw new IllegalStateException("duplicate or unknown close: " + id);
        if (record.disposition == null) {
            throw new IllegalStateException("cannot close undecided invocation: " + id);
        }
        if (!record.disposition.nativeContinuation
                && record.disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
            terminalize(key, true);
            return;
        }
        throw new IllegalStateException("duplicate or unknown close: " + id);
    }

    private void terminalize(Long id, boolean terminalMissingEvidence) {
        Record record = open.remove(id);
        if (record == null) throw new IllegalStateException("invocation already terminal: " + id);
        if (record.disposition == null) openUndecidedCount--;
        if (record.disposition != null
                && record.disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
            openDelegatedGapCount--;
        }
        if (terminalMissingEvidence) terminalMissingEvidenceCount++;
        recent.put(id, record);
        while (recent.size() > RECENT_HISTORY_CAPACITY) {
            Long eldest = recent.keySet().iterator().next();
            recent.remove(eldest);
            evictedCompletedCount++;
        }
    }

    public synchronized boolean isOpen(long id) {
        return open.containsKey(Long.valueOf(id));
    }

    public synchronized NativeRenderInvocation invocation(long id) {
        Record record = queryRecord(Long.valueOf(id));
        return record == null ? null : record.invocation;
    }

    public synchronized RenderDisposition disposition(long id) {
        Record record = queryRecord(Long.valueOf(id));
        return record == null ? null : record.disposition;
    }

    public synchronized PresentationDrawEvidence evidence(long id) {
        Record record = queryRecord(Long.valueOf(id));
        return record == null ? null : record.evidence;
    }

    private Record queryRecord(Long id) {
        Record record = open.get(id);
        return record != null ? record : recent.get(id);
    }

    public synchronized int invocationCount() { return totalInvocationCount; }
    public synchronized int dispositionCount() { return totalDispositionCount; }
    public synchronized int evidenceCount() { return totalEvidenceCount; }

    public synchronized List<NativeRenderInvocation> invocations() {
        Map<Long, NativeRenderInvocation> snapshot = new TreeMap<Long, NativeRenderInvocation>();
        for (Map.Entry<Long, Record> entry : recent.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().invocation);
        }
        for (Map.Entry<Long, Record> entry : open.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().invocation);
        }
        return Collections.unmodifiableList(
                new ArrayList<NativeRenderInvocation>(snapshot.values()));
    }

    public synchronized List<RenderDisposition> dispositions() {
        Map<Long, RenderDisposition> snapshot = new TreeMap<Long, RenderDisposition>();
        for (Map.Entry<Long, Record> entry : recent.entrySet()) {
            if (entry.getValue().disposition != null) {
                snapshot.put(entry.getKey(), entry.getValue().disposition);
            }
        }
        for (Map.Entry<Long, Record> entry : open.entrySet()) {
            if (entry.getValue().disposition != null) {
                snapshot.put(entry.getKey(), entry.getValue().disposition);
            }
        }
        return Collections.unmodifiableList(new ArrayList<RenderDisposition>(snapshot.values()));
    }

    public synchronized Map<String, Object> probeSlice() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("invocationCount", Integer.valueOf(totalInvocationCount));
        out.put("dispositionCount", Integer.valueOf(totalDispositionCount));
        out.put("evidenceCount", Integer.valueOf(totalEvidenceCount));
        out.put("noPixelIsolationCount", Integer.valueOf(noPixelIsolationCount));
        out.put("totalInvocationCount", Integer.valueOf(totalInvocationCount));
        out.put("totalDispositionCount", Integer.valueOf(totalDispositionCount));
        out.put("totalEvidenceCount", Integer.valueOf(totalEvidenceCount));
        out.put("passThrough", Integer.valueOf(passThroughCount));
        out.put("captureAndPass", Integer.valueOf(captureAndPassCount));
        out.put("delegateToArt", Integer.valueOf(delegateToArtCount));
        out.put("failOpen", Integer.valueOf(failOpenCount));
        out.put("recoveryFailOpen", Integer.valueOf(recoveryFailOpenCount));
        out.put("delegatedWithoutEvidence", Integer.valueOf(delegatedWithoutEvidenceCount()));
        out.put("unknownOwner", Integer.valueOf(unknownOwnerCount));
        out.put("orphanArtOutput", Integer.valueOf(orphanArtOutputCount));
        out.put("dispositionMismatch", Integer.valueOf(dispositionMismatchCount));
        out.put("openInvocation", Integer.valueOf(open.size()));
        out.put("openInvocationCount", Integer.valueOf(open.size()));
        out.put("recentInvocationCount", Integer.valueOf(recent.size()));
        out.put("retainedInvocationCount", Integer.valueOf(open.size() + recent.size()));
        out.put("recentHistoryCapacity", Integer.valueOf(RECENT_HISTORY_CAPACITY));
        out.put("evictedCompletedCount", Integer.valueOf(evictedCompletedCount));
        out.put("recoveryTombstoneCount", Integer.valueOf(recoveryTombstones.size()));
        out.put("recoveryTombstoneCapacity", Integer.valueOf(RECOVERY_TOMBSTONE_CAPACITY));
        out.put("leakedTransientEntity", Integer.valueOf(leakedTransientEntityCount));
        out.put("cancelledInvocation", Integer.valueOf(cancelledInvocationCount));
        return out;
    }

    /** Strict NRCC counters; a zero-valued report is required before FULL acceptance. */
    public synchronized Map<String, Object> strictReport() {
        Map<String, Object> out = strictReportWithoutAcceptance();
        out.put("accepted", Boolean.valueOf(isStrictlyAccepted(out)));
        return Collections.unmodifiableMap(out);
    }

    /** Machine-readable NRCC acceptance; every strict error counter must be zero. */
    public synchronized boolean isStrictlyAccepted() {
        return isStrictlyAccepted(strictReportWithoutAcceptance());
    }

    private Map<String, Object> strictReportWithoutAcceptance() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("runtimeUNKNOWN", Integer.valueOf(unknownOwnerCount));
        out.put("runtimeUNDECIDED", Integer.valueOf(openUndecidedCount));
        out.put("openInvocation", Integer.valueOf(open.size()));
        out.put("delegatedWithoutEvidence", Integer.valueOf(delegatedWithoutEvidenceCount()));
        out.put("dispositionMismatch", Integer.valueOf(dispositionMismatchCount));
        out.put("orphanArtOutput", Integer.valueOf(orphanArtOutputCount));
        out.put("leakedTransientEntity", Integer.valueOf(leakedTransientEntityCount));
        out.put("recoveryFailOpen", Integer.valueOf(recoveryFailOpenCount));
        out.put("unrecordedFAIL_OPEN", Integer.valueOf(recoveryFailOpenCount));
        return out;
    }

    private static boolean isStrictlyAccepted(Map<String, Object> report) {
        for (Object value : report.values()) {
            if (value instanceof Number && ((Number) value).intValue() != 0) return false;
        }
        return true;
    }

    public synchronized void clear() {
        open.clear();
        recent.clear();
        recoveryTombstones.clear();
        invocationIdHighWater = 0L;
        hasInvocationId = false;
        totalInvocationCount = 0;
        totalDispositionCount = 0;
        totalEvidenceCount = 0;
        passThroughCount = 0;
        captureAndPassCount = 0;
        delegateToArtCount = 0;
        failOpenCount = 0;
        openUndecidedCount = 0;
        openDelegatedGapCount = 0;
        terminalMissingEvidenceCount = 0;
        noPixelIsolationCount = 0;
        evictedCompletedCount = 0;
        unknownOwnerCount = 0;
        orphanArtOutputCount = 0;
        dispositionMismatchCount = 0;
        leakedTransientEntityCount = 0;
        recoveryFailOpenCount = 0;
        cancelledInvocationCount = 0;
    }
}

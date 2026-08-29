package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Process-local evidence ledger for native render invocation decisions. */
public final class NativeRenderLedger {
    private enum State { OPEN, COMPLETE }
    private final Map<Long, NativeRenderInvocation> invocations =
            new LinkedHashMap<Long, NativeRenderInvocation>();
    private final Map<Long, RenderDisposition> dispositions =
            new LinkedHashMap<Long, RenderDisposition>();
    private final Map<Long, PresentationDrawEvidence> evidence =
            new LinkedHashMap<Long, PresentationDrawEvidence>();
    private final Map<Long, State> states = new LinkedHashMap<Long, State>();
    private final Set<Long> transitionCancelled = new HashSet<Long>();
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
        if (invocations.containsKey(Long.valueOf(invocation.invocationId))) {
            throw new IllegalStateException("duplicate invocation: " + invocation.invocationId);
        }
        invocations.put(Long.valueOf(invocation.invocationId), invocation);
        states.put(Long.valueOf(invocation.invocationId), State.OPEN);
    }

    public synchronized void recordDisposition(RenderDisposition disposition) {
        if (disposition == null) throw new IllegalArgumentException("disposition required");
        Long id = Long.valueOf(disposition.invocationId);
        if (!invocations.containsKey(id)) throw new IllegalStateException("unknown invocation: " + id);
        if (dispositions.containsKey(id)) throw new IllegalStateException("duplicate disposition: " + id);
        if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                && disposition.nativeContinuation) {
            dispositionMismatchCount++;
        }
        if (disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART
                && !disposition.nativeContinuation) {
            dispositionMismatchCount++;
        }
        dispositions.put(id, disposition);
        if (disposition.nativeContinuation) states.put(id, State.COMPLETE);
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
        RenderDisposition disposition = dispositions.get(key);
        if (disposition == null || disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            throw new IllegalStateException("fallback for non-delegated invocation: " + id);
        }
        dispositionMismatchCount++;
        if (states.get(key) == State.OPEN) states.put(key, State.COMPLETE);
    }

    /** Closes delegation retired by an OFF transition before PostRender consumes it. */
    public synchronized void cancelForTransition(long id) {
        Long key = Long.valueOf(id);
        RenderDisposition disposition = dispositions.get(key);
        if (disposition == null || disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            throw new IllegalStateException("transition cancel for non-delegated invocation: " + id);
        }
        if (states.get(key) == State.COMPLETE) return;
        states.put(key, State.COMPLETE);
        transitionCancelled.add(key);
        cancelledInvocationCount++;
    }

    public synchronized void recordLeakedTransientEntity() {
        leakedTransientEntityCount++;
    }

    /** Close outstanding work during panic/recreation without deleting its evidence history. */
    public synchronized void closeForRecovery(String reason) {
        String value = reason == null || reason.trim().isEmpty() ? "recovery" : reason;
        for (Map.Entry<Long, State> entry : states.entrySet()) {
            if (entry.getValue() != State.OPEN) continue;
            Long id = entry.getKey();
            RenderDisposition disposition = dispositions.get(id);
            if (disposition == null) {
                dispositions.put(id, RenderDisposition.failOpen(id, value));
                recoveryFailOpenCount++;
            } else if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && evidence.get(id) == null) {
                if ("recovery".equals(value)) {
                    transitionCancelled.add(id);
                    cancelledInvocationCount++;
                } else {
                    dispositionMismatchCount++;
                }
            }
            entry.setValue(State.COMPLETE);
        }
    }

    public synchronized void recordEvidence(PresentationDrawEvidence drawEvidence) {
        if (drawEvidence == null) throw new IllegalArgumentException("evidence required");
        Long id = Long.valueOf(drawEvidence.invocationId);
        RenderDisposition disposition = dispositions.get(id);
        if (disposition == null) throw new IllegalStateException("unknown disposition: " + id);
        if (disposition.mode != RenderDisposition.Mode.DELEGATE_TO_ART) {
            throw new IllegalStateException("evidence for non-delegated invocation: " + id);
        }
        if (!disposition.presentationEntityId.equals(drawEvidence.entityId)) {
            throw new IllegalStateException("presentation entity mismatch: " + id);
        }
        NativeRenderInvocation invocation = invocations.get(id);
        if (invocation.frameId != drawEvidence.frameId) {
            throw new IllegalStateException("presentation frame mismatch: " + id);
        }
        if (drawEvidence.drawCount < 0) {
            throw new IllegalArgumentException("draw count must not be negative");
        }
        if (evidence.containsKey(id)) throw new IllegalStateException("duplicate evidence: " + id);
        evidence.put(id, drawEvidence);
        states.put(id, State.COMPLETE);
    }

    public synchronized int delegatedWithoutEvidenceCount() {
        int count = 0;
        for (RenderDisposition disposition : dispositions.values()) {
            if (disposition.mode == RenderDisposition.Mode.DELEGATE_TO_ART
                    && !evidence.containsKey(Long.valueOf(disposition.invocationId))
                    && !transitionCancelled.contains(Long.valueOf(disposition.invocationId))) count++;
        }
        return count;
    }

    /** Close an invocation explicitly during scene/recovery cleanup. */
    public synchronized void closeInvocation(long id) {
        Long key = Long.valueOf(id);
        if (!invocations.containsKey(key)) throw new IllegalStateException("unknown invocation: " + id);
        if (states.get(key) == State.COMPLETE) throw new IllegalStateException("duplicate close: " + id);
        states.put(key, State.COMPLETE);
    }

    public synchronized boolean isOpen(long id) {
        return states.get(Long.valueOf(id)) == State.OPEN;
    }

    public synchronized NativeRenderInvocation invocation(long id) {
        return invocations.get(Long.valueOf(id));
    }

    public synchronized RenderDisposition disposition(long id) {
        return dispositions.get(Long.valueOf(id));
    }

    public synchronized PresentationDrawEvidence evidence(long id) {
        return evidence.get(Long.valueOf(id));
    }

    public synchronized int invocationCount() { return invocations.size(); }
    public synchronized int dispositionCount() { return dispositions.size(); }
    public synchronized int evidenceCount() { return evidence.size(); }

    public synchronized List<NativeRenderInvocation> invocations() {
        return Collections.unmodifiableList(new ArrayList<NativeRenderInvocation>(invocations.values()));
    }

    public synchronized List<RenderDisposition> dispositions() {
        return Collections.unmodifiableList(new ArrayList<RenderDisposition>(dispositions.values()));
    }

    public synchronized Map<String, Object> probeSlice() {
        int pass = 0;
        int capture = 0;
        int delegate = 0;
        int failOpen = 0;
        int missingEvidence = 0;
        for (RenderDisposition d : dispositions.values()) {
            if (d.mode == RenderDisposition.Mode.PASS_THROUGH) pass++;
            if (d.mode == RenderDisposition.Mode.CAPTURE_AND_PASS) capture++;
            if (d.mode == RenderDisposition.Mode.DELEGATE_TO_ART) {
                delegate++;
                if (!evidence.containsKey(Long.valueOf(d.invocationId))
                        && !transitionCancelled.contains(Long.valueOf(d.invocationId))) missingEvidence++;
            }
            if (d.mode == RenderDisposition.Mode.FAIL_OPEN) failOpen++;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("invocationCount", Integer.valueOf(invocations.size()));
        out.put("dispositionCount", Integer.valueOf(dispositions.size()));
        out.put("evidenceCount", Integer.valueOf(evidence.size()));
        out.put("passThrough", Integer.valueOf(pass));
        out.put("captureAndPass", Integer.valueOf(capture));
        out.put("delegateToArt", Integer.valueOf(delegate));
        out.put("failOpen", Integer.valueOf(failOpen));
        out.put("recoveryFailOpen", Integer.valueOf(recoveryFailOpenCount));
        out.put("delegatedWithoutEvidence", Integer.valueOf(missingEvidence));
        out.put("unknownOwner", Integer.valueOf(unknownOwnerCount));
        out.put("orphanArtOutput", Integer.valueOf(orphanArtOutputCount));
        out.put("dispositionMismatch", Integer.valueOf(dispositionMismatchCount));
        out.put("openInvocation", Integer.valueOf(openCount()));
        out.put("leakedTransientEntity", Integer.valueOf(leakedTransientEntityCount));
        out.put("cancelledInvocation", Integer.valueOf(cancelledInvocationCount));
        return out;
    }

    /** Strict NRCC counters; a zero-valued report is required before FULL acceptance. */
    public synchronized Map<String, Object> strictReport() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Map<String, Object> probe = probeSlice();
        out.put("runtimeUNKNOWN", probe.get("unknownOwner"));
        out.put("runtimeUNDECIDED", Integer.valueOf(invocations.size() - dispositions.size()));
        out.put("openInvocation", Integer.valueOf(openCount()));
        out.put("delegatedWithoutEvidence", probe.get("delegatedWithoutEvidence"));
        out.put("dispositionMismatch", probe.get("dispositionMismatch"));
        out.put("orphanArtOutput", probe.get("orphanArtOutput"));
        out.put("leakedTransientEntity", probe.get("leakedTransientEntity"));
        out.put("recoveryFailOpen", probe.get("recoveryFailOpen"));
        out.put("unrecordedFAIL_OPEN", Integer.valueOf(recoveryFailOpenCount));
        out.put("accepted", Boolean.valueOf(isStrictlyAccepted(out)));
        return Collections.unmodifiableMap(out);
    }

    /** Machine-readable NRCC acceptance; every strict error counter must be zero. */
    public synchronized boolean isStrictlyAccepted() {
        return isStrictlyAccepted(strictReportWithoutAcceptance());
    }

    private Map<String, Object> strictReportWithoutAcceptance() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Map<String, Object> probe = probeSlice();
        out.put("runtimeUNKNOWN", probe.get("unknownOwner"));
        out.put("runtimeUNDECIDED", Integer.valueOf(invocations.size() - dispositions.size()));
        out.put("openInvocation", Integer.valueOf(openCount()));
        out.put("delegatedWithoutEvidence", probe.get("delegatedWithoutEvidence"));
        out.put("dispositionMismatch", probe.get("dispositionMismatch"));
        out.put("orphanArtOutput", probe.get("orphanArtOutput"));
        out.put("leakedTransientEntity", probe.get("leakedTransientEntity"));
        out.put("recoveryFailOpen", probe.get("recoveryFailOpen"));
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
        invocations.clear();
        dispositions.clear();
        evidence.clear();
        states.clear();
        transitionCancelled.clear();
        unknownOwnerCount = 0;
        orphanArtOutputCount = 0;
        dispositionMismatchCount = 0;
        leakedTransientEntityCount = 0;
        recoveryFailOpenCount = 0;
        cancelledInvocationCount = 0;
    }

    private int openCount() {
        int count = 0;
        for (State state : states.values()) if (state == State.OPEN) count++;
        return count;
    }
}

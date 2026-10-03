package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lifecycle evidence for native transient effect instances. */
public final class TransientEffectLedger {
    public static final int DEFAULT_RECENT_CAPACITY = 256;
    /**
     * Defense-in-depth bound for the active-record window. Completion observation is the
     * primary reclamation path; this cap guarantees the active set cannot grow without limit
     * even when a host path never reports completion.
     */
    public static final int DEFAULT_ACTIVE_CAPACITY = 4096;
    public enum State { CREATED, UPDATED, RENDERED, COMPLETED, DISPOSED }

    public static final class Record {
        public final TransientEffectIdentity identity;
        public final State state;
        public final int updateCount;
        public final int renderCount;
        public final boolean doneObserved;

        private Record(TransientEffectIdentity identity, State state, int updateCount,
                int renderCount, boolean doneObserved) {
            this.identity = identity;
            this.state = state;
            this.updateCount = updateCount;
            this.renderCount = renderCount;
            this.doneObserved = doneObserved;
        }
    }

    private static final class MutableRecord {
        final TransientEffectIdentity identity;
        State state = State.CREATED;
        int updateCount;
        int renderCount;
        boolean doneObserved;

        MutableRecord(TransientEffectIdentity identity) {
            this.identity = identity;
        }
    }

    private final Map<String, MutableRecord> records = new LinkedHashMap<String, MutableRecord>();
    private final Map<String, MutableRecord> recent = new LinkedHashMap<String, MutableRecord>();
    private final Map<String, TransientEffectIdentity> staleIdentities =
            new LinkedHashMap<String, TransientEffectIdentity>();
    private final int recentCapacity;
    private final int activeCapacity;
    private int unknownLifecycleCount;
    private int leakedCount;
    private int failOpenCount;
    private int totalCount;
    private int evictedCount;
    private int activeEvictedCount;
    /** Observations rejected because they matched a genuine terminal record for the same object. */
    private int rejectedTerminalObservationCount;

    public TransientEffectLedger() {
        this(DEFAULT_RECENT_CAPACITY, DEFAULT_ACTIVE_CAPACITY);
    }

    public TransientEffectLedger(int recentCapacity) {
        this(recentCapacity, DEFAULT_ACTIVE_CAPACITY);
    }

    public TransientEffectLedger(int recentCapacity, int activeCapacity) {
        if (recentCapacity < 1) throw new IllegalArgumentException("recent capacity required");
        if (activeCapacity < 1) throw new IllegalArgumentException("active capacity required");
        this.recentCapacity = recentCapacity;
        this.activeCapacity = activeCapacity;
    }

    public synchronized void create(TransientEffectIdentity identity) {
        if (identity == null) throw new IllegalArgumentException("identity required");
        MutableRecord active = records.get(identity.instanceId);
        if (active != null) return;
        MutableRecord terminal = recent.get(identity.instanceId);
        if (terminal != null) recent.remove(identity.instanceId);
        staleIdentities.remove(identity.instanceId);
        insertActive(identity.instanceId, new MutableRecord(identity));
    }

    public synchronized void update(TransientEffectIdentity identity, boolean done) {
        MutableRecord record = record(identity);
        if (record == null) return;
        if (record.state == State.COMPLETED || record.state == State.DISPOSED) {
            // A repeated done==true after COMPLETED or DISPOSED is a deliberate silent no-op: the
            // container instrument and the class-level super.update() Postfix can both observe the
            // same completed instance. A done==false post-termination update still throws
            // "update after effect termination" for direct ledger misuse.
            if (done) return;
            throw new IllegalStateException("update after effect termination: " + identity.instanceId);
        }
        record.updateCount++;
        record.doneObserved = record.doneObserved || done;
        if (done) {
            complete(identity);
        } else {
            record.state = State.UPDATED;
        }
    }

    /**
     * Atomic idempotent observation entry: performs the update only when the identity still owns
     * an active record, all under one lock. Returns {@code true} when the update ran and
     * {@code false} when the instance is absent or already terminal, so a benign re-observation
     * of a completed instance never throws out of an observation path. A repeated
     * {@code done == true} after {@link State#COMPLETED} or {@link State#DISPOSED} is a
     * deliberate silent no-op; a {@code done == false} update after termination still throws.
     */
    public synchronized boolean updateIfActive(TransientEffectIdentity identity, boolean done) {
        if (!hasActiveRecord(identity)) return false;
        update(identity, done);
        return true;
    }

    public synchronized void render(TransientEffectIdentity identity) {
        admitRender(identity);
    }

    /** Admits a native render only for a current or explicitly new effect instance. */
    public synchronized boolean admitRender(TransientEffectIdentity identity) {
        if (identity == null) {
            unknownLifecycleCount++;
            return false;
        }
        MutableRecord active = records.get(identity.instanceId);
        if (active != null) {
            if (!sameIdentity(active.identity, identity)) {
                unknownLifecycleCount++;
                return false;
            }
            active.renderCount++;
            active.state = State.RENDERED;
            return true;
        }
        MutableRecord terminal = recent.get(identity.instanceId);
        if (terminal != null && sameIdentity(terminal.identity, identity)) {
            rejectedTerminalObservationCount++;
            unknownLifecycleCount++;
            return false;
        }
        TransientEffectIdentity stale = staleIdentities.get(identity.instanceId);
        if (stale != null && sameIdentity(stale, identity)) {
            rejectedTerminalObservationCount++;
            unknownLifecycleCount++;
            return false;
        }
        if (terminal != null || stale != null) {
            unknownLifecycleCount++;
            return false;
        }
        MutableRecord created = new MutableRecord(identity);
        created.renderCount++;
        created.state = State.RENDERED;
        insertActive(identity.instanceId, created);
        return true;
    }

    /** True iff {@code identity} currently owns an active (non-terminal) record. */

    public synchronized boolean hasActiveRecord(TransientEffectIdentity identity) {
        if (identity == null) return false;
        MutableRecord active = records.get(identity.instanceId);
        return active != null && sameIdentity(active.identity, identity);
    }

    /**
     * Removes an active record WITHOUT retaining a terminal record (container observation detach).
     *
     * <p>Used by the container AFTER-update observation path, where {@code isDone} is not a reliable
     * end-of-life signal: the object may still be rendered (not yet removed, or re-added) after it
     * reports done. Retaining a terminal record would then make its next render a terminal
     * rejection (under NRM-13 sticky per-object ids) and inflate {@code rejectedTerminal}/
     * {@code unknownLifecycle} once per render. Detaching simply drops the active record so a later
     * render of the same object is re-admitted as a fresh active record; memory stays bounded
     * because the active record is released.
     *
     * <p>Does not touch {@code recent}, {@code staleIdentities}, {@code totalCount}, or the
     * completed count. Returns {@code true} only when an active record for {@code identity} was
     * present and removed; {@code false} for a null/absent/terminal identity. Never throws.
     */
    public synchronized boolean detach(TransientEffectIdentity identity) {
        if (identity == null) return false;
        MutableRecord active = records.get(identity.instanceId);
        if (active == null || !sameIdentity(active.identity, identity)) return false;
        return removeActive(active);
    }

    public synchronized void complete(TransientEffectIdentity identity) {
        MutableRecord record = record(identity);
        if (record == null) return;
        if (record.state == State.COMPLETED || record.state == State.DISPOSED) {
            throw new IllegalStateException("duplicate effect completion: " + identity.instanceId);
        }
        record.doneObserved = true;
        record.state = State.COMPLETED;
        if (removeActive(record)) {
            totalCount++;
            retainRecent(record);
        }
    }

    public synchronized void dispose(TransientEffectIdentity identity) {
        MutableRecord record = record(identity);
        if (record == null) return;
        if (record.state == State.DISPOSED) {
            throw new IllegalStateException("duplicate effect dispose: " + identity.instanceId);
        }
        record.state = State.DISPOSED;
        if (removeActive(record)) {
            totalCount++;
            retainRecent(record);
        }
    }

    public synchronized void clearLeaked() {
        for (MutableRecord record : records.values()) {
            if (record.state != State.DISPOSED && record.state != State.COMPLETED) leakedCount++;
        }
        markStale(records.values());
        records.clear();
    }

    /**
     * Drops recovery-owned ACTIVE records. Recovery ends every in-flight delegation, so the
     * ledger's active window is empty afterwards. An effect that is still live and rendered after
     * recovery is then RE-ADMITTED as a fresh active record by {@link #admitRender} rather than
     * rejected as terminal.
     *
     * <p>This deliberately does NOT stale-mark the dropped records. Since NRM-13, instance ids are
     * non-reusable per-object monotonic ids ({@code class#<seq>}), so {@code staleIdentities} can
     * only ever match the SAME object; it no longer protects against id reuse by a DIFFERENT
     * object. Marking a still-live record stale therefore only harms that object: on the next
     * render of the same live object, {@link #admitRender} would find its own id in
     * {@code staleIdentities} and increment {@code rejectedTerminal}/{@code unknownLifecycle} once
     * per render forever (the D1 defect: ~3900/s after {@code art present clear-panic}, permanently
     * forcing strict acceptance false). {@code staleIdentities} remains in use for its genuine
     * TERMINAL purpose only — the recent-window eviction path in {@link #retainRecent}.
     */
    public synchronized void clearCompletedForRecovery() {
        records.clear();
    }

    public synchronized int activeCount() {
        return records.size();
    }
    public synchronized int recentCount() { return recent.size(); }
    public synchronized int activeCapacity() { return activeCapacity; }
    public synchronized int totalCount() { return totalCount; }
    /** Recent-window (terminal diagnostics) evictions. */
    public synchronized int evictedCount() { return evictedCount; }
    /** Active-window cap evictions; distinct from {@link #evictedCount()}. */
    public synchronized int activeEvictedCount() { return activeEvictedCount; }
    /**
     * Genuine late observations of an already-terminal instance (same object re-observed).
     * Stays ~0 once instance ids are non-reusable; a rising value indicates id reuse.
     */
    public synchronized int rejectedTerminalObservationCount() {
        return rejectedTerminalObservationCount;
    }
    public synchronized int staleIdentityCount() { return staleIdentities.size(); }
    public synchronized int unknownLifecycleCount() { return unknownLifecycleCount; }
    public synchronized int leakedCount() { return leakedCount; }

    /** Records one observation-path failure that failed open without blocking native drawing. */
    public synchronized void recordFailOpen() { failOpenCount++; }

    public synchronized int failOpenCount() { return failOpenCount; }

    public synchronized List<Record> records() {
        List<Record> snapshot = new ArrayList<Record>(records.size() + recent.size());
        for (MutableRecord record : records.values()) snapshot.add(snapshot(record));
        for (MutableRecord record : recent.values()) snapshot.add(snapshot(record));
        return Collections.unmodifiableList(snapshot);
    }

    public synchronized Map<String, Object> probeSlice() {
        int created = 0;
        int updated = 0;
        int rendered = 0;
        int completed = 0;
        int disposed = 0;
        for (MutableRecord record : records.values()) {
            if (record.state == State.CREATED) created++;
            if (record.state == State.UPDATED) updated++;
            if (record.state == State.RENDERED) rendered++;
            if (record.state == State.COMPLETED) completed++;
        }
        for (MutableRecord record : recent.values()) {
            if (record.state == State.COMPLETED) completed++;
            if (record.state == State.DISPOSED) disposed++;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("active", Integer.valueOf(activeCount()));
        out.put("activeCap", Integer.valueOf(activeCapacity));
        out.put("recent", Integer.valueOf(recent.size()));
        out.put("total", Integer.valueOf(totalCount));
        out.put("evicted", Integer.valueOf(evictedCount));
        out.put("activeEvicted", Integer.valueOf(activeEvictedCount));
        out.put("staleIdentity", Integer.valueOf(staleIdentities.size()));
        out.put("created", Integer.valueOf(created));
        out.put("updated", Integer.valueOf(updated));
        out.put("rendered", Integer.valueOf(rendered));
        out.put("completed", Integer.valueOf(completed));
        out.put("disposed", Integer.valueOf(disposed));
        out.put("unknownLifecycle", Integer.valueOf(unknownLifecycleCount));
        out.put("rejectedTerminal", Integer.valueOf(rejectedTerminalObservationCount));
        out.put("leaked", Integer.valueOf(leakedCount));
        out.put("failOpen", Integer.valueOf(failOpenCount));
        return out;
    }

    public synchronized void reset() {
        records.clear();
        recent.clear();
        staleIdentities.clear();
        unknownLifecycleCount = 0;
        leakedCount = 0;
        failOpenCount = 0;
        totalCount = 0;
        evictedCount = 0;
        activeEvictedCount = 0;
        rejectedTerminalObservationCount = 0;
    }

    private MutableRecord record(TransientEffectIdentity identity) {
        if (identity == null) {
            unknownLifecycleCount++;
            return null;
        }
        MutableRecord record = records.get(identity.instanceId);
        if (record == null) record = recent.get(identity.instanceId);
        if (record != null && !sameIdentity(record.identity, identity)) record = null;
        if (record == null) unknownLifecycleCount++;
        return record;
    }

    private void retainRecent(MutableRecord record) {
        recent.put(record.identity.instanceId, record);
        staleIdentities.remove(record.identity.instanceId);
        while (recent.size() > recentCapacity) {
            String oldest = recent.keySet().iterator().next();
            MutableRecord evicted = recent.remove(oldest);
            staleIdentities.put(oldest, evicted.identity);
            trimStaleIdentities();
            evictedCount++;
        }
    }

    /**
     * Inserts one active record and enforces {@link #activeCapacity}. On cap overflow the oldest
     * active record is simply DROPPED from tracking: it is NOT routed into the terminal
     * {@code recent}/{@code staleIdentities} windows and the recent-window {@code evictedCount} is
     * NOT advanced. {@code recent}/{@code staleIdentities} therefore remain a genuine record of
     * dead effects only; a still-live instance whose active record was cap-evicted is re-admitted
     * by {@code admitRender} as a normal active record (it is alive), and it completes normally.
     * Each active-window eviction is counted once in {@link #activeEvictedCount}, kept distinct
     * from the recent-window {@code evictedCount}.
     */
    private void insertActive(String instanceId, MutableRecord record) {
        records.put(instanceId, record);
        while (records.size() > activeCapacity) {
            String oldest = records.keySet().iterator().next();
            records.remove(oldest);
            activeEvictedCount++;
        }
    }

    /** Removes an active record; returns true only when it was actually present. */
    private boolean removeActive(MutableRecord record) {
        return records.remove(record.identity.instanceId) != null;
    }

    private void markStale(Iterable<MutableRecord> source) {
        for (MutableRecord record : source) staleIdentities.put(record.identity.instanceId, record.identity);
        trimStaleIdentities();
    }

    private void trimStaleIdentities() {
        while (staleIdentities.size() > recentCapacity) {
            staleIdentities.remove(staleIdentities.keySet().iterator().next());
        }
    }

    /**
     * Identity equality for ledger records. {@code instanceId} is a non-reusable per-object id
     * assigned by the bridge, so an equal {@code instanceId} already implies the same live object;
     * {@code nativeIdentityHash} is diagnostic only and deliberately does not participate (its
     * values are reused after an object dies).
     */
    private static boolean sameIdentity(TransientEffectIdentity first,
            TransientEffectIdentity second) {
        return first.instanceId.equals(second.instanceId)
                && first.nativeClass.equals(second.nativeClass)
                && first.generation == second.generation;
    }

    private static Record snapshot(MutableRecord record) {
        return new Record(record.identity, record.state, record.updateCount,
                record.renderCount, record.doneObserved);
    }
}

package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lifecycle evidence for native transient effect instances. */
public final class TransientEffectLedger {
    public static final int DEFAULT_RECENT_CAPACITY = 256;
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
    private int unknownLifecycleCount;
    private int leakedCount;
    private int failOpenCount;
    private int totalCount;
    private int evictedCount;

    public TransientEffectLedger() {
        this(DEFAULT_RECENT_CAPACITY);
    }

    public TransientEffectLedger(int recentCapacity) {
        if (recentCapacity < 1) throw new IllegalArgumentException("recent capacity required");
        this.recentCapacity = recentCapacity;
    }

    public synchronized void create(TransientEffectIdentity identity) {
        if (identity == null) throw new IllegalArgumentException("identity required");
        MutableRecord active = records.get(identity.instanceId);
        if (active != null) return;
        MutableRecord terminal = recent.get(identity.instanceId);
        if (terminal != null) recent.remove(identity.instanceId);
        staleIdentities.remove(identity.instanceId);
        records.put(identity.instanceId, new MutableRecord(identity));
    }

    public synchronized void update(TransientEffectIdentity identity, boolean done) {
        MutableRecord record = record(identity);
        if (record == null) return;
        if (record.state == State.COMPLETED || record.state == State.DISPOSED) {
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
            unknownLifecycleCount++;
            return false;
        }
        TransientEffectIdentity stale = staleIdentities.get(identity.instanceId);
        if (stale != null && sameIdentity(stale, identity)) {
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
        records.put(identity.instanceId, created);
        return true;
    }

    public synchronized void complete(TransientEffectIdentity identity) {
        MutableRecord record = record(identity);
        if (record == null) return;
        if (record.state == State.COMPLETED || record.state == State.DISPOSED) {
            throw new IllegalStateException("duplicate effect completion: " + identity.instanceId);
        }
        record.doneObserved = true;
        record.state = State.COMPLETED;
        if (records.remove(identity.instanceId) != null) {
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
        if (records.remove(identity.instanceId) != null) {
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

    /** Drops recovery-owned records without classifying them as application leaks. */
    public synchronized void clearCompletedForRecovery() {
        markStale(records.values());
        records.clear();
    }

    public synchronized int activeCount() {
        return records.size();
    }
    public synchronized int recentCount() { return recent.size(); }
    public synchronized int totalCount() { return totalCount; }
    public synchronized int evictedCount() { return evictedCount; }
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
        out.put("recent", Integer.valueOf(recent.size()));
        out.put("total", Integer.valueOf(totalCount));
        out.put("evicted", Integer.valueOf(evictedCount));
        out.put("staleIdentity", Integer.valueOf(staleIdentities.size()));
        out.put("created", Integer.valueOf(created));
        out.put("updated", Integer.valueOf(updated));
        out.put("rendered", Integer.valueOf(rendered));
        out.put("completed", Integer.valueOf(completed));
        out.put("disposed", Integer.valueOf(disposed));
        out.put("unknownLifecycle", Integer.valueOf(unknownLifecycleCount));
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

    private void markStale(Iterable<MutableRecord> source) {
        for (MutableRecord record : source) staleIdentities.put(record.identity.instanceId, record.identity);
        trimStaleIdentities();
    }

    private void trimStaleIdentities() {
        while (staleIdentities.size() > recentCapacity) {
            staleIdentities.remove(staleIdentities.keySet().iterator().next());
        }
    }

    private static boolean sameIdentity(TransientEffectIdentity first,
            TransientEffectIdentity second) {
        return first.instanceId.equals(second.instanceId)
                && first.nativeClass.equals(second.nativeClass)
                && first.nativeIdentityHash == second.nativeIdentityHash
                && first.generation == second.generation;
    }

    private static Record snapshot(MutableRecord record) {
        return new Record(record.identity, record.state, record.updateCount,
                record.renderCount, record.doneObserved);
    }
}

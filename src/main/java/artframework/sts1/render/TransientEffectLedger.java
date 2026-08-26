package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lifecycle evidence for native transient effect instances. */
public final class TransientEffectLedger {
    public enum State { CREATED, UPDATED, RENDERED, COMPLETED, DISPOSED }

    public static final class Record {
        public final TransientEffectIdentity identity;
        public State state;
        public int updateCount;
        public int renderCount;
        public boolean doneObserved;

        Record(TransientEffectIdentity identity) {
            this.identity = identity;
            this.state = State.CREATED;
        }
    }

    private final Map<String, Record> records = new LinkedHashMap<String, Record>();
    private int unknownLifecycleCount;
    private int leakedCount;
    private int failOpenCount;

    public synchronized void create(TransientEffectIdentity identity) {
        if (identity == null) throw new IllegalArgumentException("identity required");
        if (records.containsKey(identity.instanceId)) return;
        records.put(identity.instanceId, new Record(identity));
    }

    public synchronized void update(TransientEffectIdentity identity, boolean done) {
        Record record = record(identity);
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
        Record record = record(identity);
        if (record == null) return;
        record.renderCount++;
        record.state = State.RENDERED;
    }

    public synchronized void complete(TransientEffectIdentity identity) {
        Record record = record(identity);
        if (record == null) return;
        if (record.state == State.COMPLETED || record.state == State.DISPOSED) {
            throw new IllegalStateException("duplicate effect completion: " + identity.instanceId);
        }
        record.doneObserved = true;
        record.state = State.COMPLETED;
    }

    public synchronized void dispose(TransientEffectIdentity identity) {
        Record record = record(identity);
        if (record == null) return;
        if (record.state == State.DISPOSED) {
            throw new IllegalStateException("duplicate effect dispose: " + identity.instanceId);
        }
        record.state = State.DISPOSED;
    }

    public synchronized void clearLeaked() {
        for (Record record : records.values()) {
            if (record.state != State.DISPOSED && record.state != State.COMPLETED) leakedCount++;
        }
        records.clear();
    }

    public synchronized int activeCount() {
        int count = 0;
        for (Record record : records.values()) {
            if (record.state != State.COMPLETED && record.state != State.DISPOSED) count++;
        }
        return count;
    }
    public synchronized int unknownLifecycleCount() { return unknownLifecycleCount; }
    public synchronized int leakedCount() { return leakedCount; }

    /** Records one observation-path failure that failed open without blocking native drawing. */
    public synchronized void recordFailOpen() { failOpenCount++; }

    public synchronized int failOpenCount() { return failOpenCount; }

    public synchronized List<Record> records() {
        return Collections.unmodifiableList(new ArrayList<Record>(records.values()));
    }

    public synchronized Map<String, Object> probeSlice() {
        int created = 0;
        int updated = 0;
        int rendered = 0;
        int completed = 0;
        for (Record record : records.values()) {
            if (record.state == State.CREATED) created++;
            if (record.state == State.UPDATED) updated++;
            if (record.state == State.RENDERED) rendered++;
            if (record.state == State.COMPLETED) completed++;
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("active", Integer.valueOf(activeCount()));
        out.put("created", Integer.valueOf(created));
        out.put("updated", Integer.valueOf(updated));
        out.put("rendered", Integer.valueOf(rendered));
        out.put("completed", Integer.valueOf(completed));
        out.put("unknownLifecycle", Integer.valueOf(unknownLifecycleCount));
        out.put("leaked", Integer.valueOf(leakedCount));
        out.put("failOpen", Integer.valueOf(failOpenCount));
        return out;
    }

    public synchronized void reset() {
        records.clear();
        unknownLifecycleCount = 0;
        leakedCount = 0;
        failOpenCount = 0;
    }

    private Record record(TransientEffectIdentity identity) {
        if (identity == null) {
            unknownLifecycleCount++;
            return null;
        }
        Record record = records.get(identity.instanceId);
        if (record == null) unknownLifecycleCount++;
        return record;
    }
}

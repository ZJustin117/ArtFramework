package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One ordered broadcast bus. Exact and regular-expression subscriptions share a registration
 * sequence; the matching set is captured when emit begins.
 */
final class SignalBus {
    private final List<Entry> entries = new ArrayList<Entry>();
    private long sequence;

    public SignalSubscription connect(String name, SignalListener listener) {
        if (name == null || name.isEmpty() || listener == null) {
            throw new IllegalArgumentException("name and listener required");
        }
        return add(name, null, listener);
    }

    public SignalSubscription connect(Pattern pattern, SignalListener listener) {
        if (pattern == null || listener == null) {
            throw new IllegalArgumentException("pattern and listener required");
        }
        return add(null, pattern, listener);
    }

    public SignalDispatchResult emit(UiSignal original) {
        if (original == null) {
            throw new IllegalArgumentException("signal required");
        }
        return emitCaptured(original, capture(original));
    }

    List<Entry> capture(UiSignal original) {
        List<Entry> matching = new ArrayList<Entry>();
        synchronized (this) {
            for (Entry entry : entries) {
                if (entry.connected && entry.matches(original.name)) matching.add(entry);
            }
        }
        Collections.sort(matching, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) { return a.sequence < b.sequence ? -1 : a.sequence == b.sequence ? 0 : 1; }
        });
        return matching;
    }

    SignalDispatchResult emitCaptured(UiSignal original, List<Entry> matching) {
        UiSignal current = original;
        for (Entry entry : matching) {
            SignalDecision decision = entry.listener.onSignal(current);
            if (decision == null || decision.kind == SignalDecision.Kind.CONTINUE) continue;
            if (decision.kind == SignalDecision.Kind.REPLACE) {
                if (decision.signal == null || !current.group.equals(decision.signal.group)
                        || !current.id.equals(decision.signal.id)
                        || !current.name.equals(decision.signal.name)) {
                    return new SignalDispatchResult(current, SignalDecision.Kind.STOP_REJECTED,
                            "replacement must preserve signal group, id and name");
                }
                current = decision.signal;
                continue;
            }
            return new SignalDispatchResult(current, decision.kind, decision.message);
        }
        return new SignalDispatchResult(current, SignalDecision.Kind.CONTINUE, "");
    }

    public synchronized void clear() {
        for (Entry entry : entries) entry.connected = false;
        entries.clear();
    }

    private synchronized SignalSubscription add(String name, Pattern pattern, SignalListener listener) {
        final Entry entry = new Entry(++sequence, name, pattern, listener);
        entries.add(entry);
        return entry;
    }

    final class Entry implements SignalSubscription {
        final long sequence;
        final String name;
        final Pattern pattern;
        final SignalListener listener;
        boolean connected = true;
        Entry(long sequence, String name, Pattern pattern, SignalListener listener) {
            this.sequence = sequence; this.name = name; this.pattern = pattern; this.listener = listener;
        }
        boolean matches(String candidate) { return name != null ? name.equals(candidate) : pattern.matcher(candidate).matches(); }
        @Override public synchronized void disconnect() { connected = false; }
        @Override public synchronized boolean isConnected() { return connected; }
    }
}

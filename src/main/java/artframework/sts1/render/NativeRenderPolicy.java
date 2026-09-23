package artframework.sts1.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** ECS-owned, host-neutral native render isolation policy snapshot. */
public final class NativeRenderPolicy {
    public enum Kind { FAMILY, SURFACE, CLASS, METHOD }
    public static final class Target {
        public final Kind kind;
        public final String value;
        private Target(Kind kind, String value) { this.kind = kind; this.value = value; }
        public static Target parse(String raw) {
            if (raw == null) throw new IllegalArgumentException("target required");
            String text = raw.trim();
            if (text.length() == 0 || "--".equals(text)) throw new IllegalArgumentException("target required");
            int colon = text.indexOf(':');
            if (colon < 0) return new Target(Kind.FAMILY, key(text));
            String prefix = text.substring(0, colon).toLowerCase(Locale.ENGLISH);
            String value = text.substring(colon + 1).trim();
            if (value.length() == 0 || "--".equals(value)) throw new IllegalArgumentException("target required");
            Kind kind;
            if ("family".equals(prefix)) kind = Kind.FAMILY;
            else if ("surface".equals(prefix)) kind = Kind.SURFACE;
            else if ("class".equals(prefix)) kind = Kind.CLASS;
            else if ("method".equals(prefix)) kind = Kind.METHOD;
            else throw new IllegalArgumentException("unknown target: " + prefix);
            if (kind == Kind.METHOD) {
                int first = value.indexOf('#');
                if (first <= 0 || first != value.lastIndexOf('#')
                        || first == value.length() - 1) {
                    throw new IllegalArgumentException("method target requires exactly <fqcn>#<method>");
                }
            }
            return new Target(kind, key(value));
        }
        private static String key(String value) { return value.trim().toLowerCase(Locale.ENGLISH); }
        public String text() { return kind.name().toLowerCase(Locale.ENGLISH) + ":" + value; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Target)) return false;
            Target that = (Target) other;
            return kind == that.kind && value.equals(that.value);
        }
        @Override public int hashCode() { return 31 * kind.hashCode() + value.hashCode(); }
        public boolean matches(NativeRenderInvocation i) {
            if (i == null) return false;
            if (kind == Kind.FAMILY) return value.equals(key(i.surfaceFamily));
            if (kind == Kind.SURFACE) return value.equals(key(i.ownerId));
            if (kind == Kind.CLASS) return value.equals(key(i.nativeClass));
            return value.equals(key(i.nativeClass + "#" + i.nativeMethod));
        }
    }
    /** Immutable host-neutral policy frame used by projection systems. */
    public static final class Snapshot {
        public final boolean isolate;
        public final long revision;
        private final List<Target> targets;
        private Snapshot(boolean isolate, long revision, List<Target> targets) {
            this.isolate = isolate; this.revision = revision;
            this.targets = Collections.unmodifiableList(new ArrayList<Target>(targets));
        }
        public boolean exempt(NativeRenderInvocation invocation) {
            return matchedTarget(invocation).length() > 0;
        }
        public String matchedTarget(NativeRenderInvocation invocation) {
            for (Target target : targets) if (target.matches(invocation)) return target.text();
            return "";
        }
    }
    private boolean isolate;
    private long revision;
    private final Set<Target> exemptions = new LinkedHashSet<Target>();
    private int isolatedCount;
    private int exemptedCount;
    private boolean lastExemptionNativeContinuation;
    private final Runnable mutationListener;

    public NativeRenderPolicy() { this(null); }

    NativeRenderPolicy(Runnable mutationListener) {
        this.mutationListener = mutationListener;
    }

    public void setIsolate(boolean enabled) {
        synchronized (this) { isolate = enabled; revision++; }
        notifyMutation();
    }
    public synchronized boolean isIsolateActive() { return isolate; }
    public void allow(Target target) {
        boolean changed = false;
        synchronized (this) { if (target != null && exemptions.add(target)) { revision++; changed = true; } }
        if (changed) notifyMutation();
    }
    public void deny(Target target) {
        boolean changed = false;
        synchronized (this) { if (target != null && exemptions.remove(target)) { revision++; changed = true; } }
        if (changed) notifyMutation();
    }
    public void clear() {
        boolean changed = false;
        synchronized (this) { if (!exemptions.isEmpty()) { exemptions.clear(); revision++; changed = true; } }
        if (changed) notifyMutation();
    }
    public void reset() {
        synchronized (this) { isolate = false; exemptions.clear(); isolatedCount = 0; exemptedCount = 0;
            lastExemptionNativeContinuation = false; revision = 0L; }
        notifyMutation();
    }

    private void notifyMutation() {
        if (mutationListener != null) mutationListener.run();
    }
    public synchronized List<String> exemptionTargets() { List<String> r = new ArrayList<String>(); for (Target t : exemptions) r.add(t.text()); return Collections.unmodifiableList(r); }
    public synchronized boolean exempt(NativeRenderInvocation i) { return matchedTarget(i).length() > 0; }
    public synchronized String matchedTarget(NativeRenderInvocation i) { for (Target t : exemptions) if (t.matches(i)) return t.text(); return ""; }
    public synchronized void recordIsolated() { isolatedCount++; }
    public synchronized void recordExempted() { exemptedCount++; }
    public synchronized void recordExempted(boolean nativeContinuation) {
        exemptedCount++;
        lastExemptionNativeContinuation = nativeContinuation;
    }
    public synchronized long revision() { return revision; }
    public synchronized Snapshot snapshot() { return new Snapshot(isolate, revision,
            new ArrayList<Target>(exemptions)); }
    public synchronized java.util.Map<String, Object> probeSlice() {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        m.put("active", Boolean.valueOf(isolate)); m.put("targets", exemptionTargets());
        m.put("resolvedExemptions", Integer.valueOf(exemptedCount));
        m.put("lastExemptionNativeContinuation", Boolean.valueOf(lastExemptionNativeContinuation));
        m.put("suppressionCounters", Integer.valueOf(isolatedCount)); m.put("revision", Long.valueOf(revision));
        return Collections.unmodifiableMap(m);
    }
}

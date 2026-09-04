package artframework.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registry for isolated operation signal groups. */
public final class SignalGroups {
    public static final String DEFAULT = "native";
    /** Isolated group for framework-owned transient runtime commands. */
    public static final String TRANSIENT_RUNTIME = "transient-runtime";
    private static final Map<String, SignalGroup> GROUPS = new LinkedHashMap<String, SignalGroup>();
    private static final Map<SignalGroup, Integer> ISOLATED_RUNTIME_OWNERS =
            new LinkedHashMap<SignalGroup, Integer>();

    static { GROUPS.put(DEFAULT, new SignalGroup(DEFAULT)); }

    private SignalGroups() {}

    public static synchronized SignalGroup get(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("group id required");
        SignalGroup group = GROUPS.get(id);
        if (group == null) {
            group = new SignalGroup(id, TRANSIENT_RUNTIME.equals(id));
            GROUPS.put(id, group);
        }
        return group;
    }

    public static SignalGroup nativeGroup() { return get(DEFAULT); }

    /**
     * Process-wide raw interoperability group. Typed transient subscriptions and dispatch belong
     * to {@link TransientSignalRuntime}; callers using this group directly retain raw UiSignal
     * compatibility only.
     */
    public static SignalGroup transientRuntimeRawGroup() { return get(TRANSIENT_RUNTIME); }

    /**
     * @deprecated Use {@link #transientRuntimeRawGroup()} only for legacy raw UiSignal
     * compatibility, or {@link TransientSignalRuntime} for typed transient registrations.
     */
    @Deprecated public static SignalGroup transientRuntimeGroup() {
        return transientRuntimeRawGroup();
    }

    /** Creates a schedule-local group so ad-hoc schedules cannot duplicate global consumers. */
    public static synchronized SignalGroup isolatedTransientRuntimeGroup() {
        String id;
        do {
            id = TRANSIENT_RUNTIME + "-schedule-" + (++isolatedSequence);
        } while (GROUPS.containsKey(id));
        SignalGroup group = new SignalGroup(id);
        GROUPS.put(id, group);
        return group;
    }

    /** Acquires a schedule-local group lease when a runtime starts using it. */
    static synchronized boolean retainIsolatedTransientRuntimeGroup(SignalGroup group) {
        if (!isIsolatedTransientRuntimeGroup(group)) return false;
        Integer owners = ISOLATED_RUNTIME_OWNERS.get(group);
        ISOLATED_RUNTIME_OWNERS.put(group, owners == null ? 1 : owners + 1);
        return true;
    }

    /** Releases one schedule-local group lease without touching shared or custom groups. */
    static synchronized void disposeIsolatedTransientRuntimeGroup(SignalGroup group) {
        Integer owners = ISOLATED_RUNTIME_OWNERS.get(group);
        if (owners == null) return;
        if (owners > 1) {
            ISOLATED_RUNTIME_OWNERS.put(group, owners - 1);
            return;
        }
        ISOLATED_RUNTIME_OWNERS.remove(group);
        if (GROUPS.get(group.id()) == group) {
            group.clear();
            GROUPS.remove(group.id());
        }
    }

    /** Test-visible identity query that does not create a missing group. */
    public static synchronized boolean isRegistered(SignalGroup group) {
        return group != null && GROUPS.get(group.id()) == group;
    }

    private static long isolatedSequence;

    /** Package boundary used to retire runtime owners when test reset replaces their group. */
    static synchronized boolean isCurrentTransientRuntimeGroup(SignalGroup group) {
        return GROUPS.get(TRANSIENT_RUNTIME) == group;
    }

    public static synchronized void resetForTests() {
        for (SignalGroup group : GROUPS.values()) group.clear();
        GROUPS.clear();
        ISOLATED_RUNTIME_OWNERS.clear();
        GROUPS.put(DEFAULT, new SignalGroup(DEFAULT));
    }

    private static boolean isIsolatedTransientRuntimeGroup(SignalGroup group) {
        return group != null && group.id().startsWith(TRANSIENT_RUNTIME + "-schedule-")
                && GROUPS.get(group.id()) == group;
    }
}

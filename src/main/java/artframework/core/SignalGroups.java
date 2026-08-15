package artframework.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registry for isolated operation signal groups. */
public final class SignalGroups {
    public static final String DEFAULT = "native";
    private static final Map<String, SignalGroup> GROUPS = new LinkedHashMap<String, SignalGroup>();

    static { GROUPS.put(DEFAULT, new SignalGroup(DEFAULT)); }

    private SignalGroups() {}

    public static synchronized SignalGroup get(String id) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("group id required");
        SignalGroup group = GROUPS.get(id);
        if (group == null) {
            group = new SignalGroup(id);
            GROUPS.put(id, group);
        }
        return group;
    }

    public static SignalGroup nativeGroup() { return get(DEFAULT); }

    public static synchronized void resetForTests() {
        for (SignalGroup group : GROUPS.values()) group.clear();
        GROUPS.clear();
        GROUPS.put(DEFAULT, new SignalGroup(DEFAULT));
    }
}

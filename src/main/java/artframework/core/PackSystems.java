package artframework.core;

import artframework.ecs.EcsSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime-owned, phase-bound system extensions. PresentationSchedule remains the sole scheduler. */
public final class PackSystems {
    private static final Map<PackSystemPhase, Map<String, EcsSystem>> BY_PHASE =
            new EnumMap<PackSystemPhase, Map<String, EcsSystem>>(PackSystemPhase.class);

    private PackSystems() {}

    public static void enable(PackSystemPhase phase, String id, EcsSystem system) {
        if (phase == null || id == null || id.isEmpty() || system == null) {
            throw new IllegalArgumentException("phase, system id, and system required");
        }
        Map<String, EcsSystem> systems = BY_PHASE.get(phase);
        if (systems == null) {
            systems = new LinkedHashMap<String, EcsSystem>();
            BY_PHASE.put(phase, systems);
        }
        EcsSystem existing = systems.get(id);
        if (existing != null) {
            throw new IllegalStateException("system already enabled: " + id);
        }
        systems.put(id, system);
    }

    public static void disable(PackSystemPhase phase, String id) {
        Map<String, EcsSystem> systems = BY_PHASE.get(phase);
        if (systems != null) systems.remove(id);
    }

    public static List<EcsSystem> systemsFor(PackSystemPhase phase) {
        Map<String, EcsSystem> systems = BY_PHASE.get(phase);
        return systems == null ? Collections.<EcsSystem>emptyList()
                : Collections.unmodifiableList(new ArrayList<EcsSystem>(systems.values()));
    }

    public static void resetForTests() {
        BY_PHASE.clear();
    }
}

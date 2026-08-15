package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable operation delivered inside one signal group. */
public final class UiSignal {
    public final String group;
    public final String id;
    public final String name;
    public final String source;
    public final Object payload;
    public final Map<String, Object> metadata;

    public UiSignal(String name, String source, Object payload) {
        this(SignalGroups.DEFAULT, null, name, source, payload, null);
    }

    public UiSignal(
            String id, String name, String source, Object payload, Map<String, Object> metadata) {
        this(SignalGroups.DEFAULT, id, name, source, payload, metadata);
    }

    public UiSignal(String group, String id, String name, String source, Object payload,
            Map<String, Object> metadata) {
        if (group == null || group.isEmpty()) {
            throw new IllegalArgumentException("signal group required");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("signal name required");
        }
        this.group = group;
        this.id = id != null && !id.isEmpty() ? id : UUID.randomUUID().toString();
        this.name = name;
        this.source = source != null ? source : "";
        this.payload = payload;
        this.metadata = metadata == null || metadata.isEmpty()
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
    }

    /** Replacement preserves routing identity so the matching listener set stays stable. */
    public UiSignal replace(Object nextPayload) {
        return new UiSignal(group, id, name, source, nextPayload, metadata);
    }

    public UiSignal replace(Object nextPayload, String nextSource, Map<String, Object> nextMetadata) {
        return new UiSignal(group, id, name, nextSource, nextPayload, nextMetadata);
    }
}

package artframework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable event delivered by the single ART signal bus. */
public final class UiSignal {
    public final String id;
    public final String name;
    public final String source;
    public final Object payload;
    public final Map<String, Object> metadata;

    public UiSignal(String name, String source, Object payload) {
        this(null, name, source, payload, null);
    }

    public UiSignal(
            String id, String name, String source, Object payload, Map<String, Object> metadata) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("signal name required");
        }
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
        return new UiSignal(id, name, source, nextPayload, metadata);
    }

    public UiSignal replace(Object nextPayload, String nextSource, Map<String, Object> nextMetadata) {
        return new UiSignal(id, name, nextSource, nextPayload, nextMetadata);
    }
}

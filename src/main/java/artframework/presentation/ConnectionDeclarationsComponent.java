package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import artframework.component.ImmutableUiValue;

/** Immutable declarative signal connection input for one presentation entity. */
public final class ConnectionDeclarationsComponent {
    public final List<Map<String, Object>> connections;
    public final List<Map<String, Object>> legacyTriggers;

    public ConnectionDeclarationsComponent(
            List<Map<String, Object>> connections, List<Map<String, Object>> legacyTriggers) {
        this.connections = copy(connections);
        this.legacyTriggers = copy(legacyTriggers);
    }

    private static List<Map<String, Object>> copy(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : source) {
            if (item != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> normalized = (Map<String, Object>) ImmutableUiValue.copy(item);
                result.add(normalized);
            }
        }
        return Collections.unmodifiableList(result);
    }
}

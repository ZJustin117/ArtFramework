package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Serializable declaration of signals an entity may emit. */
public final class SignalPortsComponent {
    public final List<String> emits;

    public SignalPortsComponent(List<String> emits) {
        this.emits = unique(emits);
    }

    public boolean canEmit(String signal) { return emits.contains(signal); }

    private static List<String> unique(List<String> ports) {
        if (ports == null || ports.isEmpty()) return Collections.emptyList();
        Set<String> seen = new LinkedHashSet<String>();
        for (String port : ports) {
            if (port == null || port.trim().isEmpty()) {
                throw new IllegalArgumentException("signal required");
            }
            seen.add(port.trim());
        }
        return Collections.unmodifiableList(new ArrayList<String>(seen));
    }
}

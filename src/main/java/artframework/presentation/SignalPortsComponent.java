package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Serializable declaration of signals an entity may emit. */
public final class SignalPortsComponent {
    public final List<String> emits;

    public SignalPortsComponent(List<String> emits) {
        this.emits = unique(emits);
    }

    public boolean canEmit(String signal) { return emits.contains(signal); }

    private static List<String> unique(List<String> ports) {
        return ports == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(new LinkedHashSet<String>(ports)));
    }
}

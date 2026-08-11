package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Serializable declaration of signals an entity may emit or accept. */
public final class SignalPortsComponent {
    public final List<String> emits;
    public final List<String> accepts;

    public SignalPortsComponent(List<String> emits, List<String> accepts) {
        this.emits = unique(emits);
        this.accepts = unique(accepts);
    }

    public boolean canEmit(String signal) { return emits.contains(signal); }

    private static List<String> unique(List<String> ports) {
        return ports == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(new LinkedHashSet<String>(ports)));
    }
}

package artframework.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable capabilities exposed by a presentation host. */
public final class HostCapabilities {

    public static final String SCENE_GRAPH = "scene_graph";
    public static final String INPUT = "input";
    public static final String SHADER_PROGRAM = "shader_program";
    public static final String OFFSCREEN_TARGET = "offscreen_target";
    public static final String FRAME_CAPTURE = "frame_capture";
    public static final String NATIVE_INTERCEPT = "native_intercept";
    public static final String ENTITY_ANCHOR = "entity_anchor";

    private final Set<String> values;

    public HostCapabilities(Set<String> values) {
        LinkedHashSet<String> copy = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    copy.add(value);
                }
            }
        }
        this.values = Collections.unmodifiableSet(copy);
    }

    public static HostCapabilities none() {
        return new HostCapabilities(Collections.<String>emptySet());
    }

    public static HostCapabilities of(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                set.add(value);
            }
        }
        return new HostCapabilities(set);
    }

    public boolean supports(String capability) {
        return values.contains(capability);
    }

    public Set<String> values() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}

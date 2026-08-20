package artframework.core;

import java.util.regex.Pattern;

/** Canonical names for signals routed through the native signal group. */
public final class SignalPaths {
    private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9._-]+");

    private SignalPaths() {}

    /** External/C2 component signal: {@code ui/<component>/<signal>}. */
    public static String component(String componentId, String signal) {
        return "ui/" + segment(componentId, "componentId") + "/" + signal(signal);
    }

    /** C1 node signal: {@code ui/<window>/<node-path>/<signal>}. */
    public static String node(String windowId, String nodePath, String signal) {
        return "ui/" + segment(windowId, "windowId") + "/" + nodePath(nodePath)
                + "/" + signal(signal);
    }

    /** Validates and canonicalizes a local signal declaration or scoped route segment. */
    public static String signal(String value) {
        return segment(value, "signal");
    }

    private static String nodePath(String value) {
        String path = required(value, "nodePath");
        String[] segments = path.split("/", -1);
        for (String segment : segments) {
            if (!SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("nodePath invalid");
            }
        }
        return path;
    }

    private static String segment(String value, String label) {
        String normalized = required(value, label);
        if (!SEGMENT.matcher(normalized).matches()) {
            throw new IllegalArgumentException(label + " invalid");
        }
        return normalized;
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " required");
        }
        return value.trim();
    }
}

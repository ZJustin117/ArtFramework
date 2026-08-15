package artframework.core;

/** Canonical names for signals routed through the native signal group. */
public final class SignalPaths {
    private SignalPaths() {}

    /** External/C2 component signal: {@code ui/<component>/<signal>}. */
    public static String component(String componentId, String signal) {
        return "ui/" + required(componentId, "componentId") + "/" + required(signal, "signal");
    }

    /** C1 node signal: {@code ui/<window>/<node-path>/<signal>}. */
    public static String node(String windowId, String nodePath, String signal) {
        return "ui/" + required(windowId, "windowId") + "/" + required(nodePath, "nodePath")
                + "/" + required(signal, "signal");
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " required");
        }
        return value;
    }
}

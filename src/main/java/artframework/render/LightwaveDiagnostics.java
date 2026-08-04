package artframework.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime-only switches for isolating Lightwave rendering on a device. */
public final class LightwaveDiagnostics {
    private static boolean c2EffectsEnabled = true;
    private static boolean c2ItemsEnabled = true;
    private static boolean c2PanelsEnabled = true;
    private static boolean forceFallback;

    private LightwaveDiagnostics() {}

    public static boolean c2EffectsEnabled() { return c2EffectsEnabled; }
    public static boolean c2ItemsEnabled() { return c2ItemsEnabled; }
    public static boolean c2PanelsEnabled() { return c2PanelsEnabled; }
    public static boolean forceFallback() { return forceFallback; }

    public static void setC2EffectsEnabled(boolean enabled) { c2EffectsEnabled = enabled; }
    public static void setC2ItemsEnabled(boolean enabled) { c2ItemsEnabled = enabled; }
    public static void setC2PanelsEnabled(boolean enabled) { c2PanelsEnabled = enabled; }
    public static void setForceFallback(boolean enabled) { forceFallback = enabled; }

    public static Map<String, Object> probeSummary() {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("c2EffectsEnabled", Boolean.valueOf(c2EffectsEnabled));
        out.put("c2ItemsEnabled", Boolean.valueOf(c2ItemsEnabled));
        out.put("c2PanelsEnabled", Boolean.valueOf(c2PanelsEnabled));
        out.put("forceFallback", Boolean.valueOf(forceFallback));
        return Collections.unmodifiableMap(out);
    }

    public static void resetForTests() {
        c2EffectsEnabled = true;
        c2ItemsEnabled = true;
        c2PanelsEnabled = true;
        forceFallback = false;
    }
}

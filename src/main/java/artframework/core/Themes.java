package artframework.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-global default theme for new presentation mounts, plus named theme registry.
 */
public final class Themes {

    private static Theme defaultTheme = StsTheme.createDefault();
    private static final Map<String, Theme> BY_NAME = new LinkedHashMap<String, Theme>();

    static {
        installNamed();
    }

    private Themes() {}

    private static void installNamed() {
        BY_NAME.clear();
        Theme sts = StsTheme.createDefault();
        sts.setName("sts");
        BY_NAME.put("sts", sts);
        Theme lw = LightwaveTheme.createDefault();
        BY_NAME.put("lightwave", lw);
        defaultTheme = sts;
    }

    public static Theme getDefault() {
        return defaultTheme;
    }

    /**
     * Project fallback theme (also used by {@link ProjectPresent#set}). Not an "active profile".
     */
    public static void setDefault(Theme theme) {
        defaultTheme = theme != null ? theme : StsTheme.createDefault();
    }

    public static void register(String name, Theme theme) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("theme name required");
        }
        if (theme == null) {
            throw new IllegalArgumentException("theme required");
        }
        if (theme.name() == null || theme.name().isEmpty()) {
            theme.setName(name);
        }
        BY_NAME.put(name, theme);
    }

    public static Theme get(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return BY_NAME.get(name);
    }

    public static List<String> names() {
        return Collections.unmodifiableList(new ArrayList<String>(BY_NAME.keySet()));
    }

    public static void resetForTests() {
        installNamed();
    }
}

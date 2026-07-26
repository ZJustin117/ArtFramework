package spireui.core;

/**
 * Process-global default theme for new {@link UiTree} mounts.
 */
public final class Themes {

    private static Theme defaultTheme = StsTheme.createDefault();

    private Themes() {}

    public static Theme getDefault() {
        return defaultTheme;
    }

    public static void setDefault(Theme theme) {
        defaultTheme = theme != null ? theme : StsTheme.createDefault();
    }

    public static void resetForTests() {
        defaultTheme = StsTheme.createDefault();
    }
}

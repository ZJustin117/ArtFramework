package spireui.component;

/**
 * Cross-axis alignment inside a box container (Godot shrink begin/center/end).
 */
public final class Align {

    public static final String BEGIN = "begin";
    public static final String CENTER = "center";
    public static final String END = "end";

    private Align() {}

    public static String normalize(String align) {
        if (align == null || align.isEmpty()) {
            return BEGIN;
        }
        String a = align.trim().toLowerCase();
        if (CENTER.equals(a) || "middle".equals(a)) {
            return CENTER;
        }
        if (END.equals(a) || "right".equals(a) || "bottom".equals(a)) {
            return END;
        }
        return BEGIN;
    }
}

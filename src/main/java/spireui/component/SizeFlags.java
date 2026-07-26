package spireui.component;

/**
 * Godot-like container sizing flags (bitmask). Main axis uses expand + stretch ratio.
 */
public final class SizeFlags {

    public static final int FILL = 1;
    public static final int EXPAND = 2;
    public static final int SHRINK_CENTER = 4;
    public static final int SHRINK_END = 8;

    /** Default: fill designated area, do not expand. */
    public static final int DEFAULT = FILL;

    private SizeFlags() {}

    public static boolean has(int flags, int bit) {
        return (flags & bit) != 0;
    }

    public static boolean expands(int flags) {
        return has(flags, EXPAND);
    }

    /** Map legacy {@code grow:true} to expand+fill. */
    public static int fromGrow(boolean grow) {
        return grow ? (FILL | EXPAND) : DEFAULT;
    }
}

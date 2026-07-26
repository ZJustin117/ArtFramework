package artframework.component;

/**
 * Built-in composition node type names.
 */
public final class UiTypes {

    public static final String WINDOW = "window";
    public static final String ROW = "row";
    public static final String COL = "col";
    public static final String STACK = "stack";
    public static final String PANEL = "panel";
    /** Frosted / liquid glass plate (container; default effect id glass). */
    public static final String GLASS = "glass";
    public static final String FRAGMENT = "fragment";
    public static final String LABEL = "label";
    public static final String BUTTON = "button";
    public static final String SLIDER = "slider";
    public static final String HITAREA = "hitarea";
    public static final String TEXTFIELD = "textfield";
    public static final String CHECKBOX = "checkbox";
    public static final String PROGRESS = "progress";
    public static final String SCROLL = "scroll";
    public static final String CENTER = "center";
    public static final String MARGIN = "margin";
    public static final String REF = "ref";
    public static final String SLOT = "slot";

    private UiTypes() {}

    public static boolean isContainer(String type) {
        return WINDOW.equals(type)
                || ROW.equals(type)
                || COL.equals(type)
                || STACK.equals(type)
                || PANEL.equals(type)
                || GLASS.equals(type)
                || FRAGMENT.equals(type)
                || SCROLL.equals(type)
                || CENTER.equals(type)
                || MARGIN.equals(type);
    }

    public static boolean isLeaf(String type) {
        return LABEL.equals(type)
                || BUTTON.equals(type)
                || SLIDER.equals(type)
                || HITAREA.equals(type)
                || TEXTFIELD.equals(type)
                || CHECKBOX.equals(type)
                || PROGRESS.equals(type);
    }

    public static boolean isExpandable(String type) {
        return REF.equals(type) || SLOT.equals(type);
    }
}

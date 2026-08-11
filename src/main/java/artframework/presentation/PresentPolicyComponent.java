package artframework.presentation;

/** C2 ownership policy; C1 entities simply omit this component. */
public final class PresentPolicyComponent {
    public final String level;
    public final boolean suppressNative;
    public final boolean ownsInput;

    public PresentPolicyComponent(String level, boolean suppressNative, boolean ownsInput) {
        this.level = level != null ? level : "OBSERVE";
        this.suppressNative = suppressNative;
        this.ownsInput = ownsInput;
    }
}

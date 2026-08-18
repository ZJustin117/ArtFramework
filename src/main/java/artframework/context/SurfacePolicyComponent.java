package artframework.context;

/** Host-neutral snapshot of a surface's current presentation policy. */
public final class SurfacePolicyComponent {
    public final String level;
    public final boolean fullPresent;
    public final boolean observe;
    public final boolean maySuppressNative;

    public SurfacePolicyComponent(String level, boolean fullPresent, boolean observe,
                                   boolean maySuppressNative) {
        this.level = level != null ? level : "OFF";
        this.fullPresent = fullPresent;
        this.observe = observe;
        this.maySuppressNative = maySuppressNative;
    }
}

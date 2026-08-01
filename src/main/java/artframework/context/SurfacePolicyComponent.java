package artframework.context;

import artframework.sts1.PresentLevel;

/** Host-neutral snapshot of a surface's current presentation policy. */
public final class SurfacePolicyComponent {
    public final PresentLevel level;
    public final boolean fullPresent;
    public final boolean observe;
    public final boolean maySuppressNative;

    public SurfacePolicyComponent(PresentLevel level, boolean fullPresent, boolean observe,
                                  boolean maySuppressNative) {
        this.level = level;
        this.fullPresent = fullPresent;
        this.observe = observe;
        this.maySuppressNative = maySuppressNative;
    }
}

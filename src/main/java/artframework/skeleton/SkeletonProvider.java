package artframework.skeleton;

/**
 * Host/format-specific skeleton runtime. Core never depends on Spine/etc.
 */
public interface SkeletonProvider {

    String id();

    SkeletonHandle load(SkeletonSource source);

    void unload(SkeletonHandle handle);
}

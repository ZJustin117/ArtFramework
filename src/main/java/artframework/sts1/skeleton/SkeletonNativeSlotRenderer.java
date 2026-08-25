package artframework.sts1.skeleton;

import artframework.skeleton.SkeletonHandle;

/**
 * Capability for a skeleton provider to draw into an already-active native host batch slot.
 * Providers that cannot draw at the native slot remain strictly non-native and must not suppress
 * the original STS Spine renderer.
 */
public interface SkeletonNativeSlotRenderer {

    /** Draw the provider-owned skeleton into the active native batch. Return true if pixels were produced. */
    boolean renderAtNativeSlot(SkeletonHandle handle, Object batch);
}

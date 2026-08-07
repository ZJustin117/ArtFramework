package artframework.core;

import artframework.skeleton.SkeletonNodeBinding;

/** ECS component attached to an art.skeleton UI node. */
public final class SkeletonNodeBindingComponent {
    public final SkeletonNodeBinding binding;

    public SkeletonNodeBindingComponent(SkeletonNodeBinding binding) {
        if (binding == null) throw new IllegalArgumentException("binding required");
        this.binding = binding;
    }
}

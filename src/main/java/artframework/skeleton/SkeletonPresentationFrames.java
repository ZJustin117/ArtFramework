package artframework.skeleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordinary signal transport for backend-owned skeleton presentation snapshots. */
public final class SkeletonPresentationFrames {
    public static final String UPDATED = "presentation/skeletons/updated";

    private SkeletonPresentationFrames() {}

    public static void publish(long frameId, List<SkeletonPresentationView> views) {
        List<SkeletonPresentationView> copy = views == null
                ? Collections.<SkeletonPresentationView>emptyList()
                : Collections.unmodifiableList(new ArrayList<SkeletonPresentationView>(views));
        // Frame state is consumed from ECS; this compatibility notification is native-group scoped.
    }

    public static final class Frame {
        public final long frameId;
        public final List<SkeletonPresentationView> views;

        private Frame(long frameId, List<SkeletonPresentationView> views) {
            this.frameId = frameId;
            this.views = views;
        }
    }
}

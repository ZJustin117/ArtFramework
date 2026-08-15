package artframework.skeleton;

import artframework.core.SignalGroups;
import artframework.core.UiSignal;
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
        SignalGroups.nativeGroup().emit(new UiSignal(UPDATED, "skeleton-presentation",
                new Frame(frameId, copy)));
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

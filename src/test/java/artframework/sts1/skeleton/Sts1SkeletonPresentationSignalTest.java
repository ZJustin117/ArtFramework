package artframework.sts1.skeleton;

import artframework.api.ArtFramework;
import artframework.core.SignalBuses;
import artframework.skeleton.FakeSkeletonProvider;
import artframework.skeleton.SkeletonAnimationComponent;
import artframework.skeleton.SkeletonAssetComponent;
import artframework.skeleton.SkeletonPoseComponent;
import artframework.skeleton.SkeletonPresentationFrames;
import artframework.skeleton.SkeletonPresentationView;
import artframework.skeleton.SkeletonVisualComponent;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class Sts1SkeletonPresentationSignalTest {
    @After public void reset() { ArtFramework.resetForTests(); }

    @Test public void backendFrameSignalCreatesProviderRuntime() {
        FakeSkeletonProvider provider = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(provider);
        Sts1SkeletonBridge.installPresentationSignals();
        SkeletonPresentationFrames.publish(1L, Arrays.asList(new SkeletonPresentationView(
                "hero", new SkeletonAssetComponent("fake", "a", "s", "", 1f),
                new SkeletonPoseComponent(1f, 2f, 0f, 1f, 1f, false, false, 0),
                new SkeletonAnimationComponent(0, "idle", true), new SkeletonVisualComponent(true))));
        assertEquals(1, Sts1SkeletonBridge.presentationSystem().size());
        assertEquals(1, provider.liveCount());
    }
}

package artframework.skeleton;

import artframework.ecs.PresentationWorld;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class SkeletonPresentationSystemTest {
    @Test public void reusesHandleAndAppliesPoseAndAnimation() {
        SkeletonProviders providers = new SkeletonProviders();
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        providers.register(fake);
        SkeletonPresentationSystem system = new SkeletonPresentationSystem(
                new PresentationWorld("test"), providers);
        SkeletonPresentationView first = view("hero", 10f, "idle_loop");
        system.sync(1L, Arrays.asList(first));
        SkeletonHandle handle = system.binding("hero").handle;
        system.sync(2L, Arrays.asList(view("hero", 20f, "attack")));
        assertSame(handle, system.binding("hero").handle);
        assertEquals(1, fake.loadCount());
        assertEquals(20f, fake.x("hero"), 0.001f);
        assertEquals("attack", fake.currentAnimation(handle, 0));
    }

    @Test public void removesMissingEntitiesAndRejectsStaleFrames() {
        SkeletonProviders providers = new SkeletonProviders();
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        providers.register(fake);
        SkeletonPresentationSystem system = new SkeletonPresentationSystem(
                new PresentationWorld("test"), providers);
        system.sync(4L, Arrays.asList(view("hero", 1f, "idle_loop")));
        system.sync(3L, Arrays.asList(view("other", 1f, "idle_loop")));
        assertNotNull(system.binding("hero"));
        system.sync(5L, Arrays.<SkeletonPresentationView>asList());
        assertEquals(0, system.size());
        assertEquals(0, fake.liveCount());
    }

    @Test public void tickAdvancesAndAppliesEveryLiveHandle() {
        SkeletonProviders providers = new SkeletonProviders();
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        providers.register(fake);
        SkeletonPresentationSystem system = new SkeletonPresentationSystem(
                new PresentationWorld("test"), providers);
        system.sync(1L, Arrays.asList(view("hero", 1f, "idle_loop")));
        system.tick(0.25f);
        assertTrue(fake.applied("hero"));
    }

    private static SkeletonPresentationView view(String id, float x, String animation) {
        return new SkeletonPresentationView(id,
                new SkeletonAssetComponent("fake", "hero.atlas", "hero.json", "", 1f),
                new SkeletonPoseComponent(x, 2f, 0f, 1f, 1f, false, false, 0),
                new SkeletonAnimationComponent(0, animation, true),
                new SkeletonVisualComponent(true));
    }
}

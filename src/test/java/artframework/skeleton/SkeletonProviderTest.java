package artframework.skeleton;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNodeRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SkeletonProviderTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void registerAndLoadFake() {
        FakeSkeletonProvider fake = new FakeSkeletonProvider();
        ArtFramework.skeletons().register(fake);
        assertTrue(ArtFramework.skeletons().contains(FakeSkeletonProvider.ID));
        SkeletonHandle h =
                ArtFramework.skeletons()
                        .load(
                                FakeSkeletonProvider.ID,
                                new SkeletonSource("hero", "a.atlas", "a.json", null));
        assertNotNull(h);
        assertTrue(h.isAlive());
        assertEquals(1, fake.loadCount());
        fake.unload(h);
        assertFalse(h.isAlive());
        assertEquals(0, fake.liveCount());
    }

    @Test
    public void unknownProviderFails() {
        try {
            ArtFramework.skeletons()
                    .load("missing", new SkeletonSource("x", "", "", null));
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("unknown"));
        }
    }

    @Test
    public void skeletonNodeTypeRegistered() {
        assertTrue(UiNodeRegistry.global().contains(ArtNodeTypes.SKELETON));
        assertTrue(
                UiNodeRegistry.global()
                        .get(ArtNodeTypes.SKELETON)
                        .defaultSignals()
                        .contains(artframework.core.SignalNames.SKELETON_EVENT));
    }

    @Test
    public void skeletonBindingIsNullSafe() {
        SkeletonBinding invalid = new SkeletonBinding(null, null);
        invalid.setAnimation("idle", true);
        invalid.addAnimation("idle", true, 0f);
        invalid.setMix("a", "b", 0.1f);
        assertFalse(invalid.isValid());
        assertEquals(null, invalid.currentAnimation());
        assertEquals(null, invalid.boneTransform("root"));
    }
}

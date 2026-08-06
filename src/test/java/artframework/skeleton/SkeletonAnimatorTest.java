package artframework.skeleton;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SkeletonAnimatorTest {

    @Test
    public void triggerOneShotQueuesInitialIdle() {
        FakeSkeletonProvider provider = new FakeSkeletonProvider();
        SkeletonHandle handle = provider.load(sourceWithAnimations("hero", "idle_loop", "attack", "hurt"));
        AnimGraph graph =
                AnimGraph.builder("idle_loop")
                        .state("idle_loop", true)
                        .state("attack", false)
                        .state("hurt", false)
                        .trigger(SkeletonAnimator.TRIGGER_ATTACK, "attack")
                        .build();
        SkeletonMixTable mixTable = SkeletonMixTable.builder(0.05f).mix("idle_loop", "attack", 0.1f).build();

        SkeletonAnimator animator = new SkeletonAnimator(provider, handle, graph, mixTable, new Random(7));
        animator.start();
        assertEquals("idle_loop", provider.currentAnimation(handle, 0));
        assertTrue(provider.trackTime("hero") >= 0f);
        assertTrue(provider.applied("hero"));

        assertTrue(animator.trigger(SkeletonAnimator.TRIGGER_ATTACK));

        assertEquals("attack", animator.currentState().id);
        assertEquals(Float.valueOf(0.1f), provider.mix("hero", "idle_loop", "attack"));
        assertTrue(provider.events("hero").contains("set:attack:false"));
        assertTrue(provider.events("hero").contains("add:idle_loop:true:0.0"));
    }

    @Test
    public void explicitNextStateChains() {
        FakeSkeletonProvider provider = new FakeSkeletonProvider();
        SkeletonHandle handle = provider.load(sourceWithAnimations("hero", "idle_loop", "cast", "relaxed_loop"));
        AnimGraph graph =
                AnimGraph.builder("idle_loop")
                        .state("idle_loop", true)
                        .state("cast", false, "relaxed_loop")
                        .state("relaxed_loop", true)
                        .trigger(SkeletonAnimator.TRIGGER_CAST, "cast")
                        .build();

        SkeletonAnimator animator = new SkeletonAnimator(provider, handle, graph, null, new Random(1));
        animator.start();
        animator.trigger(SkeletonAnimator.TRIGGER_CAST);

        assertTrue(provider.events("hero").contains("add:relaxed_loop:true:0.0"));
    }

    @Test
    public void missingAnimationWarnsWithoutCrashing() {
        FakeSkeletonProvider provider = new FakeSkeletonProvider();
        SkeletonHandle handle = provider.load(sourceWithAnimations("hero", "idle_loop"));
        AnimGraph graph =
                AnimGraph.builder("idle_loop")
                        .state("idle_loop", true)
                        .state("attack", false)
                        .trigger(SkeletonAnimator.TRIGGER_ATTACK, "attack")
                        .build();

        SkeletonAnimator animator = new SkeletonAnimator(provider, handle, graph, null, new Random(1));
        animator.start();
        animator.trigger(SkeletonAnimator.TRIGGER_ATTACK);

        assertNotNull(animator.lastWarning());
        assertTrue(animator.lastWarning().contains("attack"));
        assertEquals("idle_loop", provider.currentAnimation(handle, 0));
    }

    @Test
    public void unknownTriggerReturnsFalse() {
        FakeSkeletonProvider provider = new FakeSkeletonProvider();
        SkeletonHandle handle = provider.load(sourceWithAnimations("hero", "idle_loop"));
        AnimGraph graph = AnimGraph.builder("idle_loop").state("idle_loop", true).build();

        SkeletonAnimator animator = new SkeletonAnimator(provider, handle, graph, null, new Random(1));

        assertTrue(!animator.trigger("Missing"));
        assertNull(animator.currentState());
    }

    private static SkeletonSource sourceWithAnimations(String id, String... animations) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("animations", Arrays.asList(animations));
        params.put("animationEnd", Float.valueOf(3f));
        return new SkeletonSource(id, "", "", params);
    }
}

package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnimationPlayerTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void playTweensTargetPropAndEmitsSignals() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(UiNode.of(UiTypes.PANEL).id("dialog").prop("opacity", Float.valueOf(0f)).build())
                        .child(animPlayerNode())
                        .build();
        UiTree tree = UiTree.mount("win", root);
        AtomicInteger started = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        tree.get("motion")
                .connect(
                        AnimationPlayer.SIGNAL_STARTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                started.incrementAndGet();
                            }
                        });
        tree.get("motion")
                .connect(
                        AnimationPlayer.SIGNAL_FINISHED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                finished.incrementAndGet();
                            }
                        });
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        assertNotNull(player);
        player.play("enter");
        assertEquals(1, started.get());
        assertEquals(0f, ((Number) tree.get("dialog").prop("opacity")).floatValue(), 0.001f);
        tree.tick(0.1f);
        assertEquals(0.5f, ((Number) tree.get("dialog").prop("opacity")).floatValue(), 0.001f);
        tree.tick(0.1f);
        assertEquals(1f, ((Number) tree.get("dialog").prop("opacity")).floatValue(), 0.001f);
        assertEquals(1, finished.get());
        assertFalse(player.isPlaying());
    }

    @Test
    public void artFacadeAnimationAccessor() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(UiNode.of(UiTypes.LABEL).id("l").build())
                        .child(animPlayerNode())
                        .build();
        UiTrees.open("win", root);
        assertNotNull(ArtFramework.animation("win", "motion"));
        UiTrees.close("win");
        assertTrue(ArtFramework.animation("win", "motion") == null);
    }

    private static UiNode animPlayerNode() {
        Map<String, Object> track = new LinkedHashMap<String, Object>();
        track.put("property", "opacity");
        track.put("from", Float.valueOf(0f));
        track.put("to", Float.valueOf(1f));
        Map<String, Object> anim = new LinkedHashMap<String, Object>();
        anim.put("name", "enter");
        anim.put("target", "dialog");
        anim.put("duration", Float.valueOf(0.2f));
        anim.put("tracks", Collections.singletonList(track));
        List<Object> animations = new ArrayList<Object>();
        animations.add(anim);
        return UiNode.of(ArtNodeTypes.ANIMATION_PLAYER)
                .id("motion")
                .prop("animations", animations)
                .signals(
                        Arrays.asList(
                                AnimationPlayer.SIGNAL_STARTED,
                                AnimationPlayer.SIGNAL_FINISHED,
                                AnimationPlayer.SIGNAL_CANCELLED))
                .build();
    }
}

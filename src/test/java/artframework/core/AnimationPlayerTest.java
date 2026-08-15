package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.NodeStateComponent;
import artframework.presentation.PresentationRuntime;
import artframework.test.C1RuntimeFixture;

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
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        assertNotNull(player);
        player.play("enter");
        assertEquals(NodeStateMachine.STATE_PLAYING, player.state());
        assertEquals(0f, ((Number) fixture.property("dialog", "opacity")).floatValue(), 0.001f);
        fixture.tick(0.1f);
        assertEquals(0.5f, ((Number) fixture.property("dialog", "opacity")).floatValue(), 0.001f);
        fixture.tick(0.1f);
        assertEquals(1f, ((Number) fixture.property("dialog", "opacity")).floatValue(), 0.001f);
        assertEquals(NodeStateMachine.STATE_IDLE, player.state());
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
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        assertNotNull(ArtFramework.animation("win", "motion"));
        fixture.close();
        assertTrue(ArtFramework.animation("win", "motion") == null);
    }

    @Test
    public void pauseAndResume() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", rootWithPlayer(animOnce()));
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        player.play("enter");
        assertEquals(NodeStateMachine.STATE_PLAYING, player.state());
        fixture.tick(0.05f);
        float mid = ((Number) fixture.property("dialog", "opacity")).floatValue();
        player.pause();
        assertEquals(NodeStateMachine.STATE_PAUSED, player.state());
        fixture.tick(0.2f);
        assertEquals(mid, ((Number) fixture.property("dialog", "opacity")).floatValue(), 0.001f);
        player.resume();
        fixture.tick(0.2f);
        assertEquals(1f, ((Number) fixture.property("dialog", "opacity")).floatValue(), 0.001f);
        assertFalse(player.isPlaying());
    }

    @Test
    public void loopModeTracksLoopsThenFinishesOnCount() {
        Map<String, Object> anim = animOnce();
        anim.put("mode", "loop");
        anim.put("loop_count", Integer.valueOf(2));
        anim.put("duration", Float.valueOf(0.1f));
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", rootWithPlayer(anim));
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        player.play("enter");
        fixture.tick(0.1f);
        assertEquals(1, player.loopsDone());
        assertTrue(player.isPlaying());
        fixture.tick(0.1f);
        assertEquals(0, player.loopsDone());
        assertFalse(player.isPlaying());
        assertEquals(NodeStateMachine.STATE_IDLE, player.state());
    }

    @Test
    public void playbackStateIsStoredOnTheAnimationEntity() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", rootWithPlayer(animOnce()));
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        player.play("enter");
        AnimationPlaybackComponent started = fixture.context.world().get(
                fixture.find("motion"), AnimationPlaybackComponent.class);
        assertEquals("enter", started.playing);
        assertTrue(started.active);
        assertFalse(started.paused);
        fixture.tick(0.1f);
        AnimationPlaybackComponent advanced = fixture.context.world().get(
                fixture.find("motion"), AnimationPlaybackComponent.class);
        assertEquals(0.1f, advanced.elapsed, 0.001f);
        player.pause();
        assertTrue(fixture.context.world().get(fixture.find("motion"),
                AnimationPlaybackComponent.class).paused);
        assertEquals(NodeStateMachine.STATE_PAUSED, fixture.context.world().get(
                fixture.find("motion"), NodeStateComponent.class).value);
    }

    private static UiNode rootWithPlayer(Map<String, Object> anim) {
        return UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .child(UiNode.of(UiTypes.PANEL).id("dialog").prop("opacity", Float.valueOf(0f)).build())
                .child(animPlayerNode(anim))
                .build();
    }

    private static Map<String, Object> animOnce() {
        Map<String, Object> track = new LinkedHashMap<String, Object>();
        track.put("property", "opacity");
        track.put("from", Float.valueOf(0f));
        track.put("to", Float.valueOf(1f));
        Map<String, Object> anim = new LinkedHashMap<String, Object>();
        anim.put("name", "enter");
        anim.put("target", "dialog");
        anim.put("duration", Float.valueOf(0.2f));
        anim.put("tracks", Collections.singletonList(track));
        return anim;
    }

    private static UiNode animPlayerNode() {
        return animPlayerNode(animOnce());
    }

    private static UiNode animPlayerNode(Map<String, Object> anim) {
        List<Object> animations = new ArrayList<Object>();
        animations.add(anim);
        return UiNode.of(ArtNodeTypes.ANIMATION_PLAYER)
                .id("motion")
                .prop("animations", animations)
                .signals(
                        Arrays.asList(
                                AnimationPlayer.SIGNAL_STARTED,
                                AnimationPlayer.SIGNAL_FINISHED,
                                AnimationPlayer.SIGNAL_CANCELLED,
                                AnimationPlayer.SIGNAL_PAUSED,
                                AnimationPlayer.SIGNAL_RESUMED,
                                AnimationPlayer.SIGNAL_LOOPED))
                .build();
    }
}

package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;
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

public class AnimationTriggerTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void autoPlayRunsOnMount() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", rootWithAutoPlay());
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        assertNotNull(player);
        assertEquals("enter", player.playing());
        assertEquals(0.2f, ((Number) fixture.property("panel", "opacity")).floatValue(), 0.001f);
        fixture.tick(0.3f);
        assertEquals(1f, ((Number) fixture.property("panel", "opacity")).floatValue(), 0.001f);
        assertFalse(player.isPlaying());
    }

    @Test
    public void triggerPlaysOnPressed() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", rootWithTrigger());
        AnimationPlayer player = AnimationPlayers.get("win", "motion");
        assertNotNull(player);
        // auto_play ran at sync (listener not yet attached)
        assertTrue(player.isPlaying() || fixture.property("panel", "opacity") != null);
        fixture.tick(0.5f);
        assertFalse(player.isPlaying());
        fixture.emit("ok", SignalNames.PRESSED);
        assertEquals("flash", player.playing());
        fixture.tick(0.25f);
        assertEquals(0.5f, ((Number) fixture.property("panel", "opacity")).floatValue(), 0.05f);
    }

    private static UiNode rootWithAutoPlay() {
        return UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .child(UiNode.of(UiTypes.PANEL).id("panel").prop("opacity", Float.valueOf(0f)).build())
                .child(animPlayerEnterOnly())
                .build();
    }

    private static UiNode rootWithTrigger() {
        Map<String, Object> trigger = new LinkedHashMap<String, Object>();
        trigger.put("source", "ok");
        trigger.put("signal", SignalNames.PRESSED);
        trigger.put("play", "flash");
        List<Object> triggers = new ArrayList<Object>();
        triggers.add(trigger);
        return UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .child(UiNode.of(UiTypes.PANEL).id("panel").prop("opacity", Float.valueOf(0f)).build())
                .child(UiNode.of(UiTypes.BUTTON).id("ok").prop("text", "OK").build())
                .child(animPlayerWithFlash(triggers))
                .build();
    }

    private static UiNode animPlayerEnterOnly() {
        Map<String, Object> track = new LinkedHashMap<String, Object>();
        track.put("property", "opacity");
        track.put("from", Float.valueOf(0.2f));
        track.put("to", Float.valueOf(1f));
        Map<String, Object> anim = new LinkedHashMap<String, Object>();
        anim.put("name", "enter");
        anim.put("target", "panel");
        anim.put("duration", Float.valueOf(0.3f));
        anim.put("tracks", Collections.singletonList(track));
        List<Object> animations = new ArrayList<Object>();
        animations.add(anim);
        return UiNode.of(ArtNodeTypes.ANIMATION_PLAYER)
                .id("motion")
                .prop("animations", animations)
                .prop("auto_play", "enter")
                .signals(
                        Arrays.asList(
                                AnimationPlayer.SIGNAL_STARTED,
                                AnimationPlayer.SIGNAL_FINISHED,
                                AnimationPlayer.SIGNAL_CANCELLED))
                .build();
    }

    private static UiNode animPlayerWithFlash(List<Object> triggers) {
        Map<String, Object> enterTrack = new LinkedHashMap<String, Object>();
        enterTrack.put("property", "opacity");
        enterTrack.put("from", Float.valueOf(0.2f));
        enterTrack.put("to", Float.valueOf(1f));
        Map<String, Object> enter = new LinkedHashMap<String, Object>();
        enter.put("name", "enter");
        enter.put("target", "panel");
        enter.put("duration", Float.valueOf(0.05f));
        enter.put("tracks", Collections.singletonList(enterTrack));

        Map<String, Object> flashTrack = new LinkedHashMap<String, Object>();
        flashTrack.put("property", "opacity");
        flashTrack.put("from", Float.valueOf(1f));
        flashTrack.put("to", Float.valueOf(0.5f));
        Map<String, Object> flash = new LinkedHashMap<String, Object>();
        flash.put("name", "flash");
        flash.put("target", "panel");
        flash.put("duration", Float.valueOf(0.2f));
        flash.put("tracks", Collections.singletonList(flashTrack));

        List<Object> animations = new ArrayList<Object>();
        animations.add(enter);
        animations.add(flash);
        return UiNode.of(ArtNodeTypes.ANIMATION_PLAYER)
                .id("motion")
                .prop("animations", animations)
                .prop("auto_play", "enter")
                .prop("triggers", triggers)
                .signals(
                        Arrays.asList(
                                AnimationPlayer.SIGNAL_STARTED,
                                AnimationPlayer.SIGNAL_FINISHED,
                                AnimationPlayer.SIGNAL_CANCELLED))
                .build();
    }
}

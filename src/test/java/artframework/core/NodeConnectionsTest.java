package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.presentation.EffectPulseComponent;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHosts;
import artframework.presentation.NodeTree;
import artframework.presentation.ConnectionDeclarationsComponent;

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
import static org.junit.Assert.fail;

/** Milestone 39–40: connections exact/regex + UiActions. */
public class NodeConnectionsTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void exactMatchPlaysAnimation() {
        NodeTree tree = mountWithPlayConnection("ui/ok/pressed", false);
        ConnectionDeclarationsComponent declarations = tree.world().get(
                tree.root().entityId(), ConnectionDeclarationsComponent.class);
        assertNotNull(declarations);
        assertEquals(1, declarations.connections.size());
        AtomicInteger started = new AtomicInteger();
        tree.get("motion")
                .connect(
                        AnimationPlayer.SIGNAL_STARTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                started.incrementAndGet();
                            }
                        });
        assertTrue(NodeConnections.subscriptionCount("win") >= 1);
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, started.get());
        assertTrue(AnimationPlayers.get("win", "motion").isPlaying());
    }

    @Test
    public void regexMatchPlaysAnimation() {
        NodeTree tree = mountWithPlayConnection("ui/.+/pressed", true);
        AtomicInteger started = new AtomicInteger();
        tree.get("motion")
                .connect(
                        AnimationPlayer.SIGNAL_STARTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                started.incrementAndGet();
                            }
                        });
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, started.get());
    }

    @Test
    public void legacyTriggersNormalizeToPlay() {
        Map<String, Object> trigger = new LinkedHashMap<String, Object>();
        trigger.put("source", "ok");
        trigger.put("signal", "pressed");
        trigger.put("play", "enter");
        UiNode root =
                windowWithButtonAndPlayer(
                        Collections.singletonList(trigger), null);
        NodeTree tree = NodeTree.mount("tree:win", root, null);
        AtomicInteger started = new AtomicInteger();
        tree.get("motion")
                .connect(
                        AnimationPlayer.SIGNAL_STARTED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                started.incrementAndGet();
                            }
                        });
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, started.get());
    }

    @Test
    public void setPropFromPayload() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/wave/value_changed");
        conn.put("action", UiActions.SET_PROP);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("target", "panel");
        args.put("prop", "fx_intensity");
        args.put("from_payload", Integer.valueOf(0));
        conn.put("args", args);
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .prop("connections", Collections.singletonList(conn))
                        .child(UiNode.of(UiTypes.PANEL).id("panel").prop("fx_intensity", 0.1f).build())
                        .child(
                                UiNode.of(UiTypes.SLIDER)
                                        .id("wave")
                                        .prop("min", 0f)
                                        .prop("max", 1f)
                                        .prop("value", 0.1f)
                                        .build())
                        .build();
        NodeTree tree = artframework.presentation.NodeTrees.open("win", root, null);
        tree.emit("wave", SignalNames.VALUE_CHANGED, Float.valueOf(0.8f));
        assertEquals(0.8f, ((Number) tree.get("panel").prop("fx_intensity")).floatValue(), 0.001f);
    }

    @Test
    public void pulseEffectUpdatesBinding() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/ok/pressed");
        conn.put("action", UiActions.PULSE_EFFECT);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("target", "panel");
        args.put("effect", LightwaveEffect.ID);
        args.put("duration", Float.valueOf(0.2f));
        conn.put("args", args);
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .prop("connections", Collections.singletonList(conn))
                        .child(
                                UiNode.of(UiTypes.PANEL)
                                        .id("panel")
                                        .prop("fx_intensity", 0.55f)
                                        .effect(new artframework.component.EffectDecl(
                                                LightwaveEffect.ID,
                                                Collections.<String, Object>singletonMap(
                                                        "intensity", Float.valueOf(0.55f))))
                                        .build())
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").prop("text", "P").build())
                        .build();
        NodeTree tree = artframework.presentation.NodeTrees.open("win", root, null);
        tree.emit("ok", SignalNames.PRESSED);
        assertTrue(EffectPulse.isActive("win", "panel"));
        assertEquals(1, artframework.ecs.ArtEcs.world().query(EffectPulseComponent.class).size());
        artframework.render.LightwaveControls.tickPulses(0.1f);
        artframework.presentation.EffectsComponent effects = tree.world().get(
                tree.get("panel").entityId(), artframework.presentation.EffectsComponent.class);
        float intensity = effects.get(LightwaveEffect.ID, "ambient").floatParam("intensity", 0f);
        assertTrue(intensity > 0.55f);
        artframework.render.LightwaveControls.tickPulses(0.2f);
        assertFalse(EffectPulse.isActive("win", "panel"));
        artframework.presentation.NodeTrees.close("win");
    }

    @Test
    public void unknownActionFailsMount() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/ok/pressed");
        conn.put("action", "not.a.real.action");
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("connections", Collections.singletonList(conn))
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        try {
            NodeTree.mount("tree:win", root, null);
            fail("expected unknown action");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("unknown ui action"));
        }
    }

    @Test
    public void thirdPartyActionRegister() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.registerUiAction(
                "mod.test_hit",
                new UiAction() {
                    @Override
                    public boolean run(UiActionContext ctx) {
                        hits.incrementAndGet();
                        return true;
                    }
                });
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match_pattern", "ui/ok/.*");
        conn.put("action", "mod.test_hit");
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("connections", Collections.singletonList(conn))
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        NodeTree tree = NodeTree.mount("tree:win", root, null);
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, hits.get());
        assertTrue(ArtFramework.uiActionIds().contains("mod.test_hit"));
    }

    @Test
    public void unmountClearsSubscriptions() {
        NodeTree tree = mountWithPlayConnection("ui/ok/pressed", false);
        assertTrue(NodeConnections.subscriptionCount("win") >= 1);
        tree.unmount();
        assertEquals(0, NodeConnections.subscriptionCount("win"));
    }

    @Test
    public void builtinsRegistered() {
        UiActions.ensureBuiltins();
        assertTrue(UiActions.contains(UiActions.PLAY));
        assertTrue(UiActions.contains(UiActions.PULSE_EFFECT));
        assertTrue(UiActions.contains(UiActions.SET_PROP));
        assertTrue(UiActions.contains(UiActions.EMIT));
        assertTrue(UiActions.contains(UiActions.CLOSE_WINDOW));
        assertNotNull(ArtFramework.getUiAction(UiActions.PAUSE));
    }

    private static NodeTree mountWithPlayConnection(String match, boolean pattern) {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        if (pattern) {
            conn.put("match_pattern", match);
        } else {
            conn.put("match", match);
        }
        conn.put("action", UiActions.PLAY);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("player", "motion");
        args.put("name", "enter");
        conn.put("args", args);
        return NodeTree.mount("tree:win", windowWithButtonAndPlayer(null, Collections.singletonList(conn)), null);
    }

    private static UiNode windowWithButtonAndPlayer(
            List<Map<String, Object>> triggers, List<Map<String, Object>> connections) {
        Map<String, Object> track = new LinkedHashMap<String, Object>();
        track.put("property", "opacity");
        track.put("from", Float.valueOf(0f));
        track.put("to", Float.valueOf(1f));
        Map<String, Object> anim = new LinkedHashMap<String, Object>();
        anim.put("name", "enter");
        anim.put("target", "dialog");
        anim.put("duration", Float.valueOf(0.2f));
        anim.put("tracks", Collections.singletonList(track));
        UiNode.Builder player =
                UiNode.of(ArtNodeTypes.ANIMATION_PLAYER)
                        .id("motion")
                        .prop("animations", Collections.singletonList(anim))
                        .signals(
                                Arrays.asList(
                                        AnimationPlayer.SIGNAL_STARTED,
                                        AnimationPlayer.SIGNAL_FINISHED,
                                        AnimationPlayer.SIGNAL_CANCELLED));
        if (triggers != null) {
            player.prop("triggers", triggers);
        }
        UiNode.Builder win =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(UiNode.of(UiTypes.PANEL).id("dialog").prop("opacity", 0f).build())
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").prop("text", "OK").build())
                        .child(player.build());
        if (connections != null) {
            win.prop("connections", connections);
        }
        return win.build();
    }
}

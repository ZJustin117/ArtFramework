package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.presentation.EffectPulseComponent;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationRegistry;
import artframework.api.ArtFramework;
import artframework.component.ArtNodeTypes;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHosts;
import artframework.presentation.ConnectionDeclarationsComponent;
import artframework.presentation.EffectsComponent;
import artframework.presentation.PresentationRuntime;
import artframework.test.C1RuntimeFixture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        C1RuntimeFixture fixture = mountWithPlayConnection("ui/win/w/ok/pressed", false);
        ConnectionDeclarationsComponent declarations = fixture.context.world().get(
                fixture.root, ConnectionDeclarationsComponent.class);
        assertNotNull(declarations);
        assertEquals(1, declarations.connections.size());
        assertTrue(NodeConnections.subscriptionCount("win") >= 1);
        fixture.emit("ok", SignalNames.PRESSED);
        assertTrue(AnimationPlayers.get("win", "motion").isPlaying());
    }

    @Test
    public void regexMatchPlaysAnimation() {
        C1RuntimeFixture fixture = mountWithPlayConnection("ui/win/.+/pressed", true);
        fixture.emit("ok", SignalNames.PRESSED);
        assertTrue(AnimationPlayers.get("win", "motion").isPlaying());
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
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        fixture.emit("ok", SignalNames.PRESSED);
        assertTrue(AnimationPlayers.get("win", "motion").isPlaying());
    }

    @Test
    public void sourceShorthandResolvesTheScopedNodePath() {
        Map<String, Object> connection = new LinkedHashMap<String, Object>();
        connection.put("source", "ok");
        connection.put("signal", SignalNames.PRESSED);
        connection.put("action", UiActions.PLAY);
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("player", "motion");
        args.put("name", "enter");
        connection.put("args", args);

        C1RuntimeFixture fixture = C1RuntimeFixture.mount(
                "win", windowWithButtonAndPlayer(null, Collections.singletonList(connection)));
        fixture.emit("ok", SignalNames.PRESSED);
        assertTrue(AnimationPlayers.get("win", "motion").isPlaying());
    }

    @Test
    public void setPropFromPayload() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/win/w/wave/value_changed");
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
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        fixture.emit("wave", SignalNames.VALUE_CHANGED, Float.valueOf(0.8f));
        assertEquals(0.8f, ((Number) fixture.property("panel", "fx_intensity")).floatValue(), 0.001f);
    }

    @Test
    public void pulseEffectUpdatesBinding() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/win/w/ok/pressed");
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
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        fixture.emit("ok", SignalNames.PRESSED);
        assertTrue(EffectPulse.isActive("win", "panel"));
        assertEquals(1, artframework.ecs.ArtEcs.world().query(EffectPulseComponent.class).size());
        artframework.render.LightwaveControls.tickPulses(0.1f);
        EffectsComponent effects = fixture.context.world().get(
                fixture.find("panel"), EffectsComponent.class);
        float intensity = effects.get(LightwaveEffect.ID, "ambient").floatParam("intensity", 0f);
        assertTrue(intensity > 0.55f);
        artframework.render.LightwaveControls.tickPulses(0.2f);
        assertFalse(EffectPulse.isActive("win", "panel"));
        fixture.close();
    }

    @Test
    public void unknownActionFailsMount() {
        Map<String, Object> conn = new LinkedHashMap<String, Object>();
        conn.put("match", "ui/win/w/ok/pressed");
        conn.put("action", "not.a.real.action");
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("connections", Collections.singletonList(conn))
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        try {
            C1RuntimeFixture.mount("win", root);
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
        conn.put("match_pattern", "ui/win/w/ok/.*");
        conn.put("action", "mod.test_hit");
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("connections", Collections.singletonList(conn))
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("win", root);
        fixture.emit("ok", SignalNames.PRESSED);
        assertEquals(1, hits.get());
        assertTrue(ArtFramework.uiActionIds().contains("mod.test_hit"));
    }

    @Test
    public void unmountClearsSubscriptions() {
        C1RuntimeFixture fixture = mountWithPlayConnection("ui/win/w/ok/pressed", false);
        assertTrue(NodeConnections.subscriptionCount("win") >= 1);
        fixture.close();
        assertEquals(0, NodeConnections.subscriptionCount("win"));
    }

    @Test
    public void cleanupOfAnOldContextDoesNotDisconnectReplacementContext() {
        C1RuntimeFixture oldFixture = mountWithPlayConnection("reused", "ui/reused/w/ok/pressed", false);
        PresentationContext oldContext = oldFixture.context;
        PresentationRegistry.close(PresentationRuntime.c1Scope("reused"));

        C1RuntimeFixture replacement = mountWithPlayConnection("reused", "ui/reused/w/ok/pressed", false);
        try {
            assertTrue(NodeConnections.subscriptionCount("reused") >= 1);
            NodeConnections.clearContext(oldContext);
            assertTrue(NodeConnections.subscriptionCount("reused") >= 1);
            replacement.emit("ok", SignalNames.PRESSED);
            assertTrue(AnimationPlayers.get("reused", "motion").isPlaying());
        } finally {
            replacement.close();
        }
    }

    @Test
    public void failedSyncRollsBackEarlierDeclarations() {
        Map<String, Object> valid = new LinkedHashMap<String, Object>();
        valid.put("match", "ui/rollback/w/ok/pressed");
        valid.put("action", UiActions.PLAY);
        Map<String, Object> invalid = new LinkedHashMap<String, Object>();
        invalid.put("match", "ui/rollback/w/ok/released");
        invalid.put("action", "missing.action");
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .prop("connections", Arrays.asList(valid, invalid))
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mountWithoutConnections("rollback", root);
        try {
            try {
                NodeConnections.syncContext(fixture.context);
                fail("expected invalid action");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("unknown ui action"));
            }
            assertEquals(0, NodeConnections.subscriptionCount("rollback"));
        } finally {
            fixture.close();
        }
    }

    @Test
    public void failedResyncPreservesPreviouslyWorkingSubscriptions() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.registerUiAction("mod.rollback", new UiAction() {
            @Override public boolean run(UiActionContext ctx) {
                hits.incrementAndGet();
                return true;
            }
        });
        Map<String, Object> valid = new LinkedHashMap<String, Object>();
        valid.put("match", "ui/resync/w/ok/pressed");
        valid.put("action", "mod.rollback");
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("resync",
                windowWithButtonAndPlayer(null, Collections.singletonList(valid)));
        try {
            fixture.emit("ok", SignalNames.PRESSED);
            assertEquals(1, hits.get());
            hits.set(0);

            Map<String, Object> invalid = new LinkedHashMap<String, Object>();
            invalid.put("match", "ui/resync/w/ok/released");
            invalid.put("action", "missing.action");
            fixture.context.world().put(fixture.root,
                    ConnectionDeclarationsComponent.class,
                    new ConnectionDeclarationsComponent(Arrays.asList(valid, invalid), null));

            try {
                NodeConnections.syncContext(fixture.context);
                fail("expected invalid action");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("unknown ui action"));
            }
            assertEquals(1, NodeConnections.subscriptionCount("resync"));
            fixture.emit("ok", SignalNames.PRESSED);
            assertEquals(1, hits.get());
        } finally {
            fixture.close();
        }
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

    @Test
    public void connectionDeclarationsDeepCopyNestedArgs() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("value", "before");
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("nested", nested);
        Map<String, Object> connection = new LinkedHashMap<String, Object>();
        connection.put("match", "ui/win/w/ok/pressed");
        connection.put("action", UiActions.PLAY);
        connection.put("args", args);
        ConnectionDeclarationsComponent declarations = new ConnectionDeclarationsComponent(
                Collections.singletonList(connection), null);

        nested.put("value", "after");
        assertEquals("before", ((Map<?, ?>) ((Map<?, ?>) declarations.connections.get(0)
                .get("args")).get("nested")).get("value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionDeclarationsRejectHostValues() {
        Map<String, Object> connection = new LinkedHashMap<String, Object>();
        connection.put("host", new Object());
        new ConnectionDeclarationsComponent(Collections.singletonList(connection), null);
    }

    @Test
    public void wiredConnectionExposesImmutableNestedArgs() {
        final AtomicReference<Map<String, Object>> captured = new AtomicReference<Map<String, Object>>();
        ArtFramework.registerUiAction("mod.immutable_args", new UiAction() {
            @Override public boolean run(UiActionContext ctx) {
                captured.set(ctx.args);
                return true;
            }
        });
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("value", "before");
        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("nested", nested);
        Map<String, Object> connection = new LinkedHashMap<String, Object>();
        connection.put("match", "ui/win/w/ok/pressed");
        connection.put("action", "mod.immutable_args");
        connection.put("args", args);
        C1RuntimeFixture fixture = C1RuntimeFixture.mount(
                "win", windowWithButtonAndPlayer(null, Collections.singletonList(connection)));
        nested.put("value", "after");

        fixture.emit("ok", SignalNames.PRESSED);

        assertEquals("before", ((Map<?, ?>) captured.get().get("nested")).get("value"));
        try {
            captured.get().put("new", "value");
            fail("listener args must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    private static C1RuntimeFixture mountWithPlayConnection(String match, boolean pattern) {
        return mountWithPlayConnection("win", match, pattern);
    }

    private static C1RuntimeFixture mountWithPlayConnection(
            String windowId, String match, boolean pattern) {
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
        return C1RuntimeFixture.mount(
                windowId, windowWithButtonAndPlayer(null, Collections.singletonList(conn)));
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

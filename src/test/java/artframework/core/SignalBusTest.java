package artframework.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SignalBusTest {
    @Test
    public void signalPathsSeparateComponentsFromScopedC1Nodes() {
        assertEquals("ui/sts1.map/node_clicked", SignalPaths.component("sts1.map", "node_clicked"));
        assertEquals("ui/demo/root/actions/ok/pressed",
                SignalPaths.node("demo", "root/actions/ok", "pressed"));
    }

    @Test
    public void signalPathsTrimScopedIdentifiersAndRejectAmbiguousRoutes() {
        assertEquals("ui/sts1.map/node_clicked",
                SignalPaths.component(" sts1.map ", " node_clicked "));
        assertEquals("ui/demo/root/actions/ok/pressed",
                SignalPaths.node(" demo ", " root/actions/ok ", " pressed "));

        assertInvalidPath(new Runnable() {
            @Override public void run() { SignalPaths.component("sts1/map", "pressed"); }
        });
        assertInvalidPath(new Runnable() {
            @Override public void run() { SignalPaths.node("demo", "root//ok", "pressed"); }
        });
        assertInvalidPath(new Runnable() {
            @Override public void run() { SignalPaths.node("demo", "/root/ok", "pressed"); }
        });
        assertInvalidPath(new Runnable() {
            @Override public void run() { SignalPaths.node("demo", "root/ok", "pressed event"); }
        });
    }

    private static void assertInvalidPath(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected invalid signal path");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("invalid"));
        }
    }

    @Test
    public void rawBusRoutesRemainUnrestricted() {
        SignalBus bus = new SignalBus();
        final int[] calls = {0};
        bus.connect("legacy route/with spaces", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                calls[0]++;
                return SignalDecision.continueSignal();
            }
        });

        bus.emit(new UiSignal("legacy route/with spaces", "test", null));
        assertEquals(1, calls[0]);

        bus.connect(Pattern.compile("legacy route/.*"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                calls[0]++;
                return SignalDecision.continueSignal();
            }
        });
        bus.emit(new UiSignal("legacy route/with spaces", "test", null));
        assertEquals(3, calls[0]);
    }

    @Test
    public void exactAndRegexListenersShareRegistrationOrderAndSeeReplacement() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        bus.connect(Pattern.compile("ui/.*"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                seen.add("regex:" + signal.payload);
                return SignalDecision.replace(signal.replace("changed"));
            }
        });
        bus.connect("ui/button/clicked", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                seen.add("exact:" + signal.payload);
                return SignalDecision.continueSignal();
            }
        });

        SignalDispatchResult result = bus.emit(new UiSignal("ui/button/clicked", "button", "original"));
        assertEquals(Arrays.asList("regex:original", "exact:changed"), seen);
        assertEquals("changed", result.signal.payload);
    }

    @Test
    public void stopRejectedPreventsLaterListeners() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        bus.connect("ui/button/clicked", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                seen.add("first");
                return SignalDecision.stopRejected("no");
            }
        });
        bus.connect(Pattern.compile("ui/.*"), new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                seen.add("later");
                return SignalDecision.continueSignal();
            }
        });
        SignalDispatchResult result = bus.emit(new UiSignal("ui/button/clicked", "button", null));
        assertEquals(Arrays.asList("first"), seen);
        assertTrue(result.isRejected());
    }

    @Test
    public void stopHandledPreventsLaterListeners() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        bus.connect("op", signal -> { seen.add("first"); return SignalDecision.stopHandled("done"); });
        bus.connect("op", signal -> { seen.add("later"); return SignalDecision.continueSignal(); });

        SignalDispatchResult result = bus.emit(new UiSignal("op", "test", null));

        assertEquals(Arrays.asList("first"), seen);
        assertEquals(SignalDecision.Kind.STOP_HANDLED, result.terminal);
    }

    @Test
    public void invalidReplacementIdIsRejectedAndPreventsLaterListeners() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        bus.connect("op", signal -> SignalDecision.replace(new UiSignal(
                signal.group, "other-id", signal.name, signal.source, null, null)));
        bus.connect("op", signal -> { seen.add("later"); return SignalDecision.continueSignal(); });

        SignalDispatchResult result = bus.emit(new UiSignal(
                SignalGroups.DEFAULT, "id", "op", "test", null, null));

        assertTrue(result.isRejected());
        assertEquals(Arrays.<String>asList(), seen);
    }

    @Test
    public void invalidReplacementNameIsRejected() {
        SignalBus bus = new SignalBus();
        bus.connect("op", signal -> SignalDecision.replace(new UiSignal(
                signal.group, signal.id, "other-name", signal.source, null, null)));

        SignalDispatchResult result = bus.emit(new UiSignal(
                SignalGroups.DEFAULT, "id", "op", "test", null, null));

        assertTrue(result.isRejected());
    }

    @Test
    public void listenerExceptionPropagatesAndPreventsLaterListeners() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        bus.connect("op", signal -> { throw new IllegalStateException("boom"); });
        bus.connect("op", signal -> { seen.add("later"); return SignalDecision.continueSignal(); });

        try {
            bus.emit(new UiSignal("op", "test", null));
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
            assertEquals(Arrays.<String>asList(), seen);
            return;
        }
        throw new AssertionError("listener exception was not propagated");
    }

    @Test
    public void emitUsesListenerSnapshotWhenListenersConnectOrDisconnect() {
        SignalBus bus = new SignalBus();
        final List<String> seen = new ArrayList<String>();
        final SignalSubscription[] second = new SignalSubscription[1];
        bus.connect("op", signal -> {
            seen.add("first");
            second[0].disconnect();
            bus.connect("op", later -> { seen.add("new"); return SignalDecision.continueSignal(); });
            return SignalDecision.continueSignal();
        });
        second[0] = bus.connect("op", signal -> { seen.add("second"); return SignalDecision.continueSignal(); });

        bus.emit(new UiSignal("op", "test", null));
        assertEquals(Arrays.asList("first", "second"), seen);
        bus.emit(new UiSignal("op", "test", null));
        assertEquals(Arrays.asList("first", "second", "first", "new"), seen);
    }

    @Test
    public void subscriptionsDisconnectIndependently() {
        SignalHub hub = new SignalHub();
        final int[] seen = new int[2];
        SignalHandler first = new SignalHandler() {
            @Override public void handle(Object... args) { seen[0]++; }
        };
        SignalHandler second = new SignalHandler() {
            @Override public void handle(Object... args) { seen[1]++; }
        };

        SignalSubscription firstSubscription = hub.connect("button", "pressed", first);
        SignalSubscription secondSubscription = hub.connect("button", "pressed", second);
        firstSubscription.disconnect();
        hub.emit("button", "pressed");

        assertEquals(0, seen[0]);
        assertEquals(1, seen[1]);
        assertFalse(firstSubscription.isConnected());
        assertTrue(secondSubscription.isConnected());
    }

    @Test
    public void directDisconnectRemovesHubRegistration() {
        SignalHub hub = new SignalHub();
        SignalSubscription subscription = hub.connect("button", "pressed", args -> {});
        assertEquals(1, hub.handlerCount("button", "pressed"));

        subscription.disconnect();

        assertEquals(0, hub.handlerCount("button", "pressed"));
        assertFalse(subscription.isConnected());
    }

    @Test
    public void replacementMustStayInTheOriginalGroup() {
        SignalGroup group = SignalGroups.get("group-a");
        group.connect("op", signal -> SignalDecision.replace(
                new UiSignal("group-b", signal.id, signal.name, signal.source,
                        signal.payload, signal.metadata)));

        SignalDispatchResult result = group.dispatch(new UiSignal(
                "group-a", "id", "op", "source", null,
                java.util.Collections.<String, Object>emptyMap()));

        assertTrue(result.isRejected());
        assertTrue(result.message.contains("group"));
    }
}

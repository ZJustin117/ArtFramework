package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRuntime;
import artframework.test.C1RuntimeFixture;
import artframework.c2.NativeComponents;
import artframework.context.PresentSurfaces;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Contract tests for the signal API migration boundary. */
public class SignalApiContractTest {
    @Before public void resetSignalsBefore() {
        SignalGroups.resetForTests();
        NativeComponents.resetForTests();
        PresentSurfaces.resetForTests();
    }

    @After public void resetSignalsAfter() {
        NativeComponents.resetForTests();
        PresentSurfaces.resetForTests();
        SignalGroups.resetForTests();
    }

    @Test
    public void rawGroupDispatchPreservesDecisionResult() {
        SignalGroup group = new SignalGroup("contract");
        group.connect("op", new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopRejected("blocked");
            }
        });

        SignalDispatchResult result = group.dispatch(new UiSignal(
                "contract", "id", "op", "source", null, null));

        assertTrue(result.isRejected());
        assertEquals("blocked", result.message);
    }

    @Test
    public void publicGroupEmitRemainsResultBearingAlias() {
        SignalGroup group = new SignalGroup("emit-contract");
        group.connect("op", signal -> SignalDecision.stopHandled("handled"));

        SignalDispatchResult result = group.emit(new UiSignal(
                "emit-contract", "id", "op", "source", null, null));

        assertEquals(SignalDecision.Kind.STOP_HANDLED, result.terminal);
        assertEquals("handled", result.message);
    }

    @Test
    public void scopedC1CanReceiveDecisionAwareListener() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("contract-window", root);
        try {
            EntityId ok = fixture.find("ok");
            SignalSubscription subscription = PresentationRuntime.connectListener(
                    fixture.context, ok, SignalNames.PRESSED, new SignalListener() {
                        @Override public SignalDecision onSignal(UiSignal signal) {
                            return SignalDecision.stopRejected("no");
                        }
                    });

            SignalDispatchResult result = PresentationRuntime.dispatch(
                    fixture.context, ok, SignalNames.PRESSED);

            assertTrue(result.isRejected());
            assertEquals("no", result.message);
            subscription.disconnect();
        } finally {
            fixture.close();
        }
    }

    @Test
    public void legacyHandlerRemainsPayloadOnlyAndDisconnectable() {
        SignalHub hub = new SignalHub();
        final AtomicInteger hits = new AtomicInteger();
        SignalHandler handler = new SignalHandler() {
            @Override public void handle(Object... args) {
                assertEquals(1, args.length);
                assertEquals("payload", args[0]);
                hits.incrementAndGet();
            }
        };
        SignalSubscription subscription = hub.connect("button", SignalNames.PRESSED, handler);

        hub.dispatch("button", SignalNames.PRESSED, "payload");
        subscription.disconnect();
        hub.dispatch("button", SignalNames.PRESSED, "payload");

        assertEquals(1, hits.get());
    }

    @Test
    public void listenerDisconnectDoesNotRemoveOtherListener() {
        SignalHub hub = new SignalHub();
        final AtomicInteger firstHits = new AtomicInteger();
        final AtomicInteger secondHits = new AtomicInteger();
        SignalListener first = new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                firstHits.incrementAndGet();
                return SignalDecision.continueSignal();
            }
        };
        SignalListener second = new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                secondHits.incrementAndGet();
                return SignalDecision.continueSignal();
            }
        };
        SignalSubscription firstSubscription = hub.connectListener("isolation-button", SignalNames.PRESSED, first);
        hub.connectListener("isolation-button", SignalNames.PRESSED, second);

        firstSubscription.disconnect();
        hub.dispatch("isolation-button", SignalNames.PRESSED);

        assertEquals(0, firstHits.get());
        assertEquals(1, secondHits.get());
    }

    @Test
    public void listenerReplacementAndBothStopsAreOrdered() {
        SignalHub hub = new SignalHub();
        final String[] payload = {"original"};
        hub.connectListener("replacement-button", SignalNames.PRESSED, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.replace(signal.replace("changed"));
            }
        });
        hub.connectListener("replacement-button", SignalNames.PRESSED, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                payload[0] = String.valueOf(signal.payload);
                return SignalDecision.stopHandled("handled");
            }
        });

        SignalDispatchResult handled = hub.dispatch("replacement-button", SignalNames.PRESSED, "original");
        assertEquals("changed", payload[0]);
        assertEquals(SignalDecision.Kind.STOP_HANDLED, handled.terminal);

        SignalHub rejectedHub = new SignalHub();
        rejectedHub.connectListener("rejected-button", SignalNames.PRESSED, new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopRejected("rejected");
            }
        });
        assertTrue(rejectedHub.dispatch("rejected-button", SignalNames.PRESSED).isRejected());
    }

    @Test
    public void contextRawSubscriptionsAreClearedWithContextSignals() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("cleanup-window", root);
        final AtomicInteger exactHits = new AtomicInteger();
        final AtomicInteger regexHits = new AtomicInteger();
        try {
            PresentationRuntime.connectBus(fixture.context,
                    SignalPaths.node("cleanup-window", "w/ok", SignalNames.PRESSED),
                    signal -> { exactHits.incrementAndGet(); return SignalDecision.continueSignal(); });
            PresentationRuntime.connectBus(fixture.context, Pattern.compile("ui/cleanup-window/.*/pressed"),
                    signal -> { regexHits.incrementAndGet(); return SignalDecision.continueSignal(); });
            fixture.emit("ok", SignalNames.PRESSED);
            assertEquals(1, exactHits.get());
            assertEquals(1, regexHits.get());
            PresentationRuntime.clearSignals(fixture.context);
            fixture.emit("ok", SignalNames.PRESSED);
            assertEquals(1, exactHits.get());
            assertEquals(1, regexHits.get());
        } finally {
            fixture.close();
        }
    }

    @Test
    public void hubCleanupIndexesDisconnectMixedScopedAndRawSubscriptions() {
        SignalHub first = new SignalHub();
        SignalHub second = new SignalHub();
        final AtomicInteger firstHits = new AtomicInteger();
        final AtomicInteger secondHits = new AtomicInteger();
        SignalSubscription scoped = first.connectListener("shared", SignalNames.PRESSED,
                signal -> { firstHits.incrementAndGet(); return SignalDecision.continueSignal(); });
        SignalSubscription raw = second.connectBus(Pattern.compile("ui/shared/pressed"),
                signal -> { secondHits.incrementAndGet(); return SignalDecision.continueSignal(); });

        first.dispatch("shared", SignalNames.PRESSED);
        assertEquals(1, firstHits.get());
        assertEquals(1, secondHits.get());
        scoped.disconnect();
        second.clear();
        first.dispatch("shared", SignalNames.PRESSED);
        assertEquals(1, firstHits.get());
        assertEquals(1, secondHits.get());
        assertTrue(!scoped.isConnected());
        assertTrue(!raw.isConnected());
    }

    @Test
    public void frameworkResetDisconnectsScopedHubSubscriptions() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("reset-window", root);
        try {
            SignalSubscription subscription = PresentationRuntime.connect(
                    fixture.context, fixture.find("ok"), SignalNames.PRESSED, args -> {});
            assertTrue(subscription.isConnected());
            artframework.api.ArtFramework.resetForTests();
            assertTrue(!subscription.isConnected());
        } finally {
            artframework.api.ArtFramework.resetForTests();
        }
    }

    @Test
    public void nativeComponentUsesDispatchAsItsSingleDecisionAwarePath() {
        artframework.core.UiComponent component = NativeComponents.get(
                artframework.component.NativeTemplateIds.END_TURN);
        SignalListener listener = new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopRejected("native blocked");
            }
        };
        SignalSubscription subscription = component.connectListener(SignalNames.PRESSED, listener);
        try {
            assertTrue(component.dispatch(SignalNames.PRESSED).isRejected());
            assertTrue(component.dispatch(SignalNames.PRESSED).isRejected());
        } finally {
            subscription.disconnect();
        }
        try {
            component.dispatch(SignalNames.TOGGLED);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("undeclared native signal was accepted");
    }

    @Test
    public void presentSurfaceUsesDispatchAsItsSingleDecisionAwarePath() {
        artframework.core.UiComponent surface = PresentSurfaces.get(
                artframework.context.SurfaceIds.COMBAT_HAND);
        SignalListener listener = new SignalListener() {
            @Override public SignalDecision onSignal(UiSignal signal) {
                return SignalDecision.stopHandled("surface handled");
            }
        };
        SignalSubscription subscription = surface.connectListener(SignalNames.PRESSED, listener);
        try {
            assertEquals(SignalDecision.Kind.STOP_HANDLED,
                    surface.dispatch(SignalNames.PRESSED).terminal);
            assertEquals(SignalDecision.Kind.STOP_HANDLED,
                    surface.dispatch(SignalNames.PRESSED).terminal);
        } finally {
            subscription.disconnect();
        }
        try {
            surface.dispatch(SignalNames.TOGGLED);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("undeclared surface signal was accepted");
    }
}

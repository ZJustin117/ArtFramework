package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.PresentationContext;
import artframework.presentation.PresentationKey;
import artframework.presentation.PresentationRegistry;
import artframework.presentation.SignalPortsComponent;
import artframework.presentation.ConnectionDeclarationsComponent;
import artframework.test.C1RuntimeFixture;
import artframework.c2.NativeComponents;
import artframework.context.PresentSurfaces;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

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

    @Test
    public void entityDestroyDisconnectsSubscriptionsBeforeIdentityRecreation() {
        String scope = "entity-lifecycle";
        PresentationContext context = PresentationRegistry.context(scope);
        try {
            PresentationKey key = new PresentationKey("ui", "button");
            EntityId first = context.create(key, "button", "button", "test");
            context.world().put(first, SignalPortsComponent.class,
                    new SignalPortsComponent(java.util.Collections.singletonList(SignalNames.PRESSED)));
            final AtomicInteger hits = new AtomicInteger();
            SignalSubscription subscription = PresentationRuntime.connectListener(context, first,
                    SignalNames.PRESSED, signal -> {
                        hits.incrementAndGet();
                        return SignalDecision.continueSignal();
                    });

            assertTrue(context.destroy(first));
            assertTrue(!subscription.isConnected());
            EntityId second = context.create(key, "button", "button", "test");
            context.world().put(second, SignalPortsComponent.class,
                    new SignalPortsComponent(java.util.Collections.singletonList(SignalNames.PRESSED)));
            PresentationRuntime.dispatch(context, second, SignalNames.PRESSED);

            assertEquals(0, hits.get());
        } finally {
            PresentationRegistry.close(scope);
        }
    }

    @Test
    public void contextCloseDisconnectsRawSubscriptionsBeforeScopeRecreation() {
        String scope = "context-lifecycle";
        PresentationContext context = PresentationRegistry.context(scope);
        try {
            context.create(new PresentationKey("ui", "button"), "button", "button", "test");
            final AtomicInteger hits = new AtomicInteger();
            SignalSubscription subscription = PresentationRuntime.connectBus(context,
                    SignalPaths.node("context-lifecycle", "button", SignalNames.PRESSED),
                    signal -> {
                        hits.incrementAndGet();
                        return SignalDecision.continueSignal();
                    });

            PresentationRegistry.close(scope);
            assertTrue(!subscription.isConnected());
            PresentationContext recreated = PresentationRegistry.context(scope);
            recreated.create(new PresentationKey("ui", "button"), "button", "button", "test");
            SignalGroups.nativeGroup().dispatch(new UiSignal(
                    SignalGroups.DEFAULT, null,
                    SignalPaths.node("context-lifecycle", "button", SignalNames.PRESSED),
                    "test", null, null));

            assertEquals(0, hits.get());
        } finally {
            PresentationRegistry.close(scope);
        }
    }

    @Test
    public void obsoleteContextCloseCannotClearRecreatedScopeSignals() {
        String scope = "stale-context";
        PresentationContext oldContext = PresentationRegistry.context(scope);
        oldContext.create(new PresentationKey("ui", "button"), "button", "button", "test");
        PresentationRegistry.close(scope);

        PresentationContext current = PresentationRegistry.context(scope);
        EntityId entity = current.create(new PresentationKey("ui", "button"),
                "button", "button", "test");
        current.world().put(entity, SignalPortsComponent.class,
                new SignalPortsComponent(java.util.Collections.singletonList(SignalNames.PRESSED)));
        final AtomicInteger hits = new AtomicInteger();
        SignalSubscription subscription = PresentationRuntime.connectListener(current, entity,
                SignalNames.PRESSED, signal -> {
                    hits.incrementAndGet();
                    return SignalDecision.continueSignal();
                });

        oldContext.close();
        PresentationRuntime.dispatch(current, entity, SignalNames.PRESSED);

        assertTrue(subscription.isConnected());
        assertEquals(1, hits.get());
        PresentationRegistry.close(scope);
    }

    @Test
    public void entityDestroyClearsDeclarativeAndStateMachineSubscriptions() {
        String scope = "owned-lifecycle";
        PresentationContext context = PresentationRegistry.context(scope);
        try {
            final AtomicInteger actionHits = new AtomicInteger();
            UiActions.register("test.lifecycle_action", new UiAction() {
                @Override public boolean run(UiActionContext ignored) {
                    actionHits.incrementAndGet();
                    return true;
                }
            });
            PresentationKey key = new PresentationKey("ui", "owner");
            EntityId owner = context.create(key, "owner", "panel", "test");
            Map<String, Object> connection = new java.util.LinkedHashMap<String, Object>();
            connection.put("match", SignalPaths.node("owned-lifecycle", "owner", SignalNames.PRESSED));
            connection.put("action", "test.lifecycle_action");
            context.world().put(owner, ConnectionDeclarationsComponent.class,
                    new ConnectionDeclarationsComponent(
                            java.util.Collections.singletonList(connection), null));
            Map<String, Object> states = new java.util.LinkedHashMap<String, Object>();
            states.put("initial", "closed");
            Map<String, Object> transition = new java.util.LinkedHashMap<String, Object>();
            transition.put("from", "closed");
            transition.put("to", "open");
            transition.put("match", SignalPaths.node("owned-lifecycle", "owner", SignalNames.PRESSED));
            states.put("transitions", java.util.Collections.<Object>singletonList(transition));
            context.world().put(owner, artframework.presentation.NodePropertiesComponent.class,
                    new artframework.presentation.NodePropertiesComponent(
                            java.util.Collections.<String, Object>singletonMap("states", states)));
            NodeConnections.syncContext(context);
            NodeStateMachines.syncContext(context);
            NodeStateMachine fsm = NodeStateMachines.get("owned-lifecycle", "owner");
            assertTrue(fsm != null);
            assertEquals("closed", fsm.state());
            assertTrue(NodeConnections.subscriptionCount("owned-lifecycle") >= 1);

            assertTrue(context.destroy(owner));
            assertEquals(0, NodeConnections.subscriptionCount("owned-lifecycle"));
            assertTrue(NodeStateMachines.get("owned-lifecycle", "owner") == null);
            SignalGroups.nativeGroup().dispatch(new UiSignal(
                    SignalGroups.DEFAULT, null,
                    SignalPaths.node("owned-lifecycle", "owner", SignalNames.PRESSED),
                    "test", null, null));

            assertEquals(0, actionHits.get());
        } finally {
            PresentationRegistry.close(scope);
            UiActions.resetForTests();
        }
    }

    @Test
    public void signalOperationsRejectForeignAndRetiredEntities() {
        PresentationContext first = PresentationRegistry.context("ownership-first");
        PresentationContext second = PresentationRegistry.context("ownership-second");
        EntityId entity = first.create(new PresentationKey("ui", "button"),
                "button", "button", "test");
        first.world().put(entity, SignalPortsComponent.class,
                new SignalPortsComponent(java.util.Collections.singletonList(SignalNames.PRESSED)));

        boolean foreignConnectRejected = false;
        try {
            PresentationRuntime.connect(second, entity, SignalNames.PRESSED, args -> {});
        } catch (IllegalArgumentException expected) {
            foreignConnectRejected = true;
        }
        boolean foreignDispatchRejected = false;
        try {
            PresentationRuntime.dispatch(second, entity, SignalNames.PRESSED);
        } catch (IllegalArgumentException expected) {
            foreignDispatchRejected = true;
        }
        assertTrue(foreignConnectRejected);
        assertTrue(foreignDispatchRejected);
        assertTrue(first.destroy(entity));
        try {
            PresentationRuntime.dispatch(first, entity, SignalNames.PRESSED);
        } catch (IllegalArgumentException expected) {
            PresentationRegistry.close("ownership-first");
            PresentationRegistry.close("ownership-second");
            return;
        }
        throw new AssertionError("retired entity was accepted by signal runtime");
    }

    @Test
    public void allScopedEntitySignalOperationsRejectRetiredEntityConsistently() {
        PresentationContext context = PresentationRegistry.context("ownership-consistency");
        EntityId entity = context.create(new PresentationKey("ui", "button"),
                "button", "button", "test");
        context.world().put(entity, SignalPortsComponent.class,
                new SignalPortsComponent(java.util.Collections.singletonList(SignalNames.PRESSED)));
        SignalHandler handler = args -> {};
        SignalListener listener = signal -> SignalDecision.continueSignal();
        context.destroy(entity);

        assertOwnershipFailure(() -> PresentationRuntime.connect(
                context, entity, SignalNames.PRESSED, handler));
        assertOwnershipFailure(() -> PresentationRuntime.connectListener(
                context, entity, SignalNames.PRESSED, listener));
        assertOwnershipFailure(() -> PresentationRuntime.dispatch(
                context, entity, SignalNames.PRESSED));
        assertOwnershipFailure(() -> PresentationRuntime.disconnect(
                context, entity, SignalNames.PRESSED, handler));
        assertOwnershipFailure(() -> PresentationRuntime.disconnectListener(
                context, entity, SignalNames.PRESSED, listener));
        PresentationRegistry.close("ownership-consistency");
    }

    private static void assertOwnershipFailure(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            assertEquals("entity is not owned by presentation context", expected.getMessage());
            return;
        }
        throw new AssertionError("retired entity operation was accepted");
    }
}

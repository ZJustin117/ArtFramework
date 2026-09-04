package artframework.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransientSignalRuntimeTest {
    @Before public void resetBefore() { SignalGroups.resetForTests(); }
    @After public void resetAfter() { SignalGroups.resetForTests(); }

    @Test
    public void runtimeUsesItsOwnGroupAndPreservesSubscriptionOrder() {
        final List<String> seen = new ArrayList<String>();
        SignalGroups.nativeGroup().connect(TransientSignalPaths.SURFACE_LIFECYCLE,
                signal -> { seen.add("native"); return SignalDecision.continueSignal(); });
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        runtime.connect(TransientSignalPaths.SURFACE_LIFECYCLE,
                signal -> { seen.add("first"); return SignalDecision.continueSignal(); });
        runtime.connect(TransientSignalPaths.SURFACE_LIFECYCLE,
                signal -> { seen.add("second"); return SignalDecision.continueSignal(); });

        SignalDispatchResult result = runtime.dispatch(
                TransientSignalPaths.SURFACE_LIFECYCLE, "test", null);

        assertEquals(Arrays.asList("first", "second"), seen);
        assertEquals(SignalDecision.Kind.CONTINUE, result.terminal);
        assertEquals(SignalGroups.TRANSIENT_RUNTIME, result.signal.group);
        runtime.close();
    }

    @Test
    public void replacementAndHandledStopReturnSynchronousOutcome() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final List<String> seen = new ArrayList<String>();
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            seen.add("replace:" + signal.payload);
            return SignalDecision.replace(signal.replace("changed"));
        });
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            seen.add("handled:" + signal.payload);
            return SignalDecision.stopHandled("done");
        });
        runtime.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> { seen.add("later"); return SignalDecision.continueSignal(); });

        SignalDispatchResult result = runtime.dispatch(
                TransientSignalPaths.SURFACE_INTENT, "test", "original");

        assertEquals(Arrays.asList("replace:original", "handled:changed"), seen);
        assertEquals(SignalDecision.Kind.STOP_HANDLED, result.terminal);
        assertEquals("changed", result.signal.payload);
        assertEquals("done", result.message);
        runtime.close();
    }

    @Test
    public void closeDisconnectsOnlyOwnedSubscriptions() {
        final int[] ownedHits = {0};
        final int[] otherHits = {0};
        TransientSignalRuntime owner = new TransientSignalRuntime();
        TransientSignalRuntime other = new TransientSignalRuntime();
        owner.connect(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                signal -> { ownedHits[0]++; return SignalDecision.continueSignal(); });
        other.connect(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                signal -> { otherHits[0]++; return SignalDecision.continueSignal(); });

        owner.close();
        other.dispatch(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, "test", null);

        assertEquals(0, ownedHits[0]);
        assertEquals(1, otherHits[0]);
        assertTrue(owner.isClosed());
        other.close();
    }

    @Test(expected = IllegalStateException.class)
    public void dispatchIsRejectedAfterClose() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        runtime.close();
        runtime.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null);
    }

    @Test
    public void payloadDefensivelyCopiesNestedValuesAndRejectsHostValues() {
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        List<Object> values = new ArrayList<Object>();
        String[] tags = {"one", "two"};
        values.add(tags);
        nested.put("values", values);
        TransientSignalPayload payload = TransientSignalPayload.of(nested);
        tags[0] = "changed externally";
        values.add("changed externally");
        nested.put("other", "changed externally");

        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) payload.value();
        @SuppressWarnings("unchecked")
        List<Object> copiedValues = (List<Object>) copy.get("values");
        assertEquals(1, copy.size());
        assertEquals(1, copiedValues.size());
        assertEquals(Arrays.asList("one", "two"), copiedValues.get(0));
        assertFalse(copy == nested);
        assertImmutable(copiedValues);
        assertImmutable((List<Object>) copiedValues.get(0));
        assertUnsupported(new Object());
        assertUnsupported(new StringBuilder("host mutable"));
    }

    @Test
    public void rawUiSignalDispatchRemainsAvailableForCompatibility() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final Object hostCompatiblePayload = new Object();
        runtime.connect(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION,
                signal -> SignalDecision.stopHandled("received"));

        SignalDispatchResult result = runtime.dispatch(new UiSignal(SignalGroups.TRANSIENT_RUNTIME,
                "id", TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION, "test",
                hostCompatiblePayload, null));

        assertEquals(SignalDecision.Kind.STOP_HANDLED, result.terminal);
        assertTrue(result.signal.payload == hostCompatiblePayload);
        runtime.close();
    }

    @Test
    public void rawReplacementPayloadRemainsUnchangedInsideTypedListener() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final Object replacementPayload = new Object();
        final Object[] laterPayload = new Object[1];
        runtime.connect(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION, signal -> {
            if (!"typed".equals(signal.source)) {
                return SignalDecision.replace(signal.replace(replacementPayload));
            }
            SignalDispatchResult rawResult = runtime.dispatch(new UiSignal(
                    SignalGroups.TRANSIENT_RUNTIME, null,
                    TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION, "raw", null, null));
            assertTrue(rawResult.signal.payload == replacementPayload);
            return SignalDecision.continueSignal();
        });
        runtime.connect(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION, signal -> {
            if ("raw".equals(signal.source)) laterPayload[0] = signal.payload;
            return SignalDecision.continueSignal();
        });

        runtime.dispatch(TransientSignalPaths.AUTHORITY_BUSINESS_CONFIRMATION, "typed", null);

        assertTrue(laterPayload[0] == replacementPayload);
        runtime.close();
    }

    @Test
    public void payloadAcceptsExistingImmutableAuthorityFrame() {
        artframework.context.ContextFrame frame = artframework.context.ContextFrame.unavailable(42L);

        assertTrue(TransientSignalPayload.of(frame).value() == frame);
    }

    @Test
    public void typedPayloadIsImmutableForEveryListenerAndResultConsumer() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("nested", new Object[] {new String[] {"before"}});
        final Object[] secondPayload = new Object[1];
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) signal.payload;
            assertImmutable(root);
            @SuppressWarnings("unchecked")
            List<Object> nested = (List<Object>) root.get("nested");
            assertImmutable(nested);
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) nested.get(0);
            assertImmutable(array);
            return SignalDecision.continueSignal();
        });
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            secondPayload[0] = signal.payload;
            @SuppressWarnings("unchecked")
            List<Object> nested = (List<Object>) ((Map<String, Object>) signal.payload).get("nested");
            assertEquals(Arrays.asList("before"), nested.get(0));
            return SignalDecision.continueSignal();
        });

        SignalDispatchResult result = runtime.dispatch(
                TransientSignalPaths.SURFACE_INTENT, "test", input);

        assertTrue(secondPayload[0] == result.signal.payload);
        input.put("later", "mutated");
        assertEquals(1, ((Map<?, ?>) result.signal.payload).size());
        runtime.close();
    }

    @Test
    public void typedReplacementPayloadIsNormalizedBeforeLaterListenersAndResult() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final Object[] laterPayload = new Object[1];
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            Map<String, Object> mutable = new LinkedHashMap<String, Object>();
            mutable.put("array", new String[] {"replacement"});
            return SignalDecision.replace(signal.replace(mutable));
        });
        runtime.connect(TransientSignalPaths.SURFACE_INTENT, signal -> {
            laterPayload[0] = signal.payload;
            @SuppressWarnings("unchecked")
            List<Object> array = (List<Object>) ((Map<String, Object>) signal.payload).get("array");
            assertEquals(Arrays.asList("replacement"), array);
            assertImmutable(array);
            return SignalDecision.continueSignal();
        });

        SignalDispatchResult result = runtime.dispatch(
                TransientSignalPaths.SURFACE_INTENT, "test", "original");

        assertTrue(laterPayload[0] == result.signal.payload);
        assertImmutable((Map<String, Object>) result.signal.payload);
        runtime.close();
    }

    @Test
    public void typedReplacementRejectsUnsupportedPayloadBeforeLaterListeners() {
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final int[] laterHits = {0};
        runtime.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> SignalDecision.replace(signal.replace(new Object())));
        runtime.connect(TransientSignalPaths.SURFACE_INTENT,
                signal -> { laterHits[0]++; return SignalDecision.continueSignal(); });

        try {
            runtime.dispatch(TransientSignalPaths.SURFACE_INTENT, "test", null);
        } catch (IllegalArgumentException expected) {
            assertEquals(0, laterHits[0]);
            runtime.close();
            return;
        }
        throw new AssertionError("unsupported typed replacement was accepted");
    }

    @Test
    public void listenerDoesNotHoldRuntimeMonitorAndCloseDoesNotCancelInFlightDispatch()
            throws Exception {
        final TransientSignalRuntime runtime = new TransientSignalRuntime();
        final CountDownLatch listenerStarted = new CountDownLatch(1);
        final CountDownLatch operationDone = new CountDownLatch(1);
        final CountDownLatch allowListenerToFinish = new CountDownLatch(1);
        final int[] secondHits = {0};
        runtime.connect(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, signal -> {
            listenerStarted.countDown();
            await(operationDone);
            await(allowListenerToFinish);
            return SignalDecision.continueSignal();
        });
        runtime.connect(TransientSignalPaths.NATIVE_INTENT_LIFECYCLE,
                signal -> { secondHits[0]++; return SignalDecision.continueSignal(); });
        Thread dispatchThread = new Thread(() -> runtime.dispatch(
                TransientSignalPaths.NATIVE_INTENT_LIFECYCLE, "test", null));
        dispatchThread.start();
        assertTrue(listenerStarted.await(2, TimeUnit.SECONDS));

        Thread operationThread = new Thread(() -> {
            runtime.isClosed();
            operationDone.countDown();
            runtime.close();
        });
        operationThread.start();
        assertTrue(operationDone.await(2, TimeUnit.SECONDS));
        operationThread.join(2000L);
        assertFalse(operationThread.isAlive());
        allowListenerToFinish.countDown();
        dispatchThread.join(2000L);
        assertFalse(dispatchThread.isAlive());
        assertEquals(1, secondHits[0]);
        assertTrue(runtime.isClosed());
    }

    @Test
    public void closeAfterDispatchAdmissionPreservesTheCapturedListenerSet() throws Exception {
        final TransientSignalRuntime runtime = new TransientSignalRuntime();
        final CountDownLatch admitted = new CountDownLatch(1);
        final CountDownLatch allowSnapshot = new CountDownLatch(1);
        final int[] calls = {0};
        runtime.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });
        runtime.setDispatchAdmissionHookForTests(() -> {
            admitted.countDown();
            await(allowSnapshot);
        });
        Thread dispatch = new Thread(() -> runtime.dispatch(
                TransientSignalPaths.AUTHORITY_FRAME, "test", null));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));

        Thread close = new Thread(runtime::close);
        close.start();
        close.join(2000L);
        assertFalse(close.isAlive());
        allowSnapshot.countDown();
        dispatch.join(2000L);
        assertFalse(dispatch.isAlive());
        assertEquals(1, calls[0]);
        assertTrue(runtime.isClosed());
    }

    @Test
    public void resetAfterDispatchAdmissionPreservesTheCapturedListenerSet() throws Exception {
        final TransientSignalRuntime runtime = new TransientSignalRuntime();
        final CountDownLatch admitted = new CountDownLatch(1);
        final CountDownLatch allowDispatch = new CountDownLatch(1);
        final int[] calls = {0};
        runtime.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });
        runtime.setDispatchAdmissionHookForTests(() -> {
            admitted.countDown();
            await(allowDispatch);
        });
        Thread dispatch = new Thread(() -> runtime.dispatch(
                TransientSignalPaths.AUTHORITY_FRAME, "test", null));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));

        SignalGroups.resetForTests();
        allowDispatch.countDown();
        dispatch.join(2000L);

        assertFalse(dispatch.isAlive());
        assertEquals(1, calls[0]);
        assertRetired(() -> runtime.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null));
    }

    @Test
    public void resetRetiresExistingRuntimeWithoutCreatingAnOrphanGroup() {
        TransientSignalRuntime retired = new TransientSignalRuntime();
        SignalGroups.resetForTests();

        assertRetired(() -> retired.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.continueSignal()));
        assertRetired(() -> retired.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null));

        TransientSignalRuntime current = new TransientSignalRuntime();
        final int[] calls = {0};
        current.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });
        current.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null);
        assertEquals(1, calls[0]);
        current.close();
    }

    @Test
    public void resetRetiresAnIsolatedRuntimeForLaterConnectAndDispatch() {
        SignalGroup isolated = SignalGroups.isolatedTransientRuntimeGroup();
        TransientSignalRuntime retired = new TransientSignalRuntime(isolated, false);
        SignalGroups.resetForTests();

        assertRetired(() -> retired.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.continueSignal()));
        assertRetired(() -> retired.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null));
    }

    @Test
    public void resetRetiresIsolatedRuntimeAfterAdmissionButAdmittedDispatchCompletes() throws Exception {
        SignalGroup isolated = SignalGroups.isolatedTransientRuntimeGroup();
        final TransientSignalRuntime runtime = new TransientSignalRuntime(isolated, false);
        final CountDownLatch admitted = new CountDownLatch(1);
        final CountDownLatch allowDispatch = new CountDownLatch(1);
        final int[] calls = {0};
        runtime.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });
        runtime.setDispatchAdmissionHookForTests(() -> {
            admitted.countDown();
            await(allowDispatch);
        });

        Thread dispatch = new Thread(() -> runtime.dispatch(
                TransientSignalPaths.AUTHORITY_FRAME, "test", null));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));
        SignalGroups.resetForTests();
        allowDispatch.countDown();
        dispatch.join(2000L);

        assertFalse(dispatch.isAlive());
        assertEquals(1, calls[0]);
        assertRetired(() -> runtime.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> SignalDecision.continueSignal()));
        assertRetired(() -> runtime.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null));
    }

    @Test
    public void isolatedRuntimeCloseDisposesOnlyItsRegisteredGroup() {
        SignalGroup global = SignalGroups.transientRuntimeRawGroup();
        SignalGroup firstLocal = SignalGroups.isolatedTransientRuntimeGroup();
        SignalGroup secondLocal = SignalGroups.isolatedTransientRuntimeGroup();
        TransientSignalRuntime runtime = new TransientSignalRuntime(firstLocal, false);

        assertTrue(SignalGroups.isRegistered(global));
        assertTrue(SignalGroups.isRegistered(firstLocal));
        assertTrue(SignalGroups.isRegistered(secondLocal));
        runtime.close();
        assertTrue(SignalGroups.isRegistered(global));
        assertFalse(SignalGroups.isRegistered(firstLocal));
        assertTrue(SignalGroups.isRegistered(secondLocal));
    }

    @Test
    public void closeNeverClearsNativeOrCustomNonIsolatedGroup() {
        SignalGroup nativeGroup = SignalGroups.nativeGroup();
        SignalGroup customGroup = SignalGroups.get("custom-runtime-group");
        final int[] nativeHits = {0};
        final int[] customHits = {0};
        nativeGroup.connect("native-op", signal -> { nativeHits[0]++; return SignalDecision.continueSignal(); });
        customGroup.connect("custom-op", signal -> { customHits[0]++; return SignalDecision.continueSignal(); });

        new TransientSignalRuntime(nativeGroup, false).close();
        new TransientSignalRuntime(customGroup, false).close();

        nativeGroup.dispatch(new UiSignal(nativeGroup.id(), "id", "native-op", "test", null, null));
        customGroup.dispatch(new UiSignal(customGroup.id(), "id", "custom-op", "test", null, null));
        assertEquals(1, nativeHits[0]);
        assertEquals(1, customHits[0]);
    }

    @Test
    public void sharedIsolatedGroupIsDisposedOnlyAfterLastRuntimeCloses() {
        SignalGroup shared = SignalGroups.isolatedTransientRuntimeGroup();
        TransientSignalRuntime first = new TransientSignalRuntime(shared, false);
        TransientSignalRuntime second = new TransientSignalRuntime(shared, false);
        final int[] secondHits = {0};
        second.connect("op", signal -> { secondHits[0]++; return SignalDecision.continueSignal(); });

        first.close();
        assertTrue(SignalGroups.isRegistered(shared));
        second.dispatch("op", "test", null);
        assertEquals(1, secondHits[0]);

        second.close();
        assertFalse(SignalGroups.isRegistered(shared));
    }

    @Test
    public void resetDuringAdmittedDispatchRetainsCompletionLikeClose() throws Exception {
        final TransientSignalRuntime runtime = new TransientSignalRuntime();
        final CountDownLatch admitted = new CountDownLatch(1);
        final int[] calls = {0};
        runtime.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });
        runtime.setDispatchAdmissionHookForTests(() -> {
            admitted.countDown();
            SignalGroups.resetForTests();
        });

        Thread dispatch = new Thread(() -> runtime.dispatch(
                TransientSignalPaths.AUTHORITY_FRAME, "test", null));
        dispatch.start();
        assertTrue(admitted.await(2, TimeUnit.SECONDS));
        dispatch.join(2000L);
        assertFalse(dispatch.isAlive());
        assertEquals(1, calls[0]);
        assertTrue(runtime.isClosed() == false);
        runtime.close();
    }

    @Test(expected = IllegalStateException.class)
    public void processWideTransientGroupRejectsDirectRegistration() {
        SignalGroups.transientRuntimeRawGroup().connect("op",
                signal -> SignalDecision.continueSignal());
    }

    @Test
    public void closingNonIsolatedRuntimeNeverClearsItsSharedGroup() {
        SignalGroup nativeGroup = SignalGroups.nativeGroup();
        SignalGroup customGroup = SignalGroups.get("custom");
        final int[] nativeHits = {0};
        final int[] customHits = {0};
        nativeGroup.connect("native", signal -> {
            nativeHits[0]++;
            return SignalDecision.continueSignal();
        });
        customGroup.connect("custom", signal -> {
            customHits[0]++;
            return SignalDecision.continueSignal();
        });

        new TransientSignalRuntime(nativeGroup, false).close();
        new TransientSignalRuntime(customGroup, false).close();
        nativeGroup.dispatch(new UiSignal(SignalGroups.DEFAULT, null, "native", "test", null, null));
        customGroup.dispatch(new UiSignal("custom", null, "custom", "test", null, null));

        assertEquals(1, nativeHits[0]);
        assertEquals(1, customHits[0]);
        assertTrue(SignalGroups.isRegistered(nativeGroup));
        assertTrue(SignalGroups.isRegistered(customGroup));
    }

    @Test
    public void sharedIsolatedGroupRemainsUsableUntilItsLastRuntimeCloses() {
        SignalGroup isolated = SignalGroups.isolatedTransientRuntimeGroup();
        TransientSignalRuntime first = new TransientSignalRuntime(isolated, false);
        TransientSignalRuntime second = new TransientSignalRuntime(isolated, false);
        final int[] calls = {0};
        second.connect(TransientSignalPaths.AUTHORITY_FRAME,
                signal -> { calls[0]++; return SignalDecision.continueSignal(); });

        first.close();
        second.dispatch(TransientSignalPaths.AUTHORITY_FRAME, "test", null);

        assertEquals(1, calls[0]);
        assertTrue(SignalGroups.isRegistered(isolated));
        second.close();
        assertFalse(SignalGroups.isRegistered(isolated));
    }

    @Test
    public void processWideGroupIsExplicitlyLimitedToRawCompatibility() {
        SignalGroup processWideRawGroup = SignalGroups.transientRuntimeRawGroup();
        TransientSignalRuntime runtime = new TransientSignalRuntime();
        final Object rawPayload = new Object();
        final Object[] received = new Object[1];
        try {
            processWideRawGroup.connect(TransientSignalPaths.AUTHORITY_FRAME,
                    signal -> SignalDecision.continueSignal());
            fail("authority-only process-wide group accepted direct registration");
        } catch (IllegalStateException expected) {
            // Typed registration is owned by TransientSignalRuntime.
        }
        try {
            processWideRawGroup.connect(Pattern.compile("authority\\..*"),
                    signal -> SignalDecision.continueSignal());
            fail("authority-only process-wide pattern registration accepted directly");
        } catch (IllegalStateException expected) {
            // Raw compatibility does not permit direct group registration either.
        }
        runtime.connect(TransientSignalPaths.AUTHORITY_FRAME, signal -> {
            received[0] = signal.payload;
            return SignalDecision.continueSignal();
        });

        runtime.dispatch(new UiSignal(SignalGroups.TRANSIENT_RUNTIME, null,
                TransientSignalPaths.AUTHORITY_FRAME, "legacy", rawPayload, null));

        assertTrue(received[0] == rawPayload);
        runtime.close();
    }

    private static void assertUnsupported(Object value) {
        try {
            TransientSignalPayload.of(value);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("unsupported value was accepted");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void assertImmutable(Object value) {
        try {
            if (value instanceof Map) ((Map) value).put("mutate", "no");
            else ((List) value).add("mutate");
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("typed payload value was mutable");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static void assertRetired(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("retired"));
            return;
        }
        throw new AssertionError("reset-retired runtime remained usable");
    }
}

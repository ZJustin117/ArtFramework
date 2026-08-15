package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import artframework.presentation.NodeHierarchyComponent;
import artframework.presentation.NodeIdentityComponent;
import artframework.presentation.NodeLifecycleComponent;
import artframework.presentation.PresentationRuntime;
import artframework.test.C1RuntimeFixture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiTreeSignalTest {
    @Test public void mountBuildsNodesAndStablePaths() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("w", sample());
        try {
            assertEquals("w", PresentationRuntime.windowId(fixture.context));
            assertEquals(UiTypes.WINDOW,
                    PresentationRuntime.identity(fixture.context, fixture.root).type);
            EntityId ok = fixture.find("ok");
            assertEquals(UiTypes.BUTTON, PresentationRuntime.identity(fixture.context, ok).type);
            assertEquals(ok, fixture.find("comp_sample/main_col/actions/ok"));
        } finally { fixture.close(); }
    }

    @Test public void hierarchyAndLifecycleAreEcsBacked() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("w", sample());
        EntityId ok = fixture.find("ok");
        NodeLifecycleComponent lifecycle = PresentationRuntime.component(
                fixture.context, ok, NodeLifecycleComponent.class);
        assertTrue(lifecycle.mounted);
        assertTrue(lifecycle.ready);
        NodeHierarchyComponent hierarchy = PresentationRuntime.hierarchy(fixture.context, ok);
        assertNotNull(hierarchy.parent);
        fixture.close();
        assertFalse(fixture.context.world().contains(ok));
    }

    @Test public void signalConnectEmitDisconnect() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("w", sample());
        AtomicInteger count = new AtomicInteger();
        artframework.core.SignalHandler handler = args -> count.incrementAndGet();
        EntityId ok = fixture.find("ok");
        SignalSubscription subscription = PresentationRuntime.connect(
                fixture.context, ok, SignalNames.PRESSED, handler);
        fixture.emit("ok", SignalNames.PRESSED);
        subscription.disconnect();
        fixture.emit("ok", SignalNames.PRESSED);
        assertEquals(1, count.get());
        fixture.close();
    }

    @Test public void c1SignalsAreScopedByWindowAndNodePath() {
        C1RuntimeFixture first = C1RuntimeFixture.mount("first", sample());
        C1RuntimeFixture second = C1RuntimeFixture.mount("second", sample());
        AtomicInteger firstHits = new AtomicInteger();
        AtomicInteger secondHits = new AtomicInteger();
        try {
            PresentationRuntime.connect(first.context, first.find("ok"), SignalNames.PRESSED,
                    args -> firstHits.incrementAndGet());
            PresentationRuntime.connect(second.context, second.find("ok"), SignalNames.PRESSED,
                    args -> secondHits.incrementAndGet());

            first.emit("ok", SignalNames.PRESSED);
            assertEquals(1, firstHits.get());
            assertEquals(0, secondHits.get());

            second.emit("ok", SignalNames.PRESSED);
            assertEquals(1, firstHits.get());
            assertEquals(1, secondHits.get());
        } finally {
            first.close();
            second.close();
        }
    }

    @Test public void mutablePropsStayInEcs() {
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("w", sample());
        EntityId label = fixture.find("hello");
        PresentationRuntime.setProperty(fixture.context, label, "text", "Hi");
        assertEquals("Hi", PresentationRuntime.property(fixture.context, label, "text"));
        assertNotNull(PresentationRuntime.frame(fixture.context).items);
        fixture.close();
    }

    private static UiNode sample() {
        UiNode raw = UiNodeLoader.loadClasspath("layouts/composition_sample.json");
        return new artframework.component.TemplateExpander().expand(raw);
    }
}

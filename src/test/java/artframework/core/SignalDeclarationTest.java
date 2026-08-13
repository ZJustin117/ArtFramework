package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.ecs.EntityId;
import artframework.presentation.PresentationRuntime;
import artframework.presentation.SignalPortsComponent;
import artframework.test.C1RuntimeFixture;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SignalDeclarationTest {
    @Test public void instanceExposesDeclaredSignals() {
        C1RuntimeFixture fixture = mountButtonTree();
        SignalPortsComponent ports = PresentationRuntime.component(
                fixture.context, fixture.find("ok"), SignalPortsComponent.class);
        assertTrue(ports.canEmit(SignalNames.PRESSED));
        assertFalse(ports.canEmit(SignalNames.VALUE_CHANGED));
        assertEquals(1, ports.emits.size());
        fixture.close();
    }

    @Test public void connectAndEmitAllowDeclaredSignal() {
        C1RuntimeFixture fixture = mountButtonTree();
        final int[] n = {0};
        EntityId ok = fixture.find("ok");
        PresentationRuntime.connect(fixture.context, ok, SignalNames.PRESSED, args -> n[0]++);
        fixture.emit("ok", SignalNames.PRESSED);
        PresentationRuntime.emit(fixture.context, ok, SignalNames.PRESSED);
        assertEquals(2, n[0]);
        fixture.close();
    }

    @Test public void undeclaredPortsAreRejected() {
        C1RuntimeFixture fixture = mountButtonTree();
        EntityId ok = fixture.find("ok");
        try { fixture.emit("ok", SignalNames.TOGGLED); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(SignalNames.TOGGLED)); }
        try { PresentationRuntime.connect(fixture.context, ok, SignalNames.VALUE_CHANGED, args -> {}); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains(SignalNames.VALUE_CHANGED)); }
        fixture.close();
    }

    private static C1RuntimeFixture mountButtonTree() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        return C1RuntimeFixture.mount("win", root);
    }
}

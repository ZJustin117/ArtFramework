package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiNodeRegistry;
import artframework.component.UiNodeType;
import artframework.component.UiTypes;
import artframework.component.NodeKind;
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

    @Test public void declaredSignalsUseTheScopedSignalGrammar() {
        SignalPortsComponent ports = new SignalPortsComponent(
                java.util.Arrays.asList(" pressed ", "pressed"));
        assertEquals(java.util.Arrays.asList("pressed"), ports.emits);
        assertTrue(ports.canEmit(" pressed "));

        try {
            new SignalPortsComponent(java.util.Arrays.asList("pressed event"));
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("signal invalid"));
        }

        try {
            UiNode.of(UiTypes.BUTTON).signal("pressed/event").build();
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("signal invalid"));
        }
    }

    @Test public void defaultSignalsUseTheScopedSignalGrammar() {
        UiNodeRegistry registry = UiNodeRegistry.global();
        String validType = "test.signal_default";
        String invalidType = "test.invalid_signal_default";
        try {
            registry.register(UiNodeType.builder(validType)
                    .kind(NodeKind.LEAF)
                    .defaultSignals(java.util.Arrays.asList(" pressed "))
                    .build());
            UiNode valid = UiNode.of(validType).build();
            assertEquals(java.util.Arrays.asList("pressed"), valid.signals);
            assertTrue(valid.declaresSignal(" pressed "));

            registry.register(UiNodeType.builder(invalidType)
                    .kind(NodeKind.LEAF)
                    .defaultSignals(java.util.Arrays.asList("pressed/event"))
                    .build());
            try {
                UiNode.of(invalidType).build();
                fail();
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("signal invalid"));
            }
        } finally {
            registry.unregister(validType);
            registry.unregister(invalidType);
        }
    }

    private static C1RuntimeFixture mountButtonTree() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("w")
                .child(UiNode.of(UiTypes.BUTTON).id("ok").build()).build();
        return C1RuntimeFixture.mount("win", root);
    }
}

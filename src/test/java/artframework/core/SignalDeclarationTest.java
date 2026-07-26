package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.UiNode;
import artframework.component.UiTypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SignalDeclarationTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void instanceExposesDeclaredSignals() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        UiTree tree = UiTree.mount("win", root);
        UiInstance ok = tree.get("ok");
        assertTrue(ok.declaresSignal(SignalNames.PRESSED));
        assertFalse(ok.declaresSignal(SignalNames.VALUE_CHANGED));
        assertEquals(1, ok.signals().size());
    }

    @Test
    public void connectAndEmitAllowDeclaredSignal() {
        UiTree tree = mountButtonTree();
        final int[] n = {0};
        tree.connect(
                "ok",
                SignalNames.PRESSED,
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        n[0]++;
                    }
                });
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, n[0]);
        tree.get("ok").emit(SignalNames.PRESSED);
        assertEquals(2, n[0]);
    }

    @Test
    public void connectRejectsUndeclaredSignal() {
        UiTree tree = mountButtonTree();
        try {
            tree.connect(
                    "ok",
                    SignalNames.VALUE_CHANGED,
                    new SignalHandler() {
                        @Override
                        public void handle(Object... args) {}
                    });
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(SignalNames.VALUE_CHANGED));
        }
    }

    @Test
    public void emitRejectsUndeclaredSignal() {
        UiTree tree = mountButtonTree();
        try {
            tree.emit("ok", SignalNames.TOGGLED);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(SignalNames.TOGGLED));
        }
    }

    @Test
    public void instanceConnectRejectsUndeclared() {
        UiTree tree = mountButtonTree();
        try {
            tree.get("ok")
                    .connect(
                            "nope",
                            new SignalHandler() {
                                @Override
                                public void handle(Object... args) {}
                            });
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nope"));
        }
    }

    @Test
    public void unknownInstanceIdSkipsDeclarationCheck() {
        UiTree tree = mountButtonTree();
        final int[] n = {0};
        tree.connect(
                "ghost",
                "custom",
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        n[0]++;
                    }
                });
        tree.emit("ghost", "custom");
        assertEquals(1, n[0]);
    }

    private static UiTree mountButtonTree() {
        UiNode root =
                UiNode.of(UiTypes.WINDOW)
                        .id("w")
                        .prop("title", "T")
                        .child(UiNode.of(UiTypes.BUTTON).id("ok").build())
                        .build();
        return UiTree.mount("win", root);
    }
}

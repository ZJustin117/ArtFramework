package spireui.core;

import org.junit.After;
import org.junit.Test;
import spireui.api.SpireUI;
import spireui.api.UiOpResult;
import spireui.api.WindowClass;
import spireui.api.WindowDef;
import spireui.component.UiNode;
import spireui.component.UiNodeLoader;
import spireui.component.UiTypes;
import spireui.component.TemplateExpander;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class UiTreeSignalTest {

    @After
    public void tearDown() {
        SpireUI.resetForTests();
    }

    @Test
    public void mountBuildsInstancesAndIdIndex() {
        UiNode root = expandedSample();
        UiTree tree = UiTree.mount("w", root);
        assertEquals("w", tree.windowId());
        assertNotNull(tree.root());
        assertEquals(UiTypes.WINDOW, tree.root().type());
        assertNotNull(tree.get("ok"));
        assertEquals(UiTypes.BUTTON, tree.get("ok").type());
        assertNotNull(tree.get("intensity"));
        assertSame(tree, tree.get("ok").tree());
        assertEquals("ok", tree.get("ok").id());
    }

    @Test
    public void findByPathAndById() {
        UiTree tree = UiTree.mount("w", expandedSample());
        UiInstance ok = tree.find("main_col/actions/ok");
        assertNotNull(ok);
        assertEquals("ok", ok.id());
        assertSame(ok, tree.find("ok"));
        assertNull(tree.find("main_col/missing"));
        assertNull(tree.find("nope"));
    }

    @Test
    public void lifecycleMountReadyUnmountOrder() {
        final List<String> events = new ArrayList<String>();
        UiTree tree = UiTree.mount(
                "w",
                expandedSample(),
                new TreeLifecycle() {
                    @Override
                    public void onMount(UiInstance n) {
                        if (!n.id().isEmpty()) {
                            events.add("m:" + n.id());
                        }
                    }

                    @Override
                    public void onReady(UiInstance n) {
                        if (!n.id().isEmpty()) {
                            events.add("r:" + n.id());
                        }
                    }

                    @Override
                    public void onUnmount(UiInstance n) {
                        if (!n.id().isEmpty()) {
                            events.add("u:" + n.id());
                        }
                    }
                });
        assertTrue(events.indexOf("m:comp_sample") < events.indexOf("m:ok"));
        assertTrue(events.indexOf("r:ok") < events.indexOf("r:comp_sample"));
        assertTrue(tree.root().isMounted());
        tree.unmount();
        assertFalse(tree.root().isMounted());
        assertTrue(events.contains("u:ok"));
        assertTrue(events.indexOf("u:ok") < events.indexOf("u:comp_sample"));
    }

    @Test
    public void signalConnectEmitDisconnect() {
        UiTree tree = UiTree.mount("w", expandedSample());
        AtomicInteger presses = new AtomicInteger();
        AtomicReference<Object[]> lastArgs = new AtomicReference<Object[]>();
        SignalHandler h =
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        presses.incrementAndGet();
                        lastArgs.set(args);
                    }
                };
        tree.connect("ok", SignalNames.PRESSED, h);
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(1, presses.get());
        tree.emit("ok", SignalNames.PRESSED, "x");
        assertEquals(2, presses.get());
        assertEquals(1, lastArgs.get().length);
        tree.disconnect("ok", SignalNames.PRESSED, h);
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(2, presses.get());
    }

    @Test
    public void instanceConnectDelegatesToHub() {
        UiTree tree = UiTree.mount("w", expandedSample());
        AtomicInteger n = new AtomicInteger();
        tree.get("intensity")
                .connect(
                        SignalNames.VALUE_CHANGED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                n.incrementAndGet();
                                assertEquals(1, args.length);
                                assertEquals(0.75f, ((Float) args[0]).floatValue(), 0.001f);
                            }
                        });
        tree.get("intensity").emit(SignalNames.VALUE_CHANGED, Float.valueOf(0.75f));
        assertEquals(1, n.get());
    }

    @Test
    public void unmountClearsHandlers() {
        UiTree tree = UiTree.mount("w", expandedSample());
        AtomicInteger n = new AtomicInteger();
        tree.connect(
                "ok",
                SignalNames.PRESSED,
                new SignalHandler() {
                    @Override
                    public void handle(Object... args) {
                        n.incrementAndGet();
                    }
                });
        tree.unmount();
        tree.emit("ok", SignalNames.PRESSED);
        assertEquals(0, n.get());
    }

    @Test
    public void openSyntheticMountsTreeViaSpireUI() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        UiTree tree = SpireUI.tree("comp");
        assertNotNull(tree);
        assertNotNull(tree.get("ok"));
        assertTrue(tree.root().isMounted());
        SpireUI.close("comp");
        assertNull(SpireUI.tree("comp"));
    }

    @Test
    public void onButtonSugarConnectsSignalAndClickEmits() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        AtomicInteger viaSugar = new AtomicInteger();
        AtomicInteger viaSignal = new AtomicInteger();
        SpireUI.ops()
                .onButton(
                        "comp",
                        "ok",
                        new Runnable() {
                            @Override
                            public void run() {
                                viaSugar.incrementAndGet();
                            }
                        });
        SpireUI.tree("comp")
                .connect(
                        "ok",
                        SignalNames.PRESSED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                viaSignal.incrementAndGet();
                            }
                        });
        UiOpResult r = SpireUI.ops().clickButton("comp", "ok");
        assertTrue(r.isOk());
        assertEquals(1, viaSugar.get());
        assertEquals(1, viaSignal.get());
    }

    @Test
    public void setSliderEmitsValueChanged() {
        SpireUI.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        SpireUI.open("comp");
        AtomicReference<Float> v = new AtomicReference<Float>();
        SpireUI.tree("comp")
                .connect(
                        "intensity",
                        SignalNames.VALUE_CHANGED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                v.set((Float) args[0]);
                            }
                        });
        assertEquals(UiOpResult.Status.OK, SpireUI.ops().setSlider("comp", "intensity", 0.9f).status);
        assertEquals(0.9f, v.get().floatValue(), 0.001f);
    }

    @Test
    public void propsOverlayMutableOnInstance() {
        UiTree tree = UiTree.mount("w", expandedSample());
        UiInstance label = tree.get("hello");
        assertEquals("Hello composition", label.propString("text", ""));
        label.setProp("text", "Hi");
        assertEquals("Hi", label.propString("text", ""));
        assertEquals("Hello composition", label.decl().propString("text", ""));
    }

    @Test
    public void remountReplacesTree() {
        UiNode root = expandedSample();
        UiTree first = UiTrees.open("w", root);
        UiTree second = UiTrees.open("w", root);
        assertNotSame(first, second);
        assertSame(second, UiTrees.get("w"));
        assertFalse(first.root().isMounted());
        assertTrue(second.root().isMounted());
    }

    private static UiNode expandedSample() {
        UiNode raw = UiNodeLoader.loadClasspath("layouts/composition_sample.json");
        return new TemplateExpander().expand(raw);
    }
}

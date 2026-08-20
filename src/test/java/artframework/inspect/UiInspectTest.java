package artframework.inspect;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.UiOpResult;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.MapNodeRef;
import artframework.component.NativeTemplateIds;
import artframework.core.SignalHandler;
import artframework.core.SignalNames;
import artframework.presentation.PresentationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiInspectTest {

    @After
    public void tearDown() {
        UiLabListeners.resetForTests();
        ArtFramework.resetForTests();
    }

    @Test
    public void listSurfacesIncludesOpenWindow() {
        openComp();
        Map<String, Object> list = UiInspect.listSurfaces();
        @SuppressWarnings("unchecked")
        List<String> windows = (List<String>) list.get("windows");
        assertTrue(windows.contains("comp"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) list.get("components");
        assertFalse(components.isEmpty());
    }

    @Test
    public void listSurfacesDoesNotCreateAbsentPresentSurfaceContext() {
        PresentationRegistry.close("c2-surfaces");

        UiInspect.listSurfaces();

        assertEquals(null, PresentationRegistry.existingContext("c2-surfaces"));
    }

    @Test
    public void treeAndNodeExposeButton() {
        openComp();
        Map<String, Object> tree = UiInspect.treeMap("comp", 6);
        assertEquals("comp", tree.get("windowId"));
        assertNotNull(tree.get("root"));
        List<String> lines = UiInspect.treeLines("comp", 6);
        assertFalse(lines.isEmpty());
        boolean sawOk = false;
        for (String line : lines) {
            if (line.contains("ok") && line.contains("button")) {
                sawOk = true;
            }
        }
        assertTrue(sawOk);

        Map<String, Object> node = UiInspect.nodeMap("comp", "ok");
        assertFalse(node.containsKey("error"));
        @SuppressWarnings("unchecked")
        Map<String, Object> detail = (Map<String, Object>) node.get("node");
        assertEquals("button", detail.get("type"));
        @SuppressWarnings("unchecked")
        List<String> signals = (List<String>) detail.get("signals");
        assertTrue(signals.contains(SignalNames.PRESSED));
    }

    @Test
    public void emitFiresHandler() {
        openComp();
        final AtomicInteger presses = new AtomicInteger();
        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context("comp");
        artframework.presentation.PresentationRuntime.connect(context,
                        artframework.presentation.PresentationRuntime.find(context, "ok"),
                        SignalNames.PRESSED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                presses.incrementAndGet();
                            }
                        });
        UiOpResult r = UiInspect.emit("comp/ok", SignalNames.PRESSED);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, presses.get());
    }

    @Test
    public void emitUndeclaredFailsSoft() {
        openComp();
        UiOpResult r = UiInspect.emit("comp/ok", SignalNames.TOGGLED);
        assertEquals(UiOpResult.Status.UNAVAILABLE, r.status);
        assertTrue(r.message.contains("undeclared") || r.message.length() > 0);
    }

    @Test
    public void emitMissingWindow() {
        UiOpResult r = UiInspect.emit("missing/ok", SignalNames.PRESSED);
        assertEquals(UiOpResult.Status.NOT_BOUND, r.status);
    }

    @Test
    public void invokeSyntheticClickButton() {
        openComp();
        final AtomicInteger presses = new AtomicInteger();
        ArtFramework.ops()
                .onButton(
                        "comp",
                        "ok",
                        new Runnable() {
                            @Override
                            public void run() {
                                presses.incrementAndGet();
                            }
                        });
        UiOpResult r = UiInspect.invoke("comp", "click_button", "ok");
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, presses.get());
    }

    @Test
    public void parseArgsMapNodeAndTypes() {
        Object[] args =
                UiInspect.parseArgs(
                        new String[] {"x", "true", "1.5", "2,3,monster", "9"}, 1);
        assertEquals(4, args.length);
        assertEquals(Boolean.TRUE, args[0]);
        assertEquals(1.5f, ((Float) args[1]).floatValue(), 0.001f);
        assertTrue(args[2] instanceof MapNodeRef);
        MapNodeRef ref = (MapNodeRef) args[2];
        assertEquals(2, ref.row);
        assertEquals(3, ref.col);
        assertEquals("monster", ref.roomType);
        assertEquals(Integer.valueOf(9), args[3]);
    }

    @Test
    public void listenLogsOnEmit() {
        openComp();
        final List<String> lines = new ArrayList<String>();
        UiLabListeners.addSink(
                new UiLabListeners.LogSink() {
                    @Override
                    public void log(String line) {
                        lines.add(line);
                    }
                });
        UiOpResult listen = UiLabListeners.listen("comp/ok", SignalNames.PRESSED);
        assertEquals(UiOpResult.Status.OK, listen.status);
        UiInspect.emit("comp/ok", SignalNames.PRESSED);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith(UiLabListeners.SIGNAL_PREFIX));
        assertTrue(lines.get(0).contains("pressed"));
        UiLabListeners.unlisten("comp/ok", SignalNames.PRESSED);
        UiInspect.emit("comp/ok", SignalNames.PRESSED);
        assertEquals(1, lines.size());
    }

    @Test
    public void syntheticRetirementInvalidatesLabEntryAndRecreationRequiresRelisten() {
        openComp();
        final List<String> lines = new ArrayList<String>();
        UiLabListeners.addSink(new UiLabListeners.LogSink() {
            @Override public void log(String line) { lines.add(line); }
        });
        assertEquals(UiOpResult.Status.OK,
                UiLabListeners.listen("comp/ok", SignalNames.PRESSED).status);
        assertEquals(1, UiLabListeners.listKeys().size());

        ArtFramework.close("comp");
        assertEquals(0, UiLabListeners.listKeys().size());
        assertEquals(UiOpResult.Status.NOT_BOUND,
                UiLabListeners.unlisten("comp/ok", SignalNames.PRESSED).status);

        openComp();
        UiInspect.emit("comp/ok", SignalNames.PRESSED);
        assertEquals(0, lines.size());
        assertEquals(UiOpResult.Status.OK,
                UiLabListeners.listen("comp/ok", SignalNames.PRESSED).status);
        UiInspect.emit("comp/ok", SignalNames.PRESSED);
        assertEquals(1, lines.size());
    }

    @Test
    public void labRetirementIsolatedFromAnotherSyntheticWindow() {
        openComp();
        ArtFramework.register(new artframework.api.WindowDef(
                "comp-extra", artframework.api.WindowClass.SYNTHETIC,
                "layouts/composition_sample.json"));
        ArtFramework.open("comp-extra");
        assertEquals(UiOpResult.Status.OK,
                UiLabListeners.listen("comp/ok", SignalNames.PRESSED).status);
        assertEquals(UiOpResult.Status.OK,
                UiLabListeners.listen("comp-extra/ok", SignalNames.PRESSED).status);

        ArtFramework.close("comp");
        assertEquals(1, UiLabListeners.listKeys().size());
        assertEquals("comp-extra/ok pressed", UiLabListeners.listKeys().get(0));
    }

    @Test
    public void directSyntheticRuntimeRetirementInvalidatesControlLabEntry() {
        ArtFramework.register(new WindowDef("comp", WindowClass.SYNTHETIC,
                "layouts/composition_sample.json"));
        artframework.c1.SyntheticRuntime.open(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        assertEquals(UiOpResult.Status.OK,
                UiLabListeners.listen("comp/ok", SignalNames.PRESSED).status);

        artframework.c1.SyntheticRuntime.onClosed("comp");

        assertEquals(0, UiLabListeners.listKeys().size());
    }

    @Test
    public void nativeComponentEmit() {
        ArtFramework.register(
                new WindowDef(NativeTemplateIds.END_TURN, WindowClass.NATIVE_TEMPLATE, NativeTemplateIds.END_TURN));
        ArtFramework.bind(NativeTemplateIds.END_TURN);
        final AtomicInteger n = new AtomicInteger();
        ArtFramework.component(NativeTemplateIds.END_TURN)
                .connect(
                        SignalNames.PRESSED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                n.incrementAndGet();
                            }
                        });
        UiOpResult r = UiInspect.emit(NativeTemplateIds.END_TURN, SignalNames.PRESSED);
        assertEquals(UiOpResult.Status.OK, r.status);
        assertEquals(1, n.get());
    }

    private static void openComp() {
        ArtFramework.register(
                new WindowDef("comp", WindowClass.SYNTHETIC, "layouts/composition_sample.json"));
        ArtFramework.open("comp");
    }
}

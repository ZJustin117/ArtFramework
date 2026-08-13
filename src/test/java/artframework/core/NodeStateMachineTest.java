package artframework.core;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.NodeTree;
import artframework.presentation.NodeStateComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Milestone 41: declarative node FSM. */
public class NodeStateMachineTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void transitionOnExactMatch() {
        Map<String, Object> states = dualStateDecl("ui/open_btn/pressed", "ui/close_btn/pressed", false);
        NodeTree tree = mountFsm(states);
        NodeStateMachine fsm = NodeStateMachines.get("win", "gate");
        assertNotNull(fsm);
        assertEquals("closed", fsm.state());
        tree.emit("open_btn", SignalNames.PRESSED);
        assertEquals("open", fsm.state());
        assertEquals("open", tree.world().get(tree.get("gate").entityId(),
                NodeStateComponent.class).value);
        tree.emit("close_btn", SignalNames.PRESSED);
        assertEquals("closed", fsm.state());
    }

    @Test
    public void transitionOnRegex() {
        Map<String, Object> states = dualStateDecl("ui/open_btn/pressed", "ui/.+/pressed", true);
        // close uses regex that also matches open — order: first matching transition with from
        // Allow close only from open via separate transitions in dualStateDecl close pattern
        states = new LinkedHashMap<String, Object>();
        states.put("initial", "closed");
        List<Object> transitions = new ArrayList<Object>();
        Map<String, Object> open = new LinkedHashMap<String, Object>();
        open.put("from", "closed");
        open.put("to", "open");
        open.put("match_pattern", "ui/open_.*/pressed");
        transitions.add(open);
        Map<String, Object> close = new LinkedHashMap<String, Object>();
        close.put("from", "open");
        close.put("to", "closed");
        close.put("match_pattern", "ui/close_.*/pressed");
        transitions.add(close);
        states.put("transitions", transitions);
        NodeTree tree = mountFsm(states);
        NodeStateMachine fsm = ArtFramework.nodeState("win", "gate");
        assertEquals("closed", fsm.state());
        tree.emit("open_btn", SignalNames.PRESSED);
        assertEquals("open", fsm.state());
        tree.emit("close_btn", SignalNames.PRESSED);
        assertEquals("closed", fsm.state());
    }

    @Test
    public void stateChangedSignal() {
        Map<String, Object> states = dualStateDecl("ui/open_btn/pressed", "ui/close_btn/pressed", false);
        UiNode root = fsmRoot(states, true);
        NodeTree tree = NodeTree.mount("tree:win", root, null);
        final AtomicReference<String> last = new AtomicReference<String>();
        tree.get("gate")
                .connect(
                        SignalNames.STATE_CHANGED,
                        new SignalHandler() {
                            @Override
                            public void handle(Object... args) {
                                if (args != null && args.length > 0) {
                                    last.set(String.valueOf(args[0]));
                                }
                            }
                        });
        tree.emit("open_btn", SignalNames.PRESSED);
        assertEquals("open", last.get());
    }

    @Test
    public void onEnterRunsAction() {
        final AtomicInteger hits = new AtomicInteger();
        ArtFramework.registerUiAction(
                "mod.enter_hit",
                new UiAction() {
                    @Override
                    public boolean run(UiActionContext ctx) {
                        hits.incrementAndGet();
                        return true;
                    }
                });
        Map<String, Object> states = new LinkedHashMap<String, Object>();
        states.put("initial", "a");
        List<Object> transitions = new ArrayList<Object>();
        Map<String, Object> t = new LinkedHashMap<String, Object>();
        t.put("from", "a");
        t.put("to", "b");
        t.put("match", "ui/go/pressed");
        List<Object> onEnter = new ArrayList<Object>();
        Map<String, Object> act = new LinkedHashMap<String, Object>();
        act.put("action", "mod.enter_hit");
        onEnter.add(act);
        t.put("on_enter", onEnter);
        transitions.add(t);
        states.put("transitions", transitions);
        NodeTree tree = mountFsm(states);
        tree.emit("go", SignalNames.PRESSED);
        assertEquals("b", NodeStateMachines.get("win", "gate").state());
        assertEquals(1, hits.get());
    }

    private static Map<String, Object> dualStateDecl(String openOn, String closeOn, boolean closeRegex) {
        Map<String, Object> states = new LinkedHashMap<String, Object>();
        states.put("initial", "closed");
        List<Object> transitions = new ArrayList<Object>();
        Map<String, Object> open = new LinkedHashMap<String, Object>();
        open.put("from", "closed");
        open.put("to", "open");
        open.put("match", openOn);
        transitions.add(open);
        Map<String, Object> close = new LinkedHashMap<String, Object>();
        close.put("from", "open");
        close.put("to", "closed");
        if (closeRegex) {
            close.put("match_pattern", closeOn);
        } else {
            close.put("match", closeOn);
        }
        transitions.add(close);
        states.put("transitions", transitions);
        return states;
    }

    private static NodeTree mountFsm(Map<String, Object> states) {
        return NodeTree.mount("tree:win", fsmRoot(states, false), null);
    }

    private static UiNode fsmRoot(Map<String, Object> states, boolean stateChangedSignal) {
        UiNode.Builder gate =
                UiNode.of(UiTypes.PANEL).id("gate").prop("states", states);
        if (stateChangedSignal) {
            gate.signals(Collections.singletonList(SignalNames.STATE_CHANGED));
        }
        return UiNode.of(UiTypes.WINDOW)
                .id("w")
                .prop("title", "T")
                .child(gate.build())
                .child(UiNode.of(UiTypes.BUTTON).id("open_btn").build())
                .child(UiNode.of(UiTypes.BUTTON).id("close_btn").build())
                .child(UiNode.of(UiTypes.BUTTON).id("go").build())
                .build();
    }
}

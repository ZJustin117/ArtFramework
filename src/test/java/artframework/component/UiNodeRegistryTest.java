package artframework.component;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UiNodeRegistryTest {

    @After
    public void tearDown() {
        UiNodeRegistry.global().resetBuiltinsForTests();
    }

    @Test
    public void builtinsRegistered() {
        UiNodeRegistry r = UiNodeRegistry.global();
        assertTrue(r.contains(UiTypes.WINDOW));
        assertTrue(r.contains(UiTypes.BUTTON));
        assertTrue(r.contains(UiTypes.REF));
        assertEquals(NodeKind.CONTAINER, r.get(UiTypes.COL).kind());
        assertEquals(NodeKind.LEAF, r.get(UiTypes.LABEL).kind());
        assertEquals(NodeKind.COMPOSITION, r.get(UiTypes.SLOT).kind());
        assertTrue(r.get(UiTypes.BUTTON).allowsChildren() == false);
        assertTrue(r.get(UiTypes.PANEL).allowsChildren());
    }

    @Test
    public void thirdPartyNamespacedType() {
        UiNodeRegistry r = new UiNodeRegistry();
        r.installBuiltins();
        UiNodeType custom =
                UiNodeType.builder("my_mod.ripple")
                        .kind(NodeKind.VISUAL)
                        .allowsChildren(true)
                        .defaultSignals(Collections.singletonList("completed"))
                        .build();
        r.register(custom);
        assertSame(custom, r.get("my_mod.ripple"));
        r.unregister("my_mod.ripple");
        assertNull(r.get("my_mod.ripple"));
    }

    @Test
    public void rejectUnnamespacedThirdParty() {
        UiNodeRegistry r = new UiNodeRegistry();
        try {
            r.register(UiNodeType.builder("ripple").kind(NodeKind.VISUAL).build());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("namespace"));
        }
    }

    @Test
    public void rejectBuiltinOverwrite() {
        UiNodeRegistry r = UiNodeRegistry.global();
        try {
            r.register(UiNodeType.builder(UiTypes.BUTTON).kind(NodeKind.LEAF).build());
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("builtin") || e.getMessage().contains("button"));
        }
    }

    @Test
    public void loaderUsesRegistryForKnownAndUnknown() {
        UiNodeRegistry.global()
                .register(
                        UiNodeType.builder("my_mod.badge")
                                .kind(NodeKind.LEAF)
                                .allowsChildren(false)
                                .build());
        UiNode n =
                UiNodeLoader.parse(
                        "{"
                                + "\"type\":\"window\",\"id\":\"w\",\"title\":\"T\","
                                + "\"children\":[{\"type\":\"my_mod.badge\",\"id\":\"b\",\"text\":\"x\"}]"
                                + "}");
        assertEquals("my_mod.badge", n.children.get(0).type);
        try {
            UiNodeLoader.parse(
                    "{\"type\":\"window\",\"id\":\"w\",\"title\":\"T\","
                            + "\"children\":[{\"type\":\"not_registered\",\"id\":\"x\"}]}");
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("unknown") || e.getMessage().contains("not_registered"));
        }
    }

    @Test
    public void validateRejectsChildrenOnLeaf() {
        UiNodeType leaf =
                UiNodeType.builder("my_mod.chip")
                        .kind(NodeKind.LEAF)
                        .allowsChildren(false)
                        .build();
        UiNodeRegistry.global().register(leaf);
        UiNode bad =
                UiNode.of("my_mod.chip")
                        .id("c")
                        .child(UiNode.of(UiTypes.LABEL).id("l").build())
                        .build();
        try {
            UiNodeRegistry.global().validate(bad);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().toLowerCase().contains("child"));
        }
    }

    @Test
    public void artFacadeExposesNodes() {
        assertNotNull(artframework.api.ArtFramework.nodes());
        assertTrue(artframework.api.ArtFramework.nodes().contains(UiTypes.WINDOW));
    }

    @Test
    public void namespacedTypeRequiresDot() {
        assertTrue(UiNodeRegistry.isValidThirdPartyType("a.b"));
        assertTrue(UiNodeRegistry.isValidThirdPartyType("my_mod.ripple_effect"));
        assertFalse(UiNodeRegistry.isValidThirdPartyType("button"));
        assertFalse(UiNodeRegistry.isValidThirdPartyType(".x"));
        assertFalse(UiNodeRegistry.isValidThirdPartyType("x."));
        assertFalse(UiNodeRegistry.isValidThirdPartyType(""));
    }

    @Test
    public void defaultSignalsFromTypeDef() {
        UiNodeType t =
                UiNodeType.builder("my_mod.pulse")
                        .kind(NodeKind.BEHAVIOR)
                        .defaultSignals(Arrays.asList("started", "finished"))
                        .build();
        assertEquals(Arrays.asList("started", "finished"), t.defaultSignals());
    }
}

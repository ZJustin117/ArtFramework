package artframework.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LayoutEngineTest {

    @Test
    public void colStacksVertically() {
        UiNode root = UiNode.of(UiTypes.COL)
                .id("root")
                .layout(new LayoutSpec(200f, 100f, 0f, 10f, false))
                .child(UiNode.of(UiTypes.LABEL).id("a").layout(new LayoutSpec(0f, 20f, 0f, 0f, false)).build())
                .child(UiNode.of(UiTypes.LABEL).id("b").layout(new LayoutSpec(0f, 20f, 0f, 0f, false)).build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(new Rect(0f, 0f, 200f, 100f), r.rootBounds);
        Rect a = r.boundsOf("a");
        Rect b = r.boundsOf("b");
        assertNotNull(a);
        assertNotNull(b);
        assertEquals(0f, a.y, 0.001f);
        assertEquals(30f, b.y, 0.001f);
        assertEquals(20f, a.height, 0.001f);
    }

    @Test
    public void rowPlacesHorizontallyWithGrow() {
        UiNode root = UiNode.of(UiTypes.ROW)
                .id("row")
                .layout(new LayoutSpec(300f, 40f, 0f, 0f, false))
                .child(UiNode.of(UiTypes.BUTTON).id("l").layout(new LayoutSpec(0f, 40f, 0f, 0f, true)).build())
                .child(UiNode.of(UiTypes.BUTTON).id("r").layout(new LayoutSpec(0f, 40f, 0f, 0f, true)).build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(150f, r.boundsOf("l").width, 0.001f);
        assertEquals(150f, r.boundsOf("r").width, 0.001f);
        assertEquals(150f, r.boundsOf("r").x, 0.001f);
    }

    @Test
    public void stackOverlapsChildren() {
        UiNode root = UiNode.of(UiTypes.STACK)
                .layout(new LayoutSpec(100f, 80f, 5f, 0f, false))
                .child(UiNode.of(UiTypes.HITAREA).id("z").build())
                .child(UiNode.of(UiTypes.LABEL).id("t").build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(r.boundsOf("z"), r.boundsOf("t"));
        assertEquals(5f, r.boundsOf("z").x, 0.001f);
        assertEquals(90f, r.boundsOf("z").width, 0.001f);
    }

    @Test
    public void effectsIgnoredForGeometry() {
        UiNode withFx = UiNode.of(UiTypes.BUTTON)
                .id("b")
                .layout(new LayoutSpec(50f, 20f, 0f, 0f, false))
                .effect(new EffectDecl("glow", null))
                .build();
        UiNode bare = UiNode.of(UiTypes.BUTTON)
                .id("b")
                .layout(new LayoutSpec(50f, 20f, 0f, 0f, false))
                .build();
        assertEquals(LayoutEngine.preferredWidth(withFx), LayoutEngine.preferredWidth(bare), 0.001f);
        assertEquals(LayoutEngine.preferredHeight(withFx), LayoutEngine.preferredHeight(bare), 0.001f);
    }

    @Test
    public void compileSampleIndexesInteractiveIds() {
        LayoutResult r = Composition.compile(
                new StringBuilder()
                        .append("{\"type\":\"window\",\"id\":\"w\",\"title\":\"T\",\"width\":400,\"height\":200,")
                        .append("\"children\":[{\"type\":\"button\",\"id\":\"go\",\"text\":\"Go\"}]}")
                        .toString());
        assertTrue(r.hasId("go"));
        assertTrue(r.hasId("w"));
        assertEquals(400f, r.rootBounds.width, 0.001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void layoutRejectsUnexpandedRef() {
        LayoutEngine.layout(UiNode.of(UiTypes.REF).ref("x").build());
    }

    @Test
    public void stretchRatioSplitsExpandSpace() {
        LayoutSpec grow2 =
                new LayoutSpec(
                        0f,
                        40f,
                        0f,
                        0f,
                        0f,
                        0f,
                        SizeFlags.FILL | SizeFlags.EXPAND,
                        SizeFlags.DEFAULT,
                        2f,
                        Align.BEGIN);
        LayoutSpec grow1 =
                new LayoutSpec(
                        0f,
                        40f,
                        0f,
                        0f,
                        0f,
                        0f,
                        SizeFlags.FILL | SizeFlags.EXPAND,
                        SizeFlags.DEFAULT,
                        1f,
                        Align.BEGIN);
        UiNode root = UiNode.of(UiTypes.ROW)
                .id("row")
                .layout(new LayoutSpec(300f, 40f, 0f, 0f, false))
                .child(UiNode.of(UiTypes.BUTTON).id("a").layout(grow2).build())
                .child(UiNode.of(UiTypes.BUTTON).id("b").layout(grow1).build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(200f, r.boundsOf("a").width, 0.001f);
        assertEquals(100f, r.boundsOf("b").width, 0.001f);
    }

    @Test
    public void minWidthRaisesPreferredAboveNatural() {
        LayoutSpec min =
                new LayoutSpec(
                        0f,
                        20f,
                        150f,
                        0f,
                        0f,
                        0f,
                        SizeFlags.DEFAULT,
                        SizeFlags.DEFAULT,
                        1f,
                        Align.BEGIN);
        UiNode leaf = UiNode.of(UiTypes.BUTTON).id("b").layout(min).build();
        assertEquals(150f, LayoutEngine.preferredWidth(leaf), 0.001f);
    }

    @Test
    public void minWidthRaisesExplicitSmallWidth() {
        LayoutSpec min =
                new LayoutSpec(
                        40f,
                        20f,
                        80f,
                        0f,
                        0f,
                        0f,
                        SizeFlags.DEFAULT,
                        SizeFlags.DEFAULT,
                        1f,
                        Align.BEGIN);
        UiNode leaf = UiNode.of(UiTypes.BUTTON).id("b").layout(min).build();
        assertEquals(80f, LayoutEngine.preferredWidth(leaf), 0.001f);
    }

    @Test
    public void crossAxisAlignCenterInCol() {
        LayoutSpec child =
                new LayoutSpec(
                        50f,
                        20f,
                        0f,
                        0f,
                        0f,
                        0f,
                        SizeFlags.DEFAULT,
                        SizeFlags.DEFAULT,
                        1f,
                        Align.CENTER);
        UiNode root = UiNode.of(UiTypes.COL)
                .layout(new LayoutSpec(200f, 100f, 0f, 0f, false))
                .child(UiNode.of(UiTypes.LABEL).id("c").layout(child).build())
                .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(75f, r.boundsOf("c").x, 0.001f);
    }

    @Test
    public void jsonLoaderParsesMinAndFlags() {
        UiNode n = UiNodeLoader.parse(
                "{\"type\":\"button\",\"id\":\"b\",\"text\":\"X\",\"minWidth\":90,\"sizeFlagsH\":3,\"stretchRatio\":2}");
        assertEquals(90f, n.layout.minWidth, 0.001f);
        assertTrue(n.layout.expandsH());
        assertEquals(2f, n.layout.stretchRatio, 0.001f);
    }

    @Test
    public void gridPlacesTwoColumns() {
        UiNode root =
                UiNode.of(UiTypes.GRID)
                        .id("g")
                        .prop("columns", Integer.valueOf(2))
                        .layout(new LayoutSpec(220f, 100f, 0f, 10f, false))
                        .child(
                                UiNode.of(UiTypes.LABEL)
                                        .id("a")
                                        .layout(new LayoutSpec(0f, 20f, 0f, 0f, false))
                                        .build())
                        .child(
                                UiNode.of(UiTypes.LABEL)
                                        .id("b")
                                        .layout(new LayoutSpec(0f, 20f, 0f, 0f, false))
                                        .build())
                        .child(
                                UiNode.of(UiTypes.LABEL)
                                        .id("c")
                                        .layout(new LayoutSpec(0f, 20f, 0f, 0f, false))
                                        .build())
                        .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertEquals(0f, r.boundsOf("a").x, 0.001f);
        assertEquals(115f, r.boundsOf("b").x, 0.001f);
        assertEquals(0f, r.boundsOf("c").x, 0.001f);
        assertEquals(30f, r.boundsOf("c").y, 0.001f);
    }

    @Test
    public void tabsLaysOutOnlyActiveChild() {
        UiNode root =
                UiNode.of(UiTypes.TABS)
                        .id("t")
                        .prop("active", Integer.valueOf(1))
                        .layout(new LayoutSpec(200f, 80f, 0f, 0f, false))
                        .child(
                                UiNode.of(UiTypes.LABEL)
                                        .id("tab0")
                                        .layout(new LayoutSpec(100f, 40f, 0f, 0f, false))
                                        .build())
                        .child(
                                UiNode.of(UiTypes.LABEL)
                                        .id("tab1")
                                        .layout(new LayoutSpec(100f, 40f, 0f, 0f, false))
                                        .build())
                        .build();
        LayoutResult r = LayoutEngine.layout(root);
        assertNotNull(r.boundsOf("tab1"));
        assertEquals(null, r.boundsOf("tab0"));
    }
}

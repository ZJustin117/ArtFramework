package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.context.ContextFrame;
import artframework.context.ControlsView;
import artframework.context.FakeSignalBackend;
import artframework.context.MapNodeView;
import artframework.context.MapView;
import artframework.context.SurfaceIds;
import artframework.context.ViewportView;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import artframework.sts1.PresentSafety;
import artframework.sts1.assets.Sts1HostAssets;
import artframework.sts1.input.CombatInputRouter;
import artframework.sts1.input.RecordingIntentExecutor;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * V03: the delegated MAP surface's pixels are drawn at the NATIVE map point (the
 * {@code TopPanel.render} entry, where native {@code DungeonMapScreen.render} was skipped) rather
 * than in the post-native overlay, so the native top-panel HUD that draws afterwards is retained.
 * These tests pin the decision seam, the once-per-frame guard, fail-open, and that the band content
 * (background + edges + legend/node submissions) is unchanged versus the previous post-native draw.
 */
public class Sts1SurfaceRendererMapPointTest {

    @After
    public void tearDown() {
        Sts1SurfaceRenderer.resetMapBandGuardForTests();
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
        MapDrawPath.resetForTests();
        Sts1RenderPipeline.resetForTests();
        FullPresentMode.resetForTests();
        CombatInputRouter.resetForTests();
        PresentSafety.resetForTests();
    }

    private void publishedMapFrame() {
        Sts1HostAssets.install();
        MapView.MapBackground background = new MapView.MapBackground(
                777f, 777f - 120f, 0f, 1f, 1920, 1080, 1020f, 512f, 1f);
        List<MapView.MapEdgeView> edges = Collections.singletonList(
                new MapView.MapEdgeView(0.09f, 0.13f, 0.17f, 1f,
                        Collections.singletonList(new MapView.MapDotView(10f, 20f, 0f))));
        List<MapNodeView> nodes = Arrays.asList(
                new MapNodeView(1, 2, 100f, 200f, false, true, true, false,
                        64f, 64f, "M", "monster", ResourceIds.MAP_NODE_MONSTER),
                new MapNodeView(3, 1, 250f, 325f, true, false, false, false,
                        64f, 64f, "R", "rest", ResourceIds.MAP_NODE_REST));
        FakeSignalBackend backend = new FakeSignalBackend();
        backend.installSignals();
        backend.publish(ContextFrame.of(
                1L, 1L, "map", Collections.<artframework.context.CardView>emptyList(),
                ControlsView.empty(),
                new MapView(nodes, 1920, 1080, null, background, edges),
                new ViewportView(1920, 1080, 1920, 1080)));
        ArtFramework.publishFrame(backend.currentFrame());
    }

    private SurfaceDrawPlan fullMapPlan() {
        publishedMapFrame();
        ArtFramework.component(SurfaceIds.MAP).mount();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        return Sts1RenderPipeline.plan();
    }

    @Test
    public void fullDelegatedMapDrawsBandAtNativePointAndNotPostNative() {
        SurfaceDrawPlan plan = fullMapPlan();
        assertTrue("FULL + mounted + map scene + ready executor delegates the map",
                plan.shouldSuppressNative(SurfaceIds.MAP));
        assertFalse("top panel stays native-continuing (the HUD this fix retains)",
                plan.shouldSuppressNative(SurfaceIds.TOP_PANEL));
        assertTrue("delegated map must draw its band at the native map point",
                Sts1SurfaceRenderer.shouldDrawMapBandAtNativePoint(plan));
        assertFalse("the map band must NOT also be drawn in the post-native loop",
                Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.MAP));
        assertTrue("every other surface keeps its post-native draw",
                Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.TOP_PANEL));
        assertTrue(Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.EVENT));
        assertTrue(Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void suppressedTopPanelRoutesMapBandToPostNative() {
        fullMapPlan();
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue(plan.shouldSuppressNative(SurfaceIds.TOP_PANEL));
        assertFalse("no native HUD to sit under -> band is not drawn at the native point",
                Sts1SurfaceRenderer.shouldDrawMapBandAtNativePoint(plan));
        assertTrue("band must still be drawn exactly once, post-native",
                Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.MAP));
    }

    /**
     * FINDING 1 regression guard: when the top panel is ART-suppressed the map band routes to the
     * post-native path, which must still draw the FULL band — including the parchment background.
     * Pre-fix the post-native branch called {@code renderMap} alone (background omitted), so this
     * test failed with the background ids missing from the draw log.
     */
    @Test
    public void suppressedTopPanelPostNativeBandIncludesParchmentBackground() {
        fullMapPlan();
        ArtFramework.component(SurfaceIds.TOP_PANEL).mount();
        FullPresentMode.setTopPanelLevel(PresentLevel.FULL);
        SurfaceDrawPlan plan = Sts1RenderPipeline.plan();
        assertTrue("fixture must route the band post-native",
                Sts1SurfaceRenderer.postNativeDrawsSurface(plan, SurfaceIds.MAP));
        assertTrue("fixture must carry a parchment background",
                !MapDrawPath.backgroundItems().isEmpty());

        Sts1SurfaceRenderer.recordMapBandDrawsForTests();
        Sts1SurfaceRenderer.drawMapBandPostNative(batch());

        List<String> log = Sts1SurfaceRenderer.mapBandDrawLogForTests();
        List<String> backgroundIds = new ArrayList<String>();
        for (MapDrawPath.BackgroundDrawItem bg : MapDrawPath.backgroundItems()) {
            backgroundIds.add(bg.resourceId);
        }
        assertTrue("post-native band must include the parchment background ids (FINDING 1)",
                log.containsAll(backgroundIds));
        assertEquals("post-native band must start with the parchment background, in native order",
                backgroundIds, log.subList(0, backgroundIds.size()));

        // Full parity with the native-point route: identical content/order.
        assertEquals("post-native band content/order must equal the native-point band",
                expectedBandDraws(), log);
    }

    /**
     * The two sites share one once-per-frame guard: calling the native-point route then the
     * post-native route in the SAME projected frame paints exactly ONE complete band (background
     * included), and the second call is a no-op.
     */
    @Test
    public void nativeThenPostNativeSameFrameDrawsExactlyOneCompleteBand() {
        fullMapPlan();
        Sts1SurfaceRenderer.recordMapBandDrawsForTests();
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());
        Sts1SurfaceRenderer.drawMapBandPostNative(batch());

        assertEquals("exactly one band draw across the two sites in one frame", 1,
                Sts1SurfaceRenderer.mapBandDrawCountForTests());
        assertEquals("exactly one complete band (background included) across the two sites",
                expectedBandDraws(), Sts1SurfaceRenderer.mapBandDrawLogForTests());
    }

    @Test
    public void bandRestoresBatchColorAroundTheDraw() {
        fullMapPlan();
        com.badlogic.gdx.graphics.g2d.SpriteBatch sb = batch();
        com.badlogic.gdx.graphics.Color tint =
                new com.badlogic.gdx.graphics.Color(0.13f, 0.24f, 0.35f, 0.46f);
        sb.setColor(tint);
        float packedBefore = sb.getPackedColor();

        Sts1SurfaceRenderer.drawMapBandAtNativePoint(sb);

        assertEquals("batch color must be restored after the band (no tint leak into the native HUD)",
                Float.floatToIntBits(packedBefore), Float.floatToIntBits(sb.getPackedColor()));
    }

    @Test
    public void offOrObserveMapDrawsNothingAtNativePoint() {
        publishedMapFrame();
        ArtFramework.component(SurfaceIds.MAP).mount();
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());

        // OFF
        assertFalse(Sts1SurfaceRenderer.shouldDrawMapBandAtNativePoint(Sts1RenderPipeline.plan()));
        // OBSERVE
        FullPresentMode.setMapLevel(PresentLevel.OBSERVE);
        assertFalse("OBSERVE keeps the native map; no ART band",
                Sts1SurfaceRenderer.shouldDrawMapBandAtNativePoint(Sts1RenderPipeline.plan()));
    }

    @Test
    public void unmountedFullMapDrawsNothingAtNativePoint() {
        publishedMapFrame();
        FullPresentMode.setMapLevel(PresentLevel.FULL);
        CombatInputRouter.setExecutor(new RecordingIntentExecutor());
        // Map surface NOT mounted -> native fallback.
        assertFalse(Sts1SurfaceRenderer.shouldDrawMapBandAtNativePoint(Sts1RenderPipeline.plan()));
    }

    @Test
    public void oncePerFrameGuardClaimsExactlyOneDraw() {
        assertTrue(Sts1SurfaceRenderer.claimMapBandDrawForFrame(42L));
        assertFalse("second entry in the same projected frame must not redraw",
                Sts1SurfaceRenderer.claimMapBandDrawForFrame(42L));
        assertEquals(1, Sts1SurfaceRenderer.mapBandDrawCountForTests());
        assertTrue("a new projected frame may draw once more",
                Sts1SurfaceRenderer.claimMapBandDrawForFrame(43L));
        assertFalse(Sts1SurfaceRenderer.claimMapBandDrawForFrame(43L));
        assertEquals(2, Sts1SurfaceRenderer.mapBandDrawCountForTests());
    }

    @Test
    public void nativePointDrawInvokesBandOncePerFrame() {
        fullMapPlan();
        final int[] painted = {0};
        Sts1SurfaceRenderer.setMapBandDrawOverrideForTests(new Runnable() {
            @Override public void run() { painted[0]++; }
        });
        // Same projected frame (frameId 1) twice: TopPanel.render may be entered more than once.
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());
        assertEquals("band must paint exactly once per frame", 1, painted[0]);
        assertEquals(1, Sts1SurfaceRenderer.mapBandDrawCountForTests());
    }

    @Test
    public void failOpenBandExceptionDoesNotPropagateAndBlocksRetry() {
        fullMapPlan();
        Sts1SurfaceRenderer.setMapBandDrawOverrideForTests(new Runnable() {
            @Override public void run() { throw new IllegalStateException("band boom"); }
        });
        // Must not throw out of the native-point hook.
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());
        assertEquals("the failing attempt is still counted once", 1,
                Sts1SurfaceRenderer.mapBandDrawCountForTests());
        // The guard is claimed BEFORE the draw, so a same-frame retry is a no-op even after a failure.
        final int[] retries = {0};
        Sts1SurfaceRenderer.setMapBandDrawOverrideForTests(new Runnable() {
            @Override public void run() { retries[0]++; }
        });
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());
        assertEquals("a failed band must not be retried within the same frame", 0, retries[0]);
    }

    @Test
    public void bandContentAndOrderMatchThePreviousPostNativeDraw() {
        fullMapPlan();
        Sts1SurfaceRenderer.recordMapBandDrawsForTests();
        Sts1SurfaceRenderer.drawMapBandAtNativePoint(batch());

        List<String> expected = expectedBandDraws();
        assertFalse("fixture must carry background/edge/node draws", expected.isEmpty());
        assertEquals("band content/order must match the previous post-native draw",
                expected, Sts1SurfaceRenderer.mapBandDrawLogForTests());
    }

    /**
     * The expected band content/order (parchment background -> edges -> legend/node submissions),
     * built from the same {@code MapDrawPath} producers the renderer iterates.
     */
    private static List<String> expectedBandDraws() {
        List<String> expected = new ArrayList<String>();
        for (MapDrawPath.BackgroundDrawItem bg : MapDrawPath.backgroundItems()) {
            expected.add(bg.resourceId);
        }
        for (MapDrawPath.EdgeDrawItem edge : MapDrawPath.edgeItems()) {
            expected.add(edge.resourceId);
        }
        for (MapDrawPath.Submission s : MapDrawPath.mapSubmissionPlan()) {
            expected.add(s.resourceId);
        }
        return expected;
    }

    /** Tolerating {@link com.badlogic.gdx.graphics.g2d.SpriteBatch} double without GL. */
    private static com.badlogic.gdx.graphics.g2d.SpriteBatch batch() {
        try {
            java.lang.reflect.Field theUnsafe =
                    Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            com.badlogic.gdx.graphics.g2d.SpriteBatch sb =
                    (com.badlogic.gdx.graphics.g2d.SpriteBatch) unsafe.getClass()
                            .getMethod("allocateInstance", Class.class)
                            .invoke(unsafe, com.badlogic.gdx.graphics.g2d.SpriteBatch.class);
            java.lang.reflect.Field color =
                    com.badlogic.gdx.graphics.g2d.SpriteBatch.class.getDeclaredField("color");
            color.setAccessible(true);
            color.setFloat(sb, com.badlogic.gdx.graphics.Color.WHITE.toFloatBits());
            java.lang.reflect.Field tempColor =
                    com.badlogic.gdx.graphics.g2d.SpriteBatch.class.getDeclaredField("tempColor");
            tempColor.setAccessible(true);
            tempColor.set(sb, new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f));
            return sb;
        } catch (Exception failure) {
            throw new AssertionError("could not build tolerating SpriteBatch double", failure);
        }
    }
}

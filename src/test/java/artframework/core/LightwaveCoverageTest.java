package artframework.core;

import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.component.UiTypes;
import artframework.context.SurfaceIds;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.sts1.PresentLevel;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Acceptance coverage for Lightwave as the ArtFramework render verification profile. */
public class LightwaveCoverageTest {
    private static final List<String> VISUAL_TYPES = Arrays.asList(
            UiTypes.WINDOW,
            UiTypes.PANEL,
            UiTypes.GLASS,
            UiTypes.ROW,
            UiTypes.COL,
            UiTypes.STACK,
            UiTypes.FRAGMENT,
            UiTypes.SCROLL,
            UiTypes.CENTER,
            UiTypes.MARGIN,
            UiTypes.GRID,
            UiTypes.TABS,
            UiTypes.LABEL,
            UiTypes.BUTTON,
            UiTypes.SLIDER,
            UiTypes.TEXTFIELD,
            UiTypes.CHECKBOX,
            UiTypes.PROGRESS,
            UiTypes.HITAREA);

    private static final List<String> C2_SURFACES = Arrays.asList(
            SurfaceIds.COMBAT_HAND,
            SurfaceIds.COMBAT_CONTROLS,
            SurfaceIds.COMBAT_CARD_SLOTS,
            SurfaceIds.COMBAT_ENERGY,
            SurfaceIds.COMBAT_INTENTS,
            SurfaceIds.COMBAT_PROCEED,
            SurfaceIds.MAP,
            SurfaceIds.EVENT,
            SurfaceIds.SELECT_GRID,
            SurfaceIds.SELECT_HAND,
            SurfaceIds.REWARD_COMBAT,
            SurfaceIds.REWARD_CARD,
            SurfaceIds.REWARD_BOSS_RELIC,
            SurfaceIds.REST,
            SurfaceIds.TREASURE,
            SurfaceIds.SHOP,
            SurfaceIds.TOP_PANEL);

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void lightwavePackCoversEveryVisualC1Type() {
        PresentPacks.installBuiltinLightwavePack();
        PresentPack pack = PresentPacks.get(PresentProfiles.LIGHTWAVE);
        assertNotNull(pack);
        for (String type : VISUAL_TYPES) {
            assertFalse("missing Lightwave default for " + type,
                    pack.effectDefaultsFor(type).isEmpty());
        }
    }

    @Test
    public void lightwavePackCoversEveryNonSpineC2Surface() {
        PresentPacks.installBuiltinLightwavePack();
        PresentPack pack = PresentPacks.get(PresentProfiles.LIGHTWAVE);
        for (String surface : C2_SURFACES) {
            assertTrue("missing Lightwave effect for " + surface,
                    pack.surfaceEffects.containsKey(surface));
            assertFalse("empty Lightwave effect for " + surface,
                    pack.surfaceEffects.get(surface).isEmpty());
        }
        assertEquals(C2_SURFACES.size(), pack.surfaceEffects.size());
    }

    @Test
    public void componentGalleryInflatesAndReceivesProfileDefaults() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        ArtFramework.register(new WindowDef(
                "lightwave_components_demo",
                WindowClass.SYNTHETIC,
                "layouts/lightwave_components_demo.json"));
        ArtFramework.open("lightwave_components_demo");

        artframework.presentation.PresentationContext context =
                artframework.presentation.PresentationRuntime.context("lightwave_components_demo");
        assertNotNull(context);
        assertEquals(PresentProfiles.LIGHTWAVE,
                PresentResolve.forEntity(context,
                        artframework.presentation.PresentationRuntime.root(context)).profileId);
        RenderHost host = RenderHosts.get();
        assertNotNull(host.getTarget("c1:lightwave_components_demo:visuals"));
        assertFalse(host.effectsOf("c1:lightwave_components_demo:visuals").isEmpty());
        assertNotNull(host.getTarget("c1:lightwave_components_demo:primary"));
        assertEquals("lightwave",
                host.effectsOf("c1:lightwave_components_demo:primary").get(0).effectId);
        assertNotNull(host.getTarget("c1:lightwave_components_demo:intensity"));
        assertNotNull(host.getTarget("c1:lightwave_components_demo:progress"));
        assertNotNull(host.getTarget("c1:lightwave_components_demo:hitarea"));
        assertNotNull(host.getTarget("c1:lightwave_components_demo:grid"));
        assertNotNull(host.getTarget("c1:lightwave_components_demo:tabs"));
    }

    @Test
    public void switchingAwayCleansLightwaveC2AmbientState() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        assertFalse(RenderHosts.get().isFullFrameEnabled());
        assertFalse(PresentPackApply.probeSummary().get("boundC2Effects").toString().isEmpty());

        ArtFramework.setProjectPresent(PresentProfiles.STS);

        assertFalse(RenderHosts.get().isFullFrameEnabled());
        assertTrue(PresentPackApply.probeSummary().get("boundC2Effects").toString().equals("[]"));
        assertTrue(PresentPackApply.probeSummary().get("boundSurfaces").toString().equals("[]"));
    }

    @Test
    public void lightwaveDoesNotEnableFullPresent() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        assertEquals(PresentLevel.OFF, artframework.sts1.FullPresentMode.combatHandLevel());
        assertEquals(PresentLevel.OFF, artframework.sts1.FullPresentMode.mapLevel());
    }
}

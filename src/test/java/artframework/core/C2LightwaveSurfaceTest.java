package artframework.core;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.component.Rect;
import artframework.render.LightwaveEffect;
import artframework.render.RenderHost;
import artframework.render.RenderHosts;
import artframework.render.RenderTargetKind;
import artframework.render.RenderStateEcs;
import artframework.presentation.EffectAttachment;
import artframework.presentation.PresentationVisuals;
import org.junit.After;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class C2LightwaveSurfaceTest {
    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void lightwavePackBindsDeclaredC2SurfaceEffects() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        PresentPack pack = PresentPacks.active();
        assertFalse(pack.surfaceEffects.isEmpty());
        assertTrue(
                PresentPackApply.probeSummary()
                        .get("boundC2Effects")
                        .toString()
                        .contains(SurfaceIds.COMBAT_HAND));
        assertEquals(
                LightwaveEffect.ID,
                RenderHosts.get()
                        .effectsOf(RenderHost.c2SurfaceTargetId(SurfaceIds.COMBAT_HAND))
                        .get(0)
                        .effectId);
    }

    @Test
    public void c2SurfaceTargetUsesStableBoundsAndEffect() {
        RenderHost host = RenderHosts.get();
        RenderStateEcs.surface(SurfaceIds.EVENT, 10f, 20f, 300f, 200f, true);
        RenderStateEcs.surfaceEffects(SurfaceIds.EVENT, java.util.Collections.singletonList(
                new EffectAttachment(LightwaveEffect.ID, "ambient",
                        java.util.Collections.<String, Object>singletonMap("intensity", Float.valueOf(0.3f)))));
        host.rebuildFromEcsPlan();

        artframework.render.RenderTarget target =
                host.getTarget(RenderHost.c2SurfaceTargetId(SurfaceIds.EVENT));
        assertEquals(RenderTargetKind.C2_SURFACE, target.kind);
        assertEquals(300f, target.width(), 0.01f);
        assertEquals(200f, target.height(), 0.01f);
        assertEquals(1, host.effectsOf(target.id).size());
        assertEquals(LightwaveEffect.ID, host.effectsOf(target.id).get(0).effectId);
    }

    @Test
    public void c2ItemTargetCopiesSurfaceEffectWithExactBounds() {
        RenderHost host = RenderHosts.get();
        RenderStateEcs.surface(SurfaceIds.COMBAT_HAND, 0f, 0f, 1000f, 400f, true);
        RenderStateEcs.surfaceEffects(SurfaceIds.COMBAT_HAND, java.util.Collections.singletonList(
                new EffectAttachment(LightwaveEffect.ID, "ambient",
                        java.util.Collections.<String, Object>singletonMap("intensity", Float.valueOf(0.3f)))));
        PresentationVisuals.syncC2Item(SurfaceIds.COMBAT_HAND, "card-1",
                new Rect(200f, 30f, 250f, 350f), 1f, "card", "", "", true);
        host.rebuildFromEcsPlan();
        artframework.render.RenderTarget item =
                host.getTarget(RenderHost.c2ItemTargetId(SurfaceIds.COMBAT_HAND, "card-1"));

        assertEquals(RenderTargetKind.C2_SURFACE, item.kind);
        assertEquals(200f, item.x(), 0.01f);
        assertEquals(350f, item.height(), 0.01f);
        assertEquals(1, host.effectsOf(item.id).size());
        assertEquals(LightwaveEffect.ID, host.effectsOf(item.id).get(0).effectId);
        PresentationVisuals.removeC2Items(SurfaceIds.COMBAT_HAND);
        host.rebuildFromEcsPlan();
        assertTrue(host.getTarget(item.id) == null);
    }

    @Test
    public void c2ItemTargetTracksHandDrawItemBounds() {
        RenderHost host = RenderHosts.get();
        RenderStateEcs.surface(SurfaceIds.COMBAT_HAND, 0f, 0f, 1000f, 400f, true);
        RenderStateEcs.surfaceEffects(SurfaceIds.COMBAT_HAND, java.util.Collections.singletonList(
                new EffectAttachment(LightwaveEffect.ID, "ambient",
                        java.util.Collections.<String, Object>emptyMap())));
        artframework.sts1.render.HandDrawPath.DrawItem item =
                new artframework.sts1.render.HandDrawPath.DrawItem(
                        "card-1", "Strike_R", 300f, 250f, 0f, 0.8f, true, "", "", true, "", "");
        Rect bounds = item.bounds();
        PresentationVisuals.syncC2Item(
                SurfaceIds.COMBAT_HAND,
                item.instanceId,
                bounds, 1f, "card", "", "", true);
        host.rebuildFromEcsPlan();

        artframework.render.RenderTarget target =
                host.getTarget(RenderHost.c2ItemTargetId(SurfaceIds.COMBAT_HAND, item.instanceId));
        assertEquals(bounds.width, target.width(), 0.01f);
        assertEquals(bounds.height, target.height(), 0.01f);
        assertEquals(LightwaveEffect.ID, host.effectsOf(target.id).get(0).effectId);
    }

    @Test
    public void deactivatingLightwaveLeavesNoManagedC2Targets() {
        PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        RenderHost host = RenderHosts.get();
        assertTrue(host.getTarget(RenderHost.c2SurfaceTargetId(SurfaceIds.COMBAT_HAND)) != null);
        ArtFramework.setProjectPresent(PresentProfiles.STS);
        assertTrue(host.getTarget(RenderHost.c2SurfaceTargetId(SurfaceIds.COMBAT_HAND)) == null);
        assertFalse(PresentPackApply.probeSummary().get("boundC2Effects").toString().contains(SurfaceIds.COMBAT_HAND));
    }

    @Test
    public void chromeExposesLightwavePanelTokens() {
        PresentChromeStyle chrome = PresentProfiles.get(PresentProfiles.LIGHTWAVE).chrome;
        Map<String, Object> probe = chrome.probeSummary();
        assertTrue(chrome.panelAlpha > 0f && chrome.panelAlpha < 1f);
        assertTrue(chrome.borderWidth >= 1f);
        assertTrue(probe.containsKey("panel"));
    }
}

package artframework.c1.host;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.api.WindowClass;
import artframework.api.WindowDef;
import artframework.core.PresentProfiles;
import artframework.core.ProjectPresent;
import artframework.render.RenderHost;
import artframework.render.RenderTarget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Without Stage, effect targets still bind from LayoutEngine (origin space).
 * Documents the host must re-sync stage coords; pure path still has panel target + lightwave.
 */
public class EffectTargetSyncTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void lightwaveDemoBindsPanelTargetWithLayoutBounds() {
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");
        RenderHost host = ArtFramework.render();
        RenderTarget panel = host.getTarget("c1:lightwave_demo:panel");
        assertNotNull(panel);
        assertTrue(panel.width() > 0f);
        assertTrue(panel.height() > 0f);
        // Pure LayoutEngine coords are window-local (pad offset), not screen-centered.
        // StageHost.syncEffectTargetBounds rewrites these to stage space after act() on device.
        assertTrue(
                "layout-local x should be small vs screen center without Stage",
                panel.x() < 200f);
        assertTrue(panel.y() < 200f);
        assertEquals(1, host.effectsOf("c1:lightwave_demo:panel").size());
    }

    @Test
    public void packDefaultsBindNamedAndPathTargets() {
        artframework.core.PresentPacks.installBuiltinLightwavePack();
        ArtFramework.setProjectPresent(PresentProfiles.LIGHTWAVE);
        ArtFramework.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        RenderHost host = ArtFramework.render();
        assertNotNull(host.getTarget("c1:demo:close"));
        assertEquals(1, host.effectsOf("c1:demo:close").size());
        assertEquals("lightwave", host.effectsOf("c1:demo:close").get(0).effectId);
        assertNotNull(host.getTarget("c1:demo:hello"));
        assertEquals(1, host.effectsOf("c1:demo:hello").size());
        assertEquals("lightwave", host.effectsOf("c1:demo:hello").get(0).effectId);
    }

    @Test
    public void effectTargetActorsRegistryKeys() {
        // Without Stage, registry only fills when ComponentActors.toActor runs (device).
        // Pure open still creates RenderHost targets from layout keys.
        ArtFramework.register(
                new WindowDef("demo", WindowClass.SYNTHETIC, "layouts/demo.json"));
        ArtFramework.open("demo");
        assertNotNull(ArtFramework.render().getTarget("c1:demo:hello"));
        assertNotNull(ArtFramework.render().getTarget("c1:demo:close"));
    }

    @Test
    public void treeThemeIsLightwaveWhenDeclared() {
        ArtFramework.register(
                new WindowDef("lightwave_demo", WindowClass.SYNTHETIC, "layouts/lightwave_demo.json"));
        ArtFramework.open("lightwave_demo");
        assertNotNull(ArtFramework.tree("lightwave_demo"));
        assertEquals("lightwave", ArtFramework.tree("lightwave_demo").theme().name());
        // Process default may still be sts unless profile set
        assertEquals(PresentProfiles.STS, ProjectPresent.id());
    }
}

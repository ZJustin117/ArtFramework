package artframework.presentation;

import artframework.api.ArtFramework;
import artframework.component.Rect;
import artframework.context.PresentSurfaces;
import artframework.context.SurfaceIds;
import artframework.core.PresentPacks;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PresentationVisualsTest {
    @After public void reset() { ArtFramework.resetForTests(); }

    @Test
    public void c2VisualItemInheritsLightwaveChromeAndEffectBelowItsSurface() {
        artframework.core.PresentPacks.installBuiltinLightwavePack();
        ArtFramework.activatePresentPack("lightwave");
        PresentSurfaces.get(SurfaceIds.COMBAT_HAND).mount();
        EntityId entity = PresentationVisuals.syncC2Item(
                SurfaceIds.COMBAT_HAND, "card-1", new Rect(10f, 20f, 80f, 120f), 3f,
                "card", "card.art", "Strike", true);

        PresentationContext context = PresentationRegistry.context("c2-surfaces");
        NodeHierarchyComponent hierarchy =
                context.world().get(entity, NodeHierarchyComponent.class);
        assertNotNull(hierarchy.parent);
        assertEquals(new PresentationKey("sts1.surface", SurfaceIds.COMBAT_HAND),
                context.key(hierarchy.parent));
        ChromeComponent chrome = context.world().get(entity, ChromeComponent.class);
        assertTrue(chrome.panelAlpha < 1f);
        assertTrue(chrome.borderWidth > 0f);
        EffectsComponent effects = context.world().get(entity, EffectsComponent.class);
        assertNotNull(effects.get("lightwave", "ambient"));
        assertEquals(80f, context.world().get(entity, BoundsComponent.class).rect.width, 0.001f);
    }

    @Test
    public void registryExposesC2FramesAndStableVisualEntities() {
        EntityId item = PresentationVisuals.syncC2Item(
                "combat.hand", "card:1", new Rect(10f, 20f, 80f, 120f), 2f,
                "card", "sts1.card.strike", "Strike", true);
        boolean found = false;
        for (PresentationFrame frame : PresentationRegistry.frames()) {
            for (PresentationDrawItem draw : frame.items) {
                if (item.equals(draw.entity)) {
                    found = true;
                    assertEquals(new Rect(10f, 20f, 80f, 120f), draw.bounds);
                }
            }
        }
        assertTrue(found);
    }
}

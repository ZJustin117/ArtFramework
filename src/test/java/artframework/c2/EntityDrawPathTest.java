package artframework.c2;

import artframework.api.ArtFramework;
import artframework.context.SurfaceIds;
import artframework.sts1.FullPresentMode;
import artframework.sts1.PresentLevel;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntityDrawPathTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void snapshotParseFromMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("artResourceId", "card.art.Strike_R");
        raw.put("hp", Integer.valueOf(30));
        raw.put("maxHp", Integer.valueOf(80));
        raw.put("label", "Ironclad");
        EntitySnapshot snap = EntitySnapshot.from(raw);
        assertEquals("card.art.Strike_R", snap.artResourceId);
        assertEquals(30, snap.hp);
        assertEquals(80, snap.maxHp);
        assertEquals("Ironclad", snap.label);
    }

    @Test
    public void drawPathBuildsLaidOutSlots() {
        EntityPresent p = ArtFramework.entities();
        p.attach("r1", "relic", "burning_blood");
        p.sync("r1", EntitySnapshot.ofIcon("ui.icon.relic"));
        p.layout("r1", 100f, 200f, 1f);
        List<EntityDrawPath.DrawItem> items = EntityDrawPath.buildFromPresent();
        assertEquals(1, items.size());
        assertEquals("RELIC", items.get(0).kind);
        assertEquals(100f, items.get(0).x, 0.01f);
        assertEquals(64f, items.get(0).w, 0.01f);
    }

    @Test
    public void cardSizeUsesCardBounds() {
        float[] size = EntityDrawPath.defaultSize(EntityKind.CARD, 1f);
        assertEquals(250f, size[0], 0.01f);
        assertEquals(350f, size[1], 0.01f);
    }

    @Test
    public void cardOverlayOnlyWhenHandFullMounted() {
        assertFalse(EntityDrawPath.cardOverlayOnly());
        FullPresentMode.setCombatHandLevel(PresentLevel.FULL);
        ArtFramework.component(SurfaceIds.COMBAT_HAND).mount();
        assertTrue(EntityDrawPath.cardOverlayOnly());

        EntityPresent p = ArtFramework.entities();
        p.attach("c1", "card", "Strike");
        p.sync("c1", EntitySnapshot.ofArt("card.art.Strike_R"));
        p.layout("c1", 50f, 50f, 1f);
        List<EntityDrawPath.DrawItem> items = EntityDrawPath.buildFromPresent();
        assertEquals(1, items.size());
        assertTrue(items.get(0).overlayOnly);
    }

    @Test
    public void probeEntitiesIncludesSlots() {
        EntityPresent p = ArtFramework.entities();
        p.attach("p1", "player", "ironclad");
        p.sync("p1", EntitySnapshot.playerChrome("Ironclad", 70, 80, 0));
        p.layout("p1", 10f, 20f, 1f);
        @SuppressWarnings("unchecked")
        Map<String, Object> entities = (Map<String, Object>) ArtFramework.probe().asMap().get("entities");
        assertEquals(Integer.valueOf(1), entities.get("slotCount"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) entities.get("slots");
        assertEquals(1, slots.size());
        assertEquals("PLAYER", slots.get(0).get("kind"));
    }
}

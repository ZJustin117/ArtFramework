package artframework.c2;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.ecs.EntityId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EntityPresentTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void attachSyncLayoutDetach() {
        EntityPresent p = ArtFramework.entities();
        p.attach("p1", "player", "ironclad");
        assertTrue(p.isAttached("p1"));
        assertEquals(1, p.size());

        EntitySlot slot = p.get("p1");
        assertNotNull(slot);
        assertEquals(EntityKind.PLAYER, slot.kind);
        assertEquals("ironclad", slot.refId);
        assertFalse(slot.isLaidOut());

        Object dto = new Object();
        p.sync("p1", dto);
        assertSame(dto, p.get("p1").snapshot());

        p.layout("p1", 10f, 20f, 0.5f);
        slot = p.get("p1");
        assertTrue(slot.isLaidOut());
        assertEquals(10f, slot.x(), 0.001f);
        assertEquals(20f, slot.y(), 0.001f);
        assertEquals(0.5f, slot.scale(), 0.001f);

        p.detach("p1");
        assertFalse(p.isAttached("p1"));
        assertNull(p.get("p1"));
        assertEquals(0, p.size());
    }

    @Test
    public void reattachReplacesSlotAndNotifies() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        final List<String> detached = new ArrayList<String>();
        final List<String> attached = new ArrayList<String>();
        p.addListener(new RecordingListener(attached, detached));

        p.attach("c1", "card", "Strike");
        p.sync("c1", "v1");
        p.attach("c1", "CARD", "Defend");

        assertEquals(2, attached.size());
        assertEquals(1, detached.size());
        assertEquals("c1", detached.get(0));
        EntitySlot slot = p.get("c1");
        assertEquals(EntityKind.CARD, slot.kind);
        assertEquals("Defend", slot.refId);
        assertNull(slot.snapshot());
    }

    @Test
    public void kindParseCaseInsensitive() {
        EntityPresent p = ArtFramework.entities();
        p.attach("r1", "Relic", "burning_blood");
        p.attach("m1", "monster", "cultist");
        assertEquals(EntityKind.RELIC, p.get("r1").kind);
        assertEquals(EntityKind.MONSTER, p.get("m1").kind);
        assertEquals(2, p.listSlotIds().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownKindThrows() {
        ArtFramework.entities().attach("x", "weapon", "x");
    }

    @Test(expected = IllegalArgumentException.class)
    public void syncRequiresAttach() {
        ArtFramework.entities().sync("missing", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void layoutRequiresAttach() {
        ArtFramework.entities().layout("missing", 0f, 0f, 1f);
    }

    @Test
    public void clearDetachesAll() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        final List<String> detached = new ArrayList<String>();
        p.addListener(new RecordingListener(new ArrayList<String>(), detached));
        p.attach("a", "card", "a");
        p.attach("b", "card", "b");
        p.clear();
        assertEquals(0, p.size());
        assertEquals(2, detached.size());
    }

    @Test
    public void resetForTestsClearsEntities() {
        ArtFramework.entities().attach("z", "player", "z");
        ArtFramework.resetForTests();
        assertEquals(0, ArtFramework.entities().size());
    }

    @Test
    public void facadeSameInstanceAsRuntime() {
        assertSame(NativeTemplateRuntime.entities(), ArtFramework.entities());
    }

    @Test
    public void slotStateIsMirroredByPresentationComponents() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        p.attach("c1", "card", "Strike");
        EntityId entity = p.entityId("c1");
        assertNotNull(entity);
        assertEquals(1, p.world().query(EntitySlotIdentityComponent.class).size());
        assertEquals(EntityKind.CARD, p.world().get(entity, EntitySlotIdentityComponent.class).kind);
        assertNull(p.world().get(entity, EntitySlotSnapshotComponent.class).snapshot);
        assertFalse(p.world().get(entity, EntitySlotTransformComponent.class).laidOut);

        Object snapshot = new Object();
        p.sync("c1", snapshot);
        p.layout("c1", 10f, 20f, 0.5f);
        assertSame(snapshot, p.world().get(entity, EntitySlotSnapshotComponent.class).snapshot);
        EntitySlotTransformComponent transform = p.world().get(entity, EntitySlotTransformComponent.class);
        assertTrue(transform.laidOut);
        assertEquals(10f, transform.x, 0.001f);
        assertEquals(0.5f, transform.scale, 0.001f);

        p.detach("c1");
        assertFalse(p.world().contains(entity));
    }

    private static final class RecordingListener implements EntityPresentListener {
        private final List<String> attached;
        private final List<String> detached;

        RecordingListener(List<String> attached, List<String> detached) {
            this.attached = attached;
            this.detached = detached;
        }

        @Override
        public void onAttached(EntitySlot slot) {
            attached.add(slot.slotId);
        }

        @Override
        public void onSynced(EntitySlot slot) {}

        @Override
        public void onLaidOut(EntitySlot slot) {}

        @Override
        public void onDetached(String slotId) {
            detached.add(slotId);
        }
    }
}

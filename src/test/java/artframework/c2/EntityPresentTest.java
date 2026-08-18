package artframework.c2;

import org.junit.After;
import org.junit.Test;
import artframework.api.ArtFramework;
import artframework.ecs.EntityId;
import artframework.render.RenderProjectionQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, Object> dto = new HashMap<String, Object>();
        dto.put("label", "Ironclad");
        p.sync("p1", dto);
        assertEquals("Ironclad", p.get("p1").snapshot().label);

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
    public void presentBuildsACompleteSlot() {
        EntityPresent p = ArtFramework.entities();
        EntitySnapshot snapshot = EntitySnapshot.playerChrome("Ironclad", 70, 80, 0);

        p.present("p1", "player", "ironclad", snapshot, 10f, 20f, 0.5f);

        EntitySlot slot = p.get("p1");
        assertNotNull(slot);
        assertEquals("Ironclad", slot.snapshot().label);
        assertTrue(slot.isLaidOut());
        assertEquals(10f, slot.x(), 0.001f);
        assertEquals(20f, slot.y(), 0.001f);
        assertEquals(0.5f, slot.scale(), 0.001f);
    }

    @Test
    public void presentNotifiesCompleteLifecycleAndProjectsOnce() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        final List<String> events = new ArrayList<String>();
        p.addListener(new EntityPresentListener() {
            @Override public void onAttached(EntitySlot slot) {
                events.add("attached:" + slot.isLaidOut());
            }
            @Override public void onSynced(EntitySlot slot) {
                events.add("synced:" + (slot.snapshot() != null));
            }
            @Override public void onLaidOut(EntitySlot slot) {
                events.add("laidout:" + slot.isLaidOut());
            }
            @Override public void onDetached(String slotId) {
                events.add("detached:" + slotId);
            }
        });
        int before = RenderProjectionQueue.rebuildCountForTests();

        p.present("p1", "player", "ironclad",
                EntitySnapshot.playerChrome("Ironclad", 70, 80, 0), 10f, 20f, 1f);

        assertEquals(java.util.Arrays.asList("attached:false", "synced:true", "laidout:true"), events);
        assertEquals(before + 1, RenderProjectionQueue.rebuildCountForTests());
    }

    @Test
    public void replacementPresentNotifiesDetachThenCompleteLifecycle() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        final List<String> events = new ArrayList<String>();
        p.attach("p1", "player", "ironclad");
        p.addListener(new EntityPresentListener() {
            @Override public void onAttached(EntitySlot slot) {
                events.add("attached:" + slot.refId + ":" + (slot.snapshot() == null)
                        + ":" + slot.isLaidOut());
            }
            @Override public void onSynced(EntitySlot slot) {
                events.add("synced:" + slot.refId + ":" + slot.snapshot().label);
            }
            @Override public void onLaidOut(EntitySlot slot) {
                events.add("laidout:" + slot.refId + ":" + slot.x() + ":" + slot.y()
                        + ":" + slot.scale() + ":" + slot.isLaidOut());
            }
            @Override public void onDetached(String slotId) { events.add("detached:" + slotId); }
        });

        p.present("p1", "player", "silent",
                EntitySnapshot.playerChrome("Silent", 70, 80, 0), 10f, 20f, 1f);

        assertEquals(java.util.Arrays.asList(
                "detached:p1",
                "attached:silent:true:false",
                "synced:silent:Silent",
                "laidout:silent:10.0:20.0:1.0:true"), events);
    }

    @Test
    public void presentDefersItsSingleProjectionUntilOuterBatchFlushes() {
        EntityPresent p = ArtFramework.entities();
        int before = RenderProjectionQueue.rebuildCountForTests();

        RenderProjectionQueue.begin();
        p.present("p1", "player", "ironclad",
                EntitySnapshot.playerChrome("Ironclad", 70, 80, 0), 10f, 20f, 1f);
        assertEquals(before, RenderProjectionQueue.rebuildCountForTests());

        RenderProjectionQueue.flush();
        assertEquals(before + 1, RenderProjectionQueue.rebuildCountForTests());
    }

    @Test
    public void reattachReplacesSlotAndNotifies() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        final List<String> detached = new ArrayList<String>();
        final List<String> attached = new ArrayList<String>();
        p.addListener(new RecordingListener(attached, detached));

        p.attach("c1", "card", "Strike");
        p.sync("c1", EntitySnapshot.ofArt("card.art.Strike_R"));
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

        EntitySnapshot snapshot = EntitySnapshot.playerChrome("Ironclad", 70, 80, 0);
        p.sync("c1", snapshot);
        p.layout("c1", 10f, 20f, 0.5f);
        assertEquals("Ironclad", p.world().get(entity, EntitySlotSnapshotComponent.class).snapshot.label);
        EntitySlotTransformComponent transform = p.world().get(entity, EntitySlotTransformComponent.class);
        assertTrue(transform.laidOut);
        assertEquals(10f, transform.x, 0.001f);
        assertEquals(0.5f, transform.scale, 0.001f);

        p.detach("c1");
        assertFalse(p.world().contains(entity));
    }

    @Test
    public void syncNormalizesMutableMapIntoAnImmutableSnapshot() {
        EntityPresent p = ArtFramework.entities();
        p.attach("p1", "player", "ironclad");
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("tone", "red");
        List<Object> badges = new ArrayList<Object>();
        badges.add(nested);
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("label", "Ironclad");
        raw.put("badges", badges);

        p.sync("p1", raw);
        nested.put("tone", "green");
        badges.add("mutated");
        raw.put("label", "Silent");

        EntitySnapshot snapshot = p.get("p1").snapshot();
        assertEquals("Ironclad", snapshot.label);
        assertEquals("red", ((Map<?, ?>) ((List<?>) snapshot.extras.get("badges")).get(0)).get("tone"));
        try {
            snapshot.extras.put("new", "value");
            throw new AssertionError("extras must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
        assertEquals(snapshot, ArtFramework.entityPresent().world()
                .get(ArtFramework.entityPresent().entityId("p1"), EntitySlotSnapshotComponent.class).snapshot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void syncRejectsUnsupportedSnapshotValues() {
        EntityPresent p = ArtFramework.entities();
        p.attach("p1", "player", "ironclad");
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("extension", new Object());
        p.sync("p1", raw);
    }

    @Test
    public void syncCopiesTypedSnapshotExtras() {
        EntityPresent p = ArtFramework.entities();
        p.attach("p1", "player", "ironclad");
        Map<String, Object> nested = new HashMap<String, Object>();
        nested.put("tone", "red");
        EntitySnapshot input = new EntitySnapshot(
                0f, 0f, 1f, true, "", "", "Ironclad", 70, 80, 0, "", nested);

        p.sync("p1", input);
        nested.put("tone", "green");

        EntitySnapshot stored = p.get("p1").snapshot();
        assertEquals("red", stored.extras.get("tone"));
        assertFalse(input == stored);
    }

    @Test
    public void facadeRebindsAfterPresentationContextRecreation() {
        DefaultEntityPresent p = ArtFramework.entityPresent();
        artframework.presentation.PresentationRegistry.close("c2-entity-present");

        p.attach("p1", "player", "ironclad");
        p.sync("p1", EntitySnapshot.playerChrome("Ironclad", 70, 80, 0));
        p.layout("p1", 10f, 20f, 1f);

        assertEquals(1, p.size());
        assertEquals(1, EntityPresentViews.list().size());
        assertEquals(1, EntityDrawPath.buildFromPresent().size());
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

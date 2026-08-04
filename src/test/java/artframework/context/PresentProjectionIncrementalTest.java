package artframework.context;

import artframework.api.ArtFramework;
import artframework.ecs.EntityId;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class PresentProjectionIncrementalTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
    }

    @Test
    public void unchangedCardKeepsEntityAndComponentInstances() {
        CardView card = CardView.builder(new CardRef("c1", "Strike_R"))
                .zone(CardZone.HAND)
                .slot(0)
                .art("card-art")
                .frame("card-frame")
                .build();
        ArtFramework.publishFrame(ContextFrame.of(1L, 1L, "combat",
                Collections.singletonList(card), ControlsView.empty(), MapView.empty(), null));

        EntityId entity = ArtFramework.projection().entityId("c1");
        CardIdentityComponent identity = ArtFramework.projection().world()
                .get(entity, CardIdentityComponent.class);
        CardPlacementComponent placement = ArtFramework.projection().world()
                .get(entity, CardPlacementComponent.class);
        CardInteractionComponent interaction = ArtFramework.projection().world()
                .get(entity, CardInteractionComponent.class);
        CardAssetsComponent assets = ArtFramework.projection().world()
                .get(entity, CardAssetsComponent.class);

        FrameDiff diff = ArtFramework.publishFrame(ContextFrame.of(2L, 1L, "combat",
                Collections.singletonList(card), ControlsView.empty(), MapView.empty(), null));

        assertEquals(0, diff.updated.size());
        assertSame(entity, ArtFramework.projection().entityId("c1"));
        assertSame(identity, ArtFramework.projection().world().get(entity, CardIdentityComponent.class));
        assertSame(placement, ArtFramework.projection().world().get(entity, CardPlacementComponent.class));
        assertSame(interaction, ArtFramework.projection().world().get(entity, CardInteractionComponent.class));
        assertSame(assets, ArtFramework.projection().world().get(entity, CardAssetsComponent.class));
    }

    @Test
    public void placementChangeOnlyReplacesPlacementComponent() {
        CardView first = CardView.builder(new CardRef("c1", "Strike_R"))
                .zone(CardZone.HAND).slot(0).art("card-art").frame("card-frame").build();
        ArtFramework.publishFrame(ContextFrame.of(1L, 1L, "combat",
                Collections.singletonList(first), ControlsView.empty(), MapView.empty(), null));
        EntityId entity = ArtFramework.projection().entityId("c1");
        CardIdentityComponent identity = ArtFramework.projection().world()
                .get(entity, CardIdentityComponent.class);
        CardInteractionComponent interaction = ArtFramework.projection().world()
                .get(entity, CardInteractionComponent.class);
        CardAssetsComponent assets = ArtFramework.projection().world()
                .get(entity, CardAssetsComponent.class);

        CardView moved = CardView.builder(new CardRef("c1", "Strike_R"))
                .zone(CardZone.HAND).slot(1).art("card-art").frame("card-frame").build();
        FrameDiff diff = ArtFramework.publishFrame(ContextFrame.of(2L, 1L, "combat",
                Collections.singletonList(moved), ControlsView.empty(), MapView.empty(), null));

        assertEquals(Collections.singletonList("c1"), diff.updated);
        assertSame(identity, ArtFramework.projection().world().get(entity, CardIdentityComponent.class));
        assertSame(interaction, ArtFramework.projection().world().get(entity, CardInteractionComponent.class));
        assertSame(assets, ArtFramework.projection().world().get(entity, CardAssetsComponent.class));
        assertEquals(1, ArtFramework.projection().world()
                .get(entity, CardPlacementComponent.class).slotIndex);
    }
}

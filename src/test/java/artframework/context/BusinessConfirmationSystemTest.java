package artframework.context;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BusinessConfirmationSystemTest {
    @Test public void cardChangeConfirmsPlay() {
        CardView one = CardView.builder(new CardRef("a", "strike"))
                .zone(CardZone.HAND).slot(0).build();
        CardView two = CardView.builder(new CardRef("b", "defend"))
                .zone(CardZone.HAND).slot(0).build();
        ContextFrame before = ContextFrame.of(1L, 2L, "combat", Arrays.asList(one),
                ControlsView.empty(), MapView.empty(), null);
        ContextFrame after = ContextFrame.of(2L, 2L, "combat", Arrays.asList(two),
                ControlsView.empty(), MapView.empty(), null);
        BusinessConfirmationComponent request = new BusinessConfirmationComponent("play_card",
                BusinessConfirmationComponent.Domain.CARD, BusinessConfirmationComponent.State.PENDING,
                1L, 2L, -1L, -1L, "");
        assertEquals(BusinessConfirmationComponent.State.CONFIRMED,
                BusinessConfirmationSystem.evaluate(request, before, after).state);
    }

    @Test public void unchangedSnapshotRemainsPending() {
        ContextFrame frame = ContextFrame.of(1L, 2L, "event", null,
                ControlsView.empty(), MapView.empty(), EventView.empty(), SelectView.empty(),
                ViewportView.unavailable());
        BusinessConfirmationComponent request = new BusinessConfirmationComponent("choose_event_option",
                BusinessConfirmationComponent.Domain.EVENT, BusinessConfirmationComponent.State.PENDING,
                1L, 2L, -1L, -1L, "");
        assertEquals(BusinessConfirmationComponent.State.PENDING,
                BusinessConfirmationSystem.evaluate(request, frame, frame).state);
    }
}

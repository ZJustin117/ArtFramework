package artframework.sts1.backend;

import artframework.assets.ResourceIds;
import artframework.context.RoomShellView;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * U03: the room-shell title must be a genuine native passthrough, never a synthesized label.
 *
 * <p>VERIFIED native evidence (decompiled STS1 {@code com/megacrit/cardcrawl/}): a plain room has
 * NO native room-shell title. {@code rooms/AbstractRoom} declares no {@code title} (and no
 * {@code roomID}) field, its {@code render(SpriteBatch)} draws only the event panel/player/
 * monsters/combat UI/potions/relics/{@code renderTips} (the sole
 * {@code FontHelper.renderFontCenteredHeight} is {@code Settings.isInfo} debug), and
 * {@code rooms/EventRoom}, {@code NeowRoom} and {@code VictoryRoom} delegate to
 * {@code this.event.render(sb)} — the delegated event surface owns the event's own
 * {@code AbstractImageEvent.title}. {@code MonsterRoom}, {@code MonsterRoomElite/Boss},
 * {@code RestRoom}, {@code ShopRoom}, {@code TreasureRoom}, {@code TreasureRoomBoss},
 * {@code EmptyRoom} and {@code TrueVictoryRoom} declare no {@code title}, and a repo-wide search
 * for a {@code title} field under {@code rooms/**} returns ZERO matches. So for a plain
 * (generic/combat) room the room-shell title MUST be empty and the draw path must draw nothing.</p>
 *
 * <p>Cases 1/2 pin that native truth (they FAIL on the current synthetic
 * {@code kind}-capitalized fallback, which renders a stray non-native "Generic"/"Event"/"Neow"
 * label over the world/select screens). Case 3 pins the defensive passthrough seam: any host that
 * ever supplies a genuine non-empty {@code title} must keep it.</p>
 */
public class Sts1RoomShellTitleTest {

    /** A plain (generic/combat) native room: no {@code title}/{\code roomID} field at all. */
    public static class StubPlainRoom {
    }

    /** A room exposing an empty {@code title} field — still no genuine title. */
    public static class StubEmptyTitleRoom {
        public String title = "";
    }

    /** A defensive passthrough stub: a host that genuinely supplies a title keeps it. */
    public static class StubRealTitleRoom {
        public String title = "Real Room Title";
    }

    @Test
    public void plainRoomYieldsEmptyTitleNoSyntheticFallback() {
        RoomShellView view = Sts1PresentationBackend.readRoomShellView(new StubPlainRoom());

        assertEquals("plain room has no native title; the synthetic fallback must be gone",
                "", view.title);
        // Criterion 3: the chrome/resource mapping is unchanged for the plain case.
        assertEquals("generic", view.kind);
        assertEquals(ResourceIds.UI_ROOM_SHELL_GENERIC, view.resourceId);
    }

    @Test
    public void emptyTitleFieldStillYieldsEmptyTitle() {
        RoomShellView view = Sts1PresentationBackend.readRoomShellView(new StubEmptyTitleRoom());

        assertEquals("an empty native title must stay empty, not be capitalized into a label",
                "", view.title);
        assertEquals("generic", view.kind);
    }

    @Test
    public void realTitleFieldIsPreserved() {
        RoomShellView view = Sts1PresentationBackend.readRoomShellView(new StubRealTitleRoom());

        assertEquals("a genuine host title must pass through unchanged", "Real Room Title",
                view.title);
    }

    @Test
    public void nullRoomFailsOpenToEmptyView() {
        RoomShellView view = Sts1PresentationBackend.readRoomShellView(null);

        assertEquals("", view.title);
        assertFalse("a null room must fail open to an empty, unavailable view", view.available);
    }
}

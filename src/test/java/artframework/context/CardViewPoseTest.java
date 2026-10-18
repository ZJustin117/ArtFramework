package artframework.context;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * U05b: a caller that supplies NO card pose must get an ABSENT pose ({@code null}), not a synthetic
 * {@code CardPose.at(0,0)}. The absent pose is what makes
 * {@code SelectDrawPath.buildFromProjection}'s on-screen default layout reachable; the synthetic
 * origin pose pins every such card to the top-left screen corner.
 *
 * <p>These are observable-API assertions only: the public {@code CardView.pose} field, the builder,
 * and a public constructor. No private state is inspected.
 */
public class CardViewPoseTest {

    private static CardRef ref() {
        return new CardRef("c1", "Strike_R");
    }

    @Test
    public void builderWithoutPoseLeavesPoseAbsent() {
        CardView card =
                CardView.builder(ref())
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .build();

        assertNull(
                "U05b: a builder caller that supplies no pose must keep pose == null; a synthetic "
                        + "(0,0) pose defeats SelectDrawPath's default on-screen layout",
                card.pose);
    }

    @Test
    public void builderKeepsTheSuppliedPoseUnchanged() {
        CardPose pose = CardPose.at(10f, 20f);
        CardView card =
                CardView.builder(ref())
                        .zone(CardZone.SELECT)
                        .slot(0)
                        .pose(pose)
                        .build();

        assertSame("U05b: an explicitly supplied pose must be reported unchanged", pose, card.pose);
        assertEquals(10f, card.pose.x, 0.001f);
        assertEquals(20f, card.pose.y, 0.001f);
    }

    @Test
    public void constructorWithoutPoseLeavesPoseAbsent() {
        CardView card =
                new CardView(ref(), CardZone.SELECT, 0, null, true, false, false, false, "", "");

        assertNull(
                "U05b: the constructor must also preserve an absent pose (both the constructor and "
                        + "the builder default are relaxed)",
                card.pose);
    }

    @Test
    public void constructorKeepsTheSuppliedPoseUnchanged() {
        CardPose pose = new CardPose(5f, 6f, 0f, 1f, 0f, true);
        CardView card =
                new CardView(ref(), CardZone.SELECT, 0, pose, true, false, false, false, "", "");

        assertSame("U05b: a supplied pose must not be copied or normalized", pose, card.pose);
        assertEquals(5f, card.pose.x, 0.001f);
        assertEquals(6f, card.pose.y, 0.001f);
    }
}

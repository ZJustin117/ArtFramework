package artframework.sts1.render;

import artframework.component.Rect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Sts1HandCardRendererTest {
    @Test
    public void drawItemBoundsRemainHostNeutral() {
        HandDrawPath.DrawItem item = new HandDrawPath.DrawItem(
                "id", "Strike_R", 100f, 200f, 15f, 0.8f, true,
                "missing-art", "missing-frame", false, "", "", "Strike", "1", "Attack", "Hit");
        Rect bounds = item.bounds();
        assertEquals(200f, bounds.width, 0.01f);
        assertEquals(280f, bounds.height, 0.01f);
    }

    @Test
    public void returnsZeroWhenNoLiveCardSupplied() {
        HandDrawPath.DrawItem item = new HandDrawPath.DrawItem(
                "id", "Strike_R", 100f, 200f, 15f, 0.8f, true,
                "art", "frame", true, "", "", "Strike", "1", "Attack", "Hit");
        int result = Sts1HandCardRenderer.render(null, item, null);
        assertEquals(0, result);
    }

    @Test
    public void classifiedAsNativeCardAuthoritative() {
        assertTrue(Sts1HandCardRenderer.isNativeCardAuthoritative());
    }
}

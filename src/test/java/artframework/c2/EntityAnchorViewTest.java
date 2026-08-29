package artframework.c2;

import artframework.assets.ResourceIds;
import artframework.component.Rect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EntityAnchorViewTest {

    @Test
    public void anchorSnapshotFallsBackResourceAndDoesNotClaimCreatureArtPixels() {
        EntityAnchorView anchor = new EntityAnchorView("p1", EntityKind.PLAYER, "Ironclad",
                10f, 20f, new Rect(1f, 2f, 3f, 4f), true, false, "");

        assertEquals(ResourceIds.CHAR_UNKNOWN, anchor.assetId);
        EntitySnapshot snapshot = anchor.toSnapshot();
        assertEquals("Ironclad", snapshot.label);
        assertEquals("", snapshot.artResourceId);
        assertEquals(ResourceIds.CHAR_UNKNOWN, snapshot.extras.get("anchorAssetId"));
        assertEquals(Boolean.TRUE, snapshot.extras.get("nativePixelsAuthoritative"));
        assertFalse(anchor.claimed);
    }
}

package artframework.skeleton;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpineAtlas4xParserTest {

    @Test
    public void parsesCompactSpine4Atlas() {
        String atlas =
                "ironclad.png\n"
                        + "size:1000,269\n"
                        + "filter:Linear,Linear\n"
                        + "scale:0.32\n"
                        + "back mask\n"
                        + "bounds:384,69,48,42\n"
                        + "rotate:90\n"
                        + "belt\n"
                        + "bounds:936,82,72,28\n"
                        + "offsets:0,1,72,29\n";

        List<SpineAtlasRegion> regions = SpineAtlas4xParser.parse(atlas);

        assertEquals(2, regions.size());
        assertEquals("ironclad.png", regions.get(0).page);
        assertEquals("back mask", regions.get(0).name);
        assertEquals(384, regions.get(0).x);
        assertEquals(69, regions.get(0).y);
        assertEquals(48, regions.get(0).width);
        assertEquals(42, regions.get(0).height);
        assertEquals(90, regions.get(0).degrees);
        assertTrue(regions.get(0).rotated());
        assertEquals(0.32f, regions.get(0).scale, 0.0001f);
        assertEquals("belt", regions.get(1).name);
        assertEquals(1, regions.get(1).offsetY);
        assertEquals(72, regions.get(1).originalWidth);
        assertEquals(29, regions.get(1).originalHeight);
    }

    @Test
    public void supportsBooleanRotateFalse() {
        List<SpineAtlasRegion> regions =
                SpineAtlas4xParser.parse("page.png\nscale:1\nregion\nbounds:1,2,3,4\nrotate:false\n");

        assertEquals(1, regions.size());
        assertEquals(0, regions.get(0).degrees);
        assertFalse(regions.get(0).rotated());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRegionWithoutBounds() {
        SpineAtlas4xParser.parse("page.png\nregion\nrotate:90\n");
    }
}

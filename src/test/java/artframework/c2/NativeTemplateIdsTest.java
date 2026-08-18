package artframework.c2;

import artframework.component.NativeTemplateIds;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NativeTemplateIdsTest {

    @Test
    public void canonicalIdsAreSts1() {
        assertEquals("sts1.map", NativeTemplateIds.MAP);
        assertEquals("sts1.event", NativeTemplateIds.EVENT);
        assertEquals("sts1.select.grid", NativeTemplateIds.SELECT_GRID);
        assertEquals("sts1.select.hand", NativeTemplateIds.SELECT_HAND);
        assertEquals("sts1.endturn", NativeTemplateIds.END_TURN);
    }

    @Test
    public void legacyMapsToSts1() {
        assertEquals(NativeTemplateIds.MAP, NativeTemplateIds.canonicalize("sts.map"));
        assertEquals(NativeTemplateIds.EVENT, NativeTemplateIds.canonicalize("sts.event"));
        assertEquals(
                NativeTemplateIds.SELECT_GRID, NativeTemplateIds.canonicalize("sts.select.grid"));
        assertEquals(
                NativeTemplateIds.SELECT_HAND, NativeTemplateIds.canonicalize("sts.select.hand"));
        assertEquals(NativeTemplateIds.END_TURN, NativeTemplateIds.canonicalize("sts.endturn"));
        assertEquals(NativeTemplateIds.MAP, NativeTemplateIds.canonicalize(NativeTemplateIds.MAP));
    }

    @Test
    public void matchesAcceptsLegacy() {
        assertTrue(NativeTemplateIds.matches("sts.map", NativeTemplateIds.MAP));
        assertTrue(NativeTemplateIds.matches(NativeTemplateIds.MAP, NativeTemplateIds.MAP));
    }
}

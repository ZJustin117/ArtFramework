package artframework.sts1.render;

import artframework.context.OrbStanceView;
import artframework.context.SurfaceIds;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StanceDelegationSeamTest {

    /** Fully specified drawable stance entry; tests override individual fields as needed. */
    static OrbStanceView.Entry stanceEntry(String id, boolean visible, boolean hasImage) {
        return new OrbStanceView.Entry(id, "stance", id, 0, 0, 0, true, "res/" + id,
                artframework.component.Rect.ZERO, visible,
                45f, 0.5f, 0.25f, 0.75f, 0.8f,
                100f, 200f, 512f, 512f, 1.5f,
                hasImage, 256f, 256f);
    }

    static void publish(OrbStanceView.Entry... entries) {
        List<OrbStanceView.Entry> list = new ArrayList<OrbStanceView.Entry>();
        for (OrbStanceView.Entry entry : entries) list.add(entry);
        Sts1OrbStanceProjection.publish(new OrbStanceView(list, true));
    }

    @After
    public void tearDown() {
        StanceDelegationGate.resetForTests();
        StanceArtRenderer.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
    }

    @Test
    public void surfaceIdsCanonicalizeStance() {
        assertEquals(SurfaceIds.STANCE, SurfaceIds.canonicalize("sts1.stance"));
    }

    @Test
    public void delegationGateDefaultsOffAndToggles() {
        assertFalse(StanceDelegationGate.isActive());
        StanceDelegationGate.setActive(true);
        assertTrue(StanceDelegationGate.isActive());
        StanceDelegationGate.resetForTests();
        assertFalse(StanceDelegationGate.isActive());
    }

    @Test
    public void artRendererNotReadyWithoutProjectionEntry() {
        assertFalse(StanceArtRenderer.isReady("stance:Wrath"));
        assertFalse(StanceArtRenderer.isReady("Wrath"));
        assertFalse(StanceArtRenderer.isReady(null));
    }

    @Test
    public void artRendererReadyForVisibleImageEntry() {
        publish(stanceEntry("stance:Wrath", true, true));

        assertTrue(StanceArtRenderer.isReady("stance:Wrath"));
        assertFalse("a different owner is not ready", StanceArtRenderer.isReady("stance:Calm"));
    }

    @Test
    public void artRendererAcceptsBareEntryIdWithPrefixedOwner() {
        publish(stanceEntry("Wrath", true, true));

        assertTrue(StanceArtRenderer.isReady("Wrath"));
        assertTrue("prefixed owner form is accepted", StanceArtRenderer.isReady("stance:Wrath"));
    }

    @Test
    public void artRendererNotReadyWhenEntryHasNoImage() {
        publish(stanceEntry("stance:Wrath", true, false));

        assertFalse(StanceArtRenderer.isReady("stance:Wrath"));
    }

    @Test
    public void artRendererNotReadyWhenEntryInvisible() {
        publish(stanceEntry("stance:Wrath", false, true));

        assertFalse(StanceArtRenderer.isReady("stance:Wrath"));
    }

    @Test
    public void artRendererIgnoresNonStanceKind() {
        OrbStanceView.Entry orb = new OrbStanceView.Entry("orb:Lightning", "orb", "orb", 1, 0, 0,
                true, "res/orb", artframework.component.Rect.ZERO, true,
                0f, 1f, 1f, 1f, 1f, 1f, 1f, 64f, 64f, 1f, true, 32f, 32f);
        publish(orb);

        assertFalse(StanceArtRenderer.isReady("orb:Lightning"));
        assertFalse(StanceArtRenderer.isReady("stance:Lightning"));
    }

    @Test
    public void paramsMirrorNativeStanceDrawGeometry() {
        OrbStanceView.Entry entry = stanceEntry("stance:Wrath", true, true);
        StanceArtRenderer.DrawParams p = StanceArtRenderer.params(entry, 512, 256);

        assertEquals(entry.centerX, p.x, 0f);
        assertEquals(entry.centerY, p.y, 0f);
        assertEquals(entry.originX, p.originX, 0f);
        assertEquals(entry.originY, p.originY, 0f);
        assertEquals(entry.width, p.width, 0f);
        assertEquals(entry.height, p.height, 0f);
        assertEquals(entry.scale, p.scaleX, 0f);
        assertEquals(entry.scale, p.scaleY, 0f);
        assertEquals(-entry.angle, p.rotationDegrees, 0f);
        assertEquals(0, p.sourceX);
        assertEquals(0, p.sourceY);
        assertEquals(512, p.sourceWidth);
        assertEquals(256, p.sourceHeight);
        assertEquals(entry.colorR, p.r, 0f);
        assertEquals(entry.colorG, p.g, 0f);
        assertEquals(entry.colorB, p.b, 0f);
        assertEquals(entry.colorA, p.a, 0f);
    }
}

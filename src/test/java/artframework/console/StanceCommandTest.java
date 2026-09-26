package artframework.console;

import artframework.context.OrbStanceView;
import artframework.sts1.backend.Sts1OrbStanceProjection;
import artframework.sts1.render.StanceArtRenderer;
import artframework.sts1.render.StanceDelegationGate;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pure-logic coverage for the {@code art stance} console switch: the parse request plus the
 * default-off {@link StanceDelegationGate} semantics and the projection-derived readiness probe.
 * No game classes are needed, so these run headless.
 */
public class StanceCommandTest {

    @After
    public void tearDown() {
        StanceDelegationGate.resetForTests();
        Sts1OrbStanceProjection.resetForTests();
    }

    @Test
    public void parseStanceMapsDrawOnOffAndStatus() {
        ArtCommand.StanceRequest status = ArtCommand.parseStance(new String[0]);
        assertFalse(status.invalid);
        assertNull(status.delegate);

        assertNull(ArtCommand.parseStance(new String[] {"STATUS"}).delegate);
        assertFalse(ArtCommand.parseStance(new String[] {"status"}).invalid);

        assertEquals(Boolean.TRUE,
                ArtCommand.parseStance(new String[] {"draw", "ON"}).delegate);
        assertEquals(Boolean.FALSE,
                ArtCommand.parseStance(new String[] {"DRAW", " off "}).delegate);
    }

    @Test
    public void parseStanceRejectsUnknownInputWithoutThrowing() {
        assertTrue(ArtCommand.parseStance(new String[] {"draw"}).invalid);
        assertTrue(ArtCommand.parseStance(new String[] {"draw", "maybe"}).invalid);
        assertTrue(ArtCommand.parseStance(new String[] {"nonsense"}).invalid);
        assertTrue(ArtCommand.parseStance(new String[] {"draw", "on", "extra"}).invalid);
        assertTrue(ArtCommand.parseStance(new String[] {"on"}).invalid);
        // Null/empty tail defaults to status, matching art vfx status.
        assertFalse(ArtCommand.parseStance(null).invalid);
        assertNull(ArtCommand.parseStance(null).delegate);
    }

    @Test
    public void gateIsDefaultOffAndTogglesThroughRequestSemantics() {
        assertFalse(StanceDelegationGate.isActive());

        ArtCommand.StanceRequest on = ArtCommand.parseStance(new String[] {"draw", "on"});
        StanceDelegationGate.setActive(on.delegate.booleanValue());
        assertTrue(StanceDelegationGate.isActive());

        ArtCommand.StanceRequest off = ArtCommand.parseStance(new String[] {"draw", "off"});
        StanceDelegationGate.setActive(off.delegate.booleanValue());
        assertFalse(StanceDelegationGate.isActive());

        // status leaves the gate untouched.
        ArtCommand.parseStance(new String[] {"status"});
        assertFalse(StanceDelegationGate.isActive());
    }

    @Test
    public void readinessFalseWhenNoDrawableStanceInProjection() {
        Sts1OrbStanceProjection.resetForTests();
        assertFalse(StanceArtRenderer.isReady("stance:calm"));

        List<OrbStanceView.Entry> entries = new ArrayList<OrbStanceView.Entry>();
        entries.add(stance("calm", false, true));
        Sts1OrbStanceProjection.publish(new OrbStanceView(entries, true));
        assertFalse("invisible stance must not be ready", StanceArtRenderer.isReady("stance:calm"));

        List<OrbStanceView.Entry> images = new ArrayList<OrbStanceView.Entry>();
        images.add(stance("calm", true, false));
        Sts1OrbStanceProjection.publish(new OrbStanceView(images, true));
        assertFalse("imageless stance must not be ready", StanceArtRenderer.isReady("stance:calm"));
    }

    @Test
    public void readinessTrueForDrawableStanceOwner() {
        List<OrbStanceView.Entry> entries = new ArrayList<OrbStanceView.Entry>();
        entries.add(stance("wrath", true, true));
        Sts1OrbStanceProjection.publish(new OrbStanceView(entries, true));

        assertTrue(StanceArtRenderer.isReady("stance:wrath"));
        assertTrue(StanceArtRenderer.isReady("wrath"));
        assertFalse(StanceArtRenderer.isReady("stance:calm"));
    }

    private static OrbStanceView.Entry stance(String id, boolean visible, boolean hasImage) {
        return new OrbStanceView.Entry(id, "stance", id, 0, 0, 0, false, "res:" + id,
                null, visible, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 512f, 512f, 1f,
                hasImage, 256f, 256f);
    }
}

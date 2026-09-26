package artframework.console;

import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.AuraClaimPolicy;
import artframework.sts1.render.AuraDelegationGate;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-logic coverage for the {@code art aura} console switch: the parse request plus the
 * default-off {@link AuraDelegationGate} semantics and the injected-renderer readiness probe.
 * No game classes are needed, so these run headless.
 */
public class AuraCommandTest {

    @After
    public void tearDown() {
        AuraDelegationGate.resetForTests();
    }

    @Test
    public void parseAuraMapsOnOffAndStatus() {
        assertNull(ArtCommand.parseAura(new String[0]).delegate);
        assertFalse(ArtCommand.parseAura(new String[0]).invalid);

        assertNull(ArtCommand.parseAura(new String[] {"STATUS"}).delegate);
        assertFalse(ArtCommand.parseAura(new String[] {"status"}).invalid);

        assertEquals(Boolean.TRUE, ArtCommand.parseAura(new String[] {"ON"}).delegate);
        assertEquals(Boolean.FALSE, ArtCommand.parseAura(new String[] {" off "}).delegate);
    }

    @Test
    public void parseAuraRejectsUnknownInputWithoutThrowing() {
        assertTrue(ArtCommand.parseAura(new String[] {"maybe"}).invalid);
        assertTrue(ArtCommand.parseAura(new String[] {"on", "extra"}).invalid);
        assertTrue(ArtCommand.parseAura(new String[] {"draw", "on"}).invalid);
        // Null/empty tail defaults to status.
        assertFalse(ArtCommand.parseAura(null).invalid);
        assertNull(ArtCommand.parseAura(null).delegate);
    }

    @Test
    public void gateIsDefaultOffAndTogglesThroughRequestSemantics() {
        assertFalse(AuraDelegationGate.isActive());

        ArtCommand.AuraRequest on = ArtCommand.parseAura(new String[] {"on"});
        AuraDelegationGate.setActive(on.delegate.booleanValue());
        assertTrue(AuraDelegationGate.isActive());

        ArtCommand.AuraRequest off = ArtCommand.parseAura(new String[] {"off"});
        AuraDelegationGate.setActive(off.delegate.booleanValue());
        assertFalse(AuraDelegationGate.isActive());

        ArtCommand.parseAura(new String[] {"status"});
        assertFalse(AuraDelegationGate.isActive());
    }

    @Test
    public void readinessIsFalseForTheInertDefaultRenderer() {
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertFalse(AuraArtRenderer.isReady(AuraClaimPolicy.DIVINITY_PARTICLE_EFFECT));
    }
}

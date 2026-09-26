package artframework.console;

import artframework.sts1.render.AuraArtRenderer;
import artframework.sts1.render.AuraClaimPolicy;
import artframework.sts1.render.AuraDelegationGate;
import basemod.DevConsole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Pure-logic coverage for the {@code art aura} console switch: the parse request plus the
 * default-off {@link AuraDelegationGate} semantics and the injected-renderer readiness probe.
 * No game classes are needed, so these run headless.
 */
public class AuraCommandTest {

    @Before
    public void setUp() {
        // DevConsole's log/prompted buffers are built by its constructor, which never runs
        // headless; seed them so logVfx's DevConsole.log line lands somewhere readable.
        DevConsole.log = new ArrayList<String>();
        DevConsole.prompted = new ArrayList<Boolean>();
        AuraArtRenderer.resetDrawCountForTests();
    }

    @After
    public void tearDown() {
        AuraDelegationGate.resetForTests();
        AuraArtRenderer.resetDrawCountForTests();
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
    public void parseAuraMapsSpawnKindAndClampsCount() {
        ArtCommand.AuraRequest defaulted = ArtCommand.parseAura(new String[] {"spawn", "wrath"});
        assertFalse(defaulted.invalid);
        assertEquals("wrath", defaulted.spawnKind);
        assertEquals(3, defaulted.spawnCount);

        ArtCommand.AuraRequest explicit = ArtCommand.parseAura(new String[] {"spawn", "Stance", "5"});
        assertEquals("Stance", explicit.spawnKind);
        assertEquals(5, explicit.spawnCount);

        assertEquals(20, ArtCommand.parseAura(new String[] {"spawn", "divinity", "99"}).spawnCount);
        assertEquals(1, ArtCommand.parseAura(new String[] {"spawn", "divinity", "0"}).spawnCount);

        assertTrue("unknown kind is invalid",
                ArtCommand.parseAura(new String[] {"spawn", "bogus"}).invalid);
        assertTrue("bad count is invalid",
                ArtCommand.parseAura(new String[] {"spawn", "wrath", "many"}).invalid);
    }

    @Test
    public void parseAuraMapsClear() {
        assertFalse(ArtCommand.parseAura(new String[] {"clear"}).invalid);
        assertTrue(ArtCommand.parseAura(new String[] {"clear"}).clear);
        assertTrue(ArtCommand.parseAura(new String[] {"clear", "now"}).invalid);
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

    @Test
    public void spawnWithUnknownKindLogsErrorAndDoesNotThrow() {
        invokeAura("spawn", "bogus");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected the usage error, was: " + line, line.startsWith("ART_AURA error="));
        assertTrue(line.contains("spawn"));
    }

    @Test
    public void statusLineReportsDrawCounter() {
        invokeAura("status");
        assertTrue("expected draws=0, was: " + lastLine(), lastLine().contains("draws=0"));

        AuraArtRenderer.recordDraw();
        AuraArtRenderer.recordDraw();

        invokeAura("status");
        assertTrue("expected draws=2, was: " + lastLine(), lastLine().contains("draws=2"));
    }

    @Test
    public void clearLogsRemovalCountAndDoesNotThrow() {
        invokeAura("clear");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected a clear result line, was: " + line,
                line.startsWith("ART_AURA clear removed="));
    }

    @Test
    public void spawnWithSupportedKindLogsQueuedCount() {
        invokeAura("spawn", "wrath", "5");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected spawn result, was: " + line,
                line.startsWith("ART_AURA spawn=wrath count=5 queued="));
    }

    /** Invokes the private console entry with the argument tail after {@code art aura}. */
    private static void invokeAura(String... args) {
        try {
            Method method =
                    ArtCommand.class.getDeclaredMethod("cmdAura", String[].class, int.class);
            method.setAccessible(true);
            method.invoke(new ArtCommand(), (Object) args, 0);
        } catch (InvocationTargetException e) {
            throw new AssertionError("art aura threw", e.getCause());
        } catch (Exception e) {
            throw new AssertionError("could not invoke art aura", e);
        }
    }

    private static String lastLine() {
        return DevConsole.log.isEmpty() ? null : DevConsole.log.get(0);
    }
}

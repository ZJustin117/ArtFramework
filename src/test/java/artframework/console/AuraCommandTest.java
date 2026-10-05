package artframework.console;

import artframework.sts1.render.VfxArtRenderer;
import artframework.sts1.render.VfxClaimPolicy;
import artframework.sts1.render.VfxDelegationGate;
import basemod.DevConsole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Pure-logic coverage for the {@code art claim} console route (legacy alias {@code art aura}): the
 * parse request plus the default-off {@link VfxDelegationGate} semantics and the injected-renderer
 * readiness probe. Also pins that {@code art vfx} stays the unrelated STS2 bundle-runtime command.
 * No game classes are needed, so these run headless.
 */
public class AuraCommandTest {

    @Before
    public void setUp() {
        // DevConsole's log/prompted buffers are built by its constructor, which never runs
        // headless; seed them so logVfx's DevConsole.log line lands somewhere readable.
        DevConsole.log = new ArrayList<String>();
        DevConsole.prompted = new ArrayList<Boolean>();
        VfxArtRenderer.resetDrawCountForTests();
    }

    @After
    public void tearDown() {
        VfxDelegationGate.resetForTests();
        VfxArtRenderer.resetDrawCountForTests();
    }

    @Test
    public void parseClaimMapsOnOffAndStatus() {
        assertNull(ArtCommand.parseClaim(new String[0]).delegate);
        assertFalse(ArtCommand.parseClaim(new String[0]).invalid);

        assertNull(ArtCommand.parseClaim(new String[] {"STATUS"}).delegate);
        assertFalse(ArtCommand.parseClaim(new String[] {"status"}).invalid);

        assertEquals(Boolean.TRUE, ArtCommand.parseClaim(new String[] {"ON"}).delegate);
        assertEquals(Boolean.FALSE, ArtCommand.parseClaim(new String[] {" off "}).delegate);
    }

    @Test
    public void parseClaimRejectsUnknownInputWithoutThrowing() {
        assertTrue(ArtCommand.parseClaim(new String[] {"maybe"}).invalid);
        assertTrue(ArtCommand.parseClaim(new String[] {"on", "extra"}).invalid);
        assertTrue(ArtCommand.parseClaim(new String[] {"draw", "on"}).invalid);
        // Null/empty tail defaults to status.
        assertFalse(ArtCommand.parseClaim(null).invalid);
        assertNull(ArtCommand.parseClaim(null).delegate);
    }

    @Test
    public void parseClaimMapsSpawnKindAndClampsCount() {
        ArtCommand.ClaimRequest defaulted = ArtCommand.parseClaim(new String[] {"spawn", "wrath"});
        assertFalse(defaulted.invalid);
        assertEquals("wrath", defaulted.spawnKind);
        assertEquals(3, defaulted.spawnCount);

        ArtCommand.ClaimRequest explicit = ArtCommand.parseClaim(new String[] {"spawn", "Stance", "5"});
        assertEquals("Stance", explicit.spawnKind);
        assertEquals(5, explicit.spawnCount);

        assertEquals(20, ArtCommand.parseClaim(new String[] {"spawn", "divinity", "99"}).spawnCount);
        assertEquals(1, ArtCommand.parseClaim(new String[] {"spawn", "divinity", "0"}).spawnCount);

        assertTrue("unknown kind is invalid",
                ArtCommand.parseClaim(new String[] {"spawn", "bogus"}).invalid);
        assertTrue("bad count is invalid",
                ArtCommand.parseClaim(new String[] {"spawn", "wrath", "many"}).invalid);
    }

    @Test
    public void parseClaimMapsClear() {
        assertFalse(ArtCommand.parseClaim(new String[] {"clear"}).invalid);
        assertTrue(ArtCommand.parseClaim(new String[] {"clear"}).clear);
        assertTrue(ArtCommand.parseClaim(new String[] {"clear", "now"}).invalid);
    }

    @Test
    public void gateIsDefaultOffAndTogglesThroughRequestSemantics() {
        assertFalse(VfxDelegationGate.isActive());

        ArtCommand.ClaimRequest on = ArtCommand.parseClaim(new String[] {"on"});
        VfxDelegationGate.setActive(on.delegate.booleanValue());
        assertTrue(VfxDelegationGate.isActive());

        ArtCommand.ClaimRequest off = ArtCommand.parseClaim(new String[] {"off"});
        VfxDelegationGate.setActive(off.delegate.booleanValue());
        assertFalse(VfxDelegationGate.isActive());

        ArtCommand.parseClaim(new String[] {"status"});
        assertFalse(VfxDelegationGate.isActive());
    }

    @Test
    public void readinessIsFalseForTheInertDefaultRenderer() {
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.STANCE_AURA_EFFECT));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.WRATH_PARTICLE_EFFECT));
        assertFalse(VfxArtRenderer.isReady(VfxClaimPolicy.DIVINITY_PARTICLE_EFFECT));
    }

    @Test
    public void spawnWithUnknownKindLogsErrorAndDoesNotThrow() {
        invokeClaim("spawn", "bogus");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected the usage error, was: " + line, line.startsWith("ART_CLAIM error="));
        assertTrue(line.contains("spawn"));
    }

    @Test
    public void statusLineReportsDrawCounter() {
        invokeClaim("status");
        assertTrue("expected draws=0, was: " + lastLine(), lastLine().contains("draws=0"));

        VfxArtRenderer.recordDraw();
        VfxArtRenderer.recordDraw();

        invokeClaim("status");
        assertTrue("expected draws=2, was: " + lastLine(), lastLine().contains("draws=2"));
    }

    @Test
    public void clearLogsRemovalCountAndDoesNotThrow() {
        invokeClaim("clear");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected a clear result line, was: " + line,
                line.startsWith("ART_CLAIM clear retired="));
    }

    @Test
    public void spawnWithSupportedKindLogsQueuedCount() {
        invokeClaim("spawn", "wrath", "5");

        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected spawn result, was: " + line,
                line.startsWith("ART_CLAIM spawn=wrath count=5 queued="));
    }

    /** Invokes the private console entry with the argument tail after {@code art claim}. */
    private static void invokeClaim(String... args) {
        try {
            Method method =
                    ArtCommand.class.getDeclaredMethod("cmdClaim", String[].class, int.class);
            method.setAccessible(true);
            method.invoke(new ArtCommand(), (Object) args, 0);
        } catch (InvocationTargetException e) {
            throw new AssertionError("art claim threw", e.getCause());
        } catch (Exception e) {
            throw new AssertionError("could not invoke art claim", e);
        }
    }

    /** Drives the top-level console dispatch for a full {@code art <sub> ...} token line. */
    private static void dispatch(String... tokens) {
        try {
            Method method =
                    ArtCommand.class.getDeclaredMethod("execute", String[].class, int.class);
            method.setAccessible(true);
            method.invoke(new ArtCommand(), (Object) tokens, 0);
        } catch (InvocationTargetException e) {
            throw new AssertionError("art dispatch threw", e.getCause());
        } catch (Exception e) {
            throw new AssertionError("could not dispatch art command", e);
        }
    }

    @Test
    public void claimPrimaryAndAuraAliasRunTheSameClaimHandler() {
        dispatch("claim", "status");
        String viaClaim = lastLine();
        assertNotNull(viaClaim);
        assertTrue("expected the claim status line, was: " + viaClaim,
                viaClaim.startsWith("ART_CLAIM claim="));

        DevConsole.log.clear();
        dispatch("aura", "status");
        assertEquals("art aura must be an identical alias of art claim",
                viaClaim, lastLine());
    }

    @Test
    public void claimPrimaryAndAuraAliasToggleTheSameGate() {
        dispatch("claim", "on");
        assertTrue(VfxDelegationGate.isActive());
        assertTrue("expected the claim on line, was: " + lastLine(),
                lastLine().startsWith("ART_CLAIM claim=on"));

        dispatch("aura", "off");
        assertFalse(VfxDelegationGate.isActive());
        assertTrue("expected the alias to toggle the same gate, was: " + lastLine(),
                lastLine().startsWith("ART_CLAIM claim=off"));
    }

    @Test
    public void claimUsageAdvertisesTheFamilyNeutralRouteWithLegacyAlias() {
        dispatch("claim", "bogus");
        String line = lastLine();
        assertNotNull(line);
        assertTrue("expected usage to advertise art claim, was: " + line,
                line.contains("art claim on|off|status|spawn <kind> [count]|clear"));
        assertTrue("expected the legacy alias noted, was: " + line, line.contains("art aura"));
    }

    @Test
    public void vfxLoadStillRoutesToTheSts2BundleRuntime() {
        // `load` is the pre-existing STS2 bundle-runtime action, not the claim seam.
        dispatch("vfx", "load");
        assertTrue("expected the bundle-runtime usage error, was: " + firstLineWithPrefix("ART_VFX"),
                firstLineWithPrefix("ART_VFX").startsWith("ART_VFX error="));
        // ...and the structured envelope the art-verify runner needs for a fresh sequence.
        Map<String, Object> command = lastCommand();
        assertNotNull("art vfx must publish an ART_COMMAND envelope", command);
        assertEquals("ERROR", command.get("status"));
        assertEquals("art vfx load", command.get("command"));
    }

    @Test
    public void vfxStatusRoutesToTheBundleRuntimeAndNotTheClaimSeam() {
        // Regression: `art vfx status` must report bundle-runtime state, never claim-seam state.
        dispatch("vfx", "status");
        String line = firstLineWithPrefix("ART_VFX");
        assertNotNull(line);
        assertFalse("art vfx status must not report claim state, was: " + line,
                line.startsWith("ART_CLAIM"));
        assertTrue("art vfx status must remain the bundle-runtime status, was: " + line,
                line.startsWith("ART_VFX"));
        Map<String, Object> command = lastCommand();
        assertNotNull("art vfx status must publish an ART_COMMAND envelope", command);
        assertEquals("OK", command.get("status"));
        assertEquals("art vfx status", command.get("command"));
        assertTrue("expected statusLine in message, was: " + command.get("message"),
                String.valueOf(command.get("message")).contains("status="));
    }

    @Test
    public void vfxClearRoutesToTheBundleRuntimeAndNotTheClaimSeam() {
        dispatch("vfx", "clear");
        String line = firstLineWithPrefix("ART_VFX");
        assertNotNull(line);
        assertFalse("art vfx clear must not report claim state, was: " + line,
                line.startsWith("ART_CLAIM"));
        assertTrue("art vfx clear must remain the bundle-runtime clear, was: " + line,
                line.startsWith("ART_VFX"));
        Map<String, Object> command = lastCommand();
        assertNotNull("art vfx clear must publish an ART_COMMAND envelope", command);
        assertEquals("OK", command.get("status"));
        assertEquals("art vfx clear", command.get("command"));
    }

    @Test
    public void vfxLoadWithBundlePublishesFullCommandEnvelope() {
        // The runner matches the exact full console line, so the envelope command must include
        // the bundle argument. A nonexistent bundle fails open with an ERROR envelope, which is
        // exactly what the runner needs to resolve the step instead of stalling.
        dispatch("vfx", "load", "/nonexistent/art-vfx-bundle");
        Map<String, Object> command = lastCommand();
        assertNotNull(command);
        assertEquals("art vfx load /nonexistent/art-vfx-bundle", command.get("command"));
        assertEquals("ERROR", command.get("status"));
    }

    /** Newest ART_COMMAND envelope parsed from the DevConsole buffer, or null. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> lastCommand() {
        for (String line : DevConsole.log) {
            if (line != null && line.startsWith("ART_COMMAND ")) {
                Object parsed = artframework.component.MiniJson.parse(line.substring("ART_COMMAND ".length()));
                return parsed instanceof Map ? (Map<String, Object>) parsed : null;
            }
        }
        return null;
    }

    /** First buffered line starting with {@code prefix} (buffer is newest-first). */
    private static String firstLineWithPrefix(String prefix) {
        for (String line : DevConsole.log) {
            if (line != null && line.startsWith(prefix)) return line;
        }
        return null;
    }

    private static String lastLine() {
        return DevConsole.log.isEmpty() ? null : DevConsole.log.get(0);
    }
}

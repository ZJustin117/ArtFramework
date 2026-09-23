package artframework.sts1.render;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NativeRenderPolicyTest {
    @Test public void parsesTypedAndBareTargetsAndRejectsEmptyValues() {
        assertEquals(NativeRenderPolicy.Kind.FAMILY, NativeRenderPolicy.Target.parse("combat.hand").kind);
        assertEquals("surface:sts1.combat.hand", NativeRenderPolicy.Target.parse("surface:STS1.COMBAT.HAND").text());
        assertEquals("method:com.foo.player#render", NativeRenderPolicy.Target.parse("method:com.foo.Player#render").text());
        try { NativeRenderPolicy.Target.parse("surface:"); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void exemptionSnapshotMatchesFamilyClassAndMethod() {
        NativeRenderPolicy policy = new NativeRenderPolicy();
        NativeRenderInvocation invocation = new NativeRenderInvocation(1L, 1L, "combat",
                "sts1.combat.hand", "com.foo.Player", "render", "combat.hand", "source", null);
        policy.allow(NativeRenderPolicy.Target.parse("family:combat.hand"));
        assertTrue(policy.exempt(invocation));
        policy.clear();
        policy.allow(NativeRenderPolicy.Target.parse("class:com.foo.Player"));
        assertTrue(policy.exempt(invocation));
        policy.deny(NativeRenderPolicy.Target.parse("class:com.foo.Player"));
        assertFalse(policy.exempt(invocation));
    }

    @Test public void multipleAllowTargetsRemainExplicitAndClearRemovesAll() {
        NativeRenderPolicy policy = new NativeRenderPolicy();
        NativeRenderInvocation hand = new NativeRenderInvocation(10L, 1L, "combat",
                "sts1.combat.hand", "Player", "renderHand", "sts1.combat.hand", "hand", null);
        NativeRenderInvocation energy = new NativeRenderInvocation(11L, 1L, "combat",
                "sts1.combat.energy", "EnergyPanel", "render", "sts1.combat.energy", "energy", null);
        policy.allow(NativeRenderPolicy.Target.parse("family:sts1.combat.hand"));
        policy.allow(NativeRenderPolicy.Target.parse("family:sts1.combat.energy"));
        assertTrue(policy.exempt(hand));
        assertTrue(policy.exempt(energy));
        assertEquals(2, policy.exemptionTargets().size());
        policy.clear();
        assertFalse(policy.exempt(hand));
        assertFalse(policy.exempt(energy));
        assertTrue(policy.exemptionTargets().isEmpty());
    }

    @Test public void typedSkeletonAndEffectFamiliesMatchManifestFamilies() {
        NativeRenderInvocation skeleton = new NativeRenderInvocation(2L, 1L, "combat", "entity",
                "com.esotericsoftware.spine.SkeletonMeshRenderer", "draw", "skeleton-runtime", "s", null);
        NativeRenderInvocation effect = new NativeRenderInvocation(3L, 1L, "combat", "effect",
                "com.megacrit.cardcrawl.vfx.AbstractGameEffect", "render", "vfx-misc-root", "e", null);
        NativeRenderPolicy policy = new NativeRenderPolicy();
        policy.allow(NativeRenderPolicy.Target.parse("family:skeleton-runtime"));
        policy.allow(NativeRenderPolicy.Target.parse("family:vfx-misc-root"));
        assertTrue(policy.exempt(skeleton));
        assertTrue(policy.exempt(effect));
    }

    @Test public void methodTargetRequiresExactlyOneNonEmptyHashPair() {
        String[] invalid = {"method:#render", "method:Player#", "method:Player#render#extra"};
        for (String value : invalid) {
            try { NativeRenderPolicy.Target.parse(value); throw new AssertionError(value); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void verifyResetClearsIsolationTargetsAndCounters() {
        NativeRenderPolicy policy = NativeRenderBridge.policy();
        policy.setIsolate(true);
        policy.allow(NativeRenderPolicy.Target.parse("family:combat.hand"));
        policy.recordIsolated();
        policy.recordExempted();
        Sts1VerifyDiagnostics.resetForTests();
        assertFalse(policy.isIsolateActive());
        assertTrue(policy.exemptionTargets().isEmpty());
        assertEquals(Integer.valueOf(0), policy.probeSlice().get("suppressionCounters"));
        assertEquals(Integer.valueOf(0), policy.probeSlice().get("resolvedExemptions"));
    }
}

package artframework.sts1.render;

import com.badlogic.gdx.utils.Pool;
import com.megacrit.cardcrawl.vfx.CardTrailEffect;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VfxInitContractTest {

    @Test
    public void requiresRuntimeInitOnlyForCardTrail() {
        assertTrue(VfxInitContract.requiresRuntimeInit(VfxDrawGeometry.Kind.CARD_TRAIL));

        assertFalse(VfxInitContract.requiresRuntimeInit(VfxDrawGeometry.Kind.STANCE_AURA));
        assertFalse(VfxInitContract.requiresRuntimeInit(VfxDrawGeometry.Kind.FLASH_ATK_IMG));
        assertFalse(VfxInitContract.requiresRuntimeInit(VfxDrawGeometry.Kind.WRATH_STANCE_CHANGE));
        assertFalse(VfxInitContract.requiresRuntimeInit(
                VfxDrawGeometry.Kind.STANCE_CHANGE_ABSORPTION));
        assertFalse(VfxInitContract.requiresRuntimeInit(VfxDrawGeometry.Kind.GLOWY_FIRE_EYES));
        assertFalse(VfxInitContract.requiresRuntimeInit(null));
    }

    @Test
    public void requiresRuntimeInitIsExhaustivelyExactlyCardTrail() {
        // Genuine cross-source audit: derive the pooled-claimable set from VfxClaimPolicy's own
        // claimable FQNs (loaded WITHOUT static initialization) and the native Pool.Poolable type,
        // then require the contract to agree bidirectionally. A future pooled claimable added to the
        // policy but missing from VfxInitContract fails here (and vice versa), so this is not a
        // restatement of the contract's implementation.
        ClassLoader loader = VfxInitContractTest.class.getClassLoader();
        java.util.Set<String> poolableClaimed = new java.util.LinkedHashSet<String>();
        for (String fqn : VfxClaimPolicy.supportedClasses()) {
            Class<?> cls;
            try {
                cls = Class.forName(fqn, false, loader);
            } catch (ClassNotFoundException missing) {
                throw new AssertionError("claimable FQN is not on the test classpath: " + fqn, missing);
            }
            if (Pool.Poolable.class.isAssignableFrom(cls)) {
                poolableClaimed.add(fqn);
            }
        }
        assertEquals("exactly one claimable FQN may be Pool.Poolable",
                java.util.Collections.singleton(VfxClaimPolicy.CARD_TRAIL), poolableClaimed);

        // Bidirectional contract check over EVERY claimed FQN.
        for (String fqn : VfxClaimPolicy.supportedClasses()) {
            VfxDrawGeometry.Kind kind = VfxDrawGeometry.kindFor(fqn);
            if (kind == null) {
                continue;
            }
            assertEquals("requiresRuntimeInit must mirror Pool.Poolable for " + fqn,
                    poolableClaimed.contains(fqn), VfxInitContract.requiresRuntimeInit(kind));
        }
    }

    @Test
    public void initializerMethodAndArgsOnlyForCardTrail() {
        assertEquals("init",
                VfxInitContract.initializerMethod(VfxDrawGeometry.Kind.CARD_TRAIL));
        assertArrayEquals(new float[] {960f, 540f},
                VfxInitContract.initializerArgs(VfxDrawGeometry.Kind.CARD_TRAIL), 0f);

        assertNull(VfxInitContract.initializerMethod(VfxDrawGeometry.Kind.STANCE_AURA));
        assertNull(VfxInitContract.initializerMethod(null));
        assertNull(VfxInitContract.initializerArgs(VfxDrawGeometry.Kind.STANCE_AURA));
        assertNull(VfxInitContract.initializerArgs(null));
    }

    @Test
    public void initializerArgsReturnsFreshArrayEachCall() {
        float[] first = VfxInitContract.initializerArgs(VfxDrawGeometry.Kind.CARD_TRAIL);
        float[] second = VfxInitContract.initializerArgs(VfxDrawGeometry.Kind.CARD_TRAIL);
        assertNotNull(first);
        assertNotNull(second);
        assertFalse("each call must return a distinct array", first == second);
        first[0] = -1f;
        assertEquals("mutating one array must not affect the next",
                960f, VfxInitContract.initializerArgs(VfxDrawGeometry.Kind.CARD_TRAIL)[0], 0f);
    }

    @Test
    public void contractMatchesNativeCardTrailEffect() throws Exception {
        // The contract claims CardTrailEffect is a pooled Pool.Poolable initialized by a public
        // init(float, float). Prove it natively (reflection only; no invocation, no GL).
        assertTrue("CardTrailEffect must implement Pool.Poolable",
                Pool.Poolable.class.isAssignableFrom(CardTrailEffect.class));

        Method init = CardTrailEffect.class.getMethod("init", float.class, float.class);
        assertTrue("CardTrailEffect.init must be public",
                Modifier.isPublic(init.getModifiers()));
        assertEquals("init", init.getName());
        assertEquals("init must return void", Void.TYPE, init.getReturnType());
        assertEquals(2, init.getParameterTypes().length);
    }
}

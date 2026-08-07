package artframework.sts1.skeleton;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class Sts1NativeSkeletonRenderPolicyTest {
    @After public void clear() { Sts1NativeSkeletonRenderPolicy.clear(); }

    @Test public void suppressesOnlyClaimedObjectsWhenEnabled() {
        Object claimed = new Object();
        Object nativeOnly = new Object();
        Sts1NativeSkeletonRenderPolicy.claim(claimed);
        assertFalse(Sts1NativeSkeletonRenderPolicy.suppress(claimed));
        Sts1NativeSkeletonRenderPolicy.enable(true);
        assertTrue(Sts1NativeSkeletonRenderPolicy.suppress(claimed));
        assertFalse(Sts1NativeSkeletonRenderPolicy.suppress(nativeOnly));
        Sts1NativeSkeletonRenderPolicy.release(claimed);
        assertFalse(Sts1NativeSkeletonRenderPolicy.suppress(claimed));
    }
}

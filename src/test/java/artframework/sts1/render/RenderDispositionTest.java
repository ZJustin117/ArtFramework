package artframework.sts1.render;

import org.junit.Test;

public class RenderDispositionTest {
    @Test(expected = IllegalArgumentException.class)
    public void failOpenRequiresReason() {
        RenderDisposition.failOpen(1L, "");
    }
}

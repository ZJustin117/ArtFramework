package artframework.core;

import artframework.component.UiNode;
import artframework.component.UiTypes;
import artframework.presentation.PresentationRuntime;
import artframework.test.C1RuntimeFixture;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PresentProductionTest {
    @Test public void projectPresentResolvesForEntityAndRefreshes() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("plain").build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("plain", root);
        try {
            assertTrue(PresentResolve.forEntity(fixture.context, fixture.root).fromProject);
            ProjectPresent.set(PresentProfiles.LIGHTWAVE);
            assertEquals(PresentProfiles.LIGHTWAVE,
                    PresentResolve.forEntity(fixture.context, fixture.root).profileId);
        } finally { fixture.close(); ProjectPresent.resetForTests(); }
    }

    @Test public void declaredProfileBeatsProjectPresent() {
        UiNode root = UiNode.of(UiTypes.WINDOW).id("decl")
                .prop("present_profile", PresentProfiles.LIGHTWAVE).build();
        C1RuntimeFixture fixture = C1RuntimeFixture.mount("decl", root);
        try { assertEquals(PresentProfiles.LIGHTWAVE,
                PresentResolve.forEntity(fixture.context, fixture.root).profileId); }
        finally { fixture.close(); }
    }
}

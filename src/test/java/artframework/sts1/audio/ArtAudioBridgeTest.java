package artframework.sts1.audio;

import artframework.api.ArtFramework;
import artframework.assets.ResourceIds;
import artframework.sts1.assets.Sts1HostAssets;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArtAudioBridgeTest {

    @After
    public void tearDown() {
        ArtFramework.resetForTests();
        Sts1HostAssets.resetForTests();
    }

    @Test
    public void playResolvesCatalog() {
        Sts1HostAssets.install();
        ArtAudioBridge.Cue c = ArtAudioBridge.play(ResourceIds.AUDIO_SFX_PREFIX + "card_select");
        assertTrue(c.found);
        assertTrue(c.source.startsWith("sts1:"));
        assertEquals(1, ArtAudioBridge.log().size());
    }

    @Test
    public void mutedSkipsLog() {
        Sts1HostAssets.install();
        ArtAudioBridge.setMuted(true);
        ArtAudioBridge.play(ResourceIds.AUDIO_SFX_PREFIX + "end_turn");
        assertEquals(0, ArtAudioBridge.log().size());
        assertTrue(ArtAudioBridge.isMuted());
    }

    @Test
    public void missingStillLogged() {
        ArtAudioBridge.Cue c = ArtAudioBridge.play("audio.sfx.not_real_xyz");
        assertFalse(c.found);
        assertEquals(1, ArtAudioBridge.log().size());
    }
}

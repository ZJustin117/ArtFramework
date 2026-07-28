package artframework.sts1.audio;

import artframework.api.ArtFramework;
import artframework.assets.AssetResolveResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ART UI audio bridge (16.8): resolve ResourceIds then queue play requests. Host playback is
 * optional; pure log for JUnit / probe.
 */
public final class ArtAudioBridge {

    public static final class Cue {
        public final String resourceId;
        public final String source;
        public final boolean found;
        public final long atFrame;

        public Cue(String resourceId, String source, boolean found, long atFrame) {
            this.resourceId = resourceId != null ? resourceId : "";
            this.source = source != null ? source : "";
            this.found = found;
            this.atFrame = atFrame;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("resourceId", resourceId);
            m.put("source", source);
            m.put("found", Boolean.valueOf(found));
            m.put("atFrame", Long.valueOf(atFrame));
            return m;
        }
    }

    private static final List<Cue> LOG = new ArrayList<Cue>();
    private static final int MAX_LOG = 64;
    private static boolean muted;

    private ArtAudioBridge() {}

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static boolean isMuted() {
        return muted;
    }

    public static Cue play(String resourceId) {
        if (muted) {
            Cue c = new Cue(resourceId, "", false, ArtFramework.projection().lastFrameId());
            return c;
        }
        AssetResolveResult r = ArtFramework.assets().resolve(resourceId);
        Cue c =
                new Cue(
                        resourceId,
                        r.found || r.fallback ? r.source : "",
                        r.found,
                        ArtFramework.projection().lastFrameId());
        LOG.add(c);
        while (LOG.size() > MAX_LOG) {
            LOG.remove(0);
        }
        return c;
    }

    public static List<Cue> log() {
        return Collections.unmodifiableList(new ArrayList<Cue>(LOG));
    }

    public static Map<String, Object> probeSlice() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("muted", Boolean.valueOf(muted));
        m.put("logSize", Integer.valueOf(LOG.size()));
        List<Map<String, Object>> recent = new ArrayList<Map<String, Object>>();
        int from = Math.max(0, LOG.size() - 8);
        for (int i = from; i < LOG.size(); i++) {
            recent.add(LOG.get(i).toMap());
        }
        m.put("recent", recent);
        return m;
    }

    public static void resetForTests() {
        LOG.clear();
        muted = false;
    }
}

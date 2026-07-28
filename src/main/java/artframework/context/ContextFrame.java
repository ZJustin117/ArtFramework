package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable authority snapshot for one present frame. Hard-sync source for ART projection.
 */
public final class ContextFrame {

    public final long frameId;
    /** Increments when Backend enters a new scene; old projection must not survive it. */
    public final long sceneEpoch;
    public final String scene;
    public final List<CardView> cards;
    public final Map<String, Object> controls;
    public final Map<String, Object> map;
    public final boolean available;
    public final ViewportView viewport;

    public ContextFrame(
            long frameId,
            long sceneEpoch,
            String scene,
            List<CardView> cards,
            Map<String, Object> controls,
            Map<String, Object> map,
            boolean available,
            ViewportView viewport) {
        this.frameId = frameId;
        this.sceneEpoch = sceneEpoch;
        this.scene = scene != null ? scene : "";
        if (cards == null || cards.isEmpty()) {
            this.cards = Collections.emptyList();
        } else {
            this.cards = Collections.unmodifiableList(new ArrayList<CardView>(cards));
        }
        this.controls =
                controls == null
                        ? Collections.<String, Object>emptyMap()
                        : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(controls));
        this.map =
                map == null
                        ? Collections.<String, Object>emptyMap()
                        : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(map));
        this.available = available;
        this.viewport = viewport != null ? viewport : ViewportView.unavailable();
    }

    /** Compatibility constructor for callers without an explicit scene epoch. */
    public ContextFrame(
            long frameId,
            String scene,
            List<CardView> cards,
            Map<String, Object> controls,
            Map<String, Object> map,
            boolean available) {
        this(frameId, 0L, scene, cards, controls, map, available, null);
    }

    public static ContextFrame unavailable(long frameId) {
        return new ContextFrame(frameId, 0L, "", null, null, null, false, null);
    }

    public static ContextFrame of(long frameId, String scene, List<CardView> cards) {
        return new ContextFrame(frameId, 0L, scene, cards, null, null, true, null);
    }

    public List<CardView> cardsIn(CardZone zone) {
        if (zone == null || cards.isEmpty()) {
            return Collections.emptyList();
        }
        List<CardView> out = new ArrayList<CardView>();
        for (CardView c : cards) {
            if (c.zone == zone) {
                out.add(c);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public CardView findByInstance(String instanceId) {
        if (instanceId == null) {
            return null;
        }
        for (CardView c : cards) {
            if (instanceId.equals(c.ref.instanceId)) {
                return c;
            }
        }
        return null;
    }
}

package artframework.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable combat/chrome control snapshot for one present frame. Individual controls live in
 * {@link #controls}; common combat fields are first-class for hard-sync consumers.
 */
public final class ControlsView {

    public static final String END_TURN_ID = "end_turn";

    public final List<ControlView> controls;
    public final int energy;
    public final int handSize;
    public final int drawSize;
    public final int discardSize;
    public final int exhaustSize;
    public final boolean endTurnEnabled;
    public final boolean endTurnVisible;

    public ControlsView(
            List<ControlView> controls,
            int energy,
            int handSize,
            int drawSize,
            int discardSize,
            int exhaustSize,
            boolean endTurnEnabled,
            boolean endTurnVisible) {
        if (controls == null || controls.isEmpty()) {
            this.controls = Collections.emptyList();
        } else {
            this.controls = Collections.unmodifiableList(new ArrayList<ControlView>(controls));
        }
        this.energy = energy;
        this.handSize = handSize;
        this.drawSize = drawSize;
        this.discardSize = discardSize;
        this.exhaustSize = exhaustSize;
        this.endTurnEnabled = endTurnEnabled;
        this.endTurnVisible = endTurnVisible;
    }

    public static ControlsView empty() {
        return new ControlsView(null, 0, 0, 0, 0, 0, false, false);
    }

    /** Combat chrome with a single end-turn control entry. */
    public static ControlsView combat(
            int energy,
            int handSize,
            int drawSize,
            int discardSize,
            int exhaustSize,
            boolean endTurnEnabled,
            boolean endTurnVisible) {
        List<ControlView> list = new ArrayList<ControlView>();
        list.add(new ControlView(END_TURN_ID, "End Turn", "", endTurnVisible, endTurnEnabled));
        return new ControlsView(
                list, energy, handSize, drawSize, discardSize, exhaustSize, endTurnEnabled, endTurnVisible);
    }

    public ControlView find(String id) {
        if (id == null) {
            return null;
        }
        for (ControlView c : controls) {
            if (id.equals(c.id)) {
                return c;
            }
        }
        return null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("energy", Integer.valueOf(energy));
        m.put("handSize", Integer.valueOf(handSize));
        m.put("drawSize", Integer.valueOf(drawSize));
        m.put("discardSize", Integer.valueOf(discardSize));
        m.put("exhaustSize", Integer.valueOf(exhaustSize));
        m.put("endTurnEnabled", Boolean.valueOf(endTurnEnabled));
        m.put("endTurnVisible", Boolean.valueOf(endTurnVisible));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ControlView c : controls) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", c.id);
            row.put("text", c.text);
            row.put("iconResourceId", c.iconResourceId);
            row.put("visible", Boolean.valueOf(c.visible));
            row.put("enabled", Boolean.valueOf(c.enabled));
            list.add(row);
        }
        m.put("controls", list);
        return m;
    }
}

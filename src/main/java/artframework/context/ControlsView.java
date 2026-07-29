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
    public static final String PROCEED_ID = "proceed";
    public static final String CANCEL_ID = "cancel";
    public static final String ENERGY_ID = "energy";

    public final List<ControlView> controls;
    public final int energy;
    public final int handSize;
    public final int drawSize;
    public final int discardSize;
    public final int exhaustSize;
    public final boolean endTurnEnabled;
    public final boolean endTurnVisible;
    public final boolean proceedEnabled;
    public final boolean proceedVisible;
    public final boolean cancelEnabled;
    public final boolean cancelVisible;

    public ControlsView(
            List<ControlView> controls,
            int energy,
            int handSize,
            int drawSize,
            int discardSize,
            int exhaustSize,
            boolean endTurnEnabled,
            boolean endTurnVisible) {
        this(
                controls,
                energy,
                handSize,
                drawSize,
                discardSize,
                exhaustSize,
                endTurnEnabled,
                endTurnVisible,
                false,
                false,
                false,
                false);
    }

    public ControlsView(
            List<ControlView> controls,
            int energy,
            int handSize,
            int drawSize,
            int discardSize,
            int exhaustSize,
            boolean endTurnEnabled,
            boolean endTurnVisible,
            boolean proceedEnabled,
            boolean proceedVisible,
            boolean cancelEnabled,
            boolean cancelVisible) {
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
        this.proceedEnabled = proceedEnabled;
        this.proceedVisible = proceedVisible;
        this.cancelEnabled = cancelEnabled;
        this.cancelVisible = cancelVisible;
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
        list.add(new ControlView(ENERGY_ID, String.valueOf(energy), "", true, true));
        return new ControlsView(
                list, energy, handSize, drawSize, discardSize, exhaustSize, endTurnEnabled, endTurnVisible);
    }

    public static ControlsView combatWithProceed(
            int energy,
            int handSize,
            int drawSize,
            int discardSize,
            int exhaustSize,
            boolean endTurnEnabled,
            boolean endTurnVisible,
            boolean proceedEnabled,
            boolean proceedVisible,
            boolean cancelEnabled,
            boolean cancelVisible) {
        List<ControlView> list = new ArrayList<ControlView>();
        list.add(new ControlView(END_TURN_ID, "End Turn", "", endTurnVisible, endTurnEnabled));
        list.add(new ControlView(ENERGY_ID, String.valueOf(energy), "", true, true));
        if (proceedVisible || proceedEnabled) {
            list.add(new ControlView(PROCEED_ID, "Proceed", "", proceedVisible, proceedEnabled));
        }
        if (cancelVisible || cancelEnabled) {
            list.add(new ControlView(CANCEL_ID, "Cancel", "", cancelVisible, cancelEnabled));
        }
        return new ControlsView(
                list,
                energy,
                handSize,
                drawSize,
                discardSize,
                exhaustSize,
                endTurnEnabled,
                endTurnVisible,
                proceedEnabled,
                proceedVisible,
                cancelEnabled,
                cancelVisible);
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
        m.put("proceedEnabled", Boolean.valueOf(proceedEnabled));
        m.put("proceedVisible", Boolean.valueOf(proceedVisible));
        m.put("cancelEnabled", Boolean.valueOf(cancelEnabled));
        m.put("cancelVisible", Boolean.valueOf(cancelVisible));
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

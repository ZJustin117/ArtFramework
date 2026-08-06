package artframework.skeleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trigger-to-animation graph for a skeleton instance.
 */
public final class AnimGraph {

    public final String initialStateId;
    private final Map<String, AnimState> states;
    private final Map<String, String> triggers;

    public AnimGraph(String initialStateId, Map<String, AnimState> states, Map<String, String> triggers) {
        if (initialStateId == null || initialStateId.isEmpty()) {
            throw new IllegalArgumentException("initialStateId required");
        }
        if (states == null || !states.containsKey(initialStateId)) {
            throw new IllegalArgumentException("initial state missing: " + initialStateId);
        }
        this.initialStateId = initialStateId;
        this.states = Collections.unmodifiableMap(new LinkedHashMap<String, AnimState>(states));
        this.triggers =
                triggers == null
                        ? Collections.<String, String>emptyMap()
                        : Collections.unmodifiableMap(new LinkedHashMap<String, String>(triggers));
        for (String stateId : this.triggers.values()) {
            if (!this.states.containsKey(stateId)) {
                throw new IllegalArgumentException("trigger targets unknown state: " + stateId);
            }
        }
        for (AnimState state : this.states.values()) {
            if (state.nextStateId != null && !this.states.containsKey(state.nextStateId)) {
                throw new IllegalArgumentException("state " + state.id + " targets unknown next state: " + state.nextStateId);
            }
        }
    }

    public static Builder builder(String initialStateId) {
        return new Builder(initialStateId);
    }

    public AnimState initialState() {
        return states.get(initialStateId);
    }

    public AnimState state(String id) {
        return states.get(id);
    }

    public AnimState stateForTrigger(String trigger) {
        String id = triggers.get(trigger);
        return id == null ? null : states.get(id);
    }

    public Map<String, AnimState> states() {
        return states;
    }

    public Map<String, String> triggers() {
        return triggers;
    }

    public static final class Builder {
        private final String initial;
        private final Map<String, AnimState> states = new LinkedHashMap<String, AnimState>();
        private final Map<String, String> triggers = new LinkedHashMap<String, String>();

        private Builder(String initial) {
            this.initial = initial;
        }

        public Builder state(String id, boolean looping) {
            states.put(id, new AnimState(id, looping));
            return this;
        }

        public Builder state(String id, boolean looping, String next) {
            states.put(id, new AnimState(id, looping, next));
            return this;
        }

        public Builder trigger(String trigger, String stateId) {
            triggers.put(trigger, stateId);
            return this;
        }

        public AnimGraph build() {
            return new AnimGraph(initial, states, triggers);
        }
    }
}

package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable ordered visual attachments; ambient and pulse are distinct attachment layers. */
public final class EffectsComponent {
    private final List<EffectAttachment> attachments;

    public EffectsComponent() {
        this(Collections.<EffectAttachment>emptyList());
    }

    public EffectsComponent(List<EffectAttachment> attachments) {
        this.attachments = attachments == null || attachments.isEmpty()
                ? Collections.<EffectAttachment>emptyList()
                : Collections.unmodifiableList(new ArrayList<EffectAttachment>(attachments));
    }

    public List<EffectAttachment> attachments() {
        return attachments;
    }

    public EffectsComponent withAttachment(EffectAttachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment required");
        List<EffectAttachment> next = new ArrayList<EffectAttachment>();
        boolean replaced = false;
        for (EffectAttachment current : attachments) {
            if (current.effectId.equals(attachment.effectId) && current.layer.equals(attachment.layer)) {
                next.add(attachment);
                replaced = true;
            } else {
                next.add(current);
            }
        }
        if (!replaced) next.add(attachment);
        return new EffectsComponent(next);
    }

    public EffectAttachment get(String effectId, String layer) {
        for (EffectAttachment attachment : attachments) {
            if (attachment.effectId.equals(effectId) && attachment.layer.equals(layer)) return attachment;
        }
        return null;
    }

    public EffectsComponent without(String effectId, String layer) {
        List<EffectAttachment> next = new ArrayList<EffectAttachment>();
        for (EffectAttachment attachment : attachments) {
            if (!attachment.effectId.equals(effectId) || !attachment.layer.equals(layer)) next.add(attachment);
        }
        return new EffectsComponent(next);
    }

    /** Replace one immutable attachment parameter without exposing a mutable effect cache. */
    public EffectsComponent withParam(String effectId, String layer, String name, Object value) {
        EffectAttachment current = get(effectId, layer);
        return current != null ? withAttachment(current.withParam(name, value)) : this;
    }

    /** Replace one immutable attachment enabled state without exposing a mutable effect cache. */
    public EffectsComponent withEnabled(String effectId, String layer, boolean enabled) {
        EffectAttachment current = get(effectId, layer);
        return current != null ? withAttachment(current.withEnabled(enabled)) : this;
    }
}

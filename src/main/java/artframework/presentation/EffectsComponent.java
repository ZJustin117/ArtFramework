package artframework.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered visual attachments; ambient and pulse are distinct attachment layers. */
public final class EffectsComponent {
    private final List<EffectAttachment> attachments = new ArrayList<EffectAttachment>();

    public List<EffectAttachment> attachments() {
        return Collections.unmodifiableList(new ArrayList<EffectAttachment>(attachments));
    }

    public void put(EffectAttachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment required");
        remove(attachment.effectId, attachment.layer);
        attachments.add(attachment);
    }

    public EffectAttachment get(String effectId, String layer) {
        for (EffectAttachment attachment : attachments) {
            if (attachment.effectId.equals(effectId) && attachment.layer.equals(layer)) return attachment;
        }
        return null;
    }

    public void remove(String effectId, String layer) {
        for (int i = attachments.size() - 1; i >= 0; i--) {
            EffectAttachment attachment = attachments.get(i);
            if (attachment.effectId.equals(effectId) && attachment.layer.equals(layer)) attachments.remove(i);
        }
    }
}

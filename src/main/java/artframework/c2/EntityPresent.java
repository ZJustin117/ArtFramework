package artframework.c2;

import java.util.List;

/**
 * C2 presenter track: slot lifecycle for players/cards/relics/monsters.
 * Pure bookkeeping here; STS {@code AbstractCard.render*} / player draw later.
 * No combat authority.
 */
public interface EntityPresent {

    /**
     * Attach or replace a slot. {@code kind} is an {@link EntityKind} name
     * (e.g. {@code "card"}, case-insensitive).
     */
    void attach(String slotId, String kind, String refId);

    void sync(String slotId, Object snapshotDto);

    void layout(String slotId, float x, float y, float scale);

    void detach(String slotId);

    boolean isAttached(String slotId);

    EntitySlot get(String slotId);

    List<String> listSlotIds();

    int size();

    void clear();
}

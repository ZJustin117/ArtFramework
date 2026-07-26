package artframework.c2;

/**
 * Optional observer for presenter lifecycle (render adapters later).
 */
public interface EntityPresentListener {

    void onAttached(EntitySlot slot);

    void onSynced(EntitySlot slot);

    void onLaidOut(EntitySlot slot);

    void onDetached(String slotId);
}

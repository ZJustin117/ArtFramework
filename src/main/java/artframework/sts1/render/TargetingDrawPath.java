package artframework.sts1.render;

import artframework.api.ArtFramework;
import artframework.context.CardEntity;
import artframework.context.CardPose;
import artframework.context.MonsterIntentView;
import artframework.context.SurfaceIds;
import artframework.context.TargetingSessionComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure targeting-arrow geometry (Slice D). */
public final class TargetingDrawPath {

    public static final class DrawItem {
        public final boolean active;
        public final String cardInstanceId;
        public final String targetKey;
        public final TargetingSessionComponent.Phase phase;
        public final float startX;
        public final float startY;
        public final float endX;
        public final float endY;
        public final float controlX;
        public final float controlY;

        public DrawItem(
                boolean active,
                String cardInstanceId,
                String targetKey,
                TargetingSessionComponent.Phase phase,
                float startX,
                float startY,
                float endX,
                float endY,
                float controlX,
                float controlY) {
            this.active = active;
            this.cardInstanceId = cardInstanceId != null ? cardInstanceId : "";
            this.targetKey = targetKey != null ? targetKey : "";
            this.phase = phase != null ? phase : TargetingSessionComponent.Phase.ARMED;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.controlX = controlX;
            this.controlY = controlY;
        }
    }

    private TargetingDrawPath() {}

    public static List<DrawItem> buildFromProjection() {
        TargetingSessionComponent session = ArtFramework.projection().targetingSession();
        if (!session.active) {
            return Collections.emptyList();
        }
        CardEntity card = ArtFramework.projection().get(session.cardInstanceId);
        CardPose pose = card != null ? card.pose : null;
        if (pose == null) {
            return Collections.emptyList();
        }
        float[] target = targetFor(session.targetKey);
        return Collections.singletonList(build(session, pose.x, pose.y, target[0], target[1]));
    }

    public static DrawItem build(
            TargetingSessionComponent session,
            float sourceX, float sourceY,
            float targetX, float targetY) {
        float midX = (sourceX + targetX) * 0.5f;
        float midY = (sourceY + targetY) * 0.5f;
        float dx = targetX - sourceX;
        float dy = targetY - sourceY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float normalX = dist > 0.0001f ? -dy / dist : 0f;
        float normalY = dist > 0.0001f ? dx / dist : 0f;
        float bend = dist * 0.25f;
        float controlX = midX + normalX * bend;
        float controlY = midY + normalY * bend;
        return new DrawItem(
                session.active,
                session.cardInstanceId,
                session.targetKey,
                session.phase,
                sourceX,
                sourceY,
                targetX,
                targetY,
                controlX,
                controlY);
    }

    private static float[] targetFor(String targetKey) {
        if (targetKey == null || targetKey.isEmpty()) {
            return new float[] {0f, 0f};
        }
        for (MonsterIntentView.IntentEntry entry : ArtFramework.projection().intents().entries) {
            if (targetKey.equals(entry.monsterId)) {
                return new float[] {entry.x, entry.y};
            }
        }
        return new float[] {0f, 0f};
    }
}

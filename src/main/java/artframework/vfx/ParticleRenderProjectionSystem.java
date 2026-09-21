package artframework.vfx;

import artframework.ecs.EcsSystem;
import artframework.ecs.EcsTick;
import artframework.ecs.EntityId;
import artframework.ecs.PresentationWorld;
import artframework.render.RenderOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stateless ECS-to-draw-data projection. */
public final class ParticleRenderProjectionSystem implements EcsSystem {
    @Override public void run(PresentationWorld world, EcsTick tick) {
        for (EntityId root : world.query(VfxSceneRuntimeComponent.class, VfxSceneResourcesComponent.class)) {
            List<VfxParticleDraw> draws = new ArrayList<VfxParticleDraw>();
            List<EntityId> nodes = world.query(VfxNodeComponent.class, VfxTransformComponent.class,
                    VfxEmitterComponent.class, VfxParticleBufferComponent.class);
            for (EntityId node : nodes) {
                VfxNodeComponent metadata = world.get(node, VfxNodeComponent.class);
                if (!root.equals(metadata.rootEntity)) continue;
                VfxTransformComponent transform = world.get(node, VfxTransformComponent.class);
                if (!transform.visible) continue;
                VfxEmitterComponent emitter = world.get(node, VfxEmitterComponent.class);
                String texture = outputPath(world.get(root, VfxSceneResourcesComponent.class).resources,
                        emitter.textureResourceId);
                if (texture == null) continue;
                WorldTransform composed = compose(world, node, new HashMap<EntityId, WorldTransform>());
                if (!composed.visible) continue;
                List<VfxParticle> particles = world.get(node, VfxParticleBufferComponent.class).particles;
                for (int index = 0; index < particles.size(); index++) {
                    VfxParticle particle = particles.get(index);
                    VfxColor color = multiply(composed.color, particle.color == null
                            ? new VfxColor(1f, 1f, 1f, 1f) : particle.color);
                    VfxFlipbookDefinition flipbook = emitter.flipbook;
                    int columns = flipbook == null ? 1 : Math.max(1, flipbook.hFrames);
                    int rows = flipbook == null ? 1 : Math.max(1, flipbook.vFrames);
                    int frameCount = columns * rows;
                    float speed = flipbook == null ? 0f : (flipbook.animationSpeedMin + flipbook.animationSpeedMax) * 0.5f;
                    int frame = frameCount == 1 ? 0 : (int) Math.floor(Math.max(0f, particle.age) * speed);
                    frame = flipbook != null && flipbook.loop ? frame % frameCount : Math.min(frame, frameCount - 1);
                    draws.add(new VfxParticleDraw(world.get(root, VfxSceneRuntimeComponent.class).sceneId,
                            metadata.definitionNodeId, particle.spawnIndex, metadata.definitionOrder, texture,
                            composed.x + particle.position.x * composed.scaleX,
                            composed.y + particle.position.y * composed.scaleY,
                            composed.rotation + particle.rotationDegrees,
                            composed.scaleX * particle.scale, composed.scaleY * particle.scale,
                            color.r, color.g, color.b, color.a * particle.alpha,
                            emitter.blendMode == null ? "MIX" : emitter.blendMode,
                            composed.z, frame, columns, rows, false, false));
                }
            }
            Collections.sort(draws, new Comparator<VfxParticleDraw>() {
                @Override public int compare(VfxParticleDraw a, VfxParticleDraw b) {
                    return RenderOrder.COMPARATOR.compare(a.renderOrder, b.renderOrder);
                }
            });
            world.put(root, VfxDrawListComponent.class, new VfxDrawListComponent(new VfxDrawList(draws)));
        }
    }

    private static String outputPath(List<VfxResourceRef> resources, String id) {
        if (id == null) return null;
        for (VfxResourceRef ref : resources) {
            if (id.equals(ref.resourceId)) return ref.available() ? ref.outputPath : null;
        }
        return null;
    }

    private static WorldTransform compose(PresentationWorld world, EntityId entity,
            Map<EntityId, WorldTransform> cache) {
        WorldTransform cached = cache.get(entity); if (cached != null) return cached;
        VfxTransformComponent local = world.get(entity, VfxTransformComponent.class);
        VfxNodeComponent node = world.get(entity, VfxNodeComponent.class);
        if (node == null) {
            WorldTransform root = WorldTransform.from(local);
            cache.put(entity, root);
            return root;
        }
        WorldTransform parent = node.parentEntity != null && world.has(node.parentEntity, VfxTransformComponent.class)
                ? compose(world, node.parentEntity, cache) : WorldTransform.root();
        float radians = (float) Math.toRadians(parent.rotation);
        float px = local.position.x * parent.scaleX;
        float py = local.position.y * parent.scaleY;
        WorldTransform result = new WorldTransform(parent.x + px * (float) Math.cos(radians) - py * (float) Math.sin(radians),
                parent.y + px * (float) Math.sin(radians) + py * (float) Math.cos(radians),
                parent.rotation + local.rotationDegrees, parent.scaleX * local.scale.x,
                parent.scaleY * local.scale.y, parent.z + local.zIndex,
                parent.visible && local.visible, multiply(parent.color, local.color));
        cache.put(entity, result); return result;
    }

    private static VfxColor multiply(VfxColor a, VfxColor b) {
        return new VfxColor(a.r * b.r, a.g * b.g, a.b * b.b, a.a * b.a);
    }

    private static final class WorldTransform {
        final float x, y, rotation, scaleX, scaleY, z; final boolean visible; final VfxColor color;
        WorldTransform(float x, float y, float rotation, float scaleX, float scaleY, float z,
                boolean visible, VfxColor color) { this.x=x; this.y=y; this.rotation=rotation;
            this.scaleX=scaleX; this.scaleY=scaleY; this.z=z; this.visible=visible; this.color=color; }
        static WorldTransform from(VfxTransformComponent transform) {
            return new WorldTransform(transform.position.x, transform.position.y,
                    transform.rotationDegrees, transform.scale.x, transform.scale.y,
                    transform.zIndex, transform.visible, transform.color);
        }
        static WorldTransform root() { return new WorldTransform(0f,0f,0f,1f,1f,0f,true,
                new VfxColor(1f,1f,1f,1f)); }
    }
}

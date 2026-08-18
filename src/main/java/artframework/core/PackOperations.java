package artframework.core;

import artframework.assets.AssetPack;
import artframework.api.ArtFramework;
import artframework.api.WindowDef;
import artframework.component.ComponentRegistry;
import artframework.component.EffectDecl;
import artframework.component.LmlUiNodeLoader;
import artframework.component.UiNode;
import artframework.component.UiNodeLoader;
import artframework.ecs.EcsSystem;
import artframework.ecs.EntityId;

import java.util.List;
import java.util.Map;

/** Built-in reversible operations. Pack declarations cannot inject arbitrary lifecycle callbacks. */
public final class PackOperations {
    private PackOperations() {}

    public static <T> PackOperation createComponent(
            String id, final EntityId entity, final Class<T> type, final T value) {
        return component(id, entity, type, value, false);
    }

    public static <T> PackOperation updateComponent(
            String id, final EntityId entity, final Class<T> type, final T value) {
        return component(id, entity, type, value, true);
    }

    private static <T> PackOperation component(String id, final EntityId entity,
            final Class<T> type, final T value, final boolean replace) {
        if (PackEffectDefaultsComponent.class.equals(type)) {
            throw new IllegalArgumentException(
                    "PackEffectDefaultsComponent requires a PresentPack effectDefaults declaration");
        }
        if (PackSurfaceEffectsComponent.class.equals(type)) {
            throw new IllegalArgumentException(
                    "PackSurfaceEffectsComponent requires a PresentPack surfaceEffects declaration");
        }
        if (PackFullFrameEffectsComponent.class.equals(type)) {
            throw new IllegalArgumentException(
                    "PackFullFrameEffectsComponent requires a PresentPack fullFrameEffects declaration");
        }
        if (PackSurfaceBindingsComponent.class.equals(type)) {
            throw new IllegalArgumentException(
                    "PackSurfaceBindingsComponent requires a PresentPack bindSurfaces declaration");
        }
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(final PackWorld world) {
                final T before = world.entities().get(entity, type);
                if (!replace && before != null) {
                    throw new IllegalStateException(id() + " component already exists: " + type.getName());
                }
                world.entities().put(entity, type, value);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        if (before == null) world.entities().remove(entity, type);
                        else world.entities().put(entity, type, before);
                    }
                };
            }
        };
    }

    public static PackOperation registerAssetPack(String id, final AssetPack pack) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(final PackWorld world) {
                final AssetPack before = world.assets().getPack(pack.id);
                world.assets().registerPack(pack);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        if (before == null) world.assets().unregisterPack(pack.id);
                        else world.assets().registerPack(before);
                    }
                };
            }
        };
    }

    /** Creates one data-only ECS contribution whose values are resolved at C1 materialization. */
    static PackOperation createEffectDefaults(
            String id, final String packId, final Map<String, List<EffectDecl>> defaults) {
        return new EffectDefaultsOperation(id, packId, defaults);
    }

    static final class EffectDefaultsOperation extends PackOperation {
        final String packId;
        private final Map<String, List<EffectDecl>> defaults;

        EffectDefaultsOperation(String id, String packId, Map<String, List<EffectDecl>> defaults) {
            super(id);
            this.packId = packId;
            this.defaults = defaults;
        }

        @Override PackOperation.Undo apply(final PackWorld world) {
            final EntityId entity = world.entities().createEntity();
            world.entities().put(entity, PackEffectDefaultsComponent.class,
                    new PackEffectDefaultsComponent(packId, defaults));
            return new Undo() {
                @Override public void undo(PackWorld ignored) {
                    world.entities().destroyEntity(entity);
                }
            };
        }
    }

    static PackOperation createSurfaceEffects(
            String id, final String packId, final Map<String, List<EffectDecl>> effects) {
        return new SurfaceEffectsOperation(id, packId, effects);
    }

    static final class SurfaceEffectsOperation extends PackOperation {
        final String packId;
        private final Map<String, List<EffectDecl>> effects;

        SurfaceEffectsOperation(String id, String packId, Map<String, List<EffectDecl>> effects) {
            super(id);
            this.packId = packId;
            this.effects = effects;
        }

        @Override PackOperation.Undo apply(final PackWorld world) {
            final EntityId entity = world.entities().createEntity();
            world.entities().put(entity, PackSurfaceEffectsComponent.class,
                    new PackSurfaceEffectsComponent(packId, effects));
            return new Undo() {
                @Override public void undo(PackWorld ignored) {
                    world.entities().destroyEntity(entity);
                }
            };
        }
    }

    static PackOperation createFullFrameEffects(
            String id, final String packId, final List<EffectDecl> effects) {
        return new FullFrameEffectsOperation(id, packId, effects);
    }

    static final class FullFrameEffectsOperation extends PackOperation {
        final String packId;
        private final List<EffectDecl> effects;

        FullFrameEffectsOperation(String id, String packId, List<EffectDecl> effects) {
            super(id);
            this.packId = packId;
            this.effects = effects;
        }

        @Override PackOperation.Undo apply(final PackWorld world) {
            final EntityId entity = world.entities().createEntity();
            world.entities().put(entity, PackFullFrameEffectsComponent.class,
                    new PackFullFrameEffectsComponent(packId, effects));
            return new Undo() {
                @Override public void undo(PackWorld ignored) {
                    world.entities().destroyEntity(entity);
                }
            };
        }
    }

    static PackOperation createSurfaceBindings(
            String id, final String packId, final String profileId, final List<String> surfaceIds) {
        return new SurfaceBindingsOperation(id, packId, profileId, surfaceIds);
    }

    static final class SurfaceBindingsOperation extends PackOperation {
        final String packId;
        private final String profileId;
        private final List<String> surfaceIds;

        SurfaceBindingsOperation(String id, String packId, String profileId, List<String> surfaceIds) {
            super(id);
            this.packId = packId;
            this.profileId = profileId;
            this.surfaceIds = surfaceIds;
        }

        @Override PackOperation.Undo apply(final PackWorld world) {
            final EntityId entity = world.entities().createEntity();
            world.entities().put(entity, PackSurfaceBindingsComponent.class,
                    new PackSurfaceBindingsComponent(packId, profileId, surfaceIds));
            return new Undo() {
                @Override public void undo(PackWorld ignored) {
                    world.entities().destroyEntity(entity);
                }
            };
        }
    }

    public static PackOperation enableSystem(
            String id, final PackSystemPhase phase, final EcsSystem system) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                PackSystems.enable(phase, id(), system);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) { PackSystems.disable(phase, id()); }
                };
            }
        };
    }

    public static PackOperation enableAssetPack(String id, final String assetPackId) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(final PackWorld world) {
                final boolean before = world.assets().config().isPackEnabled(assetPackId);
                world.assets().enablePack(assetPackId, true);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        world.assets().enablePack(assetPackId, before);
                    }
                };
            }
        };
    }

    public static PackOperation registerTemplate(String id, final String name, final UiNode template) {
        return registerTemplate(id, name, template, true);
    }

    static PackOperation registerTemplate(final String id, final String name,
            final UiNode template, final boolean restoreOnDisable) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                final UiNode before = ComponentRegistry.global().get(name);
                ComponentRegistry.global().register(name, template);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        if (!restoreOnDisable && !ignored.restoreRegistrations()) return;
                        if (before == null) ComponentRegistry.global().unregister(name);
                        else ComponentRegistry.global().register(name, before);
                    }
                };
            }
        };
    }

    static PackOperation registerTemplateResource(final String id, final String name,
            final String resource, final boolean restoreOnDisable) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                UiNode template = loadTemplate(resource);
                final UiNode before = ComponentRegistry.global().get(name);
                ComponentRegistry.global().register(name, template);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        if (!restoreOnDisable && !ignored.restoreRegistrations()) return;
                        if (before == null) ComponentRegistry.global().unregister(name);
                        else ComponentRegistry.global().register(name, before);
                    }
                };
            }
        };
    }

    public static PackOperation registerWindow(String id, final WindowDef definition) {
        if (definition == null) throw new IllegalArgumentException("definition required");
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                if (ArtFramework.isRegistered(definition.id)) {
                    throw new IllegalStateException(id() + " window already registered: " + definition.id);
                }
                ArtFramework.register(definition);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        ArtFramework.unregisterWindow(definition.id);
                    }
                };
            }
        };
    }

    static PackOperation registerWindow(final String id, final WindowDef definition,
            final boolean restoreOnDisable) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                final WindowDef before = ArtFramework.registeredWindow(definition.id);
                ArtFramework.register(definition);
                return new Undo() {
                    @Override public void undo(PackWorld ignored) {
                        if (!restoreOnDisable && !ignored.restoreRegistrations()) return;
                        ArtFramework.unregisterWindow(definition.id);
                        if (before != null) ArtFramework.register(before);
                    }
                };
            }
        };
    }

    static PackOperation registerWindowResource(final String id, final String windowId,
            final String resource, final boolean restoreOnDisable) {
        return registerWindow(id, new WindowDef(windowId, artframework.api.WindowClass.SYNTHETIC,
                resource), restoreOnDisable);
    }

    private static UiNode loadTemplate(String resource) {
        if (resource == null || resource.isEmpty()) throw new IllegalArgumentException("layout resource required");
        String lower = resource.toLowerCase();
        if (lower.endsWith(".lml") || lower.endsWith(".xml")) return LmlUiNodeLoader.loadClasspath(resource);
        if (lower.endsWith(".json") || !resource.contains(".")) return UiNodeLoader.loadClasspath(resource);
        throw new IllegalArgumentException("unsupported layout format: " + resource);
    }

    public static PackOperation fail(String id, final String message) {
        return new PackOperation(id) {
            @Override PackOperation.Undo apply(PackWorld world) {
                throw new IllegalStateException(message);
            }
        };
    }
}

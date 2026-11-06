package ua.solly.kito.kaf;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class GeckoLibHandler {
    private final String modId;
    private boolean initialized;

    private GeckoLibHandler(String modId) {
        this.modId = modId;
    }

    public static GeckoLibHandler create(String modId) {
        return new GeckoLibHandler(modId);
    }

    public String modId() {
        return modId;
    }

    public GeckoLibHandler init(IEventBus modBus) {
        if (initialized) {
            return this;
        }
        initialized = true;
        BothBridge.client(() -> () -> ClientHooks.hook(modBus));
        return this;
    }

    public <T extends Entity & GeoAnimatable> GeckoLibHandler entity(
            Supplier<? extends EntityType<? extends T>> type,
            Supplier<GeoModel<T>> model
    ) {
        BothBridge.client(() -> () -> ClientHooks.entity(type, model));
        return this;
    }

    public <T extends Entity & GeoAnimatable> GeckoLibHandler entity(
            Supplier<? extends EntityType<? extends T>> type,
            String modelName
    ) {
        Supplier<GeoModel<T>> model = GeckoLibModels.lazyEntity(modId, modelName);
        return entity(type, model);
    }

    public <T extends BlockEntity & GeoAnimatable> GeckoLibHandler blockEntity(
            Supplier<? extends BlockEntityType<? extends T>> type,
            Supplier<GeoModel<T>> model
    ) {
        BothBridge.client(() -> () -> ClientHooks.blockEntity(type, model));
        return this;
    }

    public <T extends BlockEntity & GeoAnimatable> GeckoLibHandler blockEntity(
            Supplier<? extends BlockEntityType<? extends T>> type,
            String modelName
    ) {
        Supplier<GeoModel<T>> model = GeckoLibModels.lazyBlock(modId, modelName);
        return blockEntity(type, model);
    }

    public static <T extends Item & GeoItem> GeoRenderProvider itemProvider(Supplier<GeoModel<T>> model) {
        return ClientHooks.item(model);
    }

    public <T extends Item & GeoItem> GeoRenderProvider itemProvider(String modelName) {
        Supplier<GeoModel<T>> model = GeckoLibModels.lazyItem(modId, modelName);
        return itemProvider(model);
    }

    public static final class ClientHooks {
        private static final List<Consumer<EntityRenderersEvent.RegisterRenderers>> RENDERERS = new ArrayList<>();
        private static boolean hooked;

        private ClientHooks() {
        }

        static void hook(IEventBus bus) {
            if (hooked) {
                return;
            }
            hooked = true;
            bus.addListener(ClientHooks::onRegisterRenderers);
        }

        static <T extends Entity & GeoAnimatable> void entity(
                Supplier<? extends EntityType<? extends T>> type,
                Supplier<GeoModel<T>> model
        ) {
            RENDERERS.add(event -> event.registerEntityRenderer(
			        type.get(),
			        context -> {
			            GeoEntityRenderer<T> renderer = new GeoEntityRenderer<>(context, model.get());
			            renderer.addRenderLayer(new KitoHeldItemLayer<>(renderer));
			            addArmorLayers(renderer);
			            return renderer;
			        }
			));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void addArmorLayers(GeoEntityRenderer renderer) {
            renderer.addRenderLayer(new KitoArmorLayer((GeoRenderer) renderer, false));
            renderer.addRenderLayer(new KitoArmorLayer((GeoRenderer) renderer, true));
        }

        static <T extends BlockEntity & GeoAnimatable> void blockEntity(
                Supplier<? extends BlockEntityType<? extends T>> type,
                Supplier<GeoModel<T>> model
        ) {
            RENDERERS.add(event -> event.registerBlockEntityRenderer(
                    type.get(),
                    context -> new GeoBlockRenderer<T>(model.get())
            ));
        }

        static <T extends Item & GeoItem> GeoRenderProvider item(Supplier<GeoModel<T>> model) {
            return new GeoRenderProvider() {
                private GeoItemRenderer<T> renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    if (renderer == null) {
                        renderer = new GeoItemRenderer<>(model.get());
                    }
                    return renderer;
                }
            };
        }

        private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            RENDERERS.forEach(consumer -> consumer.accept(event));
        }
    }
}

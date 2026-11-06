package ua.solly.kito.kaf;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;
import java.util.function.Supplier;

public final class GeckoLibModels {
    private GeckoLibModels() {
    }

    public static ResourceLocation id(String modId, String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public static <T extends GeoAnimatable> GeoModel<T> defaultedEntity(String modId, String name) {
        return new DefaultedEntityGeoModel<>(id(modId, name));
    }

    public static <T extends GeoAnimatable> GeoModel<T> defaultedEntity(String modId, String name, boolean turnsHead) {
        return new DefaultedEntityGeoModel<>(id(modId, name), turnsHead);
    }

    public static <T extends GeoAnimatable> GeoModel<T> defaultedItem(String modId, String name) {
        return new DefaultedItemGeoModel<>(id(modId, name));
    }

    public static <T extends GeoAnimatable> GeoModel<T> defaultedBlock(String modId, String name) {
        return new DefaultedBlockGeoModel<>(id(modId, name));
    }

    public static <T extends GeoAnimatable> GeoModel<T> entity(String modId, String name) {
        return KitoModels.entity(modId, name).build();
    }

    public static <T extends GeoAnimatable> GeoModel<T> item(String modId, String name) {
        return KitoModels.item(modId, name).build();
    }

    public static <T extends GeoAnimatable> GeoModel<T> block(String modId, String name) {
        return KitoModels.block(modId, name).build();
    }

    public static <T extends GeoAnimatable> GeoModel<T> entity(String modId, String name, Function<T, ResourceLocation> texture) {
        return KitoModels.entity(modId, name).build(texture);
    }

    public static <T extends GeoAnimatable> GeoModel<T> from(KitoModels.Def def) {
        return def.build();
    }

    public static <T extends GeoAnimatable> Supplier<GeoModel<T>> lazyEntity(String modId, String name) {
        return () -> entity(modId, name);
    }

    public static <T extends GeoAnimatable> Supplier<GeoModel<T>> lazyItem(String modId, String name) {
        return () -> item(modId, name);
    }

    public static <T extends GeoAnimatable> Supplier<GeoModel<T>> lazyBlock(String modId, String name) {
        return () -> block(modId, name);
    }
}

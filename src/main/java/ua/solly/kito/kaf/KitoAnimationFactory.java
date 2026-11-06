package ua.solly.kito.kaf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Supplier;

public final class KitoAnimationFactory {
    private final String modId;
    private final GeckoLibHandler handler;

    private KitoAnimationFactory(String modId) {
        this.modId = modId;
        this.handler = GeckoLibHandler.create(modId);
    }

    public static KitoAnimationFactory create(String modId) {
        return new KitoAnimationFactory(modId);
    }

    public String modId() {
        return modId;
    }

    public GeckoLibHandler handler() {
        return handler;
    }

    public ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public KitoAnimationFactory init(IEventBus modBus) {
        handler.init(modBus);
        return this;
    }

    public KitoModels.Def entityModel(String name) {
        return KitoModels.entity(modId, name);
    }

    public KitoModels.Def itemModel(String name) {
        return KitoModels.item(modId, name);
    }

    public KitoModels.Def blockModel(String name) {
        return KitoModels.block(modId, name);
    }

    public <T extends GeoAnimatable> GeoModel<T> model(KitoModels.Def def) {
        return def.build();
    }

    public <T extends GeoAnimatable> GeoModel<T> model(KitoModels.Def def, KitoAsm asm) {
        return def.build(asm);
    }

    public <T extends Entity & GeoAnimatable> KitoAnimationFactory entity(
            Supplier<? extends EntityType<? extends T>> type,
            String modelName
    ) {
        handler.entity(type, modelName);
        return this;
    }

    public <T extends Entity & GeoAnimatable> KitoAnimationFactory entity(
            Supplier<? extends EntityType<? extends T>> type,
            KitoModels.Def def
    ) {
        Supplier<GeoModel<T>> model = def::build;
        handler.entity(type, model);
        return this;
    }

    public <T extends Entity & GeoAnimatable> KitoAnimationFactory entity(
            Supplier<? extends EntityType<? extends T>> type,
            KitoModels.Def def,
            KitoAsm asm
    ) {
        Supplier<GeoModel<T>> model = () -> def.<T>build(asm);
        handler.entity(type, model);
        return this;
    }

    public <T extends Entity & GeoAnimatable> KitoAnimationFactory entity(
            Supplier<? extends EntityType<? extends T>> type,
            Supplier<GeoModel<T>> model
    ) {
        handler.entity(type, model);
        return this;
    }

    public <T extends BlockEntity & GeoAnimatable> KitoAnimationFactory blockEntity(
            Supplier<? extends BlockEntityType<? extends T>> type,
            String modelName
    ) {
        handler.blockEntity(type, modelName);
        return this;
    }

    public <T extends BlockEntity & GeoAnimatable> KitoAnimationFactory blockEntity(
            Supplier<? extends BlockEntityType<? extends T>> type,
            KitoModels.Def def
    ) {
        Supplier<GeoModel<T>> model = def::build;
        handler.blockEntity(type, model);
        return this;
    }

    public <T extends BlockEntity & GeoAnimatable> KitoAnimationFactory blockEntity(
            Supplier<? extends BlockEntityType<? extends T>> type,
            KitoModels.Def def,
            KitoAsm asm
    ) {
        Supplier<GeoModel<T>> model = () -> def.<T>build(asm);
        handler.blockEntity(type, model);
        return this;
    }

    public <T extends Item & GeoItem> GeoRenderProvider item(String modelName) {
        return handler.itemProvider(modelName);
    }

    public <T extends Item & GeoItem> GeoRenderProvider item(KitoModels.Def def) {
        Supplier<GeoModel<T>> model = def::build;
        return GeckoLibHandler.itemProvider(model);
    }

    public <T extends Item & GeoItem> GeoRenderProvider item(KitoModels.Def def, KitoAsm asm) {
        Supplier<GeoModel<T>> model = () -> def.<T>build(asm);
        return GeckoLibHandler.itemProvider(model);
    }
}

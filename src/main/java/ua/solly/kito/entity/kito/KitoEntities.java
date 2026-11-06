package ua.solly.kito.entity.kito;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ua.solly.kito.KitoMod;

public final class KitoEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, KitoMod.MODID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KitoMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KitoEntity>> KITO = ENTITIES.register(
            "kito",
            () -> EntityType.Builder.of(KitoEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.0F)
                    .clientTrackingRange(10)
                    .build("kito")
    );

    public static final DeferredItem<DeferredSpawnEggItem> KITO_SPAWN_EGG = ITEMS.register(
            "kito_spawn_egg",
            () -> new DeferredSpawnEggItem(KITO, 0xF48FB1, 0xFFE0EC, new Item.Properties())
    );

    private KitoEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(KitoEntities::onAttributes);
        modBus.addListener(KitoEntities::onCreativeTab);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(KITO.get(), KitoEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(KITO_SPAWN_EGG);
        }
    }
}

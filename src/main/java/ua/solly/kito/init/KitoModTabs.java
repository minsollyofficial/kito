package ua.solly.kito.init;

import ua.solly.kito.KitoMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class KitoModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KitoMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KITO_MOD = REGISTRY.register("kito_mod",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.kito.kito_mod")).icon(() -> new ItemStack(KitoModItems.CONTRACEPTIVE.get())).displayItems((parameters, tabData) -> {
				tabData.accept(KitoModItems.CONTRACEPTIVE.get());
			}).build());
}

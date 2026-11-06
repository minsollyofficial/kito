/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package ua.solly.kito.init;

import ua.solly.kito.item.ContraceptiveItem;
import ua.solly.kito.KitoMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

public class KitoModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(KitoMod.MODID);
	public static final DeferredItem<Item> CONTRACEPTIVE;
	static {
		CONTRACEPTIVE = REGISTRY.register("contraceptive", ContraceptiveItem::new);
	}
	// Start of user code block custom items
	// End of user code block custom items
}
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package ua.solly.kito.init;

import ua.solly.kito.KitoMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class KitoModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, KitoMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> MENU_SCROLL = REGISTRY.register("menu_scroll", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kito", "menu_scroll")));
	public static final DeferredHolder<SoundEvent, SoundEvent> CARESS = REGISTRY.register("caress", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kito", "caress")));
	public static final DeferredHolder<SoundEvent, SoundEvent> MMM = REGISTRY.register("mmm", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kito", "mmm")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SLIDE = REGISTRY.register("slide", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kito", "slide")));
	public static final DeferredHolder<SoundEvent, SoundEvent> AHH = REGISTRY.register("ahh", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("kito", "ahh")));
}
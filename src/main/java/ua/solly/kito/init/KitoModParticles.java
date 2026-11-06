package ua.solly.kito.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ua.solly.kito.KitoMod;

public class KitoModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, KitoMod.MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HEARTS = REGISTRY.register("hearts", () -> new SimpleParticleType(false));
}

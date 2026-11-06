package ua.solly.kito.particle;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import ua.solly.kito.KitoMod;
import ua.solly.kito.init.KitoModParticles;

@EventBusSubscriber(modid = KitoMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KitoParticleClient {
    private KitoParticleClient() {
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KitoModParticles.HEARTS.get(), HeartParticle.Provider::new);
    }
}

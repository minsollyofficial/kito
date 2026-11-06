package ua.solly.kito.entity.interact.kiss;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class KitoKissClient {
    private static boolean active;
    private static float lockYaw;
    private static float lockPitch;

    private KitoKissClient() {
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean wasActive = active;
        active = false;
        if (player == null || mc.level == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof KitoCharacter kito && kito.isKissingFor(player.getUUID())) {
                if (!wasActive) {
                    lockYaw = player.getYRot();
                    lockPitch = player.getXRot();
                }
                active = true;
                player.setSprinting(false);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (active && player != null) {
            player.setYRot(lockYaw);
            player.setXRot(lockPitch);
            player.yRotO = lockYaw;
            player.xRotO = lockPitch;
        }
    }

    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (!active) {
            return;
        }
        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (active && (event.isUseItem() || event.isAttack())) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!active) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        event.setYaw(event.getYaw() - Mth.wrapDegrees(player.getViewYRot(partial) - lockYaw));
        event.setPitch(event.getPitch() - (player.getViewXRot(partial) - lockPitch));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        lockYaw = 0.0F;
        lockPitch = 0.0F;
    }
}

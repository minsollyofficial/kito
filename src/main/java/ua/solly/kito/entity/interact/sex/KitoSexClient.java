package ua.solly.kito.entity.interact.sex;

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
import net.neoforged.neoforge.network.PacketDistributor;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class KitoSexClient {
    private static final int AGE_LIMIT = 60;

    private static boolean active;
    private static boolean engaged;
    private static int targetId = -1;
    private static int thrustsLeft;
    private static float lockYaw;
    private static float lockPitch;
    private static boolean spaceDown;
    private static int thrustAge = -1;

    private KitoSexClient() {
    }

    public static void onState(boolean on, int entityId, int left) {
        if (!on) {
            clear();
            return;
        }
        boolean thrust = active && left < thrustsLeft;
        active = true;
        engaged = true;
        targetId = entityId;
        thrustsLeft = left;
        if (thrust) {
            thrustAge = 0;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lockYaw = mc.player.getYRot();
            lockPitch = mc.player.getXRot();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isEngaged() {
        return engaged;
    }

    public static int thrustsLeft() {
        return thrustsLeft;
    }

    public static int targetId() {
        return targetId;
    }

    private static void clear() {
        thrustAge = -1;
        active = false;
        engaged = false;
        targetId = -1;
        thrustsLeft = 0;
        spaceDown = false;
    }

    private static void drain(LocalPlayer player) {
        player.setYRot(lockYaw);
        player.setXRot(lockPitch);
        player.yRotO = lockYaw;
        player.xRotO = lockPitch;
    }

    public static float thrustAge(float partial) {
        return thrustAge < 0 ? -1.0F : thrustAge + partial;
    }

    public static float stroke(float age) {
        if (age < 0.0F) {
            return 0.0F;
        }
        if (age < 2.5F) {
            float t = age / 2.5F;
            return Mth.sin(t * Mth.HALF_PI);
        }
        if (age < 16.0F) {
            float t = (age - 2.5F) / 13.5F;
            float e = t * t * (3.0F - 2.0F * t);
            return 1.0F - e;
        }
        return 0.0F;
    }

    public static float tremor(float age) {
        if (age < 2.5F || age > 24.0F) {
            return 0.0F;
        }
        float t = age - 2.5F;
        return Mth.sin(t * 1.6F) * (float) Math.exp(-t * 0.28D);
    }

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && active) {
            drain(player);
        }
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clear();
            return;
        }
        if (!active) {
            return;
        }
        Entity target = mc.level.getEntity(targetId);
        if (!(target instanceof KitoCharacter) || !target.isAlive()) {
            clear();
            return;
        }
        player.setSprinting(false);
        if (thrustAge >= 0) {
            thrustAge++;
            if (thrustAge > AGE_LIMIT) {
                thrustAge = -1;
            }
        }
        boolean down = mc.options.keyJump.isDown();
        if (down && !spaceDown) {
            PacketDistributor.sendToServer(new KitoSexNet.Thrust());
        }
        spaceDown = down;
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
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float yawOffset = Mth.wrapDegrees(player.getViewYRot(partial) - lockYaw);
        float pitchOffset = player.getViewXRot(partial) - lockPitch;
        float age = thrustAge(partial);
        float shake = tremor(age) * 0.25F + stroke(age) * 0.5F;
        event.setYaw(event.getYaw() - yawOffset);
        event.setPitch(event.getPitch() - pitchOffset + shake);
        event.setRoll(event.getRoll() + tremor(age) * 0.12F);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active) {
            return;
        }
        if (event.isUseItem() || event.isAttack()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}

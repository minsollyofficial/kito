package ua.solly.kito.pp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;

final class KissPoser implements PoseProvider {
    @Override
    public void apply(AbstractClientPlayer player, float partial, PlayerPose pose) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null) {
            return;
        }
        KitoCharacter target = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof KitoCharacter kito && kito.isKissingFor(player.getUUID())) {
                target = kito;
                break;
            }
        }
        if (target == null) {
            return;
        }
        Vec3 head = target.getPosition(partial).add(0.0D, target.getBbHeight() * 0.79D, 0.0D);
        float yaw = Mth.rotLerp(partial, target.yBodyRotO, target.yBodyRot);
        double rad = Math.toRadians(yaw);
        Vec3 right = new Vec3(Math.cos(rad), 0.0D, Math.sin(rad));
        pose.right.set(head.x + right.x * 0.16D, head.y, head.z + right.z * 0.16D);
        pose.left.set(head.x - right.x * 0.16D, head.y, head.z - right.z * 0.16D);
        pose.active = true;
        pose.lean = 0.07F;
        pose.headPitch = 0.08F;
    }
}

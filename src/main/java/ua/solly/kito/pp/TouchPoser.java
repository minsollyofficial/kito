package ua.solly.kito.pp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.tactility.EntityMover;
import ua.solly.kito.entity.interact.tactility.TactilityAction;
import ua.solly.kito.entity.interact.tactility.TactilityScene;

final class TouchPoser implements PoseProvider {
    private static final float DRAG_SCALE = 0.22F;
    private static final float LEAN = 0.12F;

    @Override
    public void apply(AbstractClientPlayer player, float partial, PlayerPose pose) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null || !TactilityScene.Client.isActive()) {
            return;
        }
        TactilityAction held = TactilityScene.Client.held();
        if (held == null) {
            return;
        }
        Entity entity = mc.level.getEntity(TactilityScene.Client.targetId());
        if (!(entity instanceof KitoCharacter kito)) {
            return;
        }

        Vec3 center = kito.getPosition(partial);
        Vec3 toPlayer = new Vec3(player.getX() - center.x, 0.0D, player.getZ() - center.z);
        double len = toPlayer.length();
        double bodyYaw = Math.toRadians(Mth.rotLerp(partial, kito.yBodyRotO, kito.yBodyRot));
        Vec3 left = new Vec3(Math.cos(bodyYaw), 0.0D, Math.sin(bodyYaw));
        Vec3 forward = new Vec3(-Math.sin(bodyYaw), 0.0D, Math.cos(bodyYaw));
        toPlayer = len < 1.0E-3D ? forward : toPlayer.scale(1.0D / len);

        double side = side(held);
        double sideOffset = sideOffset(held);
        double surface = held == TactilityAction.CROWN ? 0.0D : kito.getBbWidth() * 0.5D + 0.03D;
        double height = kito.getBbHeight() * held.height();

        Vec3 target = new Vec3(center.x, center.y + height, center.z)
                .add(toPlayer.scale(surface))
                .add(left.scale(side * sideOffset));

        EntityMover.Pose touch = kito.pose();
        float blend = touch.blend(partial);
        double px = touch.x(partial) * blend;
        double py = touch.y(partial) * blend;
        double pz = touch.z(partial) * blend;
        target = target.add(left.scale(-px * DRAG_SCALE))
                .add(0.0D, -py * DRAG_SCALE, 0.0D)
                .add(forward.scale(-pz * DRAG_SCALE));

        pose.right.set(target.x, target.y, target.z);
        pose.active = true;
        pose.lean = LEAN;
        pose.headPitch = -LEAN * 0.6F;
    }

    private static double side(TactilityAction action) {
        return switch (action) {
            case CHEEK_LEFT, ARM_LEFT, LEG_LEFT, CHEST_LEFT -> 1.0D;
            case CHEEK_RIGHT, ARM_RIGHT, LEG_RIGHT, CHEST_RIGHT -> -1.0D;
            default -> 0.0D;
        };
    }

    private static double sideOffset(TactilityAction action) {
        return switch (action) {
            case ARM_LEFT, ARM_RIGHT -> 0.34D;
            case CHEST_LEFT, CHEST_RIGHT -> 0.13D;
            case CHEEK_LEFT, CHEEK_RIGHT -> 0.12D;
            case LEG_LEFT, LEG_RIGHT -> 0.14D;
            default -> 0.0D;
        };
    }
}

package ua.solly.kito.pp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.sex.KitoSexClient;
import ua.solly.kito.kaf.KitoSexMotion;

final class SexPoser implements PoseProvider {
    private static final float BASE_LEAN = 10.0F;
    private static final float THRUST_LEAN = 5.0F;
    private static final float BASE_PUSH = 0.015F;
    private static final float THRUST_PUSH = 0.07F;
    private static final float TREMOR_LEAN = 0.5F;
    private static final double GRIP_SIDE = 0.26D;
    private static final double GRIP_HEIGHT = 0.82D;
    private static final double GRIP_BACK = 0.04D;
    private static final double GRIP_DROP = 0.03D;
    private static final double BWJB_SIDE = 0.27D;
    private static final double BWJB_BASE_FORWARD = 2.0D;
    private static final double BWJB_SIT_HEIGHT = 2.0D;
    private static final double BWJB_CHEEK_DROP = 0.03D;

    @Override
    public void apply(AbstractClientPlayer player, float partial, PlayerPose pose) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null || !KitoSexClient.isActive() || !KitoSexClient.isEngaged()) {
            return;
        }
        Entity entity = mc.level.getEntity(KitoSexClient.targetId());
        if (!(entity instanceof KitoCharacter kito)) {
            return;
        }

        if ("bwjb".equals(kito.sexAnimId())) {
            Vec3 center = kito.getPosition(partial);
            double dx = player.getX() - center.x;
            double dz = player.getZ() - center.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            double fx = len < 1.0E-3D ? 0.0D : dx / len;
            double fz = len < 1.0E-3D ? 1.0D : dz / len;
            double sx = fz;
            double sz = -fx;
            float age = KitoSexClient.thrustAge(partial);
            float phase = KitoSexMotion.bwjbPhase(age / 20.0F);
            double lean = Math.toRadians(KitoSexMotion.BWJB_LEAN_MAX * phase);
            double forward = (BWJB_BASE_FORWARD + KitoSexMotion.BWJB_PUSH * phase + 16.0D * Math.sin(lean)) / 16.0D;
            double height = (BWJB_SIT_HEIGHT + 16.0D * Math.cos(lean)) / 16.0D - BWJB_CHEEK_DROP;
            double hx = center.x + fx * forward;
            double hy = center.y + height;
            double hz = center.z + fz * forward;
            pose.right.set(hx + sx * BWJB_SIDE, hy, hz + sz * BWJB_SIDE);
            pose.left.set(hx - sx * BWJB_SIDE, hy, hz - sz * BWJB_SIDE);
            pose.active = true;
            pose.lean = 0.1F + 0.04F * phase;
            pose.headPitch = 0.1F;
            return;
        }

        float age = KitoSexClient.thrustAge(partial);
        float stroke = KitoSexClient.stroke(age);
        float tremor = KitoSexClient.tremor(age);

        Vec3 center = kito.getPosition(partial);
        double dx = center.x - player.getX();
        double dz = center.z - player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-3D) {
            return;
        }
        double fx = dx / len;
        double fz = dz / len;
        double rx = -fz;
        double rz = fx;

        double hx = center.x - fx * GRIP_BACK;
        double hy = center.y + GRIP_HEIGHT - GRIP_DROP * stroke;
        double hz = center.z - fz * GRIP_BACK;
        pose.right.set(hx + rx * GRIP_SIDE, hy, hz + rz * GRIP_SIDE);
        pose.left.set(hx - rx * GRIP_SIDE, hy, hz - rz * GRIP_SIDE);

        float lean = BASE_LEAN + THRUST_LEAN * stroke + TREMOR_LEAN * tremor;
        pose.active = true;
        pose.lean = lean * Mth.DEG_TO_RAD;
        pose.hipPush = BASE_PUSH + THRUST_PUSH * stroke + 0.004F * tremor;
        pose.headPitch = -pose.lean * 0.75F;
    }
}

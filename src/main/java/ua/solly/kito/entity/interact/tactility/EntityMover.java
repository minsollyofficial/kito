package ua.solly.kito.entity.interact.tactility;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.kaf.KitoAsm;

import java.util.UUID;

public final class EntityMover {
    private static final double TOUCH_DISTANCE = 1.0D;
    private static final double APPROACH_SPEED = 0.14D;
    private static final float ROLL = 1.0F;
    private static final float PITCH = 1.0F;

    private static final float FACE_TOLERANCE = 25.0F;
    private static final float FACE_BACK_LIMIT = 100.0F;
    private static final float TURN_SPEED = 12.0F;

    private static final float BLEND_RATE = 0.25F;
    private static final float POSE_RATE = 0.35F;

    private static final float HEAD_ROLL = 0.22F;
    private static final float HEAD_PITCH = 0.22F;
    private static final float HEAD_PUSH = 0.12F;
    private static final float ARM_SWING = 0.35F;
    private static final float ARM_LIFT = 0.25F;
    private static final float ARM_SIDE = 0.20F;
    private static final float LEG_SWING = 0.18F;
    private static final float LEG_SIDE = 0.10F;
    private static final float BODY_ROLL = 0.07F;
    private static final float BODY_PITCH = 0.07F;
    private static final float BODY_PUSH = 0.05F;
    private static final float HEAD_COUNTER = -0.35F;
    private static final float WAIST_SCALE = 0.6F;
    private static final float BREAST_PITCH = 0.40F;
    private static final float BREAST_ROLL = 0.28F;
    private static final float BREAST_PUSH = 0.30F;
    private static final float SHY_LEGS = 0.14F;
    private static final float SHY_HEAD = 0.32F;
    private static final float SHY_BODY = 0.06F;
    private static final float SHY_ARM_DOWN = 0.85F;
    private static final float SHY_ARM_IN = 0.55F;
    private static final float SHY_ARM_TWIST = 0.25F;

    private static final float CROWN_ARM_LIFT = 2.3F;
    private static final float CROWN_ARM_IN = 0.45F;
    private static final float CROWN_NOD = 0.12F;
    private static final float CROWN_TILT = 0.08F;

    private EntityMover() {
    }

    public static final class Pose {
        private TactilityAction action;
        private float x;
        private float y;
        private float z;
        private float blend;
        private float ox;
        private float oy;
        private float oz;
        private float oblend;

        public void tick(int part, Vector3f target) {
            ox = x;
            oy = y;
            oz = z;
            oblend = blend;
            TactilityAction next = TactilityAction.byOrdinal(part);
            if (next != null) {
                action = next;
            }
            blend += ((next == null ? 0.0F : 1.0F) - blend) * BLEND_RATE;
            x += (target.x - x) * POSE_RATE;
            y += (target.y - y) * POSE_RATE;
            z += (target.z - z) * POSE_RATE;
            if (next == null && blend < 0.01F) {
                action = null;
                blend = 0.0F;
                x = 0.0F;
                y = 0.0F;
                z = 0.0F;
                ox = 0.0F;
                oy = 0.0F;
                oz = 0.0F;
                oblend = 0.0F;
            }
        }

        public TactilityAction action() {
            return action;
        }

        public float blend(float partialTick) {
            return Mth.lerp(partialTick, oblend, blend);
        }

        public float x(float partialTick) {
            return Mth.lerp(partialTick, ox, x);
        }

        public float y(float partialTick) {
            return Mth.lerp(partialTick, oy, y);
        }

        public float z(float partialTick) {
            return Mth.lerp(partialTick, oz, z);
        }
    }

    public static void hold(KitoCharacter kito) {
        kito.getNavigation().stop();
        kito.getMoveControl().setWantedPosition(kito.getX(), kito.getY(), kito.getZ(), 0.0D);
        kito.setZza(0.0F);
        kito.setXxa(0.0F);
        kito.setJumping(false);
        kito.setSprinting(false);
        kito.setTarget(null);
        Vec3 motion = kito.getDeltaMovement();
        kito.setDeltaMovement(0.0D, motion.y, 0.0D);
        UUID id = kito.holdUuid();
        Player player = id == null ? null : kito.level().getPlayerByUUID(id);
        if (player != null) {
            face(kito, player);
            approach(kito, player);
        }
    }

    private static void approach(KitoCharacter kito, Player player) {
        if (!kito.isInScene()) {
            return;
        }
        double dx = player.getX() - kito.getX();
        double dz = player.getZ() - kito.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= TOUCH_DISTANCE) {
            return;
        }
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float turned = Mth.approachDegrees(kito.yBodyRot, wanted, TURN_SPEED * 2.0F);
        kito.setYRot(turned);
        kito.yBodyRot = turned;
        double step = Math.min(APPROACH_SPEED, dist - TOUCH_DISTANCE);
        Vec3 motion = kito.getDeltaMovement();
        kito.setDeltaMovement(dx / dist * step, motion.y, dz / dist * step);
    }

    private static void face(KitoCharacter kito, Player player) {
        kito.getLookControl().setLookAt(player.getX(), player.getEyeY(), player.getZ());
        if (!kito.isInScene()) {
            return;
        }
        double dx = player.getX() - kito.getX();
        double dz = player.getZ() - kito.getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float diff = Mth.wrapDegrees(wanted - kito.yBodyRot);
        float abs = Math.abs(diff);
        if (abs > FACE_BACK_LIMIT || abs <= FACE_TOLERANCE) {
            return;
        }
        float yaw = Mth.approachDegrees(kito.yBodyRot, wanted, TURN_SPEED);
        kito.setYRot(yaw);
        kito.yBodyRot = yaw;
    }

    public static Vector3f toLocal(Player player, KitoCharacter kito, float right, float up) {
        double playerYaw = player.getYRot() * Mth.DEG_TO_RAD;
        double bodyYaw = kito.yBodyRot * Mth.DEG_TO_RAD;
        double rightX = -Math.cos(playerYaw);
        double rightZ = -Math.sin(playerYaw);
        double leftX = Math.cos(bodyYaw);
        double leftZ = Math.sin(bodyYaw);
        double forwardX = -Math.sin(bodyYaw);
        double forwardZ = Math.cos(bodyYaw);
        float left = (float) ((rightX * leftX + rightZ * leftZ) * right);
        float forward = (float) ((rightX * forwardX + rightZ * forwardZ) * right);
        return new Vector3f(left, up, forward);
    }

    public static void sync(KitoCharacter kito, TactilityAction held, Vector3f pose) {
        kito.setTouch(held == null ? -1 : held.ordinal(), pose);
    }

    public static <T extends GeoAnimatable> void apply(KitoAsm asm, GeoModel<T> model, KitoCharacter kito, float partialTick) {
        Pose pose = kito.pose();
        TactilityAction action = pose.action();
        if (action == null) {
            return;
        }
        float blend = pose.blend(partialTick);
        float x = pose.x(partialTick) * blend;
        float y = pose.y(partialTick) * blend;
        float z = pose.z(partialTick) * blend;
        switch (action) {
            case CROWN -> crown(asm, model, x, y, z, blend);
            case HEAD, CHEEK_LEFT, CHEEK_RIGHT -> head(asm, model, x, y, z);
            case ARM_LEFT -> arm(asm, model, KitoAsm.LEFT_ARM, x, y, z);
            case ARM_RIGHT -> arm(asm, model, KitoAsm.RIGHT_ARM, x, y, z);
            case LEG_LEFT -> leg(asm, model, KitoAsm.LEFT_LEG, x, z);
            case LEG_RIGHT -> leg(asm, model, KitoAsm.RIGHT_LEG, x, z);
            case BACK, BELLY -> torso(asm, model, x, y, z);
            case CHEST_LEFT -> breast(asm, model, KitoAsm.LEFT_BREAST, x, y, z);
            case CHEST_RIGHT -> breast(asm, model, KitoAsm.RIGHT_BREAST, x, y, z);
            case GROIN -> shy(asm, model, blend);
        }
    }

    private static <T extends GeoAnimatable> void crown(KitoAsm asm, GeoModel<T> model, float x, float y, float z, float blend) {
        rotate(asm, model, KitoAsm.HEAD,
                PITCH * (-CROWN_NOD * blend - y * HEAD_PITCH + z * HEAD_PUSH),
                0.0F,
                ROLL * (x * HEAD_ROLL + CROWN_TILT * blend));
        rotate(asm, model, KitoAsm.RIGHT_ARM, CROWN_ARM_LIFT * blend, 0.0F, CROWN_ARM_IN * blend);
        rotate(asm, model, KitoAsm.LEFT_ARM, CROWN_ARM_LIFT * blend, 0.0F, -CROWN_ARM_IN * blend);
    }

    private static <T extends GeoAnimatable> void head(KitoAsm asm, GeoModel<T> model, float x, float y, float z) {
        rotate(asm, model, KitoAsm.HEAD,
                PITCH * (-y * HEAD_PITCH + z * HEAD_PUSH),
                0.0F,
                ROLL * x * HEAD_ROLL);
    }

    private static <T extends GeoAnimatable> void arm(KitoAsm asm, GeoModel<T> model, String bone, float x, float y, float z) {
        rotate(asm, model, bone,
                PITCH * (z * ARM_SWING - y * ARM_LIFT),
                0.0F,
                ROLL * x * ARM_SIDE);
    }

    private static <T extends GeoAnimatable> void leg(KitoAsm asm, GeoModel<T> model, String bone, float x, float z) {
        rotate(asm, model, bone,
                PITCH * z * LEG_SWING,
                0.0F,
                ROLL * x * LEG_SIDE);
    }

    private static <T extends GeoAnimatable> void torso(KitoAsm asm, GeoModel<T> model, float x, float y, float z) {
        float pitch = PITCH * (-y * BODY_PITCH + z * BODY_PUSH) * WAIST_SCALE;
        float roll = ROLL * x * BODY_ROLL * WAIST_SCALE;

        rotate(asm, model, KitoAsm.WAIST, pitch, 0.0F, roll);
        rotate(asm, model, KitoAsm.HEAD, pitch * HEAD_COUNTER, 0.0F, roll * HEAD_COUNTER);
    }

    private static <T extends GeoAnimatable> void breast(KitoAsm asm, GeoModel<T> model, String bone, float x, float y, float z) {

        rotate(asm, model, bone,
                PITCH * (-y * BREAST_PITCH + z * BREAST_PUSH),
                0.0F,
                ROLL * x * BREAST_ROLL);
    }

    private static <T extends GeoAnimatable> void shy(KitoAsm asm, GeoModel<T> model, float blend) {
        rotate(asm, model, KitoAsm.LEFT_LEG, 0.0F, 0.0F, -ROLL * SHY_LEGS * blend);
        rotate(asm, model, KitoAsm.RIGHT_LEG, 0.0F, 0.0F, ROLL * SHY_LEGS * blend);
        rotate(asm, model, KitoAsm.HEAD, PITCH * SHY_HEAD * blend, 0.0F, 0.0F);
        rotate(asm, model, KitoAsm.WAIST, PITCH * SHY_BODY * blend, 0.0F, 0.0F);
        rotate(asm, model, KitoAsm.RIGHT_ARM,
                PITCH * SHY_ARM_DOWN * blend,
                0.0F,
                -ROLL * SHY_ARM_IN * blend);
        rotate(asm, model, KitoAsm.LEFT_ARM,
                PITCH * SHY_ARM_DOWN * blend,
                0.0F,
                ROLL * SHY_ARM_IN * blend);
    }

    private static <T extends GeoAnimatable> void rotate(KitoAsm asm, GeoModel<T> model, String logical, float rx, float ry, float rz) {
        asm.addRotation(logical, rx, ry, rz);
    }
}

package ua.solly.kito.entity.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;

import java.util.EnumSet;

public class KitoStayNearHomeGoal extends Goal {
    public static final double RADIUS = 25.0D;
    public static final double LEASH_SQR = RADIUS * RADIUS;
    public static final double SPEED = 1.0D;

    private final KitoCharacter kito;
    private Vec3 target;
    private int recalc;

    public KitoStayNearHomeGoal(KitoCharacter kito) {
        this.kito = kito;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!kito.home().has()) {
            return false;
        }
        if (kito.followUuid() != null) {
            return false;
        }
        if (kito.getTarget() != null || kito.tactileWith() != null || kito.isHeld()) {
            return false;
        }
        BlockPos home = kito.home().pos();
        if (home == null) {
            return false;
        }
        double dist = kito.distanceToSqr(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D);
        if (dist <= LEASH_SQR * 0.6D) {
            return false;
        }
        target = DefaultRandomPos.getPosTowards(kito, 16, 7,
                new Vec3(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D), Math.PI / 2.0D);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!kito.home().has() || kito.followUuid() != null || kito.getTarget() != null || kito.tactileWith() != null) {
            return false;
        }
        BlockPos home = kito.home().pos();
        if (home == null) {
            return false;
        }
        return kito.distanceToSqr(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D) > LEASH_SQR * 0.6D
                && !kito.getNavigation().isDone();
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        target = null;
        kito.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--recalc <= 0 || target == null) {
            recalc = reducedTickDelay(40);
            if (kito.home().has()) {
                BlockPos home = kito.home().pos();
                if (home != null) {
                    target = DefaultRandomPos.getPosTowards(kito, 16, 7,
                            new Vec3(home.getX() + 0.5D, home.getY() + 0.5D, home.getZ() + 0.5D), Math.PI / 2.0D);
                    if (target != null) {
                        kito.getNavigation().moveTo(target.x, target.y, target.z, SPEED);
                    }
                }
            }
        }
    }
}

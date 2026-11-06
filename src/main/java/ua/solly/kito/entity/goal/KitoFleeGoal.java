package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;

import java.util.EnumSet;

public class KitoFleeGoal extends Goal {
    private static final float HP_THRESHOLD = 0.25F;
    private static final double SPEED = 1.25D;

    private final KitoCharacter kito;
    private LivingEntity threat;
    private Vec3 fleePos;
    private int recalc;

    public KitoFleeGoal(KitoCharacter kito) {
        this.kito = kito;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (kito.getHealth() > kito.getMaxHealth() * HP_THRESHOLD) {
            return false;
        }
        LivingEntity attacker = kito.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) {
            return false;
        }
        threat = attacker;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return threat != null
                && threat.isAlive()
                && kito.getHealth() <= kito.getMaxHealth() * HP_THRESHOLD
                && kito.distanceToSqr(threat) < 256.0D;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        threat = null;
        fleePos = null;
        kito.getNavigation().stop();
    }

    @Override
    public void tick() {
        kito.getLookControl().setLookAt(threat, 10.0F, kito.getMaxHeadXRot());
        if (--recalc <= 0 || fleePos == null) {
            recalc = reducedTickDelay(20);
            fleePos = DefaultRandomPos.getPosAway(kito, 16, 7, threat.position());
            if (fleePos != null) {
                kito.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, SPEED);
            }
        }
    }
}

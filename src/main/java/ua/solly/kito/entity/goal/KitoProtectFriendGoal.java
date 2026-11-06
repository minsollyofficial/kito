package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import ua.solly.kito.entity.KitoCharacter;

import java.util.EnumSet;

public class KitoProtectFriendGoal extends Goal {
    private static final double SPEED = 1.2D;
    private static final double RANGE_SQR = 16.0D * 16.0D;

    private final KitoCharacter kito;
    private LivingEntity attacker;

    public KitoProtectFriendGoal(KitoCharacter kito) {
        this.kito = kito;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (kito.getHealth() <= kito.getMaxHealth() * 0.25F) {
            return false;
        }
        Player friend = kito.followPlayer();
        if (friend == null || !friend.isAlive()) {
            return false;
        }
        LivingEntity lastHurt = friend.getLastHurtByMob();
        if (lastHurt == null || !lastHurt.isAlive() || lastHurt == kito) {
            return false;
        }
        if (kito.distanceToSqr(lastHurt) > RANGE_SQR) {
            return false;
        }
        attacker = lastHurt;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return attacker != null
                && attacker.isAlive()
                && kito.getHealth() > kito.getMaxHealth() * 0.25F
                && kito.followPlayer() != null;
    }

    @Override
    public void start() {
        kito.setTarget(attacker);
    }

    @Override
    public void stop() {
        attacker = null;
        kito.setTarget(null);
        kito.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (attacker != null) {
            kito.setTarget(attacker);
            kito.getLookControl().setLookAt(attacker, 10.0F, kito.getMaxHeadXRot());
            if (kito.distanceToSqr(attacker) > 4.0D) {
                kito.getNavigation().moveTo(attacker, SPEED);
            }
        }
    }
}

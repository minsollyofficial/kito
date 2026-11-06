package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import ua.solly.kito.entity.KitoCharacter;

import java.util.EnumSet;

public class KitoFollowFriendGoal extends Goal {
    private static final double RUN_SPEED_MULT = 1.5D;

    private final KitoCharacter kito;
    private final double baseSpeed;
    private final double startDistanceSqr;
    private final double stopDistanceSqr;
    private Player friend;
    private int recalc;

    public KitoFollowFriendGoal(KitoCharacter kito, double speed, float startDistance, float stopDistance) {
        this.kito = kito;
        this.baseSpeed = speed;
        this.startDistanceSqr = startDistance * startDistance;
        this.stopDistanceSqr = stopDistance * stopDistance;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (kito.getTarget() != null || kito.tactileWith() != null) {
            return false;
        }
        Player player = kito.followPlayer();
        if (player == null || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        if (kito.distanceToSqr(player) < startDistanceSqr) {
            return false;
        }
        friend = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return friend != null
                && friend.isAlive()
                && kito.getTarget() == null
                && kito.tactileWith() == null
                && kito.isFollowedBy(friend)
                && kito.distanceToSqr(friend) > stopDistanceSqr;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        friend = null;
        kito.getNavigation().stop();
    }

    @Override
    public void tick() {
        kito.getLookControl().setLookAt(friend, 10.0F, kito.getMaxHeadXRot());
        if (--recalc <= 0) {
            recalc = reducedTickDelay(10);
            double speed = kito.isRunning() ? baseSpeed * RUN_SPEED_MULT : baseSpeed;
            kito.getNavigation().moveTo(friend, speed);
        }
    }
}

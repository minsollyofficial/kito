package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.ai.goal.Goal;
import ua.solly.kito.entity.KitoCharacter;

import java.util.EnumSet;

public class KitoPoseGoal extends Goal {
    private final KitoCharacter kito;

    public KitoPoseGoal(KitoCharacter kito) {
        this.kito = kito;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return kito.sexPose().isActive();
    }

    @Override
    public boolean canContinueToUse() {
        return kito.sexPose().isActive();
    }

    @Override
    public void stop() {
        kito.getNavigation().stop();
    }

    @Override
    public void tick() {
    }
}

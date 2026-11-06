package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import ua.solly.kito.entity.KitoCharacter;

public class KitoRetaliateGoal extends HurtByTargetGoal {
    private final KitoCharacter kito;

    public KitoRetaliateGoal(KitoCharacter kito) {
        super(kito);
        this.kito = kito;
    }

    @Override
    public boolean canUse() {
        LivingEntity attacker = kito.getLastHurtByMob();
        if (attacker instanceof Player player && !kito.shouldRetaliateAgainst(player)) {
            return false;
        }
        return super.canUse();
    }
}

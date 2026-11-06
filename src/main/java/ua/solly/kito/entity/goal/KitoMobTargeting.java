package ua.solly.kito.entity.goal;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;

@EventBusSubscriber(modid = KitoMod.MODID)
public final class KitoMobTargeting {
    private KitoMobTargeting() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob instanceof Enemy)
                || mob instanceof NeutralMob
                || mob instanceof KitoCharacter) {
            return;
        }
        for (WrappedGoal wrapped : mob.targetSelector.getAvailableGoals()) {
            if (wrapped.getGoal() instanceof HostileTargetGoal) {
                return;
            }
        }
        mob.targetSelector.addGoal(3, new HostileTargetGoal(mob));
    }

    private static final class HostileTargetGoal extends NearestAttackableTargetGoal<KitoCharacter> {
        HostileTargetGoal(Mob mob) {
            super(mob, KitoCharacter.class, true);
        }
    }
}

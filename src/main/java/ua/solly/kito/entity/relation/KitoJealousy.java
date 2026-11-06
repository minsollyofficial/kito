package ua.solly.kito.entity.relation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.tactility.TactilityLib;

import java.util.List;

public final class KitoJealousy {
    public static final double RADIUS = 16.0D;
    public static final float MIN_TRUST = 50.0F;
    public static final float DISGUST_PER_JEALOUSY = 2.0F;

    private KitoJealousy() {
    }

    public static void notifyAttention(KitoCharacter source, Player player) {
        Level level = source.level();
        if (level.isClientSide()) {
            return;
        }
        List<KitoCharacter> others = level.getEntitiesOfClass(
                KitoCharacter.class,
                source.getBoundingBox().inflate(RADIUS),
                k -> k != source && k.isAlive()
        );
        for (KitoCharacter other : others) {
            if (other.peekRelation(player).trust() < MIN_TRUST) {
                continue;
            }
            if (other.isFollowedBy(player) || other.isHoldingPlayer(player)) {
                continue;
            }
            other.moodEvent(MoodEvent.JEALOUS);
            other.relation(player).addDisgust(DISGUST_PER_JEALOUSY);
            if (player instanceof ServerPlayer sp) {
                TactilityLib.say(other, sp, "kito.mood.jealous");
            }
        }
    }
}

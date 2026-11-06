package ua.solly.kito.entity.interact;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.games.KitoGames;
import ua.solly.kito.entity.interact.sex.KitoSexScene;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.relation.MoodEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KitoKiss {
    private static final int APPROACH_TIMEOUT = 600;
    private static final int KISS_TICKS = 20;
    private static final double START_DISTANCE_SQR = 1.0D;
    private static final double WALK_SPEED = 1.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private KitoKiss() {
    }

    private static final class Session {
        private final KitoCharacter kito;
        private final ServerPlayer player;
        private int ticks;
        private boolean engaged;

        private Session(KitoCharacter kito, ServerPlayer player) {
            this.kito = kito;
            this.player = player;
        }
    }

    public static boolean start(KitoCharacter kito, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || kito.level().isClientSide()) {
            return false;
        }
        if (kito.peekRelation(player).trust() < 70.0F) {
            TactilityLib.say(kito, serverPlayer, "kito.menu.deny.kiss_trust");
            return false;
        }
        if (SESSIONS.containsKey(kito.getUUID()) || kito.sexPose().isActive()
                || KitoSexScene.isActive(serverPlayer) || kito.tactileWith() != null
                || KitoGames.isPlaying(kito)
                || player.isSpectator() || !player.isAlive()
                || serverPlayer.serverLevel() != kito.level() || kito.distanceToSqr(player) > 12.0D * 12.0D || kito.getTarget() != null) {
            TactilityLib.say(kito, serverPlayer, "kito.pose.busy");
            return false;
        }
        SESSIONS.put(kito.getUUID(), new Session(kito, serverPlayer));
        TactilityLib.say(kito, serverPlayer, "kito.kiss.start");
        return true;
    }

    public static boolean isActive(KitoCharacter kito) {
        return SESSIONS.containsKey(kito.getUUID());
    }

    public static void stop(KitoCharacter kito) {
        Session session = SESSIONS.get(kito.getUUID());
        if (session != null) {
            finish(session, false);
        }
    }

    public static void tick(KitoCharacter kito) {
        Session session = SESSIONS.get(kito.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer player = session.player;
        if (!kito.isAlive() || player.isRemoved() || !player.isAlive() || player.serverLevel() != kito.level()) {
            finish(session, false);
            return;
        }
        if (!session.engaged) {
            if (++session.ticks > APPROACH_TIMEOUT || player.isSpectator()
                    || kito.distanceToSqr(player) > 20.0D * 20.0D) {
                finish(session, false);
                TactilityLib.say(kito, player, "kito.pose.unreachable");
                return;
            }
            kito.getNavigation().moveTo(player, WALK_SPEED);
            double distance = kito.distanceToSqr(player);
            if (distance <= START_DISTANCE_SQR) {
                session.engaged = true;
                session.ticks = 0;
                kito.getNavigation().stop();
                kito.setKissing(player.getUUID(), true);
                faceEachOther(kito, player);
                TactilityLib.say(kito, player, "kito.kiss.done");
            }
            return;
        }
        session.ticks++;
        faceEachOther(kito, player);
        kito.getNavigation().stop();
        kito.setDeltaMovement(Vec3.ZERO);
        if (session.ticks >= KISS_TICKS) {
            finish(session, true);
        }
    }

    private static void faceEachOther(KitoCharacter kito, ServerPlayer player) {
        double dx = player.getX() - kito.getX();
        double dz = player.getZ() - kito.getZ();
        float yaw = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D));
        kito.setYRot(yaw);
        kito.yBodyRot = yaw;
        kito.setYHeadRot(yaw);
        float playerYaw = Mth.wrapDegrees(yaw + 180.0F);
        player.setYRot(playerYaw);
        player.yRotO = playerYaw;
        player.setXRot(8.0F);
        player.xRotO = 8.0F;
    }

    private static void finish(Session session, boolean reward) {
        SESSIONS.remove(session.kito.getUUID());
        session.kito.setKissing(null, false);
        session.kito.getNavigation().stop();
        if (reward && session.player.isAlive()) {
            session.kito.moodEvent(MoodEvent.INTIMATE);
        }
    }
}

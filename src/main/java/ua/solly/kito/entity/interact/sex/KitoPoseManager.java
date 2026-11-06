package ua.solly.kito.entity.interact.sex;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.games.KitoGames;
import ua.solly.kito.entity.interact.kiss.KitoKiss;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.entity.relation.KitoTrust;
import ua.solly.kito.entity.relation.MoodEvent;
import ua.solly.kito.init.KitoModItems;

import java.util.UUID;

public final class KitoPoseManager {
    public static final float MIN_TRUST = KitoTrust.SEX;
    public static final float MIN_MOOD = 60.0F;
    public static final float MIN_AROUSAL = 80.0F;

    private static final double MAX_START_DISTANCE_SQR = 20.0D * 20.0D;
    private static final double WALK_SPEED = 1.1D;
    private static final int APPROACH_TIMEOUT_TICKS = 900;
    private static final int REPATH_INTERVAL = 10;
    private static final int PROGRESS_INTERVAL = 20;
    private static final double PROGRESS_MIN_GAIN = 0.25D;
    private static final int STUCK_REPATH = 2;
    private static final int STUCK_PLACE = 4;
    private static final int MAX_FAILED_PATHS = 3;
    private static final double DIRECT_DRIVE_DISTANCE_SQR = 2.25D;
    private static final double ARRIVE_DISTANCE_SQR = 0.09D;
    private static final double ARRIVE_HEIGHT = 1.0D;
    private static final int SETTLE_TICKS = 10;
    private static final float TURN_SPEED = 30.0F;
    private static final double SETTLE_LERP = 0.4D;
    private static final double HOLD_DRIFT_SQR = 0.04D;

    private KitoPoseManager() {
    }

    public static void tryStart(KitoCharacter kito, Player player) {
        tryStart(kito, player, null);
    }

    public static void tryStart(KitoCharacter kito, Player player, SexAction forcedAction) {
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (kito.sexPose().isActive() || KitoSexScene.isActive(sp) || KitoGames.isPlaying(kito) || KitoKiss.isActive(kito)) {
            TactilityLib.say(kito, sp, "kito.pose.busy");
            return;
        }
        KitoRelation relation = kito.peekRelation(player);
        boolean pleasure = forcedAction == SexAction.BWJB;
        boolean protectedSex = !pleasure && hasContraceptive(sp);
        float requiredTrust = pleasure ? KitoTrust.PERSONAL_PLEASURE
                : protectedSex ? KitoTrust.SEX : KitoTrust.SEX_UNPROTECTED;
        if (relation.trust() < requiredTrust) {
            boolean onlyProtectionMissing = !pleasure && !protectedSex && relation.trust() >= KitoTrust.SEX;
            TactilityLib.say(kito, sp, onlyProtectionMissing ? "kito.menu.deny.contraceptive" : "kito.menu.deny.sex_trust");
            kito.moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        if (kito.mood().level() < MIN_MOOD) {
            TactilityLib.say(kito, sp, "kito.menu.deny.sex_mood");
            kito.moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        if (relation.arousal() < MIN_AROUSAL) {
            TactilityLib.say(kito, sp, "kito.menu.deny.sex_arousal");
            kito.moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        if (kito.tactileWith() != null || kito.getTarget() != null || kito.isOnFire()
                || kito.isInWaterOrBubble() || sp.isSpectator() || sp.isPassenger()
                || kito.distanceToSqr(sp) > MAX_START_DISTANCE_SQR) {
            TactilityLib.say(kito, sp, "tactility.kito.deny.busy");
            return;
        }
        KitoPoseFinder.Result chosen = KitoPoseFinder.choose(kito, sp, forcedAction);
        if (chosen == null) {
            TactilityLib.say(kito, sp, "kito.pose.no_spot");
            return;
        }
        if (protectedSex) {
            consumeContraceptive(sp);
        }
        kito.getNavigation().stop();
        kito.sexPose().set(chosen.action(), chosen.pos(), chosen.yaw());
        kito.setTactileWith(player.getUUID());
        KitoSexScene.register(sp, kito);
        TactilityLib.say(kito, sp, "kito.pose.start");
    }

    public static boolean hasContraceptive(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(KitoModItems.CONTRACEPTIVE.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(KitoModItems.CONTRACEPTIVE.get())) {
                return true;
            }
        }
        return false;
    }

    private static void consumeContraceptive(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(KitoModItems.CONTRACEPTIVE.get())) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(KitoModItems.CONTRACEPTIVE.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static void tick(KitoCharacter kito) {
        KitoPose pose = kito.sexPose();
        if (!pose.isActive()) {
            return;
        }
        if (!pose.isSceneActive() || pose.targetPos() == null) {
            stop(kito);
            return;
        }
        switch (pose.phase()) {
            case APPROACH -> approach(kito, pose);
            case SETTLE -> settle(kito, pose);
            case ENGAGED -> hold(kito, pose);
        }
    }

    private static void approach(KitoCharacter kito, KitoPose pose) {
        pose.tickPhase();
        if (pose.phaseTicks() > APPROACH_TIMEOUT_TICKS) {
            abort(kito, "kito.pose.unreachable");
            return;
        }
        Vec3 spot = pose.standVec();
        double dx = spot.x - kito.getX();
        double dz = spot.z - kito.getZ();
        double flat = dx * dx + dz * dz;
        boolean sameLevel = Math.abs(spot.y - kito.getY()) < ARRIVE_HEIGHT;

        if (flat <= ARRIVE_DISTANCE_SQR && sameLevel) {
            kito.getNavigation().stop();
            pose.setPhase(KitoPose.Phase.SETTLE);
            return;
        }

        if (flat <= DIRECT_DRIVE_DISTANCE_SQR && sameLevel) {
            kito.getNavigation().stop();
            kito.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, WALK_SPEED);
        } else if (pose.phaseTicks() % REPATH_INTERVAL == 1
                || (kito.getNavigation().isDone() && pose.phaseTicks() % 4 == 0)) {
            if (kito.getNavigation().moveTo(spot.x, spot.y, spot.z, WALK_SPEED)) {
                pose.resetFailedPaths();
            } else {
                pose.addFailedPath();
                if (pose.failedPaths() >= MAX_FAILED_PATHS && !forcePlace(kito, pose)) {
                    abort(kito, "kito.pose.unreachable");
                    return;
                }
            }
        }

        if (pose.phaseTicks() % PROGRESS_INTERVAL == 0) {
            double dist = Math.sqrt(flat);
            if (pose.lastDist() >= 0.0D) {
                if (pose.lastDist() - dist < PROGRESS_MIN_GAIN) {
                    pose.addStuck();
                } else {
                    pose.resetStuck();
                }
            }
            pose.setLastDist(dist);
            if (pose.stuck() >= STUCK_PLACE) {
                if (!forcePlace(kito, pose)) {
                    abort(kito, "kito.pose.unreachable");
                }
            } else if (pose.stuck() >= STUCK_REPATH) {
                kito.getNavigation().stop();
                kito.getNavigation().moveTo(spot.x, spot.y, spot.z, WALK_SPEED);
            }
        }
    }

    private static boolean forcePlace(KitoCharacter kito, KitoPose pose) {
        Vec3 spot = pose.standVec();
        if (!isFree(kito, spot)) {
            return false;
        }
        kito.getNavigation().stop();
        kito.setDeltaMovement(Vec3.ZERO);
        kito.moveTo(spot.x, spot.y, spot.z, pose.yaw(), 0.0F);
        kito.yBodyRot = pose.yaw();
        kito.setYHeadRot(pose.yaw());
        pose.setPhase(KitoPose.Phase.SETTLE);
        return true;
    }

    private static void settle(KitoCharacter kito, KitoPose pose) {
        pose.tickPhase();
        Vec3 spot = pose.standVec();
        kito.getNavigation().stop();
        float yaw = Mth.approachDegrees(kito.getYRot(), pose.yaw(), TURN_SPEED);
        Vec3 next = new Vec3(
                Mth.lerp(SETTLE_LERP, kito.getX(), spot.x),
                Mth.lerp(SETTLE_LERP, kito.getY(), spot.y),
                Mth.lerp(SETTLE_LERP, kito.getZ(), spot.z)
        );
        if (isFree(kito, next)) {
            kito.moveTo(next.x, next.y, next.z, yaw, 0.0F);
        } else {
            kito.setYRot(yaw);
        }
        kito.yBodyRot = yaw;
        kito.setYHeadRot(yaw);
        kito.setDeltaMovement(0.0D, kito.getDeltaMovement().y, 0.0D);

        double dx = spot.x - kito.getX();
        double dz = spot.z - kito.getZ();
        boolean aligned = dx * dx + dz * dz < 0.0025D && Math.abs(Mth.wrapDegrees(yaw - pose.yaw())) < 1.0F;
        if (aligned || pose.phaseTicks() >= SETTLE_TICKS) {
            if (isFree(kito, spot)) {
                kito.moveTo(spot.x, spot.y, spot.z, pose.yaw(), 0.0F);
            } else {
                kito.setYRot(pose.yaw());
            }
            kito.yBodyRot = pose.yaw();
            kito.setYHeadRot(pose.yaw());
            pose.setPhase(KitoPose.Phase.ENGAGED);
        }
    }

    private static void hold(KitoCharacter kito, KitoPose pose) {
        pose.tickPhase();
        Vec3 spot = pose.standVec();
        kito.getNavigation().stop();
        double dx = spot.x - kito.getX();
        double dz = spot.z - kito.getZ();
        if (dx * dx + dz * dz > HOLD_DRIFT_SQR && isFree(kito, spot)) {
            kito.moveTo(spot.x, spot.y, spot.z, pose.yaw(), 0.0F);
        }
        kito.setDeltaMovement(0.0D, kito.getDeltaMovement().y, 0.0D);
        kito.setYRot(pose.yaw());
        kito.yBodyRot = pose.yaw();
    }

    private static boolean isFree(KitoCharacter kito, Vec3 at) {
        AABB box = kito.getBoundingBox().move(at.x - kito.getX(), at.y - kito.getY(), at.z - kito.getZ());
        return kito.level().noCollision(kito, box);
    }

    private static void abort(KitoCharacter kito, String messageKey) {
        UUID id = kito.tactileWith();
        Player player = id == null ? null : kito.level().getPlayerByUUID(id);
        stop(kito);
        if (player instanceof ServerPlayer sp) {
            TactilityLib.say(kito, sp, messageKey);
        }
    }

    public static void stop(KitoCharacter kito) {
        KitoPose pose = kito.sexPose();
        if (!pose.isActive()) {
            return;
        }
        boolean intimate = pose.isIntimate();
        KitoSexScene.stop(kito);
        if (!pose.isActive()) {
            return;
        }
        pose.clear();
        kito.resetSexAnimationState();
        kito.getNavigation().stop();
        kito.setTactileWith(null);
        if (intimate) {
            kito.moodEvent(MoodEvent.INTIMATE);
        }
    }
}

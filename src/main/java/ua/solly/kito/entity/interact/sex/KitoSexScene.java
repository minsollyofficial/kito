package ua.solly.kito.entity.interact.sex;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.init.KitoModSounds;
import ua.solly.kito.kaf.GeckoLibAnimations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = KitoMod.MODID)
public final class KitoSexScene {
    public static final int MAX_THRUSTS = 20;
    public static final int THRUST_ANIM_TICKS = 26;
    public static final int THRUST_COOLDOWN_TICKS = 20;
    public static final int SEEK_TIMEOUT_TICKS = 1200;
    public static final int IDLE_DECAY_TICKS = 60;
    public static final float IDLE_DECAY = 10.0F;
    public static final float MIN_AROUSAL = 30.0F;
    private static final double START_DISTANCE_SQR = 9.0D;
    private static final double SEEK_DISTANCE_SQR = 400.0D;
    private static final double ACTIVE_DISTANCE_SQR = 25.0D;
    private static final double BEHIND_OFFSET = 0.75D;

    public static final class Session {
        public final ServerPlayer player;
        public final KitoCharacter kito;
        public int thrustsLeft = MAX_THRUSTS;
        public int lastAnyTick;
        public int thrustAnimTicks;
        public int cooldown;
        public int waitTicks;
        public boolean started;

        Session(ServerPlayer player, KitoCharacter kito) {
            this.player = player;
            this.kito = kito;
            this.lastAnyTick = kito.tickCount;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private KitoSexScene() {
    }

    public static void register(ServerPlayer player, KitoCharacter kito) {
        Session session = new Session(player, kito);
        SESSIONS.put(player.getUUID(), session);
        kito.sexPose().setSceneActive(true);
        kito.setTactileWith(player.getUUID());
    }

    public static boolean isActive(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void onThrust(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!session.started) {
            return;
        }
        if (session.cooldown > 0) {
            return;
        }
        if (session.thrustsLeft <= 0) {
            return;
        }
        session.cooldown = THRUST_COOLDOWN_TICKS;
        session.thrustAnimTicks = THRUST_ANIM_TICKS;
        session.lastAnyTick = session.kito.tickCount;
        session.thrustsLeft--;
        if (session.kito.sexPose().action() == SexAction.BWJB) {
            GeckoLibAnimations.trigger(session.kito, GeckoLibAnimations.ACTION_CONTROLLER, "bwjb_thrust");
        }
        KitoRelation relation = session.kito.relation(player);
        relation.setArousal(100.0F);
        playThrustSounds(session.kito);
        KitoSexNet.sendState(player, true, session.kito.getId(), session.thrustsLeft);
    }

    public static boolean isThrusting(KitoCharacter kito) {
        for (Session s : SESSIONS.values()) {
            if (s.kito == kito && s.thrustAnimTicks > 0) {
                return true;
            }
        }
        return false;
    }

    private static void playThrustSounds(KitoCharacter kito) {
        if (!(kito.level() instanceof ServerLevel level)) {
            return;
        }
        ua.solly.kito.particle.KitoHearts.burst(kito, 2, 0.55D);
        level.playSound(null, kito.getX(), kito.getY(), kito.getZ(),
                KitoModSounds.SLIDE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        SexAction action = kito.sexPose().action();
        if (action == SexAction.BWJB) {
            if (kito.getRandom().nextFloat() < 0.5F) {
                level.playSound(null, kito.getX(), kito.getY(), kito.getZ(),
                        KitoModSounds.MMM.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
        } else if (action != null && kito.getRandom().nextFloat() < 0.6F) {
            level.playSound(null, kito.getX(), kito.getY(), kito.getZ(),
                    KitoModSounds.AHH.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Session> entry : new ArrayList<>(SESSIONS.entrySet())) {
            Session session = entry.getValue();
            KitoCharacter kito = session.kito;
            ServerPlayer player = session.player;
            if (!kito.isAlive() || player.isRemoved() || !player.isAlive()) {
                end(session);
                continue;
            }
            if (!kito.sexPose().isSceneActive()) {
                end(session);
                continue;
            }

            double distSqr = player.distanceToSqr(kito);
            if (distSqr > (session.started ? ACTIVE_DISTANCE_SQR : SEEK_DISTANCE_SQR)) {
                end(session);
                continue;
            }

            if (session.thrustAnimTicks > 0) {
                session.thrustAnimTicks--;
            }
            if (session.cooldown > 0) {
                session.cooldown--;
            }

            if (!session.started) {
                session.waitTicks++;
                if (session.waitTicks > SEEK_TIMEOUT_TICKS) {
                    end(session);
                    continue;
                }
                if (kito.sexPose().isEngaged() && distSqr <= START_DISTANCE_SQR) {
                    placeBehind(player, kito);
                    session.started = true;
                    kito.sexPose().markIntimate();
                    session.lastAnyTick = kito.tickCount;
                    KitoSexNet.sendState(player, true, kito.getId(), session.thrustsLeft);
                }
                continue;
            }

            int sinceAny = kito.tickCount - session.lastAnyTick;
            if (sinceAny >= IDLE_DECAY_TICKS && sinceAny % 20 == 0) {
                KitoRelation relation = kito.relation(player);
                relation.addArousal(-IDLE_DECAY);
                if (relation.arousal() < MIN_AROUSAL) {
                    end(session);
                    continue;
                }
            }
            if (session.thrustsLeft <= 0 && session.thrustAnimTicks <= 0) {
                end(session);
            }
        }
    }

    private static void placeBehind(ServerPlayer player, KitoCharacter kito) {
        float yaw = kito.sexPose().yaw();
        double rad = Math.toRadians(yaw);
        boolean bwjb = kito.sexPose().action() == SexAction.BWJB;
        double offset = bwjb ? 0.72D : BEHIND_OFFSET;
        double x = kito.getX() + Math.sin(rad) * (bwjb ? -offset : offset);
        double z = kito.getZ() - Math.cos(rad) * (bwjb ? -offset : offset);
        double y = kito.getY();
        AABB box = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
        if (!player.level().noCollision(player, box)) {
            return;
        }
        player.connection.teleport(x, y, z, bwjb ? yaw + 180.0F : yaw, bwjb ? 32.0F : 15.0F);
    }

    private static void end(Session session) {
        SESSIONS.remove(session.player.getUUID());
        session.kito.sexPose().setSceneActive(false);
        KitoPoseManager.stop(session.kito);
        session.kito.resetSexAnimationState();
        if (session.started) {
            TactilityLib.say(session.kito, session.player, "kito.pose.ended");
        }
        KitoSexNet.sendState(session.player, false, session.kito.getId(), 0);
    }

    public static void stop(KitoCharacter kito) {
        for (Map.Entry<UUID, Session> entry : new ArrayList<>(SESSIONS.entrySet())) {
            if (entry.getValue().kito == kito) {
                end(entry.getValue());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Session session = SESSIONS.remove(sp.getUUID());
            if (session != null) {
                KitoPoseManager.stop(session.kito);
            }
        }
    }
}

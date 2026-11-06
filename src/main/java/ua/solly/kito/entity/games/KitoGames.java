package ua.solly.kito.entity.games;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.sex.KitoSexScene;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.relation.MoodEvent;
import ua.solly.kito.entity.relation.TrustAction;
import ua.solly.kito.entity.interact.kiss.KitoKiss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = KitoMod.MODID)
public final class KitoGames {
    public static final float MIN_TRUST = ua.solly.kito.entity.relation.KitoTrust.GAMES;
    private static final int HIDE_TICKS = 600;
    private static final int SEEK_TICKS = 1200;
    private static final int TAG_TICKS = 600;
    private static final double DETECT_DISTANCE_SQR = 20.0D * 20.0D;
    private static final double CATCH_DISTANCE_SQR = 1.8D * 1.8D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public enum Type {
        HIDE_AND_SEEK,
        TAG
    }

    private enum Stage {
        COUNTDOWN,
        PLAYING,
        OUTCOME_APPROACH
    }

    private static final class Session {
        private final KitoCharacter kito;
        private final ServerPlayer player;
        private final Type type;
        private final boolean kitoSeeks;
        private final boolean oldInvisible;
        private final boolean oldInvulnerable;
        private final double oldSpeed;
        private final Set<Long> visited = new HashSet<>();
        private Stage stage = Stage.COUNTDOWN;
        private int ticks;
        private int left;
        private int lastHud = -1;
        private int repath;
        private boolean hidden;
        private boolean rewarded;
        private String outcome;
        private boolean playerWon;
        private Vec3 hidingPlace;

        private Session(KitoCharacter kito, ServerPlayer player, Type type, boolean kitoSeeks) {
            this.kito = kito;
            this.player = player;
            this.type = type;
            this.kitoSeeks = kitoSeeks;
            this.oldInvisible = kito.isInvisible();
            this.oldInvulnerable = player.getAbilities().invulnerable;
            this.oldSpeed = kito.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        }

        private boolean playerIsSeeker() {
            return !kitoSeeks;
        }
    }

    private KitoGames() {
    }

    public record GameHud(int ticks) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GameHud> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "games_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GameHud> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, GameHud message) -> buf.writeInt(message.ticks()),
                (RegistryFriendlyByteBuf buf) -> new GameHud(buf.readInt())
        );

        @Override
        public CustomPacketPayload.Type<GameHud> type() {
            return TYPE;
        }
    }

    public static void registerNetwork() {
        KitoMod.addNetworkMessage(GameHud.TYPE, GameHud.CODEC, KitoGames::onGameHud);
    }

    private static void onGameHud(GameHud message, IPayloadContext context) {
        if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(() -> KitoGamesClient.setTicks(message.ticks()));
        }
    }

    public static boolean isPlaying(KitoCharacter kito) {
        return SESSIONS.containsKey(kito.getUUID());
    }

    public static boolean startHideAndSeek(KitoCharacter kito, Player player) {
        return start(kito, player, Type.HIDE_AND_SEEK);
    }

    public static boolean startTag(KitoCharacter kito, Player player) {
        return start(kito, player, Type.TAG);
    }

    private static boolean start(KitoCharacter kito, Player player, Type type) {
        if (!(player instanceof ServerPlayer sp) || kito.level().isClientSide()) {
            return false;
        }
        if (kito.peekRelation(player).trust() < MIN_TRUST) {
            TactilityLib.say(kito, sp, "kito.games.deny.trust");
            return false;
        }
        if (SESSIONS.containsKey(kito.getUUID()) || kito.sexPose().isActive() || kito.tactileWith() != null
                || KitoKiss.isActive(kito) || KitoSexScene.isActive(sp)
                || player.isSpectator() || !player.isAlive() || kito.getTarget() != null
                || sp.serverLevel() != kito.level() || kito.distanceToSqr(player) > 8.0D * 8.0D) {
            TactilityLib.say(kito, sp, "kito.games.busy");
            return false;
        }
        boolean kitoSeeks = kito.getRandom().nextBoolean();
        Session session = new Session(kito, sp, type, kitoSeeks);
        if (type == Type.TAG) {
            session.stage = Stage.PLAYING;
        }
        SESSIONS.put(kito.getUUID(), session);
        kito.getNavigation().stop();
        kito.setDeltaMovement(Vec3.ZERO);
        if (type == Type.HIDE_AND_SEEK) {
            if (kitoSeeks) {
                TactilityLib.say(kito, sp, "kito.games.start.kito_seeks");
            } else {
                TactilityLib.say(kito, sp, "kito.games.start.player_seeks");
                setInvulnerable(session, true);
                sendHud(sp, HIDE_TICKS);
            }
        } else if (kitoSeeks) {
            TactilityLib.say(kito, sp, "kito.games.tag.start.kito");
        } else {
            TactilityLib.say(kito, sp, "kito.games.tag.start.player");
        }
        if (type == Type.TAG || !kitoSeeks) {
            setRunning(session, true);
        } else {
            setRunning(session, false);
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            tick(session);
        }
    }

    private static void tick(Session session) {
        KitoCharacter kito = session.kito;
        ServerPlayer player = session.player;
        if (!kito.isAlive() || player.isRemoved() || !player.isAlive() || player.serverLevel() != kito.level()) {
            end(session, false, null);
            return;
        }
        if (kito.sexPose().isActive() || kito.tactileWith() != null || kito.getTarget() != null) {
            end(session, false, "kito.games.ended");
            return;
        }
        session.ticks++;
        session.visited.add(kito.blockPosition().asLong());
        if (session.type == Type.HIDE_AND_SEEK) {
            tickHideAndSeek(session);
        } else {
            tickTag(session);
        }
    }

    private static void tickHideAndSeek(Session session) {
        if (session.stage == Stage.OUTCOME_APPROACH) {
            approachOutcome(session);
            return;
        }
        if (session.stage == Stage.COUNTDOWN) {
            tickCountdown(session);
            return;
        }
        if (session.kitoSeeks) {
            tickKitoSeeks(session);
        } else {
            tickPlayerSeeks(session);
        }
    }

    private static void tickCountdown(Session session) {
        KitoCharacter kito = session.kito;
        session.left = HIDE_TICKS - session.ticks;
        if (session.type == Type.TAG) {
            setRunning(session, true);
            if (session.playerIsSeeker()) {
                kito.getNavigation().moveTo(session.player, 1.5D);
            } else {
                fleeFromPlayer(session, 1.55D);
            }
            if (session.ticks >= TAG_TICKS) {
                session.stage = Stage.PLAYING;
                session.ticks = 0;
                session.left = TAG_TICKS;
            }
            return;
        }
        if (session.kitoSeeks) {
            kito.getNavigation().stop();
            kito.setDeltaMovement(0.0D, kito.getDeltaMovement().y, 0.0D);
            kito.setYHeadRot(kito.getYRot() + 180.0F);
        } else {
            setInvulnerable(session, true);
            if (session.ticks % 20 == 0) {
                sendHud(session.player, Math.max(0, HIDE_TICKS - session.ticks));
            }
            setRunning(session, true);
            if (session.hidingPlace == null) {
                if (kito.distanceToSqr(session.player) < 50.0D * 50.0D) {
                    Vec3 away = new Vec3(kito.getX() - session.player.getX(), 0.0D, kito.getZ() - session.player.getZ());
                    if (away.lengthSqr() < 1.0E-4D) {
                        away = new Vec3(1.0D, 0.0D, 0.0D);
                    }
                    away = away.normalize().scale(50.0D);
                    Vec3 destination = new Vec3(session.player.getX() + away.x, session.player.getY(), session.player.getZ() + away.z);
                    if (session.ticks % 25 == 0 || kito.getNavigation().isDone()) {
                        kito.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.65D);
                    }
                } else {
                    session.hidingPlace = findHidingPlace(session);
                }
            }
            if (session.hidingPlace != null && !session.hidden) {
                if (session.ticks % 20 == 0 || kito.getNavigation().isDone()) {
                    kito.getNavigation().moveTo(session.hidingPlace.x, session.hidingPlace.y, session.hidingPlace.z, 1.35D);
                }
                if (kito.distanceToSqr(session.hidingPlace) < 4.0D) {
                    kito.getNavigation().stop();
                    kito.setInvisible(true);
                    session.hidden = true;
                }
            }
        }
        if (session.ticks >= HIDE_TICKS) {
            session.stage = Stage.PLAYING;
            session.ticks = 0;
            session.left = SEEK_TICKS;
            if (!session.kitoSeeks) {
                sendHud(session.player, 0);
                setInvulnerable(session, session.oldInvulnerable);
            }
            if (session.hidden) {
                kito.setInvisible(true);
            }
        }
    }

    private static void tickKitoSeeks(Session session) {
        KitoCharacter kito = session.kito;
        ServerPlayer player = session.player;
        session.left = SEEK_TICKS - session.ticks;
        if (kito.hasLineOfSight(player) && kito.distanceToSqr(player) <= DETECT_DISTANCE_SQR) {
            kito.setYHeadRot(kito.getYRot());
            beginOutcomeApproach(session, false, "kito.games.lose.player");
            return;
        }
        setRunning(session, true);
        if (session.ticks % 35 == 0 || kito.getNavigation().isDone()) {
            Vec3 target = new Vec3(player.getX(), player.getY(), player.getZ());
            for (int i = 0; i < 18; i++) {
                int dx = kito.getRandom().nextInt(-10, 11);
                int dz = kito.getRandom().nextInt(-10, 11);
                BlockPos candidate = player.blockPosition().offset(dx, 0, dz);
                if (!session.visited.contains(candidate.asLong())) {
                    target = new Vec3(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
                    break;
                }
            }
            kito.getNavigation().moveTo(target.x, target.y, target.z, 1.25D);
        }
        if (session.ticks >= SEEK_TICKS) {
            TactilityLib.say(kito, player, "kito.games.surrender");
            session.playerWon = true;
            end(session, true, null);
        }
    }

    private static void tickPlayerSeeks(Session session) {
        KitoCharacter kito = session.kito;
        ServerPlayer player = session.player;
        session.left = SEEK_TICKS - session.ticks;
        if (!session.hidden && session.hidingPlace != null) {
            if (kito.distanceToSqr(session.hidingPlace) < 4.0D) {
                kito.getNavigation().stop();
                kito.setInvisible(true);
                session.hidden = true;
            } else if (session.ticks % 20 == 0 || kito.getNavigation().isDone()) {
                kito.getNavigation().moveTo(session.hidingPlace.x, session.hidingPlace.y, session.hidingPlace.z, 1.35D);
            }
        }
        if ((session.hidden && player.distanceToSqr(kito) <= CATCH_DISTANCE_SQR)
                || (!kito.isInvisible() && player.hasLineOfSight(kito) && player.distanceToSqr(kito) <= DETECT_DISTANCE_SQR)) {
            kito.setInvisible(false);
            beginOutcomeApproach(session, true, "kito.games.win.player");
            return;
        }
        if (session.ticks >= SEEK_TICKS) {
            kito.setInvisible(false);
            beginOutcomeApproach(session, false, "kito.games.lose.player");
        }
    }

    private static void tickTag(Session session) {
        KitoCharacter kito = session.kito;
        ServerPlayer player = session.player;
        session.left = TAG_TICKS - session.ticks;
        setRunning(session, true);
        if (session.playerIsSeeker()) {
            fleeFromPlayer(session, 1.75D);
            if (kito.distanceToSqr(player) <= CATCH_DISTANCE_SQR) {
                session.playerWon = true;
                end(session, true, "kito.games.tag.win.player");
                return;
            }
        } else {
            kito.getNavigation().moveTo(player, 1.8D);
            if (kito.distanceToSqr(player) <= CATCH_DISTANCE_SQR) {
                session.playerWon = false;
                end(session, true, "kito.games.tag.win.kito");
                return;
            }
        }
        if (session.ticks >= TAG_TICKS) {
            session.playerWon = !session.playerIsSeeker();
            end(session, true, session.playerIsSeeker()
                    ? "kito.games.tag.win.kito" : "kito.games.tag.win.player");
        }
    }

    private static void fleeFromPlayer(Session session, double speed) {
        KitoCharacter kito = session.kito;
        Vec3 away = new Vec3(kito.getX() - session.player.getX(), 0.0D, kito.getZ() - session.player.getZ());
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        away = away.normalize().scale(18.0D);
        if (session.repath++ % 10 == 0 || kito.getNavigation().isDone()) {
            kito.getNavigation().moveTo(kito.getX() + away.x, kito.getY(), kito.getZ() + away.z, speed);
        }
    }

    private static Vec3 findHidingPlace(Session session) {
        if (!(session.kito.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos origin = session.player.blockPosition();
        Vec3 fallback = null;
        for (int i = 0; i < 80; i++) {
            int dx = session.kito.getRandom().nextInt(-70, 71);
            int dz = session.kito.getRandom().nextInt(-70, 71);
            if (dx * dx + dz * dz < 50 * 50) {
                continue;
            }
            BlockPos target = origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
            BlockState floor = level.getBlockState(pos.below());
            if (!floor.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) {
                continue;
            }
            AABB box = session.kito.getBoundingBox().move(pos.getX() + 0.5D - session.kito.getX(),
                    pos.getY() - session.kito.getY(), pos.getZ() + 0.5D - session.kito.getZ());
            if (!level.noCollision(session.kito, box)) {
                continue;
            }
            Vec3 candidate = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (fallback == null) {
                fallback = candidate;
            }
            if (!level.canSeeSky(pos)) {
                return candidate;
            }
        }
        return fallback;
    }

    private static void beginOutcomeApproach(Session session, boolean playerWon, String outcome) {
        session.stage = Stage.OUTCOME_APPROACH;
        session.playerWon = playerWon;
        session.outcome = outcome;
        session.ticks = 0;
        session.kito.setInvisible(false);
        setInvulnerable(session, session.oldInvulnerable);
        sendHud(session.player, 0);
        setRunning(session, true);
    }

    private static void approachOutcome(Session session) {
        KitoCharacter kito = session.kito;
        ServerPlayer player = session.player;
        if (kito.distanceToSqr(player) <= 2.25D || session.ticks > 300) {
            if (session.outcome != null) {
                TactilityLib.say(kito, player, session.outcome);
            }
            end(session, true, null);
            return;
        }
        kito.getNavigation().moveTo(player, 1.15D);
    }

    private static void setRunning(Session session, boolean running) {
        session.kito.setGameRunning(running);
    }

    private static void setInvulnerable(Session session, boolean value) {
        if (session.player.getAbilities().invulnerable != value) {
            session.player.getAbilities().invulnerable = value;
            session.player.onUpdateAbilities();
        }
    }

    private static void sendHud(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayer(player, new GameHud(ticks));
    }

    private static void end(Session session, boolean reward, String message) {
        if (!SESSIONS.containsKey(session.kito.getUUID())) {
            return;
        }
        SESSIONS.remove(session.kito.getUUID());
        session.kito.setInvisible(session.oldInvisible);
        session.kito.getNavigation().stop();
        session.kito.setGameRunning(false);
        setInvulnerable(session, session.oldInvulnerable);
        sendHud(session.player, 0);
        if (message != null && session.player.isAlive()) {
            TactilityLib.say(session.kito, session.player, message);
        }
        if (reward && !session.rewarded && session.player.isAlive()) {
            session.rewarded = true;
            session.kito.react(session.player, TrustAction.PLAY_GAME);
            session.kito.moodEvent(MoodEvent.PLAYED, session.playerWon ? 1.4F : 1.0F);
            TactilityLib.sendTrust(session.player, session.kito);
        }
        if (session.kito.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            session.kito.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(session.oldSpeed);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            if (session.player.getUUID().equals(player.getUUID())) {
                end(session, false, null);
            }
        }
    }
}

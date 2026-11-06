package ua.solly.kito.entity.interact.tactility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.kaf.BothBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ua.solly.kito.entity.HudSystems;

public final class TactilityLib {
    public static final double REACH = 2.4D;
    public static final double SESSION_RANGE = 4.5D;
    public static final float STROKE_STEP = 14.0F;
    public static final float STROKE_CAP = 60.0F;
    public static final float POSE_REFERENCE = 12.0F;
    public static final int STEP_COOLDOWN = 4;
    public static final int HOLD_TIMEOUT = 100;
    public static final int MESSAGE_COOLDOWN = 60;
    public static final int AROUSAL_IDLE_TICKS = 60;
    public static final float AROUSAL_DECAY = 0.5F;
    public static final float AROUSAL_FAST_DECAY = 4.0F;
    public static final float AROUSAL_DROP_FACTOR = 6.0F;
    public static final float BASE_TRUST = ua.solly.kito.entity.relation.KitoTrust.TOUCH;
    public static final float FULL_TRUST = ua.solly.kito.entity.relation.KitoTrust.TOUCH_INTIMATE;

    public static final int OP_ENTER = 0;
    public static final int OP_EXIT = 1;
    public static final int TOUCH_START = 0;
    public static final int TOUCH_MOVE = 1;
    public static final int TOUCH_END = 2;

    private static final double CROWN_FROM = 0.93D;
    private static final double HEAD_FROM = 0.78D;
    private static final double ARM_FROM = 0.45D;
    private static final double CHEST_FROM = 0.64D;
    private static final double BELLY_FROM = 0.52D;
    private static final double GROIN_FROM = 0.40D;
    private static final double CHEEK_SIDE = 0.55D;
    private static final double ARM_SIDE = 0.72D;
    private static final double GROIN_SIDE = 0.45D;

    private static final Map<UUID, Long> MUTE = new HashMap<>();
    private static boolean registered;

    private TactilityLib() {
    }

    public static final class Memory {
        public long lastIntimateTick = -1_000_000L;
        public float lastTrust = Float.NaN;
    }

    public record Request(int op, int entityId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "tactility_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Request message) -> {
                    buf.writeByte(message.op());
                    buf.writeInt(message.entityId());
                },
                (RegistryFriendlyByteBuf buf) -> new Request(buf.readByte(), buf.readInt())
        );

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Touch(int op, int part, float right, float up) implements CustomPacketPayload {
        public static final Type<Touch> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "tactility_touch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Touch> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Touch message) -> {
                    buf.writeByte(message.op());
                    buf.writeByte(message.part());
                    buf.writeFloat(message.right());
                    buf.writeFloat(message.up());
                },
                (RegistryFriendlyByteBuf buf) -> new Touch(buf.readByte(), buf.readByte(), buf.readFloat(), buf.readFloat())
        );

        @Override
        public Type<Touch> type() {
            return TYPE;
        }
    }

    public record State(boolean active, int entityId, int tier) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "tactility_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, State message) -> {
                    buf.writeBoolean(message.active());
                    buf.writeInt(message.entityId());
                    buf.writeByte(message.tier());
                },
                (RegistryFriendlyByteBuf buf) -> new State(buf.readBoolean(), buf.readInt(), buf.readByte())
        );

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    public record TrustInfo(int entityId, float trust, float arousal, float disgust, float mood) implements CustomPacketPayload {
        public static final Type<TrustInfo> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "trust_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TrustInfo> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, TrustInfo message) -> {
                    buf.writeInt(message.entityId());
                    buf.writeFloat(message.trust());
                    buf.writeFloat(message.arousal());
                    buf.writeFloat(message.disgust());
                    buf.writeFloat(message.mood());
                },
                (RegistryFriendlyByteBuf buf) -> new TrustInfo(buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
        );

        @Override
        public Type<TrustInfo> type() {
            return TYPE;
        }
    }

    public static void registerNetwork() {
        if (registered) {
            return;
        }
        registered = true;
        KitoMod.addNetworkMessage(Request.TYPE, Request.CODEC, TactilityLib::onRequest);
        KitoMod.addNetworkMessage(Touch.TYPE, Touch.CODEC, TactilityLib::onTouch);
        KitoMod.addNetworkMessage(State.TYPE, State.CODEC, TactilityLib::onState);
        KitoMod.addNetworkMessage(TrustInfo.TYPE, TrustInfo.CODEC, TactilityLib::onTrustInfo);
    }

    private static void onRequest(Request message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TactilityScene.handle(player, message);
            }
        });
    }

    private static void onTouch(Touch message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TactilityScene.handle(player, message);
            }
        });
    }

    private static void onState(State message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> BothBridge.client(() -> () ->
                TactilityScene.Client.onState(message.active(), message.entityId(), message.tier())));
    }

    private static void onTrustInfo(TrustInfo message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> BothBridge.client(() -> () ->
                HudSystems.onStats(message.entityId(), message.trust(), message.arousal(), message.disgust(), message.mood())));
    }

    public static void sendTrust(ServerPlayer player, KitoCharacter kito) {
        KitoRelation relation = kito.peekRelation(player);
        PacketDistributor.sendToPlayer(player, new TrustInfo(
                kito.getId(),
                relation.trust(),
                relation.arousal(),
                relation.disgust(),
                kito.mood().level()
        ));
    }

    public static void sendState(ServerPlayer player, boolean active, int entityId, int tier) {
        PacketDistributor.sendToPlayer(player, new State(active, entityId, tier));
    }

    public static int tierOf(KitoRelation relation) {
        if (relation.isDisgustLocked()) {
            return 0;
        }
        if (relation.trust() >= FULL_TRUST) {
            return 2;
        }
        return relation.trust() >= BASE_TRUST ? 1 : 0;
    }

    public static boolean allowedByTier(TactilityAction action, int tier) {
        return tier >= action.tier();
    }

    public static void say(KitoCharacter kito, ServerPlayer player, String key) {
        long now = player.level().getGameTime();
        Long until = MUTE.get(player.getUUID());
        if (until != null && now < until) {
            return;
        }
        MUTE.put(player.getUUID(), now + MESSAGE_COOLDOWN);
        Component name = kito.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE);
        player.sendSystemMessage(Component.empty()
                .append(name)
                .append(Component.literal(": "))
                .append(Component.translatable(key)));
    }

    public static void forget(UUID player) {
        MUTE.remove(player);
    }

    public static void tickArousal(KitoRelation relation, Memory memory, long now) {
        float trust = relation.trust();
        if (Float.isNaN(memory.lastTrust)) {
            memory.lastTrust = trust;
        }
        float drop = Math.max(0.0F, memory.lastTrust - trust);
        memory.lastTrust = trust;
        if (relation.arousal() <= 0.0F) {
            return;
        }
        float decay = 0.0F;
        if (now - memory.lastIntimateTick >= AROUSAL_IDLE_TICKS) {
            decay += AROUSAL_DECAY;
        }
        if (drop > 0.0F) {
            decay += AROUSAL_FAST_DECAY + drop * AROUSAL_DROP_FACTOR;
        }
        if (trust < FULL_TRUST) {
            decay += AROUSAL_FAST_DECAY;
        }
        if (relation.isDisgustLocked()) {
            decay += AROUSAL_FAST_DECAY;
        }
        if (decay > 0.0F) {
            relation.addArousal(-decay);
        }
    }

    public static TactilityAction pick(Player player, KitoCharacter kito, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 base = kito.getPosition(partialTick);
        double yaw = Mth.rotLerp(partialTick, kito.yBodyRotO, kito.yBodyRot) * Mth.DEG_TO_RAD;
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double ox = eye.x - base.x;
        double oy = eye.y - base.y;
        double oz = eye.z - base.z;
        double[] origin = {ox * cos + oz * sin, oy, -ox * sin + oz * cos};
        double[] dir = {look.x * cos + look.z * sin, look.y, -look.x * sin + look.z * cos};

        double halfWidth = kito.getBbWidth() * 0.5D;
        double height = kito.getBbHeight();
        double[] min = {-halfWidth, 0.0D, -halfWidth};
        double[] max = {halfWidth, height, halfWidth};

        double near = 0.0D;
        double far = REACH;
        int nearAxis = -1;
        boolean nearHigh = false;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dir[axis]) < 1.0E-9D) {
                if (origin[axis] < min[axis] || origin[axis] > max[axis]) {
                    return null;
                }
                continue;
            }
            double t1 = (min[axis] - origin[axis]) / dir[axis];
            double t2 = (max[axis] - origin[axis]) / dir[axis];
            boolean enterHigh = dir[axis] < 0.0D;
            if (t1 > t2) {
                double swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > near) {
                near = t1;
                nearAxis = axis;
                nearHigh = enterHigh;
            }
            far = Math.min(far, t2);
            if (near > far) {
                return null;
            }
        }

        BlockHitResult block = player.level().clip(new ClipContext(
                eye,
                eye.add(look.scale(REACH)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (block.getType() == HitResult.Type.BLOCK && eye.distanceToSqr(block.getLocation()) < near * near) {
            return null;
        }

        double hx = origin[0] + dir[0] * near;
        double hy = origin[1] + dir[1] * near;
        double hz = origin[2] + dir[2] * near;
        boolean top = nearAxis == 1 && nearHigh;
        return classify(hx, Mth.clamp(hy, 0.0D, height), hz, halfWidth, height, top);
    }

    private static TactilityAction classify(double x, double y, double z, double halfWidth, double height, boolean top) {
        double u = x / halfWidth;
        double v = y / height;
        boolean left = u >= 0.0D;
        boolean front = z >= 0.0D;
        double side = Math.abs(u);

        if (top || v >= CROWN_FROM) {
            return TactilityAction.CROWN;
        }
        if (v >= HEAD_FROM) {
            if (side >= CHEEK_SIDE && z >= -halfWidth * 0.4D) {
                return left ? TactilityAction.CHEEK_LEFT : TactilityAction.CHEEK_RIGHT;
            }
            return TactilityAction.HEAD;
        }
        if (v >= ARM_FROM && side >= ARM_SIDE) {
            return left ? TactilityAction.ARM_LEFT : TactilityAction.ARM_RIGHT;
        }
        if (v >= CHEST_FROM) {
            if (!front) {
                return TactilityAction.BACK;
            }
            return left ? TactilityAction.CHEST_LEFT : TactilityAction.CHEST_RIGHT;
        }
        if (v >= BELLY_FROM) {
            return front ? TactilityAction.BELLY : TactilityAction.BACK;
        }
        if (v >= GROIN_FROM && front && side < GROIN_SIDE) {
            return TactilityAction.GROIN;
        }
        return left ? TactilityAction.LEG_LEFT : TactilityAction.LEG_RIGHT;
    }
}

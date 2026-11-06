package ua.solly.kito.entity.interact.sex;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.solly.kito.KitoMod;

public final class KitoSexNet {
    private static boolean registered;

    private KitoSexNet() {
    }

    public record Thrust() implements CustomPacketPayload {
        public static final Type<Thrust> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "sex_thrust"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Thrust> CODEC = StreamCodec.unit(new Thrust());

        @Override
        public Type<Thrust> type() {
            return TYPE;
        }
    }

    public record State(boolean active, int entityId, int thrustsLeft) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "sex_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, msg) -> {
                    buf.writeBoolean(msg.active());
                    buf.writeInt(msg.entityId());
                    buf.writeByte(msg.thrustsLeft());
                },
                buf -> new State(buf.readBoolean(), buf.readInt(), buf.readByte())
        );

        @Override
        public Type<State> type() {
            return TYPE;
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        KitoMod.addNetworkMessage(Thrust.TYPE, Thrust.CODEC, KitoSexNet::onThrust);
        KitoMod.addNetworkMessage(State.TYPE, State.CODEC, KitoSexNet::onState);
    }

    private static void onThrust(Thrust msg, IPayloadContext ctx) {
        if (ctx.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                KitoSexScene.onThrust(sp);
            }
        });
    }

    private static void onState(State msg, IPayloadContext ctx) {
        if (ctx.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        ctx.enqueueWork(() -> KitoSexClient.onState(msg.active(), msg.entityId(), msg.thrustsLeft()));
    }

    public static void sendState(ServerPlayer player, boolean active, int entityId, int thrustsLeft) {
        PacketDistributor.sendToPlayer(player, new State(active, entityId, thrustsLeft));
    }
}

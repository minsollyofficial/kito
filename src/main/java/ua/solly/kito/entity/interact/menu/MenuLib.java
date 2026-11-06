package ua.solly.kito.entity.interact.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.sex.KitoPoseManager;
import ua.solly.kito.entity.interact.sex.SexAction;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.interact.tactility.TactilityScene;
import ua.solly.kito.entity.games.KitoGames;
import ua.solly.kito.entity.interact.kiss.KitoKiss;
import ua.solly.kito.entity.relation.Disposition;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.entity.relation.MoodEvent;

public final class MenuLib {
    private static final double SERVER_RANGE_SQR = 8.0D * 8.0D;
    private static boolean registered;

    private MenuLib() {
    }

    public record Select(int entityId, int action) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "menu_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Select message) -> {
                    buf.writeInt(message.entityId());
                    buf.writeByte(message.action());
                },
                (RegistryFriendlyByteBuf buf) -> new Select(buf.readInt(), buf.readByte())
        );

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    public record Hold(int entityId, boolean open) implements CustomPacketPayload {
        public static final Type<Hold> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KitoMod.MODID, "menu_hold"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hold> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Hold message) -> {
                    buf.writeInt(message.entityId());
                    buf.writeBoolean(message.open());
                },
                (RegistryFriendlyByteBuf buf) -> new Hold(buf.readInt(), buf.readBoolean())
        );

        @Override
        public Type<Hold> type() {
            return TYPE;
        }
    }

    public static void registerNetwork() {
        if (registered) {
            return;
        }
        registered = true;
        KitoMod.addNetworkMessage(Select.TYPE, Select.CODEC, MenuLib::onSelect);
        KitoMod.addNetworkMessage(Hold.TYPE, Hold.CODEC, MenuLib::onHold);
    }

    private static void onSelect(Select message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handle(player, message);
            }
        });
    }

    private static void onHold(Hold message, IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                handleHold(player, message);
            }
        });
    }

    private static void handleHold(ServerPlayer player, Hold message) {
        Entity entity = player.serverLevel().getEntity(message.entityId());
        if (!(entity instanceof KitoCharacter kito) || !kito.isAlive()) {
            return;
        }
        if (!message.open()) {
            kito.setMenuUser(player, false);
            return;
        }
        if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(kito) > SERVER_RANGE_SQR) {
            return;
        }
        kito.setMenuUser(player, true);
    }

    private static void handle(ServerPlayer player, Select message) {
        Entity entity = player.serverLevel().getEntity(message.entityId());
        if (!(entity instanceof KitoCharacter kito) || !kito.isAlive()) {
            return;
        }
        MenuAction action = MenuAction.byOrdinal(message.action());
        if (action == null || !action.availableFor(kito)) {
            return;
        }
        if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(kito) > SERVER_RANGE_SQR) {
            return;
        }
        if (denied(kito, player, action)) {
            return;
        }
        switch (action) {
            case FOLLOW -> kito.commandFollow(player);
            case HOME -> handleHome(player, kito);
            case TOUCH -> TactilityScene.start(player, kito);
            case SEX -> KitoPoseManager.tryStart(kito, player);
            case KISS -> KitoKiss.start(kito, player);
            case BWJB -> KitoPoseManager.tryStart(kito, player, SexAction.BWJB);
            case HIDE_AND_SEEK -> KitoGames.startHideAndSeek(kito, player);
            case TAG -> KitoGames.startTag(kito, player);
            case GIFT -> kito.giveHeldItem(player);
            case INVENTORY -> kito.openInventory(player);
        }
    }

    private static boolean denied(KitoCharacter kito, ServerPlayer player, MenuAction action) {
        KitoRelation relation = kito.peekRelation(player);

        if (action == MenuAction.HOME && !kito.home().isOwnedBy(player.getUUID()) && kito.home().has()) {
            return deny(kito, player, "kito.menu.deny.home_taken");
        }

        boolean ownTrustCheck = action == MenuAction.SEX || action == MenuAction.BWJB;
        if (!ownTrustCheck && relation.trust() < action.requiredTrust(kito, player)) {
            return deny(kito, player, action.denyKey());
        }

        if (action.isIntimate() && kito.hormones() != null && kito.hormones().isStressed()) {
            return deny(kito, player, "kito.menu.deny.stress");
        }

        if (action == MenuAction.FOLLOW && !kito.isFollowedBy(player)) {
            if (relation.isHurt()) {
                return deny(kito, player, "kito.menu.deny.follow_hurt");
            }
            if (kito.hasAnyFollower()) {
                return deny(kito, player, "kito.menu.deny.follow_taken");
            }
        }

        if (action == MenuAction.INVENTORY && kito.dispositionTo(player) == Disposition.AGGRESSIVE) {
            return deny(kito, player, "kito.menu.deny.inventory");
        }
        return false;
    }

    private static boolean deny(KitoCharacter kito, ServerPlayer player, String key) {
        TactilityLib.say(kito, player, key);
        kito.moodEvent(MoodEvent.DENIED_ACTION);
        return true;
    }

    private static void handleHome(ServerPlayer player, KitoCharacter kito) {
        if (kito.home().isOwnedBy(player.getUUID())) {
            kito.home().clear();
            TactilityLib.say(kito, player, "kito.menu.home_clear_ok");
            return;
        }
        if (kito.home().has()) {
            TactilityLib.say(kito, player, "kito.menu.deny.home_taken");
            kito.moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        BlockPos pos = player.blockPosition();
        kito.home().set(pos, player.getUUID());
        kito.stopFollowing();
        TactilityLib.say(kito, player, "kito.menu.home_set");
    }
}

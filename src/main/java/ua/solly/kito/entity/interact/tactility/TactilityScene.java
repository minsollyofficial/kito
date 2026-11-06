package ua.solly.kito.entity.interact.tactility;

import net.minecraft.client.Minecraft;
import ua.solly.kito.particle.KitoHearts;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.Gender;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.relation.Disposition;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.entity.relation.TrustAction;
import ua.solly.kito.init.KitoModSounds;
import ua.solly.kito.entity.HudSystems;
import ua.solly.kito.entity.relation.KitoTrust;
import ua.solly.kito.entity.relation.MoodEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = KitoMod.MODID)
public final class TactilityScene {
    public enum End {
        PLAYER,
        DISTANCE,
        TRUST,
        HURT,
        INVALID,
        LOGOUT
    }

    private static final Map<UUID, TactilityScene> SCENES = new HashMap<>();
    private static final Vector3f ZERO = new Vector3f();
    private static final int CARESS_COOLDOWN = 100;

    private final ServerPlayer player;
    private final KitoCharacter kito;
    private final Vector3f pose = new Vector3f();
    private TactilityAction held;
    private float energy;
    private float lastHealth;
    private int tier;
    private long lastStep;
    private long lastInput;
    private long nextCaress;

    private TactilityScene(ServerPlayer player, KitoCharacter kito, int tier) {
        this.player = player;
        this.kito = kito;
        this.tier = tier;
        this.lastHealth = player.getHealth();
    }

    public static TactilityScene of(Player player) {
        return SCENES.get(player.getUUID());
    }

    static void handle(ServerPlayer player, TactilityLib.Request message) {
        if (message.op() == TactilityLib.OP_EXIT) {
            end(player, End.PLAYER);
            return;
        }
        if (message.op() != TactilityLib.OP_ENTER || SCENES.containsKey(player.getUUID())) {
            return;
        }
        Entity entity = player.serverLevel().getEntity(message.entityId());
        if (entity instanceof KitoCharacter kito) {
            start(player, kito);
        }
    }

    static void handle(ServerPlayer player, TactilityLib.Touch message) {
        TactilityScene scene = SCENES.get(player.getUUID());
        if (scene == null) {
            return;
        }
        switch (message.op()) {
            case TactilityLib.TOUCH_START -> scene.begin(TactilityAction.byOrdinal(message.part()));
            case TactilityLib.TOUCH_MOVE -> scene.stroke(message.right(), message.up());
            case TactilityLib.TOUCH_END -> scene.release();
            default -> {
            }
        }
    }

    public static void start(ServerPlayer player, KitoCharacter kito) {
        if (SCENES.containsKey(player.getUUID())) {
            return;
        }
        if (kito.gender() != Gender.FEMALE || !kito.isAlive() || kito.isRemoved()) {
            return;
        }
        if (player.isSpectator() || !player.isAlive() || player.isPassenger() || player.level() != kito.level()) {
            return;
        }
        double range = TactilityLib.SESSION_RANGE;
        if (player.distanceToSqr(kito) > range * range) {
            return;
        }
        KitoRelation relation = kito.peekRelation(player);
        if (relation.trust() < KitoTrust.TOUCH) {
            TactilityLib.say(kito, player, "tactility.kito.deny.trust");
            return;
        }
        if (kito.tactileWith() != null || kito.getTarget() != null || kito.isOnFire()) {
            TactilityLib.say(kito, player, "tactility.kito.deny.busy");
            return;
        }
        if (relation.isHurt()) {
            TactilityLib.say(kito, player, "tactility.kito.deny.hurt");
            return;
        }
        if (relation.isDisgustLocked() || kito.dispositionTo(player) == Disposition.AGGRESSIVE) {
            TactilityLib.say(kito, player, "tactility.kito.deny.angry");
            return;
        }
        TactilityScene scene = new TactilityScene(player, kito, TactilityLib.tierOf(relation));
        SCENES.put(player.getUUID(), scene);
        kito.setTactileWith(player.getUUID());
        EntityMover.hold(kito);
        TactilityLib.sendState(player, true, kito.getId(), scene.tier);
    }

    public static void end(ServerPlayer player, End reason) {
        TactilityScene scene = SCENES.remove(player.getUUID());
        if (scene != null) {
            scene.close(reason);
        }
    }

    public static void abort(KitoCharacter kito, End reason) {
        UUID id = kito.tactileWith();
        if (id == null) {
            return;
        }
        TactilityScene scene = SCENES.get(id);
        if (scene != null) {
            end(scene.player, reason);
        } else {
            kito.setTactileWith(null);
        }
    }

    private void close(End reason) {
        if (player.getUUID().equals(kito.tactileWith())) {
            kito.setTactileWith(null);
        }
        kito.setTouch(-1, ZERO);
        if (reason == End.LOGOUT) {
            return;
        }
        TactilityLib.sendState(player, false, kito.getId(), 0);
        if (reason == End.TRUST) {
            TactilityLib.say(kito, player, "tactility.kito.ended");
        }
    }

    private long now() {
        return kito.level().getGameTime();
    }

    private void begin(TactilityAction part) {
        if (part == null) {
            return;
        }
        if (!part.allowedAt(kito.peekRelation(player).trust())) {
            TactilityLib.say(kito, player, "tactility.kito.deny.too_soon");
            kito.moodEvent(MoodEvent.DENIED_ACTION);
            TactilityLib.sendState(player, true, kito.getId(), tier);
            return;
        }
        held = part;
        energy = 0.0F;
        lastInput = now();
    }

    private void release() {
        held = null;
        energy = 0.0F;
        pose.set(0.0F, 0.0F, 0.0F);
    }

    private void stroke(float right, float up) {
        if (held == null || !Float.isFinite(right) || !Float.isFinite(up)) {
            return;
        }
        right = Mth.clamp(right, -TactilityLib.STROKE_CAP, TactilityLib.STROKE_CAP);
        up = Mth.clamp(up, -TactilityLib.STROKE_CAP, TactilityLib.STROKE_CAP);
        long now = now();
        lastInput = now;

        Vector3f local = EntityMover.toLocal(player, kito, right, up);
        float reference = TactilityLib.POSE_REFERENCE;
        pose.x = Mth.clamp(pose.x - local.x / reference, -1.0F, 1.0F);
        pose.y = Mth.clamp(pose.y - local.y / reference, -1.0F, 1.0F);
        pose.z = Mth.clamp(pose.z - local.z / reference, -1.0F, 1.0F);

        energy = Math.min(energy + (float) Math.hypot(right, up), TactilityLib.STROKE_STEP * 3.0F);
        if (energy >= TactilityLib.STROKE_STEP && now - lastStep >= TactilityLib.STEP_COOLDOWN) {
            energy -= TactilityLib.STROKE_STEP;
            lastStep = now;
            step();
        }
    }

    private void caress() {
        long now = now();
        if (now < nextCaress) {
            return;
        }
        nextCaress = now + CARESS_COOLDOWN;
        kito.level().playSound(null, kito.getX(), kito.getY() + kito.getBbHeight() * 0.6D, kito.getZ(),
                KitoModSounds.CARESS.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void step() {
        caress();
        if (held.petting()) {
            kito.react(player, TrustAction.PET);
        }
        if (held == TactilityAction.CROWN || held.intimate()) {
            KitoHearts.spawn(kito);
        }
        if (held.intimate()) {
            float gain = held.arousal() * kito.arousalFactor();
            kito.relation(player).addArousal(gain);
            kito.onIntimateTouch(gain);
            kito.touchMemory(player).lastIntimateTick = now();
            kito.moodEvent(MoodEvent.INTIMATE);
            KitoHearts.burst(kito, 3, held.height());
        }
    }

    private void tick() {
        if (kito.isRemoved() || !kito.isAlive() || !player.isAlive() || player.isSpectator()
                || player.isPassenger() || player.level() != kito.level()) {
            end(player, End.INVALID);
            return;
        }
        double range = TactilityLib.SESSION_RANGE;
        if (player.distanceToSqr(kito) > range * range) {
            end(player, End.DISTANCE);
            return;
        }
        float health = player.getHealth();
        if (health < lastHealth) {
            end(player, End.HURT);
            return;
        }
        lastHealth = health;
        if (kito.isOnFire()) {
            end(player, End.INVALID);
            return;
        }

        KitoRelation relation = kito.peekRelation(player);
        int currentTier = TactilityLib.tierOf(relation);
        if (currentTier == 0 || kito.dispositionTo(player) == Disposition.AGGRESSIVE) {
            end(player, End.TRUST);
            return;
        }
        if (currentTier != tier) {
            tier = currentTier;
            TactilityLib.sendState(player, true, kito.getId(), tier);
        }

        if (held != null) {
            if (!held.allowedAt(relation.trust())) {
                release();
                TactilityLib.say(kito, player, "tactility.kito.stop");
            } else if (now() - lastInput > TactilityLib.HOLD_TIMEOUT) {
                release();
            }
        }

        pose.mul(0.7F);
        EntityMover.sync(kito, held, held == null ? ZERO : pose);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SCENES.isEmpty()) {
            return;
        }
        for (TactilityScene scene : new ArrayList<>(SCENES.values())) {
            if (SCENES.get(scene.player.getUUID()) == scene) {
                scene.tick();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player, End.LOGOUT);
            TactilityLib.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            end(player, End.INVALID);
        }
    }

    @EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
    public static final class Client {
        private static final int HOVER_COLOR = 0xFFFFFF;
        private static final int HELD_COLOR = 0xFFFF55;
        private static final int HINT_COLOR = 0xAAAAAA;

        private static LocalPlayer owner;
        private static boolean active;
        private static boolean useDown;
        private static boolean blockUse;
        private static int targetId = -1;
        private static int tier;
        private static int holdTicks;
        private static TactilityAction hovered;
        private static TactilityAction held;
        private static float lockYaw;
        private static float lockPitch;
        private static float accRight;
        private static float accUp;

        private Client() {
        }

        public static void onState(boolean on, int entityId, int newTier) {
            if (!on) {
                clear();
                return;
            }
            active = true;
            targetId = entityId;
            tier = newTier;
            if (held != null && !TactilityLib.allowedByTier(held, tier)) {
                held = null;
                accRight = 0.0F;
                accUp = 0.0F;
            }
        }

        public static boolean isActive() {
            return active;
        }

        public static TactilityAction held() {
            return held;
        }

        public static int targetId() {
            return targetId;
        }

        private static void clear() {
            active = false;
            targetId = -1;
            tier = 0;
            holdTicks = 0;
            hovered = null;
            held = null;
            accRight = 0.0F;
            accUp = 0.0F;
        }

        private static void exit() {
            if (active) {
                send(new TactilityLib.Request(TactilityLib.OP_EXIT, targetId));
            }
            clear();
            blockUse = true;
        }

        private static void drain(LocalPlayer player) {
            if (held == null) {
                return;
            }
            accRight += Mth.wrapDegrees(player.getYRot() - lockYaw);
            accUp -= player.getXRot() - lockPitch;
            player.setYRot(lockYaw);
            player.setXRot(lockPitch);
            player.yRotO = lockYaw;
            player.xRotO = lockPitch;
        }

        private static void send(CustomPacketPayload payload) {
            PacketDistributor.sendToServer(payload);
        }

        private static void beginHold(LocalPlayer player) {
            held = hovered;
            lockYaw = player.getYRot();
            lockPitch = player.getXRot();
            accRight = 0.0F;
            accUp = 0.0F;
            holdTicks = 0;
            send(new TactilityLib.Touch(TactilityLib.TOUCH_START, held.ordinal(), 0.0F, 0.0F));
        }

        private static void endHold() {
            if (held == null) {
                return;
            }
            send(new TactilityLib.Touch(TactilityLib.TOUCH_END, held.ordinal(), 0.0F, 0.0F));
            held = null;
            hovered = null;
            accRight = 0.0F;
            accUp = 0.0F;
        }

        private static void sendStroke() {
            holdTicks++;
            float right = accRight;
            float up = accUp;
            accRight = 0.0F;
            accUp = 0.0F;
            if (Math.abs(right) + Math.abs(up) > 0.001F || holdTicks % 20 == 0) {
                send(new TactilityLib.Touch(TactilityLib.TOUCH_MOVE, held.ordinal(), right, up));
            }
        }

        @SubscribeEvent
        public static void onFramePre(RenderFrameEvent.Pre event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                drain(player);
            }
        }

        @SubscribeEvent
        public static void onTickPre(ClientTickEvent.Pre event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                drain(player);
            }
        }

        @SubscribeEvent
        public static void onTickPost(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) {
                clear();
                useDown = false;
                blockUse = false;
                owner = null;
                return;
            }
            if (owner != player) {
                clear();
                owner = player;
            }

            boolean down = mc.screen == null && mc.options.keyUse.isDown();
            boolean pressed = down && !useDown;
            useDown = down;
            if (!down) {
                blockUse = false;
            }

            if (!active) {
                return;
            }

            player.setSprinting(false);
            Entity target = mc.level.getEntity(targetId);
            if (!(target instanceof KitoCharacter) || !target.isAlive() || player.isDeadOrDying()) {
                send(new TactilityLib.Request(TactilityLib.OP_EXIT, targetId));
                clear();
                return;
            }

            if (held != null) {
                drain(player);
                if (!down) {
                    endHold();
                } else {
                    sendStroke();
                }
                return;
            }

            if (!pressed || hovered == null) {
                return;
            }
            if (TactilityLib.allowedByTier(hovered, tier)) {
                beginHold(player);
            } else {
                send(new TactilityLib.Touch(TactilityLib.TOUCH_START, hovered.ordinal(), 0.0F, 0.0F));
            }
        }

        @SubscribeEvent
        public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            boolean cancel = active || (blockUse && event.isUseItem());
            if (active && event.isAttack()) {
                exit();
            }
            if (cancel) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onMovement(MovementInputUpdateEvent event) {
            if (!active) {
                return;
            }
            Input input = event.getInput();
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
        }

        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
            if (held == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) {
                return;
            }
            float partial = (float) event.getPartialTick();
            float yawOffset = Mth.wrapDegrees(player.getViewYRot(partial) - lockYaw);
            float pitchOffset = player.getViewXRot(partial) - lockPitch;
            if (mc.options.getCameraType().isMirrored()) {
                pitchOffset = -pitchOffset;
            }
            event.setYaw(event.getYaw() - yawOffset);
            event.setPitch(event.getPitch() - pitchOffset);
        }

        @SubscribeEvent
        public static void onHud(RenderGuiEvent.Post event) {
            if (!active) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null || mc.options.hideGui || mc.screen != null) {
                return;
            }
            if (held == null) {
                float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
                Entity target = mc.level.getEntity(targetId);
                hovered = target instanceof KitoCharacter kito ? TactilityLib.pick(player, kito, partial) : null;
            }
            TactilityAction shown = held != null ? held : hovered;
            HudSystems.draw(
                    event.getGuiGraphics(),
                    HudSystems.trustRow(targetId),
                    shown == null ? null : new HudSystems.Row(shown.label(), held != null ? HELD_COLOR : HOVER_COLOR),
                    new HudSystems.Row(Component.translatable("tactility.kito.hint"), HINT_COLOR)
            );
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
            useDown = false;
            blockUse = false;
            owner = null;
        }
    }
}

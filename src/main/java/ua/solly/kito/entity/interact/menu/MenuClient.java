package ua.solly.kito.entity.interact.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.network.PacketDistributor;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.HudSystems;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.tactility.TactilityScene;
import ua.solly.kito.init.KitoModSounds;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class MenuClient {
    private static final int HOLD_TICKS = 5;
    private static final int SYNC_INTERVAL = 10;
    private static final double RANGE_SQR = 6.0D * 6.0D;
    private static final int W = 132;
    private static final int H = 20;
    private static final int GAP = 3;
    private static final int COLUMN_GAP = 8;
    private static final int MARGIN = 10;
    private static final int STATS_W = 90;
    private static final int STATS_ROW_H = 12;
    private static final int STATS_PAD = 4;
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHT = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");

    private enum Phase {
        IDLE,
        PENDING,
        OPEN
    }

    private static final class Node {
        private final String key;
        private final MenuAction action;
        private final List<Node> children;
        private final boolean back;

        private Node(String key, MenuAction action, List<Node> children, boolean back) {
            this.key = key;
            this.action = action;
            this.children = children;
            this.back = back;
        }

        private static Node leaf(MenuAction action) {
            return new Node(null, action, null, false);
        }

        private static Node folder(String key, List<Node> children) {
            return new Node(key, null, children, false);
        }

        private static Node back() {
            return new Node("kito.menu.back", null, null, true);
        }

        private boolean isFolder() {
            return children != null;
        }
    }

    private static final class Column {
        private final List<Node> nodes;
        private int selected;
        private float scroll;
        private float slide;

        private Column(List<Node> nodes) {
            this.nodes = nodes;
        }
    }

    private static Phase phase = Phase.IDLE;
    private static final List<Column> columns = new ArrayList<>();
    private static int targetId = -1;
    private static int holdTicks;
    private static int syncTicks;
    private static boolean useDown;
    private static float slide;
    private static float lockYaw;
    private static float lockPitch;

    private MenuClient() {
    }

    private static KitoCharacter lookedAt(Minecraft mc) {
        return mc.crosshairPickEntity instanceof KitoCharacter kito && kito.isAlive() ? kito : null;
    }

    private static List<Node> leaves(KitoCharacter kito, MenuAction... actions) {
        List<Node> list = new ArrayList<>();
        for (MenuAction action : actions) {
            if (action.availableFor(kito)) {
                list.add(Node.leaf(action));
            }
        }
        return list;
    }

    private static List<Node> withBack(List<Node> nodes) {
        List<Node> list = new ArrayList<>(nodes);
        list.add(Node.back());
        return list;
    }

    private static List<Node> rootNodes(KitoCharacter kito) {
        List<Node> root = new ArrayList<>();
        List<Node> love = leaves(kito, MenuAction.SEX, MenuAction.TOUCH, MenuAction.BWJB, MenuAction.KISS);
        if (!love.isEmpty()) {
            root.add(Node.folder("kito.menu.love", withBack(love)));
        }
        List<Node> interaction = leaves(kito, MenuAction.GIFT, MenuAction.FOLLOW, MenuAction.HOME);
        interaction.add(Node.folder("kito.menu.games", withBack(leaves(kito, MenuAction.HIDE_AND_SEEK, MenuAction.TAG))));
        root.add(Node.folder("kito.menu.interaction", withBack(interaction)));
        root.addAll(leaves(kito, MenuAction.INVENTORY));
        return root;
    }

    private static void open(Minecraft mc, KitoCharacter kito) {
        LocalPlayer player = mc.player;
        columns.clear();
        columns.add(new Column(rootNodes(kito)));
        lockYaw = player.getYRot();
        lockPitch = player.getXRot();
        phase = Phase.OPEN;
        syncTicks = 0;
        announce(kito.getId(), true);
    }

    private static void announce(int entityId, boolean open) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new MenuLib.Hold(entityId, open));
        }
    }

    private static void idle() {
        if (phase == Phase.OPEN && targetId >= 0) {
            announce(targetId, false);
        }
        phase = Phase.IDLE;
        columns.clear();
    }

    private static void reset() {
        phase = Phase.IDLE;
        columns.clear();
        targetId = -1;
        holdTicks = 0;
        useDown = false;
        slide = 0.0F;
    }

    private static void release(Minecraft mc, LocalPlayer player, KitoCharacter kito) {
        Phase was = phase;
        idle();
        if (was == Phase.PENDING) {
            for (InteractionHand hand : InteractionHand.values()) {
                InteractionResult result = mc.gameMode.interact(player, kito, hand);
                if (result.consumesAction()) {
                    player.swing(hand);
                    break;
                }
            }
        }
    }

    private static void playScroll(Minecraft mc, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(KitoModSounds.MENU_SCROLL.get(), 1.0F, pitch));
    }

    private static void click(Minecraft mc) {
        if (phase != Phase.OPEN || mc.player == null || mc.level == null || columns.isEmpty()) {
            return;
        }
        Entity entity = mc.level.getEntity(targetId);
        if (!(entity instanceof KitoCharacter kito) || !kito.isAlive()) {
            idle();
            return;
        }
        Column column = columns.get(columns.size() - 1);
        if (column.selected < 0 || column.selected >= column.nodes.size()) {
            return;
        }
        Node node = column.nodes.get(column.selected);
        if (node.back) {
            columns.remove(columns.size() - 1);
            playScroll(mc, 0.85F);
            return;
        }
        if (node.isFolder()) {
            columns.add(new Column(node.children));
            playScroll(mc, 1.0F);
            return;
        }
        PacketDistributor.sendToServer(new MenuLib.Select(kito.getId(), node.action.ordinal()));
        idle();
    }

    private static void enforceLook(LocalPlayer player) {
        player.setYRot(lockYaw);
        player.setXRot(lockPitch);
        player.yRotO = lockYaw;
        player.xRotO = lockPitch;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }
        boolean down = mc.screen == null && mc.options.keyUse.isDown();
        boolean pressed = down && !useDown;
        useDown = down;
        if (TactilityScene.Client.isActive()) {
            idle();
            return;
        }
        if (phase == Phase.IDLE) {
            if (pressed) {
                KitoCharacter kito = lookedAt(mc);
                if (kito != null) {
                    phase = Phase.PENDING;
                    targetId = kito.getId();
                    holdTicks = 0;
                }
            }
            return;
        }
        Entity entity = mc.level.getEntity(targetId);
        if (!(entity instanceof KitoCharacter kito) || !kito.isAlive()
                || player.distanceToSqr(kito) > RANGE_SQR || player.isDeadOrDying() || mc.screen != null) {
            idle();
            return;
        }
        if (!down) {
            release(mc, player, kito);
            return;
        }
        holdTicks++;
        if (phase == Phase.PENDING && holdTicks >= HOLD_TICKS) {
            open(mc, kito);
        }
        if (phase == Phase.OPEN) {
            player.setSprinting(false);
            enforceLook(player);
            if (++syncTicks % SYNC_INTERVAL == 0) {
                announce(targetId, true);
            }
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (phase == Phase.OPEN && player != null) {
            enforceLook(player);
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (phase != Phase.OPEN || player == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float yawDelta = Mth.wrapDegrees(player.getViewYRot(partial) - lockYaw);
        float pitchDelta = player.getViewXRot(partial) - lockPitch;
        event.setYaw(event.getYaw() - yawDelta);
        event.setPitch(event.getPitch() - (mc.options.getCameraType().isMirrored() ? -pitchDelta : pitchDelta));
    }

    @SubscribeEvent
    public static void onMovement(MovementInputUpdateEvent event) {
        if (phase != Phase.OPEN) {
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
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (phase == Phase.IDLE) {
            return;
        }
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (phase == Phase.PENDING && mc.level != null && mc.level.getEntity(targetId) instanceof KitoCharacter kito) {
            open(mc, kito);
        }
        if (phase != Phase.OPEN || columns.isEmpty()) {
            return;
        }
        Column column = columns.get(columns.size() - 1);
        int before = column.selected;
        double dy = event.getScrollDeltaY();
        if (dy > 0.0D) {
            column.selected = Math.max(0, column.selected - 1);
        } else if (dy < 0.0D) {
            column.selected = Math.min(column.nodes.size() - 1, column.selected + 1);
        }
        if (before != column.selected) {
            playScroll(mc, 1.25F);
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (TactilityScene.Client.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (event.isAttack()) {
            if (phase == Phase.OPEN) {
                event.setSwingHand(false);
                event.setCanceled(true);
                click(mc);
            } else if (phase == Phase.PENDING) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
            return;
        }
        if (!event.isUseItem()) {
            return;
        }
        if (phase != Phase.IDLE || lookedAt(mc) != null) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        float dt = event.getPartialTick().getRealtimeDeltaTicks();
        float goal = phase == Phase.OPEN ? 1.0F : 0.0F;
        slide += (goal - slide) * (1.0F - (float) Math.exp(-dt * 0.7F));
        float follow = 1.0F - (float) Math.exp(-dt * 0.9F);
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            column.slide += (1.0F - column.slide) * follow;
            column.scroll += (column.selected - column.scroll) * follow;
        }
        if (phase != Phase.OPEN && slide < 0.005F) {
            slide = 0.0F;
            columns.clear();
            return;
        }
        if (columns.isEmpty() || mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (!(mc.level.getEntity(targetId) instanceof KitoCharacter kito)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        Float trust = HudSystems.trustValue(targetId);
        float eased = 1.0F - (float) Math.pow(1.0F - slide, 3.0D);
        int leftX = Math.round(-W + (W + MARGIN) * eased);
        int centerY = graphics.guiHeight() / 2;
        RenderSystem.enableBlend();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            float columnEased = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, column.slide), 3.0D);
            int x = leftX + i * (W + COLUMN_GAP);
            float fade = 1.0F;
            if (i > 0) {
                x -= Math.round((W + COLUMN_GAP) * (1.0F - columnEased));
                fade = columnEased;
            }
            boolean active = i == columns.size() - 1;
            drawButtons(graphics, font, kito, mc.player, trust, column, x, centerY, active ? fade : fade * 0.5F);
        }
        if (columns.size() == 1) {
            drawStats(graphics, font, eased);
        }
        RenderSystem.disableBlend();
    }

    private static void drawButtons(GuiGraphics graphics, Font font, KitoCharacter kito, LocalPlayer player, Float trust,
                                    Column column, int x, int centerY, float fade) {
        for (int i = 0; i < column.nodes.size(); i++) {
            float d = i - column.scroll;
            float ad = Math.abs(d);
            if (ad > 1.6F) {
                continue;
            }
            float alpha = (ad <= 1.0F ? 1.0F : Math.max(0.0F, 1.0F - (ad - 1.0F) / 0.6F)) * fade;
            int a = (int) (alpha * 255.0F);
            if (a < 8) {
                continue;
            }
            Node node = column.nodes.get(i);
            boolean focus = i == column.selected;
            float required = node.action == null ? 0.0F : node.action.requiredTrust(kito, player);
            boolean enabled = trust == null || trust >= required;
            Component label = node.action != null ? node.action.label(kito, player) : Component.translatable(node.key);
            if (!enabled) {
                label = label.copy().append(Component.literal(" [" + Math.round(required) + "]"));
            }
            int y = Math.round(centerY - H / 2.0F + d * (H + GAP));
            ResourceLocation sprite = !enabled ? BUTTON_DISABLED : focus ? BUTTON_HIGHLIGHT : BUTTON;
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha * (focus ? 1.0F : 0.75F));
            graphics.blitSprite(sprite, x, y, W, H);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            int rgb = !enabled ? 0x707070 : focus ? 0xFFFFFF : 0xE0E0E0;
            int width = font.width(label);
            graphics.drawString(font, label, x + (W - width) / 2, y + (H - 8) / 2, (a << 24) | rgb, true);
            if (node.isFolder()) {
                graphics.drawString(font, ">", x + W - 10, y + (H - 8) / 2, (a << 24) | rgb, true);
            }
        }
    }

    private static void drawStats(GuiGraphics graphics, Font font, float eased) {
        if (targetId < 0) {
            return;
        }
        Component[] labels = {
                Component.translatable("kito.menu.stats.trust"),
                Component.translatable("kito.menu.stats.arousal"),
                Component.translatable("kito.menu.stats.disgust"),
                Component.translatable("kito.menu.stats.mood")
        };
        HudSystems.Row[] rows = {
                HudSystems.trustRow(targetId),
                HudSystems.arousalRow(targetId),
                HudSystems.disgustRow(targetId),
                HudSystems.moodRow(targetId)
        };
        int totalH = STATS_PAD * 2 + STATS_ROW_H * labels.length;
        int baseRight = graphics.guiWidth() + STATS_W;
        int baseLeft = graphics.guiWidth() - STATS_PAD;
        int right = Math.round(baseLeft + (baseRight - baseLeft) * (1.0F - eased));
        int x = right - STATS_W;
        int y = graphics.guiHeight() / 2 - totalH / 2;
        graphics.fill(x, y, x + STATS_W, y + totalH, 0xB0000000);
        graphics.fill(x, y, x + 1, y + totalH, 0xFFFFFFFF);
        int textLeft = x + STATS_PAD;
        int rowY = y + STATS_PAD;
        for (int i = 0; i < labels.length; i++) {
            HudSystems.Row value = rows[i];
            int color = value == null ? 0xFF707070 : value.color();
            Component valueText = value == null ? Component.literal("--") : value.text();
            graphics.drawString(font, labels[i], textLeft, rowY + 2, 0xFFE0E0E0, false);
            int valueWidth = font.width(valueText);
            graphics.drawString(font, valueText, textLeft + STATS_W - STATS_PAD * 2 - valueWidth, rowY + 2, color, false);
            rowY += STATS_ROW_H;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
}

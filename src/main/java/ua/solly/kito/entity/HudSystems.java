package ua.solly.kito.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import ua.solly.kito.KitoMod;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class HudSystems {
    public static final int TRUST_COLOR = 0xFFA855F7;
    public static final int AROUSAL_COLOR = 0xFFFF69B4;
    public static final int DISGUST_COLOR = 0xFF800020;
    public static final int MOOD_COLOR = 0xFFFFD700;

    private static final float TEXT_SCALE = 0.4F;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int PADDING = 3;
    private static final int BASE_Y = 8;
    private static final int ROW_HEIGHT = 7;
    private static final int STALE_TICKS = 40;

    public record Row(Component text, int color) {
    }

    private record TrustEntry(float trust, float arousal, float disgust, float mood, long receivedAt) {
    }

    private static final Map<Integer, TrustEntry> STATS = new HashMap<>();

    private HudSystems() {
    }

    public static void onStats(int entityId, float trust, float arousal, float disgust, float mood) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        STATS.put(entityId, new TrustEntry(trust, arousal, disgust, mood, mc.level.getGameTime()));
    }

    public static Row trustRow(int entityId) {
        TrustEntry entry = fresh(entityId);
        return entry == null ? null : new Row(Component.literal(Math.round(entry.trust()) + "%"), TRUST_COLOR);
    }

    public static Row arousalRow(int entityId) {
        TrustEntry entry = fresh(entityId);
        return entry == null ? null : new Row(Component.literal(Math.round(entry.arousal()) + "%"), AROUSAL_COLOR);
    }

    public static Row disgustRow(int entityId) {
        TrustEntry entry = fresh(entityId);
        return entry == null ? null : new Row(Component.literal(Math.round(entry.disgust()) + "%"), DISGUST_COLOR);
    }

    public static Row moodRow(int entityId) {
        TrustEntry entry = fresh(entityId);
        return entry == null ? null : new Row(Component.literal(Math.round(entry.mood()) + "%"), MOOD_COLOR);
    }

    private static TrustEntry fresh(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        TrustEntry entry = STATS.get(entityId);
        if (mc.level == null || entry == null || mc.level.getGameTime() - entry.receivedAt() > STALE_TICKS) {
            return null;
        }
        return entry;
    }

    public static void draw(GuiGraphics graphics, Row... rows) {
        Minecraft mc = Minecraft.getInstance();
        int index = 0;
        for (Row row : rows) {
            if (row == null) {
                continue;
            }
            drawRow(graphics, mc.font, row, index++);
        }
    }

    private static void drawRow(GuiGraphics graphics, Font font, Row row, int index) {
        int width = font.width(row.text());
        int height = font.lineHeight;
        int left = -width / 2;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F + BASE_Y + index * ROW_HEIGHT, 0.0F);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        graphics.fill(left - PADDING, -PADDING, left + width + PADDING, height + PADDING, BACKGROUND_COLOR);
        graphics.drawString(font, row.text(), left, 0, row.color(), false);
        pose.popPose();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATS.clear();
    }

    public static Float trustValue(int entityId) {
        TrustEntry entry = fresh(entityId);
        return entry == null ? null : entry.trust();
    }
}

package ua.solly.kito.entity.games;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import ua.solly.kito.KitoMod;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class KitoGamesClient {
    private static int remainingTicks;
    private static long receivedAt;

    private KitoGamesClient() {
    }

    public static void setTicks(int ticks) {
        remainingTicks = Math.max(0, ticks);
        Minecraft mc = Minecraft.getInstance();
        receivedAt = mc.level == null ? 0L : mc.level.getGameTime();
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (remainingTicks <= 0 || mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        long elapsed = Math.max(0L, mc.level.getGameTime() - receivedAt);
        int left = Math.max(0, remainingTicks - (int) elapsed);
        if (left <= 0) {
            return;
        }
        int seconds = (left + 19) / 20;
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        Component label = Component.translatable("kito.games.hud.invulnerable", seconds);
        int width = Math.max(106, font.width(label) + 12);
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = graphics.guiHeight() / 2 + 18;
        graphics.fill(x, y, x + width, y + 25, 0xB0000000);
        graphics.fill(x, y, x + width, y + 1, 0xCFFFFFFF);
        graphics.drawCenteredString(font, label, graphics.guiWidth() / 2, y + 4, 0xFFFFFFFF);
        int barWidth = width - 12;
        graphics.fill(x + 6, y + 17, x + 6 + barWidth, y + 21, 0xFF303030);
        graphics.fill(x + 6, y + 17, x + 6 + barWidth * left / 600, y + 21, 0xFF74D680);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        remainingTicks = 0;
        receivedAt = 0L;
    }
}

package ua.solly.kito.entity.interact.sex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import ua.solly.kito.KitoMod;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class KitoSexHud {
    private KitoSexHud() {
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        if (!KitoSexClient.isActive()) {
            return;
        }
        if (!KitoSexClient.isEngaged()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;

        Component space = Component.translatable("kito.sex.hud.space");

        int sw = font.width(space);

        graphics.drawString(font, space, cx - sw / 2, cy + 40, 0xFFFFFF, true);
    }
}

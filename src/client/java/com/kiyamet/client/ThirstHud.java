package com.kiyamet.client;

import com.kiyamet.Kiyamet;
import com.kiyamet.water.Thirst;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Water bar (10 droplets) above the hunger bar. */
public final class ThirstHud {
    static final Identifier ID = Kiyamet.id("thirst_bar");
    static final Identifier FULL = Kiyamet.id("hud/water_full"), HALF = Kiyamet.id("hud/water_half"), EMPTY = Kiyamet.id("hud/water_empty");

    static boolean shown(net.minecraft.world.entity.player.Player p) { return p != null && !p.isCreative() && !p.isSpectator(); }

    public static void init() {
        HudStatusBarHeightRegistry.addRight(ID, p -> shown(p) ? 10 : 0);
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ID, (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!shown(mc.player) || mc.options.hideGui) return;
            float w = mc.player.getAttachedOrElse(Thirst.WATER, 20f);
            int right = g.guiWidth() / 2 + 91, y = g.guiHeight() - HudStatusBarHeightRegistry.getHeight(ID);
            boolean shake = w <= 4 && mc.player.tickCount % 8 < 4;
            for (int i = 0; i < 10; i++) {
                int x = right - i * 8 - 9, yy = y + (shake && i % 2 == 0 ? 1 : 0);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, yy, 9, 9);
                float v = w - i * 2;
                if (v >= 2) g.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, yy, 9, 9);
                else if (v >= 1) g.blitSprite(RenderPipelines.GUI_TEXTURED, HALF, x, yy, 9, 9);
            }
        });
    }

    private ThirstHud() {}
}

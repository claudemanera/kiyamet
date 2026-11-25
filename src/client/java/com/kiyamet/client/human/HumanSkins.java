package com.kiyamet.client.human;

import com.kiyamet.Kiyamet;
import com.kiyamet.npc.Appearance;
import com.kiyamet.npc.Gear;
import com.kiyamet.npc.SkinPainter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Uploads the skin painted by {@link SkinPainter} for each look (appearance + gear) and caches it. */
public final class HumanSkins {
    private static final Map<Long, Identifier> CACHE = new HashMap<>();

    public static Identifier texture(int code, int gear) {
        long key = (code & 0xFFFFFFFFL) | (long) gear << 32;
        return CACHE.computeIfAbsent(key, k -> {
            int[] px = SkinPainter.paint(Appearance.unpack(code), Gear.unpack(gear), code * 31L + 7);
            NativeImage img = new NativeImage(64, 64, true);
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) img.setPixel(x, y, px[y * 64 + x]);
            Identifier id = Kiyamet.id("dynamic/human_" + Long.toHexString(k));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "kiyamet human " + k, img));
            return id;
        });
    }

    private HumanSkins() {}
}

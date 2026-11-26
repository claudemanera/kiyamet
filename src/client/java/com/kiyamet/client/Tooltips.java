package com.kiyamet.client;

import com.kiyamet.ammo.Ammo;
import com.kiyamet.item.GunItem;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Info text under every TWD item (tooltip.kiyamet.<id> in the lang file; guns show their live stats). */
public final class Tooltips {
    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("kiyamet")) return;
            String path = id.getPath();
            int at = Math.min(lines.size(), 1);
            if (stack.getItem() instanceof GunItem) {
                GunItem.Stats s = GunItem.statsFor(path);
                Ammo.Type t = Ammo.typeFor(path);
                String mode = switch (s.mode()) {
                    case SINGLE -> "single shot"; case DOUBLE -> "double shot"; case SHOTGUN -> "5 pellets"; case BURST3 -> "3-round burst"; case PIERCE -> "pierces through";
                    case EXPLODE -> "explosive"; case VOID -> "void pull"; case HARPOON -> "harpoon"; case TESLA -> "chain lightning"; case HOMING -> "homing"; case CANNON -> "cannon";
                };
                lines.add(at++, Component.literal("Damage " + fmt(s.damage()) + " · " + fmt(s.cooldown() / 20f) + " s between shots").withStyle(ChatFormatting.GRAY));
                lines.add(at++, Component.literal(mode.substring(0, 1).toUpperCase() + mode.substring(1) + " · " + (s.noise() == 0 ? "silent" : "heard " + s.noise() + " blocks away")).withStyle(ChatFormatting.GRAY));
                lines.add(at++, Component.literal("Ammo: " + t.label).withStyle(t.color));
                String eff = switch (s.effect()) { case 1 -> "Poisons"; case 2 -> "Freezes"; case 3 -> "Burns"; case 4 -> "Heavy: slows you while firing"; default -> null; };
                if (eff != null) lines.add(at++, Component.literal(eff).withStyle(ChatFormatting.DARK_AQUA));
                return;
            }
            String key = "tooltip.kiyamet." + path;
            if (!I18n.exists(key)) return;
            for (String l : I18n.get(key).split("\\|")) lines.add(at++, Component.literal(l).withStyle(ChatFormatting.GRAY));
        });
    }

    static String fmt(float f) { return f == (int) f ? String.valueOf((int) f) : String.format("%.1f", f); }

    private Tooltips() {}
}

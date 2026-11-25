package com.kiyamet.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Small helpers for the chest-style menus (named icons, lore, glint, frame panes, progress bars). */
public final class Ui {
    public static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }

    public static ItemStack icon(Item item, Component name, Component... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(st -> st.withItalic(false)));
        if (lore.length > 0) lore(s, List.of(lore));
        return s;
    }

    public static ItemStack lore(ItemStack s, List<Component> lines) {
        List<Component> out = new ArrayList<>();
        for (Component c : lines) out.add(c.copy().withStyle(st -> st.withItalic(false)));
        s.set(DataComponents.LORE, new ItemLore(out));
        return s;
    }

    public static ItemStack glint(ItemStack s, boolean on) {
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, on);
        return s;
    }

    public static ItemStack pane(Item pane) {
        ItemStack s = new ItemStack(pane);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return s;
    }

    public static ItemStack frame() { return pane(Items.GRAY_STAINED_GLASS_PANE); }
    public static ItemStack dark() { return pane(Items.BLACK_STAINED_GLASS_PANE); }

    public static Component text(String s, ChatFormatting... f) { return Component.literal(s).withStyle(f); }

    /** ■■■■■□□□□□ style bar, green filled part. */
    public static Component bar(double frac, int width) {
        int n = (int) Math.round(Math.max(0, Math.min(1, frac)) * width);
        return Component.literal("■".repeat(n)).withStyle(ChatFormatting.GREEN)
                .append(Component.literal("■".repeat(width - n)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private Ui() {}
}

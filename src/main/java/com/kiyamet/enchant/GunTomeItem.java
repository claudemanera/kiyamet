package com.kiyamet.enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** Blue gunsmith tomes: each teaches one gun enchantment. Used at the Enchantment Forge (sneak + right-click an enchanting table). */
public class GunTomeItem extends Item {
    public GunTomeItem(Properties p) { super(p); }

    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(ModEnchant.GUN_TOME);
        CompoundTag t = new CompoundTag();
        t.putString("Ench", id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Gunsmith Tome: " + Enchants.gunTitle(id)).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.AQUA)));
        String what = "";
        for (String[] g : Enchants.GUN) if (("kiyamet:" + g[0]).equals(id)) what = g[3] + " (max " + Enchants.roman(Integer.parseInt(g[2])) + ")";
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(what).withStyle(st -> st.withItalic(false).withColor(ChatFormatting.GRAY)),
                Component.literal("Arrow guns only · needs Archery 100+").withStyle(st -> st.withItalic(false).withColor(ChatFormatting.DARK_AQUA)),
                Component.literal("Hold your gun, sneak + right-click an enchanting table").withStyle(st -> st.withItalic(false).withColor(ChatFormatting.DARK_GRAY)))));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    public static ItemStack random(RandomSource r) { return of("kiyamet:" + Enchants.GUN[r.nextInt(Enchants.GUN.length)][0]); }

    public static String ench(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("Ench", "");
    }
}

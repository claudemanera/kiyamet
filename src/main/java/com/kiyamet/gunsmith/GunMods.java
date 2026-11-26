package com.kiyamet.gunsmith;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.List;

/** Attachments and paint stored on a gun. The item model shows them (custom_model_data strings + tint). */
public final class GunMods {
    public enum Slot { SCOPE, MUZZLE, LIGHT, MAG }

    public record Part(Slot slot, String id, String title, String effect, int eng, String[] cost) {}
    public record Paint(String id, String title, int color, String dye) {}

    public static final List<Part> PARTS = List.of(
            new Part(Slot.SCOPE, "red_dot", "Red Dot Sight", "+10% damage", 10, new String[]{"minecraft:iron_ingot", "2", "minecraft:redstone", "1"}),
            new Part(Slot.SCOPE, "holo", "Holo Sight", "+12% damage, easier to hit", 40, new String[]{"kiyamet:circuit", "1", "minecraft:glass_pane", "2"}),
            new Part(Slot.SCOPE, "acog", "ACOG Scope", "+25% range, +10% damage", 80, new String[]{"kiyamet:lens", "1", "minecraft:iron_ingot", "3"}),
            new Part(Slot.SCOPE, "sniper", "Sniper Scope", "+60% range, +20% damage, 15% slower", 150, new String[]{"kiyamet:lens", "2", "kiyamet:circuit", "1", "minecraft:iron_ingot", "4"}),
            new Part(Slot.MUZZLE, "short", "Short Silencer", "much quieter, -3% damage", 15, new String[]{"minecraft:iron_ingot", "3", "minecraft:white_wool", "1"}),
            new Part(Slot.MUZZLE, "long", "Long Silencer", "silent, +10% range, -6% damage", 60, new String[]{"kiyamet:metal_plate", "2", "minecraft:white_wool", "2"}),
            new Part(Slot.LIGHT, "flashlight", "Flashlight", "night vision while held", 20, new String[]{"minecraft:glowstone_dust", "2", "minecraft:iron_ingot", "2"}),
            new Part(Slot.LIGHT, "laser", "Laser Sight", "+5% damage, much easier to hit", 90, new String[]{"kiyamet:circuit", "1", "minecraft:redstone", "2", "kiyamet:lens", "1"}),
            new Part(Slot.MAG, "extended", "Extended Magazine", "20% faster firing", 20, new String[]{"minecraft:iron_ingot", "4"}),
            new Part(Slot.MAG, "drum", "Drum Magazine", "35% faster firing, a bit louder", 100, new String[]{"kiyamet:metal_plate", "2", "minecraft:iron_ingot", "4"}));

    public static final List<Paint> PAINTS = List.of(
            new Paint("none", "Factory", -1, ""), new Paint("olive", "Olive Drab", 0x8DA35A, "minecraft:green_dye"),
            new Paint("desert", "Desert Tan", 0xE0BE86, "minecraft:yellow_dye"), new Paint("arctic", "Arctic", 0xDCE8F2, "minecraft:white_dye"),
            new Paint("crimson", "Crimson", 0xD0605A, "minecraft:red_dye"), new Paint("navy", "Navy", 0x6680C8, "minecraft:blue_dye"),
            new Paint("gold", "Gold Plated", 0xF2CC55, "minecraft:gold_ingot"), new Paint("shadow", "Shadow", 0x62626E, "minecraft:black_dye"),
            new Paint("rose", "Rose", 0xEEA2CF, "minecraft:pink_dye"), new Paint("jungle", "Jungle", 0x68B884, "minecraft:lime_dye"),
            new Paint("walker", "Walker Blood", 0x7E9A62, "kiyamet:walker_guts"));

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    public static String get(ItemStack s, Slot slot) { return tag(s).getStringOr("Mod" + slot.name(), ""); }
    public static String paint(ItemStack s) { return tag(s).getStringOr("Paint", "none"); }
    public static boolean has(ItemStack s, String id) { for (Slot sl : Slot.values()) if (get(s, sl).equals(id)) return true; return false; }

    public static void set(ItemStack s, Slot slot, String id) {
        CompoundTag t = tag(s);
        if (id.isEmpty()) t.remove("Mod" + slot.name()); else t.putString("Mod" + slot.name(), id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        refresh(s);
    }

    public static void setPaint(ItemStack s, String id) {
        CompoundTag t = tag(s);
        t.putString("Paint", id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        refresh(s);
    }

    public static Part part(String id) { for (Part p : PARTS) if (p.id.equals(id)) return p; return null; }
    public static Paint paintOf(String id) { for (Paint p : PAINTS) if (p.id.equals(id)) return p; return PAINTS.get(0); }

    /** Rebuild the model data (what the gun looks like) and the lore (what's on it). */
    public static void refresh(ItemStack s) {
        List<String> strings = new ArrayList<>();
        for (Slot sl : Slot.values()) strings.add(get(s, sl).isEmpty() ? "none" : get(s, sl));
        Paint p = paintOf(paint(s));
        s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), strings, List.of(p.color)));
        List<Component> lore = new ArrayList<>();
        for (Slot sl : Slot.values()) {
            Part part = part(get(s, sl));
            if (part != null) lore.add(Component.literal("⚙ " + part.title + " (" + part.effect + ")").withStyle(st -> st.withItalic(false).withColor(net.minecraft.ChatFormatting.GRAY)));
        }
        if (!p.id.equals("none")) lore.add(Component.literal("Paint: " + p.title).withStyle(st -> st.withItalic(false).withColor(p.color)));
        if (lore.isEmpty()) s.remove(DataComponents.LORE); else s.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
    }

    private GunMods() {}
}

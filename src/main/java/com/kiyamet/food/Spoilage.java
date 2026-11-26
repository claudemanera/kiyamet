package com.kiyamet.food;

import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 1.7 food spoilage. Food gets a "best before" date the first time you carry it; when the date passes it turns into
 * Spoiled Food. The Fridge slows it down 4x, the freezer stops it. Recipes and canned food keep much longer.
 */
public final class Spoilage {
    public static final long DAY = 24000;

    /** Days a food keeps (0 = never spoils). */
    public static int life(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        return switch (id) {
            case "minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton", "minecraft:rabbit", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish" -> 3;
            case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_chicken", "minecraft:cooked_mutton", "minecraft:cooked_rabbit", "minecraft:cooked_cod", "minecraft:cooked_salmon" -> 6;
            case "minecraft:bread", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cake" -> 7;
            case "minecraft:mushroom_stew", "minecraft:rabbit_stew", "minecraft:beetroot_soup", "minecraft:suspicious_stew" -> 5;
            case "minecraft:sweet_berries", "minecraft:glow_berries", "kiyamet:strawberry", "minecraft:melon_slice" -> 4;
            case "minecraft:apple", "kiyamet:orange", "kiyamet:peach", "kiyamet:tomato", "kiyamet:cucumber", "kiyamet:eggplant", "kiyamet:bell_pepper", "kiyamet:cabbage" -> 8;
            case "minecraft:carrot", "minecraft:potato", "minecraft:beetroot", "kiyamet:corn", "kiyamet:onion", "minecraft:baked_potato" -> 12;
            case "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:golden_carrot",
                 "minecraft:honey_bottle", "minecraft:dried_kelp", "minecraft:chorus_fruit", "kiyamet:spoiled_food" -> 0;
            default -> {
                Recipes.Meal m = Recipes.meal(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath());
                yield m != null ? m.days() : 7;
            }
        };
    }

    public static boolean spoils(ItemStack s) { return s.has(DataComponents.FOOD) && life(s) > 0; }

    static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    public static long expires(ItemStack s) { return tag(s).getLongOr("Expires", -1L); }

    /** Gives fresh food its date (rounded to whole days so stacks still merge). */
    public static void stamp(Level level, ItemStack s) {
        if (expires(s) >= 0) return;
        long now = level.getGameTime();
        CompoundTag t = tag(s);
        t.putLong("Expires", (now / DAY + life(s)) * DAY + DAY / 2);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void delay(ItemStack s, long ticks) {
        long e = expires(s);
        if (e < 0) return;
        CompoundTag t = tag(s);
        t.putLong("Expires", e + ticks);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 13) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isCreative()) continue;
                Inventory inv = p.getInventory();
                long now = p.level().getGameTime();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.isEmpty() || !spoils(s)) continue;
                    stamp(p.level(), s);
                    if (now >= expires(s)) {
                        inv.setItem(i, new ItemStack(ModItems.get("spoiled_food"), s.getCount()));
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal(s.getHoverName().getString() + " went bad.").withStyle(net.minecraft.ChatFormatting.DARK_GREEN), true);
                    }
                }
            }
        });
    }

    private Spoilage() {}
}

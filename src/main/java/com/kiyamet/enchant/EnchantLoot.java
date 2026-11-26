package com.kiyamet.enchant;

import com.kiyamet.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/** Enchanted weapons found out in the world: raider drops, bosses, car trunks, story rewards. Stronger with your power. */
public final class EnchantLoot {
    private static final String[] NAMES = {"Old Faithful", "Widowmaker", "Lucille's Cousin", "Last Word", "Walker Bane", "The Persuader", "Grave Digger",
            "Mercy", "Bad News", "Sunday Best", "Rusty", "Peacemaker", "Silent Night", "Home Run", "No Return"};

    public static ItemStack weapon(ServerLevel level, int power) {
        RandomSource r = level.random;
        Item[] base = power >= 2000 ? new Item[]{Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.NETHERITE_SWORD, Items.BOW, Items.CROSSBOW, Items.TRIDENT}
                : new Item[]{Items.IRON_SWORD, Items.IRON_AXE, Items.DIAMOND_SWORD, Items.BOW, Items.CROSSBOW, Items.STONE_SWORD};
        ItemStack s;
        if (r.nextFloat() < 0.3f) s = new ItemStack(ModItems.get(ModItems.WEAPONS[r.nextInt(Math.min(ModItems.WEAPONS.length, 6 + power / 400))]));
        else s = new ItemStack(base[r.nextInt(base.length)]);
        List<Holder<Enchantment>> ok = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(h -> { if (h.value().canEnchant(s)) ok.add(h); });
        int n = 1 + r.nextInt(2 + Math.min(2, power / 1500));
        for (int i = 0; i < n && !ok.isEmpty(); i++) {
            Holder<Enchantment> h = ok.get(r.nextInt(ok.size()));
            if (!EnchantmentHelper.isEnchantmentCompatible(s.getEnchantments().keySet(), h)) continue;
            int max = Math.min(Enchants.maxCap(Enchants.id(h)), h.value().getMaxLevel() + power / 1500);
            s.enchant(h, 1 + r.nextInt(Math.max(1, max)));
        }
        if (r.nextFloat() < 0.35f) s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(NAMES[r.nextInt(NAMES.length)]).withStyle(st -> st.withItalic(false).withColor(net.minecraft.ChatFormatting.GOLD)));
        return s;
    }

    private EnchantLoot() {}
}

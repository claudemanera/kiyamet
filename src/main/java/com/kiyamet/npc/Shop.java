package com.kiyamet.npc;

import com.kiyamet.ui.Ui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Inventory helpers for payments. Prices written as "RUBY" are paid in dollars since 1.6 ($10 each). */
public final class Shop {
    public static final String RUBY = "kiyamet:ruby";

    public static int count(ServerPlayer p, Item it) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (s.is(it)) n += s.getCount(); }
        return n;
    }

    public static int count(ServerPlayer p, String id) { return id.equals(RUBY) ? com.kiyamet.money.Money.total(p) / com.kiyamet.money.Money.RATE : count(p, Ui.item(id)); }

    public static boolean take(ServerPlayer p, String id, int amount) {
        if (id.equals(RUBY)) return com.kiyamet.money.Money.take(p, amount * com.kiyamet.money.Money.RATE);   // 1.6: prices are paid in dollars
        Item it = Ui.item(id);
        if (p.isCreative()) return true;
        if (count(p, it) < amount) return false;
        for (int i = 0; i < p.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(it)) { int t = Math.min(amount, s.getCount()); s.shrink(t); amount -= t; }
        }
        return true;
    }

    public static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private Shop() {}
}

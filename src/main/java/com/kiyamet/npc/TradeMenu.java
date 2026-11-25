package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Community trader: buy supplies for rubies (top) or sell your loot for rubies (bottom). Good trades raise reputation. */
public class TradeMenu extends PanelMenu {
    record Offer(String item, int count, int rubies) {}

    static final List<Offer> SELL = List.of(
            new Offer("minecraft:bread", 6, 1), new Offer("minecraft:cooked_beef", 4, 2), new Offer("minecraft:arrow", 24, 1),
            new Offer("kiyamet:bandage", 3, 2), new Offer("kiyamet:medkit", 1, 5), new Offer("kiyamet:vaccine", 1, 10),
            new Offer("kiyamet:walker_guts", 3, 1), new Offer("kiyamet:fuel_canister", 2, 3), new Offer("kiyamet:metal_plate", 4, 3),
            new Offer("kiyamet:circuit", 2, 4), new Offer("minecraft:iron_sword", 1, 4), new Offer("minecraft:crossbow", 1, 6),
            new Offer("kiyamet:arrow_pistol", 1, 18), new Offer("kiyamet:barbed_wire", 8, 3), new Offer("minecraft:golden_apple", 1, 8),
            new Offer("kiyamet:repair_kit", 2, 4), new Offer("minecraft:saddle", 1, 6), new Offer("kiyamet:energy_cell", 1, 7), new Offer("kiyamet:radio", 1, 6));
    static final List<Offer> BUY = List.of(
            new Offer("minecraft:rotten_flesh", 32, 1), new Offer("minecraft:iron_ingot", 6, 1), new Offer("minecraft:gold_ingot", 4, 1),
            new Offer("minecraft:diamond", 1, 3), new Offer("minecraft:wheat", 24, 1), new Offer("minecraft:leather", 8, 1),
            new Offer("minecraft:coal", 16, 1), new Offer("minecraft:emerald", 2, 1), new Offer("kiyamet:tech_core", 1, 5));

    private final HumanEntity h;

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TradeMenu(id, inv, p, h), Component.literal("Trader: " + h.getName().getString())));
    }

    private TradeMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) {
        super(id, inv, p);
        this.h = h;
        redraw();
    }

    @Override
    protected void draw() {
        int rubies = Shop.count(player, Shop.RUBY);
        c.setItem(4, Ui.glint(Ui.icon(Ui.item(Shop.RUBY), Ui.text("Your rubies: " + rubies, ChatFormatting.RED, ChatFormatting.BOLD),
                Ui.text("Top: buy  |  Bottom: sell", ChatFormatting.GRAY), Ui.text("Reputation: " + Communities.reputation(player, h.community()), ChatFormatting.GREEN)), true));
        for (int i = 0; i < SELL.size(); i++) {
            Offer o = SELL.get(i);
            ItemStack s = new ItemStack(Ui.item(o.item()), o.count());
            Ui.lore(s, List.of(Ui.text("Buy for " + o.rubies() + " rubies", rubies >= o.rubies() ? ChatFormatting.GREEN : ChatFormatting.RED), Ui.text("Click to buy", ChatFormatting.YELLOW)));
            c.setItem(9 + i, s);
        }
        for (int i = 0; i < BUY.size(); i++) {
            Offer o = BUY.get(i);
            int have = Shop.count(player, o.item());
            ItemStack s = new ItemStack(Ui.item(o.item()), o.count());
            Ui.lore(s, List.of(Ui.text("Sell " + o.count() + " for " + o.rubies() + " ruby", have >= o.count() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY),
                    Ui.text("You have " + have, ChatFormatting.GRAY), Ui.text("Click to sell", ChatFormatting.YELLOW)));
            c.setItem(36 + i, Ui.glint(s, have >= o.count()));
        }
        for (int i = 27; i < 36; i++) c.setItem(i, Ui.frame());
        c.setItem(49, Ui.icon(net.minecraft.world.item.Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive()) { player.closeContainer(); return false; }
        if (slot == 49) { player.closeContainer(); return false; }
        if (slot >= 9 && slot < 9 + SELL.size()) {
            Offer o = SELL.get(slot - 9);
            if (!Shop.take(player, Shop.RUBY, o.rubies())) { player.displayClientMessage(Component.literal("Not enough rubies.").withStyle(ChatFormatting.RED), true); return false; }
            Shop.give(player, new ItemStack(Ui.item(o.item()), o.count()));
            Communities.change(player, h.community(), 1);
            return true;
        }
        if (slot >= 36 && slot < 36 + BUY.size()) {
            Offer o = BUY.get(slot - 36);
            if (!Shop.take(player, o.item(), o.count())) return false;
            Shop.give(player, new ItemStack(Ui.item(Shop.RUBY), o.rubies()));
            Communities.change(player, h.community(), 1);
            return true;
        }
        return false;
    }
}

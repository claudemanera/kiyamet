package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Pick what a companion should gather. */
public class GatherMenu extends PanelMenu {
    static final Item[] ICONS = {Items.CRAFTING_TABLE, Items.OAK_LOG, Items.COBBLESTONE, Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD, Items.DIAMOND, Items.SAND, Items.DIRT};
    static final int[] SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    private final HumanEntity h;

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GatherMenu(id, inv, p, h), Component.literal(h.getName().getString() + ": gather")));
    }

    private GatherMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) { super(id, inv, p); this.h = h; redraw(); }

    @Override
    protected void draw() {
        c.setItem(4, Ui.icon(Items.IRON_PICKAXE, Ui.text("What should " + h.getName().getString() + " gather?", ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Everything goes into their inventory", ChatFormatting.GRAY), Ui.text("They come back when the bag is full", ChatFormatting.GRAY)));
        for (int i = 0; i < Gatherer.KINDS.length; i++) {
            boolean now = h.order() == HumanEntity.Order.GATHER && h.gather.equals(Gatherer.KINDS[i][0]);
            c.setItem(SLOTS[i], Ui.glint(Ui.icon(ICONS[i], Ui.text(Gatherer.KINDS[i][1], i == 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN, ChatFormatting.BOLD),
                    Ui.text(i == 0 ? "Chops wood, makes tools, mines stone and iron," : "Click to send them", ChatFormatting.GRAY),
                    Ui.text(i == 0 ? "smelts it into iron armor, then mines ores for you" : "", ChatFormatting.GRAY)), now));
        }
        ItemStack hand = player.getMainHandItem();
        c.setItem(40, Ui.icon(hand.getItem() instanceof BlockItem ? hand.getItem() : Items.BARRIER, Ui.text("Gather the block in my hand", ChatFormatting.AQUA, ChatFormatting.BOLD),
                Ui.text(hand.getItem() instanceof BlockItem ? hand.getHoverName().getString() : "Hold a block (log, ore, stone...)", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.LEAD, Ui.text("Stop - follow me", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive() || !h.isOwnedBy(player)) { player.closeContainer(); return false; }
        String kind = null;
        for (int i = 0; i < SLOTS.length; i++) if (slot == SLOTS[i]) kind = Gatherer.KINDS[i][0];
        if (slot == 40 && player.getMainHandItem().getItem() instanceof BlockItem bi) kind = "block:" + BuiltInRegistries.BLOCK.getKey(bi.getBlock());
        if (slot == 49) { h.setOrder(HumanEntity.Order.FOLLOW); player.closeContainer(); return false; }
        if (kind == null) return false;
        h.gather = kind;
        h.setOrder(HumanEntity.Order.GATHER);
        Talk.say(player, h, kind.equals("develop") ? "On it. I'll start with some wood and work my way up." : "Got it. I'll bring it back.");
        player.closeContainer();
        return false;
    }
}

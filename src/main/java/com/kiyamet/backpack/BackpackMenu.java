package com.kiyamet.backpack;

import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Backpack contents. The last row is a toolbar (info, sort, pages, take off). Big packs (over 45 slots) have pages. */
public class BackpackMenu extends ChestMenu {
    public static final int PAGE = 45;
    private final ServerPlayer player;
    private final SimpleContainer box;
    private final BackpackTier tier;
    private final int rows, cap;
    private final boolean worn;
    private final int handSlot;          // inventory index of the backpack when opened from the hand, else -1
    private final List<ItemStack> data = new ArrayList<>();
    private int page;
    private ItemStack source;

    private static MenuType<ChestMenu> type(int rows) {
        return switch (rows) { case 1 -> MenuType.GENERIC_9x1; case 2 -> MenuType.GENERIC_9x2; case 3 -> MenuType.GENERIC_9x3; case 4 -> MenuType.GENERIC_9x4; case 5 -> MenuType.GENERIC_9x5; default -> MenuType.GENERIC_9x6; };
    }

    public static void openWorn(ServerPlayer p) {
        ItemStack s = Backpacks.worn(p);
        if (!(s.getItem() instanceof BackpackItem b)) {
            p.displayClientMessage(Component.literal("You aren't wearing a backpack. Sneak + right-click one to put it on.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        open(p, s, b.tier, true, -1);
    }

    public static void openFromHand(ServerPlayer p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!(s.getItem() instanceof BackpackItem b)) return;
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : p.getInventory().getSelectedSlot();
        open(p, s, b.tier, false, slot);
    }

    private static void open(ServerPlayer p, ItemStack s, BackpackTier t, boolean worn, int handSlot) {
        int rows = Math.min(6, t.rows() + 1);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BackpackMenu(id, inv, p, s, t, worn, handSlot, rows),
                Component.literal(t.title + "  (" + t.capacity + " slots)")));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private BackpackMenu(int id, Inventory inv, ServerPlayer p, ItemStack source, BackpackTier t, boolean worn, int handSlot, int rows) {
        this(id, inv, p, source, t, worn, handSlot, rows, new SimpleContainer(rows * 9));
    }

    private BackpackMenu(int id, Inventory inv, ServerPlayer p, ItemStack source, BackpackTier t, boolean worn, int handSlot, int rows, SimpleContainer box) {
        super(type(rows), id, inv, box, rows);
        this.player = p; this.box = box; this.tier = t; this.rows = rows; this.cap = t.capacity; this.worn = worn; this.handSlot = handSlot; this.source = source;
        ItemContainerContents contents = source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> items = new ArrayList<>();
        contents.stream().forEach(items::add);
        for (int i = 0; i < cap; i++) data.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        show();
    }

    private int pages() { return (cap + PAGE - 1) / PAGE; }
    /** usable slots on this page */
    private int view() { return Math.min(PAGE, cap - page * PAGE); }
    private int toolbar() { return (rows - 1) * 9; }

    /** box -> data for the page on screen */
    private void pull() { for (int i = 0; i < view(); i++) data.set(page * PAGE + i, box.getItem(i).copy()); }

    /** data -> box for the page on screen, plus chrome */
    private void show() {
        for (int i = 0; i < view(); i++) box.setItem(i, data.get(page * PAGE + i).copy());
        drawChrome();
    }

    private void drawChrome() {
        int v = view();
        for (int i = v; i < toolbar(); i++) box.setItem(i, Ui.icon(Items.GRAY_STAINED_GLASS_PANE, Ui.text(pages() > 1 ? "End of the backpack" : "Locked: upgrade the backpack", ChatFormatting.DARK_GRAY)));
        int tb = toolbar();
        for (int i = tb; i < tb + 9; i++) box.setItem(i, Ui.dark());
        int used = 0;
        for (ItemStack s : data) if (!s.isEmpty()) used++;
        BackpackTier next = BackpackTier.byIndex(tier.ordinal() + 1);
        box.setItem(tb + 4, Ui.glint(Ui.icon(Backpacks.ITEMS.get(tier), Ui.text(tier.title, ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Used " + used + " / " + cap + " slots", ChatFormatting.GRAY), Ui.bar(used / (double) cap, 18),
                Ui.text(pages() > 1 ? "Page " + (page + 1) + " / " + pages() : "", ChatFormatting.YELLOW),
                Ui.text(next == null ? "★ Biggest backpack" : "Upgrade: craft with 1 " + next.upgrade.substring(next.upgrade.indexOf(':') + 1).replace('_', ' ') + " → " + next.title + " (" + next.capacity + ")", ChatFormatting.AQUA)), true));
        box.setItem(tb + 2, Ui.icon(Items.HOPPER, Ui.text("Sort & stack", ChatFormatting.YELLOW)));
        if (worn) box.setItem(tb + 6, Ui.icon(Items.LEATHER, Ui.text("Take off backpack", ChatFormatting.RED), Ui.text("Moves it into your inventory", ChatFormatting.GRAY)));
        if (page > 0) box.setItem(tb, Ui.icon(Items.ARROW, Ui.text("◀ Page " + page, ChatFormatting.YELLOW)));
        if (page + 1 < pages()) box.setItem(tb + 8, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("Page " + (page + 2) + " ▶", ChatFormatting.YELLOW)));
    }

    private void save() {
        ItemStack target = worn ? Backpacks.worn(player) : handSlot >= 0 ? player.getInventory().getItem(handSlot) : ItemStack.EMPTY;
        if (!(target.getItem() instanceof BackpackItem)) return;
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : data) items.add(s.copy());
        target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        if (worn) Backpacks.setWorn(player, target);
    }

    private void sort() {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack s : data) if (!s.isEmpty()) all.add(s.copy());
        all.sort((a, b) -> a.getHoverName().getString().compareTo(b.getHoverName().getString()));
        SimpleContainer tmp = new SimpleContainer(cap);
        for (ItemStack s : all) tmp.addItem(s);
        for (int i = 0; i < cap; i++) data.set(i, tmp.getItem(i));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        int tb = toolbar();
        if (slot >= view() && slot < rows * 9) {
            pull();
            if (slot == tb + 2) { sort(); save(); show(); }
            else if (slot == tb && page > 0) { page--; show(); }
            else if (slot == tb + 8 && page + 1 < pages()) { page++; show(); }
            else if (slot == tb + 6 && worn) {
                save();
                ItemStack pack = Backpacks.worn(player);
                Backpacks.setWorn(player, ItemStack.EMPTY);
                player.closeContainer();
                if (!player.getInventory().add(pack)) player.drop(pack, false);
            }
            return;
        }
        if (slot >= rows * 9 && handSlot >= 0) {                     // never move the open backpack itself
            Slot s = slot < slots.size() ? slots.get(slot) : null;
            if (s != null && s.container == player.getInventory() && s.getContainerSlot() == handSlot) return;
        }
        if (type == ClickType.SWAP && (button == handSlot || button == 40 && handSlot == Inventory.SLOT_OFFHAND)) return;
        ItemStack carried = getCarried();
        if (slot >= 0 && slot < view() && carried.getItem() instanceof BackpackItem) return;   // no backpacks inside backpacks
        super.clicked(slot, button, type, p);
        pull();
        save();
        drawChrome();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot s = slots.get(index);
        if (!s.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = s.getItem();
        if (index < view()) {
            if (!moveItemStackTo(stack, rows * 9, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index >= rows * 9) {
            if (stack.getItem() instanceof BackpackItem) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 0, view(), false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        pull();
        save();
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player p) {
        pull();
        save();
        for (int i = 0; i < rows * 9; i++) box.setItem(i, ItemStack.EMPTY);          // contents live on the item, not in this temp box
        super.removed(p);
    }
}

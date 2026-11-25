package com.kiyamet.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Base for 6-row button panels: subclasses draw into {@link #c} in {@link #draw()} and react in {@link #click}. */
public abstract class PanelMenu extends ChestMenu {
    protected final SimpleContainer c;
    protected final ServerPlayer player;

    protected PanelMenu(int id, Inventory inv, ServerPlayer player) {
        this(id, inv, new SimpleContainer(54), player);
    }

    private PanelMenu(int id, Inventory inv, SimpleContainer c, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        this.c = c;
        this.player = player;
    }

    protected abstract void draw();

    /** @return true if something changed (redraw + click sound). */
    protected abstract boolean click(int slot, ClickType type);

    protected void redraw() {
        c.clearContent();
        for (int i = 0; i < 54; i++) c.setItem(i, i < 9 || i >= 45 ? Ui.dark() : ItemStack.EMPTY);
        draw();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, p); return; }
        if (click(slot, type)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4f, 1.2f);
            if (player.containerMenu == this) redraw();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }

    @Override
    public void removed(Player p) {
        c.clearContent();
        super.removed(p);
    }

    @Override
    public boolean stillValid(Player p) { return true; }

    public static Component title(String s) { return Component.literal(s); }
}

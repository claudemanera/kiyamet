package com.kiyamet.skill;

import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Lists every super power: unlocked ones glow when active and can be switched on/off with a click. */
public class PowersMenu extends ChestMenu {
    private final SimpleContainer shown;
    private final ServerPlayer player;
    private int page;

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new PowersMenu(id, inv, p), Component.literal("Super Powers")));
    }

    private PowersMenu(int id, Inventory inv, ServerPlayer p) { this(id, inv, new SimpleContainer(54), p); }

    private PowersMenu(int id, Inventory inv, SimpleContainer c, ServerPlayer p) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        this.shown = c;
        this.player = p;
        refresh();
    }

    private void refresh() {
        shown.clearContent();
        SkillData d = SkillManager.get(player);
        Powers.Power[] all = Powers.Power.values();
        int unlocked = 0;
        for (Powers.Power pw : all) if (pw.unlocked(d)) unlocked++;
        for (int i = 0; i < 45 && page * 45 + i < all.length; i++) {
            Powers.Power pw = all[page * 45 + i];
            boolean un = pw.unlocked(d), on = un && Powers.enabled(player, pw);
            ItemStack s = (un ? Ui.logo("power_" + pw.name().toLowerCase(), Ui.text(pw.title, on ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD)) : Ui.icon(Items.GRAY_DYE, Ui.text(pw.title, ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)));
            Ui.lore(s, java.util.List.of(
                    Ui.text(un ? (on ? "● Active" : "● Switched off") : "✘ Locked", !un ? ChatFormatting.DARK_GRAY : on ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.literal("Unlock: ").withStyle(ChatFormatting.GRAY).append(Component.translatable(pw.skill.key())).append(Component.literal(" " + pw.level + "  (you: " + d.level(pw.skill) + ")")),
                    un ? Ui.text("Click to switch " + (on ? "off" : "on"), ChatFormatting.YELLOW) : Ui.bar(d.level(pw.skill) / (double) pw.level, 16)));
            shown.setItem(i, Ui.glint(s, on));
        }
        for (int i = 45; i < 54; i++) shown.setItem(i, Ui.dark());
        shown.setItem(45, Ui.icon(Items.ARROW, Ui.text("Back to skills", ChatFormatting.YELLOW)));
        shown.setItem(49, Ui.glint(Ui.icon(Items.NETHER_STAR, Ui.text("Powers unlocked: " + unlocked + " / " + all.length, ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.bar(unlocked / (double) all.length, 20), Ui.text("Level up skills to earn more powers.", ChatFormatting.GRAY)), true));
        shown.setItem(53, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        int pages = (all.length + 44) / 45;
        if (page > 0) shown.setItem(47, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("< Previous page", ChatFormatting.AQUA)));
        if (page < pages - 1) shown.setItem(51, Ui.icon(Items.SPECTRAL_ARROW, Ui.text("Next page > (" + (page + 2) + "/" + pages + ")", ChatFormatting.AQUA)));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, p); return; }
        if (slot == 45) { SkillsMenu.open(player); return; }
        if (slot == 53) { player.closeContainer(); return; }
        Powers.Power[] all = Powers.Power.values();
        if (slot == 47 && page > 0) { page--; refresh(); return; }
        if (slot == 51 && (page + 1) * 45 < all.length) { page++; refresh(); return; }
        int idx = page * 45 + slot;
        if (slot < 45 && idx < all.length && all[idx].unlocked(SkillManager.get(player))) {
            Powers.toggle(player, all[idx]);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.3f);
            refresh();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player p) { return true; }
}

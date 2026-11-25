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
        for (int i = 0; i < all.length && i < 45; i++) {
            Powers.Power pw = all[i];
            boolean un = pw.unlocked(d), on = un && Powers.enabled(player, pw);
            if (un) unlocked++;
            ItemStack s = Ui.icon(un ? pw.icon : Items.GRAY_DYE, Ui.text(pw.title, !un ? ChatFormatting.DARK_GRAY : on ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD),
                    Ui.text(un ? (on ? "● Active" : "● Switched off") : "✘ Locked", !un ? ChatFormatting.DARK_GRAY : on ? ChatFormatting.GREEN : ChatFormatting.RED),
                    Component.literal("Unlock: ").withStyle(ChatFormatting.GRAY).append(Component.translatable(pw.skill.key())).append(Component.literal(" " + pw.level + "  (you: " + d.level(pw.skill) + ")")),
                    un ? Ui.text("Click to switch " + (on ? "off" : "on"), ChatFormatting.YELLOW) : Ui.bar(d.level(pw.skill) / (double) pw.level, 16));
            shown.setItem(i, Ui.glint(s, on));
        }
        for (int i = 45; i < 54; i++) shown.setItem(i, Ui.dark());
        shown.setItem(45, Ui.icon(Items.ARROW, Ui.text("Back to skills", ChatFormatting.YELLOW)));
        shown.setItem(49, Ui.glint(Ui.icon(Items.NETHER_STAR, Ui.text("Powers unlocked: " + unlocked + " / " + all.length, ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.bar(unlocked / (double) all.length, 20), Ui.text("Level up skills to earn more powers.", ChatFormatting.GRAY)), true));
        shown.setItem(53, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        if (slot < 0 || slot >= 54) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, p); return; }
        if (slot == 45) { SkillsMenu.open(player); return; }
        if (slot == 53) { player.closeContainer(); return; }
        Powers.Power[] all = Powers.Power.values();
        if (slot < all.length && all[slot].unlocked(SkillManager.get(player))) {
            Powers.toggle(player, all[slot]);
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.3f);
            refresh();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player p) { return true; }
}

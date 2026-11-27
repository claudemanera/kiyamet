package com.kiyamet.creation;

import com.kiyamet.element.Element;
import com.kiyamet.element.ElementItems;
import com.kiyamet.stone.StoneItem;
import com.kiyamet.stone.StoneKind;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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

/** Twelve stones become the Creation Element. Fourteen element weapons become the Creation Rod. */
public class CreationTableMenu extends ChestMenu {
    private final SimpleContainer shown;
    private final ServerPlayer player;

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CreationTableMenu(id, inv, p), Component.literal("Table of Creation")));
    }

    private CreationTableMenu(int id, Inventory inv, ServerPlayer p) { this(id, inv, new SimpleContainer(27), p); }

    private CreationTableMenu(int id, Inventory inv, SimpleContainer c, ServerPlayer p) {
        super(MenuType.GENERIC_9x3, id, inv, c, 3);
        this.shown = c;
        this.player = p;
        refresh();
    }

    static Set<StoneKind> stones(Player p) {
        Set<StoneKind> s = EnumSet.noneOf(StoneKind.class);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof StoneItem si) s.add(si.kind);
        return s;
    }

    static Set<Element> weapons(Player p) {
        Set<Element> s = EnumSet.noneOf(Element.class);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            for (Element e : Element.values()) if (p.getInventory().getItem(i).is(ElementItems.WEAPONS.get(e))) s.add(e);
        return s;
    }

    private void refresh() {
        shown.clearContent();
        for (int i = 0; i < 27; i++) shown.setItem(i, Ui.pane(Items.WHITE_STAINED_GLASS_PANE));
        Set<StoneKind> have = stones(player);
        List<Component> sl = new ArrayList<>();
        sl.add(Ui.text("Needs all 12 stones (loose, not in the gauntlet):", ChatFormatting.GRAY));
        for (StoneKind k : StoneKind.values())
            sl.add(Component.literal(have.contains(k) ? " ✔ " : " ✘ ").withStyle(have.contains(k) ? ChatFormatting.GREEN : ChatFormatting.RED).append(Component.translatable("item.kiyamet." + k.id).withStyle(ChatFormatting.WHITE)));
        sl.add(Ui.bar(have.size() / 12.0, 20));
        sl.add(Ui.text(have.size() == 12 ? "Click to create!" : have.size() + " / 12", have.size() == 12 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        shown.setItem(11, Ui.glint(Ui.lore(Ui.icon(CreationItems.CORE, Ui.text("Creation Element", ChatFormatting.WHITE, ChatFormatting.BOLD)), sl), have.size() == 12));

        Set<Element> w = weapons(player);
        List<Component> wl = new ArrayList<>();
        wl.add(Ui.text("Needs all 14 element weapons:", ChatFormatting.GRAY));
        for (Element e : Element.values())
            wl.add(Component.literal(w.contains(e) ? " ✔ " : " ✘ ").withStyle(w.contains(e) ? ChatFormatting.GREEN : ChatFormatting.RED).append(Component.literal(e.weaponTitle).withStyle(e.fmt)));
        wl.add(Ui.bar(w.size() / 14.0, 20));
        wl.add(Ui.text(w.size() == 14 ? "Click to create!" : w.size() + " / 14", w.size() == 14 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        shown.setItem(15, Ui.glint(Ui.lore(Ui.icon(CreationItems.ROD, Ui.text("Creation Rod", ChatFormatting.WHITE, ChatFormatting.BOLD)), wl), w.size() == 14));

        shown.setItem(13, Ui.glint(Ui.icon(CreationItems.AXE, Ui.text("Creation Axe", ChatFormatting.WHITE, ChatFormatting.BOLD),
                Ui.text("Hold the Creation Element and right-click an anvil", ChatFormatting.GRAY),
                Ui.text("with the Creation Rod in your inventory.", ChatFormatting.GRAY),
                Ui.text("It carries all fourteen powers.", ChatFormatting.AQUA)), true));
        shown.setItem(22, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    private void done(String what) {
        var l = player.level();
        l.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8f, 1.5f);
        l.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.2f);
        l.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.5, player.getZ(), 120, 1.2, 1.2, 1.2, 0.2);
        l.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.5, player.getZ(), 2, 0, 0, 0, 0);
        l.getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ " + player.getName().getString() + " has made the " + what + "!").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
        com.kiyamet.skill.SkillManager.addXp(player, com.kiyamet.skill.Skill.ENCHANTING, 8000);
    }

    private void give(ItemStack s) { if (!player.getInventory().add(s)) player.drop(s, false); }

    @Override
    public void clicked(int slot, int button, ClickType type, Player p) {
        if (slot < 0 || slot >= 27) { if (type != ClickType.QUICK_MOVE) super.clicked(slot, button, type, p); return; }
        if (slot == 22) { player.closeContainer(); return; }
        if (slot == 11 && stones(player).size() == StoneKind.values().length) {
            Set<StoneKind> need = EnumSet.allOf(StoneKind.class);
            for (int i = 0; i < player.getInventory().getContainerSize() && !need.isEmpty(); i++)
                if (player.getInventory().getItem(i).getItem() instanceof StoneItem si && need.remove(si.kind)) player.getInventory().getItem(i).shrink(1);
            give(new ItemStack(CreationItems.CORE));
            done("Creation Element");
        } else if (slot == 15 && weapons(player).size() == Element.values().length) {
            Set<Element> need = EnumSet.allOf(Element.class);
            for (int i = 0; i < player.getInventory().getContainerSize() && !need.isEmpty(); i++)
                for (Element e : Element.values())
                    if (need.contains(e) && player.getInventory().getItem(i).is(ElementItems.WEAPONS.get(e))) {
                        com.kiyamet.element.ElementPowers.stop(player, e);
                        player.getInventory().getItem(i).shrink(1); need.remove(e); break;
                    }
            give(new ItemStack(CreationItems.ROD));
            done("Creation Rod");
        }
        refresh();
    }

    @Override public ItemStack quickMoveStack(Player p, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player p) { return true; }
}

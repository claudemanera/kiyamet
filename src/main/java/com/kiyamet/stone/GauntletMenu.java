package com.kiyamet.stone;

import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Stone Gauntlet panel (left-click with the gauntlet in hand): 5 sockets that open with Total Power.
 * Click a socketed stone to select it, sneak-click to take it out; click a stone from your inventory to socket it.
 */
public class GauntletMenu extends PanelMenu {
    static final int[] SOCKETS = {20, 21, 22, 23, 24};
    private final List<Integer> invSlots = new ArrayList<>();

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GauntletMenu(id, inv, p), Component.literal("Stone Gauntlet")));
    }

    private GauntletMenu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

    ItemStack gauntlet() {
        ItemStack m = player.getMainHandItem();
        return m.getItem() instanceof GauntletItem ? m : ItemStack.EMPTY;
    }

    @Override
    protected void draw() {
        ItemStack g = gauntlet();
        if (g.isEmpty()) return;
        GauntletData d = g.getOrDefault(GauntletData.TYPE, GauntletData.EMPTY);
        int power = SkillManager.get(player).totalPower(), open = GauntletItem.slots(power);
        c.setItem(4, Ui.glint(Ui.icon(g.getItem(), Ui.text("Stone Gauntlet", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Ui.text("Sockets: " + d.stones().size() + " used · " + open + " / 5 open", ChatFormatting.GRAY),
                Ui.text("Total Power: " + power, ChatFormatting.GOLD),
                Ui.text("Right-click: use the selected stone", ChatFormatting.DARK_GRAY)), true));
        for (int i = 0; i < 5; i++) {
            int slot = SOCKETS[i];
            if (i < d.stones().size()) {
                StoneKind k = StoneKind.byId(d.stones().get(i));
                boolean sel = i == Math.min(d.selected(), d.stones().size() - 1);
                c.setItem(slot, Ui.glint(Ui.icon(Ui.item("kiyamet:" + k.id), Component.translatable("item.kiyamet." + k.id).withStyle(sel ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                        Ui.text(sel ? "★ Selected" : "Click: select", sel ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                        Ui.text("Sneak + click: take it out", ChatFormatting.GRAY),
                        Ui.text("Cooldown: " + (k.cooldown / 20) + " s", ChatFormatting.DARK_GRAY)), sel));
            } else if (i < open) {
                c.setItem(slot, Ui.icon(Items.LIME_STAINED_GLASS_PANE, Ui.text("Empty socket " + (i + 1), ChatFormatting.GREEN), Ui.text("Click a stone below to put it in", ChatFormatting.GRAY)));
            } else {
                c.setItem(slot, Ui.icon(Items.RED_STAINED_GLASS_PANE, Ui.text("Locked socket " + (i + 1), ChatFormatting.RED),
                        Ui.text("Opens at Total Power " + GauntletItem.SLOT_POWER[i], ChatFormatting.GRAY), Ui.bar(Math.min(1, power / (double) GauntletItem.SLOT_POWER[i]), 16)));
            }
        }
        for (int i = 9; i < 45; i++) if (i < 18 || i > 26) c.setItem(i, i >= 27 ? ItemStack.EMPTY : Ui.frame());
        c.setItem(19, Ui.frame()); c.setItem(25, Ui.frame());
        c.setItem(31, Ui.icon(Items.BOOK, Ui.text("Stones in your inventory", ChatFormatting.AQUA), Ui.text("Click one to socket it", ChatFormatting.GRAY)));
        invSlots.clear();
        int at = 36;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && at < 45; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof StoneItem si) {
                boolean has = d.stones().contains(si.kind.id);
                c.setItem(at, Ui.icon(s.getItem(), s.getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                        Ui.text(has ? "Already in the gauntlet" : "Click: put in the gauntlet", has ? ChatFormatting.GRAY : ChatFormatting.YELLOW)));
                invSlots.add(i);
                at++;
            }
        }
        if (invSlots.isEmpty()) c.setItem(40, Ui.icon(Items.BARRIER, Ui.text("No stones in your inventory", ChatFormatting.GRAY)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        ItemStack g = gauntlet();
        if (g.isEmpty() || slot == 49) { player.closeContainer(); return false; }
        GauntletData d = g.getOrDefault(GauntletData.TYPE, GauntletData.EMPTY);
        for (int i = 0; i < 5; i++) {
            if (slot != SOCKETS[i] || i >= d.stones().size()) continue;
            if (type == ClickType.QUICK_MOVE) {                 // take the stone out
                List<String> l = new ArrayList<>(d.stones());
                String id = l.remove(i);
                g.set(GauntletData.TYPE, new GauntletData(List.copyOf(l), 0));
                ItemStack back = new ItemStack(Ui.item("kiyamet:" + id));
                if (!player.getInventory().add(back)) player.drop(back, false);
            } else g.set(GauntletData.TYPE, new GauntletData(d.stones(), i));
            return true;
        }
        if (slot >= 36 && slot < 36 + invSlots.size()) {
            ItemStack s = player.getInventory().getItem(invSlots.get(slot - 36));
            if (!(s.getItem() instanceof StoneItem si) || d.stones().contains(si.kind.id)) return false;
            if (d.stones().size() >= GauntletItem.slots(SkillManager.get(player).totalPower())) {
                player.displayClientMessage(Ui.text("No free socket - raise your Total Power.", ChatFormatting.RED), true);
                return false;
            }
            g.set(GauntletData.TYPE, d.add(si.kind.id));
            if (!player.isCreative()) s.shrink(1);
            return true;
        }
        return false;
    }
}

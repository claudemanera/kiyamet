package com.kiyamet.gunsmith;

import com.kiyamet.item.GunItem;
import com.kiyamet.npc.Shop;
import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Gunsmith Bench: repair anything, and customise arrow guns: scope, silencer, light, magazine and paint.
 * Every option shows a preview of your gun with it on.
 */
public class GunBenchMenu extends PanelMenu {
    private static final int[] ROW = {9, 18, 27, 36};
    private static final int[] PAINT_SLOTS = {15, 16, 17, 24, 25, 26, 33, 34, 35, 42, 43};

    public static void open(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GunBenchMenu(id, inv, p), Component.literal("Gunsmith Bench")));
    }

    private GunBenchMenu(int id, Inventory inv, ServerPlayer p) { super(id, inv, p); redraw(); }

    private ItemStack gun() { return player.getMainHandItem(); }
    private boolean isGun() { return gun().getItem() instanceof GunItem; }
    private int eng() { return SkillManager.get(player).level(Skill.ENGINEERING); }

    private String costText(String[] c) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < c.length; i += 2) { if (b.length() > 0) b.append(", "); b.append(c[i + 1]).append("× ").append(new ItemStack(Ui.item(c[i])).getHoverName().getString()); }
        return b.toString();
    }

    private boolean canPay(String[] c) {
        if (player.isCreative()) return true;
        for (int i = 0; i < c.length; i += 2) if (Shop.count(player, c[i]) < Integer.parseInt(c[i + 1])) return false;
        return true;
    }

    private void pay(String[] c) {
        if (player.isCreative()) return;
        for (int i = 0; i < c.length; i += 2) Shop.take(player, c[i], Integer.parseInt(c[i + 1]));
    }

    @Override
    protected void draw() {
        ItemStack g = gun();
        c.setItem(4, g.isEmpty() ? Ui.icon(Items.BARRIER, Ui.text("Hold a gun (or anything to repair)", ChatFormatting.RED)) : g.copy());
        // repair
        if (g.isDamageableItem()) {
            int dmg = g.getDamageValue(), max = g.getMaxDamage();
            c.setItem(47, Ui.glint(Ui.icon(Items.ANVIL, Ui.text(dmg == 0 ? "Fully repaired" : "Repair " + (100 - dmg * 100 / max) + "% → 100%", ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Ui.text("Iron ingot: +25% · Metal plate: +50% · Repair kit: full", ChatFormatting.GRAY), Ui.text("Click: use what you have", ChatFormatting.YELLOW)), dmg > 0));
        } else c.setItem(47, Ui.icon(Items.ANVIL, Ui.text("Nothing to repair", ChatFormatting.DARK_GRAY)));
        c.setItem(51, Ui.icon(Items.BOOK, Ui.text("Gunsmith Bench", ChatFormatting.GOLD), Ui.text("Hold a gun, then pick parts.", ChatFormatting.GRAY),
                Ui.text("Parts change how it looks AND shoots.", ChatFormatting.GRAY), Ui.text("Better parts need more Engineering.", ChatFormatting.GRAY),
                Ui.text("Your Engineering: " + eng(), ChatFormatting.AQUA)));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        if (!isGun()) return;
        String[] labels = {"Scope", "Muzzle", "Light", "Magazine"};
        Item[] icons = {Items.SPYGLASS, Items.IRON_NUGGET, Items.TORCH, Items.CHEST};
        for (GunMods.Slot sl : GunMods.Slot.values()) {
            int row = ROW[sl.ordinal()];
            String cur = GunMods.get(g, sl);
            c.setItem(row, Ui.icon(icons[sl.ordinal()], Ui.text(labels[sl.ordinal()], ChatFormatting.GOLD, ChatFormatting.BOLD)));
            ItemStack none = g.copy();
            GunMods.set(none, sl, "");
            none.set(DataComponents.CUSTOM_NAME, Ui.text("No " + labels[sl.ordinal()].toLowerCase(), cur.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            Ui.lore(none, List.of(Ui.text(cur.isEmpty() ? "✔ Fitted" : "Click: remove (the part is lost)", ChatFormatting.GRAY)));
            c.setItem(row + 1, Ui.glint(none, cur.isEmpty()));
            int col = 2;
            for (GunMods.Part part : GunMods.PARTS) {
                if (part.slot() != sl) continue;
                ItemStack prev = g.copy();
                GunMods.set(prev, sl, part.id());
                boolean on = cur.equals(part.id()), eng = eng() >= part.eng(), pay = canPay(part.cost());
                prev.set(DataComponents.CUSTOM_NAME, Ui.text(part.title(), on ? ChatFormatting.GREEN : eng && pay ? ChatFormatting.YELLOW : ChatFormatting.WHITE, ChatFormatting.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Ui.text(part.effect(), ChatFormatting.AQUA));
                if (on) lore.add(Ui.text("✔ Fitted", ChatFormatting.GREEN));
                else {
                    lore.add(Ui.text("Cost: " + costText(part.cost()), pay ? ChatFormatting.GRAY : ChatFormatting.RED));
                    lore.add(Ui.text("Engineering " + part.eng(), eng ? ChatFormatting.GRAY : ChatFormatting.RED));
                    lore.add(Ui.text(eng && pay ? "Click to fit" : "Not available yet", eng && pay ? ChatFormatting.YELLOW : ChatFormatting.RED));
                }
                Ui.lore(prev, lore);
                c.setItem(row + col++, Ui.glint(prev, on));
            }
        }
        // paints
        c.setItem(6, Ui.icon(Items.BRUSH, Ui.text("Paint", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Costs one dye (or the item shown)", ChatFormatting.GRAY)));
        String curPaint = GunMods.paint(g);
        for (int i = 0; i < GunMods.PAINTS.size() && i < PAINT_SLOTS.length; i++) {
            GunMods.Paint pt = GunMods.PAINTS.get(i);
            ItemStack prev = g.copy();
            GunMods.setPaint(prev, pt.id());
            boolean on = curPaint.equals(pt.id());
            prev.set(DataComponents.CUSTOM_NAME, Component.literal(pt.title()).withStyle(st -> st.withItalic(false).withBold(true).withColor(pt.color() == -1 ? 0xFFFFFF : pt.color())));
            Ui.lore(prev, List.of(Ui.text(on ? "✔ Current paint" : pt.dye().isEmpty() ? "Free" : "Cost: 1× " + new ItemStack(Ui.item(pt.dye())).getHoverName().getString(), on ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
            c.setItem(PAINT_SLOTS[i], Ui.glint(prev, on));
        }
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 49) { player.closeContainer(); return false; }
        ItemStack g = gun();
        if (slot == 47 && g.isDamageableItem() && g.getDamageValue() > 0) {
            int max = g.getMaxDamage();
            if (player.isCreative() || Shop.take(player, "kiyamet:repair_kit", 1)) g.setDamageValue(0);
            else if (Shop.take(player, "kiyamet:metal_plate", 1)) g.setDamageValue(Math.max(0, g.getDamageValue() - max / 2));
            else if (Shop.take(player, "minecraft:iron_ingot", 1)) g.setDamageValue(Math.max(0, g.getDamageValue() - max / 4));
            else { player.displayClientMessage(Component.literal("You need iron, a metal plate or a repair kit.").withStyle(ChatFormatting.RED), true); return false; }
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
            return true;
        }
        if (!isGun()) return false;
        for (int i = 0; i < GunMods.PAINTS.size() && i < PAINT_SLOTS.length; i++) if (PAINT_SLOTS[i] == slot) {
            GunMods.Paint pt = GunMods.PAINTS.get(i);
            if (GunMods.paint(g).equals(pt.id())) return false;
            if (!pt.dye().isEmpty() && !player.isCreative() && !Shop.take(player, pt.dye(), 1)) { player.displayClientMessage(Component.literal("You need the dye.").withStyle(ChatFormatting.RED), true); return false; }
            GunMods.setPaint(g, pt.id());
            player.level().playSound(null, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1f, 1f);
            return true;
        }
        for (GunMods.Slot sl : GunMods.Slot.values()) {
            int row = ROW[sl.ordinal()];
            if (slot == row + 1) { if (GunMods.get(g, sl).isEmpty()) return false; GunMods.set(g, sl, ""); return true; }
            int col = 2;
            for (GunMods.Part part : GunMods.PARTS) {
                if (part.slot() != sl) continue;
                if (slot == row + col++) {
                    if (GunMods.get(g, sl).equals(part.id())) return false;
                    if (eng() < part.eng() && !player.isCreative()) { player.displayClientMessage(Component.literal("Needs Engineering " + part.eng()).withStyle(ChatFormatting.RED), true); return false; }
                    if (!canPay(part.cost())) { player.displayClientMessage(Component.literal("Missing materials: " + costText(part.cost())).withStyle(ChatFormatting.RED), true); return false; }
                    pay(part.cost());
                    GunMods.set(g, sl, part.id());
                    SkillManager.addXp(player, Skill.ENGINEERING, 20 + part.eng() / 4);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 1.1f);
                    player.displayClientMessage(Component.literal(part.title() + " fitted!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
                    return true;
                }
            }
        }
        return false;
    }
}

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
 * Gunsmith Bench (1.9, simpler): hold a gun. Top row = tabs (Scope, Muzzle, Light, Magazine, Paint, Repair).
 * The middle shows the choices for that tab: click one to fit it, click the fitted one again to take it off.
 */
public class GunBenchMenu extends PanelMenu {
    private static final int[] TABS = {1, 2, 3, 5, 6, 7};
    private static final String[] TAB_NAMES = {"Scope", "Muzzle", "Light", "Magazine", "Paint", "Repair"};
    private static final int[] OPTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private int tab = 0;

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

    static Item partIcon(String id) {
        return switch (id) {
            case "red_dot" -> Items.REDSTONE; case "holo" -> Items.GLASS_PANE; case "acog" -> Items.SPYGLASS; case "sniper" -> Items.ENDER_EYE;
            case "short" -> Items.IRON_NUGGET; case "long" -> Items.IRON_BARS; case "flashlight" -> Items.LANTERN; case "laser" -> Items.REDSTONE_TORCH;
            case "extended" -> Ui.item("kiyamet:magazine"); case "drum" -> Ui.item("kiyamet:magazine_drum"); default -> Items.BARRIER;
        };
    }

    @Override
    protected void draw() {
        ItemStack g = gun();
        Item[] tabIcons = {Items.SPYGLASS, Items.IRON_NUGGET, Items.LANTERN, Ui.item("kiyamet:magazine"), Items.RED_DYE, Items.ANVIL};
        c.setItem(8, Ui.glint(Ui.icon(Ui.item("kiyamet:ak47"), Ui.text("BUILD GUNS", ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text("Make any of the " + com.kiyamet.registry.ModItems.WEAPONS.length + " guns and all ammo here", ChatFormatting.GRAY), Ui.text("Click to open", ChatFormatting.YELLOW)), true));
        for (int i = 0; i < TABS.length; i++)
            c.setItem(TABS[i], Ui.glint(Ui.icon(tabIcons[i], Ui.text(TAB_NAMES[i], i == tab ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                    Ui.text(i == tab ? "▶ open" : "Click to open", ChatFormatting.GRAY)), i == tab));
        if (g.isEmpty() || !(isGun() || tab == 5 && g.isDamageableItem())) {
            c.setItem(4, Ui.icon(Items.BARRIER, Ui.text("Hold a gun in your hand", ChatFormatting.RED), Ui.text("(or any tool, for the Repair tab)", ChatFormatting.GRAY)));
            c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
            return;
        }
        List<Component> info = new ArrayList<>();
        if (isGun()) for (GunMods.Slot sl : GunMods.Slot.values()) { String cur = GunMods.get(g, sl); info.add(Ui.text(TAB_NAMES[sl.ordinal()] + ": " + (cur.isEmpty() ? "-" : GunMods.part(cur).title()), cur.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA)); }
        if (isGun()) info.add(Ui.text("Paint: " + GunMods.paintOf(GunMods.paint(g)).title(), ChatFormatting.LIGHT_PURPLE));
        c.setItem(4, Ui.glint(Ui.lore(g.copy(), info), true));
        c.setItem(49, Ui.icon(Items.BARRIER, Ui.text("Close", ChatFormatting.RED)));
        if (tab < 4) {
            GunMods.Slot sl = GunMods.Slot.values()[tab];
            String cur = GunMods.get(g, sl);
            int k = 0;
            for (GunMods.Part part : GunMods.PARTS) {
                if (part.slot() != sl) continue;
                boolean on = cur.equals(part.id()), engOk = eng() >= part.eng() || player.isCreative(), pay = canPay(part.cost());
                List<Component> lore = new ArrayList<>();
                lore.add(Ui.text(part.effect(), ChatFormatting.AQUA));
                if (on) lore.add(Ui.text("✔ Fitted · click to take it off", ChatFormatting.GREEN));
                else {
                    lore.add(Ui.text("Cost: " + costText(part.cost()), pay ? ChatFormatting.GRAY : ChatFormatting.RED));
                    lore.add(Ui.text("Needs Engineering " + part.eng(), engOk ? ChatFormatting.GRAY : ChatFormatting.RED));
                    lore.add(Ui.text(engOk && pay ? "Click to fit" : "Not available yet", engOk && pay ? ChatFormatting.YELLOW : ChatFormatting.RED));
                }
                c.setItem(OPTS[k * 2], Ui.glint(Ui.icon(partIcon(part.id()), Ui.text(part.title(), on ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD), lore.toArray(new Component[0])), on));
                k++;
            }
        } else if (tab == 4) {
            String curPaint = GunMods.paint(g);
            for (int i = 0; i < GunMods.PAINTS.size() && i < OPTS.length; i++) {
                GunMods.Paint pt = GunMods.PAINTS.get(i);
                boolean on = curPaint.equals(pt.id());
                Item icon = pt.dye().isEmpty() ? Items.WATER_BUCKET : Ui.item(pt.dye());
                ItemStack st = Ui.icon(icon, Component.literal(pt.title()).withStyle(x -> x.withItalic(false).withBold(true).withColor(pt.color() == -1 ? 0xFFFFFF : pt.color())),
                        Ui.text(on ? "✔ Current paint" : pt.dye().isEmpty() ? "Free" : "Costs 1× " + new ItemStack(Ui.item(pt.dye())).getHoverName().getString(), on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                c.setItem(OPTS[i], Ui.glint(st, on));
            }
        } else {
            int dmg = g.getDamageValue(), max = Math.max(1, g.getMaxDamage());
            c.setItem(22, Ui.glint(Ui.icon(Items.ANVIL, Ui.text(dmg == 0 ? "Fully repaired" : "Repair: " + (100 - dmg * 100 / max) + "% → 100%", ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Ui.text("Uses a repair kit (full), a metal plate (+50%) or an iron ingot (+25%)", ChatFormatting.GRAY), Ui.text("Click to repair", ChatFormatting.YELLOW)), dmg > 0));
        }
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (slot == 49) { player.closeContainer(); return false; }
        if (slot == 8) { GunCraftMenu.open(player, 0); return false; }
        for (int i = 0; i < TABS.length; i++) if (slot == TABS[i]) { tab = i; return true; }
        ItemStack g = gun();
        if (tab == 5) {
            if (slot != 22 || !g.isDamageableItem() || g.getDamageValue() == 0) return false;
            int max = g.getMaxDamage();
            if (player.isCreative() || Shop.take(player, "kiyamet:repair_kit", 1)) g.setDamageValue(0);
            else if (Shop.take(player, "kiyamet:metal_plate", 1)) g.setDamageValue(Math.max(0, g.getDamageValue() - max / 2));
            else if (Shop.take(player, "minecraft:iron_ingot", 1)) g.setDamageValue(Math.max(0, g.getDamageValue() - max / 4));
            else { player.displayClientMessage(Component.literal("You need iron, a metal plate or a repair kit.").withStyle(ChatFormatting.RED), true); return false; }
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
            return true;
        }
        if (!isGun()) return false;
        if (tab == 4) {
            for (int i = 0; i < GunMods.PAINTS.size() && i < OPTS.length; i++) if (OPTS[i] == slot) {
                GunMods.Paint pt = GunMods.PAINTS.get(i);
                if (GunMods.paint(g).equals(pt.id())) return false;
                if (!pt.dye().isEmpty() && !player.isCreative() && !Shop.take(player, pt.dye(), 1)) { player.displayClientMessage(Component.literal("You need the dye.").withStyle(ChatFormatting.RED), true); return false; }
                GunMods.setPaint(g, pt.id());
                player.level().playSound(null, player.blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1f, 1f);
                return true;
            }
            return false;
        }
        GunMods.Slot sl = GunMods.Slot.values()[tab];
        int k = 0;
        for (GunMods.Part part : GunMods.PARTS) {
            if (part.slot() != sl) continue;
            if (slot == OPTS[k * 2]) {
                if (GunMods.get(g, sl).equals(part.id())) { GunMods.set(g, sl, ""); player.displayClientMessage(Component.literal(part.title() + " taken off.").withStyle(ChatFormatting.GRAY), true); return true; }
                if (eng() < part.eng() && !player.isCreative()) { player.displayClientMessage(Component.literal("Needs Engineering " + part.eng()).withStyle(ChatFormatting.RED), true); return false; }
                if (!canPay(part.cost())) { player.displayClientMessage(Component.literal("Missing: " + costText(part.cost())).withStyle(ChatFormatting.RED), true); return false; }
                pay(part.cost());
                GunMods.set(g, sl, part.id());
                SkillManager.addXp(player, Skill.ENGINEERING, 20 + part.eng() / 4);
                player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 1.1f);
                player.displayClientMessage(Component.literal(part.title() + " fitted!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
                return true;
            }
            k++;
        }
        return false;
    }
}

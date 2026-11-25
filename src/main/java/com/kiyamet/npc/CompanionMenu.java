package com.kiyamet.npc;

import com.kiyamet.ui.PanelMenu;
import com.kiyamet.ui.Ui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Companion panel: stats, orders, gear, glow, looks editor, dismiss. */
public class CompanionMenu extends PanelMenu {
    private static final int INFO = 4, FOLLOW = 19, STAY = 20, GUARD = 21, WEAPON = 23, LOOKS = 24, GLOW = 25,
            HEAL = 30, BAG = 31, TP = 32, DISMISS = 49, JOB = 28, RENAME = 34, RUN = 39, SQUAD = 41;
    private final HumanEntity h;
    private boolean confirmDismiss;

    public static void open(ServerPlayer p, HumanEntity h) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new CompanionMenu(id, inv, p, h), Component.literal("Companion: " + h.getName().getString())));
    }

    private CompanionMenu(int id, Inventory inv, ServerPlayer p, HumanEntity h) {
        super(id, inv, p);
        this.h = h;
        redraw();
    }

    @Override
    protected void draw() {
        int lv = h.lvl();
        Appearance a = h.appearance();
        ItemStack info = Ui.icon(Items.PLAYER_HEAD, Component.literal(h.getName().getString()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                Ui.text("Level " + lv + " / " + HumanEntity.MAX_LEVEL, ChatFormatting.YELLOW),
                Ui.text("Type: " + h.profession().title, ChatFormatting.WHITE),
                Ui.text(h.specialty() == Specialty.NONE ? "No specialty" : "★ " + h.specialty().title + ": " + h.specialty().desc, h.specialty() == Specialty.NONE ? ChatFormatting.DARK_GRAY : ChatFormatting.LIGHT_PURPLE),
                lv >= HumanEntity.MAX_LEVEL ? Ui.text("★ MAX LEVEL", ChatFormatting.LIGHT_PURPLE) : Ui.bar(h.xp() / (double) HumanEntity.xpToNext(lv), 20),
                Ui.text("XP " + h.xp() + " / " + HumanEntity.xpToNext(lv) + "  (earned from kills)", ChatFormatting.DARK_GRAY),
                Ui.text("Health " + (int) h.getHealth() + " / " + (int) h.getMaxHealth(), ChatFormatting.RED),
                Ui.text("Damage " + String.format("%.1f", h.getAttributeValue(Attributes.ATTACK_DAMAGE) * Weapon.tier(h.getMainHandItem())), ChatFormatting.GOLD),
                Ui.text("Armor " + (int) h.getAttributeValue(Attributes.ARMOR), ChatFormatting.GRAY),
                Ui.text("Look: " + Appearance.OUTFIT_NAMES[Math.min(a.outfit(), Appearance.OUTFITS - 1)] + ", " + Appearance.HAIR_NAMES[a.hair()], ChatFormatting.DARK_GRAY),
                Ui.text("Rename: use a name tag on them", ChatFormatting.DARK_GRAY));
        c.setItem(INFO, Ui.glint(info, true));
        HumanEntity.Order o = h.order();
        c.setItem(FOLLOW, Ui.glint(Ui.icon(Items.LEAD, Ui.text("Follow me", o == HumanEntity.Order.FOLLOW ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                Ui.text("Stays close, teleports if left behind", ChatFormatting.GRAY)), o == HumanEntity.Order.FOLLOW));
        c.setItem(STAY, Ui.glint(Ui.icon(Items.OAK_SIGN, Ui.text("Stay here", o == HumanEntity.Order.STAY ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                Ui.text("Holds this exact spot", ChatFormatting.GRAY)), o == HumanEntity.Order.STAY));
        c.setItem(GUARD, Ui.glint(Ui.icon(Items.SHIELD, Ui.text("Guard this area", o == HumanEntity.Order.GUARD ? ChatFormatting.GREEN : ChatFormatting.WHITE, ChatFormatting.BOLD),
                Ui.text("Patrols 10 blocks around here, fights intruders", ChatFormatting.GRAY)), o == HumanEntity.Order.GUARD));
        ItemStack w = h.getMainHandItem().copy();
        if (w.isEmpty()) w = new ItemStack(Items.BARRIER);
        Ui.lore(w, java.util.List.of(Ui.text("Current weapon" + (Weapon.ranged(h.getMainHandItem()) ? " (ranged)" : " (melee)"), ChatFormatting.GOLD),
                Ui.text("Click: swap with the item in your main hand", ChatFormatting.YELLOW),
                Ui.text("Weapon power scales with their level", ChatFormatting.GRAY)));
        c.setItem(WEAPON, w);
        c.setItem(LOOKS, Ui.icon(Items.ARMOR_STAND, Ui.text("Change appearance", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Ui.text("Skin, hair, face, details, outfit", ChatFormatting.GRAY)));
        boolean glow = h.hasGlowingTag();
        c.setItem(GLOW, Ui.glint(Ui.icon(glow ? Items.GLOWSTONE_DUST : Items.GUNPOWDER, Ui.text("Glow: " + (glow ? "ON" : "OFF"), glow ? ChatFormatting.YELLOW : ChatFormatting.GRAY, ChatFormatting.BOLD),
                Ui.text("Makes them visible through walls", ChatFormatting.GRAY)), glow));
        c.setItem(HEAL, Ui.icon(Ui.item("kiyamet:medkit"), Ui.text("Heal with a medkit / bandage", ChatFormatting.RED),
                Ui.text("Uses one from your inventory", ChatFormatting.GRAY)));
        int used = 0;
        for (int i = 0; i < h.inventory().getContainerSize(); i++) if (!h.inventory().getItem(i).isEmpty()) used++;
        c.setItem(BAG, Ui.icon(Items.CHEST, Ui.text("Inventory (" + used + " / 27)", ChatFormatting.GOLD, ChatFormatting.BOLD),
                Ui.text("Give them supplies to carry", ChatFormatting.GRAY), Ui.text("Medkits & bandages inside are used automatically", ChatFormatting.GRAY)));
        c.setItem(TP, Ui.icon(Items.ENDER_PEARL, Ui.text("Call to me", ChatFormatting.DARK_AQUA), Ui.text("Teleports them to you and sets Follow", ChatFormatting.GRAY)));
        Job j = h.job();
        c.setItem(JOB, Ui.glint(Ui.icon(j.icon, Ui.text("Job: " + j.title, ChatFormatting.GOLD, ChatFormatting.BOLD), Ui.text(j.desc, ChatFormatting.GRAY),
                Ui.text("Click to change (Gunner, Scout, Guardian, Looter)", ChatFormatting.YELLOW)), j != Job.NONE));
        c.setItem(RENAME, Ui.icon(Items.NAME_TAG, Ui.text("Rename", ChatFormatting.AQUA), Ui.text("Type the new name in chat", ChatFormatting.GRAY)));
        c.setItem(RUN, Ui.icon(Items.CHEST_MINECART, Ui.text("Send on a supply run (5 min)", ChatFormatting.GOLD), Ui.text("Comes back with loot in their inventory", ChatFormatting.GRAY)));
        c.setItem(SQUAD, Ui.icon(Items.BELL, Ui.text("Group panel", ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), Ui.text("Orders for everyone, group supply runs", ChatFormatting.GRAY)));
        c.setItem(DISMISS, Ui.icon(confirmDismiss ? Items.TNT : Items.RED_DYE, Ui.text(confirmDismiss ? "Click again to confirm" : "Dismiss companion", ChatFormatting.RED),
                Ui.text("They become a free survivor again", ChatFormatting.GRAY)));
    }

    @Override
    protected boolean click(int slot, ClickType type) {
        if (!h.isAlive() || !h.isOwnedBy(player) || h.isAway()) { player.closeContainer(); return false; }
        if (slot != DISMISS) confirmDismiss = false;
        switch (slot) {
            case FOLLOW -> h.setOrder(HumanEntity.Order.FOLLOW);
            case STAY -> h.setOrder(HumanEntity.Order.STAY);
            case GUARD -> h.setOrder(HumanEntity.Order.GUARD);
            case WEAPON -> {
                ItemStack mine = player.getMainHandItem().copy();
                ItemStack theirs = h.getMainHandItem().copy();
                h.setItemSlot(EquipmentSlot.MAINHAND, mine);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, theirs);
            }
            case LOOKS -> { AppearanceMenu.open(player, h); return false; }
            case JOB -> h.setJob(Job.values()[(h.job().ordinal() + 1) % Job.values().length]);
            case RENAME -> { Rename.start(player, h); return false; }
            case RUN -> { h.sendAway(5, 1); player.closeContainer(); player.sendSystemMessage(Component.literal(h.getName().getString() + " heads out on a supply run.").withStyle(ChatFormatting.GOLD)); return false; }
            case SQUAD -> { SquadMenu.open(player); return false; }
            case GLOW -> h.setGlowingTag(!h.hasGlowingTag());
            case HEAL -> {
                for (String id : new String[]{"kiyamet:medkit", "kiyamet:bandage"}) {
                    int slotIdx = find(id);
                    if (slotIdx >= 0) {
                        player.getInventory().getItem(slotIdx).shrink(1);
                        h.heal(id.endsWith("medkit") ? h.getMaxHealth() : h.getMaxHealth() * 0.3f);
                        return true;
                    }
                }
                player.displayClientMessage(Component.literal("You have no medkit or bandage.").withStyle(ChatFormatting.RED), true);
                return false;
            }
            case BAG -> {
                player.openMenu(new SimpleMenuProvider((id, inv, pl) -> net.minecraft.world.inventory.ChestMenu.threeRows(id, inv, h.inventory()),
                        Component.literal(h.getName().getString() + "'s inventory")));
                return false;
            }
            case TP -> { h.teleportTo(player.getX(), player.getY(), player.getZ()); h.setOrder(HumanEntity.Order.FOLLOW); }
            case DISMISS -> {
                if (!confirmDismiss) { confirmDismiss = true; return true; }
                h.setOwner(null);
                player.closeContainer();
                player.sendSystemMessage(Component.literal(h.getName().getString() + " goes their own way.").withStyle(ChatFormatting.GRAY));
                return false;
            }
            default -> { return false; }
        }
        return true;
    }

    private int find(String id) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(Ui.item(id))) return i;
        return -1;
    }
}

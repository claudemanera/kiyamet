package com.kiyamet.ammo;

import com.kiyamet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.5 bullet system: 6 ammo types, magazines that hold 32-256 rounds, every gun takes one ammo type. */
public final class Ammo {
    public enum Type {
        PISTOL("pistol_ammo", "9mm", ChatFormatting.GOLD), RIFLE("rifle_ammo", "7.62mm", ChatFormatting.YELLOW),
        SHELL("shotgun_shell", "12 Gauge", ChatFormatting.RED), SNIPER("sniper_ammo", ".50 BMG", ChatFormatting.AQUA),
        ROCKET("rocket_ammo", "Rocket", ChatFormatting.DARK_GREEN), PLASMA("plasma_cell", "Plasma Cell", ChatFormatting.LIGHT_PURPLE);
        public final String id, label; public final ChatFormatting color;
        Type(String id, String label, ChatFormatting color) { this.id = id; this.label = label; this.color = color; }
        public Item item() { return ModItems.get(id); }
        public static Type byName(String n) { for (Type t : values()) if (t.name().equals(n)) return t; return null; }
        public static Type ofItem(ItemStack s) { for (Type t : values()) if (!s.isEmpty() && s.is(t.item())) return t; return null; }
    }

    public static final String[] AMMO = {"pistol_ammo", "rifle_ammo", "shotgun_shell", "sniper_ammo", "rocket_ammo", "plasma_cell"};
    public static final String[][] MAGS = {{"magazine_small", "32"}, {"magazine", "64"}, {"magazine_large", "128"}, {"magazine_drum", "256"}};

    public static Type typeFor(String gun) {
        return switch (gun) {
            case "arrow_pistol", "silenced_arrow_pistol", "service_pistol", "compact_pistol", "tactical_pistol", "hand_cannon", "revolver", "magnum_revolver",
                 "machine_pistol", "golden_pistol", "flare_pistol", "survivor_smg", "arrow_repeater" -> Type.PISTOL;
            case "arrow_shotgun", "double_arrow_rifle", "pump_shotgun", "sawed_off", "auto_shotgun", "ak_shotgun", "behemoth_shotgun" -> Type.SHELL;
            case "arrow_sniper", "bolt_sniper", "hunting_rifle", "marksman_rifle", "anti_materiel_rifle", "ghillie_sniper", "silent_sniper", "longshot_rifle", "harpoon_gun" -> Type.SNIPER;
            case "arrow_launcher", "apocalypse_cannon", "ak_grenadier" -> Type.ROCKET;
            case "tesla_arrow_gun", "arrow_railgun", "void_arrow_launcher", "homing_arrow_gun", "frost_arrow_gun", "toxic_arrow_rifle" -> Type.PLASMA;
            default -> Type.RIFLE;
        };
    }

    /** Loose rounds of a type in the inventory. */
    public static int loose(Inventory inv, Type t) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (s.is(t.item())) n += s.getCount(); }
        return n;
    }

    /** Rounds in loaded magazines of that type. */
    public static int loaded(Inventory inv, Type t) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (s.getItem() instanceof MagazineItem && MagazineItem.type(s) == t) n += MagazineItem.rounds(s); }
        return n;
    }

    /** Takes n rounds: magazines first (offhand, then the fullest-first order of the inventory), then loose rounds. */
    public static boolean take(ServerPlayer p, Type t, int n) {
        if (p.isCreative()) return true;
        Inventory inv = p.getInventory();
        if (loaded(inv, t) + loose(inv, t) < n) return false;
        ItemStack off = p.getItemInHand(InteractionHand.OFF_HAND);
        if (off.getItem() instanceof MagazineItem && MagazineItem.type(off) == t) n -= MagazineItem.drain(off, n);
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof MagazineItem && MagazineItem.type(s) == t) n -= MagazineItem.drain(s, n);
        }
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(t.item())) { int k = Math.min(n, s.getCount()); s.shrink(k); n -= k; }
        }
        return true;
    }

    public static Component hud(ServerPlayer p, ItemStack gun, Type t) {
        Inventory inv = p.getInventory();
        return Component.empty().append(gun.getHoverName().copy().withStyle(ChatFormatting.WHITE)).append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(t.label + " ").withStyle(t.color, ChatFormatting.BOLD))
                .append(Component.literal(loaded(inv, t) + " in mags").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  +" + loose(inv, t) + " loose").withStyle(ChatFormatting.GRAY));
    }

    private Ammo() {}
}

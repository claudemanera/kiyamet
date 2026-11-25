package com.kiyamet.npc;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The 4 companion jobs, chosen in the companion panel. */
public enum Job {
    NONE("No job", "Fights normally", Items.PAPER),
    GUNNER("Gunner", "+25% ranged damage, shoots 20% faster, keeps distance", Items.CROSSBOW),
    SCOUT("Scout", "Marks enemies within 40 blocks (glow), warns you of hordes, +15% speed", Items.SPYGLASS),
    GUARDIAN("Guardian", "+40% health, +6 armor, pulls enemies off you", Items.SHIELD),
    LOOTER("Looter", "Brings back twice the loot from supply runs and finds things while following", Items.CHEST);

    public final String title, desc;
    public final Item icon;

    Job(String title, String desc, Item icon) { this.title = title; this.desc = desc; this.icon = icon; }

    public static Job byName(String s) { try { return valueOf(s); } catch (IllegalArgumentException e) { return NONE; } }
}

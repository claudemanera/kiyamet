package com.kiyamet.npc;

import com.kiyamet.ui.Ui;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What NPCs carry. Ranged weapons shoot hitscan arrows; damage always scales with the NPC's single level. */
public final class Weapon {
    public static boolean ranged(ItemStack s) {
        return s.getItem() instanceof BowItem || s.getItem() instanceof CrossbowItem || s.getItem() instanceof com.kiyamet.item.GunItem;
    }

    /** Extra multiplier by weapon tier on top of the level-based damage. */
    public static float tier(ItemStack s) {
        if (s.isEmpty()) return 0.6f;
        if (s.getItem() instanceof com.kiyamet.item.GunItem) return 1.35f;
        if (s.is(Items.CROSSBOW)) return 1.15f;
        if (s.is(Items.BOW)) return 1.0f;
        if (s.is(Items.NETHERITE_SWORD) || s.is(Items.NETHERITE_AXE)) return 1.5f;
        if (s.is(Items.DIAMOND_SWORD) || s.is(Items.DIAMOND_AXE)) return 1.3f;
        if (s.is(Items.IRON_SWORD) || s.is(Items.IRON_AXE)) return 1.12f;
        return 1.0f;
    }

    public static ItemStack random(RandomSource r, Faction f, Role role, int level) {
        String[] melee = {"minecraft:wooden_sword", "minecraft:stone_axe", "minecraft:iron_axe", "minecraft:iron_sword", "minecraft:stone_sword"};
        String[] guns = {"kiyamet:arrow_pistol", "kiyamet:silenced_arrow_pistol", "kiyamet:arrow_rifle", "kiyamet:arrow_shotgun", "kiyamet:arrow_repeater", "kiyamet:service_pistol", "kiyamet:revolver", "kiyamet:ak47", "kiyamet:hunting_rifle", "kiyamet:survivor_smg", "kiyamet:aks74u"};
        String[] bigGuns = {"kiyamet:arrow_sniper", "kiyamet:arrow_gatling", "kiyamet:double_arrow_rifle", "kiyamet:incendiary_repeater", "kiyamet:rpk", "kiyamet:bolt_sniper", "kiyamet:ak12", "kiyamet:marksman_rifle", "kiyamet:ak_tactical"};
        if (f == Faction.WHISPERER) return new ItemStack(Ui.item(r.nextBoolean() ? "minecraft:stone_sword" : "minecraft:iron_axe"));
        if (role == Role.COMMANDER) return new ItemStack(Ui.item(bigGuns[r.nextInt(bigGuns.length)]));
        float gunChance = f == Faction.RAIDER ? 0.6f : role == Role.GUARD ? 0.55f : 0.2f;
        if (level > 150) gunChance += 0.2f;
        float roll = r.nextFloat();
        if (roll < gunChance) return new ItemStack(Ui.item((level > 180 ? bigGuns : guns)[r.nextInt(level > 180 ? bigGuns.length : guns.length)]));
        if (roll < gunChance + 0.2f) return new ItemStack(r.nextBoolean() ? Items.BOW : Items.CROSSBOW);
        String m = level > 200 ? "minecraft:diamond_sword" : melee[r.nextInt(melee.length)];
        return new ItemStack(Ui.item(m));
    }

    private Weapon() {}
}

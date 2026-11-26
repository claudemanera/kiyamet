package com.kiyamet.registry;

import com.kiyamet.Kiyamet;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Every item of the mod. Behaviour (stones, gadgets, arrow guns) is added in later phases by swapping the factory. */
public final class ModItems {
    public static final List<Item> ALL = new ArrayList<>();

    public static final String[] STONES = {"fire_stone", "frost_stone", "storm_stone", "nature_stone", "blood_stone",
            "shadow_stone", "soul_stone", "time_stone", "void_stone", "earth_stone"};
    public static final String[] GADGETS = {"flashlight", "sonic_lure", "grappling_gun", "night_vision", "vein_miner", "jetpack",
            "healing_syringe", "auto_turret", "flamethrower", "gravity_glove", "structure_scanner", "drone_controller", "clone_device",
            "teleport_gun", "emp_bomb", "laser_rifle", "shield_generator", "time_stopper", "black_hole_bomb", "energy_sword"};
    public static final String[] WEAPONS = {"arrow_pistol", "arrow_rifle", "double_arrow_rifle", "arrow_shotgun", "silenced_arrow_pistol",
            "arrow_repeater", "toxic_arrow_rifle", "frost_arrow_gun", "arrow_sniper", "incendiary_repeater", "tesla_arrow_gun", "arrow_gatling",
            "harpoon_gun", "arrow_railgun", "arrow_launcher", "void_arrow_launcher", "homing_arrow_gun", "apocalypse_cannon",
            "service_pistol", "compact_pistol", "tactical_pistol", "hand_cannon", "revolver", "magnum_revolver", "machine_pistol", "golden_pistol", "flare_pistol", "ak47", "akm_classic", "ak74", "aks74u", "ak_tactical", "rpk", "golden_ak", "ak12", "ak_grenadier", "bolt_sniper", "hunting_rifle", "marksman_rifle", "anti_materiel_rifle", "ghillie_sniper", "silent_sniper", "lever_rifle", "longshot_rifle", "survivor_smg",
            "pump_shotgun", "sawed_off", "auto_shotgun", "ak_shotgun", "behemoth_shotgun"};
    public static final String[] PARTS = {"circuit", "energy_cell", "vaccine", "bandage", "tech_core", "metal_plate", "fuel_canister",
            "lens", "void_shard", "energy_core", "ruby"};
    public static final String[] SINGLE = {"robot_core_trooper", "robot_core_drone", "robot_core_heavy", "radio", "walker_guts", "medkit", "repair_kit", "stone_gauntlet", "stone_codex_page_1", "stone_codex_page_2",
            "stone_codex_page_3", "stone_codex_page_4", "stone_codex_page_5", "stone_codex_page_6"};

    static {
        for (String s : STONES) { com.kiyamet.stone.StoneKind k = com.kiyamet.stone.StoneKind.byId(s); custom(s, p -> new com.kiyamet.stone.StoneItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), k)); }
        for (String s : GADGETS) {
            if (s.equals("healing_syringe")) custom(s, p -> new com.kiyamet.item.HealItem(p.stacksTo(4), com.kiyamet.item.HealItem.Kind.SYRINGE));
            else { String id = s; custom(s, p -> new com.kiyamet.item.GadgetItem(p.stacksTo(id.equals("emp_bomb") || id.equals("black_hole_bomb") || id.equals("sonic_lure") ? 8 : 1), id)); }
        }
        for (String s : WEAPONS) { com.kiyamet.item.GunItem.Stats st = com.kiyamet.item.GunItem.statsFor(s); custom(s, p -> new com.kiyamet.item.GunItem(p.durability(500 + Math.round(st.damage() * 20)), st)); }
        for (String s : com.kiyamet.ammo.Ammo.AMMO) simple(s, 99);
        for (String[] m : com.kiyamet.ammo.Ammo.MAGS) { int cap = Integer.parseInt(m[1]); custom(m[0], p -> new com.kiyamet.ammo.MagazineItem(p.stacksTo(1), cap)); }
        for (String s : PARTS) {
            if (s.equals("bandage")) custom(s, p -> new com.kiyamet.item.HealItem(p.stacksTo(16), com.kiyamet.item.HealItem.Kind.BANDAGE));
            else if (s.equals("vaccine")) custom(s, p -> new com.kiyamet.item.HealItem(p.stacksTo(8), com.kiyamet.item.HealItem.Kind.VACCINE));
            else simple(s, 64);
        }
        for (String s : SINGLE) {
            if (s.equals("medkit")) custom(s, p -> new com.kiyamet.item.HealItem(p.stacksTo(8), com.kiyamet.item.HealItem.Kind.MEDKIT));
            else if (s.equals("robot_core_trooper")) custom(s, p -> new com.kiyamet.robot.RobotCoreItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), com.kiyamet.robot.RobotKind.ROBOT_TROOPER));
            else if (s.equals("robot_core_drone")) custom(s, p -> new com.kiyamet.robot.RobotCoreItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), com.kiyamet.robot.RobotKind.SCOUT_DRONE));
            else if (s.equals("robot_core_heavy")) custom(s, p -> new com.kiyamet.robot.RobotCoreItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC), com.kiyamet.robot.RobotKind.ROBOT_HEAVY));
            else if (s.equals("radio")) custom(s, p -> new com.kiyamet.npc.Radio(p.stacksTo(1)));
            else if (s.equals("walker_guts")) custom(s, p -> new com.kiyamet.item.WalkerGutsItem(p.stacksTo(16)));
            else if (s.equals("repair_kit")) custom(s, p -> new com.kiyamet.item.RepairKitItem(p.stacksTo(16)));
            else if (s.equals("stone_gauntlet")) custom(s, p -> new com.kiyamet.stone.GauntletItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
            else simple(s, 1);
        }
        for (Block b : ModBlocks.ALL) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(b));
            Item.Properties p = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            if (b == ModBlocks.GENERATOR) p = p.stacksTo(21);
            Item item = b == ModBlocks.STEEL_GATE ? new DoubleHighBlockItem(b, p) : new BlockItem(b, p);
            ALL.add(Registry.register(BuiltInRegistries.ITEM, key, item));
        }
    }

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kiyamet.id("main"));

    private static Item simple(String name, int stack) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kiyamet.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).stacksTo(stack)));
        ALL.add(item);
        return item;
    }

    public static Item custom(String name, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Kiyamet.id(name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        ALL.add(item);
        return item;
    }

    public static Item get(String name) {
        return BuiltInRegistries.ITEM.getValue(Kiyamet.id(name));
    }

    public static void init() {}

    private ModItems() {}
}

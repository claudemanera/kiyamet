package com.kiyamet.lab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything craftable at the Laboratory Table: result, Engineering level, parts (item id -> count). */
public final class LabRecipes {
    public record Recipe(String result, int count, int level, Map<String, Integer> parts) {}
    public static final List<Recipe> ALL = new ArrayList<>();

    private static void r(String result, int count, int level, Object... parts) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < parts.length; i += 2) m.put((String) parts[i], (Integer) parts[i + 1]);
        ALL.add(new Recipe(result, count, level, m));
    }

    static {
        // parts
        r("kiyamet:metal_plate", 2, 1, "minecraft:iron_ingot", 3);
        r("kiyamet:circuit", 1, 5, "minecraft:copper_ingot", 2, "minecraft:redstone", 2, "minecraft:gold_nugget", 2);
        r("kiyamet:lens", 1, 10, "minecraft:glass", 2, "minecraft:amethyst_shard", 1);
        r("kiyamet:fuel_canister", 1, 10, "minecraft:iron_ingot", 2, "minecraft:coal", 4);
        r("kiyamet:energy_cell", 1, 30, "minecraft:redstone_block", 1, "minecraft:copper_ingot", 3, "kiyamet:circuit", 1);
        r("kiyamet:bandage", 3, 1, "minecraft:string", 3, "minecraft:paper", 1);
        r("kiyamet:medkit", 1, 40, "kiyamet:bandage", 3, "minecraft:golden_apple", 1, "minecraft:glass_bottle", 1);
        r("kiyamet:radio", 1, 15, "kiyamet:circuit", 2, "minecraft:iron_ingot", 3, "minecraft:redstone", 4);
        r("kiyamet:repair_kit", 1, 60, "kiyamet:metal_plate", 2, "kiyamet:circuit", 1, "minecraft:iron_ingot", 2);
        r("kiyamet:tech_core", 1, 200, "kiyamet:circuit", 4, "kiyamet:energy_cell", 2, "minecraft:diamond", 1);
        r("kiyamet:energy_core", 1, 500, "kiyamet:tech_core", 2, "kiyamet:energy_cell", 4, "minecraft:nether_star", 1);
        // defense
        r("kiyamet:reinforced_planks", 4, 5, "minecraft:oak_planks", 4, "minecraft:iron_ingot", 1);
        r("kiyamet:barbed_wire", 4, 20, "minecraft:iron_ingot", 6);
        r("kiyamet:steel_block", 2, 50, "kiyamet:metal_plate", 4, "minecraft:iron_block", 1);
        r("kiyamet:steel_grate", 4, 60, "minecraft:iron_bars", 6, "kiyamet:metal_plate", 2);
        r("kiyamet:spike_trap", 2, 80, "minecraft:iron_ingot", 4, "minecraft:stick", 2);
        r("kiyamet:razor_wire", 2, 120, "kiyamet:barbed_wire", 3, "minecraft:iron_block", 1);
        r("kiyamet:steel_gate", 1, 150, "kiyamet:steel_block", 6);
        r("kiyamet:generator", 1, 80, "minecraft:iron_ingot", 12, "minecraft:blast_furnace", 1, "kiyamet:circuit", 4, "kiyamet:energy_cell", 2, "minecraft:copper_ingot", 8);
        r("kiyamet:electric_fence", 4, 220, "kiyamet:barbed_wire", 4, "kiyamet:circuit", 2, "kiyamet:energy_cell", 1);
        r("kiyamet:ruby_block", 1, 250, "kiyamet:ruby", 9);
        r("kiyamet:titanium_block", 2, 400, "kiyamet:metal_plate", 4, "minecraft:diamond", 2, "kiyamet:tech_core", 1);
        r("kiyamet:energy_wall", 4, 650, "kiyamet:energy_core", 1, "kiyamet:circuit", 4, "minecraft:glass", 4);
        // gadgets
        r("kiyamet:flashlight", 1, 25, "minecraft:iron_ingot", 2, "minecraft:glowstone_dust", 2, "kiyamet:circuit", 1);
        r("kiyamet:sonic_lure", 1, 50, "kiyamet:circuit", 1, "minecraft:note_block", 1, "minecraft:redstone", 2);
        r("kiyamet:grappling_gun", 1, 75, "kiyamet:metal_plate", 2, "minecraft:string", 4, "minecraft:tripwire_hook", 1);
        r("kiyamet:night_vision", 1, 100, "kiyamet:lens", 2, "kiyamet:circuit", 1, "minecraft:leather", 2);
        r("kiyamet:vein_miner", 1, 120, "minecraft:diamond_pickaxe", 1, "kiyamet:circuit", 2);
        r("kiyamet:jetpack", 1, 150, "kiyamet:metal_plate", 4, "kiyamet:fuel_canister", 2, "kiyamet:energy_cell", 1);
        r("kiyamet:healing_syringe", 2, 175, "kiyamet:vaccine", 1, "minecraft:glass_bottle", 2, "minecraft:glistering_melon_slice", 1);
        r("kiyamet:auto_turret", 1, 200, "kiyamet:circuit", 4, "kiyamet:metal_plate", 4, "minecraft:dispenser", 1);
        r("kiyamet:flamethrower", 1, 250, "kiyamet:fuel_canister", 3, "kiyamet:metal_plate", 2, "minecraft:blaze_rod", 1);
        r("kiyamet:gravity_glove", 1, 300, "kiyamet:energy_cell", 3, "minecraft:leather", 4, "minecraft:ender_pearl", 1);
        r("kiyamet:structure_scanner", 1, 350, "kiyamet:lens", 2, "kiyamet:circuit", 3, "minecraft:compass", 1);
        r("kiyamet:drone_controller", 1, 400, "kiyamet:tech_core", 1, "kiyamet:circuit", 2, "kiyamet:lens", 1);
        r("kiyamet:clone_device", 1, 450, "kiyamet:tech_core", 1, "kiyamet:energy_cell", 2, "minecraft:totem_of_undying", 1);
        r("kiyamet:teleport_gun", 1, 550, "kiyamet:void_shard", 1, "kiyamet:energy_cell", 2, "minecraft:ender_eye", 2);
        r("kiyamet:emp_bomb", 2, 600, "kiyamet:circuit", 2, "kiyamet:energy_cell", 2, "minecraft:tnt", 1);
        r("kiyamet:laser_rifle", 1, 650, "kiyamet:tech_core", 1, "kiyamet:lens", 3, "kiyamet:energy_cell", 2);
        r("kiyamet:shield_generator", 1, 700, "kiyamet:tech_core", 1, "kiyamet:energy_cell", 3, "minecraft:shield", 1);
        r("kiyamet:time_stopper", 1, 800, "kiyamet:energy_core", 1, "minecraft:clock", 1, "kiyamet:circuit", 4);
        r("kiyamet:black_hole_bomb", 1, 900, "kiyamet:void_shard", 2, "kiyamet:energy_core", 1);
        r("kiyamet:shelter_core", 1, 120, "kiyamet:tech_core", 1, "kiyamet:energy_cell", 2, "kiyamet:circuit", 4, "minecraft:iron_block", 2);
        r("kiyamet:atv_keys", 1, 30, "minecraft:iron_ingot", 12, "kiyamet:metal_plate", 4, "kiyamet:circuit", 1, "kiyamet:fuel_canister", 1);
        r("kiyamet:car_sedan_keys", 1, 40, "minecraft:iron_ingot", 16, "kiyamet:metal_plate", 6, "kiyamet:circuit", 2, "minecraft:glass_pane", 6);
        r("kiyamet:motorcycle_keys", 1, 60, "minecraft:iron_ingot", 10, "kiyamet:metal_plate", 4, "kiyamet:circuit", 2, "minecraft:leather", 4);
        r("kiyamet:car_pickup_keys", 1, 100, "minecraft:iron_ingot", 24, "kiyamet:metal_plate", 10, "kiyamet:circuit", 3, "minecraft:chest", 2);
        r("kiyamet:car_police_keys", 1, 150, "minecraft:iron_ingot", 20, "kiyamet:metal_plate", 10, "kiyamet:circuit", 4, "minecraft:redstone_lamp", 2);
        r("kiyamet:car_ambulance_keys", 1, 200, "minecraft:iron_ingot", 24, "kiyamet:metal_plate", 12, "kiyamet:circuit", 4, "kiyamet:medkit", 3);
        r("kiyamet:car_armored_suv_keys", 1, 300, "minecraft:iron_block", 6, "kiyamet:metal_plate", 24, "kiyamet:circuit", 6, "kiyamet:energy_cell", 2);
        r("kiyamet:armored_truck_keys", 1, 450, "minecraft:iron_block", 12, "kiyamet:metal_plate", 40, "kiyamet:tech_core", 2, "kiyamet:energy_cell", 4);
        r("kiyamet:robot_core_trooper", 1, 500, "kiyamet:tech_core", 2, "kiyamet:metal_plate", 12, "kiyamet:circuit", 6, "kiyamet:energy_cell", 3);
        r("kiyamet:robot_core_drone", 1, 650, "kiyamet:tech_core", 2, "kiyamet:lens", 4, "kiyamet:circuit", 8, "kiyamet:energy_core", 1);
        r("kiyamet:robot_core_heavy", 1, 800, "kiyamet:tech_core", 4, "kiyamet:metal_plate", 24, "kiyamet:energy_core", 2, "minecraft:iron_block", 4);
        r("kiyamet:energy_sword", 1, 999, "kiyamet:energy_core", 2, "minecraft:netherite_sword", 1, "kiyamet:tech_core", 2);
        // 1.5 ammo in bulk
        r("kiyamet:pistol_ammo", 64, 30, "minecraft:iron_ingot", 3, "minecraft:gunpowder", 3, "minecraft:copper_ingot", 2);
        r("kiyamet:rifle_ammo", 64, 60, "minecraft:iron_ingot", 3, "minecraft:gunpowder", 5, "minecraft:copper_ingot", 3);
        r("kiyamet:shotgun_shell", 48, 60, "minecraft:paper", 4, "minecraft:gunpowder", 4, "minecraft:iron_ingot", 3);
        r("kiyamet:sniper_ammo", 32, 120, "minecraft:iron_ingot", 6, "minecraft:gunpowder", 6, "minecraft:copper_ingot", 4);
        r("kiyamet:rocket_ammo", 12, 200, "minecraft:tnt", 4, "kiyamet:metal_plate", 2, "minecraft:gunpowder", 4);
        r("kiyamet:plasma_cell", 32, 300, "kiyamet:energy_cell", 2, "minecraft:redstone", 6, "minecraft:glowstone_dust", 4);
        r("kiyamet:magazine_drum", 1, 100, "kiyamet:metal_plate", 4, "minecraft:iron_ingot", 6, "kiyamet:circuit", 1);
        // 1.6 throwables
        r("kiyamet:frag_grenade", 4, 120, "minecraft:iron_ingot", 4, "minecraft:gunpowder", 4);
        r("kiyamet:molotov", 4, 40, "minecraft:glass_bottle", 4, "kiyamet:fuel_canister", 1, "minecraft:string", 2);
        r("kiyamet:smoke_grenade", 4, 60, "minecraft:iron_ingot", 2, "minecraft:coal", 4, "minecraft:gunpowder", 1);
        r("kiyamet:flashbang", 4, 160, "minecraft:iron_ingot", 3, "minecraft:glowstone_dust", 4, "minecraft:gunpowder", 2);
        // arrow weapons
        String[][] guns = {{"arrow_pistol", "50"}, {"arrow_rifle", "100"}, {"double_arrow_rifle", "150"}, {"arrow_shotgun", "200"}, {"silenced_arrow_pistol", "250"},
                {"arrow_repeater", "300"}, {"toxic_arrow_rifle", "350"}, {"frost_arrow_gun", "400"}, {"arrow_sniper", "450"}, {"incendiary_repeater", "500"},
                {"tesla_arrow_gun", "550"}, {"arrow_gatling", "600"}, {"harpoon_gun", "650"}, {"arrow_railgun", "700"}, {"arrow_launcher", "750"},
                {"void_arrow_launcher", "850"}, {"homing_arrow_gun", "900"}, {"apocalypse_cannon", "999"},
                {"service_pistol", "60"}, {"compact_pistol", "80"}, {"tactical_pistol", "180"}, {"hand_cannon", "320"}, {"revolver", "120"}, {"magnum_revolver", "380"}, {"machine_pistol", "280"}, {"golden_pistol", "800"}, {"flare_pistol", "220"}, {"ak47", "300"}, {"akm_classic", "340"}, {"ak74", "400"}, {"aks74u", "360"}, {"ak_tactical", "520"}, {"rpk", "600"}, {"golden_ak", "900"}, {"ak12", "680"}, {"ak_grenadier", "720"}, {"bolt_sniper", "420"}, {"hunting_rifle", "160"}, {"marksman_rifle", "560"}, {"anti_materiel_rifle", "860"}, {"ghillie_sniper", "640"}, {"silent_sniper", "760"}, {"lever_rifle", "240"}, {"longshot_rifle", "950"}, {"survivor_smg", "200"},
                {"pump_shotgun", "140"}, {"sawed_off", "90"}, {"auto_shotgun", "460"}, {"ak_shotgun", "540"}, {"behemoth_shotgun", "820"}};
        for (String[] g : guns) {
            int lv = Integer.parseInt(g[1]);
            r("kiyamet:" + g[0], 1, lv, "kiyamet:metal_plate", 2 + lv / 200, "kiyamet:circuit", 1 + lv / 250, "minecraft:crossbow", 1);
        }
    }

    private LabRecipes() {}
}

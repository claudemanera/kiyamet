package com.kiyamet.registry;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 1.7: one creative tab per kind of thing instead of a single TWD tab. */
public final class CreativeTabs {
    enum Tab {
        WEAPONS("Weapons", "ak47"), AMMO("Ammo & Explosives", "rifle_ammo"), GADGETS("Gadgets & Tech", "jetpack"), MEDICAL("Medical & Survival", "medkit"),
        FOOD("Food & Farming", "tomato"), FURNITURE("Furniture", "double_bed"), BLOCKS("Blocks & Defense", "steel_block"), VEHICLES("Vehicles", "car_sedan_kit"),
        MONEY("Money & Bags", "money_bag"), STONES("Power Stones", "stone_gauntlet"), EGGS("Spawn Eggs", "runner_zombie_spawn_egg"), ELEMENTS("Elements", "flame_sword"), MISC("TWD Misc", "lost_note");
        final String title, icon;
        Tab(String t, String i) { title = t; icon = i; }
    }

    static Tab of(Item it) {
        String id = BuiltInRegistries.ITEM.getKey(it).getPath();
        if (id.endsWith("_spawn_egg")) return Tab.EGGS;
        if (it instanceof com.kiyamet.element.ElementWeapon || id.endsWith("_core") && !id.equals("energy_core") && !id.equals("tech_core") && !id.startsWith("robot_core") || id.equals("ancient_rod") || id.equals("sage_map")) return Tab.ELEMENTS;
        if (it instanceof com.kiyamet.item.GunItem || id.equals("gun_tome") || id.equals("energy_sword") || id.equals("laser_rifle")) return Tab.WEAPONS;
        if (id.endsWith("_ammo") || id.equals("shotgun_shell") || id.equals("plasma_cell") || id.startsWith("magazine") || id.endsWith("grenade") || id.equals("molotov")
                || id.equals("flashbang") || id.endsWith("_bomb")) return Tab.AMMO;
        if (id.endsWith("_kit") && !id.equals("repair_kit") || id.endsWith("_keys") || id.equals("vehicle_workshop") || id.equals("fuel_canister")) return Tab.VEHICLES;
        if (id.startsWith("dollar_") || id.contains("money_bag") || it instanceof com.kiyamet.backpack.BackpackItem || id.endsWith("_bag")) return Tab.MONEY;
        if (id.endsWith("_stone") && !id.contains("block") || id.equals("stone_gauntlet") || id.startsWith("stone_codex")) return Tab.STONES;
        if (id.equals("medkit") || id.equals("bandage") || id.equals("vaccine") || id.equals("healing_syringe") || id.endsWith("_pelt") || id.equals("sheepskin")
                || id.equals("walker_guts") || id.equals("repair_kit")) return Tab.MEDICAL;
        if (it.components().has(net.minecraft.core.component.DataComponents.FOOD) || id.endsWith("_seeds") || id.endsWith("_sapling") || id.endsWith("_leaves")
                || id.endsWith("_log") || id.equals("spoiled_food")) return Tab.FOOD;
        if (it instanceof BlockItem bi && (com.kiyamet.furniture.Furniture.isFurniture(bi.getBlock()) || bi.getBlock() == com.kiyamet.food.CookingTable.BLOCK || bi.getBlock() == com.kiyamet.twd.MainTable.BLOCK)) return Tab.FURNITURE;
        if (it instanceof BlockItem) return Tab.BLOCKS;
        if (it instanceof com.kiyamet.item.GadgetItem || id.startsWith("robot_core") || id.equals("radio") || id.equals("circuit") || id.equals("energy_cell")
                || id.equals("tech_core") || id.equals("metal_plate") || id.equals("lens") || id.equals("void_shard") || id.equals("energy_core")) return Tab.GADGETS;
        return Tab.MISC;
    }

    public static void init() {
        for (Tab t : Tab.values()) {
            ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Kiyamet.id("tab_" + t.name().toLowerCase()));
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.get(t.icon)))
                    .title(Component.literal("TWD: " + t.title))
                    .displayItems((params, out) -> { for (Item it : ModItems.ALL) if (of(it) == t) out.accept(it); })
                    .build());
        }
    }

    private CreativeTabs() {}
}

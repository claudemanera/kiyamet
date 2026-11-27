package com.kiyamet;

import com.kiyamet.registry.ModBlocks;
import com.kiyamet.registry.ModItems;
import com.kiyamet.registry.ModWorldgen;
import com.kiyamet.skill.ModAttachments;
import com.kiyamet.skill.SkillCommand;
import com.kiyamet.skill.SkillEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kiyamet implements ModInitializer {
    public static final String MOD_ID = "kiyamet";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        com.kiyamet.stone.GauntletData.init();
        ModBlocks.init();
        ModItems.init();
        com.kiyamet.farm.ModFarm.init();
        com.kiyamet.egg.ModEggs.init();
        com.kiyamet.item.BombProjectile.init();
        com.kiyamet.money.Money.init();
        com.kiyamet.npc.Starter.init();
        com.kiyamet.vehicle.Autopilot.init();
        com.kiyamet.season.Calendar.init();
        com.kiyamet.food.Recipes.init();
        com.kiyamet.food.CookingTable.init();
        com.kiyamet.twd.MainTable.init();
        com.kiyamet.gunsmith.GunHandling.init();
        com.kiyamet.power.Renewables.init();
        com.kiyamet.settle.Settlement.init();
        com.kiyamet.settle.Family.init();
        com.kiyamet.element.ElementItems.init();
        com.kiyamet.element.ElementMobs.init();
        com.kiyamet.element.ElementPowers.init();
        com.kiyamet.element.Temples.init();
        com.kiyamet.creation.CreationItems.init();
        com.kiyamet.creation.CreationSage.init();
        com.kiyamet.end.EndMobs.init();
        com.kiyamet.end.EndStructures.init();
        com.kiyamet.help.TwdHelp.init();
        com.kiyamet.food.Spoilage.init();
        com.kiyamet.furniture.Furniture.init();
        com.kiyamet.water.Thirst.init();
        com.kiyamet.wild.ModWild.init();
        com.kiyamet.backpack.Backpacks.init();
        ModWorldgen.init();
        com.kiyamet.world.ModStructures.init();
        ModAttachments.init();
        com.kiyamet.world.Infection.init();
        com.kiyamet.entity.ModEntities.init();
        com.kiyamet.boss.ModBosses.init();
        com.kiyamet.robot.ModRobots.init();
        com.kiyamet.vehicle.ModVehicles.init();
        com.kiyamet.ally.ModAllies.init();
        com.kiyamet.defense.DeviceSwitch.init();
        com.kiyamet.twd.Camouflage.init();
        com.kiyamet.skill.Powers.init();
        com.kiyamet.skill.Perks.init();
        com.kiyamet.npc.ModHumans.init();
        com.kiyamet.npc.Rename.init();
        com.kiyamet.npc.Radio.init();
        com.kiyamet.npc.BaseRaids.init();
        com.kiyamet.npc.WorldEvents.init();
        com.kiyamet.npc.Talk.init();
        com.kiyamet.npc.Captives.init();
        com.kiyamet.enchant.ModEnchant.init();
        com.kiyamet.season.Seasons.init();
        com.kiyamet.npc.Mind.init();
        com.kiyamet.twd.ModTwd.init();
        com.kiyamet.net.KeyActions.init();
        SkillEvents.init();
        SkillCommand.init();
        com.kiyamet.stone.StoneEvents.init();
        com.kiyamet.registry.CreativeTabs.init();
        LOG.info("Kiyamet loaded: {} items, {} blocks", ModItems.ALL.size(), ModBlocks.ALL.size());
    }
}

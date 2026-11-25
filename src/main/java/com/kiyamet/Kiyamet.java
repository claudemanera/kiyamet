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
        com.kiyamet.backpack.Backpacks.init();
        ModWorldgen.init();
        com.kiyamet.world.ModStructures.init();
        ModAttachments.init();
        com.kiyamet.world.Infection.init();
        com.kiyamet.entity.ModEntities.init();
        com.kiyamet.boss.ModBosses.init();
        com.kiyamet.robot.ModRobots.init();
        com.kiyamet.ally.ModAllies.init();
        com.kiyamet.defense.DeviceSwitch.init();
        com.kiyamet.twd.Camouflage.init();
        com.kiyamet.skill.Powers.init();
        com.kiyamet.skill.Perks.init();
        com.kiyamet.npc.ModHumans.init();
        com.kiyamet.net.KeyActions.init();
        SkillEvents.init();
        SkillCommand.init();
        com.kiyamet.stone.StoneEvents.init();
        LOG.info("Kiyamet loaded: {} items, {} blocks", ModItems.ALL.size(), ModBlocks.ALL.size());
    }
}

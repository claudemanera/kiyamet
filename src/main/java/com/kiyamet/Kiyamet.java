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
        ModBlocks.init();
        ModItems.init();
        ModWorldgen.init();
        ModAttachments.init();
        com.kiyamet.world.Infection.init();
        com.kiyamet.entity.ModEntities.init();
        SkillEvents.init();
        SkillCommand.init();
        LOG.info("Kiyamet loaded: {} items, {} blocks", ModItems.ALL.size(), ModBlocks.ALL.size());
    }
}

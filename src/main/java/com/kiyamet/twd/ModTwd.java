package com.kiyamet.twd;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

/** Shelter Core, Mission Board and Lost Notes. */
public final class ModTwd {
    private static final ResourceKey<EntityType<?>> CORE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("shelter_core"));
    private static final ResourceKey<EntityType<?>> BOARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("mission_board"));
    public static final EntityType<ShelterCore> SHELTER_CORE = Registry.register(BuiltInRegistries.ENTITY_TYPE, CORE_KEY,
            EntityType.Builder.<ShelterCore>of(ShelterCore::new, MobCategory.MISC).sized(0.9f, 1.0f).clientTrackingRange(10).fireImmune().build(CORE_KEY));
    public static final EntityType<MissionBoard> MISSION_BOARD = Registry.register(BuiltInRegistries.ENTITY_TYPE, BOARD_KEY,
            EntityType.Builder.<MissionBoard>of(MissionBoard::new, MobCategory.MISC).sized(1.1f, 1.1f).clientTrackingRange(10).fireImmune().build(BOARD_KEY));
    public static final Item SHELTER_CORE_ITEM = ModItems.custom("shelter_core", p -> new ShelterCore.CoreItem(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final Item QUEST_ITEM = ModItems.custom("quest_item", p -> new Item(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final Item LOST_NOTE = ModItems.custom("lost_note", p -> new com.kiyamet.item.LostNoteItem(p.stacksTo(1)));

    public static void init() {
        FabricDefaultAttributeRegistry.register(SHELTER_CORE, Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ARMOR, 10).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.MOVEMENT_SPEED, 0));
        FabricDefaultAttributeRegistry.register(MISSION_BOARD, Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.MOVEMENT_SPEED, 0));
        MissionBoard.init();
        Story.init();
    }

    private ModTwd() {}
}

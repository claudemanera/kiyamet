package com.kiyamet.end;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;

/** TWD 2.2: the zombie enderman. */
public final class EndMobs {
    public static final ResourceKey<EntityType<?>> ZE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("zombie_enderman"));
    public static final EntityType<ZombieEnderman> ZOMBIE_ENDERMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE, ZE_KEY,
            EntityType.Builder.<ZombieEnderman>of(ZombieEnderman::new, MobCategory.MONSTER).sized(0.6f, 2.9f).eyeHeight(2.55f).clientTrackingRange(8).build(ZE_KEY));

    public static void init() {
        FabricDefaultAttributeRegistry.register(ZOMBIE_ENDERMAN, EnderMan.createAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 4));
        net.minecraft.world.entity.SpawnPlacements.register(ZOMBIE_ENDERMAN, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
        BiomeModifications.addSpawn(BiomeSelectors.foundInTheEnd(), MobCategory.MONSTER, ZOMBIE_ENDERMAN, 8, 1, 3);
    }

    private EndMobs() {}
}

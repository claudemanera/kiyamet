package com.kiyamet.entity;

import com.kiyamet.Kiyamet;
import java.util.EnumMap;
import java.util.Map;
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
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class ModEntities {
    public static final Map<ZombieKind, EntityType<KZombie>> ZOMBIES = new EnumMap<>(ZombieKind.class);

    static {
        for (ZombieKind k : ZombieKind.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(k.id));
            EntityType<KZombie> type = EntityType.Builder.<KZombie>of((t, l) -> new KZombie(t, l, k), MobCategory.MONSTER)
                    .sized(k.width, k.height).eyeHeight(k.height * 0.88f).clientTrackingRange(8).build(key);
            ZOMBIES.put(k, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        }
    }

    public static void init() {
        ZOMBIES.forEach((k, type) -> {
            FabricDefaultAttributeRegistry.register(type, Zombie.createAttributes()
                    .add(Attributes.MAX_HEALTH, k.health)
                    .add(Attributes.MOVEMENT_SPEED, k.speed)
                    .add(Attributes.ATTACK_DAMAGE, k.damage)
                    .add(Attributes.ARMOR, k.armor)
                    .add(Attributes.FOLLOW_RANGE, k == ZombieKind.LISTENER ? 48 : 40)
                    .add(Attributes.KNOCKBACK_RESISTANCE, k.scale > 1.2f ? 0.8 : 0.1));
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, type, k.weight, 1, k.scale > 1.2f ? 1 : 4);
        });
    }

    private ModEntities() {}
}

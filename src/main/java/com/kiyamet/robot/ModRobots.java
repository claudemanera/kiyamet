package com.kiyamet.robot;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumMap;
import java.util.Map;

public final class ModRobots {
    public static final Map<RobotKind, EntityType<RobotEntity>> TYPES = new EnumMap<>(RobotKind.class);

    static {
        for (RobotKind k : RobotKind.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(k.id));
            EntityType<RobotEntity> type = EntityType.Builder.<RobotEntity>of((t, l) -> new RobotEntity(t, l, k), MobCategory.MONSTER)
                    .sized(k.width, k.height).eyeHeight(k.height * 0.85f).clientTrackingRange(8).fireImmune().build(key);
            TYPES.put(k, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type));
        }
    }

    public static void init() {
        TYPES.forEach((k, type) -> FabricDefaultAttributeRegistry.register(type, Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FLYING_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, k == RobotKind.ROBOT_HEAVY ? 0.8 : 0.3)));
    }

    private ModRobots() {}
}

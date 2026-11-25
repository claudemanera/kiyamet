package com.kiyamet.boss;

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

public final class ModBosses {
    public static final Map<BossKind, EntityType<BossEntity>> TYPES = new EnumMap<>(BossKind.class);

    static {
        for (BossKind k : BossKind.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(k.id));
            EntityType.Builder<BossEntity> b = EntityType.Builder.<BossEntity>of((t, l) -> new BossEntity(t, l, k), MobCategory.MONSTER)
                    .sized(k.width, k.height).eyeHeight(k.height * 0.88f).clientTrackingRange(10);
            if (k.fireImmune) b = b.fireImmune();
            TYPES.put(k, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key)));
        }
    }

    public static void init() {
        TYPES.forEach((k, type) -> FabricDefaultAttributeRegistry.register(type, Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.ARMOR, k.armor)
                .add(Attributes.ARMOR_TOUGHNESS, k.armor / 2)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.SCALE, 1.0)));
        BossRewards.init();
    }

    private ModBosses() {}
}

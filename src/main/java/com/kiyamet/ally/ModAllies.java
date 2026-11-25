package com.kiyamet.ally;

import com.kiyamet.Kiyamet;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModAllies {
    private static final ResourceKey<EntityType<?>> TURRET_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("auto_turret"));
    public static final EntityType<TurretDrone> TURRET = Registry.register(BuiltInRegistries.ENTITY_TYPE, TURRET_KEY,
            EntityType.Builder.<TurretDrone>of(TurretDrone::new, MobCategory.MISC).sized(0.8f, 0.5f).fireImmune().clientTrackingRange(8).build(TURRET_KEY));

    public static void init() {
        FabricDefaultAttributeRegistry.register(TURRET, Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ARMOR, 10));
    }

    private ModAllies() {}
}

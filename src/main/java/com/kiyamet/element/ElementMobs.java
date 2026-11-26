package com.kiyamet.element;

import com.kiyamet.Kiyamet;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.PathfinderMob;

/** Registers the 14 temple guardians and the 14 sages. */
public final class ElementMobs {
    public static final Map<Element, EntityType<TempleGuardian>> GUARDIANS = new EnumMap<>(Element.class);
    public static final Map<Element, EntityType<Sage>> SAGES = new EnumMap<>(Element.class);

    static {
        for (Element e : Element.values()) {
            ResourceKey<EntityType<?>> gk = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(e.guardian()));
            EntityType.Builder<TempleGuardian> gb = EntityType.Builder.<TempleGuardian>of((t, l) -> new TempleGuardian(t, l, e), MobCategory.MONSTER).sized(0.7f, 2.2f).eyeHeight(1.9f).clientTrackingRange(10);
            if (e == Element.FLAME || e == Element.FIRE) gb = gb.fireImmune();
            GUARDIANS.put(e, Registry.register(BuiltInRegistries.ENTITY_TYPE, gk, gb.build(gk)));
            ResourceKey<EntityType<?>> sk = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(e.sage()));
            SAGES.put(e, Registry.register(BuiltInRegistries.ENTITY_TYPE, sk, EntityType.Builder.<Sage>of((t, l) -> new Sage(t, l, e), MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build(sk)));
        }
    }

    public static void init() {
        GUARDIANS.forEach((e, t) -> FabricDefaultAttributeRegistry.register(t, Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 110).add(Attributes.ATTACK_DAMAGE, 13)
                .add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 6).add(Attributes.MOVEMENT_SPEED, 0.29).add(Attributes.FOLLOW_RANGE, 28).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)));
        SAGES.forEach((e, t) -> FabricDefaultAttributeRegistry.register(t, PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.2)));
    }

    private ElementMobs() {}
}

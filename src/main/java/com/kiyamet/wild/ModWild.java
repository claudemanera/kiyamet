package com.kiyamet.wild;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Registers the wild animals, their meat, and spawning (ice bears roam in winter). */
public final class ModWild {
    public static final Map<WildKind, EntityType<WildAnimal>> TYPES = new EnumMap<>(WildKind.class);
    public static final String[][] MEATS = {{"venison", "Venison", "3", "8"}, {"bear_meat", "Bear Meat", "4", "10"}, {"boar_meat", "Boar Meat", "3", "9"},
            {"moose_meat", "Moose Meat", "4", "11"}, {"big_cat_meat", "Big Cat Meat", "3", "8"}, {"croc_meat", "Crocodile Meat", "3", "8"}};

    static {
        for (WildKind k : WildKind.values()) {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id(k.id));
            EntityType<WildAnimal> t = EntityType.Builder.<WildAnimal>of((type, l) -> new WildAnimal(type, l, k), k.zombie ? MobCategory.MONSTER : MobCategory.CREATURE)
                    .sized(k.width, k.height).clientTrackingRange(10).build(key);
            TYPES.put(k, Registry.register(BuiltInRegistries.ENTITY_TYPE, key, t));
        }
    }

    public static void init() {
        TYPES.forEach((k, t) -> FabricDefaultAttributeRegistry.register(t, Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, k.hp).add(Attributes.ATTACK_DAMAGE, k.damage).add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, k.hp >= 50 ? 0.7 : 0.3).add(Attributes.ATTACK_KNOCKBACK, k.predator ? 1.2 : 0.5)
                .add(Attributes.ARMOR, k == WildKind.CROCODILE ? 8 : k.hp >= 60 ? 4 : 0)));
        for (String[] m : MEATS) {
            int n = Integer.parseInt(m[2]), c = Integer.parseInt(m[3]);
            ModItems.custom(m[0], p -> new Item(p.food(new FoodProperties(n, 0.3f, false))));
            ModItems.custom("cooked_" + m[0], p -> new Item(p.food(new FoodProperties(c, 0.9f, false))));
        }
        spawn(WildKind.LION, BiomeSelectors.includeByKey(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA), 6, 1, 3);
        spawn(WildKind.TIGER, BiomeSelectors.includeByKey(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE), 6, 1, 1);
        spawn(WildKind.BROWN_BEAR, BiomeSelectors.includeByKey(Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.DARK_FOREST), 5, 1, 2);
        spawn(WildKind.ICE_BEAR, BiomeSelectors.includeByKey(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_TAIGA, Biomes.FROZEN_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE), 5, 1, 2);
        spawn(WildKind.CROCODILE, BiomeSelectors.includeByKey(Biomes.SWAMP, Biomes.MANGROVE_SWAMP), 8, 1, 2);
        spawn(WildKind.WILD_BOAR, BiomeSelectors.includeByKey(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.SAVANNA, Biomes.BIRCH_FOREST, Biomes.TAIGA), 8, 2, 4);
        spawn(WildKind.DEER, BiomeSelectors.includeByKey(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.TAIGA, Biomes.MEADOW, Biomes.PLAINS, Biomes.FLOWER_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST), 12, 2, 4);
        spawn(WildKind.MOOSE, BiomeSelectors.includeByKey(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SWAMP), 5, 1, 2);
        // walker animals: very rare, anywhere in the overworld at night
        for (WildKind k : new WildKind[]{WildKind.ZOMBIE_LION, WildKind.ZOMBIE_TIGER, WildKind.ZOMBIE_BEAR, WildKind.ZOMBIE_DEER})
            BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, TYPES.get(k), k == WildKind.ZOMBIE_DEER ? 2 : 1, 1, 1);
        // winter: ice bears come down from the mountains
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 2400 != 1200) return;
            ServerLevel ow = server.overworld();
            if (com.kiyamet.season.Seasons.season(ow) != com.kiyamet.season.Seasons.Season.WINTER) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level() != ow || ow.random.nextFloat() > 0.3f) continue;
                if (!ow.getEntitiesOfClass(WildAnimal.class, p.getBoundingBox().inflate(64), w -> w.kind == WildKind.ICE_BEAR).isEmpty()) continue;
                double a = ow.random.nextDouble() * Math.PI * 2;
                BlockPos at = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition().offset((int) (Math.cos(a) * 36), 0, (int) (Math.sin(a) * 36)));
                if (!ow.isLoaded(at) || !(ow.getBlockState(at).is(Blocks.SNOW) || ow.getBlockState(at.below()).is(Blocks.SNOW_BLOCK) || ow.getBlockState(at.below()).is(Blocks.GRASS_BLOCK))) continue;
                WildAnimal b = TYPES.get(WildKind.ICE_BEAR).create(ow, EntitySpawnReason.EVENT);
                if (b == null) continue;
                b.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                ow.addFreshEntity(b);
            }
        });
    }

    static void spawn(WildKind k, java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> where, int weight, int min, int max) {
        BiomeModifications.addSpawn(where, MobCategory.CREATURE, TYPES.get(k), weight, min, max);
    }

    private ModWild() {}
}

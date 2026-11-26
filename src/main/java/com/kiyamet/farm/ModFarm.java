package com.kiyamet.farm;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;

/** TWD 1.6 farming: 8 crops, 2 fruit trees (orange, peach), 4 fur pelts. */
public final class ModFarm {
    /** crop id, produce food (nutrition, saturation) */
    public static final Object[][] CROPS = {{"tomato", 3, 0.4f}, {"corn", 4, 0.5f}, {"cucumber", 2, 0.4f}, {"eggplant", 3, 0.45f},
            {"bell_pepper", 3, 0.4f}, {"onion", 2, 0.3f}, {"cabbage", 3, 0.5f}, {"strawberry", 2, 0.35f}};
    public static final Object[][] TREES = {{"orange", 4, 0.4f}, {"peach", 4, 0.5f}};
    public static final List<Block> CROP_BLOCKS = new ArrayList<>(), CUTOUT = new ArrayList<>();
    public static final List<String> SEEDS = new ArrayList<>(), PRODUCE = new ArrayList<>();

    static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
    }

    static void blockItem(String name, Block b) {
        ModItems.custom(name, p -> new BlockItem(b, p.useBlockDescriptionPrefix()));
    }

    static void food(String name, int n, float sat) {
        ModItems.custom(name, p -> new Item(p.food(new FoodProperties(n, sat, false))));
        PRODUCE.add(name);
    }

    public static void init() {
        for (Object[] c : CROPS) {
            String id = (String) c[0];
            Block crop = block(id + "_crop", p -> new FarmCrop(p, id + "_seeds"), BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
            CROP_BLOCKS.add(crop); CUTOUT.add(crop);
            ModItems.custom(id + "_seeds", p -> new BlockItem(crop, p));
            SEEDS.add(id + "_seeds");
            food(id, (Integer) c[1], (Float) c[2]);
        }
        for (Object[] t : TREES) {
            String id = (String) t[0];
            Block log = block(id + "_log", RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
            blockItem(id + "_log", log);
            Block leaves = block(id + "_leaves", p -> new FruitLeaves(p, id), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).noOcclusion().randomTicks());
            blockItem(id + "_leaves", leaves); CUTOUT.add(leaves);
            TreeGrower grower = new TreeGrower(Kiyamet.MOD_ID + "_" + id, Optional.empty(),
                    Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, Kiyamet.id(id + "_tree"))), Optional.empty());
            Block sapling = block(id + "_sapling", p -> new SaplingBlock(grower, p), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING));
            blockItem(id + "_sapling", sapling); CUTOUT.add(sapling);
            food(id, (Integer) t[1], (Float) t[2]);
        }
        for (Fur.Kind k : Fur.Kind.values()) ModItems.custom(k.item, p -> new Fur.PeltItem(p.stacksTo(16), k));
        Fur.init();

        // wild fruit trees
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.PLAINS, Biomes.SPARSE_JUNGLE, Biomes.SUNFLOWER_PLAINS),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, Kiyamet.id("orange_tree_wild")));
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.CHERRY_GROVE),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, Kiyamet.id("peach_tree_wild")));

        // seeds from grass
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative()) return;
            if (!(state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN))) return;
            if (level.random.nextFloat() < 0.07f)
                Block.popResource(level, pos, new ItemStack(ModItems.get(SEEDS.get(level.random.nextInt(SEEDS.size())))));
        });
    }

    private ModFarm() {}
}

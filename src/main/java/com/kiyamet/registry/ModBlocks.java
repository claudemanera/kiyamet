package com.kiyamet.registry;

import com.kiyamet.Kiyamet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

/** Base-defense blocks + ruby ore. Durability values (zombie break time) live in DefenseRules (phase 7). */
public final class ModBlocks {
    public static final List<Block> ALL = new ArrayList<>();

    public static final Block REINFORCED_PLANKS = reg("reinforced_planks", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(4f, 6f));
    public static final Block STEEL_BLOCK = reg("steel_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8f, 12f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block STEEL_GRATE = reg("steel_grate", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(7f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block RUBY_BLOCK = reg("ruby_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(10f, 20f).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 4).requiresCorrectToolForDrops());
    public static final Block TITANIUM_BLOCK = reg("titanium_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(15f, 1200f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());
    public static final Block ENERGY_WALL = reg("energy_wall", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(50f, 3600000f).lightLevel(s -> 10).requiresCorrectToolForDrops());
    public static final Block STEEL_GATE = reg("steel_gate", p -> new DoorBlock(BlockSetType.IRON, p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(9f, 12f));
    public static final Block BARBED_WIRE = reg("barbed_wire", WebBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB).strength(2f).sound(SoundType.CHAIN));
    public static final Block RAZOR_WIRE = reg("razor_wire", WebBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.COBWEB).strength(3f).sound(SoundType.CHAIN));
    public static final Block ELECTRIC_FENCE = reg("electric_fence", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.TRIPWIRE).strength(2f).sound(SoundType.CHAIN));
    public static final Block SPIKE_TRAP = reg("spike_trap", Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.TRIPWIRE).strength(1.5f).sound(SoundType.METAL));
    public static final Block RUBY_ORE = reg("ruby_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE));
    public static final Block DEEPSLATE_RUBY_ORE = reg("deepslate_ruby_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE));

    public static final Block GUN_BENCH = reg("gun_bench", com.kiyamet.gunsmith.GunBenchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3f, 6f).sound(SoundType.WOOD));
    public static final Block VEHICLE_WORKSHOP = reg("vehicle_workshop", com.kiyamet.vehicle.WorkshopBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL));
    public static final Block LAB_TABLE = reg("lab_table", com.kiyamet.lab.LabTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f, 8f).sound(SoundType.METAL).noOcclusion());

    public static final Block GENERATOR = reg("generator", com.kiyamet.defense.GeneratorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f, 10f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(s -> s.getValue(com.kiyamet.defense.GeneratorBlock.LIT) && s.getValue(com.kiyamet.defense.GeneratorBlock.ON) ? 9 : 0));

    private static Block reg(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
        ALL.add(block);
        return block;
    }

    public static void init() {}
    private ModBlocks() {}
}

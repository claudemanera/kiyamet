package com.kiyamet.furniture;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** TWD 1.7 furniture: 20 pieces made at the Furniture Table, 3D models, beds you can sleep in, chairs you can sit on, storage, a fridge. */
public final class Furniture {
    public enum Kind { DECOR, SEAT, BED, STORAGE, FRIDGE, LAMP }
    /** id, title, kind, storage slots, light, seat height (px), shape x1 y1 z1 x2 y2 z2, cost (item, n ...) */
    public record Def(String id, String title, Kind kind, int slots, int light, int seatY, int[] box, Object[] cost) {}

    public static final List<Def> DEFS = List.of(
            new Def("double_bed", "Double Bed", Kind.BED, 0, 0, 9, new int[]{0, 0, 0, 16, 9, 16}, new Object[]{"minecraft:red_wool", 4, "minecraft:white_wool", 2, "minecraft:oak_planks", 6}),
            new Def("royal_bed", "Royal Double Bed", Kind.BED, 0, 0, 10, new int[]{0, 0, 0, 16, 10, 16}, new Object[]{"minecraft:blue_wool", 4, "minecraft:white_wool", 2, "minecraft:dark_oak_planks", 6, "minecraft:gold_ingot", 2}),
            new Def("wardrobe", "Wardrobe", Kind.STORAGE, 54, 0, 0, new int[]{0, 0, 2, 16, 31, 16}, new Object[]{"minecraft:oak_planks", 12, "minecraft:chest", 2, "minecraft:iron_nugget", 4}),
            new Def("fridge", "Fridge", Kind.FRIDGE, 54, 0, 0, new int[]{1, 0, 1, 15, 30, 15}, new Object[]{"minecraft:iron_ingot", 10, "minecraft:snow_block", 4, "minecraft:redstone", 4, "kiyamet:circuit", 1}),
            new Def("kitchen_cabinet", "Kitchen Cabinet", Kind.STORAGE, 27, 0, 0, new int[]{0, 0, 1, 16, 16, 16}, new Object[]{"minecraft:spruce_planks", 6, "minecraft:smooth_stone_slab", 2}),
            new Def("kitchen_sink", "Kitchen Sink", Kind.DECOR, 0, 0, 0, new int[]{0, 0, 1, 16, 16, 16}, new Object[]{"minecraft:spruce_planks", 4, "minecraft:iron_ingot", 3, "minecraft:water_bucket", 1}),
            new Def("stove", "Stove", Kind.DECOR, 0, 0, 0, new int[]{0, 0, 1, 16, 16, 16}, new Object[]{"minecraft:iron_ingot", 6, "minecraft:furnace", 1, "minecraft:black_concrete", 2}),
            new Def("dining_table", "Dining Table", Kind.DECOR, 0, 0, 0, new int[]{0, 0, 0, 16, 14, 16}, new Object[]{"minecraft:oak_planks", 6, "minecraft:oak_log", 2}),
            new Def("wooden_chair", "Wooden Chair", Kind.SEAT, 0, 0, 7, new int[]{3, 0, 3, 13, 9, 13}, new Object[]{"minecraft:oak_planks", 3, "minecraft:stick", 2}),
            new Def("armchair", "Armchair", Kind.SEAT, 0, 0, 7, new int[]{1, 0, 1, 15, 10, 15}, new Object[]{"minecraft:brown_wool", 4, "minecraft:oak_planks", 2}),
            new Def("sofa", "Sofa", Kind.SEAT, 0, 0, 7, new int[]{0, 0, 1, 16, 10, 15}, new Object[]{"minecraft:gray_wool", 6, "minecraft:spruce_planks", 2}),
            new Def("coffee_table", "Coffee Table", Kind.DECOR, 0, 0, 0, new int[]{1, 0, 3, 15, 7, 13}, new Object[]{"minecraft:dark_oak_planks", 3, "minecraft:glass_pane", 2}),
            new Def("bookcase", "Bookcase", Kind.STORAGE, 27, 0, 0, new int[]{0, 0, 4, 16, 16, 16}, new Object[]{"minecraft:oak_planks", 6, "minecraft:book", 6}),
            new Def("office_desk", "Office Desk", Kind.STORAGE, 9, 0, 0, new int[]{0, 0, 2, 16, 13, 16}, new Object[]{"minecraft:birch_planks", 5, "minecraft:iron_ingot", 1}),
            new Def("floor_lamp", "Floor Lamp", Kind.LAMP, 0, 15, 0, new int[]{4, 0, 4, 12, 16, 12}, new Object[]{"minecraft:iron_ingot", 2, "minecraft:glowstone", 1, "minecraft:white_wool", 1}),
            new Def("television", "Television", Kind.LAMP, 0, 6, 0, new int[]{1, 0, 5, 15, 13, 12}, new Object[]{"minecraft:black_concrete", 3, "minecraft:glass_pane", 2, "kiyamet:circuit", 2, "minecraft:redstone", 2}),
            new Def("bathtub", "Bathtub", Kind.SEAT, 0, 0, 3, new int[]{0, 0, 2, 16, 9, 14}, new Object[]{"minecraft:white_concrete", 5, "minecraft:iron_ingot", 2, "minecraft:water_bucket", 1}),
            new Def("toilet", "Toilet", Kind.SEAT, 0, 0, 8, new int[]{3, 0, 2, 13, 14, 14}, new Object[]{"minecraft:white_concrete", 4, "minecraft:iron_nugget", 2}),
            new Def("gun_rack", "Gun Rack", Kind.STORAGE, 18, 0, 0, new int[]{0, 0, 11, 16, 16, 16}, new Object[]{"minecraft:dark_oak_planks", 4, "minecraft:iron_ingot", 3}),
            new Def("nightstand", "Nightstand", Kind.STORAGE, 9, 7, 0, new int[]{2, 0, 2, 14, 14, 14}, new Object[]{"minecraft:oak_planks", 3, "minecraft:torch", 1}));

    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final List<Block> ALL = new ArrayList<>();
    public static Block TABLE;
    public static BlockEntityType<StorageBE> STORAGE_BE;
    public static EntityType<SeatEntity> SEAT;

    static Block block(String name, Function<BlockBehaviour.Properties, Block> f, BlockBehaviour.Properties p) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id(name));
        Block b = Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
        ModItems.custom(name, ip -> new BlockItem(b, ip.useBlockDescriptionPrefix()));
        return b;
    }

    public static boolean isFurniture(Block b) { return ALL.contains(b) || b == TABLE || b instanceof BagBlock; }

    public static void init() {
        for (Def d : DEFS) {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.ofFullCopy(d.kind == Kind.FRIDGE || d.id.equals("stove") || d.id.equals("gun_rack") ? Blocks.IRON_BLOCK : Blocks.OAK_PLANKS)
                    .strength(2f, 4f).noOcclusion().lightLevel(s -> d.light);
            if (d.kind == Kind.BED) p = p.sound(SoundType.WOOL);
            if (d.id.equals("bathtub") || d.id.equals("toilet")) p = p.sound(SoundType.STONE);
            Block b = d.kind == Kind.BED ? block(d.id, pp -> new FurnitureBed(d.id.equals("royal_bed") ? DyeColor.BLUE : DyeColor.RED, pp), p)
                    : block(d.id, pp -> new FurnitureBlock(pp, d), p);
            BLOCKS.put(d.id, b); ALL.add(b);
        }
        TABLE = block("furniture_table", FurnitureTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5f));
        BagBlock.register();
        List<Block> withBe = new ArrayList<>();
        for (Def d : DEFS) if (d.kind == Kind.STORAGE || d.kind == Kind.FRIDGE) withBe.add(BLOCKS.get(d.id));
        withBe.add(BagBlock.BLOCK);
        STORAGE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Kiyamet.id("storage"), FabricBlockEntityTypeBuilder.create(StorageBE::new, withBe.toArray(new Block[0])).build());
        ResourceKey<EntityType<?>> sk = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("seat"));
        SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, sk, EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.2f, 0.2f).noSave().clientTrackingRange(8).build(sk));
    }

    public static Def def(String id) { for (Def d : DEFS) if (d.id.equals(id)) return d; return null; }

    private Furniture() {}
}

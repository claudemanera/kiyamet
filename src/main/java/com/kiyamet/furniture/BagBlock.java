package com.kiyamet.furniture;

import com.kiyamet.Kiyamet;
import com.kiyamet.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Backpacks and money bags put down on the ground (sneak + right-click a block). Each is half a block tall, two fit in one
 * block on top of each other. Right-click picks the top one up again - contents and money stay inside.
 */
public class BagBlock extends BaseEntityBlock {
    /** 1-10 backpacks (by tier), 11 money bag, 12 gold money bag */
    public static final String[] IDS = {"", "canvas_satchel", "scout_pack", "hiker_pack", "survivor_pack", "ranger_pack", "medic_pack", "tactical_pack",
            "military_rucksack", "expedition_pack", "apocalypse_pack", "money_bag", "gold_money_bag"};
    public static final IntegerProperty LOWER = IntegerProperty.create("lower", 0, 12), UPPER = IntegerProperty.create("upper", 0, 12);
    public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static Block BLOCK;
    static final VoxelShape HALF = Block.box(2, 0, 2, 14, 8, 14), FULL = Block.box(2, 0, 2, 14, 16, 14);

    public BagBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LOWER, 1).setValue(UPPER, 0).setValue(FACING, Direction.NORTH));
    }

    static void register() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Kiyamet.id("placed_bag"));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new BagBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).strength(0.5f).noOcclusion().sound(SoundType.WOOL).setId(key)));
    }

    public static int index(Item it) {
        String id = BuiltInRegistries.ITEM.getKey(it).getPath();
        for (int i = 1; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(BagBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LOWER, UPPER, FACING); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return s.getValue(UPPER) > 0 ? FULL : HALF; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StorageBE(pos, state); }

    /** Called from the bag items: sneak + right-click on a block puts the bag down (or on top of another bag). */
    public static InteractionResult place(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        int idx = index(stack.getItem());
        if (idx == 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos clicked = ctx.getClickedPos();
        BlockState cs = level.getBlockState(clicked);
        if (cs.getBlock() instanceof BagBlock && cs.getValue(UPPER) == 0 && level.getBlockEntity(clicked) instanceof StorageBE be) {
            be.setItem(1, stack.copyWithCount(1));
            level.setBlockAndUpdate(clicked, cs.setValue(UPPER, idx));
        } else {
            BlockPos at = clicked.relative(ctx.getClickedFace());
            if (!level.getBlockState(at).canBeReplaced()) return InteractionResult.FAIL;
            level.setBlockAndUpdate(at, BLOCK.defaultBlockState().setValue(LOWER, idx).setValue(UPPER, 0).setValue(FACING, player.getDirection().getOpposite()));
            if (level.getBlockEntity(at) instanceof StorageBE be) be.setItem(0, stack.copyWithCount(1));
        }
        level.playSound(null, clicked, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8f, 0.9f);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof StorageBE be)) return InteractionResult.SUCCESS;
        int slot = state.getValue(UPPER) > 0 ? 1 : 0;
        ItemStack it = be.removeItemNoUpdate(slot);
        if (!it.isEmpty() && !sp.getInventory().add(it)) sp.drop(it, false);
        if (slot == 1) level.setBlockAndUpdate(pos, state.setValue(UPPER, 0));
        else { be.clearContent(); level.removeBlock(pos, false); }
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.CONSUME;
    }
}

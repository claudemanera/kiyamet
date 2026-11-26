package com.kiyamet.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A furniture piece facing the player who placed it: decor, seat, storage, fridge or lamp. */
public class FurnitureBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public final Furniture.Def def;
    private final VoxelShape[] shapes = new VoxelShape[4];

    public FurnitureBlock(Properties p, Furniture.Def def) {
        super(p);
        this.def = def;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        int[] b = def.box();
        for (Direction d : Direction.Plane.HORIZONTAL) shapes[d.get2DDataValue()] = rotate(b, d);
    }

    static VoxelShape rotate(int[] b, Direction d) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        return switch (d) {
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return simpleCodec(p -> new FurnitureBlock(p, def)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return shapes[s.getValue(FACING).get2DDataValue()]; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return def.kind() == Furniture.Kind.STORAGE || def.kind() == Furniture.Kind.FRIDGE ? new StorageBE(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || def.kind() != Furniture.Kind.FRIDGE) return null;
        return (l, p, s, be) -> { if (be instanceof StorageBE sb) sb.fridgeTick(l); };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        switch (def.kind()) {
            case SEAT -> SeatEntity.sit(sp, pos, def.seatY() / 16.0);
            case STORAGE, FRIDGE -> { if (level.getBlockEntity(pos) instanceof StorageBE be) sp.openMenu(be); }
            case DECOR -> {
                if (def.id().equals("kitchen_sink")) level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.2f);
                else return InteractionResult.PASS;
            }
            default -> { return InteractionResult.PASS; }
        }
        return InteractionResult.CONSUME;
    }
}

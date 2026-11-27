package com.kiyamet.creation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The heart of a Sage of Creation's own temple. Build the temple around it however you like. */
public class CreationAltarBlock extends Block {
    static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CreationAltarBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext c) { return SHAPE; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (by instanceof ServerPlayer p) CreationSage.founded(p, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) CreationSage.useAltar(p, pos);
        return InteractionResult.SUCCESS;
    }
}

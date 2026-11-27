package com.kiyamet.creation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** The Table of Creation: it stands in the Temple of Wisdom in the End. It cannot be broken or taken. */
public class CreationTableBlock extends Block {
    static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public CreationTableBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            if (level.dimension() != Level.END) p.displayClientMessage(Component.literal("The Table of Creation only answers in the Temple of Wisdom, in the End.").withStyle(ChatFormatting.GRAY), true);
            else CreationTableMenu.open(p);
        }
        return InteractionResult.SUCCESS;
    }
}

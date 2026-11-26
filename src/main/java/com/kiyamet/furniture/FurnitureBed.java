package com.kiyamet.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A real bed (sleep, set spawn) drawn with a 3D block model instead of the vanilla bed renderer. */
public class FurnitureBed extends BedBlock {
    public FurnitureBed(DyeColor c, Properties p) { super(c, p); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }
}

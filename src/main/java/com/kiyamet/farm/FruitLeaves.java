package com.kiyamet.farm;

import com.kiyamet.season.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fruit tree leaves: fruit 0 = bare, 1 = blossom / green fruit, 2 = ripe. Ripe fruit falls to the ground by itself now and then,
 * or you pick it with right-click. Nothing grows in winter; autumn and summer are the harvest.
 */
public class FruitLeaves extends Block {
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", 0, 2);
    final String fruit;

    public FruitLeaves(Properties p, String fruit) {
        super(p);
        this.fruit = fruit;
        registerDefaultState(stateDefinition.any().setValue(FRUIT, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FRUIT); }
    @Override protected boolean isRandomlyTicking(BlockState s) { return true; }

    @Override
    protected void randomTick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
        Seasons.Season season = Seasons.season(level);
        int f = s.getValue(FRUIT);
        if (season == Seasons.Season.WINTER) { if (f > 0 && r.nextInt(3) == 0) level.setBlock(pos, s.setValue(FRUIT, f - 1), 2); return; }
        float grow = season == Seasons.Season.SPRING ? 0.12f : season == Seasons.Season.SUMMER ? 0.2f : 0.25f;
        if (f < 2) { if (r.nextFloat() < grow) level.setBlock(pos, s.setValue(FRUIT, f + 1), 2); return; }
        // ripe: drops on its own
        if (r.nextFloat() < 0.18f && level.getBlockState(pos.below()).isAir()) {
            ItemEntity it = new ItemEntity(level, pos.getX() + 0.5, pos.getY() - 0.3, pos.getZ() + 0.5, new ItemStack(com.kiyamet.registry.ModItems.get(fruit)));
            it.setDeltaMovement(0, -0.05, 0);
            level.addFreshEntity(it);
            level.setBlock(pos, s.setValue(FRUIT, 0), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (s.getValue(FRUIT) < 2) return InteractionResult.PASS;
        if (level instanceof ServerLevel sl) {
            int n = 1 + sl.random.nextInt(2);
            Block.popResource(level, pos, new ItemStack(com.kiyamet.registry.ModItems.get(fruit), n));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 1.1f);
            level.setBlock(pos, s.setValue(FRUIT, 0), 2);
        }
        return InteractionResult.SUCCESS;
    }
}

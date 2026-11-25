package com.kiyamet.defense;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Burns fuel (fuel canister = 4 min, coal = 1 min) and powers energy walls, electric fences and turrets within 8 blocks. */
public class GeneratorBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty FUEL = IntegerProperty.create("fuel", 0, 15);
    /** Master switch from the panel: when off the generator keeps its fuel and powers nothing. */
    public static final BooleanProperty ON = BooleanProperty.create("on");
    private static final int MINUTE = 1200;

    public GeneratorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FUEL, 0).setValue(ON, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT, FUEL, ON);
    }

    public static int fuelValue(ItemStack s) {
        if (s.is(Items.COAL) || s.is(Items.CHARCOAL)) return 1;
        if (s.is(Items.COAL_BLOCK)) return 9;
        if (s.is(Items.LAVA_BUCKET)) return 10;
        if (s.getItem() == net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id("fuel_canister"))) return 4;
        return 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int v = fuelValue(stack);
        if (v == 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        int fuel = state.getValue(FUEL);
        if (fuel >= 15) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            if (!player.isCreative()) {
                if (stack.is(Items.LAVA_BUCKET)) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                else stack.shrink(1);
            }
            boolean wasLit = state.getValue(LIT);
            level.setBlock(pos, state.setValue(FUEL, Math.min(15, fuel + v)).setValue(LIT, true), 3);
            if (!wasLit) level.scheduleTick(pos, this, MINUTE);
            Power.invalidate();
            level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) GeneratorMenu.open(player, sl, pos);
        return InteractionResult.SUCCESS;
    }

    /** Adds fuel from the panel; returns the number of items used (0 or 1). */
    public static boolean addFuel(ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GeneratorBlock g)) return false;
        int v = fuelValue(stack), fuel = state.getValue(FUEL);
        if (v == 0 || fuel >= 15) return false;
        boolean wasLit = state.getValue(LIT);
        level.setBlock(pos, state.setValue(FUEL, Math.min(15, fuel + v)).setValue(LIT, true), 3);
        if (!wasLit) level.scheduleTick(pos, g, MINUTE);
        Power.invalidate();
        level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1f, 0.8f);
        return true;
    }


    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ON)) { if (state.getValue(LIT)) level.scheduleTick(pos, this, MINUTE); return; }
        int fuel = state.getValue(FUEL) - 1;
        if (fuel <= 0) {
            level.setBlock(pos, state.setValue(FUEL, 0).setValue(LIT, false), 3);
            Power.invalidate();
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
        } else {
            level.setBlock(pos, state.setValue(FUEL, fuel), 3);
            level.scheduleTick(pos, this, MINUTE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && state.getValue(ON) && random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.05, 0);
    }
}

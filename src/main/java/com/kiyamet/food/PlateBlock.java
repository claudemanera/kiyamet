package com.kiyamet.food;

import com.mojang.math.Transformation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A plate: works like an item frame laid on the table. Right-click with food: the food goes on the plate.
 * Right-click with an empty hand: take it back. Sneak + right-click with an empty hand: eat it from the plate.
 */
public class PlateBlock extends Block {
    static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 1.5, 14);
    static final String TAG = "kiyamet_plate";

    public PlateBlock(Properties p) { super(p); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos pos, CollisionContext c) { return SHAPE; }

    static Display.ItemDisplay food(Level l, BlockPos pos) {
        List<Display.ItemDisplay> d = l.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(pos), e -> e.getTags().contains(TAG));
        return d.isEmpty() ? null : d.get(0);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.has(DataComponents.FOOD)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel l)) return InteractionResult.SUCCESS;
        if (food(l, pos) != null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Display.ItemDisplay d = EntityType.ITEM_DISPLAY.create(l, EntitySpawnReason.TRIGGERED);
        if (d == null) return InteractionResult.PASS;
        d.setItemStack(stack.copyWithCount(1));
        d.setItemTransform(ItemDisplayContext.FIXED);
        float yaw = (float) Math.toRadians(-player.getYRot() + 180);
        d.setTransformation(new Transformation(new Vector3f(0, 0, 0), new Quaternionf().rotateY(yaw).rotateX((float) Math.toRadians(-90)), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf()));
        d.setPos(pos.getX() + 0.5, pos.getY() + 0.11, pos.getZ() + 0.5);
        d.addTag(TAG);
        l.addFreshEntity(d);
        if (!player.isCreative()) stack.shrink(1);
        l.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel l) || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        Display.ItemDisplay d = food(l, pos);
        if (d == null) return InteractionResult.PASS;
        ItemStack s = d.getItemStack().copy();
        d.discard();
        if (p.isShiftKeyDown() && p.getFoodData().needsFood() || p.isShiftKeyDown() && p.isCreative()) {
            ItemStack rest = s.finishUsingItem(l, p);                       // eat it right off the plate
            l.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1f, 1f);
            if (!rest.isEmpty() && !p.getInventory().add(rest)) p.drop(rest, false);
        } else {
            if (!p.getInventory().add(s)) p.drop(s, false);
            l.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel l, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, l, pos, moved);
        Display.ItemDisplay d = food(l, pos);
        if (d != null) { l.addFreshEntity(new ItemEntity(l, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, d.getItemStack().copy())); d.discard(); }
    }
}

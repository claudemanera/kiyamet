package com.kiyamet.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Invisible seat for chairs, sofas, toilets and bathtubs. Disappears when nobody sits on it. */
public class SeatEntity extends Entity {
    public SeatEntity(EntityType<?> t, Level l) { super(t, l); noPhysics = true; }

    public static void sit(ServerPlayer p, BlockPos pos, double h) {
        ServerLevel level = p.level();
        if (!level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) return;
        SeatEntity s = Furniture.SEAT.create(level, EntitySpawnReason.TRIGGERED);
        if (s == null) return;
        s.setPos(pos.getX() + 0.5, pos.getY() + h - 0.1, pos.getZ() + 0.5);
        level.addFreshEntity(s);
        p.startRiding(s, true, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (getPassengers().isEmpty() || level().getBlockState(blockPosition()).isAir() && level().getBlockState(blockPosition().below()).isAir())) discard();
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {}
    @Override protected void readAdditionalSaveData(ValueInput in) {}
    @Override protected void addAdditionalSaveData(ValueOutput out) {}
    @Override public boolean hurtServer(ServerLevel l, DamageSource s, float f) { return false; }
}

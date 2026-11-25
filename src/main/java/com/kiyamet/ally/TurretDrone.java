package com.kiyamet.ally;

import com.kiyamet.defense.Power;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;

/** Deployed by the Auto Turret gadget: hovers where it was placed and lasers enemies for 5 minutes (faster when powered). */
public class TurretDrone extends PathfinderMob implements GeoEntity {
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("move.walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Vec3 anchor;
    private int life = 6000, cooldown;
    public float bonus;

    public TurretDrone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<TurretDrone>("main", 0, t -> t.setAndContinue(HOVER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (anchor == null) anchor = position();
        Vec3 d = anchor.add(0, Math.sin(tickCount / 15.0) * 0.2, 0).subtract(position());
        setDeltaMovement(d.scale(0.3));
        if (--life <= 0) { level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.05); discard(); return; }
        if (cooldown-- > 0) return;
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16), e -> e instanceof Enemy && e.isAlive() && hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) { cooldown = 10; return; }
        boolean powered = Power.isPowered(level, blockPosition());
        cooldown = powered ? 8 : 15;
        getLookControl().setLookAt(target);
        Vec3 from = position().add(0, 0.2, 0), to = target.getEyePosition();
        int n = (int) (from.distanceTo(to) * 3);
        for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
        target.hurtServer(level, level.damageSources().mobAttack(this), 6 + bonus);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.5f, 2f);
    }
}

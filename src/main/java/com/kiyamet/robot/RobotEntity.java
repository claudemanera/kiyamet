package com.kiyamet.robot;

import com.kiyamet.entity.KZombie;
import com.kiyamet.world.Difficulty;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Robot soldiers of the tech sites: melee + laser shots. Drones hover. Immune to poison and infection. */
public class RobotEntity extends Monster implements GeoEntity {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final RobotKind kind;
    private int laserCooldown = 40;

    public RobotEntity(EntityType<? extends Monster> type, Level level, RobotKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 8 + (int) (kind.health / 8);
        if (kind.flying) setNoGravity(true);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (!kind.flying) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        }
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, KZombie.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RobotEntity>("main", 4, test -> test.setAndContinue(test.isMoving() || kind.flying ? WALK : IDLE)));
        controllers.add(new AnimationController<RobotEntity>("attack", 0, test -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != net.minecraft.world.effect.MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) triggerAnim("attack", "attack");
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity target = getTarget();
        if (kind.flying) {
            Vec3 goal = target != null ? target.position().add(0, 3.5, 0) : position().add(0, Math.sin(tickCount / 20.0) * 0.1, 0);
            Vec3 d = goal.subtract(position());
            if (d.length() > 1) d = d.normalize().scale(kind.speed);
            setDeltaMovement(getDeltaMovement().scale(0.6).add(d.scale(0.4)));
            if (target != null) getLookControl().setLookAt(target);
        }
        if (laserCooldown > 0) { laserCooldown--; return; }
        if (target == null || !target.isAlive() || distanceTo(target) > 20 || distanceTo(target) < 2.5 && !kind.flying || !hasLineOfSight(target)) return;
        laserCooldown = kind.laserCooldown;
        Vec3 from = getEyePosition(), to = target.getEyePosition();
        int n = (int) (from.distanceTo(to) * 3);
        for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
        float mult = (float) Math.min(2.0, Difficulty.multiplierNear(level, blockPosition()));
        target.hurtServer(level, level.damageSources().mobAttack(this), kind.laser * mult);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1f, 2f);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !kind.flying && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) return;
        drop(level, new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(kind == RobotKind.ROBOT_HEAVY ? 4 : 2)));
        if (random.nextFloat() < 0.6f) drop(level, new ItemStack(BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id("circuit")), 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.25f) drop(level, new ItemStack(BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id("energy_cell"))));
        if (kind == RobotKind.ROBOT_HEAVY && random.nextFloat() < 0.3f) drop(level, new ItemStack(BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id("tech_core"))));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), 40, 0.5, 0.5, 0.5, 0.3);
    }

    private void drop(ServerLevel level, ItemStack s) {
        level.addFreshEntity(new ItemEntity(level, getX(), getY() + 0.5, getZ(), s));
    }
}

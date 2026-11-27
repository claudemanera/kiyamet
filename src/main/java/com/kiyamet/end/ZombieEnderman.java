package com.kiyamet.end;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** TWD 2.2: an enderman the plague reached. It does not wait to be looked at - it hunts on sight, blinks behind you and bites. */
public class ZombieEnderman extends EnderMan implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle"), WALK = RawAnimation.begin().thenLoop("move.walk"), ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int blink;

    public ZombieEnderman(EntityType<? extends EnderMan> t, Level l) { super(t, l); xpReward = 12; }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, com.kiyamet.npc.HumanEntity.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount % 12 == 0) sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.6, getZ(), 2, 0.3, 0.8, 0.3, 0.2);
        LivingEntity t = getTarget();
        // blink: every few seconds it vanishes and comes out right behind its prey
        if (t != null && t.isAlive() && --blink <= 0 && distanceToSqr(t) > 9) {
            blink = 80 + random.nextInt(80);
            double yaw = Math.toRadians(t.getYRot());
            double x = t.getX() + Math.sin(yaw) * 2.0, z = t.getZ() - Math.cos(yaw) * 2.0;     // two steps behind its back
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 30, 0.3, 1, 0.3, 0.1);
            if (randomTeleport(x, t.getY(), z, true)) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 30, 0.3, 1, 0.3, 0.1);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            triggerAnim("main", "attack");
            if (target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0));
            if (target instanceof ServerPlayer p) com.kiyamet.world.Infection.onZombieHit(p, com.kiyamet.entity.ZombieKind.TOXIC);
        }
        return hit;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<ZombieEnderman>("main", 4, s -> s.setAndContinue(s.isMoving() ? WALK : IDLE)).triggerableAnim("attack", ATTACK));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

package com.kiyamet.element;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Armoured temple guardian: carries its element in every blow, never leaves its temple. */
public class TempleGuardian extends Monster implements GeoEntity {
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle"), WALK = RawAnimation.begin().thenLoop("move.walk"), ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    public final Element element;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TempleGuardian(EntityType<? extends Monster> t, Level l, Element e) { super(t, l); this.element = e; xpReward = 40; setPersistenceRequired(); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6, 120));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, com.kiyamet.npc.HumanEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && element != null && tickCount % 10 == 0)
            sl.sendParticles(new DustParticleOptions(element.color, 1.0f), getX(), getY() + 1.2, getZ(), 2, 0.3, 0.6, 0.3, 0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity le && element != null) { ElementPowers.onHit(element, le, this); triggerAnim("main", "attack"); }
        return hit;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<TempleGuardian>("main", 4, t -> t.setAndContinue(t.isMoving() ? WALK : IDLE)).triggerableAnim("attack", ATTACK));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

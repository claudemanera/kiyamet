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

/**
 * Auto Turret (1.6): a 3D tripod gun placed on the ground. Turns to the nearest walker, raider or masked one within
 * 22 blocks and fires on its own. Faster on generator power. Sneak + right-click to pack it back up.
 */
public class TurretDrone extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("attack.swing");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int cooldown;
    public float bonus;

    public TurretDrone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<TurretDrone>("main", 0, t -> t.setAndContinue(IDLE)).triggerableAnim("shoot", SHOOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public boolean causeFallDamage(double d, float m, DamageSource s) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (level() instanceof ServerLevel) {
            net.minecraft.world.item.ItemStack it = new net.minecraft.world.item.ItemStack(com.kiyamet.registry.ModItems.get("auto_turret"));
            if (!player.getInventory().add(it)) player.drop(it, false);
            playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 0.8f);
            discard();
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    static boolean hostile(LivingEntity e) {
        if (e instanceof Enemy) return true;
        return e instanceof com.kiyamet.npc.HumanEntity h && (h.faction() == com.kiyamet.npc.Faction.RAIDER || h.faction() == com.kiyamet.npc.Faction.WHISPERER);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (!(level() instanceof ServerLevel level)) return;
        if (cooldown-- > 0) return;
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(22), e -> hostile(e) && e.isAlive() && hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (target == null) { cooldown = 10; return; }
        boolean powered = Power.isPowered(level, blockPosition());
        cooldown = powered ? 5 : 9;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
        setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
        Vec3 look = new Vec3(dx, 0, dz).normalize();
        Vec3 from = position().add(0, 0.8, 0).add(look.scale(0.8)), to = target.getEyePosition().add(0, -0.3, 0);
        int n = (int) (from.distanceTo(to) * 2);
        for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 3, 0.05, 0.05, 0.05, 0.01);
        level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 1, 0, 0, 0, 0);
        target.hurtServer(level, level.damageSources().mobAttack(this), 5 + bonus * 2);
        level.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.7f, 1.4f);
        triggerAnim("main", "shoot");
        com.kiyamet.world.NoiseSystem.make(level, blockPosition(), 18, 4);
    }
}

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
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> ALLY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RobotEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    @org.jetbrains.annotations.Nullable private java.util.UUID owner;
    private boolean staying;

    public boolean isAlly() { return entityData.get(ALLY); }
    @org.jetbrains.annotations.Nullable public java.util.UUID owner() { return owner; }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ALLY, false);
    }

    /** Built at the Laboratory Table: fights for its owner. Engineering makes it tougher. */
    public void makeAlly(net.minecraft.server.level.ServerPlayer p, int engineering) {
        owner = p.getUUID();
        entityData.set(ALLY, true);
        setPersistenceRequired();
        double mult = 1 + engineering / 500.0;
        var hp = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(kind.health * 2 * mult);
        var dmg = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(kind.damage * mult);
        setHealth(getMaxHealth());
        setCustomName(net.minecraft.network.chat.Component.literal(p.getName().getString() + "'s " + (kind == RobotKind.ROBOT_HEAVY ? "Heavy Robot" : kind == RobotKind.SCOUT_DRONE ? "Drone" : "Robot")));
        setTarget(null);
    }

    @org.jetbrains.annotations.Nullable private net.minecraft.server.level.ServerPlayer ownerPlayer() {
        return owner != null && level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(owner) : null;
    }

    private boolean enemyOfMe(LivingEntity e) {
        if (!isAlly()) return e instanceof Player || e instanceof KZombie || e instanceof RobotEntity r && r.isAlly();
        if (e instanceof Player) return false;
        if (e instanceof RobotEntity r) return !r.isAlly();
        if (e instanceof com.kiyamet.npc.HumanEntity h) return h.faction() == com.kiyamet.npc.Faction.RAIDER || h.faction() == com.kiyamet.npc.Faction.WHISPERER;
        return e instanceof net.minecraft.world.entity.monster.Enemy;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Ally", isAlly());
        out.putBoolean("Staying", staying);
        if (owner != null) out.putString("Owner", owner.toString());
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(ALLY, in.getBooleanOr("Ally", false));
        staying = in.getBooleanOr("Staying", false);
        String o = in.getStringOr("Owner", "");
        owner = o.isEmpty() ? null : java.util.UUID.fromString(o);
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!isAlly() || !player.getUUID().equals(owner) || hand != net.minecraft.world.InteractionHand.MAIN_HAND) return super.mobInteract(player, hand);
        if (level().isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.is(net.minecraft.world.item.Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            heal(getMaxHealth() * 0.25f);
            if (!player.isCreative()) held.shrink(1);
            playSound(SoundEvents.IRON_GOLEM_REPAIR, 1f, 1.2f);
        } else {
            staying = !staying;
            getNavigation().stop();
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(getName().getString() + (staying ? ": holding position." : ": following you.")), true);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isAlly() && source.getEntity() != null && (source.getEntity().getUUID().equals(owner) || source.getEntity() instanceof com.kiyamet.npc.HumanEntity h && owner != null && owner.equals(h.owner()))) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return !isAlly() && super.removeWhenFarAway(d); }

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
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (t, l) -> enemyOfMe(t)));
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
        if (target != null && !enemyOfMe(target) && !(getLastHurtByMob() == target && !isAlly())) { setTarget(null); target = null; }
        if (isAlly() && !staying && target == null && !kind.flying) {
            var p = ownerPlayer();
            if (p != null && p.level() == level) {
                double d = distanceTo(p);
                if (d > 28) teleportTo(p.getX() + 1, p.getY(), p.getZ() + 1);
                else if (d > 6) getNavigation().moveTo(p, 1.1);
            }
        }
        if (kind.flying && isAlly() && target == null) {
            var p = ownerPlayer();
            if (p != null && p.level() == level && !staying) {
                Vec3 goal = p.position().add(0, 3, 0), d = goal.subtract(position());
                if (d.length() > 30) teleportTo(goal.x, goal.y, goal.z);
                setDeltaMovement(getDeltaMovement().scale(0.6).add((d.length() > 1 ? d.normalize().scale(kind.speed) : d).scale(0.4)));
            }
        } else if (kind.flying) {
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
        if (isAlly()) { var p = ownerPlayer(); if (p != null) p.sendSystemMessage(net.minecraft.network.chat.Component.literal(getName().getString() + " was destroyed.").withStyle(net.minecraft.ChatFormatting.DARK_RED)); return; }
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

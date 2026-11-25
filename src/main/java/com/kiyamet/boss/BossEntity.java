package com.kiyamet.boss;

import com.kiyamet.entity.KZombie;
import com.kiyamet.entity.ModEntities;
import com.kiyamet.entity.ZombieKind;
import com.kiyamet.robot.ModRobots;
import com.kiyamet.robot.RobotKind;
import com.kiyamet.world.Infection;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/** One class for all 15 bosses. Each uses the three abilities of its {@link BossKind}; below 50% HP it enters phase 2. */
public class BossEntity extends Monster implements GeoEntity {
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final BossKind kind;
    private final ServerBossEvent bar;
    private final int[] cooldowns;
    private boolean phase2;

    public BossEntity(EntityType<? extends Monster> type, Level level, BossKind kind) {
        super(type, level);
        this.kind = kind;
        this.cooldowns = new int[kind.abilities.length];
        for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = 60 + i * 50;
        this.bar = new ServerBossEvent(Component.literal(kind.title), kind.color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 200;
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BossEntity>("main", 4, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<BossEntity>("attack", 0, test -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
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
        bar.setProgress(getHealth() / getMaxHealth());
        if (!phase2 && getHealth() < getMaxHealth() / 2) {
            phase2 = true;
            bar.setName(Component.literal(kind.title + " - Enraged"));
            addEffect(new MobEffectInstance(MobEffects.SPEED, 999999, 0, false, false));
            addEffect(new MobEffectInstance(MobEffects.STRENGTH, 999999, 0, false, false));
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3f, 0.6f);
        }
        LivingEntity target = getTarget();
        for (int i = 0; i < cooldowns.length; i++) {
            Ability a = kind.abilities[i];
            if (a == Ability.SHADOW_CLOAK) { shadowCloak(level); continue; }
            if (cooldowns[i] > 0) { cooldowns[i] -= phase2 ? 2 : 1; continue; }
            if (target == null || !target.isAlive() || distanceTo(target) > 32) continue;
            if (use(level, a, target)) cooldowns[i] = a.cooldown;
        }
    }

    private List<Player> playersNear(ServerLevel level, double r) {
        return level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r), p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
    }

    private void burst(ServerLevel level, ParticleOptions p, int n, double spread) {
        level.sendParticles(p, getX(), getY() + getBbHeight() / 2, getZ(), n, spread, getBbHeight() / 3, spread, 0.05);
    }

    private void line(ServerLevel level, ParticleOptions p, Vec3 from, Vec3 to) {
        int n = (int) (from.distanceTo(to) * 3);
        for (int i = 0; i < n; i++) { Vec3 at = from.add(to.subtract(from).scale(i / (double) n)); level.sendParticles(p, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
    }

    private float dmg(float mult) { return (float) (kind.damage * mult * (phase2 ? 1.3 : 1.0)); }

    private DamageSource src(ServerLevel level) { return level.damageSources().mobAttack(this); }

    /** @return true if the ability fired (cooldown starts). */
    private boolean use(ServerLevel level, Ability a, LivingEntity target) {
        double dist = distanceTo(target);
        switch (a) {
            case INFECT_AURA -> {
                burst(level, ParticleTypes.SPORE_BLOSSOM_AIR, 120, 5);
                for (Player p : playersNear(level, 7)) {
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    if (p instanceof ServerPlayer sp) Infection.onZombieHit(sp, ZombieKind.TOXIC);
                }
            }
            case TOXIC_BURST -> {
                burst(level, ParticleTypes.ITEM_SLIME, 150, 4);
                for (Player p : playersNear(level, 6)) { p.hurtServer(level, src(level), dmg(0.6f)); p.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2)); p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0)); }
            }
            case SUMMON_DOCTORS -> summonZombies(level, ZombieKind.DOCTOR, 3);
            case SUMMON_CRAWLERS -> summonZombies(level, ZombieKind.CRAWLER, 4);
            case SUMMON_TOXIC -> summonZombies(level, ZombieKind.TOXIC, 3);
            case SUMMON_GUARDS -> summonZombies(level, ZombieKind.ARMORED, 3);
            case SUMMON_SOLDIERS -> { summonZombies(level, ZombieKind.ARMORED, 2); summonZombies(level, ZombieKind.POLICE, 2); }
            case SUMMON_ROBOTS -> {
                for (int i = 0; i < 3; i++) {
                    RobotKind rk = i == 2 ? RobotKind.SCOUT_DRONE : RobotKind.ROBOT_TROOPER;
                    Mob m = ModRobots.TYPES.get(rk).create(level, EntitySpawnReason.MOB_SUMMONED);
                    if (m != null) { m.setPos(getX() + random.nextInt(7) - 3, getY() + (rk == RobotKind.SCOUT_DRONE ? 3 : 0), getZ() + random.nextInt(7) - 3); m.setTarget(target); level.addFreshEntity(m); }
                }
                burst(level, ParticleTypes.ELECTRIC_SPARK, 80, 2);
            }
            case CHARGE -> {
                if (dist < 4 || dist > 18) return false;
                Vec3 d = target.position().subtract(position()).normalize();
                setDeltaMovement(d.x * 2.2, 0.3, d.z * 2.2);
                level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1f);
            }
            case BLEED -> {
                if (dist > 5) return false;
                target.hurtServer(level, src(level), dmg(0.8f));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
            }
            case SLAM -> {
                if (dist > 7) return false;
                slam(level, 6, 1.0f);
            }
            case LEAP_SLAM -> {
                if (!onGround() || dist < 5) return false;
                Vec3 d = target.position().subtract(position());
                setDeltaMovement(d.x * 0.15, 1.0, d.z * 0.15);
                slamPending = 12;
            }
            case REGEN -> {
                if (getHealth() > getMaxHealth() * 0.7f) return false;
                heal(getMaxHealth() * 0.08f);
                burst(level, ParticleTypes.HEART, 20, 1.5);
            }
            case SOUL_DRAIN -> {
                for (Player p : playersNear(level, 10)) { p.hurtServer(level, level.damageSources().magic(), dmg(0.4f)); heal(dmg(0.4f)); line(level, ParticleTypes.SOUL, p.getEyePosition(), getEyePosition()); }
            }
            case DARKNESS -> {
                for (Player p : playersNear(level, 20)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 1.2f);
            }
            case SPIT, FIREBALL, BEAM -> {
                if (!hasLineOfSight(target)) return false;
                ParticleOptions p = a == Ability.SPIT ? ParticleTypes.SPIT : a == Ability.FIREBALL ? ParticleTypes.FLAME : ParticleTypes.END_ROD;
                line(level, p, getEyePosition(), target.getEyePosition());
                target.hurtServer(level, a == Ability.BEAM ? level.damageSources().magic() : src(level), dmg(a == Ability.BEAM ? 0.7f : 0.5f));
                if (a == Ability.FIREBALL) target.igniteForSeconds(5);
                if (a == Ability.SPIT) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
            case FIRE_RING -> {
                burst(level, ParticleTypes.FLAME, 300, 6);
                for (Player p : playersNear(level, 7)) { p.igniteForSeconds(6); p.hurtServer(level, level.damageSources().inFire(), dmg(0.5f)); }
                level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
            }
            case TIME_SLOW -> {
                for (Player p : playersNear(level, 16)) { p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3)); p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 2)); }
                burst(level, ParticleTypes.ENCHANT, 300, 8);
                level.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3f, 0.5f);
            }
            case TELEPORT_BEHIND -> {
                if (dist < 4) return false;
                Vec3 back = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
                burst(level, ParticleTypes.PORTAL, 60, 1);
                teleportTo(back.x, target.getY(), back.z);
                burst(level, ParticleTypes.PORTAL, 60, 1);
                level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2f, 0.7f);
            }
            case SHOCKWAVE -> {
                level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1, getZ(), 12, 4, 0.5, 4, 0);
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 1f);
                for (Player p : playersNear(level, 10)) { p.hurtServer(level, level.damageSources().sonicBoom(this), dmg(0.6f)); push(p, 1.6, 0.5); }
            }
            case LEVITATE -> {
                for (Player p : playersNear(level, 14)) p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1));
            }
            case LIGHTNING -> {
                for (Player p : playersNear(level, 24)) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) { bolt.setPos(p.getX(), p.getY(), p.getZ()); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
                    p.hurtServer(level, level.damageSources().lightningBolt(), dmg(0.6f));
                }
            }
            case CURSE -> {
                for (Player p : playersNear(level, 12)) { p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1)); p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1)); p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0)); }
                burst(level, ParticleTypes.WITCH, 120, 5);
                level.playSound(null, blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 2f, 0.6f);
            }
            case ARROW_BARRAGE -> {
                for (Player p : playersNear(level, 24)) {
                    if (!hasLineOfSight(p)) continue;
                    for (int i = 0; i < 4; i++) line(level, ParticleTypes.CRIT, getEyePosition().add(random.nextGaussian(), 0, random.nextGaussian()), p.getEyePosition());
                    p.hurtServer(level, src(level), dmg(0.45f));
                }
                level.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 2f, 0.8f);
            }
            case PULL -> {
                if (dist < 4 || dist > 20) return false;
                Vec3 d = position().subtract(target.position()).normalize();
                target.setDeltaMovement(d.x * 1.8, 0.5, d.z * 1.8);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 2));
                line(level, ParticleTypes.SQUID_INK, getEyePosition(), target.getEyePosition());
            }
            case FREEZE -> {
                for (Player p : playersNear(level, 10)) { p.setTicksFrozen(p.getTicksRequiredToFreeze() + 120); p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2)); p.hurtServer(level, level.damageSources().freeze(), dmg(0.4f)); }
                burst(level, ParticleTypes.SNOWFLAKE, 300, 6);
                level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
            }
            default -> { return false; }
        }
        return true;
    }

    private int slamPending;

    @Override
    public void aiStep() {
        super.aiStep();
        if (slamPending > 0 && level() instanceof ServerLevel level) {
            slamPending--;
            if (onGround() && slamPending < 10) { slamPending = 0; slam(level, 8, 1.3f); }
        }
    }

    private void slam(ServerLevel level, double radius, float mult) {
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 8, radius / 2, 0.1, radius / 2, 0);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.6f);
        for (Player p : playersNear(level, radius)) { p.hurtServer(level, src(level), dmg(mult)); push(p, 1.2, 0.6); }
    }

    private void push(LivingEntity e, double strength, double up) {
        Vec3 d = e.position().subtract(position()).multiply(1, 0, 1).normalize();
        e.setDeltaMovement(d.x * strength, up, d.z * strength);
        e.hurtMarked = true;
    }

    private void summonZombies(ServerLevel level, ZombieKind zk, int n) {
        for (int i = 0; i < n; i++) {
            KZombie z = ModEntities.ZOMBIES.get(zk).create(level, EntitySpawnReason.MOB_SUMMONED);
            if (z == null) continue;
            z.setPos(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4);
            z.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            z.setTarget(getTarget());
            level.addFreshEntity(z);
        }
        burst(level, ParticleTypes.LARGE_SMOKE, 60, 3);
        level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 2f, 0.5f);
    }

    private void shadowCloak(ServerLevel level) {
        if (tickCount % 20 != 0) return;
        if (level.getMaxLocalRawBrightness(blockPosition()) < 8) addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        else removeEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) return;
        for (String part : kind.drops.split(",")) {
            String[] kv = part.split(":");
            Item item = BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id(kv[0]));
            int n = Integer.parseInt(kv[1]);
            level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1, getZ(), new ItemStack(item, n)));
        }
        BossRewards.onBossKilled(level, this);
        bar.removeAllPlayers();
    }
}

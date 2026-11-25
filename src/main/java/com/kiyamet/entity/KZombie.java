package com.kiyamet.entity;

import com.kiyamet.skill.SkillManager;
import com.kiyamet.world.Difficulty;
import com.kiyamet.world.Infection;
import com.kiyamet.world.NoiseSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** One class for all 14 zombie kinds; behaviour switches on {@link #kind}. Never burns in daylight. */
public class KZombie extends Zombie implements GeoEntity {
    private static final Identifier SCALE_MOD = com.kiyamet.Kiyamet.id("power_scale");
    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");
    public static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack.swing");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final ZombieKind kind;
    private int screamCooldown, leapCooldown, noiseCheck;

    public KZombie(EntityType<? extends Zombie> type, Level level, ZombieKind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = 5 + (int) (kind.health / 6);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KZombie>("main", 4, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<KZombie>("attack", 0, test -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    protected boolean isSunSensitive() { return false; }

    @Override
    public boolean isBaby() { return false; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        double mult = Difficulty.multiplierNear(level.getLevel(), blockPosition());
        apply(Attributes.MAX_HEALTH, mult);
        apply(Attributes.ATTACK_DAMAGE, mult);
        setHealth(getMaxHealth());
        setCanBreakDoors(true);
        return result;
    }

    private void apply(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double mult) {
        AttributeInstance inst = getAttribute(attr);
        if (inst == null || mult <= 1.0) return;
        inst.removeModifier(SCALE_MOD);
        inst.addPermanentModifier(new AttributeModifier(SCALE_MOD, mult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) triggerAnim("attack", "attack");
        if (hit && target instanceof LivingEntity living) {
            switch (kind) {
                case TOXIC -> living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case LISTENER -> living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                case MUTANT, TANK -> living.knockback(1.2, getX() - living.getX(), getZ() - living.getZ());
                default -> {}
            }
            if (target instanceof ServerPlayer p) Infection.onZombieHit(p, kind);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        com.kiyamet.defense.DefenseRules.tickZombie(server, this);
        LivingEntity target = getTarget();
        if (target != null && com.kiyamet.twd.Camouflage.hides(target, this)) { setTarget(null); target = null; }
        if (screamCooldown > 0) screamCooldown--;
        if (leapCooldown > 0) leapCooldown--;

        if (kind == ZombieKind.SCREAMER && target instanceof Player && screamCooldown == 0 && distanceTo(target) < 16) {
            screamCooldown = 400;
            playSound(net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, 2.5f, 0.5f);
            NoiseSystem.make(server, blockPosition(), 48, 3);
            for (KZombie z : server.getEntitiesOfClass(KZombie.class, getBoundingBox().inflate(40))) if (z != this) z.setTarget(target);
        }
        if (kind == ZombieKind.HUNTER && target != null && leapCooldown == 0 && onGround() && distanceTo(target) > 3 && distanceTo(target) < 9) {
            leapCooldown = 80;
            Vec3 d = target.position().subtract(position()).normalize();
            setDeltaMovement(d.x * 1.1, 0.55, d.z * 1.1);
        }
        if (kind == ZombieKind.ALPHA && tickCount % 40 == 0) {
            for (KZombie z : server.getEntitiesOfClass(KZombie.class, getBoundingBox().inflate(12)))
                if (z != this) z.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0));
        }
        if (target == null && ++noiseCheck >= 20) {
            noiseCheck = 0;
            BlockPos heard = NoiseSystem.loudestNear(server, blockPosition(), kind == ZombieKind.LISTENER ? 2.0 : 1.0);
            if (heard != null) getNavigation().moveTo(heard.getX() + 0.5, heard.getY(), heard.getZ() + 0.5, 1.1);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, amount * com.kiyamet.twd.Headshot.multiplier(level, this, source));
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl && random.nextFloat() < 0.35f)
            spawnAtLocation(sl, new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(com.kiyamet.Kiyamet.id("walker_guts"))));
        if (kind == ZombieKind.EXPLODER && level() instanceof ServerLevel server)
            server.explode(this, getX(), getY(), getZ(), 2.5f, Level.ExplosionInteraction.MOB);
        super.die(source);
    }

    /** Apocalypse zombies spawn day and night on any solid ground, never in water. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return true;
        BlockPos below = blockPosition().below();
        if (!level.getBlockState(below).isSolid() || !level.getFluidState(blockPosition()).isEmpty()) return false;
        if (level instanceof ServerLevel sl && com.kiyamet.world.ModStructures.inSanctuary(sl, blockPosition())) return false;
        int power = level instanceof ServerLevel s ? Difficulty.nearestPower(s, blockPosition()) : 0;
        if (power < kind.minPower) return false;
        boolean day = level instanceof ServerLevel s && s.isBrightOutside();
        return !day || random.nextFloat() < 0.35f;
    }

    public static boolean noisy(RandomSource r) { return r.nextFloat() < 0.5f; }
}

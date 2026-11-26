package com.kiyamet.item;

import com.kiyamet.Kiyamet;
import com.kiyamet.world.NoiseSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Thrown bombs (1.6): thrown far like a splash potion, go off where they land. */
public class BombProjectile extends ThrowableItemProjectile {
    private static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, Kiyamet.id("thrown_bomb"));
    public static final EntityType<BombProjectile> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
            EntityType.Builder.<BombProjectile>of(BombProjectile::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(10).build(KEY));
    public static final String[] NEW = {"frag_grenade", "molotov", "smoke_grenade", "flashbang"};

    public BombProjectile(EntityType<? extends BombProjectile> t, Level level) { super(t, level); }
    public BombProjectile(Level level, LivingEntity owner, ItemStack stack) { super(TYPE, owner, level, stack); }

    @Override protected Item getDefaultItem() { return com.kiyamet.registry.ModItems.get("frag_grenade"); }

    /** Throw one: far, in an arc, like a splash potion. */
    public static void toss(ServerLevel level, Player p, ItemStack stack) {
        BombProjectile b = new BombProjectile(level, p, stack.copyWithCount(1));
        b.shootFromRotation(p, p.getXRot(), p.getYRot(), -12f, 1.5f, 0.6f);
        level.addFreshEntity(b);
        level.playSound(null, p.blockPosition(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    public static class ThrownItem extends Item {
        public ThrownItem(Properties p) { super(p); }
        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (level instanceof ServerLevel sl) {
                toss(sl, player, s);
                player.getCooldowns().addCooldown(s, 20);
                if (!player.isCreative()) s.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;
        Vec3 c = position();
        String id = BuiltInRegistries.ITEM.getKey(getItem().getItem()).getPath();
        LivingEntity owner = getOwner() instanceof LivingEntity l ? l : null;
        AABB box = new AABB(c, c);
        switch (id) {
            case "emp_bomb" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(16), e -> e != owner)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 5)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 2));
                    if (e instanceof com.kiyamet.robot.RobotEntity) e.hurtServer(level, level.damageSources().lightningBolt(), 30);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1, c.z, 300, 6, 1.5, 6, 0.5);
                level.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2f, 0.6f);
            }
            case "black_hole_bomb" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(14), e -> e != owner)) {
                    Vec3 pull = c.subtract(e.position()).normalize().scale(1.8);
                    e.setDeltaMovement(pull.x, pull.y + 0.3, pull.z); e.hurtMarked = true;
                    e.hurtServer(level, owner == null ? level.damageSources().magic() : level.damageSources().indirectMagic(this, owner), 20);
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 500, 2, 2, 2, 0.3);
                level.playSound(null, BlockPos.containing(c), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8f, 1.6f);
            }
            case "frag_grenade" -> {
                level.explode(this, c.x, c.y, c.z, 3.2f, Level.ExplosionInteraction.NONE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(5), e -> e != owner)) e.hurtServer(level, level.damageSources().explosion(this, owner), 14);
                NoiseSystem.make(level, BlockPos.containing(c), 48, 10);
            }
            case "molotov" -> {
                BlockPos at = BlockPos.containing(c);
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dz * dz > 10 || random.nextInt(4) == 0) continue;
                    for (int dy = 2; dy >= -2; dy--) {
                        BlockPos p = at.offset(dx, dy, dz);
                        if (level.getBlockState(p).isAir() && !level.getBlockState(p.below()).isAir()) { level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p)); break; }
                    }
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(4), e -> e != owner)) e.igniteForSeconds(10);
                level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 150, 2, 0.5, 2, 0.05);
                level.playSound(null, at, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
                level.playSound(null, at, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.8f);
                NoiseSystem.make(level, at, 24, 6);
            }
            case "smoke_grenade" -> {
                for (int i = 0; i < 6; i++) level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, c.x, c.y + 0.5, c.z, 40, 3, 1.5, 3, 0.02);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(9), e -> e != owner)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
                    if (e instanceof Mob m && m.getTarget() instanceof Player) m.setTarget(null);
                }
                level.playSound(null, BlockPos.containing(c), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.6f);
            }
            case "flashbang" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box.inflate(10), e -> e != owner)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0)); e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
                    if (e instanceof Mob m) m.setTarget(null);
                }
                level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 2, 0.2, 0.2, 0.2, 0);
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 80, 1, 1, 1, 0.3);
                level.playSound(null, BlockPos.containing(c), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 3f, 1.4f);
                NoiseSystem.make(level, BlockPos.containing(c), 40, 8);
            }
            default -> {}
        }
        discard();
    }

    public static void init() {
        for (String s : NEW) com.kiyamet.registry.ModItems.custom(s, p -> new ThrownItem(p.stacksTo(16)));
    }
}

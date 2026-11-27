package com.kiyamet.element;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** What each element does while its power is on - and how it is switched off cleanly. */
public final class ElementPowers {
    static final Map<UUID, Element> ON = new HashMap<>();
    /** whoever froze / sped / slowed time last owns the clock */
    static UUID clockOwner;

    public static void init() {
        // metal: no damage at all
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> !(e instanceof ServerPlayer p && ON.get(p.getUUID()) == Element.METAL));
        ServerPlayConnectionEvents.DISCONNECT.register((h, server) -> { Element e = ON.remove(h.player.getUUID()); if (e != null) stop(h.player, e); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // safety: a power whose holder vanished gives the clock back
            if (clockOwner != null && server.getPlayerList().getPlayer(clockOwner) == null) resetClock(server);
        });
    }

    static Vec3 look(ServerPlayer p, double range, boolean entities) {
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 hit = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();
        if (entities) {
            EntityHitResult eh = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p.level(), p, eye, hit, p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1), e -> e instanceof LivingEntity && e != p, 0.3f);
            if (eh != null) return eh.getLocation();
        }
        return hit;
    }

    static boolean friend(ServerPlayer p, Entity e) {
        if (e == p || e instanceof Player) return true;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return true;
        if (e instanceof com.kiyamet.npc.HumanEntity h && h.isOwnedBy(p)) return true;
        return false;
    }

    static void resetClock(MinecraftServer s) {
        s.tickRateManager().setFrozen(false);
        s.tickRateManager().setTickRate(20f);
        clockOwner = null;
    }

    public static void start(ServerPlayer p, Element e) {
        Element old = ON.put(p.getUUID(), e);
        if (old != null && old != e) stop(p, old);
        ServerLevel l = p.level();
        MinecraftServer s = l.getServer();
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.6f);
        burst(l, p, e, 40);
        switch (e) {
            case SPIRIT -> { p.getAbilities().mayfly = true; p.getAbilities().flying = true; p.getAbilities().setFlyingSpeed(0.2f); p.onUpdateAbilities(); }
            case AIR -> { p.getAbilities().mayfly = true; p.getAbilities().setFlyingSpeed(0.05f); p.onUpdateAbilities(); p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 4, false, false)); }
            case ICE -> { s.tickRateManager().setFrozen(true); clockOwner = p.getUUID(); }
            case THUNDER -> { s.tickRateManager().setTickRate(10f); clockOwner = p.getUUID(); }
            case WIND -> { s.tickRateManager().setTickRate(40f); clockOwner = p.getUUID(); }
            default -> {}
        }
    }

    public static void stop(ServerPlayer p, Element e) {
        ON.remove(p.getUUID());
        ServerLevel l = p.level();
        switch (e) {
            case SPIRIT, AIR -> {
                boolean keep = p.isCreative() || p.isSpectator();
                p.getAbilities().setFlyingSpeed(0.05f);
                if (!keep) { p.getAbilities().mayfly = false; p.getAbilities().flying = false; p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false)); }
                p.onUpdateAbilities();
            }
            case ICE, THUNDER, WIND -> { if (p.getUUID().equals(clockOwner)) resetClock(l.getServer()); }
            case SHADOW -> p.removeEffect(MobEffects.INVISIBILITY);
            default -> {}
        }
        if (e == Element.THUNDER) { p.removeEffect(MobEffects.SPEED); p.removeEffect(MobEffects.HASTE); }
        if (e == Element.WATER) { p.removeEffect(MobEffects.JUMP_BOOST); p.removeEffect(MobEffects.DOLPHINS_GRACE); }
        l.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
    }

    static void burst(ServerLevel l, ServerPlayer p, Element e, int n) {
        int c = e.color;
        l.sendParticles(new DustParticleOptions(c, 1.6f), p.getX(), p.getY() + 1, p.getZ(), n, 0.6, 0.9, 0.6, 0.02);
    }

    static MobEffectInstance fx(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> m, int ticks, int amp) { return new MobEffectInstance(m, ticks, amp, false, false, true); }

    public static void tick(ServerPlayer p, Element e, ItemStack weapon) {
        ServerLevel l = p.level();
        int t = p.tickCount;
        if (!ON.containsKey(p.getUUID())) ON.put(p.getUUID(), e);
        if (t % 100 == 0) com.kiyamet.skill.SkillManager.addXp(p, switch (e) {            // using a power trains the body
            case SPIRIT, AIR -> com.kiyamet.skill.Skill.ACROBATICS; case SHADOW, SMOKE -> com.kiyamet.skill.Skill.STEALTH; case WATER -> com.kiyamet.skill.Skill.SWIMMING;
            case THUNDER, WIND -> com.kiyamet.skill.Skill.ATHLETICS; case METAL -> com.kiyamet.skill.Skill.DEFENSE; case EARTH -> com.kiyamet.skill.Skill.MINING;
            default -> com.kiyamet.skill.Skill.ENCHANTING; }, 15);
        if (t % 4 == 0) l.sendParticles(new DustParticleOptions(e.color, 1.1f), p.getX(), p.getY() + 0.9, p.getZ(), 3, 0.45, 0.7, 0.45, 0.01);
        switch (e) {
            case FLAME -> {                    // a cone of flame in front of you
                if (t % 2 != 0) break;
                Vec3 dir = p.getLookAngle(), eye = p.getEyePosition().add(0, -0.2, 0);
                for (int i = 1; i <= 9; i++) {
                    Vec3 at = eye.add(dir.scale(i * 0.9));
                    l.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.12 * i / 3, 0.12 * i / 3, 0.12 * i / 3, 0.02);
                }
                if (t % 6 == 0) l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4f, 1.6f);
                for (LivingEntity v : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().expandTowards(dir.scale(9)).inflate(1.5), v -> !friend(p, v))) {
                    Vec3 to = v.position().add(0, v.getBbHeight() / 2, 0).subtract(eye);
                    if (to.length() < 10 && to.normalize().dot(dir) > 0.8) { v.igniteForSeconds(5); v.hurtServer(l, l.damageSources().onFire(), 3f); }
                }
            }
            case FIRE -> {                     // fireballs
                if (t % 16 != 0) break;
                Vec3 dir = p.getLookAngle();
                LargeFireball f = new LargeFireball(l, p, dir, 2);
                f.setPos(p.getX() + dir.x * 1.5, p.getEyeY() + dir.y * 1.5 - 0.2, p.getZ() + dir.z * 1.5);
                l.addFreshEntity(f);
                l.playSound(null, p.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            case LIGHTNING -> {                // lightning where you look
                if (t % 20 != 0) break;
                Vec3 at = look(p, 48, true);
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(l, EntitySpawnReason.TRIGGERED);
                if (b != null) { b.snapTo(at.x, at.y, at.z, 0, 0); b.setCause(p); l.addFreshEntity(b); }
            }
            case ELECTRIC -> {                 // exploding lightning
                if (t % 24 != 0) break;
                Vec3 at = look(p, 48, true);
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(l, EntitySpawnReason.TRIGGERED);
                if (b != null) { b.snapTo(at.x, at.y, at.z, 0, 0); b.setCause(p); l.addFreshEntity(b); }
                if (at.distanceTo(p.position()) > 4) l.explode(p, at.x, at.y, at.z, 3.2f, Level.ExplosionInteraction.TNT);
            }
            case EARTH -> {                    // shatter a 6x6 area
                if (t % 20 != 0) break;
                Vec3 at = look(p, 32, false);
                if (at.distanceTo(p.getEyePosition()) > 31) break;
                BlockPos c = BlockPos.containing(at.add(p.getLookAngle().scale(0.5)));
                int broke = 0;
                for (BlockPos q : BlockPos.betweenClosed(c.offset(-3, -3, -3), c.offset(2, 2, 2))) {
                    BlockState s = l.getBlockState(q);
                    if (s.isAir() || s.getDestroySpeed(l, q) < 0 || s.getDestroySpeed(l, q) > 50 || q.closerThan(p.blockPosition(), 2)) continue;
                    l.destroyBlock(q, p.getRandom().nextInt(4) == 0, p); broke++;
                }
                if (broke > 0) { l.playSound(null, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 0.6f); l.sendParticles(ParticleTypes.EXPLOSION, c.getX(), c.getY(), c.getZ(), 4, 2, 2, 2, 0); }
                for (LivingEntity v : l.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(3), v -> !friend(p, v))) v.hurtServer(l, l.damageSources().playerAttack(p), 10f);
            }
            case SHADOW -> {
                p.addEffect(fx(MobEffects.INVISIBILITY, 40, 0));
                if (t % 10 == 0) for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) m.setTarget(null);
            }
            case SMOKE -> {                    // choking cloud
                l.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 6, 3.5, 1.2, 3.5, 0.01);
                l.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 1, 3, 0.5, 3, 0.01);
                if (t % 10 == 0) for (LivingEntity v : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8), v -> !friend(p, v))) {
                    v.setAirSupply(Math.max(-20, v.getAirSupply() - 60));
                    v.hurtServer(l, l.damageSources().drown(), 4f);
                    v.addEffect(fx(MobEffects.BLINDNESS, 40, 0));
                }
            }
            case SPIRIT -> {
                if (!p.getAbilities().mayfly || p.getAbilities().getFlyingSpeed() != 0.2f) { p.getAbilities().mayfly = true; p.getAbilities().setFlyingSpeed(0.2f); p.onUpdateAbilities(); }
                if (t % 3 == 0) l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.3, p.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            }
            case AIR -> {
                if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
                if (t % 3 == 0) l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 2, 0.3, 0.05, 0.3, 0.01);
            }
            case THUNDER -> {
                p.addEffect(fx(MobEffects.SPEED, 40, 7)); p.addEffect(fx(MobEffects.HASTE, 40, 3));
                if (t % 2 == 0) l.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 4, 0.3, 0.5, 0.3, 0.1);
            }
            case ICE -> {
                if (t % 5 == 0) l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 10, 6, 3, 6, 0);
                if (!l.getServer().tickRateManager().isFrozen()) { l.getServer().tickRateManager().setFrozen(true); clockOwner = p.getUUID(); }
            }
            case WATER -> {
                p.addEffect(fx(MobEffects.JUMP_BOOST, 40, 6)); p.addEffect(fx(MobEffects.DOLPHINS_GRACE, 40, 2)); p.addEffect(fx(MobEffects.WATER_BREATHING, 40, 0));
                p.fallDistance = 0;
                if (p.isInWater()) { p.addEffect(fx(MobEffects.SPEED, 40, 3)); }
            }
            case WIND -> {
                if (t % 3 == 0) l.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 1, p.getZ(), 1, 1.5, 0.5, 1.5, 0);
            }
            case METAL -> {
                if (t % 6 == 0) l.sendParticles(ParticleTypes.WAX_OFF, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.8, 0.4, 0.05);
                p.clearFire();
            }
        }
    }

    /** every hit carries the element */
    public static void onHit(Element e, LivingEntity v, LivingEntity a) {
        if (!(v.level() instanceof ServerLevel l)) return;
        switch (e) {
            case FLAME, FIRE -> v.igniteForSeconds(6);
            case LIGHTNING, THUNDER, ELECTRIC -> { v.addEffect(fx(MobEffects.SLOWNESS, 40, 3)); l.sendParticles(ParticleTypes.ELECTRIC_SPARK, v.getX(), v.getY() + 1, v.getZ(), 12, 0.3, 0.5, 0.3, 0.3); }
            case EARTH -> { v.knockback(1.4, a.getX() - v.getX(), a.getZ() - v.getZ()); v.addEffect(fx(MobEffects.SLOWNESS, 40, 1)); }
            case SHADOW -> { v.addEffect(fx(MobEffects.DARKNESS, 80, 0)); v.addEffect(fx(MobEffects.WITHER, 60, 1)); }
            case SMOKE -> v.addEffect(fx(MobEffects.BLINDNESS, 60, 0));
            case SPIRIT -> { a.heal(2f); v.addEffect(fx(MobEffects.GLOWING, 100, 0)); }
            case ICE -> { v.setTicksFrozen(200); v.addEffect(fx(MobEffects.SLOWNESS, 80, 4)); }
            case WATER -> { v.addEffect(fx(MobEffects.MINING_FATIGUE, 80, 1)); v.clearFire(); }
            case AIR -> v.setDeltaMovement(v.getDeltaMovement().add(0, 0.8, 0));
            case WIND -> v.knockback(2.2, a.getX() - v.getX(), a.getZ() - v.getZ());
            case METAL -> v.addEffect(fx(MobEffects.WEAKNESS, 80, 1));
        }
    }

    private ElementPowers() {}
}

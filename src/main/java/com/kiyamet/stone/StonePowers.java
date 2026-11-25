package com.kiyamet.stone;

import com.kiyamet.skill.SkillManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Passive and active effects of the 10 power stones. */
public final class StonePowers {
    private static final Identifier EARTH_KB = com.kiyamet.Kiyamet.id("earth_stone");
    private static final Map<UUID, Long> SPIRITS = new HashMap<>();

    /** Stones whose passive is active: socketed in a gauntlet anywhere in the inventory, or a loose stone in the offhand. */
    public static Set<StoneKind> active(Player p) {
        Set<StoneKind> set = EnumSet.noneOf(StoneKind.class);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            GauntletData d = s.get(GauntletData.TYPE);
            if (d != null) for (String id : d.stones()) { StoneKind k = StoneKind.byId(id); if (k != null) set.add(k); }
        }
        if (p.getOffhandItem().getItem() instanceof StoneItem si) set.add(si.kind);
        return set;
    }

    /** Every second. */
    public static void tickPassives(ServerPlayer p) {
        Set<StoneKind> s = active(p);
        if (s.isEmpty() && !p.getAttributes().hasModifier(Attributes.KNOCKBACK_RESISTANCE, EARTH_KB)) return;
        if (s.contains(StoneKind.FIRE)) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
        if (s.contains(StoneKind.STORM)) p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
        if (s.contains(StoneKind.NATURE) && p.tickCount % 100 < 20) p.heal(1f);
        if (s.contains(StoneKind.SHADOW) && p.level().isDarkOutside()) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(EARTH_KB);
            if (s.contains(StoneKind.EARTH)) kb.addTransientModifier(new AttributeModifier(EARTH_KB, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Called when a player damages a living entity. */
    public static void onPlayerHit(ServerPlayer p, LivingEntity target, float damage) {
        Set<StoneKind> s = active(p);
        if (s.contains(StoneKind.FROST)) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        if (s.contains(StoneKind.BLOOD)) p.heal(damage * 0.15f);
    }

    public static int cooldown(Player p, StoneKind k) {
        return active(p).contains(StoneKind.TIME) ? (int) (k.cooldown * 0.9) : k.cooldown;
    }

    private static List<LivingEntity> enemies(ServerLevel level, Player p, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(r)))
            if (e != p && e instanceof Enemy && e.isAlive()) out.add(e);
        return out;
    }

    private static void burst(ServerLevel level, Player p, SimpleParticleType type, int n, double spread) {
        level.sendParticles(type, p.getX(), p.getY() + 1, p.getZ(), n, spread, 0.6, spread, 0.05);
    }

    private static void hurt(ServerLevel level, Player p, LivingEntity e, float amount) {
        e.hurtServer(level, level.damageSources().playerAttack(p), amount);
    }

    /** Active ability. Returns false if nothing happened. */
    public static boolean activate(ServerPlayer p, StoneKind k) {
        ServerLevel level = p.level();
        float power = 1f + SkillManager.get(p).totalPower() / 10000f;
        switch (k) {
            case FIRE -> { for (LivingEntity e : enemies(level, p, 6)) { e.igniteForSeconds(6); hurt(level, p, e, 6 * power); } burst(level, p, ParticleTypes.FLAME, 120, 4); }
            case FROST -> { for (LivingEntity e : enemies(level, p, 8)) { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 4)); e.setTicksFrozen(200); hurt(level, p, e, 3 * power); } burst(level, p, ParticleTypes.SNOWFLAKE, 150, 5); }
            case STORM -> {
                Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
                LivingEntity best = null; double bd = 40;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32))) {
                    if (e == p) continue;
                    Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
                    double along = to.dot(look);
                    if (along > 0 && to.subtract(look.scale(along)).length() < 2.0 && along < bd) { bd = along; best = e; }
                }
                Vec3 at = best != null ? best.position() : eye.add(look.scale(20));
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt == null) return false;
                bolt.setPos(at.x, at.y, at.z);
                bolt.setCause(p);
                level.addFreshEntity(bolt);
                if (best != null) hurt(level, p, best, 10 * power);
            }
            case NATURE -> { p.heal(8 * power); for (LivingEntity e : enemies(level, p, 7)) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 6)); burst(level, p, ParticleTypes.HAPPY_VILLAGER, 80, 4); }
            case BLOOD -> { float total = 0; for (LivingEntity e : enemies(level, p, 6)) { hurt(level, p, e, 5 * power); total += 5 * power; } p.heal(total * 0.3f); burst(level, p, ParticleTypes.DAMAGE_INDICATOR, 60, 3); }
            case SHADOW -> { p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0)); for (LivingEntity e : enemies(level, p, 24)) if (e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == p) m.setTarget(null); burst(level, p, ParticleTypes.SMOKE, 80, 1); }
            case SOUL -> {
                for (int i = 0; i < 2; i++) {
                    IronGolem g = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
                    if (g == null) continue;
                    g.setPos(p.getX() + (i == 0 ? 2 : -2), p.getY(), p.getZ());
                    g.setPlayerCreated(true);
                    level.addFreshEntity(g);
                    SPIRITS.put(g.getUUID(), level.getGameTime() + 1200);
                }
                burst(level, p, ParticleTypes.SOUL, 80, 2);
            }
            case TIME -> { for (LivingEntity e : enemies(level, p, 16)) { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 9)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2)); } p.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 2)); burst(level, p, ParticleTypes.ENCHANT, 150, 6); }
            case VOID -> {
                Vec3 c = p.getEyePosition().add(p.getViewVector(1f).scale(8));
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(10))) {
                    if (e == p || !(e instanceof Enemy)) continue;
                    Vec3 pull = c.subtract(e.position()).normalize().scale(1.4);
                    e.setDeltaMovement(pull.x, pull.y + 0.2, pull.z); e.hurtMarked = true;
                    hurt(level, p, e, 8 * power);
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 200, 1.5, 1.5, 1.5, 0.2);
            }
            case EARTH -> { for (LivingEntity e : enemies(level, p, 7)) { e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0)); e.hurtMarked = true; hurt(level, p, e, 7 * power); } burst(level, p, ParticleTypes.CAMPFIRE_COSY_SMOKE, 60, 4); }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }

    /** Spirit golems vanish after one minute. */
    public static void tickSpirits(ServerLevel level) {
        if (SPIRITS.isEmpty()) return;
        long now = level.getGameTime();
        SPIRITS.entrySet().removeIf(en -> {
            if (en.getValue() > now) return false;
            Entity e = level.getEntity(en.getKey());
            if (e != null) e.discard();
            return e != null;
        });
    }

    private StonePowers() {}
}

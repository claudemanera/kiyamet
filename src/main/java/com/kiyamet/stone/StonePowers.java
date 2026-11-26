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
        if (s.contains(StoneKind.FIRE)) { p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false)); p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, true, false)); }
        if (s.contains(StoneKind.STORM)) { p.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1, true, false)); p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 80, 1, true, false)); }
        if (s.contains(StoneKind.NATURE)) { p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false)); if (p.tickCount % 60 < 20) p.getFoodData().eat(1, 0.3f); }
        if (s.contains(StoneKind.SHADOW)) { p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false)); if (p.isShiftKeyDown()) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false)); }
        if (s.contains(StoneKind.EARTH)) p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, true, false));
        if (s.contains(StoneKind.TIME)) p.addEffect(new MobEffectInstance(MobEffects.HASTE, 80, 1, true, false));
        if (s.contains(StoneKind.SOUL) && p.tickCount % 200 < 20) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1, true, false));
        if (s.contains(StoneKind.VOID)) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
        if (s.contains(StoneKind.BLOOD)) p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 0, true, false));
        AttributeInstance kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(EARTH_KB);
            if (s.contains(StoneKind.EARTH)) kb.addTransientModifier(new AttributeModifier(EARTH_KB, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Called when a player damages a living entity. */
    public static void onPlayerHit(ServerPlayer p, LivingEntity target, float damage) {
        Set<StoneKind> s = active(p);
        if (s.contains(StoneKind.FROST)) { target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2)); target.setTicksFrozen(Math.min(300, target.getTicksFrozen() + 60)); }
        if (s.contains(StoneKind.BLOOD)) p.heal(damage * 0.25f);
        if (s.contains(StoneKind.FIRE)) target.igniteForSeconds(4);
        if (s.contains(StoneKind.STORM) && p.getRandom().nextFloat() < 0.12f && p.level() instanceof ServerLevel sl) {
            LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl, EntitySpawnReason.TRIGGERED);
            if (b != null) { b.setPos(target.getX(), target.getY(), target.getZ()); b.setCause(p); sl.addFreshEntity(b); }
        }
    }

    public static int cooldown(Player p, StoneKind k) {
        return (int) (k.cooldown * 0.5 * (active(p).contains(StoneKind.TIME) ? 0.75 : 1));
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
        float power = 2f + SkillManager.get(p).totalPower() / 2500f;
        switch (k) {
            case FIRE -> {   // inferno: big fire ring, burning for 12 s, fire resistance + strength for you
                for (LivingEntity e : enemies(level, p, 12)) { e.igniteForSeconds(12); hurt(level, p, e, 9 * power); e.knockback(0.8, p.getX() - e.getX(), p.getZ() - e.getZ()); }
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 1)); burst(level, p, ParticleTypes.FLAME, 400, 8); burst(level, p, ParticleTypes.LAVA, 60, 5);
            }
            case FROST -> {  // blizzard: everything around frozen solid for 10 s
                for (LivingEntity e : enemies(level, p, 14)) { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 6)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2)); e.setTicksFrozen(400); hurt(level, p, e, 6 * power); }
                burst(level, p, ParticleTypes.SNOWFLAKE, 500, 10);
            }
            case STORM -> {  // chain storm: up to 6 bolts on the closest enemies
                List<LivingEntity> es = enemies(level, p, 24);
                es.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(p)));
                int n = 0;
                for (LivingEntity e : es) {
                    if (n++ >= 6) break;
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) { bolt.setPos(e.getX(), e.getY(), e.getZ()); bolt.setCause(p); level.addFreshEntity(bolt); }
                    hurt(level, p, e, 14 * power);
                }
                if (n == 0) {
                    Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(20));
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) { bolt.setPos(at.x, at.y, at.z); bolt.setCause(p); level.addFreshEntity(bolt); }
                }
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 2));
            }
            case NATURE -> {  // full heal, regeneration III, roots everyone around
                p.heal(20 * power); p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 2)); p.removeEffect(MobEffects.POISON); p.removeEffect(MobEffects.WITHER);
                for (LivingEntity e : enemies(level, p, 12)) { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 9)); e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1)); }
                burst(level, p, ParticleTypes.HAPPY_VILLAGER, 300, 8);
            }
            case BLOOD -> {  // blood drain: heavy damage to all around, half comes back as health
                float total = 0;
                for (LivingEntity e : enemies(level, p, 12)) { hurt(level, p, e, 10 * power); total += 10 * power; e.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1)); }
                p.heal(total * 0.5f); p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
                burst(level, p, ParticleTypes.DAMAGE_INDICATOR, 200, 6);
            }
            case SHADOW -> {  // 30 s invisible, faster, every enemy forgets you
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0)); p.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 1));
                for (LivingEntity e : enemies(level, p, 40)) { if (e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == p) m.setTarget(null); e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0)); }
                burst(level, p, ParticleTypes.LARGE_SMOKE, 200, 2);
            }
            case SOUL -> {   // 3 spirit golems for 3 minutes, stronger with power
                for (int i = 0; i < 3; i++) {
                    IronGolem g = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
                    if (g == null) continue;
                    g.setPos(p.getX() + (i - 1) * 2.5, p.getY(), p.getZ() + (i == 1 ? 2 : 0));
                    g.setPlayerCreated(true);
                    g.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 3600, Math.min(4, (int) power - 1)));
                    g.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 3600, 1));
                    level.addFreshEntity(g);
                    SPIRITS.put(g.getUUID(), level.getGameTime() + 3600);
                }
                burst(level, p, ParticleTypes.SOUL, 200, 3);
            }
            case TIME -> {   // time stop: enemies frozen for 12 s, you get speed + haste
                for (LivingEntity e : enemies(level, p, 24)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 255)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 4));
                    if (e instanceof net.minecraft.world.entity.Mob m) { m.setNoAi(true); FROZEN.put(m.getUUID(), level.getGameTime() + 240); }
                }
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 3)); p.addEffect(new MobEffectInstance(MobEffects.HASTE, 240, 2));
                burst(level, p, ParticleTypes.ENCHANT, 400, 10);
            }
            case VOID -> {   // black hole: pulls in everything for 4 seconds, crushes it
                Vec3 c = p.getEyePosition().add(p.getViewVector(1f).scale(10));
                HOLES.add(new Object[]{p.getUUID(), c, level.getGameTime() + 80, power});
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 400, 2, 2, 2, 0.3);
            }
            case EARTH -> {   // quake: everything around launched, slowed and battered
                for (LivingEntity e : enemies(level, p, 12)) { e.setDeltaMovement(e.getDeltaMovement().add(0, 1.4, 0)); e.hurtMarked = true; hurt(level, p, e, 12 * power); e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3)); }
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 2));
                burst(level, p, ParticleTypes.CAMPFIRE_COSY_SMOKE, 200, 8); burst(level, p, ParticleTypes.EXPLOSION, 12, 6);
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }

    private static final Map<UUID, Long> FROZEN = new HashMap<>();
    private static final List<Object[]> HOLES = new ArrayList<>();

    /** Spirit golems vanish after one minute. */
    /** Every tick: black holes from the Void stone. */
    public static void tickHoles(ServerLevel level) {
        if (HOLES.isEmpty()) return;
        long t = level.getGameTime();
        HOLES.removeIf(h -> {
            Vec3 c = (Vec3) h[1];
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer((UUID) h[0]);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(14))) {
                if (e == owner || !(e instanceof Enemy)) continue;
                Vec3 pull = c.subtract(e.position()).normalize().scale(0.6);
                e.setDeltaMovement(pull.x, pull.y + 0.08, pull.z); e.hurtMarked = true;
                if (t % 10 == 0 && owner != null) hurt(level, owner, e, 3 * (Float) h[3]);
            }
            level.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 30, 1, 1, 1, 0.5);
            return t >= (Long) h[2];
        });
    }

    public static void tickSpirits(ServerLevel level) {
        long t = level.getGameTime();
        FROZEN.entrySet().removeIf(en -> {
            if (en.getValue() > t) return false;
            if (level.getEntity(en.getKey()) instanceof net.minecraft.world.entity.Mob m) m.setNoAi(false);
            return true;
        });
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

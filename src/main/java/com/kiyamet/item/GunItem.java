package com.kiyamet.item;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.world.NoiseSystem;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Arrow weapons: fire instantly (no bow pull), use 1 arrow per shot from the inventory, hitscan.
 * Damage grows with Archery level (+50% at 999).
 */
public class GunItem extends Item {
    public enum Mode { SINGLE, DOUBLE, SHOTGUN, BURST3, PIERCE, EXPLODE, VOID, HARPOON, TESLA, HOMING, CANNON }
    public record Stats(float damage, int cooldown, Mode mode, int noise, int effect) {}
    private final Stats stats;

    public GunItem(Properties p, Stats stats) { super(p); this.stats = stats; }

    public static Stats statsFor(String id) {
        return switch (id) {
            case "arrow_pistol" -> new Stats(5, 10, Mode.SINGLE, 20, 0);
            case "arrow_rifle" -> new Stats(10, 20, Mode.SINGLE, 24, 0);
            case "double_arrow_rifle" -> new Stats(9, 24, Mode.DOUBLE, 24, 0);
            case "arrow_shotgun" -> new Stats(4, 30, Mode.SHOTGUN, 28, 0);
            case "silenced_arrow_pistol" -> new Stats(8, 12, Mode.SINGLE, 0, 0);
            case "arrow_repeater" -> new Stats(4, 5, Mode.SINGLE, 24, 0);
            case "toxic_arrow_rifle" -> new Stats(10, 24, Mode.SINGLE, 24, 1);
            case "frost_arrow_gun" -> new Stats(12, 24, Mode.SINGLE, 24, 2);
            case "arrow_sniper" -> new Stats(30, 50, Mode.PIERCE, 40, 0);
            case "incendiary_repeater" -> new Stats(7, 28, Mode.BURST3, 28, 3);
            case "tesla_arrow_gun" -> new Stats(14, 30, Mode.TESLA, 30, 0);
            case "arrow_gatling" -> new Stats(5, 3, Mode.SINGLE, 32, 4);
            case "harpoon_gun" -> new Stats(18, 24, Mode.HARPOON, 20, 0);
            case "arrow_railgun" -> new Stats(60, 40, Mode.PIERCE, 40, 0);
            case "arrow_launcher" -> new Stats(20, 40, Mode.EXPLODE, 48, 0);
            case "void_arrow_launcher" -> new Stats(25, 60, Mode.VOID, 40, 0);
            case "homing_arrow_gun" -> new Stats(16, 20, Mode.HOMING, 24, 0);
            case "apocalypse_cannon" -> new Stats(45, 80, Mode.CANNON, 64, 0);
            default -> new Stats(5, 20, Mode.SINGLE, 20, 0);
        };
    }

    private static boolean useArrow(ServerPlayer p, int n) {
        if (p.isCreative()) return true;
        int have = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (s.is(Items.ARROW)) have += s.getCount(); }
        if (have < n) return false;
        for (int i = 0; i < p.getInventory().getContainerSize() && n > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Items.ARROW)) { int t = Math.min(n, s.getCount()); s.shrink(t); n -= t; }
        }
        return true;
    }

    private void hit(ServerLevel level, ServerPlayer p, LivingEntity e, float dmg) {
        e.hurtServer(level, level.damageSources().arrow(null, p), dmg);
        switch (stats.effect) {
            case 1 -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            case 2 -> { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2)); e.setTicksFrozen(Math.min(300, e.getTicksFrozen() + 80)); }
            case 3 -> e.igniteForSeconds(5);
            default -> {}
        }
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.min(60, d.length() * 2);
        for (int i = 0; i < n; i++) { Vec3 at = from.add(d.scale(i / (double) n)); level.sendParticles(type, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
    }

    @Override
    public InteractionResult use(Level lvl, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p) || !(lvl instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        int arrows = stats.mode == Mode.SHOTGUN ? 1 : stats.mode == Mode.BURST3 ? 3 : stats.mode == Mode.DOUBLE ? 2 : 1;
        int archery = SkillManager.get(p).level(Skill.ARCHERY);
        int cost = p.getRandom().nextFloat() < Math.min(0.4f, archery / 2500f) ? 0 : arrows;            // 1.2: high Archery sometimes saves ammo
        if (cost > 0 && !useArrow(p, cost)) { level.playSound(null, p.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.4f); return InteractionResult.FAIL; }
        float dmg = stats.damage * (1f + SkillManager.get(p).level(Skill.ARCHERY) / 2000f);
        Vec3 eye = p.getEyePosition().add(0, -0.1, 0);
        double range = stats.mode == Mode.PIERCE ? 96 : 48;
        switch (stats.mode) {
            case SINGLE, HOMING -> {
                List<LivingEntity> es = Raycast.entities(p, range, stats.mode == Mode.HOMING ? 2.5 : 0.3);
                if (!es.isEmpty()) hit(level, p, es.get(0), dmg);
                trail(level, eye, Raycast.impact(p, range), ParticleTypes.CRIT);
            }
            case DOUBLE, BURST3 -> {
                List<LivingEntity> es = Raycast.entities(p, range, 0.3);
                if (!es.isEmpty()) for (int i = 0; i < arrows; i++) hit(level, p, es.get(0), dmg);
                trail(level, eye, Raycast.impact(p, range), stats.effect == 3 ? ParticleTypes.FLAME : ParticleTypes.CRIT);
            }
            case SHOTGUN -> {
                for (LivingEntity e : Raycast.entities(p, 14, 1.4)) hit(level, p, e, dmg * 3);
                trail(level, eye, Raycast.impact(p, 14), ParticleTypes.CRIT);
            }
            case PIERCE -> {
                for (LivingEntity e : Raycast.entities(p, range, 0.3)) hit(level, p, e, dmg);
                trail(level, eye, Raycast.impact(p, range), ParticleTypes.END_ROD);
            }
            case EXPLODE, CANNON -> {
                Vec3 at = Raycast.impact(p, 64);
                level.explode(p, at.x, at.y, at.z, stats.mode == Mode.CANNON ? 4.5f : 2.5f, Level.ExplosionInteraction.NONE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(stats.mode == Mode.CANNON ? 6 : 3))) if (e != p) hit(level, p, e, dmg);
                trail(level, eye, at, ParticleTypes.SMOKE);
            }
            case VOID -> {
                Vec3 at = Raycast.impact(p, 48);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(9))) {
                    if (e == p) continue;
                    Vec3 pull = at.subtract(e.position()).normalize().scale(1.2);
                    e.setDeltaMovement(pull.x, pull.y + 0.2, pull.z); e.hurtMarked = true; hit(level, p, e, dmg);
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 150, 1.2, 1.2, 1.2, 0.2);
            }
            case HARPOON -> {
                List<LivingEntity> es = Raycast.entities(p, 32, 0.4);
                if (!es.isEmpty()) {
                    LivingEntity e = es.get(0); hit(level, p, e, dmg);
                    Vec3 pull = p.position().subtract(e.position()).normalize().scale(1.6);
                    e.setDeltaMovement(pull.x, 0.4, pull.z); e.hurtMarked = true;
                } else {
                    Vec3 at = Raycast.impact(p, 32), pull = at.subtract(p.position()).normalize().scale(1.8);
                    p.setDeltaMovement(pull.x, pull.y + 0.3, pull.z); p.hurtMarked = true;
                }
                trail(level, eye, Raycast.impact(p, 32), ParticleTypes.BUBBLE_POP);
            }
            case TESLA -> {
                List<LivingEntity> es = Raycast.entities(p, range, 0.3);
                if (!es.isEmpty()) {
                    LivingEntity first = es.get(0); hit(level, p, first, dmg);
                    int jumps = 0;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(6))) {
                        if (e == p || e == first || jumps >= 3) continue;
                        hit(level, p, e, 8); jumps++;
                        trail(level, first.position().add(0, 1, 0), e.position().add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK);
                    }
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) { bolt.setPos(first.getX(), first.getY(), first.getZ()); bolt.setVisualOnly(true); level.addFreshEntity(bolt); }
                }
                trail(level, eye, Raycast.impact(p, range), ParticleTypes.ELECTRIC_SPARK);
            }
        }
        if (stats.effect == 4) p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 1, true, false));
        level.playSound(null, p.blockPosition(), stats.noise == 0 ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS,
                stats.noise == 0 ? 0.2f : 1.0f, stats.mode == Mode.CANNON ? 0.5f : 1.3f);
        if (stats.noise > 0) NoiseSystem.make(level, p.blockPosition(), stats.noise, 6);
        p.getCooldowns().addCooldown(stack, stats.cooldown);
        return InteractionResult.CONSUME;
    }
}

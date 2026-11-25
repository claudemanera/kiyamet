package com.kiyamet.item;

import com.kiyamet.skill.Skill;
import com.kiyamet.skill.SkillManager;
import com.kiyamet.world.NoiseSystem;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The 20 lab gadgets (except healing syringe and auto turret, which have their own classes). */
public class GadgetItem extends Item {
    public static final TagKey<Structure> SCANNABLE = TagKey.create(Registries.STRUCTURE, com.kiyamet.Kiyamet.id("scannable"));
    private static final Map<UUID, Long> FROZEN = new HashMap<>();
    private static final Map<UUID, Long> CLONES = new HashMap<>();
    private final String id;

    public GadgetItem(Properties p, String id) { super(p); this.id = id; }

    private static List<LivingEntity> enemies(ServerLevel l, Vec3 at, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) if (e instanceof Enemy && e.isAlive()) out.add(e);
        return out;
    }

    @Override
    public InteractionResult use(Level lvl, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer p) || !(lvl instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        int cd = 20;
        boolean consume = false;
        switch (id) {
            case "flashlight" -> { p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, true, false)); cd = 40; }
            case "night_vision" -> { p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, true, false)); cd = 100; }
            case "sonic_lure" -> {
                BlockHitResult b = Raycast.block(p, 40);
                BlockPos at = b.getType() == HitResult.Type.MISS ? p.blockPosition() : b.getBlockPos();
                NoiseSystem.make(level, at, 48, 20);
                for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(40))) if (m instanceof Enemy) m.getNavigation().moveTo(at.getX(), at.getY(), at.getZ(), 1.0);
                level.sendParticles(ParticleTypes.NOTE, at.getX() + .5, at.getY() + 1.5, at.getZ() + .5, 20, .5, .5, .5, 1);
                level.playSound(null, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 3f, 1f);
                cd = 200; consume = true;
            }
            case "grappling_gun" -> {
                BlockHitResult b = Raycast.block(p, 36);
                if (b.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
                Vec3 d = b.getLocation().subtract(p.position()).normalize().scale(2.0);
                p.setDeltaMovement(d.x, Math.max(0.5, d.y + 0.4), d.z); p.hurtMarked = true; p.fallDistance = 0;
                cd = 30;
            }
            case "vein_miner" -> {
                BlockHitResult b = Raycast.block(p, 6);
                if (b.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
                BlockState start = level.getBlockState(b.getBlockPos());
                List<BlockPos> todo = new ArrayList<>(List.of(b.getBlockPos())), done = new ArrayList<>();
                while (!todo.isEmpty() && done.size() < 48) {
                    BlockPos at = todo.remove(0);
                    if (done.contains(at) || level.getBlockState(at).getBlock() != start.getBlock()) continue;
                    done.add(at);
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) todo.add(at.offset(dx, dy, dz));
                }
                for (BlockPos at : done) level.destroyBlock(at, true, p);
                SkillManager.addXp(p, Skill.MINING, done.size() * 3);
                cd = 40;
            }
            case "jetpack" -> {
                Vec3 v = p.getDeltaMovement();
                p.setDeltaMovement(v.x + p.getViewVector(1f).x * 0.4, 1.1, v.z + p.getViewVector(1f).z * 0.4); p.hurtMarked = true;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 30, 0.2, 0.1, 0.2, 0.02);
                cd = 30;
            }
            case "flamethrower" -> {
                Vec3 look = p.getViewVector(1f);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8))) {
                    if (e == p) continue;
                    Vec3 to = e.position().subtract(p.position());
                    if (to.length() < 8 && to.normalize().dot(look) > 0.75) { e.igniteForSeconds(5); e.hurtServer(level, level.damageSources().playerAttack(p), 4); }
                }
                for (int i = 1; i < 16; i++) { Vec3 at = p.getEyePosition().add(look.scale(i * 0.5)); level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.15 * i / 4, 0.15 * i / 4, 0.15 * i / 4, 0.01); }
                NoiseSystem.make(level, p.blockPosition(), 20, 4);
                cd = 8;
            }
            case "gravity_glove" -> {
                List<LivingEntity> es = Raycast.entities(p, 16, 0.5);
                if (es.isEmpty()) return InteractionResult.FAIL;
                LivingEntity e = es.get(0);
                Vec3 look = p.getViewVector(1f);
                e.setDeltaMovement(look.x * 2.2, 1.0, look.z * 2.2); e.hurtMarked = true;
                e.hurtServer(level, level.damageSources().playerAttack(p), 6);
                cd = 40;
            }
            case "structure_scanner" -> {
                Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, level.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE).getOrThrow(SCANNABLE), p.blockPosition(), 64, false);
                if (found == null) p.sendSystemMessage(Component.translatable("kiyamet.msg.scan_none").withStyle(ChatFormatting.GRAY));
                else {
                    BlockPos at = found.getFirst();
                    String name = found.getSecond().unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
                    int dist = (int) Math.sqrt(at.distSqr(p.blockPosition()));
                    p.sendSystemMessage(Component.translatable("kiyamet.msg.scan_found", name.replace('_', ' '), at.getX(), at.getZ(), dist).withStyle(ChatFormatting.AQUA));
                    SkillManager.addXp(p, Skill.EXPLORATION, 20);
                }
                cd = 200;
            }
            case "drone_controller" -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(48))) if (e instanceof Enemy) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                cd = 400;
            }
            case "clone_device" -> {
                IronGolem g = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED);
                if (g == null) return InteractionResult.FAIL;
                g.setPos(p.getX() + 1.5, p.getY(), p.getZ()); g.setPlayerCreated(true); level.addFreshEntity(g);
                CLONES.put(g.getUUID(), level.getGameTime() + 1800);
                cd = 2400;
            }
            case "teleport_gun" -> {
                BlockHitResult b = Raycast.block(p, 64);
                if (b.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
                BlockPos at = b.getBlockPos().relative(b.getDirection());
                p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5); p.fallDistance = 0;
                level.playSound(null, at, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
                cd = 60;
            }
            case "emp_bomb" -> {
                for (LivingEntity e : enemies(level, p.position(), 16)) { e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 5)); e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2)); }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 200, 6, 1, 6, 0.5);
                cd = 100; consume = true;
            }
            case "laser_rifle" -> {
                List<LivingEntity> es = Raycast.entities(p, 64, 0.3);
                if (!es.isEmpty()) es.get(0).hurtServer(level, level.damageSources().playerAttack(p), 16);
                Vec3 to = Raycast.impact(p, 64), from = p.getEyePosition();
                for (int i = 0; i < 40; i++) { Vec3 at = from.add(to.subtract(from).scale(i / 40.0)); level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0); }
                cd = 15;
            }
            case "shield_generator" -> {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 2)); p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 4));
                cd = 1200;
            }
            case "time_stopper" -> {
                for (LivingEntity e : enemies(level, p.position(), 24)) if (e instanceof Mob m) { m.setNoAi(true); FROZEN.put(m.getUUID(), level.getGameTime() + 100); }
                level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 300, 8, 2, 8, 0.5);
                cd = 1800;
            }
            case "black_hole_bomb" -> {
                Vec3 c = Raycast.impact(p, 32);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(14))) {
                    if (e == p) continue;
                    Vec3 pull = c.subtract(e.position()).normalize().scale(1.8);
                    e.setDeltaMovement(pull.x, pull.y + 0.3, pull.z); e.hurtMarked = true;
                    e.hurtServer(level, level.damageSources().playerAttack(p), 20);
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 400, 2, 2, 2, 0.3);
                cd = 600; consume = true;
            }
            case "energy_sword" -> {
                Vec3 look = p.getViewVector(1f);
                p.setDeltaMovement(look.x * 1.6, 0.2, look.z * 1.6); p.hurtMarked = true;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4))) {
                    if (e == p) continue;
                    if (e.position().subtract(p.position()).normalize().dot(look) > 0.4) e.hurtServer(level, level.damageSources().playerAttack(p), 22);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + look.x * 2, p.getY() + 1, p.getZ() + look.z * 2, 3, 1, 0.2, 1, 0);
                cd = 40;
            }
            case "auto_turret" -> {
                com.kiyamet.ally.TurretDrone t = com.kiyamet.ally.ModAllies.TURRET.create(level, EntitySpawnReason.TRIGGERED);
                if (t != null) {
                    Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5)).add(0, 2.2, 0);
                    t.setPos(at.x, at.y, at.z);
                    t.bonus = com.kiyamet.skill.SkillManager.get(p).level(com.kiyamet.skill.Skill.ENGINEERING) / 100f;
                    level.addFreshEntity(t);
                }
                cd = 1200;
            }
            default -> { return InteractionResult.PASS; }
        }
        SkillManager.addXp(p, Skill.ENGINEERING, 3);
        if (consume && !p.isCreative()) stack.shrink(1);
        p.getCooldowns().addCooldown(stack, cd);
        return InteractionResult.CONSUME;
    }

    /** Unfreezes time-stopped mobs and removes expired clones. */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        FROZEN.entrySet().removeIf(en -> {
            if (en.getValue() > now) return false;
            if (level.getEntity(en.getKey()) instanceof Mob m) { m.setNoAi(false); return true; }
            return false;
        });
        CLONES.entrySet().removeIf(en -> {
            if (en.getValue() > now) return false;
            var e = level.getEntity(en.getKey());
            if (e != null) { e.discard(); return true; }
            return false;
        });
    }
}
